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

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import net.haugr.eds.core.managers.SanityManager;
import net.haugr.eds.core.model.Settings;

/**
 * <p>Generally, the database is always trustworthy, it is a system designed
 * to be entrusted with data - meaning that the bits and bytes going in
 * should match those coming out. However, if certain parts of the data are
 * not accessed or used in a long time, the disc used to persist the data on
 * may develop problems which can corrupt data over time.</p>
 *
 * <p>The simplest solution is to make sure that data is used frequently and
 * as long as the data extracted matches the one that was persisted -
 * everything is fine. If a problem occurs over time, then a flag is set which
 * will mark the data invalid. This way, the corrupted record can be either
 * removed or replaced with a valid record from a backup.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Stateless
@Transactional
public class SanitizerBean {

    private final SanityManager manager;

    public SanitizerBean() {
        this(null, Settings.getInstance());
    }

    @Inject
    public SanitizerBean(final EntityManager entityManager) {
        this(entityManager, Settings.getInstance());
    }

    public SanitizerBean(final EntityManager entityManager, final Settings settings) {
        this(new SanityManager(settings, entityManager));
    }

    public SanitizerBean(final SanityManager manager) {
        this.manager = manager;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public void sanitize() {
        manager.sanitize();
    }
}
