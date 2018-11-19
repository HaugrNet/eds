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
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.requests.MasterKeyRequest;
import io.javadog.cws.api.responses.MasterKeyResponse;
import io.javadog.cws.core.enums.MemberRole;
import io.javadog.cws.core.exceptions.CryptoException;
import io.javadog.cws.core.jce.MasterKey;
import io.javadog.cws.core.jce.SecretCWSKey;
import io.javadog.cws.core.model.CommonDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.MemberEntity;

import javax.persistence.EntityManager;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * <p>Business Logic implementation for the CWS MasterKey request.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class MasterKeyService extends Serviceable<CommonDao, MasterKeyResponse, MasterKeyRequest> {

    private static final Logger LOG = Logger.getLogger(MasterKeyService.class.getName());

    public MasterKeyService(final Settings settings, final EntityManager entityManager) {
        super(settings, new CommonDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MasterKeyResponse perform(final MasterKeyRequest request) {
        final MasterKeyResponse response;

        if ((request != null) && request.validate().isEmpty()) {
            if (Objects.equals(request.getAccountName(), Constants.ADMIN_ACCOUNT)) {
                response = checkRequest(request);
            } else {
                response = new MasterKeyResponse(ReturnCode.AUTHENTICATION_WARNING, "Given Account is not permitted to perform this request.");
            }
        } else {
            response = new MasterKeyResponse(ReturnCode.VERIFICATION_WARNING, "Cannot process the request, the given data is invalid.");
        }

        return response;
    }

    private MasterKeyResponse checkRequest(final MasterKeyRequest request) {
        // First, retrieve the existing MasterKey, which we need to check.
        // Primarily, as updating it to an invalid key can be devastating. So
        // it is initially being saved, just in case...
        // What if the keys are the same ?
        final MasterKey masterKey = MasterKey.getInstance(settings);
        final SecretCWSKey oldMasterKey = masterKey.getKey();
        final SecretCWSKey newMasterKey = masterKey.generateMasterKey(request.getSecret());

        // For this request, the default account checks won't work, hence it
        // must be checked directly, with the keys
        final MemberEntity admin = findAdmin(request);
        final MasterKeyResponse response;

        // First check is with the new Key, as it is assumed that the primary
        // invocation of this request is to unlock a system, rather than update
        // the Master Key. If this fails, then try with the old Key, and if that
        // works, the Key should be updated. If both fails ... tough!
        if (checkCredentials(newMasterKey, admin, request.getCredential())) {
            // Default check, new MasterKey is correct, updating the Key to
            // reflect this.
            masterKey.setKey(newMasterKey);
            response = new MasterKeyResponse(ReturnCode.SUCCESS, "MasterKey unlocked.");
        } else if (checkCredentials(oldMasterKey, admin, request.getCredential())) {
            // The old masterKey was needed to unlock, but a new secret has been
            // requested - so let's update the masterKey, and also the Salt for
            // the System Administrator... However, this can *only* be done, if
            // no other accounts exists in the system!
            if (dao.countMembers() == 1) {
                // Only one account exists, the System Administrator - so we can
                // update the key.
                masterKey.setKey(newMasterKey);
                updateMemberPassword(admin, request.getCredential());
                response = new MasterKeyResponse(ReturnCode.SUCCESS, "MasterKey updated.");
            } else {
                response = new MasterKeyResponse(ReturnCode.ILLEGAL_ACTION, "Cannot alter the MasterKey, as Member Accounts exists.");
            }
        } else {
            // Neither keys worked, will just return a Authentication Warning
            response = new MasterKeyResponse(ReturnCode.AUTHENTICATION_WARNING, "Invalid credentials.");
        }

        return response;
    }

    private boolean checkCredentials(final SecretCWSKey masterKey, final MemberEntity admin, final byte[] secret) {
        boolean result = false;

        try {
            // First, decrypt the Salt for the Administrator, using the "MasterKey".
            final byte[] encrypted = Base64.getDecoder().decode(admin.getSalt());
            final byte[] decrypted = crypto.decrypt(masterKey, encrypted);
            final String salt = crypto.bytesToString(decrypted);

            // With the decrypted Salt, we can try to unlock the Private Key, if it
            // fails, then a CryptoException is thrown, which we'll ignore here.
            final SecretCWSKey key = crypto.generatePasswordKey(admin.getPbeAlgorithm(), secret, salt);
            crypto.extractAsymmetricKey(admin.getRsaAlgorithm(), key, salt, admin.getPublicKey(), admin.getPrivateKey());

            // To ensure that the PBE key is no longer usable, we're destroying
            // it now.
            key.destroy();
            result = true;
        } catch (CryptoException e) {
            LOG.log(Settings.DEBUG, "Decrypting the System Administrator Account failed: " + e.getMessage(), e);
        }

        return result;
    }

    private MemberEntity findAdmin(final Authentication authentication) {
        List<MemberEntity> admins = dao.findMemberByRole(MemberRole.ADMIN);
        final MemberEntity admin;

        if (admins.isEmpty()) {
            admin = createNewAccount(Constants.ADMIN_ACCOUNT, MemberRole.ADMIN, authentication.getCredential());
        } else {
            // Fetch the first administrator, the one with Id 1.
            admin = admins.get(0);
        }

        return admin;
    }
}
