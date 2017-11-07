/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.TrustLevel;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessCircleRequestTest {

    @Test
    public void testClassflow() {
        final String circleId = UUID.randomUUID().toString();
        final String circleName = "New Circle Name";
        final String memberId = UUID.randomUUID().toString();

        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        request.setAction(Action.ADD);
        request.setCircleId(circleId);
        request.setCircleName(circleName);
        request.setMemberId(memberId);
        request.setTrustLevel(TrustLevel.WRITE);
        final Map<String, String> errors = request.validate();

        assertThat(errors.isEmpty(), is(true));
        assertThat(request.getAccountName(), is(Constants.ADMIN_ACCOUNT));
        assertThat(request.getCredential(), is(Constants.ADMIN_ACCOUNT));
        assertThat(request.getAction(), is(Action.ADD));
        assertThat(request.getCircleId(), is(circleId));
        assertThat(request.getCircleName(), is(circleName));
        assertThat(request.getMemberId(), is(memberId));
        assertThat(request.getTrustLevel(), is(TrustLevel.WRITE));
    }

    @Test
    public void testEmptyClass() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        final Map<String, String> errors = request.validate();

        assertThat(errors.size(), is(3));
        assertThat(errors.get(Constants.FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Credential is missing."));
        assertThat(errors.get(Constants.FIELD_ACTION), is("No action has been provided."));
    }

    @Test
    public void testInvalidAction() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        request.setAction(Action.PROCESS);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_ACTION), is("Not supported Action has been provided."));
    }

    @Test
    public void testActionCreate() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        request.setCircleName("New Circle");
        request.setMemberId(UUID.randomUUID().toString());
        request.setAction(Action.CREATE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionCreateFail() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        request.setCircleName(null);
        request.setMemberId("Invalid Member Id");
        request.setAction(Action.CREATE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(2));
        assertThat(errors.get(Constants.FIELD_CIRCLE_NAME), is("Cannot create a new Circle, without the Circle Name."));
        assertThat(errors.get(Constants.FIELD_MEMBER_ID), is("Cannot create a new Circle, without an initial Circle Administrator, please provide a Member Id."));
    }

    @Test
    public void testActionUpdate() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        request.setCircleId(UUID.randomUUID().toString());
        request.setCircleName("New Circle Name");
        request.setAction(Action.UPDATE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionUpdateFail() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        request.setCircleId("Invalid Circle Id");
        request.setCircleName("Invalid Circle Name, as it is too long and thus not acceptable. The max length of a Circle Name is 75 characters.");
        request.setAction(Action.UPDATE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(2));
        assertThat(errors.get(Constants.FIELD_CIRCLE_ID), is("Cannot update the Circle Name, without knowing the Circle Id."));
        assertThat(errors.get(Constants.FIELD_CIRCLE_NAME), is("The circleName may not exceed 75 characters."));
    }

    @Test
    public void testActionDelete() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        request.setCircleId(UUID.randomUUID().toString());
        request.setAction(Action.DELETE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionDeleteFail() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        request.setCircleId(null);
        request.setAction(Action.DELETE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_CIRCLE_ID), is("Cannot delete a Circle, without knowing the Circle Id."));
    }

    @Test
    public void testActionAdd() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        request.setCircleId(UUID.randomUUID().toString());
        request.setMemberId(UUID.randomUUID().toString());
        request.setTrustLevel(TrustLevel.WRITE);
        request.setAction(Action.ADD);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionAddFail() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        request.setCircleId(null);
        request.setMemberId("Invalid MemberId");
        request.setTrustLevel(null);
        request.setAction(Action.ADD);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(3));
        assertThat(errors.get(Constants.FIELD_CIRCLE_ID), is("Cannot add a Trustee to a Circle, without a Circle Id."));
        assertThat(errors.get(Constants.FIELD_MEMBER_ID), is("Cannot add a Trustee to a Circle, without a Member Id."));
        assertThat(errors.get(Constants.FIELD_TRUSTLEVEL), is("Cannot add a Trustee to a Circle, without an initial TrustLevel."));
    }

    @Test
    public void testActionAlter() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        request.setCircleId(UUID.randomUUID().toString());
        request.setMemberId(UUID.randomUUID().toString());
        request.setTrustLevel(TrustLevel.WRITE);
        request.setAction(Action.ALTER);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionAlterFail() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        request.setCircleId("Invalid Circle Id");
        request.setMemberId(null);
        request.setTrustLevel(null);
        request.setAction(Action.ALTER);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(3));
        assertThat(errors.get(Constants.FIELD_CIRCLE_ID), is("Cannot alter a Trustees TrustLevel, without knowing the Circle Id."));
        assertThat(errors.get(Constants.FIELD_MEMBER_ID), is("Cannot alter a Trustees TrustLevel, without knowing the Member Id."));
        assertThat(errors.get(Constants.FIELD_TRUSTLEVEL), is("Cannot alter a Trustees TrustLevel, without knowing the new TrustLevel."));
    }

    @Test
    public void testActionRemove() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        request.setCircleId(UUID.randomUUID().toString());
        request.setMemberId(UUID.randomUUID().toString());
        request.setAction(Action.REMOVE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionRemoveFail() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        request.setCircleId(null);
        request.setMemberId("invalid Member Id");
        request.setAction(Action.REMOVE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(2));
        assertThat(errors.get(Constants.FIELD_CIRCLE_ID), is("Cannot remove a Trustee from a Circle, without knowing the Circle Id."));
        assertThat(errors.get(Constants.FIELD_MEMBER_ID), is("Cannot remove a Trustee from a Circle, without knowing the Member Id."));
    }
}
