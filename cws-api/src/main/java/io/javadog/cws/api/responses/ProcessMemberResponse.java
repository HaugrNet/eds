/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.responses;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Member;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "processMemberResponse", propOrder = { "id", "member", "armoredKey" })
public final class ProcessMemberResponse extends CWSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement private String id = null;
    @XmlElement private Member member = null;
    @XmlElement private String armoredKey = null;

    public ProcessMemberResponse() {
        // Empty Constructor, required for WebServices
    }

    public ProcessMemberResponse(final int returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setId(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setMember(final Member member) {
        this.member = member;
    }

    public Member getMember() {
        return member;
    }

    public void setArmoredKey(final String armoredKey) {
        this.armoredKey = armoredKey;
    }

    public String getArmoredKey() {
        return armoredKey;
    }
}
