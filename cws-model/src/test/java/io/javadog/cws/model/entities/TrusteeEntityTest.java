/*
 * =============================================================================
 * Copyright (c) 2010-2017, JavaDog.IO
 * -----------------------------------------------------------------------------
 * Project: ZObEL (cws-model)
 * =============================================================================
 */
package io.javadog.cws.model.entities;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class TrusteeEntityTest extends DatabaseSetup {

    @Test
    public void testEntity() {
        final MemberEntity member = prepareMember("Trustee Member", "public Key", "private Key");
        final CircleEntity circle = prepareCircle("Trustee Circle");
        final KeyEntity key = prepareKey();
        final TrusteeEntity entity = new TrusteeEntity();
        entity.setMember(member);
        entity.setCircle(circle);
        entity.setKey(key);
        entity.setTrustLevel(TrustLevel.ADMIN);
        entity.setCircleKey(UUID.randomUUID().toString());
        persist(entity);

        assertThat(entity.getId(), is(not(nullValue())));
    }
}
