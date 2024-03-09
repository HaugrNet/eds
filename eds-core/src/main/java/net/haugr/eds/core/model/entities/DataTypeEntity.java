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

import net.haugr.eds.api.common.Constants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * <p>EDS DataType Entity, maps the DataType table from the Database.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Entity
@NamedQuery(name = "type.findAll",
        query = "select d " +
                "from DataTypeEntity d " +
                "order by d.id asc")
@NamedQuery(name = "type.findByName",
        query = "select d from DataTypeEntity d " +
                "where lower(d.name) = lower(:name)")
@NamedQuery(name = "type.countUsage",
        query = "select count(m.id) " +
                "from MetadataEntity m " +
                "where m.type = :type")
@Table(name = "eds_datatypes")
public class DataTypeEntity extends EDSEntity {

    @Column(name = "datatype_name", unique = true, nullable = false, length = Constants.MAX_NAME_LENGTH)
    private String name = null;

    @Column(name = "datatype_value", nullable = false)
    private String type = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
