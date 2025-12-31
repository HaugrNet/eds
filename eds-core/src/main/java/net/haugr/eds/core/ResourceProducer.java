/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2025, haugr.net
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

import jakarta.annotation.Resource;
import jakarta.ejb.TimerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * <p>Certain resources cannot be injected directly via constructor
 * injection, unless they have been exposed. This bean ensures that
 * they are properly exposed.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ApplicationScoped
public class ResourceProducer {

    @PersistenceContext(unitName = "edsDS")
    private EntityManager entityManager;

    @Resource
    private TimerService timerService;

    @Produces
    public EntityManager produceEntityManager() {
        return entityManager;
    }

    @Produces
    public TimerService produceTimerService() {
        return timerService;
    }
}
