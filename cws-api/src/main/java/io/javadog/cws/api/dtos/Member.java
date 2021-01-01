/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.api.dtos;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.MemberRole;
import io.javadog.cws.api.common.Utilities;
import java.io.Serializable;
import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>The Member Object, is used as Accounts in CWS. The Object consists of an
 * Authentication Object, a description, creation and modification dates.</p>
 *
 * <p>The Authentication Object is mandatory, the description is optional, and
 * only serves as a hint for other Members.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = Constants.FIELD_MEMBER, propOrder = { Constants.FIELD_MEMBER_ID, Constants.FIELD_ACCOUNT_NAME, Constants.FIELD_MEMBER_ROLE, Constants.FIELD_PUBLIC_KEY, Constants.FIELD_ADDED })
public final class Member implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = Constants.FIELD_MEMBER_ID, required = true)
    private String memberId = null;

    @XmlElement(name = Constants.FIELD_ACCOUNT_NAME, required = true)
    private String accountName = null;

    @XmlElement(name = Constants.FIELD_MEMBER_ROLE, required = true)
    private MemberRole memberRole = null;

    // The Public Key is an optional value which may or may not be provided,
    // hence it is only stored but not used for anything. For the same reason,
    // it is not used as part of the Standard Object methods, #equals(),
    // #hashCode() and #toString().
    @XmlElement(name = Constants.FIELD_PUBLIC_KEY, required = true)
    private String publicKey = null;

    @XmlElement(name = Constants.FIELD_ADDED, required = true)
    private Date added = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setMemberId(final String memberId) {
        this.memberId = memberId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setMemberRole(final MemberRole memberRole) {
        this.memberRole = memberRole;
    }

    public MemberRole getMemberRole() {
        return memberRole;
    }

    public void setPublicKey(final String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setAdded(final Date added) {
        this.added = Utilities.copy(added);
    }

    public Date getAdded() {
        return Utilities.copy(added);
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Member{" +
                "memberId='" + memberId + '\'' +
                ", accountName='" + accountName + '\'' +
                ", memberRole='" + memberRole + '\'' +
                ", added=" + added +
                '}';
    }
}
