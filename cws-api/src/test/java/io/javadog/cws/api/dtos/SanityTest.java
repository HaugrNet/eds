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
        final Sanity sanity = new Sanity();
        final Sanity sameSanity = new Sanity();
        final Sanity emptySanity = new Sanity();

        sanity.setDataId(UUID.randomUUID().toString());
        sanity.setChanged(Utilities.newDate());
        sameSanity.setDataId(sanity.getDataId());
        sameSanity.setChanged(sanity.getChanged());

        assertThat(sanity.toString(), is(sameSanity.toString()));
        assertThat(sanity.toString(), is(not(emptySanity.toString())));
    }
}
