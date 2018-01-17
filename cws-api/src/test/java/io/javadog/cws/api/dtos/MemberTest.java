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
        final Member member = prepareMember(UUID.randomUUID().toString(), "Member AccountName", new Date());
        final Member sameMember = new Member();
        final Member emptyMember = new Member();

        sameMember.setMemberId(member.getMemberId());
        sameMember.setAccountName(member.getAccountName());
        sameMember.setAdded(member.getAdded());

        assertThat(member.equals(null), is(false));
        assertThat(member.equals(member), is(true));
        assertThat(member.equals(sameMember), is(true));
        assertThat(member.equals(emptyMember), is(false));

        assertThat(member.hashCode(), is(sameMember.hashCode()));
        assertThat(member.hashCode(), is(not(emptyMember.hashCode())));

        assertThat(member.toString(), is(sameMember.toString()));
        assertThat(member.toString(), is(not(emptyMember.toString())));
    }

    @Test
    public void testEquakity() {
        final String memberId1 = UUID.randomUUID().toString();
        final String memberId2 = UUID.randomUUID().toString();
        final String accountName1 = "First Account Name";
        final String accountNamw2 = "Second Account Name";
        final Date added1 = new Date(1212121212L);
        final Date added2 = new Date(2121212121L);
        final Member member1 = prepareMember(memberId1, accountName1, added1);
        final Member member2 = prepareMember(memberId2, accountName1, added1);
        final Member member3 = prepareMember(memberId1, accountNamw2, added1);
        final Member member4 = prepareMember(memberId1, accountName1, added2);

        assertThat(member1.equals(member2), is(false));
        assertThat(member2.equals(member1), is(false));
        assertThat(member1.equals(member3), is(false));
        assertThat(member3.equals(member1), is(false));
        assertThat(member1.equals(member4), is(false));
        assertThat(member4.equals(member1), is(false));
    }

    // =========================================================================
    // Internal Helper Method
    // =========================================================================

    private static Member prepareMember(final String memberId, final String accountName, final Date added) {
        final Member member = new Member();
        member.setMemberId(memberId);
        member.setAccountName(accountName);
        member.setAdded(added);

        return member;
    }
}
