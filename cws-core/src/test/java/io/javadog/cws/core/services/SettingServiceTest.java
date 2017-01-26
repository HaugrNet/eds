package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.model.EntityManagerSetup;
import org.junit.Test;

import javax.persistence.Query;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SettingServiceTest extends EntityManagerSetup {

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
    }

    @Test(expected = CWSException.class)
    public void testNonAdminRequest() {
        final SettingService service = new SettingService(new Settings(), entityManager);
        final SettingRequest request = prepareRequest();
        request.setName("not admin");

        service.process(request);
    }

    @Test
    public void testInvokingRequest() {
        final SettingService service = new SettingService(new Settings(), entityManager);
        final SettingRequest request = prepareRequest();

        final SettingResponse response = service.process(request);
        assertThat(response.getReturnCode(), is(0));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    private static SettingRequest prepareRequest() {
        final SettingRequest request = new SettingRequest();
        request.setName(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());

        return request;
    }
}
