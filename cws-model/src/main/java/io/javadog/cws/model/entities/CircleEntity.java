package io.javadog.cws.model.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@Table(name = "circles")
@NamedQueries({
        @NamedQuery(name = "circle.findAll",
                query = "select c " +
                        "from CircleEntity c " +
                        "order by name asc")
})
public class CircleEntity extends Externable {

    @Column(name = "name", nullable = false, unique = true)
    private String name = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
