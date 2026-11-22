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
import net.haugr.eds.core.enums.SanityStatus;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
@NamedQuery(name = "data.findContentInfoByMetadata",
        query = "select d.id, d.key.id " +
                "from DataEntity d " +
                "where d.metadata = :metadata")
@NamedQuery(name = "data.updateContentKey",
        query = "update DataEntity d set" +
                " d.key = :key," +
                " d.sanityStatus = :status," +
                " d.sanityChecked = :checked," +
                " d.altered = :checked " +
                "where d.id = :id")
@NamedQuery(name = "data.updateSanityStatus",
        query = "update DataEntity d set" +
                " d.sanityStatus = :status," +
                " d.sanityChecked = :checked " +
                "where d.id = :id")
@NamedQuery(name = "data.deleteByCircle",
        query = "delete from DataEntity d " +
                "where d.metadata in (select m from MetadataEntity m where m.circle = :circle)")
@NamedQuery(name = "data.deleteByMetadata",
        query = "delete from DataEntity d " +
                "where d.metadata = :metadata")
@NamedQuery(name = "data.findChecksum",
        query = "select d.checksum " +
                "from DataEntity d " +
                "where d.id = :id")
@NamedQuery(name = "data.countWithStatus",
        query = "select count(d.id) " +
                "from DataEntity d " +
                "where d.sanityStatus = :status")
// The two queries below read small content as a whole: its parts are
// fetched in the same statement, so a change or delete which commits
// meanwhile can't leave the content without its parts, or with those of
// another version (one statement sees one snapshot of the database).
@NamedQuery(name = "data.findByMetadata",
        query = "select d " +
                "from DataEntity d " +
                "  left join fetch d.parts " +
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
                "from DataEntity d" +
                "  left join fetch d.parts," +
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

    // The encrypted data, a row of eds_data_parts for each part of it
    // (PLAN.md, Streaming). Here it is read and written as a whole, which is
    // for data small enough to be held in memory; larger data is streamed
    // part by part with the EncryptedDataStore, and never loaded here. The
    // parts are only read when the data is asked for.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "eds_data_parts", joinColumns = @JoinColumn(name = "data_id", referencedColumnName = "id"))
    private List<DataPart> parts = new java.util.ArrayList<>();

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

    /**
     * Sets the encrypted data as a whole. The parts are given as a new
     * list, so all rows of the Data Object are replaced, whatever was
     * stored for it, also by a streamed write.
     *
     * @param data The encrypted data; null or empty for none
     */
    public void setData(final byte[] data) {
        this.parts = DataPart.split(data);
    }

    /**
     * Reads the encrypted data as a whole, which loads all its parts.
     *
     * @return The encrypted data; empty for none
     */
    public byte[] getData() {
        return DataPart.join(parts);
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
