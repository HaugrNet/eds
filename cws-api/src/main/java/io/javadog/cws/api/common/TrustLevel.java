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
package io.javadog.cws.api.common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import java.util.EnumSet;
import java.util.Set;

/**
 * <p>Different Levels of trust, used by the action checks to see if a given
 * Member may perform a specific function.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = Constants.FIELD_TRUSTLEVEL)
public enum TrustLevel {

    ALL,
    READ,
    WRITE,
    ADMIN,
    SYSOP;

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
        return getLevels(expected).contains(level);
    }

    /**
     * Returns the explicit list of TrustLevels, which is permitted or contained
     * by the given TrustLevel.
     *
     * @param level The TrustLevel to fetch the set of permitted TrustLevel's for
     * @return Set with all permitted TrustLevel's
     */
    public static Set<TrustLevel> getLevels(final TrustLevel level) {
        final EnumSet<TrustLevel> allowed;

        switch (level) {
            case SYSOP:
                allowed = EnumSet.of(SYSOP);
                break;
            case ADMIN:
                allowed = EnumSet.of(SYSOP, ADMIN);
                break;
            case WRITE:
                allowed = EnumSet.of(SYSOP, ADMIN, WRITE);
                break;
            case READ:
                allowed = EnumSet.of(SYSOP, ADMIN, WRITE, READ);
                break;
            default:
                allowed = EnumSet.allOf(TrustLevel.class);
                break;
        }

        return allowed;
    }
}
