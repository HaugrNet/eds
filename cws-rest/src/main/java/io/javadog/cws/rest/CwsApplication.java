/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-rest)
 * =============================================================================
 */
package io.javadog.cws.rest;

import io.javadog.cws.api.responses.CwsResponse;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@ApplicationPath("/")
public class CwsApplication extends Application {

    public static final String CONSUMES = MediaType.APPLICATION_XML;
    public static final String PRODUCES = MediaType.APPLICATION_XML;

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> set = new HashSet();

        set.add(CircleService.class);
        set.add(DataService.class);
        set.add(DataTypeService.class);
        set.add(MemberService.class);
        set.add(SanityService.class);
        set.add(SettingService.class);
        set.add(SignatureService.class);
        set.add(TrusteeService.class);
        set.add(VersionService.class);

        return set;
    }

    public static Response buildResponse(final CwsResponse cwsResponse) {
        return Response
                .status(cwsResponse.getReturnCode().getHttpCode())
                .type(PRODUCES)
                .entity(cwsResponse)
                .build();
    }
}
