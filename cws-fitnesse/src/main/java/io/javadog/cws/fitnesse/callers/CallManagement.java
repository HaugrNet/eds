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
package io.javadog.cws.fitnesse.callers;

import io.javadog.cws.api.Management;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.requests.MasterKeyRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import io.javadog.cws.api.requests.SanityRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
import io.javadog.cws.api.responses.MasterKeyResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.ProcessTrusteeResponse;
import io.javadog.cws.api.responses.SanityResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.VersionResponse;
import io.javadog.cws.client.ManagementRestClient;
import io.javadog.cws.client.ManagementSoapClient;
import io.javadog.cws.fitnesse.exceptions.StopTestException;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class CallManagement {

    private static Management management = null;

    private CallManagement() {
        // Private Constructor, this is a utility Class.
    }

    private static void prepareCWS(final String type, final String url) {
        if (management == null) {
            switch (type) {
                case "REST":
                    management = new ManagementRestClient(url);
                    break;
                case "SOAP":
                    management = new ManagementSoapClient(url);
                    break;
                default:
                    throw new StopTestException("Unknown Request Type for CWS, supported is either REST or SOAP");
            }
        }
    }

    // =========================================================================
    // Management Interface Functionality
    // =========================================================================

    public static VersionResponse version(final String type, final String url) {
        prepareCWS(type, url);
        return management.version();
    }

    public static MasterKeyResponse masterKey(final String type, final String url, final MasterKeyRequest request) {
        prepareCWS(type, url);
        return management.masterKey(request);
    }

    public static SettingResponse settings(final String type, final String url, final SettingRequest request) {
        prepareCWS(type, url);
        return management.settings(request);
    }

    public static SanityResponse sanitized(final String type, final String url, final SanityRequest request) {
        prepareCWS(type, url);
        return management.sanitized(request);
    }

    public static FetchMemberResponse fetchMembers(final String type, final String url, final FetchMemberRequest request) {
        prepareCWS(type, url);
        return management.fetchMembers(request);
    }

    public static ProcessMemberResponse processMember(final String type, final String url, final ProcessMemberRequest request) {
        prepareCWS(type, url);
        return management.processMember(request);
    }

    public static FetchCircleResponse fetchCircles(final String type, final String url, final FetchCircleRequest request) {
        prepareCWS(type, url);
        return management.fetchCircles(request);
    }

    public static ProcessCircleResponse processCircle(final String type, final String url, final ProcessCircleRequest request) {
        prepareCWS(type, url);
        return management.processCircle(request);
    }

    public static FetchTrusteeResponse fetchTrustees(final String type, final String url, final FetchTrusteeRequest request) {
        prepareCWS(type, url);
        return management.fetchTrustees(request);
    }

    public static ProcessTrusteeResponse processTrustee(final String type, final String url, final ProcessTrusteeRequest request) {
        prepareCWS(type, url);
        return management.processTrustee(request);
    }
}
