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
package net.haugr.eds.quarkus;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import net.haugr.eds.core.SanitizerBean;

/**
 * <p>Quarkus Sanitizer Bean, which handles the scheduled sanitizing of the EDS.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ApplicationScoped
public class QuarkusSanitizerBean extends SanitizerBean {

    /**
     * CDI Constructor.
     *
     * @param entityManager EDS EntityManager instance
     */
    @Inject
    public QuarkusSanitizerBean(final EntityManager entityManager) {
        super(entityManager);
    }

    /**
     * Scheduled Sanitizing.
     */
    @Scheduled(every = "1h")
    @Transactional
    public void runSanitizing() {
        sanitize();
    }
}
