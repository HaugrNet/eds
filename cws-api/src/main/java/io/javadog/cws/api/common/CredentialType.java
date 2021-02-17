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
package io.javadog.cws.api.common;

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
 * @since CWS 1.0
 */
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
