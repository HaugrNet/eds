/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
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
 * @since  CWS 1.0
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
