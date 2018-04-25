/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
 */
package io.javadog.cws.client;

import io.javadog.cws.api.Management;
import io.javadog.cws.api.common.Constants;
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

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
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
            throw new CWSClientException(e);
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
    public MasterKeyResponse masterKey(final MasterKeyRequest request) {
        return map(client.masterKey(map(request)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SanityResponse sanitized(final SanityRequest request) {
        return map(client.sanitized(map(request)));
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

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchTrusteeResponse fetchTrustees(final FetchTrusteeRequest request) {
        return map(client.fetchTrustees(map(request)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessTrusteeResponse processTrustee(final ProcessTrusteeRequest request) {
        return map(client.processTrustee(map(request)));
    }

    // =========================================================================
    // Internal mapping of the API <-> WS Request & Response Objects
    // =========================================================================

    private static VersionResponse map(final VersionResult ws) {
        VersionResponse api = null;

        if (ws != null) {
            api = new VersionResponse();
            Mapper.fillResponse(api, ws);
            api.setVersion(ws.getVersion());
        }

        return api;
    }

    private static io.javadog.cws.ws.SettingRequest map(final SettingRequest api) {
        io.javadog.cws.ws.SettingRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.SettingRequest();
            Mapper.fillAuthentication(ws, api);
            ws.setSettings(mapSettings(api.getSettings()));
        }

        return ws;
    }

    private static SettingResponse map(final SettingResult ws) {
        SettingResponse api = null;

        if (ws != null) {
            api = new SettingResponse();
            Mapper.fillResponse(api, ws);
            api.setSettings(mapSettings(ws.getSettings()));
        }

        return api;
    }

    private static io.javadog.cws.ws.MasterKeyRequest map(final MasterKeyRequest api) {
        io.javadog.cws.ws.MasterKeyRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.MasterKeyRequest();
            Mapper.fillAuthentication(ws, api);
            ws.setSecret(api.getSecret());
        }

        return ws;
    }

    private static MasterKeyResponse map(final MasterKeyResult ws) {
        MasterKeyResponse api = null;

        if (ws != null) {
            api = new MasterKeyResponse();
            Mapper.fillResponse(api, ws);
        }

        return api;
    }

    private static io.javadog.cws.ws.SanityRequest map(final SanityRequest api) {
        io.javadog.cws.ws.SanityRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.SanityRequest();
            Mapper.fillAuthentication(ws, api);
            ws.setCircleId(Mapper.convert(Constants.FIELD_CIRCLE_ID, api.getCircleId()));
            ws.setSince(Mapper.map(api.getSince()));
        }

        return ws;
    }

    private static SanityResponse map(final SanityResult ws) {
        SanityResponse api = null;

        if (ws != null) {
            api = new SanityResponse();
            Mapper.fillResponse(api, ws);
            api.setSanities(Mapper.mapSanities(ws.getSanities()));
        }

        return api;
    }

    private static io.javadog.cws.ws.FetchMemberRequest map(final FetchMemberRequest api) {
        io.javadog.cws.ws.FetchMemberRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.FetchMemberRequest();
            Mapper.fillAuthentication(ws, api);
            ws.setMemberId(Mapper.convert(Constants.FIELD_MEMBER_ID, api.getMemberId()));
        }

        return ws;
    }

    private static FetchMemberResponse map(final FetchMemberResult ws) {
        FetchMemberResponse api = null;

        if (ws != null) {
            api = new FetchMemberResponse();
            Mapper.fillResponse(api, ws);
            api.setMembers(Mapper.mapMembers(ws.getMembers()));
            api.setCircles(Mapper.mapCircles(ws.getCircles()));
        }

        return api;
    }

    private static io.javadog.cws.ws.ProcessMemberRequest map(final ProcessMemberRequest api) {
        io.javadog.cws.ws.ProcessMemberRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.ProcessMemberRequest();
            Mapper.fillAuthentication(ws, api);
            ws.setAction(Mapper.map(api.getAction()));
            ws.setMemberId(Mapper.convert(Constants.FIELD_MEMBER_ID, api.getMemberId()));
            ws.setPublicKey(api.getPublicKey());
            ws.setNewAccountName(Mapper.convert(Constants.FIELD_NEW_ACCOUNT_NAME, api.getNewAccountName()));
            ws.setNewCredential(Mapper.convert(Constants.FIELD_NEW_CREDENTIAL, api.getNewCredential()));
        }

        return ws;
    }

    private static ProcessMemberResponse map(final ProcessMemberResult ws) {
        ProcessMemberResponse api = null;

        if (ws != null) {
            api = new ProcessMemberResponse();
            Mapper.fillResponse(api, ws);
            api.setMemberId(ws.getMemberId());
            api.setSignature(ws.getSignature());
        }

        return api;
    }

    private static io.javadog.cws.ws.FetchCircleRequest map(final FetchCircleRequest api) {
        io.javadog.cws.ws.FetchCircleRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.FetchCircleRequest();
            Mapper.fillAuthentication(ws, api);
        }

        return ws;
    }

    private static FetchCircleResponse map(final FetchCircleResult ws) {
        FetchCircleResponse api = null;

        if (ws != null) {
            api = new FetchCircleResponse();
            Mapper.fillResponse(api, ws);
            api.setCircles(Mapper.mapCircles(ws.getCircles()));
        }

        return api;
    }

    private static io.javadog.cws.ws.ProcessCircleRequest map(final ProcessCircleRequest api) {
        io.javadog.cws.ws.ProcessCircleRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.ProcessCircleRequest();
            Mapper.fillAuthentication(ws, api);
            ws.setAction(Mapper.map(api.getAction()));
            ws.setCircleId(Mapper.convert(Constants.FIELD_CIRCLE_ID, api.getCircleId()));
            ws.setCircleName(Mapper.convert(Constants.FIELD_CIRCLE_NAME, api.getCircleName()));
            ws.setMemberId(Mapper.convert(Constants.FIELD_MEMBER_ID, api.getMemberId()));
            ws.setCircleKey(Mapper.convert(Constants.FIELD_CIRCKE_KEY, api.getCircleKey()));
        }

        return ws;
    }

    private static ProcessCircleResponse map(final ProcessCircleResult ws) {
        ProcessCircleResponse api = null;

        if (ws != null) {
            api = new ProcessCircleResponse();
            Mapper.fillResponse(api, ws);
            api.setCircleId(ws.getCircleId());
        }

        return api;
    }

    private static io.javadog.cws.ws.FetchTrusteeRequest map(final FetchTrusteeRequest api) {
        io.javadog.cws.ws.FetchTrusteeRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.FetchTrusteeRequest();
            Mapper.fillAuthentication(ws, api);
            ws.setCircleId(Mapper.convert(Constants.FIELD_CIRCLE_ID, api.getCircleId()));
        }

        return ws;
    }

    private static FetchTrusteeResponse map(final FetchTrusteeResult ws) {
        FetchTrusteeResponse api = null;

        if (ws != null) {
            api = new FetchTrusteeResponse();
            Mapper.fillResponse(api, ws);
            api.setTrustees(Mapper.mapTrustees(ws.getTrustees()));
        }

        return api;
    }

    private static io.javadog.cws.ws.ProcessTrusteeRequest map(final ProcessTrusteeRequest api) {
        io.javadog.cws.ws.ProcessTrusteeRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.ProcessTrusteeRequest();
            Mapper.fillAuthentication(ws, api);
            ws.setAction(Mapper.map(api.getAction()));
            ws.setCircleId(Mapper.convert(Constants.FIELD_CIRCLE_ID, api.getCircleId()));
            ws.setMemberId(Mapper.convert(Constants.FIELD_MEMBER_ID, api.getMemberId()));
            ws.setTrustLevel(Mapper.map(api.getTrustLevel()));
        }

        return ws;
    }

    private static ProcessTrusteeResponse map(final ProcessTrusteeResult ws) {
        ProcessTrusteeResponse api = null;

        if (ws != null) {
            api = new ProcessTrusteeResponse();
            Mapper.fillResponse(api, ws);
        }

        return api;
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
        final Map<String, String> api = new HashMap<>();

        if (ws != null) {
            for (final SettingResult.Settings.Entry entry : ws.getEntry()) {
                api.put(entry.getKey(), entry.getValue());
            }
        }

        return api;
    }
}
