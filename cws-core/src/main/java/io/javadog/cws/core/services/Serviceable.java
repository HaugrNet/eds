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
import io.javadog.cws.api.common.Utilities;
import io.javadog.cws.api.dtos.Circle;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.requests.CircleIdRequest;
import io.javadog.cws.api.requests.Verifiable;
import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.enums.MemberRole;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.exceptions.AuthenticationException;
import io.javadog.cws.core.exceptions.AuthorizationException;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.exceptions.CryptoException;
import io.javadog.cws.core.exceptions.VerificationException;
import io.javadog.cws.core.jce.CWSKeyPair;
import io.javadog.cws.core.jce.Crypto;
import io.javadog.cws.core.jce.IVSalt;
import io.javadog.cws.core.jce.SecretCWSKey;
import io.javadog.cws.core.model.CommonDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.CircleEntity;
import io.javadog.cws.core.model.entities.DataEntity;
import io.javadog.cws.core.model.entities.MemberEntity;
import io.javadog.cws.core.model.entities.TrusteeEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>Common Business Logic, used by the Business Logic classes.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public abstract class Serviceable<D extends CommonDao, R extends CwsResponse, A extends Authentication> {

    protected final Settings settings;
    protected final Crypto crypto;
    protected final D dao;

    protected List<TrusteeEntity> trustees = new ArrayList<>(0);
    protected MemberEntity member = null;
    protected CWSKeyPair keyPair = null;

    protected Serviceable(final Settings settings, final D dao) {
        this.crypto = new Crypto(settings);
        this.settings = settings;
        this.dao = dao;
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
    public abstract R perform(A request);

    /**
     * <p>To ensure that sensitive data (keys) have as short a lifespan in the
     * memory as possible, they must be destroyed, which this method will
     * handle.</p>
     *
     * @throws CryptoException if a problem occurred with destroying keys
     */
    public void destroy() {
        if (keyPair != null) {
            keyPair.getPrivate().destroy();
        }
    }

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
        if (settings.isReady()) {
            // If available, let's extract the CircleId so it can be used to improve
            // accuracy of the checks and reduce the amount of data fetched from the
            // database in preparation to perform these checks.
            String circleId = null;
            if (authentication instanceof CircleIdRequest) {
                circleId = ((CircleIdRequest) authentication).getCircleId();
            }

            // Step 1; Verify if the given data is sufficient to complete the
            //         request. If not sufficient, no need to continue and involve
            //         the DB, so an Exception will be thrown.
            verify(authentication);

            // Step 2; Authentication. This part is a bit more tricky, since CWS
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
                //     database may result in someone being able to unlock the
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
                checkAccount(authentication, circleId);

                //     Check if the Member is valid, i.e. if the given
                //     Credentials can correctly decrypt the Private Key for
                //     the Account. If not, then an Exception is thrown.
                checkCredentials(member, authentication.getCredential(), member.getPrivateKey());
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
        } else {
            throw new CWSException(ReturnCode.DATABASE_ERROR, "The Database is invalid, CWS neither can nor will work correctly until resolved.");
        }
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
                    builder.append("\nKey: ");
                    builder.append(error.getKey());
                    builder.append(", Error: ");
                    builder.append(error.getValue());
                }

                throw new VerificationException("Request Object contained errors:" + builder);
            }
        } else {
            throw new VerificationException("Cannot Process a NULL Object.");
        }
    }

    private void verifySession(final A authentication, final String circleId) {
        final byte[] masterEncrypted = crypto.encryptWithMasterKey(authentication.getCredential());
        final String checksum = crypto.generateChecksum(masterEncrypted);
        final MemberEntity memberEntity = dao.findMemberByChecksum(checksum);

        if (memberEntity != null) {
            if (Utilities.newDate().before(memberEntity.getSessionExpire())) {
                checkCredentials(memberEntity, masterEncrypted, memberEntity.getSessionCrypto());
            } else {
                dao.removeSession(memberEntity);
                throw new AuthenticationException("The Session has expired.");
            }
        } else {
            throw new AuthenticationException("No Session could be found.");
        }

        // If the CircleId is present, find the Member Account, which matches it
        // or throw an Exception if no match was found. If the CircleId is not
        // present, use the found Member Entity.
        if (circleId == null) {
            member = memberEntity;
        } else {
            member = dao.findMemberByNameAndCircleId(memberEntity.getName(), circleId);
        }
    }

    private void checkAccount(final A authentication, final String circleId) {
        // If the External Circle Id is given and the member is not the
        // Administrator (who cannot be part of a Circle), we will use
        // the CircleId in the checks.
        final String account = trim(authentication.getAccountName());
        if ((circleId != null) && !Objects.equals(account, Constants.ADMIN_ACCOUNT)) {
            member = dao.findMemberByNameAndCircleId(account, circleId);
        } else {
            member = dao.findMemberByName(account);

            if (member == null) {
                if (Objects.equals(Constants.ADMIN_ACCOUNT, account)) {
                    member = createNewAccount(Constants.ADMIN_ACCOUNT, MemberRole.ADMIN, authentication.getCredential());
                } else {
                    throw new AuthenticationException("Could not uniquely identify an account for '" + account + "'.");
                }
            }
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
     * @param member   The Member to update the Asymmetric Key & Password for
     * @param password The new Password
     * @return The new Asymmetric Key
     */
    protected final CWSKeyPair updateMemberPassword(final MemberEntity member, final byte[] password) {
        final KeyAlgorithm pbeAlgorithm = settings.getPasswordAlgorithm();
        final KeyAlgorithm rsaAlgorithm = settings.getAsymmetricAlgorithm();
        final IVSalt salt = new IVSalt();
        final SecretCWSKey key = crypto.generatePasswordKey(pbeAlgorithm, password, salt.getArmored());
        key.setSalt(salt);

        final CWSKeyPair pair = crypto.generateAsymmetricKey(rsaAlgorithm);
        final String publicKey = crypto.armoringPublicKey(pair.getPublic().getKey());
        final String privateKey = crypto.armoringPrivateKey(key, pair.getPrivate().getKey());

        member.setSalt(crypto.encryptWithMasterKey(salt.getArmored()));
        member.setPbeAlgorithm(pbeAlgorithm);
        member.setRsaAlgorithm(rsaAlgorithm);
        member.setPrivateKey(privateKey);
        member.setPublicKey(publicKey);
        dao.persist(member);

        return pair;
    }

    private void checkCredentials(final MemberEntity entity, final byte[] credential, final String armoredPrivateKey) {
        try {
            final String salt = crypto.decryptWithMasterKey(entity.getSalt());
            final SecretCWSKey key = crypto.generatePasswordKey(entity.getPbeAlgorithm(), credential, salt);
            keyPair = crypto.extractAsymmetricKey(entity.getRsaAlgorithm(), key, salt, entity.getPublicKey(), armoredPrivateKey);

            // To ensure that the PBE key is no longer usable, we're destroying
            // it now.
            key.destroy();
        } catch (CryptoException e) {
            // If an incorrect Passphrase was used to generate the PBE key, then
            // a Bad Padding Exception should've been thrown, which is converted
            // into a CWS Crypto Exception. If that is the case, the Member has
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

        // The System Admin is automatically permitted to perform a number of
        // Actions, without being part of a Circle. So these checks must be
        // made separately based on the actual Request.
        if (member.getMemberRole() != MemberRole.ADMIN) {
            trustees = findTrustees(member, circleId, TrustLevel.getLevels(action.getTrustLevel()));

            if ((action.getTrustLevel() != TrustLevel.ALL) && trustees.isEmpty()) {
                throw new AuthorizationException("The requesting Account is not permitted to " + action.getDescription());
            }
        }
    }

    protected final SecretCWSKey extractCircleKey(final DataEntity entity) {
        final TrusteeEntity trustee = findTrustee(entity.getMetadata().getCircle().getExternalId());

        return crypto.extractCircleKey(entity.getKey().getAlgorithm(), keyPair.getPrivate(), trustee.getCircleKey());
    }

    protected byte[] decryptData(final DataEntity entity) {
        final String armoredSalt = crypto.decryptWithMasterKey(entity.getInitialVector());
        final SecretCWSKey key = extractCircleKey(entity);
        final IVSalt salt = new IVSalt(armoredSalt);
        key.setSalt(salt);

        return crypto.decrypt(key, entity.getData());
    }

    protected final byte[] encryptExternalKey(final SecretCWSKey circleKey, final String externalKey) {
        byte[] encryptedKey = null;

        if (externalKey != null) {
            circleKey.setSalt(new IVSalt(settings.getSalt()));
            encryptedKey = crypto.encrypt(circleKey, crypto.stringToBytes(externalKey));
        }

        return encryptedKey;
    }

    protected final String decryptExternalKey(final TrusteeEntity trustee) {
        final byte[] encryptedKey = trustee.getCircle().getCircleKey();
        String externalKey = null;

        if (encryptedKey != null) {
            final SecretCWSKey circleKey = crypto.extractCircleKey(trustee.getKey().getAlgorithm(), keyPair.getPrivate(), trustee.getCircleKey());
            circleKey.setSalt(new IVSalt(settings.getSalt()));
            externalKey = crypto.bytesToString(crypto.decrypt(circleKey, encryptedKey));
        }

        return externalKey;
    }

    protected static Circle convert(final CircleEntity entity, final String circleKey) {
        final Circle circle = new Circle();

        circle.setCircleId(entity.getExternalId());
        circle.setCircleName(entity.getName());
        circle.setCircleKey(circleKey);
        circle.setAdded(entity.getAdded());

        return circle;
    }

    protected final TrusteeEntity findTrustee(final String externalCircleId) {
        TrusteeEntity found = null;

        for (final TrusteeEntity trustee : trustees) {
            if (Objects.equals(trustee.getCircle().getExternalId(), externalCircleId)) {
                found = trustee;
            }
        }

        if (found == null) {
            throw new CWSException(ReturnCode.AUTHORIZATION_WARNING, "The current Account is not allowed to perform the given action.");
        }

        return found;
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
}
