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
package net.haugr.eds.api;

import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.FetchCircleRequest;
import net.haugr.eds.api.requests.FetchMemberRequest;
import net.haugr.eds.api.requests.FetchTrusteeRequest;
import net.haugr.eds.api.requests.MasterKeyRequest;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.requests.ProcessTrusteeRequest;
import net.haugr.eds.api.requests.SanityRequest;
import net.haugr.eds.api.requests.SettingRequest;
import net.haugr.eds.api.responses.AuthenticateResponse;
import net.haugr.eds.api.responses.FetchCircleResponse;
import net.haugr.eds.api.responses.FetchMemberResponse;
import net.haugr.eds.api.responses.FetchTrusteeResponse;
import net.haugr.eds.api.responses.MasterKeyResponse;
import net.haugr.eds.api.responses.ProcessCircleResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.api.responses.ProcessTrusteeResponse;
import net.haugr.eds.api.responses.SanityResponse;
import net.haugr.eds.api.responses.SettingResponse;
import net.haugr.eds.api.responses.VersionResponse;

/**
 * This interface contains the functionality needed to set up, configure, and
 * control an EDS instance.
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public interface Management {

    /**
     * <p>Returns the current Version of the running EDS instance. This method
     * serves mainly as a simple way to check if the system is operational.</p>
     *
     * @return The EDS Version
     */
    VersionResponse version();

    /**
     * <p>This request allows the System Administrator to read and alter the
     * Settings of this EDS system. Please be aware that some fields cannot
     * be altered once the system is started, as it may have fatal consequences
     * for running the system.</p>
     *
     * <p>The following is the standard settings, and the values which they will
     * expect. If the value is invalid or cannot be transferred into something
     * that the EDS can understand, then no entries are being updated and the
     * request will respond with a warning</p>
     *
     * <ul>
     *   <li>
     *     <b>eds.crypto.symmetric.algorithm</b> - default value: 'AES_GCM_256'<br>
     *     Allowed Values: 'AES_GCM_256'<br>
     *     <i>The Algorithm used for the Symmetric Keys in EDS. All data is
     *     stored using this Algorithm. Although it can be changed, please test
     *     the EDS carefully before doing so. And please be aware that the
     *     information here is only used for generating new Keys, so changing
     *     things will not affect existing records.</i>
     *   </li>
     *   <li>
     *     <b>eds.crypto.asymmetric.algorithm</b> - default value: 'MLKEM1024_P384'<br>
     *     Allowed Values: 'MLKEM1024_P384' (hybrid ML-KEM-1024 + ECDH P-384)<br>
     *     <i> Asymmetric Encryption (Public &amp; Private Key) is used for
     *     sharing the Symmetric Keys, not for encrypting any data.</i>
     *   </li>
     *   <li>
     *     <b>eds.crypto.pbe.algorithm</b> - default value: 'PBE_GCM_256'<br>
     *     Allowed Values: 'PBE_GCM_256'<br>
     *     <i>If a Member is using something else than a Key to unlock their
     *     Account, the EDS will use the following Password Based Encryption,
     *     PBE, algorithm to do the trick. The provided information is extended
     *     with an instance-specific Salt and a Member Account-specific Salt to
     *     ensure that enough entropy is available to create a strong enough Key
     *     to unlock the Private Key for the Account.</i>
     *   </li>
     *   <li>
     *     <b>eds.crypto.pbe.iterations</b> - default value: '210000'<br>
     *     Allowed Values: A number, at least 1000<br>
     *     <i>The PBE algorithm (PBKDF2 with HMAC-SHA512) is extending the Member
     *     credentials with the Salt over a number of iterations. The setting
     *     is used when a passphrase is set, and kept with the account, which
     *     uses its own number at each login; so it can be changed at any
     *     time, and counts from the next change of a passphrase. The higher the
     *     number, the more costly it is to guess the credentials, if the
     *     database is stolen - but it is also more costly for each request
     *     made with credentials. The default follows the recommendation from
     *     the <a href="https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html">OWASP Password Storage Cheat Sheet</a>.
     *     Lowering it is only advisable for testing. See <a href="https://en.wikipedia.org/wiki/PBKDF2">Wikipedia</a>
     *     for more information.</i>
     *   </li>
     *   <li>
     *     <b>eds.crypto.hash.algorithm</b> - default value: 'SHA_512'<br>
     *     Allowed Values: 'SHA_384' or 'SHA_512'<br>
     *     <i>For the CheckSums or Fingerprints we're generating - for stored
     *     Data Objects, it is a simple mechanism to ensure the integrity of
     *     the stored data.</i>
     *   </li>
     *   <li>
     *     <b>eds.system.locale</b> - default value: 'EN'<br>
     *     Allowed Values: Any language abbreviation<br>
     *     <i>For correctly dealing with Strings, it is important that the
     *     Locale is set to ensure that it is done properly. By default, the
     *     Locale is English (EN), but if preferred, any other can be chosen. As
     *     long as they follow the <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IETF BCP 47</a>
     *     allowed values.</i>
     *   </li>
     *   <li>
     *     <b>eds.system.charset</b> - default value: 'UTF-8'<br>
     *     Allowed Values: Any Character Set supported by Java<br>
     *     <i>When applying armoring to the raw keys, it means using a Base64
     *     encoding and decoding. However, they have to be saved using a
     *     character set. Any character set can be used, but if keys have been
     *     stored using one, changing it will cause problems as they may not be
     *     read out safely again. So, please only change this if you are really
     *     sure.</i>
     *   </li>
     *   <li>
     *     <b>eds.show.all.circles</b> - default value: 'false'<br>
     *     Allowed Values: Boolean - 'true' or 'false'<br>
     *     <i>Exposing all Circles means that it is possible for a member,
     *     other than the System Administrator, to be able to view Circles who
     *     they are not having a Trustee relationship with - If the value is set
     *     to true.<br>
     *       If the value is set to false, then it is only possible to extract a
     *     list of Circles with whom the Member is having a Trustee relationship
     *     with.</i>
     *   </li>
     *   <li>
     *     <b>eds.show.trustees</b> - default value: true<br>
     *     Allowed Values: Boolean - 'true' or 'false'<br>
     *     <i>Privacy is important, however - there may be reasons to reduce the
     *     privacy level and allow that a Member can view information about
     *     other Members even if there is no direct relation between the two. If
     *     two members share a Circle, then they will automatically be able to
     *     view each other, but if not, then this setting applies. By default, it
     *     is set to True - as EDS should be used by organizations or companies
     *     where all members already share information.</i>
     *   </li>
     *   <li>
     *     <b>eds.sanity.check.startup</b> - default value: 'true'<br>
     *     Allowed Values: Boolean - 'true' or 'false'<br>
     *     <i> Overtime, it can happen that the data is deteriorating. Meaning
     *     that some bits can change and thus result in data which cannot
     *     be recovered as the decryption will give a completely false Object
     *     back. When data is stored, it is having a checksum of the encrypted
     *     bytes, which is also read out when the data is requested. If the
     *     checksum fails, then it is not possible to recover the original data
     *     anymore.<br>
     *       However, as most systems also use backups, it is possible to
     *     recover the encrypted data from a backup, but the question is how far
     *     back the backup has to go. To ensure that a backup is correct and
     *     that there are no problems in the database, the sanity checks can be
     *     enabled at startup, meaning that when EDS is started up, all
     *     encrypted data is checked and verified. If a check fails - then the
     *     field is marked with a failed Sanity check, and the date of the
     *     check.</i>
     *   </li>
     *   <li>
     *     <b>eds.sanity.check.interval.days</b> - default value: '180'<br>
     *     Allowed Values: Any integer<br>
     *     <i>Please see the comment for the 'eds.sanity.check.startup', for the
     *     motivation and reason for the sanity check. This setting sets the
     *     interval at which the sanity checks should be made. By default, it
     *     is set to 180 days, but it can be altered if needed.</i>
     *   </li>
     *   <li>
     *     <b>eds.session.timeout.minutes</b> - default value: '480'<br>
     *     Allowed Values: Any integer<br>
     *     <i>The maximum amount of time a Session may be used.</i>
     *   </li>
     *   <li>
     *     <b>eds.session.max.per.member</b> - default value: '10'<br>
     *     Allowed Values: Any integer from 1<br>
     *     <i>The Sessions a Member may have at a time, one per login. A
     *     further login ends the Session which expires first.</i>
     *   </li>
     *   <li>
     *     <b>eds.masterkey.url</b> - default value: ''<br>
     *     Allowed Values: Any valid URL<br>
     *     <i>Reference to a series of bytes, which will be used as the secret
     *     when generating the MasterKey. It must be reachable for the EDS
     *     instance(s) at startup.</i>
     *   </li>
     *   <li>
     *     <b>eds.cors.value</b> - default value: '*'<br>
     *     Allowed Values: See <a href="https://enable-cors.org/">enable-cors</a><br>
     *     <i>The Cross Origin Resource Sharing value, which determines who may
     *     invoke the EDS. It is required for most JavaScript-based Clients.</i>
     *   </li>
     * </ul>
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    SettingResponse settings(SettingRequest request);

    /**
     * <p>The Master Key is a special symmetric key, which is used to encrypt
     * and decrypt all Initial Vectors &amp; Member Salt values. The key is only
     * kept in memory and is never persisted. Meaning that it must be set when
     * the EDS instance is started - otherwise, the entire system is rendered
     * useless.</p>
     *
     * <p>There is a default Master Key, which is based on known values in the
     * system. So, even if this key is not set by an Administrator, it is still
     * used throughout the system. However, as the default key is based on known
     * values, it cannot be used to enhance security. To achieve this, it
     * must be set by the System Administrator.</p>
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    MasterKeyResponse masterKey(MasterKeyRequest request);

    /**
     * <p>Data stored encrypted is nothing but a long array of bytes. If, over
     * time, an error occurs in the data storage so a few bits have been flipped,
     * then it is not possible to decrypt the data.</p>
     *
     * <p>The stored data is having a checksum, which is written when the data
     * is stored and checked when the data is read out. The checksum is made
     * from the encrypted data.</p>
     *
     * <p>The build-in sanity checks will run over all persisted data either at
     * predefined intervals or during startup. If a record is no longer valid.
     * I.e., the checksum becomes invalid. Then the record is marked as failed,
     * and thus unusable. It should be possible for administrators to recover
     * these from backups, but it requires that it is known when the failure
     * occurred - which this request can help with finding out.</p>
     *
     * <p>The request can be invoked by the System Administrator, which can then
     * return a complete list of all failures, or it can be invoked by a
     * Circle Administrator and result in a list of Objects failing. It is also
     * possible to provide a timestamp to only get failures reported since
     * a certain time.</p>
     *
     * <p>The response Object contains a Map of ObjectIds which has failed, with
     * the value being the timestamp of the first check where it failed.</p>
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    SanityResponse sanitized(SanityRequest request);

    /**
     * <p>Simple check to see if a Member is authenticated to work with this
     * EDS instance. The Response will return {@link ReturnCode#SUCCESS}
     * if authentication information exists, otherwise an error is returned
     * together with a more detailed message.</p>
     *
     * @param request Authentication Request Object
     * @return Response Object with Authentication and Return values
     */
    AuthenticateResponse authenticated(Authentication request);

    /**
     * <p>Allows the retrieval of existing Member Accounts from the System if
     * a specific Account is requested, then a list of Circles where the Account
     * is also a Trustee is returned.</p>
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    FetchMemberResponse fetchMembers(FetchMemberRequest request);

    /**
     * <p>When a fresh installation is made, the Database contains one account,
     * which is the System Administrator (admin). This account does not contain
     * any pre-defined security settings, i.e., no Asymmetric Key. Rather, the
     * first invocation will set the Key on the account, based on the security
     * credentials provided. This is done to avoid that the System Administrator
     * account remains unchanged, i.e., that the default information is still
     * present.</p>
     *
     * <p>Any following ProcessMember request made must be one of the
     * following:</p>
     * <ul>
     *   <li>Add new Member</li>
     *   <li>Alter Member Roles</li>
     *   <li>Login to link a Session temporarily with an Account</li>
     *   <li>Logout to stop linking an Account with a Session</li>
     *   <li>Update existing Member</li>
     *   <li>Update Credential</li>
     *   <li>Remove Member</li>
     * </ul>
     *
     * <p><b>Add new Member:</b> <i>Adding a new Member can be done by either
     * the System Administrator or a Circle Administrator, in preparation to
     * adding the Member to one or more Circles being administrated by the
     * Circle Administrator.</i></p>
     *
     * <p><b>Alter Member:</b> <i>It is possible for a System Administrator to
     * alter Accounts and change the Role - which may allow someone else to
     * become System Administrator, or simply revoke the role.</i></p>
     *
     * <p>A special variant for creating new Accounts is via Invitations, where
     * the System Administrator can issue an invitation to a potential Member.
     * The Response then contains a one-time invitation token, which the
     * Member uses to set their own credentials.</p>
     *
     * <p><b>Login / Logout</b> <i>Linking and unlinking a Session with an
     * Account. This is to help websites to use EDS without needing to store
     * the Account credentials unsafely.</i></p>
     *
     * <p><b>Update existing Member</b> <i>This action will only affect a
     * member's own Account. Whereby it is allowed to change the Account Name,
     * provided the name has not been taken. It is also possible to update the
     * credentials (PassPhrase) and to change the Public Key belonging to an
     * account and which others may see.</i></p>
     *
     * <p><b>Update Credential</b> <i>It is possible for an active member to
     * update the credentials used for authentication, but it is not possible
     * to update the credentials for anyone else. Not even an administrator can
     * update the credentials for anyone else. The reason for this is that the
     * credentials are used to unlock the internal keys, and this cannot be done
     * by anyone else than the current owner of an account.</i></p>
     *
     * <p><b>Remove Member</b> <i>Removing a Member from the system can be done
     * either by the Member (to remove themselves) or by the System
     * Administrator. A Circle Administrator cannot remove a Member from the
     * system. Removal of a Member will automatically remove all Data belonging
     * to the Member, i.e., the Circle relations and the Member details.</i></p>
     *
     * <p>It is also possible, via this request, to invalidate the account,
     * meaning that the account will appear correct, but it is not possible to
     * either read or write data in any Circles. The Account can be later
     * corrected by replacing all existing Trustee records.</p>
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
     * <p>If a member makes a request to create a new Circle, then the
     * initial Circle Administrator will automatically be set to the Member. If
     * the request is made by the System administrator, then a Member Id is
     * required, as it is not allowed for the System Administrator to be part of
     * any Circles.</p>
     *
     * <p>The Request supports the following Actions:</p>
     * <ul>
     *   <li><b>{@link Action#CREATE}</b> a new
     *   Circle. This can only be performed by the System Administrator. It
     *   allows the creation of a new Circle with a default new Circle
     *   Administrator, which cannot be the System Administrator.</li>
     *   <li><b>{@link Action#UPDATE}</b> an existing
     *   Circle, i.e., rename it.</li>
     *   <li><b>{@link Action#DELETE}</b> an existing
     *   Circle from the System. This action cannot be reverted - once Deleted,
     *   the Keys and Data will also be deleted.</li>
     * </ul>
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    ProcessCircleResponse processCircle(ProcessCircleRequest request);

    /**
     * <p>With this request, it is possible to retrieve a list of Trustees,
     * belonging to either a Member or a Circle. A Trustee is a member who
     * has been granted access to a Circle either as Circle Administrator,
     * Write access, or Read access only.</p>
     *
     * <p>If no MemberId or CircleId is provided, a listing of the requesting
     * Member's Trustee information is being returned. If both MemberId and
     * CircleId are set, then a list with either one or zero records is being
     * returned.</p>
     *
     * <p>It should be noted that the request is limited in what it returns,
     * so unless the requesting user is System Administrator, only Trustee
     * information from Circles of Trust, of which the user belongs too, is
     * returned. If no information can be found, an error is returned.</p>
     *
     * @param request Fetch Circle Request Object
     * @return Fetch Circle Response Object with error information
     */
    FetchTrusteeResponse fetchTrustees(FetchTrusteeRequest request);

    /**
     * <p>With this request it is possible to process Trustees. A Trustee is a
     * Member that has been granted access to a Circle, and thereby is able to
     * access the Circle Key to both encrypt and decrypt Circle Data.</p>
     *
     * <p>The Request supports the following Actions:</p>
     * <ul>
     *   <li><b>{@link Action#ADD}</b> a new Trustee
     *   to the Circle, i.e., an Account other than the System Administrator with
     *   a specific Trust level.</li>
     *   <li><b>{@link Action#ALTER}</b> the level of
     *   trust for a given Trustee towards the Circle.</li>
     *   <li><b>{@link Action#REMOVE}</b> a Trustee
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
