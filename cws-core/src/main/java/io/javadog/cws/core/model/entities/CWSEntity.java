/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.model.entities;

import static io.javadog.cws.api.common.Utilities.copy;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * <p>CWS Entities contain some commonalities, this Class acts as a Super Class
 * for other Entities.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@MappedSuperclass
public class CWSEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "altered", nullable = false)
    private Date altered = null;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "added", nullable = false, updatable = false)
    private Date added = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public final void setId(final Long id) {
        this.id = id;
    }

    public final Long getId() {
        return id;
    }

    public final void setAltered(final Date modified) {
        this.altered = copy(modified);
    }

    public final Date getAltered() {
        return copy(altered);
    }

    public final void setAdded(final Date created) {
        this.added = copy(created);
    }

    public final Date getAdded() {
        return copy(added);
    }
}
