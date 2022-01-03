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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.haugr.cws.api.common.ReturnCode;
import net.haugr.cws.core.exceptions.CWSException;
import net.haugr.cws.core.services.SettingService;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import net.haugr.cws.core.setup.fakes.FailService;
import net.haugr.cws.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class CommonBeanTest extends DatabaseSetup {

    @Test
    void testConstantsConstructor() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        final Constructor<CommonBean> constructor = CommonBean.class.getDeclaredConstructor();
        assertFalse(constructor.canAccess(null));
        constructor.setAccessible(true);
        final CommonBean commonBean = constructor.newInstance();

        assertNotNull(commonBean);
    }

    @Test
    void testDestroy() {
        final SettingService service = new SettingService(settings, entityManager);
        final FailService failService = new FailService(settings, entityManager);

        CommonBean.destroy(null);
        CommonBean.destroy(service);
        CommonBean.destroy(failService);

        final CWSException cause = assertThrows(CWSException.class, () -> failService.perform(null));
        assertEquals(ReturnCode.ERROR, cause.getReturnCode());
        assertEquals("Method is not implemented.", cause.getMessage());
    }
}
