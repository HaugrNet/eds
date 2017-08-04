/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.common.CWSKey;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.entities.MemberEntity;

import javax.persistence.EntityManager;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
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
                response = processInvitation(request);
            } else {
                verifyRequest(request, Permission.PROCESS_MEMBER);

                switch (request.getAction()) {
                    case PROCESS:
                        response = processMember(request);
                        break;
                    case INVITE:
                        response = inviteMember(request);
                        break;
                    case DELETE:
                        response = deleteMember(request);
                        break;
                    default:
                        response = new ProcessMemberResponse(ReturnCode.ILLEGAL_ACTION, "Unsupported request.");
                }
            }

            return response;
        } else {
            throw new VerificationException("Cannot Process a NULL Object.");
        }
    }

    private ProcessMemberResponse processMember(final ProcessMemberRequest request) {
        final String externalId = request.getMemberId();
        final ProcessMemberResponse response;

        if (externalId != null) {
            final MemberEntity current = dao.find(MemberEntity.class, externalId);
            if (Objects.equals(current.getId(), member.getId())) {
                // Members are allowed to process themselves
                response = processSelf(request);
            } else {
                // It is only allowed for a Member to process their own Account,
                // i.e. change AccountName or set new Credentials
                throw new CWSException(ReturnCode.AUTHORIZATION_WARNING, "Requesting member is not allowed to process this Account.");
            }
        } else {
            final String accountName = request.getAccountName().trim();

            final MemberEntity found = dao.findMemberByName(accountName);
            if (found == null) {
                final MemberEntity created = createNewAccount(accountName, request.getNewCredential());
                response = new ProcessMemberResponse();
                response.setId(created.getExternalId());
            } else {
                throw new CWSException(ReturnCode.CONSTRAINT_ERROR, "An Account with the same AccountName already exist.");
            }
        }

        return response;
    }

    private ProcessMemberResponse inviteMember(final ProcessMemberRequest request) {
        final ProcessMemberResponse response;

        // Invitations can only be issued by the System Administrator, not by
        // Circle Administrators.
        if (Objects.equals(Constants.ADMIN_ACCOUNT, request.getAccount())) {
            final String memberName = request.getAccountName().trim();
            final MemberEntity existing = dao.findMemberByName(memberName);

            if (existing == null) {
                final String uuid = UUID.randomUUID().toString();
                final String signature = crypto.sign(keyPair.getPrivate(), crypto.stringToBytes(uuid));

                final MemberEntity entity = new MemberEntity();
                entity.setName(memberName);
                entity.setSalt(uuid);
                entity.setAlgorithm(settings.getSignatureAlgorithm());
                entity.setPrivateKey(CredentialType.SIGNATURE.name());
                entity.setPublicKey(signature);
                dao.persist(entity);

                response = new ProcessMemberResponse();
                response.setSignature(signature);
            } else {
                response = new ProcessMemberResponse(ReturnCode.CONSTRAINT_ERROR, "Cannot create an invitation, as as the account already exists.");
            }
        } else {
            response = new ProcessMemberResponse(ReturnCode.ILLEGAL_ACTION, "Not permitted to perform this Action.");
        }

        return response;
    }

    private ProcessMemberResponse deleteMember(final ProcessMemberRequest request) {
        final MemberEntity found = dao.find(MemberEntity.class, request.getMemberId());
        final ProcessMemberResponse response;

        if (found != null) {
            if (Objects.equals(found.getName(), Constants.ADMIN_ACCOUNT)) {
                response = new ProcessMemberResponse(ReturnCode.IDENTIFICATION_ERROR, "It is not permitted to delete the Admin Account.");
            } else {
                dao.delete(found);
                response = new ProcessMemberResponse();
            }
        } else {
            response = new ProcessMemberResponse(ReturnCode.IDENTIFICATION_ERROR, "No such Account exist.");
        }

        return response;
    }

    private ProcessMemberResponse processInvitation(final ProcessMemberRequest request) {
        final MemberEntity account = dao.findMemberByName(request.getAccount());
        final ProcessMemberResponse response;

        if (account != null) {
            if (Objects.equals(account.getPrivateKey(), CredentialType.SIGNATURE.name())) {
                final String secret = account.getSalt();
                final MemberEntity admin = dao.findMemberByName(Constants.ADMIN_ACCOUNT);
                final PublicKey publicKey = crypto.dearmoringPublicKey(admin.getPublicKey());

                if (crypto.verify(publicKey, crypto.stringToBytes(secret), request.getCredential())) {
                    final String salt = UUID.randomUUID().toString();
                    final String newSecret = UUID.randomUUID().toString();
                    final CWSKey pair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
                    final CWSKey key = crypto.generatePasswordKey(settings.getSymmetricAlgorithm(), newSecret, salt);
                    key.setSalt(salt);

                    account.setSalt(salt);
                    account.setAlgorithm(pair.getAlgorithm());
                    account.setPublicKey(crypto.armoringPublicKey(pair.getPublic()));
                    account.setPrivateKey(crypto.armoringPrivateKey(key, pair.getPrivate()));
                    dao.persist(account);

                    response = new ProcessMemberResponse();
                    response.setId(account.getExternalId());
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

    private ProcessMemberResponse processSelf(final ProcessMemberRequest request) {
        final ProcessMemberResponse response = new ProcessMemberResponse();
        response.setId(member.getExternalId());

        if (request.getAccountName() != null) {
            final String accountName = request.getAccountName().trim();
            final MemberEntity existing = dao.findMemberByName(accountName);

            if (existing == null) {
                member.setName(accountName);
            } else {
                throw new CWSException(ReturnCode.CONSTRAINT_ERROR, "The new Account Name already exists.");
            }
        }

        if ((request.getCredentialType() != null) && (request.getCredential() != null)) {
            final String salt = UUID.randomUUID().toString();
            final CWSKey key = crypto.generatePasswordKey(settings.getPasswordAlgorithm(), request.getCredential(), salt);
            final CWSKey pair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
            final byte[] encryptedPrivateKey = crypto.encrypt(key, pair.getPrivate().getEncoded());
            final String base64EncryptedPrivateKey = Base64.getEncoder().encodeToString(encryptedPrivateKey);
            final String armoredPublicKey = crypto.armoringPublicKey(pair.getPublic());
            member.setSalt(salt);
            member.setPrivateKey(base64EncryptedPrivateKey);
            member.setPublicKey(armoredPublicKey);
        }

        dao.persist(member);

        return response;
    }

    private MemberEntity createNewAccount(final String accountName, final String credential) {
        final String salt = UUID.randomUUID().toString();
        final CWSKey key = crypto.generatePasswordKey(settings.getPasswordAlgorithm(), credential, settings.getSalt());
        final CWSKey pair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final byte[] encryptedPrivateKey = crypto.encrypt(key, pair.getPrivate().getEncoded());
        final String base64EncryptedPrivateKey = Base64.getEncoder().encodeToString(encryptedPrivateKey);
        final String armoredPublicKey = crypto.armoringPublicKey(pair.getPublic());

        final MemberEntity account = new MemberEntity();
        account.setKey(pair);
        account.setName(accountName);
        account.setSalt(salt);
        account.setPrivateKey(base64EncryptedPrivateKey);
        account.setPublicKey(armoredPublicKey);
        dao.persist(account);

        return account;
    }
}
