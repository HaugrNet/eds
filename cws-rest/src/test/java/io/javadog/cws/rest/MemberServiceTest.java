/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2020, JavaDog.io
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
package io.javadog.cws.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.ManagementBean;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class MemberServiceTest extends DatabaseSetup {

    @Test
    void testCreate() {
        final MemberService service = prepareService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.create(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedCreate() {
        final MemberService service = prepareFlawedService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.create(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testInvite() {
        final MemberService service = prepareService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.invite(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedInvite() {
        final MemberService service = prepareFlawedService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.invite(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testLogin() {
        final MemberService service = prepareService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.login(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedLogin() {
        final MemberService service = prepareFlawedService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.login(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testLogout() {
        final MemberService service = prepareService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.logout(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedLogout() {
        final MemberService service = prepareFlawedService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.logout(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testAlter() {
        final MemberService service = prepareService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.alter(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedAlter() {
        final MemberService service = prepareFlawedService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.alter(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testUpdate() {
        final MemberService service = prepareService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.update(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedUpdate() {
        final MemberService service = prepareFlawedService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.update(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testInvalidate() {
        final MemberService service = prepareService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.invalidate(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedInvalidate() {
        final MemberService service = prepareFlawedService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.invalidate(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testDelete() {
        final MemberService service = prepareService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.delete(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedDelete() {
        final MemberService service = prepareFlawedService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.delete(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFetch() {
        final MemberService service = prepareService();
        final FetchMemberRequest request = new FetchMemberRequest();

        final Response response = service.fetch(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedFetch() {
        final MemberService service = prepareFlawedService();
        final FetchMemberRequest request = new FetchMemberRequest();

        final Response response = service.fetch(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    // =========================================================================
    // Internal Test Setup Methods
    // =========================================================================

    private static MemberService prepareFlawedService() {
        final MemberService service = instantiate(MemberService.class);
        setField(service, "bean", null);

        return service;
    }

    private MemberService prepareService() {
        final ManagementBean bean = instantiate(ManagementBean.class);
        setField(bean, "entityManager", entityManager);

        final MemberService service = instantiate(MemberService.class);
        setField(service, "bean", bean);

        return service;
    }
}
