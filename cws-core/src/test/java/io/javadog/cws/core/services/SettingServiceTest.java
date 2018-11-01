/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.MasterKeyRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.MasterKeyResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.AuthorizationException;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.entities.MemberEntity;
import org.junit.Test;

import javax.persistence.Query;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>This Test Class, is testing the following Service Classes in one, as they
 * are all fairly small but also connected.</p>
 *
 * <ul>
 *   <li>MasterKeyService</li>
 *   <li>SettingService</li>
 * </ul>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SettingServiceTest extends DatabaseSetup {

    @Test
    public void testCreatingAdmin() {
        // For most tests we need to have the Admin account present, so by
        // default it already exists. So, to test that we actually *can* create
        // the Admin Account, we first have to delete it from the DB, which
        // we're doing here. Since each test is running within a transaction,
        // and they are rolled back after completion, this should not disturb
        // other tests.
        final Query query = entityManager.createQuery("delete from MemberEntity where id = :id");
        query.setParameter("id", 1L);
        query.executeUpdate();

        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(StandardSetting.values().length));
    }

    @Test
    public void testNonAdminRequest() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING,
                "Cannot complete this request, as it is only allowed for the System Administrator.");

        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, MEMBER_1);
        assertThat(request.getAccountName(), is(not(Constants.ADMIN_ACCOUNT)));

        service.perform(request);
    }

    @Test
    public void testInvokingRequestWithNullSettingsList() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(StandardSetting.values().length));
    }

    @Test
    public void testInvokingRequestWithEmptySettings() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        request.setSettings(mySettings);

        final SettingResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(StandardSetting.values().length));
    }

    @Test
    public void testInvokingRequestWithNullKey() {
        prepareCause(CWSException.class, ReturnCode.SETTING_WARNING, "Setting Keys may neither be null nor empty.");
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put(null, "NullKey");
        request.setSettings(mySettings);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testInvokingRequestWithEmptyKey() {
        prepareCause(CWSException.class, ReturnCode.SETTING_WARNING, "Setting Keys may neither be null nor empty.");
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("", "EmptyKey");
        request.setSettings(mySettings);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    /**
     * <p>Certain values in the CWS Settings are considered critical, meaning
     * that they can only be updated if no accounts, except the system
     * administrator, exists. However, the problem with these tests is that they
     * need to alter existing data, meaning that the underlying database may
     * have a lock set, which will affect other tests.</p>
     */
    @Test
    public void testUpdatingCriticalValues() {
        // Before starting, all member accounts must be removed
        final List<MemberEntity> members = dao.findAllAscending(MemberEntity.class, "id");
        for (final MemberEntity member : members) {
            dao.delete(member);
        }

        prepareCause(CWSException.class, ReturnCode.SETTING_WARNING, "Invalid Integer value for 'PBE_ITERATIONS'.");

        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final SettingResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));

        final Map<String, String> mySettings = new HashMap<>(response.getSettings());
        mySettings.put(StandardSetting.CWS_SALT.getKey(), "new SALT");
        mySettings.put(StandardSetting.PBE_ITERATIONS.getKey(), "100000");
        request.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));
        request.setSettings(mySettings);
        final SettingResponse update = service.perform(request);
        assertThat(update.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));

        // Running a verification check, to ensure that the System Administrator
        // can still access the system, after the SALT was updated.
        final SettingRequest checkRequest = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final SettingResponse checkResponse = service.perform(checkRequest);
        assertThat(checkResponse.isOk(), is(true));

        // Finally the negative test, seeing what happens if an invalid PBE
        // iteration count is defined
        mySettings.put(StandardSetting.PBE_ITERATIONS.getKey(), "-100000");
        final SettingRequest negativeRequest = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        negativeRequest.setSettings(mySettings);
        service.perform(negativeRequest);
    }

    @Test
    public void testInvokingRequestUpdateExistingSetting() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        // First invocation, retrieving the list of current values so we can
        // check that it is being updated
        final SettingResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(StandardSetting.values().length));
        assertThat(response.getSettings().get(StandardSetting.CWS_CHARSET.getKey()), is("UTF-8"));

        // The internal collection used is unmodifiable. So we simply copy the
        // list from the response and update one of the existing values
        final Map<String, String> mySettings = new HashMap<>(response.getSettings());
        mySettings.put(StandardSetting.CWS_CHARSET.getKey(), "ISO-8859-15");
        request.setSettings(mySettings);
        request.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));

        final SettingResponse update = service.perform(request);
        assertThat(update.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(update.getReturnMessage(), is("Ok"));
        assertThat(update.getSettings().size(), is(StandardSetting.values().length));
        assertThat(update.getSettings().get(StandardSetting.CWS_CHARSET.getKey()), is("ISO-8859-15"));
    }

    @Test
    public void testAddingAndRemovingCustomSetting() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("my.setting.key", "value");
        request.setSettings(mySettings);

        final SettingResponse update = service.perform(request);
        assertThat(update.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(update.getReturnMessage(), is("Ok"));
        assertThat(update.getSettings().size(), is(StandardSetting.values().length + 1));
    }

    @Test
    public void testInvokingRequestUpdateNotAllowedExistingSetting() {
        prepareCause(CWSException.class, ReturnCode.SETTING_WARNING, "The setting cws.system.salt may not be overwritten.");
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("cws.system.salt", "Enabling Kill Switch");
        assertThat(mySettings.get("cws.system.salt"), is("Enabling Kill Switch"));
        request.setSettings(mySettings);

        service.perform(request);
    }

    @Test
    public void testInvokingRequestAddNewSetting() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("cws.test.setting", "Setting Value");
        request.setSettings(mySettings);

        final SettingResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(StandardSetting.values().length + 1));
        assertThat(response.getSettings().get("cws.test.setting"), is("Setting Value"));
    }

    @Test
    public void testDeletingCustomSetting() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("cws.test.setting", "Test Value");
        request.setSettings(mySettings);
        assertThat(request.validate().isEmpty(), is(true));

        final SettingResponse response = service.perform(request);
        assertThat(response.isOk(), is(true));
        assertThat(response.getSettings().size(), is(StandardSetting.values().length + 1));

        mySettings.put("cws.test.setting", null);
        request.setCredential(crypto.stringToBytes(Constants.ADMIN_ACCOUNT));
        request.setSettings(mySettings);
        final SettingResponse deleteResponse = service.perform(request);
        assertThat(deleteResponse.isOk(), is(true));
        assertThat(deleteResponse.getSettings().size(), is(StandardSetting.values().length));
    }

    @Test
    public void testDeletingStandardSetting() {
        prepareCause(CWSException.class, ReturnCode.SETTING_WARNING, "The value for the key 'cws.system.charset' is undefined.");
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put(StandardSetting.CWS_CHARSET.getKey(), null);
        request.setSettings(mySettings);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testSetCryptoAlgorithms() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request1 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final SettingResponse response1 = service.perform(request1);
        assertThat(response1.isOk(), is(true));

        final Map<String, String> mySettings = response1.getSettings();
        mySettings.put(StandardSetting.SYMMETRIC_ALGORITHM.getKey(), KeyAlgorithm.AES_CBC_192.name());
        mySettings.put(StandardSetting.ASYMMETRIC_ALGORITHM.getKey(), KeyAlgorithm.RSA_8192.name());
        mySettings.put(StandardSetting.SIGNATURE_ALGORITHM.getKey(), KeyAlgorithm.SHA_256.name());
        mySettings.put(StandardSetting.PBE_ALGORITHM.getKey(), KeyAlgorithm.PBE_192.name());
        mySettings.put(StandardSetting.HASH_ALGORITHM.getKey(), KeyAlgorithm.SHA_256.name());

        final SettingRequest request2 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        request2.setSettings(mySettings);
        final SettingResponse response2 = service.perform(request2);
        assertThat(response2.isOk(), is(true));
    }

    @Test
    public void testSetInvalidSymmetricAlgorithm() {
        prepareCause(CWSException.class, ReturnCode.SETTING_WARNING, "Unsupported Crypto Algorithm for 'cws.crypto.symmetric.algorithm'.");
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request1 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final SettingResponse response1 = service.perform(request1);
        assertThat(response1.isOk(), is(true));

        final Map<String, String> mySettings = response1.getSettings();
        mySettings.put(StandardSetting.SYMMETRIC_ALGORITHM.getKey(), KeyAlgorithm.RSA_8192.name());

        final SettingRequest request2 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        request2.setSettings(mySettings);
        service.perform(request2);
    }

    @Test
    public void testInvalidCharset() {
        prepareCause(CWSException.class, ReturnCode.SETTING_WARNING, "Invalid Character set value for 'cws.system.charset'.");
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> newSettings = new HashMap<>();
        newSettings.put(StandardSetting.CWS_CHARSET.getKey(), "UTF-9");
        request.setSettings(newSettings);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testSanityInterval() {
        prepareCause(CWSException.class, ReturnCode.SETTING_WARNING, "Invalid Integer value for 'SANITY_INTERVAL'.");
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request1 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final SettingResponse response1 = service.perform(request1);
        assertThat(response1.isOk(), is(true));

        final Map<String, String> mySettings = response1.getSettings();
        mySettings.put(StandardSetting.SANITY_INTERVAL.getKey(), "7");
        mySettings.put(StandardSetting.SANITY_STARTUP.getKey(), "nope");

        final SettingRequest request2 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        request2.setSettings(mySettings);
        final SettingResponse response2 = service.perform(request2);
        assertThat(response2.isOk(), is(true));

        mySettings.put(StandardSetting.SANITY_INTERVAL.getKey(), "weekly");
        final SettingRequest request3 = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        request3.setSettings(mySettings);
        service.perform(request3);
        assertThat(response1.isOk(), is(true));
    }

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
    public void testUpdatingMasterKeyAdminOnly() {
        // Before starting, all member accounts must be removed
        final List<MemberEntity> members = dao.findAllAscending(MemberEntity.class, "id");
        for (final MemberEntity member : members) {
            if (!Constants.ADMIN_ACCOUNT.equals(member.getName())) {
                dao.delete(member);
            }
        }

        final MasterKeyService service = new MasterKeyService(settings, entityManager);
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret("MasterKey".getBytes(Charset.defaultCharset()));

        final MasterKeyResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("MasterKey updated."));

        // Before we're done - set the MasterKey back to the default, which is
        // using the System Administrator account name as secret.
        final MasterKeyRequest resetRequest = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        resetRequest.setSecret(resetRequest.getCredential());
        final MasterKeyResponse resetResponse = service.perform(resetRequest);
        assertThat(resetResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(resetResponse.getReturnMessage(), is("MasterKey updated."));
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
}
