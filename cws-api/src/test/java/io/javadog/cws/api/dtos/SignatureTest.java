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

import org.junit.Test;

import java.util.Date;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SignatureTest {

    @Test
    public void testClass() {
        final String signatureValue = UUID.randomUUID().toString();
        final Date expires = new Date(123L);
        final Long verifications = 1L;
        final Date created = new Date(321L);
        final Date lastVerification = new Date();

        final Signature signature = new Signature();
        signature.setSignature(signatureValue);
        signature.setExpires(expires);
        signature.setVerifications(verifications);
        signature.setCreated(created);
        signature.setLastVerification(lastVerification);

        assertThat(signature.getSignature(), is(signatureValue));
        assertThat(signature.getExpires(), is(expires));
        assertThat(signature.getVerifications(), is(verifications));
        assertThat(signature.getCreated(), is(created));
        assertThat(signature.getLastVerification(), is(lastVerification));
    }

    @Test
    public void testStandardMethods() {
        final Signature signature = new Signature();
        final Signature sameSignature = new Signature();
        final Signature emptySignature = new Signature();

        signature.setSignature(UUID.randomUUID().toString());
        signature.setExpires(new Date(123L));
        signature.setVerifications(3L);
        signature.setCreated(new Date(321L));
        signature.setLastVerification(new Date());
        sameSignature.setSignature(signature.getSignature());
        sameSignature.setExpires(signature.getExpires());
        sameSignature.setVerifications(signature.getVerifications());
        sameSignature.setCreated(signature.getCreated());
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
}
