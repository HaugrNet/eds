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
import net.haugr.eds.client.model.Action;
import net.haugr.eds.fitnesse.utils.Constants;
import net.haugr.eds.client.model.CredentialType;
import net.haugr.eds.client.model.Circle;
import net.haugr.eds.client.model.DataType;
import net.haugr.eds.client.model.Member;
import net.haugr.eds.client.model.Authentication;
import net.haugr.eds.client.model.FetchCircleRequest;
import net.haugr.eds.client.model.FetchDataTypeRequest;
import net.haugr.eds.client.model.FetchMemberRequest;
import net.haugr.eds.client.model.MasterKeyRequest;
import net.haugr.eds.client.model.ProcessCircleRequest;
import net.haugr.eds.client.model.ProcessDataTypeRequest;
import net.haugr.eds.client.model.ProcessMemberRequest;
import net.haugr.eds.client.model.FetchCircleResponse;
import net.haugr.eds.client.model.FetchDataTypeResponse;
import net.haugr.eds.client.model.FetchMemberResponse;
import net.haugr.eds.client.model.MasterKeyResponse;
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

    /**
     * Passphrase of the System Administrator Account, it must match the
     * ADMIN_PASSPHRASE defined on the FitNesse Setup page.
     */
    public static final String ADMIN_PASSPHRASE = "admin pjvgr.yxupm-37i23.8kq4h_9s";

    public void setUrl(final String url) {
        // The environment variable EDS_URL replaces the URL of the WIKI, so
        // the suites can run against an EDS started for them (mvn -Pfitnesse
        // verify). FitNesse passes its environment on to the test system.
        final String override = System.getenv("EDS_URL");
        updateUrl(((override != null) && !override.isBlank()) ? override : url);
    }

    /**
     * <p>EDS starts locked, unless a MasterKey source is configured. This
     * unlocks EDS with the given secret, or sets up the MasterKey on a fresh
     * instance. On an unlocked EDS with the same MasterKey, it does nothing.</p>
     *
     * @param secret MasterKey secret
     * @return The return message
     */
    public String unlockMasterKey(final String secret) {
        final MasterKeyRequest request = prepareAdminRequest(MasterKeyRequest.class);
        request.setSecret(secret.getBytes(Charset.defaultCharset()));

        final MasterKeyResponse response = CallManagement.masterKey(requestUrl, request);
        return (response != null) ? response.getReturnMessage() : null;
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
        clearInvitations();
    }

    public void removeDataTypes() {
        final FetchDataTypeRequest fetchRequest = prepareAdminRequest(FetchDataTypeRequest.class);
        final ProcessDataTypeRequest deleteRequest = prepareAdminRequest(ProcessDataTypeRequest.class);
        deleteRequest.setAction(Action.DELETE);

        final FetchDataTypeResponse fetchResponse = CallShare.fetchDataTypes(requestUrl, fetchRequest);
        if (fetchResponse != null) {
            for (final DataType dataType : fetchResponse.getDataTypes()) {
                if (!Constants.DATA_TYPENAME.equalsIgnoreCase(dataType.getTypeName()) && !Constants.FOLDER_TYPENAME.equalsIgnoreCase(dataType.getTypeName())
                        && !Constants.ENTRY_TYPENAME.equalsIgnoreCase(dataType.getTypeName()) && !Constants.THUMBNAIL_TYPENAME.equalsIgnoreCase(dataType.getTypeName())) {
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
            request.setCredential(ADMIN_PASSPHRASE.getBytes(Charset.defaultCharset()));
            request.setCredentialType(CredentialType.PASSPHRASE);

            return request;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new StopTestException("Cannot instantiate Request Object", e);
        }
    }
}
