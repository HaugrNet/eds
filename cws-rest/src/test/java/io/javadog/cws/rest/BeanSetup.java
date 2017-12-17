/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-rest)
 * =============================================================================
 */
package io.javadog.cws.rest;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.SettingBean;
import io.javadog.cws.core.exceptions.CWSException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class BeanSetup extends DatabaseSetup {

    protected SettingBean prepareSettingBean() {
        try {
            final SettingBean bean = SettingBean.class.getConstructor().newInstance();
            setField(bean, "entityManager", entityManager);

            // Just invoking the postConstruct method, has to be done manually,
            // as the Bean is not managed in our tests ;-)
            bean.init();

            return bean;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    protected static void setField(final Object instance, final String fieldName, final Object value) {
        try {
            final Class<?> clazz = instance.getClass();
            final Field field;

            field = clazz.getDeclaredField(fieldName);
            final boolean accessible = field.isAccessible();

            field.setAccessible(true);
            field.set(instance, value);
            field.setAccessible(accessible);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot set Field", e);
        }
    }
}
