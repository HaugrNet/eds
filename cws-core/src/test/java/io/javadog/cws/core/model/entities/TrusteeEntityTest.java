/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2020, JavaDog.io
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
package io.javadog.cws.core.model.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.javadog.cws.api.common.MemberRole;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.KeyAlgorithm;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class TrusteeEntityTest extends DatabaseSetup {

    @Test
    void testEntity() {
        final KeyAlgorithm algorithm = settings.getAsymmetricAlgorithm();
        final String externalId = UUID.randomUUID().toString();
        final MemberEntity member = prepareMember(externalId, "Trustee Member", algorithm, "public Key", "private Key", MemberRole.STANDARD);
        final CircleEntity circle = prepareCircle(UUID.randomUUID().toString(), "Trustee Circle");
        final String circleKey = UUID.randomUUID().toString();
        final KeyEntity key = prepareKey();
        final TrusteeEntity entity = new TrusteeEntity();
        entity.setMember(member);
        entity.setCircle(circle);
        entity.setKey(key);
        entity.setTrustLevel(TrustLevel.ADMIN);
        entity.setCircleKey(circleKey);
        persist(entity);

        final TrusteeEntity found = find(TrusteeEntity.class, entity.getId());
        assertEquals(member.getId(), found.getMember().getId());
        assertEquals(circle.getId(), found.getCircle().getId());
        assertEquals(key.getId(), found.getKey().getId());

        found.setCircleKey("New Key");
        found.setTrustLevel(TrustLevel.WRITE);
        persist(found);

        final TrusteeEntity updated = find(TrusteeEntity.class, entity.getId());
        assertNotNull(updated);
        assertNotEquals(TrustLevel.ADMIN, updated.getTrustLevel());
        assertNotEquals(circleKey, updated.getCircleKey());
    }
}
