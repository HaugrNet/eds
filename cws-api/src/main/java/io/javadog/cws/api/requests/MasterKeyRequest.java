/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2020, JavaDog.io
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
package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.Utilities;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

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
 * <p>Either the secret or a URL to a secret must be given. This is mandatory
 * information, which must be provided to alter the MasterKey. Setting a new
 * MasterKey can only be performed before adding Member Accounts, since it is
 * not possible to re-encrypt the Member Accounts later, as the keys are all
 * stores encrypted and can only be decrypted with member secrets.</p>
 *
 * <p>Please see {@link Authentication} for information about the account and
 * credentials information.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "masterKeyRequest")
@XmlType(name = "masterKeyRequest", propOrder = { Constants.FIELD_SECRET, Constants.FIELD_URL })
public final class MasterKeyRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = Constants.FIELD_SECRET)
    private byte[] secret = null;

    @XmlElement(name = Constants.FIELD_URL)
    private String url = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setSecret(final byte[] secret) {
        this.secret = Utilities.copy(secret);
    }

    public byte[] getSecret() {
        return Utilities.copy(secret);
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
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

        if ((secret == null) == (url == null)) {
            // Both fields can neither be set nor missing.
            errors.put(Constants.FIELD_SECRET, "Either the secret or the URL must be given to alter the MasterKey.");
            errors.put(Constants.FIELD_URL, "Either the secret or the URL must be given to alter the MasterKey.");
        } else {
            if (secret != null) {
                checkNotNullOrEmpty(errors, Constants.FIELD_SECRET, secret, "The secret for the MasterKey is missing.");
            } else {
                checkUrl(errors, url);
            }
        }

        return errors;
    }
}
