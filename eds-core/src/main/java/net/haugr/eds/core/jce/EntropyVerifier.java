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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.haugr.eds.core.exceptions.CryptoException;

/**
 * <p>Estimates the entropy of a secret and verifies that it meets the EDS
 * minimum. The same rules apply to all secrets, regardless of whether they
 * are user passwords, passphrases or the MasterKey source data.</p>
 *
 * <p>The true entropy of a secret is a property of how it was generated,
 * and it cannot be calculated from the value itself. This class therefore
 * computes a <b>conservative estimate</b>: the number of bits an attacker
 * needs, if the attacker knows the common patterns people use. The estimate
 * follows the idea behind zxcvbn. The secret is split into the cheapest
 * combination of the following parts:</p>
 *
 * <ul>
 *   <li><b>Literals</b>, symbols without a recognised pattern. For text, each
 *       symbol costs log2 of the character pool (lower case, upper case,
 *       digits, symbols, non-ASCII). For binary data, the pool is the number
 *       of distinct byte values actually present, as binary data has no
 *       character classes.</li>
 *   <li><b>Repeats</b>, a part that also occurs earlier in the secret
 *       (e.g. {@code abcabc} or {@code aaaa}).</li>
 *   <li><b>Sequences</b>, symbols with a constant step of up to 2
 *       (e.g. {@code 12345}, {@code fedcba}, {@code 13579}).</li>
 *   <li><b>Keyboard patterns</b>, runs along a row of a US, German or French
 *       keyboard (e.g. {@code qwertz}, {@code asdfgh}), forwards or
 *       backwards. Text only.</li>
 *   <li><b>Dictionary words</b>, entries from the blocklist or the context
 *       (e.g. account name), case-insensitive, with common leetspeak
 *       substitutions ({@code p@ssw0rd}), forwards or backwards. Text
 *       only.</li>
 * </ul>
 *
 * <p>A secret is accepted when all of the following hold. The rules follow
 * NIST SP 800-63B (length over composition, blocklist checks, no character
 * class requirements):</p>
 *
 * <ol>
 *   <li>It contains at least {@value #MINIMUM_LENGTH} symbols (Unicode code
 *       points for text, bytes for binary data).</li>
 *   <li>It is not, as a whole, a blocklisted value or a context term.</li>
 *   <li>Its estimated entropy is at least {@value #MINIMUM_ENTROPY_BITS}
 *       bits.</li>
 * </ol>
 *
 * <p>The input is treated as text if it is valid UTF-8 without control
 * characters. Otherwise it is treated as binary. Only the first
 * {@value #MAX_ANALYZED_SYMBOLS} symbols are analysed, which keeps the cost
 * bounded. For longer secrets the estimate covers that prefix only, so a
 * secret is never rated higher than its prefix warrants.</p>
 *
 * <p><b>Limitations.</b> A passed check doesn't prove that the secret is
 * random. It only rejects secrets that are demonstrably weak. A passphrase of
 * common words not on the blocklist is rated by its characters, so it is
 * rated higher than a word-based attacker would need. No word list is
 * bundled, as any list is bound to a locale, and EDS must run anywhere
 * without one. This is also why passwords are still stretched with PBKDF2
 * rather than derived with HKDF alone. The bundled blocklist
 * is small and should be replaced with a larger list for production use.
 * Unicode normalisation (NFKC) is not applied here, as the normalisation must
 * match the one used when the credential is processed.</p>
 *
 * <p>Instances are immutable and thread-safe. Internal copies of the secret
 * are cleared after use; the given byte array is not modified.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class EntropyVerifier {

    /** Minimum number of symbols, following NIST SP 800-63B. */
    public static final int MINIMUM_LENGTH = 15;
    /** Minimum estimated entropy, matching the 128-bit security level. */
    public static final double MINIMUM_ENTROPY_BITS = 128.0;
    /** Upper bound on the number of analysed symbols, to bound the cost. */
    public static final int MAX_ANALYZED_SYMBOLS = 512;

    private static final String BLOCKLIST_RESOURCE = "entropy-blocklist.txt";
    private static final int MIN_PATTERN_LENGTH = 3;
    private static final int MIN_KEYBOARD_LENGTH = 4;
    private static final int MIN_WORD_LENGTH = 4;
    private static final int MAX_SEQUENCE_STEP = 2;
    // Steps -2 .. +2, i.e. five choices for the attacker.
    private static final double SEQUENCE_STEP_BITS = log2(2.0 * MAX_SEQUENCE_STEP + 1);
    private static final double REVERSED_BITS = 1.0;
    private static final double LEET_BITS = 1.0;

    private static final int POOL_LOWER = 26;
    private static final int POOL_UPPER = 26;
    private static final int POOL_DIGITS = 10;
    private static final int POOL_SYMBOLS = 33;
    // Rough size of the additional letters in a typical European alphabet.
    // Kept low on purpose, as overestimating is the dangerous direction.
    private static final int POOL_NON_ASCII = 32;

    private static final String[] KEYBOARD_ROWS = {
            "1234567890",
            // US / UK
            "qwertyuiop", "asdfghjkl", "zxcvbnm",
            // German
            "qwertzuiop", "yxcvbnm",
            // French
            "azertyuiop", "qsdfghjklm", "wxcvbn"
    };
    private static final int[][] KEYBOARD_PATTERNS = buildKeyboardPatterns();
    private static final double KEYBOARD_BITS = log2(countKeyboardSymbols());

    private static final Map<Integer, Integer> LEET = Map.ofEntries(
            Map.entry((int) '4', (int) 'a'),
            Map.entry((int) '@', (int) 'a'),
            Map.entry((int) '8', (int) 'b'),
            Map.entry((int) '(', (int) 'c'),
            Map.entry((int) '3', (int) 'e'),
            Map.entry((int) '6', (int) 'g'),
            Map.entry((int) '1', (int) 'i'),
            Map.entry((int) '!', (int) 'i'),
            Map.entry((int) '0', (int) 'o'),
            Map.entry((int) '5', (int) 's'),
            Map.entry((int) '$', (int) 's'),
            Map.entry((int) '7', (int) 't'),
            Map.entry((int) '+', (int) 't'),
            Map.entry((int) '2', (int) 'z'));

    private final Trie words;
    private final Trie reversedWords;
    private final int wordCount;

    /**
     * Reasons why a secret is rejected.
     */
    public enum Violation {

        /** The secret is null or empty. */
        EMPTY,

        /** The secret has fewer than {@value #MINIMUM_LENGTH} symbols. */
        TOO_SHORT,

        /** The secret, as a whole, is a blocklisted value or a context term. */
        BLOCKLISTED,

        /** The estimated entropy is below {@value #MINIMUM_ENTROPY_BITS} bits. */
        INSUFFICIENT_ENTROPY
    }

    /**
     * Result of a verification.
     *
     * @param estimatedBits Conservative estimate of the entropy, in bits
     * @param binary        True if the secret was analysed as binary data
     * @param violations    The rules the secret violates, empty if accepted
     */
    public record Result(double estimatedBits, boolean binary, Set<Violation> violations) {

        /**
         * Canonical constructor, making the violations immutable.
         *
         * @param estimatedBits Conservative estimate of the entropy, in bits
         * @param binary        True if the secret was analysed as binary data
         * @param violations    The rules the secret violates
         */
        public Result {
            violations = Set.copyOf(violations);
        }

        /**
         * Returns true if the secret meets all requirements.
         *
         * @return True if accepted, otherwise false
         */
        public boolean isAcceptable() {
            return violations.isEmpty();
        }
    }

    /**
     * Creates a verifier using the blocklist bundled with EDS.
     *
     * @throws CryptoException if the bundled blocklist cannot be read
     */
    public EntropyVerifier() {
        this(loadBundledBlocklist());
    }

    /**
     * Creates a verifier with the given blocklist, replacing the bundled
     * one. Entries are compared case-insensitively and with leetspeak
     * substitutions; entries shorter than four symbols are ignored, as they
     * are covered by the literal costs.
     *
     * @param blocklist Blocked words and passwords
     */
    public EntropyVerifier(final Collection<String> blocklist) {
        final Trie forward = new Trie();
        final Trie backward = new Trie();
        int count = 0;

        for (final String entry : blocklist) {
            if (entry != null) {
                final int[] normalized = normalize(entry.trim().codePoints().toArray());
                if (normalized.length >= MIN_WORD_LENGTH && forward.add(normalized)) {
                    backward.add(reverse(normalized));
                    count++;
                }
            }
        }

        this.words = forward;
        this.reversedWords = backward;
        this.wordCount = count;
    }

    /**
     * Verifies the given secret.
     *
     * @param secret  The secret to verify, as UTF-8 text or binary data
     * @param context Terms an attacker can guess from the context, such as
     *                the account name, which are treated as dictionary words
     * @return The verification result
     */
    public Result verify(final byte[] secret, final String... context) {
        if (secret == null || secret.length == 0) {
            return new Result(0.0, false, EnumSet.of(Violation.EMPTY));
        }

        final int[] text = decodeText(secret);
        final boolean binary = text == null;
        final int[] symbols = binary ? toUnsigned(secret) : text;

        try {
            return analyse(symbols, binary, buildContext(context));
        } finally {
            Arrays.fill(symbols, 0);
        }
    }

    // =========================================================================
    // Internal Logic
    // =========================================================================

    private Result analyse(final int[] all, final boolean binary, final Trie context) {
        final int length = Math.min(all.length, MAX_ANALYZED_SYMBOLS);
        final int[] original = Arrays.copyOf(all, length);
        final int[] lowered = binary ? original : lowerCase(original);
        final int[] normalized = binary ? original : normalize(original);

        try {
            final double literalBits = binary ? log2(Math.max(2, countDistinct(original))) : log2(textPool(original));
            final double dictionaryBits = log2(Math.max(2, wordCount + context.size()));
            final double[] cost = new double[length + 1];
            Arrays.fill(cost, Double.POSITIVE_INFINITY);
            cost[0] = 0.0;

            // Every pattern leads from a position to a later one, so when
            // position i is reached, cost[i] is final (shortest path in a DAG).
            for (int i = 0; i < length; i++) {
                relax(cost, i, 1, literalBits);
                addRepeats(cost, original, i);
                addSequences(cost, original, i, literalBits);
                if (!binary) {
                    addKeyboardPatterns(cost, lowered, i);
                    addWords(cost, original, lowered, normalized, i, words, dictionaryBits, 0.0);
                    addWords(cost, original, lowered, normalized, i, reversedWords, dictionaryBits, REVERSED_BITS);
                    addWords(cost, original, lowered, normalized, i, context, dictionaryBits, 0.0);
                }
            }

            final EnumSet<Violation> violations = EnumSet.noneOf(Violation.class);
            if (all.length < MINIMUM_LENGTH) {
                violations.add(Violation.TOO_SHORT);
            }
            if (!binary && all.length == length && (words.contains(normalized) || context.contains(normalized))) {
                violations.add(Violation.BLOCKLISTED);
            }
            if (cost[length] < MINIMUM_ENTROPY_BITS) {
                violations.add(Violation.INSUFFICIENT_ENTROPY);
            }

            return new Result(cost[length], binary, violations);
        } finally {
            Arrays.fill(original, 0);
            Arrays.fill(lowered, 0);
            Arrays.fill(normalized, 0);
        }
    }

    private static void relax(final double[] cost, final int from, final int length, final double bits) {
        final int to = from + length;
        if (cost[from] + bits < cost[to]) {
            cost[to] = cost[from] + bits;
        }
    }

    /**
     * A part repeating an earlier part costs the position of the earlier
     * occurrence plus the length, as in Lempel-Ziv compression.
     */
    private static void addRepeats(final double[] cost, final int[] symbols, final int i) {
        int longest = 0;
        for (int j = 0; j < i; j++) {
            int matched = 0;
            while (i + matched < symbols.length && symbols[j + matched] == symbols[i + matched]) {
                matched++;
            }
            longest = Math.max(longest, matched);
        }

        for (int len = MIN_PATTERN_LENGTH; len <= longest; len++) {
            relax(cost, i, len, 1.0 + log2(i) + log2(len));
        }
    }

    /**
     * A sequence costs its first symbol, the step and the length.
     */
    private static void addSequences(final double[] cost, final int[] symbols, final int i, final double literalBits) {
        if (i + 1 < symbols.length) {
            final int step = symbols[i + 1] - symbols[i];
            if (Math.abs(step) <= MAX_SEQUENCE_STEP) {
                int len = 2;
                while (i + len < symbols.length && symbols[i + len] - symbols[i + len - 1] == step) {
                    len++;
                }
                for (int l = MIN_PATTERN_LENGTH; l <= len; l++) {
                    relax(cost, i, l, literalBits + SEQUENCE_STEP_BITS + log2(l));
                }
            }
        }
    }

    /**
     * A keyboard pattern costs the choice of the starting key and direction,
     * plus the length.
     */
    private static void addKeyboardPatterns(final double[] cost, final int[] lowered, final int i) {
        for (final int[] row : KEYBOARD_PATTERNS) {
            for (int p = 0; p < row.length; p++) {
                int len = 0;
                while (p + len < row.length && i + len < lowered.length && row[p + len] == lowered[i + len]) {
                    len++;
                }
                for (int l = MIN_KEYBOARD_LENGTH; l <= len; l++) {
                    relax(cost, i, l, KEYBOARD_BITS + log2(l));
                }
            }
        }
    }

    /**
     * A dictionary word costs the choice of word, plus its capitalisation,
     * the use of leetspeak and the direction.
     */
    private static void addWords(final double[] cost, final int[] original, final int[] lowered, final int[] normalized,
                                 final int i, final Trie trie, final double dictionaryBits, final double extraBits) {
        Trie.Node node = trie.root;
        for (int k = i; k < normalized.length && node != null; k++) {
            node = node.children.get(normalized[k]);
            if (node != null && node.terminal) {
                final int len = k - i + 1;
                final double bits = dictionaryBits + extraBits
                        + caseBits(original, i, len)
                        + leetBits(lowered, normalized, i, len);
                relax(cost, i, len, bits);
            }
        }
    }

    private static double caseBits(final int[] original, final int from, final int length) {
        int upper = 0;
        for (int k = from; k < from + length; k++) {
            if (Character.isUpperCase(original[k])) {
                upper++;
            }
        }

        final double bits;
        if (upper == 0) {
            bits = 0.0;
        } else if (upper == length || (upper == 1 && Character.isUpperCase(original[from]))) {
            bits = 1.0;
        } else {
            // The number of ways to place the upper (or lower) case letters.
            final int smaller = Math.min(upper, length - upper);
            double variants = 0.0;
            for (int k = 1; k <= smaller; k++) {
                variants += binomial(length, k);
            }
            bits = log2(variants);
        }

        return bits;
    }

    private static double leetBits(final int[] lowered, final int[] normalized, final int from, final int length) {
        for (int k = from; k < from + length; k++) {
            if (lowered[k] != normalized[k]) {
                return LEET_BITS;
            }
        }

        return 0.0;
    }

    private static int textPool(final int[] symbols) {
        boolean lower = false;
        boolean upper = false;
        boolean digit = false;
        boolean symbol = false;
        boolean nonAscii = false;

        for (final int cp : symbols) {
            if (cp >= 'a' && cp <= 'z') {
                lower = true;
            } else if (cp >= 'A' && cp <= 'Z') {
                upper = true;
            } else if (cp >= '0' && cp <= '9') {
                digit = true;
            } else if (cp < 0x80) {
                symbol = true;
            } else {
                nonAscii = true;
            }
        }

        return (lower ? POOL_LOWER : 0)
                + (upper ? POOL_UPPER : 0)
                + (digit ? POOL_DIGITS : 0)
                + (symbol ? POOL_SYMBOLS : 0)
                + (nonAscii ? POOL_NON_ASCII : 0);
    }

    private static int countDistinct(final int[] symbols) {
        final boolean[] seen = new boolean[256];
        int distinct = 0;
        for (final int b : symbols) {
            if (!seen[b]) {
                seen[b] = true;
                distinct++;
            }
        }

        return distinct;
    }

    /**
     * Decodes the secret as strict UTF-8. Returns null if the secret is not
     * valid UTF-8 or contains control characters, i.e. if it is binary.
     */
    private static int[] decodeText(final byte[] secret) {
        final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        CharBuffer chars = null;
        try {
            chars = decoder.decode(ByteBuffer.wrap(secret));
            final int[] codePoints = chars.codePoints().toArray();
            for (final int cp : codePoints) {
                if (Character.isISOControl(cp)) {
                    Arrays.fill(codePoints, 0);
                    return null;
                }
            }

            return codePoints;
        } catch (CharacterCodingException e) {
            return null;
        } finally {
            if (chars != null && chars.hasArray()) {
                Arrays.fill(chars.array(), '\0');
            }
        }
    }

    private static int[] toUnsigned(final byte[] bytes) {
        final int[] symbols = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            symbols[i] = bytes[i] & 0xFF;
        }

        return symbols;
    }

    private static int[] lowerCase(final int[] codePoints) {
        final int[] result = new int[codePoints.length];
        for (int i = 0; i < codePoints.length; i++) {
            result[i] = Character.toLowerCase(codePoints[i]);
        }

        return result;
    }

    private static int[] normalize(final int[] codePoints) {
        final int[] result = lowerCase(codePoints);
        for (int i = 0; i < result.length; i++) {
            result[i] = LEET.getOrDefault(result[i], result[i]);
        }

        return result;
    }

    private static int[] reverse(final int[] codePoints) {
        final int[] result = new int[codePoints.length];
        for (int i = 0; i < codePoints.length; i++) {
            result[i] = codePoints[codePoints.length - 1 - i];
        }

        return result;
    }

    private static Trie buildContext(final String... context) {
        final Trie trie = new Trie();
        if (context != null) {
            for (final String term : context) {
                if (term != null) {
                    final int[] normalized = normalize(term.trim().codePoints().toArray());
                    if (normalized.length >= MIN_PATTERN_LENGTH) {
                        trie.add(normalized);
                    }
                }
            }
        }

        return trie;
    }

    private static List<String> loadBundledBlocklist() {
        try (InputStream stream = EntropyVerifier.class.getResourceAsStream(BLOCKLIST_RESOURCE)) {
            if (stream == null) {
                throw new CryptoException("The bundled entropy blocklist is missing.");
            }

            final List<String> entries = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                while (line != null) {
                    final String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        entries.add(trimmed);
                    }
                    line = reader.readLine();
                }
            }

            return entries;
        } catch (IOException e) {
            throw new CryptoException("The bundled entropy blocklist cannot be read.", e);
        }
    }

    private static int[][] buildKeyboardPatterns() {
        final int[][] patterns = new int[KEYBOARD_ROWS.length * 2][];
        for (int i = 0; i < KEYBOARD_ROWS.length; i++) {
            final int[] row = KEYBOARD_ROWS[i].codePoints().toArray();
            patterns[2 * i] = row;
            patterns[2 * i + 1] = reverse(row);
        }

        return patterns;
    }

    private static int countKeyboardSymbols() {
        int count = 0;
        for (final int[] pattern : KEYBOARD_PATTERNS) {
            count += pattern.length;
        }

        return count;
    }

    private static double binomial(final int n, final int k) {
        double result = 1.0;
        for (int i = 1; i <= k; i++) {
            result = result * (n - k + i) / i;
        }

        return result;
    }

    private static double log2(final double value) {
        return Math.log(value) / Math.log(2.0);
    }

    /**
     * Minimal prefix tree over Unicode code points, used for the dictionary
     * lookups, so no String copies of the secret are created.
     */
    private static final class Trie {

        private static final class Node {
            private final Map<Integer, Node> children = new HashMap<>();
            private boolean terminal = false;
        }

        private final Node root = new Node();
        private int size = 0;

        private boolean add(final int[] word) {
            Node node = root;
            for (final int cp : word) {
                node = node.children.computeIfAbsent(cp, key -> new Node());
            }

            final boolean added = !node.terminal;
            node.terminal = true;
            if (added) {
                size++;
            }

            return added;
        }

        private boolean contains(final int[] word) {
            Node node = root;
            for (int i = 0; i < word.length && node != null; i++) {
                node = node.children.get(word[i]);
            }

            return node != null && node.terminal;
        }

        private int size() {
            return size;
        }
    }
}
