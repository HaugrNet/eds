package io.javadog.cws.model.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@Table(name = "circles", uniqueConstraints = @UniqueConstraint(name = "circle_name", columnNames = "name"))
public class CircleEntity extends CWSEntity {

    @Id
    @SequenceGenerator(name = "memberSequence", sequenceName = "member_sequence")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "memberSequence")
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name = null;

    @Column(name = "description")
    private String description = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
