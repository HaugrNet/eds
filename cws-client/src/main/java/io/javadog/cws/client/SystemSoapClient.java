/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
 */
package io.javadog.cws.client;

import io.javadog.cws.api.System;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.VersionResponse;
import io.javadog.cws.ws.FetchCircleResult;
import io.javadog.cws.ws.FetchMemberResult;
import io.javadog.cws.ws.ProcessCircleResult;
import io.javadog.cws.ws.ProcessMemberResult;
import io.javadog.cws.ws.SettingResult;
import io.javadog.cws.ws.System_Service;
import io.javadog.cws.ws.VersionResult;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SystemSoapClient extends Mapper implements System {

    private static final QName SERVICE_NAME = new QName("http://ws.cws.javadog.io/", "system");
    private final io.javadog.cws.ws.System client;

    /**
     * Simple CXF based SOAP Client for the CWS Share logic.
     *
     * @param wsdl WSDL Location for a running CWS 1.0 instance
     * @throws MalformedURLException if the URL is incorrect
     */
    public SystemSoapClient(final String wsdl) throws MalformedURLException {
        final URL wsdlURL = new URL(wsdl);
        final System_Service service = new System_Service(wsdlURL, SERVICE_NAME);
        client = service.getSystem();
    }

    // =========================================================================
    // Implementation of the System Interface
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public VersionResponse version() {
        return map(client.version());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettingResponse settings(final SettingRequest request) {
        return map(client.settings(map(request)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchMemberResponse fetchMembers(final FetchMemberRequest request) {
        return map(client.fetchMembers(map(request)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessMemberResponse processMember(final ProcessMemberRequest request) {
        return map(client.processMember(map(request)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchCircleResponse fetchCircles(final FetchCircleRequest request) {
        return map(client.fetchCircles(map(request)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessCircleResponse processCircle(final ProcessCircleRequest request) {
        return map(client.processCircle(map(request)));
    }

    // =========================================================================
    // Internal mapping of the API <-> WS Objects
    // =========================================================================

    private static VersionResponse map(final VersionResult ws) {
        VersionResponse api = null;

        if (ws != null) {
            api = new VersionResponse();
            fillResponse(api, ws);
        }

        return api;
    }

    private static io.javadog.cws.ws.SettingRequest map(final SettingRequest api) {
        io.javadog.cws.ws.SettingRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.SettingRequest();
            fillAuthentication(ws, api);
            ws.setSettings(mapSettings(api.getSettings()));
        }

        return ws;
    }

    private static io.javadog.cws.ws.SettingRequest.Settings mapSettings(final Map<String, String> api) {
        return null;
    }

    private static SettingResponse map(final SettingResult ws) {
        SettingResponse api = null;

        if (ws != null) {
            api = new SettingResponse();
            fillResponse(api, ws);
        }

        return api;
    }

    private static io.javadog.cws.ws.FetchMemberRequest map(final FetchMemberRequest api) {
        io.javadog.cws.ws.FetchMemberRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.FetchMemberRequest();
            fillAuthentication(ws, api);
            ws.setMemberId(api.getMemberId());
        }

        return ws;
    }

    private static FetchMemberResponse map(final FetchMemberResult ws) {
        FetchMemberResponse api = null;

        if (ws != null) {
            api = new FetchMemberResponse();
            fillResponse(api, ws);
        }

        return api;
    }

    private static io.javadog.cws.ws.ProcessMemberRequest map(final ProcessMemberRequest api) {
        io.javadog.cws.ws.ProcessMemberRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.ProcessMemberRequest();
            fillAuthentication(ws, api);
            ws.setAction(map(api.getAction()));
            ws.setMemberId(api.getMemberId());
            ws.setNewAccountName(api.getNewAccountName());
            ws.setNewCredential(api.getNewCredential());
        }

        return ws;
    }

    private static ProcessMemberResponse map(final ProcessMemberResult ws) {
        ProcessMemberResponse api = null;

        if (ws != null) {
            api = new ProcessMemberResponse();
            fillResponse(api, ws);
            api.setMemberId(ws.getMemberId());
            api.setSignature(ws.getSignature());
        }

        return api;
    }

    private static io.javadog.cws.ws.FetchCircleRequest map(final FetchCircleRequest api) {
        io.javadog.cws.ws.FetchCircleRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.FetchCircleRequest();
            fillAuthentication(ws, api);
        }

        return ws;
    }

    private static FetchCircleResponse map(final FetchCircleResult ws) {
        FetchCircleResponse api = null;

        if (ws != null) {
            api = new FetchCircleResponse();
            fillResponse(api, ws);
        }

        return api;
    }

    private static io.javadog.cws.ws.ProcessCircleRequest map(final ProcessCircleRequest api) {
        io.javadog.cws.ws.ProcessCircleRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.ProcessCircleRequest();
            fillAuthentication(ws, api);
            ws.setAction(map(api.getAction()));
            ws.setCircleId(api.getCircleId());
            ws.setCircleName(api.getCircleName());
            ws.setMemberId(api.getMemberId());
            ws.setTrustLevel(map(api.getTrustLevel()));
        }

        return ws;
    }

    private static ProcessCircleResponse map(final ProcessCircleResult ws) {
        ProcessCircleResponse api = null;

        if (ws != null) {
            api = new ProcessCircleResponse();
            fillResponse(api, ws);
            api.setCircleId(ws.getCircleId());
        }

        return api;
    }
}
