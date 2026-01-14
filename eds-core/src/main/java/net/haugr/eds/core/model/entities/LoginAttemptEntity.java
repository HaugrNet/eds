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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * <p>EDS Login Attempt Entity, used to track authentication attempts for
 * rate-limiting purposes.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Entity
@NamedQuery(name = "loginAttempt.countRecentFailures",
        query = "select count(a) " +
                "from LoginAttemptEntity a " +
                "where lower(a.accountName) = lower(:accountName) " +
                "  and a.success = false " +
                "  and a.added >= :since")
@NamedQuery(name = "loginAttempt.deleteByAccountName",
        query = "delete from LoginAttemptEntity a " +
                "where lower(a.accountName) = lower(:accountName)")
@NamedQuery(name = "loginAttempt.deleteOlderThan",
        query = "delete from LoginAttemptEntity a " +
                "where a.added < :cutoff")
@Table(name = "eds_login_attempts")
public class LoginAttemptEntity extends EDSEntity {

    @Column(name = "account_name", nullable = false, length = 75)
    private String accountName = null;

    @Column(name = "success", nullable = false)
    private Boolean success = false;

    @Column(name = "ip_address", length = 45)
    private String ipAddress = null;

    // =========================================================================
    // Entity Setters & Getters
    // =========================================================================

    public void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setSuccess(final Boolean success) {
        this.success = success;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setIpAddress(final String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
