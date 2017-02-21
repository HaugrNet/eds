package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchCirclesServiceTest extends DatabaseSetup {

    @Test
    public void testFetchAll() {
        final Settings settings = new Settings();
        final Servicable<FetchCircleResponse, FetchCircleRequest> service = new FetchCirclesService(settings, entityManager);

        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
    }
}
