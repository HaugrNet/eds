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

import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.responses.CwsResponse;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class BaseRestClient {

    protected  ResteasyClient client = new ResteasyClientBuilder().build();
    private final String baseURL;

    protected BaseRestClient(final String baseURL) {
        this.baseURL = baseURL;
    }

    protected <R extends Authentication, C extends CwsResponse> C runRequest(final Class<C> clazz, final String requestURL, final R request) {
        final String url = baseURL + requestURL;
        client = new ResteasyClientBuilder().build();
        final ResteasyWebTarget target = client.target(url);
        final Entity<R> entity = Entity.entity(request, MediaType.APPLICATION_XML);

        try (final Response response = target.request().accept(MediaType.APPLICATION_XML).post(entity)) {
            return response.readEntity(clazz);
        }
    }
}
