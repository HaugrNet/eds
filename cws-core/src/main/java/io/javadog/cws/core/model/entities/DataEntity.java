/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
 * mailto: cws AT JavaDog DOT io
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
package io.javadog.cws.core.model.entities;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.Utilities;
import io.javadog.cws.core.enums.SanityStatus;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * <p>CWS Data Entity, maps the Data table from the Database.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
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
@Table(name = "cws_data")
public class DataEntity extends CWSEntity {

    @OneToOne(targetEntity = MetadataEntity.class, fetch = FetchType.EAGER, optional = false)
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
