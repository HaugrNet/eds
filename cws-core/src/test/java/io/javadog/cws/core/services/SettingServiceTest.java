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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.entities.MemberEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * @since CWS 1.0
 */
final class SettingServiceTest extends DatabaseSetup {

    @Test
    void testCreatingAdmin() {
        // For most tests we need to have the Admin account present, so by
        // default it already exists. So, to test that we actually *can* create
        // the Admin Account, we first have to delete it from the DB, which
        // we're doing here. Since each test is running within a transaction,
        // and they are rolled back after completion, this should not disturb
        // other tests.
        final int deleted = entityManager
                .createQuery("delete from MemberEntity where id = :id")
                .setParameter("id", 1L)
                .executeUpdate();
        assertEquals(1, deleted);

        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(StandardSetting.values().length, response.getSettings().size());
    }

    @Test
    void testNonAdminRequest() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, MEMBER_1);
        assertNotEquals(Constants.ADMIN_ACCOUNT, request.getAccountName());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("Cannot complete this request, as it is only allowed for the System Administrator.", cause.getMessage());
    }

    @Test
    void testInvokingRequestWithNullSettingsList() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(StandardSetting.values().length, response.getSettings().size());
    }

    @Test
    void testInvokingRequestWithEmptySettings() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        request.setSettings(mySettings);

        final SettingResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(StandardSetting.values().length, response.getSettings().size());
    }

    @Test
    void testInvokingRequestWithNullKey() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put(null, "NullKey");
        request.setSettings(mySettings);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.SETTING_WARNING, cause.getReturnCode());
        assertEquals("Setting Keys may neither be null nor empty.", cause.getMessage());
    }

    @Test
    void testInvokingRequestWithEmptyKey() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("", "EmptyKey");
        request.setSettings(mySettings);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.SETTING_WARNING, cause.getReturnCode());
        assertEquals("Setting Keys may neither be null nor empty.", cause.getMessage());
    }

    /**
     * <p>Certain values in the CWS Settings are considered critical, meaning
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

        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final SettingResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());

        final Map<String, String> mySettings = new HashMap<>(response.getSettings());
        mySettings.put(StandardSetting.CWS_SALT.getKey(), "new SALT");
        mySettings.put(StandardSetting.PBE_ITERATIONS.getKey(), "100000");
        request.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));
        request.setSettings(mySettings);
        final SettingResponse update = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), update.getReturnCode());

        // Running a verification check, to ensure that the System Administrator
        // can still access the system, after the SALT was updated.
        final SettingRequest checkRequest = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final SettingResponse checkResponse = service.perform(checkRequest);
        assertTrue(checkResponse.isOk());

        // Finally the negative test, seeing what happens if an invalid PBE
        // iteration count is defined
        mySettings.put(StandardSetting.PBE_ITERATIONS.getKey(), "-100000");
        final SettingRequest negativeRequest = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        negativeRequest.setSettings(mySettings);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(negativeRequest));
        assertEquals(ReturnCode.SETTING_WARNING, cause.getReturnCode());
        assertEquals("Invalid Integer value for 'PBE_ITERATIONS'.", cause.getMessage());
    }

    @Test
    void testInvokingRequestUpdateExistingSetting() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        // First invocation, retrieving the list of current values so we can
        // check that it is being updated
        final SettingResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(StandardSetting.values().length, response.getSettings().size());
        assertEquals("UTF-8", response.getSettings().get(StandardSetting.CWS_CHARSET.getKey()));

        // The internal collection used is unmodifiable. So we simply copy the
        // list from the response and update one of the existing values
        final Map<String, String> mySettings = new HashMap<>(response.getSettings());
        mySettings.put(StandardSetting.CWS_CHARSET.getKey(), "ISO-8859-15");
        request.setSettings(mySettings);
        request.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));

        final SettingResponse update = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), update.getReturnCode());
        assertEquals("Ok", update.getReturnMessage());
        assertEquals(StandardSetting.values().length, update.getSettings().size());
        assertEquals("ISO-8859-15", update.getSettings().get(StandardSetting.CWS_CHARSET.getKey()));
    }

    @Test
    void testAddingAndRemovingCustomSetting() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("my.setting.key", "value");
        request.setSettings(mySettings);

        final SettingResponse update = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), update.getReturnCode());
        assertEquals("Ok", update.getReturnMessage());
        assertEquals(StandardSetting.values().length + 1, update.getSettings().size());
    }

    @Test
    void testInvokingRequestUpdateNotAllowedExistingSetting() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("cws.system.salt", "Enabling Kill Switch");
        assertEquals("Enabling Kill Switch", mySettings.get("cws.system.salt"));
        request.setSettings(mySettings);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.SETTING_WARNING, cause.getReturnCode());
        assertEquals("The setting cws.system.salt may not be overwritten.", cause.getMessage());
    }

    @Test
    void testInvokingRequestAddNewSetting() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("cws.test.setting", "Setting Value");
        request.setSettings(mySettings);

        final SettingResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(StandardSetting.values().length + 1, response.getSettings().size());
        assertEquals("Setting Value", response.getSettings().get("cws.test.setting"));
    }

    @Test
    void testDeletingCustomSetting() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("cws.test.setting", "Test Value");
        request.setSettings(mySettings);
        assertTrue(request.validate().isEmpty());

        final SettingResponse response = service.perform(request);
        assertTrue(response.isOk());
        assertEquals(StandardSetting.values().length + 1, response.getSettings().size());

        mySettings.put("cws.test.setting", null);
        request.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));
        request.setSettings(mySettings);
        final SettingResponse deleteResponse = service.perform(request);
        assertTrue(deleteResponse.isOk());
        assertEquals(StandardSetting.values().length, deleteResponse.getSettings().size());
    }

    @Test
    void testDeletingStandardSetting() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put(StandardSetting.CWS_CHARSET.getKey(), null);
        request.setSettings(mySettings);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.SETTING_WARNING, cause.getReturnCode());
        assertEquals("The value for the key 'cws.system.charset' is undefined.", cause.getMessage());
    }

    @Test
    void testSetCryptoAlgorithms() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request1 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final SettingResponse response1 = service.perform(request1);
        assertTrue(response1.isOk());

        final Map<String, String> mySettings = response1.getSettings();
        mySettings.put(StandardSetting.SYMMETRIC_ALGORITHM.getKey(), KeyAlgorithm.AES_CBC_192.name());
        mySettings.put(StandardSetting.ASYMMETRIC_ALGORITHM.getKey(), KeyAlgorithm.RSA_8192.name());
        mySettings.put(StandardSetting.SIGNATURE_ALGORITHM.getKey(), KeyAlgorithm.SHA_256.name());
        mySettings.put(StandardSetting.PBE_ALGORITHM.getKey(), KeyAlgorithm.PBE_192.name());
        mySettings.put(StandardSetting.HASH_ALGORITHM.getKey(), KeyAlgorithm.SHA_256.name());

        final SettingRequest request2 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        request2.setSettings(mySettings);
        final SettingResponse response2 = service.perform(request2);
        assertTrue(response2.isOk());
    }

    @Test
    void testSetInvalidSymmetricAlgorithm() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request1 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final SettingResponse response1 = service.perform(request1);
        assertTrue(response1.isOk());

        final Map<String, String> mySettings = response1.getSettings();
        mySettings.put(StandardSetting.SYMMETRIC_ALGORITHM.getKey(), KeyAlgorithm.RSA_8192.name());

        final SettingRequest request2 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        request2.setSettings(mySettings);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request2));
        assertEquals(ReturnCode.SETTING_WARNING, cause.getReturnCode());
        assertEquals("Unsupported Crypto Algorithm for 'cws.crypto.symmetric.algorithm'.", cause.getMessage());
    }

    @Test
    void testInvalidCharset() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> newSettings = new HashMap<>();
        newSettings.put(StandardSetting.CWS_CHARSET.getKey(), "UTF-9");
        request.setSettings(newSettings);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.SETTING_WARNING, cause.getReturnCode());
        assertEquals("Invalid Character set value for 'cws.system.charset'.", cause.getMessage());
    }

    @Test
    void testSanityInterval() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request1 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final SettingResponse response1 = service.perform(request1);
        assertTrue(response1.isOk());

        final Map<String, String> mySettings = response1.getSettings();
        mySettings.put(StandardSetting.SANITY_INTERVAL.getKey(), "7");
        mySettings.put(StandardSetting.SANITY_STARTUP.getKey(), "nope");

        final SettingRequest request2 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        request2.setSettings(mySettings);
        final SettingResponse response2 = service.perform(request2);
        assertTrue(response2.isOk());

        mySettings.put(StandardSetting.SANITY_INTERVAL.getKey(), "weekly");
        final SettingRequest request3 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        request3.setSettings(mySettings);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request3));
        assertEquals(ReturnCode.SETTING_WARNING, cause.getReturnCode());
        assertEquals("Invalid Integer value for 'SANITY_INTERVAL'.", cause.getMessage());
    }

    @Test
    void testUpdateMasterKeyUrlSetting() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> newSettings = new HashMap<>();
        newSettings.put(StandardSetting.MASTERKEY_URL.getKey(), "https://cool.url/to/new/key");
        request.setSettings(newSettings);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.SETTING_WARNING, cause.getReturnCode());
        assertEquals("The setting cws.masterkey.url may not be changed with this request.", cause.getMessage());
    }
}
