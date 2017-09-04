/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-model)
 * =============================================================================
 */
package io.javadog.cws.model.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@NamedQueries(
        @NamedQuery(name = "data.findByMemberAndExternalId",
                query = "select d " +
                        "from DataEntity d " +
                        "inner join TrusteeEntity t on d.metadata.circle.id = t.circle.id " +
                        "where d.metadata.externalId = :eid" +
                        "  and t.member.id = :mid" +
                        "  and t.trustLevel in :trustLevels"))
@Table(name = "data")
public class DataEntity extends CWSEntity {

    @OneToOne(targetEntity = MetadataEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "metadata_id", referencedColumnName = "id", nullable = false, updatable = false)
    private MetadataEntity metadata = null;

    @ManyToOne(targetEntity = KeyEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "key_id", referencedColumnName = "id", nullable = false, updatable = false)
    private KeyEntity key = null;

    @Column(name = "encrypted_data", nullable = false)
    private byte[] data = null;

    @Column(name = "initial_vector", nullable = false, updatable = false, length = 256)
    private String initialVector = null;

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
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public void setInitialVector(final String initialVector) {
        this.initialVector = initialVector;
    }

    public String getInitialVector() {
        return initialVector;
    }
}
