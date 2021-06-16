/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.client.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.responses.CwsResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import javax.ws.rs.core.MediaType;

/**
 * <p>Common functionality for the Gson based CWS REST Clients. It handles the
 * actual requests.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.1
 */
public class GsonRestClient {

    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final Gson GSON = createGsonInstance();
    private final String baseURL;

    protected GsonRestClient(final String baseURL) {
        this.baseURL = baseURL;
    }

    protected <R extends Authentication, C extends CwsResponse> C runRequest(final Class<C> clazz, final String requestURL, final R request) {
        final HttpRequest httpRequest = HttpRequest
                .newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(baseURL + requestURL))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .header("Accept", MediaType.APPLICATION_JSON)
                .POST(prepareBodyPublisher(request))
                .build();
        try {
            final HttpResponse<String> response = HttpClient
                    .newHttpClient()
                    .send(httpRequest, HttpResponse.BodyHandlers.ofString(CHARSET));

            return GSON.fromJson(response.body(), clazz);
        } catch (IOException | JsonSyntaxException | IllegalStateException e) {
            throw new RESTClientException("Communication / Transformation problem: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RESTClientException("Thread was interrupted: " + e.getMessage(), e);
        }
    }

    private static <R extends Authentication> HttpRequest.BodyPublisher prepareBodyPublisher(final R request) {
        final String tmp = GSON.toJson(request);
        // For some weird reason, the ASCII character '=' is being replaced
        // by the UTF-8 encoded alternative in the Json. The '=' is a white
        // space filler for Base64 encoded strings.
        final String json = tmp.replace("\\u003d", "=");

        return ("null".equals(json))
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json, CHARSET);
    }

    // =========================================================================
    // GSon Adapters, to process Objects which otherwise cause problems
    // =========================================================================

    private static Gson createGsonInstance() {
        final GsonBuilder builder = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(byte[].class, new ByteArrayAdapter());

        return builder.create();
    }

    /**
     * GSon Adapter to handle Dates.
     */
    static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalDateTime deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(json.getAsJsonPrimitive().getAsString())), ZoneOffset.UTC);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonElement serialize(final LocalDateTime src, final Type typeOfSrc, final JsonSerializationContext context) {
            return new JsonPrimitive(src.toInstant(ZoneOffset.UTC).toEpochMilli());
        }
    }

    /**
     * GSon Adapter to handle Byte Arrays.
     */
    static class ByteArrayAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {

        /**
         * {@inheritDoc}
         */
        @Override
        public byte[] deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
            return Base64.getDecoder().decode(json.getAsJsonPrimitive().getAsString());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonElement serialize(final byte[] src, final Type typeOfSrc, final JsonSerializationContext context) {
            return new JsonPrimitive(Base64.getEncoder().encodeToString(src));
        }
    }
}
