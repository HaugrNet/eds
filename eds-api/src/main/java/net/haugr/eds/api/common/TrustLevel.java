/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.api.common;

import java.util.EnumSet;
import java.util.Set;

/**
 * <p>Different Levels of trust, used by the action checks to see if a given
 * Member may perform a specific function.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public enum TrustLevel {

    /** All Trust Levels. */
    ALL,
    /** Read Only. */
    READ,
    /** Read &amp; Write. */
    WRITE,
    /** Read, Write &amp; Circle Administration. */
    ADMIN,
    /** System Operator, Read, Write &amp; Circle Administration. */
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
        return switch (level) {
            case SYSOP -> EnumSet.of(SYSOP);
            case ADMIN -> EnumSet.of(SYSOP, ADMIN);
            case WRITE -> EnumSet.of(SYSOP, ADMIN, WRITE);
            case READ -> EnumSet.of(SYSOP, ADMIN, WRITE, READ);
            default -> EnumSet.allOf(TrustLevel.class);
        };
    }
}
