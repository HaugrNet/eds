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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.EntryEntity;
import net.haugr.eds.core.model.entities.MetadataEntity;
import org.junit.jupiter.api.Test;

/**
 * The author and the owner tag of an entry (FEATURES.md), sealed with the
 * Circle Secret, and sealed anew when it is rotated (PLAN.md, Crypto C6).
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class EntrySealsTest {

    private static final String CIRCLE_ID = "d8838d7d-71e7-433d-8790-af7c080e9de9";
    private static final String MEMBER_ID = "d842fa67-5387-44e6-96e3-4e8a7ead4c8d";

    private final EntrySeals seals = new EntrySeals(new Crypto(Settings.getInstance()));
    private final SecretEDSKey current = Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
    private final SecretEDSKey retiring = Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
    private final SecretEDSKey other = Crypto.generateSymmetricKey(KeyAlgorithm.AES_GCM_256);
    private final AtomicInteger reads = new AtomicInteger();

    @Test
    void testTheRetiringSecretIsReadOnlyIfTheCurrentOneFails() {
        final Supplier<SecretEDSKey> previous = previous(retiring);

        assertEquals(MEMBER_ID, seals.openAuthor(current, previous, CIRCLE_ID, sealedWith(current)));
        assertEquals(0, reads.get());
        assertEquals(MEMBER_ID, seals.openAuthor(current, previous, CIRCLE_ID, sealedWith(retiring)));
        assertEquals(1, reads.get());
    }

    @Test
    void testAnAuthorWhichOpensWithNeitherSecretIsNone() {
        assertNull(seals.openAuthor(current, previous(retiring), CIRCLE_ID, sealedWith(other)));
        // Without a rotation there is no retiring secret.
        assertNull(seals.openAuthor(current, previous(null), CIRCLE_ID, sealedWith(retiring)));
        assertEquals(2, reads.get());
    }

    @Test
    void testAnEntryWithoutAuthorHasNone() {
        final EntryEntity entry = sealedWith(current);
        entry.setAuthor(null);

        assertNull(seals.openAuthor(current, previous(retiring), CIRCLE_ID, entry));
        assertEquals(0, reads.get());
        assertFalse(seals.reseal(retiring, current, CIRCLE_ID, entry));
        assertNull(entry.getAuthor());
    }

    @Test
    void testAnEntryIsSealedAnewWithTheNewSecret() {
        final EntryEntity entry = sealedWith(retiring);

        assertTrue(seals.reseal(retiring, current, CIRCLE_ID, entry));
        assertEquals(MEMBER_ID, seals.openAuthor(current, previous(null), CIRCLE_ID, entry));
        assertEquals(seals.ownerTag(current, CIRCLE_ID, MEMBER_ID), entry.getOwnerTag());

        // Once is enough: an entry sealed with the new secret stays as it is.
        final byte[] author = entry.getAuthor();
        assertFalse(seals.reseal(retiring, current, CIRCLE_ID, entry));
        assertArrayEquals(author, entry.getAuthor());
    }

    @Test
    void testAnEntrySealedWithAnotherSecretIsLeftAsItIs() {
        final EntryEntity entry = sealedWith(other);
        final byte[] author = entry.getAuthor();

        assertFalse(seals.reseal(retiring, current, CIRCLE_ID, entry));
        assertArrayEquals(author, entry.getAuthor());
        assertEquals(seals.ownerTag(other, CIRCLE_ID, MEMBER_ID), entry.getOwnerTag());
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    /** The retiring secret, as the manager gives it: counting how often it is read. */
    private Supplier<SecretEDSKey> previous(final SecretEDSKey secret) {
        return () -> {
            reads.incrementAndGet();
            return secret;
        };
    }

    private EntryEntity sealedWith(final SecretEDSKey circleSecret) {
        final MetadataEntity metadata = new MetadataEntity();
        metadata.setExternalId("8e9c7a10-6f0b-4d1e-9a51-3c2f4b6d7e81");
        final EntryEntity entry = new EntryEntity();
        entry.setMetadata(metadata);
        entry.setAuthor(seals.sealAuthor(circleSecret, CIRCLE_ID, MEMBER_ID));
        entry.setOwnerTag(seals.ownerTag(circleSecret, CIRCLE_ID, MEMBER_ID));
        return entry;
    }
}
