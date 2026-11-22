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

import java.security.SecureRandom;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
import net.haugr.eds.core.jce.EntropyVerifier;
import net.haugr.eds.core.jce.MasterKey;
import net.haugr.eds.core.jce.MemberKeyPair;
import net.haugr.eds.core.enums.SanityStatus;
import net.haugr.eds.core.jce.DataFormat;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.model.entities.DataEntity;
import net.haugr.eds.core.model.entities.KeyEntity;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.model.entities.SessionEntity;
import net.haugr.eds.core.model.entities.MetadataEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;
import net.haugr.eds.core.rotation.CircleRotations;
import net.haugr.eds.core.status.StatusCollector;

/**
 * <p>Common Business Logic, used by the Business Logic classes.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public abstract class AbstractManager<D extends CommonDao, R extends EDSResponse, A extends Authentication> {

    /** Verifies the strength of new secrets, it is immutable and thread-safe. */
    private static final EntropyVerifier ENTROPY_VERIFIER = new EntropyVerifier();
    /** Random source for salts. */
    protected static final SecureRandom RANDOM = new SecureRandom();
    /** Length of the random PBKDF2 salt per Member. */
    private static final int PASSWORD_SALT_BYTES = 16;

    // Labels and AAD values, which bind each protected value to its purpose
    // (PLAN.md, Crypto). Changing any of them makes existing data unreadable.
    /** HMAC label for the Session lookup checksum. */
    protected static final String SESSION_LOOKUP_LABEL = "eds/session/lookup";
    /** HKDF salt for the Session key. */
    protected static final String SESSION_KEY_SALT = "eds/session";
    /** AAD prefix for the Circle Secret of a Trustee. */
    protected static final String TRUSTEE_AAD = "eds/trustee|";
    /** AAD prefix for the MasterKey outer layer of a Trustee's Circle Secret. */
    protected static final String TRUSTEE_OUTER_AAD = "eds/trustee-outer|";
    /** HMAC label for invitation tokens. */
    protected static final String INVITATION_LABEL = "eds/invitation";
    /** Label (HKDF info and AAD) for the external Circle key. */
    protected static final String EXTERNAL_KEY_LABEL = "eds/circle/external-key|";
    /** HKDF label of the subkey which seals a Trustee's secret (E2E.md). */
    protected static final String TRUSTEE_SECRET_LABEL = "eds/trustee/external-key|";
    /** HKDF salt for subkeys of the Circle Secret. */
    private static final String CIRCLE_SUBKEY_SALT = "eds/circle";

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
    /** The Member's keys, unlocked by the authentication. */
    protected MemberKeyPair keyPair = null;
    /** The Session of this request, if it came with one. */
    protected SessionEntity session = null;

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
            verifyAccount(authentication, circleId);

            //     Check if the Member is valid, i.e. if the given
            //     Credentials can correctly decrypt the Private Key for
            //     the Account. If not, then an Exception is thrown.
            checkCredentials(member, authentication.getCredential());
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
        if (!MasterKey.getInstance().isUnlocked()) {
            throw new EDSException(ReturnCode.CRYPTO_ERROR, "EDS is locked, the MasterKey is not loaded.");
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
        final byte[] token = authentication.getCredential();
        final String checksum = crypto.hmacWithMasterKey(SESSION_LOOKUP_LABEL, token);
        // A Member may have several Sessions (PLAN.md, "Several sessions");
        // the token finds the one of this request.
        final SessionEntity sessionEntity = dao.findSessionByChecksum(checksum);
        final MemberEntity memberEntity;

        if (sessionEntity != null) {
            memberEntity = sessionEntity.getMember();
            if (Utilities.newDate().isBefore(sessionEntity.getExpire())) {
                // The expiry of this Session is part of its key and AADs.
                memberEntity.setSessionExpire(sessionEntity.getExpire());
                // No PBKDF2 here: the Session key is derived from the
                // high-entropy token with HKDF (PLAN.md, Crypto §2a).
                try {
                    final SecretEDSKey sessionKey = sessionKey(memberEntity, token);
                    keyPair = crypto.openPrivateKeys(sessionKey, sessionKeyAad(memberEntity), sessionOuterAad(memberEntity), memberPublicKeys(memberEntity), sessionEntity.getCrypto());
                } catch (CryptoException e) {
                    throw new AuthenticationException("Cannot authenticate the Account from the given Session.", e);
                }
                session = sessionEntity;
            } else {
                dao.removeSession(sessionEntity);
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
            memberEntity = claimStone(account, authentication.getCredential());
        }

        checkMemberAccount(memberEntity, circleId);
    }

    /**
     * <p>A stone has no account to begin with, and so no name and no
     * passphrase which someone could have left as they were: the first who
     * comes to a stone without accounts becomes its System Administrator,
     * under the name and with the passphrase given. Once there is an
     * account, an unknown name is an unknown name, whatever it is.</p>
     *
     * <p>Two who come first at the same moment must not both become it, so
     * they are let in one at a time: the row of the version is locked until
     * the transaction of the first has ended.</p>
     *
     * @param accountName The name given, which is not the name of an account
     * @param credential  The passphrase given
     * @return The new System Administrator
     * @throws AuthenticationException if the stone has an account already
     */
    protected final MemberEntity claimStone(final String accountName, final byte[] credential) {
        dao.lockForTheFirstAccount();
        if (dao.countMembers() != 0) {
            throw new AuthenticationException("Could not uniquely identify an account for '" + accountName + "'.");
        }

        return createNewAccount(accountName, MemberRole.ADMIN, credential);
    }

    /**
     * The System Administrator of the stone: its first account, which has
     * the lowest Id of those with the role. Others may be given the role;
     * this one can't lose it, can't be deleted, and is the one who may give
     * the MasterKey. It is known by being the first, not by a name.
     *
     * @return The System Administrator, or null on a stone without accounts
     */
    protected final MemberEntity systemAdministrator() {
        final List<MemberEntity> administrators = dao.findMemberByRole(MemberRole.ADMIN);
        return administrators.isEmpty() ? null : administrators.getFirst();
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
        // For the status of the stone: how many accounts are active, not which.
        StatusCollector.getInstance().active(memberEntity.getId());
        if ((circleId == null) || (memberEntity.getMemberRole() == MemberRole.ADMIN)) {
            member = memberEntity;
        } else {
            member = dao.findMemberByNameAndCircleId(memberEntity.getName(), circleId);
        }
    }

    protected final MemberEntity createNewAccount(final String accountName, final MemberRole role, final byte[] credential) {
        verifyEntropy("Credential", credential, accountName);

        final MemberEntity account = new MemberEntity();
        account.setName(accountName);
        account.setMemberRole(role);
        updateMemberPassword(account, credential);

        return account;
    }

    /**
     * Verifies that a new secret, chosen by the caller, is strong enough to
     * be used, see {@link EntropyVerifier}. It must only be invoked for new
     * secrets, not for existing secrets used to unlock something, as these
     * may have been set before the verification was introduced.
     *
     * @param name    Name of the secret, used in the error message
     * @param secret  The new secret to verify
     * @param context Terms an attacker can guess, such as the account name
     * @throws VerificationException if the secret is too weak
     */
    protected static void verifyEntropy(final String name, final byte[] secret, final String... context) {
        final EntropyVerifier.Result result = ENTROPY_VERIFIER.verify(secret, context);

        if (!result.isAcceptable()) {
            // The real bar is the estimated entropy (128 bits), not the
            // minimum length, so the advice is about that: 20 random
            // characters of all classes give about 131 bits.
            throw new VerificationException("The " + name + " is too weak. Use at least 20 random characters"
                    + " (letters, digits and symbols), or a passphrase of several random words.");
        }
    }

    /**
     * This method will update the Member Password, and at the same time also
     * update the Asymmetric key belonging to the member. If the Asymmetric key
     * is used for anything, then it must also be updated in all places it is
     * being used. Otherwise, the action of invoking this request will result
     * in an invalidated account.
     *
     * <p>The private keys are double wrapped (PLAN.md, Crypto §2): inside with
     * the PBKDF2 password key, outside with the MasterKey. Any Session ends,
     * as its copy of the keys belongs to the old keys.</p>
     *
     * @param member   The Member to update the Keys &amp; Password for
     * @param password The new Password
     * @return The new Member Keys
     */
    protected final MemberKeyPair updateMemberPassword(final MemberEntity member, final byte[] password) {
        final KeyAlgorithm pbeAlgorithm = settings.getPasswordAlgorithm();
        final KeyAlgorithm keyAlgorithm = settings.getAsymmetricAlgorithm();
        if (member.getExternalId() == null) {
            // The external Id is part of the AAD, so it must be known before
            // the keys are encrypted.
            member.setExternalId(UUID.randomUUID().toString());
        }
        // A new passphrase gets the current iterations; they stay with the
        // account, and are used at each login.
        final int iterations = settings.getPasswordIterations();
        member.setPbeAlgorithm(pbeAlgorithm);
        member.setPbeIterations(iterations);
        member.setKeyAlgorithm(keyAlgorithm);

        final byte[] salt = new byte[PASSWORD_SALT_BYTES];
        RANDOM.nextBytes(salt);
        final SecretEDSKey key = crypto.generatePasswordKey(pbeAlgorithm, password, salt, iterations);
        final MemberKeyPair pair = MemberKeyPair.generate(keyAlgorithm);

        member.setSalt(crypto.sealWithMasterKey(memberSaltAad(member), salt));
        member.setPublicKey(pair.getPublic().armored());
        member.setPrivateKey(crypto.protectPrivateKeys(key, memberKeyAad(member), memberOuterAad(member), pair));
        // New keys end every Session of the Member.
        dao.removeSessions(member);
        session = null;
        dao.save(member);

        return pair;
    }

    /**
     * Opens the Member's private keys with the given passphrase.
     *
     * @param entity     The Member
     * @param credential The passphrase
     * @return The Member's keys
     * @throws CryptoException if the passphrase is wrong
     */
    protected final MemberKeyPair openMemberKeys(final MemberEntity entity, final byte[] credential) {
        final SecretEDSKey key = generateMemberKey(entity, credential);
        return crypto.openPrivateKeys(key, memberKeyAad(entity), memberOuterAad(entity), memberPublicKeys(entity), entity.getPrivateKey());
    }

    protected static MemberKeyPair.Public memberPublicKeys(final MemberEntity entity) {
        return MemberKeyPair.Public.fromArmored(entity.getKeyAlgorithm(), entity.getPublicKey());
    }

    /**
     * Derives the Session key from the Session token (PLAN.md, Crypto §2a), bound
     * to the Member and the expiry of the Session.
     *
     * @param entity The Member, with the Session expiry set
     * @param token  The Session token
     * @return Session key
     */
    protected final SecretEDSKey sessionKey(final MemberEntity entity, final byte[] token) {
        final String info = "eds/session|" + entity.getExternalId() + '|' + sessionExpiry(entity);
        return Crypto.deriveKey(token, crypto.stringToBytes(SESSION_KEY_SALT), crypto.stringToBytes(info));
    }

    private static long sessionExpiry(final MemberEntity entity) {
        return entity.getSessionExpire().toEpochSecond(ZoneOffset.UTC);
    }

    /**
     * <p>Wraps the Circle Secret for a Trustee (PLAN.md, Crypto §3), double
     * wrapped: inside with the hybrid KEM to the Member's public keys,
     * outside with the MasterKey. Neither the MasterKey alone nor the
     * Member's keys alone can recover it. Both layers are bound to the
     * Circle, the Member and the version of the secret (its key row), so a
     * wrapped secret can't be moved to another row (PLAN.md, Crypto C6).</p>
     *
     * @param recipient The Member to wrap the Circle Secret for
     * @param circle    The Circle
     * @param key       The key row of the Circle Secret, saved already
     * @param circleKey The Circle Secret
     * @return The stored, double wrapped Circle Secret
     */
    protected final String wrapCircleKey(final MemberEntity recipient, final CircleEntity circle, final KeyEntity key, final SecretEDSKey circleKey) {
        return wrapCircleKey(memberPublicKeys(recipient), recipient, circle, key, circleKey);
    }

    protected final String wrapCircleKey(final MemberKeyPair.Public recipientKeys, final MemberEntity recipient, final CircleEntity circle,
                                         final KeyEntity key, final SecretEDSKey circleKey) {
        final String binding = trusteeBinding(circle, recipient, key);
        final String inner = Crypto.wrapCircleKey(recipientKeys, crypto.stringToBytes(TRUSTEE_AAD + binding), circleKey);
        return crypto.sealWithMasterKey(TRUSTEE_OUTER_AAD + binding, crypto.stringToBytes(inner));
    }

    /**
     * Unwraps the Circle Secret of the Trustee, with the MasterKey and the
     * requesting Member's keys. If the Circle's secret is due to be rotated,
     * or is being rotated, the unwrapped secret is handed to the rotation
     * (PLAN.md, Crypto C6), which can't start without it.
     *
     * @param trustee The Trustee relation of the requesting Member
     * @return The Circle Secret
     */
    protected final SecretEDSKey unwrapCircleKey(final TrusteeEntity trustee) {
        final SecretEDSKey secret = unwrap(trustee, trustee.getKey(), trustee.getCircleKey());
        CircleRotations.offer(trustee, secret, () -> (trustee.getPreviousKey() != null)
                ? unwrap(trustee, trustee.getPreviousKey(), trustee.getPreviousCircleKey())
                : null);
        return secret;
    }

    /**
     * The Circle Secret a Data Object is encrypted with: the current one,
     * or, while the Circle's secret is rotated, the retiring one for an
     * object which isn't re-encrypted yet (PLAN.md, Crypto C6).
     *
     * @param trustee The Trustee relation of the requesting Member
     * @param keyId   The key row of the object, {@code eds_data.key_id}
     * @return The Circle Secret of the object
     * @throws CryptoException if the Trustee has no such secret
     */
    protected final SecretEDSKey unwrapCircleKey(final TrusteeEntity trustee, final long keyId) {
        if (Objects.equals(trustee.getKey().getId(), keyId)) {
            return unwrapCircleKey(trustee);
        }
        final KeyEntity previous = trustee.getPreviousKey();
        if ((previous != null) && Objects.equals(previous.getId(), keyId)) {
            // The current secret is unwrapped as well, so a rotation which
            // waits for a Trustee gets both.
            unwrapCircleKey(trustee);
            return unwrap(trustee, previous, trustee.getPreviousCircleKey());
        }

        throw new CryptoException("The Circle Secret of the object is not available.");
    }

    /**
     * Gives a new Trustee the Circle Secret, wrapped to their keys, from the
     * Trustee row of the requesting Member: the current secret, and while the
     * Circle's secret is rotated also the retiring one, so the new Trustee
     * reads the objects which aren't re-encrypted yet (PLAN.md, Crypto C6).
     *
     * @param target  The new Trustee row, with Member and Circle set
     * @param source  The requesting Member's Trustee row in the Circle
     * @param current The current Circle Secret, unwrapped from the source
     */
    protected final void grantCircleSecrets(final TrusteeEntity target, final TrusteeEntity source, final SecretEDSKey current) {
        final CircleEntity circle = source.getCircle();
        target.setKey(source.getKey());
        target.setCircleKey(wrapCircleKey(target.getMember(), circle, source.getKey(), current));
        final KeyEntity previous = source.getPreviousKey();
        if (previous != null) {
            target.setPreviousKey(previous);
            target.setPreviousCircleKey(wrapCircleKey(target.getMember(), circle, previous, unwrap(source, previous, source.getPreviousCircleKey())));
        }
    }

    /**
     * Wraps the Circle Secrets of the requesting Member's Trustee row anew,
     * to new keys of the Member: the current, and the retiring one while
     * the Circle's secret is rotated.
     *
     * @param trustee The Trustee row of the requesting Member
     * @param keys    The Member's new public keys
     */
    protected final void rewrapCircleSecrets(final TrusteeEntity trustee, final MemberKeyPair.Public keys) {
        final SecretEDSKey current = unwrap(trustee, trustee.getKey(), trustee.getCircleKey());
        final KeyEntity previous = trustee.getPreviousKey();
        final SecretEDSKey retiring = (previous != null) ? unwrap(trustee, previous, trustee.getPreviousCircleKey()) : null;
        trustee.setCircleKey(wrapCircleKey(keys, trustee.getMember(), trustee.getCircle(), trustee.getKey(), current));
        if (retiring != null) {
            trustee.setPreviousCircleKey(wrapCircleKey(keys, trustee.getMember(), trustee.getCircle(), previous, retiring));
        }
    }

    private SecretEDSKey unwrap(final TrusteeEntity trustee, final KeyEntity key, final String wrapped) {
        final String binding = trusteeBinding(trustee.getCircle(), trustee.getMember(), key);
        final String inner = crypto.bytesToString(crypto.openWithMasterKey(TRUSTEE_OUTER_AAD + binding, wrapped));
        return Crypto.unwrapCircleKey(key.getAlgorithm(), keyPair, crypto.stringToBytes(TRUSTEE_AAD + binding), inner);
    }

    /**
     * What a wrapped Circle Secret is bound to: the Circle, the Member and the
     * version of the secret.
     *
     * @param circle The Circle
     * @param member The Member
     * @param key    The key row of the Circle Secret
     * @return The binding, part of both AADs
     */
    protected static String trusteeBinding(final CircleEntity circle, final MemberEntity member, final KeyEntity key) {
        return circle.getExternalId() + '|' + member.getExternalId() + '|' + key.getId();
    }

    /**
     * Generates the PBKDF2 key for the Member, from the Member's salt, which
     * is protected by the MasterKey.
     *
     * @param entity     Member
     * @param credential Passphrase, or Session secret
     * @return Password Key
     */
    protected final SecretEDSKey generateMemberKey(final MemberEntity entity, final byte[] credential) {
        final byte[] salt = crypto.openWithMasterKey(memberSaltAad(entity), entity.getSalt());
        return crypto.generatePasswordKey(entity.getPbeAlgorithm(), credential, salt, entity.getPbeIterations());
    }

    protected static String memberSaltAad(final MemberEntity entity) {
        return "eds/member/salt|" + entity.getExternalId();
    }

    protected final byte[] memberKeyAad(final MemberEntity entity) {
        return crypto.stringToBytes("eds/member/key|" + entity.getExternalId() + '|' + entity.getKeyAlgorithm());
    }

    protected static String memberOuterAad(final MemberEntity entity) {
        return "eds/member/key-outer|" + entity.getExternalId() + '|' + entity.getKeyAlgorithm();
    }

    protected final byte[] sessionKeyAad(final MemberEntity entity) {
        return crypto.stringToBytes("eds/member/session|" + entity.getExternalId() + '|' + sessionExpiry(entity) + '|' + entity.getKeyAlgorithm());
    }

    protected static String sessionOuterAad(final MemberEntity entity) {
        return "eds/member/session-outer|" + entity.getExternalId() + '|' + sessionExpiry(entity) + '|' + entity.getKeyAlgorithm();
    }

    private void checkCredentials(final MemberEntity entity, final byte[] credential) {
        try {
            keyPair = openMemberKeys(entity, credential);
        } catch (CryptoException e) {
            // If an incorrect Passphrase was used to generate the PBE key, then
            // a Bad Padding Exception should've been thrown, which is converted
            // into an EDS Crypto Exception. If that is the case, the Member has
            // provided invalid credentials - with which it is not possible to
            // extract the KeyPair for the Account.
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
        return unwrapCircleKey(findTrustee(entity.getMetadata().getCircle().getExternalId()), entity.getKey().getId());
    }

    protected byte[] decryptData(final DataEntity entity) {
        final SecretEDSKey circleSecret = extractCircleKey(entity);
        return DataFormat.decrypt(circleSecret, dataContext(entity), entity.getData());
    }

    /**
     * The Data Object, which the stored format is bound to (PLAN.md, Crypto §4).
     *
     * @param entity Data Entity, with Metadata and Key set
     * @return Data Format Context
     */
    protected static DataFormat.Context dataContext(final DataEntity entity) {
        final MetadataEntity metadata = entity.getMetadata();
        return new DataFormat.Context(metadata.getCircle().getExternalId(), metadata.getExternalId(), entity.getKey().getId());
    }

    protected final byte[] encryptExternalKey(final SecretEDSKey circleKey, final CircleEntity circle, final String externalKey) {
        byte[] encryptedKey = null;

        if (externalKey != null) {
            final byte[] label = crypto.stringToBytes(EXTERNAL_KEY_LABEL + circle.getExternalId());
            encryptedKey = Crypto.seal(externalKeySubkey(circleKey, label), label, crypto.stringToBytes(externalKey));
        }

        return encryptedKey;
    }

    private SecretEDSKey externalKeySubkey(final SecretEDSKey circleKey, final byte[] label) {
        final byte[] secret = circleKey.getEncoded();
        final SecretEDSKey key = Crypto.deriveKey(secret, crypto.stringToBytes(CIRCLE_SUBKEY_SALT), label);
        Arrays.fill(secret, (byte) 0);

        return key;
    }

    /**
     * Seals the secret of a Member in a Circle (E2E.md): text of a client,
     * which EDS doesn't look into. It is sealed with a subkey of the Circle
     * Secret, bound to the Circle and the Member, so a copy of the database
     * doesn't show it, and neither a new passphrase nor a new MasterKey
     * touches it.
     *
     * @param circleKey The Circle Secret
     * @param circle    The Circle
     * @param owner     The Member whose secret it is
     * @param secret    The secret; null or empty for none
     * @return The sealed secret, or null for none
     */
    protected final byte[] sealTrusteeSecret(final SecretEDSKey circleKey, final CircleEntity circle, final MemberEntity owner, final String secret) {
        byte[] sealed = null;

        if ((secret != null) && !secret.isEmpty()) {
            final byte[] label = trusteeSecretLabel(circle, owner);
            sealed = Crypto.seal(externalKeySubkey(circleKey, label), label, crypto.stringToBytes(secret));
        }

        return sealed;
    }

    /**
     * Opens the secret of a Trustee, see {@link #sealTrusteeSecret}.
     *
     * @param circleKey The Circle Secret
     * @param trustee   The Trustee whose secret it is
     * @return The secret, or null without one
     */
    protected final String openTrusteeSecret(final SecretEDSKey circleKey, final TrusteeEntity trustee) {
        final byte[] sealed = trustee.getExternalKey();
        String secret = null;

        if (sealed != null) {
            final byte[] label = trusteeSecretLabel(trustee.getCircle(), trustee.getMember());
            secret = crypto.bytesToString(Crypto.open(externalKeySubkey(circleKey, label), label, sealed));
        }

        return secret;
    }

    private byte[] trusteeSecretLabel(final CircleEntity circle, final MemberEntity owner) {
        return crypto.stringToBytes(TRUSTEE_SECRET_LABEL + circle.getExternalId() + '|' + owner.getExternalId());
    }

    /**
     * Opens the external key of a Circle with its Circle Secret.
     *
     * @param circleKey The Circle Secret
     * @param circle    The Circle
     * @return The external key, or null without one
     */
    protected final String openExternalKey(final SecretEDSKey circleKey, final CircleEntity circle) {
        final byte[] encryptedKey = circle.getCircleKey();
        String externalKey = null;

        if (encryptedKey != null) {
            final byte[] label = crypto.stringToBytes(EXTERNAL_KEY_LABEL + circle.getExternalId());
            externalKey = crypto.bytesToString(Crypto.open(externalKeySubkey(circleKey, label), label, encryptedKey));
        }

        return externalKey;
    }

    protected final String decryptExternalKey(final TrusteeEntity trustee) {
        final byte[] encryptedKey = trustee.getCircle().getCircleKey();
        String externalKey = null;

        if (encryptedKey != null) {
            final SecretEDSKey circleKey = unwrapCircleKey(trustee);
            final byte[] label = crypto.stringToBytes(EXTERNAL_KEY_LABEL + trustee.getCircle().getExternalId());
            externalKey = crypto.bytesToString(Crypto.open(externalKeySubkey(circleKey, label), label, encryptedKey));
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
        circle.setAvatar(entity.getAvatar());
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

    /**
     * Encrypts small content (PLAN.md, Crypto §4) and stores it for new Metadata,
     * within the storage ceiling. The plain bytes are overwritten afterwards.
     *
     * @param dataDao  The Data DAO of the manager
     * @param trustee  Trustee of the writing Member in the Circle
     * @param metadata The Metadata, already saved
     * @param bytes    The content
     */
    protected final void encryptAndSave(final DataDao dataDao, final TrusteeEntity trustee, final MetadataEntity metadata, final byte[] bytes) {
        encryptAndSave(dataDao, trustee, unwrapCircleKey(trustee), metadata, bytes);
    }

    /**
     * As {@link #encryptAndSave(DataDao, TrusteeEntity, MetadataEntity, byte[])},
     * for a caller which has unwrapped the Circle Secret already.
     *
     * @param dataDao      The Data DAO of the manager
     * @param trustee      Trustee of the writing Member in the Circle
     * @param circleSecret The Circle Secret, unwrapped from the Trustee
     * @param metadata     The Metadata, already saved
     * @param bytes        The content
     */
    protected final void encryptAndSave(final DataDao dataDao, final TrusteeEntity trustee, final SecretEDSKey circleSecret, final MetadataEntity metadata, final byte[] bytes) {
        try {
            final DataEntity entity = new DataEntity();
            entity.setMetadata(metadata);
            entity.setKey(trustee.getKey());
            entity.setData(DataFormat.encrypt(circleSecret, dataContext(entity), bytes));
            StorageCeiling.check(settings, dataDao, -1, entity.getData().length);
            entity.setChecksum(crypto.generateChecksum(entity.getData()));
            entity.setSanityStatus(SanityStatus.OK);
            entity.setSanityChecked(Utilities.newDate());
            dataDao.save(entity);
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }

}
