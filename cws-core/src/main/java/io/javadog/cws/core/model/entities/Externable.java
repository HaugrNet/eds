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
 * <p>For Entities, which is externally accessible, i.e. requires an Id to help
 * identify the Objects will all extend this Class, as it contain the common
 * Externable Id, which differs from the internal Id.</p>
 *
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

    public final void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public final String getExternalId() {
        return externalId;
    }
}
