/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.event.Event;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.requests.FetchCircleRequest;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.requests.ProcessTrusteeRequest;
import net.haugr.eds.api.responses.FetchCircleResponse;
import net.haugr.eds.api.responses.FetchDataResponse;
import net.haugr.eds.api.responses.ProcessCircleResponse;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.api.responses.ProcessTrusteeResponse;
import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.rotation.CircleRotations;
import net.haugr.eds.core.socket.AccessChange;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * <p>Common test class for the Process & Fetch Circle Services.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class ManagementBeanCircleTest extends DatabaseSetup {

    @Test
    void testOnlyASuccessfulChangeIsAnnounced() {
        final List<Object> announced = new ArrayList<>();
        final ManagementBean bean = new ManagementBean(entityManager, settings, null, announcer(announced));

        final ProcessCircleRequest create = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        create.setAction(Action.CREATE);
        create.setCircleName("announced");
        final ProcessCircleResponse created = bean.processCircle(create);
        assertTrue(created.isOk(), created.getReturnMessage());
        // A new Circle ends no access, so the socket isn't told (ticket 18).
        assertEquals(List.of(), announced);

        // A change of it names it, so only its connections are checked.
        final ProcessCircleRequest update = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        update.setAction(Action.UPDATE);
        update.setCircleId(created.getCircleId());
        update.setCircleName("announced again");
        final ProcessCircleResponse updated = bean.processCircle(update);
        assertTrue(updated.isOk(), updated.getReturnMessage());
        assertEquals(List.of(AccessChange.of(created.getCircleId())), announced);

        // The name of another Circle fails, and isn't announced.
        final ProcessCircleRequest again = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        again.setAction(Action.UPDATE);
        again.setCircleId(created.getCircleId());
        again.setCircleName(CIRCLE_2);
        assertFalse(bean.processCircle(again).isOk());
        assertEquals(1, announced.size());
    }

    @Test
    void testEachChangeNamesWhatItMayEnd() {
        final List<Object> announced = new ArrayList<>();
        final ManagementBean bean = new ManagementBean(entityManager, settings, null, announcer(announced));

        // A new account ends no access.
        final ProcessMemberRequest create = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        create.setAction(Action.CREATE);
        create.setNewAccountName("announced member");
        create.setNewCredential(credentialOf("announced member"));
        assertTrue(bean.processMember(create).isOk());
        assertEquals(List.of(), announced);

        // A login may end another Session of the account: everyone.
        final ProcessMemberRequest login = prepareRequest(ProcessMemberRequest.class, MEMBER_2);
        login.setAction(Action.LOGIN);
        assertTrue(bean.processMember(login).isOk());
        assertEquals(List.of(AccessChange.EVERYONE), announced);

        // A change of a Trustee names its Circle.
        final ProcessTrusteeRequest alter = prepareRequest(ProcessTrusteeRequest.class, MEMBER_1);
        alter.setAction(Action.ALTER);
        alter.setCircleId(CIRCLE_1_ID);
        alter.setMemberId(MEMBER_2_ID);
        alter.setTrustLevel(TrustLevel.READ);
        assertTrue(bean.processTrustee(alter).isOk());
        assertEquals(List.of(AccessChange.EVERYONE, AccessChange.of(CIRCLE_1_ID)), announced);
    }

    @Test
    void testADeletedCircleIsNoLongerWaitedFor() {
        // Ticket 2: a rotation put off for a Circle is forgotten with it.
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest create = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        create.setAction(Action.CREATE);
        create.setCircleName("put off");
        final ProcessCircleResponse created = bean.processCircle(create);
        assertTrue(created.isOk(), created.getReturnMessage());
        final long circle = dao.find(CircleEntity.class, created.getCircleId()).getId();
        CircleRotations.retryLater(circle);
        try {
            assertTrue(CircleRotations.isWaiting(circle));
            final ProcessCircleRequest delete = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
            delete.setAction(Action.DELETE);
            delete.setCircleId(created.getCircleId());
            assertTrue(bean.processCircle(delete).isOk());
            assertFalse(CircleRotations.isWaiting(circle));
        } finally {
            CircleRotations.forget(circle);
        }
    }

    @Test
    void testEmptyFetchRequest() {
        final ManagementBean bean = prepareManagementBean();
        final FetchCircleRequest request = new FetchCircleRequest();
        // Just making sure that the account is missing
        assertNull(request.getAccountName());

        // Should throw a VerificationException, as the request is invalid.
        final FetchCircleResponse response = bean.fetchCircles(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing.", response.getReturnMessage());
    }

    @Test
    void testEmptyProcessRequest() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest request = new ProcessCircleRequest();
        // Just making sure that the account is missing
        assertNull(request.getAccountName());

        // Should throw a VerificationException, as the request is invalid.
        final ProcessCircleResponse response = bean.processCircle(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("""
                Request Object contained errors:
                Key: credential, Error: The Session (Credential) is missing.
                Key: action, Error: No action has been provided.""", response.getReturnMessage());
    }

    @Test
    void testCreateAndReadCircle() {
        final ManagementBean bean = prepareManagementBean();
        final ShareBean shareBean = prepareShareBean();
        final ProcessCircleRequest createRequest = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        createRequest.setAction(Action.CREATE);
        createRequest.setMemberId(MEMBER_5_ID);
        createRequest.setCircleName("One");

        final ProcessCircleResponse createResponse = bean.processCircle(createRequest);
        assertTrue(createResponse.isOk());

        final byte[] bytes = generateData(512);
        final String data = crypto.bytesToString(bytes);
        final ProcessDataRequest addRequest = prepareRequest(ProcessDataRequest.class, MEMBER_5);
        addRequest.setAction(Action.ADD);
        addRequest.setCircleId(createResponse.getCircleId());
        addRequest.setDataName("My Data Object");
        addRequest.setData(bytes);
        final ProcessDataResponse processDataResponse = shareBean.processData(addRequest);
        assertTrue(processDataResponse.isOk());

        // Read the root folder for the Circle
        final FetchDataRequest dataRootRequest = prepareRequest(FetchDataRequest.class, MEMBER_5);
        dataRootRequest.setCircleId(createResponse.getCircleId());
        final FetchDataResponse dataRootResponse = shareBean.fetchData(dataRootRequest);
        assertTrue(dataRootResponse.isOk());
        assertEquals(1, dataRootResponse.getMetadata().size());
        assertEquals("My Data Object", dataRootResponse.getMetadata().getFirst().getDataName());

        // Read the newly created Data Object
        final FetchDataRequest dataFileRequest = prepareRequest(FetchDataRequest.class, MEMBER_5);
        dataFileRequest.setDataId(dataRootResponse.getMetadata().getFirst().getDataId());
        final FetchDataResponse dataFileResponse = shareBean.fetchData(dataFileRequest);
        assertTrue(dataFileResponse.isOk());
        assertEquals(1, dataFileResponse.getMetadata().size());
        assertEquals("My Data Object", dataFileResponse.getMetadata().getFirst().getDataName());
        assertEquals(data, crypto.bytesToString(dataFileResponse.getData()));
    }

    @Test
    void testFetchAllCirclesAsAdminWithShowCirclesTrue() {
        final Settings mySettings = newSettings();
        final ManagementBean bean = prepareManagementBean();
        mySettings.set(StandardSetting.SHOW_CIRCLES.getKey(), "true");
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchCircleResponse response = bean.fetchCircles(request);

        assertTrue(response.isOk());
        detailedCircleAssertion(response, CIRCLE_1, CIRCLE_2, CIRCLE_3);
    }

    @Test
    void testFetchAllCirclesAsAdminWithShowCirclesFalse() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_CIRCLES.getKey(), "false");
        final ManagementBean bean = prepareManagementBean();
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchCircleResponse response = bean.fetchCircles(request);

        assertTrue(response.isOk());
        detailedCircleAssertion(response, CIRCLE_1, CIRCLE_2, CIRCLE_3);
    }

    @Test
    void testFetchAllCirclesAsMember1WithShowCirclesTrue() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_CIRCLES.getKey(), "true");
        final ManagementBean bean = prepareManagementBean(mySettings);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_1);
        final FetchCircleResponse response = bean.fetchCircles(request);

        assertTrue(response.isOk());
        detailedCircleAssertion(response, CIRCLE_1, CIRCLE_2, CIRCLE_3);
    }

    @Test
    void testFetchAllCirclesAsMember1WithShowCirclesFalse() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_CIRCLES.getKey(), "false");
        final ManagementBean bean = prepareManagementBean(mySettings);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_1);
        final FetchCircleResponse response = bean.fetchCircles(request);

        assertTrue(response.isOk());
        detailedCircleAssertion(response, CIRCLE_1, CIRCLE_2);
    }

    @Test
    void testFetchAllCirclesAsMember5WithShowCirclesFalse() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.SHOW_CIRCLES.getKey(), "false");
        final ManagementBean bean = prepareManagementBean(mySettings);
        final FetchCircleRequest request = prepareRequest(FetchCircleRequest.class, MEMBER_5);
        final FetchCircleResponse response = bean.fetchCircles(request);

        assertTrue(response.isOk());
        detailedCircleAssertion(response, CIRCLE_3);
    }

    @Test
    void testCreateCircle() {
        final String circleName = "A Circle";
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setMemberId(MEMBER_1_ID);
        request.setCircleName(circleName);

        final ProcessCircleResponse response = bean.processCircle(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Circle '" + circleName + "' was successfully created.", response.getReturnMessage());
        assertNotNull(response.getCircleId());

        // By default (eds.show.all.circles false), a Member gets only their
        // own Circles: circle1, circle2 and the new one.
        final FetchCircleRequest fetchRequest = prepareRequest(FetchCircleRequest.class, MEMBER_1);
        final FetchCircleResponse fetchResponse = bean.fetchCircles(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals(3, fetchResponse.getCircles().size());
        // Circles are sorted by name, so our newly created Circle will be the first
        assertEquals(response.getCircleId(), fetchResponse.getCircles().getFirst().getCircleId());
        assertEquals(circleName, fetchResponse.getCircles().getFirst().getCircleName());
    }

    @Test
    void testCreateCircleAsMember() {
        final String circleName = "My Circle";
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.CREATE);
        request.setCircleName(circleName);
        request.setMemberId(MEMBER_1_ID);

        final ProcessCircleResponse response = bean.processCircle(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Circle '" + circleName + "' was successfully created.", response.getReturnMessage());
    }

    @Test
    void testCreateCircleAsNewUser() {
        final ManagementBean bean = prepareManagementBean();
        final String newUser = "newUser";

        final ProcessMemberRequest newMemberRequest = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        newMemberRequest.setAction(Action.CREATE);
        newMemberRequest.setNewAccountName(newUser);
        newMemberRequest.setNewCredential(credentialOf(newUser));
        final ProcessMemberResponse newMemberResponse = bean.processMember(newMemberRequest);
        assertEquals("The Member '" + newUser + "' was successfully added to EDS.", newMemberResponse.getReturnMessage());

        final String circleName = "New Circle";
        final ProcessCircleRequest newCircleRequest = prepareRequest(ProcessCircleRequest.class, newUser);
        newCircleRequest.setAction(Action.CREATE);
        newCircleRequest.setCircleName(circleName);
        final ProcessCircleResponse newCircleResponse = bean.processCircle(newCircleRequest);
        assertEquals("The Circle '" + circleName + "' was successfully created.", newCircleResponse.getReturnMessage());

        final ProcessTrusteeRequest newTrusteeRequest = prepareRequest(ProcessTrusteeRequest.class, newUser);
        newTrusteeRequest.setAction(Action.ADD);
        newTrusteeRequest.setCircleId(newCircleResponse.getCircleId());
        newTrusteeRequest.setMemberId(MEMBER_5_ID);
        newTrusteeRequest.setTrustLevel(TrustLevel.WRITE);
        final ProcessTrusteeResponse newTrusteeResponse = bean.processTrustee(newTrusteeRequest);
        assertEquals("The Member '" + MEMBER_5 + "' was successfully added as trustee to '" + circleName + "'.", newTrusteeResponse.getReturnMessage());
    }

    @Test
    void testCreateCircleWithExternalCircleKey() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest createRequest = prepareRequest(ProcessCircleRequest.class, MEMBER_5);
        createRequest.setAction(Action.CREATE);
        createRequest.setCircleName("Extra Encrypted");
        createRequest.setCircleKey(UUID.randomUUID().toString());

        final ProcessCircleResponse createResponse = bean.processCircle(createRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), createResponse.getReturnCode());
        assertNotNull(createResponse.getCircleId());

        final ProcessCircleRequest updateRequest = prepareRequest(ProcessCircleRequest.class, MEMBER_5);
        updateRequest.setCredential(credentialOf(MEMBER_5));
        updateRequest.setAction(Action.UPDATE);
        updateRequest.setCircleId(createResponse.getCircleId());
        updateRequest.setCircleKey(UUID.randomUUID().toString());
        final ProcessCircleResponse updateResponse = bean.processCircle(updateRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), updateResponse.getReturnCode());

        // member5's own Circles only: circle3 and the new one.
        final FetchCircleRequest fetchRequest = prepareRequest(FetchCircleRequest.class, MEMBER_5);
        final FetchCircleResponse fetchResponse = bean.fetchCircles(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals(2, fetchResponse.getCircles().size());
        // Sorting alphabetically on lowercase names should reveal a correct
        // sorting where the list is as follows:
        //  * circle3
        //  * Extra Encrypted
        // Note, that it could be considered a bug that the list earlier was
        // sorted with uppercase letters before lowercase letters, thus 'Z' came
        // before 'a'. The queries sort on lower(name), so the order is the same
        // for all databases, regardless of their collation.
        assertEquals(createResponse.getCircleId(), fetchResponse.getCircles().get(1).getCircleId());
        assertEquals(updateRequest.getCircleKey(), fetchResponse.getCircles().get(1).getCircleKey());
    }

    @Test
    void testCreateCircleWithInvalidCircleAdmin() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setMemberId(UUID.randomUUID().toString());
        request.setCircleName("My Circle");
        assertTrue(request.validate().isEmpty());

        final ProcessCircleResponse response = bean.processCircle(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Cannot create a new Circle with a non-existing Circle Administrator.", response.getReturnMessage());
    }

    @Test
    void testCreateCircleWithSystemAdminAsCircleAdmin() {
        final String circleName = "My circle";
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setMemberId(ADMIN_ID);
        request.setCircleName(circleName);

        final ProcessCircleResponse response = bean.processCircle(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Circle '" + circleName + "' was successfully created.", response.getReturnMessage());
    }

    @Test
    void testCreateCircleWithExistingName() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.CREATE);
        request.setCircleName(CIRCLE_1);
        request.setMemberId(MEMBER_1_ID);
        assertTrue(request.validate().isEmpty());

        final ProcessCircleResponse response = bean.processCircle(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("A Circle with the requested name already exists.", response.getReturnMessage());
    }

    @Test
    void testUpdateExistingCircleAsAdmin() {
        final String circleName = "Circle One";
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.UPDATE);
        request.setCircleName(circleName);
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = bean.processCircle(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Circle '" + circleName + "' was successfully updated.", response.getReturnMessage());

        final FetchCircleRequest fetchRequest = prepareRequest(FetchCircleRequest.class, Constants.ADMIN_ACCOUNT);
        final FetchCircleResponse fetchResponse = bean.fetchCircles(fetchRequest);
        assertEquals(ReturnCode.SUCCESS.getCode(), fetchResponse.getReturnCode());
        assertEquals(3, fetchResponse.getCircles().size());
        assertEquals(CIRCLE_1_ID, fetchResponse.getCircles().getFirst().getCircleId());
        assertEquals("Circle One", fetchResponse.getCircles().getFirst().getCircleName());
    }

    @Test
    void testUpdateExistingCircleAsCircleAdmin() {
        final String circleName = "Circle One";
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setCircleName(circleName);
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = bean.processCircle(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Circle '" + circleName + "' was successfully updated.", response.getReturnMessage());
    }

    @Test
    void testUpdateExistingCircleAsCircleMember() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_2);
        request.setAction(Action.UPDATE);
        request.setCircleName("Circle One");
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = bean.processCircle(request);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Only a Circle Administrator may perform this action.", response.getReturnMessage());
    }

    @Test
    void testUpdateCircleAsNonMember() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_5);
        request.setAction(Action.UPDATE);
        request.setCircleName("Circle One");
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = bean.processCircle(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No Trustee information found for member 'member5' and circle '" + CIRCLE_1_ID + "'.", response.getReturnMessage());
    }

    @Test
    void testUpdateNonExistingCircle() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.UPDATE);
        request.setCircleName("Circle One");
        request.setCircleId(UUID.randomUUID().toString());

        final ProcessCircleResponse response = bean.processCircle(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No Circle could be found with the given Id.", response.getReturnMessage());
    }

    @Test
    void testUpdateExistingCircleWithExistingName() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setCircleName(CIRCLE_2);
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = bean.processCircle(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("A Circle with the requested name already exists.", response.getReturnMessage());
    }

    @Test
    void testUpdateExistingCircleWithOwnName() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setCircleName(CIRCLE_1);
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = bean.processCircle(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Circle '" + CIRCLE_1 + "' was successfully updated.", response.getReturnMessage());
    }

    @Test
    void testDeleteCircleAsAdmin() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = bean.processCircle(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Circle '" + CIRCLE_1 + "' has successfully been removed from EDS.", response.getReturnMessage());
    }

    @Test
    void testDeleteCircleAsCircleMember() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_2);
        request.setAction(Action.DELETE);
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = bean.processCircle(request);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Only a Circle Administrator may perform this action.", response.getReturnMessage());
    }

    @Test
    void testDeleteCircleAsCircleAdmin() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, MEMBER_1);
        request.setAction(Action.DELETE);
        request.setCircleId(CIRCLE_1_ID);

        final ProcessCircleResponse response = bean.processCircle(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("The Circle '" + CIRCLE_1 + "' has successfully been removed from EDS.", response.getReturnMessage());
    }

    @Test
    void testDeleteNotExistingCircle() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class, Constants.ADMIN_ACCOUNT);
        request.setAction(Action.DELETE);
        request.setCircleId(UUID.randomUUID().toString());

        final ProcessCircleResponse response = bean.processCircle(request);
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("No Circle could be found with the given Id.", response.getReturnMessage());
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

    /** An Event which keeps what is fired. */
    @SuppressWarnings("unchecked")
    private static Event<AccessChange> announcer(final List<Object> announced) {
        return (Event<AccessChange>) Proxy.newProxyInstance(ManagementBeanCircleTest.class.getClassLoader(), new Class<?>[] { Event.class },
                (proxy, method, args) -> {
                    if ("fire".equals(method.getName())) {
                        announced.add(args[0]);
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
