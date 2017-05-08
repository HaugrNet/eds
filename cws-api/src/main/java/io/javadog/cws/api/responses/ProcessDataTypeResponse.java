/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.responses;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.DataType;

import javax.xml.bind.annotation.XmlElement;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessDataTypeResponse extends CWSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement
    private DataType objectType = null;

    public ProcessDataTypeResponse() {
        // Empty Constructor, required for WebServices
    }

    public ProcessDataTypeResponse(final int returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    public void setObjectType(final DataType objectType) {
        this.objectType = objectType;
    }

    public DataType getObjectType() {
        return objectType;
    }
}
