/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
 */
package io.javadog.cws.client;

import io.javadog.cws.api.Share;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.FetchDataTypeResponse;
import io.javadog.cws.api.responses.FetchSignatureResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.api.responses.ProcessDataTypeResponse;
import io.javadog.cws.api.responses.SignResponse;
import io.javadog.cws.api.responses.VerifyResponse;
import io.javadog.cws.ws.FetchDataResult;
import io.javadog.cws.ws.FetchDataTypeResult;
import io.javadog.cws.ws.FetchSignatureResult;
import io.javadog.cws.ws.ProcessDataResult;
import io.javadog.cws.ws.ProcessDataTypeResult;
import io.javadog.cws.ws.Share_Service;
import io.javadog.cws.ws.SignResult;
import io.javadog.cws.ws.VerifyResult;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ShareSoapClient extends Mapper implements Share {

    private static final QName SERVICE_NAME = new QName("http://ws.cws.javadog.io/", "share");
    private final io.javadog.cws.ws.Share client;

    /**
     * Simple CXF based SOAP Client for the CWS Share logic.
     *
     * @param wsdl WSDL Location for a running CWS 1.0 instance
     * @throws MalformedURLException if the URL is incorrect
     */
    public ShareSoapClient(final String wsdl) throws MalformedURLException {
        final URL wsdlURL = new URL(wsdl);
        final Share_Service service = new Share_Service(wsdlURL, SERVICE_NAME);
        client = service.getShare();
    }

    // =========================================================================
    // Implementation of the Share Interface
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessDataTypeResponse processDataType(final ProcessDataTypeRequest request) {
        return map(client.processDataType(map(request)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchDataTypeResponse fetchDataTypes(final FetchDataTypeRequest request) {
        return map(client.fetchDataTypes(map(request)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessDataResponse processData(final ProcessDataRequest request) {
        return map(client.processData(map(request)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchDataResponse fetchData(final FetchDataRequest request) {
        return map(client.fetchData(map(request)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SignResponse sign(final SignRequest request) {
        return map(client.sign(map(request)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VerifyResponse verify(final VerifyRequest request) {
        return map(client.verify(map(request)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchSignatureResponse fetchSignatures(final FetchSignatureRequest request) {
        return map(client.fetchSignatures(map(request)));
    }

    // =========================================================================
    // Internal mapping of the API <-> WS Objects
    // =========================================================================

    private static io.javadog.cws.ws.ProcessDataTypeRequest map(final ProcessDataTypeRequest api) {
        io.javadog.cws.ws.ProcessDataTypeRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.ProcessDataTypeRequest();
            fillAuthentication(ws, api);
        }

        return ws;
    }

    private static ProcessDataTypeResponse map(final ProcessDataTypeResult ws) {
        ProcessDataTypeResponse api = null;

        if (ws != null) {
            api = new ProcessDataTypeResponse();
            fillResponse(api, ws);
        }

        return api;
    }

    private static io.javadog.cws.ws.FetchDataTypeRequest map(final FetchDataTypeRequest api) {
        io.javadog.cws.ws.FetchDataTypeRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.FetchDataTypeRequest();
            fillAuthentication(ws, api);
        }

        return ws;
    }

    private static FetchDataTypeResponse map(final FetchDataTypeResult ws) {
        FetchDataTypeResponse api = null;

        if (ws != null) {
            api = new FetchDataTypeResponse();
            fillResponse(api, ws);
        }

        return api;
    }

    private static io.javadog.cws.ws.ProcessDataRequest map(final ProcessDataRequest api) {
        io.javadog.cws.ws.ProcessDataRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.ProcessDataRequest();
            fillAuthentication(ws, api);
        }

        return ws;
    }

    private static ProcessDataResponse map(final ProcessDataResult ws) {
        ProcessDataResponse api = null;

        if (ws != null) {
            api = new ProcessDataResponse();
            fillResponse(api, ws);
        }

        return api;
    }

    private static io.javadog.cws.ws.FetchDataRequest map(final FetchDataRequest api) {
        io.javadog.cws.ws.FetchDataRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.FetchDataRequest();
            fillAuthentication(ws, api);
        }

        return ws;
    }

    private static FetchDataResponse map(final FetchDataResult ws) {
        FetchDataResponse api = null;

        if (ws != null) {
            api = new FetchDataResponse();
            fillResponse(api, ws);
            api.setData(ws.getData());
        }

        return api;
    }

    private static io.javadog.cws.ws.SignRequest map(final SignRequest api) {
        io.javadog.cws.ws.SignRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.SignRequest();
            fillAuthentication(ws, api);
            ws.setData(api.getData());
            ws.setExpires(map(api.getExpires()));
        }

        return ws;
    }

    private static SignResponse map(final SignResult ws) {
        SignResponse api = null;

        if (ws != null) {
            api = new SignResponse();
            fillResponse(api, ws);
            api.setSignature(ws.getSignature());
        }

        return api;
    }

    private static io.javadog.cws.ws.VerifyRequest map(final VerifyRequest api) {
        io.javadog.cws.ws.VerifyRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.VerifyRequest();
            fillAuthentication(ws, api);
            ws.setSignature(api.getSignature());
            ws.setData(api.getData());
        }

        return ws;
    }

    private static VerifyResponse map(final VerifyResult ws) {
        VerifyResponse api = null;

        if (ws != null) {
            api = new VerifyResponse();
            fillResponse(api, ws);
            api.setVerified(ws.isVerified());
        }

        return api;
    }

    private static io.javadog.cws.ws.FetchSignatureRequest map(final FetchSignatureRequest api) {
        io.javadog.cws.ws.FetchSignatureRequest ws = null;

        if (api != null) {
            ws = new io.javadog.cws.ws.FetchSignatureRequest();
            fillAuthentication(ws, api);
        }

        return ws;
    }

    private static FetchSignatureResponse map(final FetchSignatureResult ws) {
        FetchSignatureResponse api = null;

        if (ws != null) {
            api = new FetchSignatureResponse();
            fillResponse(api, ws);
        }

        return api;
    }
}
