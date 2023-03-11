/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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
package net.haugr.eds.api.requests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.TestUtilities;
import net.haugr.eds.api.common.Constants;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class FetchCircleRequestTest {

    @Test
    void testClassFlow() {
        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        final Map<String, String> errors = request.validate();

        assertTrue(errors.isEmpty());
        assertEquals(Constants.ADMIN_ACCOUNT, request.getAccountName());
        assertEquals(Constants.ADMIN_ACCOUNT, TestUtilities.convert(request.getCredential()));
    }

    @Test
    void testEmptyClass() {
        final FetchCircleRequest request = new FetchCircleRequest();
        final Map<String, String> errors = request.validate();

        assertEquals(1, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
    }
}
