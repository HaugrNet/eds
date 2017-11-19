/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
 */
package io.javadog.cws.client;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.System;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.VersionResponse;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class SystemSoapClientTest {

    private static final String SYSTEM_URL = "http://localhost:8080/cws/system?wsdl";

    @Test
    public void testVersion() {
        final System system = new SystemSoapClient(SYSTEM_URL);
        final VersionResponse response = system.version();
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getVersion(), is("0.7-SNAPSHOT"));
    }

    @Test
    public void testSettings() {
        final System system = new SystemSoapClient(SYSTEM_URL);
        final SettingRequest request = new SettingRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);

        final SettingResponse response = system.settings(request);
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
    }
}
