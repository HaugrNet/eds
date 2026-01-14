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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.CredentialType;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.dtos.Circle;
import net.haugr.eds.api.requests.AbstractRequest;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.CircleIdRequest;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.core.StartupBean;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.exceptions.AuthenticationException;
import net.haugr.eds.core.exceptions.AuthorizationException;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.exceptions.CryptoException;
import net.haugr.eds.core.exceptions.VerificationException;
import net.haugr.eds.core.jce.EDSKeyPair;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.jce.IVSalt;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.model.entities.DataEntity;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;

/**
 * <p>Common Business Logic, used by the Business Logic classes.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public abstract class AbstractManager<D extends CommonDao, R extends EDSResponse, A extends Authentication> {

    /** The Settings. */
    protected final Settings settings;
    /** The Crypto Library. */
    protected final Crypto crypto;
    /** The DAO Library. */
    protected final D dao;
    /** List of Trustees. */
    protected List<TrusteeEntity> trustees = new ArrayList<>(0);
    /** The Member. */
    protected MemberEntity member = null;
    /** The Member's internal KeyPair. */
    protected EDSKeyPair keyPair = null;

    /**
     * General Constructor.
     *
     * @param settings Settings Instance
     * @param dao      Manager Dao instance
     */
    protected AbstractManager(final Settings settings, final D dao) {
        this.crypto = new Crypto(settings);
        this.settings = settings;
        this.dao = dao;
    }

    /**
     * <p>The main processing method for the given Service. Takes care of the
     * Business Logic for the request and returns the response.</p>
     *
     * <p>The first step in the method will be to verity the Request Object,
     * to ensure that it has the required information to perform the request
     * and successfully complete it without any strange errors.</p>
     *
     * @param request Request Object to perform
     * @return Response Object with the result of the processing
     * @throws RuntimeException if an unknown error occurred
     */
    public abstract R perform(A request);

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
     * @param authentication Request Object to use for the checks
     * @param action         The Action for the permission check
     */
    protected final void verifyRequest(final A authentication, final Permission action) {
        // As this is the first common logic element for all service requests,
        // it is also the logical place to have a check to see if the system is
        // ready to be used. If not, then an Exception is thrown.
        throwIfSystemIsNotReady();

        // If available, let's extract the CircleId, so it can be used to
        // improve accuracy of the checks and reduce the amount of data fetched
        // from the database in preparation to perform these checks.
        final String circleId = authentication instanceof CircleIdRequest request
                ? request.getCircleId()
                : null;

        // Step 1; Verify if the given data is sufficient to complete the
        //         request. If not sufficient, no need to continue and involve
        //         the DB, so an Exception will be thrown.
        verify(authentication);

        // Step 2; Authentication. This part is a bit more tricky, since EDS
        //         supports that members can come in either with a username
        //         and password or with a session.
        if (authentication.getCredentialType() == CredentialType.SESSION) {
            // 2.a Session Authentication. The same value (Session Key) is
            //     used to both find the account and authenticate the
            //     account. Hence, a special set of information is checked
            //     as part of this process.
            //       The verification of the Session will result in an
            //     Exception, if the session has expired, cannot be found
            //     or is not valid.
            //       For valid sessions, the MasterKey has been used to
            //     encrypt the SessionKey, before it is being used to unlock
            //     the Member KeyPair. This is to prevent that a copy of the
            //     database may result in unwanted unlocking of the
            //     Account details with just the SessionKey alone.
            verifySession(authentication, circleId);
        } else {
            // 2.b Find the Member by the given credentials, if nothing is
            //     found, then no need to continue. Unless, the account not
            //     found is the Administrator Account, in which case we will
            //     add a new Account with the given Credentials.
            //       Note; if the CircleId is already given, it will be used
            //     as part of the lookup, thus limiting what is being
            //     searched and also allow the checks to end earlier.
            //     However, equally important, this check is a premature
            //     check and will *not* count in the final Business Logic!
            final String account = trim(authentication.getAccountName());

            //     Check rate limit before attempting authentication to
            //     prevent brute-force attacks.
            checkRateLimit(account);

            verifyAccount(authentication, circleId);

            //     Check if the Member is valid, i.e. if the given
            //     Credentials can correctly decrypt the Private Key for
            //     the Account. If not, then an Exception is thrown.
            checkCredentials(account, member, authentication.getCredential(), member.getPrivateKey());
        }

        // Step 3; Final check, ensure that the Member is having the correct
        //         level of Access to any Circle - which doesn't necessarily
        //         mean to the requesting Circle, as it requires deeper
        //         checking.
        //           Note; if the CircleId is already given, then it will be
        //         used to also check of the Member is Authorized. Again, this
        //         check is only a premature check and will not count against
        //         the final checks in the Business Logic.
        checkAuthorization(action, circleId);
    }

    /**
     * <p>Checks if the ready flag is set to true in the settings, if not then
     * a {@link EDSException} is thrown. The flag is updated to true via the
     * {@link StartupBean}.</p>
     */
    private void throwIfSystemIsNotReady() {
        if (!settings.isReady()) {
            throw new EDSException(ReturnCode.DATABASE_ERROR, "The Database is invalid, EDS neither can nor will work correctly until resolved.");
        }
    }

    /**
     * <p>General Verification Method, takes the given Request Object and
     * invokes the validate method on it, to ensure that it is correct.</p>
     *
     * <p>If the given Object is null, or if it contains one or more problems,
     * then an Exception is thrown, as it is not possible for the EDS to
     * complete the request with this Request Object, the thrown Exception will
     * contain all the information needed to correct the problem.</p>
     *
     * @param request Given Request Object to verify
     * @throws VerificationException if the given Object is null or invalid
     */
    protected static void verify(final AbstractRequest request) {
        throwConditionalNullException(request,
                ReturnCode.VERIFICATION_WARNING, "Cannot Process a NULL Object.");

        final Map<String, String> errors = request.validate();
        if (!errors.isEmpty()) {
            final int capacity = errors.size() * 75;
            final StringBuilder builder = new StringBuilder(capacity);

            for (final Map.Entry<String, String> error : errors.entrySet()) {
                builder.append("\nKey: ");
                builder.append(error.getKey());
                builder.append(", Error: ");
                builder.append(error.getValue());
            }

            throw new VerificationException("Request Object contained errors:" + builder);
        }
    }

    /**
     * Verifies that the given Session is valid, and if so, tries to find the
     * Member Account based on the Session information.
     *
     * @param authentication Authentication information with Session
     * @param circleId       Optional Circle ID
     */
    private void verifySession(final A authentication, final String circleId) {
        final byte[] masterEncrypted = crypto.encryptWithMasterKey(authentication.getCredential());
        final String checksum = crypto.generateChecksum(masterEncrypted);
        final MemberEntity memberEntity = dao.findMemberByChecksum(checksum);

        if (memberEntity != null) {
            if (Utilities.newDate().isBefore(memberEntity.getSessionExpire())) {
                checkCredentials(memberEntity, masterEncrypted, memberEntity.getSessionCrypto());
            } else {
                dao.removeSession(memberEntity);
                throw new AuthenticationException("The Session has expired.");
            }
        } else {
            throw new AuthenticationException("No Session could be found.");
        }

        checkMemberAccount(memberEntity, circleId);
    }

    /**
     * Verifies that the given account is value, and if so, tries to find the
     * Member Account based on the Credential information.
     *
     * @param authentication Authentication Account Name &amp; Credential
     * @param circleId       Optional Circle ID
     */
    private void verifyAccount(final A authentication, final String circleId) {
        final String account = trim(authentication.getAccountName());
        MemberEntity memberEntity = dao.findMemberByName(account);

        if (memberEntity == null) {
            if (Objects.equals(Constants.ADMIN_ACCOUNT, account)) {
                memberEntity = createNewAccount(Constants.ADMIN_ACCOUNT, MemberRole.ADMIN, authentication.getCredential());
            } else {
                throw new AuthenticationException("Could not uniquely identify an account for '" + account + "'.");
            }
        }

        checkMemberAccount(memberEntity, circleId);
    }

    /**
     * Checks if the given Member Account is an Administrator, or if the
     * optional Circle ID is present, it checks the relation between the given
     * Member Entity and the Circle ID. If no relation exist, then the dao used
     * will throw a {@code EDSException} with an Identification warning.
     *
     * @param memberEntity MemberEntity to check
     * @param circleId     Optional Circle ID to verify relation with
     */
    private void checkMemberAccount(final MemberEntity memberEntity, final String circleId) {
        if ((circleId == null) || (memberEntity.getMemberRole() == MemberRole.ADMIN)) {
            member = memberEntity;
        } else {
            member = dao.findMemberByNameAndCircleId(memberEntity.getName(), circleId);
        }
    }

    protected final MemberEntity createNewAccount(final String accountName, final MemberRole role, final byte[] credential) {
        final MemberEntity account = new MemberEntity();
        account.setName(accountName);
        account.setMemberRole(role);
        updateMemberPassword(account, credential);

        return account;
    }

    /**
     * This method will update the Member Password, and at the same time also
     * update the Asymmetric key belonging to the member. If the Asymmetric key
     * is used for anything, then it must also be updated in all places it is
     * being used. Otherwise, the action of invoking this request will result
     * in an invalidated account.
     *
     * @param member   The Member to update the Asymmetric Key &amp; Password for
     * @param password The new Password
     * @return The new Asymmetric Key
     */
    protected final EDSKeyPair updateMemberPassword(final MemberEntity member, final byte[] password) {
        final KeyAlgorithm pbeAlgorithm = settings.getPasswordAlgorithm();
        final KeyAlgorithm rsaAlgorithm = settings.getAsymmetricAlgorithm();
        final IVSalt salt = new IVSalt();
        final SecretEDSKey key = crypto.generatePasswordKey(pbeAlgorithm, password, salt.getArmored());
        key.setSalt(salt);

        final EDSKeyPair pair = Crypto.generateAsymmetricKey(rsaAlgorithm);
        final String publicKey = Crypto.armoringPublicKey(pair.getPublic().getKey());
        final String privateKey = Crypto.encryptAndArmorPrivateKey(key, pair.getPrivate().getKey());

        member.setSalt(crypto.encryptWithMasterKey(salt.getArmored()));
        member.setPbeAlgorithm(pbeAlgorithm);
        member.setRsaAlgorithm(rsaAlgorithm);
        member.setPrivateKey(privateKey);
        member.setPublicKey(publicKey);
        dao.save(member);

        return pair;
    }

    /**
     * Checks if the account is rate-limited due to too many failed
     * authentication attempts.
     *
     * @param accountName The account name to check
     * @throws EDSException if the account is temporarily blocked
     */
    private void checkRateLimit(final String accountName) {
        final int maxAttempts = settings.getLoginRetryLimit();
        final int windowMinutes = settings.getLoginRetryWindowMinutes();
        final LocalDateTime since = Utilities.newDate().minusMinutes(windowMinutes);

        final long failedAttempts = dao.countRecentFailedAttempts(accountName, since);

        if (failedAttempts >= maxAttempts) {
            throw new EDSException(ReturnCode.AUTHENTICATION_BLOCKED,
                    "Account temporarily blocked due to too many failed authentication attempts. Please try again later.");
        }
    }

    private void checkCredentials(final String accountName, final MemberEntity entity, final byte[] credential, final String armoredPrivateKey) {
        try {
            final String salt = crypto.decryptWithMasterKey(entity.getSalt());
            final SecretEDSKey key = crypto.generatePasswordKey(entity.getPbeAlgorithm(), credential, salt);
            keyPair = crypto.extractAsymmetricKey(entity.getRsaAlgorithm(), key, salt, entity.getPublicKey(), armoredPrivateKey);

            // Successful authentication - clear any previous failed attempts
            dao.clearLoginAttempts(accountName);
        } catch (CryptoException e) {
            // If an incorrect Passphrase was used to generate the PBE key, then
            // a Bad Padding Exception should've been thrown, which is converted
            // into an EDS Crypto Exception. If that is the case, the Member has
            // provided invalid credentials - with which it is not possible to
            // extract the KeyPair for the Account.

            // Record the failed attempt for rate-limiting
            dao.recordLoginAttempt(accountName, false, null);

            throw new AuthenticationException("Cannot authenticate the Account from the given Credentials.", e);
        }
    }

    /**
     * Overloaded method for session-based authentication where rate-limiting
     * is not applicable (session keys are not guessable).
     */
    private void checkCredentials(final MemberEntity entity, final byte[] credential, final String armoredPrivateKey) {
        try {
            final String salt = crypto.decryptWithMasterKey(entity.getSalt());
            final SecretEDSKey key = crypto.generatePasswordKey(entity.getPbeAlgorithm(), credential, salt);
            keyPair = crypto.extractAsymmetricKey(entity.getRsaAlgorithm(), key, salt, entity.getPublicKey(), armoredPrivateKey);
        } catch (CryptoException e) {
            throw new AuthenticationException("Cannot authenticate the Account from the given Credentials.", e);
        }
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
    private void checkAuthorization(final Permission action, final String circleId) {
        // There is a couple of requests, which is only allowed to be made by
        // the System Administrator.
        if ((action.getTrustLevel() == TrustLevel.SYSOP) && (member.getMemberRole() != MemberRole.ADMIN)) {
            throw new AuthorizationException("Cannot complete this request, as it is only allowed for the System Administrator.");
        }

        trustees = findTrustees(member, circleId, TrustLevel.getLevels(action.getTrustLevel()));

        // The System Admin is automatically permitted to perform a number of
        // Actions, without being part of a Circle. So these checks must be
        // made separately based on the actual Request.
        if ((member.getMemberRole() != MemberRole.ADMIN) && (action.getTrustLevel() != TrustLevel.ALL) && trustees.isEmpty()) {
            throw new AuthorizationException("The requesting Account is not permitted to " + action.getDescription());
        }
    }

    private SecretEDSKey extractCircleKey(final DataEntity entity) {
        final TrusteeEntity trustee = findTrustee(entity.getMetadata().getCircle().getExternalId());

        return Crypto.extractCircleKey(entity.getKey().getAlgorithm(), keyPair.getPrivate(), trustee.getCircleKey());
    }

    protected byte[] decryptData(final DataEntity entity) {
        final String armoredSalt = crypto.decryptWithMasterKey(entity.getInitialVector());
        final SecretEDSKey key = extractCircleKey(entity);
        final IVSalt salt = new IVSalt(armoredSalt);
        key.setSalt(salt);

        return Crypto.decrypt(key, entity.getData());
    }

    protected final byte[] encryptExternalKey(final SecretEDSKey circleKey, final String externalKey) {
        byte[] encryptedKey = null;

        if (externalKey != null) {
            circleKey.setSalt(new IVSalt(settings.getSalt()));
            encryptedKey = Crypto.encrypt(circleKey, crypto.stringToBytes(externalKey));
        }

        return encryptedKey;
    }

    protected final String decryptExternalKey(final TrusteeEntity trustee) {
        final byte[] encryptedKey = trustee.getCircle().getCircleKey();
        String externalKey = null;

        if (encryptedKey != null) {
            final SecretEDSKey circleKey = Crypto.extractCircleKey(trustee.getKey().getAlgorithm(), keyPair.getPrivate(), trustee.getCircleKey());
            circleKey.setSalt(new IVSalt(settings.getSalt()));
            externalKey = crypto.bytesToString(Crypto.decrypt(circleKey, encryptedKey));
        }

        return externalKey;
    }

    /**
     * Converts a Circle Entity to a Circle DTO.
     *
     * @param entity    Circle Entity
     * @param circleKey Optional Circle Key
     * @return Circle DTO
     */
    protected static Circle convert(final CircleEntity entity, final String circleKey) {
        final Circle circle = new Circle();

        circle.setCircleId(entity.getExternalId());
        circle.setCircleName(entity.getName());
        circle.setCircleKey(circleKey);
        circle.setAdded(entity.getAdded());

        return circle;
    }

    /**
     * Finds a Trustee Entity for the current Member, based on the external
     * CircleId.
     *
     * @param externalCircleId External CircleId to find Trustee Entity for
     * @return Trustee Entity
     * @throws EDSException if no Trustee Entity was found
     */
    protected final TrusteeEntity findTrustee(final String externalCircleId) {
        for (final TrusteeEntity trustee : trustees) {
            if (Objects.equals(trustee.getCircle().getExternalId(), externalCircleId)) {
                return trustee;
            }
        }

        throw new EDSException(ReturnCode.AUTHORIZATION_WARNING, "The current Account is not allowed to perform the given action.");
    }

    private List<TrusteeEntity> findTrustees(final MemberEntity member, final String circleId, final Set<TrustLevel> permissions) {
        final List<TrusteeEntity> found;

        if (circleId != null) {
            found = dao.findTrusteesByMemberAndCircle(member, circleId, permissions);
        } else {
            found = dao.findTrusteesByMember(member, permissions);
        }

        return found;
    }

    protected static String trim(final String value) {
        return (value != null) ? value.trim() : "";
    }

    protected static boolean isEmpty(final String value) {
        return (value == null) || value.isEmpty();
    }

    /**
     * <p>For null checks, which may otherwise cause Static Analysis problems,
     * this method was added, which simply checks the given Object if it is
     * null, and in that case, it invokes the method
     * {@link #throwConditionalException(boolean, ReturnCode, String)}.</p>
     *
     * @param obj        Object to check if it is null or not
     * @param returnCode The ReturnCode for the Exception
     * @param message    The message for the Exception
     * @throws EDSException if the given Object is null
     * @see #throwConditionalException(boolean, ReturnCode, String)
     */
    public static void throwConditionalNullException(final Object obj, final ReturnCode returnCode, final String message) {
        throwConditionalException(obj == null, returnCode, message);
    }

    /**
     * <p>General method to throw an Exception if the condition has been met,
     * i.e. if the given boolean value is true. The Exception thrown will be
     * the general {@link EDSException} with the given {@link ReturnCode} and
     * message.</p>
     *
     * @param condition  Boolean condition. If true, then throw an exception
     * @param returnCode The ReturnCode for the Exception
     * @param message    The message for the Exception
     * @throws EDSException if the condition is true
     */
    public static void throwConditionalException(final boolean condition, final ReturnCode returnCode, final String message) {
        if (condition) {
            throw new EDSException(returnCode, message);
        }
    }
}
