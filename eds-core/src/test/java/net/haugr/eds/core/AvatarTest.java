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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.dtos.Circle;
import net.haugr.eds.api.dtos.Trustee;
import net.haugr.eds.api.requests.FetchCircleRequest;
import net.haugr.eds.api.requests.FetchMemberRequest;
import net.haugr.eds.api.requests.FetchTrusteeRequest;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.FetchCircleResponse;
import net.haugr.eds.api.responses.FetchMemberResponse;
import net.haugr.eds.api.responses.FetchTrusteeResponse;
import net.haugr.eds.api.responses.ProcessCircleResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * Avatars of Members and Circles: the ID of a picture the clients know,
 * which EDS only stores and returns.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class AvatarTest extends DatabaseSetup {

    @Test
    void testMemberSetsAndRemovesOwnAvatar() {
        final ManagementBean bean = prepareManagementBean();

        assertTrue(updateOwnAvatar(bean, "raven").isOk());
        assertEquals("raven", avatarOfMember1(bean));
        // Others see it too, e.g. among the Trustees of a shared Circle.
        assertEquals("raven", trusteeOfMember1InCircle1(bean).getAvatar());

        // Without an avatar in the request, it stays.
        final ProcessMemberRequest unchanged = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        unchanged.setAction(Action.UPDATE);
        unchanged.setPublicKey(UUID.randomUUID().toString());
        assertTrue(bean.processMember(unchanged).isOk());
        assertEquals("raven", avatarOfMember1(bean));

        // An empty avatar removes it.
        assertTrue(updateOwnAvatar(bean, "").isOk());
        assertNull(avatarOfMember1(bean));
    }

    @Test
    void testInvalidAvatarIsRefused() {
        final ManagementBean bean = prepareManagementBean();

        final ProcessMemberResponse response = updateOwnAvatar(bean, "Raven!");
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertTrue(response.getReturnMessage().contains("avatar"));

        final ProcessMemberResponse tooLong = updateOwnAvatar(bean, "a".repeat(33));
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), tooLong.getReturnCode());
    }

    @Test
    void testCircleAvatarOnCreateAndUpdate() {
        final ManagementBean bean = prepareManagementBean();
        final String externalKey = UUID.randomUUID().toString();

        final ProcessCircleRequest create = prepareRequest(ProcessCircleRequest.class, MEMBER_5);
        create.setAction(Action.CREATE);
        create.setCircleName("Longship");
        create.setCircleKey(externalKey);
        create.setAvatar("longship");
        final ProcessCircleResponse created = bean.processCircle(create);
        assertTrue(created.isOk());
        assertEquals("longship", circleOfMember5(bean, created.getCircleId()).getAvatar());

        // Only the avatar changes; the name and the external key stay.
        final ProcessCircleRequest update = prepareRequest(ProcessCircleRequest.class, MEMBER_5);
        update.setAction(Action.UPDATE);
        update.setCircleId(created.getCircleId());
        update.setAvatar("hearth");
        assertTrue(bean.processCircle(update).isOk());
        final Circle updated = circleOfMember5(bean, created.getCircleId());
        assertEquals("hearth", updated.getAvatar());
        assertEquals("Longship", updated.getCircleName());
        assertEquals(externalKey, updated.getCircleKey());

        // The Trustee records carry it, for the own rooms of a Member.
        final FetchTrusteeRequest trustees = prepareRequest(FetchTrusteeRequest.class, MEMBER_5);
        final FetchTrusteeResponse mine = bean.fetchTrustees(trustees);
        assertTrue(mine.getTrustees().stream().anyMatch(t -> created.getCircleId().equals(t.getCircleId()) && "hearth".equals(t.getCircleAvatar())));

        final ProcessCircleRequest remove = prepareRequest(ProcessCircleRequest.class, MEMBER_5);
        remove.setAction(Action.UPDATE);
        remove.setCircleId(created.getCircleId());
        remove.setAvatar("");
        assertTrue(bean.processCircle(remove).isOk());
        assertNull(circleOfMember5(bean, created.getCircleId()).getAvatar());
    }

    @Test
    void testInvalidCircleAvatarIsRefused() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest create = prepareRequest(ProcessCircleRequest.class, MEMBER_5);
        create.setAction(Action.CREATE);
        create.setCircleName("Bad Avatar");
        create.setAvatar("<svg>");
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), bean.processCircle(create).getReturnCode());
    }

    private ProcessMemberResponse updateOwnAvatar(final ManagementBean bean, final String avatar) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setAvatar(avatar);
        return bean.processMember(request);
    }

    private String avatarOfMember1(final ManagementBean bean) {
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, MEMBER_2);
        request.setMemberId(MEMBER_1_ID);
        final FetchMemberResponse response = bean.fetchMembers(request);
        assertTrue(response.isOk());
        return response.getMembers().getFirst().getAvatar();
    }

    private Trustee trusteeOfMember1InCircle1(final ManagementBean bean) {
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, MEMBER_2);
        request.setCircleId(CIRCLE_1_ID);
        final FetchTrusteeResponse response = bean.fetchTrustees(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getTrustees().stream().filter(t -> MEMBER_1_ID.equals(t.getMemberId())).findFirst().orElseThrow();
    }

    private Circle circleOfMember5(final ManagementBean bean, final String circleId) {
        final FetchCircleResponse response = bean.fetchCircles(prepareRequest(FetchCircleRequest.class, MEMBER_5));
        assertTrue(response.isOk());
        return response.getCircles().stream().filter(c -> circleId.equals(c.getCircleId())).findFirst().orElseThrow();
    }
}
