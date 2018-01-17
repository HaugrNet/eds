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
public final class SignatureTest {

    @Test
    public void testClassflow() {
        final String checksum = UUID.randomUUID().toString();
        final Date expires = new Date(123L);
        final Long verifications = 1L;
        final Date added = new Date(321L);
        final Date lastVerification = new Date();

        final Signature signature = new Signature();
        signature.setChecksum(checksum);
        signature.setExpires(expires);
        signature.setVerifications(verifications);
        signature.setAdded(added);
        signature.setLastVerification(lastVerification);

        assertThat(signature.getChecksum(), is(checksum));
        assertThat(signature.getExpires(), is(expires));
        assertThat(signature.getVerifications(), is(verifications));
        assertThat(signature.getAdded(), is(added));
        assertThat(signature.getLastVerification(), is(lastVerification));
    }

    @Test
    public void testStandardMethods() {
        final Signature signature = prepareSignature(UUID.randomUUID().toString(), new Date(123L), 3L, new Date(321L), new Date());
        final Signature sameSignature = new Signature();
        final Signature emptySignature = new Signature();

        sameSignature.setChecksum(signature.getChecksum());
        sameSignature.setExpires(signature.getExpires());
        sameSignature.setVerifications(signature.getVerifications());
        sameSignature.setAdded(signature.getAdded());
        sameSignature.setLastVerification(signature.getLastVerification());

        assertThat(signature.equals(null), is(false));
        assertThat(signature.equals(signature), is(true));
        assertThat(signature.equals(sameSignature), is(true));
        assertThat(signature.equals(emptySignature), is(false));

        assertThat(signature.hashCode(), is(sameSignature.hashCode()));
        assertThat(signature.hashCode(), is(not(emptySignature.hashCode())));

        assertThat(signature.toString(), is(sameSignature.toString()));
        assertThat(signature.toString(), is(not(emptySignature.toString())));
    }

    @Test
    public void testEquality() {
        final String checksum1 = UUID.randomUUID().toString();
        final String checksum2 = UUID.randomUUID().toString();
        final Date date1 = new Date(1212121212L);
        final Date date2 = new Date(2121212121L);
        final Long verifications1 = 1L;
        final Long verifications2 = 2L;

        final Signature signature1 = prepareSignature(checksum1, date1, verifications1, date1, date1);
        final Signature signature2 = prepareSignature(checksum2, date1, verifications1, date1, date1);
        final Signature signature3 = prepareSignature(checksum1, date2, verifications1, date1, date1);
        final Signature signature4 = prepareSignature(checksum1, date1, verifications2, date1, date1);
        final Signature signature5 = prepareSignature(checksum1, date1, verifications1, date2, date1);
        final Signature signature6 = prepareSignature(checksum1, date1, verifications1, date1, date2);

        assertThat(signature1.equals(signature2), is(false));
        assertThat(signature2.equals(signature1), is(false));
        assertThat(signature1.equals(signature3), is(false));
        assertThat(signature3.equals(signature1), is(false));
        assertThat(signature1.equals(signature4), is(false));
        assertThat(signature4.equals(signature1), is(false));
        assertThat(signature1.equals(signature5), is(false));
        assertThat(signature5.equals(signature1), is(false));
        assertThat(signature1.equals(signature6), is(false));
        assertThat(signature6.equals(signature1), is(false));
    }

    // =========================================================================
    // Internal Helper Method
    // =========================================================================

    private static Signature prepareSignature(final String checksum, final Date expires, final Long verifications, final Date added, final Date lastVerification) {
        final Signature signature = new Signature();
        signature.setChecksum(checksum);
        signature.setExpires(expires);
        signature.setVerifications(verifications);
        signature.setAdded(added);
        signature.setLastVerification(lastVerification);

        return signature;
    }
}
