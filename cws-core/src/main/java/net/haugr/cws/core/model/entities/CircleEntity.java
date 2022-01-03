/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
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

import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.common.Utilities;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * <p>CWS Circle Entity, maps the Circle table from the Database.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@Entity
@Table(name = "cws_circles")
@NamedQuery(name = "circle.findByName",
        query = "select c from CircleEntity c " +
                "where lower(c.name) = lower(:name)")
public class CircleEntity extends Externable {

    @Column(name = "name", unique = true, nullable = false, length = Constants.MAX_NAME_LENGTH)
    private String name = null;

    @Column(name = "external_key")
    private byte[] circleKey = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setCircleKey(final byte[] circleKey) {
        this.circleKey = Utilities.copy(circleKey);
    }

    public byte[] getCircleKey() {
        return Utilities.copy(circleKey);
    }
}
