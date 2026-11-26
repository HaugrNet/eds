/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.core.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Supplier;
import jakarta.persistence.EntityManager;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.model.entities.EDSEntity;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.model.entities.DataEntity;
import net.haugr.eds.core.model.entities.DataTypeEntity;
import net.haugr.eds.core.model.entities.Externable;
import net.haugr.eds.core.model.entities.MasterKeyEntity;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.model.entities.MetadataEntity;
import net.haugr.eds.core.model.entities.SessionEntity;
import net.haugr.eds.core.model.entities.SettingEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;

/**
 * <p>Common DAO functionality, used throughout EDS.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public class CommonDao {

    protected static final String EXTERNAL_ID = "externalId";
    protected static final String PARENT_ID = "parentId";
    protected static final String MEMBER = "member";
    protected static final String STATUS = "status";
    protected static final String SINCE = "since";
    protected static final String NAME = "name";
    protected final EntityManager entityManager;
    // Changes of the stored bytes of Circles, not yet written; by the
    // internal ID of the Circle, in order, see changeStoredBytes.
    private final Map<Long, Long> storedBytesChanges = new TreeMap<>();
    private boolean storedBytesHeld = false;

    /**
     * The Entity Manager of this DAO, for a DAO of another kind which is to
     * work in the same persistence context.
     *
     * @return The Entity Manager
     */
    public EntityManager entityManager() {
        return entityManager;
    }

    /**
     * <p>Default Constructor.</p>
     *
     * @param entityManager EntityManager instance with transactional control
     */
    public CommonDao(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Prepares a new Named Query instance to be used directly.
     *
     * @param query Name of the Named Query
     * @return Prepared Query
     */
    public Query createNamedQuery(final String query) {
        return entityManager.createNamedQuery(query);
    }

    /**
     * <p>Persist, create or updates, an Entity. If it is derived from
     * {@link Externable}, then a new external Id (UUID) is set if it currently
     * does not have one.</p>
     *
     * <p>Regardless of an Entity is being created or updated, the method will
     * always update the alteration data and ensure that the creation date is
     * defined as well.</p>
     *
     * @param entity EDS Entity to persist (create or update)
     * @return Created or Updated EDS Entity
     */
    public EDSEntity save(final EDSEntity entity) {
        entity.setAltered(Utilities.newDate());
        final EDSEntity saved;

        if (entity.getId() == null) {
            if ((entity instanceof Externable externable) && (externable.getExternalId() == null)) {
                externable.setExternalId(UUID.randomUUID().toString());
            }
            entity.setAdded(Utilities.newDate());

            entityManager.persist(entity);
            saved = entity;
            if (entity instanceof CircleEntity circle) {
                // Every Circle has its row of stored bytes from the start;
                // without it, a change of them would be lost without notice.
                // The Circle's ID is set by the insert of persist (IDENTITY).
                entityManager.createNativeQuery("INSERT INTO eds_circle_storage (circle_id) VALUES (?)")
                        .setParameter(1, circle.getId())
                        .executeUpdate();
            } else if (entity instanceof DataEntity data) {
                // New content saved as a whole adds its parts to its Circle;
                // content is only ever given to a new entity (see setData).
                changeStoredBytes(data.getMetadata().getCircle().getId(), data.storedLength());
            }
        } else {
            saved = entityManager.merge(entity);
        }

        return saved;
    }

    /**
     * <p>Runs a read of many rows without the flush Hibernate does before
     * each query (ticket 28). That flush checks every entity of the
     * persistence context for changes, the byte arrays of the content among
     * them, so a read which runs a query for each of the rows it shows grew
     * with the square of them. What was changed before is written first; the
     * read itself must change nothing. The flush mode is set back afterwards,
     * so the rest of the transaction works as before.</p>
     *
     * @param read The read
     * @param <T>  What it gives
     * @return What the read gives
     */
    public <T> T reading(final Supplier<T> read) {
        entityManager.flush();
        // Hibernate's own mode, as JPA's has no MANUAL or ALWAYS to restore.
        // The session is the EntityManager's, not one to close here.
        final FlushMode mode = entityManager.unwrap(Session.class).getHibernateFlushMode();
        entityManager.unwrap(Session.class).setHibernateFlushMode(FlushMode.COMMIT);
        try {
            return read.get();
        } finally {
            entityManager.unwrap(Session.class).setHibernateFlushMode(mode);
        }
    }

    /**
     * The names of the Members of the given external IDs, in one query.
     *
     * @param externalIds External IDs of Members
     * @return The name of each Member found, by its external ID
     */
    public Map<String, String> findMemberNames(final Collection<String> externalIds) {
        final Map<String, String> names = HashMap.newHashMap(externalIds.size());
        if (!externalIds.isEmpty()) {
            for (final Object[] row : entityManager.createQuery("select m.externalId, m.name from MemberEntity m where m.externalId in :ids", Object[].class)
                    .setParameter("ids", externalIds)
                    .getResultList()) {
                names.put((String) row[0], (String) row[1]);
            }
        }
        return names;
    }

    /**
     * Takes entities out of the persistence context, so they aren't checked
     * for changes at the commit. Only for entities which were read and
     * have no change which isn't written yet.
     *
     * @param entities The entities
     */
    public void detach(final Collection<? extends EDSEntity> entities) {
        entities.forEach(entityManager::detach);
    }

    public <E extends EDSEntity> E find(final Class<E> edsEntity, final Long id) {
        return entityManager.find(edsEntity, id);
    }

    public <E extends EDSEntity> E find(final Class<E> edsEntity, final Long id, final LockModeType lockModeType) {
        return entityManager.find(edsEntity, id, lockModeType);
    }

    public <E extends Externable> E find(final Class<E> edsEntity, final String externalId) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<E> query = builder.createQuery(edsEntity);
        final Root<E> entity = query.from(edsEntity);
        query.select(entity).where(builder.equal(entity.get(EXTERNAL_ID), externalId));

        return findSingleRecord(entityManager.createQuery(query));
    }

    /**
     * <p>Uses the JPA {@link CriteriaBuilder} to create a new query to find
     * all matching records, sorted ascending according to the given
     * parameters.</p>
     *
     * <p>Text fields are sorted case-insensitively, so the order is the same
     * for all databases, regardless of their collation.</p>
     *
     * @param edsEntity The EDS Entity to find all records for
     * @param orderBy   The field to order the ascending sorting by
     * @param <E>       The EDS Entity to use in the query
     * @return List of sorted records from the database
     */
    public <E extends EDSEntity> List<E> findAllAscending(final Class<E> edsEntity, final String orderBy) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<E> query = builder.createQuery(edsEntity);
        final Root<E> entity = query.from(edsEntity);
        final Path<Object> field = entity.get(orderBy);
        if (String.class.equals(field.getJavaType())) {
            query.orderBy(builder.asc(builder.lower(field.as(String.class))));
        } else {
            query.orderBy(builder.asc(field));
        }

        return findList(entityManager.createQuery(query));
    }

    public <E extends EDSEntity> E getReference(final Class<E> edsEntity, final Long id) {
        return entityManager.getReference(edsEntity, id);
    }

    /**
     * Locks the row of a Circle until the end of the transaction, so writes
     * which must not run in parallel for one Circle are serialized.
     *
     * @param circle The Circle
     */
    public void lockCircle(final CircleEntity circle) {
        entityManager.lock(circle, LockModeType.PESSIMISTIC_WRITE);
    }

    /**
     * Finds a Circle and locks its row until the end of the transaction. A
     * Circle which is gone gives null, also one which was deleted while this
     * waited for the lock, so work which runs beside the requests (e.g. the
     * rotation of its Circle Secret) can leave it alone. This holds for a
     * Circle which isn't in the persistence context yet; one which is gets
     * its lock there, which fails if its row is gone.
     *
     * @param id Internal ID of the Circle
     * @return The locked Circle, or null if it is gone
     */
    public CircleEntity findAndLockCircle(final long id) {
        return entityManager.find(CircleEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    /**
     * Locks the row of a Member by its internal ID, if it is still there.
     * Members are locked before Circles, as the rotation of the MasterKey
     * takes them (ticket 1); one deleted while this waited gives false.
     *
     * @param id Internal ID of the Member
     * @return Whether the Member is there, and locked
     */
    public boolean lockMemberIfThere(final long id) {
        return !entityManager.createQuery("select m from MemberEntity m where m.id = :id", MemberEntity.class)
                .setParameter("id", id)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList()
                .isEmpty();
    }

    /**
     * Locks the row of a Circle by its internal ID, if it is still there: a
     * Circle deleted while this waited for its lock gives false, so the
     * request can say so (ticket 2).
     *
     * @param id Internal ID of the Circle
     * @return Whether the Circle is there, and locked
     */
    public boolean lockCircleIfThere(final long id) {
        return !entityManager.createQuery("select c from CircleEntity c where c.id = :id", CircleEntity.class)
                .setParameter("id", id)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList()
                .isEmpty();
    }

    /**
     * All Members, locked for an update, in the order of their IDs: for the
     * rotation of the MasterKey, which wraps every Member's keys anew.
     *
     * @return The Members
     */
    public List<MemberEntity> lockMembers() {
        return entityManager.createQuery("select m from MemberEntity m order by m.id", MemberEntity.class)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }

    /**
     * Locks the rows of several Circles, in the order of their IDs, so two
     * transactions which lock some of the same never wait for each other.
     *
     * @param circles The Circles
     */
    public void lockCircles(final Collection<CircleEntity> circles) {
        circles.stream()
                .collect(java.util.stream.Collectors.toMap(CircleEntity::getId, java.util.function.Function.identity(), (a, b) -> a, TreeMap::new))
                .values()
                .forEach(this::lockCircle);
    }

    /**
     * Deletes the content of all Data Objects of a Circle, before the Circle
     * itself and its Circle Secrets are deleted. Each content row goes before
     * its parts, which the database deletes with it (a bulk delete of the
     * entity would take the parts first): the rotation of the Circle Secret
     * holds a content row while it replaces its parts, so each would wait for
     * the other. The rows are locked first, in the order of their IDs (on
     * PostgreSQL; H2 locks in the order it finds them), so two transactions
     * which each hold some of them wait in one direction only.
     *
     * @param circle The Circle
     */
    public void deleteContentOfCircle(final CircleEntity circle) {
        entityManager.flush();
        entityManager.createNativeQuery("SELECT id FROM eds_data WHERE metadata_id IN "
                        + "(SELECT id FROM eds_metadata WHERE circle_id = ?) ORDER BY id FOR UPDATE")
                .setParameter(1, circle.getId())
                .getResultList();
        entityManager.createNativeQuery("DELETE FROM eds_data WHERE metadata_id IN (SELECT id FROM eds_metadata WHERE circle_id = ?)")
                .setParameter(1, circle.getId())
                .executeUpdate();
    }

    /**
     * Deletes a Circle, whose Trustees and content are deleted already; the
     * database takes its Circle Secrets, folders, features and invitations
     * with it.
     *
     * @param circle The Circle
     */
    public void deleteCircle(final CircleEntity circle) {
        entityManager.flush();
        entityManager.detach(circle);
        entityManager.createQuery("delete from CircleEntity c where c.id = :id")
                .setParameter("id", circle.getId())
                .executeUpdate();
    }

    /**
     * Reads the entity anew from the database, e.g. after the row of its
     * Circle was locked, so it shows what was committed until then. What
     * this transaction changed is written first, so it isn't lost.
     *
     * @param entity The entity
     * @param <E>    Entity type
     */
    public <E extends EDSEntity> void refresh(final E entity) {
        entityManager.flush();
        entityManager.refresh(entity);
    }

    /**
     * Deletes an entity. The database deletes the content of a Data Object
     * with it (ON DELETE CASCADE), which the code doesn't see, so what is
     * still stored for it is taken from its Circle's total first. The
     * callers delete the content before, so normally there is nothing left.
     *
     * @param entity The entity
     * @param <E>    Entity type
     */
    public <E extends EDSEntity> void delete(final E entity) {
        if ((entity instanceof MetadataEntity metadata) && (metadata.getId() != null)) {
            changeStoredBytes(metadata.getCircle().getId(), -storedLengthOfObject(metadata.getId()));
        } else if ((entity instanceof DataEntity data) && (data.getId() != null)) {
            changeStoredBytes(data.getMetadata().getCircle().getId(), -storedLengthOfData(data.getId()));
        }
        entityManager.remove(entity);
    }

    // =========================================================================
    // The stored bytes of each Circle (PLAN.md, Streaming, "The storage
    // ceiling: a total for each Circle")
    // -------------------------------------------------------------------------
    // eds_circle_storage.stored_bytes is the sum of part_length of the parts
    // of the Circle's objects. It changes in the same transaction as the
    // parts, and only by a statement which adds to it: the entities have no
    // version, so reading, adding and saving it would lose a change made
    // meanwhile. It isn't part of CircleEntity, so a save of the Circle can't
    // write an old value back.
    //   The statement holds the Circle's row of eds_circle_storage until the
    // commit, so writes to one Circle wait for each other there; never for
    // the row of eds_circles, which the delete of a Circle locks first (see
    // the schema). A request which streams content holds its changes until
    // the parts are written, to keep that wait short, and writes them in the
    // order of the Circles' IDs, so two moves between the same two Circles
    // can't each hold one and wait for the other.
    // =========================================================================

    /**
     * Changes the stored bytes of a Circle. The change is written at once,
     * unless the changes are held (see {@link #holdStoredBytes()}).
     *
     * @param circleId Internal ID of the Circle
     * @param bytes    Bytes added, negative for bytes removed
     */
    public void changeStoredBytes(final long circleId, final long bytes) {
        if (bytes != 0) {
            storedBytesChanges.merge(circleId, bytes, Long::sum);
            if (!storedBytesHeld) {
                writeStoredBytes();
            }
        }
    }

    /**
     * Holds the changes of the stored bytes until {@link #writeStoredBytes()},
     * e.g. while content is streamed after its thumbnails were deleted, or
     * for the two Circles of a move.
     */
    public void holdStoredBytes() {
        storedBytesHeld = true;
    }

    /**
     * Writes the changes of the stored bytes, in the order of the Circles'
     * IDs, and ends a hold. A Circle which is gone meanwhile has no row to
     * change, and nothing to keep.
     */
    public void writeStoredBytes() {
        storedBytesHeld = false;
        for (final Map.Entry<Long, Long> change : storedBytesChanges.entrySet()) {
            entityManager.createNativeQuery("UPDATE eds_circle_storage SET stored_bytes = stored_bytes + ? WHERE circle_id = ?")
                    .setParameter(1, change.getValue())
                    .setParameter(2, change.getKey())
                    .executeUpdate();
        }
        storedBytesChanges.clear();
    }

    /**
     * The stored bytes of all Circles together, with the changes of this DAO
     * which are held: a row for each Circle is read, not the parts.
     *
     * @return Stored bytes
     */
    public long storedBytes() {
        final long written = ((Number) entityManager.createNativeQuery("SELECT COALESCE(SUM(stored_bytes), 0) FROM eds_circle_storage")
                .getSingleResult()).longValue();
        long held = 0;
        for (final long change : storedBytesChanges.values()) {
            held += change;
        }

        return written + held;
    }

    /**
     * The stored length of the content of a Data Object, without reading it.
     *
     * @param metadataId Internal ID of the Data Object
     * @return Length of its parts, 0 if it has none
     */
    protected long storedLengthOfObject(final long metadataId) {
        return ((Number) entityManager.createNativeQuery("SELECT COALESCE(SUM(p.part_length), 0) FROM eds_data d "
                        + "JOIN eds_data_parts p ON p.data_id = d.id WHERE d.metadata_id = ?")
                .setParameter(1, metadataId)
                .getSingleResult()).longValue();
    }

    private long storedLengthOfData(final long dataId) {
        return ((Number) entityManager.createNativeQuery("SELECT COALESCE(SUM(part_length), 0) FROM eds_data_parts WHERE data_id = ?")
                .setParameter(1, dataId)
                .getSingleResult()).longValue();
    }

    /**
     * Ends one Session.
     *
     * @param session The Session to end
     */
    public void removeSession(final SessionEntity session) {
        entityManager.remove(session);
    }

    /**
     * Ends all Sessions of a Member (PLAN.md, "Several sessions").
     *
     * @param member The Member, who may be new and not yet saved
     */
    public void removeSessions(final MemberEntity member) {
        member.setSessionExpire(null);
        if (member.getId() != null) {
            entityManager
                    .createNamedQuery("session.removeByMember")
                    .setParameter(MEMBER, member)
                    .executeUpdate();
        }
    }

    /**
     * The Sessions of a Member, the one which expires first at the top.
     *
     * @param member The Member
     * @return Their Sessions
     */
    public List<SessionEntity> findSessions(final MemberEntity member) {
        return entityManager
                .createNamedQuery("session.findByMember", SessionEntity.class)
                .setParameter(MEMBER, member)
                .getResultList();
    }

    public MemberEntity findMemberByName(final String name) {
        final Query query = entityManager
                .createNamedQuery("member.findByName")
                .setParameter("name", name);

        return findSingleRecord(query);
    }

    public List<MemberEntity> findMemberByRole(final MemberRole role) {
        final Query query = entityManager
                .createNamedQuery("member.findByRole")
                .setParameter("role", role);

        return findList(query);
    }

    /**
     * The checksums are not having any uniqueness assigned to them, meaning
     * that there may exist multiple checksums with the same value - but as
     * all checksums are short-lived (hours), the problem is ignored.
     *
     * @param checksum Checksum of a Member SessionKey
     * @return MemberEntity with a matching SessionKey checksum
     */
    public SessionEntity findSessionByChecksum(final String checksum) {
        final Query query = entityManager
                .createNamedQuery("session.findByChecksum")
                .setParameter("checksum", checksum);

        return findSingleRecord(query);
    }

    public MemberEntity findMemberByNameAndCircleId(final String name, final String externalCircleId) {
        final Query query = entityManager
                .createNamedQuery("member.findByNameAndCircle")
                .setParameter("name", name)
                .setParameter("externalCircleId", externalCircleId);
        final List<MemberEntity> found = findList(query);

        if (found.isEmpty()) {
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING,
                    "No Trustee information found for member '" + name + "' and circle '" + externalCircleId + "'.");
        }

        return found.getFirst();
    }

    public List<TrusteeEntity> findTrusteesByMember(final MemberEntity member, final Set<TrustLevel> permissions) {
        final Query query = entityManager
                .createNamedQuery("trust.findByMember")
                .setParameter(MEMBER, member)
                .setParameter("permissions", permissions);

        return findList(query);
    }

    public List<TrusteeEntity> findTrusteesByMemberAndCircle(final MemberEntity member, final String externalCircleId, final Set<TrustLevel> permissions) {
        final Query query = entityManager
                .createNamedQuery("trust.findByMemberAndExternalCircleId")
                .setParameter(MEMBER, member)
                .setParameter("externalCircleId", externalCircleId)
                .setParameter("permissions", permissions);

        return findList(query);
    }

    public CircleEntity findCircleByName(final String name) {
        final Query query = entityManager
                .createNamedQuery("circle.findByName")
                .setParameter("name", name);

        return findSingleRecord(query);
    }

    public List<DataTypeEntity> findAllTypes() {
        final Query query = entityManager.createNamedQuery("type.findAll");

        return findList(query);
    }

    /**
     * Finds a unique DataType in the system. If none exists, or it is not
     * possible to find a unique record, then an Exception is thrown.
     *
     * @param name Name of the DataType to find
     * @return Found DataType Entity
     * @throws EDSException if no unique value could be found
     */
    public DataTypeEntity findDataTypeByName(final String name) {
        final Query query = entityManager
                .createNamedQuery("type.findByName")
                .setParameter("name", name);

        return findSingleRecord(query);
    }

    public long countDataTypeUsage(final DataTypeEntity dataType) {
        final Query query = entityManager
                .createNamedQuery("type.countUsage")
                .setParameter("type", dataType);

        return (long) query.getSingleResult();
    }

    public TrusteeEntity findTrusteeByCircleAndMember(final String externalCircleId, final String externalMemberId) {
        final Query query = entityManager
                .createNamedQuery("trustee.findByCircleAndMember")
                .setParameter("ecid", externalCircleId)
                .setParameter("emid", externalMemberId);

        return findSingleRecord(query);
    }

    public SettingEntity findSettingByKey(final StandardSetting setting) {
        final Query query = entityManager
                .createNamedQuery("setting.findByName")
                .setParameter("name", setting.getKey());

        return findSingleRecord(query);
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Detaches the entity from the persistence context, so later changes
     * made directly via JDBC aren't overwritten by a flush.
     *
     * @param entity Entity to detach
     */
    public void detach(final EDSEntity entity) {
        entityManager.detach(entity);
    }

    /**
     * Finds the current MasterKey record, i.e. the one with the highest
     * version, or null if no MasterKey was set up yet.
     *
     * @return Current MasterKey record, or null
     */
    public MasterKeyEntity findCurrentMasterKey() {
        final Query query = entityManager
                .createNamedQuery("masterkey.findAllByVersion")
                .setMaxResults(1);

        return findSingleRecord(query);
    }

    // =========================================================================
    // Authentication attempt limits (#68)
    // -------------------------------------------------------------------------
    // The columns failed_logins, first_failed_at and locked_until of
    // eds_members are only read and written here, by single statements, and
    // are not part of MemberEntity: so a request which saves its Member can't
    // write an old count back, and attempts at the same moment can't
    // overwrite each other's count, as each statement locks the row while it
    // changes it.
    // =========================================================================

    /**
     * <p>Counts an attempt at the passphrase of an account, before it is
     * checked, and locks the account when the count reaches the limit. The
     * window starts anew when there is none, when it has passed, or after a
     * lock which has ended. An attempt while the account is locked isn't
     * counted, and doesn't extend the lock.</p>
     *
     * <p>The first statement locks the row, so an attempt at the same moment
     * waits until this one is committed, and then counts on top of it, or
     * finds the account locked.</p>
     *
     * @param memberId    The internal Id of the account
     * @param now         The time of the attempt
     * @param windowStart Attempts before this time are outside the window
     * @param max         The number of attempts which locks the account
     * @param lockedUntil Until when the account is then locked
     * @return True if the passphrase may be tried, false if the account is
     * locked
     */
    public boolean beginAttempt(final long memberId, final LocalDateTime now, final LocalDateTime windowStart, final int max, final LocalDateTime lockedUntil) {
        final int counted = entityManager.createNativeQuery(
                        "update eds_members set" +
                        "  failed_logins = case when (first_failed_at is null) or (first_failed_at < :windowStart) or (locked_until is not null)" +
                        "                       then 1 else failed_logins + 1 end," +
                        "  first_failed_at = case when (first_failed_at is null) or (first_failed_at < :windowStart) or (locked_until is not null)" +
                        "                         then :now else first_failed_at end," +
                        "  locked_until = null " +
                        "where id = :id" +
                        "  and ((locked_until is null) or (locked_until <= :now))")
                .setParameter("id", memberId)
                .setParameter("now", now)
                .setParameter("windowStart", windowStart)
                .executeUpdate();
        if (counted > 0) {
            entityManager.createNativeQuery(
                            "update eds_members set locked_until = :lockedUntil " +
                            "where id = :id" +
                            "  and failed_logins >= :max")
                    .setParameter("id", memberId)
                    .setParameter("lockedUntil", lockedUntil)
                    .setParameter("max", max)
                    .executeUpdate();
        }

        return counted > 0;
    }

    /**
     * Clears the count of attempts, and a lock: after an accepted passphrase,
     * which proves the account's owner, and when the account gets a new
     * passphrase. Nothing is written when there is nothing to clear.
     *
     * @param memberId The internal Id of the account
     */
    public void clearFailedLogins(final long memberId) {
        entityManager.createNativeQuery(
                        "update eds_members set failed_logins = 0, first_failed_at = null, locked_until = null " +
                        "where id = :id" +
                        "  and ((failed_logins <> 0) or (first_failed_at is not null) or (locked_until is not null))")
                .setParameter("id", memberId)
                .executeUpdate();
    }

    /**
     * Lets those who come to a stone without accounts in one at a time,
     * until the transaction ends: the row of the schema version, which
     * every stone has, is locked for it. What is locked says nothing; it
     * is a row which is there before any account is.
     */
    public void lockForTheFirstAccount() {
        entityManager.createQuery("select v from VersionEntity v order by v.id asc")
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }

    public Long countMembers() {
        final Query query = entityManager.createNamedQuery("member.countMembers");
        final Object obj = findSingleRecord(query);

        return (Long) obj;
    }

    // =========================================================================
    // Internal Methods, handling the actual lookup to simplify error handling
    // =========================================================================

    /**
     * <p>Returns the first record matching the query. If no records are found,
     * a null is returned.</p>
     *
     * <p>This method is preferred to be used over the JPA Query method
     * {@link Query#getFirstResult()}, as it throws an exception if no result
     * was found. Rather than having Exception handling to deal with a fairly
     * common case - a null is returned so it can be dealt with in a better
     * way.</p>
     *
     * <p>The Java 8 {@link java.util.Optional} class was discarded, since all
     * it does is wrap the null in an Object, that as well requires a check.
     * This does not mean that the {@link java.util.Optional} class is useless,
     * only that in this case, it is simply not used as the null has a specific
     * meaning.</p>
     *
     * @param query JPA Query to run
     * @param <E>   Entity Type to return
     * @return First Entity matching Query or null
     */
    public static <E> E findSingleRecord(final Query query) {
        final List<E> found = findList(query);

        return found.isEmpty() ? null : found.getFirst();
    }

    /**
     * <p>Wrapper for the JPA {@link Query#getResultList()} method, as it may
     * (according to the specifications) return a null rather than an empty
     * list if no records could be found.</p>
     *
     * @param query JPA Query to execute
     * @param <E>   Entity Type to return a list if
     * @return List of found Entities or an empty list if none were found
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> findList(final Query query) {
        try {
            final List<E> list = query.getResultList();

            // JPA does not specify the exact behavior of the getResultList()
            // call, hence if a null is returned, we're converting it into an
            // empty list. Which is always easier to deal with.
            return (list != null) ? list : new ArrayList<>(0);
        } catch (IllegalStateException | PersistenceException e) {
            // This should be and will hopefully remain unreachable code.
            throw new EDSException(ReturnCode.DATABASE_ERROR, e.getMessage(), e);
        }
    }
}
