/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.cws.fitnesse.callers;

import net.haugr.cws.api.Management;
import net.haugr.cws.api.requests.Authentication;
import net.haugr.cws.api.requests.FetchCircleRequest;
import net.haugr.cws.api.requests.FetchMemberRequest;
import net.haugr.cws.api.requests.FetchTrusteeRequest;
import net.haugr.cws.api.requests.InventoryRequest;
import net.haugr.cws.api.requests.MasterKeyRequest;
import net.haugr.cws.api.requests.ProcessCircleRequest;
import net.haugr.cws.api.requests.ProcessMemberRequest;
import net.haugr.cws.api.requests.ProcessTrusteeRequest;
import net.haugr.cws.api.requests.SanityRequest;
import net.haugr.cws.api.requests.SettingRequest;
import net.haugr.cws.api.responses.AuthenticateResponse;
import net.haugr.cws.api.responses.FetchCircleResponse;
import net.haugr.cws.api.responses.FetchMemberResponse;
import net.haugr.cws.api.responses.FetchTrusteeResponse;
import net.haugr.cws.api.responses.InventoryResponse;
import net.haugr.cws.api.responses.MasterKeyResponse;
import net.haugr.cws.api.responses.ProcessCircleResponse;
import net.haugr.cws.api.responses.ProcessMemberResponse;
import net.haugr.cws.api.responses.ProcessTrusteeResponse;
import net.haugr.cws.api.responses.SanityResponse;
import net.haugr.cws.api.responses.SettingResponse;
import net.haugr.cws.api.responses.VersionResponse;
import net.haugr.cws.client.rest.ManagementRestClient;

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
