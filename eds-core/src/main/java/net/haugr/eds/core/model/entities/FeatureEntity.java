/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import net.haugr.eds.api.common.Constants;

/**
 * <p>A feature which the stone offers (FEATURES.md): its name, and the declaration
 * of the kinds of its entries and their rules, as the System Administrator
 * gave it. The stone doesn't know what a feature is for.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Entity
@Table(name = "eds_features")
public class FeatureEntity extends EDSEntity {

    @Column(name = "name", unique = true, nullable = false, updatable = false, length = Constants.MAX_FEATURE_NAME_LENGTH)
    private String name = null;

    @Column(name = "version", nullable = false)
    private Integer version = null;

    @Column(name = "declaration", nullable = false, length = Constants.MAX_DECLARATION_LENGTH)
    private String declaration = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public Integer getVersion() {
        return version;
    }

    public void setDeclaration(final String declaration) {
        this.declaration = declaration;
    }

    public String getDeclaration() {
        return declaration;
    }
}
