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
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.dtos.Metadata;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

/**
 * <p>Common test class for the Process & Fetch Data Services.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class DataServiceTest extends DatabaseSetup {

    @Test
    public void testEmptyProcessRequest() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors:" +
                        "\nKey: credentialType, Error: CredentialType is missing, null or invalid." +
                        "\nKey: metadata, Error: Data is missing, null or invalid." +
                        "\nKey: credential, Error: Credential is missing, null or invalid." +
                        "\nKey: account, Error: Account is missing, null or invalid.");

        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = new ProcessDataRequest();
        assertThat(request.getAccount(), is(nullValue()));

        service.perform(request);
    }

    @Test
    public void testEmptyFetchRequest() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors:" +
                        "\nKey: credentialType, Error: CredentialType is missing, null or invalid." +
                        "\nKey: credential, Error: Credential is missing, null or invalid." +
                        "\nKey: circleId, Error: Either the CircleId or an Object Data Id must be provided." +
                        "\nKey: account, Error: Account is missing, null or invalid.");

        final FetchDataService service = new FetchDataService(settings, entityManager);
        final FetchDataRequest request = new FetchDataRequest();
        assertThat(request.getAccount(), is(nullValue()));

        service.perform(request);
    }

    @Test
    public void testSavingAndReadingData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final FetchDataService dataService = new FetchDataService(settings, entityManager);

        // 1 MB large Data
        final byte[] data = generateData(1048576);
        final ProcessDataRequest saveRequest = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        final Metadata metaData = new Metadata();
        metaData.setCircleId(CIRCLE_1_ID);
        metaData.setName("MyData");
        metaData.setTypeName("data");
        saveRequest.setAction(Action.PROCESS);
        saveRequest.setMetadata(metaData);
        saveRequest.setBytes(data);
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertThat(saveResponse.getReturnCode(), is(ReturnCode.SUCCESS));

        final FetchDataRequest fetchRequest = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest.setDataId(saveResponse.getMetadata().getId());
        final FetchDataResponse fetchResponse = dataService.perform(fetchRequest);
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS));
    }
}
