/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
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
package io.javadog.cws.rest;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.ManagementBean;
import io.javadog.cws.core.ShareBean;
import io.javadog.cws.core.exceptions.CWSException;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
class BeanSetup extends DatabaseSetup {

    /**
     * <p>Prepares a Authenticated Service instance. If the given Objects are
     * present, then an internal Management Bean is set and so is the given
     * resources. If no Objects are given, an empty service instance is
     * returned without any bean or other settings set.</p>
     *
     * @param objects Resources to be injected into Service instance
     * @return New Service instance with given resources injected
     */
    protected static AuthenticatedService prepareAuthenticatedService(final Object... objects) {
        return prepareService(AuthenticatedService.class, ManagementBean.class, objects);
    }

    /**
     * <p>Prepares a Circle Service instance. If the given Objects are present,
     * then an internal Management Bean is set and so is the given resources.
     * If no Objects are given, an empty service instance is returned without
     * any bean or other settings set.</p>
     *
     * @param objects Resources to be injected into Service instance
     * @return New Service instance with given resources injected
     */
    protected static CircleService prepareCircleService(final Object... objects) {
        return prepareService(CircleService.class, ManagementBean.class, objects);
    }

    /**
     * <p>Prepares a Data Service instance. If the given Objects are present,
     * then an internal Share Bean is set and so is the given resources. If no
     * Objects are given, an empty service instance is returned without any
     * bean or other settings set.</p>
     *
     * @param objects Resources to be injected into Service instance
     * @return New Service instance with given resources injected
     */
    protected static DataService prepareDataService(final Object... objects) {
        return prepareService(DataService.class, ShareBean.class, objects);
    }

    /**
     * <p>Prepares a DataType Service instance. If the given Objects are
     * present, then an internal Share Bean is set and so is the given
     * resources. If no Objects are given, an empty service instance is
     * returned without any bean or other settings set.</p>
     *
     * @param objects Resources to be injected into Service instance
     * @return New Service instance with given resources injected
     */
    protected static DataTypeService prepareDataTypeService(final Object... objects) {
        return prepareService(DataTypeService.class, ShareBean.class, objects);
    }

    /**
     * <p>Prepares a Inventory Service instance. If the given Objects are
     * present, then an internal Management Bean is set and so is the given
     * resources. If no Objects are given, an empty service instance is
     * returned without any bean or other settings set.</p>
     *
     * @param objects Resources to be injected into Service instance
     * @return New Service instance with given resources injected
     */
    protected static InventoryService prepareInventoryService(final Object... objects) {
        return prepareService(InventoryService.class, ManagementBean.class, objects);
    }

    /**
     * <p>Prepares a MasterKey Service instance. If the given Objects are
     * present, then an internal Management Bean is set and so is the given
     * resources. If no Objects are given, an empty service instance is
     * returned without any bean or other settings set.</p>
     *
     * @param objects Resources to be injected into Service instance
     * @return New Service instance with given resources injected
     */
    protected static MasterKeyService prepareMasterKeyService(final Object... objects) {
        return prepareService(MasterKeyService.class, ManagementBean.class, objects);
    }

    /**
     * <p>Prepares a Member Service instance. If the given Objects are present,
     * then an internal Management Bean is set and so is the given resources.
     * If no Objects are given, an empty service instance is returned without
     * any bean or other settings set.</p>
     *
     * @param objects Resources to be injected into Service instance
     * @return New Service instance with given resources injected
     */
    protected static MemberService prepareMemberService(final Object... objects) {
        return prepareService(MemberService.class, ManagementBean.class, objects);
    }

    /**
     * <p>Prepares a Sanity Service instance. If the given Objects are present,
     * then an internal Management Bean is set and so is the given resources.
     * If no Objects are given, an empty service instance is returned without
     * any bean or other settings set.</p>
     *
     * @param objects Resources to be injected into Service instance
     * @return New Service instance with given resources injected
     */
    protected static SanityService prepareSanityService(final Object... objects) {
        return prepareService(SanityService.class, ManagementBean.class, objects);
    }

    /**
     * <p>Prepares a Setting Service instance. If the given Objects are
     * present, then an internal Share Bean is set and so is the given
     * resources. If no Objects are given, an empty service instance is
     * returned without any bean or other settings set.</p>
     *
     * @param objects Resources to be injected into Service instance
     * @return New Service instance with given resources injected
     */
    protected static SettingService prepareSettingService(final Object... objects) {
        return prepareService(SettingService.class, ShareBean.class, objects);
    }

    /**
     * <p>Prepares a Signature Service instance. If the given Objects are
     * present, then an internal Share Bean is set and so is the given
     * resources. If no Objects are given, an empty service instance is
     * returned without any bean or other settings set.</p>
     *
     * @param objects Resources to be injected into Service instance
     * @return New Service instance with given resources injected
     */
    protected static SignatureService prepareSignatureService(final Object... objects) {
        return prepareService(SignatureService.class, ShareBean.class, objects);
    }

    /**
     * <p>Prepares a Trustee Service instance. If the given Objects are
     * present, then an internal Management Bean is set and so is the given
     * resources. If no Objects are given, an empty service instance is
     * returned without any bean or other settings set.</p>
     *
     * @param objects Resources to be injected into Service instance
     * @return New Service instance with given resources injected
     */
    protected static TrusteeService prepareTrusteeService(final Object... objects) {
        return prepareService(TrusteeService.class, ManagementBean.class, objects);
    }

    /**
     * <p>Prepares a Version Service instance. If the given Objects are
     * present, then an internal Management Bean is set and so is the given
     * resources. If no Objects are given, an empty service instance is
     * returned without any bean or other settings set.</p>
     *
     * @param objects Resources to be injected into Service instance
     * @return New Service instance with given resources injected
     */
    protected static VersionService prepareVersionService(final Object... objects) {
        return prepareService(VersionService.class, ManagementBean.class, objects);
    }

    private static <S, B> S prepareService(final Class<S> serviceClass, final Class<B> beanClass, final Object... objects) {
        try {
            final S service = serviceClass.newInstance();
            if (objects.length > 0) {
                final B bean = beanClass.newInstance();
                inject(service, bean);
                for (final Object object : objects) {
                    inject(service, object);
                    inject(bean, object);
                }
            }

            return service;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new CWSException(ReturnCode.ERROR, e);
        }
    }
}
