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

import jakarta.ejb.TimerService;
import jakarta.persistence.EntityManager;
import net.haugr.eds.core.SanitizerBean;
import net.haugr.eds.core.StartupBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * <p>Startup Bean for EDS on Spring, it is a singleton, which handles loading of the
 * settings. It also runs the EDS Sanity checks.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Component
public class SpringStartupBean extends StartupBean {

    /**
     * Spring Constructor.
     *
     * @param entityManager EDS EntityManager instance
     * @param sanitizerBean EDS Sanitizer Bean instance
     * @param timerService  Spring TimerService wrapper
     */
    @Autowired
    public SpringStartupBean(final EntityManager entityManager, final SanitizerBean sanitizerBean, final TimerService timerService) {
        super(entityManager, sanitizerBean, timerService);
    }

    /**
     * Spring application ready event listener.
     *
     * @param event The ApplicationReadyEvent
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(final ApplicationReadyEvent event) {
        startup();
    }
}
