/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-war)
 * =============================================================================
 */
package io.javadog.cws.war;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.core.services.SettingService;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CommonBeanTest extends BeanSetup {

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
        final SettingService service = new SettingService(settings, entityManager);
        final FailService failService = new FailService(settings, entityManager);
        assertThat(failService.perform(null), is(nullValue()));

        CommonBean.destroy(null);
        CommonBean.destroy(service);
        CommonBean.destroy(failService);
    }

    /**
     * This Class only serves the purpose of providing a Service, which will
     * not work, i.e. fail when invoked. It is present to help test the error
     * handling.
     */
    private static final class FailService extends Serviceable<SettingResponse, SettingRequest> {

        /**
         * Default Constructor.
         *
         * @param settings      CWS Settings
         * @param entityManager Entity Manager instance
         */
        private FailService(final Settings settings, final EntityManager entityManager) {
            super(settings, entityManager);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SettingResponse perform(final SettingRequest request) {
            return null;
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
