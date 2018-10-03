/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Utilities;
import org.junit.Test;

import java.util.Date;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class MemberTest {

    @Test
    public void testClassflow() {
        final String memberId = UUID.randomUUID().toString();
        final String accountName = "Member AccountName";
        final Date added = new Date();

        final Member member = new Member();
        member.setMemberId(memberId);
        member.setAccountName(accountName);
        member.setAdded(added);

        assertThat(member.getMemberId(), is(memberId));
        assertThat(member.getAccountName(), is(accountName));
        assertThat(member.getAdded(), is(added));
    }

    @Test
    public void testStandardMethods() {
        final Member member = new Member();
        final Member sameMember = new Member();
        final Member emptyMember = new Member();

        member.setMemberId(UUID.randomUUID().toString());
        member.setAccountName(UUID.randomUUID().toString());
        member.setAdded(Utilities.newDate());
        sameMember.setMemberId(member.getMemberId());
        sameMember.setAccountName(member.getAccountName());
        sameMember.setAdded(member.getAdded());

        assertThat(member.toString(), is(sameMember.toString()));
        assertThat(member.toString(), is(not(emptyMember.toString())));
    }
}
