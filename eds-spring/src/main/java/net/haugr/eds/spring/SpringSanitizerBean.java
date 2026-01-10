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
package net.haugr.eds.spring;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import net.haugr.eds.core.SanitizerBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * <p>Spring Sanitizer Bean, which handles the scheduled sanitizing of the EDS.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Component
public class SpringSanitizerBean extends SanitizerBean {

    /**
     * Spring Constructor.
     *
     * @param entityManager EDS EntityManager instance
     */
    @Autowired
    public SpringSanitizerBean(final EntityManager entityManager) {
        super(entityManager);
    }

    /**
     * Scheduled Sanitizing runs every hour.
     */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void runSanitizing() {
        sanitize();
    }
}
