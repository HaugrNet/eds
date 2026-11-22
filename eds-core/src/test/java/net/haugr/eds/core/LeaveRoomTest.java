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

import java.util.List;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.requests.ProcessTrusteeRequest;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.api.responses.ProcessTrusteeResponse;
import net.haugr.eds.core.model.entities.TrusteeEntity;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>Leaving a Circle and removing Members (Rune): every Trustee may leave,
 * only administrators remove others.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class LeaveRoomTest extends DatabaseSetup {

    @Test
    void testMemberLeaves() {
        final ManagementBean bean = prepareManagementBean();
        // member2 may write in circle1, but not administer it, and leaves.
        final ProcessTrusteeResponse left = bean.processTrustee(remove(MEMBER_2, MEMBER_2_ID));
        assertEquals(ReturnCode.SUCCESS.getCode(), left.getReturnCode(), left.getReturnMessage());
        assertTrue(trustees(MEMBER_2_ID).isEmpty());
    }

    @Test
    void testOnlyAdministratorsRemoveOthers() {
        final ManagementBean bean = prepareManagementBean();
        // member3 may only read circle1.
        final ProcessTrusteeResponse refused = bean.processTrustee(remove(MEMBER_3, MEMBER_2_ID));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), refused.getReturnCode());
        assertEquals(1, trustees(MEMBER_2_ID).size());
        // member1 administers circle1.
        assertEquals(ReturnCode.SUCCESS.getCode(), bean.processTrustee(remove(MEMBER_1, MEMBER_3_ID)).getReturnCode());
        assertTrue(trustees(MEMBER_3_ID).isEmpty());
    }

    @Test
    void testLastAdministratorMayLeave() {
        final ManagementBean bean = prepareManagementBean();
        assertEquals(ReturnCode.SUCCESS.getCode(), bean.processTrustee(remove(MEMBER_1, MEMBER_1_ID)).getReturnCode());
        assertTrue(trustees(MEMBER_1_ID).isEmpty());
    }

    /**
     * The last Trustee holds the last copy of the Circle Secret which can be
     * unwrapped, so they stay, also when the System Administrator asks.
     */
    @Test
    void testLastTrusteeStays() {
        final ManagementBean bean = prepareManagementBean();
        assertEquals(ReturnCode.SUCCESS.getCode(), bean.processTrustee(remove(MEMBER_1, MEMBER_2_ID)).getReturnCode());
        assertEquals(ReturnCode.SUCCESS.getCode(), bean.processTrustee(remove(MEMBER_1, MEMBER_3_ID)).getReturnCode());
        assertEquals(1, allTrustees().size(), "The test needs member1 to be the last Trustee of circle1.");

        final ProcessTrusteeResponse refused = bean.processTrustee(remove(MEMBER_1, MEMBER_1_ID));
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), refused.getReturnCode(), refused.getReturnMessage());
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), bean.processTrustee(remove(Constants.ADMIN_ACCOUNT, MEMBER_1_ID)).getReturnCode());
        assertEquals(1, trustees(MEMBER_1_ID).size());
    }

    /**
     * Deleting the account of the last Trustee would lose the Circle Secret
     * just as leaving would, so it is refused too, for the Member and for
     * the System Administrator.
     */
    @Test
    void testAccountOfLastTrusteeStays() {
        final ManagementBean bean = prepareManagementBean();
        assertEquals(ReturnCode.SUCCESS.getCode(), bean.processTrustee(remove(MEMBER_1, MEMBER_2_ID)).getReturnCode());
        assertEquals(ReturnCode.SUCCESS.getCode(), bean.processTrustee(remove(MEMBER_1, MEMBER_3_ID)).getReturnCode());

        final ProcessMemberRequest own = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        own.setAction(Action.DELETE);
        final ProcessMemberResponse refused = bean.processMember(own);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), refused.getReturnCode(), refused.getReturnMessage());

        final ProcessMemberRequest byAdministrator = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        byAdministrator.setAction(Action.DELETE);
        byAdministrator.setMemberId(MEMBER_1_ID);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), bean.processMember(byAdministrator).getReturnCode());
        assertEquals(1, trustees(MEMBER_1_ID).size());
    }

    @Test
    void testNotAMember() {
        final ManagementBean bean = prepareManagementBean();
        // member4 isn't in circle1: the Circle isn't found for them, and
        // nothing changes.
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), bean.processTrustee(remove(MEMBER_4, MEMBER_4_ID)).getReturnCode());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), bean.processTrustee(remove(MEMBER_4, MEMBER_2_ID)).getReturnCode());
        assertEquals(1, trustees(MEMBER_2_ID).size());
    }

    private List<TrusteeEntity> trustees(final String memberId) {
        return entityManager.createQuery("select t from TrusteeEntity t where t.member.externalId = :member and t.circle.externalId = :circle", TrusteeEntity.class)
                .setParameter("member", memberId).setParameter("circle", CIRCLE_1_ID).getResultList();
    }

    private List<TrusteeEntity> allTrustees() {
        return entityManager.createQuery("select t from TrusteeEntity t where t.circle.externalId = :circle", TrusteeEntity.class)
                .setParameter("circle", CIRCLE_1_ID).getResultList();
    }

    private static ProcessTrusteeRequest remove(final String account, final String memberId) {
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, account);
        request.setAction(Action.REMOVE);
        request.setCircleId(CIRCLE_1_ID);
        request.setMemberId(memberId);
        return request;
    }
}
