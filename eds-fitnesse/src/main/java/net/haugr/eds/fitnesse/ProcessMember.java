/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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
package net.haugr.eds.fitnesse;

import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.fitnesse.callers.CallManagement;
import net.haugr.eds.fitnesse.utils.Converter;

/**
 * <p>FitNesse Fixture for the EDS ProcessMember feature.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class ProcessMember extends EDSRequest<ProcessMemberResponse> {

    private Action action = null;
    private MemberRole memberRole = null;
    private String memberId = null;
    private String publicKey = null;
    private String newAccountName = null;
    private byte[] newCredential = null;

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setAction(final String action) {
        this.action = Converter.findAction(action);
    }

    public void setMemberRole(final String memberRole) {
        this.memberRole = Converter.findMemberRole(memberRole);
    }

    public void setMemberId(final String memberId) {
        this.memberId = getId(Converter.preCheck(memberId));
    }

    public void setPublicKey(final String publicKey) {
        this.publicKey = Converter.preCheck(publicKey);
    }

    public void setNewAccountName(final String newAccountName) {
        this.newAccountName = Converter.preCheck(newAccountName);
    }

    public void setNewCredential(final String newCredential) {
        this.newCredential = Converter.convertBytes(newCredential);
    }

    public String memberId() {
        return ((newAccountName != null) && (response.getMemberId() != null)) ? (newAccountName + EXTENSION_ID) : null;
    }

    public String signature() {
        return ((newAccountName != null) && (response.getSignature() != null)) ? (newAccountName + EXTENSION_SIGNATURE) : null;
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class);
        request.setAction(action);
        request.setMemberRole(memberRole);
        request.setMemberId(memberId);
        request.setPublicKey(publicKey);
        request.setNewAccountName(newAccountName);
        request.setNewCredential(newCredential);

        response = CallManagement.processMember(requestUrl, request);

        // Ensuring that the internal mapping of Ids with accounts being
        // used is synchronized.
        processId(action, memberId, newAccountName, response);
        setSignature(newAccountName + EXTENSION_SIGNATURE, response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();

        // Reset internal values
        action = null;
        memberRole = null;
        memberId = null;
        publicKey = null;
        newAccountName = null;
        newCredential = null;
    }
}
