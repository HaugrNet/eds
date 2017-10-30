/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
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

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public interface System {

    /**
     * <p>Returns the current Version of the running CWS instance. This method
     * serves mainly as a simply way to check if the system is operational.</p>
     *
     * @return The CWS Version
     */
    VersionResponse version();

    /**
     * <p>This request allows the System Administrator to read and alter the
     * Settings of this CWS system. Please be aware, that some fields cannot
     * be altered once the system is started, as it may have fatal consequences
     * for running system.</p>
     *
     * <p>The following Settings exists:</p>
     * <ul>
     *   <li><b>cws.crypto.symmetric.algorithm</b> - updateable</li>
     *   <li><b>cws.crypto.asymmetric.algorithm</b> - updateable</li>
     *   <li><b>cws.crypto.signature.algorithm</b> - updateable</li>
     *   <li><b>cws.crypto.password.algorithm</b> - updateable</li>
     *   <li><b>cws.crypto.hash.algorithm</b> - updateable</li>
     *   <li><b>cws.system.salt</b> - not updateable</li>
     *   <li><b>cws.system.locale</b> - updateable</li>
     *   <li><b>cws.system.charset</b> - updateable</li>
     *   <li><b>cws.expose.admin</b> - updateable</li>
     *   <li><b>cws.show.trustees</b> - updateable</li>
     * </ul>
     *
     * <p>The System Salt is not updateable via this request, as it is will then
     * act as a &quot;kill-switch&quot;. This value must be updated via standard
     * SQL updates. Although it is a fairly important feature, it should not be
     * too easy to change it by mistake.</p>
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    SettingResponse settings(SettingRequest request);

    /**
     * <p>Allows the retrieval of existing Member Accounts from the System, if
     * a specific Account is requested, then a list of Circles where the Account
     * is also a Trustee is returned.</p>
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
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
     * <p>A special variant for creating new Accounts is via Invitations, where
     * the System Administrator can issue a signed invitation to a potential
     * Member. If this is made, then the armoredKey, will contain a Signature,
     * rather than a Private Key in the Response.</p>
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

    /**
     * <p>With this request it is possible to process Circles and Trustees. A
     * Trustee, is an Account which has been granted access to a Circle, and
     * thus also has access to the the Circle Key to both encrypt and decrypt
     * the data belonging to a Circle.</p>
     *
     * <p>The Request supports the following Actions:</p>
     * <ul>
     *   <li><b>{@link io.javadog.cws.api.common.Action#CREATE}</b> a new
     *   Circle. This can only be performed by the System Administrator. It
     *   allows the creation of a new Circle with a default new Circle
     *   Administrator, which cannot be the System Administrator.</li>
     *   <li><b>{@link io.javadog.cws.api.common.Action#UPDATE}</b> an existing
     *   Circle, i.e. rename it.</li>
     *   <li><b>{@link io.javadog.cws.api.common.Action#DELETE}</b> an existing
     *   Circle from the System. This action cannot be reverted - once Deleted,
     *   the Keys and Data will also be deleted.</li>
     *   <li><b>{@link io.javadog.cws.api.common.Action#ADD}</b> a new Trustee
     *   to the Circle, i.e. an Account other than the System Administrator with
     *   a specific Trust level.</li>
     *   <li><b>{@link io.javadog.cws.api.common.Action#ALTER}</b> the level of
     *   trust for a given Trustee towards the Circle.</li>
     *   <li><b>{@link io.javadog.cws.api.common.Action#REMOVE}</b> a Trustee
     *   from the Circle, meaning that the Account will no longer be able to
     *   access any data belonging to the Circle.</li>
     * </ul>
     *
     * <p>The System Administrator is not allowed to be a Trustee of a Circle,
     * as this may pose as a conflict of interest or potentially as a Security
     * issue.</p>
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    ProcessCircleResponse processCircle(ProcessCircleRequest request);
}
