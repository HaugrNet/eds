/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
 */
package io.javadog.cws.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.Management;
import io.javadog.cws.api.Share;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.SignResponse;
import io.javadog.cws.api.responses.VerifyResponse;
import io.javadog.cws.api.responses.VersionResponse;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class RestClientTest {

    private final Management management = new ManagementRestClient(Base.URL);
    private final Share share = new ShareRestClient(Base.URL);

    @Test
    public void testVersion() {
        final VersionResponse response = management.version();
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getHttpCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getVersion(), is("1.0-SNAPSHOT"));
    }

    @Test
    public void testSettings() {
        final SettingRequest request = Base.prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);

        final SettingResponse response = management.settings(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getHttpCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testSigningVerify() {
        final SignRequest signRequest = Base.prepareRequest(SignRequest.class, Constants.ADMIN_ACCOUNT);
        final byte[] document = UUID.randomUUID().toString().getBytes(Charset.forName("UTF-8"));
        signRequest.setData(document);
        final SignResponse signResponse = share.sign(signRequest);
        assertThat(signResponse.isOk(), is(true));
        assertThat(signResponse.getSignature(), is(not(nullValue())));

        final VerifyRequest verifyRequest = Base.prepareRequest(VerifyRequest.class, Constants.ADMIN_ACCOUNT);
        verifyRequest.setData(document);
        verifyRequest.setSignature(signResponse.getSignature());

        final VerifyResponse verifyResponse = share.verify(verifyRequest);
        assertThat(verifyResponse.isOk(), is(true));
        assertThat(verifyResponse.isVerified(), is(true));
    }
}
