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
package io.javadog.cws.rest;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * <p>The CWS REST Application, i.e. the listing of all CWS based REST Service
 * classes, which should be exposed.</p>
 *
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
        final Set<Class<?>> set = new HashSet<>();

        set.add(CircleService.class);
        set.add(DataService.class);
        set.add(DataTypeService.class);
        set.add(MasterKeyService.class);
        set.add(MemberService.class);
        set.add(SanityService.class);
        set.add(SettingService.class);
        set.add(SignatureService.class);
        set.add(TrusteeService.class);
        set.add(VersionService.class);

        return set;
    }
}
