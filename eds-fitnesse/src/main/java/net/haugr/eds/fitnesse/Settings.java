/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.fitnesse;

import net.haugr.eds.api.requests.SettingRequest;
import net.haugr.eds.api.responses.SettingResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.haugr.eds.fitnesse.callers.CallManagement;

/**
 * <p>FitNesse Fixture for the EDS Settings feature.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class Settings extends EDSRequest<SettingResponse> {

    private final Map<String, String> theSettings = new ConcurrentHashMap<>();
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
        theSettings.clear();

        final SettingRequest request = prepareRequest(SettingRequest.class);
        final SettingResponse response = CallManagement.settings(requestUrl, request);

        if (response != null) {
            theSettings.putAll(response.getSettings());
        }
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
        final Map<String, String> settings = new ConcurrentHashMap<>();
        settings.put(key, value);

        final SettingRequest request = prepareRequest(SettingRequest.class);
        request.setSettings(settings);

        response = CallManagement.settings(requestUrl, request);
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
