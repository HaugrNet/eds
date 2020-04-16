/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2020, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.MemberRole;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.requests.MasterKeyRequest;
import io.javadog.cws.api.responses.MasterKeyResponse;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.AuthenticationException;
import io.javadog.cws.core.exceptions.CryptoException;
import io.javadog.cws.core.jce.Crypto;
import io.javadog.cws.core.jce.MasterKey;
import io.javadog.cws.core.jce.SecretCWSKey;
import io.javadog.cws.core.model.CommonDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.MemberEntity;
import io.javadog.cws.core.model.entities.SettingEntity;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.persistence.EntityManager;

/**
 * <p>Business Logic implementation for the CWS MasterKey request.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
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
        verify(request);

        if (!Objects.equals(request.getAccountName(), Constants.ADMIN_ACCOUNT)) {
            throw new AuthenticationException("Given Account is not permitted to perform this request.");
        }

        return checkRequest(request);
    }

    private MasterKeyResponse checkRequest(final MasterKeyRequest request) {
        // First, retrieve the existing MasterKey, which we need to check.
        // Primarily, as updating it to an invalid key can be devastating. So
        // it is initially being saved, just in case...
        // What if the keys are the same ?
        final MasterKey masterKey = MasterKey.getInstance(settings);
        final SecretCWSKey oldMasterKey = masterKey.getKey();
        final SecretCWSKey newMasterKey = prepareNewMasterKey(masterKey, request);

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
            throwConditionalException(dao.countMembers() != 1,
                    ReturnCode.ILLEGAL_ACTION, "Cannot alter the MasterKey, as Member Accounts exists.");
            masterKey.setKey(newMasterKey);
            updateMemberPassword(admin, request.getCredential());
            response = new MasterKeyResponse(ReturnCode.SUCCESS, "MasterKey updated.");
        } else {
            // Neither keys worked, throw Authentication Exception
            throw new AuthenticationException("Invalid credentials.");
        }

        return response;
    }

    private SecretCWSKey prepareNewMasterKey(final MasterKey masterKey, final MasterKeyRequest request) {
        byte[] rawSecret = request.getSecret();

        if (rawSecret == null) {
            rawSecret = MasterKey.readMasterKeySecretFromUrl(request.getUrl());
            updateMasterKeySetting(request.getUrl());
        } else {
            // If set via a secret, the URL should be removed to prevent a
            // restart to use the URL.
            updateMasterKeySetting("");
        }

        return masterKey.generateMasterKey(rawSecret);
    }

    private void updateMasterKeySetting(final String url) {
        SettingEntity entity = dao.findSettingByKey(StandardSetting.MASTERKEY_URL);

        if (entity == null) {
            entity = new SettingEntity();
            entity.setName(StandardSetting.MASTERKEY_URL.getKey());
        }

        entity.setSetting(url);
        dao.persist(entity);
    }

    private boolean checkCredentials(final SecretCWSKey masterKey, final MemberEntity admin, final byte[] secret) {
        boolean result = false;

        try {
            // First, decrypt the Salt for the Administrator, using the "MasterKey".
            final byte[] encrypted = Base64.getDecoder().decode(admin.getSalt());
            final byte[] decrypted = Crypto.decrypt(masterKey, encrypted);
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
            LOG.log(Settings.DEBUG, e, () -> "Decrypting the System Administrator Account failed: " + e.getMessage());
        }

        return result;
    }

    private MemberEntity findAdmin(final Authentication authentication) {
        final List<MemberEntity> admins = dao.findMemberByRole(MemberRole.ADMIN);
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
