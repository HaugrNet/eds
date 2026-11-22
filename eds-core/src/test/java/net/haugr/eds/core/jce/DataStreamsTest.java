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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Properties;
import java.util.Random;
import javax.crypto.spec.SecretKeySpec;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * <p>Tests for the streaming crypto API (PLAN.md, Crypto, phase C5): streamed round
 * trips with bounded memory, range decryption at and around chunk boundaries,
 * rejection of tampered chunk sequences, re-encryption, and the C4 test
 * vectors.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class DataStreamsTest extends DatabaseSetup {

    private static final HexFormat HEX = HexFormat.of();
    private static final DataFormat.Context CONTEXT = new DataFormat.Context("circle", "object", 1);
    private static final int SMALL_CHUNK = 32;

    @ParameterizedTest
    @ValueSource(strings = { "empty", "partial", "exact", "multi" })
    void testVectors(final String name) throws IOException {
        final Properties vectors = new Properties();
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("test-vectors/data-format-v1.properties")) {
            vectors.load(stream);
        }
        final SecretEDSKey secret = new SecretEDSKey(KeyAlgorithm.AES_GCM_256, new SecretKeySpec(HEX.parseHex(vectors.getProperty("secret")), "AES"));
        final DataFormat.Context context = new DataFormat.Context(vectors.getProperty("circleId"), vectors.getProperty("objectId"), Long.parseLong(vectors.getProperty("circleSecretVersion")));
        final byte[] plain = HEX.parseHex(vectors.getProperty(name + ".plaintext"));
        final byte[] stored = HEX.parseHex(vectors.getProperty(name + ".stored"));

        final DataStreams.EncryptingStream encrypting = DataStreams.encrypt(secret, context, new ByteArrayInputStream(plain), plain.length,
                Integer.parseInt(vectors.getProperty(name + ".chunkSize")), HEX.parseHex(vectors.getProperty("salt")), HEX.parseHex(vectors.getProperty("prefix")));
        assertEquals(stored.length, encrypting.storedLength());
        assertArrayEquals(stored, encrypting.readAllBytes());
        assertArrayEquals(plain, DataStreams.decrypt(secret, context, new ByteArrayInputStream(stored), stored.length).readAllBytes());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 31, 32, 33, 63, 64, 65, 100, 1000 })
    void testStreamsMatchTheByteArrayFormat(final int size) throws IOException {
        final SecretEDSKey secret = newSecret();
        final byte[] plain = bytes(size);

        // Streamed encryption, decrypted by the byte[] API ...
        final byte[] streamed = DataStreams.encrypt(secret, CONTEXT, new ByteArrayInputStream(plain), size, SMALL_CHUNK, bytes(32), bytes(7)).readAllBytes();
        assertEquals(DataFormat.ciphertextLength(size, SMALL_CHUNK), streamed.length);
        assertArrayEquals(plain, DataFormat.decrypt(secret, CONTEXT, streamed));

        // A source of unknown length gives the very same bytes, and knows
        // the stored length once it has been read.
        final DataStreams.EncryptingStream toTheEnd = DataStreams.encrypt(secret, CONTEXT, new ByteArrayInputStream(plain), DataStreams.UNKNOWN_LENGTH, SMALL_CHUNK, bytes(32), bytes(7));
        assertArrayEquals(streamed, toTheEnd.readAllBytes());
        assertEquals(streamed.length, toTheEnd.storedLength());

        // ... and the byte[] encryption, decrypted as a stream.
        final byte[] stored = DataFormat.encrypt(secret, CONTEXT, plain);
        assertArrayEquals(plain, DataStreams.decrypt(secret, CONTEXT, new ByteArrayInputStream(stored), stored.length).readAllBytes());
    }

    /**
     * A 64 MiB object is encrypted and decrypted as a chain of streams, with
     * no full copy of it anywhere. The encrypting stream never reads more
     * than one chunk ahead of what it has produced.
     */
    @Test
    void testLargeStreamWithBoundedMemory() throws Exception {
        final SecretEDSKey secret = newSecret();
        final long size = 64L * 1024 * 1024 + 123;
        final MessageDigest expected = MessageDigest.getInstance("SHA-512");
        final MessageDigest actual = MessageDigest.getInstance("SHA-512");

        try (CountingSource source = new CountingSource(size, expected);
             DataStreams.EncryptingStream encrypting = DataStreams.encrypt(secret, CONTEXT, source, size);
             CountingStream stored = new CountingStream(encrypting);
             InputStream decrypting = DataStreams.decrypt(secret, CONTEXT, stored, encrypting.storedLength())) {
            final byte[] buffer = new byte[10_000];
            long total = 0;
            int read = decrypting.read(buffer);
            while (read != -1) {
                actual.update(buffer, 0, read);
                total += read;
                // Bounded read-ahead: the source is at most one chunk (plus the
                // chunk in the decryptor) ahead of the plaintext delivered.
                assertTrue(source.consumed - total <= 2L * DataFormat.DEFAULT_CHUNK_SIZE, "read-ahead " + (source.consumed - total));
                read = decrypting.read(buffer);
            }

            assertEquals(size, total);
            assertEquals(encrypting.storedLength(), stored.count);
        }
        assertArrayEquals(expected.digest(), actual.digest());
    }

    @Test
    void testRangesAroundChunkBoundaries() throws IOException {
        final SecretEDSKey secret = newSecret();
        final byte[] plain = bytes(100);
        final byte[] stored = DataFormat.encrypt(secret, CONTEXT, plain, SMALL_CHUNK, bytes(32), bytes(7));
        final DataFormat.Header header = DataFormat.Header.read(stored);
        final int[] points = { 0, 1, 30, 31, 32, 33, 63, 64, 65, 95, 96, 97, 98, 99 };

        for (final int from : points) {
            for (final int to : points) {
                if (to < from) {
                    continue;
                }
                final DataStreams.Range range = DataStreams.plan(header, stored.length, from, to);
                assertEquals(from / SMALL_CHUNK, range.firstChunk());
                assertEquals(to / SMALL_CHUNK, range.lastChunk());
                final byte[] slice = Arrays.copyOfRange(stored, (int) range.storedOffset(), (int) (range.storedOffset() + range.storedLength()));
                final byte[] result = DataStreams.decrypt(secret, CONTEXT, header, stored.length, range, new ByteArrayInputStream(slice)).readAllBytes();
                assertArrayEquals(Arrays.copyOfRange(plain, from, to + 1), result, "Range " + from + '-' + to);
            }
        }
    }

    @Test
    void testRangeWithDefaultChunkSize() throws IOException {
        final SecretEDSKey secret = newSecret();
        final byte[] plain = bytes(3 * DataFormat.DEFAULT_CHUNK_SIZE + 10);
        final byte[] stored = DataFormat.encrypt(secret, CONTEXT, plain);
        final DataFormat.Header header = DataFormat.Header.read(stored);

        // A small read in the middle only needs the one chunk around it.
        final DataStreams.Range range = DataStreams.plan(header, stored.length, 70_000, 70_099);
        assertEquals(1, range.firstChunk());
        assertEquals(1, range.lastChunk());
        assertEquals(DataFormat.DEFAULT_CHUNK_SIZE + DataFormat.TAG_BYTES, range.storedLength());
        final byte[] slice = Arrays.copyOfRange(stored, (int) range.storedOffset(), (int) (range.storedOffset() + range.storedLength()));
        assertArrayEquals(Arrays.copyOfRange(plain, 70_000, 70_100), DataStreams.decrypt(secret, CONTEXT, header, stored.length, range, new ByteArrayInputStream(slice)).readAllBytes());
    }

    @Test
    void testInvalidRanges() {
        final byte[] stored = DataFormat.encrypt(newSecret(), CONTEXT, bytes(100));
        final DataFormat.Header header = DataFormat.Header.read(stored);

        assertThrows(EDSException.class, () -> DataStreams.plan(header, stored.length, -1, 10));
        assertThrows(EDSException.class, () -> DataStreams.plan(header, stored.length, 10, 9));
        assertThrows(EDSException.class, () -> DataStreams.plan(header, stored.length, 0, 100));
    }

    @Test
    void testTamperedStreamsAreRejected() {
        final SecretEDSKey secret = newSecret();
        final int sealed = SMALL_CHUNK + DataFormat.TAG_BYTES;
        final int header = DataFormat.HEADER_BYTES;
        final byte[] stored = DataFormat.encrypt(secret, CONTEXT, bytes(4 * SMALL_CHUNK), SMALL_CHUNK, bytes(32), bytes(7));

        final byte[] reordered = Arrays.copyOf(stored, stored.length);
        System.arraycopy(stored, header + sealed, reordered, header, sealed);
        System.arraycopy(stored, header, reordered, header + sealed, sealed);
        assertRejected(secret, reordered, reordered.length);

        // Truncated at a chunk boundary, with the stored length to match.
        final byte[] truncated = Arrays.copyOf(stored, stored.length - sealed);
        assertRejected(secret, truncated, truncated.length);

        // Truncated: fewer bytes than the declared stored length.
        assertRejected(secret, Arrays.copyOf(stored, stored.length - 5), stored.length);

        // Extended: more bytes than the declared stored length.
        final byte[] extended = Arrays.copyOf(stored, stored.length + sealed);
        System.arraycopy(stored, stored.length - sealed, extended, stored.length, sealed);
        assertRejected(secret, extended, stored.length);

        final byte[] flipped = Arrays.copyOf(stored, stored.length);
        flipped[header + sealed + 3] ^= 1;
        assertRejected(secret, flipped, flipped.length);
    }

    @Test
    void testDeclaredLengthMustMatch() {
        final SecretEDSKey secret = newSecret();

        final IOException shorter = assertThrows(IOException.class, () -> DataStreams.encrypt(secret, CONTEXT, new ByteArrayInputStream(bytes(10)), 20).readAllBytes());
        assertEquals("The plaintext is shorter than the declared 20 bytes.", shorter.getMessage());
        final IOException longer = assertThrows(IOException.class, () -> DataStreams.encrypt(secret, CONTEXT, new ByteArrayInputStream(bytes(30)), 20).readAllBytes());
        assertEquals("The plaintext is longer than the declared 20 bytes.", longer.getMessage());
    }

    @Test
    void testNegativeDeclaredLengthIsRejected() {
        final SecretEDSKey secret = newSecret();
        final ByteArrayInputStream plain = new ByteArrayInputStream(bytes(10));

        final EDSException cause = assertThrows(EDSException.class, () -> DataStreams.encrypt(secret, CONTEXT, plain, -2));
        assertEquals("The plaintext length must not be negative.", cause.getMessage());
    }

    /**
     * Reading one byte at a time gives the same bytes as reading in blocks,
     * both when encrypting and when decrypting a range.
     */
    @Test
    void testSingleByteReads() throws IOException {
        final SecretEDSKey secret = newSecret();
        final byte[] plain = bytes(100);
        final byte[] salt = bytes(32);
        final byte[] prefix = bytes(7);
        final byte[] expected = DataFormat.encrypt(secret, CONTEXT, plain, SMALL_CHUNK, salt, prefix);

        try (DataStreams.EncryptingStream encrypting = DataStreams.encrypt(secret, CONTEXT, new ByteArrayInputStream(plain), plain.length, SMALL_CHUNK, salt, prefix)) {
            assertArrayEquals(expected, readByteByByte(encrypting));
        }

        final DataFormat.Header header = DataFormat.Header.read(expected);
        final DataStreams.Range range = DataStreams.plan(header, expected.length, 30, 70);
        final byte[] slice = Arrays.copyOfRange(expected, (int) range.storedOffset(), (int) (range.storedOffset() + range.storedLength()));
        try (InputStream decrypting = DataStreams.decrypt(secret, CONTEXT, header, expected.length, range, new ByteArrayInputStream(slice))) {
            assertArrayEquals(Arrays.copyOfRange(plain, 30, 71), readByteByByte(decrypting));
        }
    }

    /**
     * An empty object has one empty chunk, which is verified on the first
     * read; reading again after the end stays at the end.
     */
    @Test
    void testReadingAnEmptyObjectAfterItsEnd() throws IOException {
        final SecretEDSKey secret = newSecret();
        final byte[] stored = DataFormat.encrypt(secret, CONTEXT, new byte[0]);

        try (InputStream decrypting = DataStreams.decrypt(secret, CONTEXT, new ByteArrayInputStream(stored), stored.length)) {
            assertEquals(-1, decrypting.read());
            assertEquals(-1, decrypting.read());
            assertEquals(-1, decrypting.read(new byte[10], 0, 10));
            assertEquals(0, decrypting.read(new byte[10], 0, 0));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 10, 65536, 200_000 })
    void testReencryption(final int size) throws IOException {
        final SecretEDSKey sourceSecret = newSecret();
        final SecretEDSKey targetSecret = newSecret();
        final DataFormat.Context target = new DataFormat.Context("other circle", "copied object", 3);
        final byte[] plain = bytes(size);
        final byte[] stored = DataFormat.encrypt(sourceSecret, CONTEXT, plain);

        final DataStreams.EncryptingStream reencrypted = DataStreams.reencrypt(sourceSecret, CONTEXT, new ByteArrayInputStream(stored), stored.length, targetSecret, target);
        final byte[] result = reencrypted.readAllBytes();
        assertEquals(reencrypted.storedLength(), result.length);
        assertArrayEquals(plain, DataFormat.decrypt(targetSecret, target, result));
        assertThrows(EDSException.class, () -> DataFormat.decrypt(sourceSecret, CONTEXT, result));
    }

    /**
     * The rotation of a Circle Secret keeps the header, and so the ETag, of
     * the same content, while the key changes with the secret and its
     * version (PLAN.md, Crypto C6).
     */
    @ParameterizedTest
    @ValueSource(ints = { 0, 10, 65536, 200_000 })
    void testRotation(final int size) throws IOException {
        final SecretEDSKey retiring = newSecret();
        final SecretEDSKey current = newSecret();
        final DataFormat.Context rotated = new DataFormat.Context("circle", "object", 2);
        final byte[] plain = bytes(size);
        final byte[] stored = DataFormat.encrypt(retiring, CONTEXT, plain);

        final byte[] result = DataStreams.rotate(retiring, CONTEXT, new ByteArrayInputStream(stored), stored.length, current, rotated).readAllBytes();
        assertArrayEquals(plain, DataFormat.decrypt(current, rotated, result));
        assertArrayEquals(Arrays.copyOf(stored, DataFormat.HEADER_BYTES), Arrays.copyOf(result, DataFormat.HEADER_BYTES));
        assertEquals(DataFormat.etag(DataFormat.Header.read(stored)), DataFormat.etag(DataFormat.Header.read(result)));
        assertNotEquals(HEX.formatHex(stored), HEX.formatHex(result));
        assertThrows(EDSException.class, () -> DataFormat.decrypt(retiring, CONTEXT, result));
        // The same version again would use the same key and nonces.
        final ByteArrayInputStream again = new ByteArrayInputStream(stored);
        assertThrows(EDSException.class, () -> DataStreams.rotate(retiring, CONTEXT, again, stored.length, current, CONTEXT));
    }

    @Test
    void testReencryptionOfTamperedSourceFails() {
        final SecretEDSKey secret = newSecret();
        final byte[] stored = DataFormat.encrypt(secret, CONTEXT, bytes(200_000));
        stored[stored.length - 1] ^= 1;

        final SecretEDSKey target = newSecret();

        assertThrows(EDSException.class, () -> reencryptAll(secret, stored, target));
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private static void assertRejected(final SecretEDSKey secret, final byte[] stored, final long storedLength) {
        final Exception cause = assertThrows(Exception.class, () -> decryptAll(secret, stored, storedLength));
        assertTrue((cause instanceof IOException) || (cause instanceof EDSException), cause.toString());
    }

    private static byte[] decryptAll(final SecretEDSKey secret, final byte[] stored, final long storedLength) throws IOException {
        return DataStreams.decrypt(secret, CONTEXT, new ByteArrayInputStream(stored), storedLength).readAllBytes();
    }

    private static byte[] reencryptAll(final SecretEDSKey secret, final byte[] stored, final SecretEDSKey target) throws IOException {
        return DataStreams.reencrypt(secret, CONTEXT, new ByteArrayInputStream(stored), stored.length, target, CONTEXT).readAllBytes();
    }

    private static byte[] readByteByByte(final InputStream stream) throws IOException {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        int value = stream.read();
        while (value != -1) {
            result.write(value);
            value = stream.read();
        }
        assertEquals(-1, stream.read());

        return result.toByteArray();
    }

    private static SecretEDSKey newSecret() {
        return Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
    }

    // Seeded on purpose: the test data must be the same in every run.
    @SuppressWarnings("findsecbugs:PREDICTABLE_RANDOM")
    private static byte[] bytes(final int size) {
        final byte[] bytes = new byte[size];
        new Random(size).nextBytes(bytes);
        return bytes;
    }

    /** Generates pseudo-random plaintext, counting what was read. */
    // The digest belongs to the stream, which one thread reads.
    @SuppressWarnings("java:S9428")
    private static final class CountingSource extends InputStream {
        private final long size;
        // Seeded on purpose: the test data must be the same in every run.
        @SuppressWarnings("findsecbugs:PREDICTABLE_RANDOM")
        private final Random random = new Random(42);
        private final MessageDigest digest;
        private long consumed = 0;

        private CountingSource(final long size, final MessageDigest digest) {
            this.size = size;
            this.digest = digest;
        }

        @Override
        public int read() {
            final byte[] one = new byte[1];
            return (read(one, 0, 1) == 1) ? (one[0] & 0xFF) : -1;
        }

        @Override
        public int read(final byte[] bytes, final int offset, final int length) {
            if (consumed >= size) {
                return -1;
            }
            final int count = (int) Math.min(length, size - consumed);
            final byte[] chunk = new byte[count];
            random.nextBytes(chunk);
            System.arraycopy(chunk, 0, bytes, offset, count);
            digest.update(chunk);
            consumed += count;
            return count;
        }
    }

    /** Counts the bytes passed through. */
    private static final class CountingStream extends InputStream {
        private final InputStream source;
        private long count = 0;

        private CountingStream(final InputStream source) {
            this.source = source;
        }

        @Override
        public int read() throws IOException {
            final int value = source.read();
            if (value != -1) {
                count++;
            }
            return value;
        }

        @Override
        public int read(final byte[] bytes, final int offset, final int length) throws IOException {
            final int read = source.read(bytes, offset, length);
            if (read > 0) {
                count += read;
            }
            return read;
        }
    }
}
