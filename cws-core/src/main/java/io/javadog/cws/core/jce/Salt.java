/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.jce;

import io.javadog.cws.core.model.Settings;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * <p>In CWS 1.0, the default Salt was generated as a UUID, however this is not
 * secure enough, as the entropy is not as high as a proper random byte array.
 * Thus, this Class has been added, to handle the difference between the UUID
 * based Salts and the Random generated.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.1
 */
public final class Salt {

    // For AES, the blocksize is always 128 bit and thus the IV must also be of
    // the same size. See: https://en.wikipedia.org/wiki/Initialization_vector
    private static final int IV_SIZE = 16;

    // UUIDs have a specific length, which is being used to determine if a
    // given String is armored with Base64, or generated as a UUID.
    private static final int UUID_LENGTH = UUID.randomUUID().toString().length();

    private final String armored;

    public Salt() {
        final byte[] random = new byte[IV_SIZE];
        new SecureRandom().nextBytes(random);
        this.armored = Base64.getEncoder().encodeToString(random);
    }

    public Salt(final String armored) {
        this.armored = armored;
    }

    public String getArmored() {
        return armored;
    }

    public byte[] getBytes() {
        final byte[] bytes;

        if (armored.length() == UUID_LENGTH) {
            // UUID generated Salt
            bytes = armored.getBytes(Settings.getInstance().getCharset());
        } else {
            bytes = Base64.getDecoder().decode(armored);
        }

        return bytes;
    }
}
