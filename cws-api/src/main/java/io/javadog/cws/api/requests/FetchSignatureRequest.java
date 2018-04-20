/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Constants;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>When the Share Request 'FetchSignatureRequest' is invoked, it requires a
 * Request Object, with the Authentication information.</p>
 *
 * <p>For more details, please see the 'fetchcircles' request in the Management
 * interface: {@link io.javadog.cws.api.Share#fetchSignatures(FetchSignatureRequest)}</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "fetchSignatureRequest")
@XmlType(name = "fetchSignatureRequest")
public final class FetchSignatureRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
}
