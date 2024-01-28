/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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
package net.haugr.eds.core.enums;

import static net.haugr.eds.api.common.Constants.SUN_EC;
import static net.haugr.eds.api.common.Constants.SUN_JCE;

/**
 * <p>The core part for all cryptographic operations is the Algorithms used for
 * the various cryptographic operations. EDS uses a mixture of Synchronous (AES)
 * and Asynchronous (RSA) Algorithms.</p>
 *
 * <p>The Java Cryptographic Extension (JCE) defines a number of Algorithms,
 * which any provider must support. Rather than allowing all of them, the EDS
 * is limiting them too two Algorithms, AES (Symmetric) and RSA (Asymmetric)
 * Cryptography. From the official list (see below), the Algorithms also require
 * a Block Cipher Mode, either ECB (Electronic CodeBook) or Cipher Block
 * Chaining (CBC). Padding is also a requirement, where the choices is between
 * NoPadding (require exact length) or PKCS1Padding/PKCS5Padding (default).</p>
 *
 * <p>The official list of Algorithms that Providers must support as a minimum
 * is <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/javax/crypto/Cipher.html">here</a>:</p>
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
 * <p>To limit the choices, EDS is simply focusing on 2 Algorithms only and only
 * allow the KeySize to be optional. This enum is listing those - however, not
 * all the Algorithms are available unless the JCE Unlimited Strength
 * Jurisdiction Policy Files are added. However, as of Java 8u162 and Java 9,
 * it is <a href="https://bugs.openjdk.java.net/browse/JDK-8170157">enabled by
 * default</a>.</p>
 *
 * <p>EDS not only uses symmetric and asymmetric encryption, also password based
 * encryption, or PBE, is used, to convert member provided passphrases into a
 * SecretKey, which can be used to unlock the Account.</p>
 *
 * <p>The listing below also refer to a derived algorithm. This is used for the
 * Password Based Encryption algorithms, as they need one algorithm to create
 * the Key, but once created, they have to be used with a different algorithm.
 * Otherwise there will be problems with them.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public enum KeyAlgorithm {

    // Signature Algorithms
    SHA_256(Type.SIGNATURE, "RSA", Transformation.SIG256, 256, SUN_JCE, null),
    SHA_512(Type.SIGNATURE, "RSA", Transformation.SIG512, 512, SUN_JCE, null),

    // Symmetric Algorithms
    // The AES CBC variant is not considered safe, and are pending
    // removal. However, removal requires that existing data can be
    // migrated to different Algorithms, again requiring the re-key
    // (#43) feature.
    AES_CBC_128(Type.SYMMETRIC, "AES", Transformation.AES_CBC, 128, SUN_JCE, null),
    AES_CBC_192(Type.SYMMETRIC, "AES", Transformation.AES_CBC, 192, SUN_JCE, null),
    AES_CBC_256(Type.SYMMETRIC, "AES", Transformation.AES_CBC, 256, SUN_JCE, null),
    // Current Production Algorithm
    AES_GCM_128(Type.SYMMETRIC, "AES", Transformation.AES_GCM_128, 128, SUN_JCE, null),
    AES_GCM_192(Type.SYMMETRIC, "AES", Transformation.AES_GCM_192, 192, SUN_JCE, null),
    AES_GCM_256(Type.SYMMETRIC, "AES", Transformation.AES_GCM_256, 256, SUN_JCE, null),

    // Password Based Encryption (PBE) Algorithms
    PBE_CBC_128(Type.PASSWORD, "AES", Transformation.PBE, 128, SUN_JCE, AES_CBC_128),
    PBE_CBC_192(Type.PASSWORD, "AES", Transformation.PBE, 192, SUN_JCE, AES_CBC_192),
    PBE_CBC_256(Type.PASSWORD, "AES", Transformation.PBE, 256, SUN_JCE, AES_CBC_256),
    PBE_GCM_128(Type.PASSWORD, "AES", Transformation.PBE, 128, SUN_JCE, AES_GCM_128),
    PBE_GCM_192(Type.PASSWORD, "AES", Transformation.PBE, 192, SUN_JCE, AES_GCM_192),
    PBE_GCM_256(Type.PASSWORD, "AES", Transformation.PBE, 256, SUN_JCE, AES_GCM_256),

    // Asymmetric Algorithms
    RSA_2048(Type.ASYMMETRIC, "RSA", Transformation.RSA, 2048, SUN_JCE, null),
    RSA_4096(Type.ASYMMETRIC, "RSA", Transformation.RSA, 4096, SUN_JCE, null),
    RSA_8192(Type.ASYMMETRIC, "RSA", Transformation.RSA, 8192, SUN_JCE, null),
    // Elliptic Curves (See: https://openjdk.java.net/jeps/324)
    X25519(Type.ASYMMETRIC, "", Transformation.RSA, 123, SUN_EC, null),
    X448(Type.ASYMMETRIC, "", Transformation.RSA, 123, SUN_EC, null);

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
        // This Transformation is no longer recommended
        AES_CBC("AES/CBC/PKCS5Padding"),
        AES_GCM_128("AES_128/GCM/NoPadding"),
        AES_GCM_192("AES_192/GCM/NoPadding"),
        AES_GCM_256("AES_256/GCM/NoPadding"),
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
    private final String provider;
    private final KeyAlgorithm derived;

    KeyAlgorithm(final Type type, final String algorithm, final Transformation transformation, final int length, final String provider, final KeyAlgorithm derived) {
        this.type = type;
        this.algorithm = algorithm;
        this.transformation = transformation;
        this.length = length;
        this.provider = provider;
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

    public String getProvider() {
        return provider;
    }

    public KeyAlgorithm getDerived() {
        return derived;
    }
}
