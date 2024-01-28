/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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
package net.haugr.eds.fitnesse.callers;

import net.haugr.eds.api.Management;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.FetchCircleRequest;
import net.haugr.eds.api.requests.FetchMemberRequest;
import net.haugr.eds.api.requests.FetchTrusteeRequest;
import net.haugr.eds.api.requests.InventoryRequest;
import net.haugr.eds.api.requests.MasterKeyRequest;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.requests.ProcessTrusteeRequest;
import net.haugr.eds.api.requests.SanityRequest;
import net.haugr.eds.api.requests.SettingRequest;
import net.haugr.eds.api.responses.AuthenticateResponse;
import net.haugr.eds.api.responses.FetchCircleResponse;
import net.haugr.eds.api.responses.FetchMemberResponse;
import net.haugr.eds.api.responses.FetchTrusteeResponse;
import net.haugr.eds.api.responses.InventoryResponse;
import net.haugr.eds.api.responses.MasterKeyResponse;
import net.haugr.eds.api.responses.ProcessCircleResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.api.responses.ProcessTrusteeResponse;
import net.haugr.eds.api.responses.SanityResponse;
import net.haugr.eds.api.responses.SettingResponse;
import net.haugr.eds.api.responses.VersionResponse;
import net.haugr.eds.client.rest.ManagementRestClient;

/**
 * <p>EDS Management invocation class.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class CallManagement {

    private static Management management = null;

    private CallManagement() {
        // Private Constructor, this is a utility Class.
    }

    private static void prepareEDS(final String url) {
        if (management == null) {
            management = new ManagementRestClient(url);
        }
    }

    // =========================================================================
    // Management Interface Functionality
    // =========================================================================

    /**
     * Prepares a call for the EDS Version Service, using the given URL as
     * Endpoint.
     *
     * @param url     Endpoint URL for the EDS call
     * @return Result from the call
     */
    public static VersionResponse version(final String url) {
        prepareEDS(url);
        return management.version();
    }

    /**
     * Prepares a call for the EDS MasterKey Service, using the given URL as
     * Endpoint, and the given Request Object as argument.
     *
     * @param url     Endpoint URL for the EDS call
     * @param request Request Object for the call
     * @return Response from the call
     */
    public static MasterKeyResponse masterKey(final String url, final MasterKeyRequest request) {
        prepareEDS(url);
        return management.masterKey(request);
    }

    /**
     * Prepares a call for the EDS Settings Service, using the given URL as
     * Endpoint, and the given Request Object as argument.
     *
     * @param url     Endpoint URL for the EDS call
     * @param request Request Object for the call
     * @return Response from the call
     */
    public static SettingResponse settings(final String url, final SettingRequest request) {
        prepareEDS(url);
        return management.settings(request);
    }

    /**
     * Prepares a call for the EDS Sanitized Service, using the given URL as
     * Endpoint, and the given Request Object as argument.
     *
     * @param url     Endpoint URL for the EDS call
     * @param request Request Object for the call
     * @return Response from the call
     */
    public static SanityResponse sanitized(final String url, final SanityRequest request) {
        prepareEDS(url);
        return management.sanitized(request);
    }

    /**
     * Prepares a call for the EDS Inventory Service, using the given URL as
     * Endpoint, and the given Request Object as argument.
     *
     * @param url     Endpoint URL for the EDS call
     * @param request Request Object for the call
     * @return Response from the call
     */
    public static InventoryResponse inventory(final String url, final InventoryRequest request) {
        prepareEDS(url);
        return management.inventory(request);
    }

    /**
     * Prepares a call for the EDS Authenticated Service, using the given URL
     * as Endpoint, and the given Request Object as argument.
     *
     * @param url     Endpoint URL for the EDS call
     * @param request Request Object for the call
     * @return Response from the call
     */
    public static AuthenticateResponse authenticated(final String url, final Authentication request) {
        prepareEDS(url);
        return management.authenticated(request);
    }

    /**
     *
     * @param url     Endpoint URL for the EDS call
     * @param request Request Object for the call
     * @return Response from the call
     */
    public static FetchMemberResponse fetchMembers(final String url, final FetchMemberRequest request) {
        prepareEDS(url);
        return management.fetchMembers(request);
    }

    /**
     * Prepares a call for the EDS ProcessMember Service, using the given URL
     * as Endpoint, and the given Request Object as argument.
     *
     * @param url     Endpoint URL for the EDS call
     * @param request Request Object for the call
     * @return Response from the call
     */
    public static ProcessMemberResponse processMember(final String url, final ProcessMemberRequest request) {
        prepareEDS(url);
        return management.processMember(request);
    }

    /**
     * Prepares a call for the EDS FetchCircles Service, using the given URL
     * as Endpoint, and the given Request Object as argument.
     *
     * @param url     Endpoint URL for the EDS call
     * @param request Request Object for the call
     * @return Response from the call
     */
    public static FetchCircleResponse fetchCircles(final String url, final FetchCircleRequest request) {
        prepareEDS(url);
        return management.fetchCircles(request);
    }

    /**
     * Prepares a call for the EDS ProcessCircle Service, using the given URL
     * as Endpoint, and the given Request Object as argument.
     *
     * @param url     Endpoint URL for the EDS call
     * @param request Request Object for the call
     * @return Response from the call
     */
    public static ProcessCircleResponse processCircle(final String url, final ProcessCircleRequest request) {
        prepareEDS(url);
        return management.processCircle(request);
    }

    /**
     * Prepares a call for the EDS FetchTrustees Service, using the given URL
     * as Endpoint, and the given Request Object as argument.
     *
     * @param url     Endpoint URL for the EDS call
     * @param request Request Object for the call
     * @return Response from the call
     */
    public static FetchTrusteeResponse fetchTrustees(final String url, final FetchTrusteeRequest request) {
        prepareEDS(url);
        return management.fetchTrustees(request);
    }

    /**
     * Prepares a call for the EDS ProcessTrustee Service, using the given URL
     * as Endpoint, and the given Request Object as argument.
     *
     * @param url     Endpoint URL for the EDS call
     * @param request Request Object for the call
     * @return Response from the call
     */
    public static ProcessTrusteeResponse processTrustee(final String url, final ProcessTrusteeRequest request) {
        prepareEDS(url);
        return management.processTrustee(request);
    }
}
