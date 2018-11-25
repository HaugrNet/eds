/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.api.common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>When invoking a processing Request to the CWS, it must be with a specific
 * type of Action, which may or may not be allowed.</p>
 *
 * <p>This enumerated type contain all the allowed Actions, and each Request
 * will allow one or more of these.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "action")
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
     * <p>The Action Update, is for updating existing records.</p>
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
     * <p>This Action allows someone who have write-access to a Circle, to copy
     * a Data Object to another Circle, where they have write access.</p>
     */
    COPY,

    /**
     * <p>This Action allows someone who have write-access to a Circle, to move
     * a Data Object to another Circle, where they have write access.</p>
     */
    MOVE,

    /**
     * <p>This Action Alters a Trustee's relation to a Circle, which means
     * change the Trust Level. If invoked by the System Administrator, then it
     * can only be used to alter the TrustLevel, not to grant access to the
     * Data by giving the Member access to the Circle Key, as the System
     * Administrator itself is not having access to this.</p>
     */
    ALTER,

    /**
     * <p>Invalidating an Account, means that the Account is being made
     * unreadable and it is not possible for Account to be used again.</p>
     */
    INVALIDATE,

    /**
     * <p>This Action allows a Circle or System Administrator to remove a Member
     * from a Circle. A Circle Administrator cannot remove themselves from a
     * Circle.</p>
     */
    REMOVE
}
