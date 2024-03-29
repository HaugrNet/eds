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

/**
 * <p>Each Member of EDS has some general permissions defined, which is linked
 * to their Role in the system. The default role for members is "STANDARD", and
 * for administrators it is "ADMIN".</p>
 *
 * @author Kim Jensen
 * @since EDS 1.1
 */
public enum MemberRole {

    /** Administrator Role. */
    ADMIN,
    /** Standard Role. */
    STANDARD
}
