/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-rest)
 * =============================================================================
 */
package io.javadog.cws.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@ApplicationPath("/")
public class CwsApplication extends Application {

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
}
