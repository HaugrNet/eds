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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * Spring Boot Application entry point for EDS.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@SpringBootApplication
@EntityScan(basePackages = "net.haugr.eds.core.model.entities")
public class EDSSpringApplication {

    /**
     * Main entry point.
     *
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(EDSSpringApplication.class, args);
    }
}
