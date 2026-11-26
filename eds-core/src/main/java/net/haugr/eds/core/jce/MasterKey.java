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
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.KDF;
import javax.crypto.SecretKey;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.exceptions.CryptoException;
import net.haugr.eds.core.exceptions.EDSException;

/**
 * <p>The MasterKey is the kill-switch of EDS (PLAN.md, Crypto §1). Its input is
 * read from an external high-entropy source, and the derived keys are only
 * kept in memory. Removing the source makes every stored secret unreadable.</p>
 *
 * <p>The raw input is never used directly. HKDF-SHA512 derives a pseudo
 * random key from it, and separate subkeys per purpose:</p>
 * <ul>
 *   <li><b>{@value #WRAP_LABEL}</b> - AES-256-GCM key for the outer wrap of
 *   stored secrets, always with a random nonce.</li>
 *   <li><b>{@value #HMAC_LABEL}</b> - HMAC-SHA512 key for lookup tokens, such
 *   as sessions and invitations.</li>
 *   <li><b>{@value #KCV_LABEL}</b> - the key check value, stored in the
 *   database together with the version, so a wrong MasterKey is detected
 *   without decrypting anything.</li>
 * </ul>
 *
 * <p>Until a valid MasterKey is installed, EDS is locked: every operation
 * which needs the MasterKey fails.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class MasterKey {

    public static final String WRAP_LABEL = "eds/mk/wrap";
    public static final String HMAC_LABEL = "eds/mk/hmac";
    public static final String KCV_LABEL = "eds/mk/kcv";

    private static final String HKDF_ALGORITHM = "HKDF-SHA512";
    private static final byte[] HKDF_SALT = "eds/masterkey".getBytes(StandardCharsets.UTF_8);
    private static final int WRAP_KEY_BYTES = 32;
    private static final int HMAC_KEY_BYTES = 64;
    private static final int KCV_BYTES = 32;
    private static final int BUFFER_SIZE = 512;

    /** The most a MasterKey source may hold: a secret, not a file (ticket 6). */
    public static final int MAX_SECRET_BYTES = 16 * 1024;
    /** Connect and read timeout of an https: source, in milliseconds. */
    public static final int TIMEOUT_MILLIS = 5_000;
    /** The longest the reading of a source may take altogether, in milliseconds. */
    public static final long DEADLINE_MILLIS = 10_000;
    /** The most sources read at the same time. */
    public static final int MAX_FETCHES = 2;
    private static final Semaphore FETCHES = new Semaphore(MAX_FETCHES);
    private static final String UNREADABLE = "The MasterKey source could not be read";
    /** The answer when {@value #MAX_FETCHES} sources are being read already; it depends on no URL. */
    public static final String SOURCES_BUSY = UNREADABLE + " (other sources are being read).";
    private static final MasterKey INSTANCE = new MasterKey();

    /**
     * The keys derived from a MasterKey secret.
     *
     * @param wrapKey AES-256-GCM key for the outer wrap
     * @param hmacKey HMAC-SHA512 key for lookup tokens
     * @param kcv     Key check value, Base64 encoded
     */
    public record Material(SecretEDSKey wrapKey, byte[] hmacKey, String kcv) {

        // The key check value and the HMAC key, both derived from the same
        // secret as the wrap key, identify the material; the HMAC key is
        // compared in constant time, and no key is ever shown.
        @Override
        public boolean equals(final Object other) {
            return (other instanceof Material material)
                    && Objects.equals(kcv, material.kcv)
                    && MessageDigest.isEqual(hmacKey, material.hmacKey);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(kcv);
        }

        @Override
        public String toString() {
            return "Material[kcv=" + kcv + ']';
        }
    }

    /**
     * The installed MasterKey: its material together with its version. Both
     * are read and replaced as one, so an encryption never pairs the key of
     * one version with the number of another, also not while the MasterKey
     * is rotated.
     *
     * @param material Derived key material
     * @param version  The MasterKey version, as stored in the database
     */
    record Installed(Material material, int version) {
    }

    private final AtomicReference<Installed> installed = new AtomicReference<>();

    private MasterKey() {
        // Singleton, the MasterKey is shared by the whole instance.
    }

    public static MasterKey getInstance() {
        return INSTANCE;
    }

    /**
     * Derives the MasterKey material from the given secret, using
     * HKDF-SHA512. The secret itself is not kept.
     *
     * @param secret Raw MasterKey secret, from the external source
     * @return Derived key material
     */
    public static Material derive(final byte[] secret) {
        if ((secret == null) || (secret.length == 0)) {
            throw new CryptoException("The MasterKey secret is empty.");
        }

        try {
            final KDF hkdf = KDF.getInstance(HKDF_ALGORITHM);
            final SecretKey prk = hkdf.deriveKey("Generic", HKDFParameterSpec.ofExtract()
                    .addIKM(secret)
                    .addSalt(HKDF_SALT)
                    .extractOnly());
            final byte[] wrap = expand(hkdf, prk, WRAP_LABEL, WRAP_KEY_BYTES);
            final byte[] hmac = expand(hkdf, prk, HMAC_LABEL, HMAC_KEY_BYTES);
            final byte[] kcv = expand(hkdf, prk, KCV_LABEL, KCV_BYTES);

            final KeyAlgorithm algorithm = KeyAlgorithm.AES_GCM_256;
            final SecretEDSKey wrapKey = new SecretEDSKey(algorithm, new SecretKeySpec(wrap, algorithm.getName()));
            final String armoredKcv = Base64.getEncoder().encodeToString(kcv);
            Arrays.fill(wrap, (byte) 0);
            Arrays.fill(kcv, (byte) 0);

            return new Material(wrapKey, hmac, armoredKcv);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    private static byte[] expand(final KDF hkdf, final SecretKey prk, final String label, final int length) throws InvalidAlgorithmParameterException {
        return hkdf.deriveData(HKDFParameterSpec.expandOnly(prk, label.getBytes(StandardCharsets.UTF_8), length));
    }

    /**
     * Installs the given material as the current MasterKey, which unlocks
     * EDS. The caller must have checked the material against the key check
     * value stored in the database.
     *
     * @param material Derived MasterKey material
     * @param version  The MasterKey version, as stored in the database
     */
    public void install(final Material material, final int version) {
        installed.set(new Installed(material, version));
    }

    /**
     * Removes the MasterKey from memory, which locks EDS.
     */
    public void lock() {
        installed.set(null);
    }

    public boolean isUnlocked() {
        return installed.get() != null;
    }

    public int getVersion() {
        final Installed current = installed.get();
        return (current == null) ? 0 : current.version();
    }

    public String getKcv() {
        return current().material().kcv();
    }

    byte[] getHmacKey() {
        return current().material().hmacKey();
    }

    /**
     * The installed MasterKey, for an encryption or decryption which needs
     * both its key and its version.
     *
     * @return The installed MasterKey
     * @throws EDSException if EDS is locked
     */
    Installed current() {
        final Installed current = installed.get();
        if (current == null) {
            throw new EDSException(ReturnCode.CRYPTO_ERROR, "EDS is locked, the MasterKey is not loaded.");
        }

        return current;
    }

    /**
     * <p>Reads the secret of the MasterKey from its source (ticket 6). The
     * source may be given before anyone is authenticated (a locked stone
     * can't check a passphrase without the MasterKey), so it is held
     * tight:</p>
     * <ul>
     *   <li>only {@code file:} without a host (a regular file, not a device,
     *   a pipe or a directory) and {@code https:} (no redirects);</li>
     *   <li>at most {@value #MAX_SECRET_BYTES} bytes;</li>
     *   <li>{@value #DEADLINE_MILLIS} ms for it all, the TLS handshake and
     *   the headers included: it is read in a thread of its own, which is
     *   interrupted at the deadline (a virtual thread's socket closes);</li>
     *   <li>at most {@value #MAX_FETCHES} at the same time; another is
     *   refused at once, so slow sources can't hold the stone's database
     *   connections.</li>
     * </ul>
     *
     * <p>A failure gives a fixed message, with the kind of the failure
     * only: the message of the cause may hold the URL with its secret
     * (ticket 5), so neither it nor the cause is passed on.</p>
     *
     * @param masterKeyUrl URL of the source
     * @return The secret
     * @throws EDSException with NETWORK_ERROR, if it can't be read
     */
    public static byte[] readMasterKeySecretFromUrl(final String masterKeyUrl) {
        return awaitFetch(startFetch(sourceOf(masterKeyUrl)));
    }

    /**
     * Starts the reading in a virtual thread of its own, if fewer than
     * {@value #MAX_FETCHES} are read; the thread gives its permit back when
     * it ends, also if it is cancelled before it ran.
     */
    private static Future<byte[]> startFetch(final URI uri) {
        if (!FETCHES.tryAcquire()) {
            throw new EDSException(ReturnCode.NETWORK_ERROR, SOURCES_BUSY);
        }
        final FutureTask<byte[]> task = new FutureTask<>(() ->
                "file".equals(uri.getScheme().toLowerCase(Locale.ROOT)) ? readFile(Path.of(uri)) : readHttps(uri.toURL()));
        try {
            Thread.ofVirtual().name("masterkey-source").start(() -> {
                try {
                    task.run();
                } finally {
                    FETCHES.release();
                }
            });
        } catch (RuntimeException e) {
            FETCHES.release();
            throw e;
        }

        return task;
    }

    // java:S1166 - the cause is left out on purpose: its message may hold
    // the URL, and with it the secret (ticket 5).
    @SuppressWarnings("java:S1166")
    private static byte[] awaitFetch(final Future<byte[]> fetch) {
        try {
            return fetch.get(DEADLINE_MILLIS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException _) {
            fetch.cancel(true);
            throw new EDSException(ReturnCode.NETWORK_ERROR, UNREADABLE + " (it took longer than " + DEADLINE_MILLIS + " ms).");
        } catch (InterruptedException _) {
            fetch.cancel(true);
            Thread.currentThread().interrupt();
            throw new EDSException(ReturnCode.NETWORK_ERROR, UNREADABLE + " (interrupted).");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof EDSException failure) {
                throw failure;
            }
            throw new EDSException(ReturnCode.NETWORK_ERROR, UNREADABLE + " (" + e.getCause().getClass().getSimpleName() + ").");
        }
    }

    // java:S1166 - as above, the cause may hold the URL (ticket 5).
    @SuppressWarnings("java:S1166")
    private static URI sourceOf(final String masterKeyUrl) {
        final URI uri;
        try {
            uri = new URI(masterKeyUrl);
        } catch (URISyntaxException _) {
            throw new EDSException(ReturnCode.NETWORK_ERROR, UNREADABLE + " (URISyntaxException).");
        }
        final String scheme = (uri.getScheme() == null) ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        final boolean file = "file".equals(scheme) && (uri.getRawAuthority() == null);
        if (!file && !"https".equals(scheme)) {
            // A file: URL with a host would be a share on that host (UNC).
            throw new EDSException(ReturnCode.NETWORK_ERROR, "The MasterKey source must be a file: URL without a host, or an https: URL.");
        }

        return uri;
    }

    // The path is the MasterKey source; only a regular file is read, and
    // what it holds only derives the MasterKey, it is never returned.
    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    private static byte[] readFile(final Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new EDSException(ReturnCode.NETWORK_ERROR, UNREADABLE + " (not a regular file).");
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            return readLimited(inputStream);
        }
    }

    // What the URL gives only derives the MasterKey, it is never returned.
    // Its scheme, size, time and number are limited (ticket 6).
    @SuppressWarnings("findsecbugs:URLCONNECTION_SSRF_FD")
    private static byte[] readHttps(final URL url) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            final int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new EDSException(ReturnCode.NETWORK_ERROR, UNREADABLE + " (HTTP " + status + ").");
            }
            if (connection.getContentLengthLong() > MAX_SECRET_BYTES) {
                throw tooLarge();
            }
            try (InputStream inputStream = connection.getInputStream()) {
                return readLimited(inputStream);
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Reads at most {@value #MAX_SECRET_BYTES} bytes.
     *
     * @param inputStream The source
     * @return The bytes
     * @throws IOException if reading fails
     * @throws EDSException if the source holds more
     */
    static byte[] readLimited(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(BUFFER_SIZE);
        final byte[] buffer = new byte[BUFFER_SIZE];

        int read = inputStream.read(buffer);
        while (read != -1) {
            if (outputStream.size() + read > MAX_SECRET_BYTES) {
                throw tooLarge();
            }
            outputStream.write(buffer, 0, read);
            read = inputStream.read(buffer);
        }

        return outputStream.toByteArray();
    }

    /**
     * @return How many more sources may be read now
     */
    static int availableFetches() {
        return FETCHES.availablePermits();
    }

    private static EDSException tooLarge() {
        return new EDSException(ReturnCode.NETWORK_ERROR, UNREADABLE + " (it holds more than " + MAX_SECRET_BYTES + " bytes).");
    }
}
