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
import net.haugr.eds.core.SanitizerBean;

/**
 * <p>Schedules the hourly EDS Sanity checks, with the Quarkus Scheduler. The
 * checks themselves are made by the {@link SanitizerBean}, which runs them in
 * a transaction.</p>
 *
 * <p>This is the only part of the Sanity checks which is specific to Quarkus;
 * another runtime would schedule {@link SanitizerBean#sanitize()} with its own
 * means, e.g. Jakarta Concurrency.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ApplicationScoped
public class SanitizerSchedule {

    private final SanitizerBean sanitizerBean;

    /**
     * CDI Constructor.
     *
     * @param sanitizerBean The EDS Sanitizer Bean
     */
    @Inject
    public SanitizerSchedule(final SanitizerBean sanitizerBean) {
        this.sanitizerBean = sanitizerBean;
    }

    /**
     * Runs the hourly Sanity check.
     */
    @Scheduled(identity = "eds-sanitizer", every = "1h")
    void runSanitizing() {
        sanitizerBean.sanitize();
    }
}
