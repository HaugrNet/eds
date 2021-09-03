/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2021, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.cws.core.model.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import net.haugr.cws.api.common.Utilities;
import net.haugr.cws.core.setup.DatabaseSetup;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class SignatureEntityTest extends DatabaseSetup {

    @Test
    void testEntity() {
        final MemberEntity member = find(MemberEntity.class, 6L);
        final String checksum = UUID.randomUUID().toString();
        final String publicKey = UUID.randomUUID().toString();
        final LocalDateTime expires = Utilities.newDate();
        final SignatureEntity entity = new SignatureEntity();
        entity.setMember(member);
        entity.setPublicKey(publicKey);
        entity.setChecksum(checksum);
        entity.setExpires(expires);
        entity.setVerifications(123L);

        dao.persist(entity);
        final List<SignatureEntity> found = dao.findAllAscending(SignatureEntity.class, "id");
        assertFalse(found.isEmpty());
        assertEquals(member, found.get(0).getMember());
        assertEquals(publicKey, found.get(0).getPublicKey());
        assertEquals(checksum, found.get(0).getChecksum());
        assertEquals(expires, found.get(0).getExpires());
        assertEquals(Long.valueOf(123L), found.get(0).getVerifications());
    }
}
