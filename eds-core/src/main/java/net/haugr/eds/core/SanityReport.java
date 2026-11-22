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

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>The outcome of the sanity checks (PLAN.md, S4, and PLAN.md, Crypto §4),
 * so monitoring can raise an alarm: when the last run ended, how many Data
 * Objects it checked and found damaged, and how many are marked as failed
 * in total. Shared by the whole instance; the health check reads it.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class SanityReport {

    /** A snapshot of the report. */
    public record Snapshot(LocalDateTime lastRun, boolean enabled, long checked, long flawed, long failedTotal) {
    }

    private static final AtomicReference<Snapshot> CURRENT = new AtomicReference<>(new Snapshot(null, true, 0, 0, 0));

    private SanityReport() {
        // Utility class.
    }

    public static Snapshot current() {
        return CURRENT.get();
    }

    static void completed(final LocalDateTime lastRun, final boolean enabled, final long checked, final long flawed, final long failedTotal) {
        CURRENT.set(new Snapshot(lastRun, enabled, checked, flawed, failedTotal));
    }

    /** A download found a damaged Data Object, and marked it as failed. */
    static void contentFailed() {
        CURRENT.updateAndGet(snapshot -> new Snapshot(snapshot.lastRun(), snapshot.enabled(), snapshot.checked(), snapshot.flawed(), snapshot.failedTotal() + 1));
    }
}
