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
package net.haugr.eds.fitnesse.callers;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.haugr.eds.client.ApiClient;
import net.haugr.eds.client.ApiException;
import net.haugr.eds.fitnesse.exceptions.StopTestException;

/**
 * <p>The generated client (PLAN.md, L3): one {@link ApiClient} for the
 * EDS URL, and the handling of the HTTP status of the content endpoints and
 * the resource API, which answer a warning with the HTTP status of its return
 * code and the usual JSON body.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class Calls {

    private static final Map<Class<?>, Object> APIS = new ConcurrentHashMap<>(16);
    private static ApiClient client = null;
    private static String clientUrl = null;

    private Calls() {
        // Private Constructor, this is a utility Class.
    }

    /**
     * The client for the given URL; a new one if the URL changed.
     *
     * @param url Base URL of EDS, e.g. {@code http://localhost:8080/api/v1}
     * @return The client
     */
    public static synchronized ApiClient client(final String url) {
        if ((client == null) || !url.equals(clientUrl)) {
            client = new ApiClient();
            client.updateBaseUri(url);
            clientUrl = url;
            APIS.clear();
        }

        return client;
    }

    /**
     * One instance of a generated API class per URL. Each instance builds
     * its own HttpClient, so they are kept rather than created per call.
     *
     * @param url     Base URL of EDS
     * @param type    The API class, e.g. {@code MembersApi.class}
     * @param factory Its constructor taking the client
     * @param <T>     The API class
     * @return The instance
     */
    public static synchronized <T> T api(final String url, final Class<T> type, final Function<ApiClient, T> factory) {
        final ApiClient current = client(url);
        return type.cast(APIS.computeIfAbsent(type, key -> factory.apply(current)));
    }

    /**
     * The Authorization header for a Session token, for the endpoints with
     * the security scheme {@code session}.
     *
     * @param session The Session token, or null for none
     * @return The headers, empty without a token
     */
    public static Map<String, String> bearer(final byte[] session) {
        return (session != null)
                ? Map.of("Authorization", "Bearer " + Base64.getEncoder().encodeToString(session))
                : Map.of();
    }

    @FunctionalInterface
    public interface Call<R> {
        R run() throws ApiException;
    }

    /**
     * Runs a call. A response with an error status and a JSON body is read
     * as the response type, as the tests check its return code.
     *
     * @param url   Base URL of EDS
     * @param type  The response type
     * @param call  The call
     * @param <R>   The response type
     * @return The response
     */
    static <R> R run(final String url, final Class<R> type, final Call<R> call) {
        try {
            return call.run();
        } catch (ApiException e) {
            return readError(url, type, e);
        }
    }

    public static <R> R readError(final String url, final Class<R> type, final ApiException e) {
        final String body = e.getResponseBody();
        if ((e.getCode() == 0) || (body == null) || body.isBlank()) {
            throw new StopTestException("Request failed with HTTP status " + e.getCode() + ": " + e.getMessage(), e);
        }

        try {
            return client(url).getObjectMapper().readValue(body, type);
        } catch (java.io.IOException ioe) {
            throw new StopTestException("Cannot read the response with HTTP status " + e.getCode() + ": " + body, ioe);
        }
    }
}
