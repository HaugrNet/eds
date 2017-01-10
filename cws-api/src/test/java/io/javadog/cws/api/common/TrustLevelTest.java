package io.javadog.cws.api.common;

import org.junit.Test;

import static io.javadog.cws.api.common.TrustLevel.ADMIN;
import static io.javadog.cws.api.common.TrustLevel.GUEST;
import static io.javadog.cws.api.common.TrustLevel.READ;
import static io.javadog.cws.api.common.TrustLevel.WRITE;
import static io.javadog.cws.api.common.TrustLevel.isAllowed;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class TrustLevelTest {

    @Test
    public void testIsAllowed() {
        assertThat(isAllowed(GUEST, GUEST), is(true));
        assertThat(isAllowed(GUEST, READ), is(false));
        assertThat(isAllowed(GUEST, WRITE), is(false));
        assertThat(isAllowed(GUEST, ADMIN), is(false));

        assertThat(isAllowed(READ, GUEST), is(true));
        assertThat(isAllowed(READ, READ), is(true));
        assertThat(isAllowed(READ, WRITE), is(false));
        assertThat(isAllowed(READ, ADMIN), is(false));

        assertThat(isAllowed(WRITE, GUEST), is(true));
        assertThat(isAllowed(WRITE, READ), is(true));
        assertThat(isAllowed(WRITE, WRITE), is(true));
        assertThat(isAllowed(WRITE, ADMIN), is(false));

        assertThat(isAllowed(ADMIN, GUEST), is(true));
        assertThat(isAllowed(ADMIN, READ), is(true));
        assertThat(isAllowed(ADMIN, WRITE), is(true));
        assertThat(isAllowed(ADMIN, ADMIN), is(true));
    }
}
