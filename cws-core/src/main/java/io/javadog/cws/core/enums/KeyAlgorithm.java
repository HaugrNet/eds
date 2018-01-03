/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.enums;

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
 * Jurisdiction Policy Files are added. However, as of Java 8u162 and Java 9,
 * it is <a href="https://bugs.openjdk.java.net/browse/JDK-8170157">enabled by
 * default</a>.</p>
 *
 * <p>CWS not only uses symmetric and asymmetric encryption, also password based
 * encryption, or PBE, is used, to convert member provided passphrase's into a
 * SecretKey, which can be used to unlock the Account.</p>
 *
 * <p>The listing below also refer to a derived algorithm. This is used for the
 * Password Based Encryption algorithms, as they need one algorithm to create
 * the Key, but once created, they have to be used with a different algorithm.
 * Otherwise there will be problems with them.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public enum KeyAlgorithm {

    // Signature Algorithms
    SHA256(Type.SIGNATURE, "RSA", Transformation.SIG256, 256, null),
    SHA512(Type.SIGNATURE, "RSA", Transformation.SIG512, 512, null),

    // Symmetric Algorithms
    AES128(Type.SYMMETRIC, "AES", Transformation.AES, 128, null),
    // Note, prior to Java 8u162 (January 2018) & Java 9, JCE the Unlimited
    // Strength was disabled by default, after this - it is enabled by default:
    //  o https://bugs.openjdk.java.net/browse/JDK-8170157
    AES192(Type.SYMMETRIC, "AES", Transformation.AES, 192, null),
    AES256(Type.SYMMETRIC, "AES", Transformation.AES, 256, null),

    // Password Based Encryption (PBE) Algorithms
    PBE128(Type.PASSWORD, "AES", Transformation.PBE, 128, AES128),
    PBE192(Type.PASSWORD, "AES", Transformation.PBE, 192, AES192),
    PBE256(Type.PASSWORD, "AES", Transformation.PBE, 256, AES256),

    // Asymmetric Algorithms
    RSA2048(Type.ASYMMETRIC, "RSA", Transformation.RSA, 2048, null),
    RSA4096(Type.ASYMMETRIC, "RSA", Transformation.RSA, 4096, null),
    RSA8192(Type.ASYMMETRIC, "RSA", Transformation.RSA, 8192, null);

    public enum Type {
        SYMMETRIC,
        ASYMMETRIC,
        SIGNATURE,
        PASSWORD
    }

    public enum Transformation {
        SIG256("SHA256WithRSA"),
        SIG512("SHA512WithRSA"),
        PBE("PBKDF2WithHmacSHA256"),
        AES("AES/CBC/PKCS5Padding"),
        RSA("RSA/ECB/PKCS1Padding");

        private final String value;

        Transformation(final String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }

    // =========================================================================
    // Internal Functionality
    // =========================================================================

    private final Type type;
    private final String algorithm;
    private final Transformation transformation;
    private final int length;
    private final KeyAlgorithm derived;

    KeyAlgorithm(final Type type, final String algorithm, final Transformation transformation, final int length, final KeyAlgorithm derived) {
        this.type = type;
        this.algorithm = algorithm;
        this.transformation = transformation;
        this.length = length;
        this.derived = derived;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return algorithm;
    }

    public String getTransformation() {
        return transformation.getValue();
    }

    public int getLength() {
        return length;
    }

    public KeyAlgorithm getDerived() {
        return derived;
    }
}
