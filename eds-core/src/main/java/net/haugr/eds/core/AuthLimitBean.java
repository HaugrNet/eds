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
package net.haugr.eds.core;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import net.haugr.eds.core.managers.AuthAttempts;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.model.Settings;

/**
 * <p>Keeps the count of the attempts at the passphrase of an account, which
 * locks it once too many are made within a window (#68), see
 * {@link AuthAttempts}. The Beans give it to their managers, which call it
 * where the passphrase is checked ({@code AbstractManager.checkPassphrase}).</p>
 *
 * <p>The count of an attempt and of a success each run in a transaction of
 * their own ({@link Transactional.TxType#REQUIRES_NEW}), committed before the
 * passphrase is checked, and right after it was accepted. So does the read of
 * the account before the passphrase is checked (ticket 28). So a request which
 * fails, and is rolled back, doesn't take its attempt or its success with
 * it, and attempts at the same moment see each other's count. The request
 * has written nothing to the row of its account at that point, so this
 * transaction never waits for the request's.</p>
 *
 * <p>The lock is time-based and heals itself, so no account, the System
 * Administrator included, can be locked out for good; a new passphrase, as
 * when an invitation is accepted, clears it at once. The feature is off when
 * {@code eds.auth.max.failed.attempts} is 0 (the default).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ApplicationScoped
public class AuthLimitBean implements AuthAttempts {

    private final EntityManager entityManager;
    private final Settings settings;

    /**
     * Default Constructor, required by the CDI Container.
     */
    public AuthLimitBean() {
        this(null, Settings.getInstance());
    }

    /**
     * CDI Constructor.
     *
     * @param entityManager Entity Manager
     */
    @Inject
    public AuthLimitBean(final EntityManager entityManager) {
        this(entityManager, Settings.getInstance());
    }

    /**
     * Constructor for testing, with explicit Settings.
     *
     * @param entityManager Entity Manager
     * @param settings      EDS Settings
     */
    public AuthLimitBean(final EntityManager entityManager, final Settings settings) {
        this.entityManager = entityManager;
        this.settings = settings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public boolean begin(final long memberId) {
        return AuthAttempts.begin(new CommonDao(entityManager), settings, memberId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void succeeded(final long memberId) {
        new CommonDao(entityManager).clearFailedLogins(memberId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Account findAccount(final String name, final String circleId) {
        return AuthAttempts.findAccount(new CommonDao(entityManager), name, circleId);
    }
}
