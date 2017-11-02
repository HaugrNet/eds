/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-war)
 * =============================================================================
 */
package io.javadog.cws.war;

import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Serviceable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class CommonBean {

    private static final Logger log = LoggerFactory.getLogger(CommonBean.class);

    public static void destroy(final Serviceable<?, ?> serviceable) {
        if (serviceable != null) {
            try {
                serviceable.destroy();
            } catch (CWSException e) {
                log.error("Failed destroying the Service: {}", e.getMessage(), e);
            }
        }
    }
}
