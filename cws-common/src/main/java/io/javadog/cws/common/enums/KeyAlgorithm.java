/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-common)
 * =============================================================================
 */
package io.javadog.cws.common.enums;

/**
 * <p>The core part for all cryptographic operations is the Algorithms used for
 * the various cryptographic operations. CWS uses a mixture of Synchronous (AES)
 * and Asynchronous (RSA) Algorithms.</p>
 *
 * <p>The Java Cryptographic Extension (JCE) defines a number of Algorithms,
 * which any provider must support. Rather than allowing all of them, the CWS
 * is limiting them too two Algorithms, AES (Symmetric) and RSA (Asymmetric)
 * Cryptography. From the official list (see below), the Algorithms also require
 * a Block Cipher Mode, either ECB (Electronic CodeBook) or Cipher Block
 * Chaining (CBC). Padding is also a requirement, where the choices is between
 * NoPadding (require exact length) or PKCS1Padding/PKCS5Padding (default).</p>
 *
 * <p>The official list of Algorithms that Providers must support as a minimum
 * is <a href="http://docs.oracle.com/javase/8/docs/api/javax/crypto/Cipher.html">here</a>:</p>
 * <ol>
 *   <li>AES/CBC/NoPadding (128)</li>
 *   <li>AES/CBC/PKCS5Padding (128)</li>
 *   <li>AES/ECB/NoPadding (128)</li>
 *   <li>AES/ECB/PKCS5Padding (128)</li>
 *   <li>DES/CBC/NoPadding (56)</li>
 *   <li>DES/CBC/PKCS5Padding (56)</li>
 *   <li>DES/ECB/NoPadding (56)</li>
 *   <li>DES/ECB/PKCS5Padding (56)</li>
 *   <li>DESede/CBC/NoPadding (168)</li>
 *   <li>DESede/CBC/PKCS5Padding (168)</li>
 *   <li>DESede/ECB/NoPadding (168)</li>
 *   <li>DESede/ECB/PKCS5Padding (168)</li>
 *   <li>RSA/ECB/PKCS1Padding (1024, 2048)</li>
 *   <li>RSA/ECB/OAEPWithSHA-1AndMGF1Padding (1024, 2048)</li>
 *   <li>RSA/ECB/OAEPWithSHA-256AndMGF1Padding (1024, 2048)</li>
 * </ol>
 *
 * <p>To limit the choices, CWS is simply focusing on 2 Algorithms only and only
 * allow the KeySize to be optional. This enum is listing those - however, not
 * all the Algorithms are available unless the JCE Unlimited Strength
 * Jurisdiction Policy Files are added.</p>
 *
 * <p>VWS not only uses symmetric and asymmetric encryption, also password based
 * encryption or PBE is used, to convert member provided passphrase's into a
 * SecretKey, which can be used to unlock the Account.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public enum KeyAlgorithm {

    // Signature Algorithms
    SHA256(Type.SIGNATURE, "RSA", "SHA256WithRSA", 256),
    SHA512(Type.SIGNATURE, "RSA", "SHA512WithRSA", 512),

    // Password Based Encryption (PBE) Algorithms
    PBE128(Type.PASSWORD, "AES", "PBKDF2WithHmacSHA256", 128),
    PBE192(Type.PASSWORD, "AES", "PBKDF2WithHmacSHA256", 192),
    PBE256(Type.PASSWORD, "AES", "PBKDF2WithHmacSHA256", 256),

    // Symmetric Algorithms
    AES128(Type.SYMMETRIC, "AES", "AES/CBC/PKCS5Padding", 128),
    AES192(Type.SYMMETRIC, "AES", "AES/CBC/PKCS5Padding", 192), // Require JCE Unlimited Strength Files
    AES256(Type.SYMMETRIC, "AES", "AES/CBC/PKCS5Padding", 256), // Require JCE Unlimited Strength Files

    // Asymmetric Algorithms
    RSA1024(Type.ASYMMETRIC, "RSA", "RSA/ECB/PKCS1Padding", 1024),
    RSA2048(Type.ASYMMETRIC, "RSA", "RSA/ECB/PKCS1Padding", 2048),
    RSA4096(Type.ASYMMETRIC, "RSA", "RSA/ECB/PKCS1Padding", 4096),
    RSA8192(Type.ASYMMETRIC, "RSA", "RSA/ECB/PKCS1Padding", 8192);

    public enum Type {
        SYMMETRIC,
        ASYMMETRIC,
        SIGNATURE,
        PASSWORD
    }

    // =========================================================================
    // Internal Functionality
    // =========================================================================

    private final Type type;
    private final String algorithm;
    private final String transformation;
    private final int length;

    KeyAlgorithm(final Type type, final String algorithm, final String transformation, final int length) {
        this.type = type;
        this.algorithm = algorithm;
        this.transformation = transformation;
        this.length = length;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return algorithm;
    }

    public String getTransformation() {
        return transformation;
    }

    public int getLength() {
        return length;
    }
}
