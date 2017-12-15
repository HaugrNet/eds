/*
 * =====================================================================================================================
 * Copyright (c) 2010-2017, secunet Security Networks AG, Germany
 * ---------------------------------------------------------------------------------------------------------------------
 * Project: DÜbEL (duebel-ws160)
 * =====================================================================================================================
 */
package io.javadog.cws.rest;

import io.javadog.cws.core.SystemBean;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@ApplicationPath("version")
@Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
public class VersionService extends Application {

    @Inject private SystemBean bean;

    @GET
    //@Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Response version() {
        return Response.ok().entity(bean.version()).build();
    }
}
