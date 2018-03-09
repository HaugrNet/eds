/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.fitnesse;

import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.dtos.Circle;
import io.javadog.cws.api.dtos.Member;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.fitnesse.exceptions.StopTestException;
import io.javadog.cws.fitnesse.utils.Converter;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public abstract class CwsRequest<R extends CwsResponse> {

    // Internal mapping of the Member AccountNames & the MemberId, so the tests
    // can refer to the names rather than the Id's, as the latter will change
    // per test run.
    protected Map<String, String> members = new HashMap<>();

    // Internal mapping of the Circle Names & the CircleId, so the tests can
    // refer to the names rather than the Id's, as the latter will change per
    // test run.
    protected Map<String, String> circles = new HashMap<>();

    protected String accountName = null;
    protected byte[] credential = null;
    protected R response = null;

    public void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    public void setCredential(final String credential) {
        this.credential = Converter.convertBytes(credential);
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
            request.setCredentialType(CredentialType.PASSPHRASE);
            request.setAccountName(accountName);
            request.setCredential(credential);

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
    }

    // =========================================================================
    // Operating the internal mapping of Members & Circles
    // =========================================================================

    protected void setMembers(final FetchMemberResponse memberResponse) {
        if (memberResponse != null) {
            for (final Member member : memberResponse.getMembers()) {
                members.put(member.getAccountName(), member.getMemberId());
            }
        }
    }

    protected String getMemberId(final String accountName) {
        return members.entrySet().stream()
                .filter((Map.Entry<String, String> entry) -> Objects.equals(entry.getKey(), accountName))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }

    protected String getMembersNames() {
        return members.keySet().toString();
    }

    protected void setCircles(final FetchCircleResponse circleResponse) {
        for (final Circle circle : circleResponse.getCircles()) {
            circles.put(circle.getCircleName(), circle.getCircleId());
        }
    }

    protected String getCircleId(final String circleName) {
        return circles.entrySet().stream()
                .filter((Map.Entry<String, String> entry) -> Objects.equals(entry.getKey(), circleName))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }

    protected String getCircleNames() {
        return circles.keySet().toString();
    }
}
