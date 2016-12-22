package io.javadog.cws.api.common;

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
@XmlType(name = "action")
public enum Action {

    /**
     * <p>If no Actions are allowed, this is a dummy value.</p>
     */
    NONE,

    /**
     * <p>The Action Process covers creating and updating records.</p>
     */
    PROCESS,

    /**
     * <p>The Action Delete covers removal of records, which is a permanent
     * irreversible Action.</p>
     */
    DELETE,

    /**
     * <p>This is a special Action, which will enforce a complete re-encryption
     * of all data belonging the Object in question.</p>
     *
     * <p>Note, that only for those Objects where a Key is stored, is this
     * Action allowed, which include processing Members and processing Circles.
     * For other requests, this Action is not allowed.</p>
     */
    REKEY
}
