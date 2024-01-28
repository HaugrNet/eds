/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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
package net.haugr.eds.rest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.model.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Common Service class, for all REST calls. Contain a single method to
 * invoke the correct bean & method, with error handling.</p>
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
     * <p>This method will invoke the EDS Logic, using Reflection to find the
     * correct method and parse the Request Object into it. The response is
     * converted into a REST Response and returned back. All successful
     * requests are logged with a simple message including duration.</p>
     *
     * <p>If an error occurred, then an error log entry is added, with all
     * the information available, to help track down the problem. All known
     * errors in EDS are properly converted to a ReturnCode plus Message, and
     * returned to the recipient for proper error handling.</p>
     *
     * <p>As EDS is not a HTTP Service, but rather an application, all
     * requests will respond with a success, this is to allow that any
     * request failing can also provide some additional error information,
     * which is not allowed for any non-successful calls.</p>
     *
     * @param settings     Current EDS Settings, to read the LOCALE from
     * @param bean         The EDS Bean to invoke
     * @param invokeMethod The method to invoke on the EDS Bean
     * @param request      The Request Object
     * @param logAction    The Action information to be logged
     * @return Response from EDS or error response if request failed
     */
    public static Response runRequest(final Settings settings, final Object bean, final String invokeMethod, final Authentication request, final String logAction) {
        final long startTime = System.nanoTime();
        EDSResponse response;

        try {
            final Method method = findMethod(bean, invokeMethod);
            response = (request == null)
                    ? (EDSResponse) method.invoke(bean)
                    : (EDSResponse) method.invoke(bean, request);

            final String duration = calculateDuration(settings.getLocale(), startTime);
            LOGGER.info("{} completed in {} ms", logAction, duration);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException e) {
            final String duration = calculateDuration(settings.getLocale(), startTime);
            LOGGER.error("{} completed in {} ms with error: {}", logAction, duration, e.getMessage(), e);
            response = new EDSResponse(ReturnCode.ERROR, e.getMessage());
        }

        return Response
                .ok()
                .type(PRODUCES)
                .entity(response)
                .build();
    }

    /**
     * <p>Attempts to find the requested method and returns it. If no such
     * method exists, an exception is thrown.</p>
     *
     * @param bean         EDS Bean to find the method in
     * @param invokeMethod Method to find
     * @return Found method
     * @throws EDSException if no method was found
     */
    private static Method findMethod(final Object bean, final String invokeMethod) {
        final Method[] methods = bean.getClass().getMethods();

        for (final Method method : methods) {
            if (method.getName().equals(invokeMethod)) {
                return method;
            }
        }

        // As we only deal with existing, working & tested functionality,
        // this is considered unreachable code, added to avoid having
        // Needless null checks.
        throw new EDSException(ReturnCode.ILLEGAL_SERVICE, "No such method: " + invokeMethod);
    }

    /**
     * <p>Calculates the duration from the given start nano time to the
     * current nano Time, and returns a String with the milliseconds
     * passed.</p>
     *
     * @param locale        Locale to use for converting to String
     * @param startNanoTime Start nano time to use for calculation
     * @return String with number of milliseconds
     */
    private static String calculateDuration(final Locale locale, final long startNanoTime) {
        return String.format(locale, "%.2f", ((System.nanoTime() - startNanoTime) / NANO_SECOND));
    }
}
