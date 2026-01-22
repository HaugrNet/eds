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

import net.haugr.eds.core.model.Settings;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * <p>Spring Boot Health Indicator for EDS. Reports the health status based on
 * whether the EDS system has been properly initialized (database schema
 * verified, settings loaded).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Component("edsHealthIndicator")
public class EDSHealthIndicator implements HealthIndicator {

    private final Settings settings;

    /**
     * Default Constructor.
     */
    public EDSHealthIndicator() {
        this.settings = Settings.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Health health() {
        if (settings.isReady()) {
            return Health.up()
                    .withDetail("status", "EDS is ready")
                    .build();
        } else {
            return Health.down()
                    .withDetail("status", "EDS is not ready - database may not be initialized")
                    .build();
        }
    }
}
