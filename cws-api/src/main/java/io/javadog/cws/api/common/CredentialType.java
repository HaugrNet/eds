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
package io.javadog.cws.api.common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>This is the types of credentials allowed for the Authentication Process.
 * It can be either an Invitation (Signature, with a shared secret). Or it can
 * be a Passphrase, which together a System &amp; Account Salt.</p>
 *
 * <p>Regardless of the type, the information given will be used to decrypt the
 * Asymmetric Key, which was generated and stored with the Account. The Private
 * part of the Key is encrypted using the credentials, and the Public part of
 * the Key is stored as is, so it can be used by others for encryption.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = Constants.FIELD_CREDENTIALTYPE)
public enum CredentialType {

    /**
     * If the Passphrase is chosen, then the information will be used together
     * with both an Account and a System specific Salt to create enough entropy
     * to generate a Symmetric key which can be used to unlock the Asymmetric
     * Key for the Account.
     */
    PASSPHRASE,

    /**
     * <p>For websites, where members have a session, this can be added to the
     * CWS for convenience - and also to prevent that a member must continuously
     * provide the login credentials. It also helps preventing that websites
     * have to store users credentials alongside a session. Sessions have a
     * pre-defined maximum life-time, after which a Session is considered dead
     * and attempts to use it after this point will lead to a user error.</p>
     *
     * <p>If a session is used, then the credentials will hold the user session
     * while the accountName is omitted.</p>
     */
    SESSION,

    /**
     * If the Signature is chosen, it is because a Member has been invited to
     * create their own Account. The Signature is provided by the System
     * Administrator to the potential Member, who can then create the Account
     * with a higher degree of control over the Credentials.
     */
    SIGNATURE
}
