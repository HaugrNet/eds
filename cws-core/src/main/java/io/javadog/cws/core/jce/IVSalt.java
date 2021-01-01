/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.core.jce;

import io.javadog.cws.core.model.Settings;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * <p>In CWS 1.0, the default Salt was generated as a UUID, however this may
 * not be secure enough, as the entropy is not as high as a proper random byte
 * array. Thus, this Class has been added, to handle the difference between
 * the UUID based Salts and the Random generated.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.1
 */
public final class IVSalt {

    private static final Logger LOG = Logger.getLogger(IVSalt.class.getName());
    // For AES, the block size is always 128 bit and thus the IV must also be of
    // the same size. See: https://en.wikipedia.org/wiki/Initialization_vector
    private static final int IV_SIZE = 16;
    // The armored length of the IV will always have this length.
    private static final int ARMORED_LENGTH = 24;

    private final String armored;

    public IVSalt() {
        // According to the SonarQube rule (from FindBugs/SpotBugs Security)
        // https://sonarcloud.io/coding_rules?open=squid:S3329&rule_key=squid:S3329
        // the IV should be generated using the SecureRandom class as follows.
        final byte[] random = new byte[IV_SIZE];
        new SecureRandom().nextBytes(random);
        this.armored = Base64.getEncoder().encodeToString(random);
    }

    public IVSalt(final String armored) {
        this.armored = armored;
    }

    public String getArmored() {
        return armored;
    }

    public byte[] getBytes() {
        final byte[] bytes = new byte[IV_SIZE];
        // Default assumption - the Salt is a UUID
        byte[] rawSalt = armored.getBytes(Settings.getInstance().getCharset());

        if (armored.length() == ARMORED_LENGTH) {
            try {
                rawSalt = Base64.getDecoder().decode(armored);
            } catch (IllegalArgumentException e) {
                LOG.log(Settings.DEBUG, e, () -> "IVSalt is not Base64 encoded: " + e.getMessage());
            }
        }

        System.arraycopy(rawSalt, 0, bytes, 0, IV_SIZE);
        return bytes;
    }
}
