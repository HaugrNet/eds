/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-soap)
 * =============================================================================
 */
package io.javadog.cws.soap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.FetchDataTypeResponse;
import io.javadog.cws.api.responses.FetchSignatureResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.api.responses.ProcessDataTypeResponse;
import io.javadog.cws.api.responses.SignResponse;
import io.javadog.cws.api.responses.VerifyResponse;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class ShareServiceTest extends BeanSetup {

    @Test
    public void testProcessDataType() {
        final ShareService service = prepareShareService();
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.PROCESS);
        request.setTypeName("TestType");
        request.setType("Test Type Value");

        final ProcessDataTypeResponse response = service.processDataType(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getDataType(), is(not(nullValue())));
        assertThat(response.getDataType().getTypeName(), is("TestType"));
        assertThat(response.getDataType().getType(), is("Test Type Value"));
    }

    @Test
    public void testProcessDataTypeWithNullRequest() {
        final ShareService service = prepareShareService();
        final ProcessDataTypeRequest request = null;

        final ProcessDataTypeResponse response = service.processDataType(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testProcessDataTypeWithEmptyRequest() {
        final ShareService service = prepareShareService();
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final ProcessDataTypeResponse response = service.processDataType(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFlawedProcessDataType() {
        final ShareService service = prepareFlawedShareService();
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final ProcessDataTypeResponse response = service.processDataType(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR.getCode()));
    }

    @Test
    public void testFetchDataTypes() {
        final ShareService service = prepareShareService();
        final FetchDataTypeRequest request = prepareRequest(FetchDataTypeRequest.class, MEMBER_1);

        final FetchDataTypeResponse response = service.fetchDataTypes(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getDataTypes().size(), is(2));
        assertThat(response.getDataTypes().get(0).getTypeName(), is("data"));
        assertThat(response.getDataTypes().get(0).getType(), is("Data Object"));
        assertThat(response.getDataTypes().get(1).getTypeName(), is("folder"));
        assertThat(response.getDataTypes().get(1).getType(), is("Folder"));
    }

    @Test
    public void testFetchDataTypesWithNullRequest() {
        final ShareService service = prepareShareService();
        final FetchDataTypeRequest request = null;

        final FetchDataTypeResponse response = service.fetchDataTypes(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFetchDataTypesWithEmptyRequest() {
        final ShareService service = prepareShareService();
        final FetchDataTypeRequest request = new FetchDataTypeRequest();

        final FetchDataTypeResponse response = service.fetchDataTypes(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFlawedFetchDataTypes() {
        final ShareService service = prepareFlawedShareService();
        final FetchDataTypeRequest request = new FetchDataTypeRequest();

        final FetchDataTypeResponse response = service.fetchDataTypes(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR.getCode()));
    }

    @Test
    public void testProcessData() {
        final ShareService service = prepareShareService();
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setData("alfa beta gamma".getBytes(settings.getCharset()));
        request.setCircleId(CIRCLE_1_ID);
        request.setDataName("Data Name");
        request.setTypeName("data");

        final ProcessDataResponse response = service.processData(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getDataId(), is(not(nullValue())));
    }

    @Test
    public void testProcessDataWithNullRequest() {
        final ShareService service = prepareShareService();
        final ProcessDataRequest request = null;

        final ProcessDataResponse response = service.processData(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testProcessDataWithEmptyRequest() {
        final ShareService service = prepareShareService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final ProcessDataResponse response = service.processData(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFlawedProcessData() {
        final ShareService service = prepareFlawedShareService();
        final ProcessDataRequest request = new ProcessDataRequest();

        final ProcessDataResponse response = service.processData(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR.getCode()));
    }

    @Test
    public void testFetchData() {
        final ShareService service = prepareShareService();
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, MEMBER_1);
        request.setCircleId(CIRCLE_1_ID);

        final FetchDataResponse response = service.fetchData(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getMetadata().isEmpty(), is(true));
    }

    @Test
    public void testFetchDataWithNullRequest() {
        final ShareService service = prepareShareService();
        final FetchDataRequest request = null;

        final FetchDataResponse response = service.fetchData(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFetchDataWithEmptyRequest() {
        final ShareService service = prepareShareService();
        final FetchDataRequest request = new FetchDataRequest();

        final FetchDataResponse response = service.fetchData(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFlawedFetchData() {
        final ShareService service = prepareFlawedShareService();
        final FetchDataRequest request = new FetchDataRequest();

        final FetchDataResponse response = service.fetchData(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR.getCode()));
    }

    @Test
    public void testSignatures() {
        final ShareService service = prepareShareService();
        final SignRequest signRequest = prepareRequest(SignRequest.class, MEMBER_1);
        final byte[] data = "alfa".getBytes(settings.getCharset());
        signRequest.setData(data);
        final SignResponse signResponse = service.sign(signRequest);
        assertThat(signResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));

        final VerifyRequest verifyRequest = prepareRequest(VerifyRequest.class, MEMBER_2);
        verifyRequest.setData(data);
        verifyRequest.setSignature(signResponse.getSignature());
        final VerifyResponse verifyResponse = service.verify(verifyRequest);
        assertThat(verifyResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
    }

    @Test
    public void testSignWithNullRequest() {
        final ShareService service = prepareShareService();
        final SignRequest request = null;

        final SignResponse response = service.sign(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testSignWithEmptyRequest() {
        final ShareService service = prepareShareService();
        final SignRequest request = new SignRequest();

        final SignResponse response = service.sign(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFlawedSign() {
        final ShareService service = prepareFlawedShareService();
        final SignRequest request = new SignRequest();

        final SignResponse response = service.sign(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR.getCode()));
    }

    @Test
    public void testVerifyWithNullRequest() {
        final ShareService service = prepareShareService();
        final VerifyRequest request = null;

        final VerifyResponse response = service.verify(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testVerifyWithEmptyRequest() {
        final ShareService service = prepareShareService();
        final VerifyRequest request = new VerifyRequest();

        final VerifyResponse response = service.verify(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFlawedVerify() {
        final ShareService service = prepareFlawedShareService();
        final VerifyRequest request = new VerifyRequest();

        final VerifyResponse response = service.verify(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR.getCode()));
    }

    @Test
    public void testFetchSignatures() {
        final ShareService service = prepareShareService();
        final FetchSignatureRequest request = prepareRequest(FetchSignatureRequest.class, MEMBER_1);

        final FetchSignatureResponse response = service.fetchSignatures(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getSignatures().isEmpty(), is(true));
    }

    @Test
    public void testFetchSignaturesWithNullRequest() {
        final ShareService service = prepareShareService();
        final FetchSignatureRequest request = null;

        final FetchSignatureResponse response = service.fetchSignatures(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFetchSignaturesWithEmptyRequest() {
        final ShareService service = prepareShareService();
        final FetchSignatureRequest request = new FetchSignatureRequest();

        final FetchSignatureResponse response = service.fetchSignatures(request);
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
    }

    @Test
    public void testFlawedFetchSignatures() {
        final ShareService service = prepareFlawedShareService();
        final FetchSignatureRequest request = new FetchSignatureRequest();

        final FetchSignatureResponse response = service.fetchSignatures(request);
        assertThat(response.getReturnCode(), is(ReturnCode.ERROR.getCode()));
    }
}
