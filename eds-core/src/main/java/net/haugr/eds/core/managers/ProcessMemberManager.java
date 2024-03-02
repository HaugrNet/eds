/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.EntityManager;
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
import net.haugr.eds.core.exceptions.AuthenticationException;
import net.haugr.eds.core.exceptions.AuthorizationException;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.exceptions.IdentificationException;
import net.haugr.eds.core.exceptions.IllegalActionException;
import net.haugr.eds.core.exceptions.VerificationException;
import net.haugr.eds.core.jce.EDSKeyPair;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.jce.IVSalt;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.MemberDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;

/**
 * <p>Business Logic implementation for the EDS ProcessMember request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class ProcessMemberManager extends AbstractManager<MemberDao, ProcessMemberResponse, ProcessMemberRequest> {

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

        if (request.getCredentialType() == CredentialType.SIGNATURE) {
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
        // Creating new Members, can only be performed by the System
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
        final MemberEntity created = createNewAccount(accountName, role, request.getNewCredential());
        final ProcessMemberResponse response = new ProcessMemberResponse(theMember(created) + " was successfully added to EDS.");
        response.setMemberId(created.getExternalId());

        return response;
    }

    private ProcessMemberResponse inviteMember(final ProcessMemberRequest request) {
        // Invitations can only be issued by the System Administrator, not by
        // anyone else.
        if (member.getMemberRole() != MemberRole.ADMIN) {
            throw new IllegalActionException("Members are not permitted to invite new Members.");
        }

        final String memberName = request.getNewAccountName().trim();
        final MemberEntity found = dao.findMemberByName(memberName);
        if (found != null) {
            throw new EDSException(ReturnCode.CONSTRAINT_ERROR, "Cannot create an invitation, as the account already exists.");
        }

        final String uuid = UUID.randomUUID().toString();
        final byte[] signature = crypto.sign(keyPair.getPrivate().getKey(), crypto.stringToBytes(uuid));

        final MemberEntity entity = new MemberEntity();
        entity.setName(memberName);
        entity.setSalt(crypto.encryptWithMasterKey(uuid));
        entity.setPbeAlgorithm(settings.getPasswordAlgorithm());
        entity.setRsaAlgorithm(settings.getSignatureAlgorithm());
        entity.setPrivateKey(CredentialType.SIGNATURE.name());
        entity.setPublicKey(Base64.getEncoder().encodeToString(signature));
        entity.setMemberRole(whichRole(request));
        dao.save(entity);

        final ProcessMemberResponse response = new ProcessMemberResponse("An invitation was successfully issued for '" + memberName + "'.");
        response.setMemberId(entity.getExternalId());
        response.setSignature(signature);

        return response;
    }

    private static MemberRole whichRole(final ProcessMemberRequest request) {
        return (request.getMemberRole() != null) ? request.getMemberRole() : MemberRole.STANDARD;
    }

    private ProcessMemberResponse loginMember(final ProcessMemberRequest request) {
        // Step 1; Based on the Session Key, we're building an encrypted file,
        // which again will be used as the base for both the check sums and the
        // PBE based key to encrypt the Member's private key.
        final byte[] rawSessionKey = request.getNewCredential();
        final byte[] masterEncrypted = crypto.encryptWithMasterKey(rawSessionKey);
        // Done with the sessionKey, destroy in memory, so the Garbage Collector
        // can later clean it up, this way, it should be harder for a hacker to
        // extract it from memory.
        Arrays.fill(rawSessionKey, (byte) 0);

        // Now to the exciting part, the Salt is taken from the Member, and used
        // to generate a new PBE based Symmetric Key, which again is used to
        // encrypt the already encrypted SessionKey. This making it a bit more
        // challenging to extract the information, if there is no access to the
        // MasterKey.
        final String salt = crypto.decryptWithMasterKey(member.getSalt());
        final SecretEDSKey key = crypto.generatePasswordKey(member.getPbeAlgorithm(), masterEncrypted, salt);
        final String privateKey = Crypto.encryptAndArmorPrivateKey(key, keyPair.getPrivate().getKey());
        final String checksum = crypto.generateChecksum(masterEncrypted);

        member.setSessionChecksum(checksum);
        member.setSessionCrypto(privateKey);
        member.setSessionExpire(Utilities.newDate().plusMinutes(settings.getSessionTimeout()));
        dao.save(member);

        return new ProcessMemberResponse(theMember(member) + " has successfully logged in.");
    }

    private ProcessMemberResponse logoutMember() {
        dao.removeSession(member);
        return new ProcessMemberResponse();
    }

    private ProcessMemberResponse alterMember(final ProcessMemberRequest request) {
        if (member.getMemberRole() != MemberRole.ADMIN) {
            throw new AuthorizationException("Only Administrators may update the Role of a member.");
        }

        if (member.getExternalId().equals(request.getMemberId())) {
            throw new IllegalActionException("It is not permitted to alter own account.");
        }

        final String memberId = request.getMemberId();
        final MemberEntity entity = dao.find(MemberEntity.class, memberId);
        entity.setMemberRole(request.getMemberRole());
        dao.save(entity);

        final ProcessMemberResponse response = new ProcessMemberResponse(theMember(entity) + " has successfully been given the new role '" + request.getMemberRole() + "'.");
        response.setMemberId(memberId);

        return response;
    }

    private ProcessMemberResponse updateMember(final ProcessMemberRequest request) {
        final String newAccountName = trim(request.getNewAccountName());

        updateOwnAccountName(newAccountName);
        updateOwnCredential(request);
        updateOwnPublicKey(request);
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

            final EDSKeyPair pair = updateMemberPassword(member, credential);
            Arrays.fill(credential, (byte) 0);

            final List<TrusteeEntity> list = dao.findTrusteesByMember(member, EnumSet.allOf(TrustLevel.class));
            for (final TrusteeEntity trustee : list) {
                final KeyAlgorithm algorithm = trustee.getKey().getAlgorithm();
                final SecretEDSKey circleKey = Crypto.extractCircleKey(algorithm, keyPair.getPrivate(), trustee.getCircleKey());
                trustee.setCircleKey(Crypto.encryptAndArmorCircleKey(pair.getPublic(), circleKey));

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
     * usable, i.e. the Member can still access the Account, but it is not
     * possible to access any encrypted data, as the keys used for the
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

        dao.removeSession(member);
        updateMemberPassword(member, request.getCredential());

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
            dao.delete(member);
            response = new ProcessMemberResponse(ReturnCode.SUCCESS, theMember(member) + " has been successfully deleted.");
        }

        return response;
    }

    private ProcessMemberResponse processDeleteAsAdmin(final ProcessMemberRequest request) {
        final MemberEntity found = dao.find(MemberEntity.class, request.getMemberId());

        if (found == null) {
            throw new IdentificationException("No such Account exist.");
        }
        if (Objects.equals(member.getId(), found.getId())) {
            throw new IllegalActionException("It is not permitted to delete yourself.");
        }

        dao.delete(found);
        return new ProcessMemberResponse(ReturnCode.SUCCESS, theMember(found) + " has successfully been deleted.");
    }

    private ProcessMemberResponse processInvitation(final ProcessMemberRequest request) {
        final MemberEntity account = dao.findMemberByName(request.getAccountName());
        if (account == null) {
            throw new IdentificationException("Account does not exist.");
        }

        if (!Objects.equals(account.getPrivateKey(), CredentialType.SIGNATURE.name())) {
            throw new VerificationException("Account does not have an invitation pending.");
        }

        final String secret = crypto.decryptWithMasterKey(account.getSalt());
        // Although multiple System Administrators may exist, it will
        // make everything very cumbersome if all their keys have to be
        // checked. Hence, it is limited to the first.
        final MemberEntity admin = dao.findMemberByName(Constants.ADMIN_ACCOUNT);
        final PublicKey publicKey = crypto.dearmoringPublicKey(admin.getPublicKey());

        if (!crypto.verify(publicKey, crypto.stringToBytes(secret), request.getCredential())) {
            throw new AuthenticationException("The given signature is invalid.");
        }

        final KeyAlgorithm pbeAlgorithm = settings.getPasswordAlgorithm();
        final IVSalt salt = new IVSalt();
        final byte[] newSecret = request.getNewCredential();
        final EDSKeyPair pair = Crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final SecretEDSKey key = crypto.generatePasswordKey(pbeAlgorithm, newSecret, salt.getArmored());
        key.setSalt(salt);

        account.setSalt(crypto.encryptWithMasterKey(salt.getArmored()));
        account.setPbeAlgorithm(pbeAlgorithm);
        account.setRsaAlgorithm(pair.getAlgorithm());
        account.setMemberKey(request.getPublicKey());
        account.setPublicKey(Crypto.armoringPublicKey(pair.getPublic().getKey()));
        account.setPrivateKey(Crypto.encryptAndArmorPrivateKey(key, pair.getPrivate().getKey()));
        dao.save(account);

        final ProcessMemberResponse response = new ProcessMemberResponse("The invitation was successfully processed for '" + account.getName() + "'.");
        response.setMemberId(account.getExternalId());

        return response;
    }

    /**
     * <p>Wrapper method to ensure that the member is always presented the
     * same way. The method simply returns the Member + member name.</p>
     *
     * @param member Member Entity to read the name from
     * @return String starting with 'the Member' and then the member name quoted
     */
    private static String theMember(final MemberEntity member) {
        return "The Member '" + member.getName() + "'";
    }
}
