/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.core.services.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import net.haugr.eds.api.common.ReturnCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

/**
 * Every return code is documented, and the generated list matches the enum.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class ReturnCodeCausesTest {

    @ParameterizedTest
    @EnumSource(ReturnCode.class)
    void testEveryReturnCodeHasACause(final ReturnCode returnCode) {
        assertFalse(ReturnCodeCauses.of(returnCode).isBlank(), "No cause documented for " + returnCode);
    }

    @Test
    void testReturnCodeListMatchesEnum() {
        final List<Map<String, Object>> list = EDSOpenApiFilter.returnCodeList();
        assertEquals(ReturnCode.values().length, list.size());
        for (int i = 0; i < list.size(); i++) {
            final ReturnCode returnCode = ReturnCode.values()[i];
            assertEquals(returnCode.name(), list.get(i).get("name"));
            assertEquals(returnCode.getCode(), list.get(i).get("returnCode"));
        }
    }
}
