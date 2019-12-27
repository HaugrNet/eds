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
package io.javadog.cws.client.soap;

import io.javadog.cws.api.Management;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.FetchTrusteeRequest;
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
import io.javadog.cws.api.responses.MasterKeyResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.ProcessTrusteeResponse;
import io.javadog.cws.api.responses.SanityResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.VersionResponse;
import io.javadog.cws.ws.AuthenticateResult;
import io.javadog.cws.ws.FetchCircleResult;
import io.javadog.cws.ws.FetchMemberResult;
import io.javadog.cws.ws.FetchTrusteeResult;
import io.javadog.cws.ws.Management_Service;
import io.javadog.cws.ws.MasterKeyResult;
import io.javadog.cws.ws.ProcessCircleResult;
import io.javadog.cws.ws.ProcessMemberResult;
import io.javadog.cws.ws.ProcessTrusteeResult;
import io.javadog.cws.ws.SanityResult;
import io.javadog.cws.ws.SettingResult;
import io.javadog.cws.ws.VersionResult;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.namespace.QName;

/**
 * <p>Apache CXF based based SOAP Client for the CWS Management functionality.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class ManagementSoapClient implements Management {

    private static final QName SERVICE_NAME = new QName("http://ws.cws.javadog.io/", "management");
    private final io.javadog.cws.ws.Management client;

    /**
     * Constructor for the CWS Management SOAP Client. It takes the base URL for
     * the CWS Instance to communicate with, which is the protocol, hostname,
     * port and deployment name. For example;
     * &quot;http://localhost:8080/cws&quot;.
     *
     * @param baseURL Base URL for the CWS Instance
     */
    public ManagementSoapClient(final String baseURL) {
        try {
            final URL wsdlURL = new URL(baseURL + "/management?wsdl");
            final Management_Service service = new Management_Service(wsdlURL, SERVICE_NAME);
            client = service.getManagement();
        } catch (MalformedURLException e) {
            throw new SOAPClientException(e);
        }
    }

    // =========================================================================
    // Implementation of the System Interface
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public VersionResponse version() {
        VersionResponse response = null;

        final VersionResult result = client.version();
        if (result != null) {
            response = new VersionResponse();
            Mapper.fillResponse(response, result);
            response.setVersion(result.getVersion());
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettingResponse settings(final SettingRequest request) {
        SettingResponse response = null;

        if (request != null) {
            final io.javadog.cws.ws.SettingRequest ws = new io.javadog.cws.ws.SettingRequest();
            Mapper.fillAuthentication(ws, request);
            ws.setSettings(mapSettings(request.getSettings()));

            final SettingResult result = client.settings(ws);
            if (result != null) {
                response = new SettingResponse();
                Mapper.fillResponse(response, result);
                response.setSettings(mapSettings(result.getSettings()));
            }
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MasterKeyResponse masterKey(final MasterKeyRequest request) {
        MasterKeyResponse response = null;

        if (request != null) {
            final io.javadog.cws.ws.MasterKeyRequest ws = new io.javadog.cws.ws.MasterKeyRequest();
            Mapper.fillAuthentication(ws, request);
            ws.setSecret(request.getSecret());
            ws.setUrl(request.getUrl());

            final MasterKeyResult result = client.masterKey(ws);
            if (result != null) {
                response = new MasterKeyResponse();
                Mapper.fillResponse(response, result);
            }
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SanityResponse sanitized(final SanityRequest request) {
        SanityResponse response = null;

        if (request != null) {
            final io.javadog.cws.ws.SanityRequest ws = new io.javadog.cws.ws.SanityRequest();
            Mapper.fillAuthentication(ws, request);
            ws.setCircleId(Mapper.convert(Constants.FIELD_CIRCLE_ID, request.getCircleId()));
            ws.setSince(Mapper.map(request.getSince()));

            final SanityResult result = client.sanitized(ws);
            if (result != null) {
                response = new SanityResponse();
                Mapper.fillResponse(response, result);
                response.setSanities(Mapper.mapSanities(result.getSanities()));
            }
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthenticateResponse authenticated(final Authentication request) {
        AuthenticateResponse response = null;

        if (request != null) {
            final io.javadog.cws.ws.Authentication ws = new io.javadog.cws.ws.Authentication();
            Mapper.fillAuthentication(ws, request);

            final AuthenticateResult result = client.authenticated(ws);
            if (result != null) {
                response = new AuthenticateResponse();
                Mapper.fillResponse(response, result);
                response.setMemberId(result.getMemberId());
            }
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchMemberResponse fetchMembers(final FetchMemberRequest request) {
        FetchMemberResponse response = null;

        if (request != null) {
            final io.javadog.cws.ws.FetchMemberRequest ws = new io.javadog.cws.ws.FetchMemberRequest();
            Mapper.fillAuthentication(ws, request);
            ws.setMemberId(Mapper.convert(Constants.FIELD_MEMBER_ID, request.getMemberId()));

            final FetchMemberResult result = client.fetchMembers(ws);
            if (result != null) {
                response = new FetchMemberResponse();
                Mapper.fillResponse(response, result);
                response.setMembers(Mapper.mapMembers(result.getMembers()));
                response.setCircles(Mapper.mapCircles(result.getCircles()));
            }
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessMemberResponse processMember(final ProcessMemberRequest request) {
        ProcessMemberResponse response = null;

        if (request != null) {
            final io.javadog.cws.ws.ProcessMemberRequest ws = new io.javadog.cws.ws.ProcessMemberRequest();
            Mapper.fillAuthentication(ws, request);
            ws.setAction(Mapper.map(request.getAction()));
            ws.setMemberId(Mapper.convert(Constants.FIELD_MEMBER_ID, request.getMemberId()));
            ws.setMemberRole(Mapper.map(request.getMemberRole()));
            ws.setPublicKey(request.getPublicKey());
            ws.setNewAccountName(Mapper.convert(Constants.FIELD_NEW_ACCOUNT_NAME, request.getNewAccountName()));
            ws.setNewCredential(Mapper.convert(Constants.FIELD_NEW_CREDENTIAL, request.getNewCredential()));

            final ProcessMemberResult result = client.processMember(ws);
            if (result != null) {
                response = new ProcessMemberResponse();
                Mapper.fillResponse(response, result);
                response.setMemberId(result.getMemberId());
                response.setSignature(result.getSignature());
            }
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchCircleResponse fetchCircles(final FetchCircleRequest request) {
        FetchCircleResponse response = null;

        if (request != null) {
            final io.javadog.cws.ws.FetchCircleRequest ws = new io.javadog.cws.ws.FetchCircleRequest();
            Mapper.fillAuthentication(ws, request);

            final FetchCircleResult result = client.fetchCircles(ws);
            if (result != null) {
                response = new FetchCircleResponse();
                Mapper.fillResponse(response, result);
                response.setCircles(Mapper.mapCircles(result.getCircles()));
            }
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessCircleResponse processCircle(final ProcessCircleRequest request) {
        ProcessCircleResponse response = null;

        if (request != null) {
            final io.javadog.cws.ws.ProcessCircleRequest ws = new io.javadog.cws.ws.ProcessCircleRequest();
            Mapper.fillAuthentication(ws, request);
            ws.setAction(Mapper.map(request.getAction()));
            ws.setCircleId(Mapper.convert(Constants.FIELD_CIRCLE_ID, request.getCircleId()));
            ws.setCircleName(Mapper.convert(Constants.FIELD_CIRCLE_NAME, request.getCircleName()));
            ws.setMemberId(Mapper.convert(Constants.FIELD_MEMBER_ID, request.getMemberId()));
            ws.setCircleKey(Mapper.convert(Constants.FIELD_CIRCLE_KEY, request.getCircleKey()));

            final ProcessCircleResult result = client.processCircle(ws);
            if (result != null) {
                response = new ProcessCircleResponse();
                Mapper.fillResponse(response, result);
                response.setCircleId(result.getCircleId());
            }
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchTrusteeResponse fetchTrustees(final FetchTrusteeRequest request) {
        FetchTrusteeResponse response = null;

        if (request != null) {
            final io.javadog.cws.ws.FetchTrusteeRequest ws = new io.javadog.cws.ws.FetchTrusteeRequest();
            Mapper.fillAuthentication(ws, request);
            ws.setCircleId(Mapper.convert(Constants.FIELD_CIRCLE_ID, request.getCircleId()));
            ws.setMemberId(Mapper.convert(Constants.FIELD_MEMBER_ID, request.getMemberId()));

            final FetchTrusteeResult result = client.fetchTrustees(ws);
            if (result != null) {
                response = new FetchTrusteeResponse();
                Mapper.fillResponse(response, result);
                response.setTrustees(Mapper.mapTrustees(result.getTrustees()));
            }
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessTrusteeResponse processTrustee(final ProcessTrusteeRequest request) {
        ProcessTrusteeResponse response = null;

        if (request != null) {
            final io.javadog.cws.ws.ProcessTrusteeRequest ws = new io.javadog.cws.ws.ProcessTrusteeRequest();
            Mapper.fillAuthentication(ws, request);
            ws.setAction(Mapper.map(request.getAction()));
            ws.setCircleId(Mapper.convert(Constants.FIELD_CIRCLE_ID, request.getCircleId()));
            ws.setMemberId(Mapper.convert(Constants.FIELD_MEMBER_ID, request.getMemberId()));
            ws.setTrustLevel(Mapper.map(request.getTrustLevel()));

            final ProcessTrusteeResult result = client.processTrustee(ws);
            if (result != null) {
                response = new ProcessTrusteeResponse();
                Mapper.fillResponse(response, result);
            }
        }

        return response;
    }

    // =========================================================================
    // Mapping of sub components
    // =========================================================================

    private static io.javadog.cws.ws.SettingRequest.Settings mapSettings(final Map<String, String> api) {
        io.javadog.cws.ws.SettingRequest.Settings ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.SettingRequest.Settings();
            final List<io.javadog.cws.ws.SettingRequest.Settings.Entry> list = ws.getEntry();
            for (final Map.Entry<String, String> map : api.entrySet()) {
                final io.javadog.cws.ws.SettingRequest.Settings.Entry entry = new io.javadog.cws.ws.SettingRequest.Settings.Entry();
                entry.setKey(map.getKey());
                entry.setValue(map.getValue());
                list.add(entry);
            }
        }

        return ws;
    }

    private static Map<String, String> mapSettings(final SettingResult.Settings ws) {
        final Map<String, String> api = new ConcurrentHashMap<>();

        if (ws != null) {
            for (final SettingResult.Settings.Entry entry : ws.getEntry()) {
                api.put(entry.getKey(), entry.getValue());
            }
        }

        return api;
    }
}
