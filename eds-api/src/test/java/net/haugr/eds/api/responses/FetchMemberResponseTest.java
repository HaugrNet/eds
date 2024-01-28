/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.dtos.Circle;
import net.haugr.eds.api.dtos.Member;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class FetchMemberResponseTest {

    @Test
    void testClassFlow() {
        final List<Circle> circles = new ArrayList<>(1);
        circles.add(new Circle());
        final List<Member> members = new ArrayList<>(1);
        members.add(new Member());

        final FetchMemberResponse response = new FetchMemberResponse();
        response.setCircles(circles);
        response.setMembers(members);

        Assertions.assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertTrue(response.isOk());
        assertEquals(members, response.getMembers());
        assertEquals(circles, response.getCircles());
    }

    @Test
    void testError() {
        final String msg = "FetchMember Request failed due to Verification Problems.";
        final FetchMemberResponse response = new FetchMemberResponse(ReturnCode.VERIFICATION_WARNING, msg);

        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals(msg, response.getReturnMessage());
        assertFalse(response.isOk());
        assertTrue(response.getMembers().isEmpty());
        assertTrue(response.getCircles().isEmpty());
    }
}
