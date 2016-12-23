package io.javadog.cws.model.entities;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.common.exceptions.ModelException;
import io.javadog.cws.model.EntityManagerSetup;
import io.javadog.cws.model.ProcessMemberDao;
import io.javadog.cws.model.jpa.ProcessMemberJpaDao;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
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
public final class MemberEntityTest {

    // =========================================================================
    // Test Setup
    // =========================================================================
    private static final EntityManager entityManager = EntityManagerSetup.createEntityManagerInstance();

    @Before
    public void begin() {
        entityManager.getTransaction().begin();
    }

    @After
    public void rollback() {
        entityManager.getTransaction().rollback();
    }

    @AfterClass
    public static void close() {
        entityManager.close();
    }

    // =========================================================================
    // JUnit Tests
    // =========================================================================

    @Test
    public void testAddContent() {
        final MemberEntity entity = new MemberEntity();
        entity.setExternalId(UUID.randomUUID().toString());
        entity.setName(Constants.ADMIN_ACCOUNT);
        entity.setArmoredPublicKey(UUID.randomUUID().toString());
        entity.setArmoredEncryptedPrivateKey(UUID.randomUUID().toString());
        entity.setModified(new Date());
        entity.setCreated(new Date());
        assertThat(entity.getId(), is(nullValue()));

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
        final ProcessMemberDao dao = new ProcessMemberJpaDao(entityManager);
        dao.findMemberByName(Constants.ADMIN_ACCOUNT);
    }

    @Test
    public void testFindAll() {
        final String jql = "select m from MemberEntity m";
        final Query query = entityManager.createQuery(jql);
        final List<MemberEntity> found = query.getResultList();
        assertThat(found.isEmpty(), is(true));
    }
}
