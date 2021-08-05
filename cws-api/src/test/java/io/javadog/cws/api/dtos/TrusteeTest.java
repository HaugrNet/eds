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

import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.common.Utilities;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class TrusteeTest {

    @Test
    void testClassFlow() {
        for (final String zoneId : ZoneId.getAvailableZoneIds()) {
            System.out.println(zoneId);
        }
        final String memberId = UUID.randomUUID().toString();
        final String accountName = UUID.randomUUID().toString();
        final String publicKey = UUID.randomUUID().toString();
        final String circleId = UUID.randomUUID().toString();
        final String circleName = UUID.randomUUID().toString();
        final TrustLevel trustLevel = TrustLevel.WRITE;
        final LocalDateTime lastModified = Utilities.newDate(456L);
        final LocalDateTime added = Utilities.newDate(123L);

        final Trustee trustee = new Trustee();
        trustee.setMemberId(memberId);
        trustee.setAccountName(accountName);
        trustee.setPublicKey(publicKey);
        trustee.setCircleId(circleId);
        trustee.setCircleName(circleName);
        trustee.setTrustLevel(trustLevel);
        trustee.setChanged(lastModified);
        trustee.setAdded(added);

        assertEquals(memberId, trustee.getMemberId());
        assertEquals(accountName, trustee.getAccountName());
        assertEquals(publicKey, trustee.getPublicKey());
        assertEquals(circleId, trustee.getCircleId());
        assertEquals(circleName, trustee.getCircleName());
        assertEquals(trustLevel, trustee.getTrustLevel());
        assertEquals(lastModified, trustee.getChanged());
        assertEquals(added, trustee.getAdded());
    }

    @Test
    void testStandardMethods() {
        final Trustee trustee = new Trustee();
        final Trustee sameTrustee = new Trustee();
        final Trustee emptyTrustee = new Trustee();

        trustee.setMemberId(UUID.randomUUID().toString());
        trustee.setAccountName(UUID.randomUUID().toString());
        trustee.setPublicKey(UUID.randomUUID().toString());
        trustee.setCircleId(UUID.randomUUID().toString());
        trustee.setCircleName(UUID.randomUUID().toString());
        trustee.setTrustLevel(TrustLevel.WRITE);
        trustee.setChanged(Utilities.newDate(456L));
        trustee.setAdded(Utilities.newDate(123L));
        sameTrustee.setMemberId(trustee.getMemberId());
        sameTrustee.setAccountName(trustee.getAccountName());
        sameTrustee.setPublicKey(trustee.getPublicKey());
        sameTrustee.setCircleId(trustee.getCircleId());
        sameTrustee.setCircleName(trustee.getCircleName());
        sameTrustee.setTrustLevel(trustee.getTrustLevel());
        sameTrustee.setChanged(trustee.getChanged());
        sameTrustee.setAdded(trustee.getAdded());

        assertEquals(sameTrustee.toString(), trustee.toString());
        assertNotEquals(emptyTrustee.toString(), trustee.toString());
    }
}
