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

import io.quarkus.runtime.StartupEvent;
import jakarta.ejb.TimerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import net.haugr.eds.core.SanitizerBean;
import net.haugr.eds.core.StartupBean;

/**
 * <p>Startup Bean for EDS on Quarkus, it is a singleton, which handles loading of the
 * settings. It also runs the EDS Sanity checks.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ApplicationScoped
public class QuarkusStartupBean extends StartupBean {

    /**
     * CDI Constructor.
     *
     * @param entityManager EDS EntityManager instance
     * @param sanitizerBean EDS Sanitizer Bean instance
     * @param timerService  Quarkus TimerService wrapper
     */
    @Inject
    public QuarkusStartupBean(final EntityManager entityManager, final SanitizerBean sanitizerBean, final TimerService timerService) {
        super(entityManager, sanitizerBean, timerService);
    }

    void onStart(@Observes StartupEvent event) {
        startup();
    }
}
