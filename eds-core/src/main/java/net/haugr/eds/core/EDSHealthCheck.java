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

import jakarta.enterprise.context.ApplicationScoped;
import net.haugr.eds.core.model.Settings;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * <p>MicroProfile Health Check for EDS on WildFly. Reports the readiness status
 * based on whether the EDS system has been properly initialized (database schema
 * verified, settings loaded).</p>
 *
 * <p>This check is used for Kubernetes readiness probes. Liveness is handled
 * by the default WildFly liveness check.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Readiness
@ApplicationScoped
public class EDSHealthCheck implements HealthCheck {

    private final Settings settings;

    /**
     * Default Constructor.
     */
    public EDSHealthCheck() {
        this.settings = Settings.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HealthCheckResponse call() {
        if (settings.isReady()) {
            return HealthCheckResponse.named("EDS")
                    .up()
                    .withData("status", "EDS is ready")
                    .build();
        } else {
            return HealthCheckResponse.named("EDS")
                    .down()
                    .withData("status", "EDS is not ready - database may not be initialized")
                    .build();
        }
    }
}
