/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.core.jce;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.haugr.eds.core.jce.EntropyVerifier.Result;
import net.haugr.eds.core.jce.EntropyVerifier.Violation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for the {@link EntropyVerifier}. Random input is generated with a
 * fixed seed, so the tests are deterministic.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class EntropyVerifierTest {

    private static final EntropyVerifier VERIFIER = new EntropyVerifier();
    private static final String PRINTABLE = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

    // =========================================================================
    // Rejected Secrets
    // =========================================================================

    @Test
    void testNullAndEmptyAreRejected() {
        assertEquals(Set.of(Violation.EMPTY), VERIFIER.verify(null).violations());
        assertEquals(Set.of(Violation.EMPTY), VERIFIER.verify(new byte[0]).violations());
    }

    @Test
    void testShortRandomPasswordIsRejected() {
        // 12 random characters; well chosen, but too short by both rules.
        final Result result = verify("q7#Lm2!xZp9@");

        assertFalse(result.isAcceptable());
        assertTrue(result.violations().contains(Violation.TOO_SHORT));
        assertTrue(result.violations().contains(Violation.INSUFFICIENT_ENTROPY));
    }

    @Test
    void testBlocklistedPasswordIsRejected() {
        final Result result = verify("password");

        assertTrue(result.violations().contains(Violation.BLOCKLISTED));
        assertTrue(result.estimatedBits() < 16, "Estimate: " + result.estimatedBits());
    }

    @Test
    void testLeetspeakAndCaseDoNotHideBlocklistedWords() {
        final Result result = verify("P@ssW0rd");

        assertTrue(result.violations().contains(Violation.BLOCKLISTED));
        assertTrue(result.estimatedBits() < 20, "Estimate: " + result.estimatedBits());
    }

    @Test
    void testTypicalComplexityRulePasswordIsRejected() {
        // Satisfies classic composition rules, but is a word plus patterns.
        final Result result = verify("Password123456!!!!");

        assertFalse(result.isAcceptable());
        assertEquals(Set.of(Violation.INSUFFICIENT_ENTROPY), result.violations());
        assertTrue(result.estimatedBits() < 50, "Estimate: " + result.estimatedBits());
    }

    /**
     * Repeats, sequences and keyboard patterns (German keyboard rows,
     * partly reversed) are detected, each estimated below its bound.
     */
    @ParameterizedTest
    @CsvSource({
            "abcXYZ123abcXYZ123abcXYZ123abcXYZ123, 60",
            "abcdefghijklmnopqrstuvwxyz0123456789, 40",
            "qwertzuiopasdfghjklmnbvcxy, 50"
    })
    void testPatternsAreDetected(final String secret, final int bound) {
        final Result result = verify(secret);

        assertFalse(result.isAcceptable());
        assertTrue(result.estimatedBits() < bound, "Estimate: " + result.estimatedBits());
    }

    @Test
    void testReversedWordsAreDetected() {
        final Result forward = verify("sunshinefootball");
        final Result backward = verify("llabtooffenihsnus");

        assertFalse(backward.isAcceptable());
        assertTrue(backward.estimatedBits() < 30, "Estimate: " + backward.estimatedBits());
        assertTrue(Math.abs(forward.estimatedBits() - backward.estimatedBits()) < 10);
    }

    @Test
    void testContextTermsAreDetected() {
        final String secret = "KimJensenKimJensen2026";
        final Result withoutContext = verify(secret);
        final Result withContext = VERIFIER.verify(bytes(secret), "kim", "jensen");

        assertTrue(withContext.estimatedBits() < withoutContext.estimatedBits());
        assertTrue(withContext.estimatedBits() < 50, "Estimate: " + withContext.estimatedBits());
    }

    @Test
    void testContextTermAsWholeIsBlocklisted() {
        final Result result = VERIFIER.verify(bytes("Account.Name.Example"), "account.name.example");

        assertTrue(result.violations().contains(Violation.BLOCKLISTED));
    }

    @Test
    void testShortPassphraseIsRejected() {
        // Allowed by NIST's 15 character minimum, but far below 128 bits.
        final Result result = verify("sunflowerpianos");

        assertEquals(Set.of(Violation.INSUFFICIENT_ENTROPY), result.violations());
    }

    @Test
    void testShortBinaryKeyIsRejected() {
        // 16 random bytes; the observed alphabet keeps the estimate low.
        final Result result = VERIFIER.verify(randomBytes(16, 1L));

        assertTrue(result.binary());
        assertTrue(result.violations().contains(Violation.INSUFFICIENT_ENTROPY));
    }

    @Test
    void testZeroKeyIsRejected() {
        final Result result = VERIFIER.verify(new byte[64]);

        assertTrue(result.binary());
        assertEquals(Set.of(Violation.INSUFFICIENT_ENTROPY), result.violations());
        assertTrue(result.estimatedBits() < 20, "Estimate: " + result.estimatedBits());
    }

    @Test
    void testLowDiversityBinaryIsRejected() {
        // 64 random bytes, but only using the values 0 and 1, i.e. 64 bits.
        final byte[] key = randomBytes(64, 2L);
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) (key[i] & 1);
        }
        final Result result = VERIFIER.verify(key);

        assertTrue(result.binary());
        assertEquals(Set.of(Violation.INSUFFICIENT_ENTROPY), result.violations());
    }

    @Test
    void testLongInputWithWeakPrefixIsRejected() {
        // Only the first 512 symbols are analysed; a weak prefix fails.
        final byte[] key = new byte[2048];
        final byte[] tail = randomBytes(1024, 3L);
        System.arraycopy(tail, 0, key, 1024, tail.length);

        assertFalse(VERIFIER.verify(key).isAcceptable());
    }

    // =========================================================================
    // Accepted Secrets
    // =========================================================================

    @Test
    void testRandomPasswordIsAccepted() {
        final Result result = verify(randomText(24, 4L));

        assertTrue(result.isAcceptable(), "Estimate: " + result.estimatedBits());
        assertFalse(result.binary());
    }

    @Test
    void testLongPassphraseIsAccepted() {
        final Result result = verify("Ferry-Lantern-Quietly-Orbits-Vintage-Pebble");

        assertTrue(result.isAcceptable(), "Estimate: " + result.estimatedBits());
    }

    @Test
    void testUnicodePassphraseIsAccepted() {
        final Result result = verify("Grüße aus Kopenhagen, wo Möwen über Hafenkränen kreisen");

        assertTrue(result.isAcceptable(), "Estimate: " + result.estimatedBits());
        assertFalse(result.binary());
    }

    @Test
    void testRandomBinaryMasterKeyIsAccepted() {
        final Result result = VERIFIER.verify(randomBytes(32, 5L));

        assertTrue(result.binary());
        assertTrue(result.isAcceptable(), "Estimate: " + result.estimatedBits());
    }

    @Test
    void testBase64MasterKeyIsAccepted() {
        // As generated with: openssl rand -base64 48
        final byte[] key = bytes(Base64.getEncoder().encodeToString(randomBytes(48, 6L)));
        final Result result = VERIFIER.verify(key);

        assertFalse(result.binary());
        assertTrue(result.isAcceptable(), "Estimate: " + result.estimatedBits());
    }

    @Test
    void testHexMasterKeyIsAccepted() {
        // As generated with: openssl rand -hex 32
        final byte[] key = bytes(HexFormat.of().formatHex(randomBytes(32, 7L)));
        final Result result = VERIFIER.verify(key);

        assertTrue(result.isAcceptable(), "Estimate: " + result.estimatedBits());
    }

    @Test
    void testLongRandomKeyIsAccepted() {
        final Result result = VERIFIER.verify(randomBytes(4096, 8L));

        assertTrue(result.isAcceptable(), "Estimate: " + result.estimatedBits());
    }

    // =========================================================================
    // General Behaviour
    // =========================================================================

    @Test
    void testSameRulesForPasswordsAndKeys() {
        // The same text, given as password or as MasterKey data, gives the
        // same result, as the verifier has no notion of the secret's purpose.
        final String secret = randomText(30, 9L);

        assertEquals(verify(secret), VERIFIER.verify(bytes(secret)));
    }

    @Test
    void testInputIsNotModified() {
        final byte[] secret = randomBytes(64, 10L);
        final byte[] copy = Arrays.copyOf(secret, secret.length);
        VERIFIER.verify(secret);

        assertArrayEquals(copy, secret);
    }

    @Test
    void testCustomBlocklistReplacesBundled() {
        final EntropyVerifier custom = new EntropyVerifier(List.of("rumpelstilzchen"));

        assertTrue(custom.verify(bytes("Rumpelstilzchen")).violations().contains(Violation.BLOCKLISTED));
        assertFalse(custom.verify(bytes("password")).violations().contains(Violation.BLOCKLISTED));
    }

    @Test
    void testOnlyWholeEntriesOfTheBlocklistAreBlocked() {
        final EntropyVerifier custom = new EntropyVerifier(Arrays.asList(null, "rumpelstilzchen", " "));

        // A part of an entry is neither blocked nor a word of the blocklist.
        assertFalse(custom.verify(bytes("rumpelstilz")).violations().contains(Violation.BLOCKLISTED));
        assertTrue(custom.verify(bytes("rumpelstilz")).estimatedBits() > custom.verify(bytes("rumpelstilzchen")).estimatedBits());
    }

    @Test
    void testAnUpperCaseLetterWithinAWordCostsMoreThanAtItsStart() {
        final EntropyVerifier custom = new EntropyVerifier(List.of("rumpelstilzchen"));

        final double first = custom.verify(bytes("Rumpelstilzchen")).estimatedBits();
        final double within = custom.verify(bytes("rumpelStilzchen")).estimatedBits();

        assertTrue(within > first, within + " should be more than " + first);
        final double two = custom.verify(bytes("RumpelStilzchen")).estimatedBits();
        assertTrue(two > first, two + " should be more than " + first);
        // All in upper case is as easily guessed as the first letter.
        assertEquals(first, custom.verify(bytes("RUMPELSTILZCHEN")).estimatedBits(), 0.000001);
    }

    @Test
    void testUpperCaseOnlyCountsAsOneAlphabet() {
        final Result upper = verify("ZXQJVKWPBGMFHDY");
        final Result lower = verify("zxqjvkwpbgmfhdy");

        assertEquals(lower.estimatedBits(), upper.estimatedBits(), 0.000001);
        assertEquals(lower.violations(), upper.violations());
    }

    @Test
    void testMissingContextTermsAreIgnored() {
        final String secret = "KimJensenKimJensen2026";
        final double without = verify(secret).estimatedBits();

        assertEquals(without, VERIFIER.verify(bytes(secret), (String[]) null).estimatedBits(), 0.000001);
        assertEquals(VERIFIER.verify(bytes(secret), "kim").estimatedBits(), VERIFIER.verify(bytes(secret), "kim", null).estimatedBits(), 0.000001);
    }

    @Test
    void testViolationsAreImmutable() {
        final Result result = verify("password");

        final Set<Violation> violations = result.violations();

        assertThrows(UnsupportedOperationException.class, violations::clear);
    }

    @Test
    void testWorstCaseInputIsFast() {
        // Highly repetitive input is the worst case for the repeat search.
        final byte[] secret = bytes("a".repeat(100_000));

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> VERIFIER.verify(secret));
    }

    // =========================================================================
    // Internal Helpers
    // =========================================================================

    private static Result verify(final String secret) {
        return VERIFIER.verify(bytes(secret));
    }

    private static byte[] bytes(final String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    // Seeded on purpose: the test data must be the same in every run.
    @SuppressWarnings("findsecbugs:PREDICTABLE_RANDOM")
    private static byte[] randomBytes(final int length, final long seed) {
        final byte[] bytes = new byte[length];
        new Random(seed).nextBytes(bytes);

        return bytes;
    }

    private static String randomText(final int length, final long seed) {
        // Seeded on purpose: the test data must be the same in every run.
        @SuppressWarnings("findsecbugs:PREDICTABLE_RANDOM")
        final Random random = new Random(seed);
        final StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(PRINTABLE.charAt(random.nextInt(PRINTABLE.length())));
        }

        return builder.toString();
    }
}
