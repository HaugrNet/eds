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

import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.dtos.Member;
import net.haugr.eds.api.requests.FetchMemberRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>The settings of a Member for clients (language, theme, ...): a JSON
 * object which EDS stores with the account and returns to the Member
 * only.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class MemberSettingsTest extends DatabaseSetup {

    private static final String SETTINGS = "{\"language\":\"de\",\"layout\":{\"pane\":\"chat\"}}";

    @Test
    void testMemberSetsReadsAndRemovesOwnSettings() {
        final ManagementBean bean = prepareManagementBean();
        assertNull(fetch(bean, MEMBER_1, MEMBER_1_ID).getSettings());

        assertTrue(update(bean, MEMBER_1, SETTINGS).isOk());
        assertEquals(SETTINGS, fetch(bean, MEMBER_1, MEMBER_1_ID).getSettings());

        // Without settings in the request, they stay.
        final ProcessMemberRequest unchanged = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        unchanged.setAction(Action.UPDATE);
        unchanged.setAvatar("raven");
        assertTrue(bean.processMember(unchanged).isOk());
        assertEquals(SETTINGS, fetch(bean, MEMBER_1, MEMBER_1_ID).getSettings());

        // Empty settings remove them.
        assertTrue(update(bean, MEMBER_1, "").isOk());
        assertNull(fetch(bean, MEMBER_1, MEMBER_1_ID).getSettings());
    }

    @Test
    void testSettingsAreOnlyForTheMember() {
        final ManagementBean bean = prepareManagementBean();
        assertTrue(update(bean, MEMBER_1, SETTINGS).isOk());

        // Neither another Member nor the System Administrator gets them,
        // by the ID or in the list of all Members.
        assertNull(fetch(bean, MEMBER_2, MEMBER_1_ID).getSettings());
        assertNull(fetch(bean, "admin", MEMBER_1_ID).getSettings());
        final FetchMemberRequest all = prepareRequest(FetchMemberRequest.class, MEMBER_1);
        assertTrue(bean.fetchMembers(all).getMembers().stream().allMatch(m -> m.getSettings() == null));
    }

    @Test
    void testInvalidSettingsAreRefused() {
        final ManagementBean bean = prepareManagementBean();

        final ProcessMemberResponse noObject = update(bean, MEMBER_1, "[1, 2]");
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), noObject.getReturnCode());
        assertEquals("The settings must be a JSON object.", noObject.getReturnMessage());
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), update(bean, MEMBER_1, "{\"language\":").getReturnCode());

        final ProcessMemberResponse tooLong = update(bean, MEMBER_1, "{\"a\":\"" + "x".repeat(4096) + "\"}");
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), tooLong.getReturnCode());
        assertTrue(tooLong.getReturnMessage().contains("settings"));
        assertNull(fetch(bean, MEMBER_1, MEMBER_1_ID).getSettings());
    }

    private ProcessMemberResponse update(final ManagementBean bean, final String account, final String settings) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, account);
        request.setAction(Action.UPDATE);
        request.setSettings(settings);
        return bean.processMember(request);
    }

    private Member fetch(final ManagementBean bean, final String account, final String memberId) {
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, account);
        request.setMemberId(memberId);
        return bean.fetchMembers(request).getMembers().getFirst();
    }
}
