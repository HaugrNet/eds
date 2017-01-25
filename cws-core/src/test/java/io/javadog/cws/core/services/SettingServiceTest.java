package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.model.EntityManagerSetup;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SettingServiceTest extends EntityManagerSetup {

    @Test
    public void testInvokingRequest() {
        final SettingService service = new SettingService(new Settings(), entityManager);
        final SettingRequest request = new SettingRequest();
        request.setName(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());

        final SettingResponse response = service.process(request);
        assertThat(response.getReturnCode(), is(0));
        assertThat(response.getReturnMessage(), is("Ok"));
    }
}
