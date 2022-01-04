/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.cws.core;

import net.haugr.cws.core.exceptions.CWSException;
import net.haugr.cws.core.managers.AbstractManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Common functionality for the CWS Beans.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class CommonBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonBean.class);

    /**
     * Private Constructor, this is a utility Class.
     */
    private CommonBean() {
    }

    /**
     * <p>Invokes the standard method {@link AbstractManager#destroy()} on the given
     * Service instance, which should ensure that any active Keys will be
     * destroyed.</p>
     *
     * @param manager Internal Manager instance to invoke destroy() on
     */
    public static void destroy(final AbstractManager<?, ?, ?> manager) {
        if (manager != null) {
            try {
                manager.destroy();
            } catch (CWSException e) {
                LOGGER.warn("Failed destroying the Service: {}", e.getMessage(), e);
            }
        }
    }
}
