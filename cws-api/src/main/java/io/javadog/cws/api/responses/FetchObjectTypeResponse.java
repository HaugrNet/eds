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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchObjectTypeResponse extends CWSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = 8868831828030258226L;

    private final List<ObjectType> types = new ArrayList<>(0);

    public FetchObjectTypeResponse() {
        // Empty Constructor, required for WebServices
    }

    public FetchObjectTypeResponse(final int returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    // =========================================================================
    // Setters & Getters
    // =========================================================================

    public void setTypes(final List<ObjectType> types) {
        this.types.addAll(types);
    }

    public List<ObjectType> getTypes() {
        return Collections.unmodifiableList(types);
    }
}
