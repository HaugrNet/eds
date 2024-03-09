/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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
package net.haugr.eds.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.FetchMemberRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Disabled("Upgrading to Jakarta EE 10 requires a re-write of the Endpoint tests")
final class MemberServiceTest extends BeanSetup {

    @Test
    void testCreate() {
        final MemberService service = prepareMemberService(settings, entityManager);
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.create(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedCreate() {
        final MemberService service = prepareMemberService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.create(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testInvite() {
        final MemberService service = prepareMemberService(settings, entityManager);
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.invite(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedInvite() {
        final MemberService service = prepareMemberService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.invite(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testLogin() {
        final MemberService service = prepareMemberService(settings, entityManager);
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.login(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedLogin() {
        final MemberService service = prepareMemberService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.login(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testLogout() {
        final MemberService service = prepareMemberService(settings, entityManager);
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.logout(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedLogout() {
        final MemberService service = prepareMemberService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.logout(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testAlter() {
        final MemberService service = prepareMemberService(settings, entityManager);
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.alter(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedAlter() {
        final MemberService service = prepareMemberService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.alter(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testUpdate() {
        final MemberService service = prepareMemberService(settings, entityManager);
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.update(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedUpdate() {
        final MemberService service = prepareMemberService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.update(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testInvalidate() {
        final MemberService service = prepareMemberService(settings, entityManager);
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.invalidate(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedInvalidate() {
        final MemberService service = prepareMemberService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.invalidate(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testDelete() {
        final MemberService service = prepareMemberService(settings, entityManager);
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.delete(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedDelete() {
        final MemberService service = prepareMemberService();
        final ProcessMemberRequest request = new ProcessMemberRequest();

        final Response response = service.delete(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFetch() {
        final MemberService service = prepareMemberService(settings, entityManager);
        final FetchMemberRequest request = new FetchMemberRequest();

        final Response response = service.fetch(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }

    @Test
    void testFlawedFetch() {
        final MemberService service = prepareMemberService();
        final FetchMemberRequest request = new FetchMemberRequest();

        final Response response = service.fetch(request);
        assertEquals(ReturnCode.SUCCESS.getHttpCode(), response.getStatus());
    }
}
