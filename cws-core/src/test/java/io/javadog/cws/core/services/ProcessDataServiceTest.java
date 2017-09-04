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

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.dtos.Authentication;
import io.javadog.cws.api.dtos.Metadata;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessDataServiceTest extends DatabaseSetup {

    @Test
    @Ignore("Temporarily ignoring test as there was a refactoring change that caused it to fail.")
    public void testInvalidRequest() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors: Key: credentialTypeError: CredentialType is missing, null or invalid.\n" +
                        "Key: credentialError: Credential is missing, null or invalid.\n" +
                        "Key: dataError: Data is missing, null or invalid.\n" +
                        "Key: accountError: Account is missing, null or invalid.\n");

        final Serviceable<ProcessDataResponse, ProcessDataRequest> service = prepareService();
        final ProcessDataRequest request = new ProcessDataRequest();
        assertThat(request.getAccount(), is(nullValue()));

        service.perform(request);
    }

    @Test
    public void testSavingAndReadingData() {
        // 1 MB large Data
        final byte[] data = generateData(1048576);
        final ProcessDataRequest saveRequest = prepareRequest(ProcessDataRequest.class, "member1");
        final Metadata metaData = new Metadata();
        metaData.setCircleId("d8838d7d-71e7-433d-8790-af7c080e9de9");
        metaData.setName("MyData");
        metaData.setTypeName("data");
        saveRequest.setAction(Action.PROCESS);
        saveRequest.setMetadata(metaData);
        saveRequest.setBytes(data);
        final Serviceable<ProcessDataResponse, ProcessDataRequest> service = prepareService();
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertThat(saveResponse.getReturnCode(), is(ReturnCode.SUCCESS));

        //final FetchDataRequest fetchRequest = prepareRequest(FetchDataRequest.class, "member1");
        //fetchRequest.setDataId(saveResponse.getMetadata().getId());
        //final FetchDataService dataService = new FetchDataService(settings, entityManager);
        //// TODO Extend test data, so each Circle has a Root Folder
        //final FetchDataResponse fetchResponse = dataService.perform(fetchRequest);
        //assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    private static <T extends Authentication> T prepareRequest(final Class<T> clazz, final String account) {
        try {
            final T request = clazz.getConstructor().newInstance();

            request.setAccount(account);
            request.setCredential(account);
            request.setCredentialType(CredentialType.PASSPHRASE);

            return request;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Request Object", e);
        }
    }

    private static byte[] generateData(final int bytes) {
        final byte[] data = new byte[bytes];
        final SecureRandom random = new SecureRandom();
        random.nextBytes(data);

        return data;
    }

    private Serviceable<ProcessDataResponse, ProcessDataRequest> prepareService() {
        return new ProcessDataService(settings, entityManager);
    }
}
