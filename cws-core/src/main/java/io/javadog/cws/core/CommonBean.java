/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core;

import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.services.Serviceable;

import java.util.logging.Logger;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CommonBean {

    private static final Logger log = Logger.getLogger(CommonBean.class.getName());

    /**
     * Private Constructor, this is a utility Class.
     */
    private CommonBean() {
    }

    public static void destroy(final Serviceable<?, ?> serviceable) {
        if (serviceable != null) {
            try {
                serviceable.destroy();
            } catch (CWSException e) {
                log.log(Settings.WARN, "Failed destroying the Service: " + e.getMessage(), e);
            }
        }
    }
}
