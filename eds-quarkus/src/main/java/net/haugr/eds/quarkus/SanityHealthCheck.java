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

import io.smallrye.health.api.HealthGroup;
import jakarta.enterprise.context.ApplicationScoped;
import net.haugr.eds.core.SanityReport;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

/**
 * <p>Reports the outcome of the sanity checks (PLAN.md, S4) as the
 * health group {@value #GROUP}, on the management port:
 * {@code /q/health/group/sanity}. It is DOWN while any Data Object is marked
 * as failed, so monitoring raises an alarm, while a good backup still
 * exists. It is a separate group, so damaged data doesn't take the instance
 * out of service (readiness).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@HealthGroup(SanityHealthCheck.GROUP)
@ApplicationScoped
public class SanityHealthCheck implements HealthCheck {

    public static final String GROUP = "sanity";

    /**
     * {@inheritDoc}
     */
    @Override
    public HealthCheckResponse call() {
        final SanityReport.Snapshot report = SanityReport.current();
        final HealthCheckResponseBuilder builder = HealthCheckResponse.named("EDS sanity")
                .status(report.failedTotal() == 0)
                .withData("enabled", report.enabled())
                .withData("lastRun", (report.lastRun() != null) ? report.lastRun().toString() : "never")
                .withData("checkedLastRun", report.checked())
                .withData("failedLastRun", report.flawed())
                .withData("failedObjects", report.failedTotal());

        return builder.build();
    }
}
