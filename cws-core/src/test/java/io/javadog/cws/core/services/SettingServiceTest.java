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
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.AuthorizationException;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.entities.MemberEntity;
import org.junit.Test;

import javax.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
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
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(13));
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
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(13));
    }

    @Test
    public void testInvokingRequestWithEmptySettings() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        request.setSettings(mySettings);

        final SettingResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(13));
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

    @Test
    public void testUpdatingSalt() {
        // Before starting, all member accounts must be removed
        final List<MemberEntity> members = dao.findAllAscending(MemberEntity.class, "id");
        for (final MemberEntity member : members) {
            dao.delete(member);
        }
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final SettingResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));

        final Map<String, String> mySettings = new HashMap<>(response.getSettings());
        mySettings.put(StandardSetting.CWS_SALT.getKey(), "new SALT");
        request.setSettings(mySettings);
        final SettingResponse update = service.perform(request);
        assertThat(update.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testInvokingRequestUpdateExistingSetting() {
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        // First invocation, retrieving the list of current values so we can
        // check that it is being updated
        final SettingResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(13));
        assertThat(response.getSettings().get(StandardSetting.CWS_CHARSET.getKey()), is("UTF-8"));

        // The internal collection used is unmodifiable. So we simply copy the
        // list from the response and update one of the existing values
        final Map<String, String> mySettings = new HashMap<>(response.getSettings());
        mySettings.put(StandardSetting.CWS_CHARSET.getKey(), "ISO-8859-15");
        request.setSettings(mySettings);

        final SettingResponse update = service.perform(request);
        assertThat(update.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(update.getReturnMessage(), is("Ok"));
        assertThat(update.getSettings().size(), is(13));
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
        assertThat(update.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(update.getReturnMessage(), is("Ok"));
        assertThat(update.getSettings().size(), is(14));
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
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(14));
        assertThat(response.getSettings().get("cws.test.setting"), is("Setting Value"));
    }

    @Test
    public void testAddSettingWithNullValue() {
        prepareCause(CWSException.class, ReturnCode.SETTING_WARNING, "Cannot add a setting without a value.");
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("cws.test.setting", null);
        request.setSettings(mySettings);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
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
        assertThat(response.getSettings().size(), is(14));

        mySettings.put("cws.test.setting", null);
        request.setSettings(mySettings);
        final SettingResponse deleteResponse = service.perform(request);
        assertThat(deleteResponse.isOk(), is(true));
        assertThat(deleteResponse.getSettings().size(), is(13));
    }

    @Test
    public void testDeletingStandardSetting() {
        prepareCause(CWSException.class, ReturnCode.SETTING_WARNING, "It is not allowed to delete standard settings.");
        final SettingService service = new SettingService(newSettings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put(StandardSetting.CWS_CHARSET.getKey(), null);
        request.setSettings(mySettings);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }
}
