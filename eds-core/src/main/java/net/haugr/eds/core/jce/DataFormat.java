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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.core.exceptions.CryptoException;

/**
 * <p>The storage format of Data Objects (PLAN.md, Crypto §4), format version 1.
 * Every Data Object, small or large, uses this one chunked format, the STREAM
 * construction with AES-256-GCM.</p>
 *
 * <p><b>Header</b> ({@value #HEADER_BYTES} bytes), at the start of the stored
 * value: {@code version (1) | chunk size (4, big-endian) | HKDF salt (32) |
 * nonce prefix (7)}. It isn't secret, but it is authenticated, as it is part
 * of the AAD of every chunk.</p>
 *
 * <p><b>Object key</b> = HKDF-SHA512(ikm = Circle Secret, salt = header salt,
 * info = {@code "eds/object|" circleId "|" objectId "|" circleSecretVersion}),
 * 32 bytes. Every write uses a new random salt, so an object key is never used
 * for two different contents.</p>
 *
 * <p><b>Chunks</b>: the plaintext is split into chunks of the chunk size; the
 * last chunk may be shorter, and an empty object is one empty chunk. Each
 * chunk is sealed with AES-256-GCM: nonce = {@code prefix (7) | chunk index
 * (4, big-endian) | last-chunk flag (1: 0x01 for the last chunk, else 0x00)},
 * AAD = {@code header | "|" circleId "|" objectId "|" circleSecretVersion}.
 * This detects reordered, dropped, truncated, extended and swapped chunks.</p>
 *
 * <p>The plaintext size follows from the stored size: {@code chunks =
 * ceil((stored - header) / (chunk size + 16))} and {@code plaintext = stored -
 * header - 16 * chunks}.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class DataFormat {

    /** The format version, first byte of the header. */
    public static final byte VERSION = 1;
    /** The chunk size EDS writes, 64 KiB. */
    public static final int DEFAULT_CHUNK_SIZE = 65536;
    /** The smallest chunk size accepted when reading. */
    public static final int MIN_CHUNK_SIZE = 16;
    /** The largest chunk size accepted when reading, 16 MiB. */
    public static final int MAX_CHUNK_SIZE = 1 << 24;
    public static final int SALT_BYTES = 32;
    public static final int PREFIX_BYTES = 7;
    public static final int HEADER_BYTES = 1 + Integer.BYTES + SALT_BYTES + PREFIX_BYTES;
    public static final int TAG_BYTES = Constants.GCM_TAG_BITS / 8;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final byte LAST_CHUNK = 1;

    /**
     * Identifies the Data Object, which the encryption is bound to.
     *
     * @param circleId            External ID of the Circle
     * @param objectId            External ID of the Data Object
     * @param circleSecretVersion Version of the Circle Secret (key id)
     */
    public record Context(String circleId, String objectId, long circleSecretVersion) {

        String binding() {
            return circleId + '|' + objectId + '|' + circleSecretVersion;
        }
    }

    /**
     * The parsed header of a stored Data Object.
     *
     * @param chunkSize Chunk size
     * @param salt      HKDF salt
     * @param prefix    Nonce prefix
     * @param encoded   The header bytes
     */
    public record Header(int chunkSize, byte[] salt, byte[] prefix, byte[] encoded) {

        @Override
        public boolean equals(final Object other) {
            return (other instanceof Header(final int otherChunkSize, final byte[] otherSalt, final byte[] otherPrefix, final byte[] otherEncoded))
                    && (chunkSize == otherChunkSize)
                    && Arrays.equals(salt, otherSalt)
                    && Arrays.equals(prefix, otherPrefix)
                    && Arrays.equals(encoded, otherEncoded);
        }

        @Override
        public int hashCode() {
            return Objects.hash(chunkSize, Arrays.hashCode(salt), Arrays.hashCode(prefix), Arrays.hashCode(encoded));
        }

        @Override
        public String toString() {
            return "Header[chunkSize=" + chunkSize + ", salt=" + HexFormat.of().formatHex(salt) + ", prefix=" + HexFormat.of().formatHex(prefix) + ']';
        }

        /**
         * Reads the header from the start of a stored Data Object.
         *
         * @param stored The stored bytes, at least the header
         * @return Header
         */
        public static Header read(final byte[] stored) {
            if ((stored == null) || (stored.length < HEADER_BYTES)) {
                throw new CryptoException("The stored data is too short for the header.");
            }
            final ByteBuffer buffer = ByteBuffer.wrap(stored, 0, HEADER_BYTES);
            final byte version = buffer.get();
            if (version != VERSION) {
                throw new CryptoException("Unsupported data format version " + version + '.');
            }
            final int chunkSize = buffer.getInt();
            if ((chunkSize < MIN_CHUNK_SIZE) || (chunkSize > MAX_CHUNK_SIZE)) {
                throw new CryptoException("Invalid chunk size " + chunkSize + '.');
            }
            final byte[] salt = new byte[SALT_BYTES];
            buffer.get(salt);
            final byte[] prefix = new byte[PREFIX_BYTES];
            buffer.get(prefix);

            return new Header(chunkSize, salt, prefix, Arrays.copyOf(stored, HEADER_BYTES));
        }

        static Header create(final int chunkSize, final byte[] salt, final byte[] prefix) {
            final ByteBuffer buffer = ByteBuffer.allocate(HEADER_BYTES);
            buffer.put(VERSION).putInt(chunkSize).put(salt).put(prefix);

            return new Header(chunkSize, salt, prefix, buffer.array());
        }
    }

    private DataFormat() {
        // Utility class.
    }

    /**
     * Encrypts a Data Object with a new random salt and nonce prefix, and the
     * default chunk size.
     *
     * @param circleSecret The Circle Secret
     * @param context      The Data Object
     * @param plain        The plaintext
     * @return The stored format
     */
    public static byte[] encrypt(final SecretEDSKey circleSecret, final Context context, final byte[] plain) {
        final byte[] salt = new byte[SALT_BYTES];
        final byte[] prefix = new byte[PREFIX_BYTES];
        RANDOM.nextBytes(salt);
        RANDOM.nextBytes(prefix);

        return encrypt(circleSecret, context, plain, DEFAULT_CHUNK_SIZE, salt, prefix);
    }

    /**
     * Encrypts with the given parameters. Only for test vectors; production
     * code must use {@link #encrypt(SecretEDSKey, Context, byte[])}, so salt
     * and prefix are never reused.
     */
    static byte[] encrypt(final SecretEDSKey circleSecret, final Context context, final byte[] plain, final int chunkSize, final byte[] salt, final byte[] prefix) {
        final Header header = Header.create(chunkSize, salt, prefix);
        final SecretEDSKey objectKey = objectKey(circleSecret, context, salt);
        final byte[] aad = aad(header, context);
        final long chunks = chunkCount(plain.length, chunkSize);
        final ByteArrayOutputStream stream = new ByteArrayOutputStream((int) ciphertextLength(plain.length, chunkSize));
        stream.writeBytes(header.encoded());

        for (long index = 0; index < chunks; index++) {
            final int from = (int) (index * chunkSize);
            final int to = Math.min(plain.length, from + chunkSize);
            final boolean last = index == (chunks - 1);
            stream.writeBytes(Crypto.seal(objectKey, chunkNonce(prefix, index, last), aad, Arrays.copyOfRange(plain, from, to)));
        }

        return stream.toByteArray();
    }

    /**
     * Decrypts a stored Data Object, verifying every chunk and the order,
     * completeness and binding of the chunks.
     *
     * @param circleSecret The Circle Secret
     * @param context      The Data Object
     * @param stored       The stored format
     * @return The plaintext
     */
    public static byte[] decrypt(final SecretEDSKey circleSecret, final Context context, final byte[] stored) {
        final Header header = Header.read(stored);
        final int chunkSize = header.chunkSize();
        final long chunks = storedChunkCount(stored.length, chunkSize);
        final SecretEDSKey objectKey = objectKey(circleSecret, context, header.salt());
        final byte[] aad = aad(header, context);
        final ByteArrayOutputStream stream = new ByteArrayOutputStream((int) plaintextLength(stored.length, chunkSize));
        final int sealedChunk = chunkSize + TAG_BYTES;

        for (long index = 0; index < chunks; index++) {
            final int from = (int) (HEADER_BYTES + index * sealedChunk);
            final int to = (int) Math.min(stored.length, from + (long) sealedChunk);
            final boolean last = index == (chunks - 1);
            stream.writeBytes(Crypto.open(objectKey, chunkNonce(header.prefix(), index, last), aad, Arrays.copyOfRange(stored, from, to)));
        }

        return stream.toByteArray();
    }

    /**
     * The size of the stored format for a plaintext of the given length.
     *
     * @param plaintextLength Plaintext length
     * @param chunkSize       Chunk size
     * @return Stored length, header included
     */
    public static long ciphertextLength(final long plaintextLength, final int chunkSize) {
        return HEADER_BYTES + plaintextLength + TAG_BYTES * chunkCount(plaintextLength, chunkSize);
    }

    /**
     * The plaintext size of a stored Data Object.
     *
     * @param storedLength Stored length, header included
     * @param chunkSize    Chunk size, from the header
     * @return Plaintext length
     */
    public static long plaintextLength(final long storedLength, final int chunkSize) {
        return storedLength - HEADER_BYTES - TAG_BYTES * storedChunkCount(storedLength, chunkSize);
    }

    /**
     * The ETag of a stored object (PLAN.md, Streaming): the first 16 bytes of the
     * SHA-512 of the header's HKDF salt, Base64url, quoted. The salt changes
     * with every write, so the ETag does too, and only the header is needed.
     *
     * @param header Header of the stored object
     * @return Quoted ETag
     */
    public static String etag(final Header header) {
        try {
            final byte[] digest = java.security.MessageDigest.getInstance("SHA-512").digest(header.salt());
            return '"' + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(digest, 16)) + '"';
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    static long chunkCount(final long plaintextLength, final int chunkSize) {
        return Math.max(1, (plaintextLength + chunkSize - 1) / chunkSize);
    }

    private static long storedChunkCount(final long storedLength, final int chunkSize) {
        final long body = storedLength - HEADER_BYTES;
        final long sealedChunk = (long) chunkSize + TAG_BYTES;
        final long chunks = (body + sealedChunk - 1) / sealedChunk;
        final long lastChunk = body - (chunks - 1) * sealedChunk;
        if ((body < TAG_BYTES) || (lastChunk < TAG_BYTES)) {
            throw new CryptoException("The stored data is truncated.");
        }
        if (chunks > 0xFFFFFFFFL) {
            throw new CryptoException("The stored data has too many chunks.");
        }

        return chunks;
    }

    static SecretEDSKey objectKey(final SecretEDSKey circleSecret, final Context context, final byte[] salt) {
        final byte[] secret = circleSecret.getEncoded();
        final SecretEDSKey key = Crypto.deriveKey(secret, salt, ("eds/object|" + context.binding()).getBytes(StandardCharsets.UTF_8));
        Arrays.fill(secret, (byte) 0);

        return key;
    }

    static byte[] aad(final Header header, final Context context) {
        final byte[] binding = ('|' + context.binding()).getBytes(StandardCharsets.UTF_8);
        final byte[] aad = Arrays.copyOf(header.encoded(), HEADER_BYTES + binding.length);
        System.arraycopy(binding, 0, aad, HEADER_BYTES, binding.length);

        return aad;
    }

    static Nonce chunkNonce(final byte[] prefix, final long index, final boolean last) {
        final ByteBuffer buffer = ByteBuffer.allocate(Constants.GCM_NONCE_BYTES);
        buffer.put(prefix).putInt((int) index).put(last ? LAST_CHUNK : 0);

        return Nonce.of(buffer.array());
    }
}
