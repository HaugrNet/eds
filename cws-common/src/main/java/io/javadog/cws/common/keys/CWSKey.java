/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-common)
 * =============================================================================
 */
package io.javadog.cws.common.keys;

import io.javadog.cws.common.Settings;
import io.javadog.cws.common.enums.KeyAlgorithm;

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
     * simply throw a {@link DestroyFailedException}. Hence this method will
     * simply ignore this exception by logging it as a debug message. Hopefully
     * the debug logs will be reduced in the future, once the implementation has
     * been added.</p>
     */
    protected void destroyKey() {
        try {
            ((Destroyable) key).destroy();
        } catch (DestroyFailedException e) {
            log.log(Settings.INFO, "The Key could not be destroyed, as the implementation was not added: " + e.getMessage(), e);
        }
    }
}
