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
package net.haugr.eds.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.dtos.Metadata;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.requests.ProcessDataTypeRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.DownloadTokenResponse;
import net.haugr.eds.api.responses.FetchDataResponse;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.stream.ContentDownload;
import org.junit.jupiter.api.Test;

/**
 * <p>Tests for the media support of the PWA (PLAN.md, L5): download
 * tokens and thumbnails, via the ShareBean. The HTTP layer is tested in
 * eds-quarkus.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class MediaTest extends DatabaseSetup {

    private static final byte[] IMAGE = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3, 4, 5 };

    @Test
    void testDownloadToken() throws IOException {
        final ShareBean bean = prepareShareBean();
        final byte[] session = login(MEMBER_1);
        final byte[] content = generateData(1000);
        final String dataId = upload(bean, session, "token.mp4", "video/mp4", content);

        final DownloadTokenResponse response = bean.downloadToken(copy(session), dataId);
        assertTrue(response.isOk(), response.getReturnMessage());
        assertTrue(response.getToken().matches("[A-Za-z0-9_-]+"), "URL-safe: " + response.getToken());
        final LocalDateTime now = Utilities.newDate();
        assertTrue(response.getExpires().isAfter(now.plusMinutes(59)) && response.getExpires().isBefore(now.plusMinutes(61)));

        final ContentDownload download = bean.prepareContentWithToken(response.getToken(), dataId, "bytes=10-19", null);
        assertTrue(download.isOk(), () -> download.error().getReturnMessage());
        assertEquals(206, download.status());
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        bean.streamContent(download, output);
        assertArrayEquals(java.util.Arrays.copyOfRange(content, 10, 20), output.toByteArray());

        // Two tokens for the same object differ, both work.
        final DownloadTokenResponse second = bean.downloadToken(copy(session), dataId);
        assertNotEquals(response.getToken(), second.getToken());
        assertTrue(bean.prepareContentWithToken(second.getToken(), dataId, null, null).isOk());
    }

    @Test
    void testDownloadTokenIsBoundToItsObject() {
        final ShareBean bean = prepareShareBean();
        final byte[] session = login(MEMBER_1);
        final String first = upload(bean, session, "first", null, generateData(10));
        final String second = upload(bean, session, "second", null, generateData(10));
        final String token = bean.downloadToken(copy(session), first).getToken();

        assertAuthenticationWarning(bean.prepareContentWithToken(token, second, null, null));
        assertAuthenticationWarning(bean.prepareContentWithToken("not-a-token", first, null, null));
        assertAuthenticationWarning(bean.prepareContentWithToken(token.substring(0, token.length() - 2) + "AA", first, null, null));
    }

    @Test
    void testDownloadTokenEndsWithTheSession() {
        final ShareBean bean = prepareShareBean();
        final byte[] session = login(MEMBER_1);
        final String dataId = upload(bean, session, "logout", null, generateData(10));
        final String token = bean.downloadToken(copy(session), dataId).getToken();
        assertTrue(bean.prepareContentWithToken(token, dataId, null, null).isOk());

        final ProcessMemberRequest logout = prepareSessionRequest(ProcessMemberRequest.class, session);
        logout.setAction(Action.LOGOUT);
        assertTrue(prepareManagementBean().processMember(logout).isOk());
        assertFalse(bean.prepareContentWithToken(token, dataId, null, null).isOk());
    }

    @Test
    void testExpiredDownloadToken() {
        final Settings settings = newSettings();
        settings.set(StandardSetting.DOWNLOAD_TOKEN_LIFETIME.getKey(), "0");
        final ShareBean bean = prepareShareBean(settings);
        final byte[] session = login(MEMBER_1);
        final String dataId = upload(bean, session, "expired", null, generateData(10));

        final String token = bean.downloadToken(copy(session), dataId).getToken();
        assertAuthenticationWarning(bean.prepareContentWithToken(token, dataId, null, null));
    }

    @Test
    void testDownloadTokenNeedsAccess() {
        final ShareBean bean = prepareShareBean();
        final byte[] session = login(MEMBER_1);
        final String dataId = upload(bean, session, "access", null, generateData(10));

        // member3 may read circle1, member5 not.
        assertTrue(bean.downloadToken(login(MEMBER_3), dataId).isOk());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), bean.downloadToken(login(MEMBER_5), dataId).getReturnCode());
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), bean.downloadToken(new byte[0], dataId).getReturnCode());
        final String folderId = addFolder(bean, "folder for a token");
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), bean.downloadToken(copy(session), folderId).getReturnCode());
    }

    @Test
    void testThumbnail() throws IOException {
        final ShareBean bean = prepareShareBean();
        final byte[] session = login(MEMBER_1);
        final String dataId = upload(bean, session, "photo.jpg", "image/jpeg", generateData(500));
        assertNull(metadata(bean, session, dataId).getThumbnailId());

        final ProcessDataResponse stored = bean.storeThumbnail(copy(session), dataId, "image/jpeg", IMAGE.clone());
        assertTrue(stored.isOk(), stored.getReturnMessage());
        final String thumbnailId = stored.getDataId();
        assertEquals(thumbnailId, metadata(bean, session, dataId).getThumbnailId());

        // The thumbnail isn't listed, not even with the hidden entries, but
        // the listing carries its ID.
        for (final boolean hidden : new boolean[] { false, true }) {
            final FetchDataRequest list = prepareSessionRequest(FetchDataRequest.class, session);
            list.setCircleId(CIRCLE_1_ID);
            list.setIncludeHidden(hidden);
            final FetchDataResponse listing = bean.fetchData(list);
            assertTrue(listing.getMetadata().stream().noneMatch(m -> thumbnailId.equals(m.getDataId())));
            assertEquals(thumbnailId, listing.getMetadata().stream().filter(m -> dataId.equals(m.getDataId())).findFirst().orElseThrow().getThumbnailId());
        }

        // Its content is served inline, also with a download token.
        final ContentDownload download = bean.prepareContentWithToken(bean.downloadToken(copy(session), thumbnailId).getToken(), thumbnailId, null, null);
        assertTrue(download.isOk());
        assertEquals("image/jpeg", download.contentType());
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        bean.streamContent(download, output);
        assertArrayEquals(IMAGE, output.toByteArray());

        // A new thumbnail replaces the old one.
        final String replaced = bean.storeThumbnail(copy(session), dataId, "image/png", IMAGE.clone()).getDataId();
        assertNotEquals(thumbnailId, replaced);
        assertEquals(replaced, metadata(bean, session, dataId).getThumbnailId());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), bean.prepareContent(copy(session), thumbnailId, null, null).error().getReturnCode());

        // New content removes it, as it no longer matches.
        assertTrue(bean.uploadContent(copy(session), dataId, new ByteArrayInputStream(generateData(10)), 10, null).isOk());
        assertNull(metadata(bean, session, dataId).getThumbnailId());
    }

    @Test
    void testThumbnailGoesWithItsDataObject() {
        final ShareBean bean = prepareShareBean();
        final byte[] session = login(MEMBER_1);
        final String dataId = upload(bean, session, "deleted.jpg", "image/jpeg", generateData(50));
        final String thumbnailId = bean.storeThumbnail(copy(session), dataId, "image/webp", IMAGE.clone()).getDataId();

        final ProcessDataRequest delete = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        delete.setAction(Action.DELETE);
        delete.setDataId(dataId);
        assertTrue(bean.processData(delete).isOk());
        final FetchDataRequest fetch = prepareSessionRequest(FetchDataRequest.class, session);
        fetch.setDataId(thumbnailId);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), bean.fetchData(fetch, true).getReturnCode());
    }

    @Test
    void testThumbnailRules() {
        final ShareBean bean = prepareShareBean();
        final byte[] session = login(MEMBER_1);
        final String dataId = upload(bean, session, "rules.jpg", "image/jpeg", generateData(50));

        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), bean.storeThumbnail(copy(session), dataId, "image/svg+xml", IMAGE.clone()).getReturnCode());
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), bean.storeThumbnail(copy(session), dataId, null, IMAGE.clone()).getReturnCode());
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), bean.storeThumbnail(copy(session), dataId, "image/png", new byte[0]).getReturnCode());
        assertEquals(ReturnCode.CONTENT_TOO_LARGE.getCode(), bean.storeThumbnail(copy(session), dataId, "image/png", new byte[Constants.MAX_THUMBNAIL_SIZE + 1]).getReturnCode());
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), bean.storeThumbnail(copy(session), addFolder(bean, "no thumbnail"), "image/png", IMAGE.clone()).getReturnCode());
        // member3 may only read circle1.
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), bean.storeThumbnail(login(MEMBER_3), dataId, "image/png", IMAGE.clone()).getReturnCode());

        // A thumbnail has no thumbnail, and is only written by the endpoint.
        final String thumbnailId = bean.storeThumbnail(copy(session), dataId, "image/png", IMAGE.clone()).getDataId();
        assertNotNull(thumbnailId);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), bean.storeThumbnail(copy(session), thumbnailId, "image/png", IMAGE.clone()).getReturnCode());
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), bean.uploadContent(copy(session), thumbnailId, new ByteArrayInputStream(IMAGE), IMAGE.length, null).getReturnCode());
        final ProcessDataRequest update = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        update.setAction(Action.UPDATE);
        update.setDataId(thumbnailId);
        update.setDataName("renamed");
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), bean.processData(update).getReturnCode());
        final ProcessDataRequest add = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "fake thumbnail", 10);
        add.setTypeName(Constants.THUMBNAIL_TYPENAME);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), bean.processData(add).getReturnCode());

        // The type stays.
        final ProcessDataTypeRequest deleteType = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        deleteType.setAction(Action.DELETE);
        deleteType.setTypeName(Constants.THUMBNAIL_TYPENAME);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), bean.processDataType(deleteType).getReturnCode());
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private static void assertAuthenticationWarning(final ContentDownload download) {
        assertFalse(download.isOk());
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), download.error().getReturnCode());
    }

    private static byte[] copy(final byte[] session) {
        return session.clone();
    }

    private static Metadata metadata(final ShareBean bean, final byte[] session, final String dataId) {
        final FetchDataRequest request = prepareSessionRequest(FetchDataRequest.class, session);
        request.setDataId(dataId);
        final FetchDataResponse response = bean.fetchData(request, true);
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getMetadata().getFirst();
    }

    private static String addFolder(final ShareBean bean, final String name) {
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, name, 0);
        request.setTypeName(Constants.FOLDER_TYPENAME);
        final ProcessDataResponse response = bean.processData(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getDataId();
    }

    private String upload(final ShareBean bean, final byte[] session, final String name, final String mediaType, final byte[] content) {
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, name, 0);
        request.setMediaType(mediaType);
        final ProcessDataResponse registered = bean.processData(request);
        assertTrue(registered.isOk(), registered.getReturnMessage());
        final ProcessDataResponse uploaded = bean.uploadContent(copy(session), registered.getDataId(), new ByteArrayInputStream(content), content.length, null);
        assertTrue(uploaded.isOk(), uploaded.getReturnMessage());
        return registered.getDataId();
    }

    private byte[] login(final String account) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, account);
        request.setAction(Action.LOGIN);
        return prepareManagementBean().processMember(request).getSession();
    }
}
