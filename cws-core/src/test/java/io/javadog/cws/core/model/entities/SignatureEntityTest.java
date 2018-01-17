/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.model.entities;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.core.DatabaseSetup;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.UUID;

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
        assertThat(found.isEmpty(), is(false));
        assertThat(found.get(0).getMember(), is(member));
        assertThat(found.get(0).getPublicKey(), is(publicKey));
        assertThat(found.get(0).getChecksum(), is(checksum));
        assertThat(found.get(0).getExpires(), is(expires));
        assertThat(found.get(0).getVerifications(), is(123L));
    }
}
