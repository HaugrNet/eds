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
public final class SanityTest {

    @Test
    public void testClassflow() {
        final String dataId = UUID.randomUUID().toString();
        final Date changed = new Date();

        final Sanity sanity = new Sanity();
        sanity.setDataId(dataId);
        sanity.setChanged(changed);

        assertThat(sanity.getDataId(), is(dataId));
        assertThat(sanity.getChanged(), is(changed));
    }

    @Test
    public void testStandardMethods() {
        final Sanity sanity = prepareSanity(UUID.randomUUID().toString(), new Date());
        final Sanity sameSanity = new Sanity();
        final Sanity emptySanity = new Sanity();

        sameSanity.setDataId(sanity.getDataId());
        sameSanity.setChanged(sanity.getChanged());

        assertThat(sanity.equals(null), is(false));
        assertThat(sanity.equals(sanity), is(true));
        assertThat(sanity.equals(sameSanity), is(true));
        assertThat(sanity.equals(emptySanity), is(false));

        assertThat(sanity.hashCode(), is(sameSanity.hashCode()));
        assertThat(sanity.hashCode(), is(not(emptySanity.hashCode())));

        assertThat(sanity.toString(), is(sameSanity.toString()));
        assertThat(sanity.toString(), is(not(emptySanity.toString())));
    }

    @Test
    public void testEquality() {
        final String dataId1 = UUID.randomUUID().toString();
        final String dataId2 = UUID.randomUUID().toString();
        final Date changed1 = new Date(1212121212L);
        final Date changed2 = new Date(2121212121L);
        final Sanity sanity1 = prepareSanity(dataId1, changed1);
        final Sanity sanity2 = prepareSanity(dataId2, changed1);
        final Sanity sanity3 = prepareSanity(dataId1, changed2);

        assertThat(sanity1.equals(sanity2), is(false));
        assertThat(sanity2.equals(sanity1), is(false));
        assertThat(sanity1.equals(sanity3), is(false));
        assertThat(sanity3.equals(sanity1), is(false));
    }

    // =========================================================================
    // Internal Helper Method
    // =========================================================================

    private static Sanity prepareSanity(final String dataId, final Date changed) {
        final Sanity sanity = new Sanity();
        sanity.setDataId(dataId);
        sanity.setChanged(changed);

        return sanity;
    }
}
