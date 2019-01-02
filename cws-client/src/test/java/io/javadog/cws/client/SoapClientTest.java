/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.client;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.Management;
import io.javadog.cws.api.Share;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SoapClientTest extends AbstractClientTestCase {

    private final Management management = new ManagementSoapClient(Base.URL);
    private final Share share = new ShareSoapClient(Base.URL);

    @Override
    public Management getManagement() {
        return management;
    }

    @Override
    public Share getShare() {
        return share;
    }

    @Test
    public void testCorrectInstances() {
        assertThat(management instanceof ManagementSoapClient, is(true));
        assertThat(share instanceof ShareSoapClient, is(true));
    }
}
