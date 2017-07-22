/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import org.junit.Test;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class MemberTest {

    @Test
    public void testClass() {
        final String admin = Constants.ADMIN_ACCOUNT;
        final String credentials = UUID.randomUUID().toString();

        final Authentication authentication = new Authentication();
        authentication.setAccount(admin);
        authentication.setCredentialType(CredentialType.PASSPHRASE);
        authentication.setCredential(credentials);

        final String id = UUID.randomUUID().toString();
        final Date created = new Date(123L);

        final Member member = new Member();
        member.setId(id);
        member.setAuthentication(authentication);
        member.setAdded(created);

        assertThat(member.getId(), is(id));
        assertThat(member.getAuthentication(), is(authentication));
        assertThat(member.getAdded(), is(created));

        final Map<String, String> errors = member.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testEmptyObjectValidation() {
        final Member member = new Member();
        final Map<String, String> errors = member.validate();
        assertThat(errors.size(), is(1));
    }

    @Test
    public void testNullId() {
        final Member member = new Member();
        member.setId(null);
        assertThat(member.getId(), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyId() {
        final Member member = new Member();
        member.setId("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidId() {
        final Member member = new Member();
        member.setId("123");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullAuthentication() {
        final Member member = new Member();
        member.setAuthentication(null);
    }
}
