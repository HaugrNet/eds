/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-model)
 * =============================================================================
 */
package io.javadog.cws.model.entities;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.common.enums.KeyAlgorithm;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.List;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class MemberEntityTest extends DatabaseSetup {

    @Test
    public void testEntity() {
        final MemberEntity entity = prepareMember("New Account Name", "My Super Secret");
        persistAndDetach(entity);

        final long id = entity.getId();

        final MemberEntity found = find(MemberEntity.class, id);
        assertThat(found.getId(), is(entity.getId()));
        assertThat(found.getExternalId(), is(entity.getExternalId()));
        assertThat(found.getName(), is(entity.getName()));
        assertThat(found.getSalt(), is(entity.getSalt()));
        assertThat(found.getPublicKey(), is(entity.getPublicKey()));
        assertThat(found.getPrivateKey(), is(entity.getPrivateKey()));
        assertThat(found.getKey(), is(nullValue()));
    }

    @Test(expected = PersistenceException.class)
    public void testPersistDetachedEntity() {
        final MemberEntity entity = new MemberEntity();
        entity.setId(12341234L);
        entityManager.persist(entity);
    }

    @Test
    public void testUpdateEntity() {
        final String credential = "Updateable Account";
        final KeyAlgorithm algorithm = settings.getAsymmetricAlgorithm();
        final String publicKey = UUID.randomUUID().toString();
        final String privateKey = UUID.randomUUID().toString();
        final MemberEntity entity = prepareMember(credential, algorithm, publicKey, privateKey);

        final long lastModified = entity.getModified().getTime();

        persist(entity);
        assertThat(entity.getModified().getTime() >= lastModified, is(true));
    }

    @Test
    public void testAddContent() {
        final String credential = "Account Name";
        final KeyAlgorithm algorithm = settings.getAsymmetricAlgorithm();
        final String publicKey = UUID.randomUUID().toString();
        final String privateKey = UUID.randomUUID().toString();
        final MemberEntity entity = prepareMember(credential, algorithm, publicKey, privateKey);
        assertThat(entity.getId(), is(not(nullValue())));

        entityManager.persist(entity);
        assertThat(entity.getId(), is(not(nullValue())));
        // Now, let's flush all not-saved records to the DB and clear the Cache,
        // this way, a lookup will hit the database and not the Cache.
        entityManager.flush();
        entityManager.clear();

        final Query query = entityManager.createQuery("select m from MemberEntity m order by id desc");
        final List<MemberEntity> list = query.getResultList();
        // Now, a couple of checks. First, that we have 2 records, the default
        // Administration Account and the newly created Account
        assertThat(list.size(), is(7));

        final MemberEntity found = list.get(0);
        // Next, check that the newly found entity is not the same Object as the
        // first, this is done via the Reference Pointers, since an
        // Objects.equals() will yield the same result
        assertThat(entity.hashCode(), is(not(found.hashCode())));
        // Now, we'll ensure that it is the same Entity by comparing the Id's
        assertThat(entity.getId(), is(found.getId()));
    }

    @Test
    public void testDaoFindMemberAdmin() {
        final MemberEntity admin = dao.findMemberByName(Constants.ADMIN_ACCOUNT);
        assertThat(admin, is(not(nullValue())));
        assertThat(admin.getName(), is(Constants.ADMIN_ACCOUNT));

        final MemberEntity adminByExternalId = dao.find(MemberEntity.class, admin.getExternalId());
        assertThat(adminByExternalId, is(not(nullValue())));
        assertThat(adminByExternalId.getId(), is(admin.getId()));
    }

    @Test
    public void testDaoFindMemberUnknown() {
        final MemberEntity nullEntity = dao.find(MemberEntity.class, UUID.randomUUID().toString());
        assertThat(nullEntity, is(nullValue()));

        final MemberEntity found = dao.findMemberByName("Unknown");
        assertThat(found, is(nullValue()));
    }

    @Test
    public void testFindAll() {
        final String jql = "select m from MemberEntity m";
        final Query query = entityManager.createQuery(jql);
        final List<MemberEntity> found = query.getResultList();

        assertThat(found.size(), is(6));
        assertThat(found.get(0).getName(), is(Constants.ADMIN_ACCOUNT));
        assertThat(found.get(1).getName(), is("member1"));
        assertThat(found.get(2).getName(), is("member2"));
        assertThat(found.get(3).getName(), is("member3"));
        assertThat(found.get(4).getName(), is("member4"));
        assertThat(found.get(5).getName(), is("member5"));
    }

    //@Test
    //public void testPrepareData() {
    //    System.out.println("-- Default Administrator User, it is set at the first request to the System, and");
    //    System.out.println("-- is thus needed for loads of tests. Remaining Accounts is for \"member1\" to");
    //    System.out.println("-- \"member5\", which is all used as part of the tests.");
    //    System.out.println("INSERT INTO members (external_id, name, salt, algorithm, public_key, private_key) VALUES");
    //
    //    final MemberEntity admin = createAndPrintMember(Constants.ADMIN_ACCOUNT);
    //    createAndPrintMember("member1");
    //    createAndPrintMember("member2");
    //    createAndPrintMember("member3");
    //    createAndPrintMember("member4");
    //    createAndPrintMember("member5");
    //
    //    assertThat(admin.getAlgorithm(), is(KeyAlgorithm.RSA2048));
    //}
    //
    //private MemberEntity createAndPrintMember(final String name) {
    //    final MemberEntity entity = prepareMember(name, name);
    //    dao.persist(entity);
    //
    //    // Expecting a "INSERT INTO members (external_id, name, salt, algorithm, public_key, private_key) VALUES" line before
    //    System.out.println("    ('" + entity.getExternalId() + "', '" + name + "', '" + entity.getSalt() + "', '" + entity.getAlgorithm() + "', '" + entity.getPublicKey() + "', '" + entity.getPrivateKey() + "),");
    //
    //    return entity;
    //}
}
