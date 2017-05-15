/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.responses;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.dtos.DataType;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fetchDataTypeResponse")
public final class FetchDataTypeResponse extends CWSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = "types")
    private final List<DataType> types = new ArrayList<>(0);

    // =========================================================================
    // Object Constructors
    // =========================================================================

    public FetchDataTypeResponse() {
        // Empty Constructor, required for WebServices
    }

    public FetchDataTypeResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    // =========================================================================
    // Setters & Getters
    // =========================================================================

    public void setTypes(final List<DataType> types) {
        this.types.addAll(types);
    }

    public List<DataType> getTypes() {
        return Collections.unmodifiableList(types);
    }
}
