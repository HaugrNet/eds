/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api;

import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import io.javadog.cws.api.requests.SanityRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.ProcessTrusteeResponse;
import io.javadog.cws.api.responses.SanityResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.VersionResponse;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public interface Management {

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
     * <p>Note, that the System Salt is only updateable as long as no Accounts
     * have been added, after which - it is no longer allowed to update it. This
     * is because the System Salt is used as part of the Passphrase to Key
     * process in the Authentication logic.</p>
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    SettingResponse settings(SettingRequest request);

    /**
     * <p>Data stored encrypted is nothing but a long array of bytes. If, over
     * time, an error occurs the da storage so a few bits have been flipped,
     * then it is not possible to decrypt the data.</p>
     *
     * <p>The stored data is having a checksum, which is written when the data
     * is stored and checked when the data is read out. The checksum is made
     * from the encrypted data.</p>
     *
     * <p>The build-in sanity checks will run over all persisted data either at
     * predefined intervals or during startup. If a record is no longer valid,
     * i.e. the checksum becomes invalid, then the record is marked as failed,
     * and thus unusable. It should be possible for administrators to recover
     * these from backups, but it requires that it is known when the failure
     * occurred - which this request can help with ascertaining.</p>
     *
     * <p>The request can be invoked by the System Administrator, which can then
     * return a complete list of all failures, or it can be invoked by a
     * Circle Administrator, and result in a list of Objects failing for a given
     * Circle or all Circles Administrated by the Circle Administrator. It is
     * also possible to provide a timestamp, to only get failures reported since
     * a certain time.</p>
     *
     * <p>The response Object contain a Map of ObjectIds which has failed, with
     * the value being the timestamp of the first check where it failed.</p>
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    SanityResponse sanitized(SanityRequest request);

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
     * <p>It is also possible, via this request, to invalidate the account,
     * meaning that the account will appear correct, but it is not possible to
     * either read or write data in any Circles. The Account can be later
     * corrected, by replacing all existing Trustee records.</p>
     *
     * @param request Request Object with details for the Processing
     * @return Response Object with the result of the Processing
     */
    ProcessMemberResponse processMember(ProcessMemberRequest request);

    /**
     * <p>This request will fetch a list of all Circles in the system.</p>
     *
     * @param request Fetch Circle Request Object
     * @return Fetch Circle Response Object with error information
     */
    FetchCircleResponse fetchCircles(FetchCircleRequest request);

    /**
     * <p>With this request it is possible to process Circles. The request may
     * be invoked by all members, allowing members to create the different types
     * of connections needed to sharing of data.</p>
     *
     * <p>If a request to create a new Circle is made by a member, then the
     * initial Circle Administrator will automatically be set to the Member. If
     * the request is made by the System administrator, then a Member Id is
     * required, as it is not allowed for the System Administrator to be part of
     * any Circles.</p>
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
     * </ul>
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    ProcessCircleResponse processCircle(ProcessCircleRequest request);

    /**
     * <p>With this request, it is possible to retrieve the list of Trustees,
     * belonging to a Circle. A Trustee, is a member who has been granted access
     * to a Circle either as Circle Administrator, Write access or Read access
     * only.</p>
     *
     * <p>If No Circle was found for a given Id, or if a different error occurred
     * during the handling of the Request, then an error is set and the list of
     * Trustee returned will be empty</p>
     *
     * @param request Fetch Circle Request Object
     * @return Fetch Circle Response Object with error information
     */
    FetchTrusteeResponse fetchTrustees(FetchTrusteeRequest request);

    /**
     * <p>With this request it is possible to process Trustees. A Trustee, is a
     * Member which has been granted access to a Circle, and thereby is able to
     * access the Circle Key to both encrypt and decrypt Circle Data.</p>
     *
     * <p>The Request supports the following Actions:</p>
     * <ul>
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
    ProcessTrusteeResponse processTrustee(ProcessTrusteeRequest request);
}
