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
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import io.javadog.cws.api.requests.SanityRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.ProcessTrusteeResponse;
import io.javadog.cws.api.responses.SanityResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.VersionResponse;
import io.javadog.cws.client.ManagementSoapClient;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CallManagement {

    private static final Management MANAGEMENT = new ManagementSoapClient("http://localhost:8080/cws");

    private CallManagement() {
        // Private Constructor, this is a utility Class.
    }

    // =========================================================================
    // Management Interface Functionality
    // =========================================================================

    public static VersionResponse version() {
        return MANAGEMENT.version();
    }

    public static SettingResponse settings(final SettingRequest request) {
        return MANAGEMENT.settings(request);
    }

    public static SanityResponse sanitized(final SanityRequest request) {
        return MANAGEMENT.sanitized(request);
    }

    public static FetchMemberResponse fetchMembers(final FetchMemberRequest request) {
        return MANAGEMENT.fetchMembers(request);
    }

    public static ProcessMemberResponse processMember(final ProcessMemberRequest request) {
        return MANAGEMENT.processMember(request);
    }

    public static FetchCircleResponse fetchCircles(final FetchCircleRequest request) {
        return MANAGEMENT.fetchCircles(request);
    }

    public static ProcessCircleResponse processCircle(final ProcessCircleRequest request) {
        return MANAGEMENT.processCircle(request);
    }

    public static FetchTrusteeResponse fetchTrustees(final FetchTrusteeRequest request) {
        return MANAGEMENT.fetchTrustees(request);
    }

    public static ProcessTrusteeResponse processTrustee(final ProcessTrusteeRequest request) {
        return MANAGEMENT.processTrustee(request);
    }
}
