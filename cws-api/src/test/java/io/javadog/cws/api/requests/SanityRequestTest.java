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
package io.javadog.cws.api.requests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.TestUtilities;
import io.javadog.cws.api.common.Constants;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class SanityRequestTest {

    @Test
    void testClassflow() {
        final String circleId = UUID.randomUUID().toString();
        final Date since = new Date();

        final SanityRequest sanityRequest = new SanityRequest();
        sanityRequest.setAccountName(Constants.ADMIN_ACCOUNT);
        sanityRequest.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        sanityRequest.setCircleId(circleId);
        sanityRequest.setSince(since);

        final Map<String, String> errors = sanityRequest.validate();
        assertTrue(errors.isEmpty());
        assertEquals(Constants.ADMIN_ACCOUNT, sanityRequest.getAccountName());
        assertEquals(Constants.ADMIN_ACCOUNT, TestUtilities.convert(sanityRequest.getCredential()));
        assertEquals(circleId, sanityRequest.getCircleId());
        assertEquals(since, sanityRequest.getSince());
    }

    @Test
    void testEmptyClass() {
        final SanityRequest sanityRequest = new SanityRequest();
        final Map<String, String> errors = sanityRequest.validate();

        assertFalse(errors.isEmpty());
        assertEquals(1, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
    }

    @Test
    void testClassWithInvalidValues() {
        final String circleId = "Invalid Circle Id";

        final SanityRequest sanityRequest = new SanityRequest();
        sanityRequest.setCircleId(circleId);

        final Map<String, String> errors = sanityRequest.validate();
        assertFalse(errors.isEmpty());
        assertEquals(2, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
        assertEquals("The Circle Id is invalid.", errors.get(Constants.FIELD_CIRCLE_ID));
    }
}
