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
package net.haugr.eds.fitnesse;

import java.nio.charset.StandardCharsets;
import net.haugr.eds.client.model.EDSResponse;
import net.haugr.eds.fitnesse.callers.ContentResult;
import net.haugr.eds.fitnesse.callers.CallShare;
import net.haugr.eds.fitnesse.utils.Converter;

/**
 * <p>FitNesse Fixture for the content download and HEAD (PLAN.md, S5).
 * The credential is the name a Session was stored under at login.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class DownloadContent extends EDSRequest<EDSResponse> {

    private String dataId = null;
    private String token = null;
    private String range = null;
    private String ifRange = null;
    private boolean head = false;
    private ContentResult result = null;
    private byte[] body = null;

    public void setDataId(final String dataId) {
        this.dataId = getId(Converter.preCheck(dataId));
        if (this.dataId == null) {
            this.dataId = Converter.preCheck(dataId);
        }
    }

    /** A download token (or the name it was stored under), instead of the Session. */
    public void setToken(final String token) {
        final String value = Converter.preCheck(token);
        final String stored = getId(value);
        this.token = (stored != null) ? stored : value;
    }

    public void setRange(final String range) {
        this.range = Converter.preCheck(range);
    }

    public void setIfRange(final String ifRange) {
        this.ifRange = Converter.preCheck(ifRange);
    }

    public void setHead(final String head) {
        this.head = Boolean.parseBoolean(Converter.preCheck(head));
    }

    public String status() {
        return (result != null) ? String.valueOf(result.status()) : null;
    }

    /** The content as text, for short textual content. */
    public String content() {
        return (body != null) ? new String(body, StandardCharsets.UTF_8) : null;
    }

    /** Content-Length of the content; null for an error, which has a JSON body. */
    public String contentLength() {
        return ((result != null) && result.isOk()) ? String.valueOf(result.contentLength()) : null;
    }

    public String contentRange() {
        return (result != null) ? result.contentRange() : null;
    }

    /** Content-Type of the content; null for an error. */
    public String contentType() {
        return ((result != null) && result.isOk()) ? result.contentType() : null;
    }

    /** "inline" or "attachment", from Content-Disposition. */
    public String disposition() {
        final String disposition = (result != null) ? result.contentDisposition() : null;
        return (disposition != null) ? disposition.substring(0, disposition.indexOf(';')) : null;
    }

    /** Whether the received bytes are the generated content at the range. */
    public String matchesGenerated() {
        if ((body == null) || (result == null)) {
            return null;
        }
        final String contentRange = result.contentRange();
        final long offset = (contentRange != null) ? Long.parseLong(contentRange.substring("bytes ".length(), contentRange.indexOf('-'))) : 0;
        return String.valueOf(GeneratedContent.matches(body, offset));
    }

    @Override
    public String returnCode() {
        if (result == null) {
            return null;
        }
        final boolean failed = (result.error() != null) && (result.error().getReturnCode() != null) && (result.error().getReturnCode() != 0);
        return failed ? String.valueOf(result.error().getReturnCode()) : String.valueOf(result.status());
    }

    @Override
    public void execute() {
        if (head) {
            result = CallShare.contentHeaders(requestUrl, credential, dataId);
        } else {
            result = (token != null)
                    ? CallShare.downloadContent(requestUrl, credential, token, dataId, range, ifRange)
                    : CallShare.downloadContent(requestUrl, credential, dataId, range, ifRange);
            body = result.content();
        }
    }

    @Override
    public void reset() {
        super.reset();
        dataId = null;
        token = null;
        range = null;
        ifRange = null;
        head = false;
        result = null;
        body = null;
    }
}
