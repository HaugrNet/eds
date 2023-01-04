/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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
package net.haugr.cws.api.responses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.cws.api.TestUtilities;
import net.haugr.cws.api.common.ReturnCode;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class ProcessMemberResponseTest {

    @Test
    void testClassFlow() {
        final String memberId = UUID.randomUUID().toString();
        final String signature = "Invitation Signature";

        final ProcessMemberResponse response = new ProcessMemberResponse();
        response.setMemberId(memberId);
        response.setSignature(TestUtilities.convert(signature));

        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertTrue(response.isOk());
        assertEquals(memberId, response.getMemberId());
        assertEquals(signature, TestUtilities.convert(response.getSignature()));
    }

    @Test
    void testMessageConstructor() {
        final String message = "Request was successfully processed.";
        final ProcessMemberResponse response = new ProcessMemberResponse(message);

        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals(message, response.getReturnMessage());
        assertNull(response.getMemberId());
        assertNull(response.getSignature());
    }

    @Test
    void testError() {
        final String msg = "ProcessMember Request failed due to Verification Problems.";
        final ProcessMemberResponse response = new ProcessMemberResponse(ReturnCode.VERIFICATION_WARNING, msg);

        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals(msg, response.getReturnMessage());
        assertFalse(response.isOk());
        assertNull(response.getMemberId());
        assertNull(response.getSignature());
    }
}
