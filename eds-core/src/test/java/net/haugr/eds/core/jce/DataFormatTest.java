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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Properties;
import javax.crypto.spec.SecretKeySpec;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/**
 * <p>Tests for the Data Object format (PLAN.md, Crypto §4), including the published
 * test vectors in {@code test-vectors/data-format-v1.properties}, which were
 * computed independently from the specification.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class DataFormatTest extends DatabaseSetup {

    private static final HexFormat HEX = HexFormat.of();
    private static final DataFormat.Context CONTEXT = new DataFormat.Context("circle", "object", 1);

    @ParameterizedTest
    @ValueSource(strings = { "empty", "partial", "exact", "multi" })
    void testVectors(final String name) throws IOException {
        final Properties vectors = load("test-vectors/data-format-v1.properties");
        final SecretEDSKey secret = secret(HEX.parseHex(vectors.getProperty("secret")));
        final DataFormat.Context context = new DataFormat.Context(vectors.getProperty("circleId"), vectors.getProperty("objectId"), Long.parseLong(vectors.getProperty("circleSecretVersion")));
        final byte[] salt = HEX.parseHex(vectors.getProperty("salt"));
        final byte[] prefix = HEX.parseHex(vectors.getProperty("prefix"));
        final int chunkSize = Integer.parseInt(vectors.getProperty(name + ".chunkSize"));
        final byte[] plain = HEX.parseHex(vectors.getProperty(name + ".plaintext"));
        final byte[] stored = HEX.parseHex(vectors.getProperty(name + ".stored"));

        assertArrayEquals(HEX.parseHex(vectors.getProperty("objectKey")), DataFormat.objectKey(secret, context, salt).getEncoded());
        assertArrayEquals(stored, DataFormat.encrypt(secret, context, plain, chunkSize, salt, prefix));
        assertArrayEquals(plain, DataFormat.decrypt(secret, context, stored));
        assertEquals(stored.length, DataFormat.ciphertextLength(plain.length, chunkSize));
        assertEquals(plain.length, DataFormat.plaintextLength(stored.length, chunkSize));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 65535, 65536, 65537, 200_000, 3 * 65536 })
    void testRoundTripWithDefaultChunkSize(final int size) {
        final SecretEDSKey secret = Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
        final byte[] plain = generateData(size);
        final byte[] input = (plain == null) ? new byte[0] : plain;

        final byte[] stored = DataFormat.encrypt(secret, CONTEXT, input);
        assertEquals(DataFormat.ciphertextLength(input.length, DataFormat.DEFAULT_CHUNK_SIZE), stored.length);
        assertEquals(input.length, DataFormat.plaintextLength(stored.length, DataFormat.DEFAULT_CHUNK_SIZE));
        assertArrayEquals(input, DataFormat.decrypt(secret, CONTEXT, stored));
    }

    @Test
    void testEveryWriteUsesANewObjectKey() {
        final SecretEDSKey secret = Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
        final byte[] plain = generateData(100);

        final byte[] first = DataFormat.encrypt(secret, CONTEXT, plain);
        final byte[] second = DataFormat.encrypt(secret, CONTEXT, plain);
        assertFalse(Arrays.equals(DataFormat.Header.read(first).salt(), DataFormat.Header.read(second).salt()));
        assertFalse(Arrays.equals(first, second));
    }

    // The header is given as the expected value on purpose: only so does
    // the assertion call Header.equals(null), which is part of what is
    // checked; assertNotNull, or null first, would leave equals out.
    @SuppressWarnings({ "java:S2701", "java:S3415" })
    @Test
    void testHeadersOfTheSameObjectAreEqual() {
        final SecretEDSKey secret = Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
        final byte[] stored = DataFormat.encrypt(secret, CONTEXT, generateData(100));
        final byte[] other = DataFormat.encrypt(secret, CONTEXT, generateData(100));

        final DataFormat.Header header = DataFormat.Header.read(stored);
        assertEquals(header, DataFormat.Header.read(stored));
        assertEquals(header.hashCode(), DataFormat.Header.read(stored).hashCode());
        assertNotEquals(header, DataFormat.Header.read(other));
        assertNotEquals(header, null);
        assertTrue(header.toString().startsWith("Header[chunkSize=" + DataFormat.DEFAULT_CHUNK_SIZE + ", salt="), header.toString());
    }

    @Test
    void testBoundToTheObject() {
        final SecretEDSKey secret = Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
        final byte[] stored = DataFormat.encrypt(secret, CONTEXT, generateData(100));

        final DataFormat.Context otherObject = new DataFormat.Context("circle", "other object", 1);
        final DataFormat.Context otherCircle = new DataFormat.Context("other circle", "object", 1);
        final DataFormat.Context otherVersion = new DataFormat.Context("circle", "object", 2);
        final SecretEDSKey otherSecret = Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
        assertThrows(EDSException.class, () -> DataFormat.decrypt(secret, otherObject, stored));
        assertThrows(EDSException.class, () -> DataFormat.decrypt(secret, otherCircle, stored));
        assertThrows(EDSException.class, () -> DataFormat.decrypt(secret, otherVersion, stored));
        assertThrows(EDSException.class, () -> DataFormat.decrypt(otherSecret, CONTEXT, stored));
    }

    /**
     * Reordered, dropped, truncated, extended and modified chunks are all
     * detected. The chunk size is small, so there are several chunks.
     */
    @Test
    void testTamperedChunksAreRejected() {
        final SecretEDSKey secret = Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
        final int chunk = 32;
        final int sealed = chunk + DataFormat.TAG_BYTES;
        final byte[] stored = DataFormat.encrypt(secret, CONTEXT, generateData(4 * chunk), chunk, generateData(32), generateData(7));
        final int header = DataFormat.HEADER_BYTES;
        assertEquals(header + 4 * sealed, stored.length);

        // Swapping the first two chunks.
        final byte[] reordered = Arrays.copyOf(stored, stored.length);
        System.arraycopy(stored, header + sealed, reordered, header, sealed);
        System.arraycopy(stored, header, reordered, header + sealed, sealed);
        assertThrows(EDSException.class, () -> DataFormat.decrypt(secret, CONTEXT, reordered));

        // Dropping the last chunk (truncation at a chunk boundary).
        final byte[] truncated = Arrays.copyOf(stored, stored.length - sealed);
        assertThrows(EDSException.class, () -> DataFormat.decrypt(secret, CONTEXT, truncated));

        // Dropping a chunk in the middle.
        final byte[] dropped = new byte[stored.length - sealed];
        System.arraycopy(stored, 0, dropped, 0, header + sealed);
        System.arraycopy(stored, header + 2 * sealed, dropped, header + sealed, 2 * sealed);
        assertThrows(EDSException.class, () -> DataFormat.decrypt(secret, CONTEXT, dropped));

        // Extending with a copy of the last chunk.
        final byte[] extended = Arrays.copyOf(stored, stored.length + sealed);
        System.arraycopy(stored, stored.length - sealed, extended, stored.length, sealed);
        assertThrows(EDSException.class, () -> DataFormat.decrypt(secret, CONTEXT, extended));

        // Truncation inside a chunk, a flipped bit, and a changed header.
        final byte[] cut = Arrays.copyOf(stored, stored.length - 5);
        assertThrows(EDSException.class, () -> DataFormat.decrypt(secret, CONTEXT, cut));
        for (final int position : new int[] { 0, 3, 10, header - 1, header + 1, stored.length - 1 }) {
            final byte[] modified = Arrays.copyOf(stored, stored.length);
            modified[position] ^= 0x01;
            assertThrows(EDSException.class, () -> DataFormat.decrypt(secret, CONTEXT, modified), "Modified at " + position);
        }
    }

    // Null is passed on purpose, to check how it is refused.
    @SuppressWarnings("findbugs:NP_NULL_PARAM_DEREF_NONVIRTUAL")
    @Test
    void testInvalidHeaders() {
        final SecretEDSKey secret = Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
        assertThrows(EDSException.class, () -> DataFormat.decrypt(secret, CONTEXT, new byte[10]));
        assertThrows(EDSException.class, () -> DataFormat.decrypt(secret, CONTEXT, null));

        final byte[] stored = DataFormat.encrypt(secret, CONTEXT, generateData(10));
        final byte[] headerOnly = Arrays.copyOf(stored, DataFormat.HEADER_BYTES);
        assertThrows(EDSException.class, () -> DataFormat.decrypt(secret, CONTEXT, headerOnly));
        final byte[] version = Arrays.copyOf(stored, stored.length);
        version[0] = 2;
        final EDSException cause = assertThrows(EDSException.class, () -> DataFormat.decrypt(secret, CONTEXT, version));
        assertEquals("Unsupported data format version 2.", cause.getMessage());
    }

    /**
     * Headers are only equal if every field is; the encoded bytes are
     * compared too, although they are normally derived from the others.
     */
    @Test
    void testHeadersDifferingInOneField() {
        final byte[] salt = generateData(DataFormat.SALT_BYTES);
        final byte[] prefix = generateData(DataFormat.PREFIX_BYTES);
        final DataFormat.Header header = DataFormat.Header.create(64, salt, prefix);

        assertEquals(header, DataFormat.Header.create(64, salt.clone(), prefix.clone()));
        assertNotEquals(header, DataFormat.Header.create(128, salt, prefix));
        assertNotEquals(header, DataFormat.Header.create(64, flipped(salt), prefix));
        assertNotEquals(header, DataFormat.Header.create(64, salt, flipped(prefix)));
        assertNotEquals(header, new DataFormat.Header(64, salt, prefix, flipped(header.encoded())));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, DataFormat.MIN_CHUNK_SIZE - 1, DataFormat.MAX_CHUNK_SIZE + 1, -1 })
    void testInvalidChunkSizes(final int chunkSize) {
        final byte[] stored = DataFormat.encrypt(Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256), CONTEXT, generateData(10));
        ByteBuffer.wrap(stored).putInt(1, chunkSize);

        final EDSException cause = assertThrows(EDSException.class, () -> DataFormat.Header.read(stored));
        assertEquals("Invalid chunk size " + chunkSize + '.', cause.getMessage());
    }

    @Test
    void testImpossibleStoredLengths() {
        final int chunk = DataFormat.MIN_CHUNK_SIZE;
        final long sealed = (long) chunk + DataFormat.TAG_BYTES;

        // Shorter than one tag, in the only or in the last chunk.
        final EDSException empty = assertThrows(EDSException.class, () -> DataFormat.plaintextLength(DataFormat.HEADER_BYTES + DataFormat.TAG_BYTES - 1L, chunk));
        assertEquals("The stored data is truncated.", empty.getMessage());
        final EDSException last = assertThrows(EDSException.class, () -> DataFormat.plaintextLength(DataFormat.HEADER_BYTES + sealed + 5, chunk));
        assertEquals("The stored data is truncated.", last.getMessage());

        // More chunks than the 32 bit chunk counter of the nonce can number.
        final EDSException many = assertThrows(EDSException.class, () -> DataFormat.plaintextLength(DataFormat.HEADER_BYTES + (0x1_0000_0000L + 1) * sealed, chunk));
        assertEquals("The stored data has too many chunks.", many.getMessage());
        assertEquals(0xFFFF_FFFFL * chunk, DataFormat.plaintextLength(DataFormat.HEADER_BYTES + 0xFFFF_FFFFL * sealed, chunk));
    }

    @Test
    void testHybridKemVector() throws Exception {
        final Properties vector = load("test-vectors/hybrid-kem-v1.properties");
        final MemberKeyPair.Public publicKeys = new MemberKeyPair.Public(KeyAlgorithm.MLKEM1024_P384,
                java.security.KeyFactory.getInstance("ML-KEM").generatePublic(new java.security.spec.X509EncodedKeySpec(HEX.parseHex(vector.getProperty("mlkemPublic")))),
                java.security.KeyFactory.getInstance("EC").generatePublic(new java.security.spec.X509EncodedKeySpec(HEX.parseHex(vector.getProperty("ecPublic")))));
        final MemberKeyPair keys = MemberKeyPair.of(publicKeys, MemberKeyPair.encode(HEX.parseHex(vector.getProperty("mlkemPrivate")), HEX.parseHex(vector.getProperty("ecPrivate"))));
        final byte[] wrapped = HEX.parseHex(vector.getProperty("wrapped"));
        final byte[] aad = HEX.parseHex(vector.getProperty("aad"));

        assertArrayEquals(HEX.parseHex(vector.getProperty("secret")), HybridKem.unwrap(keys, aad, wrapped));

        // The combiner on its own.
        final ByteBuffer buffer = ByteBuffer.wrap(wrapped, 1, wrapped.length - 1);
        final byte[] ct = MemberKeyPair.readPart(buffer);
        final byte[] epk = MemberKeyPair.readPart(buffer);
        final byte[] kek = HybridKem.combine(HEX.parseHex(vector.getProperty("ssKem")), HEX.parseHex(vector.getProperty("ssEcdh")), ct, epk, publicKeys).getEncoded();
        assertArrayEquals(HEX.parseHex(vector.getProperty("kek")), kek);
    }

    private static byte[] flipped(final byte[] bytes) {
        final byte[] copy = Arrays.copyOf(bytes, bytes.length);
        copy[copy.length - 1] ^= 1;
        return copy;
    }

    private static SecretEDSKey secret(final byte[] bytes) {
        return new SecretEDSKey(KeyAlgorithm.AES_GCM_256, new SecretKeySpec(bytes, "AES"));
    }

    private static Properties load(final String resource) throws IOException {
        final Properties properties = new Properties();
        try (InputStream stream = DataFormatTest.class.getClassLoader().getResourceAsStream(resource)) {
            properties.load(stream);
        }

        return properties;
    }
}
