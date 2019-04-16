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
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
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
 * @since CWS 1.0
 */
public class CwsRequest<R extends CwsResponse> {

    protected static String requestType = "SOAP";
    protected static String requestUrl = "http://localhost:8080/cws";

    // All Ids in CWS which is externally exposed is UUIDs, meaning that they
    // are always unique. And as the use of them should also be made with
    // easily identifiable, yet unique names, they are simply stored in a
    // single internal record, where the name have "_id" appended, and this
    // is then used to both store, delete and find Ids.
    private static final Map<String, String> ids = new HashMap<>(16);

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

    public String returnCode() {
        return (response != null) ? String.valueOf(response.getReturnCode()) : "null";
    }

    public String returnMessage() {
        return (response != null) ? response.getReturnMessage() : "null";
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
    public void execute() {
        throw new StopTestException("Unsupported Action.");
    }

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

    protected void addCircleInfo(final StringBuilder builder, final List<Circle> circles) {
        for (int i = 0; i < circles.size(); i++) {
            final Circle circle = circles.get(i);
            if (i >= 1) {
                builder.append(", ");
            }
            builder.append("Circle{circleId='")
                    .append(getKey(circle.getCircleId()))
                    .append("', circleName='")
                    .append(circle.getCircleName())
                    .append("', circleKey='")
                    .append(circle.getCircleKey())
                    .append("'}");
        }
    }

    protected static void clearAndAddAdminId(final String key, final String value) {
        ids.clear();
        ids.put(key, value);
    }

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

    protected static void processId(final Action action, final String currentKey, final String newKey, final ProcessDataResponse response) {
        if (response != null) {
            processId(action, currentKey, newKey, response.getDataId());
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

    protected String getKey(final String id) {
        String key = null;

        if (id != null) {
            key = ids.entrySet().stream()
                    .filter((Map.Entry<String, String> entry) -> Objects.equals(entry.getValue(), id))
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(null);
        }

        return key;
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
}
