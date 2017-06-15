/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "trustLevel")
public enum TrustLevel {

    GUEST,
    READ,
    WRITE,
    ADMIN,
    SYSOP;

    /**
     * Checks the given Trust Level against an expected, to verifySignature if the given
     * level may perform an action. If the given Trust Level is the same or
     * higher than the expected, then a trust is returned, otherwise a false is
     * returned.
     *
     * @param level    The Trust Level to check against the expected
     * @param expected The expected Trust Level
     * @return True, if the given trust level is allowed, otherwise false
     */
    public static boolean isAllowed(final TrustLevel level, final TrustLevel expected) {
        return level.ordinal() >= expected.ordinal();
    }
}
