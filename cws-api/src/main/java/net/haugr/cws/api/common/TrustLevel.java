/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.cws.api.common;

import java.util.EnumSet;
import java.util.Set;

/**
 * <p>Different Levels of trust, used by the action checks to see if a given
 * Member may perform a specific function.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public enum TrustLevel {

    ALL,
    READ,
    WRITE,
    ADMIN,
    SYSOP;

    /**
     * Checks the given Trust Level against an expected, to verify if the given
     * level may perform an action. If the given Trust Level is the same or
     * higher than the expected, then a trust is returned, otherwise false is
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
