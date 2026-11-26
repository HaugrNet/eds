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
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.requests.FetchMemberRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.SessionEntity;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A Member may have several Sessions (PLAN.md, "Several sessions"): a login
 * adds one, a logout ends the one it comes with.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class SessionsTest extends DatabaseSetup {

    @Test
    void testTwoSessionsOfOneAccountSideBySide() {
        final ManagementBean bean = prepareManagementBean();
        // Other tests may have left Sessions of the Member behind.
        final int before = sessions(MEMBER_1);
        final byte[] first = login(bean, MEMBER_1);
        final byte[] second = login(bean, MEMBER_1);

        assertTrue(works(bean, first));
        assertTrue(works(bean, second));
        assertEquals(before + 2, sessions(MEMBER_1));
    }

    @Test
    void testLogoutEndsOnlyItsOwnSession() {
        final ManagementBean bean = prepareManagementBean();
        final byte[] first = login(bean, MEMBER_1);
        final byte[] second = login(bean, MEMBER_1);

        assertTrue(bean.processMember(logoutRequest(first)).isOk());

        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), fetch(bean, first));
        assertTrue(works(bean, second));
    }

    @Test
    void testALogoutWithoutASessionEndsNone() {
        final ManagementBean bean = prepareManagementBean();
        final byte[] session = login(bean, MEMBER_1);
        final int before = sessions(MEMBER_1);
        final ProcessMemberRequest logout = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        logout.setAction(Action.LOGOUT);

        assertTrue(bean.processMember(logout).isOk());

        assertEquals(before, sessions(MEMBER_1));
        assertTrue(works(bean, session));
    }

    @Test
    void testSessionsOfOtherMembersAreLeftAlone() {
        final ManagementBean bean = prepareManagementBean();
        final byte[] mine = login(bean, MEMBER_1);
        final byte[] theirs = login(bean, MEMBER_2);

        assertTrue(bean.processMember(logoutRequest(mine)).isOk());

        assertTrue(works(bean, theirs));
    }

    @Test
    void testNewPassphraseEndsAllSessions() {
        final ManagementBean bean = prepareManagementBean();
        final byte[] first = login(bean, MEMBER_1);
        final byte[] second = login(bean, MEMBER_1);
        final byte[] other = login(bean, MEMBER_2);

        final ProcessMemberRequest update = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        update.setAction(Action.UPDATE);
        update.setNewCredential(crypto.stringToBytes(NEW_PASSPHRASE));
        assertTrue(bean.processMember(update).isOk());

        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), fetch(bean, first));
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), fetch(bean, second));
        assertTrue(works(bean, other));
    }

    @Test
    void testInvalidatingEndsAllSessions() {
        final ManagementBean bean = prepareManagementBean();
        final byte[] first = login(bean, MEMBER_1);
        final byte[] second = login(bean, MEMBER_1);

        // Invalidating needs the passphrase (ticket 7).
        final ProcessMemberRequest invalidate = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        invalidate.setAction(Action.INVALIDATE);
        assertTrue(bean.processMember(invalidate).isOk());

        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), fetch(bean, first));
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), fetch(bean, second));
    }

    @Test
    void testLimitEndsTheSessionWhichExpiresFirst() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SESSION_MAX_PER_MEMBER.getKey(), "2");
        final ManagementBean bean = prepareManagementBean(mySettings);

        final byte[] first = login(bean, MEMBER_1);
        final byte[] second = login(bean, MEMBER_1);
        final byte[] third = login(bean, MEMBER_1);

        assertEquals(2, sessions(MEMBER_1));
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), fetch(bean, first));
        assertTrue(works(bean, second));
        assertTrue(works(bean, third));
    }

    @Test
    void testExpiredSessionsAreRemovedByTheSanityJob() {
        final Settings expiring = newSettings();
        expiring.set(StandardSetting.SESSION_TIMEOUT.getKey(), "-1");
        final int before = sessions(MEMBER_1);
        login(prepareManagementBean(expiring), MEMBER_1);
        final ManagementBean bean = prepareManagementBean();
        final byte[] lasting = login(bean, MEMBER_1);
        assertEquals(before + 2, sessions(MEMBER_1));

        prepareSanitizerBean().sanitize();
        entityManager.clear();

        final LocalDateTime now = Utilities.newDate();
        final List<SessionEntity> left = dao.findSessions(dao.findMemberByName(MEMBER_1));
        assertTrue(left.size() <= (before + 1));
        assertTrue(left.stream().allMatch(session -> session.getExpire().isAfter(now)));
        assertTrue(works(bean, lasting));
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private int sessions(final String account) {
        return dao.findSessions(dao.findMemberByName(account)).size();
    }

    private static byte[] login(final ManagementBean bean, final String account) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, account);
        request.setAction(Action.LOGIN);
        final ProcessMemberResponse response = bean.processMember(request);
        assertTrue(response.isOk(), response.getReturnMessage());

        return response.getSession();
    }

    private static ProcessMemberRequest logoutRequest(final byte[] session) {
        final ProcessMemberRequest request = prepareSessionRequest(ProcessMemberRequest.class, session);
        request.setAction(Action.LOGOUT);

        return request;
    }

    private static int fetch(final ManagementBean bean, final byte[] session) {
        return bean.fetchMembers(prepareSessionRequest(FetchMemberRequest.class, session.clone())).getReturnCode();
    }

    private static boolean works(final ManagementBean bean, final byte[] session) {
        return fetch(bean, session) == ReturnCode.SUCCESS.getCode();
    }
}
