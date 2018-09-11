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
package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.Utilities;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Map;

/**
 * <p>The MasterKey Request Object is needed to change the internally used
 * MasterKey. The MasterKey is not persisted but must be set when the system is
 * started to &quot;unlock&quot; it. The same request can also be used to lock
 * a running system, so nothing will work.</p>
 *
 * <p>MasterKey's is used to encrypt/decrypt the &quot;salt&quot; of a member,
 * which is a piece of static information used to check the member credentials.
 * The MasterKey is also used to encrypt/decrypt all IVs or Initial Vectors,
 * which is the initial random information used to encrypt and decrypt a piece
 * of data in the system. So, without the MasterKey, neither Member accounts nor
 * data can be retrieved, as it acts as a second lock for both parts.</p>
 *
 * <p>The secret is this a mandatory information which must be provided to alter
 * the MasterKey. Setting a new MasterKey can only be performed before adding
 * Member Accounts, since it is not possible to re-encrypt the Member Accounts
 * later.</p>
 *
 * <p>Please see {@link Authentication} for information about the account and
 * credentials information.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "masterKeyRequest")
@XmlType(name = "masterKeyRequest", propOrder = Constants.FIELD_SECRET)
public final class MasterKeyRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @NotNull
    @XmlElement(name = Constants.FIELD_SECRET, required = true)
    private byte[] secret = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setSecret(final byte[] secret) {
        this.secret = Utilities.copy(secret);
    }

    public byte[] getSecret() {
        return Utilities.copy(secret);
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> validate() {
        final Map<String, String> errors = super.validate();

        checkNotNullOrEmpty(errors, Constants.FIELD_SECRET, secret, "The secret for the MasterKey is missing.");

        return errors;
    }
}
