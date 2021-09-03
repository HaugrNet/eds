/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2021, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.cws.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.common.ReturnCode;
import net.haugr.cws.api.requests.MasterKeyRequest;
import net.haugr.cws.api.responses.MasterKeyResponse;
import net.haugr.cws.core.enums.StandardSetting;
import net.haugr.cws.core.exceptions.CWSException;
import net.haugr.cws.core.jce.MasterKey;
import net.haugr.cws.core.model.Settings;
import net.haugr.cws.core.model.entities.MemberEntity;
import net.haugr.cws.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

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
@DisabledOnOs(value = {OS.WINDOWS, OS.MAC})
final class ManagementBeanMasterKeyTest extends DatabaseSetup {

    @Test
    void testUpdateMasterKeyWithNullRequest() {
        final ManagementBean bean = prepareManagementBean();

        final MasterKeyResponse response = bean.masterKey(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Cannot Process a NULL Object.", response.getReturnMessage());
    }

    @Test
    void testUpdateMasterKeyWithEmptyRequest() {
        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = new MasterKeyRequest();

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Request Object contained errors:\n" +
                "Key: credential, Error: The Session (Credential) is missing.\n" +
                "Key: secret, Error: Either the secret or the URL must be given to alter the MasterKey.\n" +
                "Key: url, Error: Either the secret or the URL must be given to alter the MasterKey.", response.getReturnMessage());
    }

    @Test
    void testUpdateMasterKeyAsMember() {
        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, MEMBER_1);
        request.setSecret("New MasterKey".getBytes(Charset.defaultCharset()));

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Given Account is not permitted to perform this request.", response.getReturnMessage());
    }

    @Test
    void testUpdateMasterKeyAsAdminWithWrongCredentials() {
        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCredential("root".getBytes(Charset.defaultCharset()));
        request.setSecret("New MasterKey".getBytes(Charset.defaultCharset()));

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Invalid credentials.", response.getReturnMessage());
    }

    @Test
    void testUpdatingMasterKeySecretZeroMembers() {
        // Before starting, all member accounts must be removed
        final List<MemberEntity> members = dao.findAllAscending(MemberEntity.class, "id");
        for (final MemberEntity member : members) {
            dao.delete(member);
        }

        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(request.getCredential());
        final MasterKeyResponse response = bean.masterKey(request);
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

        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setUrl(file);

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("MasterKey updated.", response.getReturnMessage());

        revertMasterKeyToOriginal(bean);
    }

    @Test
    void testUpdatingMasterKeyAdminOnly() throws IOException {
        // Before starting, all member accounts must be removed, as well as the
        // MasterKey Setting.
        deleteNonAdminMembers();
        assertEquals(1, deleteMasterKeySetting());

        final String path = tempDir() + "secret_master_key.bin";
        Files.write(Paths.get(path), generateData(8192));

        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest urlRequest = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        urlRequest.setUrl("file://" + path);

        final MasterKeyResponse urlResponse = bean.masterKey(urlRequest);
        assertEquals("MasterKey updated.", urlResponse.getReturnMessage());
        assertEquals(ReturnCode.SUCCESS.getCode(), urlResponse.getReturnCode());

        final MasterKeyRequest secretRequest = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        secretRequest.setSecret("MasterKey".getBytes(Charset.defaultCharset()));

        final MasterKeyResponse secretResponse = bean.masterKey(secretRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), secretResponse.getReturnCode());
        assertEquals("MasterKey updated.", secretResponse.getReturnMessage());

        revertMasterKeyToOriginal(bean);
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
     * as the normal, un-configured MasterKey, is the same as the default. and
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
        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret("MasterKey".getBytes(Charset.defaultCharset()));

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), response.getReturnCode());
        assertEquals("Cannot alter the MasterKey, as Member Accounts exists.", response.getReturnMessage());
    }

    @Test
    void testUpdateMasterKeyToCurrent() {
        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(request.getCredential());

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("MasterKey unlocked.", response.getReturnMessage());
    }

    @Test
    void testUpdateMasterKeyWithUnreachableURL() {
        final String path = tempDir() + "not_existing_file.bin";
        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setUrl("file://" + path);

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.NETWORK_ERROR.getCode(), response.getReturnCode());
        assertEquals(path + " (No such file or directory)", response.getReturnMessage());
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

    private int deleteMasterKeySetting() {
        final String jql = "delete from SettingEntity s where s.name = :name";
        return entityManager
                .createQuery(jql)
                .setParameter("name", StandardSetting.MASTERKEY_URL.getKey())
                .executeUpdate();
    }

    /**
     * Reverts the MasterKey to the original or default MasterKey, since other
     * tests will otherwise fail.
     *
     * @param bean ManagementBean instance
     */
    private static void revertMasterKeyToOriginal(final ManagementBean bean) {
        final MasterKeyRequest resetRequest = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        resetRequest.setSecret(resetRequest.getCredential());

        final MasterKeyResponse resetResponse = bean.masterKey(resetRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), resetResponse.getReturnCode());
        assertEquals("MasterKey updated.", resetResponse.getReturnMessage());
    }
}
