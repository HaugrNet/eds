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
package io.javadog.cws.core.jce;

import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.model.Settings;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import java.security.Key;
import java.util.logging.Logger;

/**
 * <p>Common CWS Key, used for all crypto operations.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public abstract class CWSKey<T extends Key> {

    private static final Logger LOG = Logger.getLogger(CWSKey.class.getName());

    protected boolean destroyed = false;
    protected final T key;

    private final KeyAlgorithm algorithm;

    /**
     * Default Constructor.
     *
     * @param algorithm Key Algorithm
     * @param key       Key
     */
    protected CWSKey(final KeyAlgorithm algorithm, final T key) {
        this.algorithm = algorithm;
        this.key = key;
    }

    public abstract T getKey();

    public final byte[] getEncoded() {
        return key.getEncoded();
    }

    public final KeyAlgorithm getAlgorithm() {
        return algorithm;
    }

    public final boolean isDestroyed() {
        return destroyed;
    }

    /**
     * <p>The Secret &amp; Private Keys both extend the {@link Destroyable }
     * interface, which means that they in theory can be destroyed. However,
     * many of the implementations only have the default behaviour, which is to
     * simply throw a {@link DestroyFailedException }. Hence this method will
     * simply ignore this exception by logging it as a debug message. Hopefully
     * the debug logs will be reduced in the future, once the implementation has
     * been added.</p>
     *
     * <p>Unfortunately, it seems that the functionality is not implemented in
     * Java 8, so to avoid large stacktrace in the logs, the code to destroy
     * the keys is revoked and listed as pending..</p>
     */
    protected static void destroyKey() {
        // From the OpenJDK bugs: (pending feature implementation)
        //  o https://bugs.openjdk.java.net/browse/JDK-6263419
        //     - No way to clean the memory for a java.security.Key
        //     - Bug is closed, but comments are interesting
        //  o https://bugs.openjdk.java.net/browse/JDK-8008795
        //     - Clean memory in JCE implementations of private key and secret key
        //  o https://bugs.openjdk.java.net/browse/JDK-8158689
        //     - java.security.KeyPair should implement Destroyable
        //  o https://bugs.openjdk.java.net/browse/JDK-8160206
        //     - SecretKeySpec should implement destroy()
        LOG.log(Settings.DEBUG, "Java support for destroying keys is not yet implemented.");
    }
}
