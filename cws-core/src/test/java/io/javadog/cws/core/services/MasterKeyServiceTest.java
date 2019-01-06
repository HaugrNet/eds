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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

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
import org.junit.Test;

import javax.persistence.Query;
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
 * @author Kim Jensen
 * @since  CWS 1.1
 */
public final class MasterKeyServiceTest extends DatabaseSetup {

    @Test
    public void testUpdateMasterKeyWithNullRequest() {
        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest request = null;

        final MasterKeyResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("Cannot process the request, the given data is invalid."));
    }

    @Test
    public void testUpdateMasterKeyWithEmptyRequest() {
        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest request = new MasterKeyRequest();

        final MasterKeyResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("Cannot process the request, the given data is invalid."));
    }

    @Test
    public void testUpdateMasterKeyAsMember() {
        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, MEMBER_1);
        request.setSecret("New MasterKey".getBytes(Charset.defaultCharset()));

        final MasterKeyResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.AUTHENTICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("Given Account is not permitted to perform this request."));
    }

    @Test
    public void testUpdateMasterKeyAsAdminWithWrongCredentials() {
        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCredential("root".getBytes(Charset.defaultCharset()));
        request.setSecret("New MasterKey".getBytes(Charset.defaultCharset()));

        final MasterKeyResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.AUTHENTICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("Invalid credentials."));
    }

    @Test
    public void testUpdatingMasterKeyZeroMembers() {
        // Before starting, all member accounts must be removed
        final List<MemberEntity> members = dao.findAllAscending(MemberEntity.class, "id");
        for (final MemberEntity member : members) {
            dao.delete(member);
        }

        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(request.getCredential());
        final MasterKeyResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("MasterKey unlocked."));
    }

    @Test
    public void testUpdatingMasterKeyAdminOnly() throws IOException {
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
        assertThat(urlResponse.getReturnMessage(), is("MasterKey updated."));
        assertThat(urlResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));

        final MasterKeyRequest secretRequest = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        secretRequest.setSecret("MasterKey".getBytes(Charset.defaultCharset()));

        final MasterKeyResponse secretResponse = service.perform(secretRequest);
        assertThat(secretResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(secretResponse.getReturnMessage(), is("MasterKey updated."));

        // Before we're done - set the MasterKey back to the default, which is
        // using the System Administrator account name as secret.
        final MasterKeyRequest resetRequest = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        resetRequest.setSecret(resetRequest.getCredential());
        final MasterKeyResponse resetResponse = service.perform(resetRequest);
        assertThat(resetResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(resetResponse.getReturnMessage(), is("MasterKey updated."));
    }

    @Test
    public void testStartingMasterKeyWithURLFailed() {
        final String file = tempDir() + "not_existing_file.bin";
        prepareCause(CWSException.class, ReturnCode.NETWORK_ERROR, file + " (No such file or directory)");

        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.MASTERKEY_URL.getKey(), "file://" + file);
        assertThat(mySettings.getMasterKeyURL(), is(not(settings.getMasterKeyURL())));

        newMasterKey(mySettings);
    }

    /**
     * Testing that the MasterKey from file, where the file has the same content
     * as the normal, unconfigured MasterKey, is the same as the default. and
     * that a different content file will have different key.
     *
     * @throws IOException If unable to write to the MasterKey file
     */
    @Test
    public void testStartingMasterKeyWithURLSuccess() throws IOException {
        final String file = tempDir() + "masterKey.bin";
        final MasterKey defaultMasterKey = MasterKey.getInstance(settings);
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.MASTERKEY_URL.getKey(), "file://" + file);

        Files.write(Paths.get(file), Constants.ADMIN_ACCOUNT.getBytes(settings.getCharset()));
        final MasterKey masterKey1 = newMasterKey(mySettings);
        assertThat(defaultMasterKey.getKey().getKey(), is(masterKey1.getKey().getKey()));

        Files.write(Paths.get(file), UUID.randomUUID().toString().getBytes(settings.getCharset()));
        final MasterKey masterKey2 = newMasterKey(mySettings);
        assertThat(defaultMasterKey.getKey().getKey(), is(not(masterKey2.getKey().getKey())));
    }

    @Test
    public void testUpdateMasterKeyWhenMembersExist() {
        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret("MasterKey".getBytes(Charset.defaultCharset()));

        final MasterKeyResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ILLEGAL_ACTION.getCode()));
        assertThat(response.getReturnMessage(), is("Cannot alter the MasterKey, as Member Accounts exists."));
    }

    @Test
    public void testUpdateMasterKeyToCurrent() {
        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(request.getCredential());

        final MasterKeyResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("MasterKey unlocked."));
    }

    @Test
    public void testUpdateMasterKeyWithUnreachabledURL() {
        final String path = tempDir() + "not_existing_file.bin";
        prepareCause(CWSException.class, ReturnCode.NETWORK_ERROR, path + " (No such file or directory)");

        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setUrl("file://" + path);

        assertThat(request.validate().isEmpty(), is(true));
        service.perform(request);
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
}
