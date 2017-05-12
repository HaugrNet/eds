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
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.responses.FetchDataTypeResponse;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchDataTypeServiceTest extends DatabaseSetup {

    @Test
    public void testEmptyRequest() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors: Key: credentialTypeError: CredentialType is missing, null or invalid.\n" +
                "Key: credentialError: Credential is missing, null or invalid.\n" +
                "Key: accountError: Account is missing, null or invalid.\n");

        final Serviceable<FetchDataTypeResponse, FetchDataTypeRequest> service = prepareService();
        final FetchDataTypeRequest request = new FetchDataTypeRequest();
        assertThat(request.getAccount(), is(nullValue()));

        service.perform(request);
    }

    @Test
    public void testAdminRequest() {
        final Serviceable<FetchDataTypeResponse, FetchDataTypeRequest> service = prepareService();
        final FetchDataTypeRequest request = buildRequestWithCredentials(Constants.ADMIN_ACCOUNT);
        final FetchDataTypeResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getTypes().size(), is(1));
        assertThat(response.getTypes().get(0).getName(), is("folder"));
        assertThat(response.getTypes().get(0).getType(), is("Folder"));
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private Serviceable<FetchDataTypeResponse, FetchDataTypeRequest> prepareService() {
        return new FetchDataTypeService(settings, entityManager);
    }

    private static FetchDataTypeRequest buildRequestWithCredentials(final String account) {
        final FetchDataTypeRequest request = new FetchDataTypeRequest();
        request.setAccount(account);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(account.toCharArray());

        return request;
    }
}
