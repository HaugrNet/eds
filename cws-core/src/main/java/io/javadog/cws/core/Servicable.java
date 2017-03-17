/*
 * =============================================================================
 * Copyright (c) 2010-2017, JavaDog.IO
 * -----------------------------------------------------------------------------
 * Project: ZObEL (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.common.Verifiable;
import io.javadog.cws.api.dtos.Authentication;
import io.javadog.cws.api.responses.CWSResponse;
import io.javadog.cws.common.Crypto;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.AuthorizationException;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.model.CommonDao;
import io.javadog.cws.model.entities.MemberEntity;
import io.javadog.cws.model.entities.TrusteeEntity;
import io.javadog.cws.model.jpa.CommonJpaDao;

import javax.crypto.SecretKey;
import javax.persistence.EntityManager;
import java.security.Key;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public abstract class Servicable<R extends CWSResponse, V extends Authentication> {

    protected final Settings settings;
    protected final CommonDao dao;
    protected final Crypto crypto;
    protected List<TrusteeEntity> trustees;
    protected MemberEntity member;
    protected KeyPair keyPair;

    protected Servicable(final Settings settings, final EntityManager entityManager) {
        this.dao = new CommonJpaDao(entityManager);
        this.crypto = new Crypto(settings);
        this.settings = settings;
    }

    /**
     * <p>The main processing method for the given Service. Takes care of the
     * Business Logic for the request, and returns the response.</p>
     *
     * <p>The first step in the method, will be to verity the Request Object,
     * to ensure that it has the required information to perform the request,
     * and successfully complete it without any strange errors.</p>
     *
     * @param request Request Object to perform
     * @return Response Object with the result of the processing
     * @throws RuntimeException if an unknown error occurred
     */
    public abstract R perform(V request);

    /**
     * <p>All incoming requests must be verified, so it is clear if the given
     * information (data) is sufficient to complete the request, and also to
     * ensure that the requesting party is authenticated and authorized for the
     * given action.</p>
     *
     * <p>If the data is insufficient or if the requesting party cannot be
     * properly authenticated or authorized for the request, an Exception is
     * thrown.</p>
     *
     * @param verifiable Request Object to use for the checks
     * @param action     The Action for the permission check
     */
    protected void verifyRequest(final V verifiable, final Permission action) {
        // Step 1; Verify if the given data is sufficient to complete the
        //         request. If not sufficient, no need to continue and involve
        //         the DB, so an Exception will be thrown.
        verify(verifiable);

        // Step 2; Find the Member by the given credentials, if nothing is
        //         found, then no need to continue.
        member = dao.findMemberByNameCredential(verifiable.getAccount());

        // Step 3; Check if the Member is valid, i.e. if the given Credentials
        //         can correctly decrypt the Private Key for the Account. If
        //         not, then an Exception is thrown.
        checkAccountCredentials(verifiable);

        // Step 4; Final check, ensure that the Member is having the correct
        //         level of Access to any Circle - which doesn't necessarily
        //         mean to the requesting Circle, as it requires deeper
        //         checking.
        checkAccount(action);
    }

    /**
     * <p>General Verification Method, takes the given Request Object and
     * invokes the validate method on it, to ensure that it is correct.</p>
     *
     * <p>If the given Object is null, or if it contains one or more problems,
     * then an Exception is thrown, as it is not possible for the CWS to
     * complete the request with this Request Object, the thrown Exception will
     * contain all the information needed to correct the problem.</p>
     *
     * @param verifiable Given Request Object to verify
     * @throws VerificationException if the given Object is null or invalid
     */
    private static void verify(final Verifiable verifiable) {
        if (verifiable != null) {
            final Map<String, String> errors = verifiable.validate();
            if (!errors.isEmpty()) {
                final int capacity = errors.size() * 75;
                final StringBuilder builder = new StringBuilder(capacity);

                for (final Map.Entry<String, String> error : errors.entrySet()) {
                    builder.append("Key: ");
                    builder.append(error.getKey());
                    builder.append("Error: ");
                    builder.append(error.getValue());
                    builder.append('\n');
                }

                throw new VerificationException("Request Object contained errors: " + builder);
            }
        } else {
            throw new VerificationException("Cannot Process a NULL Object.");
        }
    }

    private void checkAccountCredentials(final V verifiable) {
        final Key key = extractKeyFromCredentials(verifiable, member.getSalt());
        final String toCheck = UUID.randomUUID().toString();
        keyPair = crypto.extractAsymmetricKey(key, member.getSalt(), member.getPublicKey(), member.getPrivateKey());

        final byte[] toEncrypt = toCheck.getBytes(crypto.getCharSet());
        final byte[] encrypted = crypto.encrypt(keyPair.getPublic(), toEncrypt);
        final byte[] decrypted = crypto.decrypt(keyPair.getPrivate(), encrypted);
        final String result = new String(decrypted, crypto.getCharSet());

        if (!Objects.equals(result, toCheck)) {
            throw new AuthorizationException("Cannot authenticate the Member from the given Credentials.");
        }
    }

    protected Key extractKeyFromCredentials(final V verifiable, final String salt) {
        final SecretKey key;

        if (verifiable.getCredentialType() == CredentialType.KEY) {
            key = crypto.convertCredentialToKey(verifiable.getCredential());
        } else {
            key = crypto.convertPasswordToKey(verifiable.getCredential(), salt);
        }

        return key;
    }

    private void checkAccount(final Permission action) {
        // The System Admin is automatically permitted to perform a number of
        // Actions, without being part of a Circle. So these checks must be
        // made separately based on the actual Request.
        if (!Objects.equals(Constants.ADMIN_ACCOUNT, member.getName())) {
            trustees = dao.findTrustByMember(member);
            boolean trusted = false;
            for (final TrusteeEntity trust : trustees) {
                if (TrustLevel.isAllowed(trust.getTrustLevel(), action.getTrustLevel())) {
                    trusted = true;
                }
            }
            if (!trusted) {
                throw new AuthorizationException("The requesting Account is not permitted to " + action.getDescription());
            }
        }
    }
}
