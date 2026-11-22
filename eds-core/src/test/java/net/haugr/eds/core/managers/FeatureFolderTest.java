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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.FeatureState;
import net.haugr.eds.api.dtos.Entry;
import net.haugr.eds.api.requests.EntryRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.EntryResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.core.FeatureBean;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.model.FeatureDao;
import net.haugr.eds.core.model.entities.MetadataEntity;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The rules for the folders of the features of a Circle (FEATURES.md): what
 * belongs to a feature, which names are reserved, and what is hidden while
 * a feature is switched off.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class FeatureFolderTest extends DatabaseSetup {

    private static final String BOARD = "board";
    private static final String DECLARATION = """
            {"feature": "board", "version": 1, "kinds": {"note": {"add": "WRITE", "size": 100}}}""";

    private final DataDao dataDao = new DataDao(entityManager);
    private final FeatureDao featureDao = new FeatureDao(entityManager);
    private FeatureBean features = null;
    private Entry note = null;

    @BeforeEach
    void switchOn() {
        features = new FeatureBean(entityManager, settings);
        assertTrue(features.putFeature(session(Constants.ADMIN_ACCOUNT), BOARD, DECLARATION).isOk());
        assertTrue(features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD, FeatureState.ON).isOk());
        final EntryRequest body = new EntryRequest();
        body.setKind("note");
        body.setContent(Base64.getEncoder().encodeToString("a note".getBytes(StandardCharsets.UTF_8)));
        final EntryResponse added = features.addEntry(session(MEMBER_2), CIRCLE_1_ID, BOARD, body);
        assertTrue(added.isOk(), added.getReturnMessage());
        note = added.getEntries().getFirst();
    }

    @Test
    void testNothingBelongsToAFeature() {
        assertFalse(FeatureFolder.isRoot(null));
        assertFalse(FeatureFolder.isEntry(null));
        assertFalse(FeatureFolder.isFeatureFolder(dataDao, null));
        assertFalse(FeatureFolder.isFeature(dataDao, null));
        assertFalse(FeatureFolder.isSwitchedOff(dataDao, null));
        assertFalse(FeatureFolder.isReservedName(dataDao, null, ".board"));
    }

    @Test
    void testTheFolderAndTheEntriesBelongToTheFeature() {
        final MetadataEntity folder = metadata("m.name = '.board'");
        final MetadataEntity entry = metadata("m.externalId = '" + note.getEntryId() + "'");

        // With either DAO, as the data requests have their own.
        for (final DataDao dao : new DataDao[] { dataDao, featureDao }) {
            assertTrue(FeatureFolder.isFeatureFolder(dao, folder));
            assertTrue(FeatureFolder.isFeature(dao, folder));
            assertTrue(FeatureFolder.isFeature(dao, entry));
            assertFalse(FeatureFolder.isFeatureFolder(dao, entry));
        }
        assertTrue(FeatureFolder.isEntry(entry));
        assertFalse(FeatureFolder.isEntry(folder));
        assertFalse(FeatureFolder.isRoot(folder));
        assertTrue(FeatureFolder.isRoot(root()));
    }

    @Test
    void testOnlyAFolderOfAFeatureIsTheFolderOfAFeature() {
        final MetadataEntity root = root();

        // A folder without a name, with another name, or with a dot but of no feature.
        final MetadataEntity unnamed = folder(root, null);
        assertFalse(FeatureFolder.isFeatureFolder(featureDao, unnamed));
        assertFalse(FeatureFolder.isSwitchedOff(featureDao, unnamed));
        assertFalse(FeatureFolder.isFeatureFolder(featureDao, folder(root, "notes")));
        final MetadataEntity hidden = folder(root, ".hidden");
        assertFalse(FeatureFolder.isFeatureFolder(featureDao, hidden));
        assertFalse(FeatureFolder.isSwitchedOff(featureDao, hidden));
    }

    @Test
    void testTheNameOfTheFolderIsReservedInTheRootOnly() {
        final MetadataEntity root = root();

        assertTrue(FeatureFolder.isReservedName(dataDao, root, " .Board "));
        assertTrue(FeatureFolder.isReservedName(featureDao, root, ".board"));
        assertFalse(FeatureFolder.isReservedName(dataDao, root, ".calendar"));
        assertFalse(FeatureFolder.isReservedName(dataDao, root, BOARD));
        assertFalse(FeatureFolder.isReservedName(dataDao, root, null));
        assertFalse(FeatureFolder.isReservedName(dataDao, metadata("m.name = '.board'"), ".board"));
    }

    @Test
    void testWhileTheFeatureIsSwitchedOffItsFolderAndEntriesAreHidden() {
        final MetadataEntity folder = metadata("m.name = '.board'");
        final MetadataEntity entry = metadata("m.externalId = '" + note.getEntryId() + "'");
        assertFalse(FeatureFolder.isSwitchedOff(dataDao, folder));
        assertFalse(FeatureFolder.isSwitchedOff(dataDao, entry));
        assertFalse(FeatureFolder.isSwitchedOff(dataDao, root()));

        assertTrue(features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD, FeatureState.READONLY).isOk());
        assertFalse(FeatureFolder.isSwitchedOff(dataDao, folder));
        assertTrue(features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, BOARD, FeatureState.OFF).isOk());
        assertTrue(FeatureFolder.isSwitchedOff(dataDao, folder));
        assertTrue(FeatureFolder.isSwitchedOff(featureDao, entry));
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private MetadataEntity root() {
        return metadata("m.parentId = 0");
    }

    private MetadataEntity metadata(final String condition) {
        return entityManager.createQuery("select m from MetadataEntity m where m.circle.externalId = :circle and " + condition, MetadataEntity.class)
                .setParameter("circle", CIRCLE_1_ID).getSingleResult();
    }

    /** A folder which isn't stored; its ID is that of the root, which is no feature's. */
    private MetadataEntity folder(final MetadataEntity root, final String name) {
        final MetadataEntity folder = new MetadataEntity();
        folder.setId(root.getId());
        folder.setParentId(root.getId());
        folder.setCircle(root.getCircle());
        folder.setType(root.getType());
        folder.setName(name);
        return folder;
    }

    private byte[] session(final String account) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, account);
        request.setAction(Action.LOGIN);
        final ProcessMemberResponse response = prepareManagementBean().processMember(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getSession();
    }
}
