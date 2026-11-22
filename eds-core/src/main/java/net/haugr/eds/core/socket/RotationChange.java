/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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
package net.haugr.eds.core.socket;

/**
 * <p>The rotation of a Circle's secret started or ended (PLAN.md, Crypto C6),
 * committed: the socket tells the connections which joined the Circle.</p>
 *
 * @param circleId External ID of the Circle
 * @param ended    False when it started, true when it ended
 * @author Kim Jensen
 * @since EDS 2.0
 */
public record RotationChange(String circleId, boolean ended) {
}
