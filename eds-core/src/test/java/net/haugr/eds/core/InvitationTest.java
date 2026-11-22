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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.CredentialType;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.core.enums.Status;
import net.haugr.eds.core.model.entities.InvitationEntity;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>Invitations into a Circle by its administrators (Rune): the invited
 * Member joins the Circle when accepting, sealed with the invitation token;
 * an existing account is added at once.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class InvitationTest extends DatabaseSetup {

    private static final String PASSPHRASE = "a fresh passphrase for the guest, 2026 edition!";

    @Test
    void testCircleAdministratorInvitesIntoTheCircle() {
        final ManagementBean bean = prepareManagementBean();
        // member1 administers circle1.
        final ProcessMemberResponse invited = bean.processMember(invite(MEMBER_1, "rune guest", CIRCLE_1_ID, TrustLevel.READ));
        assertEquals(ReturnCode.SUCCESS.getCode(), invited.getReturnCode(), invited.getReturnMessage());
        assertNotNull(invited.getInvitation());
        assertEquals("An invitation to the Circle 'circle1' was successfully issued for 'rune guest'.", invited.getReturnMessage());

        // The sealed Circle Secret isn't readable as such.
        final List<InvitationEntity> grants = entityManager.createQuery("select i from InvitationEntity i where i.member.name = 'rune guest'", InvitationEntity.class).getResultList();
        assertEquals(1, grants.size());
        assertEquals(TrustLevel.READ, grants.getFirst().getTrustLevel());

        final ProcessMemberResponse accepted = bean.processMember(accept("rune guest", invited.getInvitation(), PASSPHRASE));
        assertEquals(ReturnCode.SUCCESS.getCode(), accepted.getReturnCode(), accepted.getReturnMessage());

        // The guest is now a READ Trustee, can open the Circle, and the
        // grant is gone.
        final TrusteeEntity trustee = entityManager.createQuery("select t from TrusteeEntity t where t.member.name = 'rune guest'", TrusteeEntity.class).getSingleResult();
        assertEquals(TrustLevel.READ, trustee.getTrustLevel());
        assertEquals(CIRCLE_1_ID, trustee.getCircle().getExternalId());
        assertTrue(entityManager.createQuery("select i from InvitationEntity i where i.member.name = 'rune guest'", InvitationEntity.class).getResultList().isEmpty());

        final FetchDataRequest fetch = new FetchDataRequest();
        fetch.setAccountName("rune guest");
        fetch.setCredential(PASSPHRASE.getBytes(StandardCharsets.UTF_8));
        fetch.setCircleId(CIRCLE_1_ID);
        assertEquals(ReturnCode.SUCCESS.getCode(), prepareShareBean().fetchData(fetch).getReturnCode());
    }

    @Test
    void testExistingAccountIsAddedAtOnce() {
        final ManagementBean bean = prepareManagementBean();
        // member5 isn't in circle1 yet.
        final ProcessMemberResponse added = bean.processMember(invite(MEMBER_1, MEMBER_5, CIRCLE_1_ID, null));
        assertEquals(ReturnCode.SUCCESS.getCode(), added.getReturnCode(), added.getReturnMessage());
        assertNull(added.getInvitation());
        assertEquals(MEMBER_5_ID, added.getMemberId());
        final TrusteeEntity trustee = entityManager.createQuery("select t from TrusteeEntity t where t.member.name = :name and t.circle.externalId = :circle", TrusteeEntity.class)
                .setParameter("name", MEMBER_5).setParameter("circle", CIRCLE_1_ID).getSingleResult();
        assertEquals(TrustLevel.WRITE, trustee.getTrustLevel());

        final ProcessMemberResponse again = bean.processMember(invite(MEMBER_1, MEMBER_5, CIRCLE_1_ID, null));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), again.getReturnCode());
    }

    @Test
    void testOnlyCircleAdministratorsInvite() {
        final ManagementBean bean = prepareManagementBean();
        // member2 may write in circle1, but not administer it.
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), bean.processMember(invite(MEMBER_2, "not invited", CIRCLE_1_ID, null)).getReturnCode());
        // member4 isn't in circle1 at all.
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), bean.processMember(invite(MEMBER_4, "not invited", CIRCLE_1_ID, null)).getReturnCode());
        // Without a Circle, only the System Administrator invites.
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), bean.processMember(invite(MEMBER_1, "not invited", null, null)).getReturnCode());
        // SYSOP isn't given by invitations.
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), bean.processMember(invite(MEMBER_1, "not invited", CIRCLE_1_ID, TrustLevel.SYSOP)).getReturnCode());
    }

    @Test
    void testPendingInvitationAndWrongToken() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberResponse invited = bean.processMember(invite(MEMBER_1, "pending guest", CIRCLE_1_ID, null));
        assertEquals(ReturnCode.SUCCESS.getCode(), invited.getReturnCode());
        // The name has a pending invitation.
        assertEquals(ReturnCode.CONSTRAINT_ERROR.getCode(), bean.processMember(invite(MEMBER_1, "pending guest", CIRCLE_1_ID, null)).getReturnCode());

        final byte[] wrong = invited.getInvitation().clone();
        wrong[0] ^= 1;
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), bean.processMember(accept("pending guest", wrong, PASSPHRASE)).getReturnCode());
        assertTrue(entityManager.createQuery("select t from TrusteeEntity t where t.member.name = 'pending guest'", TrusteeEntity.class).getResultList().isEmpty());

        // The right token still works.
        assertEquals(ReturnCode.SUCCESS.getCode(), bean.processMember(accept("pending guest", invited.getInvitation(), PASSPHRASE)).getReturnCode());
    }

    /**
     * An expired invitation doesn't hold its name: the next one replaces it,
     * with a new token and its own room access.
     */
    @Test
    void testExpiredInvitationIsReplaced() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberResponse first = bean.processMember(invite(MEMBER_1, "late guest", CIRCLE_1_ID, TrustLevel.ADMIN));
        assertEquals(ReturnCode.SUCCESS.getCode(), first.getReturnCode());
        entityManager.createQuery("update MemberEntity m set m.invitationExpire = :expired where m.name = 'late guest'")
                .setParameter("expired", Utilities.newDate().minusMinutes(1)).executeUpdate();
        entityManager.clear();

        final ProcessMemberResponse second = bean.processMember(invite(MEMBER_1, "late guest", CIRCLE_1_ID, TrustLevel.READ));
        assertEquals(ReturnCode.SUCCESS.getCode(), second.getReturnCode(), second.getReturnMessage());
        assertEquals(first.getMemberId(), second.getMemberId());

        // The old token is void, the new one gives the new access only.
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), bean.processMember(accept("late guest", first.getInvitation(), PASSPHRASE)).getReturnCode());
        assertEquals(ReturnCode.SUCCESS.getCode(), bean.processMember(accept("late guest", second.getInvitation(), PASSPHRASE)).getReturnCode());
        final List<TrusteeEntity> trustees = entityManager.createQuery("select t from TrusteeEntity t where t.member.name = 'late guest'", TrusteeEntity.class).getResultList();
        assertEquals(1, trustees.size());
        assertEquals(TrustLevel.READ, trustees.getFirst().getTrustLevel());
    }

    /**
     * The Circle Secret sealed in an invitation may be rotated before the
     * invitation is accepted (PLAN.md, Crypto C6): the account is set up,
     * but the access went with the secret.
     */
    @Test
    void testAnInvitationWhoseSecretWasRotatedGivesNoAccess() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberResponse invited = bean.processMember(invite(MEMBER_1, "rotated guest", CIRCLE_1_ID, TrustLevel.READ));
        assertEquals(ReturnCode.SUCCESS.getCode(), invited.getReturnCode(), invited.getReturnMessage());
        final InvitationEntity grant = grants("rotated guest").getFirst();
        grant.getKey().setStatus(Status.RETIRING);
        save(grant.getKey());

        final ProcessMemberResponse accepted = bean.processMember(accept("rotated guest", invited.getInvitation(), PASSPHRASE));
        assertEquals(ReturnCode.SUCCESS.getCode(), accepted.getReturnCode(), accepted.getReturnMessage());

        assertTrue(entityManager.createQuery("select t from TrusteeEntity t where t.member.name = 'rotated guest'", TrusteeEntity.class).getResultList().isEmpty());
        assertTrue(grants("rotated guest").isEmpty());
    }

    @Test
    void testAnEmptyVaultIsntKept() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberResponse invited = bean.processMember(invite(MEMBER_1, "vault guest", CIRCLE_1_ID, TrustLevel.READ));
        assertEquals(ReturnCode.SUCCESS.getCode(), invited.getReturnCode(), invited.getReturnMessage());
        final ProcessMemberRequest request = accept("vault guest", invited.getInvitation(), PASSPHRASE);
        request.setVault("");

        final ProcessMemberResponse accepted = bean.processMember(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), accepted.getReturnCode(), accepted.getReturnMessage());
        assertNull(entityManager.createQuery("select m from MemberEntity m where m.name = 'vault guest'", MemberEntity.class).getSingleResult().getVault());
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private static ProcessMemberRequest invite(final String inviter, final String name, final String circleId, final TrustLevel trustLevel) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, inviter);
        request.setAction(Action.INVITE);
        request.setNewAccountName(name);
        request.setCircleId(circleId);
        request.setTrustLevel(trustLevel);
        return request;
    }

    private static ProcessMemberRequest accept(final String name, final byte[] token, final String passphrase) {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(name);
        request.setAction(Action.UPDATE);
        request.setCredentialType(CredentialType.INVITATION);
        request.setCredential(token.clone());
        request.setNewCredential(passphrase.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private List<InvitationEntity> grants(final String name) {
        return entityManager.createQuery("select i from InvitationEntity i where i.member.name = :name", InvitationEntity.class)
                .setParameter("name", name).getResultList();
    }
}
