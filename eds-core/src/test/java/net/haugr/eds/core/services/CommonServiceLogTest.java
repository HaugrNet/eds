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
package net.haugr.eds.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.core.LogCapture;
import net.haugr.eds.core.ShareBean;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.stream.ContentDownload;
import org.junit.jupiter.api.Test;

/**
 * A request answered with an error Return Code (HTTP 5xx on the endpoints
 * with status codes) is logged as WARN, with the Return Code and message.
 * Before, it was logged as completed, and a 500 left no trace in the log.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class CommonServiceLogTest {

    private final Settings settings = Settings.getInstance();

    @Test
    void testAnErrorAnswerIsLoggedAsWarn() {
        try (LogCapture log = LogCapture.of(CommonService.class);
             Response response = CommonService.runStreamRequest(settings,
                     () -> new EDSResponse(ReturnCode.DATABASE_ERROR, "The database is gone."), "POST /test")) {

            assertEquals(500, response.getStatus());
            assertThat(log.warnings()).singleElement().asString()
                    .startsWith("POST /test completed in ")
                    .endsWith(" ms with DATABASE_ERROR: The database is gone.");
        }
    }

    @Test
    void testAnErrorAnswerOfAnEndpointWithoutStatusIsLoggedAsWarn() {
        try (LogCapture log = LogCapture.of(CommonService.class);
             Response response = CommonService.runRequest(settings,
                     () -> new EDSResponse(ReturnCode.CRYPTO_ERROR, "Tag mismatch"), "POST /test")) {

            assertEquals(200, response.getStatus());
            assertThat(log.warnings()).singleElement().asString().endsWith(" ms with CRYPTO_ERROR: Tag mismatch");
        }
    }

    @Test
    void testAWarningOrSuccessIsNotLoggedAsWarn() {
        try (LogCapture log = LogCapture.of(CommonService.class);
             Response warning = CommonService.runStreamRequest(settings,
                     () -> new EDSResponse(ReturnCode.IDENTIFICATION_WARNING, "No such Data Object."), "GET /test");
             Response success = CommonService.runStreamRequest(settings, () -> new EDSResponse(""), "GET /test")) {
            assertEquals(404, warning.getStatus());
            assertEquals(200, success.getStatus());

            assertThat(log.warnings()).isEmpty();
        }
    }

    @Test
    void testAnUnknownReturnCodeIsAnError() {
        assertTrue(CommonService.isError(null));
        assertFalse(CommonService.isError(ReturnCode.INTEGRITY_WARNING));
        assertTrue(CommonService.isError(ReturnCode.INSUFFICIENT_STORAGE));
        // A busy stone is summed up by its limit, not logged for each request (ticket 20).
        assertFalse(CommonService.isError(ReturnCode.SERVICE_BUSY));
    }

    @Test
    void testADownloadAnsweredWithAnErrorIsLoggedAsWarn() {
        final DataService service = new DataService(failingDownloads(ReturnCode.INTEGRITY_ERROR, "The content is damaged."));
        try (LogCapture log = LogCapture.of(DataService.class);
             Response response = service.downloadContent("an-id", "Bearer AAAA", null, null, null)) {
            assertEquals(500, response.getStatus());
            assertThat(log.warnings()).singleElement().asString()
                    .startsWith("GET ")
                    .endsWith(" of the Data Object an-id answered with INTEGRITY_ERROR: The content is damaged.");
        }
    }

    @Test
    void testADownloadAnsweredWithAWarningIsNotLoggedAsWarn() {
        final DataService service = new DataService(failingDownloads(ReturnCode.IDENTIFICATION_WARNING, "No such Data Object."));
        try (LogCapture log = LogCapture.of(DataService.class);
             Response response = service.contentHeaders("an-id", "Bearer AAAA", null, null, null)) {
            assertEquals(404, response.getStatus());
            assertThat(log.warnings()).isEmpty();
        }
    }

    private static ShareBean failingDownloads(final ReturnCode returnCode, final String message) {
        return new ShareBean() {
            @Override
            public ContentDownload prepareContent(final byte[] sessionToken, final String dataId, final String range, final String ifRange) {
                return ContentDownload.failed(new EDSResponse(returnCode, message));
            }
        };
    }
}
