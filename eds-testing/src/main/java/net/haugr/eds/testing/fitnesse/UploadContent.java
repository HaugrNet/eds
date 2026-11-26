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
package net.haugr.eds.testing.fitnesse;

import net.haugr.eds.testing.client.model.ProcessDataResponse;
import net.haugr.eds.testing.fitnesse.callers.CallShare;
import net.haugr.eds.testing.fitnesse.utils.Converter;

/**
 * <p>FitNesse Fixture for the content upload (PLAN.md, S5). The
 * credential is the name a Session was stored under at login.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class UploadContent extends EDSRequest<ProcessDataResponse> {

    private String dataId = null;
    private byte[] content = null;
    private String ifMatch = null;

    public void setDataId(final String dataId) {
        this.dataId = getId(Converter.preCheck(dataId));
        if (this.dataId == null) {
            this.dataId = Converter.preCheck(dataId);
        }
    }

    public void setContent(final String content) {
        this.content = Converter.convertBytes(content);
    }

    public void setGeneratedSize(final String size) {
        final int value = Converter.parseInt(size, -1);
        if (value >= 0) {
            this.content = GeneratedContent.create(value);
        }
    }

    public void setIfMatch(final String ifMatch) {
        this.ifMatch = Converter.preCheck(ifMatch);
    }

    /**
     * Whether the response has an ETag.
     *
     * @return "set", or null
     */
    public String etag() {
        return ((response != null) && (response.getEtag() != null)) ? "set" : null;
    }

    @Override
    public void execute() {
        final byte[] bytes = (content != null) ? content : new byte[0];
        response = CallShare.uploadContent(requestUrl, credential, dataId, bytes, ifMatch);
    }

    @Override
    public void reset() {
        super.reset();
        dataId = null;
        content = null;
        ifMatch = null;
    }
}
