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
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.CREATE);
        request.setCircleId(circleId);
        request.setCircleName(circleName);
        request.setMemberId(memberId);
        final Map<String, String> errors = request.validate();

        assertThat(errors.isEmpty(), is(true));
        assertThat(request.getAccountName(), is(Constants.ADMIN_ACCOUNT));
        assertThat(TestUtilities.convert(request.getCredential()), is(Constants.ADMIN_ACCOUNT));
        assertThat(request.getAction(), is(Action.CREATE));
        assertThat(request.getCircleId(), is(circleId));
        assertThat(request.getCircleName(), is(circleName));
        assertThat(request.getMemberId(), is(memberId));
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
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.PROCESS);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_ACTION), is("Not supported Action has been provided."));
    }

    @Test
    public void testActionCreate() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
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
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleName(null);
        request.setMemberId("Invalid Member Id");
        request.setAction(Action.CREATE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_CIRCLE_NAME), is("Cannot create a new Circle, without the Circle Name."));
    }

    @Test
    public void testActionUpdate() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
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
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
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
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId(UUID.randomUUID().toString());
        request.setAction(Action.DELETE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionDeleteFail() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId(null);
        request.setAction(Action.DELETE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_CIRCLE_ID), is("Cannot delete a Circle, without knowing the Circle Id."));
    }
}
