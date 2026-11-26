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
import java.util.function.Function;
import java.util.function.Supplier;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.haugr.eds.api.common.CredentialType;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.Authentication;
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
    private static final String BEARER = "Bearer ";
    /** Name of the OpenAPI security scheme of the session-based endpoints. */
    public static final String SESSION_SCHEME = "session";
    /** Documentation of the responses of the ID-based resource API. */
    public static final String RESOURCE_SUCCESS = "Success; returnCode is 200.";
    public static final String RESOURCE_WARNING = "A warning; the HTTP status is the returnCode of the JSON body.";
    public static final String RESOURCE_ERROR = "An error; the HTTP status is the returnCode of the JSON body.";
    private static final Double NANO_SECOND = 1000000.0D;

    private CommonService() {
        // Empty Constructor, this is a utility Class.
    }

    /**
     * <p>Runs a request of a streaming endpoint (PLAN.md, Streaming). Unlike the
     * other endpoints, which always answer HTTP 200, these answer with the
     * HTTP status of the Return Code, as the plans decide for the streaming
     * endpoints. The body is the JSON response.</p>
     *
     * @param settings EDS Settings
     * @param action   The request
     * @param endpoint Endpoint, for logging
     * @return Response with the matching HTTP status
     */
    public static Response runStreamRequest(final Settings settings, final Supplier<EDSResponse> action, final String endpoint) {
        final Response response = runRequest(settings, action, endpoint);
        final EDSResponse entity = (EDSResponse) response.getEntity();
        final ReturnCode returnCode = ReturnCode.findReturnCode(entity.getReturnCode());
        final int status = (returnCode != null) ? returnCode.getHttpCode() : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

        return Response.fromResponse(response).status(status).build();
    }

    /**
     * <p>Runs a request of the ID-based resource API (PLAN.md, L1). These
     * endpoints authenticate only with the Session token from the
     * {@code Authorization: Bearer} header, like the content endpoints;
     * credentials in the body are replaced by it. They answer with the HTTP
     * status of the Return Code, and the usual JSON response as body.</p>
     *
     * @param settings      EDS Settings
     * @param authorization The Authorization header
     * @param request       The request, with the values from path and body
     * @param action        The business call
     * @param endpoint      Method and path, for logging
     * @param <T>           Request type
     * @return Response with the matching HTTP status
     */
    public static <T extends Authentication> Response runResourceRequest(final Settings settings, final String authorization, final T request,
                                                                         final Function<T, EDSResponse> action, final String endpoint) {
        final byte[] token = bearerToken(authorization);
        if ((token == null) || (token.length == 0)) {
            final ReturnCode returnCode = ReturnCode.AUTHENTICATION_WARNING;
            LOGGER.info("{} rejected: no Session token", endpoint);
            return Response.status(returnCode.getHttpCode())
                    .type(PRODUCES)
                    .entity(new EDSResponse(returnCode, "The Authorization header with a Session token is missing."))
                    .build();
        }

        request.setAccountName(null);
        request.setCredentialType(CredentialType.SESSION);
        request.setCredential(token);

        return runStreamRequest(settings, () -> action.apply(request), endpoint);
    }

    /**
     * Reads the Session token from an {@code Authorization: Bearer} header,
     * as sent by the streaming endpoints, Base64 (standard or URL-safe).
     *
     * @param authorization The Authorization header, or null
     * @return The token, or null if missing or invalid
     */
    public static byte[] bearerToken(final String authorization) {
        byte[] token = null;

        if ((authorization != null) && authorization.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            final String value = authorization.substring(BEARER.length()).trim();
            // The two alphabets differ only in '+' and '/' against '-' and
            // '_'; each decoder refuses the characters of the other.
            final boolean urlSafe = (value.indexOf('-') >= 0) || (value.indexOf('_') >= 0);
            try {
                token = (urlSafe ? java.util.Base64.getUrlDecoder() : java.util.Base64.getDecoder()).decode(value);
            } catch (IllegalArgumentException e) {
                // The token stays null, as it is missing.
                LOGGER.debug("The Session token of the Authorization header is no Base64.", e);
            }
        }

        return token;
    }

    /**
     * <p>This method will invoke the EDS Logic using the provided action
     * supplier. The response is converted into a REST Response and returned.
     * All successful requests are logged with a simple message including
     * duration.</p>
     *
     * <p>If an error occurred, then an error log entry is added, with all
     * the information available, to help track down the problem. A request
     * which is answered with an error Return Code, e.g. after the bean
     * caught the cause, is logged as WARN with the Return Code and message. All known
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
            final ReturnCode returnCode = ReturnCode.findReturnCode(response.getReturnCode());
            if (isError(returnCode)) {
                // An error the request was answered with: the stone itself
                // failed, so it is logged as more than a completed request.
                LOGGER.warn("{} completed in {} ms with {}: {}", endpoint, duration, returnCode, response.getReturnMessage());
            } else {
                LOGGER.info("{} completed in {} ms", endpoint, duration);
            }
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
     * Tells if a Return Code is of the errors, which the endpoints with HTTP
     * status codes answer with a status of 5xx; unlike the warnings, they are
     * no fault of the request, and are logged as WARN. A busy stone isn't:
     * whoever sends many passphrases at once controls how many are answered
     * so, and their limit logs a summary instead (ticket 20).
     *
     * @param returnCode The Return Code, or null if unknown
     * @return True for an error, or an unknown Return Code
     */
    public static boolean isError(final ReturnCode returnCode) {
        return (returnCode == null)
                || ((returnCode.getClassification() == ReturnCode.Classification.CLASS_ERROR) && (returnCode != ReturnCode.SERVICE_BUSY));
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
