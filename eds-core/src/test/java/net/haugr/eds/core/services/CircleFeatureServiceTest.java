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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.FeatureState;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.dtos.Entry;
import net.haugr.eds.api.requests.CircleFeatureRequest;
import net.haugr.eds.api.requests.EntryRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.CircleFeatureResponse;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.api.responses.EntryResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.core.FeatureBean;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The features of a Circle and their entries, through the resource API
 * (FEATURES.md): the Session token comes from the Authorization header, and
 * the HTTP status is that of the Return Code.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class CircleFeatureServiceTest extends DatabaseSetup {

    private static final String BOARD = "board";
    private static final String DECLARATION = """
            {"feature": "board", "version": 1, "kinds": {"note": {"add": "WRITE", "change": ["author"], "remove": ["author"], "close": ["administrators"], "size": 100}}}""";

    private FeatureBean features = null;
    private CircleFeatureService service = null;

    @BeforeEach
    void offer() {
        features = new FeatureBean(entityManager, settings);
        service = new CircleFeatureService(features);
        assertTrue(features.putFeature(session(Constants.ADMIN_ACCOUNT), BOARD, DECLARATION).isOk());
    }

    @Test
    void testTheFeaturesOfACircle() {
        final String admin = bearer(MEMBER_1);
        final CircleFeatureRequest on = new CircleFeatureRequest();
        on.setState(FeatureState.ON);

        assertOk(service.putCircleFeature(admin, CIRCLE_1_ID, BOARD, on));
        try (Response response = service.getCircleFeatures(bearer(MEMBER_3), CIRCLE_1_ID)) {
            final CircleFeatureResponse found = (CircleFeatureResponse) entity(response, ReturnCode.SUCCESS);
            assertEquals(FeatureState.ON, found.getFeatures().getFirst().getState());
        }
        // Without a state, nothing is switched.
        try (Response response = service.putCircleFeature(admin, CIRCLE_1_ID, BOARD, null)) {
            entity(response, ReturnCode.VERIFICATION_WARNING);
        }
        assertOk(service.deleteCircleFeature(admin, CIRCLE_1_ID, BOARD));
    }

    @Test
    void testTheEntriesOfAFeature() {
        assertTrue(features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD, FeatureState.ON).isOk());
        final String writer = bearer(MEMBER_2);
        final String reader = bearer(MEMBER_3);

        final String entryId;
        try (Response response = service.postEntry(writer, CIRCLE_1_ID, BOARD, body("note", "first"))) {
            entryId = ((EntryResponse) entity(response, ReturnCode.SUCCESS)).getEntries().getFirst().getEntryId();
        }
        assertOk(service.putEntry(writer, CIRCLE_1_ID, BOARD, entryId, body(null, "changed")));
        try (Response response = service.getEntry(reader, CIRCLE_1_ID, BOARD, entryId)) {
            final Entry entry = ((EntryResponse) entity(response, ReturnCode.SUCCESS)).getEntries().getFirst();
            assertEquals("changed", new String(Base64.getDecoder().decode(entry.getContent()), StandardCharsets.UTF_8));
        }
        try (Response response = service.getEntries(reader, CIRCLE_1_ID, BOARD, "note", null, null, null, null, null, null, false)) {
            assertEquals(entryId, ((EntryResponse) entity(response, ReturnCode.SUCCESS)).getEntries().getFirst().getEntryId());
        }
        try (Response response = service.getEntryCount(reader, CIRCLE_1_ID, BOARD, null, null)) {
            assertEquals(1L, ((EntryResponse) entity(response, ReturnCode.SUCCESS)).getCount());
        }

        // Closing needs to say whether it closes or opens.
        try (Response response = service.putEntryClosed(bearer(MEMBER_1), CIRCLE_1_ID, BOARD, entryId, null)) {
            entity(response, ReturnCode.VERIFICATION_WARNING);
        }
        final EntryRequest closed = new EntryRequest();
        closed.setClosed(Boolean.TRUE);
        assertOk(service.putEntryClosed(bearer(MEMBER_1), CIRCLE_1_ID, BOARD, entryId, closed));
        closed.setClosed(Boolean.FALSE);
        assertOk(service.putEntryClosed(bearer(MEMBER_1), CIRCLE_1_ID, BOARD, entryId, closed));

        assertOk(service.deleteEntry(writer, CIRCLE_1_ID, BOARD, entryId));
        try (Response response = service.getEntry(reader, CIRCLE_1_ID, BOARD, entryId)) {
            entity(response, ReturnCode.IDENTIFICATION_WARNING);
        }
    }

    @Test
    void testWithoutATokenNothingIsAsked() {
        try (Response response = service.getCircleFeatures(null, CIRCLE_1_ID)) {
            entity(response, ReturnCode.AUTHENTICATION_WARNING);
        }
    }

    @Test
    void testOutsideCdiTheServiceFails() {
        final CircleFeatureService unwired = new CircleFeatureService();

        try (Response response = unwired.getCircleFeatures(bearer(MEMBER_3), CIRCLE_1_ID)) {
            entity(response, ReturnCode.ERROR);
        }
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private static void assertOk(final Response response) {
        try (response) {
            entity(response, ReturnCode.SUCCESS);
        }
    }

    /** The body of a response, whose status must be that of the Return Code. */
    private static EDSResponse entity(final Response response, final ReturnCode returnCode) {
        final EDSResponse entity = (EDSResponse) response.getEntity();
        assertEquals(returnCode.getCode(), entity.getReturnCode(), entity.getReturnMessage());
        assertEquals(returnCode.getHttpCode(), response.getStatus());
        return entity;
    }

    private static EntryRequest body(final String kind, final String text) {
        final EntryRequest body = new EntryRequest();
        body.setKind(kind);
        body.setContent(Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)));
        return body;
    }

    private String bearer(final String account) {
        return "Bearer " + Base64.getEncoder().encodeToString(session(account));
    }

    private byte[] session(final String account) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, account);
        request.setAction(Action.LOGIN);
        final ProcessMemberResponse response = prepareManagementBean().processMember(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getSession();
    }
}
