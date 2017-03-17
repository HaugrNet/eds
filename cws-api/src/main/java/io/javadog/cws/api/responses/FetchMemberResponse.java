/*
 * =============================================================================
 * Copyright (c) 2010-2017, JavaDog.IO
 * -----------------------------------------------------------------------------
 * Project: ZObEL (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.responses;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Circle;
import io.javadog.cws.api.dtos.Member;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fetchMemberResponse", propOrder = { "members", "circles" })
public final class FetchMemberResponse extends CWSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    private final List<Member> members = new ArrayList<>(0);
    private final List<Circle> circles = new ArrayList<>(0);

    public FetchMemberResponse() {
        // Empty Constructor, required for WebServices
    }

    public FetchMemberResponse(final int returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    // =========================================================================
    // Setters & Getters
    // =========================================================================

    public void setMembers(final List<Member> members) {
        this.members.addAll(members);
    }

    public List<Member> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public void setCircles(final List<Circle> circles) {
        this.circles.addAll(circles);
    }

    public List<Circle> getCircles() {
        return Collections.unmodifiableList(circles);
    }
}
