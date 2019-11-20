/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.MemberRole;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.exceptions.AuthenticationException;
import io.javadog.cws.core.exceptions.AuthorizationException;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.exceptions.IdentificationException;
import io.javadog.cws.core.exceptions.IllegalActionException;
import io.javadog.cws.core.exceptions.VerificationException;
import io.javadog.cws.core.jce.CWSKeyPair;
import io.javadog.cws.core.jce.IVSalt;
import io.javadog.cws.core.jce.SecretCWSKey;
import io.javadog.cws.core.model.MemberDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.MemberEntity;
import io.javadog.cws.core.model.entities.TrusteeEntity;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.EntityManager;

/**
 * <p>Business Logic implementation for the CWS ProcessMember request.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class ProcessMemberService extends Serviceable<MemberDao, ProcessMemberResponse, ProcessMemberRequest> {

    public ProcessMemberService(final Settings settings, final EntityManager entityManager) {
        super(settings, new MemberDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessMemberResponse perform(final ProcessMemberRequest request) {
        if (request != null) {
            final ProcessMemberResponse response;

            if (request.getCredentialType() == CredentialType.SIGNATURE) {
                verify(request);
                final byte[] newCredential = request.getNewCredential();
                if ((newCredential == null) || (newCredential.length == 0)) {
                    throw new VerificationException("The " + Constants.FIELD_NEW_CREDENTIAL + " is missing in Request.");
                }

                response = processInvitation(request);
            } else {
                // Pre-checks, & destruction of credentials
                verifyRequest(request, Permission.PROCESS_MEMBER);
                Arrays.fill(request.getCredential(), (byte) 0);
                response = processActions(request);
            }

            return response;
        } else {
            throw new VerificationException("Cannot Process a NULL Object.");
        }
    }

    private ProcessMemberResponse processActions(final ProcessMemberRequest request) {
        final ProcessMemberResponse response;

        switch (request.getAction()) {
            case CREATE:
                response = createMember(request);
                break;
            case INVITE:
                response = inviteMember(request);
                break;
            case LOGIN:
                response = loginMember(request);
                break;
            case LOGOUT:
                response = logoutMember();
                break;
            case ALTER:
                response = alterMember(request);
                break;
            case UPDATE:
                response = updateMember(request);
                break;
            case INVALIDATE:
                response = invalidate(request);
                break;
            case DELETE:
                response = deleteMember(request);
                break;
            default:
                // Unreachable Code by design.
                throw new IllegalActionException("Unsupported Action.");
        }

        return response;
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
            throw new CWSException(ReturnCode.IDENTIFICATION_WARNING, "An Account with the requested AccountName already exist.");
        }

        final MemberRole role = whichRole(request);
        final MemberEntity created = createNewAccount(accountName, role, request.getNewCredential());
        final ProcessMemberResponse response = new ProcessMemberResponse(theMember(created) + " was successfully added to CWS.");
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
            throw new CWSException(ReturnCode.CONSTRAINT_ERROR, "Cannot create an invitation, as the account already exists.");
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
        dao.persist(entity);

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
        // encrypt the already encrypted SessionKey. This making it a but more
        // challenging to extract the information, if there is no access to the
        // MasterKey.
        final String salt = crypto.decryptWithMasterKey(member.getSalt());
        final SecretCWSKey key = crypto.generatePasswordKey(member.getPbeAlgorithm(), masterEncrypted, salt);
        final String privateKey = crypto.armoringPrivateKey(key, keyPair.getPrivate().getKey());
        final String checksum = crypto.generateChecksum(masterEncrypted);

        member.setSessionChecksum(checksum);
        member.setSessionCrypto(privateKey);
        member.setSessionExpire(calculateSessionExpiration());
        dao.persist(member);

        // Key's no longer being used, must be destroyed.
        key.destroy();

        return new ProcessMemberResponse(theMember(member) + " has successfully logged in.");
    }

    private Date calculateSessionExpiration() {
        return Date.from(LocalDateTime
                .now()
                .plusMinutes(settings.getSessionTimeout())
                .atZone(ZoneId.systemDefault())
                .toInstant());
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
        dao.persist(entity);

        final ProcessMemberResponse response = new ProcessMemberResponse(theMember(entity) + " has successfully been given the new role '" + request.getMemberRole() + "'.");
        response.setMemberId(memberId);

        return response;
    }

    private ProcessMemberResponse updateMember(final ProcessMemberRequest request) {
        final String newAccountName = trim(request.getNewAccountName());

        updateOwnAccountName(newAccountName);
        updateOwnCredential(request);
        updateOwnPublicKey(request);
        dao.persist(member);

        final ProcessMemberResponse response = new ProcessMemberResponse(theMember(member) + " was successfully updated.");
        response.setMemberId(member.getExternalId());
        return response;
    }

    private void updateOwnAccountName(final String newAccountName) {
        if (!isEmpty(newAccountName)) {
            final MemberEntity existing = dao.findMemberByName(newAccountName);
            if (existing != null) {
                throw new CWSException(ReturnCode.CONSTRAINT_ERROR, "The new Account Name already exists.");
            }

            member.setName(newAccountName);
        }
    }

    private void updateOwnCredential(final ProcessMemberRequest request) {
        final byte[] credential = request.getNewCredential();
        if (credential != null) {
            if (request.getCredentialType() != CredentialType.PASSPHRASE) {
                throw new CWSException(ReturnCode.VERIFICATION_WARNING, "It is only permitted to update the credentials when authenticating with Passphrase.");
            }

            final CWSKeyPair pair = updateMemberPassword(member, credential);
            Arrays.fill(credential, (byte) 0);

            final List<TrusteeEntity> list = dao.findTrusteesByMember(member, EnumSet.allOf(TrustLevel.class));
            for (final TrusteeEntity trustee : list) {
                final KeyAlgorithm algorithm = trustee.getKey().getAlgorithm();
                final SecretCWSKey circleKey = crypto.extractCircleKey(algorithm, keyPair.getPrivate(), trustee.getCircleKey());
                trustee.setCircleKey(crypto.encryptAndArmorCircleKey(pair.getPublic(), circleKey));

                dao.persist(trustee);
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
        final CWSKeyPair pair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final SecretCWSKey key = crypto.generatePasswordKey(pbeAlgorithm, newSecret, salt.getArmored());
        key.setSalt(salt);

        account.setSalt(crypto.encryptWithMasterKey(salt.getArmored()));
        account.setPbeAlgorithm(pbeAlgorithm);
        account.setRsaAlgorithm(pair.getAlgorithm());
        account.setMemberKey(request.getPublicKey());
        account.setPublicKey(crypto.armoringPublicKey(pair.getPublic().getKey()));
        account.setPrivateKey(crypto.armoringPrivateKey(key, pair.getPrivate().getKey()));
        dao.persist(account);

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
