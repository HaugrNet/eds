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

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public String getExternalId() {
        return externalId;
    }
}
