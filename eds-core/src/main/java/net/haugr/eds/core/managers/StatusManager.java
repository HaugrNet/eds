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
package net.haugr.eds.core.managers;

import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.CredentialType;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.api.responses.StatusKeyResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.enums.Status;
import net.haugr.eds.core.exceptions.AuthorizationException;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.model.SchemaManager;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.StatusKeyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Business logic of the status of a stone (STATUS.md): the status keys,
 * who may read the status, and the queries of the inventory.</p>
 *
 * <p>A status key reads the status and nothing else. It is 256 random
 * bits, returned once; the stone keeps its SHA-512 hash. The System
 * Administrator creates keys, each with a label, lists the labels and
 * withdraws a key.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class StatusManager extends AbstractManager<DataDao, EDSResponse, Authentication> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusManager.class);

    // The Circles whose stored bytes differ from the sum of their parts, or
    // which have no row of them (-1); in one statement, so both are read from
    // the same state of the database.
    private static final String DIFFERING_TOTALS = "SELECT c.id, COALESCE(s.stored_bytes, -1), COALESCE(SUM(p.part_length), 0) FROM eds_circles c "
            + "LEFT JOIN eds_circle_storage s ON s.circle_id = c.id "
            + "LEFT JOIN eds_metadata m ON m.circle_id = c.id "
            + "LEFT JOIN eds_data d ON d.metadata_id = m.id "
            + "LEFT JOIN eds_data_parts p ON p.data_id = d.id "
            + "GROUP BY c.id, s.stored_bytes "
            + "HAVING COALESCE(s.stored_bytes, -1) <> COALESCE(SUM(p.part_length), 0) "
            + "ORDER BY c.id";

    /** The parameter of the queries of the rotations of Circle Secrets. */
    private static final String RETIRING = "retiring";

    /** The length of a status key in bytes. */
    public static final int KEY_LENGTH = 32;

    /**
     * What a stone tells about its database.
     *
     * @param product    The kind of database
     * @param persistent False for a database which loses its data with the stone
     * @param mark       A mark of the database server, or null
     */
    public record Database(String product, Boolean persistent, String mark) {
    }

    public StatusManager(final Settings settings, final EntityManager entityManager) {
        super(settings, new DataDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EDSResponse perform(final Authentication request) {
        verifyRequest(request, Permission.SETTING);
        return new EDSResponse(member.getName() + " successfully authenticated.");
    }

    // =========================================================================
    // Who may read the status
    // =========================================================================

    /**
     * Authenticates the Session of the System Administrator.
     *
     * @param token Session token
     */
    public void administrator(final byte[] token) {
        verifyRequest(request(token), Permission.SETTING);
        if (member.getMemberRole() != MemberRole.ADMIN) {
            throw new AuthorizationException("The status is read by the System Administrator, or with a status key.");
        }
    }

    /**
     * The hash of a key, as it is kept.
     *
     * @param key The key
     * @return SHA-512 of the key, as hex
     */
    public static String checksum(final byte[] key) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512").digest(key));
        } catch (NoSuchAlgorithmException e) {
            throw new EDSException(ReturnCode.CRYPTO_ERROR, e.getMessage(), e);
        }
    }

    /**
     * True if the hash is that of a status key of this stone.
     *
     * @param checksum The hash of a key, see {@link #checksum(byte[])}
     * @return True for a status key
     */
    public boolean isStatusKey(final String checksum) {
        return !dao.entityManager()
                .createQuery("select k.id from StatusKeyEntity k where k.checksum = :checksum", Long.class)
                .setParameter("checksum", checksum)
                .setMaxResults(1)
                .getResultList().isEmpty();
    }

    // =========================================================================
    // The status keys
    // =========================================================================

    public StatusKeyResponse keys(final byte[] token) {
        administrator(token);

        final StatusKeyResponse response = new StatusKeyResponse();
        response.setKeys(labels());

        return response;
    }

    public StatusKeyResponse createKey(final byte[] token, final String label) {
        administrator(token);
        final String trimmed = (label != null) ? label.trim() : "";
        throwConditionalException(trimmed.isEmpty() || (trimmed.length() > Constants.MAX_NAME_LENGTH),
                ReturnCode.VERIFICATION_WARNING, "The label of a status key has 1 to " + Constants.MAX_NAME_LENGTH + " characters.");
        throwConditionalException(find(trimmed) != null,
                ReturnCode.IDENTIFICATION_WARNING, "A status key with the label '" + trimmed + "' exists already.");

        final byte[] key = new byte[KEY_LENGTH];
        RANDOM.nextBytes(key);
        final StatusKeyEntity entity = new StatusKeyEntity();
        entity.setLabel(trimmed);
        entity.setChecksum(checksum(key));
        dao.save(entity);

        final StatusKeyResponse response = new StatusKeyResponse("The status key '" + trimmed + "' was created. It is shown once.");
        response.setKey(Base64.getUrlEncoder().withoutPadding().encodeToString(key));
        response.setKeys(labels());
        java.util.Arrays.fill(key, (byte) 0);

        return response;
    }

    /**
     * Withdraws a status key.
     *
     * @param token Session token of the System Administrator
     * @param label The label of the key
     * @return The labels which are left, and in the message the hash which is no longer valid
     */
    public StatusKeyResponse deleteKey(final byte[] token, final String label) {
        administrator(token);
        final StatusKeyEntity entity = find((label != null) ? label.trim() : "");
        throwConditionalNullException(entity,
                ReturnCode.IDENTIFICATION_WARNING, "There is no status key with the label '" + label + "'.");
        withdrawn = entity.getChecksum();
        dao.delete(entity);

        final StatusKeyResponse response = new StatusKeyResponse("The status key '" + entity.getLabel() + "' was withdrawn.");
        final List<String> keys = new ArrayList<>(labels());
        keys.remove(entity.getLabel());
        response.setKeys(keys);

        return response;
    }

    private String withdrawn = null;

    /**
     * The hash of the key which {@link #deleteKey} withdrew.
     *
     * @return The hash, or null
     */
    public String withdrawn() {
        return withdrawn;
    }

    private StatusKeyEntity find(final String label) {
        final List<StatusKeyEntity> found = dao.entityManager()
                .createQuery("select k from StatusKeyEntity k where k.label = :label", StatusKeyEntity.class)
                .setParameter("label", label)
                .setMaxResults(1)
                .getResultList();
        return found.isEmpty() ? null : found.getFirst();
    }

    private List<String> labels() {
        return dao.entityManager()
                .createQuery("select k.label from StatusKeyEntity k order by k.label asc", String.class)
                .getResultList();
    }

    // =========================================================================
    // The inventory, and the database
    // =========================================================================

    /**
     * What the stone holds, in general numbers. Each is a query.
     *
     * @return The numbers by their names
     */
    public Map<String, Double> inventory() {
        final Map<String, Double> values = new LinkedHashMap<>();
        // The storage in use is the sum of the Circles' totals, as the
        // ceiling has it (PLAN.md, Streaming, "The storage ceiling").
        values.put("storageUsed", (double) dao.storedBytes());
        values.put("storageCeiling", (double) dao.withConnection(connection -> SchemaManager.ceiling(settings, connection)));
        checkStoredBytes();
        values.put("accounts", count("select count(m.id) from MemberEntity m where m.invitationExpire is null"));
        values.put("circles", count("select count(c.id) from CircleEntity c"));
        values.put("files", count("select count(m.id) from MetadataEntity m where m.type.name not in ('folder', 'thumbnail', 'entry')"));
        values.put("entries", count("select count(m.id) from MetadataEntity m where m.type.name = 'entry'"));
        values.put("sessions", (double) dao.entityManager()
                .createQuery("select count(s.id) from SessionEntity s where s.expire > :now", Long.class)
                .setParameter("now", Utilities.newDate())
                .getSingleResult());
        // The rotation of Circle Secrets (PLAN.md, Crypto C6): those due and
        // not yet started, those under way, and the objects still to go.
        values.put("rotationsDue", (double) dao.entityManager()
                .createQuery("select count(c.id) from CircleEntity c where c.rotateCircleKeyDue <= :now"
                        + " and not exists (select k.id from KeyEntity k where k.circle = c and k.status = :retiring)", Long.class)
                .setParameter("now", Utilities.newDate())
                .setParameter(RETIRING, Status.RETIRING)
                .getSingleResult());
        values.put("rotationsRunning", (double) dao.entityManager()
                .createQuery("select count(k.id) from KeyEntity k where k.status = :retiring", Long.class)
                .setParameter(RETIRING, Status.RETIRING)
                .getSingleResult());
        values.put("rotationObjects", (double) dao.entityManager()
                .createQuery("select count(d.id) from DataEntity d where d.key.status = :retiring", Long.class)
                .setParameter(RETIRING, Status.RETIRING)
                .getSingleResult());

        return values;
    }

    /**
     * Compares the stored bytes of each Circle with the sum of the parts of
     * its objects, and warns of each which differs. A difference is a path
     * which adds or removes parts without changing the total. Nothing is
     * corrected: a correction here would race the writers.
     *
     * @return Number of Circles whose total differs
     */
    public int checkStoredBytes() {
        return dao.withConnection(connection -> {
            int differing = 0;
            try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(DIFFERING_TOTALS)) {
                while (result.next()) {
                    differing++;
                    LOGGER.warn("The stored bytes of the Circle with the internal id {} are {}, but the parts of its objects have {} bytes.",
                            result.getLong(1), result.getLong(2), result.getLong(3));
                }
            }
            return differing;
        });
    }

    private double count(final String jpql) {
        return dao.entityManager().createQuery(jpql, Long.class).getSingleResult();
    }

    /**
     * The kind of database, and a mark for its server: stones on the same
     * database server have the same mark, without the address being told.
     *
     * @return What is told about the database
     */
    public Database database() {
        return dao.withConnection(connection -> {
            final java.sql.DatabaseMetaData metaData = connection.getMetaData();
            final boolean persistent = SchemaManager.persistentDatabase(connection);
            return new Database(metaData.getDatabaseProductName(), persistent, persistent ? mark(metaData.getURL()) : null);
        });
    }

    /**
     * The mark of a database server: a hash of the host and port of its
     * JDBC URL.
     *
     * @param url The JDBC URL
     * @return 16 hex characters, or null if the URL names no server
     */
    public static String mark(final String url) {
        if (url == null) {
            return null;
        }
        final int start = url.indexOf("//");
        if (start < 0) {
            return null;
        }
        String server = url.substring(start + 2);
        final int at = server.indexOf('@');
        if (at >= 0) {
            server = server.substring(at + 1);
        }
        for (final char end : new char[] { '/', '?', ';' }) {
            final int index = server.indexOf(end);
            if (index >= 0) {
                server = server.substring(0, index);
            }
        }
        if (server.isEmpty()) {
            return null;
        }
        try {
            final byte[] hash = MessageDigest.getInstance("SHA-256").digest(("eds/status/database|" + server.toLowerCase(java.util.Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new EDSException(ReturnCode.CRYPTO_ERROR, e.getMessage(), e);
        }
    }

    /**
     * Times a trivial query. The stone can't read the load of the
     * database, but the time its own work takes rises with it.
     *
     * @return Milliseconds
     */
    public double probe() {
        final long start = System.nanoTime();
        dao.withConnection(StatusManager::selectOne);
        return (System.nanoTime() - start) / 1_000_000.0;
    }

    private static boolean selectOne(final Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT 1")) {
            return result.next();
        }
    }

    private static Authentication request(final byte[] token) {
        throwConditionalException((token == null) || (token.length == 0),
                ReturnCode.AUTHENTICATION_WARNING, "The Session token is missing.");
        final Authentication request = new Authentication();
        request.setCredentialType(CredentialType.SESSION);
        request.setCredential(token);

        return request;
    }
}
