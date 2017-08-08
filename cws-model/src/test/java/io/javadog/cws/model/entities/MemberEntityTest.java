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
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.common.CWSKey;
import io.javadog.cws.common.enums.KeyAlgorithm;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Ignore;
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

    @Test
    @Ignore("This test is only present to help generate test data.")
    public void testPrepareData() {
        println("-- Default Administrator User, it is set at the first request to the System, and");
        println("-- is thus needed for loads of tests. Remaining Accounts is for \"member1\" to");
        println("-- \"member5\", which is all used as part of the tests.");
        println("INSERT INTO members (external_id, name, salt, algorithm, public_key, private_key) VALUES");

        final MemberEntity admin = createAndPrintMember(Constants.ADMIN_ACCOUNT, ',');
        final MemberEntity member1 = createAndPrintMember("member1", ',');
        final MemberEntity member2 = createAndPrintMember("member2", ',');
        final MemberEntity member3 = createAndPrintMember("member3", ',');
        final MemberEntity member4 = createAndPrintMember("member4", ',');
        final MemberEntity member5 = createAndPrintMember("member5", ';');

        println("");
        println("-- Default, we have 3 Circles as part of the test setup, using the very");
        println("-- imaginative names, 'circle1' to 'circle3'.");
        println("");
        println("INSERT INTO circles (external_id, name) VALUES");
        final CircleEntity circle1 = createAndPrintCircle("circle1", ',');
        final CircleEntity circle2 = createAndPrintCircle("circle2", ',');
        final CircleEntity circle3 = createAndPrintCircle("circle3", ';');

        println("");
        println("-- For each Circle, we need to have a unique Key, but with the same settings.");
        println("INSERT INTO keys (algorithm, salt, status) VALUES");
        final KeyEntity key1 = createAndPrintKey(',');
        final KeyEntity key2 = createAndPrintKey(',');
        final KeyEntity key3 = createAndPrintKey(';');

        println("");
        println("-- With the Members created, and the Circles and Keys added, it is possible to");
        println("-- also create a number of Trustees, in this case we add Member 1-3 to Circle 1,");
        println("-- Member 1-4 to Circle 2 and Member 2-5 to Circle 3.");
        println("-- The Trust Level is different for each Member.");
        println("INSERT INTO trustees (external_id, member_id, circle_id, key_id, trust_level, circle_key) VALUES");
        final CWSKey cwsKey1 = crypto.generateSymmetricKey(key1.getAlgorithm(), key1.getSalt());
        final CWSKey cwsKey2 = crypto.generateSymmetricKey(key1.getAlgorithm(), key1.getSalt());
        final CWSKey cwsKey3 = crypto.generateSymmetricKey(key1.getAlgorithm(), key1.getSalt());
        createAndPrintTrustee(member1, circle1, key1, cwsKey1, TrustLevel.ADMIN, ',');
        createAndPrintTrustee(member2, circle1, key1, cwsKey1, TrustLevel.WRITE, ',');
        createAndPrintTrustee(member3, circle1, key1, cwsKey1, TrustLevel.READ,  ',');
        createAndPrintTrustee(member1, circle2, key2, cwsKey2, TrustLevel.ADMIN, ',');
        createAndPrintTrustee(member2, circle2, key2, cwsKey2, TrustLevel.WRITE, ',');
        createAndPrintTrustee(member3, circle2, key2, cwsKey2, TrustLevel.READ,  ',');
        createAndPrintTrustee(member4, circle2, key2, cwsKey2, TrustLevel.ADMIN, ',');
        createAndPrintTrustee(member2, circle3, key3, cwsKey3, TrustLevel.WRITE, ',');
        createAndPrintTrustee(member3, circle3, key3, cwsKey3, TrustLevel.READ,  ',');
        createAndPrintTrustee(member4, circle3, key3, cwsKey3, TrustLevel.ADMIN, ',');
        createAndPrintTrustee(member5, circle3, key3, cwsKey3, TrustLevel.GUEST, ';');

        assertThat(admin.getAlgorithm(), is(KeyAlgorithm.RSA2048));
    }

    private MemberEntity createAndPrintMember(final String name, final char delimiter) {
        final MemberEntity entity = prepareMember(name, name);
        dao.persist(entity);

        println("    ('" + entity.getExternalId() + "', '" + name + "', '" + entity.getSalt() + "', '" + entity.getAlgorithm() + "', '" + entity.getPublicKey() + "', '" + entity.getPrivateKey() + "')" + delimiter);

        return entity;
    }

    private CircleEntity createAndPrintCircle(final String name, final char delimiter) {
        final CircleEntity entity = prepareCircle(name);

        println("    ('" + entity.getExternalId() + "', '" + entity.getName() + "')" + delimiter);

        return entity;
    }

    private KeyEntity createAndPrintKey(final char delimiter) {
        final KeyEntity entity = prepareKey();

        println("    ('" + entity.getAlgorithm() + "', '" + entity.getSalt() + "', '" + entity.getStatus() + "')" + delimiter);

        return entity;
    }

    private void createAndPrintTrustee(final MemberEntity member, final CircleEntity circle, final KeyEntity key, final CWSKey circleKey, final TrustLevel trustLevel, final char delimiter) {
        final String armoredKey = crypto.encryptAndArmorCircleKey(member.getKey(), circleKey);
        final TrusteeEntity entity = new TrusteeEntity();
        entity.setMember(member);
        entity.setCircle(circle);
        entity.setKey(key);
        entity.setTrustLevel(trustLevel);
        entity.setCircleKey(armoredKey);
        dao.persist(entity);

        println("    ('" + entity.getExternalId() + "', " + member.getId() + ", " + circle.getId() + ", " + key.getId() + ", '" + trustLevel + "', '" + armoredKey + "')" + delimiter);
    }

    private static void println(final String str) {
        System.out.println(str);
    }
}
