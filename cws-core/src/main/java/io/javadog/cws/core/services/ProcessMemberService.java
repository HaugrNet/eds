package io.javadog.cws.core.services;

import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.dtos.Authentication;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.common.Crypto;
import io.javadog.cws.common.Settings;
import io.javadog.cws.core.Action;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.model.entities.MemberEntity;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.persistence.EntityManager;
import java.security.KeyPair;
import java.util.Base64;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessMemberService extends Servicable<ProcessMemberResponse, ProcessMemberRequest> {

    public ProcessMemberService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessMemberResponse process(final ProcessMemberRequest request) {
        verifyAndCheckRequest(request, Action.PROCESS_MEMBER);

        return new ProcessMemberResponse();
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
        final String base64EncryptedPrivateKey = Base64.getEncoder().encodeToString(encryptedPrivateKey);
        final String armoredPublicKey = Crypto.armorPublicKey(pair.getPublic());

        final MemberEntity account = new MemberEntity();
        account.setName(authentication.getName());
        account.setSalt(salt);
        account.setPrivateKey(base64EncryptedPrivateKey);
        account.setPublicKey(armoredPublicKey);
        dao.persist(account);

        return account;
    }
}
