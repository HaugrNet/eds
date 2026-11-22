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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
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

    // The URL is set by the System Administrator, and what it gives is only
    // used to derive the MasterKey, never returned: no request forgery.
    @SuppressWarnings("findsecbugs:URLCONNECTION_SSRF_FD")
    public static byte[] readMasterKeySecretFromUrl(final String masterKeyUrl) {
        try {
            final URL url = new URI(masterKeyUrl).toURL();
            final URLConnection connection = url.openConnection();
            return readContent(connection);
        } catch (URISyntaxException | IOException | IllegalArgumentException e) {
            throw new EDSException(ReturnCode.NETWORK_ERROR, e.getMessage(), e);
        }
    }

    private static byte[] readContent(final URLConnection connection) throws IOException {
        final int length = connection.getContentLength();

        try (InputStream inputStream = connection.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream(Math.max(length, BUFFER_SIZE))) {
            final byte[] buffer = new byte[BUFFER_SIZE];

            int read = inputStream.read(buffer);
            while (read != -1) {
                outputStream.write(buffer, 0, read);
                read = inputStream.read(buffer);
            }

            return outputStream.toByteArray();
        }
    }
}
