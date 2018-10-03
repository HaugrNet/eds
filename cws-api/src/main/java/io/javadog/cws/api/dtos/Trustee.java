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
package io.javadog.cws.api.dtos;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.common.Utilities;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.Date;

/**
 * <p>A Trustee, is a Member of a Circle, with a granted Trust Level.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = Constants.FIELD_TRUSTEE, propOrder = { Constants.FIELD_MEMBER_ID, Constants.FIELD_PUBLIC_KEY, Constants.FIELD_CIRCLE_ID, Constants.FIELD_TRUSTLEVEL, Constants.FIELD_CHANGED, Constants.FIELD_ADDED })
public final class Trustee implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = Constants.FIELD_MEMBER_ID)
    private String memberId = null;

    // The Public Key is an optional value which may or may not be provided,
    // hence it is only stored but not used for anything. For the same reason,
    // it is not used as part of the Standard Object methods, #equals(),
    // #hashCode() and #toString().
    @XmlElement(name = Constants.FIELD_PUBLIC_KEY, required = true)
    private String publicKey = null;

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

    public void setPublicKey(final String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKey() {
        return publicKey;
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
        this.changed = Utilities.copy(changed);
    }

    public Date getChanged() {
        return Utilities.copy(changed);
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
        // Note, that the public key is omitted deliberately.
        return "Trustee{" +
                "memberId=" + memberId +
                ", circleId=" + circleId +
                ", trustLevel=" + trustLevel +
                ", changed=" + changed +
                ", added=" + added +
                '}';
    }
}
