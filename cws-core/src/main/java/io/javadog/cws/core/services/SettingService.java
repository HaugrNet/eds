package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.common.Crypto;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.model.entities.MemberEntity;
import io.javadog.cws.model.entities.SettingEntity;

import javax.crypto.spec.IvParameterSpec;
import javax.persistence.EntityManager;
import java.security.Key;
import java.security.KeyPair;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * <p>The Setting Service, allows for checking and updating existing Settings
 * and add new Settings, needed for Clients. Certain of the existing Settings
 * have a flag that will not allow them to be updated. This was done to ensure
 * that it is clear what existing settings is being used such as Cryptographic
 * Algorithms, padding, etc. However, as the internal Cryptographic Operations
 * is strongly linked to the Algorithms, they may be seen but not updated.</p>
 *
 * <p>For the same reason, the request can <b>only</b> be invoked by the System
 * Administrator. Since changing things must be strictly limited.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SettingService extends Servicable<SettingResponse, SettingRequest> {

    public SettingService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettingResponse process(final SettingRequest request) {
        if (Objects.equals(request.getAccount(), Constants.ADMIN_ACCOUNT)) {
            checkAccount(request);

            final Map<String, SettingEntity> currentSettings = convertSettings(dao.readSettings());
            for (final Map.Entry<String, String> entry : request.getSettings().entrySet()) {
                processSetting(currentSettings, entry);
            }

            final SettingResponse response = new SettingResponse();
            response.setSettings(transformSettings(currentSettings));

            return response;
        } else {
            throw new CWSException(Constants.IDENTIFICATION_WARNING, "Cannot complete this request, as it is only allowed for the System Administrator.");
        }
    }

    /**
     * Processes an existing or new Setting, and both saves the result in the
     * database and ensures that the given map is updated.
     *
     * @param currentSettings Current Settings to check and update
     * @param entry Entry from the given set of Settings to process
     */
    private void processSetting(final Map<String, SettingEntity> currentSettings, final Map.Entry<String, String> entry) {
        if (currentSettings.containsKey(entry.getKey())) {
            final SettingEntity entity = currentSettings.get(entry.getKey());

            if (!Objects.equals(entity.getSetting(), entry.getValue())) {
                if (entity.getModifiable()) {
                    entity.setSetting(entry.getValue());
                    dao.persist(entity);
                } else {
                    throw new CWSException(Constants.PROPERTY_ERROR, "The setting " + entry.getKey() + " may not be overwritten.");
                }
            }
        } else {
            final SettingEntity setting = new SettingEntity();
            setting.setName(entry.getKey());
            setting.setSetting(entry.getValue());
            setting.setModifiable(true);
            dao.persist(setting);

            currentSettings.put(setting.getName(), setting);
        }
    }

    private static Map<String, SettingEntity> convertSettings(final List<SettingEntity> list) {
        final Map<String, SettingEntity> map = new HashMap<>(16);

        for (final SettingEntity setting : list) {
            map.put(setting.getName(), setting);
        }

        return map;
    }

    private static Map<String, String> transformSettings(final Map<String, SettingEntity> settings) {
        final Map<String, String> map = new HashMap<>();

        for (final Map.Entry<String, SettingEntity> entry : settings.entrySet()) {
            map.put(entry.getKey(), entry.getValue().getSetting());
        }

        return map;
    }

    private void checkAccount(final SettingRequest request) {
        try {
            verifyRequest(request, Permission.SETTING);
        } catch (CWSException e) {
            if (e.getReturnCode() == Constants.IDENTIFICATION_WARNING) {
                // Account doesn't exist, so we're creating a new one based on
                // the credentials. This also means that we won't throw the
                // exception.
                member = createNewAdminAccount(request);
            } else {
                // Account does exist but given credentials doesn't match, so
                // we'll just re-throw the error
                throw e;
            }
        }
    }

    private MemberEntity createNewAdminAccount(final SettingRequest request) {
        final String salt = UUID.randomUUID().toString();
        final Key key = extractKeyFromCredentials(request, salt);

        final KeyPair pair = crypto.generateAsymmetricKey();
        final IvParameterSpec iv = crypto.generateInitialVector(salt);
        final byte[] encryptedPrivateKey = crypto.encrypt(key, iv, pair.getPrivate().getEncoded());
        final String base64EncryptedPrivateKey = Base64.getEncoder().encodeToString(encryptedPrivateKey);
        final String armoredPublicKey = Crypto.armorPublicKey(pair.getPublic());

        final MemberEntity account = new MemberEntity();
        account.setName(Constants.ADMIN_ACCOUNT);
        account.setSalt(salt);
        account.setPrivateKey(base64EncryptedPrivateKey);
        account.setPublicKey(armoredPublicKey);
        dao.persist(account);

        return account;
    }
}
