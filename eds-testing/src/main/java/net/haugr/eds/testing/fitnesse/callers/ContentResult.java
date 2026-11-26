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
package net.haugr.eds.testing.fitnesse.callers;

import net.haugr.eds.testing.client.model.EDSResponse;

/**
 * <p>Result of a content download or HEAD request: the HTTP status, the
 * headers of interest, the content, and the JSON body of an error.</p>
 *
 * @param status             HTTP status
 * @param contentLength      Content-Length, or -1
 * @param contentRange       Content-Range, or null
 * @param contentType        Content-Type, or null
 * @param contentDisposition Content-Disposition, or null
 * @param etag               ETag, or null
 * @param content            The received content; null for HEAD or an error
 * @param error              The JSON body of an error, or null
 * @author Kim Jensen
 * @since EDS 2.0
 */
public record ContentResult(int status, long contentLength, String contentRange, String contentType,
                            String contentDisposition, String etag, byte[] content, EDSResponse error) {

    public boolean isOk() {
        return (status == 200) || (status == 206);
    }
}
