/*
 * =============================================================================
 * Copyright (c) 2010-2017, JavaDog.IO
 * -----------------------------------------------------------------------------
 * Project: ZObEL (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.common;

import javax.xml.bind.annotation.XmlType;

/**
 * <p>This is the types of credentials allowed for the Authentication Process.
 * It can be either an Asymmetric Key (Private Key only or Public/Private Key
 * pair), which is armored using a simply Base64 encoding. Or it can be a
 * Passphrase, which together a System & Account Salt.</p>
 *
 * <p>Regardless of the type, the information given will be used to decrypt the
 * Asymmetric Key, which was generated and stored with the Account. The Private
 * part of the Key is encrypted using the credentials, and the Public part of
 * the Key is stored as is, so it can be used by others for encryption.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlType(name = "credentialType")
public enum CredentialType {

    /**
     * If the Key is chosen, the CWS is expecting an Armored (Base64 encoded)
     * Asymmetric Key, either the Private Key or a Key Pair (Private/Public)
     * Key. The Private Key part will be used to unlock the Asymmetric Key for
     * the Account.
     */
    KEY,

    /**
     * If the Passphrase is chosen, then the information will be used together
     * with both an Account and a System specific Salt to create enough entropy
     * to generate a Symmetric key which can be used to unlock the Asymmetric
     * Key for the Account.
     */
    PASSPHRASE
}
