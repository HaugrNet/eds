package io.javadog.cws.core;

import io.javadog.cws.api.common.Constants;
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

import javax.persistence.EntityManager;
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
     * <p>The method will all an unverified Request Object, and will as the
     * first step verify it before starting the actual processing. The
     * verification of the request Object is made regardless, to avoid that any
     * strange errors can or will occur.</p>
     *
     * @param request Request Object to process
     * @return Response Object with the result of the processing
     * @throws RuntimeException if an unknown error occurred
     */
    public abstract R process(V request);

    protected void verifyAndCheckRequest(final V verifiable, final Action action) {
        verify(verifiable);
        member = dao.findMemberByNameCredential(verifiable.getName());
        keyPair = checkAccountCredentials();
        checkAccount(action);
    }

    /**
     * <p>General Verification Method, takes the given Request Object and
     * invokes the validate method on it, to ensure that it is correct.</p>
     *        final KeyPair keyPair = checkAccountCredentials(entity);

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

    private KeyPair checkAccountCredentials() {
        final String toCheck = UUID.randomUUID().toString();
        keyPair = crypto.dearmorAsymmetricKey(member.getPublicKey(), member.getPrivateKey());

        final byte[] toEncrypt = toCheck.getBytes(crypto.getCharSet());
        final byte[] encrypted = crypto.encrypt(keyPair.getPublic(), toEncrypt);
        final byte[] decrypted = crypto.decrypt(keyPair.getPrivate(), encrypted);
        final String result = new String(decrypted, crypto.getCharSet());

        if (!Objects.equals(result, toCheck)) {
            throw new AuthorizationException("Cannot authenticate the Member from the given Credentials.");
        }

        return keyPair;
    }

    private void checkAccount(final Action action) {
        if (!Objects.equals(Constants.ADMIN_ACCOUNT, member.getName())) {
            final List<TrusteeEntity> trusts = dao.findTrustByMember(member);
            boolean trusted = false;
            for (final TrusteeEntity trust : trusts) {
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
