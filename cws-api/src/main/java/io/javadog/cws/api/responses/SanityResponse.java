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
package io.javadog.cws.api.responses;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.dtos.Sanity;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>Returns a list of Data Objects, which have failed their sanity checks,
 * i.e. the bytes stored which may somehow have been corrupted over time, so
 * they no longer can be decrypted.</p>
 *
 * <p>If the CircleId is given in the request, then the resulting list is
 * limited to the records for the given Circle, same applies to the timestamp,
 * since, which means that only newer record information is being retrieved.</p>
 *
 * <p>For any system, having <i>any</i> results from request is considered a
 * disaster, as the underlying data storage cannot be trusted.</p>
 *
 * <p>Please see {@link CwsResponse} for information about the result of the
 * processing.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "sanityResult")
@XmlType(name = "sanityResult", propOrder = Constants.FIELD_SANITIES )
public final class SanityResponse extends CwsResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = Constants.FIELD_SANITIES, required = true)
    private final List<Sanity> sanities = new ArrayList<>(0);

    // =========================================================================
    // Object Constructors
    // =========================================================================

    /**
     * Empty Constructor, to use if the setters are invoked. This is required
     * for WebServices to work properly.
     */
    public SanityResponse() {
        // Empty Constructor, required for WebServices
    }

    /**
     * Error Constructor, used if an error occurred, and the request could not
     * complete successfully.
     *
     * @param returnCode    The CWS Return Code
     * @param returnMessage The CWS Return Message
     */
    public SanityResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    public void setSanities(final List<Sanity> sanities) {
        this.sanities.addAll(sanities);
    }

    public List<Sanity> getSanities() {
        return Collections.unmodifiableList(sanities);
    }
}
