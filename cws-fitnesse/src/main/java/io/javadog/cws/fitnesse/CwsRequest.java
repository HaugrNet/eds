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
package io.javadog.cws.fitnesse;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.dtos.Circle;
import io.javadog.cws.api.dtos.DataType;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.fitnesse.exceptions.StopTestException;
import io.javadog.cws.fitnesse.utils.Converter;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public abstract class CwsRequest<R extends CwsResponse> {

    protected static String requestType = "SOAP";
    protected static String requestUrl = "http://localhost:8080/cws";

    // If it is not possible to find a matching value, then this should be used.
    protected static final String UNDEFINED = "undefined";

    // All Ids in CWS which is externally exposed is UUIDs, meaning that they
    // are always unique. And as the use of them should also be made with
    // easily identifiable, yet unique names, they are simply stored in a
    // single internal record, where the name have "_id" appended, and this
    // is then used to both store, delete and find Ids.
    private static final Map<String, String> ids = new HashMap<>(16);

    // Internal mapping of the Member AccountNames & the MemberId, so the tests
    // can refer to the names rather than the Id's, as the latter will change
    // per test run.
    private final Map<String, String> members = new HashMap<>();

    // Internal mapping of the Circle Names & the CircleId, so the tests can
    // refer to the names rather than the Id's, as the latter will change per
    // test run.
    protected final Map<String, String> circles = new HashMap<>();

    // For all Data processing, it helps to cache the existing types, so they
    // can easily be retrieved and used if new data is being added or retrieved.
    private final Map<String, String> dataTypes = new HashMap<>();

    private String accountName = null;
    protected byte[] credential = null;
    private CredentialType credentialType = null;
    protected R response = null;

    public static void updateTypeAndUrl(final String type, final String url) {
        requestType = type;
        requestUrl = url;
    }

    public void setAccountName(final String accountName) {
        this.accountName = Converter.preCheck(accountName);
    }

    public void setCredential(final String credential) {
        this.credential = Converter.convertBytes(credential);
    }

    public void setCredentialType(final String credentialType) {
        this.credentialType = Converter.findCredentialType(credentialType);
    }

    public int returnCode() {
        return response.getReturnCode();
    }

    public String returnMessage() {
        return response.getReturnMessage();
    }

    protected <T extends Authentication> T prepareRequest(final Class<T> clazz) {
        try {
            final T request = clazz.getConstructor().newInstance();
            request.setAccountName(accountName);
            request.setCredential(credential);
            request.setCredentialType(credentialType);

            return request;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new StopTestException("Cannot instantiate Request Object", e);
        }
    }

    /**
     * When using the Fit Table Fixture, FitNesse is invoking the execute
     * method, which runs the request. It builds the Request Object and saves
     * the Response Object.
     */
    public abstract void execute();

    /**
     * When using the Fit Table Fixture, FitNesse is invoking the reset method,
     * before preparing a new line in the table to build a new request.
     */
    public void reset() {
        this.accountName = null;
        this.credential = null;
        this.credentialType = null;
    }

    // =========================================================================
    // Operating the internal mapping of Members & Circles
    // =========================================================================

    protected static void processId(final Action action, final String currentKey, final String newKey, final ProcessMemberResponse response) {
        if (response != null) {
            processId(action, currentKey, newKey, response.getMemberId());
        }
    }

    protected static void processId(final Action action, final String currentKey, final String newKey, final ProcessCircleResponse response) {
        if (response != null) {
            processId(action, currentKey, newKey, response.getCircleId());
        }
    }

    private static void processId(final Action action, final String currentKey, final String newKey, final String id) {
        switch (action) {
            case ADD:
            case CREATE:
            case PROCESS:
                if ((newKey != null) && (id != null)) {
                    ids.put(newKey + "_id", id);
                }
                break;
            case REMOVE:
            case DELETE:
                if (currentKey != null) {
                    ids.remove(currentKey);
                }
                break;
            default:
                break;
        }
    }

    protected String getId(final String key) {
        String id = null;

        if (key != null) {
            id = ids.entrySet().stream()
                    .filter((Map.Entry<String, String> entry) -> Objects.equals(entry.getKey(), key))
                    .map(Map.Entry::getValue)
                    .findFirst().orElse(null);
        }

        return id;
    }

    protected String getMemberNames() {
        return members.keySet().toString();
    }

    protected void setCircles(final FetchCircleResponse circleResponse) {
        for (final Circle circle : circleResponse.getCircles()) {
            circles.put(circle.getCircleName(), circle.getCircleId());
        }
    }

    protected String getCircleNames() {
        return circles.keySet().toString();
    }

    protected void setDataTypes(final List<DataType> dataTypes) {
        for (final DataType dataType : dataTypes) {
            this.dataTypes.put(dataType.getTypeName(), dataType.getType());
        }
    }

    protected boolean hasDataType(final String typeName) {
        return dataTypes.get(typeName) != null;
    }

    protected String getDataType(final String typeName) {
        return dataTypes.getOrDefault(typeName, UNDEFINED);
    }
}
