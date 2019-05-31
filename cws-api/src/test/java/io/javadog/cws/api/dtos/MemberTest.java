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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.javadog.cws.api.common.MemberRole;
import io.javadog.cws.api.common.Utilities;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class MemberTest {

    @Test
    void testClassflow() {
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

        assertEquals(memberId, member.getMemberId());
        assertEquals(accountName, member.getAccountName());
        assertEquals(MemberRole.STANDARD, member.getMemberRole());
        assertEquals(publicKey, member.getPublicKey());
        assertEquals(added, member.getAdded());
    }

    @Test
    void testStandardMethods() {
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

        assertEquals(sameMember.toString(), member.toString());
        assertNotEquals(emptyMember.toString(), member.toString());
    }
}
