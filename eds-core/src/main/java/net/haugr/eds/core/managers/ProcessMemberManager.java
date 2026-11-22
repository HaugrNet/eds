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
package net.haugr.eds.core.managers;

import java.security.MessageDigest;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.EntityManager;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.CredentialType;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.enums.Status;
import net.haugr.eds.core.exceptions.AuthenticationException;
import net.haugr.eds.core.exceptions.AuthorizationException;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.exceptions.IdentificationException;
import net.haugr.eds.core.exceptions.IllegalActionException;
import net.haugr.eds.core.exceptions.VerificationException;
import net.haugr.eds.core.jce.MemberKeyPair;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.MemberDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.model.entities.InvitationEntity;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.model.entities.SessionEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;

/**
 * <p>Business Logic implementation for the EDS ProcessMember request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class ProcessMemberManager extends AbstractManager<MemberDao, ProcessMemberResponse, ProcessMemberRequest> {

    private static final int INVALIDATED_PASSWORD_LENGTH = 64;
    // 256 bits, PLAN.md, Crypto "Invitations".
    private static final int INVITATION_TOKEN_LENGTH = 32;
    // 256 bits, PLAN.md, Crypto §2a.
    private static final int SESSION_TOKEN_LENGTH = 32;
    // Marker for the key columns of an Account with a pending invitation.
    static final String INVITATION_MARKER = CredentialType.INVITATION.name();
    // Labels of the Circle Secret sealed for an invitation into a Circle.
    private static final String GRANT_SALT = "eds/invitation/grant";
    private static final String GRANT_AAD = "eds/invitation/grant|";
    private static final String GRANT_OUTER_AAD = "eds/invitation/grant-outer|";
    // A stone may have several administrators; only the first, which claimed
    // it, makes, unmakes or deletes them, as it alone gives the MasterKey.
    private static final String ONLY_FIRST_ADMINISTRATOR = "Only the first System Administrator may make, unmake or delete administrators.";
    private static final String SYSTEM_ADMINISTRATOR_NOT_DELETABLE = "The System Administrator Account may not be deleted.";

    public ProcessMemberManager(final Settings settings, final EntityManager entityManager) {
        super(settings, new MemberDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessMemberResponse perform(final ProcessMemberRequest request) {
        throwConditionalNullException(request, ReturnCode.VERIFICATION_WARNING, "Cannot Process a NULL Object.");
        final ProcessMemberResponse response;

        if (request.getCredentialType() == CredentialType.INVITATION) {
            verify(request);
            final byte[] newCredential = request.getNewCredential();
            throwConditionalException((newCredential == null) || (newCredential.length == 0),
                    ReturnCode.VERIFICATION_WARNING, "The " + Constants.FIELD_NEW_CREDENTIAL + " is missing in Request.");

            response = processInvitation(request);
        } else {
            // Pre-checks, & destruction of credentials
            verifyRequest(request, Permission.PROCESS_MEMBER);
            Arrays.fill(request.getCredential(), (byte) 0);
            response = processActions(request);
        }

        return response;
    }

    private ProcessMemberResponse processActions(final ProcessMemberRequest request) {
        return switch (request.getAction()) {
            case CREATE -> createMember(request);
            case INVITE -> inviteMember(request);
            case LOGIN -> loginMember(request);
            case LOGOUT -> logoutMember();
            case ALTER -> alterMember(request);
            case UPDATE -> updateMember(request);
            case INVALIDATE -> invalidate(request);
            case DELETE -> deleteMember(request);
            // Unreachable Code by design.
            default -> throw new IllegalActionException("Unsupported Action.");
        };
    }

    private ProcessMemberResponse createMember(final ProcessMemberRequest request) {
        // Creating new Members can only be performed by the System
        // Administrator, not by anyone else.
        if (member.getMemberRole() != MemberRole.ADMIN) {
            throw new AuthorizationException("Members are not permitted to create new Accounts.");
        }

        final String accountName = request.getNewAccountName().trim();
        final MemberEntity found = dao.findMemberByName(accountName);
        if (found != null) {
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING, "An Account with the requested AccountName already exist.");
        }

        final MemberRole role = whichRole(request);
        if ((role == MemberRole.ADMIN) && !isSystemAdministrator(member)) {
            throw new AuthorizationException(ONLY_FIRST_ADMINISTRATOR);
        }
        final MemberEntity created = createNewAccount(accountName, role, request.getNewCredential());
        final ProcessMemberResponse response = new ProcessMemberResponse(theMember(created) + " was successfully added to EDS.");
        response.setMemberId(created.getExternalId());

        return response;
    }

    /**
     * <p>Invites a new Member. The System Administrator may invite anyone;
     * other Members only into a Circle they administer (PLAN.md, Crypto,
     * Invitations). With a Circle, the invited Member gets access to it when
     * accepting: the Circle Secret is sealed with a key derived from the
     * token, so only the person with the token can unlock it, and nobody has
     * to grant the access after the acceptance. A name which already has an
     * account is added to the Circle at once, without an invitation.</p>
     *
     * <p>A name with a pending invitation can't be invited again, but an
     * expired invitation doesn't hold its name: it is replaced by the new
     * one, with a new token, and whatever room access it had goes. (The
     * sanity check removes expired invitations too, but only when it
     * runs.)</p>
     */
    private ProcessMemberResponse inviteMember(final ProcessMemberRequest request) {
        if (request.getMemberRole() == MemberRole.ADMIN) {
            throw new IllegalActionException("An invitation makes a user; an administrator is made by creating the account, or by changing the role afterwards.");
        }
        final boolean systemAdministrator = member.getMemberRole() == MemberRole.ADMIN;
        final TrusteeEntity inviter = findInvitingTrustee(request.getCircleId(), systemAdministrator);
        if (inviter != null) {
            // The current Circle Secret is given, also if a rotation started
            // meanwhile; see PLAN.md, Crypto C6.
            dao.lockCircle(inviter.getCircle());
            dao.refresh(inviter);
        }
        final TrustLevel trustLevel = invitedTrustLevel(request);

        final String memberName = request.getNewAccountName().trim();
        final MemberEntity found = dao.findMemberByName(memberName);
        final boolean expired = (found != null) && hasExpiredInvitation(found);
        if ((found != null) && !expired) {
            if (Objects.equals(found.getPrivateKey(), INVITATION_MARKER)) {
                throw new EDSException(ReturnCode.CONSTRAINT_ERROR, "Cannot create an invitation, as one is pending for the name until " + found.getInvitationExpire() + " (UTC).");
            }
            if (inviter == null) {
                throw new EDSException(ReturnCode.CONSTRAINT_ERROR, "Cannot create an invitation, as the account already exists.");
            }
            return addToCircle(found, inviter, trustLevel, request.getSecret());
        }

        // The token is only returned to the inviter. EDS keeps a keyed hash
        // (HMAC with a MasterKey-derived key), so a copy of the database
        // can't be used to accept the invitation.
        final byte[] token = new byte[INVITATION_TOKEN_LENGTH];
        RANDOM.nextBytes(token);

        // An invitation makes a user, never an administrator: a link which
        // falls into the wrong hands must not give more than that.
        final MemberEntity entity = invitedAccount(expired ? found : null, memberName, token, MemberRole.STANDARD);

        String into = "";
        if (inviter != null) {
            final InvitationEntity grant = new InvitationEntity();
            grant.setMember(entity);
            grant.setCircle(inviter.getCircle());
            grant.setKey(inviter.getKey());
            grant.setTrustLevel(trustLevel);
            final SecretEDSKey circleKey = unwrapCircleKey(inviter);
            grant.setCircleKey(sealGrant(token, entity, inviter.getCircle(), circleKey));
            // The invited Member's secret in the Circle (E2E.md) waits with
            // the invitation, sealed as it will be with the Trustee.
            grant.setExternalKey(sealTrusteeSecret(circleKey, inviter.getCircle(), entity, request.getSecret()));
            dao.save(grant);
            into = " to the Circle '" + inviter.getCircle().getName() + "'";
        }

        final ProcessMemberResponse response = new ProcessMemberResponse("An invitation" + into + " was successfully issued for '" + memberName + "'.");
        response.setMemberId(entity.getExternalId());
        response.setInvitation(token);

        return response;
    }

    /**
     * The account of an invitation, which only the token can turn into a
     * real one. An expired invitation is taken over: its token and its room
     * access are replaced, so nothing of it can be used any more.
     */
    private MemberEntity invitedAccount(final MemberEntity expired, final String name, final byte[] token, final MemberRole role) {
        final MemberEntity entity = (expired != null) ? expired : new MemberEntity();
        if (expired != null) {
            for (final InvitationEntity old : dao.findInvitations(expired)) {
                dao.delete(old);
            }
        }
        entity.setName(name);
        entity.setSalt(crypto.hmacWithMasterKey(INVITATION_LABEL, token));
        entity.setPbeAlgorithm(settings.getPasswordAlgorithm());
        entity.setPbeIterations(settings.getPasswordIterations());
        entity.setKeyAlgorithm(settings.getAsymmetricAlgorithm());
        entity.setPrivateKey(INVITATION_MARKER);
        entity.setPublicKey(INVITATION_MARKER);
        entity.setMemberRole(role);
        entity.setInvitationExpire(Utilities.newDate().plusHours(settings.getInvitationTimeout()));
        dao.save(entity);

        return entity;
    }

    private static boolean hasExpiredInvitation(final MemberEntity account) {
        return Objects.equals(account.getPrivateKey(), INVITATION_MARKER)
                && (account.getInvitationExpire() != null)
                && account.getInvitationExpire().isBefore(Utilities.newDate());
    }

    private TrusteeEntity findInvitingTrustee(final String circleId, final boolean systemAdministrator) {
        if (circleId == null) {
            if (!systemAdministrator) {
                throw new IllegalActionException("Members may only invite into a Circle which they administer.");
            }
            return null;
        }

        final TrusteeEntity trustee = dao.findTrusteeByCircleAndMember(circleId, member.getExternalId());
        if ((trustee == null) || !TrustLevel.getLevels(TrustLevel.ADMIN).contains(trustee.getTrustLevel())) {
            throw new AuthorizationException("Only an administrator of the Circle may invite into it.");
        }

        return trustee;
    }

    private static TrustLevel invitedTrustLevel(final ProcessMemberRequest request) {
        final TrustLevel level = (request.getTrustLevel() != null) ? request.getTrustLevel() : TrustLevel.WRITE;
        if (!EnumSet.of(TrustLevel.READ, TrustLevel.WRITE, TrustLevel.ADMIN).contains(level)) {
            throw new VerificationException("The trust level of an invited Member must be READ, WRITE or ADMIN.");
        }

        return level;
    }

    private ProcessMemberResponse addToCircle(final MemberEntity existing, final TrusteeEntity inviter, final TrustLevel trustLevel, final String secret) {
        if (dao.findTrusteeByCircleAndMember(inviter.getCircle().getExternalId(), existing.getExternalId()) != null) {
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING, "The Member is already a trustee of the requested Circle.");
        }

        final TrusteeEntity trustee = new TrusteeEntity();
        trustee.setMember(existing);
        trustee.setCircle(inviter.getCircle());
        trustee.setTrustLevel(trustLevel);
        final SecretEDSKey circleKey = unwrapCircleKey(inviter);
        grantCircleSecrets(trustee, inviter, circleKey);
        trustee.setExternalKey(sealTrusteeSecret(circleKey, inviter.getCircle(), existing, secret));
        dao.save(trustee);

        final ProcessMemberResponse response = new ProcessMemberResponse(theMember(existing) + " was added to the Circle '" + inviter.getCircle().getName() + "'.");
        response.setMemberId(existing.getExternalId());

        return response;
    }

    /**
     * Seals the Circle Secret for an invitation: inside with a key derived
     * from the invitation token with HKDF-SHA512, outside with the MasterKey,
     * both bound to the invited Member and the Circle.
     */
    private String sealGrant(final byte[] token, final MemberEntity invited, final CircleEntity circle, final SecretEDSKey circleKey) {
        final String binding = grantBinding(invited, circle);
        final byte[] aad = crypto.stringToBytes(GRANT_AAD + binding);
        final byte[] encoded = circleKey.getEncoded();
        try {
            final byte[] inner = Crypto.seal(grantKey(token, binding), aad, encoded);
            return crypto.sealWithMasterKey(GRANT_OUTER_AAD + binding, inner);
        } finally {
            Arrays.fill(encoded, (byte) 0);
        }
    }

    private SecretEDSKey openGrant(final byte[] token, final InvitationEntity grant) {
        final String binding = grantBinding(grant.getMember(), grant.getCircle());
        final byte[] inner = crypto.openWithMasterKey(GRANT_OUTER_AAD + binding, grant.getCircleKey());
        final byte[] raw = Crypto.open(grantKey(token, binding), crypto.stringToBytes(GRANT_AAD + binding), inner);
        try {
            final KeyAlgorithm algorithm = grant.getKey().getAlgorithm();
            return new SecretEDSKey(algorithm, new javax.crypto.spec.SecretKeySpec(raw, algorithm.getName()));
        } finally {
            Arrays.fill(raw, (byte) 0);
        }
    }

    private SecretEDSKey grantKey(final byte[] token, final String binding) {
        return Crypto.deriveKey(token, crypto.stringToBytes(GRANT_SALT), crypto.stringToBytes(GRANT_AAD + binding));
    }

    private static String grantBinding(final MemberEntity invited, final CircleEntity circle) {
        return invited.getExternalId() + '|' + circle.getExternalId();
    }

    private static MemberRole whichRole(final ProcessMemberRequest request) {
        return (request.getMemberRole() != null) ? request.getMemberRole() : MemberRole.STANDARD;
    }

    private ProcessMemberResponse loginMember(final ProcessMemberRequest request) {
        // Step 1; A random Session token (PLAN.md, Crypto §2a), returned once. The
        // database only gets an HMAC of it for the lookup, which can't be
        // computed without the MasterKey.
        final byte[] token = new byte[SESSION_TOKEN_LENGTH];
        RANDOM.nextBytes(token);
        final String checksum = crypto.hmacWithMasterKey(SESSION_LOOKUP_LABEL, token);

        // Step 2; The Session copy of the private keys, double wrapped like
        // the permanent copy: inside with the Session key (HKDF of the token,
        // the Member and the expiry), outside with the MasterKey. Requests
        // with the Session thus don't need PBKDF2.
        member.setSessionExpire(Utilities.newDate().plusMinutes(settings.getSessionTimeout()).truncatedTo(ChronoUnit.SECONDS));
        final SecretEDSKey key = sessionKey(member, token);
        final String privateKey = crypto.protectPrivateKeys(key, sessionKeyAad(member), sessionOuterAad(member), keyPair);

        // Step 3; A Session of its own (PLAN.md, "Several sessions"): the
        // Member's other Sessions stay, up to the limit, beyond which the
        // one that expires first ends.
        final List<SessionEntity> sessions = dao.findSessions(member);
        for (int i = 0; i <= (sessions.size() - settings.getSessionMaxPerMember()); i++) {
            dao.removeSession(sessions.get(i));
        }
        final SessionEntity created = new SessionEntity();
        created.setMember(member);
        created.setChecksum(checksum);
        created.setCrypto(privateKey);
        created.setExpire(member.getSessionExpire());
        dao.save(created);

        final ProcessMemberResponse response = new ProcessMemberResponse(theMember(member) + " has successfully logged in.");
        response.setSession(token);
        // Clients need it to recognise the Member, e.g. their own entries of a feature.
        response.setMemberId(member.getExternalId());

        return response;
    }

    private ProcessMemberResponse logoutMember() {
        // A logout ends the Session it comes with, and leaves the Member's
        // other Sessions alone. Without a Session, there is nothing to end.
        if (session != null) {
            dao.removeSession(session);
            session = null;
        }
        return new ProcessMemberResponse();
    }

    private ProcessMemberResponse alterMember(final ProcessMemberRequest request) {
        if (member.getMemberRole() != MemberRole.ADMIN) {
            throw new AuthorizationException("Only Administrators may update the Role of a member.");
        }
        if (!isSystemAdministrator(member)) {
            throw new AuthorizationException(ONLY_FIRST_ADMINISTRATOR);
        }

        if (member.getExternalId().equals(request.getMemberId())) {
            throw new IllegalActionException("It is not permitted to alter own account.");
        }

        final String memberId = request.getMemberId();
        final MemberEntity entity = dao.find(MemberEntity.class, memberId);
        if (entity == null) {
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING, "The requested Member cannot be found.");
        }
        if (isSystemAdministrator(entity)) {
            throw new IllegalActionException("The role of the System Administrator Account may not be altered.");
        }
        entity.setMemberRole(request.getMemberRole());
        dao.save(entity);

        final ProcessMemberResponse response = new ProcessMemberResponse(theMember(entity) + " has successfully been given the new role '" + request.getMemberRole() + "'.");
        response.setMemberId(memberId);

        return response;
    }

    /** The Member's settings for clients must be a JSON object; EDS doesn't look into it. */
    private static String checkedSettings(final String settings) {
        try (jakarta.json.JsonReader reader = jakarta.json.Json.createReader(new java.io.StringReader(settings))) {
            reader.readObject();
            return settings;
        } catch (jakarta.json.JsonException | IllegalStateException e) {
            final VerificationException exception = new VerificationException("The settings must be a JSON object.");
            exception.initCause(e);
            throw exception;
        }
    }

    private ProcessMemberResponse updateMember(final ProcessMemberRequest request) {
        final String newAccountName = trim(request.getNewAccountName());

        // The new Credential is verified before anything is changed, so a
        // weak Credential doesn't leave the Account partially updated.
        if ((request.getNewCredential() != null) && (request.getCredentialType() == CredentialType.PASSPHRASE)) {
            verifyEntropy("Credential", request.getNewCredential(), member.getName(), newAccountName);
        }

        updateOwnAccountName(newAccountName);
        updateOwnCredential(request);
        updateOwnPublicKey(request);
        if (request.getAvatar() != null) {
            // An empty avatar removes it.
            member.setAvatar(request.getAvatar().isEmpty() ? null : request.getAvatar());
        }
        if (request.getSettings() != null) {
            // Empty settings remove them.
            member.setSettings(request.getSettings().isBlank() ? null : checkedSettings(request.getSettings()));
        }
        if (request.getVault() != null) {
            // The vault (E2E.md) is stored as given; an empty one removes it.
            member.setVault(request.getVault().isEmpty() ? null : request.getVault());
        }
        dao.save(member);

        final ProcessMemberResponse response = new ProcessMemberResponse(theMember(member) + " was successfully updated.");
        response.setMemberId(member.getExternalId());
        return response;
    }

    private void updateOwnAccountName(final String newAccountName) {
        if (!isEmpty(newAccountName)) {
            final MemberEntity existing = dao.findMemberByName(newAccountName);
            if (existing != null) {
                throw new EDSException(ReturnCode.CONSTRAINT_ERROR, "The new Account Name already exists.");
            }

            member.setName(newAccountName);
        }
    }

    private void updateOwnCredential(final ProcessMemberRequest request) {
        final byte[] credential = request.getNewCredential();
        if (credential != null) {
            if (request.getCredentialType() != CredentialType.PASSPHRASE) {
                throw new EDSException(ReturnCode.VERIFICATION_WARNING, "It is only permitted to update the credentials when authenticating with Passphrase.");
            }

            // The new keys first, which takes long, for PBKDF2 and a new key
            // pair. Then the Circles are locked, and the Trustee rows read
            // anew. A rotation of a Circle Secret, see PLAN.md, Crypto C6,
            // either came before and wrapped to the old keys, which are still
            // those of this request, so it is wrapped anew here, or it comes
            // after and wraps to the new keys. Neither undoes the other.
            final MemberKeyPair pair = updateMemberPassword(member, credential);
            Arrays.fill(credential, (byte) 0);
            final List<TrusteeEntity> list = dao.findTrusteesByMember(member, EnumSet.allOf(TrustLevel.class));
            dao.lockCircles(list.stream().map(TrusteeEntity::getCircle).toList());
            list.forEach(dao::refresh);

            for (final TrusteeEntity trustee : list) {
                rewrapCircleSecrets(trustee, pair.getPublic());
                dao.save(trustee);
            }
        }
    }

    private void updateOwnPublicKey(final ProcessMemberRequest request) {
        if (request.getPublicKey() != null) {
            member.setMemberKey(request.getPublicKey());
        }
    }

    /**
     * <p>Invalidating the account means making a change, so it is still
     * usable, i.e., the Member can still access the Account, but it is not
     * possible to access any encrypted data, as the key used for the
     * encryption has been altered.</p>
     *
     * <p>This way, the account will appear fine, but will be useless.</p>
     *
     * @param request Request Object
     * @return New Response Object
     */
    private ProcessMemberResponse invalidate(final ProcessMemberRequest request) {
        // Invitations can only be issued by the System Administrator, not by
        // anyone else.
        if (member.getMemberRole() == MemberRole.ADMIN) {
            throw new IllegalActionException("The System Administrator Account may not be invalidated.");
        }

        // The credential of the request has already been overwritten with
        // zeros, so it cannot be used. Instead, the Account is given a random
        // password, which is never revealed.
        final byte[] randomPassword = new byte[INVALIDATED_PASSWORD_LENGTH];
        RANDOM.nextBytes(randomPassword);

        dao.removeSessions(member);
        updateMemberPassword(member, randomPassword);
        Arrays.fill(randomPassword, (byte) 0);

        final ProcessMemberResponse response = new ProcessMemberResponse();
        response.setReturnMessage(theMember(member) + " has been Invalidated.");

        return response;
    }

    private ProcessMemberResponse deleteMember(final ProcessMemberRequest request) {
        final ProcessMemberResponse response;

        if (request.getMemberId() != null) {
            if (member.getMemberRole() != MemberRole.ADMIN) {
                throw new IllegalActionException("Members are not permitted to delete Accounts.");
            }

            response = processDeleteAsAdmin(request);
        } else {
            // Deleting self
            if (isSystemAdministrator(member)) {
                throw new IllegalActionException(SYSTEM_ADMINISTRATOR_NOT_DELETABLE);
            }
            checkNotLastTrustee(member);
            rotateAfterLeaving(member);
            dao.delete(member);
            response = new ProcessMemberResponse(ReturnCode.SUCCESS, theMember(member) + " has been successfully deleted.");
        }

        return response;
    }

    /**
     * The Circles of a Member who is deleted rotate their Circle Secret now,
     * as for a Trustee who leaves (PLAN.md, Crypto C6).
     *
     * @param leaving The Member who is deleted
     */
    private void rotateAfterLeaving(final MemberEntity leaving) {
        for (final TrusteeEntity trustee : dao.findTrusteesByMember(leaving, EnumSet.allOf(TrustLevel.class))) {
            final CircleEntity circle = trustee.getCircle();
            circle.setRotateCircleKeyDue(Utilities.newDate());
            dao.save(circle);
            // The row goes with the account, by the database.
            dao.detach(trustee);
        }
    }

    private ProcessMemberResponse processDeleteAsAdmin(final ProcessMemberRequest request) {
        final MemberEntity found = dao.find(MemberEntity.class, request.getMemberId());

        if (found == null) {
            throw new IdentificationException("No such Account exist.");
        }
        if (Objects.equals(member.getId(), found.getId())) {
            throw new IllegalActionException("It is not permitted to delete yourself.");
        }
        if (isSystemAdministrator(found)) {
            throw new IllegalActionException(SYSTEM_ADMINISTRATOR_NOT_DELETABLE);
        }
        if ((found.getMemberRole() == MemberRole.ADMIN) && !isSystemAdministrator(member)) {
            throw new AuthorizationException(ONLY_FIRST_ADMINISTRATOR);
        }
        checkNotLastTrustee(found);
        rotateAfterLeaving(found);

        dao.delete(found);
        return new ProcessMemberResponse(ReturnCode.SUCCESS, theMember(found) + " has successfully been deleted.");
    }

    /**
     * With an account its Trustee records go. If it is the last Trustee of a
     * Circle, the last copy of the Circle Secret which can be unwrapped
     * would go with it, and the Circle's data could never be read again. So
     * such Circles are deleted, or get another Trustee, first; the same rule
     * as for leaving a Circle (see {@link ProcessTrusteeManager}).
     */
    private void checkNotLastTrustee(final MemberEntity account) {
        final List<String> circles = dao.findCircleNamesOfLastTrustee(account);
        if (!circles.isEmpty()) {
            throw new IllegalActionException(theMember(account) + " is the last Member of '" + String.join("', '", circles)
                    + "', whose data nobody could read again. Delete these Circles or add another Member to them first.");
        }
    }

    private ProcessMemberResponse processInvitation(final ProcessMemberRequest request) {
        final MemberEntity account = dao.findMemberByName(request.getAccountName());
        if (account == null) {
            throw new IdentificationException("Account does not exist.");
        }

        if (!Objects.equals(account.getPrivateKey(), INVITATION_MARKER) || (account.getInvitationExpire() == null)) {
            throw new VerificationException("Account does not have an invitation pending.");
        }

        // The token is compared in constant time; the expiry is checked
        // afterwards, so an expired invitation doesn't reveal anything
        // without the correct token.
        final byte[] token = request.getCredential();
        try {
            final boolean valid = MessageDigest.isEqual(
                    crypto.stringToBytes(account.getSalt()),
                    crypto.stringToBytes(crypto.hmacWithMasterKey(INVITATION_LABEL, token)));
            if (!valid) {
                throw new AuthenticationException("The given invitation is invalid.");
            }
            if (account.getInvitationExpire().isBefore(Utilities.newDate())) {
                throw new AuthenticationException("The invitation has expired.");
            }

            final byte[] newSecret = request.getNewCredential();
            verifyEntropy("Credential", newSecret, account.getName());

            account.setMemberKey(request.getPublicKey());
            if ((request.getVault() != null) && !request.getVault().isEmpty()) {
                account.setVault(request.getVault());
            }
            account.setInvitationExpire(null);
            updateMemberPassword(account, newSecret);
            Arrays.fill(newSecret, (byte) 0);

            // The room access of the invitation: the Circle Secret is opened
            // with the token, and wrapped to the new Member's keys.
            for (final InvitationEntity grant : dao.findInvitations(account)) {
                if (grant.getKey().getStatus() != Status.ACTIVE) {
                    // The Circle Secret was rotated since the invitation was
                    // issued (PLAN.md, Crypto C6); the access went with it.
                    dao.delete(grant);
                    continue;
                }
                final TrusteeEntity trustee = new TrusteeEntity();
                trustee.setMember(account);
                trustee.setCircle(grant.getCircle());
                trustee.setKey(grant.getKey());
                trustee.setTrustLevel(grant.getTrustLevel());
                trustee.setCircleKey(wrapCircleKey(account, grant.getCircle(), grant.getKey(), openGrant(token, grant)));
                // The secret which waited with the invitation (E2E.md).
                trustee.setExternalKey(grant.getExternalKey());
                dao.save(trustee);
                dao.delete(grant);
            }
        } finally {
            Arrays.fill(token, (byte) 0);
        }

        final ProcessMemberResponse response = new ProcessMemberResponse("The invitation was successfully processed for '" + account.getName() + "'.");
        response.setMemberId(account.getExternalId());

        return response;
    }

    /**
     * The System Administrator is the first account of the stone, see
     * {@link #systemAdministrator()}. A stone only takes a first account
     * while it has none, so there must always be this one: it may neither
     * be deleted nor have its role altered. It is not known by its name,
     * which it chose and may change like any other account.
     */
    private boolean isSystemAdministrator(final MemberEntity entity) {
        final MemberEntity first = systemAdministrator();
        return (entity != null) && (first != null) && Objects.equals(first.getId(), entity.getId());
    }

    /**
     * <p>Wrapper method to ensure that the member is always presented the
     * same way. The method simply returns the Member and member name.</p>
     *
     * @param member Member Entity to read the name from
     * @return String starting with 'the Member' and then the member name quoted
     */
    private static String theMember(final MemberEntity member) {
        return "The Member '" + member.getName() + "'";
    }
}
