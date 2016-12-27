package io.javadog.cws.model.entities;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@MappedSuperclass
public class CWSEntity {

    @Column(name = "external_id", unique = true, nullable = false, updatable = false)
    private String externalId = null;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "modified", nullable = false)
    private Date modified = null;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created", nullable = false, updatable = false)
    private Date created = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setModified(final Date modified) {
        this.modified = modified;
    }

    public Date getModified() {
        return modified;
    }

    public void setCreated(final Date created) {
        this.created = created;
    }

    public Date getCreated() {
        return created;
    }
}
