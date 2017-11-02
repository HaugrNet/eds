/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

import static io.javadog.cws.api.common.Constants.FIELD_CHANGED;
import static io.javadog.cws.api.common.Constants.FIELD_CIRCLE;
import static io.javadog.cws.api.common.Constants.FIELD_MEMBER;
import static io.javadog.cws.api.common.Constants.FIELD_SINCE;
import static io.javadog.cws.api.common.Constants.FIELD_TRUSTLEVEL;
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
@XmlType(name = "trustee", propOrder = { FIELD_CIRCLE, FIELD_MEMBER, FIELD_TRUSTLEVEL, FIELD_CHANGED, FIELD_SINCE })
public final class Trustee implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = FIELD_CIRCLE, required = true)
    private Circle circle = null;

    @XmlElement(name = FIELD_MEMBER, required = true)
    private Member member = null;

    @XmlElement(name = FIELD_TRUSTLEVEL, required = true)
    private TrustLevel trustLevel = null;

    @XmlElement(name = FIELD_CHANGED)
    private Date changed = null;

    @XmlElement(name = FIELD_SINCE)
    private Date since = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setCircle(final Circle circle) {
        this.circle = circle;
    }

    public Circle getCircle() {
        return circle;
    }

    public void setMember(final Member member) {
        this.member = member;
    }

    public Member getMember() {
        return member;
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

    public void setSince(final Date since) {
        this.since = copy(since);
    }

    public Date getSince() {
        return copy(since);
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
                Objects.equals(circle, that.circle) &&
                Objects.equals(member, that.member) &&
                Objects.equals(changed, that.changed) &&
                Objects.equals(since, that.since);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(circle, member, trustLevel, changed, since);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Trustee{" +
                "circle=" + circle +
                ", member=" + member +
                ", trustLevel=" + trustLevel +
                ", changed=" + changed +
                ", since=" + since +
                '}';
    }
}
