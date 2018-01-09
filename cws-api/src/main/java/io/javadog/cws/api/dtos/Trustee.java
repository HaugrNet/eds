/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

import static io.javadog.cws.api.common.Utilities.copy;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.TrustLevel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * <p>A Trustee, is a Member of a Circle, with a granted Trust Level.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = Constants.FIELD_TRUSTEE, propOrder = { Constants.FIELD_MEMBER_ID, Constants.FIELD_CIRCLE_ID, Constants.FIELD_TRUSTLEVEL, Constants.FIELD_CHANGED, Constants.FIELD_ADDED })
public final class Trustee implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = Constants.FIELD_MEMBER_ID)
    private String memberId = null;

    @XmlElement(name = Constants.FIELD_CIRCLE_ID)
    private String circleId = null;

    @XmlElement(name = Constants.FIELD_TRUSTLEVEL)
    private TrustLevel trustLevel = null;

    @XmlElement(name = Constants.FIELD_CHANGED)
    private Date changed = null;

    @XmlElement(name = Constants.FIELD_ADDED)
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

    public void setCircleId(final String circleId) {
        this.circleId = circleId;
    }

    public String getCircleId() {
        return circleId;
    }

    public void setTrustLevel(final TrustLevel trustLevel) {
        this.trustLevel = trustLevel;
    }

    public TrustLevel getTrustLevel() {
        return trustLevel;
    }

    public void setChanged(final Date changed) {
        this.changed = copy(changed);
    }

    public Date getChanged() {
        return copy(changed);
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

        if (!(obj instanceof Trustee)) {
            return false;
        }

        final Trustee that = (Trustee) obj;
        return (trustLevel == that.trustLevel) &&
                Objects.equals(memberId, that.memberId) &&
                Objects.equals(circleId, that.circleId) &&
                Objects.equals(changed, that.changed) &&
                Objects.equals(added, that.added);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(memberId, circleId, trustLevel, changed, added);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Trustee{" +
                "memberId=" + memberId +
                ", circleId=" + circleId +
                ", trustLevel=" + trustLevel +
                ", changed=" + changed +
                ", added=" + added +
                '}';
    }
}
