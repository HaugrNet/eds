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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.requests.FetchObjectTypeRequest;
import io.javadog.cws.api.responses.FetchObjectTypeResponse;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchObjectTypeServiceTest extends DatabaseSetup {

    @Test
    public void testEmptyRequest() {
        prepareCause(VerificationException.class, Constants.VERIFICATION_WARNING,
                "Request Object contained errors: Key: credentialTypeError: CredentialType is missing, null or invalid.\n" +
                "Key: credentialError: Credential is missing, null or invalid.\n" +
                "Key: accountError: Account is missing, null or invalid.\n");

        final Serviceable<FetchObjectTypeResponse, FetchObjectTypeRequest> service = prepareService();
        final FetchObjectTypeRequest request = new FetchObjectTypeRequest();
        assertThat(request.getAccount(), is(nullValue()));

        service.perform(request);
    }

    @Test
    public void testAdminRequest() {
        final Serviceable<FetchObjectTypeResponse, FetchObjectTypeRequest> service = prepareService();
        final FetchObjectTypeRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        final FetchObjectTypeResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getTypes().size(), is(1));
        assertThat(response.getTypes().get(0).getName(), is("folder"));
        assertThat(response.getTypes().get(0).getType(), is("Folder"));
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private Serviceable<FetchObjectTypeResponse, FetchObjectTypeRequest> prepareService() {
        return new FetchObjectTypeService(settings, entityManager);
    }

    private static FetchObjectTypeRequest buildRequestWithCredentials(final String account) {
        final FetchObjectTypeRequest request = new FetchObjectTypeRequest();
        request.setAccount(account);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(account.toCharArray());

        return request;
    }
}
