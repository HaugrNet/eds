/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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
package net.haugr.eds.api.common;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>This is the types of credentials allowed for the Authentication Process.
 * It can be a Passphrase, a Session, or an Invitation token issued by the
 * System Administrator.</p>
 *
 * <p>Regardless of the type, the information given will be used to decrypt the
 * Asymmetric Key, which was generated and stored with the Account. The Private
 * part of the Key is encrypted using the credentials, and the Public part of
 * the Key is stored as is, so it can be used by others for encryption.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Schema(description = "How the credential in a request is to be used. Values: PASSPHRASE (default; the credential is the Member's "
        + "passphrase), SESSION (the credential is a session key from a login; the account name is omitted), "
        + "INVITATION (the credential is the invitation token from an invite; used once to set the new Member's passphrase).")
public enum CredentialType {

    /**
     * If the Passphrase is chosen, then the information will be used together
     * with both an Account and a System-specific Salt. This should create
     * enough entropy to generate a Symmetric key, which can be used to unlock
     * the Asymmetric Key for the Account.
     */
    PASSPHRASE,

    /**
     * <p>For websites where members have a session, this can be added to the
     * EDS for convenience - and also to prevent that a member must continuously
     * provide the login credentials. It also helps prevent that websites
     * have to store users' credentials alongside a session. Sessions have a
     * pre-defined maximum life-time, after which a Session is considered dead
     * and attempts to use it after this point will lead to a user error.</p>
     *
     * <p>If a session is used, then the credentials will hold the user session
     * while the accountName is omitted.</p>
     */
    SESSION,

    /**
     * If the Invitation is chosen, it is because a Member has been invited to
     * create their own Account. The invitation token is returned once to the
     * System Administrator, who passes it on to the potential Member. The
     * Member then uses it once to set their own Credentials.
     */
    INVITATION
}
