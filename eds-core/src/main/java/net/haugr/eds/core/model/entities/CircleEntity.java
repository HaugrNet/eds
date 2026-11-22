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

import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.Utilities;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * <p>EDS Circle Entity maps the Circle table from the Database.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Entity
@Table(name = "eds_circles")
@NamedQuery(name = "circle.findByName",
        query = "select c from CircleEntity c " +
                "where lower(c.name) = lower(:name)")
public class CircleEntity extends Externable {

    @Column(name = "name", unique = true, nullable = false, length = Constants.MAX_NAME_LENGTH)
    private String name = null;

    @Column(name = "avatar", length = Constants.MAX_AVATAR_LENGTH)
    private String avatar = null;

    @Column(name = "external_key")
    private byte[] circleKey = null;

    /**
     * When the Circle Secret is to be rotated next (PLAN.md, Crypto C6); null
     * when it isn't due at all.
     */
    @Column(name = "rotate_circle_key_due")
    private LocalDateTime rotateCircleKeyDue = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setAvatar(final String avatar) {
        this.avatar = avatar;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setRotateCircleKeyDue(final LocalDateTime rotateCircleKeyDue) {
        this.rotateCircleKeyDue = rotateCircleKeyDue;
    }

    public LocalDateTime getRotateCircleKeyDue() {
        return rotateCircleKeyDue;
    }

    /**
     * Whether the rotation of the Circle Secret is due.
     *
     * @param now The time now
     * @return True if the time it is due has passed
     */
    public boolean isRotationDue(final LocalDateTime now) {
        return (rotateCircleKeyDue != null) && !rotateCircleKeyDue.isAfter(now);
    }

    public void setCircleKey(final byte[] circleKey) {
        this.circleKey = Utilities.copy(circleKey);
    }

    public byte[] getCircleKey() {
        return Utilities.copy(circleKey);
    }
}
