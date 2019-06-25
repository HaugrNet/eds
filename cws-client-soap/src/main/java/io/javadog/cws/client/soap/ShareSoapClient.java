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
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;

/**
 * <p>Apache CXF based based SOAP Client for the CWS Share functionality.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class ShareSoapClient implements Share {

    private static final QName SERVICE_NAME = new QName("http://ws.cws.javadog.io/", "share");
    private final io.javadog.cws.ws.Share client;

    /**
     * Constructor for the CWS Share SOAP Client. It takes the base URL for the
     * CWS Instance to communicate with, which is the protocol, hostname, port
     * and deployment name. For example; &quot;http://localhost:8080/cws&quot;.
     *
     * @param baseURL Base URL for the CWS Instance
     */
    public ShareSoapClient(final String baseURL) {
        try {
            final URL wsdlURL = new URL(baseURL + "/share?wsdl");
            final Share_Service service = new Share_Service(wsdlURL, SERVICE_NAME);
            client = service.getShare();
        } catch (MalformedURLException e) {
            throw new SOAPClientException(e);
        }
    }

    // =========================================================================
    // Implementation of the Share Interface
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessDataTypeResponse processDataType(final ProcessDataTypeRequest request) {
        ProcessDataTypeResponse response = null;

        if (request != null) {
            final io.javadog.cws.ws.ProcessDataTypeRequest ws = new io.javadog.cws.ws.ProcessDataTypeRequest();
            Mapper.fillAuthentication(ws, request);
            ws.setAction(Mapper.map(request.getAction()));
            ws.setTypeName(request.getTypeName());
            ws.setType(request.getType());

            final ProcessDataTypeResult result = client.processDataType(ws);
            if (result != null) {
                response = new ProcessDataTypeResponse();
                Mapper.fillResponse(response, result);
                response.setDataType(Mapper.map(result.getDataType()));
            }
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchDataTypeResponse fetchDataTypes(final FetchDataTypeRequest request) {
        FetchDataTypeResponse response = null;

        if (request != null) {
            final io.javadog.cws.ws.FetchDataTypeRequest ws = new io.javadog.cws.ws.FetchDataTypeRequest();
            Mapper.fillAuthentication(ws, request);

            final FetchDataTypeResult result = client.fetchDataTypes(ws);
            if (result != null) {
                response = new FetchDataTypeResponse();
                Mapper.fillResponse(response, result);
                response.setDataTypes(Mapper.mapDataTypes(result.getDataTypes()));
            }
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessDataResponse processData(final ProcessDataRequest request) {
        ProcessDataResponse response = null;

        if (request != null) {
            final io.javadog.cws.ws.ProcessDataRequest ws = new io.javadog.cws.ws.ProcessDataRequest();
            Mapper.fillAuthentication(ws, request);
            ws.setAction(Mapper.map(request.getAction()));
            ws.setDataId(request.getDataId());
            ws.setCircleId(request.getCircleId());
            ws.setDataName(request.getDataName());
            ws.setFolderId(request.getFolderId());
            ws.setTypeName(request.getTypeName());
            ws.setData(request.getData());

            final ProcessDataResult result = client.processData(ws);
            if (result != null) {
                response = new ProcessDataResponse();
                Mapper.fillResponse(response, result);
                response.setDataId(result.getDataId());
            }
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchDataResponse fetchData(final FetchDataRequest request) {
        FetchDataResponse response = null;

        if (request != null) {
            final io.javadog.cws.ws.FetchDataRequest ws = new io.javadog.cws.ws.FetchDataRequest();
            Mapper.fillAuthentication(ws, request);
            ws.setCircleId(request.getCircleId());
            ws.setDataId(request.getDataId());
            ws.setPageNumber(request.getPageNumber());
            ws.setPageSize(request.getPageSize());

            final FetchDataResult result = client.fetchData(ws);
            if (result != null) {
                response = new FetchDataResponse();
                Mapper.fillResponse(response, result);
                response.setMetadata(Mapper.mapMetadata(result.getMetadata()));
                response.setRecords(result.getRecords());
                response.setData(result.getData());

            }
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SignResponse sign(final SignRequest request) {
        SignResponse response = null;

        if (request != null) {
            final io.javadog.cws.ws.SignRequest ws = new io.javadog.cws.ws.SignRequest();
            Mapper.fillAuthentication(ws, request);
            ws.setData(request.getData());
            ws.setExpires(Mapper.map(request.getExpires()));

            final SignResult result = client.sign(ws);
            if (result != null) {
                response = new SignResponse();
                Mapper.fillResponse(response, result);
                response.setSignature(result.getSignature());
            }
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VerifyResponse verify(final VerifyRequest request) {
        VerifyResponse response = null;

        if (request != null) {
            final io.javadog.cws.ws.VerifyRequest ws = new io.javadog.cws.ws.VerifyRequest();
            Mapper.fillAuthentication(ws, request);
            ws.setSignature(request.getSignature());
            ws.setData(request.getData());

            final VerifyResult result = client.verify(ws);
            if (result != null) {
                response = new VerifyResponse();
                Mapper.fillResponse(response, result);
                response.setVerified(result.isVerified());
            }
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchSignatureResponse fetchSignatures(final FetchSignatureRequest request) {
        FetchSignatureResponse response = null;

        if (request != null) {
            final io.javadog.cws.ws.FetchSignatureRequest ws = new io.javadog.cws.ws.FetchSignatureRequest();
            Mapper.fillAuthentication(ws, request);

            final FetchSignatureResult result = client.fetchSignatures(ws);
            if (result != null) {
                response = new FetchSignatureResponse();
                Mapper.fillResponse(response, result);
                response.setSignatures(Mapper.mapSignatures(result.getSignatures()));
            }
        }

        return response;
    }
}
