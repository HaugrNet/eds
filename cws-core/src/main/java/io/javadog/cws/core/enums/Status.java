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
 * <p>The Status for the CWS Keys.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public enum Status {

    /** Active means that this can be used safely. */
    ACTIVE,

    /** Deprecation is a temporary step before being suspended or deleted. */
    DEPRECATED,
}
