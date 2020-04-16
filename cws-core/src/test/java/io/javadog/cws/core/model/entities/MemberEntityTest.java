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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.MemberRole;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.GenerateTestData;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.jce.CWSKeyPair;
import io.javadog.cws.core.jce.Crypto;
import io.javadog.cws.core.model.CommonDao;
import java.util.List;
import java.util.UUID;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class MemberEntityTest extends DatabaseSetup {

    @Test
    void testEntity() {
        final CWSKeyPair keyPair = Crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final String externalId = UUID.randomUUID().toString();
        final MemberEntity entity = prepareMember(externalId, "New Account Name", "My Super Secret", keyPair, MemberRole.STANDARD);
        persistAndDetach(entity);

        final long id = entity.getId();

        final MemberEntity found = find(MemberEntity.class, id);
        assertEquals(entity.getId(), found.getId());
        assertEquals(entity.getExternalId(), found.getExternalId());
        assertEquals(entity.getName(), found.getName());
        assertEquals(entity.getSalt(), found.getSalt());
        assertEquals(entity.getPublicKey(), found.getPublicKey());
        assertEquals(entity.getPrivateKey(), found.getPrivateKey());
        assertEquals(MemberRole.STANDARD, found.getMemberRole());
        assertNull(found.getSessionChecksum());
        assertNull(found.getSessionCrypto());
        assertNull(found.getSessionExpire());
    }

    @Test
    void testPersistDetachedEntity() {
        final MemberEntity entity = new MemberEntity();
        entity.setId(12341234L);

        assertThrows(PersistenceException.class, () -> entityManager.persist(entity));
    }

    @Test
    void testUpdateEntity() {
        final String credential = "Updateable Account";
        final KeyAlgorithm algorithm = settings.getAsymmetricAlgorithm();
        final String publicKey = UUID.randomUUID().toString();
        final String privateKey = UUID.randomUUID().toString();
        final String externalId = UUID.randomUUID().toString();
        final MemberEntity entity = prepareMember(externalId, credential, algorithm, publicKey, privateKey, MemberRole.STANDARD);
        final long lastModified = entity.getAltered().getTime();

        persist(entity);
        assertTrue(entity.getAltered().getTime() >= lastModified);
    }

    @Test
    void testAddContent() {
        final String credential = "Account Name";
        final KeyAlgorithm algorithm = settings.getAsymmetricAlgorithm();
        final String publicKey = UUID.randomUUID().toString();
        final String privateKey = UUID.randomUUID().toString();
        final String externalId = UUID.randomUUID().toString();
        final MemberEntity entity = prepareMember(externalId, credential, algorithm, publicKey, privateKey, MemberRole.STANDARD);
        assertNotNull(entity.getId());

        entityManager.persist(entity);
        assertNotNull(entity.getId());
        // Now, let's flush all not-saved records to the DB and clear the Cache,
        // this way, a lookup will hit the database and not the Cache.
        entityManager.flush();
        entityManager.clear();

        final Query query = entityManager.createQuery("select m from MemberEntity m order by id desc");
        final List<MemberEntity> list = CommonDao.findList(query);
        // Now, a couple of checks. First, that we have 2 records, the default
        // Administration Account and the newly created Account
        assertEquals(7, list.size());

        final MemberEntity found = list.get(0);
        // Next, check that the newly found entity is not the same Object as the
        // first, this is done via the Reference Pointers, since an
        // Objects.equals() will yield the same result
        assertNotEquals(found.hashCode(), entity.hashCode());
        // Now, we'll ensure that it is the same Entity by comparing the Id's
        assertEquals(found.getId(), entity.getId());
    }

    @Test
    void testDaoFindMemberAdmin() {
        final MemberEntity admin = dao.findMemberByName(Constants.ADMIN_ACCOUNT);
        assertNotNull(admin);
        assertEquals(Constants.ADMIN_ACCOUNT, admin.getName());

        final MemberEntity adminByExternalId = dao.find(MemberEntity.class, admin.getExternalId());
        assertNotNull(adminByExternalId);
        assertEquals(admin.getId(), adminByExternalId.getId());
    }

    @Test
    void testDaoFindMemberUnknown() {
        final MemberEntity nullEntity = dao.find(MemberEntity.class, UUID.randomUUID().toString());
        assertNull(nullEntity);

        final MemberEntity found = dao.findMemberByName("Unknown");
        assertNull(found);
    }

    @Test
    void testFindAll() {
        final String jql = "select m from MemberEntity m";
        final Query query = entityManager.createQuery(jql);
        final List<MemberEntity> found = CommonDao.findList(query);

        assertEquals(6, found.size());
        assertEquals(Constants.ADMIN_ACCOUNT, found.get(0).getName());
        assertEquals(MEMBER_1, found.get(1).getName());
        assertEquals(MEMBER_2, found.get(2).getName());
        assertEquals(MEMBER_3, found.get(3).getName());
        assertEquals(MEMBER_4, found.get(4).getName());
        assertEquals(MEMBER_5, found.get(5).getName());
    }

    /**
     * This test is not really testing anything, only assuring that the
     * generation of test data for the database is working.
     */
    @Test
    void testPreparingTestData() {
        final GenerateTestData generator = new GenerateTestData();
        final String sql = generator.prepareTestData();
        assertTrue(sql.length() > 19000);
    }
}
