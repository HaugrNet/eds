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

import javax.json.bind.adapter.JsonbAdapter;
import java.util.Base64;

/**
 * According to the JSON-B documentation, Byte arrays are dealt with by default
 * as BYTES, rather than BASE_64. To change this behaviour, either the
 * configuration must be set, or an adapter is required. For this, the simplest
 * solution is the adapter solution. For more information, see
 * <a href="http://json-b.net/docs/user-guide.html#binary-encoding">this</a>.
 *
 * @author Kim Jensen
 * @since CWS 2.0
 */
public final class ByteArrayAdapter implements JsonbAdapter<byte[], String> {

    /**
     * {@inheritDoc}
     */
    @Override
    public String adaptToJson(final byte[] obj) {
        return obj != null ? Base64.getEncoder().encodeToString(obj) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] adaptFromJson(final String obj) {
        return obj != null ? Base64.getDecoder().decode(obj) : null;
    }
}
