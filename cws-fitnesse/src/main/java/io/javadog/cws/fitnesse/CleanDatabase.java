/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-fitnesse)
 * =============================================================================
 */
package io.javadog.cws.fitnesse;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.dtos.Circle;
import io.javadog.cws.api.dtos.Member;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.fitnesse.callers.CallManagement;
import io.javadog.cws.fitnesse.exceptions.StopTestException;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;

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
 * @since  CWS 1.0
 */
public final class CleanDatabase {

    public void removeCircles() {
        final FetchCircleRequest fetchRequest = prepareRequest(FetchCircleRequest.class);
        final ProcessCircleRequest processRequest = prepareRequest(ProcessCircleRequest.class);
        processRequest.setAction(Action.DELETE);

        final FetchCircleResponse fetchResponse = CallManagement.fetchCircles(fetchRequest);
        for (final Circle circle : fetchResponse.getCircles()) {
            processRequest.setCircleId(circle.getCircleId());
            CallManagement.processCircle(processRequest);
        }
    }

    public void removeMembers() {
        final FetchMemberRequest fetchRequest = prepareRequest(FetchMemberRequest.class);
        final ProcessMemberRequest processRequest = prepareRequest(ProcessMemberRequest.class);
        processRequest.setAction(Action.DELETE);

        final FetchMemberResponse fetchResponse = CallManagement.fetchMembers(fetchRequest);
        for (final Member member : fetchResponse.getMembers()) {
//            if (!Objects.equals(member.getAccountName(), Constants.ADMIN_ACCOUNT)) {
                processRequest.setMemberId(member.getMemberId());
                CallManagement.processMember(processRequest);
//            }
        }
    }

    private static <T extends Authentication> T prepareRequest(final Class<T> clazz) {
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
