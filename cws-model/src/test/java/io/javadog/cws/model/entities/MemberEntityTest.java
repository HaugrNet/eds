package io.javadog.cws.model.entities;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.common.exceptions.ModelException;
import io.javadog.cws.model.EntityManagerSetup;
import io.javadog.cws.model.CommonDao;
import io.javadog.cws.model.jpa.CommonJpaDao;
import org.junit.Ignore;
import org.junit.Test;

import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Ignore
public final class MemberEntityTest extends EntityManagerSetup {

    @Test
    public void testEntity() {
        final String externalId = UUID.randomUUID().toString();
        final String credential = Constants.ADMIN_ACCOUNT;
        final String publicKey = UUID.randomUUID().toString();
        final String privateKey = UUID.randomUUID().toString();
        final Date modified = new Date();
        final Date created = new Date();

        final MemberEntity entity = new MemberEntity();
        entity.setExternalId(externalId);
        entity.setCredential(credential);
        entity.setPublicKey(publicKey);
        entity.setPrivateKey(privateKey);
        entity.setModified(modified);
        entity.setCreated(created);

        // Now, save Entity and clear Cache
        entityManager.persist(entity);
        entityManager.flush();
        entityManager.clear();
        final long id = entity.getId();

        final MemberEntity found = entityManager.find(MemberEntity.class, id);
        assertThat(found.getId(), is(entity.getId()));
        assertThat(found.getExternalId(), is(externalId));
        assertThat(found.getCredential(), is(credential));
        assertThat(found.getPublicKey(), is(publicKey));
        assertThat(found.getPrivateKey(), is(privateKey));
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
        final String credential = Constants.ADMIN_ACCOUNT;
        final String publicKey = UUID.randomUUID().toString();
        final String privateKey = UUID.randomUUID().toString();
        final MemberEntity entity = prepareMember(credential, publicKey, privateKey);

        final long lastModified = entity.getModified().getTime();

        persist(entity);
        assertThat(entity.getModified().getTime() >= lastModified, is(true));
    }

    @Test
    public void testAddContent() {
        final String credential = Constants.ADMIN_ACCOUNT;
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

        final Query query = entityManager.createQuery("select m from MemberEntity m");
        final List<MemberEntity> list = query.getResultList();
        // Now, a couple of checks. First, that we have 1 and only 1 record
        assertThat(list.size(), is(1));

        final MemberEntity found = list.get(0);
        // Next, check that the newly found entity is not the same Object as the
        // first, this is done via the Reference Pointers, since an
        // Objects.equals() will yield the same result
        assertThat(entity.toString(), is(not(found.toString())));
        // Now, we'll ensure that it is the same Entity by comparing the Id's
        assertThat(entity.getId(), is(found.getId()));
    }

    @Test(expected = ModelException.class)
    public void testDaoFindMemberByName() {
        final CommonDao dao = new CommonJpaDao(entityManager);
        dao.findMemberByNameCredential(Constants.ADMIN_ACCOUNT);
    }

    @Test
    public void testFindAll() {
        final String jql = "select m from MemberEntity m";
        final Query query = entityManager.createQuery(jql);
        final List<MemberEntity> found = query.getResultList();
        assertThat(found.isEmpty(), is(true));
    }
}
