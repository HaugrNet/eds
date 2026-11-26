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
package net.haugr.eds.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.sql.DataSource;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.requests.MasterKeyRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.MasterKeyResponse;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <p>The rotation of the MasterKey and a Member changed meanwhile (ticket 13):
 * the rotation wraps every Member's keys anew, so it locks the Members
 * before it reads them, and a change committed meanwhile isn't overwritten.
 * In a database of its own, as the rotation is committed.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class MasterKeyRotationLockTest extends DatabaseSetup {

    // How long the holder waits for a row, before it counts as a deadlock.
    private static final int HOLDER_TIMEOUT_MILLIS = 3000;

    // A database of its own for each test, as each rotation is committed.
    private EntityManagerFactory own = null;
    private DataSource database = null;

    @BeforeEach
    void openADatabase() {
        final String url = "jdbc:h2:mem:masterkey-locks-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=20000";
        own = Persistence.createEntityManagerFactory("net.haugr.jpa", Map.of("jakarta.persistence.jdbc.url", url));
        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(url);
        dataSource.setUser("sa");
        database = dataSource;
    }

    @AfterEach
    void closeTheDatabase() throws SQLException {
        try (Connection connection = database.getConnection(); java.sql.Statement statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        }
        own.close();
    }

    @Test
    void testARotationDoesNotOverwriteAMemberChangedMeanwhile() throws Exception {
        final String changed = "changed meanwhile";
        final long member = idOf(MEMBER_2);

        try (Connection holder = database.getConnection()) {
            holder.setAutoCommit(false);
            // A change of the Member, not yet committed, holds its row.
            try (PreparedStatement statement = holder.prepareStatement("UPDATE eds_members SET name = ? WHERE id = ?")) {
                statement.setString(1, changed);
                statement.setLong(2, member);
                assertEquals(1, statement.executeUpdate());
            }
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                final Future<MasterKeyResponse> rotated = executor.submit(() -> inOwnTransaction(em -> {
                    final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
                    request.setSecret(NEW_MASTERKEY_SECRET.getBytes(StandardCharsets.UTF_8));
                    return new ManagementBean(em, settings).masterKey(request);
                }));
                awaitBlocked(rotated);
                assertFalse(rotated.isDone(), "The rotation didn't wait for the Member");

                holder.commit();
                final MasterKeyResponse response = rotated.get(30, TimeUnit.SECONDS);
                assertTrue(response.isOk(), response.getReturnMessage());
            }
        }

        // The rotation read the Member after the change, so it is kept.
        assertEquals(changed, nameOf(member));
    }

    @Test
    void testARotationLocksTheCirclesBeforeItDeletesInvitees() throws Exception {
        // Ticket 1: a pending invitee goes with its invitations, which the
        // rotation of a Circle Secret changes under the lock of its Circle;
        // so the rotation of the MasterKey locks the Circles before it
        // deletes anything.
        assertTrue(inOwnTransaction(em -> {
            final ProcessMemberRequest invite = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
            invite.setAction(Action.INVITE);
            invite.setNewAccountName("invited meanwhile");
            invite.setCircleId(CIRCLE_1_ID);
            return new ManagementBean(em, settings).processMember(invite);
        }).isOk());
        final long circle = single("SELECT id FROM eds_circles WHERE external_id = ?", CIRCLE_1_ID);

        try (Connection holder = database.getConnection()) {
            holder.setAutoCommit(false);
            try (java.sql.Statement statement = holder.createStatement()) {
                statement.execute("SET LOCK_TIMEOUT " + HOLDER_TIMEOUT_MILLIS);
            }
            // As the rotation of the Circle Secret: the Circle first.
            try (PreparedStatement statement = holder.prepareStatement("SELECT id FROM eds_circles WHERE id = ? FOR UPDATE")) {
                statement.setLong(1, circle);
                try (ResultSet locked = statement.executeQuery()) {
                    if (!locked.next()) {
                        fail("Nothing to lock.");
                    }
                }
            }
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                final Future<MasterKeyResponse> rotated = executor.submit(() -> inOwnTransaction(em -> {
                    final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
                    request.setSecret(NEW_MASTERKEY_SECRET.getBytes(StandardCharsets.UTF_8));
                    return new ManagementBean(em, settings).masterKey(request);
                }));
                awaitBlocked(rotated);
                assertFalse(rotated.isDone(), "The rotation didn't wait for the Circle");

                // The rotation has deleted nothing yet: the invitation is free.
                assertDoesNotThrow(() -> {
                    try (PreparedStatement statement = holder.prepareStatement("UPDATE eds_invitations SET key_id = key_id WHERE circle_id = ?")) {
                        statement.setLong(1, circle);
                        assertEquals(1, statement.executeUpdate());
                    }
                });
                holder.commit();

                final MasterKeyResponse response = rotated.get(30, TimeUnit.SECONDS);
                assertTrue(response.isOk(), response.getReturnMessage());
            }
        }
        assertEquals(0L, single("SELECT COUNT(*) FROM eds_invitations WHERE circle_id = ?", String.valueOf(circle)));
    }

    private long single(final String sql, final String value) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    fail("No result of " + sql);
                }
                return result.getLong(1);
            }
        }
    }

    private <T> T inOwnTransaction(final Function<EntityManager, T> work) {
        try (EntityManager em = own.createEntityManager()) {
            em.getTransaction().begin();
            try {
                final T result = work.apply(em);
                em.getTransaction().commit();
                return result;
            } finally {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
            }
        }
    }

    private long idOf(final String account) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT id FROM eds_members WHERE name = ?")) {
            statement.setString(1, account);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    fail("No Member " + account);
                }
                return result.getLong(1);
            }
        }
    }

    private String nameOf(final long member) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT name FROM eds_members WHERE id = ?")) {
            statement.setLong(1, member);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    fail("No Member " + member);
                }
                return result.getString(1);
            }
        }
    }

    /** Waits until a session waits for a lock, or the step has ended. */
    private void awaitBlocked(final Future<?> step) throws SQLException {
        final long until = System.currentTimeMillis() + 20_000;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS WHERE BLOCKER_ID IS NOT NULL")) {
            while ((System.currentTimeMillis() < until) && !step.isDone()) {
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next() && (result.getLong(1) >= 1)) {
                        return;
                    }
                }
            }
        }
        if (!step.isDone()) {
            fail("The rotation didn't come to wait for the lock.");
        }
    }
}
