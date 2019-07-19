/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
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

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.MasterKeyRequest;
import io.javadog.cws.api.responses.MasterKeyResponse;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.jce.MasterKey;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.MemberEntity;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import javax.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * <p>This Test Class, is testing the following Service Classes in one, as they
 * are all fairly small but also connected.</p>
 *
 * <ul>
 *   <li>MasterKeyService</li>
 * </ul>
 *
 * <p>Note that this test class is not working under Windows, as the expected
 * error messages are truncated and only contain a fraction of the information
 * as the Linux variant contains.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.1
 */
@DisabledOnOs(value = { OS.WINDOWS, OS.MAC })
final class MasterKeyServiceTest extends DatabaseSetup {

    @Test
    void testUpdateMasterKeyWithNullRequest() {
        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest request = null;

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Cannot Process a NULL Object.", cause.getMessage());
    }

    @Test
    void testUpdateMasterKeyWithEmptyRequest() {
        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest request = new MasterKeyRequest();

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Request Object contained errors:\n" +
                "Key: credential, Error: The Session (Credential) is missing.\n" +
                "Key: secret, Error: Either the secret or the URL must be given to alter the MasterKey.\n" +
                "Key: url, Error: Either the secret or the URL must be given to alter the MasterKey.", cause.getMessage());
    }

    @Test
    void testUpdateMasterKeyAsMember() {
        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, MEMBER_1);
        request.setSecret("New MasterKey".getBytes(Charset.defaultCharset()));

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.AUTHENTICATION_WARNING, cause.getReturnCode());
        assertEquals("Given Account is not permitted to perform this request.", cause.getMessage());
    }

    @Test
    void testUpdateMasterKeyAsAdminWithWrongCredentials() {
        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCredential("root".getBytes(Charset.defaultCharset()));
        request.setSecret("New MasterKey".getBytes(Charset.defaultCharset()));

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.AUTHENTICATION_WARNING, cause.getReturnCode());
        assertEquals("Invalid credentials.", cause.getMessage());
    }

    @Test
    void testUpdatingMasterKeySecretZeroMembers() {
        // Before starting, all member accounts must be removed
        final List<MemberEntity> members = dao.findAllAscending(MemberEntity.class, "id");
        for (final MemberEntity member : members) {
            dao.delete(member);
        }

        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(request.getCredential());
        final MasterKeyResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("MasterKey unlocked.", response.getReturnMessage());
    }

    @Test
    void testUpdatingMasterKeyUrlZeroMembers() throws IOException {
        // Before starting, all member accounts must be removed
        final String path = tempDir() + "secret_master_key.bin";
        final String file = "file://" + path;
        Files.write(Paths.get(path), generateData(8192));
        final List<MemberEntity> members = dao.findAllAscending(MemberEntity.class, "id");
        for (final MemberEntity member : members) {
            dao.delete(member);
        }

        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setUrl(file);

        final MasterKeyResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("MasterKey updated.", response.getReturnMessage());

        revertMasterKeyToOriginal(service);
    }

    @Test
    void testUpdatingMasterKeyAdminOnly() throws IOException {
        // Before starting, all member accounts must be removed, as well as the
        // MasterKey Setting.
        deleteNonAdminMembers();
        deleteMasterKeySetting();

        final String path = tempDir() + "secret_master_key.bin";
        Files.write(Paths.get(path), generateData(8192));

        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest urlRequest = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        urlRequest.setUrl("file://" + path);

        final MasterKeyResponse urlResponse = service.perform(urlRequest);
        assertEquals("MasterKey updated.", urlResponse.getReturnMessage());
        assertEquals(ReturnCode.SUCCESS.getCode(), urlResponse.getReturnCode());

        final MasterKeyRequest secretRequest = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        secretRequest.setSecret("MasterKey".getBytes(Charset.defaultCharset()));

        final MasterKeyResponse secretResponse = service.perform(secretRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), secretResponse.getReturnCode());
        assertEquals("MasterKey updated.", secretResponse.getReturnMessage());

        revertMasterKeyToOriginal(service);
    }

    @Test
    void testStartingMasterKeyWithURLFailed() {
        final String file = tempDir() + "not_existing_file.bin";
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.MASTERKEY_URL.getKey(), "file://" + file);
        assertNotEquals(settings.getMasterKeyURL(), mySettings.getMasterKeyURL());

        final CWSException cause = assertThrows(CWSException.class, () -> newMasterKey(mySettings));
        assertEquals(ReturnCode.NETWORK_ERROR, cause.getReturnCode());
        assertEquals(file + " (No such file or directory)", cause.getMessage());
    }

    /**
     * Testing that the MasterKey from file, where the file has the same content
     * as the normal, unconfigured MasterKey, is the same as the default. and
     * that a different content file will have different key.
     *
     * @throws IOException If unable to write to the MasterKey file
     */
    @Test
    void testStartingMasterKeyWithURLSuccess() throws IOException {
        final String file = tempDir() + "masterKey.bin";
        final MasterKey defaultMasterKey = MasterKey.getInstance(settings);
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.MASTERKEY_URL.getKey(), "file://" + file);

        Files.write(Paths.get(file), Constants.ADMIN_ACCOUNT.getBytes(settings.getCharset()));
        final MasterKey masterKey1 = newMasterKey(mySettings);
        assertEquals(masterKey1.getKey().getKey(), defaultMasterKey.getKey().getKey());

        Files.write(Paths.get(file), UUID.randomUUID().toString().getBytes(settings.getCharset()));
        final MasterKey masterKey2 = newMasterKey(mySettings);
        assertNotEquals(masterKey2.getKey().getKey(), defaultMasterKey.getKey().getKey());
    }

    @Test
    void testUpdateMasterKeyWhenMembersExist() {
        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret("MasterKey".getBytes(Charset.defaultCharset()));

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.ILLEGAL_ACTION, cause.getReturnCode());
        assertEquals("Cannot alter the MasterKey, as Member Accounts exists.", cause.getMessage());
    }

    @Test
    void testUpdateMasterKeyToCurrent() {
        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(request.getCredential());

        final MasterKeyResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("MasterKey unlocked.", response.getReturnMessage());
    }

    @Test
    void testUpdateMasterKeyWithUnreachabledURL() {
        final String path = tempDir() + "not_existing_file.bin";
        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setUrl("file://" + path);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.NETWORK_ERROR, cause.getReturnCode());
        assertEquals(path + " (No such file or directory)", cause.getMessage());
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private static String tempDir() {
        return System.getProperty("java.io.tmpdir") + File.separator;
    }

    private void deleteNonAdminMembers() {
        final List<MemberEntity> members = dao.findAllAscending(MemberEntity.class, "id");
        for (final MemberEntity member : members) {
            if (!Constants.ADMIN_ACCOUNT.equals(member.getName())) {
                dao.delete(member);
            }
        }
    }

    private void deleteMasterKeySetting() {
        final String jql = "delete from SettingEntity s where s.name = :name";
        final Query query = entityManager.createQuery(jql);
        query.setParameter("name", StandardSetting.MASTERKEY_URL.getKey());
        query.executeUpdate();
    }

    /**
     * Reverts the MasterKey to the original or default MasterKey, since other
     * tests will otherwise fail.
     *
     * @param service MasterKey Service instance
     */
    private static void revertMasterKeyToOriginal(final MasterKeyService service) {
        final MasterKeyRequest resetRequest = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        resetRequest.setSecret(resetRequest.getCredential());

        final MasterKeyResponse resetResponse = service.perform(resetRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), resetResponse.getReturnCode());
        assertEquals("MasterKey updated.", resetResponse.getReturnMessage());
    }
}
