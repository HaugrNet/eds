/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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
package net.haugr.eds.api.requests;

import net.haugr.eds.api.common.ByteArrayAdapter;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.Utilities;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTypeAdapter;
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
 * @since EDS 1.0
 */
@JsonbPropertyOrder({ Constants.FIELD_SECRET, Constants.FIELD_URL })
public final class MasterKeyRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The Secret as Bytes. */
    @JsonbProperty(value = Constants.FIELD_SECRET, nillable = true)
    @JsonbTypeAdapter(ByteArrayAdapter.class)
    private byte[] secret = null;

    /** The Secret as URL. */
    @JsonbProperty(value = Constants.FIELD_URL, nillable = true)
    private String url = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    /**
     * Set the Secret from Bytes.
     *
     * @param secret Secret from Bytes
     */
    public void setSecret(final byte[] secret) {
        this.secret = Utilities.copy(secret);
    }

    /**
     * Retrieves the Secret from Bytes.
     *
     * @return Secret from Bytes
     */
    public byte[] getSecret() {
        return Utilities.copy(secret);
    }

    /**
     * Set the Secret from a URL.
     *
     * @param url Secret from the URL
     */
    public void setUrl(final String url) {
        this.url = url;
    }

    /**
     * Retrieves the Secret from the URL.
     *
     * @return Secret from the URL
     */
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
