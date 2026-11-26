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

import java.security.MessageDigest;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.MasterKeyRequest;
import net.haugr.eds.api.responses.MasterKeyResponse;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.exceptions.AuthenticationException;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.jce.MasterKey;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.MasterKeyEntity;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.model.entities.SettingEntity;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Business Logic for the EDS MasterKey (PLAN.md, Crypto §1): loading it at
 * startup, unlocking a locked EDS, the first setup, and rotating it.</p>
 *
 * <p>The database only holds the version and key check value (KCV) of the
 * MasterKey. A secret whose KCV doesn't match the stored one is rejected,
 * without decrypting anything.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class MasterKeyManager extends AbstractManager<CommonDao, MasterKeyResponse, MasterKeyRequest> {

    /** Environment variable, which overrides the {@code eds.masterkey.url} setting. */
    public static final String MASTERKEY_URL_ENV = "EDS_MASTERKEY_URL";

    private static final Logger LOGGER = LoggerFactory.getLogger(MasterKeyManager.class);
    private static final String INVALID_CREDENTIALS = "Invalid credentials.";

    private final MasterKey masterKey = MasterKey.getInstance();
    /** A rotated MasterKey, installed only once its transaction committed (ticket 13). */
    private MasterKey.Material rotated = null;
    private int rotatedVersion = 0;

    public MasterKeyManager(final Settings settings, final EntityManager entityManager) {
        super(settings, new CommonDao(entityManager));
    }

    // =========================================================================
    // Startup
    // =========================================================================

    /**
     * <p>Loads the MasterKey from the external source at startup. The source
     * is the URL in the environment variable {@value #MASTERKEY_URL_ENV}, the
     * system property {@code eds.masterkey.url}, or the
     * {@code eds.masterkey.url} setting, in this order.</p>
     *
     * <p>Without a source, or if the source can't be read or doesn't match
     * the stored key check value, EDS stays locked. It can then be unlocked
     * with the MasterKey request. There is no fallback secret.</p>
     *
     * @return True if EDS was unlocked
     */
    public boolean loadAtStartup() {
        final String url = findMasterKeyUrl();
        boolean unlocked = false;

        if (url.isEmpty()) {
            LOGGER.warn("No MasterKey source is configured ({} or {}), EDS is locked until it is unlocked with the MasterKey request.",
                    MASTERKEY_URL_ENV, StandardSetting.MASTERKEY_URL.getKey());
        } else {
            try {
                final byte[] secret = MasterKey.readMasterKeySecretFromUrl(url);
                final MasterKeyEntity stored = dao.findCurrentMasterKey();

                if (stored == null) {
                    setupMasterKey(secret);
                    LOGGER.info("The MasterKey was set up, version 1.");
                    unlocked = true;
                } else if (matches(MasterKey.derive(secret), stored)) {
                    masterKey.install(MasterKey.derive(secret), stored.getVersion());
                    LOGGER.info("The MasterKey was loaded, version {}.", stored.getVersion());
                    unlocked = true;
                } else {
                    LOGGER.error("The MasterKey from the configured source doesn't match the stored key check value, EDS is locked.");
                }
            } catch (EDSException e) {
                LOGGER.error("The MasterKey could not be loaded, EDS is locked: {}", e.getMessage(), e);
                masterKey.lock();
            }
        }

        return unlocked;
    }

    private String findMasterKeyUrl() {
        String url = System.getenv(MASTERKEY_URL_ENV);
        if ((url == null) || url.isBlank()) {
            url = System.getProperty(StandardSetting.MASTERKEY_URL.getKey());
        }
        if ((url == null) || url.isBlank()) {
            url = settings.getMasterKeyURL();
        }

        return url.trim();
    }

    // =========================================================================
    // MasterKey Request
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public MasterKeyResponse perform(final MasterKeyRequest request) {
        verify(request);

        checkPermitted(request);

        final MasterKeyResponse response;
        if (masterKey.isUnlocked()) {
            // The passphrase first: nothing is read from a URL for whoever
            // doesn't know it (ticket 6).
            checkPassphrase(findAdmin(request), request.getCredential(), INVALID_CREDENTIALS);
            response = processUnlocked(request, readRawSecret(request));
        } else {
            response = processLocked(request, readSecretWhileLocked(request));
        }

        return response;
    }

    /**
     * While EDS is locked, the passphrase can't be checked before the secret
     * is read: the keys of the accounts are wrapped with the MasterKey. A
     * source which can't be read is then answered as a wrong secret, so the
     * answer tells nothing about files or hosts (ticket 6). The log tells
     * the kind of the failure, without the URL (ticket 5). Only the refusal
     * because other sources are being read is passed on as it is.
     */
    private static byte[] readSecretWhileLocked(final MasterKeyRequest request) {
        try {
            return readRawSecret(request);
        } catch (EDSException e) {
            if (MasterKey.SOURCES_BUSY.equals(e.getMessage())) {
                // Depends on no URL, so it tells nothing; and the System
                // Administrator must see why, not a wrong secret.
                throw e;
            }
            LOGGER.warn("The MasterKey request's source could not be read while EDS is locked: {}", e.getMessage());
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }
    }

    private MasterKeyResponse processLocked(final MasterKeyRequest request, final byte[] rawSecret) {
        final MasterKeyEntity stored = dao.findCurrentMasterKey();
        final MasterKey.Material material = MasterKey.derive(rawSecret);

        if (stored == null) {
            // First setup of this instance.
            setupMasterKey(rawSecret);
        } else if (matches(material, stored)) {
            masterKey.install(material, stored.getVersion());
        } else {
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }

        // The MasterKey is correct; the System Administrator must still
        // authenticate, otherwise EDS stays locked. It is locked again on any
        // failure from here on, also one which isn't an EDS Exception (the
        // count runs in transactions of its own), and also when the first
        // account of a stone can't be made, so EDS is never left unlocked
        // without the System Administrator's passphrase.
        boolean authenticated = false;
        try {
            checkPassphrase(findAdmin(request), request.getCredential(), INVALID_CREDENTIALS);
            authenticated = true;
        } finally {
            if (!authenticated) {
                masterKey.lock();
            }
        }
        updateMasterKeySetting(request);

        return new MasterKeyResponse(ReturnCode.SUCCESS, "MasterKey unlocked.");
    }

    // The passphrase is checked before the secret is read, see perform.
    private MasterKeyResponse processUnlocked(final MasterKeyRequest request, final byte[] rawSecret) {
        final MasterKey.Material material = MasterKey.derive(rawSecret);
        final MasterKeyResponse response;

        if (MessageDigest.isEqual(crypto.stringToBytes(material.kcv()), crypto.stringToBytes(masterKey.getKcv()))) {
            updateMasterKeySetting(request);
            response = new MasterKeyResponse(ReturnCode.SUCCESS, "MasterKey unlocked.");
        } else {
            verifyEntropy("MasterKey secret", rawSecret);
            rotate(material);
            updateMasterKeySetting(request);
            response = new MasterKeyResponse(ReturnCode.SUCCESS, "MasterKey updated.");
        }

        return response;
    }

    // =========================================================================
    // Internal Methods
    // =========================================================================

    /**
     * <p>Rotates the MasterKey (PLAN.md, Crypto §1): every outer layer is
     * re-wrapped with the new MasterKey in the request's transaction, so a
     * failure leaves the old MasterKey in place. Data Objects are not
     * touched, as the MasterKey isn't part of the data path.</p>
     *
     * <ul>
     *   <li>Member salts and private keys are re-wrapped.</li>
     *   <li>Sessions end, and pending invitations are deleted, as their
     *   tokens are keyed with the old MasterKey.</li>
     *   <li>The Circle Secrets of all Trustees are re-wrapped.</li>
     * </ul>
     *
     * @param material The new MasterKey
     */
    private void rotate(final MasterKey.Material material) {
        final int version = masterKey.getVersion() + 1;

        // Every Session ends: their outer layer is of the old MasterKey.
        dao.createNamedQuery("session.removeAll").executeUpdate();

        // The Members are locked, in the order of their IDs, before they are
        // read: a new passphrase committed meanwhile would otherwise be
        // overwritten by its old keys, wrapped anew (ticket 13). Then the
        // Circles, before anything changes: a pending invitee goes with its
        // invitations and Trustee rows, which the rotation of a Circle Secret
        // changes under the lock of its Circle (ticket 1).
        final List<MemberEntity> members = dao.lockMembers();
        dao.lockCircles(dao.findAllAscending(CircleEntity.class, "id"));
        for (final MemberEntity entity : members) {
            if (ProcessMemberManager.INVITATION_MARKER.equals(entity.getPrivateKey())) {
                dao.delete(entity);
            } else {
                entity.setSalt(crypto.rewrapWithMasterKey(memberSaltAad(entity), entity.getSalt(), material, version));
                entity.setPrivateKey(crypto.rewrapWithMasterKey(memberOuterAad(entity), entity.getPrivateKey(), material, version));
                dao.save(entity);
            }
        }

        // No rotation of a Circle Secret may change a Trustee row meanwhile
        // (PLAN.md, Crypto C6), or one of the two would undo the other.
        // The Trustees are read after the Circles are locked: one added while
        // the rotation waited for its Circle is wrapped anew too.
        final List<TrusteeEntity> allTrustees = dao.findAllAscending(TrusteeEntity.class, "id");
        allTrustees.forEach(dao::refresh);
        for (final TrusteeEntity trustee : allTrustees) {
            final String aad = TRUSTEE_OUTER_AAD + trusteeBinding(trustee.getCircle(), trustee.getMember(), trustee.getKey());
            trustee.setCircleKey(crypto.rewrapWithMasterKey(aad, trustee.getCircleKey(), material, version));
            // The retiring Circle Secret, while it is rotated (PLAN.md, Crypto C6).
            if (trustee.getPreviousKey() != null) {
                final String previousAad = TRUSTEE_OUTER_AAD + trusteeBinding(trustee.getCircle(), trustee.getMember(), trustee.getPreviousKey());
                trustee.setPreviousCircleKey(crypto.rewrapWithMasterKey(previousAad, trustee.getPreviousCircleKey(), material, version));
            }
            dao.save(trustee);
        }

        saveMasterKey(version, material.kcv());
        // Installed when the transaction committed, not before: until then
        // the rows are wrapped with the old MasterKey, for every other
        // request, and a rollback leaves the old one in place (ticket 13).
        rotated = material;
        rotatedVersion = version;
    }

    /**
     * Installs the MasterKey this request rotated to, once its transaction
     * has committed: at once without a transaction of the container (the
     * tests), not at all if the transaction is rolled back.
     *
     * @param transactions The registry of the transaction, or null
     */
    public void installAfterCommit(final TransactionSynchronizationRegistry transactions) {
        final MasterKey.Material material = rotated;
        if (material == null) {
            return;
        }
        final int version = rotatedVersion;
        rotated = null;
        if (transactions == null) {
            masterKey.install(material, version);
        } else if (transactions.getTransactionStatus() == Status.STATUS_ACTIVE) {
            transactions.registerInterposedSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {
                    // Nothing before.
                }

                @Override
                public void afterCompletion(final int status) {
                    if (status == Status.STATUS_COMMITTED) {
                        masterKey.install(material, version);
                    } else {
                        LOGGER.warn("The MasterKey wasn't changed, as its transaction ended with the status {}.", status);
                    }
                }
            });
        }
    }

    private void setupMasterKey(final byte[] secret) {
        verifyEntropy("MasterKey secret", secret);
        final MasterKey.Material material = MasterKey.derive(secret);
        masterKey.install(material, 1);
        saveMasterKey(1, material.kcv());
    }

    private void saveMasterKey(final int version, final String kcv) {
        final MasterKeyEntity entity = new MasterKeyEntity();
        entity.setVersion(version);
        entity.setKcv(kcv);
        dao.save(entity);
    }

    private boolean matches(final MasterKey.Material material, final MasterKeyEntity stored) {
        return MessageDigest.isEqual(crypto.stringToBytes(material.kcv()), crypto.stringToBytes(stored.getKcv()));
    }

    private static byte[] readRawSecret(final MasterKeyRequest request) {
        byte[] rawSecret = request.getSecret();

        if (rawSecret == null) {
            rawSecret = MasterKey.readMasterKeySecretFromUrl(request.getUrl());
        }

        return rawSecret;
    }

    /**
     * Stores the URL of the MasterKey, so it is used after a restart. It
     * must only be invoked once the System Administrator credentials have
     * been verified, as the URL is otherwise open for anyone to change.
     *
     * @param request Request Object with the MasterKey
     */
    private void updateMasterKeySetting(final MasterKeyRequest request) {
        // If set via a secret, the URL should be removed to prevent a
        // restart to use the URL.
        final String url = (request.getSecret() == null) ? request.getUrl() : "";
        SettingEntity entity = dao.findSettingByKey(StandardSetting.MASTERKEY_URL);

        if (entity == null) {
            entity = new SettingEntity();
            entity.setName(StandardSetting.MASTERKEY_URL.getKey());
        }

        entity.setSetting(url);
        dao.save(entity);
    }

    /**
     * Only the System Administrator may give the MasterKey: the first
     * account of the stone, whatever its name. On a stone without accounts
     * nobody is it yet, and who gives the first MasterKey becomes it. This
     * is checked before anything is done with the secret.
     */
    private void checkPermitted(final Authentication authentication) {
        final MemberEntity admin = systemAdministrator();
        final boolean permitted = (admin != null)
                ? admin.getName().equalsIgnoreCase(trim(authentication.getAccountName()))
                : (dao.countMembers() == 0);
        if (!permitted) {
            throw new AuthenticationException("Given Account is not permitted to perform this request.");
        }
    }

    private MemberEntity findAdmin(final Authentication authentication) {
        final MemberEntity admin = systemAdministrator();

        return (admin != null) ? admin : claimStone(trim(authentication.getAccountName()), authentication.getCredential());
    }
}
