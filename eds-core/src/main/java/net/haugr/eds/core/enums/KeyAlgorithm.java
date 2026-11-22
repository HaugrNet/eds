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
package net.haugr.eds.core.enums;

import static net.haugr.eds.api.common.Constants.SUN_JCE;

/**
 * <p>The Algorithms EDS uses (PLAN.md, Crypto, "Algorithm baseline"). The set is
 * deliberately small, so it meets BSI TR-02102, ANSSI and NSA CNSA 2.0 at the
 * same time:</p>
 * <ul>
 *   <li><b>AES_GCM_256</b> - symmetric encryption of keys and data.</li>
 *   <li><b>PBE_GCM_256</b> - PBKDF2-HMAC-SHA512 for Member passphrases, the
 *   derived key is used with AES-256-GCM.</li>
 *   <li><b>MLKEM1024_P384</b> - the Member keys, hybrid key encapsulation with
 *   ML-KEM-1024 and ECDH P-384.</li>
 * </ul>
 *
 * <p>The derived algorithm is used for the Password Based Encryption
 * algorithm, as it needs one algorithm to create the Key, and another to use
 * it.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public enum KeyAlgorithm {

    // Symmetric Algorithms; only AES-256-GCM remains (CNSA 2.0 / BSI TR-02102).
    AES_GCM_256(Type.SYMMETRIC, "AES", Transformation.AES_GCM_256, 256, SUN_JCE, null),

    // Password Based Encryption (PBE) Algorithms
    PBE_GCM_256(Type.PASSWORD, "AES", Transformation.PBE, 256, SUN_JCE, AES_GCM_256),

    // Asymmetric Algorithms; the Member keys are a hybrid of ML-KEM-1024 and
    // ECDH P-384, see HybridKem (PLAN.md, Crypto §2 and §3).
    MLKEM1024_P384(Type.ASYMMETRIC, "ML-KEM-1024+P-384", Transformation.HYBRID_KEM, 1024, SUN_JCE, null);

    /**
     * The Algorithm Type, i.e., how it should be used.
     */
    public enum Type {
        SYMMETRIC,
        ASYMMETRIC,
        PASSWORD
    }

    /**
     * Transformation Algorithms, i.e., mapping to the internal JCE algorithms.
     */
    public enum Transformation {
        PBE("PBKDF2WithHmacSHA512"),
        AES_GCM_256("AES_256/GCM/NoPadding"),
        HYBRID_KEM("ML-KEM-1024/ECDH-P384/HKDF-SHA512/AES-256-GCM");

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
