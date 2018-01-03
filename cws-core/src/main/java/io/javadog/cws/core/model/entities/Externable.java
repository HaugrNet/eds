/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.model.entities;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@MappedSuperclass
public class Externable extends CWSEntity {

    @Column(name = "external_id", unique = true, nullable = false, updatable = false, length = 36)
    private String externalId = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public String getExternalId() {
        return externalId;
    }
}
