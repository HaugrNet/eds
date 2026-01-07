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
package net.haugr.eds.core.services;

import java.util.Locale;
import java.util.function.Supplier;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.core.model.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Common Service class, for all REST calls. Contain a single method to
 * invoke the correct bean and method, with error handling.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class CommonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonService.class);
    public static final String CONSUMES = MediaType.APPLICATION_JSON;
    public static final String PRODUCES = MediaType.APPLICATION_JSON;
    private static final Double NANO_SECOND = 1000000.0D;

    private CommonService() {
        // Empty Constructor, this is a utility Class.
    }

    /**
     * <p>This method will invoke the EDS Logic using the provided action
     * supplier. The response is converted into a REST Response and returned.
     * All successful requests are logged with a simple message including
     * duration.</p>
     *
     * <p>If an error occurred, then an error log entry is added, with all
     * the information available, to help track down the problem. All known
     * errors in EDS are properly converted to a ReturnCode plus Message and
     * returned to the recipient for proper error handling.</p>
     *
     * <p>As EDS is not an HTTP Service, but rather an application, all
     * requests will respond with a success. This is to allow that any
     * request failing can also provide some additional error information,
     * which is not allowed for any non-successful calls.</p>
     *
     * @param settings  Current EDS Settings, to read the LOCALE from
     * @param action    The action to execute, supplied as a lambda
     * @param endpoint  The complete endpoint information to be logged
     * @return Response from EDS or error response if the request failed
     */
    public static Response runRequest(final Settings settings, final Supplier<EDSResponse> action, final String endpoint) {
        final long startTime = System.nanoTime();
        EDSResponse response;

        try {
            response = action.get();
            final String duration = calculateDuration(settings.getLocale(), startTime);
            LOGGER.info("{} completed in {} ms", endpoint, duration);
        } catch (RuntimeException e) {
            final String duration = calculateDuration(settings.getLocale(), startTime);
            LOGGER.error("{} completed in {} ms with error: {}", endpoint, duration, e.getMessage(), e);
            response = new EDSResponse(ReturnCode.ERROR, e.getMessage());
        }

        return Response
                .ok()
                .type(PRODUCES)
                .entity(response)
                .build();
    }

    /**
     * <p>Calculates the duration from the given start nano time to the
     * current nano Time and returns a String with the milliseconds
     * passed.</p>
     *
     * @param locale        Locale to use for converting to String
     * @param startNanoTime Start nano time to use for calculation
     * @return String with the number of milliseconds
     */
    private static String calculateDuration(final Locale locale, final long startNanoTime) {
        return String.format(locale, "%.2f", ((System.nanoTime() - startNanoTime) / NANO_SECOND));
    }
}
