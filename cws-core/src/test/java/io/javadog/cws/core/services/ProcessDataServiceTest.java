/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessDataServiceTest extends DatabaseSetup {

    @Test
    public void testInvalidRequest() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors: Key: credentialTypeError: CredentialType is missing, null or invalid.\n" +
                        "Key: credentialError: Credential is missing, null or invalid.\n" +
                        "Key: actionError: No action has been provided.\n" +
                        "Key: accountError: Account is missing, null or invalid.\n");

        final Serviceable<ProcessDataResponse, ProcessDataRequest> service = prepareService();
        final ProcessDataRequest request = new ProcessDataRequest();
        assertThat(request.getAccount(), is(nullValue()));

        service.perform(request);
    }

    private Serviceable<ProcessDataResponse, ProcessDataRequest> prepareService() {
        return new ProcessDataService(settings, entityManager);
    }
}
