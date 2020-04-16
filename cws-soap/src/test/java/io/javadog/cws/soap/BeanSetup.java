/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2020, JavaDog.io
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
package io.javadog.cws.soap;

import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.ManagementBean;
import io.javadog.cws.core.ShareBean;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
class BeanSetup extends DatabaseSetup {

    /**
     * <p>Prepares a Share Service instance. If the given Objects are present,
     * then an internal Share Bean is set and so is the given resources. If no
     * objects are given, an empty service instance is returned without any
     * bean or other settings set.</p>
     *
     * @param objects Resources to be injected into Service instance
     * @return New Service instance with given resources injected
     */
    protected static ShareService prepareShareService(final Object... objects) {
        final ShareService service = new ShareService();
        if (objects.length > 0) {
            final ShareBean bean = new ShareBean();
            inject(service, bean);
            for (final Object object : objects) {
                inject(service, object);
                inject(bean, object);
            }
        }

        return service;
    }

    /**
     * <p>Prepares a Management Service instance. If the given Objects are
     * present, then an internal Management Bean is set and so is the given
     * resources. If no objects are given, an empty service instance is
     * returned without any bean or other settings set.</p>
     *
     * @param objects Resources to be injected into Service instance
     * @return New Service instance with given resources injected
     */
    protected static ManagementService prepareManagementService(final Object... objects) {
        final ManagementService service = new ManagementService();
        if (objects.length > 0) {
            final ManagementBean bean = new ManagementBean();
            inject(service, bean);
            for (final Object object : objects) {
                inject(service, object);
                inject(bean, object);
            }
        }

        return service;
    }
}
