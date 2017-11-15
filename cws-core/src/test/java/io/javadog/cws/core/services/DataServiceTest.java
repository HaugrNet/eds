/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.common.exceptions.VerificationException;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

import java.util.UUID;

/**
 * <p>Common test class for the Process & Fetch Data Services.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class DataServiceTest extends DatabaseSetup {

    @Test
    public void testEmptyProcessRequest() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors:" +
                        "\nKey: credential, Error: The Credential is missing." +
                        "\nKey: accountName, Error: AccountName is missing, null or invalid." +
                        "\nKey: action, Error: No action has been provided.");

        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = new ProcessDataRequest();
        assertThat(request.getAccountName(), is(nullValue()));

        service.perform(request);
    }

    @Test
    public void testEmptyFetchRequest() {
        prepareCause(VerificationException.class, ReturnCode.VERIFICATION_WARNING,
                "Request Object contained errors:" +
                        "\nKey: credential, Error: The Credential is missing." +
                        "\nKey: circle & data Id, Error: Either a Circle or Data Id must be provided." +
                        "\nKey: accountName, Error: AccountName is missing, null or invalid.");

        final FetchDataService service = new FetchDataService(settings, entityManager);
        final FetchDataRequest request = new FetchDataRequest();
        assertThat(request.getAccountName(), is(nullValue()));

        service.perform(request);
    }

    @Test
    public void testSavingAndReadingData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final FetchDataService dataService = new FetchDataService(settings, entityManager);

        // 1 MB large Data
        final ProcessDataRequest saveRequest = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "My Data", 1048576);
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertThat(saveResponse.getReturnCode(), is(ReturnCode.SUCCESS));

        final FetchDataRequest fetchRequest = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest.setDataId(saveResponse.getDataId());
        final FetchDataResponse fetchResponse = dataService.perform(fetchRequest);
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS));
    }

    @Test
    public void testSaveAndUpdateData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final FetchDataService dataService = new FetchDataService(settings, entityManager);

        // 1 MB large Data
        final ProcessDataRequest saveRequest = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "My Data", 1048576);
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertThat(saveResponse.getReturnCode(), is(ReturnCode.SUCCESS));

        final FetchDataRequest fetchRequest1 = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest1.setDataId(saveResponse.getDataId());
        final FetchDataResponse fetchResponse1 = dataService.perform(fetchRequest1);
        assertThat(fetchResponse1.getReturnCode(), is(ReturnCode.SUCCESS));

        final ProcessDataRequest updateRequest = prepareUpdateRequest(MEMBER_1, saveResponse.getDataId());
        updateRequest.setDataName("New Name");
        updateRequest.setData(generateData(1048576));
        final ProcessDataResponse updateResponse = service.perform(updateRequest);
        assertThat(updateResponse.getReturnCode(), is(ReturnCode.SUCCESS));

        assertThat(saveResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        final FetchDataRequest fetchRequest2 = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest2.setDataId(saveResponse.getDataId());
        final FetchDataResponse fetchResponse2 = dataService.perform(fetchRequest2);
        assertThat(fetchResponse2.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(fetchResponse1.getMetadata().get(0).getDataId(), is(fetchResponse2.getMetadata().get(0).getDataId()));
        assertThat(fetchResponse1.getMetadata().get(0).getDataName(), is("My Data"));
        assertThat(fetchResponse2.getMetadata().get(0).getDataName(), is("New Name"));
    }

    @Test
    public void testAddEmptyData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "The Data", 0);

        final ProcessDataResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testAddEmptyAndUpdateData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest saveRequest = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "The Data", 0);

        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertThat(saveResponse.getReturnCode(), is(ReturnCode.SUCCESS));

        final ProcessDataRequest updateRequest = prepareUpdateRequest(MEMBER_1, saveResponse.getDataId());
        updateRequest.setData(generateData(1048576));
        service.perform(updateRequest);
    }

    @Test
    public void testAddDataWithoutPermission() {
        prepareCause(CWSException.class, ReturnCode.AUTHORIZATION_WARNING, "The requesting Account is not permitted to Process Data.");

        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareAddRequest(MEMBER_5, CIRCLE_3_ID, "The Data", 1048576);
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testUpdatingDataWithoutPermission() {
        prepareCause(CWSException.class, ReturnCode.AUTHORIZATION_WARNING, "The current Account is not allowed to perform the given action.");

        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest saveRequest = prepareAddRequest(MEMBER_2, CIRCLE_2_ID, "The Data", 1048576);
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertThat(saveResponse.isOk(), is(true));

        final ProcessDataRequest updateRequest = prepareUpdateRequest(MEMBER_3, saveResponse.getDataId());
        updateRequest.setDataName("New Name");
        service.perform(updateRequest);
    }

    @Test
    public void testDeletingDataWithoutPermission() {
        prepareCause(CWSException.class, ReturnCode.AUTHORIZATION_WARNING, "The current Account is not allowed to perform the given action.");

        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest saveRequest = prepareAddRequest(MEMBER_2, CIRCLE_2_ID, "The Data", 1048576);
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertThat(saveResponse.isOk(), is(true));

        final ProcessDataRequest updateRequest = prepareDeleteRequest(MEMBER_3, saveResponse.getDataId());
        service.perform(updateRequest);
    }

    @Test
    public void testFetchingInvalidDataId() {
        final FetchDataService service = new FetchDataService(settings, entityManager);
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, MEMBER_4);
        request.setCircleId(CIRCLE_3_ID);
        request.setDataId(UUID.randomUUID().toString());

        final FetchDataResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is("No information could be found for the given Id."));
    }

    @Test
    public void testAddDataToUnknownFolder() {
        prepareCause(CWSException.class, ReturnCode.IDENTIFICATION_WARNING, "Provided FolderId is not a folder.");

        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareAddRequest(MEMBER_4, CIRCLE_3_ID, "Data File", 512);
        request.setFolderId(UUID.randomUUID().toString());
        assertThat(request.validate().isEmpty(), is(true));

        service.perform(request);
    }

    @Test
    public void testAddDataToInvalidFolder() {
        prepareCause(CWSException.class, ReturnCode.IDENTIFICATION_WARNING, "Provided FolderId is not a folder.");

        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareAddRequest(MEMBER_4, CIRCLE_3_ID, "Data File", 512);
        assertThat(request.validate().isEmpty(), is(true));
        final ProcessDataResponse response = service.perform(request);
        assertThat(response.isOk(), is(true));

        // We're taking the previously generated Data Id and uses that as folder.
        request.setFolderId(response.getDataId());
        service.perform(request);
    }

    @Test
    public void testUpdateNotExistingData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, MEMBER_4);
        request.setAction(Action.UPDATE);
        request.setCircleId(CIRCLE_2_ID);
        request.setDataId(UUID.randomUUID().toString());
        request.setDataName("New Name for our not existing Data");
        request.setData(generateData(512));

        final ProcessDataResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is("The requested Data Object could not be found."));
    }

    @Test
    public void testSaveAndDeleteData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest saveRequest = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "The Data", 524288);
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertThat(saveResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(saveResponse.getReturnMessage(), is("Ok"));

        final ProcessDataRequest deleteRequest = prepareDeleteRequest(MEMBER_1, saveResponse.getDataId());
        final ProcessDataResponse deleteResponse = service.perform(deleteRequest);
        assertThat(deleteResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(deleteResponse.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testSaveAndDeleteDataWithoutPermission() {
        prepareCause(CWSException.class, ReturnCode.AUTHORIZATION_WARNING, "The current Account is not allowed to perform the given action.");

        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest saveRequest = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "Known Data", 524288);
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertThat(saveResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(saveResponse.getReturnMessage(), is("Ok"));

        final ProcessDataRequest deleteRequest = prepareDeleteRequest(MEMBER_3, saveResponse.getDataId());
        service.perform(deleteRequest);
    }

    @Test
    public void testSaveAndDeleteDataWithoutAccess() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest saveRequest = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "More Data", 524288);
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertThat(saveResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(saveResponse.getReturnMessage(), is("Ok"));

        final ProcessDataRequest deleteRequest = prepareDeleteRequest(MEMBER_4, saveResponse.getDataId());
        final ProcessDataResponse deleteResponse = service.perform(deleteRequest);
        assertThat(deleteResponse.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
        assertThat(deleteResponse.getReturnMessage(), is("The requested Data Object could not be found."));
    }

    @Test
    public void testDeleteNotExistingData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareDeleteRequest(MEMBER_1, UUID.randomUUID().toString());
        final ProcessDataResponse response = service.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is("The requested Data Object could not be found."));
    }

    @Test
    public void testAddUpdateAndDeleteFolder() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "folder1", 0);
        request.setTypeName("folder");
        final ProcessDataResponse addResponse = service.perform(request);
        assertThat(addResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(addResponse.getReturnMessage(), is("Ok"));
        assertThat(addResponse.getDataId(), is(not(nullValue())));

        final ProcessDataRequest updateRequest = prepareUpdateRequest(MEMBER_1, addResponse.getDataId());
        updateRequest.setDataName("updated Folder Name");
        final ProcessDataResponse updateResponse = service.perform(updateRequest);
        assertThat(updateResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(updateResponse.getReturnMessage(), is("Ok"));
        assertThat(updateResponse.getDataId(), is(addResponse.getDataId()));

        final ProcessDataRequest deleteRequest = prepareDeleteRequest(MEMBER_1, addResponse.getDataId());
        final ProcessDataResponse deleteResponse = service.perform(deleteRequest);
        assertThat(deleteResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(deleteResponse.getReturnMessage(), is("Ok"));
        assertThat(deleteResponse.getDataId(), is(nullValue()));
    }

    @Test
    public void testAddSameFolder() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "folder1", 0);
        request.setTypeName("folder");
        final ProcessDataResponse response1 = service.perform(request);
        assertThat(response1.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response1.getReturnMessage(), is("Ok"));
        assertThat(response1.getDataId(), is(not(nullValue())));

        final ProcessDataResponse response2 = service.perform(request);
        assertThat(response2.getReturnCode(), is(ReturnCode.INTEGRITY_WARNING));
        assertThat(response2.getReturnMessage(), is("Another record with the same name already exists."));
        assertThat(response2.getDataId(), is(nullValue()));
    }

    @Test
    public void testDeleteFolderWithData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest addFolderRequest = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "folder1", 0);
        addFolderRequest.setTypeName("folder");
        final ProcessDataResponse addFolderResponse = service.perform(addFolderRequest);
        assertThat(addFolderResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(addFolderResponse.getReturnMessage(), is("Ok"));
        assertThat(addFolderResponse.getDataId(), is(not(nullValue())));

        final ProcessDataRequest addDataRequest = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "More Data", 524288);
        addDataRequest.setFolderId(addFolderResponse.getDataId());
        final ProcessDataResponse addDataResponse = service.perform(addDataRequest);
        assertThat(addDataResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(addDataResponse.getReturnMessage(), is("Ok"));

        final ProcessDataRequest deleteFolderRequest = prepareDeleteRequest(MEMBER_1, addFolderResponse.getDataId());
        final ProcessDataResponse deleteFolderResponse = service.perform(deleteFolderRequest);
        assertThat(deleteFolderResponse.getReturnCode(), is(ReturnCode.INTEGRITY_WARNING));
        assertThat(deleteFolderResponse.getReturnMessage(), is("The requested Folder cannot be removed as it is not empty."));
        assertThat(deleteFolderResponse.getDataId(), is(nullValue()));
    }

    @Test
    public void testMoveFolder() {
        prepareCause(CWSException.class, ReturnCode.ILLEGAL_ACTION, "It is not permitted to move Folders.");

        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest addFolderRequest = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "folder1", 0);
        addFolderRequest.setTypeName("folder");
        final ProcessDataResponse addFolderResponse1 = service.perform(addFolderRequest);
        assertThat(addFolderResponse1.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(addFolderResponse1.getReturnMessage(), is("Ok"));
        assertThat(addFolderResponse1.getDataId(), is(not(nullValue())));
        final String folderId1 = addFolderResponse1.getDataId();

        addFolderRequest.setDataName("folder2");
        final ProcessDataResponse addFolderResponse2 = service.perform(addFolderRequest);
        assertThat(addFolderResponse2.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(addFolderResponse2.getReturnMessage(), is("Ok"));
        assertThat(addFolderResponse2.getDataId(), is(not(nullValue())));
        final String folderId2 = addFolderResponse2.getDataId();

        final ProcessDataRequest moveFolderRequest = prepareUpdateRequest(MEMBER_1, folderId2);
        moveFolderRequest.setFolderId(folderId1);
        service.perform(moveFolderRequest);
    }

    @Test
    public void testMoveData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest addFolderRequest = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "folder1", 0);
        addFolderRequest.setTypeName("folder");
        final ProcessDataResponse addFolderResponse = service.perform(addFolderRequest);
        assertThat(addFolderResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(addFolderResponse.getReturnMessage(), is("Ok"));
        assertThat(addFolderResponse.getDataId(), is(not(nullValue())));

        final ProcessDataRequest addDataRequest = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "my data", 512);
        final ProcessDataResponse addDataResponse = service.perform(addDataRequest);
        assertThat(addDataResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(addDataResponse.getReturnMessage(), is("Ok"));
        assertThat(addDataResponse.getDataId(), is(not(nullValue())));

        final ProcessDataRequest moveDataRequest = prepareUpdateRequest(MEMBER_1, addDataResponse.getDataId());
        moveDataRequest.setFolderId(addFolderResponse.getDataId());
        final ProcessDataResponse moveFolderResponse = service.perform(moveDataRequest);
        assertThat(moveFolderResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(moveFolderResponse.getReturnMessage(), is("Ok"));
        assertThat(moveFolderResponse.getDataId(), is(addDataResponse.getDataId()));
    }

    @Test
    public void testMovingDataToFolderWhereSameNameDataExist() {
        prepareCause(CWSException.class, ReturnCode.IDENTIFICATION_WARNING, "The name provided is already being used in the given folder.");
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final FetchDataService readSearvice = new FetchDataService(settings, entityManager);

        // Step 1, create 2 folders
        final ProcessDataRequest folderRequest1 = prepareAddRequest(MEMBER_4, CIRCLE_3_ID, "First Folder", 0);
        folderRequest1.setTypeName(Constants.FOLDER_TYPENAME);
        final ProcessDataResponse folderResponse1 = service.perform(folderRequest1);
        assertThat(folderResponse1.isOk(), is(true));
        final ProcessDataRequest folderRequest2 = prepareAddRequest(MEMBER_4, CIRCLE_3_ID, "Second Folder", 0);
        folderRequest2.setTypeName(Constants.FOLDER_TYPENAME);
        final ProcessDataResponse folderResponse2 = service.perform(folderRequest2);
        assertThat(folderResponse2.isOk(), is(true));

        // Step 2, Create 1 data record for each folder, with the same name
        final ProcessDataRequest dataRequest1 = prepareAddRequest(MEMBER_4, CIRCLE_3_ID, "The Data", 524288);
        dataRequest1.setFolderId(folderResponse1.getDataId());
        final ProcessDataResponse dataResponse1 = service.perform(dataRequest1);
        assertThat(dataResponse1.isOk(), is(true));
        final ProcessDataRequest dataRequest2 = prepareAddRequest(MEMBER_4, CIRCLE_3_ID, "The Data", 524288);
        dataRequest2.setFolderId(folderResponse2.getDataId());
        final ProcessDataResponse dataResponse2 = service.perform(dataRequest2);
        assertThat(dataResponse2.isOk(), is(true));

        // Step 3, Verify that we have the correct data structure.
        final FetchDataRequest readRootRequest = prepareReadRequest(MEMBER_4, CIRCLE_3_ID, null);
        final FetchDataResponse readRootResponse = readSearvice.perform(readRootRequest);
        assertThat(readRootResponse.isOk(), is(true));
        assertThat(readRootResponse.getMetadata().size(), is(2));
        assertThat(readRootResponse.getMetadata().get(0).getDataId(), is(folderResponse2.getDataId()));
        assertThat(readRootResponse.getMetadata().get(1).getDataId(), is(folderResponse1.getDataId()));

        final FetchDataRequest readFolder1Request = prepareReadRequest(MEMBER_4, CIRCLE_3_ID, folderResponse1.getDataId());
        final FetchDataResponse readFolder1Response = readSearvice.perform(readFolder1Request);
        assertThat(readFolder1Response.isOk(), is(true));
        assertThat(readFolder1Response.getMetadata().size(), is(1));
        assertThat(readFolder1Response.getMetadata().get(0).getDataId(), is(dataResponse1.getDataId()));

        final FetchDataRequest readFolder2Request = prepareReadRequest(MEMBER_4, CIRCLE_3_ID, folderResponse2.getDataId());
        final FetchDataResponse readFolder2Response = readSearvice.perform(readFolder2Request);
        assertThat(readFolder2Response.isOk(), is(true));
        assertThat(readFolder2Response.getMetadata().size(), is(1));
        assertThat(readFolder2Response.getMetadata().get(0).getDataId(), is(dataResponse2.getDataId()));

        // Step 4, Move the data from folder 1 to folder 2.
        final ProcessDataRequest moveRequest = prepareUpdateRequest(MEMBER_4, CIRCLE_3_ID);
        moveRequest.setDataId(dataResponse1.getDataId());
        moveRequest.setFolderId(folderResponse2.getDataId());
        service.perform(moveRequest);
    }

    @Test
    public void testMoveDataToDifferentCircle() {
        prepareCause(CWSException.class, ReturnCode.ILLEGAL_ACTION, "Moving Data from one Circle to another is not permitted.");

        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest addFolderRequest = prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "folder1", 0);
        addFolderRequest.setTypeName("folder");
        final ProcessDataResponse addFolderResponse = service.perform(addFolderRequest);
        assertThat(addFolderResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(addFolderResponse.getReturnMessage(), is("Ok"));
        assertThat(addFolderResponse.getDataId(), is(not(nullValue())));

        final ProcessDataRequest addDataRequest = prepareAddRequest(MEMBER_1, CIRCLE_2_ID, "my data", 512);
        final ProcessDataResponse addDataResponse = service.perform(addDataRequest);
        assertThat(addDataResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(addDataResponse.getReturnMessage(), is("Ok"));
        assertThat(addDataResponse.getDataId(), is(not(nullValue())));

        final ProcessDataRequest moveDataRequest = prepareUpdateRequest(MEMBER_1, addDataResponse.getDataId());
        moveDataRequest.setFolderId(addFolderResponse.getDataId());
        service.perform(moveDataRequest);
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private static ProcessDataRequest prepareAddRequest(final String account, final String circleId, final String dataName, final int bytes) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, account);
        request.setAction(Action.ADD);
        request.setCircleId(circleId);
        request.setDataName(dataName);
        request.setTypeName(Constants.DATA_TYPENAME);
        request.setData(generateData(bytes));

        return request;
    }

    private static FetchDataRequest prepareReadRequest(final String account, final String circleId, final String dataId) {
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, account);
        request.setCircleId(circleId);
        request.setDataId(dataId);

        return request;
    }

    private static ProcessDataRequest prepareUpdateRequest(final String account, final String dataId) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, account);
        request.setAction(Action.UPDATE);
        request.setDataId(dataId);

        return request;
    }

    private static ProcessDataRequest prepareDeleteRequest(final String account, final String dataId) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, account);
        request.setAction(Action.DELETE);
        request.setDataId(dataId);

        return request;
    }
}
