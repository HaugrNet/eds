/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import io.javadog.cws.core.DatabaseSetup;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SignatureEntityTest extends DatabaseSetup {

    @Test
    public void testEntity() {
        final MemberEntity member = find(MemberEntity.class, 6L);
        final String checksum = UUID.randomUUID().toString();
        final String publicKey = UUID.randomUUID().toString();
        final Date expires = new Date();
        final SignatureEntity entity = new SignatureEntity();
        entity.setMember(member);
        entity.setPublicKey(publicKey);
        entity.setChecksum(checksum);
        entity.setExpires(expires);
        entity.setVerifications(123L);

        dao.persist(entity);
        final List<SignatureEntity> found = dao.findAllAscending(SignatureEntity.class, "id");
        assertFalse(found.isEmpty());
        assertThat(found.get(0).getMember(), is(member));
        assertThat(found.get(0).getPublicKey(), is(publicKey));
        assertThat(found.get(0).getChecksum(), is(checksum));
        assertThat(found.get(0).getExpires(), is(expires));
        assertThat(found.get(0).getVerifications(), is(123L));
    }
}
