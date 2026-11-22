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
import net.haugr.eds.core.jce.MasterKey;

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
     * <p>Runs the hourly Sanity check, unless EDS is not yet started.</p>
     *
     * <p>The scheduler may fire this before startup has finished. On a fresh
     * database that is the window in which the schema is still being created,
     * so a run then would hit tables that do not exist yet. The MasterKey is
     * the reliable "started" signal: it is loaded only after the schema exists
     * (its key check value is read from {@code eds_masterkey}), so an unlocked
     * MasterKey means the schema is in place. While EDS is locked there is also
     * nothing useful to sanitize, so the check is simply skipped until the next
     * hour. The initial Sanity check at startup is run separately, by
     * {@code StartupBean}, after the schema and the MasterKey are ready.</p>
     */
    @Scheduled(identity = "eds-sanitizer", every = "1h")
    void runSanitizing() {
        if (MasterKey.getInstance().isUnlocked()) {
            sanitizerBean.sanitize();
        }
    }
}
