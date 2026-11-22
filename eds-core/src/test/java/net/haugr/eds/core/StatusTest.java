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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.dtos.StatusSample;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.api.responses.StatusKeyResponse;
import net.haugr.eds.api.responses.StatusResponse;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.jce.MasterKey;
import net.haugr.eds.core.managers.StatusManager;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.StatusKeyEntity;
import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.status.StatusCollector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The status of a stone (STATUS.md): who may read it, the status keys, and
 * what it tells.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class StatusTest extends DatabaseSetup {

    private static final long SECOND = 1_000_000_000L;

    @BeforeEach
    @AfterEach
    void forget() {
        StatusCollector.getInstance().reset();
    }

    // =========================================================================
    // Status keys
    // =========================================================================

    @Test
    void testOnlyTheSystemAdministratorHasStatusKeys() {
        final StatusBean status = status();

        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), status.createKey(session(MEMBER_1), "mine").getReturnCode());
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), status.keys(session(MEMBER_1)).getReturnCode());
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), status.deleteKey(session(MEMBER_1), "mine").getReturnCode());
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), status.keys(new byte[32]).getReturnCode());
    }

    @Test
    void testStatusKeysAreCreatedAndListedByTheirLabels() {
        final StatusBean status = status();

        final StatusKeyResponse created = status.createKey(session(Constants.ADMIN_ACCOUNT), " overview at home ");
        assertTrue(created.isOk(), created.getReturnMessage());
        assertEquals(List.of("overview at home"), created.getKeys());
        assertEquals(StatusManager.KEY_LENGTH, Base64.getUrlDecoder().decode(created.getKey()).length);
        assertTrue(status.createKey(session(Constants.ADMIN_ACCOUNT), "a script").isOk());

        final StatusKeyResponse listed = status.keys(session(Constants.ADMIN_ACCOUNT));
        assertEquals(List.of("a script", "overview at home"), listed.getKeys());
        assertNull(listed.getKey());
    }

    @Test
    void testALabelIsThereOnceAndHasALength() {
        final StatusBean status = status();
        assertTrue(status.createKey(session(Constants.ADMIN_ACCOUNT), "a script").isOk());

        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), status.createKey(session(Constants.ADMIN_ACCOUNT), "a script").getReturnCode());
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), status.createKey(session(Constants.ADMIN_ACCOUNT), "  ").getReturnCode());
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), status.createKey(session(Constants.ADMIN_ACCOUNT), null).getReturnCode());
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), status.createKey(session(Constants.ADMIN_ACCOUNT), "x".repeat(76)).getReturnCode());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), status.deleteKey(session(Constants.ADMIN_ACCOUNT), "unknown").getReturnCode());
    }

    @Test
    void testOnlyTheHashOfAKeyIsKept() {
        final StatusKeyResponse created = status().createKey(session(Constants.ADMIN_ACCOUNT), "hashed");
        final byte[] key = Base64.getUrlDecoder().decode(created.getKey());
        entityManager.flush();
        entityManager.clear();

        final StatusKeyEntity entity = entityManager.createQuery("select k from StatusKeyEntity k where k.label = 'hashed'", StatusKeyEntity.class).getSingleResult();
        assertEquals(StatusManager.checksum(key), entity.getChecksum());
        assertFalse(entity.getChecksum().contains(created.getKey()));
        assertEquals(128, entity.getChecksum().length());
    }

    @Test
    void testTheStatusKeysNeedASession() {
        final StatusBean status = status();

        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), status.keys(null).getReturnCode());
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), status.keys(new byte[0]).getReturnCode());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), status.deleteKey(session(Constants.ADMIN_ACCOUNT), null).getReturnCode());
    }

    @Test
    void testTheManagerAuthenticatesTheSystemAdministrator() {
        final StatusManager manager = new StatusManager(settings, entityManager);

        final EDSResponse response = manager.perform(prepareSessionRequest(Authentication.class, session(Constants.ADMIN_ACCOUNT)));
        assertTrue(response.isOk(), response.getReturnMessage());
        assertTrue(response.getReturnMessage().endsWith(" successfully authenticated."), response.getReturnMessage());
    }

    // =========================================================================
    // Who reads the status
    // =========================================================================

    @Test
    void testAStatusKeyReadsTheStatus() {
        final StatusBean status = status();
        final byte[] key = key(status, "overview");

        final StatusResponse response = status.status(key, null, null);

        assertTrue(response.isOk(), response.getReturnMessage());
        assertEquals(Constants.EDS_VERSION, response.getVersion());
        assertTrue(response.getAwake());
        assertNotNull(response.getTime());
        assertNotNull(response.getStarted());
        assertNotNull(response.getLoad());
        assertNotNull(response.getInventory());
        // The day comes only when asked for.
        assertNull(response.getDay());
        assertNotNull(status.status(key, null, true).getDay());
    }

    @Test
    void testWhoElseReadsTheStatus() {
        final StatusBean status = status();
        key(status, "overview");

        // The System Administrator with their Session.
        assertTrue(status.status(session(Constants.ADMIN_ACCOUNT), null, null).isOk());
        // Nobody else, and no key which isn't one.
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), status.status(session(MEMBER_1), null, null).getReturnCode());
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), status.status(new byte[32], null, null).getReturnCode());
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), status.status(new byte[16], null, null).getReturnCode());
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), status.status(new byte[0], null, null).getReturnCode());
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), status.status(null, null, null).getReturnCode());
    }

    @Test
    void testAWithdrawnKeyIsRefusedAtOnce() {
        final StatusBean status = status();
        final byte[] key = key(status, "to withdraw");
        final byte[] other = key(status, "to keep");
        assertTrue(status.status(key, null, null).isOk());

        final StatusKeyResponse withdrawn = status.deleteKey(session(Constants.ADMIN_ACCOUNT), "to withdraw");
        assertTrue(withdrawn.isOk(), withdrawn.getReturnMessage());
        assertEquals(List.of("to keep"), withdrawn.getKeys());

        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), status.status(key, null, null).getReturnCode());
        assertTrue(status.status(other, null, null).isOk());
    }

    @Test
    void testASleepingStoneAnswersAStatusKey() {
        final StatusBean status = status();
        final byte[] key = key(status, "overview");
        final byte[] admin = session(Constants.ADMIN_ACCOUNT);

        MasterKey.getInstance().lock();

        final StatusResponse asleep = status.status(key, null, null);
        assertTrue(asleep.isOk(), asleep.getReturnMessage());
        assertFalse(asleep.getAwake());
        // A Session can't be read without the MasterKey.
        assertFalse(status.status(admin, null, null).isOk());
    }

    @Test
    void testAStatusKeyReadsNothingElse() {
        final StatusBean status = status();
        final byte[] key = key(status, "overview");

        // It is no Session: neither the keys nor anything of the other beans.
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), status.keys(key.clone()).getReturnCode());
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), new FeatureBean(entityManager, settings).features(key.clone()).getReturnCode());
    }

    // =========================================================================
    // What the status tells
    // =========================================================================

    @Test
    void testLoadAndInventoryAreGathered() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.STATUS_LOAD_INTERVAL.getKey(), "1");
        mySettings.set(StandardSetting.STATUS_INVENTORY_INTERVAL.getKey(), "1");
        mySettings.set(StandardSetting.STONE_NAME.getKey(), " north ");
        final StatusBean status = new StatusBean(entityManager, mySettings);
        final byte[] key = key(status, "overview");
        // Someone is active.
        session(MEMBER_1);
        session(MEMBER_2);

        status.tick(SECOND);
        status.tick(SECOND + 400_000_000L);
        status.tick(2 * SECOND);
        status.tick(3 * SECOND);

        final StatusResponse response = status.status(key, null, true);
        assertTrue(response.isOk(), response.getReturnMessage());
        assertEquals("north", response.getName());
        assertEquals("H2", response.getDatabase());
        assertFalse(response.getPersistent());
        // A database in memory has no server to mark.
        assertNull(response.getDatabaseMark());

        // Three samples of the load: the tick in between found nothing due.
        assertEquals(1L, response.getLoad().getInterval());
        assertEquals(List.of(1L, 2L, 3L), response.getLoad().getSamples().stream().map(StatusSample::getSequence).toList());
        final Map<String, Double> first = response.getLoad().getSamples().getFirst().getValues();
        assertTrue(first.get("accounts") >= 2, String.valueOf(first));
        assertTrue(first.get("databaseMillis") >= 0);
        assertEquals(List.of(3L), status.status(key, 2L, null).getLoad().getSamples().stream().map(StatusSample::getSequence).toList());
        assertFalse(response.getDay().getSamples().isEmpty());

        // One sample of the inventory, of the test data.
        assertEquals(60L, response.getInventory().getInterval());
        assertEquals(1, response.getInventory().getSamples().size());
        final Map<String, Double> inventory = response.getInventory().getSamples().getFirst().getValues();
        assertEquals(6.0, inventory.get("accounts"));
        assertEquals(3.0, inventory.get("circles"));
        assertTrue(inventory.get("sessions") >= 2);
        assertTrue(inventory.get("storageUsed") >= 0);
        assertTrue(inventory.get("storageCeiling") > 0);
        assertTrue(inventory.containsKey("files"));
        assertTrue(inventory.containsKey("entries"));
    }

    @Test
    void testTheLoadCanBeSwitchedOff() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.STATUS_LOAD_INTERVAL.getKey(), "0");
        final StatusBean status = new StatusBean(entityManager, mySettings);
        final byte[] key = key(status, "overview");

        status.tick(SECOND);
        status.tick(10 * SECOND);

        final StatusResponse response = status.status(key, null, true);
        assertTrue(response.getLoad().getSamples().isEmpty());
        assertEquals(0L, response.getLoad().getInterval());
        // The inventory is gathered all the same.
        assertEquals(1, response.getInventory().getSamples().size());
    }

    @Test
    void testOnlyNumbersAreTold() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.STATUS_LOAD_INTERVAL.getKey(), "1");
        final StatusBean status = new StatusBean(entityManager, mySettings);
        final byte[] key = key(status, "overview");
        session(MEMBER_1);
        status.tick(SECOND);

        final StatusResponse response = status.status(key, null, true);
        final StringBuilder told = new StringBuilder();
        for (final var series : List.of(response.getLoad(), response.getDay(), response.getInventory())) {
            for (final StatusSample sample : series.getSamples()) {
                told.append(sample.getValues().keySet());
            }
        }
        // No name of an account or a Circle, and no ID, anywhere.
        for (final String secret : List.of(MEMBER_1, MEMBER_1_ID, CIRCLE_1, CIRCLE_1_ID, Constants.ADMIN_ACCOUNT)) {
            assertFalse(told.toString().contains(secret), secret);
        }
        assertNull(response.getName());
    }

    @Test
    void testTheDatabaseIsTimedAgainAfterTenSeconds() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.STATUS_LOAD_INTERVAL.getKey(), "1");
        final StatusBean status = new StatusBean(entityManager, mySettings);
        final StatusCollector collector = StatusCollector.getInstance();

        status.tick(SECOND);
        assertTrue(collector.load(null).getSamples().getLast().getValues().containsKey("databaseMillis"));
        // Between two timings, the last number is told.
        collector.database(Double.NaN);
        status.tick(2 * SECOND);
        assertFalse(collector.load(null).getSamples().getLast().getValues().containsKey("databaseMillis"));
        status.tick(12 * SECOND);
        assertTrue(collector.load(null).getSamples().getLast().getValues().containsKey("databaseMillis"));
    }

    @Test
    void testADatabaseWhichIsntReachedIsToldAsSuch() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.STATUS_LOAD_INTERVAL.getKey(), "1");
        mySettings.set(StandardSetting.STATUS_INVENTORY_INTERVAL.getKey(), "2");
        final StatusBean status = new StatusBean(closedEntityManager(), mySettings);
        final StatusCollector collector = StatusCollector.getInstance();
        collector.database(1.0);

        status.tick(SECOND);

        // No number is better than an old one.
        final List<StatusSample> load = collector.load(null).getSamples();
        assertEquals(1, load.size());
        assertFalse(load.getFirst().getValues().containsKey("databaseMillis"));
        // No inventory, and the next try is after a minute, not the interval.
        assertTrue(collector.inventory(true).getSamples().isEmpty());
        assertFalse(collector.inventoryDue(30 * SECOND, 120));
        assertTrue(collector.inventoryDue(62 * SECOND, 120));
    }

    @Test
    void testTheMarkOfADatabaseServer() {
        final String mark = StatusManager.mark("jdbc:postgresql://db.example.com:5432/eds_one?ssl=true");

        assertNotNull(mark);
        assertEquals(16, mark.length());
        assertFalse(mark.contains("example"));
        // The same server, whatever the database, the user or the case.
        assertEquals(mark, StatusManager.mark("jdbc:postgresql://db.example.com:5432/eds_two"));
        assertEquals(mark, StatusManager.mark("jdbc:postgresql://user:secret@DB.example.com:5432/eds_two"));
        // Another server, another port.
        assertNotEquals(mark, StatusManager.mark("jdbc:postgresql://db2.example.com:5432/eds_one"));
        assertNotEquals(mark, StatusManager.mark("jdbc:postgresql://db.example.com:5433/eds_one"));
        // Nothing to mark.
        assertNull(StatusManager.mark("jdbc:h2:mem:eds"));
        assertNull(StatusManager.mark("jdbc:postgresql:///eds"));
        assertNull(StatusManager.mark(null));
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private StatusBean status() {
        return new StatusBean(entityManager, settings);
    }

    private byte[] key(final StatusBean status, final String label) {
        final StatusKeyResponse created = status.createKey(session(Constants.ADMIN_ACCOUNT), label);
        assertTrue(created.isOk(), created.getReturnMessage());
        return Base64.getUrlDecoder().decode(created.getKey());
    }

    private byte[] session(final String account) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, account);
        request.setAction(Action.LOGIN);
        final ProcessMemberResponse response = prepareManagementBean().processMember(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getSession();
    }
}
