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
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.exceptions.VerificationException;
import io.javadog.cws.core.jce.CWSKeyPair;
import io.javadog.cws.core.jce.IVSalt;
import io.javadog.cws.core.jce.SecretCWSKey;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.MemberEntity;
import io.javadog.cws.core.model.entities.TrusteeEntity;

import javax.persistence.EntityManager;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
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
public final class ProcessMemberService extends Serviceable<ProcessMemberResponse, ProcessMemberRequest> {

    public ProcessMemberService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
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

                switch (request.getAction()) {
                    case CREATE:
                        response = createMember(request);
                        break;
                    case INVITE:
                        response = inviteMember(request);
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
            }

            return response;
        } else {
            throw new VerificationException("Cannot Process a NULL Object.");
        }
    }

    private ProcessMemberResponse createMember(final ProcessMemberRequest request) {
        final ProcessMemberResponse response;

        // Creating new Members, can only be performed by the System
        // Administrator, not by anyone else.
        if (Objects.equals(Constants.ADMIN_ACCOUNT, request.getAccountName())) {
            final String accountName = request.getNewAccountName().trim();

            final MemberEntity found = dao.findMemberByName(accountName);
            if (found == null) {
                final MemberEntity created = createNewAccount(accountName, request.getNewCredential());
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
        if (Objects.equals(Constants.ADMIN_ACCOUNT, request.getAccountName())) {
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

    private ProcessMemberResponse processSelf(final ProcessMemberRequest request) {
        final ProcessMemberResponse response = new ProcessMemberResponse();
        final String newAccountName = trim(request.getNewAccountName());
        response.setMemberId(member.getExternalId());

        if (!isEmpty(newAccountName)) {
            if (!Objects.equals(Constants.ADMIN_ACCOUNT, member.getName())) {
                final MemberEntity existing = dao.findMemberByName(newAccountName);

                if (existing == null) {
                    member.setName(newAccountName);
                } else {
                    throw new CWSException(ReturnCode.CONSTRAINT_ERROR, "The new Account Name already exists.");
                }
            } else {
                throw new CWSException(ReturnCode.ILLEGAL_ACTION, "It is not permitted for the System Administrator to change the Account name.");
            }
        }

        if (request.getNewCredential() != null) {
            final CWSKeyPair pair = updateMemberPassword(member, request.getNewCredential());

            final List<TrusteeEntity> list = dao.findTrustByMember(member, EnumSet.allOf(TrustLevel.class));
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
        if (!Objects.equals(Constants.ADMIN_ACCOUNT, request.getAccountName())) {
            updateMemberPassword(member, request.getCredential());

            response = new ProcessMemberResponse();
            response.setReturnMessage("Account has been Invalidated.");
        } else {
            response = new ProcessMemberResponse(ReturnCode.ILLEGAL_ACTION, "The System Administrator Account may not be invalidated.");
        }

        return response;
    }

    private ProcessMemberResponse deleteMember(final ProcessMemberRequest request) {
        final ProcessMemberResponse response;

        if (request.getMemberId() != null) {
            if (Objects.equals(Constants.ADMIN_ACCOUNT, member.getName())) {
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
            if (Objects.equals(found.getName(), Constants.ADMIN_ACCOUNT)) {
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
