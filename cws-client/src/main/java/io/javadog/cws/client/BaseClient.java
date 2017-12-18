/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
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
public class BaseClient {

    protected final ResteasyClient client = new ResteasyClientBuilder().build();
    protected final String baseURL;

    protected BaseClient(final String baseURL) {
        this.baseURL = baseURL;
    }

    protected <R extends Authentication> Response runRequest(final String requestURL, final R request) {
        final String url = baseURL + requestURL;
        final ResteasyWebTarget target = client.target(url);
        final Entity<R> entity = Entity.entity(request, MediaType.APPLICATION_JSON);
        return target.request().post(entity);
    }
}
