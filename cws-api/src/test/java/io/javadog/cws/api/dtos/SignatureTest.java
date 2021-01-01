/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
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

import io.javadog.cws.api.common.Utilities;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class SignatureTest {

    @Test
    void testClassFlow() {
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

        assertEquals(checksum, signature.getChecksum());
        assertEquals(expires, signature.getExpires());
        assertEquals(verifications, signature.getVerifications());
        assertEquals(added, signature.getAdded());
        assertEquals(lastVerification, signature.getLastVerification());
    }

    @Test
    void testStandardMethods() {
        final Signature signature = new Signature();
        final Signature sameSignature = new Signature();
        final Signature emptySignature = new Signature();

        signature.setChecksum(UUID.randomUUID().toString());
        signature.setExpires(Utilities.newDate(123L));
        signature.setVerifications(3L);
        signature.setAdded(Utilities.newDate(321L));
        signature.setLastVerification(Utilities.newDate());
        sameSignature.setChecksum(signature.getChecksum());
        sameSignature.setExpires(signature.getExpires());
        sameSignature.setVerifications(signature.getVerifications());
        sameSignature.setAdded(signature.getAdded());
        sameSignature.setLastVerification(signature.getLastVerification());

        assertEquals(sameSignature.toString(), signature.toString());
        assertNotEquals(emptySignature.toString(), signature.toString());
    }
}
