/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2021, haugr.net
 * mailto: cws AT haugr DOT net
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.cws.core.model.entities;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * <p>For Entities, which is externally accessible, i.e. requires an Id to help
 * identify the Objects will all extend this Class, as it contain the common
 * Externable Id, which differs from the internal Id.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
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
