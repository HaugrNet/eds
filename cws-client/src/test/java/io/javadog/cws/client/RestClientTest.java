/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
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
public final class RestClientTest extends AbstractClientTestCase {

    private final Management management = new ManagementRestClient(Base.URL);
    private final Share share = new ShareRestClient(Base.URL);

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
        assertThat(management instanceof ManagementRestClient, is(true));
        assertThat(share instanceof ShareRestClient, is(true));
    }
}
