/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.enums.MemberRole;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.exceptions.VerificationException;
import io.javadog.cws.core.jce.CWSKeyPair;
import io.javadog.cws.core.jce.IVSalt;
import io.javadog.cws.core.jce.SecretCWSKey;
import io.javadog.cws.core.model.MemberDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.MemberEntity;
import io.javadog.cws.core.model.entities.TrusteeEntity;

import javax.persistence.EntityManager;
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

/**
 * <p>Business Logic implementation for the CWS ProcessMember request.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
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
                final byte[] newCredential = request.getNewCredential();
                if ((newCredential != null) && (newCredential.length > 0)) {
                    response = processInvitation(request);
                } else {
                    response = new ProcessMemberResponse(ReturnCode.VERIFICATION_WARNING, "The " + Constants.FIELD_NEW_CREDENTIAL + " is missing in Request.");
                }
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
            case UPDATE:
                response = processSelf(request);
                break;
            case INVALIDATE:
                response = invalidate(request);
                break;
            case DELETE:
                response = deleteMember(request);
                break;
            default:
                // Unreachable Code by design.
                throw new CWSException(ReturnCode.ILLEGAL_ACTION, "Unsupported Action.");
        }

        return response;
    }

    private ProcessMemberResponse createMember(final ProcessMemberRequest request) {
        final ProcessMemberResponse response;

        // Creating new Members, can only be performed by the System
        // Administrator, not by anyone else.
        if (member.getMemberRole() == MemberRole.ADMIN) {
            final String accountName = request.getNewAccountName().trim();

            final MemberEntity found = dao.findMemberByName(accountName);
            if (found == null) {
                final MemberEntity created = createNewAccount(accountName, MemberRole.STANDARD, request.getNewCredential());
                response = new ProcessMemberResponse();
                response.setMemberId(created.getExternalId());
            } else {
                throw new CWSException(ReturnCode.IDENTIFICATION_WARNING, "An Account with the same AccountName already exist.");
            }
        } else {
            response = new ProcessMemberResponse(ReturnCode.AUTHORIZATION_WARNING, "Members are not permitted to create new Accounts.");
        }

        return response;
    }

    private ProcessMemberResponse inviteMember(final ProcessMemberRequest request) {
        final ProcessMemberResponse response;

        // Invitations can only be issued by the System Administrator, not by
        // anyone else.
        if (member.getMemberRole() == MemberRole.ADMIN) {
            final String memberName = request.getNewAccountName().trim();
            final MemberEntity existing = dao.findMemberByName(memberName);

            if (existing == null) {
                final String uuid = UUID.randomUUID().toString();
                final byte[] signature = crypto.sign(keyPair.getPrivate().getKey(), crypto.stringToBytes(uuid));

                final MemberEntity entity = new MemberEntity();
                entity.setName(memberName);
                entity.setSalt(crypto.encryptWithMasterKey(uuid));
                entity.setPbeAlgorithm(settings.getPasswordAlgorithm());
                entity.setRsaAlgorithm(settings.getSignatureAlgorithm());
                entity.setPrivateKey(CredentialType.SIGNATURE.name());
                entity.setPublicKey(Base64.getEncoder().encodeToString(signature));
                entity.setMemberRole(MemberRole.STANDARD);
                dao.persist(entity);

                response = new ProcessMemberResponse();
                response.setSignature(signature);
            } else {
                response = new ProcessMemberResponse(ReturnCode.CONSTRAINT_ERROR, "Cannot create an invitation, as as the account already exists.");
            }
        } else {
            response = new ProcessMemberResponse(ReturnCode.ILLEGAL_ACTION, "Members are not permitted to invite new Members.");
        }

        return response;
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

        return new ProcessMemberResponse();
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

    private ProcessMemberResponse processSelf(final ProcessMemberRequest request) {
        final ProcessMemberResponse response = new ProcessMemberResponse();
        final String newAccountName = trim(request.getNewAccountName());
        response.setMemberId(member.getExternalId());

        if (!isEmpty(newAccountName)) {
            if (member.getMemberRole() == MemberRole.ADMIN) {
                throw new CWSException(ReturnCode.ILLEGAL_ACTION, "It is not permitted for the System Administrator to change the Account name.");
            } else {
                final MemberEntity existing = dao.findMemberByName(newAccountName);

                if (existing == null) {
                    member.setName(newAccountName);
                } else {
                    throw new CWSException(ReturnCode.CONSTRAINT_ERROR, "The new Account Name already exists.");
                }
            }
        }

        final byte[] credential = request.getNewCredential();
        if (credential != null) {
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

        if (request.getPublicKey() != null) {
            member.setMemberKey(request.getPublicKey());
        }

        dao.persist(member);

        return response;
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
        final ProcessMemberResponse response;

        // Invitations can only be issued by the System Administrator, not by
        // anyone else.
        if (member.getMemberRole() == MemberRole.ADMIN) {
            response = new ProcessMemberResponse(ReturnCode.ILLEGAL_ACTION, "The System Administrator Account may not be invalidated.");
        } else {
            dao.removeSession(member);
            updateMemberPassword(member, request.getCredential());

            response = new ProcessMemberResponse();
            response.setReturnMessage("Account has been Invalidated.");
        }

        return response;
    }

    private ProcessMemberResponse deleteMember(final ProcessMemberRequest request) {
        final ProcessMemberResponse response;

        if (request.getMemberId() != null) {
            if (member.getMemberRole() == MemberRole.ADMIN) {
                response = processDeleteAsAdmin(request);
            } else {
                response = new ProcessMemberResponse(ReturnCode.ILLEGAL_ACTION, "Members are not permitted to delete Accounts.");
            }
        } else {
            // Deleting self
            dao.delete(member);
            response = new ProcessMemberResponse(ReturnCode.SUCCESS, "The Member '" + member.getName() + "' has been successfully deleted.");
        }

        return response;
    }

    private ProcessMemberResponse processDeleteAsAdmin(final ProcessMemberRequest request) {
        final MemberEntity found = dao.find(MemberEntity.class, request.getMemberId());
        final ProcessMemberResponse response;

        if (found != null) {
            if (found.getMemberRole() == MemberRole.ADMIN) {
                response = new ProcessMemberResponse(ReturnCode.IDENTIFICATION_ERROR, "It is not permitted to delete the Admin Account.");
            } else {
                dao.delete(found);
                response = new ProcessMemberResponse(ReturnCode.SUCCESS, "The Member '" + found.getName() + "' has successfully been deleted.");
            }
        } else {
            response = new ProcessMemberResponse(ReturnCode.IDENTIFICATION_ERROR, "No such Account exist.");
        }

        return response;
    }

    private ProcessMemberResponse processInvitation(final ProcessMemberRequest request) {
        final MemberEntity account = dao.findMemberByName(request.getAccountName());
        final ProcessMemberResponse response;

        if (account != null) {
            if (Objects.equals(account.getPrivateKey(), CredentialType.SIGNATURE.name())) {
                final String secret = crypto.decryptWithMasterKey(account.getSalt());
                // Although multiple System Administrators may exist, it will
                // make everything very cumbersome if all their keys have to be
                // checked. Hence, it is limited to the first.
                final MemberEntity admin = dao.findMemberByName(Constants.ADMIN_ACCOUNT);
                final PublicKey publicKey = crypto.dearmoringPublicKey(admin.getPublicKey());

                if (crypto.verify(publicKey, crypto.stringToBytes(secret), request.getCredential())) {
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

                    response = new ProcessMemberResponse();
                    response.setMemberId(account.getExternalId());
                } else {
                    response = new ProcessMemberResponse(ReturnCode.AUTHENTICATION_WARNING, "The given signature is invalid.");
                }
            } else {
                response = new ProcessMemberResponse(ReturnCode.VERIFICATION_WARNING, "Account does not have an invitation pending.");
            }
        } else {
            response = new ProcessMemberResponse(ReturnCode.IDENTIFICATION_WARNING, "Account does not exist.");
        }

        return response;
    }
}
