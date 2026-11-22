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
package net.haugr.eds.api.common;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>When invoking a processing Request to the EDS, it must be with a specific
 * type of Action, which may or may not be allowed.</p>
 *
 * <p>These enumerated types contain all the allowed Actions, and each Request
 * will allow one or more of these.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Schema(description = "The operation a processing request performs. The endpoints set it themselves, so clients normally don't send it. Values: "
        + "PROCESS (generic processing), CREATE (create a Member or Circle), UPDATE (update a Member, Circle, Data Object or Data Type), "
        + "INVITE (invite a new Member), LOGIN (start a session), LOGOUT (end a session), DELETE (delete a Member, Circle, "
        + "Data Object or Data Type), ADD (add a Data Object or Trustee), COPY (copy a Data Object to another Circle), MOVE (move "
        + "a Data Object to another Circle or folder), ALTER (change a Member's role or a Trustee's trust level), INVALIDATE "
        + "(invalidate a Member's credentials), REMOVE (remove a Trustee), SECRETS (set the secrets of several Trustees of a Circle), "
        + "ROTATE (rotate the Circle Secret of a Circle).")
public enum Action {

    /**
     * <p>The Action Process covers creating and updating records.</p>
     */
    PROCESS,

    /**
     * <p>The Action Create is for creating new records.</p>
     */
    CREATE,

    /**
     * <p>The Action Update is for updating existing records.</p>
     */
    UPDATE,

    /**
     * <p>For Invitations, which will allow the System Administrator to create
     * new Accounts by issuing a signed invitation.</p>
     */
    INVITE,

    /**
     * For creating a Session for an existing Account, the Login Action must be
     * used.
     */
    LOGIN,

    /**
     * When a Session is no longer needed for an Account, the Logout Action must
     * be used.
     */
    LOGOUT,

    /**
     * <p>The Action Delete covers removal of records, which is a permanent
     * irreversible Action.</p>
     */
    DELETE,

    /**
     * <p>This Action adds a Trustee to a Circle.</p>
     */
    ADD,

    /**
     * <p>This Action allows someone who has write-access to a Circle to copy
     * a Data Object to another Circle, where they have write access.</p>
     */
    COPY,

    /**
     * <p>This Action allows someone who has write-access to a Circle to move
     * a Data Object to another Circle, where they have write access.</p>
     */
    MOVE,

    /**
     * <p>This Action is used when either altering an Account or a Trustee. If
     * the request was ProcessMember, then it is possible to change the Role of
     * the Member in the system, i.e., grant or revoke Administrative rights. If
     * the request was ProcessTrustee, then it is possible for a Circle
     * Administrator to change the Trust Level for another Trustee.</p>
     *
     * <p>Note; it is not possible for a System Administrator to grant someone
     * access to a Circle of Trust, unless the System Administrator is part of
     * the Circle.</p>
     */
    ALTER,

    /**
     * <p>Invalidating an Account means that the Account is being made
     * unreadable, and it is not possible for Account to be used again.</p>
     */
    INVALIDATE,

    /**
     * <p>This Action allows a Circle or System Administrator to remove a Member
     * from a Circle. A Circle Administrator cannot remove themselves from a
     * Circle.</p>
     */
    REMOVE,

    /**
     * <p>This Action allows a Circle Administrator to set the secrets of
     * several Trustees of a Circle, and the Circle's external key, in one
     * step, where either all or nothing is changed (E2E.md).</p>
     */
    SECRETS,

    /**
     * <p>This Action allows a Circle Administrator to have the Circle Secret
     * rotated now (PLAN.md, Crypto C6): the Circle gets a new secret, and its
     * Data Objects are re-encrypted with it in the background.</p>
     */
    ROTATE
}
