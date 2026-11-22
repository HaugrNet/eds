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
package net.haugr.eds.api.responses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.TestUtilities;
import net.haugr.eds.api.common.ReturnCode;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class ProcessMemberResponseTest {

    @Test
    void testClassFlow() {
        final String memberId = UUID.randomUUID().toString();
        final String invitation = "Invitation Token";

        final ProcessMemberResponse response = new ProcessMemberResponse();
        response.setMemberId(memberId);
        response.setInvitation(TestUtilities.convert(invitation));
        response.setSession(TestUtilities.convert("Session Token"));

        Assertions.assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertTrue(response.isOk());
        assertEquals(memberId, response.getMemberId());
        assertEquals(invitation, TestUtilities.convert(response.getInvitation()));
        assertEquals("Session Token", TestUtilities.convert(response.getSession()));
    }

    @Test
    void testMessageConstructor() {
        final String message = "Request was successfully processed.";
        final ProcessMemberResponse response = new ProcessMemberResponse(message);

        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals(message, response.getReturnMessage());
        assertNull(response.getMemberId());
        assertNull(response.getInvitation());
    }

    @Test
    void testError() {
        final String msg = "ProcessMember Request failed due to Verification Problems.";
        final ProcessMemberResponse response = new ProcessMemberResponse(ReturnCode.VERIFICATION_WARNING, msg);

        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals(msg, response.getReturnMessage());
        assertFalse(response.isOk());
        assertNull(response.getMemberId());
        assertNull(response.getInvitation());
    }
}
