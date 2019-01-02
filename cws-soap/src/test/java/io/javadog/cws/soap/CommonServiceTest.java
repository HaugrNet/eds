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
package io.javadog.cws.soap;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.core.CommonBean;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.CommonDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.services.Serviceable;
import io.javadog.cws.core.services.SettingService;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import javax.persistence.EntityManager;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CommonServiceTest extends BeanSetup {

    @Test
    public void testConstantsConstructor() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        final Constructor<CommonBean> constructor = CommonBean.class.getDeclaredConstructor();
        assertThat(constructor.isAccessible(), is(false));
        constructor.setAccessible(true);
        final CommonBean commonBean = constructor.newInstance();

        assertThat(commonBean, is(not(nullValue())));
    }

    @Test
    public void testDestroy() {
        thrown.expect(CWSException.class);
        thrown.expectMessage("Method is not implemented.");

        final SettingService service = new SettingService(settings, entityManager);
        final FailService failService = new FailService(settings, entityManager);

        CommonBean.destroy(null);
        CommonBean.destroy(service);
        CommonBean.destroy(failService);

        assertThat(failService.perform(null), is(nullValue()));
    }

    /**
     * This Class only serves the purpose of providing a Service, which will
     * not work, i.e. fail when invoked. It is present to help test the error
     * handling.
     */
    private static final class FailService extends Serviceable<CommonDao, SettingResponse, SettingRequest> {

        /**
         * Default Constructor.
         *
         * @param settings      CWS Settings
         * @param entityManager Entity Manager instance
         */
        private FailService(final Settings settings, final EntityManager entityManager) {
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
}
