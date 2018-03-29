/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.jce;

import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.model.Settings;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import java.security.Key;
import java.util.logging.Logger;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public abstract class CWSKey<T extends Key> {

    private static final Logger log = Logger.getLogger(CWSKey.class.getName());

    protected boolean destroyed = false;
    private final KeyAlgorithm algorithm;
    protected final T key;

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

    public byte[] getEncoded() {
        return key.getEncoded();
    }

    public KeyAlgorithm getAlgorithm() {
        return algorithm;
    }

    public boolean isDestroyed() {
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
    protected void destroyKey() {
        // From the OpenJDK bugs:
        //  o https://bugs.openjdk.java.net/browse/JDK-6263419
        //     - No way to clean the memory for a java.security.Key
        //     - Bug is closed, but comments are interesting
        //  o https://bugs.openjdk.java.net/browse/JDK-8008795
        //     - Clean memory in JCE implementations of private key and secret key
        //  o https://bugs.openjdk.java.net/browse/JDK-8158689
        //     - java.security.KeyPair should implement Destroyable
        //  o https://bugs.openjdk.java.net/browse/JDK-8160206
        //     - SecretKeySpec should implement destroy()
        log.log(Settings.DEBUG, "Java support for destroying keys is not yet implemented.");
    }
}
