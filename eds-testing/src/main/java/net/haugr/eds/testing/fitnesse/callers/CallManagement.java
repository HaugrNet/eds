/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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
package net.haugr.eds.testing.fitnesse.callers;

import net.haugr.eds.testing.client.api.AuthenticationApi;
import net.haugr.eds.testing.client.api.CirclesApi;
import net.haugr.eds.testing.client.api.MasterKeyApi;
import net.haugr.eds.testing.client.api.MembersApi;
import net.haugr.eds.testing.client.api.SanityApi;
import net.haugr.eds.testing.client.api.SettingsApi;
import net.haugr.eds.testing.client.api.TrusteesApi;
import net.haugr.eds.testing.client.api.VersionApi;
import net.haugr.eds.testing.client.model.Authentication;
import net.haugr.eds.testing.client.model.AuthenticateResponse;
import net.haugr.eds.testing.client.model.FetchCircleRequest;
import net.haugr.eds.testing.client.model.FetchCircleResponse;
import net.haugr.eds.testing.client.model.FetchMemberRequest;
import net.haugr.eds.testing.client.model.FetchMemberResponse;
import net.haugr.eds.testing.client.model.FetchTrusteeRequest;
import net.haugr.eds.testing.client.model.FetchTrusteeResponse;
import net.haugr.eds.testing.client.model.MasterKeyRequest;
import net.haugr.eds.testing.client.model.MasterKeyResponse;
import net.haugr.eds.testing.client.model.ProcessCircleRequest;
import net.haugr.eds.testing.client.model.ProcessCircleResponse;
import net.haugr.eds.testing.client.model.ProcessMemberRequest;
import net.haugr.eds.testing.client.model.ProcessMemberResponse;
import net.haugr.eds.testing.client.model.ProcessTrusteeRequest;
import net.haugr.eds.testing.client.model.ProcessTrusteeResponse;
import net.haugr.eds.testing.client.model.SanityRequest;
import net.haugr.eds.testing.client.model.SanityResponse;
import net.haugr.eds.testing.client.model.SettingRequest;
import net.haugr.eds.testing.client.model.SettingResponse;
import net.haugr.eds.testing.client.model.VersionResponse;
import net.haugr.eds.testing.fitnesse.exceptions.StopTestException;

/**
 * <p>Calls of the management operations, with the client generated from the
 * API contract. The process requests go to the endpoint of their action.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.1
 */
public final class CallManagement {

    private static final String UNSUPPORTED = "Unsupported action: ";

    private CallManagement() {
        // Private Constructor, this is a utility Class.
    }

    public static VersionResponse version(final String url) {
        return Calls.run(url, VersionResponse.class, () -> Calls.api(url, VersionApi.class, VersionApi::new).version());
    }

    public static MasterKeyResponse masterKey(final String url, final MasterKeyRequest request) {
        return Calls.run(url, MasterKeyResponse.class, () -> Calls.api(url, MasterKeyApi.class, MasterKeyApi::new).masterKey(request));
    }

    public static SettingResponse settings(final String url, final SettingRequest request) {
        return Calls.run(url, SettingResponse.class, () -> Calls.api(url, SettingsApi.class, SettingsApi::new).settings(request));
    }

    public static SanityResponse sanitized(final String url, final SanityRequest request) {
        return Calls.run(url, SanityResponse.class, () -> Calls.api(url, SanityApi.class, SanityApi::new).sanitized(request));
    }

    public static AuthenticateResponse authenticated(final String url, final Authentication request) {
        return Calls.run(url, AuthenticateResponse.class, () -> Calls.api(url, AuthenticationApi.class, AuthenticationApi::new).authenticated(request));
    }

    public static FetchMemberResponse fetchMembers(final String url, final FetchMemberRequest request) {
        return Calls.run(url, FetchMemberResponse.class, () -> Calls.api(url, MembersApi.class, MembersApi::new).fetchMembers(request));
    }

    public static ProcessMemberResponse processMember(final String url, final ProcessMemberRequest request) {
        final MembersApi api = Calls.api(url, MembersApi.class, MembersApi::new);
        return Calls.run(url, ProcessMemberResponse.class, () -> switch (request.getAction()) {
            case CREATE -> api.createMember(request);
            case INVITE -> api.inviteMember(request);
            case LOGIN -> api.login(request);
            case LOGOUT -> api.logout(request);
            case ALTER -> api.alterMember(request);
            case UPDATE -> api.updateMember(request);
            case INVALIDATE -> api.invalidate(request);
            case DELETE -> api.deleteMember(request);
            default -> throw new StopTestException(UNSUPPORTED + request.getAction());
        });
    }

    public static FetchCircleResponse fetchCircles(final String url, final FetchCircleRequest request) {
        return Calls.run(url, FetchCircleResponse.class, () -> Calls.api(url, CirclesApi.class, CirclesApi::new).fetchCircles(request));
    }

    public static ProcessCircleResponse processCircle(final String url, final ProcessCircleRequest request) {
        final CirclesApi api = Calls.api(url, CirclesApi.class, CirclesApi::new);
        return Calls.run(url, ProcessCircleResponse.class, () -> switch (request.getAction()) {
            case CREATE -> api.createCircle(request);
            case UPDATE -> api.updateCircle(request);
            case DELETE -> api.deleteCircle(request);
            case ROTATE -> api.rotateCircle(request);
            default -> throw new StopTestException(UNSUPPORTED + request.getAction());
        });
    }

    public static FetchTrusteeResponse fetchTrustees(final String url, final FetchTrusteeRequest request) {
        return Calls.run(url, FetchTrusteeResponse.class, () -> Calls.api(url, TrusteesApi.class, TrusteesApi::new).fetchTrustees(request));
    }

    public static ProcessTrusteeResponse processTrustee(final String url, final ProcessTrusteeRequest request) {
        final TrusteesApi api = Calls.api(url, TrusteesApi.class, TrusteesApi::new);
        return Calls.run(url, ProcessTrusteeResponse.class, () -> switch (request.getAction()) {
            case ADD -> api.addTrustee(request);
            case ALTER -> api.alterTrustee(request);
            case REMOVE -> api.removeTrustee(request);
            default -> throw new StopTestException(UNSUPPORTED + request.getAction());
        });
    }

    public static ProcessTrusteeResponse setSecrets(final String url, final ProcessTrusteeRequest request) {
        return Calls.run(url, ProcessTrusteeResponse.class, () -> Calls.api(url, TrusteesApi.class, TrusteesApi::new).setSecrets(request));
    }
}
