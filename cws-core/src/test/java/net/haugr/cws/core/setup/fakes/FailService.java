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
package net.haugr.cws.core.setup.fakes;

import net.haugr.cws.api.common.ReturnCode;
import net.haugr.cws.api.requests.SettingRequest;
import net.haugr.cws.api.responses.SettingResponse;
import net.haugr.cws.core.exceptions.CWSException;
import net.haugr.cws.core.model.CommonDao;
import net.haugr.cws.core.model.Settings;
import net.haugr.cws.core.services.Serviceable;
import javax.persistence.EntityManager;

/**
 * This Class only serves the purpose of providing a Service, which will
 * not work, i.e. fail when invoked. It is present to help test the error
 * handling.
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class FailService extends Serviceable<CommonDao, SettingResponse, SettingRequest> {

    /**
     * Default Constructor.
     *
     * @param settings      CWS Settings
     * @param entityManager Entity Manager instance
     */
    public FailService(final Settings settings, final EntityManager entityManager) {
        super(settings, new CommonDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettingResponse perform(final SettingRequest request) {
        throw new CWSException(ReturnCode.ERROR, "Method is not implemented.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        throw new CWSException(ReturnCode.CRYPTO_ERROR, "Cannot destroy failed service.");
    }
}
