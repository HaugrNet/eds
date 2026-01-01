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
package net.haugr.eds.fitnesse;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.Objects;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.CredentialType;
import net.haugr.eds.api.dtos.Circle;
import net.haugr.eds.api.dtos.DataType;
import net.haugr.eds.api.dtos.Member;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.FetchCircleRequest;
import net.haugr.eds.api.requests.FetchDataTypeRequest;
import net.haugr.eds.api.requests.FetchMemberRequest;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import net.haugr.eds.api.requests.ProcessDataTypeRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.FetchCircleResponse;
import net.haugr.eds.api.responses.FetchDataTypeResponse;
import net.haugr.eds.api.responses.FetchMemberResponse;
import net.haugr.eds.fitnesse.callers.CallManagement;
import net.haugr.eds.fitnesse.callers.CallShare;
import net.haugr.eds.fitnesse.exceptions.StopTestException;

/**
 * <p>Before the testing with FitNesse can start, certain existing data must be
 * removed, so the tests will work correctly. This Fixture will ensure that this
 * is taken care of.</p>
 *
 * <p>As the EDS Data model specifically removes Objects, which is references by
 * both Circles &amp; Members, hence by removing all Circles, and also all
 * Members, the database should be cleaned in preparation for testing.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class ControlEds extends EDSRequest<FetchMemberResponse> {

    public void setUrl(final String url) {
        updateUrl(url);
    }

    public void removeCircles() {
        final FetchCircleRequest fetchRequest = prepareAdminRequest(FetchCircleRequest.class);
        final ProcessCircleRequest processRequest = prepareAdminRequest(ProcessCircleRequest.class);
        processRequest.setAction(Action.DELETE);

        final FetchCircleResponse fetchResponse = CallManagement.fetchCircles(requestUrl, fetchRequest);
        if (fetchResponse != null) {
            for (final Circle circle : fetchResponse.getCircles()) {
                processRequest.setCircleId(circle.getCircleId());
                CallManagement.processCircle(requestUrl, processRequest);
            }
        }
    }

    public void removeMembers() {
        final FetchMemberRequest fetchRequest = prepareAdminRequest(FetchMemberRequest.class);
        final ProcessMemberRequest processRequest = prepareAdminRequest(ProcessMemberRequest.class);
        processRequest.setAction(Action.DELETE);
        String adminId = null;

        final FetchMemberResponse fetchResponse = CallManagement.fetchMembers(requestUrl, fetchRequest);
        if (fetchResponse != null) {
            for (final Member member : fetchResponse.getMembers()) {
                if (!Objects.equals(member.getAccountName(), Constants.ADMIN_ACCOUNT)) {
                    processRequest.setMemberId(member.getMemberId());
                    CallManagement.processMember(requestUrl, processRequest);
                } else {
                    adminId = member.getMemberId();
                }
            }
        }

        clearAndAddAdminId(adminId);
        clearSignatures();
    }

    public void removeDataTypes() {
        final FetchDataTypeRequest fetchRequest = prepareAdminRequest(FetchDataTypeRequest.class);
        final ProcessDataTypeRequest deleteRequest = prepareAdminRequest(ProcessDataTypeRequest.class);
        deleteRequest.setAction(Action.DELETE);

        final FetchDataTypeResponse fetchResponse = CallShare.fetchDataTypes(requestUrl, fetchRequest);
        if (fetchResponse != null) {
            for (final DataType dataType : fetchResponse.getDataTypes()) {
                if (!Constants.DATA_TYPENAME.equalsIgnoreCase(dataType.getTypeName()) && !Constants.FOLDER_TYPENAME.equalsIgnoreCase(dataType.getTypeName())) {
                    deleteRequest.setTypeName(dataType.getTypeName());
                    CallShare.processDataType(requestUrl, deleteRequest);
                }
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
