package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

import javax.persistence.Query;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
        final Query query = entityManager.createQuery("delete from MemberEntity");
        query.executeUpdate();

        final SettingService service = new SettingService(new Settings(), entityManager);
        final SettingRequest request = prepareRequest();

        final SettingResponse response = service.process(request);
        assertThat(response.getReturnCode(), is(0));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(9));
    }

    @Test(expected = CWSException.class)
    public void testNonAdminRequest() {
        final SettingService service = new SettingService(new Settings(), entityManager);
        final SettingRequest request = prepareRequest();
        request.setName("not admin");

        service.process(request);
    }

    @Test
    public void testInvokingRequestWithNullSettingsList() {
        final SettingService service = new SettingService(new Settings(), entityManager);
        final SettingRequest request = prepareRequest();

        final SettingResponse response = service.process(request);
        assertThat(response.getReturnCode(), is(0));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(9));
    }

    @Test
    public void testInvokingRequestWithEmptySettings() {
        final SettingService service = new SettingService(new Settings(), entityManager);
        final SettingRequest request = prepareRequest();
        final Map<String, String> settings = new HashMap<>();
        request.setSettings(settings);

        final SettingResponse response = service.process(request);
        assertThat(response.getReturnCode(), is(0));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(9));
    }

    @Test
    public void testInvokingRequestUpdateExistingSetting() {
        final SettingService service = new SettingService(new Settings(), entityManager);
        final SettingRequest request = prepareRequest();

        // First invocation, retrieving the list of current values so we can
        // check that it is being updated
        final SettingResponse response = service.process(request);
        assertThat(response.getReturnCode(), is(0));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(9));
        assertThat(response.getSettings().get("cws.crypto.symmetric.keylength"), is("128"));

        // The internal collection used is unmodifiable. So we simply copy the
        // list from the response and update one of the existing values
        final Map<String, String> settings = new HashMap<>(response.getSettings());
        settings.put("cws.crypto.symmetric.keylength", "256");
        request.setSettings(settings);

        final SettingResponse update = service.process(request);
        assertThat(update.getReturnCode(), is(0));
        assertThat(update.getReturnMessage(), is("Ok"));
        assertThat(update.getSettings().size(), is(9));
        assertThat(update.getSettings().get("cws.crypto.symmetric.keylength"), is("256"));
    }

    @Test(expected = CWSException.class)
    public void testInvokingRequestUpdateNotAllowedExistingSetting() {
        final SettingService service = new SettingService(new Settings(), entityManager);
        final SettingRequest request = prepareRequest();
        final Map<String, String> settings = new HashMap<>();
        settings.put("cws.crypto.symmetric.algorithm", "DES");
        request.setSettings(settings);

        service.process(request);
    }

    @Test
    public void testInvokingRequestAddNewSetting() {
        final SettingService service = new SettingService(new Settings(), entityManager);
        final SettingRequest request = prepareRequest();
        final Map<String, String> settings = new HashMap<>();
        settings.put("cws.test.setting", "Setting Value");
        request.setSettings(settings);

        final SettingResponse response = service.process(request);
        assertThat(response.getReturnCode(), is(0));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSettings().size(), is(10));
        assertThat(response.getSettings().get("cws.test.setting"), is("Setting Value"));
    }

    private static SettingRequest prepareRequest() {
        final SettingRequest request = new SettingRequest();
        request.setName(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());

        return request;
    }
}
