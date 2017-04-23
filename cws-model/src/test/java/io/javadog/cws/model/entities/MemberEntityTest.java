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
import io.javadog.cws.common.Crypto;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.ModelException;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.security.KeyPair;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class MemberEntityTest extends DatabaseSetup {

    @Test
    public void testEntity() {
        final Crypto crypto = new Crypto(new Settings());
        final String externalId = UUID.randomUUID().toString();
        final String name = "New Account Name";
        final String publicKey = UUID.randomUUID().toString();
        final String privateKey = UUID.randomUUID().toString();
        final String salt = UUID.randomUUID().toString();
        final KeyPair keyPair = crypto.generateAsymmetricKey();
        final Date modified = new Date();
        final Date created = new Date();

        final MemberEntity entity = new MemberEntity();
        entity.setExternalId(externalId);
        entity.setName(name);
        entity.setSalt(salt);
        entity.setPublicKey(publicKey);
        entity.setPrivateKey(privateKey);
        entity.setKeyPair(keyPair);
        entity.setModified(modified);
        entity.setCreated(created);
        persistAndDetach(entity);

        final long id = entity.getId();

        final MemberEntity found = find(MemberEntity.class, id);
        assertThat(found.getId(), is(entity.getId()));
        assertThat(found.getExternalId(), is(externalId));
        assertThat(found.getName(), is(name));
        assertThat(found.getSalt(), is(salt));
        assertThat(found.getPublicKey(), is(publicKey));
        assertThat(found.getPrivateKey(), is(privateKey));
        assertThat(found.getKeyPair(), is(nullValue()));
        // ToDo fix the Time checks, as they are tricky when it comes to the millis.
        assertThat(found.getModified().getTime(), is(modified.getTime()));
        assertThat(found.getCreated().getTime(), is(created.getTime()));
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
        final String publicKey = UUID.randomUUID().toString();
        final String privateKey = UUID.randomUUID().toString();
        final MemberEntity entity = prepareMember(credential, publicKey, privateKey);

        final long lastModified = entity.getModified().getTime();

        persist(entity);
        assertThat(entity.getModified().getTime() >= lastModified, is(true));
    }

    @Test
    public void testAddContent() {
        final String credential = "Account Name";
        final String publicKey = UUID.randomUUID().toString();
        final String privateKey = UUID.randomUUID().toString();
        final MemberEntity entity = prepareMember(credential, publicKey, privateKey);
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

        final MemberEntity adminByExternalId = dao.findMemberByExternalId(admin.getExternalId());
        assertThat(adminByExternalId, is(not(nullValue())));
        assertThat(adminByExternalId.getId(), is(admin.getId()));
    }

    @Test
    public void testDaoFindMemberUnknown() {
        final MemberEntity nullEntity = dao.findMemberByExternalId(UUID.randomUUID().toString());
        assertThat(nullEntity, is(nullValue()));

        prepareCause(ModelException.class, Constants.IDENTIFICATION_WARNING, "No member found with 'Unknown'.");
        dao.findMemberByName("Unknown");
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
}
