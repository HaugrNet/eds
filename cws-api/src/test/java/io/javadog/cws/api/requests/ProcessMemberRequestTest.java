/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.TestUtilities;
import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessMemberRequestTest {

    @Test
    public void testClassflow() {
        final String memberId = UUID.randomUUID().toString();
        final String newAccountName = "New AccountName";
        final String newCredential = "New Credential";

        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.CREATE);
        request.setNewAccountName(newAccountName);
        request.setNewCredential(TestUtilities.convert(newCredential));
        request.setMemberId(memberId);
        final Map<String, String> errors = request.validate();

        assertThat(errors.isEmpty(), is(true));
        assertThat(request.getAccountName(), is(Constants.ADMIN_ACCOUNT));
        assertThat(TestUtilities.convert(request.getCredential()), is(Constants.ADMIN_ACCOUNT));
        assertThat(request.getAction(), is(Action.CREATE));
        assertThat(request.getNewAccountName(), is(newAccountName));
        assertThat(TestUtilities.convert(request.getNewCredential()), is(newCredential));
        assertThat(request.getMemberId(), is(memberId));
    }

    @Test
    public void testEmptyClass() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        final Map<String, String> errors = request.validate();

        assertThat(errors.size(), is(3));
        assertThat(errors.get(Constants.FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Credential is missing."));
        assertThat(errors.get(Constants.FIELD_ACTION), is("No action has been provided."));
    }

    @Test
    public void testInvalidAction() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.ADD);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_ACTION), is("Not supported Action has been provided."));
    }

    @Test
    public void testActionCreate() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setNewAccountName("New AccountName");
        request.setNewCredential(TestUtilities.convert("New Credential"));
        request.setAction(Action.CREATE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionCreateFail() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.CREATE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(2));
        assertThat(errors.get(Constants.FIELD_NEW_ACCOUNT_NAME), is("The New Account Name is missing."));
        assertThat(errors.get(Constants.FIELD_NEW_CREDENTIAL), is("The Credentials are required to create new Account."));
    }

    @Test
    public void testActionInvite() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setNewAccountName("New AccountName");
        request.setAction(Action.INVITE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionInviteFail() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.INVITE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_NEW_ACCOUNT_NAME), is("The New Account Name is missing."));
    }

    @Test
    public void testActionLogin() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setCredential(TestUtilities.convert(UUID.randomUUID().toString()));
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setNewCredential(TestUtilities.convert(UUID.randomUUID().toString()));
        request.setAction(Action.LOGIN);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionLoginFail() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAction(Action.LOGIN);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(3));
        assertThat(errors.get(Constants.FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Credential is missing."));
        assertThat(errors.get(Constants.FIELD_NEW_CREDENTIAL), is("The Credentials are required to create new Session."));
    }

    @Test
    public void testActionLogout() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCredentialType(CredentialType.SESSION);
        request.setAction(Action.LOGOUT);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionLogoutFail() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAction(Action.LOGOUT);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(2));
        assertThat(errors.get(Constants.FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Credential is missing."));
    }

    @Test
    public void testActionProcess() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setNewAccountName("Updated AccountName");
        request.setAction(Action.UPDATE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionProcessFail() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setNewAccountName("Ridiculously and invalid Name for an Account - it has to exceed our total maximum, hence this awfully name.");
        request.setAction(Action.INVITE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_NEW_ACCOUNT_NAME), is("The newAccountName may not exceed 256 characters."));
    }

    @Test
    public void testInvalidate() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.INVALIDATE);
        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionDelete() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setMemberId(UUID.randomUUID().toString());
        request.setAction(Action.DELETE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionDeleteFail() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setMemberId("Invalid Member Id");
        request.setAction(Action.DELETE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_MEMBER_ID), is("The given memberId is invalid."));
    }
}
