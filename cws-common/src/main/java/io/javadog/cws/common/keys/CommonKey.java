/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-common)
 * =============================================================================
 */
package io.javadog.cws.common.keys;

import io.javadog.cws.common.enums.KeyAlgorithm;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public interface CommonKey<T> {

    KeyAlgorithm getAlgorithm();
    T getKey();
}
