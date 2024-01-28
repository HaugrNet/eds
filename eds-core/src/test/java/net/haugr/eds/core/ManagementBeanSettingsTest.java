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
package net.haugr.eds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.SettingRequest;
import net.haugr.eds.api.responses.SettingResponse;
import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.model.entities.MemberEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * <p>This Test Class, is testing the following Service Classes in one, as they
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
        // For most tests we need to have the Admin account present, so by
        // default it already exists. So, to test that we actually *can* create
        // the Admin Account, we first have to delete it from the DB, which
        // we're doing here. Since each test is running within a transaction,
        // and they are rolled back after completion, this should not disturb
        // other tests.
        final int deleted = entityManager
                .createQuery("delete from MemberEntity m where m.id = :id")
                .setParameter("id", 1L)
                .executeUpdate();
        assertEquals(1, deleted);

        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        Assertions.assertEquals(StandardSetting.values().length, response.getSettings().size());
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
     * administrator, exists. However, the problem with these tests is that they
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
        mySettings.put(StandardSetting.EDS_SALT.getKey(), "new SALT");
        mySettings.put(StandardSetting.PBE_ITERATIONS.getKey(), "100000");
        request.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));
        request.setSettings(mySettings);
        final SettingResponse update = bean.settings(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), update.getReturnCode());

        // Running a verification check, to ensure that the System Administrator
        // can still access the system, after the SALT was updated.
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
        assertEquals("UTF-8", response.getSettings().get(StandardSetting.EDS_CHARSET.getKey()));

        // The internal collection used is unmodifiable. So we simply copy the
        // list from the response and update one of the existing values
        final Map<String, String> mySettings = new HashMap<>(response.getSettings());
        mySettings.put(StandardSetting.EDS_CHARSET.getKey(), "ISO-8859-15");
        request.setSettings(mySettings);
        request.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));

        final SettingResponse update = bean.settings(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), update.getReturnCode());
        assertEquals("Ok", update.getReturnMessage());
        assertEquals(StandardSetting.values().length, update.getSettings().size());
        assertEquals("ISO-8859-15", update.getSettings().get(StandardSetting.EDS_CHARSET.getKey()));
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
    void testInvokingRequestUpdateNotAllowedExistingSetting() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("eds.system.salt", "Enabling Kill Switch");
        assertEquals("Enabling Kill Switch", mySettings.get("eds.system.salt"));
        request.setSettings(mySettings);

        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), response.getReturnCode());
        assertEquals("The setting eds.system.salt may not be overwritten.", response.getReturnMessage());
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
        request.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));
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
        mySettings.put(StandardSetting.EDS_CHARSET.getKey(), null);
        request.setSettings(mySettings);

        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), response.getReturnCode());
        assertEquals("The value for the key 'eds.system.charset' is undefined.", response.getReturnMessage());
    }

    @Test
    void testSetCryptoAlgorithms() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request1 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final SettingResponse response1 = bean.settings(request1);
        assertTrue(response1.isOk());

        final Map<String, String> mySettings = response1.getSettings();
        mySettings.put(StandardSetting.SYMMETRIC_ALGORITHM.getKey(), KeyAlgorithm.AES_CBC_192.name());
        mySettings.put(StandardSetting.ASYMMETRIC_ALGORITHM.getKey(), KeyAlgorithm.RSA_8192.name());
        mySettings.put(StandardSetting.SIGNATURE_ALGORITHM.getKey(), KeyAlgorithm.SHA_256.name());
        mySettings.put(StandardSetting.PBE_ALGORITHM.getKey(), KeyAlgorithm.PBE_CBC_192.name());
        mySettings.put(StandardSetting.HASH_ALGORITHM.getKey(), KeyAlgorithm.SHA_256.name());

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
        mySettings.put(StandardSetting.SYMMETRIC_ALGORITHM.getKey(), KeyAlgorithm.RSA_8192.name());

        final SettingRequest request2 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        request2.setSettings(mySettings);

        final SettingResponse response2 = bean.settings(request2);
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), response2.getReturnCode());
        assertEquals("Unsupported Crypto Algorithm for 'eds.crypto.symmetric.algorithm'.", response2.getReturnMessage());
    }

    @Test
    void testInvalidCharset() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> newSettings = new HashMap<>();
        newSettings.put(StandardSetting.EDS_CHARSET.getKey(), "UTF-9");
        request.setSettings(newSettings);

        final SettingResponse response = bean.settings(request);
        assertEquals(ReturnCode.SETTING_WARNING.getCode(), response.getReturnCode());
        assertEquals("Invalid Character set value for 'eds.system.charset'.", response.getReturnMessage());
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
}
