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
    public void testClassflow() {
        final Member member = prepareMember(UUID.randomUUID().toString(), "Member AccountName", new Date());
        final Circle circle = prepareCircle(UUID.randomUUID().toString(), "Circle Name", new Date());
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
        final Member member = prepareMember(UUID.randomUUID().toString(), "Member AccountName", new Date());
        final Circle circle = prepareCircle(UUID.randomUUID().toString(), "Circle Name", new Date());
        final TrustLevel trustLevel = TrustLevel.WRITE;
        final Date lastModified = new Date(456L);
        final Date created = new Date(123L);

        final Trustee trustee = prepareTrustee(member, circle, trustLevel, lastModified, created);
        final Trustee sameTrustee = new Trustee();
        final Trustee emptyTrustee = new Trustee();

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

    @Test
    public void testEquality() {
        final Member member1 = prepareMember(UUID.randomUUID().toString(), "Member1", new Date());
        final Member member2 = prepareMember(UUID.randomUUID().toString(), "Member2", new Date());
        final Circle circle1 = prepareCircle(UUID.randomUUID().toString(), "Circle1", new Date());
        final Circle circle2 = prepareCircle(UUID.randomUUID().toString(), "Circle2", new Date());
        final TrustLevel trustLevel1 = TrustLevel.WRITE;
        final TrustLevel trustLevel2 = TrustLevel.READ;
        final Date date1 = new Date(1212121212L);
        final Date date2 = new Date(2121212121L);

        final Trustee trustee1 = prepareTrustee(member1, circle1, trustLevel1, date1, date1);
        final Trustee trustee2 = prepareTrustee(member2, circle1, trustLevel1, date1, date1);
        final Trustee trustee3 = prepareTrustee(member1, circle2, trustLevel1, date1, date1);
        final Trustee trustee4 = prepareTrustee(member1, circle1, trustLevel2, date1, date1);
        final Trustee trustee5 = prepareTrustee(member1, circle1, trustLevel1, date2, date1);
        final Trustee trustee6 = prepareTrustee(member1, circle1, trustLevel1, date1, date2);

        assertThat(trustee1.equals(trustee2), is(false));
        assertThat(trustee2.equals(trustee1), is(false));
        assertThat(trustee1.equals(trustee3), is(false));
        assertThat(trustee3.equals(trustee1), is(false));
        assertThat(trustee1.equals(trustee4), is(false));
        assertThat(trustee4.equals(trustee1), is(false));
        assertThat(trustee1.equals(trustee5), is(false));
        assertThat(trustee5.equals(trustee1), is(false));
        assertThat(trustee1.equals(trustee6), is(false));
        assertThat(trustee6.equals(trustee1), is(false));
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private static Member prepareMember(final String memberID, final String accountName, final Date added) {
        final Member member = new Member();
        member.setMemberId(memberID);
        member.setAccountName(accountName);
        member.setAdded(added);

        return member;
    }

    private static Circle prepareCircle(final String circleID, final String circleName, final Date created) {
        final Circle circle = new Circle();
        circle.setCircleId(circleID);
        circle.setCircleName(circleName);
        circle.setCreated(created);

        return circle;
    }

    private static Trustee prepareTrustee(final Member member, final Circle circle, final TrustLevel trustLevel, final Date lastModified, final Date since) {
        final Trustee trustee = new Trustee();
        trustee.setMember(member);
        trustee.setCircle(circle);
        trustee.setTrustLevel(trustLevel);
        trustee.setChanged(lastModified);
        trustee.setSince(since);

        return trustee;
    }
}
