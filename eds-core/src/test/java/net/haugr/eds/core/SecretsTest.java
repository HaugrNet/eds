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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.CredentialType;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.dtos.Trustee;
import net.haugr.eds.api.dtos.TrusteeSecret;
import net.haugr.eds.api.requests.FetchCircleRequest;
import net.haugr.eds.api.requests.FetchMemberRequest;
import net.haugr.eds.api.requests.FetchTrusteeRequest;
import net.haugr.eds.api.requests.MasterKeyRequest;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.requests.ProcessTrusteeRequest;
import net.haugr.eds.api.responses.FetchMemberResponse;
import net.haugr.eds.api.responses.FetchTrusteeResponse;
import net.haugr.eds.api.responses.ProcessCircleResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.api.responses.ProcessTrusteeResponse;
import net.haugr.eds.core.model.entities.InvitationEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * <p>The values a stone keeps for clients which encrypt themselves
 * (E2E.md): the secret of a Member in a Circle, and the Member's vault. EDS
 * doesn't look into either; these tests are about who writes and reads
 * them, and when they go.</p>
 *
 * <p>In the test data, member1 administers circle1, member2 may write in
 * it, member3 may read, and member4 and member5 aren't in it.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class SecretsTest extends DatabaseSetup {

    private static final String PASSPHRASE = "a fresh passphrase for the guest, 2026 edition!";
    private static final String SECRET_1 = "wrapped-for-member1:AAAA";
    private static final String SECRET_2 = "wrapped-for-member2:BBBB";
    private static final String SECRET_3 = "wrapped-for-member3:CCCC";

    // =========================================================================
    // The secret of a Member in a Circle
    // =========================================================================

    @Test
    void testCreatorGetsTheirSecretWithTheCircle() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest create = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        create.setAction(Action.CREATE);
        create.setCircleName("a chamber");
        create.setCircleKey("says: this Circle is a chamber");
        create.setSecret(SECRET_1);
        final ProcessCircleResponse created = bean.processCircle(create);
        assertTrue(created.isOk(), created.getReturnMessage());

        assertEquals(SECRET_1, secretOf(bean, MEMBER_1, created.getCircleId(), MEMBER_1_ID));
    }

    @Test
    void testEveryTrusteeSetsTheirOwnSecretAlsoWithRead() {
        final ManagementBean bean = prepareManagementBean();

        assertTrue(bean.processTrustee(secretRequest(MEMBER_3, CIRCLE_1_ID, MEMBER_3_ID, SECRET_3)).isOk());

        assertEquals(SECRET_3, secretOf(bean, MEMBER_3, CIRCLE_1_ID, MEMBER_3_ID));
        // Their trust level is what it was.
        assertEquals(TrustLevel.READ, trustee(MEMBER_3, CIRCLE_1_ID).getTrustLevel());
    }

    @Test
    void testOnlyAdministratorsSetTheSecretOfAnother() {
        final ManagementBean bean = prepareManagementBean();

        // member2 may write in circle1, which isn't enough.
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), bean.processTrustee(secretRequest(MEMBER_2, CIRCLE_1_ID, MEMBER_3_ID, SECRET_3)).getReturnCode());
        assertNull(trustee(MEMBER_3, CIRCLE_1_ID).getExternalKey());
        // member4 isn't in circle1 at all.
        assertFalse(bean.processTrustee(secretRequest(MEMBER_4, CIRCLE_1_ID, MEMBER_3_ID, SECRET_3)).isOk());
        assertNull(trustee(MEMBER_3, CIRCLE_1_ID).getExternalKey());
        // The System Administrator isn't either, and has no key to seal with.
        assertFalse(bean.processTrustee(secretRequest(Constants.ADMIN_ACCOUNT, CIRCLE_1_ID, MEMBER_3_ID, SECRET_3)).isOk());
        assertNull(trustee(MEMBER_3, CIRCLE_1_ID).getExternalKey());

        // member1 administers circle1.
        assertTrue(bean.processTrustee(secretRequest(MEMBER_1, CIRCLE_1_ID, MEMBER_3_ID, SECRET_3)).isOk());
        assertEquals(SECRET_3, secretOf(bean, MEMBER_3, CIRCLE_1_ID, MEMBER_3_ID));
    }

    @Test
    void testSecretIsReadByItsMemberAndTheAdministratorsOnly() {
        final ManagementBean bean = prepareManagementBean();
        assertTrue(bean.processTrustee(secretRequest(MEMBER_1, CIRCLE_1_ID, MEMBER_2_ID, SECRET_2)).isOk());
        assertTrue(bean.processTrustee(secretRequest(MEMBER_1, CIRCLE_1_ID, MEMBER_3_ID, SECRET_3)).isOk());

        // The administrator reads all of the Circle.
        assertEquals(SECRET_2, secretOf(bean, MEMBER_1, CIRCLE_1_ID, MEMBER_2_ID));
        assertEquals(SECRET_3, secretOf(bean, MEMBER_1, CIRCLE_1_ID, MEMBER_3_ID));
        // The others read their own, and not each other's.
        assertEquals(SECRET_2, secretOf(bean, MEMBER_2, CIRCLE_1_ID, MEMBER_2_ID));
        assertNull(secretOf(bean, MEMBER_2, CIRCLE_1_ID, MEMBER_3_ID));
        assertNull(secretOf(bean, MEMBER_3, CIRCLE_1_ID, MEMBER_2_ID));
        // The System Administrator sees the Trustees, without their secrets.
        assertNull(secretOf(bean, Constants.ADMIN_ACCOUNT, CIRCLE_1_ID, MEMBER_2_ID));
    }

    @Test
    void testSecretIsSealedInTheDatabase() {
        final ManagementBean bean = prepareManagementBean();
        assertTrue(bean.processTrustee(secretRequest(MEMBER_1, CIRCLE_1_ID, MEMBER_2_ID, SECRET_2)).isOk());

        final byte[] stored = trustee(MEMBER_2, CIRCLE_1_ID).getExternalKey();
        assertNotNull(stored);
        assertFalse(new String(stored, StandardCharsets.ISO_8859_1).contains(SECRET_2));
    }

    @Test
    void testEmptySecretRemovesIt() {
        final ManagementBean bean = prepareManagementBean();
        assertTrue(bean.processTrustee(secretRequest(MEMBER_2, CIRCLE_1_ID, MEMBER_2_ID, SECRET_2)).isOk());

        assertTrue(bean.processTrustee(secretRequest(MEMBER_2, CIRCLE_1_ID, MEMBER_2_ID, "")).isOk());

        assertNull(trustee(MEMBER_2, CIRCLE_1_ID).getExternalKey());
        assertNull(secretOf(bean, MEMBER_2, CIRCLE_1_ID, MEMBER_2_ID));
    }

    @Test
    void testSecretWithTrustLevelAndWithNewTrustee() {
        final ManagementBean bean = prepareManagementBean();

        // Altering the trust level and the secret together.
        final ProcessTrusteeRequest alter = secretRequest(MEMBER_1, CIRCLE_1_ID, MEMBER_3_ID, SECRET_3);
        alter.setTrustLevel(TrustLevel.WRITE);
        assertTrue(bean.processTrustee(alter).isOk());
        assertEquals(TrustLevel.WRITE, trustee(MEMBER_3, CIRCLE_1_ID).getTrustLevel());
        assertEquals(SECRET_3, secretOf(bean, MEMBER_3, CIRCLE_1_ID, MEMBER_3_ID));

        // A new Trustee comes with their secret.
        final ProcessTrusteeRequest add = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        add.setAction(Action.ADD);
        add.setCircleId(CIRCLE_1_ID);
        add.setMemberId(MEMBER_5_ID);
        add.setTrustLevel(TrustLevel.READ);
        add.setSecret("wrapped-for-member5");
        assertTrue(bean.processTrustee(add).isOk());
        assertEquals("wrapped-for-member5", secretOf(bean, MEMBER_5, CIRCLE_1_ID, MEMBER_5_ID));
    }

    @Test
    void testSecretGoesWithTheTrustee() {
        final ManagementBean bean = prepareManagementBean();
        assertTrue(bean.processTrustee(secretRequest(MEMBER_3, CIRCLE_1_ID, MEMBER_3_ID, SECRET_3)).isOk());

        final ProcessTrusteeRequest remove = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        remove.setAction(Action.REMOVE);
        remove.setCircleId(CIRCLE_1_ID);
        remove.setMemberId(MEMBER_3_ID);
        assertTrue(bean.processTrustee(remove).isOk());

        assertTrue(entityManager.createQuery("select t from TrusteeEntity t where t.member.name = :name and t.circle.externalId = :circle", TrusteeEntity.class)
                .setParameter("name", MEMBER_3).setParameter("circle", CIRCLE_1_ID).getResultList().isEmpty());
    }

    // =========================================================================
    // Setting several secrets at once
    // =========================================================================

    @Test
    void testSetSecretsSetsAllAndTheCircleKey() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessTrusteeRequest request = secretsRequest(MEMBER_1, CIRCLE_1_ID,
                new TrusteeSecret(MEMBER_1_ID, SECRET_1), new TrusteeSecret(MEMBER_2_ID, SECRET_2), new TrusteeSecret(MEMBER_3_ID, SECRET_3));
        request.setCircleKey("chamber, key version 2");

        final ProcessTrusteeResponse response = bean.processTrustee(request);

        assertTrue(response.isOk(), response.getReturnMessage());
        assertEquals(SECRET_1, secretOf(bean, MEMBER_1, CIRCLE_1_ID, MEMBER_1_ID));
        assertEquals(SECRET_2, secretOf(bean, MEMBER_2, CIRCLE_1_ID, MEMBER_2_ID));
        assertEquals(SECRET_3, secretOf(bean, MEMBER_3, CIRCLE_1_ID, MEMBER_3_ID));
        final FetchCircleRequest fetch = prepareRequest(FetchCircleRequest.class, MEMBER_3);
        assertEquals("chamber, key version 2", bean.fetchCircles(fetch).getCircles().stream()
                .filter(circle -> CIRCLE_1_ID.equals(circle.getCircleId())).findFirst().orElseThrow().getCircleKey());
    }

    @Test
    void testSetSecretsIsAllOrNothing() {
        final ManagementBean bean = prepareManagementBean();
        assertTrue(bean.processTrustee(secretRequest(MEMBER_1, CIRCLE_1_ID, MEMBER_2_ID, "the old secret")).isOk());

        // member4 isn't a Trustee of circle1.
        final ProcessTrusteeRequest request = secretsRequest(MEMBER_1, CIRCLE_1_ID,
                new TrusteeSecret(MEMBER_2_ID, SECRET_2), new TrusteeSecret(MEMBER_4_ID, "for nobody"), new TrusteeSecret(MEMBER_3_ID, SECRET_3));
        request.setCircleKey("never stored");
        final ProcessTrusteeResponse response = bean.processTrustee(request);

        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("the old secret", secretOf(bean, MEMBER_2, CIRCLE_1_ID, MEMBER_2_ID));
        assertNull(secretOf(bean, MEMBER_3, CIRCLE_1_ID, MEMBER_3_ID));
    }

    @Test
    void testSetSecretsNeedsAnAdministrator() {
        final ManagementBean bean = prepareManagementBean();

        final ProcessTrusteeResponse response = bean.processTrustee(secretsRequest(MEMBER_2, CIRCLE_1_ID, new TrusteeSecret(MEMBER_2_ID, SECRET_2)));

        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertNull(trustee(MEMBER_2, CIRCLE_1_ID).getExternalKey());
    }

    @Test
    void testSetSecretsLeavesTheOthersAndRemovesWithAnEmptyOne() {
        final ManagementBean bean = prepareManagementBean();
        assertTrue(bean.processTrustee(secretsRequest(MEMBER_1, CIRCLE_1_ID, new TrusteeSecret(MEMBER_2_ID, SECRET_2), new TrusteeSecret(MEMBER_3_ID, SECRET_3))).isOk());

        assertTrue(bean.processTrustee(secretsRequest(MEMBER_1, CIRCLE_1_ID, new TrusteeSecret(MEMBER_3_ID, ""))).isOk());

        assertEquals(SECRET_2, secretOf(bean, MEMBER_2, CIRCLE_1_ID, MEMBER_2_ID));
        assertNull(trustee(MEMBER_3, CIRCLE_1_ID).getExternalKey());
    }

    @Test
    void testSetSecretsNeedsSomethingToSet() {
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        request.setAction(Action.SECRETS);
        request.setCircleId(CIRCLE_1_ID);

        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), prepareManagementBean().processTrustee(request).getReturnCode());
    }

    // =========================================================================
    // Secrets and invitations
    // =========================================================================

    @Test
    void testSecretWaitsWithTheInvitationAndIsCarriedOver() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest invite = invite(MEMBER_1, "chamber guest", CIRCLE_1_ID);
        invite.setSecret("wrapped-under-the-invite-secret");
        final ProcessMemberResponse invited = bean.processMember(invite);
        assertTrue(invited.isOk(), invited.getReturnMessage());

        // It waits, sealed, with the invitation.
        final List<InvitationEntity> grants = entityManager.createQuery("select i from InvitationEntity i where i.member.name = 'chamber guest'", InvitationEntity.class).getResultList();
        assertEquals(1, grants.size());
        assertNotNull(grants.getFirst().getExternalKey());
        assertFalse(new String(grants.getFirst().getExternalKey(), StandardCharsets.ISO_8859_1).contains("wrapped-under"));

        // Accepting needs nobody else, and brings the vault along.
        final ProcessMemberRequest accept = accept("chamber guest", invited.getInvitation());
        accept.setPublicKey("the guest's public key");
        accept.setVault("the guest's wrapped private key");
        assertTrue(bean.processMember(accept).isOk());

        final FetchTrusteeRequest fetch = new FetchTrusteeRequest();
        fetch.setAccountName("chamber guest");
        fetch.setCredential(PASSPHRASE.getBytes(StandardCharsets.UTF_8));
        fetch.setCircleId(CIRCLE_1_ID);
        fetch.setMemberId(invited.getMemberId());
        final FetchTrusteeResponse fetched = bean.fetchTrustees(fetch);
        assertTrue(fetched.isOk(), fetched.getReturnMessage());
        assertEquals("wrapped-under-the-invite-secret", fetched.getTrustees().getFirst().getSecret());
        assertEquals("the guest's public key", fetched.getTrustees().getFirst().getPublicKey());
        assertTrue(entityManager.createQuery("select i from InvitationEntity i where i.member.name = 'chamber guest'", InvitationEntity.class).getResultList().isEmpty());

        // The guest then replaces it with one wrapped to their own key.
        final ProcessTrusteeRequest own = new ProcessTrusteeRequest();
        own.setAccountName("chamber guest");
        own.setCredential(PASSPHRASE.getBytes(StandardCharsets.UTF_8));
        own.setAction(Action.ALTER);
        own.setCircleId(CIRCLE_1_ID);
        own.setMemberId(invited.getMemberId());
        own.setSecret("wrapped-to-my-own-key");
        assertTrue(bean.processTrustee(own).isOk());
        assertEquals("wrapped-to-my-own-key", secretOf(bean, MEMBER_1, CIRCLE_1_ID, invited.getMemberId()));

        final FetchMemberRequest self = new FetchMemberRequest();
        self.setAccountName("chamber guest");
        self.setCredential(PASSPHRASE.getBytes(StandardCharsets.UTF_8));
        self.setMemberId(invited.getMemberId());
        assertEquals("the guest's wrapped private key", bean.fetchMembers(self).getMembers().getFirst().getVault());
    }

    @Test
    void testExistingAccountGetsTheSecretAtOnce() {
        final ManagementBean bean = prepareManagementBean();
        // member5 isn't in circle1 yet.
        final ProcessMemberRequest invite = invite(MEMBER_1, MEMBER_5, CIRCLE_1_ID);
        invite.setSecret("wrapped-to-member5");

        assertTrue(bean.processMember(invite).isOk());

        assertEquals("wrapped-to-member5", secretOf(bean, MEMBER_5, CIRCLE_1_ID, MEMBER_5_ID));
    }

    @Test
    void testSecretOfAnInvitationNeedsACircle() {
        final ProcessMemberRequest invite = invite(Constants.ADMIN_ACCOUNT, "no chamber", null);
        invite.setSecret("for no Circle");

        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), prepareManagementBean().processMember(invite).getReturnCode());
    }

    // =========================================================================
    // Secrets outlive passphrases and MasterKeys
    // =========================================================================

    @Test
    void testSecretIsReadAfterANewPassphrase() {
        final ManagementBean bean = prepareManagementBean();
        assertTrue(bean.processTrustee(secretRequest(MEMBER_2, CIRCLE_1_ID, MEMBER_2_ID, SECRET_2)).isOk());

        final ProcessMemberRequest update = prepareRequest(ProcessMemberRequest.class, MEMBER_2);
        update.setAction(Action.UPDATE);
        update.setNewCredential(crypto.stringToBytes(NEW_PASSPHRASE));
        assertTrue(bean.processMember(update).isOk());

        final FetchTrusteeRequest fetch = new FetchTrusteeRequest();
        fetch.setAccountName(MEMBER_2);
        fetch.setCredential(crypto.stringToBytes(NEW_PASSPHRASE));
        fetch.setCircleId(CIRCLE_1_ID);
        fetch.setMemberId(MEMBER_2_ID);
        assertEquals(SECRET_2, bean.fetchTrustees(fetch).getTrustees().getFirst().getSecret());
        // And by the administrator, whose passphrase didn't change.
        assertEquals(SECRET_2, secretOf(bean, MEMBER_1, CIRCLE_1_ID, MEMBER_2_ID));
    }

    @Test
    void testSecretIsReadAfterANewMasterKey() {
        final ManagementBean bean = prepareManagementBean();
        assertTrue(bean.processTrustee(secretRequest(MEMBER_2, CIRCLE_1_ID, MEMBER_2_ID, SECRET_2)).isOk());
        final byte[] before = trustee(MEMBER_2, CIRCLE_1_ID).getExternalKey();

        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(NEW_MASTERKEY_SECRET.getBytes(StandardCharsets.UTF_8));
        assertTrue(bean.masterKey(request).isOk());

        // The rotation doesn't touch it, and it still opens.
        assertEquals(new String(before, StandardCharsets.ISO_8859_1), new String(trustee(MEMBER_2, CIRCLE_1_ID).getExternalKey(), StandardCharsets.ISO_8859_1));
        assertEquals(SECRET_2, secretOf(bean, MEMBER_2, CIRCLE_1_ID, MEMBER_2_ID));
    }

    // =========================================================================
    // The Member's vault
    // =========================================================================

    @Test
    void testVaultIsTheMembersAlone() {
        final ManagementBean bean = prepareManagementBean();
        assertTrue(bean.processMember(vaultRequest(MEMBER_1, "my wrapped private key")).isOk());

        assertEquals("my wrapped private key", vaultOf(bean, MEMBER_1, MEMBER_1_ID));
        // Neither another Member nor the System Administrator gets it.
        assertNull(vaultOf(bean, MEMBER_2, MEMBER_1_ID));
        assertNull(vaultOf(bean, Constants.ADMIN_ACCOUNT, MEMBER_1_ID));
        final FetchMemberResponse all = bean.fetchMembers(prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT));
        assertTrue(all.getMembers().stream().allMatch(member -> member.getVault() == null));
    }

    @Test
    void testVaultStaysUntilAnEmptyOneRemovesIt() {
        final ManagementBean bean = prepareManagementBean();
        assertTrue(bean.processMember(vaultRequest(MEMBER_1, "my wrapped private key")).isOk());

        // An update without a vault leaves it.
        final ProcessMemberRequest other = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        other.setAction(Action.UPDATE);
        other.setPublicKey("a public key");
        assertTrue(bean.processMember(other).isOk());
        assertEquals("my wrapped private key", vaultOf(bean, MEMBER_1, MEMBER_1_ID));

        assertTrue(bean.processMember(vaultRequest(MEMBER_1, "")).isOk());
        assertNull(vaultOf(bean, MEMBER_1, MEMBER_1_ID));
    }

    @Test
    void testVaultAndSecretHaveALimit() {
        final ManagementBean bean = prepareManagementBean();
        final String tooLong = "x".repeat(Constants.MAX_VAULT_LENGTH + 1);
        final String justRight = "x".repeat(Constants.MAX_VAULT_LENGTH);

        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), bean.processMember(vaultRequest(MEMBER_1, tooLong)).getReturnCode());
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), bean.processTrustee(secretRequest(MEMBER_1, CIRCLE_1_ID, MEMBER_1_ID, tooLong)).getReturnCode());

        assertTrue(bean.processMember(vaultRequest(MEMBER_1, justRight)).isOk());
        assertTrue(bean.processTrustee(secretRequest(MEMBER_1, CIRCLE_1_ID, MEMBER_1_ID, justRight)).isOk());
        assertEquals(justRight, vaultOf(bean, MEMBER_1, MEMBER_1_ID));
        assertEquals(justRight, secretOf(bean, MEMBER_1, CIRCLE_1_ID, MEMBER_1_ID));
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private static ProcessTrusteeRequest secretRequest(final String account, final String circleId, final String memberId, final String secret) {
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, account);
        request.setAction(Action.ALTER);
        request.setCircleId(circleId);
        request.setMemberId(memberId);
        request.setSecret(secret);
        return request;
    }

    private static ProcessTrusteeRequest secretsRequest(final String account, final String circleId, final TrusteeSecret... secrets) {
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, account);
        request.setAction(Action.SECRETS);
        request.setCircleId(circleId);
        request.setSecrets(List.of(secrets));
        return request;
    }

    private static ProcessMemberRequest vaultRequest(final String account, final String vault) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, account);
        request.setAction(Action.UPDATE);
        request.setVault(vault);
        return request;
    }

    private static ProcessMemberRequest invite(final String inviter, final String name, final String circleId) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, inviter);
        request.setAction(Action.INVITE);
        request.setNewAccountName(name);
        request.setCircleId(circleId);
        return request;
    }

    private static ProcessMemberRequest accept(final String name, final byte[] token) {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(name);
        request.setAction(Action.UPDATE);
        request.setCredentialType(CredentialType.INVITATION);
        request.setCredential(token.clone());
        request.setNewCredential(PASSPHRASE.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    /** The secret of a Trustee, as the given account gets it. */
    private static String secretOf(final ManagementBean bean, final String account, final String circleId, final String memberId) {
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, account);
        request.setCircleId(circleId);
        final FetchTrusteeResponse response = bean.fetchTrustees(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        // The secret may be null, which a Stream can't carry.
        for (final Trustee trustee : response.getTrustees()) {
            if (memberId.equals(trustee.getMemberId())) {
                return trustee.getSecret();
            }
        }
        return null;
    }

    private static String vaultOf(final ManagementBean bean, final String account, final String memberId) {
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class, account);
        request.setMemberId(memberId);
        final FetchMemberResponse response = bean.fetchMembers(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getMembers().getFirst().getVault();
    }

    private TrusteeEntity trustee(final String account, final String circleId) {
        entityManager.flush();
        entityManager.clear();
        return entityManager.createQuery("select t from TrusteeEntity t where t.member.name = :name and t.circle.externalId = :circle", TrusteeEntity.class)
                .setParameter("name", account).setParameter("circle", circleId).getSingleResult();
    }
}
