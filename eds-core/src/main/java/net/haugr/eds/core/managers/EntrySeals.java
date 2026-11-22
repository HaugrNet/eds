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

import java.util.Arrays;
import java.util.Base64;
import java.util.function.Supplier;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.entities.EntryEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>What the stone keeps about the author of an entry of a feature
 * (FEATURES.md), without the database showing it: the author, sealed with
 * a subkey of the Circle Secret, and the owner tag, a keyed value of the
 * Circle Secret by which a Member's own entries are found. Both are made
 * anew when the Circle Secret is rotated (PLAN.md, Crypto C6).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class EntrySeals {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntrySeals.class);
    private static final String SUBKEY_SALT = "eds/circle";
    private static final String OWNER_LABEL = "eds/entry/owner|";
    private static final String AUTHOR_LABEL = "eds/entry/author|";

    private final Crypto crypto;

    EntrySeals(final Crypto crypto) {
        this.crypto = crypto;
    }

    private SecretEDSKey subkey(final SecretEDSKey circleSecret, final String label) {
        final byte[] secret = circleSecret.getEncoded();
        try {
            return Crypto.deriveKey(secret, crypto.stringToBytes(SUBKEY_SALT), crypto.stringToBytes(label));
        } finally {
            Arrays.fill(secret, (byte) 0);
        }
    }

    /**
     * A keyed value of the Circle Secret for a Member, by which their own
     * entries are found.
     *
     * @param circleSecret The Circle Secret
     * @param circleId     The Circle
     * @param memberId     The Member
     * @return The owner tag
     */
    String ownerTag(final SecretEDSKey circleSecret, final String circleId, final String memberId) {
        final byte[] tag = subkey(circleSecret, OWNER_LABEL + circleId + '|' + memberId).getEncoded();
        try {
            return Base64.getEncoder().encodeToString(tag);
        } finally {
            Arrays.fill(tag, (byte) 0);
        }
    }

    byte[] sealAuthor(final SecretEDSKey circleSecret, final String circleId, final String memberId) {
        final byte[] label = crypto.stringToBytes(AUTHOR_LABEL + circleId);
        return Crypto.seal(subkey(circleSecret, AUTHOR_LABEL + circleId), label, crypto.stringToBytes(memberId));
    }

    /**
     * Opens the author of an entry.
     *
     * @param circleSecret The Circle Secret it was sealed with
     * @param circleId     The Circle
     * @param entry        The entry
     * @return The Member's external ID, or null if it can't be opened
     */
    String openAuthor(final SecretEDSKey circleSecret, final String circleId, final EntryEntity entry) {
        final String author = tryOpenAuthor(circleSecret, circleId, entry);
        if ((author == null) && (entry.getAuthor() != null)) {
            LOGGER.warn("The author of the entry {} can't be read.", entry.getMetadata().getExternalId());
        }
        return author;
    }

    /**
     * Opens the author of an entry while the Circle Secret may be rotated
     * (PLAN.md, Crypto C6): the entries are sealed anew one by one, so it is
     * either secret.
     *
     * @param current  The current Circle Secret
     * @param previous The retiring one, read only if needed; null without a rotation
     * @param circleId The Circle
     * @param entry    The entry
     * @return The Member's external ID, or null if it can't be opened
     */
    String openAuthor(final SecretEDSKey current, final Supplier<SecretEDSKey> previous, final String circleId, final EntryEntity entry) {
        String author = tryOpenAuthor(current, circleId, entry);
        final SecretEDSKey retiring = ((author == null) && (entry.getAuthor() != null)) ? previous.get() : null;
        if (retiring != null) {
            author = tryOpenAuthor(retiring, circleId, entry);
        }
        if ((author == null) && (entry.getAuthor() != null)) {
            LOGGER.warn("The author of the entry {} can't be read.", entry.getMetadata().getExternalId());
        }
        return author;
    }

    private String tryOpenAuthor(final SecretEDSKey circleSecret, final String circleId, final EntryEntity entry) {
        if (entry.getAuthor() == null) {
            return null;
        }
        try {
            final byte[] label = crypto.stringToBytes(AUTHOR_LABEL + circleId);
            return crypto.bytesToString(Crypto.open(subkey(circleSecret, AUTHOR_LABEL + circleId), label, entry.getAuthor()));
        } catch (RuntimeException e) {
            LOGGER.debug("The author of the entry {} doesn't open with this secret.", entry.getMetadata().getExternalId(), e);
            return null;
        }
    }

    /**
     * Seals the author and the owner tag of an entry anew, under another
     * Circle Secret: when the Circle Secret is rotated. An entry sealed
     * with the new secret already is left as it is.
     *
     * @param from     The Circle Secret they may be sealed with
     * @param to       The new Circle Secret
     * @param circleId The Circle
     * @param entry    The entry, which is changed
     * @return True if the entry was changed
     */
    boolean reseal(final SecretEDSKey from, final SecretEDSKey to, final String circleId, final EntryEntity entry) {
        if (tryOpenAuthor(to, circleId, entry) != null) {
            return false;
        }
        final String memberId = tryOpenAuthor(from, circleId, entry);
        if (memberId == null) {
            return false;
        }
        entry.setAuthor(sealAuthor(to, circleId, memberId));
        entry.setOwnerTag(ownerTag(to, circleId, memberId));
        return true;
    }
}
