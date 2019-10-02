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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.ProcessTrusteeResponse;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.Settings;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * <p>Common test class for the Process & Fetch Circle Services.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class CircleServiceTest extends DatabaseSetup {

    @Test
    void testEmptyFetchRequest() {
        final FetchCircleService service = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest request = new FetchCircleRequest();
        // Just making sure that the account is missing
        assertNull(request.getAccountName());

        // Should throw a VerificationException, as the request is invalid.
        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing.", cause.getMessage());
    }

    @Test
    void testEmptyProcessRequest() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = new ProcessCircleRequest();
        // Just making sure that the account is missing
        assertNull(request.getAccountName());

        // Should throw a VerificationException, as the request is invalid.
        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing." +
                "\nKey: action, Error: No action has been provided.", cause.getMessage());
    }

    @Test
    void testCreateAndReadCircle() {
        final ProcessCircleService processService = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest createRequest = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        createRequest.setAction(Action.CREATE);
        createRequest.setMemberId(MEMBER_5_ID);
        createRequest.setCircleName("One");

        final ProcessCircleResponse createResponse = processService.perform(createRequest);
        assertTrue(createResponse.isOk());

        final byte[] bytes = generateData(512);
        final String data = crypto.bytesToString(bytes);
        final ProcessDataService processDataService = new ProcessDataService(settings, entityManager);
        final ProcessDataRequest addRequest = prepareRequest(ProcessDataRequest.class, MEMBER_5);
        addRequest.setAction(Action.ADD);
        addRequest.setCircleId(createResponse.getCircleId());
        addRequest.setDataName("My Data Object");
        addRequest.setData(bytes);
        final ProcessDataResponse processDataResponse = processDataService.perform(addRequest);
        assertTrue(processDataResponse.isOk());

        // Read root folder for the Circle
        final FetchDataService dataService = new FetchDataService(settings, entityManager);
        final FetchDataRequest dataRootRequest = prepareRequest(FetchDataRequest.class, MEMBER_5);
        dataRootRequest.setCircleId(createResponse.getCircleId());
        final FetchDataResponse dataRootResponse = dataService.perform(dataRootRequest);
        assertTrue(dataRootResponse.isOk());
        assertEquals(1, dataRootResponse.getMetadata().size());
        assertEquals("My Data Object", dataRootResponse.getMetadata().get(0).getDataName());

        // Read the newly created Data Object
        final FetchDataRequest dataFileRequest = prepareRequest(FetchDataRequest.class, MEMBER_5);
        dataFileRequest.setDataId(dataRootResponse.getMetadata().get(0).getDataId());
        final FetchDataResponse dataFileResponse = dataService.perform(dataFileRequest);
        assertTrue(dataFileResponse.isOk());
        assertEquals(1, dataFileResponse.getMetadata().size());
        assertEquals("My Data Object", dataFileResponse.getMetadata().get(0).getDataName());
        assertEquals(data, crypto.bytesToString(dataFileResponse.getData()));
    }

    @Test
    void testFetchAllCirclesAsAdminWithShowCirclesTrue() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_CIRCLES.getKey(), "true");
        final FetchCircleService service = new FetchCircleService(mySettings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchCircleResponse response = service.perform(request);

        assertTrue(response.isOk());
        detailedCircleAssertion(response, CIRCLE_1, CIRCLE_2, CIRCLE_3);
    }

    @Test
    void testFetchAllCirclesAsAdminWithShowCirclesFalse() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_CIRCLES.getKey(), "false");
        final FetchCircleService service = new FetchCircleService(mySettings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchCircleResponse response = service.perform(request);

        assertTrue(response.isOk());
        detailedCircleAssertion(response, CIRCLE_1, CIRCLE_2, CIRCLE_3);
    }

    @Test
    void testFetchAllCirclesAsMember1WithShowCirclesTrue() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_CIRCLES.getKey(), "true");
        final FetchCircleService service = new FetchCircleService(mySettings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_1);
        final FetchCircleResponse response = service.perform(request);

        assertTrue(response.isOk());
        detailedCircleAssertion(response, CIRCLE_1, CIRCLE_2, CIRCLE_3);
    }

    @Test
    void testFetchAllCirclesAsMember1WithShowCirclesFalse() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_CIRCLES.getKey(), "false");
        final FetchCircleService service = new FetchCircleService(mySettings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_1);
        final FetchCircleResponse response = service.perform(request);

        assertTrue(response.isOk());
        detailedCircleAssertion(response, CIRCLE_1, CIRCLE_2);
    }

    @Test
    void testFetchAllCirclesAsMember5WithShowCirclesFalse() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_CIRCLES.getKey(), "false");
        final FetchCircleService service = new FetchCircleService(mySettings, entityManager);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_5);
        final FetchCircleResponse response = service.perform(request);

        assertTrue(response.isOk());
        detailedCircleAssertion(response, CIRCLE_3);
    }

    @Test
    void testCreateCircle() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setMemberId(MEMBER_1_ID);
        request.setCircleName("a circle");

        final ProcessCircleResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertNotNull(response.getCircleId());

        final FetchCircleService fetchService = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest fetchRequest = prepareRequest(FetchCircleRequest.class, MEMBER_1);
        final FetchCircleResponse fetchResponse = fetchService.perform(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals(4, fetchResponse.getCircles().size());
        // Circles are sorted by name, so our newly created Circle will be the first
        assertEquals(response.getCircleId(), fetchResponse.getCircles().get(0).getCircleId());
        assertEquals("a circle", fetchResponse.getCircles().get(0).getCircleName());
    }

    @Test
    void testCreateCircleAsMember() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.CREATE);
        request.setCircleName("My Circle");
        request.setMemberId(MEMBER_1_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testCreateCircleAsNewUser() {
        final ProcessMemberService memberService = new ProcessMemberService(settings, entityManager);
        final ProcessCircleService circleService = new ProcessCircleService(settings, entityManager);
        final ProcessTrusteeService trusteeService = new ProcessTrusteeService(settings, entityManager);
        final String newUser = "newUser";

        final ProcessMemberRequest newMemberRequest = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        newMemberRequest.setAction(Action.CREATE);
        newMemberRequest.setNewAccountName(newUser);
        newMemberRequest.setNewCredential(crypto.stringToBytes(newUser));
        final ProcessMemberResponse newMemberResponse = memberService.perform(newMemberRequest);
        assertEquals("Ok", newMemberResponse.getReturnMessage());

        final ProcessCircleRequest newCircleRequest = prepareRequest(ProcessCircleRequest.class, newUser);
        newCircleRequest.setAction(Action.CREATE);
        newCircleRequest.setCircleName("new Circle");
        final ProcessCircleResponse newCircleResponse = circleService.perform(newCircleRequest);
        assertEquals("Ok", newCircleResponse.getReturnMessage());

        final ProcessTrusteeRequest newTrusteeRequest = prepareRequest(ProcessTrusteeRequest.class, newUser);
        newTrusteeRequest.setAction(Action.ADD);
        newTrusteeRequest.setCircleId(newCircleResponse.getCircleId());
        newTrusteeRequest.setMemberId(MEMBER_5_ID);
        newTrusteeRequest.setTrustLevel(TrustLevel.WRITE);
        final ProcessTrusteeResponse newTrusteeResponse = trusteeService.perform(newTrusteeRequest);
        assertEquals("Ok", newTrusteeResponse.getReturnMessage());
    }

    @Test
    void testCreateCircleWithExternalCircleKey() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest createRequest = prepareRequest(ProcessCircleRequest.class, MEMBER_5);
        createRequest.setAction(Action.CREATE);
        createRequest.setCircleName("Extra Encrypted");
        createRequest.setCircleKey(UUID.randomUUID().toString());

        final ProcessCircleResponse createResponse = service.perform(createRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), createResponse.getReturnCode());
        assertNotNull(createResponse.getCircleId());

        final ProcessCircleRequest updateRequest = prepareRequest(ProcessCircleRequest.class, MEMBER_5);
        updateRequest.setCredential(crypto.stringToBytes(MEMBER_5));
        updateRequest.setAction(Action.UPDATE);
        updateRequest.setCircleId(createResponse.getCircleId());
        updateRequest.setCircleKey(UUID.randomUUID().toString());
        final ProcessCircleResponse updateResponse = service.perform(updateRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), updateResponse.getReturnCode());

        final FetchCircleService fetchService = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest fetchRequest = prepareRequest(FetchCircleRequest.class, MEMBER_5);
        final FetchCircleResponse fetchResponse = fetchService.perform(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals(4, fetchResponse.getCircles().size());
        // Sorting alphabetically on lowercase names should reveal a correct
        // sorting where the list is as follows:
        //  * circle1
        //  * circle2
        //  * circle3
        //  * Extra Encrypted
        // Note, that it could be considered a bug that the list earlier was
        // sorted with uppercase letters before lowercase letters, thus 'Z' came
        // before 'a'. With the case insensitive indexes, it should be fixed.
        assertEquals(createResponse.getCircleId(), fetchResponse.getCircles().get(3).getCircleId());
        assertEquals(updateRequest.getCircleKey(), fetchResponse.getCircles().get(3).getCircleKey());
    }

    @Test
    void testCreateCircleWithInvalidCircleAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setMemberId(UUID.randomUUID().toString());
        request.setCircleName("My Circle");
        assertTrue(request.validate().isEmpty());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Cannot create a new Circle with a non-existing Circle Administrator.", cause.getMessage());
    }

    @Test
    void testCreateCircleWithSystemAdminAsCircleAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setMemberId(ADMIN_ID);
        request.setCircleName("My Circle");

        final ProcessCircleResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testCreateCircleWithExistingName() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setCircleName(CIRCLE_1);
        request.setMemberId(MEMBER_1_ID);
        assertTrue(request.validate().isEmpty());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("A Circle with the requested name already exists.", cause.getMessage());
    }

    @Test
    void testUpdateExistingCircleAsAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.UPDATE);
        request.setCircleName("Circle One");
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());

        final FetchCircleService fetchService = new FetchCircleService(settings, entityManager);
        final FetchCircleRequest fetchRequest = prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchCircleResponse fetchResponse = fetchService.perform(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals(3, fetchResponse.getCircles().size());
        assertEquals(CIRCLE_1_ID, fetchResponse.getCircles().get(0).getCircleId());
        assertEquals("Circle One", fetchResponse.getCircles().get(0).getCircleName());
    }

    @Test
    void testUpdateExistingCircleAsCircleAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setCircleName("Circle One");
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testUpdateExistingCircleAsCircleMember() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_2);
        request.setAction(Action.UPDATE);
        request.setCircleName("Circle One");
        request.setCircleId(CIRCLE_1_ID);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("Only a Circle Administrator may perform this action.", cause.getMessage());
    }

    @Test
    void testUpdateCircleAsNonMember() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_5);
        request.setAction(Action.UPDATE);
        request.setCircleName("Circle One");
        request.setCircleId(CIRCLE_1_ID);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("No Trustee information found for member 'member5' and circle '" + CIRCLE_1_ID + "'.", cause.getMessage());
    }

    @Test
    void testUpdateNonExistingCircle() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.UPDATE);
        request.setCircleName("Circle One");
        request.setCircleId(UUID.randomUUID().toString());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("No Circle could be found with the given Id.", cause.getMessage());
    }

    @Test
    void testUpdateExistingCircleWithExistingName() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setCircleName(CIRCLE_2);
        request.setCircleId(CIRCLE_1_ID);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("A Circle with the requested name already exists.", cause.getMessage());
    }

    @Test
    void testUpdateExistingCircleWithOwnName() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setCircleName(CIRCLE_1);
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testDeleteCircleAsAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testDeleteCircleAsCircleMember() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_2);
        request.setAction(Action.DELETE);
        request.setCircleId(CIRCLE_1_ID);

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.AUTHORIZATION_WARNING, cause.getReturnCode());
        assertEquals("Only a Circle Administrator may perform this action.", cause.getMessage());
    }

    @Test
    void testDeleteCircleAsCircleAdmin() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.DELETE);
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
    }

    @Test
    void testDeleteNotExistingCircle() {
        final ProcessCircleService service = new ProcessCircleService(settings, entityManager);
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        request.setCircleId(UUID.randomUUID().toString());

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.IDENTIFICATION_WARNING, cause.getReturnCode());
        assertEquals("No Circle could be found with the given Id.", cause.getMessage());
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private static void detailedCircleAssertion(final FetchCircleResponse response, final String... circleNames) {
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());

        if ((circleNames != null) && (circleNames.length > 0)) {
            assertEquals(circleNames.length, response.getCircles().size());
            for (int i = 0; i < circleNames.length; i++) {
                assertEquals(circleNames[i], response.getCircles().get(i).getCircleName());
            }
        } else {
            assertTrue(response.getCircles().isEmpty());
        }
    }
}
