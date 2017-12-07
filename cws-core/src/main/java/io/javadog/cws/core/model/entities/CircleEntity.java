/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.model.entities;

import static io.javadog.cws.api.common.Constants.MAX_NAME_LENGTH;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@Table(name = "cws_circles")
@NamedQueries({
        @NamedQuery(name = "circle.findAll",
                    query = "select c " +
                            "from CircleEntity c " +
                            "order by name asc"),
        @NamedQuery(name = "circle.findByName",
                    query = "select c from CircleEntity c " +
                            "where lower(name) = lower(:name)")
})
public class CircleEntity extends Externable {

    @Column(name = "name", unique = true, nullable = false, length = MAX_NAME_LENGTH)
    private String name = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
