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
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.core.enums.SanityStatus;
import java.time.LocalDateTime;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * <p>EDS Data Entity, maps the Data table from the Database.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Entity
@NamedQuery(name = "data.findByMetadata",
        query = "select d " +
                "from DataEntity d " +
                "where d.metadata = :metadata")
@NamedQuery(name = "data.findAllWithState",
        query = "select d " +
                "from DataEntity d " +
                "where d.sanityStatus = :status" +
                "  and d.sanityChecked >= :since")
@NamedQuery(name = "data.findAllWithStateForCircle",
        query = "select d " +
                "from DataEntity d " +
                "where d.sanityStatus = :status" +
                "  and d.sanityChecked >= :since" +
                "  and d.metadata.circle.externalId = :externalId")
@NamedQuery(name = "data.findAllWithStateForMember",
        query = "select d " +
                "from DataEntity d, TrusteeEntity t " +
                "where t.circle = d.metadata.circle" +
                "  and d.sanityStatus = :status" +
                "  and d.sanityChecked >= :since" +
                "  and t.member = :member")
@NamedQuery(name = "data.findByMemberAndExternalId",
        query = "select d " +
                "from DataEntity d," +
                "     TrusteeEntity t " +
                "where d.metadata.circle.id = t.circle.id" +
                "  and d.metadata.externalId = :externalId" +
                "  and t.member = :member" +
                "  and t.trustLevel in :trustLevels")
@NamedQuery(name = "data.findIdsForSanityCheck",
        query = "select d.id " +
                "from DataEntity d " +
                "where d.sanityStatus = :status" +
                "  and d.sanityChecked <= :date " +
                "order by d.id asc")
@Table(name = "eds_data")
public class DataEntity extends EDSEntity {

    @OneToOne(targetEntity = MetadataEntity.class, fetch = FetchType.EAGER, cascade = CascadeType.PERSIST, optional = false)
    @JoinColumn(name = "metadata_id", referencedColumnName = "id", nullable = false, updatable = false)
    private MetadataEntity metadata = null;

    @ManyToOne(targetEntity = KeyEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "key_id", referencedColumnName = "id", nullable = false, updatable = false)
    private KeyEntity key = null;

    @Column(name = "encrypted_data", nullable = false)
    private byte[] data = null;

    @Column(name = "initial_vector", nullable = false, length = Constants.MAX_STRING_LENGTH)
    private String initialVector = null;

    @Column(name = "checksum", nullable = false, length = Constants.MAX_STRING_LENGTH)
    private String checksum = null;

    @Enumerated(EnumType.STRING)
    @Column(name = "sanity_status", nullable = false, length = Constants.MAX_STRING_LENGTH)
    private SanityStatus sanityStatus = null;

    @Column(name = "sanity_checked", nullable = false)
    private LocalDateTime sanityChecked = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setMetadata(final MetadataEntity object) {
        this.metadata = object;
    }

    public MetadataEntity getMetadata() {
        return metadata;
    }

    public void setKey(final KeyEntity key) {
        this.key = key;
    }

    public KeyEntity getKey() {
        return key;
    }

    public void setData(final byte[] data) {
        this.data = Utilities.copy(data);
    }

    public byte[] getData() {
        return Utilities.copy(data);
    }

    public void setInitialVector(final String initialVector) {
        this.initialVector = initialVector;
    }

    public String getInitialVector() {
        return initialVector;
    }

    public void setChecksum(final String checksum) {
        this.checksum = checksum;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setSanityStatus(final SanityStatus sanityStatus) {
        this.sanityStatus = sanityStatus;
    }

    public SanityStatus getSanityStatus() {
        return sanityStatus;
    }

    public void setSanityChecked(final LocalDateTime sanityChecked) {
        this.sanityChecked = sanityChecked;
    }

    public LocalDateTime getSanityChecked() {
        return sanityChecked;
    }
}
