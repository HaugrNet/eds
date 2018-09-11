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

import io.javadog.cws.api.common.Constants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * <p>CWS DataType Entity, maps the DataType table from the Database.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "type.findAll",
                query = "select d " +
                        "from DataTypeEntity d " +
                        "order by d.name asc"),
        @NamedQuery(name = "type.findByName",
                query = "select d from DataTypeEntity d " +
                        "where lower(d.name) = lower(:name)"),
        @NamedQuery(name = "type.countUsage",
                query = "select count(m.id) " +
                        "from MetadataEntity m " +
                        "where m.type = :type")
})
@Table(name = "cws_datatypes")
public class DataTypeEntity extends CWSEntity {

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
