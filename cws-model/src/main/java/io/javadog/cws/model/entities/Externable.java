/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-model)
 * =============================================================================
 */
package io.javadog.cws.model.entities;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@MappedSuperclass
public class Externable extends CWSEntity {

    @Column(name = "external_id", length = 36, unique = true, nullable = false, updatable = false)
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
