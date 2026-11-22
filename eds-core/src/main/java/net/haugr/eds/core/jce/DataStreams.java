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

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.core.exceptions.CryptoException;

/**
 * <p>Streaming access to the Data Object format (PLAN.md, Crypto §4 and phase C5).
 * The format is the same as {@link DataFormat}; this class only processes it
 * chunk by chunk, so memory use is bounded by the chunk size, whatever the
 * size of the object.</p>
 *
 * <ul>
 *   <li>{@link #encrypt(SecretEDSKey, DataFormat.Context, InputStream, long)}
 *   - an InputStream of the stored format, from a plaintext stream of known
 *   length. The exact stored length is known up front, for a streamed
 *   database write.</li>
 *   <li>{@link #encrypt(SecretEDSKey, DataFormat.Context, InputStream)} -
 *   the same from a plaintext stream of unknown length (a pipe); the stored
 *   length is only known once the stream has been read to its end.</li>
 *   <li>{@link #plan(DataFormat.Header, long, long, long)} - maps a plaintext
 *   byte range to the chunks and the stored bytes that cover it.</li>
 *   <li>{@link #decrypt(SecretEDSKey, DataFormat.Context, DataFormat.Header, long, Range, InputStream)}
 *   - decrypts the planned run of chunks, verifies each chunk, and returns
 *   exactly the requested bytes.</li>
 *   <li>{@link #reencrypt(SecretEDSKey, DataFormat.Context, InputStream, long, SecretEDSKey, DataFormat.Context)}
 *   - re-encrypts a stored object for another Circle or Circle Secret.</li>
 * </ul>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class DataStreams {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** The plaintext length of a stream which is read to its end. */
    static final long UNKNOWN_LENGTH = -1;

    /**
     * The chunks and stored bytes covering a plaintext range.
     *
     * @param firstChunk   Index of the first chunk
     * @param lastChunk    Index of the last chunk
     * @param storedOffset Offset of the first chunk in the stored object
     * @param storedLength Length of the stored bytes of the run of chunks
     * @param skip         Plaintext bytes to skip in the first chunk
     * @param length       Plaintext bytes to return
     */
    public record Range(long firstChunk, long lastChunk, long storedOffset, long storedLength, int skip, long length) {
    }

    private DataStreams() {
        // Utility class.
    }

    // =========================================================================
    // Encryption
    // =========================================================================

    /**
     * Encrypts a plaintext stream of known length into the stored format,
     * with a new random salt and nonce prefix and the default chunk size.
     *
     * @param circleSecret The Circle Secret
     * @param context      The Data Object
     * @param plain        The plaintext, exactly {@code plainLength} bytes
     * @param plainLength  The plaintext length
     * @return Stream of the stored format
     */
    public static EncryptingStream encrypt(final SecretEDSKey circleSecret, final DataFormat.Context context, final InputStream plain, final long plainLength) {
        final byte[] salt = new byte[DataFormat.SALT_BYTES];
        final byte[] prefix = new byte[DataFormat.PREFIX_BYTES];
        RANDOM.nextBytes(salt);
        RANDOM.nextBytes(prefix);

        return new EncryptingStream(circleSecret, context, plain, plainLength, DataFormat.DEFAULT_CHUNK_SIZE, salt, prefix);
    }

    /**
     * Encrypts a plaintext stream of unknown length into the stored format,
     * with a new random salt and nonce prefix and the default chunk size.
     * The result is the same format as with a known length; which chunk is
     * the last is found by reading one byte ahead.
     *
     * @param circleSecret The Circle Secret
     * @param context      The Data Object
     * @param plain        The plaintext, read to its end
     * @return Stream of the stored format
     */
    public static EncryptingStream encrypt(final SecretEDSKey circleSecret, final DataFormat.Context context, final InputStream plain) {
        final byte[] salt = new byte[DataFormat.SALT_BYTES];
        final byte[] prefix = new byte[DataFormat.PREFIX_BYTES];
        RANDOM.nextBytes(salt);
        RANDOM.nextBytes(prefix);

        return new EncryptingStream(circleSecret, context, plain, UNKNOWN_LENGTH, DataFormat.DEFAULT_CHUNK_SIZE, salt, prefix);
    }

    /**
     * Encrypts with the given parameters. Only for test vectors.
     */
    static EncryptingStream encrypt(final SecretEDSKey circleSecret, final DataFormat.Context context, final InputStream plain,
                                    final long plainLength, final int chunkSize, final byte[] salt, final byte[] prefix) {
        return new EncryptingStream(circleSecret, context, plain, plainLength, chunkSize, salt, prefix);
    }

    /**
     * InputStream of the stored format. It reads one chunk of plaintext at a
     * time; the source must deliver exactly the declared number of bytes, or
     * is read to its end if the length is unknown.
     */
    public static final class EncryptingStream extends InputStream {

        private final InputStream source;
        private final long plainLength;
        private final int chunkSize;
        private final byte[] prefix;
        private final SecretEDSKey objectKey;
        private final byte[] aad;
        private final long chunks;
        private final DataFormat.Header header;
        private long storedLength;
        private byte[] buffer;
        private int position = 0;
        private long nextChunk = 0;
        // Unknown length only: the byte read ahead, and whether the last
        // chunk has been sealed.
        private int ahead = -1;
        private boolean finished = false;

        private EncryptingStream(final SecretEDSKey circleSecret, final DataFormat.Context context, final InputStream source,
                                 final long plainLength, final int chunkSize, final byte[] salt, final byte[] prefix) {
            if ((plainLength < 0) && (plainLength != UNKNOWN_LENGTH)) {
                throw new CryptoException("The plaintext length must not be negative.");
            }
            this.header = DataFormat.Header.create(chunkSize, salt, prefix);
            this.source = Objects.requireNonNull(source);
            this.plainLength = plainLength;
            this.chunkSize = chunkSize;
            this.prefix = Utilities.copy(prefix);
            this.objectKey = DataFormat.objectKey(circleSecret, context, salt);
            this.aad = DataFormat.aad(header, context);
            if (plainLength == UNKNOWN_LENGTH) {
                this.chunks = UNKNOWN_LENGTH;
                this.storedLength = DataFormat.HEADER_BYTES;
            } else {
                this.chunks = DataFormat.chunkCount(plainLength, chunkSize);
                this.storedLength = DataFormat.ciphertextLength(plainLength, chunkSize);
            }
            this.buffer = header.encoded();
        }

        /**
         * The header of the stored format, e.g. for the ETag.
         *
         * @return Header
         */
        public DataFormat.Header header() {
            return header;
        }

        /**
         * The exact length of the stored format. With a declared plaintext
         * length it is known before any data is read; with an unknown one
         * it is what has been produced so far, and final once the stream
         * has been read to its end.
         *
         * @return Stored length, header included
         */
        public long storedLength() {
            return storedLength;
        }

        @Override
        public int read() throws IOException {
            if (!fill()) {
                return -1;
            }

            return buffer[position++] & 0xFF;
        }

        @Override
        public int read(final byte[] bytes, final int offset, final int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            if (length == 0) {
                return 0;
            }
            if (!fill()) {
                return -1;
            }
            final int count = Math.min(length, buffer.length - position);
            System.arraycopy(buffer, position, bytes, offset, count);
            position += count;

            return count;
        }

        private boolean fill() throws IOException {
            if (position < buffer.length) {
                return true;
            }
            if (plainLength == UNKNOWN_LENGTH) {
                return fillToTheEnd();
            }
            if (nextChunk >= chunks) {
                return false;
            }

            final long consumed = nextChunk * chunkSize;
            final int size = (int) Math.min(chunkSize, plainLength - consumed);
            final byte[] plain = source.readNBytes(size);
            if (plain.length != size) {
                throw new IOException("The plaintext is shorter than the declared " + plainLength + " bytes.");
            }
            final boolean last = nextChunk == (chunks - 1);
            if (last && (source.read() != -1)) {
                throw new IOException("The plaintext is longer than the declared " + plainLength + " bytes.");
            }

            buffer = Crypto.seal(objectKey, DataFormat.chunkNonce(prefix, nextChunk, last), aad, plain);
            Arrays.fill(plain, (byte) 0);
            position = 0;
            nextChunk++;

            return true;
        }

        /**
         * The next chunk of a source of unknown length. A chunk is the last
         * one if it is short, or if nothing follows it, which reading one
         * byte ahead tells; so an empty source gives one empty chunk, and a
         * source of exactly n chunks gives n, as with a declared length.
         */
        private boolean fillToTheEnd() throws IOException {
            if (finished) {
                return false;
            }

            final byte[] plain = new byte[chunkSize];
            int size = 0;
            // The byte read ahead needs no reset: a full chunk reads the next
            // one below, and after the last chunk the stream is finished.
            if (ahead != -1) {
                plain[size++] = (byte) ahead;
            }
            size += source.readNBytes(plain, size, chunkSize - size);
            boolean last = size < chunkSize;
            if (!last) {
                ahead = source.read();
                last = ahead == -1;
            }

            final byte[] chunk = (size == chunkSize) ? plain : Arrays.copyOf(plain, size);
            buffer = Crypto.seal(objectKey, DataFormat.chunkNonce(prefix, nextChunk, last), aad, chunk);
            Arrays.fill(plain, (byte) 0);
            Arrays.fill(chunk, (byte) 0);
            storedLength += buffer.length;
            position = 0;
            nextChunk++;
            finished = last;

            return true;
        }

        @Override
        public void close() throws IOException {
            source.close();
        }
    }

    // =========================================================================
    // Range reads
    // =========================================================================

    /**
     * Maps the plaintext byte range {@code [from, to]} (inclusive, as in
     * HTTP Range) to the chunks and stored bytes covering it.
     *
     * @param header       The header of the stored object
     * @param storedLength The stored length, header included
     * @param from         First plaintext byte
     * @param to           Last plaintext byte, inclusive
     * @return The Range
     * @throws CryptoException if the range is outside the plaintext
     */
    public static Range plan(final DataFormat.Header header, final long storedLength, final long from, final long to) {
        final int chunkSize = header.chunkSize();
        final long plainLength = DataFormat.plaintextLength(storedLength, chunkSize);
        if ((from < 0) || (to < from) || (to >= plainLength)) {
            throw new CryptoException("The range " + from + '-' + to + " is outside the " + plainLength + " bytes of the object.");
        }

        final long sealedChunk = (long) chunkSize + DataFormat.TAG_BYTES;
        final long firstChunk = from / chunkSize;
        final long lastChunk = to / chunkSize;
        final long storedOffset = DataFormat.HEADER_BYTES + firstChunk * sealedChunk;
        final long storedEnd = Math.min(storedLength, DataFormat.HEADER_BYTES + (lastChunk + 1) * sealedChunk);

        return new Range(firstChunk, lastChunk, storedOffset, storedEnd - storedOffset, (int) (from - firstChunk * chunkSize), to - from + 1);
    }

    /**
     * Decrypts a planned run of chunks. The stream must deliver the stored
     * bytes of the range, starting at {@link Range#storedOffset()}. Every
     * chunk is verified before any of its bytes are returned.
     *
     * @param circleSecret The Circle Secret
     * @param context      The Data Object
     * @param header       The header of the stored object
     * @param storedLength The stored length, header included
     * @param range        The planned Range
     * @param chunks       The stored bytes of the range
     * @return Stream of exactly the requested plaintext bytes
     */
    public static InputStream decrypt(final SecretEDSKey circleSecret, final DataFormat.Context context, final DataFormat.Header header,
                                      final long storedLength, final Range range, final InputStream chunks) {
        return new DecryptingStream(circleSecret, context, header, storedLength, range, chunks);
    }

    /**
     * Decrypts a whole stored object, read from the stream (header
     * included).
     *
     * @param circleSecret The Circle Secret
     * @param context      The Data Object
     * @param stored       The stored object
     * @param storedLength The stored length, header included
     * @return Stream of the plaintext
     * @throws IOException if the header can't be read
     */
    public static InputStream decrypt(final SecretEDSKey circleSecret, final DataFormat.Context context, final InputStream stored, final long storedLength) throws IOException {
        final DataFormat.Header header = readHeader(stored);
        final long plainLength = DataFormat.plaintextLength(storedLength, header.chunkSize());
        final Range range = (plainLength == 0)
                ? new Range(0, 0, DataFormat.HEADER_BYTES, storedLength - DataFormat.HEADER_BYTES, 0, 0)
                : plan(header, storedLength, 0, plainLength - 1);

        return new DecryptingStream(circleSecret, context, header, storedLength, range, stored);
    }

    /**
     * Reads the header from the start of a stored object stream.
     *
     * @param stored Stream of the stored object
     * @return Header
     * @throws IOException if the stream can't be read
     */
    public static DataFormat.Header readHeader(final InputStream stored) throws IOException {
        return DataFormat.Header.read(stored.readNBytes(DataFormat.HEADER_BYTES));
    }

    private static final class DecryptingStream extends InputStream {

        private final InputStream source;
        private final SecretEDSKey objectKey;
        private final byte[] aad;
        private final byte[] prefix;
        private final int chunkSize;
        private final long totalChunks;
        private final Range range;
        private long nextChunk;
        private long remaining;
        private byte[] buffer = new byte[0];
        private int position = 0;

        private DecryptingStream(final SecretEDSKey circleSecret, final DataFormat.Context context, final DataFormat.Header header,
                                 final long storedLength, final Range range, final InputStream source) {
            this.source = Objects.requireNonNull(source);
            this.objectKey = DataFormat.objectKey(circleSecret, context, header.salt());
            this.aad = DataFormat.aad(header, context);
            this.prefix = header.prefix();
            this.chunkSize = header.chunkSize();
            this.totalChunks = DataFormat.chunkCount(DataFormat.plaintextLength(storedLength, chunkSize), chunkSize);
            this.range = range;
            this.nextChunk = range.firstChunk();
            this.remaining = range.length();
            if (range.length() == 0) {
                // An empty object: its single, empty chunk must still verify.
                this.nextChunk = 0;
            }
        }

        @Override
        public int read() throws IOException {
            if (!fill()) {
                return -1;
            }
            remaining--;

            return buffer[position++] & 0xFF;
        }

        @Override
        public int read(final byte[] bytes, final int offset, final int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            if (length == 0) {
                return 0;
            }
            if (!fill()) {
                return -1;
            }
            final int count = (int) Math.min(Math.min(length, buffer.length - position), remaining);
            System.arraycopy(buffer, position, bytes, offset, count);
            position += count;
            remaining -= count;

            return count;
        }

        private boolean fill() throws IOException {
            if ((position < buffer.length) && (remaining > 0)) {
                return true;
            }
            if ((remaining <= 0) && (range.length() > 0)) {
                return false;
            }
            if (nextChunk > range.lastChunk()) {
                return false;
            }

            final boolean last = nextChunk == (totalChunks - 1);
            final int sealedSize = last
                    ? (int) (range.storedOffset() + range.storedLength() - DataFormat.HEADER_BYTES - (nextChunk * ((long) chunkSize + DataFormat.TAG_BYTES)))
                    : (chunkSize + DataFormat.TAG_BYTES);
            final byte[] sealed = source.readNBytes(sealedSize);
            if (sealed.length != sealedSize) {
                throw new IOException("The stored data is truncated.");
            }

            buffer = Crypto.open(objectKey, DataFormat.chunkNonce(prefix, nextChunk, last), aad, sealed);
            position = (nextChunk == range.firstChunk()) ? range.skip() : 0;
            nextChunk++;

            if (last && (source.read() != -1)) {
                // Data after the last chunk means the object was extended.
                throw new IOException("The stored data has bytes after the last chunk.");
            }

            // An empty object has nothing to return, but its chunk was
            // verified.
            return (range.length() > 0) && (position < buffer.length);
        }

        @Override
        public void close() throws IOException {
            source.close();
        }
    }

    // =========================================================================
    // Re-encryption
    // =========================================================================

    /**
     * Re-encrypts a stored object chunk by chunk, e.g. to copy or move it to
     * another Circle, or to rotate the Circle Secret. The plaintext never
     * exists in full.
     *
     * @param sourceSecret  Circle Secret of the source
     * @param sourceContext The source Data Object
     * @param stored        Stream of the stored source object, header included
     * @param storedLength  Stored length of the source
     * @param targetSecret  Circle Secret of the target
     * @param targetContext The target Data Object
     * @return Stream of the target's stored format
     * @throws IOException if the source header can't be read
     */
    public static EncryptingStream reencrypt(final SecretEDSKey sourceSecret, final DataFormat.Context sourceContext, final InputStream stored, final long storedLength,
                                             final SecretEDSKey targetSecret, final DataFormat.Context targetContext) throws IOException {
        final DataFormat.Header header = readHeader(stored);
        final long plainLength = DataFormat.plaintextLength(storedLength, header.chunkSize());
        final Range range = (plainLength == 0)
                ? new Range(0, 0, DataFormat.HEADER_BYTES, storedLength - DataFormat.HEADER_BYTES, 0, 0)
                : plan(header, storedLength, 0, plainLength - 1);
        final InputStream plain = new DecryptingStream(sourceSecret, sourceContext, header, storedLength, range, stored);

        return encrypt(targetSecret, targetContext, plain, plainLength);
    }

    /**
     * Re-encrypts a stored object for the rotation of its Circle Secret
     * (PLAN.md, Crypto C6): as {@link #reencrypt}, but the header keeps its
     * chunk size, salt and nonce prefix. The object key still changes, as
     * it is derived from the new Circle Secret and its version (the
     * context), so no key and nonce are used twice; and as the content
     * doesn't change, neither does its ETag, which comes from the salt: a
     * client's If-Match and If-Range stay valid.
     *
     * @param sourceSecret  The retiring Circle Secret
     * @param sourceContext The Data Object, with the retiring secret's version
     * @param stored        Stream of the stored object, header included
     * @param storedLength  Stored length
     * @param targetSecret  The new Circle Secret
     * @param targetContext The Data Object, with the new secret's version
     * @return Stream of the stored format under the new secret
     * @throws IOException if the header can't be read
     */
    public static EncryptingStream rotate(final SecretEDSKey sourceSecret, final DataFormat.Context sourceContext, final InputStream stored, final long storedLength,
                                          final SecretEDSKey targetSecret, final DataFormat.Context targetContext) throws IOException {
        if (sourceContext.circleSecretVersion() == targetContext.circleSecretVersion()) {
            throw new CryptoException("A rotation needs another Circle Secret.");
        }
        final DataFormat.Header header = readHeader(stored);
        final long plainLength = DataFormat.plaintextLength(storedLength, header.chunkSize());
        final Range range = (plainLength == 0)
                ? new Range(0, 0, DataFormat.HEADER_BYTES, storedLength - DataFormat.HEADER_BYTES, 0, 0)
                : plan(header, storedLength, 0, plainLength - 1);
        final InputStream plain = new DecryptingStream(sourceSecret, sourceContext, header, storedLength, range, stored);

        return new EncryptingStream(targetSecret, targetContext, plain, plainLength, header.chunkSize(), header.salt(), header.prefix());
    }
}
