/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.responses;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.ReturnCode;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SettingResponseTest {

    @Test
    public void testClassflow() {
        final Map<String, String> settings = new HashMap();
        settings.put("key1", "value1");
        settings.put("key2", "value2");
        settings.put("key3", "value3");

        final SettingResponse response = new SettingResponse();
        response.setSettings(settings);

        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.isOk(), is(true));
        assertThat(response.getSettings(), is(settings));
    }

    @Test
    public void testError() {
        final String msg = "Setting Request failed due to Verification Problems.";
        final SettingResponse response = new SettingResponse(ReturnCode.VERIFICATION_WARNING, msg);

        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is(msg));
        assertThat(response.isOk(), is(false));
        assertThat(response.getSettings().isEmpty(), is(true));
    }
}
