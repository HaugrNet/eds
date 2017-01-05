package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.dtos.Authentication;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.common.Crypto;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.ModelException;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.model.CommonDao;
import io.javadog.cws.model.entities.MemberEntity;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.KeyPair;
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

        checkAdminForInitialLogin(request);

        return new ProcessMemberResponse();
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
    private MemberEntity checkAdminForInitialLogin(final ProcessMemberRequest request) {
        MemberEntity entity;

        try {
            entity = dao.findMemberByNameCredential(request.getName());
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
