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

import net.haugr.eds.core.features.EntryQuery;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.FeatureState;
import net.haugr.eds.api.dtos.Entry;
import net.haugr.eds.api.requests.EntryRequest;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.CircleFeatureResponse;
import net.haugr.eds.api.responses.EntryResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.api.responses.FeatureResponse;
import net.haugr.eds.core.managers.FeatureManager;
import net.haugr.eds.core.model.entities.DataEntity;
import net.haugr.eds.core.model.entities.EntryEntity;
import net.haugr.eds.core.model.entities.MetadataEntity;
import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.features.FeatureEvent;
import org.junit.jupiter.api.Test;

/**
 * <p>Features (FEATURES.md): a feature is a declaration, and the stone stores and
 * guards its entries by the rules of it, without knowing what they are.
 * These tests go through every rule of the fixed set.</p>
 *
 * <p>In the test data, member1 administers circle1, member2 may write in
 * it, member3 may read, and member4 isn't in it.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class FeaturesTest extends DatabaseSetup {

    private static final String BOARD = "board";
    private static final String THREAD = "thread";
    private static final String THREAD_DECLARATION = """
            {"feature": "thread", "version": 1, "kinds": {
              "topic": {"add": "WRITE", "close": ["administrators"], "size": 100},
              "reply": {"add": "WRITE", "about": "topic", "close": ["administrators"], "size": 100}
            }}""";
    private static final String DECLARATION = """
            {"feature": "board", "version": 1, "signals": 1000, "kinds": {
              "note":   {"add": "WRITE", "change": ["author"], "remove": ["author", "administrators"], "close": ["administrators"], "size": 100},
              "pin":    {"add": "ADMIN", "change": ["adders"], "remove": ["adders"], "size": 100},
              "vote":   {"add": "READ", "change": ["author"], "remove": ["author"], "about": "note", "keep": "member-about", "size": 10},
              "status": {"add": "READ", "change": ["author"], "keep": "member", "size": 50},
              "log":    {"add": "WRITE", "keep": "count:3", "size": 50},
              "tick":   {"add": "WRITE", "keep": "age:3600", "size": 50}
            }}""";

    // =========================================================================
    // The features of the stone
    // =========================================================================

    @Test
    void testOnlyTheSystemAdministratorDeclaresFeatures() {
        final FeatureBean features = features();

        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), features.putFeature(session(MEMBER_1), BOARD, DECLARATION).getReturnCode());
        final FeatureResponse added = features.putFeature(session(Constants.ADMIN_ACCOUNT), BOARD, DECLARATION);
        assertTrue(added.isOk(), added.getReturnMessage());
        assertEquals("The feature 'board' was successfully added.", added.getReturnMessage());

        // Every Member reads what the stone offers.
        final FeatureResponse offered = features.features(session(MEMBER_3));
        assertTrue(offered.isOk());
        assertEquals(1, offered.getFeatures().size());
        assertEquals(BOARD, offered.getFeatures().getFirst().getName());
        assertEquals(1, offered.getFeatures().getFirst().getVersion());
        assertEquals(DECLARATION, offered.getFeatures().getFirst().getDeclaration());

        // A declaration is replaced, and counts from then on.
        final FeatureResponse replaced = features.putFeature(session(Constants.ADMIN_ACCOUNT), BOARD, DECLARATION.replace("\"version\": 1", "\"version\": 2"));
        assertEquals("The feature 'board' was successfully replaced.", replaced.getReturnMessage());
        assertEquals(2, features.features(session(MEMBER_3)).getFeatures().getFirst().getVersion());
    }

    @Test
    void testDeclarationsWhichAreRefused() {
        final FeatureBean features = features();
        final byte[] admin = session(Constants.ADMIN_ACCOUNT);

        // The name in the path is the name in the declaration.
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), features.putFeature(admin, "other", DECLARATION).getReturnCode());
        // Not a declaration.
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), features.putFeature(admin, BOARD, "{}").getReturnCode());
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), features.putFeature(admin, BOARD, null).getReturnCode());
        assertTrue(features.features(admin).getFeatures().isEmpty());
    }

    @Test
    void testAFeatureWhichACircleHasCantBeRemoved() {
        final FeatureBean features = features();
        final byte[] admin = session(Constants.ADMIN_ACCOUNT);
        offer(features);
        assertTrue(features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD, FeatureState.OFF).isOk());

        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), features.deleteFeature(session(MEMBER_1), BOARD).getReturnCode());
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), features.deleteFeature(admin, BOARD).getReturnCode());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), features.deleteFeature(admin, "unknown").getReturnCode());

        assertTrue(features.removeCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD).isOk());
        assertTrue(features.deleteFeature(admin, BOARD).isOk());
        assertTrue(features.features(admin).getFeatures().isEmpty());
    }

    // =========================================================================
    // The features of a Circle
    // =========================================================================

    @Test
    void testAdministratorsOfACircleSwitchItsFeatures() {
        final FeatureBean features = features();
        offer(features);

        // member2 may write in circle1, which isn't enough; member4 isn't in it.
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), features.setCircleFeature(session(MEMBER_2), CIRCLE_1_ID, BOARD, FeatureState.ON).getReturnCode());
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), features.setCircleFeature(session(MEMBER_4), CIRCLE_1_ID, BOARD, FeatureState.ON).getReturnCode());
        // Only what the stone offers.
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, "unknown", FeatureState.ON).getReturnCode());
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD, null).getReturnCode());
        assertTrue(features.circleFeatures(session(MEMBER_3), CIRCLE_1_ID).getFeatures().isEmpty());

        final CircleFeatureResponse on = features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD, FeatureState.ON);
        assertTrue(on.isOk(), on.getReturnMessage());
        assertEquals("The feature 'board' is switched on in the Circle 'circle1'.", on.getReturnMessage());

        // Every Trustee sees it, and it has its hidden folder.
        final CircleFeatureResponse seen = features.circleFeatures(session(MEMBER_3), CIRCLE_1_ID);
        assertEquals(1, seen.getFeatures().size());
        assertEquals(BOARD, seen.getFeatures().getFirst().getName());
        assertEquals(FeatureState.ON, seen.getFeatures().getFirst().getState());
        assertNotNull(folder());
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), features.circleFeatures(session(MEMBER_4), CIRCLE_1_ID).getReturnCode());
    }

    @Test
    void testDataRequestsDontWriteWhatBelongsToAFeature() {
        final FeatureBean features = features();
        on(features);
        final Entry note = add(features, MEMBER_2, "note", null, null, "a note").getEntries().getFirst();
        final ShareBean share = prepareShareBean();

        // Nothing is added to the folder of the feature.
        final ProcessDataRequest into = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "smuggled", 10);
        into.setFolderId(folder().getExternalId());
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), share.processData(into).getReturnCode());
        // The name of the folder is reserved in the root of every Circle.
        final ProcessDataRequest named = prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, ".Board", 10);
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), share.processData(named).getReturnCode());
        // An entry is neither updated nor deleted by the data requests.
        final ProcessDataRequest update = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        update.setAction(Action.UPDATE);
        update.setDataId(note.getEntryId());
        update.setDataName("renamed");
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), share.processData(update).getReturnCode());
        final ProcessDataRequest delete = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        delete.setAction(Action.DELETE);
        delete.setDataId(note.getEntryId());
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), share.processData(delete).getReturnCode());

        // They may read it, while the feature isn't switched off.
        final FetchDataRequest fetch = prepareRequest(FetchDataRequest.class, MEMBER_3);
        fetch.setDataId(note.getEntryId());
        assertTrue(share.fetchData(fetch).isOk());
        assertTrue(features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD, FeatureState.OFF).isOk());
        final FetchDataRequest hidden = prepareRequest(FetchDataRequest.class, MEMBER_3);
        hidden.setDataId(note.getEntryId());
        assertFalse(share.fetchData(hidden).isOk());
    }

    @Test
    void testReadOnlyIsAnArchive() {
        final FeatureBean features = features();
        on(features);
        final Entry note = add(features, MEMBER_2, "note", null, null, "a note").getEntries().getFirst();

        assertTrue(features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD, FeatureState.READONLY).isOk());
        assertEquals(1, features.entries(session(MEMBER_3), CIRCLE_1_ID, BOARD, EntryQuery.all()).getEntries().size());
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), add(features, MEMBER_2, "note", null, null, "another").getReturnCode());
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), features.changeEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, note.getEntryId(), body(null, null, null, "changed")).getReturnCode());
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), features.removeEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, note.getEntryId()).getReturnCode());
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), features.closeEntry(session(MEMBER_1), CIRCLE_1_ID, BOARD, note.getEntryId(), true).getReturnCode());
    }

    @Test
    void testSwitchedOffIsKeptAndHidden() {
        final FeatureBean features = features();
        on(features);
        final Entry note = add(features, MEMBER_2, "note", null, null, "a note").getEntries().getFirst();

        assertTrue(features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD, FeatureState.OFF).isOk());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), features.entries(session(MEMBER_1), CIRCLE_1_ID, BOARD, EntryQuery.all()).getReturnCode());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), features.entry(session(MEMBER_1), CIRCLE_1_ID, BOARD, note.getEntryId()).getReturnCode());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), features.count(session(MEMBER_1), CIRCLE_1_ID, BOARD, null, null).getReturnCode());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), add(features, MEMBER_2, "note", null, null, "another").getReturnCode());
        assertEquals(FeatureState.OFF, features.circleFeatures(session(MEMBER_3), CIRCLE_1_ID).getFeatures().getFirst().getState());

        // Switched on again, everything is as it was.
        assertTrue(features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD, FeatureState.ON).isOk());
        final Entry back = features.entry(session(MEMBER_3), CIRCLE_1_ID, BOARD, note.getEntryId()).getEntries().getFirst();
        assertEquals("a note", text(back));
        assertEquals(note.getPosition(), back.getPosition());
    }

    @Test
    void testRemovingAFeatureFromACircleTakesItsEntries() {
        final FeatureBean features = features();
        on(features);
        final Entry note = add(features, MEMBER_2, "note", null, null, "a note").getEntries().getFirst();
        add(features, MEMBER_3, "vote", note.getEntryId(), null, "yes");

        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), features.removeCircleFeature(session(MEMBER_2), CIRCLE_1_ID, BOARD).getReturnCode());
        final CircleFeatureResponse removed = features.removeCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD);
        assertTrue(removed.isOk(), removed.getReturnMessage());
        assertEquals("The feature 'board' was removed from the Circle 'circle1', with 2 entries.", removed.getReturnMessage());

        entityManager.flush();
        entityManager.clear();
        assertTrue(features.circleFeatures(session(MEMBER_1), CIRCLE_1_ID).getFeatures().isEmpty());
        assertNull(folder());
        assertTrue(entityManager.createQuery("select e from EntryEntity e", EntryEntity.class).getResultList().isEmpty());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), features.removeCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD).getReturnCode());

        // It starts anew when it is switched on again.
        assertTrue(features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD, FeatureState.ON).isOk());
        assertEquals(1L, add(features, MEMBER_2, "note", null, null, "first again").getEntries().getFirst().getPosition());
    }

    // =========================================================================
    // Adding entries
    // =========================================================================

    @Test
    void testWhoMayAdd() {
        final FeatureBean features = features();
        on(features);

        // A note needs WRITE, a pin ADMIN, a status only READ.
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), add(features, MEMBER_3, "note", null, null, "x").getReturnCode());
        assertTrue(add(features, MEMBER_2, "note", null, null, "x").isOk());
        assertTrue(add(features, MEMBER_1, "note", null, null, "x").isOk());
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), add(features, MEMBER_2, "pin", null, null, "x").getReturnCode());
        assertTrue(add(features, MEMBER_1, "pin", null, null, "x").isOk());
        assertTrue(add(features, MEMBER_3, "status", null, null, "x").isOk());
        // Not in the Circle.
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), add(features, MEMBER_4, "status", null, null, "x").getReturnCode());
    }

    @Test
    void testAnEntryNeedsAKindAndAContent() {
        final FeatureBean features = features();
        on(features);
        final byte[] member2 = session(MEMBER_2);

        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), add(features, MEMBER_2, "unknown", null, null, "x").getReturnCode());
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), add(features, MEMBER_2, null, null, null, "x").getReturnCode());
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), features.addEntry(member2, CIRCLE_1_ID, BOARD, null).getReturnCode());
        // The content: there, Base64, and within the size of the kind.
        final EntryRequest without = body("note", null, null, null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), features.addEntry(member2, CIRCLE_1_ID, BOARD, without).getReturnCode());
        final EntryRequest garbled = body("note", null, null, null);
        garbled.setContent("not base64 !!");
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), features.addEntry(member2, CIRCLE_1_ID, BOARD, garbled).getReturnCode());
        assertEquals(ReturnCode.CONTENT_TOO_LARGE.getCode(), add(features, MEMBER_2, "note", null, null, "x".repeat(101)).getReturnCode());
        assertTrue(add(features, MEMBER_2, "note", null, null, "x".repeat(100)).isOk());
        assertTrue(add(features, MEMBER_2, "note", null, null, "").isOk());
    }

    @Test
    void testWhatAnEntryIsAbout() {
        final FeatureBean features = features();
        on(features);
        final Entry note = add(features, MEMBER_2, "note", null, null, "a note").getEntries().getFirst();
        final Entry status = add(features, MEMBER_2, "status", null, null, "here").getEntries().getFirst();

        // A vote is about a note, a note about nothing.
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), add(features, MEMBER_2, "vote", null, null, "yes").getReturnCode());
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), add(features, MEMBER_2, "vote", status.getEntryId(), null, "yes").getReturnCode());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), add(features, MEMBER_2, "vote", MEMBER_1_ID, null, "yes").getReturnCode());
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), add(features, MEMBER_2, "note", note.getEntryId(), null, "x").getReturnCode());
        assertTrue(add(features, MEMBER_2, "vote", note.getEntryId(), null, "yes").isOk());
    }

    @Test
    void testAPageIsReadWithTheSameQueriesWhateverItsSize() {
        // Ticket 28: what each entry is about, its author and its content were
        // read with a query for each entry, and each query first checked all
        // entities read so far for changes.
        final FeatureBean features = features();
        final Listed listed = listed(features);
        entityManager.flush();
        entityManager.clear();
        final long[] few = statementsOfAPage(features, 5);

        for (int i = 0; i < 6; i++) {
            final Entry note = add(features, (i % 2 == 0) ? MEMBER_1 : MEMBER_2, "note", null, null, "more " + i).getEntries().getFirst();
            add(features, MEMBER_3, "vote", note.getEntryId(), null, "vote " + i);
        }
        assertTrue(add(features, MEMBER_1, "vote", listed.n1().getEntryId(), null, "one more").isOk());
        entityManager.flush();
        entityManager.clear();
        final long[] many = statementsOfAPage(features, 18);

        assertEquals(few[0], many[0], "the statements of a page of 5 and of 18 entries");
        // No flush before each of the page's queries: so the entities read
        // for an entry (it, its Metadata, ...) are checked for changes once,
        // not again before every later query. Without it, 11 checks more for
        // each entry; with it, 3.
        assertTrue(many[1] - few[1] <= 4L * (18 - 5),
                "checks for changes of a page of 5 and of 18 entries: " + few[1] + " and " + many[1]);
    }

    @Test
    void testAReadPageKeepsTheRestOfTheTransactionAsItWas() {
        // The read runs without a flush before each query; afterwards the
        // flush mode is the one before, and a change is written as usual.
        final FeatureBean features = features();
        final Listed listed = listed(features);
        entityManager.flush();
        entityManager.clear();
        final jakarta.persistence.FlushModeType before = entityManager.getFlushMode();
        assertEquals(5, features.entries(session(MEMBER_3), CIRCLE_1_ID, BOARD, EntryQuery.all().content(true)).getEntries().size());
        assertEquals(before, entityManager.getFlushMode());
        // The content read for the page isn't kept in the persistence context.
        assertTrue(entityManager.unwrap(org.hibernate.Session.class).getStatistics().getEntityKeys().stream()
                .noneMatch(key -> (key instanceof org.hibernate.engine.spi.EntityKey entity)
                        && entity.getEntityName().endsWith(".DataEntity")), "content kept after the page was read");

        assertTrue(features.changeEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, listed.n1().getEntryId(), body(null, null, null, "changed")).isOk());
        entityManager.flush();
        entityManager.clear();
        final List<Entry> read = features.entries(session(MEMBER_3), CIRCLE_1_ID, BOARD, EntryQuery.all().kind("note").oldestFirst(true).content(true)).getEntries();
        assertEquals("changed", text(read.getFirst()));
    }

    /** The statements and the checks for changes (dirty checks) of reading the page of all entries of the board with their content, which must have the given size. */
    private long[] statementsOfAPage(final FeatureBean features, final int size) {
        final byte[] token = session(MEMBER_3);
        final org.hibernate.stat.Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(org.hibernate.SessionFactory.class).getStatistics();
        statistics.clear();
        statistics.setStatisticsEnabled(true);
        final long[] dirtyChecks = new long[1];
        entityManager.unwrap(org.hibernate.engine.spi.SharedSessionContractImplementor.class).getEventListenerManager()
                .addListener(new org.hibernate.SessionEventListener() {
                    @Override
                    public void dirtyCalculationStart() {
                        dirtyChecks[0]++;
                    }
                });
        try {
            final List<Entry> page = features.entries(token, CIRCLE_1_ID, BOARD, EntryQuery.all().content(true)).getEntries();
            assertEquals(size, page.size());
            assertTrue(page.stream().allMatch(entry -> (entry.getAccountName() != null) && (text(entry) != null)));
            assertTrue(page.stream().filter(entry -> "vote".equals(entry.getKind())).allMatch(entry -> entry.getAbout() != null));
            return new long[]{statistics.getPrepareStatementCount(), dirtyChecks[0]};
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    @Test
    void testNamesAreUniqueInTheFeatureOfTheCircle() {
        final FeatureBean features = features();
        on(features);

        assertTrue(add(features, MEMBER_2, "note", null, "2026-11/first", "x").isOk());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), add(features, MEMBER_2, "note", null, "2026-11/first", "x").getReturnCode());
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), add(features, MEMBER_2, "note", null, "n".repeat(76), "x").getReturnCode());
    }

    @Test
    void testAnEntryNeedsAFeatureOfTheCircle() {
        final FeatureBean features = features();
        on(features);

        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), features.addEntry(session(MEMBER_2), CIRCLE_1_ID, "unknown", body("note", null, null, "x")).getReturnCode());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), features.addEntry(session(MEMBER_2), CIRCLE_2_ID, BOARD, body("note", null, null, "x")).getReturnCode());
    }

    @Test
    void testTheStoneNotesAuthorTimeAndPosition() {
        final FeatureBean features = features();
        on(features);

        final Entry first = add(features, MEMBER_2, "note", null, "a name", "the first").getEntries().getFirst();
        final Entry second = add(features, MEMBER_1, "note", null, null, "the second").getEntries().getFirst();

        assertEquals(1L, first.getPosition());
        assertEquals(2L, second.getPosition());
        assertEquals(MEMBER_2_ID, first.getMemberId());
        assertEquals(MEMBER_2, first.getAccountName());
        assertEquals(MEMBER_1_ID, second.getMemberId());
        assertEquals("note", first.getKind());
        assertEquals(BOARD, first.getFeature());
        assertEquals(CIRCLE_1_ID, first.getCircleId());
        assertEquals("a name", first.getName());
        assertNotNull(second.getName());
        assertNotNull(first.getAdded());
        assertNull(first.getChanged());
        assertNull(first.getAbout());
        assertFalse(first.getClosed());
        assertEquals("the first", text(first));

        // Read again, by someone else.
        final Entry read = features.entry(session(MEMBER_3), CIRCLE_1_ID, BOARD, first.getEntryId()).getEntries().getFirst();
        assertEquals("the first", text(read));
        assertEquals(MEMBER_2_ID, read.getMemberId());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), features.entry(session(MEMBER_3), CIRCLE_1_ID, BOARD, MEMBER_1_ID).getReturnCode());
    }

    @Test
    void testTheDatabaseShowsNeitherAuthorNorContent() {
        final FeatureBean features = features();
        on(features);
        final Entry note = add(features, MEMBER_2, "note", null, null, "nobody reads this in the database").getEntries().getFirst();
        entityManager.flush();
        entityManager.clear();

        final EntryEntity entry = entityManager.createQuery("select e from EntryEntity e where e.metadata.externalId = :id", EntryEntity.class)
                .setParameter("id", note.getEntryId()).getSingleResult();
        final String author = new String(entry.getAuthor(), StandardCharsets.ISO_8859_1);
        assertFalse(author.contains(MEMBER_2_ID));
        assertFalse(author.contains(MEMBER_2));
        assertFalse(entry.getOwnerTag().contains(MEMBER_2_ID));
        final DataEntity data = entityManager.createQuery("select d from DataEntity d where d.metadata.externalId = :id", DataEntity.class)
                .setParameter("id", note.getEntryId()).getSingleResult();
        assertFalse(new String(data.getData(), StandardCharsets.ISO_8859_1).contains("nobody reads this"));

        // The same Member has the same tag in a Circle, and another one elsewhere.
        final Entry again = add(features, MEMBER_2, "note", null, null, "x").getEntries().getFirst();
        assertTrue(features.setCircleFeature(session(MEMBER_1), CIRCLE_2_ID, BOARD, FeatureState.ON).isOk());
        final Entry elsewhere = features.addEntry(session(MEMBER_2), CIRCLE_2_ID, BOARD, body("note", null, null, "x")).getEntries().getFirst();
        entityManager.flush();
        entityManager.clear();
        assertEquals(entry.getOwnerTag(), ownerTag(again));
        assertNotEquals(entry.getOwnerTag(), ownerTag(elsewhere));
    }

    // =========================================================================
    // Changing, removing and closing
    // =========================================================================

    @Test
    void testOnlyItsAuthorChangesANote() {
        final FeatureBean features = features();
        on(features);
        final Entry note = add(features, MEMBER_2, "note", null, null, "by member2").getEntries().getFirst();

        // Not even an administrator.
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), features.changeEntry(session(MEMBER_1), CIRCLE_1_ID, BOARD, note.getEntryId(), body(null, null, null, "no")).getReturnCode());
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), features.changeEntry(session(MEMBER_3), CIRCLE_1_ID, BOARD, note.getEntryId(), body(null, null, null, "no")).getReturnCode());
        final EntryResponse changed = features.changeEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, note.getEntryId(), body(null, null, "renamed", "changed"));
        assertTrue(changed.isOk(), changed.getReturnMessage());
        final Entry after = features.entry(session(MEMBER_3), CIRCLE_1_ID, BOARD, note.getEntryId()).getEntries().getFirst();
        assertEquals("changed", text(after));
        assertEquals("renamed", after.getName());
        assertNotNull(after.getChanged());
        // The position and the author stay.
        assertEquals(note.getPosition(), after.getPosition());
        assertEquals(MEMBER_2_ID, after.getMemberId());
    }

    @Test
    void testOnlyTheNameOrOnlyTheContentIsChanged() {
        final FeatureBean features = features();
        on(features);
        final Entry note = add(features, MEMBER_2, "note", null, null, "by member2").getEntries().getFirst();
        assertTrue(features.changeEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, note.getEntryId(), body(null, null, "renamed", "changed")).isOk());

        assertTrue(features.changeEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, note.getEntryId(), body(null, null, "renamed again", null)).isOk());
        assertEquals("changed", text(features.entry(session(MEMBER_3), CIRCLE_1_ID, BOARD, note.getEntryId()).getEntries().getFirst()));
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), features.changeEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, note.getEntryId(), body(null, null, null, null)).getReturnCode());
        assertEquals(ReturnCode.CONTENT_TOO_LARGE.getCode(), features.changeEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, note.getEntryId(), body(null, null, null, "x".repeat(101))).getReturnCode());
    }

    @Test
    void testWhoMayChangePinsAndLogs() {
        final FeatureBean features = features();
        on(features);
        final Entry pin = add(features, MEMBER_1, "pin", null, null, "by member1").getEntries().getFirst();
        final Entry log = add(features, MEMBER_2, "log", null, null, "nobody changes a log").getEntries().getFirst();

        // A pin: everyone who may add one, which are the administrators.
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), features.changeEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, pin.getEntryId(), body(null, null, null, "no")).getReturnCode());
        assertTrue(features.changeEntry(session(MEMBER_1), CIRCLE_1_ID, BOARD, pin.getEntryId(), body(null, null, null, "yes")).isOk());

        // A log: nobody, also not its author.
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), features.changeEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, log.getEntryId(), body(null, null, null, "no")).getReturnCode());
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), features.removeEntry(session(MEMBER_1), CIRCLE_1_ID, BOARD, log.getEntryId()).getReturnCode());
    }

    @Test
    void testWhoMayRemoveAndWhatGoesAlong() {
        final FeatureBean features = features();
        on(features);
        final Entry mine = add(features, MEMBER_2, "note", null, null, "by member2").getEntries().getFirst();
        final Entry theirs = add(features, MEMBER_1, "note", null, null, "by member1").getEntries().getFirst();
        add(features, MEMBER_3, "vote", mine.getEntryId(), null, "yes");
        add(features, MEMBER_1, "vote", mine.getEntryId(), null, "no");
        final Entry other = add(features, MEMBER_3, "vote", theirs.getEntryId(), null, "yes").getEntries().getFirst();

        // A note: its author and the administrators.
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), features.removeEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, theirs.getEntryId()).getReturnCode());
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), features.removeEntry(session(MEMBER_3), CIRCLE_1_ID, BOARD, mine.getEntryId()).getReturnCode());

        // What is about an entry goes with it.
        final EntryResponse removed = features.removeEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, mine.getEntryId());
        assertTrue(removed.isOk(), removed.getReturnMessage());
        assertEquals(3, removed.getEntries().size());
        assertEquals(mine.getEntryId(), removed.getEntries().getLast().getEntryId());
        assertNull(removed.getEntries().getFirst().getContent());
        entityManager.flush();
        entityManager.clear();
        final List<Entry> left = features.entries(session(MEMBER_3), CIRCLE_1_ID, BOARD, EntryQuery.all().oldestFirst(true)).getEntries();
        assertEquals(List.of(theirs.getEntryId(), other.getEntryId()), left.stream().map(Entry::getEntryId).toList());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), features.removeEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, mine.getEntryId()).getReturnCode());

        // An administrator removes the note of another.
        final Entry again = add(features, MEMBER_2, "note", null, null, "by member2").getEntries().getFirst();
        assertTrue(features.removeEntry(session(MEMBER_1), CIRCLE_1_ID, BOARD, again.getEntryId()).isOk());
    }

    @Test
    void testWhoMayClose() {
        final FeatureBean features = features();
        on(features);
        final Entry note = add(features, MEMBER_2, "note", null, null, "settled").getEntries().getFirst();
        final Entry vote = add(features, MEMBER_3, "vote", note.getEntryId(), null, "yes").getEntries().getFirst();

        // A note is closed by the administrators; a vote by nobody.
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), features.closeEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, note.getEntryId(), true).getReturnCode());
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), features.closeEntry(session(MEMBER_3), CIRCLE_1_ID, BOARD, vote.getEntryId(), true).getReturnCode());
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), features.closeEntry(session(MEMBER_1), CIRCLE_1_ID, BOARD, note.getEntryId(), null).getReturnCode());
        final EntryResponse closed = features.closeEntry(session(MEMBER_1), CIRCLE_1_ID, BOARD, note.getEntryId(), true);
        assertTrue(closed.isOk(), closed.getReturnMessage());
        assertTrue(closed.getEntries().getFirst().getClosed());
    }

    @Test
    void testAClosedEntryStaysAsItIs() {
        final FeatureBean features = features();
        on(features);
        final Entry note = add(features, MEMBER_2, "note", null, null, "settled").getEntries().getFirst();
        final Entry vote = add(features, MEMBER_3, "vote", note.getEntryId(), null, "yes").getEntries().getFirst();
        assertTrue(features.closeEntry(session(MEMBER_1), CIRCLE_1_ID, BOARD, note.getEntryId(), true).isOk());

        // Neither it nor what is about it changes, and nothing new is about it.
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), features.changeEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, note.getEntryId(), body(null, null, null, "no")).getReturnCode());
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), features.removeEntry(session(MEMBER_1), CIRCLE_1_ID, BOARD, note.getEntryId()).getReturnCode());
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), features.changeEntry(session(MEMBER_3), CIRCLE_1_ID, BOARD, vote.getEntryId(), body(null, null, null, "no")).getReturnCode());
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), features.removeEntry(session(MEMBER_3), CIRCLE_1_ID, BOARD, vote.getEntryId()).getReturnCode());
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), add(features, MEMBER_2, "vote", note.getEntryId(), null, "late").getReturnCode());
        // Also not by replacing one's own vote.
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), add(features, MEMBER_3, "vote", note.getEntryId(), null, "no").getReturnCode());
        assertEquals("yes", text(features.entry(session(MEMBER_1), CIRCLE_1_ID, BOARD, vote.getEntryId()).getEntries().getFirst()));
    }

    @Test
    void testAnEntryOpenedAgainWorksAsBefore() {
        final FeatureBean features = features();
        on(features);
        final Entry note = add(features, MEMBER_2, "note", null, null, "settled").getEntries().getFirst();
        final Entry vote = add(features, MEMBER_3, "vote", note.getEntryId(), null, "yes").getEntries().getFirst();
        assertTrue(features.closeEntry(session(MEMBER_1), CIRCLE_1_ID, BOARD, note.getEntryId(), true).isOk());

        // Opened again, all that a closed entry refuses works.
        assertTrue(features.closeEntry(session(MEMBER_1), CIRCLE_1_ID, BOARD, note.getEntryId(), false).isOk());
        assertTrue(features.changeEntry(session(MEMBER_3), CIRCLE_1_ID, BOARD, vote.getEntryId(), body(null, null, null, "no")).isOk());
        assertTrue(add(features, MEMBER_2, "vote", note.getEntryId(), null, "late").isOk());
        assertTrue(features.removeEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, note.getEntryId()).isOk());
    }

    @Test
    void testWhatIsAboutAClosedEntryStaysAsItIs() {
        final FeatureBean features = features();
        assertTrue(features.putFeature(session(Constants.ADMIN_ACCOUNT), THREAD, THREAD_DECLARATION).isOk());
        assertTrue(features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, THREAD, FeatureState.ON).isOk());
        final Entry topic = features.addEntry(session(MEMBER_2), CIRCLE_1_ID, THREAD, body("topic", null, null, "a question")).getEntries().getFirst();
        final Entry reply = features.addEntry(session(MEMBER_2), CIRCLE_1_ID, THREAD, body("reply", topic.getEntryId(), null, "an answer")).getEntries().getFirst();

        // While the topic is open, a reply to it is closed and opened again.
        assertTrue(features.closeEntry(session(MEMBER_1), CIRCLE_1_ID, THREAD, reply.getEntryId(), true).isOk());
        assertTrue(features.closeEntry(session(MEMBER_1), CIRCLE_1_ID, THREAD, reply.getEntryId(), false).isOk());

        // Once the topic is closed, the reply is neither closed nor opened.
        assertTrue(features.closeEntry(session(MEMBER_1), CIRCLE_1_ID, THREAD, topic.getEntryId(), true).isOk());
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), features.closeEntry(session(MEMBER_1), CIRCLE_1_ID, THREAD, reply.getEntryId(), true).getReturnCode());
    }

    // =========================================================================
    // What is kept
    // =========================================================================

    @Test
    void testOnePerMember() {
        final FeatureBean features = features();
        on(features);
        final Entry first = add(features, MEMBER_3, "status", null, null, "here").getEntries().getFirst();
        final Entry other = add(features, MEMBER_2, "status", null, null, "there").getEntries().getFirst();

        final EntryResponse replaced = add(features, MEMBER_3, "status", null, null, "elsewhere");

        // The entry keeps its ID, and stands at the end.
        assertTrue(replaced.isOk(), replaced.getReturnMessage());
        assertEquals("The entry was successfully replaced.", replaced.getReturnMessage());
        final Entry second = replaced.getEntries().getFirst();
        assertEquals(first.getEntryId(), second.getEntryId());
        assertEquals("elsewhere", text(second));
        assertTrue(second.getPosition() > other.getPosition());
        assertNotNull(second.getChanged());
        assertEquals(2L, features.count(session(MEMBER_1), CIRCLE_1_ID, BOARD, "status", null).getCount());
        assertEquals("there", text(features.entry(session(MEMBER_1), CIRCLE_1_ID, BOARD, other.getEntryId()).getEntries().getFirst()));

        // A name given with the new one is the name of the entry.
        final EntryResponse renamed = add(features, MEMBER_3, "status", null, "away", "gone");
        assertTrue(renamed.isOk(), renamed.getReturnMessage());
        assertEquals("away", renamed.getEntries().getFirst().getName());
    }

    @Test
    void testOnePerMemberAboutEachEntry() {
        final FeatureBean features = features();
        on(features);
        final Entry one = add(features, MEMBER_2, "note", null, null, "one").getEntries().getFirst();
        final Entry two = add(features, MEMBER_2, "note", null, null, "two").getEntries().getFirst();

        final Entry first = add(features, MEMBER_3, "vote", one.getEntryId(), null, "yes").getEntries().getFirst();
        final Entry elsewhere = add(features, MEMBER_3, "vote", two.getEntryId(), null, "yes").getEntries().getFirst();
        add(features, MEMBER_1, "vote", one.getEntryId(), null, "no");
        final Entry again = add(features, MEMBER_3, "vote", one.getEntryId(), null, "no").getEntries().getFirst();

        assertEquals(first.getEntryId(), again.getEntryId());
        assertNotEquals(first.getEntryId(), elsewhere.getEntryId());
        assertEquals(one.getEntryId(), again.getAbout());
        final List<Entry> about = features.entries(session(MEMBER_2), CIRCLE_1_ID, BOARD, EntryQuery.all().kind("vote").about(one.getEntryId()).oldestFirst(true)).getEntries();
        assertEquals(2, about.size());
        assertEquals(List.of("no", "no"), about.stream().map(FeaturesTest::text).toList());
        assertEquals(3L, features.count(session(MEMBER_2), CIRCLE_1_ID, BOARD, "vote", null).getCount());
    }

    @Test
    void testTheNewestUpToANumber() {
        final FeatureBean features = features();
        on(features);
        for (int i = 1; i <= 5; i++) {
            assertTrue(add(features, MEMBER_2, "log", null, null, "line " + i).isOk());
        }
        entityManager.flush();
        entityManager.clear();

        final List<Entry> kept = features.entries(session(MEMBER_3), CIRCLE_1_ID, BOARD, EntryQuery.all().kind("log").oldestFirst(true)).getEntries();
        assertEquals(List.of("line 3", "line 4", "line 5"), kept.stream().map(FeaturesTest::text).toList());
    }

    @Test
    void testEntriesUpToAnAge() {
        final FeatureBean features = features();
        on(features);
        final Entry old = add(features, MEMBER_2, "tick", null, null, "old").getEntries().getFirst();
        add(features, MEMBER_2, "tick", null, null, "older");
        final Entry note = add(features, MEMBER_2, "note", null, null, "notes stay").getEntries().getFirst();
        entityManager.flush();
        // Two hours ago; a tick is kept for one.
        entityManager.createNativeQuery("update eds_entries set added = dateadd('HOUR', -2, added) where kind = 'tick'").executeUpdate();
        entityManager.createNativeQuery("update eds_entries set added = dateadd('HOUR', -2, added) where kind = 'note'").executeUpdate();
        entityManager.clear();
        final Entry fresh = add(features, MEMBER_2, "tick", null, null, "fresh").getEntries().getFirst();
        entityManager.flush();
        entityManager.clear();

        final List<Entry> kept = features.entries(session(MEMBER_3), CIRCLE_1_ID, BOARD, EntryQuery.all().oldestFirst(true)).getEntries();
        assertEquals(List.of(note.getEntryId(), fresh.getEntryId()), kept.stream().map(Entry::getEntryId).toList());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), features.entry(session(MEMBER_3), CIRCLE_1_ID, BOARD, old.getEntryId()).getReturnCode());
        // Also when the entries of the kind are asked for.
        final List<Entry> ticks = features.entries(session(MEMBER_3), CIRCLE_1_ID, BOARD, EntryQuery.all().kind("tick")).getEntries();
        assertEquals(List.of(fresh.getEntryId()), ticks.stream().map(Entry::getEntryId).toList());
    }

    // =========================================================================
    // Listing and counting
    // =========================================================================

    @Test
    void testListingInOrderAndFiltered() {
        final FeatureBean features = features();
        final Listed listed = listed(features);
        final byte[] reader = session(MEMBER_3);

        // The newest first, unless asked otherwise.
        assertEquals(List.of(5L, 4L, 3L, 2L, 1L), positions(features.entries(reader, CIRCLE_1_ID, BOARD, EntryQuery.all())));
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L), positions(features.entries(reader, CIRCLE_1_ID, BOARD, EntryQuery.all().oldestFirst(true))));
        // By kind, by what an entry is about, by the start of the name.
        assertEquals(List.of(3L, 2L, 1L), positions(features.entries(reader, CIRCLE_1_ID, BOARD, EntryQuery.all().kind("note"))));
        assertEquals(List.of(5L), positions(features.entries(reader, CIRCLE_1_ID, BOARD, EntryQuery.all().about(listed.n2().getEntryId()))));
        assertTrue(features.entries(reader, CIRCLE_1_ID, BOARD, EntryQuery.all().about(listed.n1().getEntryId())).getEntries().isEmpty());
        assertEquals(List.of(2L, 1L), positions(features.entries(reader, CIRCLE_1_ID, BOARD, EntryQuery.all().namePrefix("2026-11/"))));
        // The name is taken as it is, not as a pattern.
        assertEquals(List.of(4L), positions(features.entries(reader, CIRCLE_1_ID, BOARD, EntryQuery.all().namePrefix("100%_"))));
        assertTrue(features.entries(reader, CIRCLE_1_ID, BOARD, EntryQuery.all().namePrefix("%")).getEntries().isEmpty());
    }

    @Test
    void testListingInPages() {
        final FeatureBean features = features();
        listed(features);
        final byte[] reader = session(MEMBER_3);

        // From a position.
        assertEquals(List.of(5L, 4L), positions(features.entries(reader, CIRCLE_1_ID, BOARD, EntryQuery.all().limit(2))));
        assertEquals(List.of(3L, 2L), positions(features.entries(reader, CIRCLE_1_ID, BOARD, EntryQuery.all().before(4L).limit(2))));
        assertEquals(List.of(4L, 5L), positions(features.entries(reader, CIRCLE_1_ID, BOARD, EntryQuery.all().after(3L).oldestFirst(true))));
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), features.entries(reader, CIRCLE_1_ID, BOARD, EntryQuery.all().limit(0)).getReturnCode());
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), features.entries(reader, CIRCLE_1_ID, BOARD, EntryQuery.all().limit(Constants.MAX_ENTRY_PAGE + 1)).getReturnCode());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), features.entries(reader, CIRCLE_1_ID, BOARD, EntryQuery.all().about(MEMBER_1_ID)).getReturnCode());
    }

    @Test
    void testListingWithTheContentOrWithout() {
        final FeatureBean features = features();
        final Listed listed = listed(features);
        final byte[] reader = session(MEMBER_3);

        assertEquals("p", text(features.entries(reader, CIRCLE_1_ID, BOARD, EntryQuery.all().kind("pin")).getEntries().getFirst()));
        assertNull(features.entries(reader, CIRCLE_1_ID, BOARD, EntryQuery.all().kind("pin").content(false)).getEntries().getFirst().getContent());
        assertEquals(listed.pin().getEntryId(), features.entries(reader, CIRCLE_1_ID, BOARD, EntryQuery.all().kind("pin").content(false)).getEntries().getFirst().getEntryId());
    }

    @Test
    void testCounting() {
        final FeatureBean features = features();
        final Listed listed = listed(features);
        final byte[] reader = session(MEMBER_3);

        // Counting, which is how a client finds what wasn't seen yet.
        assertEquals(5L, features.count(reader, CIRCLE_1_ID, BOARD, null, null).getCount());
        assertEquals(3L, features.count(reader, CIRCLE_1_ID, BOARD, "note", null).getCount());
        assertEquals(2L, features.count(reader, CIRCLE_1_ID, BOARD, "note", listed.n1().getPosition()).getCount());
        assertEquals(0L, features.count(reader, CIRCLE_1_ID, BOARD, "note", listed.n3().getPosition()).getCount());
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), features.count(session(MEMBER_4), CIRCLE_1_ID, BOARD, null, null).getReturnCode());
    }

    // =========================================================================
    // What the others are told
    // =========================================================================

    @Test
    void testWhatHappenedIsCollected() {
        on(features());
        final String circleId = CIRCLE_1_ID;

        final FeatureManager adding = new FeatureManager(settings, entityManager);
        final Entry note = adding.addEntry(session(MEMBER_2), circleId, BOARD, body("note", null, null, "x")).getEntries().getFirst();
        assertEquals(1, adding.events().size());
        assertEquals(FeatureEvent.Change.ADDED, adding.events().getFirst().change());
        assertEquals(note.getEntryId(), adding.events().getFirst().entry().getEntryId());
        assertEquals(BOARD, adding.events().getFirst().feature());
        assertEquals(circleId, adding.events().getFirst().circleId());
        new FeatureManager(settings, entityManager).addEntry(session(MEMBER_3), circleId, BOARD, body("vote", note.getEntryId(), null, "yes"));

        final FeatureManager changing = new FeatureManager(settings, entityManager);
        changing.changeEntry(session(MEMBER_2), circleId, BOARD, note.getEntryId(), body(null, null, null, "y"));
        assertEquals(List.of(FeatureEvent.Change.CHANGED), changing.events().stream().map(FeatureEvent::change).toList());

        // Removing tells about what went along, the deepest first.
        final FeatureManager removing = new FeatureManager(settings, entityManager);
        removing.removeEntry(session(MEMBER_2), circleId, BOARD, note.getEntryId());
        assertEquals(List.of(FeatureEvent.Change.REMOVED, FeatureEvent.Change.REMOVED), removing.events().stream().map(FeatureEvent::change).toList());
        assertEquals("vote", removing.events().getFirst().entry().getKind());
        assertEquals(note.getEntryId(), removing.events().getLast().entry().getEntryId());

        final FeatureManager switching = new FeatureManager(settings, entityManager);
        switching.setCircleFeature(session(MEMBER_1), circleId, BOARD, FeatureState.OFF);
        assertEquals(List.of(FeatureEvent.Change.FEATURE), switching.events().stream().map(FeatureEvent::change).toList());
        assertNull(switching.events().getFirst().entry());

        // A refused request tells nothing.
        final FeatureBean refused = features();
        assertFalse(refused.addEntry(session(MEMBER_2), circleId, BOARD, body("note", null, null, "x")).isOk());
    }

    @Test
    void testRemovingTakesAlongWhatIsAboutItAsDeepAsItGoes() {
        // A chain of kinds, each about the one before: as deep as removing looks.
        final StringBuilder kinds = new StringBuilder();
        for (int i = 0; i <= 16; i++) {
            final String about = (i > 0) ? ", \"about\": \"k" + (i - 1) + '"' : "";
            kinds.append((i > 0) ? ", " : "").append("\"k").append(i).append("\": {\"add\": \"WRITE\", \"remove\": [\"author\"]").append(about).append(", \"size\": 10}");
        }
        final String declaration = "{\"feature\": \"chain\", \"version\": 1, \"kinds\": {" + kinds + "}}";
        final FeatureBean features = features();
        assertTrue(features.putFeature(session(Constants.ADMIN_ACCOUNT), "chain", declaration).isOk());
        assertTrue(features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, "chain", FeatureState.ON).isOk());
        final byte[] writer = session(MEMBER_2);
        String about = null;
        String first = null;
        for (int i = 0; i <= 16; i++) {
            final EntryResponse added = features.addEntry(writer, CIRCLE_1_ID, "chain", body("k" + i, about, null, "x"));
            assertTrue(added.isOk(), added.getReturnMessage());
            about = added.getEntries().getFirst().getEntryId();
            first = (first != null) ? first : about;
        }

        final EntryResponse removed = features.removeEntry(writer, CIRCLE_1_ID, "chain", first);
        assertTrue(removed.isOk(), removed.getReturnMessage());
        assertEquals(0L, features.count(writer, CIRCLE_1_ID, "chain", null, null).getCount());
    }

    @Test
    void testWhatARequestLacks() {
        final FeatureBean features = features();
        on(features);
        final Entry note = add(features, MEMBER_2, "note", null, null, "a note").getEntries().getFirst();
        final byte[] member2 = session(MEMBER_2);

        // No Session token.
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), features.entries(null, CIRCLE_1_ID, BOARD, EntryQuery.all()).getReturnCode());
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), features.entries(new byte[0], CIRCLE_1_ID, BOARD, EntryQuery.all()).getReturnCode());
        // A name which can't be a feature's.
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, "Not A Feature", FeatureState.ON).getReturnCode());
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), features.entries(member2, CIRCLE_1_ID, "Not A Feature", EntryQuery.all()).getReturnCode());
        // No entry, no change, and a name of blanks only.
        assertEquals(ReturnCode.IDENTIFICATION_WARNING.getCode(), features.entry(member2, CIRCLE_1_ID, BOARD, null).getReturnCode());
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), features.changeEntry(member2, CIRCLE_1_ID, BOARD, note.getEntryId(), null).getReturnCode());
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), add(features, MEMBER_2, "note", null, "   ", "x").getReturnCode());
        assertEquals("a note", text(features.entry(member2, CIRCLE_1_ID, BOARD, note.getEntryId()).getEntries().getFirst()));
    }

    @Test
    void testADamagedEntryDoesntHideTheOthers() {
        final FeatureBean features = features();
        on(features);
        final Entry garbled = add(features, MEMBER_2, "note", null, null, "garbled").getEntries().getFirst();
        final Entry emptied = add(features, MEMBER_2, "note", null, null, "emptied").getEntries().getFirst();
        add(features, MEMBER_2, "note", null, null, "whole");
        entityManager.flush();

        // The content and the author of one are garbled; the content of another is gone.
        final DataEntity data = data(garbled);
        data.setData(new byte[data.getData().length]);
        entity(garbled).setAuthor(new byte[48]);
        entityManager.remove(data(emptied));
        entityManager.flush();
        entityManager.clear();

        final List<Entry> listed = features.entries(session(MEMBER_3), CIRCLE_1_ID, BOARD, EntryQuery.all().oldestFirst(true)).getEntries();
        assertEquals(3, listed.size());
        assertEquals(garbled.getEntryId(), listed.getFirst().getEntryId());
        assertNull(listed.getFirst().getContent());
        assertNull(listed.getFirst().getMemberId());
        assertNull(listed.getFirst().getAccountName());
        assertEquals(emptied.getEntryId(), listed.get(1).getEntryId());
        assertNull(listed.get(1).getContent());
        assertEquals(MEMBER_2, listed.get(1).getAccountName());
        assertEquals("whole", text(listed.get(2)));
    }

    @Test
    void testAFeatureWhoseFolderIsGoneIsRemovedAllTheSame() {
        final FeatureBean features = features();
        on(features);
        entityManager.remove(folder());
        entityManager.flush();

        final CircleFeatureResponse removed = features.removeCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD);
        assertTrue(removed.isOk(), removed.getReturnMessage());
        assertEquals("The feature 'board' was removed from the Circle 'circle1', with 0 entries.", removed.getReturnMessage());
        assertTrue(features.circleFeatures(session(MEMBER_1), CIRCLE_1_ID).getFeatures().isEmpty());
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private FeatureBean features() {
        return new FeatureBean(entityManager, settings);
    }

    private void offer(final FeatureBean features) {
        final FeatureResponse response = features.putFeature(session(Constants.ADMIN_ACCOUNT), BOARD, DECLARATION);
        assertTrue(response.isOk(), response.getReturnMessage());
    }

    /** The stone offers the board, and circle1 has it switched on. */
    private void on(final FeatureBean features) {
        offer(features);
        final CircleFeatureResponse response = features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD, FeatureState.ON);
        assertTrue(response.isOk(), response.getReturnMessage());
    }

    /** The entries the listing tests list: three notes, a pin, and a vote about the second note. */
    private record Listed(Entry n1, Entry n2, Entry n3, Entry pin) {}

    private Listed listed(final FeatureBean features) {
        on(features);
        final Entry n1 = add(features, MEMBER_2, "note", null, "2026-11/a", "n1").getEntries().getFirst();
        final Entry n2 = add(features, MEMBER_2, "note", null, "2026-11/b", "n2").getEntries().getFirst();
        final Entry n3 = add(features, MEMBER_2, "note", null, "2026-12/a", "n3").getEntries().getFirst();
        final Entry pin = add(features, MEMBER_1, "pin", null, "100%_sure", "p").getEntries().getFirst();
        add(features, MEMBER_3, "vote", n2.getEntryId(), null, "yes");
        return new Listed(n1, n2, n3, pin);
    }

    private EntryResponse add(final FeatureBean features, final String account, final String kind, final String about, final String name, final String text) {
        return features.addEntry(session(account), CIRCLE_1_ID, BOARD, body(kind, about, name, text));
    }

    private static EntryRequest body(final String kind, final String about, final String name, final String text) {
        final EntryRequest body = new EntryRequest();
        body.setKind(kind);
        body.setAbout(about);
        body.setName(name);
        body.setContent((text != null) ? Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)) : null);
        return body;
    }

    private static String text(final Entry entry) {
        return new String(Base64.getDecoder().decode(entry.getContent()), StandardCharsets.UTF_8);
    }

    private static List<Long> positions(final EntryResponse response) {
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getEntries().stream().map(Entry::getPosition).toList();
    }

    private String ownerTag(final Entry entry) {
        return entityManager.createQuery("select e from EntryEntity e where e.metadata.externalId = :id", EntryEntity.class)
                .setParameter("id", entry.getEntryId()).getSingleResult().getOwnerTag();
    }

    private EntryEntity entity(final Entry entry) {
        return entityManager.createQuery("select e from EntryEntity e where e.metadata.externalId = :id", EntryEntity.class)
                .setParameter("id", entry.getEntryId()).getSingleResult();
    }

    private DataEntity data(final Entry entry) {
        return entityManager.createQuery("select d from DataEntity d where d.metadata.externalId = :id", DataEntity.class)
                .setParameter("id", entry.getEntryId()).getSingleResult();
    }

    private MetadataEntity folder() {
        final List<MetadataEntity> found = entityManager.createQuery("select m from MetadataEntity m where m.name = '.board' and m.circle.externalId = :circle", MetadataEntity.class)
                .setParameter("circle", CIRCLE_1_ID).getResultList();
        return found.isEmpty() ? null : found.getFirst();
    }

    private byte[] session(final String account) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, account);
        request.setAction(Action.LOGIN);
        final ProcessMemberResponse response = prepareManagementBean().processMember(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getSession();
    }
}
