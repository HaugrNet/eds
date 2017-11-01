/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.TrustLevel;
import org.junit.Test;

import java.util.Date;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class TrusteeTest {

    @Test
    public void testClass() {
        final Member member = createMember();
        final Circle circle = createCircle();
        final TrustLevel trustLevel = TrustLevel.WRITE;
        final Date lastModified = new Date(456L);
        final Date created = new Date(123L);

        final Trustee trustee = new Trustee();
        trustee.setMember(member);
        trustee.setCircle(circle);
        trustee.setTrustLevel(trustLevel);
        trustee.setChanged(lastModified);
        trustee.setSince(created);

        assertThat(trustee.getMember(), is(member));
        assertThat(trustee.getCircle(), is(circle));
        assertThat(trustee.getTrustLevel(), is(trustLevel));
        assertThat(trustee.getChanged(), is(lastModified));
        assertThat(trustee.getSince(), is(created));
    }

    @Test
    public void testStandardMethods() {
        final Member member = createMember();
        final Circle circle = createCircle();
        final TrustLevel trustLevel = TrustLevel.WRITE;
        final Date lastModified = new Date(456L);
        final Date created = new Date(123L);

        final Trustee trustee = new Trustee();
        final Trustee sameTrustee = new Trustee();
        final Trustee emptyTrustee = new Trustee();

        trustee.setMember(member);
        trustee.setCircle(circle);
        trustee.setTrustLevel(trustLevel);
        trustee.setChanged(lastModified);
        trustee.setSince(created);
        sameTrustee.setMember(trustee.getMember());
        sameTrustee.setCircle(trustee.getCircle());
        sameTrustee.setTrustLevel(trustee.getTrustLevel());
        sameTrustee.setChanged(trustee.getChanged());
        sameTrustee.setSince(trustee.getSince());

        assertThat(trustee.equals(null), is(false));
        assertThat(trustee.equals(trustee), is(true));
        assertThat(trustee.equals(sameTrustee), is(true));
        assertThat(trustee.equals(emptyTrustee), is(false));

        assertThat(trustee.hashCode(), is(sameTrustee.hashCode()));
        assertThat(trustee.hashCode(), is(not(emptyTrustee.hashCode())));

        assertThat(trustee.toString(), is(sameTrustee.toString()));
        assertThat(trustee.toString(), is(not(emptyTrustee.toString())));
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private static Member createMember() {
        final Member member = new Member();
        member.setMemberId(UUID.randomUUID().toString());
        member.setAccountName("Member AccountName");
        member.setAdded(new Date());

        return member;
    }

    private static Circle createCircle() {
        final Circle circle = new Circle();
        circle.setCircleId(UUID.randomUUID().toString());
        circle.setName("Circle Name");
        circle.setCreated(new Date());

        return circle;
    }
}
