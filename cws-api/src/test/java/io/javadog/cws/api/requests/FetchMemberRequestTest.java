/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static io.javadog.cws.api.common.Constants.FIELD_ACCOUNT_NAME;
import static io.javadog.cws.api.common.Constants.FIELD_CREDENTIAL;
import static io.javadog.cws.api.common.Constants.FIELD_MEMBER_ID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchMemberRequestTest {

    @Test
    public void testClass() {
        final String memberId = UUID.randomUUID().toString();

        final FetchMemberRequest request = new FetchMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT);
        request.setMemberId(memberId);

        assertThat(request.getMemberId(), is(memberId));
        assertThat(request.validate(), is(not(nullValue())));
        assertThat(request.validate().size(), is(0));
    }

    @Test
    public void testClassWithoutMemberId() {
        final FetchMemberRequest request = new FetchMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT);

        assertThat(request.getMemberId(), is(nullValue()));
        assertThat(request.validate(), is(not(nullValue())));
        assertThat(request.validate().size(), is(0));
    }

    @Test
    public void testClassWithForcedMemberId() throws NoSuchFieldException, IllegalAccessException {
        final String memberId = Constants.ADMIN_ACCOUNT;

        final FetchMemberRequest request = new FetchMemberRequest();
        request.setAccountName(Constants.ADMIN_ACCOUNT);
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setCredential(Constants.ADMIN_ACCOUNT);

        final Field field = request.getClass().getDeclaredField("memberId");
        field.setAccessible(true);
        field.set(request, memberId);

        final Map<String, String> errors = request.validate();

        assertThat(request.getMemberId(), is(memberId));
        assertThat(errors, is(not(nullValue())));
        assertThat(errors.size(), is(1));
        assertThat(errors.get(FIELD_MEMBER_ID), is("The Member Id is invalid."));
    }

    @Test
    public void testEmptyClass() {
        final FetchMemberRequest request = new FetchMemberRequest();
        final Map<String, String> errors = request.validate();

        assertThat(request.getMemberId(), is(nullValue()));
        assertThat(errors, is(not(nullValue())));
        assertThat(errors.size(), is(2));
        assertThat(errors.get(FIELD_ACCOUNT_NAME), is("AccountName is missing, null or invalid."));
        assertThat(errors.get(FIELD_CREDENTIAL), is("The Credential is missing."));
    }
}
