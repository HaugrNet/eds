/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
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
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.AuthorizationException;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.DatabaseSetup;
import org.junit.Test;

import javax.persistence.Query;
import java.util.HashMap;
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

        final SettingService service = new SettingService(new Settings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(12));
    }

    @Test
    public void testNonAdminRequest() {
        prepareCause(AuthorizationException.class, ReturnCode.AUTHORIZATION_WARNING,
                "Cannot complete this request, as it is only allowed for the System Administrator.");

        final SettingService service = new SettingService(new Settings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, MEMBER_1);
        assertThat(request.getAccountName(), is(not(Constants.ADMIN_ACCOUNT)));

        service.perform(request);
    }

    @Test
    public void testInvokingRequestWithNullSettingsList() {
        final SettingService service = new SettingService(new Settings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(12));
    }

    @Test
    public void testInvokingRequestWithEmptySettings() {
        final SettingService service = new SettingService(new Settings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        request.setSettings(mySettings);

        final SettingResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(12));
    }

    @Test
    public void testInvokingRequestUpdateExistingSetting() {
        final SettingService service = new SettingService(new Settings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        // First invocation, retrieving the list of current values so we can
        // check that it is being updated
        final SettingResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(12));
        assertThat(response.getSettings().get(Settings.CWS_CHARSET), is("UTF-8"));

        // The internal collection used is unmodifiable. So we simply copy the
        // list from the response and update one of the existing values
        final Map<String, String> mySettings = new HashMap<>(response.getSettings());
        mySettings.put(Settings.CWS_CHARSET, "ISO-8859-15");
        request.setSettings(mySettings);

        final SettingResponse update = service.perform(request);
        assertThat(update.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(update.getReturnMessage(), is("Ok"));
        assertThat(update.getSettings().size(), is(12));
        assertThat(update.getSettings().get(Settings.CWS_CHARSET), is("ISO-8859-15"));
    }

    @Test
    public void testInvokingRequestUpdateNotAllowedExistingSetting() {
        prepareCause(CWSException.class, ReturnCode.PROPERTY_ERROR, "The setting cws.system.salt may not be overwritten.");
        final SettingService service = new SettingService(new Settings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("cws.system.salt", "Enabling Kill Switch");
        assertThat(mySettings.get("cws.system.salt"), is("Enabling Kill Switch"));
        request.setSettings(mySettings);

        service.perform(request);
    }

    @Test
    public void testInvokingRequestAddNewSetting() {
        final SettingService service = new SettingService(new Settings(), entityManager);
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        final Map<String, String> mySettings = new HashMap<>();
        mySettings.put("cws.test.setting", "Setting Value");
        request.setSettings(mySettings);

        final SettingResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(13));
        assertThat(response.getSettings().get("cws.test.setting"), is("Setting Value"));
    }
}
