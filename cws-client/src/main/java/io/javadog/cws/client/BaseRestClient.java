/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
 */
package io.javadog.cws.client;

import io.javadog.cws.api.requests.Authentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class BaseRestClient {

    protected  ResteasyClient client = new ResteasyClientBuilder().build();
    protected final String baseURL;

    protected BaseRestClient(final String baseURL) {
        this.baseURL = baseURL;
    }

    protected <R extends Authentication> Response runRequest(final String requestURL, final R request) {
        final String url = baseURL + requestURL;
        client = new ResteasyClientBuilder().build();
        final ResteasyWebTarget target = client.target(url);
        final Entity<R> entity = Entity.entity(request, MediaType.APPLICATION_XML);
        return target.request().accept(MediaType.APPLICATION_XML).post(entity);
    }

    protected void close(final Response response) {
        response.close();
    }
}
