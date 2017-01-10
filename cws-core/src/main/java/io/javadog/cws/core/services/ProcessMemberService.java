package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.dtos.Authentication;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.common.Crypto;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.AuthorizationException;
import io.javadog.cws.common.exceptions.CryptoException;
import io.javadog.cws.common.exceptions.ModelException;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.model.CommonDao;
import io.javadog.cws.model.entities.MemberEntity;
import io.javadog.cws.model.entities.TrusteeEntity;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessMemberService extends Servicable<ProcessMemberResponse, ProcessMemberRequest> {

    private final Settings settings;
    private final CommonDao dao;
    private final Crypto crypto;

    public ProcessMemberService(final Settings settings, final CommonDao dao) {
        this.settings = settings;
        this.dao = dao;

        this.crypto = new Crypto(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessMemberResponse process(final ProcessMemberRequest request) {
        verify(request);

        final MemberEntity member = findMemberAccount(request);
        checkAccountCredentials(member);

        return new ProcessMemberResponse();
    }

    private static KeyPair convertKey(final MemberEntity member) {
        try {
            final byte[] rawPublicKey = Crypto.base64Decode(member.getPublicKey());
            final byte[] rawPrivateKey = Crypto.base64Decode(member.getPrivateKey());
            final String algorithm = member.getAlgorithm();

            final X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(rawPublicKey);
            final X509EncodedKeySpec privateKeySpec = new X509EncodedKeySpec(rawPrivateKey);

            final KeyFactory factory = KeyFactory.getInstance(algorithm);
            final PublicKey publicKey = factory.generatePublic(publicKeySpec);
            final PrivateKey privateKey = factory.generatePrivate(privateKeySpec);

            return new KeyPair(publicKey, privateKey);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptoException(e);
        }
    }

    private void checkAccountCredentials(final MemberEntity member) {
        final String toCheck = UUID.randomUUID().toString();
        final KeyPair keyPair = convertKey(member);
        final byte[] toEncrypt = toCheck.getBytes(crypto.getCharSet());
        final byte[] encrypted = crypto.encrypt(keyPair.getPublic(), toEncrypt);

    }

    /**
     * <p>Checks the System or Circle Administrator Login, to see if a valid
     * Account exist, if not - then a second check is made, to see if this is a
     * new setup where the System Administrator Account has not yet been
     * initialized.</p>
     *
     * @param request Process Member Request Object with Account Credentials
     * @return Circle or System Administrator Account
     * @throws ModelException if no Account could be found
     */
    private MemberEntity findMemberAccount(final ProcessMemberRequest request) {
        MemberEntity entity;

        try {
            // Perhaps we should merge the queries into a single query which reads all Trust information instead.
            entity = dao.findMemberByNameCredential(request.getName());
            checkAccount(entity, TrustLevel.ADMIN);
        } catch (ModelException e) {
            if (Objects.equals(Constants.ADMIN_ACCOUNT, request.getName()) && (e.getReturnCode() == Constants.IDENTIFICATION_WARNING)) {
                // First login
                entity = createNewAccount(request);
            } else {
                throw e;
            }
        }

        return entity;
    }

    /**
     * A Member must either be the System Admin or a Circle Admin to be allowed
     * to process Member Accounts. If not allowed, then an
     * {@code AuthorizationException} is thrown.
     *
     * @param member
     * @param level
     * @throws AuthorizationException if not allowed to process Member Accounts
     */
    private void checkAccount(final MemberEntity member, final TrustLevel level) {
        if (!Objects.equals(Constants.ADMIN_ACCOUNT, member.getName())) {
            final List<TrusteeEntity> trusts = dao.findTrustByMember(member);
            boolean trusted = false;
            for (final TrusteeEntity trust : trusts) {
                if (TrustLevel.isAllowed(trust.getTrustLevel(), level)) {
                    trusted = true;
                }
            }
            if (!trusted) {
                throw new AuthorizationException("THe requesting Account is not permitted to perform this Action.");
            }
        }
    }

    private MemberEntity createNewAccount(final Authentication authentication) {
        final CredentialType type = authentication.getCredentialType();
        final String salt = UUID.randomUUID().toString();
        final SecretKey key;

        if (type == CredentialType.KEY) {
            key = crypto.convertCredentialToKey(authentication.getCredential());
        } else {
            key = crypto.convertPasswordToKey(authentication.getCredential(), salt + settings.getSalt());
        }
        final KeyPair pair = crypto.generateAsymmetricKey();
        final IvParameterSpec iv = crypto.generateInitialVector(salt);
        final byte[] encryptedPrivateKey = crypto.encrypt(key, iv, pair.getPrivate().getEncoded());
        final String armoredPrivateKey = Crypto.base64Encode(pair.getPublic().getEncoded());
        final String armoredPublicKey = Crypto.base64Encode(encryptedPrivateKey);

        final MemberEntity account = new MemberEntity();
        account.setName(authentication.getName());
        account.setSalt(salt);
        account.setPrivateKey(armoredPrivateKey);
        account.setPublicKey(armoredPublicKey);
        dao.persist(account);

        return account;
    }
}
