/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2021, haugr.net
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
import net.haugr.cws.api.common.CredentialType;
import net.haugr.cws.api.common.MemberRole;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class ProcessMemberRequestTest {

    @Test
    void testClassFlow() {
        final String memberId = UUID.randomUUID().toString();
        final String newAccountName = "New AccountName";
        final String newCredential = "New Credential";
        final String publicKey = UUID.randomUUID().toString();

        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.CREATE);
        request.setMemberId(memberId);
        request.setMemberRole(MemberRole.ADMIN);
        request.setNewAccountName(newAccountName);
        request.setNewCredential(TestUtilities.convert(newCredential));
        request.setPublicKey(publicKey);
        final Map<String, String> errors = request.validate();

        assertTrue(errors.isEmpty());
        assertEquals(Constants.ADMIN_ACCOUNT, request.getAccountName());
        assertEquals(Constants.ADMIN_ACCOUNT, TestUtilities.convert(request.getCredential()));
        assertEquals(Action.CREATE, request.getAction());
        assertEquals(memberId, request.getMemberId());
        assertEquals(MemberRole.ADMIN, request.getMemberRole());
        assertEquals(newAccountName, request.getNewAccountName());
        assertEquals(newCredential, TestUtilities.convert(request.getNewCredential()));
        assertEquals(publicKey, request.getPublicKey());
    }

    @Test
    void testEmptyClass() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        final Map<String, String> errors = request.validate();

        assertEquals(2, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
        assertEquals("No action has been provided.", errors.get(Constants.FIELD_ACTION));
    }

    @Test
    void testInvalidAction() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.ADD);

        final Map<String, String> errors = request.validate();
        assertEquals(1, errors.size());
        assertEquals("Not supported Action has been provided.", errors.get(Constants.FIELD_ACTION));
    }

    @Test
    void testActionCreate() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setNewAccountName("New AccountName");
        request.setNewCredential(TestUtilities.convert("New Credential"));
        request.setAction(Action.CREATE);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionCreateFail() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.CREATE);

        final Map<String, String> errors = request.validate();
        assertEquals(2, errors.size());
        assertEquals("The New Account Name is missing.", errors.get(Constants.FIELD_NEW_ACCOUNT_NAME));
        assertEquals("The Credentials are required to create new Account.", errors.get(Constants.FIELD_NEW_CREDENTIAL));
    }

    @Test
    void testActionInvite() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setNewAccountName("New AccountName");
        request.setAction(Action.INVITE);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionInviteFail() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.INVITE);

        final Map<String, String> errors = request.validate();
        assertEquals(1, errors.size());
        assertEquals("The New Account Name is missing.", errors.get(Constants.FIELD_NEW_ACCOUNT_NAME));
    }

    @Test
    void testActionLogin() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setCredential(TestUtilities.convert(UUID.randomUUID().toString()));
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setNewCredential(TestUtilities.convert(UUID.randomUUID().toString()));
        request.setAction(Action.LOGIN);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionLoginFail() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAction(Action.LOGIN);

        final Map<String, String> errors = request.validate();
        assertEquals(2, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
        assertEquals("The Credentials are required to create new Session.", errors.get(Constants.FIELD_NEW_CREDENTIAL));
    }

    @Test
    void testActionLogout() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCredentialType(CredentialType.SESSION);
        request.setAction(Action.LOGOUT);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionLogoutFail() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAction(Action.LOGOUT);

        final Map<String, String> errors = request.validate();
        assertEquals(1, errors.size());
        assertEquals("The Session (Credential) is missing.", errors.get(Constants.FIELD_CREDENTIAL));
    }

    @Test
    void testActionAlter() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setMemberId(UUID.randomUUID().toString());
        request.setMemberRole(MemberRole.ADMIN);
        request.setAction(Action.ALTER);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionAlterFail() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setMemberId("123");
        request.setAction(Action.ALTER);

        final Map<String, String> errors = request.validate();
        assertEquals(2, errors.size());
        assertEquals("The given memberId is invalid.", errors.get(Constants.FIELD_MEMBER_ID));
        assertEquals("The Role is missing.", errors.get(Constants.FIELD_MEMBER_ROLE));
    }

    @Test
    void testActionUpdate() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setNewAccountName("Updated AccountName");
        request.setAction(Action.UPDATE);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionUpdateFail() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setNewAccountName("Ridiculously and invalid Name for an Account - it has to exceed our total maximum, hence this awfully name.");
        request.setAction(Action.UPDATE);

        final Map<String, String> errors = request.validate();
        assertEquals(1, errors.size());
        assertEquals("The newAccountName may not exceed 256 characters.", errors.get(Constants.FIELD_NEW_ACCOUNT_NAME));
    }

    @Test
    void testInvalidate() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.INVALIDATE);
        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionDelete() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setMemberId(UUID.randomUUID().toString());
        request.setAction(Action.DELETE);

        final Map<String, String> errors = request.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testActionDeleteFail() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setMemberId("Invalid Member Id");
        request.setAction(Action.DELETE);

        final Map<String, String> errors = request.validate();
        assertEquals(1, errors.size());
        assertEquals("The given memberId is invalid.", errors.get(Constants.FIELD_MEMBER_ID));
    }
}
