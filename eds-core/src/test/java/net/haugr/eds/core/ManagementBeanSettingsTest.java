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
package net.haugr.eds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.SettingRequest;
import net.haugr.eds.api.responses.SettingResponse;
import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.enums.HashAlgorithm;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.MemberEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * <p>This Test Class is testing the following Service Classes in one, as they
 * are all fairly small but also connected.</p>
 *
 * <ul>
 *   <li>SettingService</li>
 * </ul>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class ManagementBeanSettingsTest extends DatabaseSetup {

    @Test
    void testCreatingAdmin() {
        // For most tests the System Administrator is present, so by default
        // it already exists. To test that a stone without accounts takes
        // the first who comes as its System Administrator, all accounts are
        // deleted first. Each test runs in a transaction which is rolled
        // back, so this doesn't disturb other tests.
        entityManager.createQuery("delete from MemberEntity m").executeUpdate();

        // The name is the choice of whoever comes; the stone sets none.
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request = prepareRequest(SettingRequest.class, "sysop");

        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        Assertions.assertEquals(StandardSetting.values().length, response.getSettings().size());
    }

    /**
     * Only a stone without accounts takes a first one. With the System
     * Administrator gone but other accounts left, which the stone itself
     * never lets happen, nobody becomes it by using a name: the name
     * "admin" was made anew by whoever used it first, before 2026-11-18.
     */
    @Test
    void testNoAdminIsMadeWhileThereAreAccounts() {
        final int deleted = entityManager
                .createQuery("delete from MemberEntity m where m.id = :id")
                .setParameter("id", 1L)
                .executeUpdate();
        assertEquals(1, deleted);

        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingResponse response = bean.settings(prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT));
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
        // An unknown name is answered as a wrong passphrase (ticket 21).
        assertEquals("Cannot authenticate the Account from the given Credentials.", response.getReturnMessage());
    }

    @Test
    void testNonAdminRequest() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request = prepareRequest(SettingRequest.class, MEMBER_1);
        assertNotEquals(Constants.ADMIN_ACCOUNT, request.getAccountName());

        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Cannot complete this request, as it is only allowed for the System Administrator.", response.getReturnMessage());
    }

    @Test
    void testInvokingRequestWithNullSettingsList() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(StandardSetting.values().length, response.getSettings().size());
    }

    @Test
    void testInvokingRequestWithEmptySettings() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        request.setSettings(mySettings);

        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(StandardSetting.values().length, response.getSettings().size());
    }

    @Test
    void testInvokingRequestWithNullKey() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put(null, "NullKey");
        request.setSettings(mySettings);

        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), response.getReturnCode());
        assertEquals("Setting Keys may neither be null nor empty.", response.getReturnMessage());
    }

    @Test
    void testInvokingRequestWithEmptyKey() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("", "EmptyKey");
        request.setSettings(mySettings);

        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), response.getReturnCode());
        assertEquals("Setting Keys may neither be null nor empty.", response.getReturnMessage());
    }

    /**
     * <p>Certain values in the EDS Settings are considered critical, meaning
     * that they can only be updated if no accounts, except the system
     * administrator, exist. However, the problem with these tests is that they
     * need to alter existing data, meaning that the underlying database may
     * have a lock set, which will affect other tests.</p>
     */
    @Test
    void testUpdatingCriticalValues() {
        // Before starting, all member accounts must be removed
        final List<MemberEntity> members = dao.findAllAscending(MemberEntity.class, "id");
        for (final MemberEntity member : members) {
            dao.delete(member);
        }

        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());

        final Map<String, String> mySettings = new HashMap<>(response.getSettings());
        mySettings.put(StandardSetting.PBE_ITERATIONS.getKey(), "100000");
        request.setCredential(credentialOf(Constants.ADMIN_ACCOUNT));
        request.setSettings(mySettings);
        final SettingResponse update = bean.settings(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), update.getReturnCode());

        // Running a verification check to ensure that the System Administrator
        // can still access the system, after the iterations were updated.
        final SettingRequest checkRequest = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final SettingResponse checkResponse = bean.settings(checkRequest);
        assertTrue(checkResponse.isOk());

        // Finally, the negative test, seeing what happens if an invalid PBE
        // iteration count is defined
        mySettings.put(StandardSetting.PBE_ITERATIONS.getKey(), "-100000");
        final SettingRequest negativeRequest = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        negativeRequest.setSettings(mySettings);

        final SettingResponse negativeResponse = bean.settings(negativeRequest);
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), negativeResponse.getReturnCode());
        assertEquals("Invalid Integer value for 'PBE_ITERATIONS'.", negativeResponse.getReturnMessage());
    }

    @Test
    void testInvokingRequestUpdateExistingSetting() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        // First invocation, retrieving the list of current values, so we can
        // check that it is being updated
        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(StandardSetting.values().length, response.getSettings().size());
        assertEquals("EN", response.getSettings().get(StandardSetting.EDS_LOCALE.getKey()));

        // The internal collection used is unmodifiable. So we simply copy the
        // list from the response and update one of the existing values
        final Map<String, String> mySettings = new HashMap<>(response.getSettings());
        mySettings.put(StandardSetting.EDS_LOCALE.getKey(), "DA");
        request.setSettings(mySettings);
        request.setCredential(credentialOf(Constants.ADMIN_ACCOUNT));

        final SettingResponse update = bean.settings(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), update.getReturnCode());
        assertEquals("Ok", update.getReturnMessage());
        assertEquals(StandardSetting.values().length, update.getSettings().size());
        assertEquals("DA", update.getSettings().get(StandardSetting.EDS_LOCALE.getKey()));
    }

    @Test
    void testAddingAndRemovingCustomSetting() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("my.setting.key", "value");
        request.setSettings(mySettings);

        final SettingResponse update = bean.settings(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), update.getReturnCode());
        assertEquals("Ok", update.getReturnMessage());
        assertEquals(StandardSetting.values().length + 1, update.getSettings().size());
    }

    @Test
    void testChangingIterationsWithMembers() {
        // The accounts keep the iterations of their passphrases, so the
        // setting may change while Members exist: it counts for new
        // passphrases only.
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("eds.crypto.pbe.iterations", "2048");
        request.setSettings(mySettings);

        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("2048", response.getSettings().get("eds.crypto.pbe.iterations"));

        // The System Administrator still logs in, with the iterations of the account.
        final SettingResponse again = bean.settings(prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT));
        assertTrue(again.isOk());

        // Too few iterations are refused.
        final SettingRequest weak = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        weak.setSettings(Map.of("eds.crypto.pbe.iterations", "999"));
        final SettingResponse refused = bean.settings(weak);
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), refused.getReturnCode());
        assertEquals("The value for 'eds.crypto.pbe.iterations' must be from 1000 to 999999999.", refused.getReturnMessage());
    }

    @Test
    void testAllowedValues() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingResponse response = bean.settings(prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT));
        assertTrue(response.isOk());
        final Map<String, java.util.List<String>> allowed = response.getAllowedValues();
        assertEquals(4, allowed.size());
        assertEquals(java.util.List.of("AES_GCM_256"), allowed.get("eds.crypto.symmetric.algorithm"));
        assertEquals(java.util.List.of("MLKEM1024_P384"), allowed.get("eds.crypto.asymmetric.algorithm"));
        assertEquals(java.util.List.of("PBE_GCM_256"), allowed.get("eds.crypto.pbe.algorithm"));
        assertEquals(java.util.List.of("SHA_384", "SHA_512"), allowed.get("eds.crypto.hash.algorithm"));
    }

    @Test
    void testInvokingRequestAddNewSetting() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("eds.test.setting", "Setting Value");
        request.setSettings(mySettings);

        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(StandardSetting.values().length + 1, response.getSettings().size());
        assertEquals("Setting Value", response.getSettings().get("eds.test.setting"));
    }

    @Test
    void testDeletingCustomSetting() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("eds.test.setting", "Test Value");
        request.setSettings(mySettings);
        assertTrue(request.validate().isEmpty());

        final SettingResponse response = bean.settings(request);
        assertTrue(response.isOk());
        assertEquals(StandardSetting.values().length + 1, response.getSettings().size());

        mySettings.put("eds.test.setting", null);
        request.setCredential(credentialOf(Constants.ADMIN_ACCOUNT));
        request.setSettings(mySettings);
        final SettingResponse deleteResponse = bean.settings(request);
        assertTrue(deleteResponse.isOk());
        assertEquals(StandardSetting.values().length, deleteResponse.getSettings().size());
    }

    @Test
    void testDeletingStandardSetting() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put(StandardSetting.EDS_LOCALE.getKey(), null);
        request.setSettings(mySettings);

        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), response.getReturnCode());
        assertEquals("The value for the key 'eds.system.locale' is undefined.", response.getReturnMessage());
    }

    @Test
    void testSetCryptoAlgorithms() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request1 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final SettingResponse response1 = bean.settings(request1);
        assertTrue(response1.isOk());

        final Map<String, String> mySettings = prepareSettingsMap(response1);

        final SettingRequest request2 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        request2.setSettings(mySettings);
        final SettingResponse response2 = bean.settings(request2);
        assertTrue(response2.isOk());
    }

    @Test
    void testSetInvalidSymmetricAlgorithm() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request1 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final SettingResponse response1 = bean.settings(request1);
        assertTrue(response1.isOk());

        final Map<String, String> mySettings = response1.getSettings();
        mySettings.put(StandardSetting.SYMMETRIC_ALGORITHM.getKey(), KeyAlgorithm.MLKEM1024_P384.name());

        final SettingRequest request2 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        request2.setSettings(mySettings);

        final SettingResponse response2 = bean.settings(request2);
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), response2.getReturnCode());
        assertEquals("Unsupported Crypto Algorithm for 'eds.crypto.symmetric.algorithm'.", response2.getReturnMessage());
    }

    @Test
    void testSetInvalidHashAlgorithm() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request1 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final SettingResponse response1 = bean.settings(request1);
        assertTrue(response1.isOk());

        final Map<String, String> mySettings = response1.getSettings();
        mySettings.put(StandardSetting.HASH_ALGORITHM.getKey(), "SHA_256");

        final SettingRequest request2 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        request2.setSettings(mySettings);

        final SettingResponse response2 = bean.settings(request2);
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), response2.getReturnCode());
        assertEquals("Unsupported Crypto Algorithm for 'eds.crypto.hash.algorithm'.", response2.getReturnMessage());
    }

    @Test
    void testTheCharacterSetIsNoLongerASetting() {
        // Ticket 14: another character set made every stored key unreadable.
        // The old key is now a custom setting, which EDS doesn't read.
        final Settings settings = newSettings();
        final ManagementBean bean = prepareManagementBean(settings);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> newSettings = new HashMap<>();
        newSettings.put("eds.system.charset", "UTF-16");
        request.setSettings(newSettings);

        assertTrue(bean.settings(request).isOk());
        assertEquals("UTF-8", settings.getCharset().name());
        // The administrator still gets in, with the same passphrase.
        assertTrue(bean.settings(prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT)).isOk());
    }

    @Test
    void testReadinessIsNotChangedWithTheRequest() {
        // Ticket 14: set to false, every request was refused until a restart.
        final Settings settings = newSettings();
        final ManagementBean bean = prepareManagementBean(settings);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> newSettings = new HashMap<>();
        newSettings.put(StandardSetting.IS_READY.getKey(), "false");
        request.setSettings(newSettings);

        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), response.getReturnCode());
        assertEquals("The setting eds.is.ready may not be changed with this request.", response.getReturnMessage());
        assertTrue(settings.isReady());

        // Nor with blanks around the key, which is stored trimmed (the review).
        newSettings.clear();
        newSettings.put(" " + StandardSetting.IS_READY.getKey() + " ", "false");
        request.setSettings(newSettings);
        request.setCredential(credentialOf(Constants.ADMIN_ACCOUNT));
        final SettingResponse padded = bean.settings(request);
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), padded.getReturnCode());
        assertEquals("The setting eds.is.ready may not be changed with this request.", padded.getReturnMessage());
        assertTrue(settings.isReady());
        assertTrue(bean.settings(prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT)).isOk());
    }

    @Test
    void testSanityInterval() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request1 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final SettingResponse response1 = bean.settings(request1);
        assertTrue(response1.isOk());

        final Map<String, String> mySettings = response1.getSettings();
        mySettings.put(StandardSetting.SANITY_INTERVAL.getKey(), "7");
        mySettings.put(StandardSetting.SANITY_STARTUP.getKey(), "nope");

        final SettingRequest request2 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        request2.setSettings(mySettings);
        final SettingResponse response2 = bean.settings(request2);
        assertTrue(response2.isOk());

        mySettings.put(StandardSetting.SANITY_INTERVAL.getKey(), "weekly");
        final SettingRequest request3 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        request3.setSettings(mySettings);

        final SettingResponse response3 = bean.settings(request3);
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), response3.getReturnCode());
        assertEquals("Invalid Integer value for 'SANITY_INTERVAL'.", response3.getReturnMessage());
    }

    @Test
    void testUpdateMasterKeyUrlSetting() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> newSettings = new HashMap<>();
        newSettings.put(StandardSetting.MASTERKEY_URL.getKey(), "https://cool.url/to/new/key");
        request.setSettings(newSettings);

        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), response.getReturnCode());
        assertEquals("The setting eds.masterkey.url may not be changed with this request.", response.getReturnMessage());
    }

    @Test
    void testStoneName() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final String key = StandardSetting.STONE_NAME.getKey();

        assertEquals(ReturnCode.SUCCESS.getCode(), change(bean, key, "Test Stone").getReturnCode());
        final SettingResponse tooLong = change(bean, key, "x".repeat(Constants.MAX_STONE_NAME_LENGTH + 1));
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), tooLong.getReturnCode());
        assertEquals("The value for 'eds.stone.name' has at most 64 characters.", tooLong.getReturnMessage());

        // A stone needn't have a name, so the name can be removed again.
        assertEquals(ReturnCode.SUCCESS.getCode(), change(bean, key, "").getReturnCode());
    }

    @Test
    void testStatusIntervals() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final String load = StandardSetting.STATUS_LOAD_INTERVAL.getKey();
        final String inventory = StandardSetting.STATUS_INVENTORY_INTERVAL.getKey();

        assertEquals(ReturnCode.SUCCESS.getCode(), change(bean, load, "10").getReturnCode());
        final SettingResponse tooOften = change(bean, load, "3601");
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), tooOften.getReturnCode());
        assertEquals("The value for 'eds.status.load.interval.seconds' must be from 0 to 3600.", tooOften.getReturnMessage());
        // More digits than an int has are refused, not parsed.
        assertEquals("The value for 'eds.status.load.interval.seconds' must be from 0 to 3600.", change(bean, load, "12345678901").getReturnMessage());
        assertEquals(ReturnCode.SUCCESS.getCode(), change(bean, load, StandardSetting.STATUS_LOAD_INTERVAL.getValue()).getReturnCode());

        assertEquals(ReturnCode.SUCCESS.getCode(), change(bean, inventory, "120").getReturnCode());
        final SettingResponse never = change(bean, inventory, "0");
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), never.getReturnCode());
        assertEquals("The value for 'eds.status.inventory.interval.minutes' must be from 1 to 10080.", never.getReturnMessage());
        assertEquals(ReturnCode.SUCCESS.getCode(), change(bean, inventory, StandardSetting.STATUS_INVENTORY_INTERVAL.getValue()).getReturnCode());
    }

    @Test
    void testCircleSecretRotationSettings() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final String days = StandardSetting.CIRCLE_SECRET_ROTATION_DAYS.getKey();
        final String threads = StandardSetting.CIRCLE_SECRET_ROTATION_THREADS.getKey();

        assertEquals(ReturnCode.SUCCESS.getCode(), change(bean, days, "0").getReturnCode());
        assertEquals("The value for 'eds.circle.secret.rotation.days' must be from 0 to 36500.", change(bean, days, "36501").getReturnMessage());
        assertEquals(ReturnCode.SUCCESS.getCode(), change(bean, days, StandardSetting.CIRCLE_SECRET_ROTATION_DAYS.getValue()).getReturnCode());

        assertEquals(ReturnCode.SUCCESS.getCode(), change(bean, threads, "8").getReturnCode());
        assertEquals("The value for 'eds.circle.secret.rotation.threads' must be from 1 to 64.", change(bean, threads, "0").getReturnMessage());
        assertEquals(ReturnCode.SUCCESS.getCode(), change(bean, threads, StandardSetting.CIRCLE_SECRET_ROTATION_THREADS.getValue()).getReturnCode());
    }

    @Test
    void testIterationsFitAnInt() {
        final ManagementBean bean = prepareManagementBean(newSettings());

        final SettingResponse response = change(bean, StandardSetting.PBE_ITERATIONS.getKey(), "12345678901");
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), response.getReturnCode());
        assertTrue(response.getReturnMessage().endsWith("to 999999999."), response.getReturnMessage());
    }

    private SettingResponse change(final ManagementBean bean, final String key, final String value) {
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> settings = new HashMap<>();
        settings.put(key, value);
        request.setSettings(settings);

        return bean.settings(request);
    }

    private static Map<String, String> prepareSettingsMap(final SettingResponse response) {
        final Map<String, String> mySettings = response.getSettings();
        mySettings.put(StandardSetting.SYMMETRIC_ALGORITHM.getKey(), KeyAlgorithm.AES_GCM_256.name());
        mySettings.put(StandardSetting.ASYMMETRIC_ALGORITHM.getKey(), KeyAlgorithm.MLKEM1024_P384.name());
        mySettings.put(StandardSetting.PBE_ALGORITHM.getKey(), KeyAlgorithm.PBE_GCM_256.name());
        mySettings.put(StandardSetting.HASH_ALGORITHM.getKey(), HashAlgorithm.SHA_384.name());
        mySettings.put(StandardSetting.INVITATION_TIMEOUT.getKey(), "24");

        return mySettings;
    }
}
