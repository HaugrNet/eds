/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.core.model.entities;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * <p>EDS Entities contain some commonalities, this Class acts as a Super Class
 * for other Entities.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@MappedSuperclass
public class EDSEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private Long id = null;

    @Column(name = "altered", nullable = false)
    private LocalDateTime altered = null;

    @Column(name = "added", nullable = false, updatable = false)
    private LocalDateTime added = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setAltered(final LocalDateTime modified) {
        this.altered = modified;
    }

    public LocalDateTime getAltered() {
        return altered;
    }

    public void setAdded(final LocalDateTime created) {
        this.added = created;
    }

    public LocalDateTime getAdded() {
        return added;
    }
}
