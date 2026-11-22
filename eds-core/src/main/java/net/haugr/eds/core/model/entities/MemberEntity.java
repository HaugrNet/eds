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
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.core.enums.KeyAlgorithm;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * <p>EDS Member Entity, maps the Member table from the Database.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Entity
@NamedQuery(name = "member.countMembers",
        query = "select count(m.id) " +
                "from MemberEntity m")
@NamedQuery(name = "member.findByName",
        query = "select m " +
                "from MemberEntity m " +
                "where lower(m.name) = lower(:name)")
@NamedQuery(name = "member.findByRole",
        query = "select m " +
                "from MemberEntity m " +
                "where m.memberRole = :role " +
                "order by m.id asc")
@NamedQuery(name = "member.findByNameAndCircle",
        query = "select e.member " +
                "from TrusteeEntity e " +
                "where lower(e.member.name) = lower(:name)" +
                "  and e.circle.externalId = :externalCircleId")
@NamedQuery(name = "member.removeExpiredInvitations",
        query = "delete from MemberEntity " +
                "where invitationExpire <= current_timestamp")
@Table(name = "eds_members")
public class MemberEntity extends Externable {

    @Column(name = "name", unique = true, nullable = false, length = Constants.MAX_NAME_LENGTH)
    private String name = null;

    @Column(name = "salt", unique = true, nullable = false, length = 256)
    private String salt = null;

    @Enumerated(EnumType.STRING)
    @Column(name = "pbe_algorithm", nullable = false, length = 25)
    private KeyAlgorithm pbeAlgorithm = null;

    // The PBKDF2 iterations the keys of this account were protected with:
    // the setting at the time the passphrase was set. Used at each login, so
    // the setting can change without locking anyone out.
    @Column(name = "pbe_iterations", nullable = false)
    private Integer pbeIterations = null;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_algorithm", nullable = false, length = 25)
    private KeyAlgorithm keyAlgorithm = null;

    @Column(name = "external_key")
    private String memberKey = null;

    @Column(name = "public_key", nullable = false, length = 4096)
    private String publicKey = null;

    @Column(name = "private_key", nullable = false, length = 16384)
    private String privateKey = null;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 10)
    private MemberRole memberRole = null;

    @Column(name = "avatar", length = Constants.MAX_AVATAR_LENGTH)
    private String avatar = null;

    // The Member's own settings for clients (language, theme, ...), a JSON
    // object which EDS only stores.
    @Column(name = "settings", length = Constants.MAX_MEMBER_SETTINGS_LENGTH)
    private String settings = null;

    /**
     * The Member's vault (E2E.md): text of a client, stored as given and
     * returned to the Member only.
     */
    @Column(name = "external_vault", length = Constants.MAX_VAULT_LENGTH)
    private String vault = null;

    /**
     * The expiry of the Session this request came with, or which a login
     * just made (PLAN.md, "Several sessions"). Not stored with the Member:
     * the Sessions are rows of their own, see {@link SessionEntity}.
     */
    @Transient
    private LocalDateTime sessionExpire = null;

    @Column(name = "invitation_expire")
    private LocalDateTime invitationExpire = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setName(final String identifier) {
        this.name = identifier;
    }

    public String getName() {
        return name;
    }

    public void setSalt(final String salt) {
        this.salt = salt;
    }

    public String getSalt() {
        return salt;
    }

    public void setPbeAlgorithm(final KeyAlgorithm pbeAlgorithm) {
        this.pbeAlgorithm = pbeAlgorithm;
    }

    public KeyAlgorithm getPbeAlgorithm() {
        return pbeAlgorithm;
    }

    public void setPbeIterations(final Integer pbeIterations) {
        this.pbeIterations = pbeIterations;
    }

    public Integer getPbeIterations() {
        return pbeIterations;
    }

    public void setKeyAlgorithm(final KeyAlgorithm algorithm) {
        this.keyAlgorithm = algorithm;
    }

    public KeyAlgorithm getKeyAlgorithm() {
        return keyAlgorithm;
    }

    public void setMemberKey(final String memberKey) {
        this.memberKey = memberKey;
    }

    public String getMemberKey() {
        return memberKey;
    }

    public void setPublicKey(final String armoredPublicKey) {
        this.publicKey = armoredPublicKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPrivateKey(final String armoredEncryptedPrivateKey) {
        this.privateKey = armoredEncryptedPrivateKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setMemberRole(final MemberRole memberRole) {
        this.memberRole = memberRole;
    }

    public MemberRole getMemberRole() {
        return memberRole;
    }

    public void setAvatar(final String avatar) {
        this.avatar = avatar;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setSettings(final String settings) {
        this.settings = settings;
    }

    public String getSettings() {
        return settings;
    }

    public void setSessionExpire(final LocalDateTime sessionExpire) {
        this.sessionExpire = sessionExpire;
    }

    public LocalDateTime getSessionExpire() {
        return sessionExpire;
    }

    public void setInvitationExpire(final LocalDateTime invitationExpire) {
        this.invitationExpire = invitationExpire;
    }

    public LocalDateTime getInvitationExpire() {
        return invitationExpire;
    }

    public void setVault(final String vault) {
        this.vault = vault;
    }

    public String getVault() {
        return vault;
    }
}
