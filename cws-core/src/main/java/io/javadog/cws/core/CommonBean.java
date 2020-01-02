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
package io.javadog.cws.core;

import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.services.Serviceable;
import java.util.logging.Logger;

/**
 * <p>Common functionality for the CWS Beans.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class CommonBean {

    private static final Logger LOG = Logger.getLogger(CommonBean.class.getName());

    /**
     * Private Constructor, this is a utility Class.
     */
    private CommonBean() {
    }

    public static void destroy(final Serviceable<?, ?, ?> serviceable) {
        if (serviceable != null) {
            try {
                serviceable.destroy();
            } catch (CWSException e) {
                LOG.log(Settings.WARN, "Failed destroying the Service: " + e.getMessage(), e);
            }
        }
    }
}
