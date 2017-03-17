/*
 * =============================================================================
 * Copyright (c) 2010-2017, JavaDog.IO
 * -----------------------------------------------------------------------------
 * Project: ZObEL (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api;

import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.VersionResponse;

import javax.jws.WebService;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@WebService
public interface System {

    /**
     * <p>Returns the current Version of the running CWS instance. This method
     * serves mainly as a simply way to check if the system is operational.</p>
     *
     * @return The CWS Version
     */
    VersionResponse version();

    SettingResponse settings(SettingRequest request);

    FetchMemberResponse fetchMembers(FetchMemberRequest request);

    /**
     * <p>When a fresh installation is made, the Database contain one account,
     * which is the System Administrator (admin), this account does not contain
     * any pre-defined security settings, i.e. no Asymmetric Key. Rather, the
     * first invocation will set the Key on the account, based on the security
     * credentials provided. This is done to avoid that the System Administrator
     * account remain unchanged, i.e. that the default information is still
     * present.</p>
     *
     * <p>Any subsequent ProcessMember request made, must be one of the
     * following:</p>
     * <ul>
     *   <li>Add new Member</li>
     *   <li>Update existing Member</li>
     *   <li>Remove Member</li>
     * </ul>
     *
     * <p><b>Add new Member:</b> <i>Adding a new Member can be done by either
     * the System Administrator or a Circle Administrator, in preparation to
     * adding the Member to one or more Circles being administrated by the
     * Circle Administrator.</i></p>
     *
     * <p><b>Update existing Member</b> <i>Updating an existing Member can only
     * be done by Members themselves. The information to update, is the
     * Credentials and Member name. As long as the Member name doesn't conflict
     * with existing Member names.</i></p>
     *
     * <p><b>Remove Member</b> <i>Removing a Member from the system, can be done
     * either by the Member (to remove themselves) or by the System
     * Administrator. A Circle Administrator cannot remove a Member from the
     * system. Removal of a Member will automatically remove all Data belonging
     * to the Member, i.e. the Circle relations and the Member details.</i></p>
     *
     * @param request Request Object with details for the Processing
     * @return Response Object with the result of the Processing
     */
    ProcessMemberResponse processMember(ProcessMemberRequest request);

    /**
     * <p>Retrieval of a Circle can be made with or without a Circle Id. If no Id
     * is given in the request, then CWS will simply return a list of all existing
     * Circles. If the Id was given, then the CWS will return the found Circle and
     * the list of Trustees for the Circle, i.e. the Members who have access and
     * their current Trust Level.</p>
     *
     * <p>If No Circle was found for a given Id, or if a different error occurred
     * during the handling of the Request, then an error is set and both the Circle
     * and Trustee Lists returned will be empty</p>
     *
     * @param request Fetch Circle Request Object
     * @return Fetch Circle Response Object with error information
     */
    FetchCircleResponse fetchCircles(FetchCircleRequest request);

    ProcessCircleResponse processCircle(ProcessCircleRequest request);
}
