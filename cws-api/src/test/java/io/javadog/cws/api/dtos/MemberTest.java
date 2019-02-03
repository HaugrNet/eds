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
package io.javadog.cws.api.dtos;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.MemberRole;
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
        final String publicKey = UUID.randomUUID().toString();
        final Date added = new Date();

        final Member member = new Member();
        member.setMemberId(memberId);
        member.setAccountName(accountName);
        member.setMemberRole(MemberRole.STANDARD);
        member.setPublicKey(publicKey);
        member.setAdded(added);

        assertThat(member.getMemberId(), is(memberId));
        assertThat(member.getAccountName(), is(accountName));
        assertThat(member.getMemberRole(), is(MemberRole.STANDARD));
        assertThat(member.getPublicKey(), is(publicKey));
        assertThat(member.getAdded(), is(added));
    }

    @Test
    public void testStandardMethods() {
        final Member member = new Member();
        final Member sameMember = new Member();
        final Member emptyMember = new Member();

        member.setMemberId(UUID.randomUUID().toString());
        member.setAccountName(UUID.randomUUID().toString());
        member.setMemberRole(MemberRole.ADMIN);
        member.setAdded(Utilities.newDate());
        sameMember.setMemberId(member.getMemberId());
        sameMember.setAccountName(member.getAccountName());
        sameMember.setMemberRole(member.getMemberRole());
        sameMember.setAdded(member.getAdded());

        assertThat(member.toString(), is(sameMember.toString()));
        assertThat(member.toString(), is(not(emptyMember.toString())));
    }
}
