/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
 */
package io.javadog.cws.client;

import io.javadog.cws.api.Management;
import io.javadog.cws.api.Share;
import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.dtos.Trustee;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.ProcessTrusteeResponse;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Base {

    public static final String URL = "http://localhost:8080/cws";

    private Base() {
        // Private Constructor, this is a Utility Class.
    }

    public static <A extends Authentication> A prepareRequest(final Class<A> clazz, final String account) {
        try {
            final A request = clazz.getConstructor().newInstance();

            request.setAccountName(account);
            request.setCredential(account.getBytes(Charset.forName("UTF-8")));
            request.setCredentialType(CredentialType.PASSPHRASE);

            return request;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new CWSClientException("Cannot instantiate Request Object", e);
        }
    }

    public static String createAccount(final Management management, final String accountName) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setNewAccountName(accountName);
        request.setNewCredential(accountName.getBytes(Charset.forName("UTF-8")));

        final ProcessMemberResponse response = management.processMember(request);
        throwIfFailed(response);

        return response.getMemberId();
    }

    public static String createCircle(final Management management, final String circleName, final String accountName) {
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, accountName);
        request.setAction(Action.CREATE);
        request.setCircleName(circleName);

        final ProcessCircleResponse response = management.processCircle(request);
        throwIfFailed(response);

        return response.getCircleId();
    }

    public static void addTrustee(final Management management, final String circleAdminAccount, final String circleId, final String memberId) {
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class, circleAdminAccount);
        request.setAction(Action.ADD);
        request.setCircleId(circleId);
        request.setMemberId(memberId);
        request.setTrustLevel(TrustLevel.WRITE);

        final ProcessTrusteeResponse response = management.processTrustee(request);
        throwIfFailed(response);
    }

    public static List<Trustee> fetchTrustees(final Management management, final String memberAccount, final String circleId) {
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class, memberAccount);
        request.setCircleId(circleId);
        final FetchTrusteeResponse response = management.fetchTrustees(request);

        throwIfFailed(response);
        return response.getTrustees();
    }

    public static String addData(final Share share, final String accountName, final String circleId, final String dataName, final byte[] data) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, accountName);
        request.setAction(Action.ADD);
        request.setCircleId(circleId);
        request.setDataName(dataName);
        request.setData(data);

        final ProcessDataResponse response = share.processData(request);
        throwIfFailed(response);

        return response.getDataId();
    }

    public static FetchDataResponse readFolderContent(final Share share, final String accountName, final String circleId, final String... folderId) {
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, accountName);
        request.setCircleId(circleId);
        if ((folderId != null) && (folderId.length == 1)) {
            request.setDataId(folderId[0]);
        }

        final FetchDataResponse response = share.fetchData(request);
        throwIfFailed(response);

        return response;
    }

    public static byte[] readData(final Share share, final String accountName, final String dataId) {
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, accountName);
        request.setDataId(dataId);

        final FetchDataResponse response = share.fetchData(request);
        throwIfFailed(response);

        return response.getData();
    }

    public static byte[] generateData(final int bytes) {
        final byte[] data;

        if (bytes > 0) {
            data = new byte[bytes];
            final SecureRandom random = new SecureRandom();
            random.nextBytes(data);
        } else {
            data = null;
        }

        return data;
    }

    public static byte[] toBytes(final String str) {
        return str.getBytes(Charset.defaultCharset());
    }

    public static String toString(final byte[] bytes) {
        return new String(bytes, Charset.defaultCharset());
    }

    private static void throwIfFailed(final CwsResponse response) {
        if (!response.isOk()) {
            throw new CWSClientException(response.getReturnMessage());
        }
    }
}
