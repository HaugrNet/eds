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

import io.javadog.cws.api.common.TrustLevel;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Date;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class TrusteeTest {

    @Test
    public void testClassflow() {
        final String memberId = UUID.randomUUID().toString();
        final String circleId = UUID.randomUUID().toString();
        final TrustLevel trustLevel = TrustLevel.WRITE;
        final Date lastModified = new Date(456L);
        final Date added = new Date(123L);

        final Trustee trustee = new Trustee();
        trustee.setMemberId(memberId);
        trustee.setCircleId(circleId);
        trustee.setTrustLevel(trustLevel);
        trustee.setChanged(lastModified);
        trustee.setAdded(added);

        assertThat(trustee.getMemberId(), is(memberId));
        assertThat(trustee.getCircleId(), is(circleId));
        assertThat(trustee.getTrustLevel(), is(trustLevel));
        assertThat(trustee.getChanged(), is(lastModified));
        assertThat(trustee.getAdded(), is(added));
    }

    @Test
    public void testStandardMethods() {
        final long circleId = new SecureRandom().nextLong();
        final String memberId = UUID.randomUUID().toString();
        final TrustLevel trustLevel = TrustLevel.WRITE;
        final Date lastModified = new Date(456L);
        final Date created = new Date(123L);

        final Trustee trustee = prepareTrustee(circleId, memberId, trustLevel, lastModified, created);
        final Trustee sameTrustee = new Trustee();
        final Trustee emptyTrustee = new Trustee();

        sameTrustee.setMemberId(trustee.getMemberId());
        sameTrustee.setCircleId(trustee.getCircleId());
        sameTrustee.setTrustLevel(trustee.getTrustLevel());
        sameTrustee.setChanged(trustee.getChanged());
        sameTrustee.setAdded(trustee.getAdded());

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
        final long circleId1 = new SecureRandom().nextLong();
        final long circleId2 = new SecureRandom().nextLong();
        final String memberId1 = UUID.randomUUID().toString();
        final String memberId2 = UUID.randomUUID().toString();
        final TrustLevel trustLevel1 = TrustLevel.WRITE;
        final TrustLevel trustLevel2 = TrustLevel.READ;
        final Date date1 = new Date(1212121212L);
        final Date date2 = new Date(2121212121L);

        final Trustee trustee1 = prepareTrustee(circleId1, memberId1, trustLevel1, date1, date1);
        final Trustee trustee2 = prepareTrustee(circleId2, memberId1, trustLevel1, date1, date1);
        final Trustee trustee3 = prepareTrustee(circleId1, memberId2, trustLevel1, date1, date1);
        final Trustee trustee4 = prepareTrustee(circleId1, memberId1, trustLevel2, date1, date1);
        final Trustee trustee5 = prepareTrustee(circleId1, memberId1, trustLevel1, date2, date1);
        final Trustee trustee6 = prepareTrustee(circleId1, memberId1, trustLevel1, date1, date2);

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

    private static Trustee prepareTrustee(final long circleId, final String memberId, final TrustLevel trustLevel, final Date lastModified, final Date added) {
        final Trustee trustee = new Trustee();
        trustee.setMemberId(memberId);
        trustee.setCircleId(String.valueOf(circleId));
        trustee.setTrustLevel(trustLevel);
        trustee.setChanged(lastModified);
        trustee.setAdded(added);

        return trustee;
    }
}
