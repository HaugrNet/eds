package io.javadog.cws.api;

import io.javadog.cws.api.dtos.Member;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.VersionResponse;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public interface System {

    /**
     * <p>Returns the current Version of the running CWS instance.</p>
     *
     * @return The CWS Version
     */
    VersionResponse version();

    /**
     * <p>Retrieves a collection of Circles from the CWS. A Circle, is used to
     * share data between trusted Members. A Member can be anything from a
     * Person, Organization, Company, etc.</p>
     *
     * @param member
     * @param request
     * @return
     */
    FetchCircleResponse fetchCircles(Member member, FetchCircleRequest request);
    ProcessCircleResponse processCircle(Member member, ProcessCircleRequest request);

    FetchMemberResponse fetchMembers(Member member, FetchMemberRequest request);
    ProcessMemberResponse processMember(Member member, ProcessMemberRequest request);
}
