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
package net.haugr.cws.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.cws.api.common.Action;
import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.common.ReturnCode;
import net.haugr.cws.api.common.Utilities;
import net.haugr.cws.api.dtos.Metadata;
import net.haugr.cws.api.requests.FetchDataRequest;
import net.haugr.cws.api.requests.ProcessDataRequest;
import net.haugr.cws.api.responses.FetchDataResponse;
import net.haugr.cws.api.responses.ProcessDataResponse;
import net.haugr.cws.core.enums.SanityStatus;
import net.haugr.cws.core.setup.DatabaseSetup;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * <p>Common test class for the Process & Fetch Data Services.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class ShareBeanDataTest extends DatabaseSetup {

    @Test
    void testEmptyProcessRequest() {
        final ShareBean bean = prepareShareBean();
        final ProcessDataRequest request = new ProcessDataRequest();

        final ProcessDataResponse response = bean.processData(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing." +
                "\nKey: action, Error: No action has been provided.", response.getReturnMessage());
    }

    @Test
    void testEmptyFetchRequest() {
        final ShareBean bean = prepareShareBean();
        final FetchDataRequest request = new FetchDataRequest();

        final FetchDataResponse response = bean.fetchData(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing." +
                "\nKey: circle & data Id, Error: Either a Circle Id, Data Id, or Data Name must be provided.", response.getReturnMessage());
    }

    @Test
    void testSavingAndReadingData() {
        final ShareBean bean = prepareShareBean();

        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "My Data", LARGE_SIZE_BYTES);
        // Since the logic is deliberately clearing data post encryption, and
        // it means that the bytes from the request will be overwritten by
        // zeroes. To ensure that the check works, a String representing the
        // byte array is stored prior.
        final String toSave = crypto.bytesToString(saveRequest.getData());
        final ProcessDataResponse saveResponse = bean.processData(saveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());

        final FetchDataRequest fetchRequest = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest.setDataId(saveResponse.getDataId());
        final FetchDataResponse fetchResponse = bean.fetchData(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals(toSave, crypto.bytesToString(fetchResponse.getData()));
    }

    @Test
    void testSavingAndReadingDataByName() {
        final ShareBean bean = prepareShareBean();
        final String name = "My Data";

        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, name, LARGE_SIZE_BYTES);
        final String toSave = crypto.bytesToString(saveRequest.getData());
        final ProcessDataResponse saveResponse = bean.processData(saveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());

        final FetchDataRequest fetchRequest = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest.setDataName(name);
        final FetchDataResponse fetchResponse = bean.fetchData(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals(toSave, crypto.bytesToString(fetchResponse.getData()));
    }

    @Test
    void testSaveAndUpdateData() {
        final ShareBean bean = prepareShareBean();

        // 1 MB large Data
        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "My Data", LARGE_SIZE_BYTES);
        final ProcessDataResponse saveResponse = bean.processData(saveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());

        final FetchDataRequest fetchRequest1 = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest1.setDataId(saveResponse.getDataId());
        final FetchDataResponse fetchResponse1 = bean.fetchData(fetchRequest1);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse1.getReturnCode());

        final ProcessDataRequest updateRequest = prepareUpdateRequest(MEMBER_1, saveResponse.getDataId());
        updateRequest.setDataName("New Name");
        updateRequest.setData(generateData(LARGE_SIZE_BYTES));
        final ProcessDataResponse updateResponse = bean.processData(updateRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), updateResponse.getReturnCode());

        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());
        final FetchDataRequest fetchRequest2 = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest2.setDataId(saveResponse.getDataId());
        final FetchDataResponse fetchResponse2 = bean.fetchData(fetchRequest2);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse2.getReturnCode());
        assertEquals(fetchResponse2.getMetadata().get(0).getDataId(), fetchResponse1.getMetadata().get(0).getDataId());
        assertEquals("My Data", fetchResponse1.getMetadata().get(0).getDataName());
        assertEquals("New Name", fetchResponse2.getMetadata().get(0).getDataName());
    }

    @Test
    void testSaveAndUpdateSimpleData() {
        final ShareBean bean = prepareShareBean();
        final String dataName = "status";
        final String initContent = "NEW";
        final String updateContent = "ACCEPTED";

        final ProcessDataRequest saveRequest = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        saveRequest.setAction(Action.ADD);
        saveRequest.setCircleId(CIRCLE_1_ID);
        saveRequest.setDataName(dataName);
        saveRequest.setData(initContent.getBytes(StandardCharsets.UTF_8));
        final ProcessDataResponse saveResponse = bean.processData(saveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());

        final ProcessDataRequest updateRequest = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        updateRequest.setAction(Action.UPDATE);
        updateRequest.setCircleId(CIRCLE_1_ID);
        updateRequest.setDataId(saveResponse.getDataId());
        updateRequest.setDataName(dataName);
        updateRequest.setData(updateContent.getBytes(StandardCharsets.UTF_8));
        final ProcessDataResponse updateResponse = bean.processData(updateRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), updateResponse.getReturnCode());

        final FetchDataRequest fetchRequest = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest.setDataId(saveResponse.getDataId());
        final FetchDataResponse fetchResponse = bean.fetchData(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals(dataName, fetchResponse.getMetadata().get(0).getDataName());
        assertEquals(updateContent, new String(fetchResponse.getData(), StandardCharsets.UTF_8));
    }

    @Test
    void testAddingAndFetchingData() {
        final ShareBean bean = prepareShareBean();

        final FetchDataRequest fetchRequest = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest.setCircleId(CIRCLE_1_ID);
        final FetchDataResponse emptyResponse = bean.fetchData(fetchRequest);
        assertTrue(emptyResponse.isOk());
        assertEquals(0L, emptyResponse.getRecords());

        // Add some data...
        assertTrue(bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "My Data 1", LARGE_SIZE_BYTES)).isOk());
        assertTrue(bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "My Data 2", LARGE_SIZE_BYTES)).isOk());
        assertTrue(bean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "My Data 3", LARGE_SIZE_BYTES)).isOk());

        fetchRequest.setCredential(crypto.stringToBytes(MEMBER_1));
        final FetchDataResponse fullResponse = bean.fetchData(fetchRequest);
        assertTrue(fullResponse.isOk());
        assertEquals(3L, fullResponse.getRecords());
    }

    @Test
    void testAddEmptyData() {
        final String dataName = "The Data";
        final ShareBean bean = prepareShareBean();
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, dataName, 0);

        final ProcessDataResponse response = bean.processData(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Data Object '" + dataName + "' was successfully added to the Circle '" + CIRCLE_1 + "'.", response.getReturnMessage());

        final FetchDataRequest readRequest = prepareReadRequest(MEMBER_1, CIRCLE_1_ID, response.getDataId());
        final FetchDataResponse readResponse = bean.fetchData(readRequest);
        assertTrue(readResponse.isOk());
    }

    @Test
    void testAddDataWithInvalidChecksum() {
        final ShareBean bean = prepareShareBean();
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "The Data", 524288);

        final ProcessDataResponse response = bean.processData(request);
        assertTrue(response.isOk());
        falsifyChecksum(response, Utilities.newDate(), SanityStatus.FAILED);

        // Now to the actual test - reading the data with invalid checksum
        final FetchDataRequest readRequest = prepareReadRequest(MEMBER_1, CIRCLE_1_ID, response.getDataId());
        final FetchDataResponse readResponse = bean.fetchData(readRequest);
        assertEquals(ReturnCode.INTEGRITY_ERROR.getCode(), readResponse.getReturnCode());
        assertEquals("The Encrypted Data Checksum is invalid, the data appears to have been corrupted.", readResponse.getReturnMessage());
    }

    @Test
    void testAddEmptyAndUpdateData() {
        final ShareBean bean = prepareShareBean();
        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "The Data", 0);

        final ProcessDataResponse saveResponse = bean.processData(saveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());

        final ProcessDataRequest updateRequest = prepareUpdateRequest(MEMBER_1, saveResponse.getDataId());
        updateRequest.setData(generateData(LARGE_SIZE_BYTES));
        final ProcessDataResponse updateResponse = bean.processData(updateRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), updateResponse.getReturnCode());
    }

    @Test
    void testAddAndMoveToInvalidFolder() {
        final ShareBean bean = prepareShareBean();
        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "The Data", LARGE_SIZE_BYTES);

        final ProcessDataResponse saveResponse = bean.processData(saveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());

        final ProcessDataRequest updateRequest = prepareUpdateRequest(MEMBER_1, saveResponse.getDataId());
        updateRequest.setFolderId(UUID.randomUUID().toString());

        final ProcessDataResponse response = bean.processData(updateRequest);
        assertEquals(ReturnCode.INTEGRITY_WARNING.getCode(), response.getReturnCode());
        assertEquals("No existing Folder could be found.", response.getReturnMessage());
    }

    @Test
    void testAddDataWithoutPermission() {
        final ShareBean bean = prepareShareBean();
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_5, CIRCLE_3_ID, "The Data", LARGE_SIZE_BYTES);

        final ProcessDataResponse response = bean.processData(request);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The requesting Account is not permitted to Process Data.", response.getReturnMessage());
    }

    @Test
    void testUpdatingDataWithoutPermission() {
        final ShareBean bean = prepareShareBean();
        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_2, CIRCLE_2_ID, "The Data", LARGE_SIZE_BYTES);
        final ProcessDataResponse saveResponse = bean.processData(saveRequest);
        assertTrue(saveResponse.isOk());

        final ProcessDataRequest updateRequest = prepareUpdateRequest(MEMBER_3, saveResponse.getDataId());
        updateRequest.setDataName("New Name");

        final ProcessDataResponse response = bean.processData(updateRequest);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The current Account is not allowed to perform the given action.", response.getReturnMessage());
    }

    @Test
    void testAddDataWithInvalidDataType() {
        final ShareBean bean = prepareShareBean();
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "The Data", 512);
        request.setTypeName("Weird");

        final ProcessDataResponse response = bean.processData(request);
        assertEquals(ReturnCode.INTEGRITY_WARNING.getCode(), response.getReturnCode());
        assertEquals("Cannot find a matching DataType for the Object.", response.getReturnMessage());
    }

    @Test
    void testDeletingDataWithoutPermission() {
        final ShareBean bean = prepareShareBean();
        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_2, CIRCLE_2_ID, "The Data", LARGE_SIZE_BYTES);
        final ProcessDataResponse saveResponse = bean.processData(saveRequest);

        final ProcessDataRequest updateRequest = prepareDeleteRequest(MEMBER_3, saveResponse.getDataId());
        final ProcessDataResponse response = bean.processData(updateRequest);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The current Account is not allowed to perform the given action.", response.getReturnMessage());
    }

    @Test
    void testFetchingInvalidDataId() {
        final ShareBean bean = prepareShareBean();
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, MEMBER_4);
        request.setCircleId(CIRCLE_3_ID);
        request.setDataId(UUID.randomUUID().toString());

        final FetchDataResponse response = bean.fetchData(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No information could be found for the given Id.", response.getReturnMessage());
    }

    @Test
    void testAddDataToUnknownFolder() {
        final ShareBean bean = prepareShareBean();
        final String folderId = UUID.randomUUID().toString();

        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Data File", 512);
        request.setFolderId(folderId);

        final ProcessDataResponse response = bean.processData(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Provided FolderId '" + folderId + "' is not a folder.", response.getReturnMessage());
    }

    @Test
    void testAddDataToInvalidFolder() {
        final ShareBean bean = prepareShareBean();
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Data File", 512);
        final ProcessDataResponse saveResponse = bean.processData(request);
        assertTrue(saveResponse.isOk());
        final String folderId = saveResponse.getDataId();

        // We're taking the previously generated Data ID and uses that as folder.
        request.setCredential(crypto.stringToBytes(MEMBER_4));
        request.setFolderId(folderId);

        final ProcessDataResponse response = bean.processData(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Provided FolderId '" + folderId + "' is not a folder.", response.getReturnMessage());
    }

    @Test
    void testUpdateNotExistingData() {
        final ShareBean bean = prepareShareBean();
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, MEMBER_4);
        request.setAction(Action.UPDATE);
        request.setCircleId(CIRCLE_2_ID);
        request.setDataId(UUID.randomUUID().toString());
        request.setDataName("New Name for our not existing Data");
        request.setData(generateData(512));

        final ProcessDataResponse response = bean.processData(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The Data Object could not be found.", response.getReturnMessage());
    }

    @Test
    void testSaveAndDeleteData() {
        final ShareBean bean = prepareShareBean();
        final String name = "The Data";

        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, name, 524288);
        final ProcessDataResponse saveResponse = bean.processData(saveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());
        assertEquals("The Data Object '" + name + "' was successfully added to the Circle '" + CIRCLE_1 + "'.", saveResponse.getReturnMessage());

        final ProcessDataRequest deleteRequest = prepareDeleteRequest(MEMBER_1, saveResponse.getDataId());
        final ProcessDataResponse deleteResponse = bean.processData(deleteRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), deleteResponse.getReturnCode());
        assertEquals("The Data Object '" + name + "' has been removed from the Circle '" + CIRCLE_1 + "'.", deleteResponse.getReturnMessage());
    }

    @Test
    void testSaveAndDeleteDataWithoutPermission() {
        final ShareBean bean = prepareShareBean();
        final String name = "Known Data";

        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, name, 524288);
        final ProcessDataResponse saveResponse = bean.processData(saveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());
        assertEquals("The Data Object '" + name + "' was successfully added to the Circle '" + CIRCLE_1 + "'.", saveResponse.getReturnMessage());

        final ProcessDataRequest deleteRequest = prepareDeleteRequest(MEMBER_3, saveResponse.getDataId());
        final ProcessDataResponse response = bean.processData(deleteRequest);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The current Account is not allowed to perform the given action.", response.getReturnMessage());
    }

    @Test
    void testSaveAndDeleteDataWithoutAccess() {
        final ShareBean bean = prepareShareBean();
        final String dataName = "More Data";

        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, dataName, 524288);
        final ProcessDataResponse saveResponse = bean.processData(saveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());
        assertEquals("The Data Object '" + dataName + "' was successfully added to the Circle '" + CIRCLE_1 + "'.", saveResponse.getReturnMessage());

        final ProcessDataRequest deleteRequest = prepareDeleteRequest(MEMBER_4, saveResponse.getDataId());
        final ProcessDataResponse response = bean.processData(deleteRequest);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The Data Object could not be found.", response.getReturnMessage());
    }

    @Test
    void testDeleteNotExistingData() {
        final ShareBean bean = prepareShareBean();
        final ProcessDataRequest request = prepareDeleteRequest(MEMBER_1, UUID.randomUUID().toString());

        final ProcessDataResponse response = bean.processData(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The Data Object could not be found.", response.getReturnMessage());
    }

    @Test
    void testAddUpdateAndDeleteFolder() {
        final ShareBean bean = prepareShareBean();
        final String folderName = "folder";

        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, folderName, 0);
        request.setTypeName("folder");
        final ProcessDataResponse addResponse = bean.processData(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), addResponse.getReturnCode());
        assertEquals("The Folder '" + folderName + "' was successfully added to the Circle '" + CIRCLE_1 + "'.", addResponse.getReturnMessage());
        assertNotNull(addResponse.getDataId());

        final String newFolderName = "updated Folder Name";
        final ProcessDataRequest updateRequest = prepareUpdateRequest(MEMBER_1, addResponse.getDataId());
        updateRequest.setDataName(newFolderName);
        final ProcessDataResponse updateResponse = bean.processData(updateRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), updateResponse.getReturnCode());
        assertEquals("The Data Object '" + newFolderName + "' was successfully updated.", updateResponse.getReturnMessage());
        assertEquals(addResponse.getDataId(), updateResponse.getDataId());

        final ProcessDataRequest deleteRequest = prepareDeleteRequest(MEMBER_1, addResponse.getDataId());
        final ProcessDataResponse deleteResponse = bean.processData(deleteRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), deleteResponse.getReturnCode());
        assertEquals("The Data Object '" + newFolderName + "' has been removed from the Circle '" + CIRCLE_1 + "'.", deleteResponse.getReturnMessage());
        assertNull(deleteResponse.getDataId());
    }

    @Test
    void testAddSameFolder() {
        final ShareBean bean = prepareShareBean();
        final String folderName = "folder1";

        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, folderName, 0);
        request.setTypeName(Constants.FOLDER_TYPENAME);
        final ProcessDataResponse addResponse = bean.processData(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), addResponse.getReturnCode());
        assertEquals("The Folder '" + folderName + "' was successfully added to the Circle '" + CIRCLE_1 + "'.", addResponse.getReturnMessage());
        assertNotNull(addResponse.getDataId());

        request.setCredential(crypto.stringToBytes(MEMBER_1));

        final ProcessDataResponse response = bean.processData(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Another record with the same name already exists.", response.getReturnMessage());
    }

    @Test
    void testCopyData() {
        final ShareBean bean = prepareShareBean();
        final String toCopy = "toCopy";

        final ProcessDataRequest copyAddRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, toCopy, 524288);
        final ProcessDataResponse copyAddResponse = bean.processData(copyAddRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), copyAddResponse.getReturnCode());
        assertEquals("The Data Object '" + toCopy + "' was successfully added to the Circle '" + CIRCLE_1 + "'.", copyAddResponse.getReturnMessage());
        assertNotNull(copyAddResponse.getDataId());

        final ProcessDataRequest copyRequest = prepareCopyDataRequest(MEMBER_1, copyAddResponse.getDataId(), CIRCLE_2_ID, null);
        final ProcessDataResponse copyResponse = bean.processData(copyRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), copyResponse.getReturnCode());
        assertEquals("The Data Object '" + toCopy + "' was successfully copied from '" + CIRCLE_1 + "' to '" + CIRCLE_2 + "'.", copyResponse.getReturnMessage());
        assertNotNull(copyResponse.getDataId());
        assertNotEquals(copyAddResponse.getDataId(), copyResponse.getDataId());

        final FetchDataRequest copyFetchRequest1 = prepareReadRequest(MEMBER_1, null, copyAddResponse.getDataId());
        final FetchDataResponse copyFetchResponse1 = bean.fetchData(copyFetchRequest1);
        assertEquals(ReturnCode.SUCCESS.getCode(), copyFetchResponse1.getReturnCode());
        final Metadata metadata1 = copyFetchResponse1.getMetadata().get(0);

        final FetchDataRequest copyFetchRequest2 = prepareReadRequest(MEMBER_1, null, copyResponse.getDataId());
        final FetchDataResponse copyFetchResponse2 = bean.fetchData(copyFetchRequest2);
        assertEquals(ReturnCode.SUCCESS.getCode(), copyFetchResponse2.getReturnCode());
        final Metadata copyMetadata2 = copyFetchResponse2.getMetadata().get(0);

        // Comparing the first Object with the copied Object, name, type and
        // data must be the same, the ID's not.
        assertEquals(metadata1.getDataName(), copyMetadata2.getDataName());
        assertEquals(metadata1.getTypeName(), copyMetadata2.getTypeName());
        assertNotEquals(metadata1.getDataId(), copyMetadata2.getDataId());
        assertEquals(CIRCLE_1_ID, metadata1.getCircleId());
        assertEquals(CIRCLE_2_ID, copyMetadata2.getCircleId());
        assertArrayEquals(copyFetchResponse1.getData(), copyFetchResponse2.getData());
    }

    @Test
    void testMoveData() {
        final ShareBean bean = prepareShareBean();
        final String toMove = "toMove";

        final ProcessDataRequest moveAddRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, toMove, 524288);
        final ProcessDataResponse moveAddResponse = bean.processData(moveAddRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), moveAddResponse.getReturnCode());
        assertEquals("The Data Object '" + toMove + "' was successfully added to the Circle '" + CIRCLE_1 + "'.", moveAddResponse.getReturnMessage());
        assertNotNull(moveAddResponse.getDataId());

        final ProcessDataRequest moveRequest = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        moveRequest.setAction(Action.MOVE);
        moveRequest.setDataId(moveAddResponse.getDataId());
        moveRequest.setTargetCircleId(CIRCLE_2_ID);

        final ProcessDataResponse moveResponse = bean.processData(moveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), moveResponse.getReturnCode());
        assertEquals("The Data Object '" + toMove + "' was successfully moved from '" + CIRCLE_1 + "' to '" + CIRCLE_2 + "'.", moveResponse.getReturnMessage());
        assertNotNull(moveResponse.getDataId());
        assertNotEquals(moveAddResponse.getDataId(), moveResponse.getDataId());

        final FetchDataRequest moveFetchRequest1 = prepareReadRequest(MEMBER_1, null, moveResponse.getDataId());
        final FetchDataResponse moveFetchResponse1 = bean.fetchData(moveFetchRequest1);
        assertEquals(ReturnCode.SUCCESS.getCode(), moveFetchResponse1.getReturnCode());

        final FetchDataRequest moveFetchRequest2 = prepareReadRequest(MEMBER_1, null, moveAddResponse.getDataId());
        final FetchDataResponse response = bean.fetchData(moveFetchRequest2);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No information could be found for the given Id.", response.getReturnMessage());
    }

    @Test
    void testCopyFolder() {
        final ShareBean bean = prepareShareBean();
        final String folderName = "folderToCopy";

        final ProcessDataRequest addRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, folderName, 0);
        addRequest.setTypeName(Constants.FOLDER_TYPENAME);
        final ProcessDataResponse addResponse = bean.processData(addRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addResponse.getReturnCode());
        assertEquals("The Folder '" + folderName + "' was successfully added to the Circle '" + CIRCLE_1 + "'.", addResponse.getReturnMessage());
        assertNotNull(addResponse.getDataId());

        final ProcessDataRequest copyRequest = prepareCopyDataRequest(MEMBER_1, addResponse.getDataId(), CIRCLE_2_ID, null);
        final ProcessDataResponse response = bean.processData(copyRequest);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), response.getReturnCode());
        assertEquals("It is not permitted to copy or move folders.", response.getReturnMessage());
    }

    @Test
    void testCopyEmptyData() {
        final ShareBean bean = prepareShareBean();
        final String dataName = "emptyData";

        final ProcessDataRequest emptyAddRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, dataName, 0);
        final ProcessDataResponse emptyAddResponse = bean.processData(emptyAddRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), emptyAddResponse.getReturnCode());
        assertEquals("The Data Object '" + dataName + "' was successfully added to the Circle '" + CIRCLE_1 + "'.", emptyAddResponse.getReturnMessage());
        assertNotNull(emptyAddResponse.getDataId());

        final ProcessDataRequest emptyCopyRequest = prepareCopyDataRequest(MEMBER_1, emptyAddResponse.getDataId(), CIRCLE_2_ID, null);
        final ProcessDataResponse emptyCopyResponse = bean.processData(emptyCopyRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), emptyCopyResponse.getReturnCode());
        assertEquals("The Data Object '" + dataName + "' was successfully copied from '" + CIRCLE_1 + "' to '" + CIRCLE_2 + "'.", emptyCopyResponse.getReturnMessage());
        assertNotNull(emptyCopyResponse.getDataId());
        assertNotEquals(emptyAddResponse.getDataId(), emptyCopyResponse.getDataId());

        final FetchDataRequest emptyFetchRequest1 = prepareReadRequest(MEMBER_1, null, emptyAddResponse.getDataId());
        final FetchDataResponse emptyFetchResponse1 = bean.fetchData(emptyFetchRequest1);
        assertEquals(ReturnCode.SUCCESS.getCode(), emptyFetchResponse1.getReturnCode());
        final Metadata metadata1 = emptyFetchResponse1.getMetadata().get(0);

        final FetchDataRequest emptyFetchRequest2 = prepareReadRequest(MEMBER_1, null, emptyCopyResponse.getDataId());
        final FetchDataResponse emptyFetchResponse2 = bean.fetchData(emptyFetchRequest2);
        assertEquals(ReturnCode.SUCCESS.getCode(), emptyFetchResponse2.getReturnCode());
        final Metadata emptyMetadata2 = emptyFetchResponse2.getMetadata().get(0);

        // Comparing the first Object with the copied Object, name, type and
        // data must be the same, the ID's not.
        assertEquals(metadata1.getDataName(), emptyMetadata2.getDataName());
        assertEquals(metadata1.getTypeName(), emptyMetadata2.getTypeName());
        assertNotEquals(metadata1.getDataId(), emptyMetadata2.getDataId());
        assertEquals(CIRCLE_1_ID, metadata1.getCircleId());
        assertEquals(CIRCLE_2_ID, emptyMetadata2.getCircleId());
        assertArrayEquals(emptyFetchResponse1.getData(), emptyFetchResponse2.getData());
    }

    @Test
    void testCopyDataNotExistingData() {
        final ShareBean bean = prepareShareBean();
        final String dataId = UUID.randomUUID().toString();

        final ProcessDataRequest request = prepareCopyDataRequest(MEMBER_1, dataId, CIRCLE_2_ID, null);
        assertTrue(request.validate().isEmpty());

        final ProcessDataResponse response = bean.processData(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No data could be found for the given Data Id '" + dataId + "'.", response.getReturnMessage());
    }

    @Test
    void testCopyDataToNotExistingFolder() {
        final ShareBean bean = prepareShareBean();
        final String folderId = UUID.randomUUID().toString();
        final String dataName = "toCopy";

        final ProcessDataRequest addRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, dataName, 524288);
        final ProcessDataResponse addResponse = bean.processData(addRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addResponse.getReturnCode());
        assertEquals("The Data Object '" + dataName + "' was successfully added to the Circle '" + CIRCLE_1 + "'.", addResponse.getReturnMessage());
        assertNotNull(addResponse.getDataId());

        final ProcessDataRequest copyRequest = prepareCopyDataRequest(MEMBER_1, addResponse.getDataId(), CIRCLE_2_ID, folderId);
        final ProcessDataResponse response = bean.processData(copyRequest);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Provided FolderId '" + folderId + "' is not a folder.", response.getReturnMessage());
    }

    @Test
    void testCopyDataToNonTrusteeCircle() {
        final ShareBean bean = prepareShareBean();
        final String dataId = UUID.randomUUID().toString();

        final ProcessDataRequest request = prepareCopyDataRequest(MEMBER_4, dataId, CIRCLE_1_ID, null);
        assertTrue(request.validate().isEmpty());

        final ProcessDataResponse response = bean.processData(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The member has no trustee relationship with the target Circle '" + CIRCLE_1_ID + "'.", response.getReturnMessage());
    }

    @Test
    void testCopyDataWithoutPermissionInTarget() {
        final ShareBean bean = prepareShareBean();
        final String dataName = "emptyData";

        final ProcessDataRequest addRequest = prepareAddDataRequest(MEMBER_3, CIRCLE_3_ID, dataName, 512);
        final ProcessDataResponse addResponse = bean.processData(addRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addResponse.getReturnCode());
        assertEquals("The Data Object '" + dataName + "' was successfully added to the Circle '" + CIRCLE_3 + "'.", addResponse.getReturnMessage());
        assertNotNull(addResponse.getDataId());

        final ProcessDataRequest copyRequest = prepareCopyDataRequest(MEMBER_3, addResponse.getDataId(), CIRCLE_1_ID, null);
        final ProcessDataResponse response = bean.processData(copyRequest);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Member is not permitted to perform this action for the target Circle.", response.getReturnMessage());
    }

    @Test
    void testDeleteFolderWithData() {
        final ShareBean bean = prepareShareBean();
        final String folderName = "folder1";

        final ProcessDataRequest addFolderRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, folderName, 0);
        addFolderRequest.setTypeName(Constants.FOLDER_TYPENAME);
        final ProcessDataResponse addFolderResponse = bean.processData(addFolderRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addFolderResponse.getReturnCode());
        assertEquals("The Folder '" + folderName + "' was successfully added to the Circle '" + CIRCLE_1 + "'.", addFolderResponse.getReturnMessage());
        assertNotNull(addFolderResponse.getDataId());

        final String moreData = "More Data";
        final ProcessDataRequest addDataRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, moreData, 524288);
        addDataRequest.setFolderId(addFolderResponse.getDataId());
        final ProcessDataResponse addDataResponse = bean.processData(addDataRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addDataResponse.getReturnCode());
        assertEquals("The Data Object '" + moreData + "' was successfully added to the Circle '" + CIRCLE_1 + "'.", addDataResponse.getReturnMessage());

        final ProcessDataRequest deleteFolderRequest = prepareDeleteRequest(MEMBER_1, addFolderResponse.getDataId());
        assertTrue(deleteFolderRequest.validate().isEmpty());

        final ProcessDataResponse response = bean.processData(deleteFolderRequest);
        assertEquals(ReturnCode.INTEGRITY_WARNING.getCode(), response.getReturnCode());
        assertEquals("The Folder cannot be removed as it is not empty.", response.getReturnMessage());
    }

    @Test
    void testMoveFolderWithAddRequest() {
        final ShareBean bean = prepareShareBean();
        final String folderName1 = "folder1";

        final ProcessDataRequest addFolderRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, folderName1, 0);
        addFolderRequest.setTypeName(Constants.FOLDER_TYPENAME);
        final ProcessDataResponse addFolderResponse1 = bean.processData(addFolderRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addFolderResponse1.getReturnCode());
        assertEquals("The Folder '" + folderName1 + "' was successfully added to the Circle '" + CIRCLE_1 + "'.", addFolderResponse1.getReturnMessage());
        assertNotNull(addFolderResponse1.getDataId());
        final String folderId1 = addFolderResponse1.getDataId();

        final String folderName2 = "folder2";
        addFolderRequest.setCredential(crypto.stringToBytes(MEMBER_1));
        addFolderRequest.setDataName(folderName2);
        final ProcessDataResponse addFolderResponse2 = bean.processData(addFolderRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addFolderResponse2.getReturnCode());
        assertEquals("The Folder '" + folderName2 + "' was successfully added to the Circle '" + CIRCLE_1 + "'.", addFolderResponse2.getReturnMessage());
        assertNotNull(addFolderResponse2.getDataId());
        final String folderId2 = addFolderResponse2.getDataId();

        final ProcessDataRequest moveFolderRequest = prepareUpdateRequest(MEMBER_1, folderId2);
        moveFolderRequest.setFolderId(folderId1);

        final ProcessDataResponse response = bean.processData(moveFolderRequest);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), response.getReturnCode());
        assertEquals("It is not permitted to move Folders.", response.getReturnMessage());
    }

    @Test
    void testMoveDataWithAddRequest() {
        final ShareBean bean = prepareShareBean();
        final String folderName = "folder1";

        final ProcessDataRequest createFolderRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, folderName, 0);
        createFolderRequest.setTypeName(Constants.FOLDER_TYPENAME);
        final ProcessDataResponse createFolderResponse = bean.processData(createFolderRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), createFolderResponse.getReturnCode());
        assertEquals("The Folder '" + folderName + "' was successfully added to the Circle '" + CIRCLE_1 + "'.", createFolderResponse.getReturnMessage());
        assertNotNull(createFolderResponse.getDataId());

        final String myData = "My Data";
        final ProcessDataRequest addDataRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, myData, 512);
        final ProcessDataResponse addDataResponse = bean.processData(addDataRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addDataResponse.getReturnCode());
        assertEquals("The Data Object '" + myData + "' was successfully added to the Circle '" + CIRCLE_1 + "'.", addDataResponse.getReturnMessage());
        assertNotNull(addDataResponse.getDataId());

        final ProcessDataRequest moveDataRequest = prepareUpdateRequest(MEMBER_1, addDataResponse.getDataId());
        moveDataRequest.setFolderId(createFolderResponse.getDataId());
        final ProcessDataResponse moveFolderResponse = bean.processData(moveDataRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), moveFolderResponse.getReturnCode());
        assertEquals("The Data Object '" + myData + "' was successfully updated.", moveFolderResponse.getReturnMessage());
        assertEquals(addDataResponse.getDataId(), moveFolderResponse.getDataId());
    }

    @Test
    void testMovingDataToFolderWhereSameNameDataExist() {
        final ShareBean bean = prepareShareBean();

        // Step 1, create 2 folders
        final ProcessDataRequest folderRequest1 = prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "First Folder", 0);
        folderRequest1.setTypeName(Constants.FOLDER_TYPENAME);
        final ProcessDataResponse folderResponse1 = bean.processData(folderRequest1);
        assertTrue(folderResponse1.isOk());
        final ProcessDataRequest folderRequest2 = prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Second Folder", 0);
        folderRequest2.setTypeName(Constants.FOLDER_TYPENAME);
        final ProcessDataResponse folderResponse2 = bean.processData(folderRequest2);
        assertTrue(folderResponse2.isOk());

        // Step 2, Create 1 data record for each folder, with the same name
        final String dataName = "The Data";
        final ProcessDataRequest dataRequest1 = prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, dataName, 524288);
        dataRequest1.setFolderId(folderResponse1.getDataId());
        final ProcessDataResponse dataResponse1 = bean.processData(dataRequest1);
        assertTrue(dataResponse1.isOk());
        final ProcessDataRequest dataRequest2 = prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, dataName, 524288);
        dataRequest2.setFolderId(folderResponse2.getDataId());
        final ProcessDataResponse dataResponse2 = bean.processData(dataRequest2);
        assertTrue(dataResponse2.isOk());

        // Step 3, Verify that we have the correct data structure.
        final FetchDataRequest readRootRequest = prepareReadRequest(MEMBER_4, CIRCLE_3_ID, null);
        final FetchDataResponse readRootResponse = bean.fetchData(readRootRequest);
        assertTrue(readRootResponse.isOk());
        assertEquals(2, readRootResponse.getMetadata().size());
        assertEquals(folderResponse2.getDataId(), readRootResponse.getMetadata().get(0).getDataId());
        assertEquals(folderResponse1.getDataId(), readRootResponse.getMetadata().get(1).getDataId());

        final FetchDataRequest readFolder1Request = prepareReadRequest(MEMBER_4, CIRCLE_3_ID, folderResponse1.getDataId());
        final FetchDataResponse readFolder1Response = bean.fetchData(readFolder1Request);
        assertTrue(readFolder1Response.isOk());
        assertEquals(1, readFolder1Response.getMetadata().size());
        assertEquals(dataResponse1.getDataId(), readFolder1Response.getMetadata().get(0).getDataId());

        final FetchDataRequest readFolder2Request = prepareReadRequest(MEMBER_4, CIRCLE_3_ID, folderResponse2.getDataId());
        final FetchDataResponse readFolder2Response = bean.fetchData(readFolder2Request);
        assertTrue(readFolder2Response.isOk());
        assertEquals(1, readFolder2Response.getMetadata().size());
        assertEquals(dataResponse2.getDataId(), readFolder2Response.getMetadata().get(0).getDataId());

        // Step 4, Move the data from folder 1 to folder 2.
        final ProcessDataRequest moveRequest = prepareUpdateRequest(MEMBER_4, CIRCLE_3_ID);
        moveRequest.setDataId(dataResponse1.getDataId());
        moveRequest.setFolderId(folderResponse2.getDataId());

        final ProcessDataResponse response = bean.processData(moveRequest);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The name '" + dataName + "' provided is already being used in the given folder.", response.getReturnMessage());
    }

    @Test
    void testMoveDataToDifferentCircleWithAddRequest() {
        final ShareBean bean = prepareShareBean();
        final String folderName = "folder1";

        final ProcessDataRequest addFolderRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, folderName, 0);
        addFolderRequest.setTypeName(Constants.FOLDER_TYPENAME);
        final ProcessDataResponse addFolderResponse = bean.processData(addFolderRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addFolderResponse.getReturnCode());
        assertEquals("The Folder '" + folderName + "' was successfully added to the Circle '" + CIRCLE_1 + "'.", addFolderResponse.getReturnMessage());
        assertNotNull(addFolderResponse.getDataId());

        final String dataName = "My Data";
        final ProcessDataRequest addDataRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, dataName, 512);
        final ProcessDataResponse addDataResponse = bean.processData(addDataRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addDataResponse.getReturnCode());
        assertEquals("The Data Object '" + dataName + "' was successfully added to the Circle '" + CIRCLE_2 + "'.", addDataResponse.getReturnMessage());
        assertNotNull(addDataResponse.getDataId());

        final ProcessDataRequest moveDataRequest = prepareUpdateRequest(MEMBER_1, addDataResponse.getDataId());
        moveDataRequest.setFolderId(addFolderResponse.getDataId());

        final ProcessDataResponse response = bean.processData(moveDataRequest);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), response.getReturnCode());
        assertEquals("Moving Data from one Circle to another is not permitted.", response.getReturnMessage());
    }

    /**
     * <p>See <a href="https://github.com/JavaDogs/cws/issues/57">Github</a>.</p>
     *
     * <p>From the stacktrace with a build using a snapshot from 2019-07-24:</p>
     * <pre>
     * Caused by: java.lang.NullPointerException
     *         at deployment.cws.war//ProcessDataService.processAddData(ProcessDataService.java:98)
     * </pre>
     *
     * <p>From the description, how it was caused: <i>It might happen when
     * adding a file and then again trying to add the same file with the
     * same id.</i></p>
     *
     * <p>The Python code to add the data is:</p>
     * <pre>
     * def add_file(url, login, pw, circle_id, blob, uid):
     *     data = dict(circleId=circle_id,
     *                 dataName=uid,
     *                 data=base64.b64encode(blob))
     *     response = _post(url, login, pw, 'data/addData', data)
     *     return response
     * </pre>
     *
     * <p>The test that appear to fail:</p>
     * <pre>
     *     # ... upload file
     *     blob = 'ABC'
     *     uid = u'123'
     *     data = pycws.add_file(
     *         URL, 'admin', 'admin', circle_id, blob, uid)
     *     assert data["returnCode"] == 200
     *     file_id = data.get('dataId')
     * </pre>
     */
    @Test
    void testBugReport57() {
        // It should be noted, that for a normal DB status, it was not possible
        // to replicate the error. Hence, this attempt to corrupt the database
        // prior to adding new data.
        final int deleted = entityManager
                .createQuery("delete from MetadataEntity where circle.id = :cid")
                .setParameter("cid", 1L)
                .executeUpdate();
        assertEquals(1, deleted);

        final ShareBean bean = prepareShareBean();
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "My Object", 1024);

        final ProcessDataResponse response = bean.processData(request);
        assertEquals(ReturnCode.INTEGRITY_ERROR.getCode(), response.getReturnCode());
        assertTrue(response.getReturnMessage().contains("No Parent could be found for the Circle"));
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private static FetchDataRequest prepareReadRequest(final String account, final String circleId, final String dataId) {
        final FetchDataRequest dataRequest = prepareRequest(FetchDataRequest.class, account);
        dataRequest.setCircleId(circleId);
        dataRequest.setDataId(dataId);

        return dataRequest;
    }

    private static ProcessDataRequest prepareUpdateRequest(final String account, final String dataId) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, account);
        request.setAction(Action.UPDATE);
        request.setDataId(dataId);

        return request;
    }

    private static ProcessDataRequest prepareCopyDataRequest(final String account, final String dataId, final String targetCircleId, final String targetFolderId) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, account);
        request.setAction(Action.COPY);
        request.setDataId(dataId);
        request.setTargetCircleId(targetCircleId);
        request.setTargetFolderId(targetFolderId);

        return request;
    }

    private static ProcessDataRequest prepareDeleteRequest(final String account, final String dataId) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, account);
        request.setAction(Action.DELETE);
        request.setDataId(dataId);

        return request;
    }
}
