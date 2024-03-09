/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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

import java.util.Base64;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.EntityManager;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.MasterKeyRequest;
import net.haugr.eds.api.responses.MasterKeyResponse;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.exceptions.AuthenticationException;
import net.haugr.eds.core.exceptions.CryptoException;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.jce.MasterKey;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.model.entities.SettingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Business Logic implementation for the EDS MasterKey request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class MasterKeyManager extends AbstractManager<CommonDao, MasterKeyResponse, MasterKeyRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MasterKeyManager.class);

    public MasterKeyManager(final Settings settings, final EntityManager entityManager) {
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
        final SecretEDSKey oldMasterKey = masterKey.getKey();
        final SecretEDSKey newMasterKey = prepareNewMasterKey(masterKey, request);

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

    private SecretEDSKey prepareNewMasterKey(final MasterKey masterKey, final MasterKeyRequest request) {
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
        dao.save(entity);
    }

    private boolean checkCredentials(final SecretEDSKey masterKey, final MemberEntity admin, final byte[] secret) {
        boolean result = false;

        try {
            // First, decrypt the Salt for the Administrator, using the "MasterKey".
            final byte[] encrypted = Base64.getDecoder().decode(admin.getSalt());
            final byte[] decrypted = Crypto.decrypt(masterKey, encrypted);
            final String salt = crypto.bytesToString(decrypted);

            // With the decrypted Salt, we can try to unlock the Private Key, if it
            // fails, then a CryptoException is thrown, which we'll ignore here.
            final SecretEDSKey key = crypto.generatePasswordKey(admin.getPbeAlgorithm(), secret, salt);
            crypto.extractAsymmetricKey(admin.getRsaAlgorithm(), key, salt, admin.getPublicKey(), admin.getPrivateKey());
            result = true;
        } catch (CryptoException e) {
            LOGGER.debug("Decrypting the System Administrator Account failed: {}", e.getMessage(), e);
        }

        return result;
    }

    private MemberEntity findAdmin(final Authentication authentication) {
        final List<MemberEntity> admins = dao.findMemberByRole(MemberRole.ADMIN);
        final MemberEntity admin;

        if (admins.isEmpty()) {
            admin = createNewAccount(Constants.ADMIN_ACCOUNT, MemberRole.ADMIN, authentication.getCredential());
        } else {
            // Fetch the first administrator, the one with ID 1.
            admin = admins.getFirst();
        }

        return admin;
    }
}
