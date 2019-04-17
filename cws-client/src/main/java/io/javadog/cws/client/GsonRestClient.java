/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
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
package io.javadog.cws.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.responses.CwsResponse;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * @author Kim Jensen
 * @since CWS 1.1
 */
public class GsonRestClient {

    private static final String CONTENT_TYPE = "application/json";
    private final String baseURL;

    protected GsonRestClient(final String baseURL) {
        this.baseURL = baseURL;
    }

    protected <R extends Authentication, C extends CwsResponse> C runRequest(final Class<C> clazz, final String requestURL, final R request) {
        HttpURLConnection connection = null;

        try {
            final URL url = new URL(baseURL + requestURL);
            final Gson gson = createGsonInstance();
            final String json = gson.toJson(request);
            connection = (HttpURLConnection) url.openConnection();

            sendRequest(connection, json);
            final int responseCode = connection.getResponseCode();
            final C response;

            if (responseCode == ReturnCode.SUCCESS.getCode()) {
                final String received = readResponse(connection);
                response = gson.fromJson(received, clazz);
            } else {
                response = clazz
                        .getConstructor(ReturnCode.class, String.class)
                        .newInstance(ReturnCode.findReturnCode(responseCode),
                                connection.getResponseMessage());
            }

            return response;
        } catch (IOException | InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new CWSClientException(e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static Gson createGsonInstance() {
        final GsonBuilder builder = new GsonBuilder()
                .registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, type, context) -> {
                    try {
                        return new Date(Long.valueOf(json.getAsJsonPrimitive().getAsString()));
                    } catch (NumberFormatException e) {
                        throw new CWSClientException(e);
                    }
                })
                .registerTypeAdapter(Date.class, (JsonSerializer<Date>) (date, type, context) -> new JsonPrimitive(date.getTime()));

        return builder.create();
    }

    private static void sendRequest(final HttpURLConnection connection, final String json) throws IOException {
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", CONTENT_TYPE);

        if (!"null".equals(json)) {
            connection.setRequestProperty("Content-Type", CONTENT_TYPE + "; " + StandardCharsets.UTF_8.displayName());
            connection.setRequestProperty("Content-Length", String.valueOf(json.length()));
            connection.setDoOutput(true);

            try (final DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                outputStream.writeChars(json);
            }
        }
    }

    private static String readResponse(final HttpURLConnection connection) throws IOException {
        try (final InputStream inputStream = connection.getInputStream();
                final InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                final BufferedReader reader = new BufferedReader(streamReader)) {
            final StringBuilder response = new StringBuilder(0);
            String line = reader.readLine();
            while (line != null) {
                response.append(line);
                line = reader.readLine();
            }

            return response.toString();
        }
    }
}
