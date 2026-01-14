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
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.responses.AuthenticateResponse;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>Tests for rate-limiting functionality (GitHub Issue #68).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class ManagementBeanRateLimitTest extends DatabaseSetup {

    /**
     * Test that authentication works normally when under the rate limit.
     */
    @Test
    void testAuthenticationUnderRateLimit() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.LOGIN_RETRY_LIMIT.getKey(), "5");
        mySettings.set(StandardSetting.LOGIN_RETRY_WINDOW_MINUTES.getKey(), "15");
        final ManagementBean bean = prepareManagementBean(mySettings);

        // Attempt authentication with correct credentials - should succeed
        final Authentication request = prepareRequest(Authentication.class, MEMBER_1);
        final AuthenticateResponse response = bean.authenticated(request);

        assertTrue(response.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
    }

    /**
     * Test that authentication is blocked after exceeding the rate limit.
     */
    @Test
    void testAuthenticationBlockedAfterExceedingRateLimit() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.LOGIN_RETRY_LIMIT.getKey(), "3");
        mySettings.set(StandardSetting.LOGIN_RETRY_WINDOW_MINUTES.getKey(), "15");
        final ManagementBean bean = prepareManagementBean(mySettings);

        // Make 3 failed authentication attempts (reaching the limit)
        for (int i = 0; i < 3; i++) {
            final Authentication badRequest = prepareRequest(Authentication.class, MEMBER_1);
            badRequest.setCredential(crypto.stringToBytes("wrong_password_" + i));
            final AuthenticateResponse failedResponse = bean.authenticated(badRequest);
            assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), failedResponse.getReturnCode());
        }

        // Now even a correct password should be blocked
        final Authentication correctRequest = prepareRequest(Authentication.class, MEMBER_1);
        final AuthenticateResponse blockedResponse = bean.authenticated(correctRequest);

        assertEquals(ReturnCode.AUTHENTICATION_BLOCKED.getCode(), blockedResponse.getReturnCode());
        assertTrue(blockedResponse.getReturnMessage().contains("temporarily blocked"));
    }

    /**
     * Test that failed attempts just under the limit still allow authentication.
     */
    @Test
    void testAuthenticationSucceedsJustUnderRateLimit() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.LOGIN_RETRY_LIMIT.getKey(), "3");
        mySettings.set(StandardSetting.LOGIN_RETRY_WINDOW_MINUTES.getKey(), "15");
        final ManagementBean bean = prepareManagementBean(mySettings);

        // Make 2 failed attempts (under the limit of 3)
        for (int i = 0; i < 2; i++) {
            final Authentication badRequest = prepareRequest(Authentication.class, MEMBER_1);
            badRequest.setCredential(crypto.stringToBytes("wrong_password_" + i));
            final AuthenticateResponse failedResponse = bean.authenticated(badRequest);
            assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), failedResponse.getReturnCode());
        }

        // Now a correct password should still work
        final Authentication correctRequest = prepareRequest(Authentication.class, MEMBER_1);
        final AuthenticateResponse successResponse = bean.authenticated(correctRequest);

        assertTrue(successResponse.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), successResponse.getReturnCode());
    }

    /**
     * Test that successful authentication clears failed attempt history.
     */
    @Test
    void testSuccessfulAuthenticationClearsFailedAttempts() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.LOGIN_RETRY_LIMIT.getKey(), "3");
        mySettings.set(StandardSetting.LOGIN_RETRY_WINDOW_MINUTES.getKey(), "15");
        final ManagementBean bean = prepareManagementBean(mySettings);

        // Make 2 failed attempts
        for (int i = 0; i < 2; i++) {
            final Authentication badRequest = prepareRequest(Authentication.class, MEMBER_1);
            badRequest.setCredential(crypto.stringToBytes("wrong_password_" + i));
            bean.authenticated(badRequest);
        }

        // Successful login should clear the failed attempts
        final Authentication correctRequest = prepareRequest(Authentication.class, MEMBER_1);
        final AuthenticateResponse successResponse = bean.authenticated(correctRequest);
        assertTrue(successResponse.isOk());

        // Now make 2 more failed attempts (should be allowed since counter was reset)
        for (int i = 0; i < 2; i++) {
            final Authentication badRequest = prepareRequest(Authentication.class, MEMBER_1);
            badRequest.setCredential(crypto.stringToBytes("another_wrong_password_" + i));
            final AuthenticateResponse failedResponse = bean.authenticated(badRequest);
            // Should still be AUTHENTICATION_WARNING, not BLOCKED
            assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), failedResponse.getReturnCode());
        }

        // And correct credentials should still work
        final Authentication finalRequest = prepareRequest(Authentication.class, MEMBER_1);
        final AuthenticateResponse finalResponse = bean.authenticated(finalRequest);
        assertTrue(finalResponse.isOk());
    }

    /**
     * Test that rate limiting is per-account (different accounts tracked separately).
     */
    @Test
    void testRateLimitingIsPerAccount() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.LOGIN_RETRY_LIMIT.getKey(), "2");
        mySettings.set(StandardSetting.LOGIN_RETRY_WINDOW_MINUTES.getKey(), "15");
        final ManagementBean bean = prepareManagementBean(mySettings);

        // Exceed rate limit for MEMBER_1
        for (int i = 0; i < 2; i++) {
            final Authentication badRequest = prepareRequest(Authentication.class, MEMBER_1);
            badRequest.setCredential(crypto.stringToBytes("wrong_password"));
            bean.authenticated(badRequest);
        }

        // MEMBER_1 should now be blocked
        final Authentication member1Request = prepareRequest(Authentication.class, MEMBER_1);
        final AuthenticateResponse member1Response = bean.authenticated(member1Request);
        assertEquals(ReturnCode.AUTHENTICATION_BLOCKED.getCode(), member1Response.getReturnCode());

        // But MEMBER_2 should still be able to authenticate
        final Authentication member2Request = prepareRequest(Authentication.class, MEMBER_2);
        final AuthenticateResponse member2Response = bean.authenticated(member2Request);
        assertTrue(member2Response.isOk());
        assertEquals(ReturnCode.SUCCESS.getCode(), member2Response.getReturnCode());
    }

    /**
     * Test rate limiting with custom settings (single retry allowed).
     */
    @Test
    void testRateLimitingWithSingleRetry() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.LOGIN_RETRY_LIMIT.getKey(), "1");
        mySettings.set(StandardSetting.LOGIN_RETRY_WINDOW_MINUTES.getKey(), "15");
        final ManagementBean bean = prepareManagementBean(mySettings);

        // Single failed attempt should trigger block
        final Authentication badRequest = prepareRequest(Authentication.class, MEMBER_3);
        badRequest.setCredential(crypto.stringToBytes("wrong"));
        final AuthenticateResponse failedResponse = bean.authenticated(badRequest);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), failedResponse.getReturnCode());

        // Account should now be blocked
        final Authentication correctRequest = prepareRequest(Authentication.class, MEMBER_3);
        final AuthenticateResponse blockedResponse = bean.authenticated(correctRequest);
        assertEquals(ReturnCode.AUTHENTICATION_BLOCKED.getCode(), blockedResponse.getReturnCode());
    }
}
