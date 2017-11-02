/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static io.javadog.cws.api.ReflectiveTesting.reflectiveCorrection;
import static io.javadog.cws.api.common.Constants.FIELD_ACCOUNT_NAME;
import static io.javadog.cws.api.common.Constants.FIELD_CREDENTIAL;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SettingRequestTest {

    @Test
    public void testClass() {
        final Map<String, String> settings = new HashMap<>();
        settings.put("Setting1", "Value1");

        final SettingRequest request = new SettingRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        assertThat(request.getSettings().isEmpty(), is(true));

        request.setSettings(settings);
        assertThat(request.getSettings().size(), is(1));
        assertThat(request.getSettings().get("Setting1"), is("Value1"));

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testChangingSettings() {
        final Map<String, String> settings = new HashMap<>();
        settings.put("Setting1", "Value1");

        final SettingRequest request = new SettingRequest();
        request.setSettings(settings);
        assertThat(request.getSettings().size(), is(1));

        settings.put("Setting2", "Value2");
        assertThat(request.getSettings().size(), is(1));

        request.setSettings(settings);
        assertThat(request.getSettings().size(), is(2));

        final Map<String, String> current = request.getSettings();
        assertThat(current.size(), is(2));
        current.put("Setting3", "Value3");

        assertThat(request.getSettings().size(), is(2));
        assertThat(request.getSettings().get("Setting1"), is("Value1"));
        assertThat(request.getSettings().get("Setting2"), is("Value2"));
    }

    @Test
    public void testNullSettings() {
        final SettingRequest request = new SettingRequest();
        reflectiveCorrection(request, "settings", null);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(2));
        assertThat(errors.get(FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(FIELD_CREDENTIAL), is("The Credential is missing."));
    }
}
