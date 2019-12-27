/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2020, JavaDog.io
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
import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class ProcessCircleRequestTest {

    @Test
    void testClassflow() {
        final String circleId = UUID.randomUUID().toString();
        final String circleName = "New Circle Name";
        final String memberId = UUID.randomUUID().toString();
        final String circleKey = UUID.randomUUID().toString();

        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.CREATE);
        request.setCircleId(circleId);
        request.setCircleName(circleName);
        request.setMemberId(memberId);
        request.setCircleKey(circleKey);
        final Map<String, String> errors = request.validate();

        assertTrue(errors.isEmpty());
        assertEquals(Constants.ADMIN_ACCOUNT, request.getAccountName());
        assertEquals(Constants.ADMIN_ACCOUNT, TestUtilities.convert(request.getCredential()));
        assertEquals(Action.CREATE, request.getAction());
        assertEquals(circleId, request.getCircleId());
        assertEquals(circleName, request.getCircleName());
        assertEquals(memberId, request.getMemberId());
        assertEquals(circleKey, request.getCircleKey());
    }

    @Test
    void testEmptyClass() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        final Map<String, String> errors = request.validate();

        assertEquals(2, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
        assertEquals("No action has been provided.", errors.get(Constants.FIELD_ACTION));
    }

    @Test
    void testInvalidAction() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.PROCESS);

        final Map<String, String> errors = request.validate();
        assertEquals(1, errors.size());
        assertEquals("Not supported Action has been provided.", errors.get(Constants.FIELD_ACTION));
    }

    @Test
    void testActionCreate() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleName("New Circle");
        request.setMemberId(UUID.randomUUID().toString());
        request.setAction(Action.CREATE);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionCreateFail() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleName(null);
        request.setMemberId("Invalid Member Id");
        request.setAction(Action.CREATE);

        final Map<String, String> errors = request.validate();
        assertEquals(1, errors.size());
        assertEquals("Cannot create a new Circle, without the Circle Name.", errors.get(Constants.FIELD_CIRCLE_NAME));
    }

    @Test
    void testActionUpdate() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId(UUID.randomUUID().toString());
        request.setCircleName("New Circle Name");
        request.setAction(Action.UPDATE);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionUpdateFail() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId("Invalid Circle Id");
        request.setCircleName("Invalid Circle Name, as it is too long and thus not acceptable. The max length of a Circle Name is 75 characters.");
        request.setAction(Action.UPDATE);

        final Map<String, String> errors = request.validate();
        assertEquals(2, errors.size());
        assertEquals("Cannot update the Circle Name, without knowing the Circle Id.", errors.get(Constants.FIELD_CIRCLE_ID));
        assertEquals("The circleName may not exceed 75 characters.", errors.get(Constants.FIELD_CIRCLE_NAME));
    }

    @Test
    void testActionDelete() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId(UUID.randomUUID().toString());
        request.setAction(Action.DELETE);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionDeleteFail() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId(null);
        request.setAction(Action.DELETE);

        final Map<String, String> errors = request.validate();
        assertEquals(1, errors.size());
        assertEquals("Cannot delete a Circle, without knowing the Circle Id.", errors.get(Constants.FIELD_CIRCLE_ID));
    }

    @Test
    void testNonEmptyName() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAction(Action.CREATE);
        request.setCircleName("  Name ");

        final Map<String, String> errors = request.validate();
        assertFalse(errors.containsKey(Constants.FIELD_CIRCLE_NAME));
    }

    @Test
    void testEmptyName() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAction(Action.CREATE);
        request.setCircleName("   ");

        final Map<String, String> errors = request.validate();
        assertEquals("Cannot create a new Circle, without the Circle Name.", errors.get(Constants.FIELD_CIRCLE_NAME));
    }
}
