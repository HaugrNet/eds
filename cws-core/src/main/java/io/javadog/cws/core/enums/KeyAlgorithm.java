/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
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
    SHA_256(Type.SIGNATURE, "RSA", Transformation.SIG256, 256, null),
    SHA_512(Type.SIGNATURE, "RSA", Transformation.SIG512, 512, null),

    // Symmetric Algorithms
    AES_CBC_128(Type.SYMMETRIC, "AES", Transformation.AES, 128, null),
    AES_CBC_192(Type.SYMMETRIC, "AES", Transformation.AES, 192, null),
    AES_CBC_256(Type.SYMMETRIC, "AES", Transformation.AES, 256, null),
    // Following 3 are not yet production ready - the Crypto library
    // doesn't support them yet, work in progress!
    AES_GCM_128(Type.SYMMETRIC, "AES", Transformation.GCM, 128, null),

    // Password Based Encryption (PBE) Algorithms
    PBE_128(Type.PASSWORD, "AES", Transformation.PBE, 128, AES_CBC_128),
    PBE_192(Type.PASSWORD, "AES", Transformation.PBE, 192, AES_CBC_192),
    PBE_256(Type.PASSWORD, "AES", Transformation.PBE, 256, AES_CBC_256),

    // Asymmetric Algorithms
    RSA_2048(Type.ASYMMETRIC, "RSA", Transformation.RSA, 2048, null),
    RSA_4096(Type.ASYMMETRIC, "RSA", Transformation.RSA, 4096, null),
    RSA_8192(Type.ASYMMETRIC, "RSA", Transformation.RSA, 8192, null);

    /**
     * The Algorithm Type, i.e. how it should be used.
     */
    public enum Type {
        SYMMETRIC,
        ASYMMETRIC,
        SIGNATURE,
        PASSWORD
    }

    /**
     * Transformation Algorithms, i.e. mapping to the internal JCE algorithms.
     */
    public enum Transformation {
        SIG256("SHA256WithRSA"),
        SIG512("SHA512WithRSA"),
        PBE("PBKDF2WithHmacSHA256"),
        AES("AES/CBC/PKCS5Padding"),
        GCM("AES/GCM/NoPadding"),
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

    public Transformation getTransformation() {
        return transformation;
    }

    public String getTransformationValue() {
        return transformation.getValue();
    }

    public int getLength() {
        return length;
    }

    public KeyAlgorithm getDerived() {
        return derived;
    }
}
