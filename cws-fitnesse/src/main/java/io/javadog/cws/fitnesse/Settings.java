/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.fitnesse;

import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.fitnesse.callers.CallManagement;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Settings extends CwsRequest<SettingResponse> {

    private Map<String, String> theSettings = new HashMap<>();
    private String key = null;
    private String value = null;

    // =========================================================================
    // Default Constructors, used for Script & Decision Tables
    // =========================================================================

    /**
     * Default Empty Constructor, required for the Decision Table.
     */
    public Settings() {
    }

    /**
     * Default Constructor for the Script Table.
     *
     * @param accountName System Administrator Account Name
     * @param credential  System Administrator Credentials (passphrase)
     */
    public Settings(final String accountName, final String credential) {
        setAccountName(accountName);
        setCredential(credential);

        final SettingRequest request = prepareRequest(SettingRequest.class);
        theSettings = CallManagement.settings(request).getSettings();
    }

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setKey(final String key) {
        this.key = key;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public String defined() {
        final String defined = response.getSettings().get(key);
        return (defined != null) ? defined : "undefined";
    }

    public String valueForKey(final String key) {
        return theSettings.getOrDefault(key, "undefined");
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final Map<String, String> settings = new HashMap<>();
        settings.put(key, value);

        final SettingRequest request = prepareRequest(SettingRequest.class);
        request.setSettings(settings);

        response = CallManagement.settings(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();

        this.key = null;
        this.value = null;
    }
}
