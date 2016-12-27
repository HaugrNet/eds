package io.javadog.cws.model.entities;

import io.javadog.cws.api.common.TrustLevel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Entity
@Table(name = "trustees")
public class TrusteeEntity extends CWSEntity {

    @Id
    @SequenceGenerator(name = "trusteeSequence", sequenceName = "trustee_sequence")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trusteeSequence")
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private Long id = null;

    @ManyToOne(targetEntity = MemberEntity.class, fetch = FetchType.EAGER, optional = false)
    private MemberEntity member = null;

    @ManyToOne(targetEntity = CircleEntity.class, fetch = FetchType.EAGER, optional = false)
    private CircleEntity circle = null;

    @Column(name = "trust_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private TrustLevel trustLevel = null;

    @Column(name = "armored_key", nullable = false)
    private String armoredKey = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setMember(final MemberEntity member) {
        this.member = member;
    }

    public MemberEntity getMember() {
        return member;
    }

    public void setCircle(final CircleEntity circle) {
        this.circle = circle;
    }

    public CircleEntity getCircle() {
        return circle;
    }

    public void setTrustLevel(final TrustLevel trustLevel) {
        this.trustLevel = trustLevel;
    }

    public TrustLevel getTrustLevel() {
        return trustLevel;
    }

    public void setArmoredKey(final String armoredKey) {
        this.armoredKey = armoredKey;
    }

    public String getArmoredKey() {
        return armoredKey;
    }
}
