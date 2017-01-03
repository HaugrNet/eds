package io.javadog.cws.model.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@Table(name = "objectdata")
public class DataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private Long id;

    @OneToOne(targetEntity = ObjectEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "object_id", referencedColumnName = "id", nullable = false, updatable = false)
    private ObjectEntity object = null;

    @ManyToOne(targetEntity = KeyEntity.class, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "key_id", referencedColumnName = "id", nullable = false, updatable = false)
    private KeyEntity key = null;

    @Column(name = "encrypted_data", nullable = false)
    private byte[] data = null;

    @Column(name = "initial_vector", length = 256, nullable = false, updatable = false)
    private String initialVector = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setObject(final ObjectEntity object) {
        this.object = object;
    }

    public ObjectEntity getObject() {
        return object;
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
