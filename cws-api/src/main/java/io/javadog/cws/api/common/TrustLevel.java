package io.javadog.cws.api.common;

import javax.xml.bind.annotation.XmlType;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlType(name = "trustLevel")
public enum TrustLevel {

    GUEST,
    READ,
    WRITE,
    ADMIN;

    /**
     * Checks the given Trust Level against an expected, to verify if the given
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
