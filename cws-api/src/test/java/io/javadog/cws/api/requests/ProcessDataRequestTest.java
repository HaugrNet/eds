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
public final class ProcessDataRequestTest {

    @Test
    public void testClassflow() {
        final String dataId = UUID.randomUUID().toString();
        final String circleId = UUID.randomUUID().toString();
        final String targetCircleId = UUID.randomUUID().toString();
        final String dataName = "Data Name";
        final String folderId = UUID.randomUUID().toString();
        final String targetFolderId = UUID.randomUUID().toString();
        final String typeName = "The Type";
        final byte[] data = { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5 };

        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.ADD);
        request.setDataId(dataId);
        request.setCircleId(circleId);
        request.setTargetCircleId(targetCircleId);
        request.setDataName(dataName);
        request.setFolderId(folderId);
        request.setTargetFolderId(targetFolderId);
        request.setTypeName(typeName);
        request.setData(data);
        final Map<String, String> errors = request.validate();

        assertThat(errors.isEmpty(), is(true));
        assertThat(request.getAccountName(), is(Constants.ADMIN_ACCOUNT));
        assertThat(TestUtilities.convert(request.getCredential()), is(Constants.ADMIN_ACCOUNT));
        assertThat(request.getAction(), is(Action.ADD));
        assertThat(request.getDataId(), is(dataId));
        assertThat(request.getCircleId(), is(circleId));
        assertThat(request.getTargetCircleId(), is(targetCircleId));
        assertThat(request.getDataName(), is(dataName));
        assertThat(request.getFolderId(), is(folderId));
        assertThat(request.getTargetFolderId(), is(targetFolderId));
        assertThat(request.getTypeName(), is(typeName));
        assertThat(request.getData(), is(data));
    }

    @Test
    public void testEmptyClass() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAction(null);
        final Map<String, String> errors = request.validate();

        assertThat(errors.size(), is(3));
        assertThat(errors.get(Constants.FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(Constants.FIELD_CREDENTIAL), is("The Credential is missing."));
        assertThat(errors.get(Constants.FIELD_ACTION), is("No action has been provided."));
    }

    @Test
    public void testInvalidAction() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setAction(Action.PROCESS);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_ACTION), is("Not supported Action has been provided."));
    }

    @Test
    public void testActionAdd() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId(UUID.randomUUID().toString());
        request.setFolderId(UUID.randomUUID().toString());
        request.setDataName("New Data");
        request.setTypeName("Data Type name");
        request.setAction(Action.ADD);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionAddFail() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setCircleId(null);
        request.setFolderId("Invalid folder Id");
        request.setAction(Action.ADD);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(3));
        assertThat(errors.get(Constants.FIELD_CIRCLE_ID), is("The Circle Id is missing or invalid."));
        assertThat(errors.get(Constants.FIELD_FOLDER_ID), is("The Folder Id is invalid."));
    }

    @Test
    public void testActionUpdate() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setDataId(UUID.randomUUID().toString());
        request.setFolderId(UUID.randomUUID().toString());
        request.setDataName("Updated Data Name");
        request.setAction(Action.UPDATE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionUpdateFail() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setDataId(null);
        request.setFolderId("Invalid Folder Id");
        request.setDataName("Too long new name for the Data, it is only allowed to be under 75 characters long, and this should hopefully exceed that.");
        request.setAction(Action.UPDATE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(3));
        assertThat(errors.get(Constants.FIELD_DATA_ID), is("The Data Id to update is missing or invalid."));
        assertThat(errors.get(Constants.FIELD_FOLDER_ID), is("The Folder Id is invalid."));
        assertThat(errors.get(Constants.FIELD_DATA_NAME), is("The new name of the Data Object is invalid."));
    }

    @Test
    public void testActionCopy() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setDataId(UUID.randomUUID().toString());
        request.setTargetCircleId(UUID.randomUUID().toString());
        request.setTargetFolderId(UUID.randomUUID().toString());
        request.setAction(Action.COPY);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionCopyFail() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setDataId(null);
        request.setAction(Action.COPY);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(2));
        assertThat(errors.get(Constants.FIELD_DATA_ID), is("The Data Id to copy is missing or invalid."));
        assertThat(errors.get(Constants.FIELD_TARGET_CIRCLE_ID), is("The target Circle Id is missing or invalid."));
    }

    @Test
    public void testActionMove() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setDataId(UUID.randomUUID().toString());
        request.setTargetCircleId(UUID.randomUUID().toString());
        request.setTargetFolderId(UUID.randomUUID().toString());
        request.setAction(Action.MOVE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionMoveFail() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setDataId(null);
        request.setAction(Action.MOVE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(2));
        assertThat(errors.get(Constants.FIELD_DATA_ID), is("The Data Id to move is missing or invalid."));
        assertThat(errors.get(Constants.FIELD_TARGET_CIRCLE_ID), is("The target Circle Id is missing or invalid."));
    }

    @Test
    public void testActionDelete() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setDataId(UUID.randomUUID().toString());
        request.setAction(Action.DELETE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testActionDeleteFail() {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredential(TestUtilities.convert(Constants.ADMIN_ACCOUNT));
        request.setDataId(null);
        request.setAction(Action.DELETE);

        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(Constants.FIELD_DATA_ID), is("The Data Id to delete is missing or invalid."));
    }
}
