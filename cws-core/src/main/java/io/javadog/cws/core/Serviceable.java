/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core;

import static io.javadog.cws.api.common.Constants.ADMIN_ACCOUNT;
import static io.javadog.cws.api.common.ReturnCode.IDENTIFICATION_WARNING;

import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.common.Verifiable;
import io.javadog.cws.api.dtos.Authentication;
import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.common.Crypto;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.AuthenticationException;
import io.javadog.cws.common.exceptions.AuthorizationException;
import io.javadog.cws.common.exceptions.ModelException;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.model.CommonDao;
import io.javadog.cws.model.entities.MemberEntity;
import io.javadog.cws.model.entities.TrusteeEntity;
import io.javadog.cws.model.jpa.CommonJpaDao;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.persistence.EntityManager;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.KeyPair;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public abstract class Serviceable<R extends CwsResponse, V extends Authentication> {

    protected final Settings settings;
    protected final CommonDao dao;
    protected final Crypto crypto;

    protected List<TrusteeEntity> trustees = null;
    protected MemberEntity member = null;
    protected KeyPair keyPair = null;

    protected Serviceable(final Settings settings, final EntityManager entityManager) {
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
    protected void verifyRequest(final V verifiable, final Permission action, final String... externalCircleId) {
        // Step 1; Verify if the given data is sufficient to complete the
        //         request. If not sufficient, no need to continue and involve
        //         the DB, so an Exception will be thrown.
        verify(verifiable);

        // Step 2; Find the Member by the given credentials, if nothing is
        //         found, then no need to continue. Unless, the account not
        //         found is the Administrator Account, in which case we will
        //         add a new Account with the given Credentials.
        //           Note; if the CircleId is already given, it will be used as
        //         part of the lookup, thus limiting what is being searched and
        //         also allow the checks to end earlier. However, equally
        //         important, this check is a premature check and will *not*
        //         count in the final Business Logic!
        checkAccount(verifiable, externalCircleId);

        // Step 3; Check if the Member is valid, i.e. if the given Credentials
        //         can correctly decrypt the Private Key for the Account. If
        //         not, then an Exception is thrown.
        checkCredentials(verifiable);

        // Step 4; Final check, ensure that the Member is having the correct
        //         level of Access to any Circle - which doesn't necessarily
        //         mean to the requesting Circle, as it requires deeper
        //         checking.
        //           Note; if the CircleId is already given, then it will be
        //         used to also check of the Member is Authorized. Again, this
        //         check is only a premature check and will not count against
        //         the final checks in the Business Logic.
        checkAuthorization(action, externalCircleId);
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

    private void checkAccount(final V verifiable, final String... circleId) {
        try {
            // If the External Circle Id is given and the member is not the
            // Administrator (who cannot be part of a Circle), we will use
            // the CircleId in the checks.
            if ((circleId != null) && (circleId.length == 1) && !Objects.equals(verifiable.getAccount(), ADMIN_ACCOUNT)) {
                member = dao.findMemberByNameAndCircleId(verifiable.getAccount(), circleId[0]);
            } else {
                member = dao.findMemberByName(verifiable.getAccount());
            }
        } catch (ModelException e) {
            if ((e.getReturnCode() == IDENTIFICATION_WARNING) && Objects.equals(ADMIN_ACCOUNT, verifiable.getAccount())) {
                member = createNewAdminAccount(verifiable);
            } else {
                throw e;
            }
        }
    }

    private MemberEntity createNewAdminAccount(final V verifiable) {
        final String salt = UUID.randomUUID().toString();
        final Key key = extractKeyFromCredentials(verifiable, salt);

        final KeyPair pair = crypto.generateAsymmetricKey();
        final IvParameterSpec iv = crypto.generateInitialVector(salt);
        final byte[] encryptedPrivateKey = crypto.encrypt(key, iv, pair.getPrivate().getEncoded());
        final String base64EncryptedPrivateKey = Base64.getEncoder().encodeToString(encryptedPrivateKey);
        final String armoredPublicKey = Crypto.armorKey(pair.getPublic());

        final MemberEntity account = new MemberEntity();
        account.setName(ADMIN_ACCOUNT);
        account.setSalt(salt);
        account.setPrivateKey(base64EncryptedPrivateKey);
        account.setPublicKey(armoredPublicKey);
        dao.persist(account);

        return account;
    }

    private void checkCredentials(final V verifiable) {
        final Key key = extractKeyFromCredentials(verifiable, member.getSalt());
        final String toCheck = UUID.randomUUID().toString();
        final Charset charset = settings.getCharset();
        keyPair = crypto.extractAsymmetricKey(key, member.getSalt(), member.getPublicKey(), member.getPrivateKey());

        final byte[] toEncrypt = toCheck.getBytes(charset);
        final byte[] encrypted = crypto.encrypt(keyPair.getPublic(), toEncrypt);
        final byte[] decrypted = crypto.decrypt(keyPair.getPrivate(), encrypted);
        final String result = new String(decrypted, charset);

        if (!Objects.equals(result, toCheck)) {
            throw new AuthenticationException("Cannot authenticate the Account '" + verifiable.getAccount() + "' from the given Credentials.");
        }
    }

    private Key extractKeyFromCredentials(final V verifiable, final String salt) {
        final SecretKey key;

        if (verifiable.getCredentialType() == CredentialType.KEY) {
            key = crypto.convertCredentialToKey(verifiable.getCredential());
        } else {
            key = crypto.convertPasswordToKey(verifiable.getCredential(), salt);
        }

        return key;
    }

    /**
     * The checks here will verify if a Member is allowed to perform a given
     * action. The optional CircleId has already been part of the Authentication
     * of the Member, and if it is present, it means that the Member has been
     * linked together with a specific Circle, so we will use it as part of the
     * database lookup.
     *
     * @param action   Action that is to be performed
     * @param circleId Optional External CircleId
     */
    private void checkAuthorization(final Permission action, final String... circleId) {
        // There is a couple of requests, which is only allowed to be made by
        // the System Administrator.
        if ((action.getTrustLevel() == TrustLevel.SYSOP) && !Objects.equals(ADMIN_ACCOUNT, member.getName())) {
            throw new AuthorizationException("Cannot complete this request, as it is only allowed for the System Administrator.");
        }

        // The System Admin is automatically permitted to perform a number of
        // Actions, without being part of a Circle. So these checks must be
        // made separately based on the actual Request.
        if (!Objects.equals(ADMIN_ACCOUNT, member.getName())) {
            findTrustees(member, circleId);
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

    private void findTrustees(final MemberEntity member, final String... circleId) {
        if ((circleId != null) && (circleId.length == 1)) {
            trustees = dao.findTrustByMemberAndCircle(member, circleId[0]);
        } else {
            trustees = dao.findTrustByMember(member);
        }
    }
}
