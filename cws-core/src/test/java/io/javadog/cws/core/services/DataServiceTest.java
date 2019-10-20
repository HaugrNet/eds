/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
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
package io.javadog.cws.core.services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.dtos.Metadata;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.SanityStatus;
import io.javadog.cws.core.exceptions.CWSException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Query;
import org.junit.jupiter.api.Test;

/**
 * <p>Common test class for the Process & Fetch Data Services.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class DataServiceTest extends DatabaseSetup {

    @Test
    void testEmptyProcessRequest() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = new ProcessDataRequest();

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing." +
                "\nKey: action, Error: No action has been provided.", cause.getMessage());
    }

    @Test
    void testEmptyFetchRequest() {
        final FetchDataService service = new FetchDataService(settings, entityManager);
        final FetchDataRequest request = new FetchDataRequest();

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing." +
                "\nKey: circle & data Id, Error: Either a Circle or Data Id must be provided.", cause.getMessage());
    }

    @Test
    void testSavingAndReadingData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final FetchDataService dataService = new FetchDataService(settings, entityManager);

        // 1 MB large Data
        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "My Data", 1048576);
        // Since the logic is deliberately clearing data post encryption, and
        // it means that the bytes from the request will be overwritten by
        // zeroes. To ensure that the check works, a String representing the
        // byte array is stored prior.
        final String toSave = crypto.bytesToString(saveRequest.getData());
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());

        final FetchDataRequest fetchRequest = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest.setDataId(saveResponse.getDataId());
        final FetchDataResponse fetchResponse = dataService.perform(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals(toSave, crypto.bytesToString(fetchResponse.getData()));
    }

    @Test
    void testSaveAndUpdateData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final FetchDataService dataService = new FetchDataService(settings, entityManager);

        // 1 MB large Data
        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "My Data", 1048576);
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());

        final FetchDataRequest fetchRequest1 = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest1.setDataId(saveResponse.getDataId());
        final FetchDataResponse fetchResponse1 = dataService.perform(fetchRequest1);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse1.getReturnCode());

        final ProcessDataRequest updateRequest = prepareUpdateRequest(MEMBER_1, saveResponse.getDataId());
        updateRequest.setDataName("New Name");
        updateRequest.setData(generateData(1048576));
        final ProcessDataResponse updateResponse = service.perform(updateRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), updateResponse.getReturnCode());

        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());
        final FetchDataRequest fetchRequest2 = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest2.setDataId(saveResponse.getDataId());
        final FetchDataResponse fetchResponse2 = dataService.perform(fetchRequest2);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse2.getReturnCode());
        assertEquals(fetchResponse2.getMetadata().get(0).getDataId(), fetchResponse1.getMetadata().get(0).getDataId());
        assertEquals("My Data", fetchResponse1.getMetadata().get(0).getDataName());
        assertEquals("New Name", fetchResponse2.getMetadata().get(0).getDataName());
    }

    @Test
    void testSaveAndUpdateSimpleData() {
        final ProcessDataService saveService = new ProcessDataService(settings, entityManager);
        final String dataName = "status";
        final String initContent = "NEW";
        final String updateContent = "ACCEPTED";

        final ProcessDataRequest saveRequest = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        saveRequest.setAction(Action.ADD);
        saveRequest.setCircleId(CIRCLE_1_ID);
        saveRequest.setDataName(dataName);
        saveRequest.setData(initContent.getBytes(StandardCharsets.UTF_8));
        final ProcessDataResponse saveResponse = saveService.perform(saveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());

        final ProcessDataRequest updateRequest = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        updateRequest.setAction(Action.UPDATE);
        updateRequest.setCircleId(CIRCLE_1_ID);
        updateRequest.setDataId(saveResponse.getDataId());
        updateRequest.setDataName(dataName);
        updateRequest.setData(updateContent.getBytes(StandardCharsets.UTF_8));
        final ProcessDataResponse updateResponse = saveService.perform(updateRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), updateResponse.getReturnCode());

        final FetchDataService readService = new FetchDataService(settings, entityManager);
        final FetchDataRequest fetchRequest = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest.setDataId(saveResponse.getDataId());
        final FetchDataResponse fetchResponse = readService.perform(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals(dataName, fetchResponse.getMetadata().get(0).getDataName());
        assertEquals(updateContent, new String(fetchResponse.getData(), StandardCharsets.UTF_8));
    }

    @Test
    void testAddingAndFetchingData() {
        final ProcessDataService processService = new ProcessDataService(settings, entityManager);
        final FetchDataService fetchService = new FetchDataService(settings, entityManager);

        final FetchDataRequest fetchRequest = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetchRequest.setCircleId(CIRCLE_1_ID);
        final FetchDataResponse emptyResponse = fetchService.perform(fetchRequest);
        assertTrue(emptyResponse.isOk());
        assertEquals(0L, emptyResponse.getRecords());

        // Add some data...
        assertTrue(processService.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "My Data 1", 1048576)).isOk());
        assertTrue(processService.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "My Data 2", 1048576)).isOk());
        assertTrue(processService.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "My Data 3", 1048576)).isOk());

        fetchRequest.setCredential(crypto.stringToBytes(MEMBER_1));
        final FetchDataResponse fullResponse = fetchService.perform(fetchRequest);
        assertTrue(fullResponse.isOk());
        assertEquals(3L, fullResponse.getRecords());
    }

    @Test
    void testAddEmptyData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "The Data", 0);

        final ProcessDataResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());

        final FetchDataService readService = new FetchDataService(settings, entityManager);
        final FetchDataRequest readRequest = prepareReadRequest(MEMBER_1, CIRCLE_1_ID, response.getDataId());
        final FetchDataResponse readResponse = readService.perform(readRequest);
        assertTrue(readResponse.isOk());
    }

    @Test
    void testCreateCircleAsNewMember() {
        final String accountName = "accountName";
        final ProcessMemberRequest memberRequest = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        memberRequest.setNewAccountName(accountName);
        memberRequest.setNewCredential(crypto.stringToBytes(accountName));
        memberRequest.setAction(Action.CREATE);
        final ProcessMemberService memberService = new ProcessMemberService(settings, entityManager);
        final ProcessMemberResponse memberResponse = memberService.perform(memberRequest);
        assertTrue(memberResponse.isOk());

        final ProcessCircleRequest circleRequest = prepareRequest(ProcessCircleRequest.class, accountName);
        circleRequest.setCircleName("circleName");
        circleRequest.setAction(Action.CREATE);
        final ProcessCircleService circleService = new ProcessCircleService(settings, entityManager);
        final ProcessCircleResponse circleResponse = circleService.perform(circleRequest);
        assertTrue(circleResponse.isOk());
    }

    @Test
    void testAddDataWithInvalidChecksum() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "The Data", 524288);

        final ProcessDataResponse response = service.perform(request);
        assertTrue(response.isOk());
        falsifyChecksum(response, new Date(), SanityStatus.FAILED);

        // Now to the actual test - reading the data with invalid checksum
        final FetchDataService readService = new FetchDataService(settings, entityManager);
        final FetchDataRequest readRequest = prepareReadRequest(MEMBER_1, CIRCLE_1_ID, response.getDataId());
        final FetchDataResponse readResponse = readService.perform(readRequest);
        assertEquals(ReturnCode.INTEGRITY_ERROR.getCode(), readResponse.getReturnCode());
        assertEquals("The Encrypted Data Checksum is invalid, the data appears to have been corrupted.", readResponse.getReturnMessage());
    }

    @Test
    void testAddEmptyAndUpdateData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "The Data", 0);

        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());

        final ProcessDataRequest updateRequest = prepareUpdateRequest(MEMBER_1, saveResponse.getDataId());
        updateRequest.setData(generateData(1048576));
        service.perform(updateRequest);
    }

    @Test
    void testAddAndMoveToInvalidFolder() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "The Data", 1048576);

        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());

        final ProcessDataRequest updateRequest = prepareUpdateRequest(MEMBER_1, saveResponse.getDataId());
        updateRequest.setFolderId(UUID.randomUUID().toString());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(updateRequest));
        assertEquals(ReturnCode.INTEGRITY_WARNING, cause.getReturnCode());
        assertEquals("No existing Folder could be found.", cause.getMessage());
    }

    @Test
    void testAddDataWithoutPermission() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_5, CIRCLE_3_ID, "The Data", 1048576);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("The requesting Account is not permitted to Process Data.", cause.getMessage());
    }

    @Test
    void testUpdatingDataWithoutPermission() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_2, CIRCLE_2_ID, "The Data", 1048576);
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertTrue(saveResponse.isOk());

        final ProcessDataRequest updateRequest = prepareUpdateRequest(MEMBER_3, saveResponse.getDataId());
        updateRequest.setDataName("New Name");

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(updateRequest));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("The current Account is not allowed to perform the given action.", cause.getMessage());
    }

    @Test
    void testAddDataWithInvalidDataType() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "The Data", 512);
        request.setTypeName("Weird");

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.INTEGRITY_WARNING, cause.getReturnCode());
        assertEquals("Cannot find a matching DataType for the Object.", cause.getMessage());
    }

    @Test
    void testDeletingDataWithoutPermission() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_2, CIRCLE_2_ID, "The Data", 1048576);
        final ProcessDataResponse saveResponse = service.perform(saveRequest);

        final ProcessDataRequest updateRequest = prepareDeleteRequest(MEMBER_3, saveResponse.getDataId());
        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(updateRequest));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("The current Account is not allowed to perform the given action.", cause.getMessage());
    }

    @Test
    void testFetchingInvalidDataId() {
        final FetchDataService service = new FetchDataService(settings, entityManager);
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, MEMBER_4);
        request.setCircleId(CIRCLE_3_ID);
        request.setDataId(UUID.randomUUID().toString());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("No information could be found for the given Id.", cause.getMessage());
    }

    @Test
    void testAddDataToUnknownFolder() {
        final String folderId = UUID.randomUUID().toString();

        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Data File", 512);
        request.setFolderId(folderId);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Provided FolderId '" + folderId + "' is not a folder.", cause.getMessage());
    }

    @Test
    void testAddDataToInvalidFolder() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Data File", 512);
        final ProcessDataResponse response = service.perform(request);
        assertTrue(response.isOk());
        final String folderId = response.getDataId();

        // We're taking the previously generated Data Id and uses that as folder.
        request.setCredential(crypto.stringToBytes(MEMBER_4));
        request.setFolderId(folderId);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Provided FolderId '" + folderId + "' is not a folder.", cause.getMessage());
    }

    @Test
    void testUpdateNotExistingData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, MEMBER_4);
        request.setAction(Action.UPDATE);
        request.setCircleId(CIRCLE_2_ID);
        request.setDataId(UUID.randomUUID().toString());
        request.setDataName("New Name for our not existing Data");
        request.setData(generateData(512));

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("The requested Data Object could not be found.", cause.getMessage());
    }

    @Test
    void testSaveAndDeleteData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "The Data", 524288);
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());
        assertEquals("Ok", saveResponse.getReturnMessage());

        final ProcessDataRequest deleteRequest = prepareDeleteRequest(MEMBER_1, saveResponse.getDataId());
        final ProcessDataResponse deleteResponse = service.perform(deleteRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), deleteResponse.getReturnCode());
        assertEquals("Ok", deleteResponse.getReturnMessage());
    }

    @Test
    void testSaveAndDeleteDataWithoutPermission() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "Known Data", 524288);
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());
        assertEquals("Ok", saveResponse.getReturnMessage());

        final ProcessDataRequest deleteRequest = prepareDeleteRequest(MEMBER_3, saveResponse.getDataId());
        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(deleteRequest));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("The current Account is not allowed to perform the given action.", cause.getMessage());
    }

    @Test
    void testSaveAndDeleteDataWithoutAccess() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest saveRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "More Data", 524288);
        final ProcessDataResponse saveResponse = service.perform(saveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());
        assertEquals("Ok", saveResponse.getReturnMessage());

        final ProcessDataRequest deleteRequest = prepareDeleteRequest(MEMBER_4, saveResponse.getDataId());
        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(deleteRequest));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("The requested Data Object could not be found.", cause.getMessage());
    }

    @Test
    void testDeleteNotExistingData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareDeleteRequest(MEMBER_1, UUID.randomUUID().toString());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("The requested Data Object could not be found.", cause.getMessage());
    }

    @Test
    void testAddUpdateAndDeleteFolder() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "folder1", 0);
        request.setTypeName("folder");
        final ProcessDataResponse addResponse = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), addResponse.getReturnCode());
        assertEquals("Ok", addResponse.getReturnMessage());
        assertNotNull(addResponse.getDataId());

        final ProcessDataRequest updateRequest = prepareUpdateRequest(MEMBER_1, addResponse.getDataId());
        updateRequest.setDataName("updated Folder Name");
        final ProcessDataResponse updateResponse = service.perform(updateRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), updateResponse.getReturnCode());
        assertEquals("Ok", updateResponse.getReturnMessage());
        assertEquals(addResponse.getDataId(), updateResponse.getDataId());

        final ProcessDataRequest deleteRequest = prepareDeleteRequest(MEMBER_1, addResponse.getDataId());
        final ProcessDataResponse deleteResponse = service.perform(deleteRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), deleteResponse.getReturnCode());
        assertEquals("Ok", deleteResponse.getReturnMessage());
        assertNull(deleteResponse.getDataId());
    }

    @Test
    void testAddSameFolder() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "folder1", 0);
        request.setTypeName(Constants.FOLDER_TYPENAME);
        final ProcessDataResponse response1 = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response1.getReturnCode());
        assertEquals("Ok", response1.getReturnMessage());
        assertNotNull(response1.getDataId());

        request.setCredential(crypto.stringToBytes(MEMBER_1));
        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Another record with the same name already exists.", cause.getMessage());
    }

    @Test
    void testCopyData() {
        final ProcessDataService copyDataService = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest copyAddRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "toCopy", 524288);
        final ProcessDataResponse copyAddResponse = copyDataService.perform(copyAddRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), copyAddResponse.getReturnCode());
        assertEquals("Ok", copyAddResponse.getReturnMessage());
        assertNotNull(copyAddResponse.getDataId());

        final ProcessDataRequest copyRequest = prepareCopyDataRequest(MEMBER_1, copyAddResponse.getDataId(), CIRCLE_2_ID, null);
        final ProcessDataResponse copyResponse = copyDataService.perform(copyRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), copyResponse.getReturnCode());
        assertEquals("Ok", copyResponse.getReturnMessage());
        assertNotNull(copyResponse.getDataId());
        assertNotEquals(copyAddResponse.getDataId(), copyResponse.getDataId());

        final FetchDataService copyFetchService = new FetchDataService(settings, entityManager);
        final FetchDataRequest copyFetchRequest1 = prepareReadRequest(MEMBER_1, null, copyAddResponse.getDataId());
        final FetchDataResponse copyFetchResponse1 = copyFetchService.perform(copyFetchRequest1);
        assertEquals(ReturnCode.SUCCESS.getCode(), copyFetchResponse1.getReturnCode());
        final Metadata metadata1 = copyFetchResponse1.getMetadata().get(0);

        final FetchDataRequest copyFetchRequest2 = prepareReadRequest(MEMBER_1, null, copyResponse.getDataId());
        final FetchDataResponse copyFetchResponse2 = copyFetchService.perform(copyFetchRequest2);
        assertEquals(ReturnCode.SUCCESS.getCode(), copyFetchResponse2.getReturnCode());
        final Metadata copyMetadata2 = copyFetchResponse2.getMetadata().get(0);

        // Comparing the first Object with the copied Object, name, type and
        // data must be the same, the Id's not.
        assertEquals(metadata1.getDataName(), copyMetadata2.getDataName());
        assertEquals(metadata1.getTypeName(), copyMetadata2.getTypeName());
        assertNotEquals(metadata1.getDataId(), copyMetadata2.getDataId());
        assertEquals(CIRCLE_1_ID, metadata1.getCircleId());
        assertEquals(CIRCLE_2_ID, copyMetadata2.getCircleId());
        assertArrayEquals(copyFetchResponse1.getData(), copyFetchResponse2.getData());
    }

    @Test
    void testMoveData() {
        final ProcessDataService moveDataService = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest moveAddRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "toMove", 524288);
        final ProcessDataResponse moveAddResponse = moveDataService.perform(moveAddRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), moveAddResponse.getReturnCode());
        assertEquals("Ok", moveAddResponse.getReturnMessage());
        assertNotNull(moveAddResponse.getDataId());

        final ProcessDataRequest moveRequest = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        moveRequest.setAction(Action.MOVE);
        moveRequest.setDataId(moveAddResponse.getDataId());
        moveRequest.setTargetCircleId(CIRCLE_2_ID);

        final ProcessDataResponse moveResponse = moveDataService.perform(moveRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), moveResponse.getReturnCode());
        assertEquals("Ok", moveResponse.getReturnMessage());
        assertNotNull(moveResponse.getDataId());
        assertNotEquals(moveAddResponse.getDataId(), moveResponse.getDataId());

        final FetchDataService moveFetchService = new FetchDataService(settings, entityManager);
        final FetchDataRequest moveFetchRequest1 = prepareReadRequest(MEMBER_1, null, moveResponse.getDataId());
        final FetchDataResponse moveFetchResponse1 = moveFetchService.perform(moveFetchRequest1);
        assertEquals(ReturnCode.SUCCESS.getCode(), moveFetchResponse1.getReturnCode());

        final FetchDataRequest moveFetchRequest2 = prepareReadRequest(MEMBER_1, null, moveAddResponse.getDataId());
        final CWSException cause = assertThrows(CWSException.class, () -> moveFetchService.perform(moveFetchRequest2));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("No information could be found for the given Id.", cause.getMessage());
    }

    @Test
    void testCopyFolder() {
        final ProcessDataService dataService = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest addRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "folderToCopy", 0);
        addRequest.setTypeName(Constants.FOLDER_TYPENAME);
        final ProcessDataResponse addResponse = dataService.perform(addRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addResponse.getReturnCode());
        assertEquals("Ok", addResponse.getReturnMessage());
        assertNotNull(addResponse.getDataId());

        final ProcessDataRequest copyRequest = prepareCopyDataRequest(MEMBER_1, addResponse.getDataId(), CIRCLE_2_ID, null);
        final CWSException cause = assertThrows(CWSException.class, () -> dataService.perform(copyRequest));
        assertEquals(ReturnCode.ILLEGAL_ACTION, cause.getReturnCode());
        assertEquals("It is not permitted to copy or move folders.", cause.getMessage());
    }

    @Test
    void testCopyEmptyData() {
        final ProcessDataService dataService = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest emptyAddRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "emptyData", 0);
        final ProcessDataResponse emptyAddResponse = dataService.perform(emptyAddRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), emptyAddResponse.getReturnCode());
        assertEquals("Ok", emptyAddResponse.getReturnMessage());
        assertNotNull(emptyAddResponse.getDataId());

        final ProcessDataRequest emptyCopyRequest = prepareCopyDataRequest(MEMBER_1, emptyAddResponse.getDataId(), CIRCLE_2_ID, null);
        final ProcessDataResponse emptyCopyResponse = dataService.perform(emptyCopyRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), emptyCopyResponse.getReturnCode());
        assertEquals("Ok", emptyCopyResponse.getReturnMessage());
        assertNotNull(emptyCopyResponse.getDataId());
        assertNotEquals(emptyAddResponse.getDataId(), emptyCopyResponse.getDataId());

        final FetchDataService emptyFetchService = new FetchDataService(settings, entityManager);
        final FetchDataRequest emptyFetchRequest1 = prepareReadRequest(MEMBER_1, null, emptyAddResponse.getDataId());
        final FetchDataResponse emptyFetchResponse1 = emptyFetchService.perform(emptyFetchRequest1);
        assertEquals(ReturnCode.SUCCESS.getCode(), emptyFetchResponse1.getReturnCode());
        final Metadata metadata1 = emptyFetchResponse1.getMetadata().get(0);

        final FetchDataRequest emptyFetchRequest2 = prepareReadRequest(MEMBER_1, null, emptyCopyResponse.getDataId());
        final FetchDataResponse emptyFetchResponse2 = emptyFetchService.perform(emptyFetchRequest2);
        assertEquals(ReturnCode.SUCCESS.getCode(), emptyFetchResponse2.getReturnCode());
        final Metadata emptyMetadata2 = emptyFetchResponse2.getMetadata().get(0);

        // Comparing the first Object with the copied Object, name, type and
        // data must be the same, the Id's not.
        assertEquals(metadata1.getDataName(), emptyMetadata2.getDataName());
        assertEquals(metadata1.getTypeName(), emptyMetadata2.getTypeName());
        assertNotEquals(metadata1.getDataId(), emptyMetadata2.getDataId());
        assertEquals(CIRCLE_1_ID, metadata1.getCircleId());
        assertEquals(CIRCLE_2_ID, emptyMetadata2.getCircleId());
        assertArrayEquals(emptyFetchResponse1.getData(), emptyFetchResponse2.getData());
    }

    @Test
    void testCopyDataNotExistingData() {
        final String dataId = UUID.randomUUID().toString();

        final ProcessDataService dataService = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest copyRequest = prepareCopyDataRequest(MEMBER_1, dataId, CIRCLE_2_ID, null);
        assertTrue(copyRequest.validate().isEmpty());

        final CWSException cause = assertThrows(CWSException.class, () -> dataService.perform(copyRequest));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("No data could be found for the given Data Id '" + dataId + "'.", cause.getMessage());
    }

    @Test
    void testCopyDataToNotExistingFolder() {
        final String folderId = UUID.randomUUID().toString();

        final ProcessDataService dataService = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest addRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "toCopy", 524288);
        final ProcessDataResponse addResponse = dataService.perform(addRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addResponse.getReturnCode());
        assertEquals("Ok", addResponse.getReturnMessage());
        assertNotNull(addResponse.getDataId());

        final ProcessDataRequest copyRequest = prepareCopyDataRequest(MEMBER_1, addResponse.getDataId(), CIRCLE_2_ID, folderId);
        final CWSException cause = assertThrows(CWSException.class, () -> dataService.perform(copyRequest));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Provided FolderId '" + folderId + "' is not a folder.", cause.getMessage());
    }

    @Test
    void testCopyDataToNonTrusteeCircle() {
        final String dataId = UUID.randomUUID().toString();

        final ProcessDataService dataService = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest copyRequest = prepareCopyDataRequest(MEMBER_4, dataId, CIRCLE_1_ID, null);
        assertTrue(copyRequest.validate().isEmpty());

        final CWSException cause = assertThrows(CWSException.class, () -> dataService.perform(copyRequest));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("The member has no trustee relationship with the target Circle '" + CIRCLE_1_ID + "'.", cause.getMessage());
    }

    @Test
    void testCopyDataWithoutPermissionInTarget() {
        final ProcessDataService dataService = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest addRequest = prepareAddDataRequest(MEMBER_3, CIRCLE_3_ID, "emptyData", 512);
        final ProcessDataResponse addResponse = dataService.perform(addRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addResponse.getReturnCode());
        assertEquals("Ok", addResponse.getReturnMessage());
        assertNotNull(addResponse.getDataId());

        final ProcessDataRequest copyRequest = prepareCopyDataRequest(MEMBER_3, addResponse.getDataId(), CIRCLE_1_ID, null);
        final CWSException cause = assertThrows(CWSException.class, () -> dataService.perform(copyRequest));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("Member is not permitted to perform this action for the target Circle.", cause.getMessage());
    }

    @Test
    void testDeleteFolderWithData() {
        final ProcessDataService dataService = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest addFolderRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "folder1", 0);
        addFolderRequest.setTypeName("folder");
        final ProcessDataResponse addFolderResponse = dataService.perform(addFolderRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addFolderResponse.getReturnCode());
        assertEquals("Ok", addFolderResponse.getReturnMessage());
        assertNotNull(addFolderResponse.getDataId());

        final ProcessDataRequest addDataRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "More Data", 524288);
        addDataRequest.setFolderId(addFolderResponse.getDataId());
        final ProcessDataResponse addDataResponse = dataService.perform(addDataRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addDataResponse.getReturnCode());
        assertEquals("Ok", addDataResponse.getReturnMessage());

        final ProcessDataRequest deleteFolderRequest = prepareDeleteRequest(MEMBER_1, addFolderResponse.getDataId());
        assertTrue(deleteFolderRequest.validate().isEmpty());

        final CWSException cause = assertThrows(CWSException.class, () -> dataService.perform(deleteFolderRequest));
        assertEquals(ReturnCode.INTEGRITY_WARNING, cause.getReturnCode());
        assertEquals("The requested Folder cannot be removed as it is not empty.", cause.getMessage());
    }

    @Test
    void testMoveFolderWithAddRequest() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest addFolderRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "folder1", 0);
        addFolderRequest.setTypeName("folder");
        final ProcessDataResponse addFolderResponse1 = service.perform(addFolderRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addFolderResponse1.getReturnCode());
        assertEquals("Ok", addFolderResponse1.getReturnMessage());
        assertNotNull(addFolderResponse1.getDataId());
        final String folderId1 = addFolderResponse1.getDataId();

        addFolderRequest.setCredential(crypto.stringToBytes(MEMBER_1));
        addFolderRequest.setDataName("folder2");
        final ProcessDataResponse addFolderResponse2 = service.perform(addFolderRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addFolderResponse2.getReturnCode());
        assertEquals("Ok", addFolderResponse2.getReturnMessage());
        assertNotNull(addFolderResponse2.getDataId());
        final String folderId2 = addFolderResponse2.getDataId();

        final ProcessDataRequest moveFolderRequest = prepareUpdateRequest(MEMBER_1, folderId2);
        moveFolderRequest.setFolderId(folderId1);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(moveFolderRequest));
        assertEquals(ReturnCode.ILLEGAL_ACTION, cause.getReturnCode());
        assertEquals("It is not permitted to move Folders.", cause.getMessage());
    }

    @Test
    void testMoveDataWithAddRequest() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest createFolderRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "folder1", 0);
        createFolderRequest.setTypeName("folder");
        final ProcessDataResponse createFolderResponse = service.perform(createFolderRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), createFolderResponse.getReturnCode());
        assertEquals("Ok", createFolderResponse.getReturnMessage());
        assertNotNull(createFolderResponse.getDataId());

        final ProcessDataRequest addDataRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "my data", 512);
        final ProcessDataResponse addDataResponse = service.perform(addDataRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addDataResponse.getReturnCode());
        assertEquals("Ok", addDataResponse.getReturnMessage());
        assertNotNull(addDataResponse.getDataId());

        final ProcessDataRequest moveDataRequest = prepareUpdateRequest(MEMBER_1, addDataResponse.getDataId());
        moveDataRequest.setFolderId(createFolderResponse.getDataId());
        final ProcessDataResponse moveFolderResponse = service.perform(moveDataRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), moveFolderResponse.getReturnCode());
        assertEquals("Ok", moveFolderResponse.getReturnMessage());
        assertEquals(addDataResponse.getDataId(), moveFolderResponse.getDataId());
    }

    @Test
    void testMovingDataToFolderWhereSameNameDataExist() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final FetchDataService readService = new FetchDataService(settings, entityManager);

        // Step 1, create 2 folders
        final ProcessDataRequest folderRequest1 = prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "First Folder", 0);
        folderRequest1.setTypeName(Constants.FOLDER_TYPENAME);
        final ProcessDataResponse folderResponse1 = service.perform(folderRequest1);
        assertTrue(folderResponse1.isOk());
        final ProcessDataRequest folderRequest2 = prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Second Folder", 0);
        folderRequest2.setTypeName(Constants.FOLDER_TYPENAME);
        final ProcessDataResponse folderResponse2 = service.perform(folderRequest2);
        assertTrue(folderResponse2.isOk());

        // Step 2, Create 1 data record for each folder, with the same name
        final ProcessDataRequest dataRequest1 = prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "The Data", 524288);
        dataRequest1.setFolderId(folderResponse1.getDataId());
        final ProcessDataResponse dataResponse1 = service.perform(dataRequest1);
        assertTrue(dataResponse1.isOk());
        final ProcessDataRequest dataRequest2 = prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "The Data", 524288);
        dataRequest2.setFolderId(folderResponse2.getDataId());
        final ProcessDataResponse dataResponse2 = service.perform(dataRequest2);
        assertTrue(dataResponse2.isOk());

        // Step 3, Verify that we have the correct data structure.
        final FetchDataRequest readRootRequest = prepareReadRequest(MEMBER_4, CIRCLE_3_ID, null);
        final FetchDataResponse readRootResponse = readService.perform(readRootRequest);
        assertTrue(readRootResponse.isOk());
        assertEquals(2, readRootResponse.getMetadata().size());
        assertEquals(folderResponse2.getDataId(), readRootResponse.getMetadata().get(0).getDataId());
        assertEquals(folderResponse1.getDataId(), readRootResponse.getMetadata().get(1).getDataId());

        final FetchDataRequest readFolder1Request = prepareReadRequest(MEMBER_4, CIRCLE_3_ID, folderResponse1.getDataId());
        final FetchDataResponse readFolder1Response = readService.perform(readFolder1Request);
        assertTrue(readFolder1Response.isOk());
        assertEquals(1, readFolder1Response.getMetadata().size());
        assertEquals(dataResponse1.getDataId(), readFolder1Response.getMetadata().get(0).getDataId());

        final FetchDataRequest readFolder2Request = prepareReadRequest(MEMBER_4, CIRCLE_3_ID, folderResponse2.getDataId());
        final FetchDataResponse readFolder2Response = readService.perform(readFolder2Request);
        assertTrue(readFolder2Response.isOk());
        assertEquals(1, readFolder2Response.getMetadata().size());
        assertEquals(dataResponse2.getDataId(), readFolder2Response.getMetadata().get(0).getDataId());

        // Step 4, Move the data from folder 1 to folder 2.
        final ProcessDataRequest moveRequest = prepareUpdateRequest(MEMBER_4, CIRCLE_3_ID);
        moveRequest.setDataId(dataResponse1.getDataId());
        moveRequest.setFolderId(folderResponse2.getDataId());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(moveRequest));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("The name provided is already being used in the given folder.", cause.getMessage());
    }

    @Test
    void testMoveDataToDifferentCircleWithAddRequest() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest addFolderRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "folder1", 0);
        addFolderRequest.setTypeName("folder");
        final ProcessDataResponse addFolderResponse = service.perform(addFolderRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addFolderResponse.getReturnCode());
        assertEquals("Ok", addFolderResponse.getReturnMessage());
        assertNotNull(addFolderResponse.getDataId());

        final ProcessDataRequest addDataRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, "my data", 512);
        final ProcessDataResponse addDataResponse = service.perform(addDataRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), addDataResponse.getReturnCode());
        assertEquals("Ok", addDataResponse.getReturnMessage());
        assertNotNull(addDataResponse.getDataId());

        final ProcessDataRequest moveDataRequest = prepareUpdateRequest(MEMBER_1, addDataResponse.getDataId());
        moveDataRequest.setFolderId(addFolderResponse.getDataId());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(moveDataRequest));
        assertEquals(ReturnCode.ILLEGAL_ACTION, cause.getReturnCode());
        assertEquals("Moving Data from one Circle to another is not permitted.", cause.getMessage());
    }

    /**
     * <p>See <a href="https://github.com/JavaDogs/cws/issues/57">Github</a>.</p>
     *
     * <p>From the stacktrace with a build using a snapshot from 2019-07-24:</p>
     * <pre>
     * Caused by: java.lang.NullPointerException
     *         at deployment.cws.war//io.javadog.cws.core.services.ProcessDataService.processAddData(ProcessDataService.java:98)
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
        final Query query = entityManager.createQuery("delete from MetadataEntity where circle.id = :cid");
        query.setParameter("cid", 1L);
        query.executeUpdate();

        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "My Object", 1024);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.INTEGRITY_ERROR, cause.getReturnCode());
        assertTrue(cause.getMessage().contains("No Parent could be found for the Circle"));
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
