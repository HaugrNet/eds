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
package net.haugr.cws.api.requests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.cws.api.TestUtilities;
import net.haugr.cws.api.common.Action;
import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.common.TrustLevel;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class ProcessTrusteeRequestTest {

    @Test
    void testClassFlow() {
        final String circleId = UUID.randomUUID().toString();
        final String memberId = UUID.randomUUID().toString();

        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.ADD);
        request.setCircleId(circleId);
        request.setMemberId(memberId);
        request.setTrustLevel(TrustLevel.WRITE);
        final Map<String, String> errors = request.validate();

        assertTrue(errors.isEmpty());
        assertEquals(Constants.ADMIN_ACCOUNT, request.getAccountName());
        assertEquals(Constants.ADMIN_ACCOUNT, TestUtilities.convert(request.getCredential()));
        assertEquals(Action.ADD, request.getAction());
        assertEquals(circleId, request.getCircleId());
        assertEquals(memberId, request.getMemberId());
        assertEquals(TrustLevel.WRITE, request.getTrustLevel());
    }

    @Test
    void testEmptyClass() {
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();
        final Map<String, String> errors = request.validate();

        assertEquals(2, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
        assertEquals("No action has been provided.", errors.get(Constants.FIELD_ACTION));
    }

    @Test
    void testInvalidAction() {
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.PROCESS);

        final Map<String, String> errors = request.validate();
        assertEquals(1, errors.size());
        assertEquals("Not supported Action has been provided.", errors.get(Constants.FIELD_ACTION));
    }

    @Test
    void testActionAdd() {
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId(UUID.randomUUID().toString());
        request.setMemberId(UUID.randomUUID().toString());
        request.setTrustLevel(TrustLevel.WRITE);
        request.setAction(Action.ADD);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionAddFail() {
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId(null);
        request.setMemberId("Invalid MemberId");
        request.setTrustLevel(null);
        request.setAction(Action.ADD);

        final Map<String, String> errors = request.validate();
        assertEquals(3, errors.size());
        assertEquals("Cannot add a Trustee to a Circle, without a Circle Id.", errors.get(Constants.FIELD_CIRCLE_ID));
        assertEquals("Cannot add a Trustee to a Circle, without a Member Id.", errors.get(Constants.FIELD_MEMBER_ID));
        assertEquals("Cannot add a Trustee to a Circle, without an initial TrustLevel.", errors.get(Constants.FIELD_TRUSTLEVEL));
    }

    @Test
    void testActionAlter() {
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId(UUID.randomUUID().toString());
        request.setMemberId(UUID.randomUUID().toString());
        request.setTrustLevel(TrustLevel.WRITE);
        request.setAction(Action.ALTER);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionAlterFail() {
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId("Invalid Circle Id");
        request.setMemberId(null);
        request.setTrustLevel(null);
        request.setAction(Action.ALTER);

        final Map<String, String> errors = request.validate();
        assertEquals(3, errors.size());
        assertEquals("Cannot alter a Trustees TrustLevel, without knowing the Circle Id.", errors.get(Constants.FIELD_CIRCLE_ID));
        assertEquals("Cannot alter a Trustees TrustLevel, without knowing the Member Id.", errors.get(Constants.FIELD_MEMBER_ID));
        assertEquals("Cannot alter a Trustees TrustLevel, without knowing the new TrustLevel.", errors.get(Constants.FIELD_TRUSTLEVEL));
    }

    @Test
    void testActionRemove() {
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId(UUID.randomUUID().toString());
        request.setMemberId(UUID.randomUUID().toString());
        request.setAction(Action.REMOVE);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionRemoveFail() {
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId(null);
        request.setMemberId("invalid Member Id");
        request.setAction(Action.REMOVE);

        final Map<String, String> errors = request.validate();
        assertEquals(2, errors.size());
        assertEquals("Cannot remove a Trustee from a Circle, without knowing the Circle Id.", errors.get(Constants.FIELD_CIRCLE_ID));
        assertEquals("Cannot remove a Trustee from a Circle, without knowing the Member Id.", errors.get(Constants.FIELD_MEMBER_ID));
    }

    @Test
    void testValidTrustLevel() {
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId(UUID.randomUUID().toString());
        request.setMemberId(UUID.randomUUID().toString());
        request.setTrustLevel(TrustLevel.SYSOP);
        request.setAction(Action.ADD);

        final Map<String, String> errors = request.validate();
        assertEquals(1, errors.size());
        assertEquals("The TrustLevel must be one of [READ, WRITE, ADMIN].", errors.get(Constants.FIELD_TRUSTLEVEL));
    }
}
