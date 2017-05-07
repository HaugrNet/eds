/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.responses;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.ObjectType;

import javax.xml.bind.annotation.XmlElement;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessObjectTypeResponse extends CWSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement
    private ObjectType objectType = null;

    public ProcessObjectTypeResponse() {
        // Empty Constructor, required for WebServices
    }

    public ProcessObjectTypeResponse(final int returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    public void setObjectType(final ObjectType objectType) {
        this.objectType = objectType;
    }

    public ObjectType getObjectType() {
        return objectType;
    }
}
