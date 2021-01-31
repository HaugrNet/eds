/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
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
package io.javadog.cws.fitnesse.callers;

import io.javadog.cws.api.Management;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.requests.InventoryRequest;
import io.javadog.cws.api.requests.MasterKeyRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import io.javadog.cws.api.requests.SanityRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.AuthenticateResponse;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
import io.javadog.cws.api.responses.InventoryResponse;
import io.javadog.cws.api.responses.MasterKeyResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.ProcessTrusteeResponse;
import io.javadog.cws.api.responses.SanityResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.VersionResponse;
import io.javadog.cws.client.rest.ManagementRestClient;

/**
 * <p>CWS Management invocation class.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class CallManagement {

    private static Management management = null;

    private CallManagement() {
        // Private Constructor, this is a utility Class.
    }

    private static void prepareCWS(final String url) {
        if (management == null) {
            management = new ManagementRestClient(url);
        }
    }

    // =========================================================================
    // Management Interface Functionality
    // =========================================================================

    public static VersionResponse version(final String url) {
        prepareCWS(url);
        return management.version();
    }

    public static MasterKeyResponse masterKey(final String url, final MasterKeyRequest request) {
        prepareCWS(url);
        return management.masterKey(request);
    }

    public static SettingResponse settings(final String url, final SettingRequest request) {
        prepareCWS(url);
        return management.settings(request);
    }

    public static SanityResponse sanitized(final String url, final SanityRequest request) {
        prepareCWS(url);
        return management.sanitized(request);
    }

    public static InventoryResponse inventory(final String url, final InventoryRequest request) {
        prepareCWS(url);
        return management.inventory(request);
    }

    public static AuthenticateResponse authenticated(final String url, final Authentication request) {
        prepareCWS(url);
        return management.authenticated(request);
    }

    public static FetchMemberResponse fetchMembers(final String url, final FetchMemberRequest request) {
        prepareCWS(url);
        return management.fetchMembers(request);
    }

    public static ProcessMemberResponse processMember(final String url, final ProcessMemberRequest request) {
        prepareCWS(url);
        return management.processMember(request);
    }

    public static FetchCircleResponse fetchCircles(final String url, final FetchCircleRequest request) {
        prepareCWS(url);
        return management.fetchCircles(request);
    }

    public static ProcessCircleResponse processCircle(final String url, final ProcessCircleRequest request) {
        prepareCWS(url);
        return management.processCircle(request);
    }

    public static FetchTrusteeResponse fetchTrustees(final String url, final FetchTrusteeRequest request) {
        prepareCWS(url);
        return management.fetchTrustees(request);
    }

    public static ProcessTrusteeResponse processTrustee(final String url, final ProcessTrusteeRequest request) {
        prepareCWS(url);
        return management.processTrustee(request);
    }
}
