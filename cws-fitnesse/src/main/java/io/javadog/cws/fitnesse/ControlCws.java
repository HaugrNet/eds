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
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.dtos.Circle;
import io.javadog.cws.api.dtos.DataType;
import io.javadog.cws.api.dtos.Member;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchDataTypeResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.fitnesse.callers.CallManagement;
import io.javadog.cws.fitnesse.callers.CallShare;
import io.javadog.cws.fitnesse.exceptions.StopTestException;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * <p>Before the testing with FitNesse can start, certain existing data must be
 * removed, so the tests will work correctly. This Fixture will ensure that this
 * is taken care of.</p>
 *
 * <p>As the CWS Data model specifically removes Objects, which is references by
 * both Circles &amp; Members, hence by removing all Circles, and also all
 * Members, the database should be cleaned in preparation for testing.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class ControlCws extends CwsRequest<FetchMemberResponse> {

    public void setTypeAndUrl(final String type, final String url) {
        CwsRequest.updateTypeAndUrl(type, url);
    }

    public void removeCircles() {
        final FetchCircleRequest fetchRequest = prepareAdminRequest(FetchCircleRequest.class);
        final ProcessCircleRequest processRequest = prepareAdminRequest(ProcessCircleRequest.class);
        processRequest.setAction(Action.DELETE);

        final FetchCircleResponse fetchResponse = CallManagement.fetchCircles(requestType, requestUrl, fetchRequest);
        for (final Circle circle : fetchResponse.getCircles()) {
            processRequest.setCircleId(circle.getCircleId());
            CallManagement.processCircle(requestType, requestUrl, processRequest);
        }
    }

    public void removeMembers() {
        final FetchMemberRequest fetchRequest = prepareAdminRequest(FetchMemberRequest.class);
        final ProcessMemberRequest processRequest = prepareAdminRequest(ProcessMemberRequest.class);
        processRequest.setAction(Action.DELETE);
        String adminId = null;

        final FetchMemberResponse fetchResponse = CallManagement.fetchMembers(requestType, requestUrl, fetchRequest);
        for (final Member member : fetchResponse.getMembers()) {
            if (!Objects.equals(member.getAccountName(), Constants.ADMIN_ACCOUNT)) {
                processRequest.setMemberId(member.getMemberId());
                CallManagement.processMember(requestType, requestUrl, processRequest);
            } else {
                adminId = member.getMemberId();
            }
        }

        clearAndAddAdminId(adminId);
        clearSignatures();
    }

    public void removeDataTypes() {
        final FetchDataTypeRequest fetchRequest = prepareAdminRequest(FetchDataTypeRequest.class);
        final ProcessDataTypeRequest deleteRequest = prepareAdminRequest(ProcessDataTypeRequest.class);
        deleteRequest.setAction(Action.DELETE);

        final FetchDataTypeResponse fetchResponse = CallShare.fetchDataTypes(requestType, requestUrl, fetchRequest);
        for (final DataType dataType : fetchResponse.getDataTypes()) {
            if (!dataType.getTypeName().equals(Constants.DATA_TYPENAME) && !dataType.getTypeName().equals(Constants.FOLDER_TYPENAME)) {
                deleteRequest.setTypeName(dataType.getTypeName());
                CallShare.processDataType(requestType, requestUrl, deleteRequest);
            }
        }
    }

    private static <T extends Authentication> T prepareAdminRequest(final Class<T> clazz) {
        try {
            final T request = clazz.getConstructor().newInstance();

            request.setAccountName(Constants.ADMIN_ACCOUNT);
            request.setCredential(Constants.ADMIN_ACCOUNT.getBytes(Charset.defaultCharset()));
            request.setCredentialType(CredentialType.PASSPHRASE);

            return request;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new StopTestException("Cannot instantiate Request Object", e);
        }
    }
}
