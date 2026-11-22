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
 * <p>A status key (STATUS.md): a key which reads the status of the stone
 * and nothing else. Only its SHA-512 hash is kept, with a label by which
 * the System Administrator tells the keys apart and withdraws one. The hash
 * is plain, not keyed with the MasterKey, so the status is answered while
 * the stone is locked, and a key survives a change of the MasterKey; the
 * key is 256 random bits, so the hash can't be turned back.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Entity
@Table(name = "eds_status_keys")
public class StatusKeyEntity extends EDSEntity {

    @Column(name = "label", unique = true, nullable = false, updatable = false, length = Constants.MAX_NAME_LENGTH)
    private String label = null;

    @Column(name = "checksum", unique = true, nullable = false, updatable = false, length = 128)
    private String checksum = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setChecksum(final String checksum) {
        this.checksum = checksum;
    }

    public String getChecksum() {
        return checksum;
    }
}
