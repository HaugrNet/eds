/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

import static io.javadog.cws.api.common.Utilities.copy;

import io.javadog.cws.api.common.Constants;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * <p>The Member Object, is used as Accounts in CWS. The Object consists of an
 * Authentication Object, a description, creation and modification dates.</p>
 *
 * <p>The Authentication Object is mandatory, the description is optional, and
 * only serves as a hint for other Members.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "member", propOrder = { "memberId", "accountName",  "added" })
public final class Member implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final String FIELD_MEMBER_ID = "memberId";
    private static final String FIELD_AUTHENTICATION = "accountName";
    private static final String FIELD_ADDED = "added";

    @XmlElement(name = FIELD_MEMBER_ID)
    private String memberId = null;

    @XmlElement(name = FIELD_AUTHENTICATION, required = true)
    private String accountName = null;

    @XmlElement(name = FIELD_ADDED)
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

    public void setAdded(final Date added) {
        this.added = copy(added);
    }

    public Date getAdded() {
        return copy(added);
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Member)) {
            return false;
        }

        final Member that = (Member) obj;
        return Objects.equals(memberId, that.memberId) &&
                Objects.equals(accountName, that.accountName) &&
                Objects.equals(added, that.added);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(memberId, accountName, added);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Member{" +
                "memberId='" + memberId + '\'' +
                ", accountName='" + accountName + '\'' +
                ", added=" + added +
                '}';
    }
}
