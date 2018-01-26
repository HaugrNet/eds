/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.TestUtilities;
import io.javadog.cws.api.common.Constants;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SettingRequestTest {

    @Test
    public void testClassflow() {
        final Map<String, String> settings = new HashMap<>();
        settings.put("Setting1", "Value1");

        final SettingRequest request = new SettingRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        assertThat(request.getSettings().isEmpty(), is(true));

        request.setSettings(settings);
        assertThat(request.getSettings().size(), is(1));
        assertThat(request.getSettings().get("Setting1"), is("Value1"));

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testEmptyClass() {
        final SettingRequest request = new SettingRequest();

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(2));
        assertThat(errors.get(Constants.FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Credential is missing."));
    }
}
