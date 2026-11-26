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

import net.haugr.eds.core.features.EntryQuery;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.CredentialType;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.FeatureState;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.dtos.CircleFeature;
import net.haugr.eds.api.dtos.Entry;
import net.haugr.eds.api.dtos.Feature;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.EntryRequest;
import net.haugr.eds.api.responses.CircleFeatureResponse;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.api.responses.EntryResponse;
import net.haugr.eds.api.responses.FeatureResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.exceptions.AuthorizationException;
import net.haugr.eds.core.exceptions.IllegalActionException;
import net.haugr.eds.core.exceptions.VerificationException;
import net.haugr.eds.core.jce.DataFormat;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.FeatureDao;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.model.entities.CircleFeatureEntity;
import net.haugr.eds.core.model.entities.DataEntity;
import net.haugr.eds.core.model.entities.EntryEntity;
import net.haugr.eds.core.model.entities.MetadataEntity;
import net.haugr.eds.core.model.entities.FeatureEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;
import net.haugr.eds.core.features.FeatureDeclaration;
import net.haugr.eds.core.features.FeatureEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Business logic of the features (FEATURES.md). A feature is a declaration: the
 * kinds of its entries, and for each kind rules from the fixed set which
 * the stone knows. This class stores the entries and enforces the rules. It
 * never looks into the content of an entry, which is the clients' alone, in
 * a room and in a chamber alike.</p>
 *
 * <p>An entry is a Data Object of the type {@code entry} in the folder of
 * its feature, a hidden folder of the Circle's root folder, so its content is
 * encrypted with the Circle Secret like any other content (PLAN.md, Crypto
 * §4). Beside it, the stone notes the kind, the place in the order of the
 * feature, the entry it is about, whether it is closed, and the author. The
 * author is sealed with a subkey of the Circle Secret, and the stone finds a
 * Member's own entries by a keyed value of that secret, so a copy of the
 * database doesn't show who wrote what.</p>
 *
 * <p>Every call authenticates the Session token, like a request of the
 * resource API. What happened is collected in {@link #events()}, for those
 * who are told about it.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class FeatureManager extends AbstractManager<FeatureDao, EDSResponse, Authentication> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureManager.class);

    private static final String MEDIA_TYPE = "application/octet-stream";
    /** How far the stone follows what an entry is about, when it looks for a closed one. */
    private static final int MAX_DEPTH = 16;
    /** The default number of entries in a listing. */
    public static final int DEFAULT_PAGE = 50;

    private final List<FeatureEvent> events = new ArrayList<>();
    private final EntrySeals seals;
    /** The Trustee row the request reads with, for content on a retiring secret. */
    private TrusteeEntity reader = null;
    /** The retiring Circle Secret of the reader, while it is rotated; unwrapped once. */
    private SecretEDSKey retiring = null;

    public FeatureManager(final Settings settings, final EntityManager entityManager) {
        super(settings, new FeatureDao(entityManager));
        this.seals = new EntrySeals(crypto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EDSResponse perform(final Authentication request) {
        verifyRequest(request, Permission.FETCH_CIRCLE);
        return new EDSResponse(member.getName() + " successfully authenticated.");
    }

    /**
     * What the last call changed, in the order it happened.
     *
     * @return The events
     */
    public List<FeatureEvent> events() {
        return Collections.unmodifiableList(events);
    }

    // =========================================================================
    // For the socket
    // =========================================================================

    /**
     * The Member of a Session, for a connection of the socket.
     *
     * @param memberId      ID of the Member
     * @param accountName   Account name of the Member
     * @param sessionExpire When the Session ends
     */
    public record Identity(String memberId, String accountName, LocalDateTime sessionExpire) {
    }

    /**
     * Authenticates the Session token.
     *
     * @param token Session token
     * @return The Member
     */
    public Identity identify(final byte[] token) {
        perform(request(token));
        return new Identity(member.getExternalId(), member.getName(), member.getSessionExpire());
    }

    /**
     * The Circles which the Member of the Session may read, i.e. whose
     * changes a connection may be told about.
     *
     * @param token Session token
     * @return IDs of the Circles
     */
    public Set<String> readableCircles(final byte[] token) {
        verifyRequest(request(token), Permission.FETCH_CIRCLE);
        final List<TrusteeEntity> found = dao.findTrusteesByMember(member, TrustLevel.getLevels(TrustLevel.READ));
        final Set<String> circles = HashSet.newHashSet(found.size());
        for (final TrusteeEntity trustee : found) {
            circles.add(trustee.getCircle().getExternalId());
        }

        return circles;
    }

    /**
     * Checks that the Member may send a signal in a feature of a Circle: a
     * short text which is passed on to the others and not stored. Every
     * Trustee may, if the feature is switched on and declares signals.
     *
     * @param token    Session token
     * @param circleId ID of the Circle
     * @param feature     Name of the feature
     * @return The least milliseconds between two signals of a connection
     */
    public long signal(final byte[] token, final String circleId, final String feature) {
        verifyRequest(request(token), Permission.FETCH_DATA);
        final TrusteeEntity trustee = findTrustee(circleId);
        final CircleFeatureEntity circleFeature = writable(trustee, feature);
        final FeatureDeclaration declaration = declaration(circleFeature);
        throwConditionalException(!declaration.hasSignals(),
                ReturnCode.ILLEGAL_ACTION, featureNamed(feature) + " has no signals.");

        return declaration.signals();
    }

    // =========================================================================
    // The features of the stone
    // =========================================================================

    /**
     * The features which the stone offers. Every Member may read them, so a
     * client knows what it can show.
     *
     * @param token Session token
     * @return The features
     */
    public FeatureResponse features(final byte[] token) {
        verifyRequest(request(token), Permission.FETCH_CIRCLE);

        final FeatureResponse response = new FeatureResponse();
        for (final FeatureEntity entity : dao.findFeatures()) {
            response.addFeature(convert(entity));
        }

        return response;
    }

    /**
     * Adds a feature to those the stone offers, or replaces its declaration.
     * A replaced declaration counts for what is written afterwards.
     *
     * @param token       Session token of the System Administrator
     * @param name        Name of the feature
     * @param declaration The declaration
     * @return The feature
     */
    public FeatureResponse putFeature(final byte[] token, final String name, final String declaration) {
        verifyRequest(request(token), Permission.FETCH_CIRCLE);
        onlySystemAdministrator();

        final FeatureDeclaration parsed = FeatureDeclaration.parse(declaration, maxSize());
        throwConditionalException(!Objects.equals(parsed.name(), name),
                ReturnCode.VERIFICATION_WARNING, "The declaration is of the feature '" + parsed.name() + "', not of '" + name + "'.");

        FeatureEntity entity = dao.findFeature(name);
        final boolean added = entity == null;
        if (added) {
            entity = new FeatureEntity();
            entity.setName(name);
        }
        entity.setVersion(parsed.version());
        entity.setDeclaration(declaration);
        dao.save(entity);

        final FeatureResponse response = new FeatureResponse(featureNamed(name) + " was successfully " + (added ? "added." : "replaced."));
        response.addFeature(convert(entity));

        return response;
    }

    /**
     * Removes a feature from those the stone offers. A feature which a Circle
     * has, switched on or off, can't be removed.
     *
     * @param token Session token of the System Administrator
     * @param name  Name of the feature
     * @return The outcome
     */
    public FeatureResponse deleteFeature(final byte[] token, final String name) {
        verifyRequest(request(token), Permission.FETCH_CIRCLE);
        onlySystemAdministrator();

        final FeatureEntity entity = offered(name);
        final long circles = dao.countCirclesUsing(entity);
        throwConditionalException(circles > 0,
                ReturnCode.ILLEGAL_ACTION, featureNamed(name) + " can't be removed, as " + circles + " Circle(s) have it.");
        dao.delete(entity);

        return new FeatureResponse(featureNamed(name) + " was successfully removed.");
    }

    // =========================================================================
    // The features of a Circle
    // =========================================================================

    /**
     * The features of a Circle with their state, for every Trustee.
     *
     * @param token    Session token
     * @param circleId ID of the Circle
     * @return The features of the Circle
     */
    public CircleFeatureResponse circleFeatures(final byte[] token, final String circleId) {
        verifyRequest(request(token), Permission.FETCH_DATA);
        final TrusteeEntity trustee = findTrustee(circleId);

        final CircleFeatureResponse response = new CircleFeatureResponse();
        for (final CircleFeatureEntity entity : dao.findCircleFeatures(trustee.getCircle())) {
            response.addFeature(convert(entity));
        }

        return response;
    }

    /**
     * Switches a feature on in a Circle, off, or to read-only. Switching it
     * on for the first time makes its folder. Switched off, the entries
     * are kept and hidden.
     *
     * @param token    Session token of an administrator of the Circle
     * @param circleId ID of the Circle
     * @param name     Name of the feature
     * @param state    The state to give it
     * @return The feature of the Circle
     */
    public CircleFeatureResponse setCircleFeature(final byte[] token, final String circleId, final String name, final FeatureState state) {
        verifyRequest(request(token), Permission.FETCH_DATA);
        final TrusteeEntity trustee = administrator(circleId);
        throwConditionalNullException(state,
                ReturnCode.VERIFICATION_WARNING, "The state is missing: ON, READONLY or OFF.");
        final FeatureEntity feature = offered(name);
        final CircleEntity circle = trustee.getCircle();

        // Two requests don't both make the folder.
        dao.lockCircle(circle);
        CircleFeatureEntity entity = dao.findCircleFeature(circle, name);
        if (entity == null) {
            entity = new CircleFeatureEntity();
            entity.setCircle(circle);
            entity.setFeature(feature);
            entity.setFolderId(createFolder(circleId, name).getId());
            entity.setNextPosition(1L);
        }
        entity.setState(state);
        dao.save(entity);
        events.add(new FeatureEvent(FeatureEvent.Change.FEATURE, circleId, name, null));

        final CircleFeatureResponse response = new CircleFeatureResponse(featureNamed(name) + " is " + describe(state) + " in the Circle '" + circle.getName() + "'.");
        response.addFeature(convert(entity));

        return response;
    }

    /**
     * Removes a feature from a Circle, with all its entries. This is the
     * explicit step which switching off isn't.
     *
     * @param token    Session token of an administrator of the Circle
     * @param circleId ID of the Circle
     * @param name     Name of the feature
     * @return The outcome
     */
    public CircleFeatureResponse removeCircleFeature(final byte[] token, final String circleId, final String name) {
        verifyRequest(request(token), Permission.FETCH_DATA);
        final TrusteeEntity trustee = administrator(circleId);
        final CircleEntity circle = trustee.getCircle();

        dao.lockCircle(circle);
        final CircleFeatureEntity entity = dao.findCircleFeature(circle, name);
        throwConditionalNullException(entity,
                ReturnCode.IDENTIFICATION_WARNING, "The Circle doesn't have the feature '" + name + "'.");

        int removed = 0;
        for (final EntryEntity entry : dao.findAllEntries(entity)) {
            final MetadataEntity metadata = entry.getMetadata();
            dao.delete(entry);
            dao.deleteContent(metadata);
            dao.delete(metadata);
            removed++;
        }
        final MetadataEntity folder = dao.find(MetadataEntity.class, entity.getFolderId());
        dao.delete(entity);
        if (folder != null) {
            dao.delete(folder);
        }
        events.add(new FeatureEvent(FeatureEvent.Change.FEATURE, circleId, name, null));

        return new CircleFeatureResponse(featureNamed(name) + " was removed from the Circle '" + circle.getName() + "', with " + removed + " entries.");
    }

    // =========================================================================
    // Entries
    // =========================================================================

    /**
     * Adds an entry. For a kind which keeps one entry per Member, or one
     * per Member about each entry, a new entry replaces the Member's old
     * one, which keeps its ID and gets a new position.
     *
     * @param token    Session token
     * @param circleId ID of the Circle
     * @param feature     Name of the feature
     * @param body     Kind, content, and optionally name and the entry it is about
     * @return The entry
     */
    public EntryResponse addEntry(final byte[] token, final String circleId, final String feature, final EntryRequest body) {
        verifyRequest(request(token), Permission.FETCH_DATA);
        final TrusteeEntity trustee = findTrustee(circleId);
        final CircleFeatureEntity circleFeature = writable(trustee, feature);
        final FeatureDeclaration.Kind kind = kind(circleFeature, (body != null) ? body.getKind() : null);
        throwConditionalException(!has(trustee, kind.add()),
                ReturnCode.AUTHORIZATION_WARNING, "Adding an entry of the kind '" + kind.name() + "' needs the trust level " + kind.add() + " in the Circle.");
        final byte[] content = content(body.getContent(), kind, true);
        final String name = name(body.getName());

        // The position, and the one entry a Member may have, need the
        // entries of the Circle to stand still. The feature was read before
        // the lock, so its next position may be stale: another request may
        // have taken it meanwhile. It is read again under the lock.
        // Unwrapped before the lock, so the other writers of the Circle
        // don't wait for it (ticket 28); checked again under the lock.
        final Early early = unwrapEarly(trustee);
        dao.lockCircle(trustee.getCircle());
        dao.entityManager().refresh(circleFeature);
        final EntryEntity about = about(circleFeature, kind, body.getAbout());
        final SecretEDSKey circleSecret = circleSecret(trustee, early);

        if ((kind.keep() == FeatureDeclaration.Keep.MEMBER) || (kind.keep() == FeatureDeclaration.Keep.MEMBER_ABOUT)) {
            final EntryEntity own = dao.findOwnEntry(circleFeature, kind.name(), ownerTags(circleSecret, circleId), (kind.keep() == FeatureDeclaration.Keep.MEMBER_ABOUT) ? about.getId() : null);
            if (own != null) {
                refuseClosed(own);
                return replace(trustee, circleFeature, circleSecret, own, name, content);
            }
        }

        final MetadataEntity metadata = new MetadataEntity();
        metadata.setCircle(trustee.getCircle());
        metadata.setParentId(circleFeature.getFolderId());
        metadata.setType(dao.findDataTypeByName(Constants.ENTRY_TYPENAME));
        metadata.setMediaType(MEDIA_TYPE);
        metadata.setName(freeName(circleFeature, name, null));
        dao.save(metadata);
        encryptAndSave(dao, trustee, circleSecret, metadata, content);

        final EntryEntity entry = new EntryEntity();
        entry.setMetadata(metadata);
        entry.setCircleFeature(circleFeature);
        entry.setKind(kind.name());
        entry.setPosition(nextPosition(circleFeature));
        entry.setAboutId((about != null) ? about.getId() : null);
        entry.setOwnerTag(seals.ownerTag(circleSecret, circleId, member.getExternalId()));
        entry.setAuthor(seals.sealAuthor(circleSecret, circleId, member.getExternalId()));
        entry.setClosed(Boolean.FALSE);
        dao.save(entry);

        final Entry added = convert(entry, circleId, circleSecret, true);
        events.add(new FeatureEvent(FeatureEvent.Change.ADDED, circleId, feature, added));
        prune(circleFeature, kind, circleId, circleSecret);

        final EntryResponse response = new EntryResponse("The entry was successfully added.");
        response.addEntry(added);

        return response;
    }

    /**
     * Changes the content of an entry, its name, or both.
     *
     * @param token    Session token
     * @param circleId ID of the Circle
     * @param feature     Name of the feature
     * @param entryId  ID of the entry
     * @param body     The new content, the new name, or both
     * @return The entry
     */
    public EntryResponse changeEntry(final byte[] token, final String circleId, final String feature, final String entryId, final EntryRequest body) {
        verifyRequest(request(token), Permission.FETCH_DATA);
        final TrusteeEntity trustee = findTrustee(circleId);
        final CircleFeatureEntity circleFeature = writable(trustee, feature);
        throwConditionalException((body == null) || ((body.getContent() == null) && (body.getName() == null)),
                ReturnCode.VERIFICATION_WARNING, "Neither a new content nor a new name was given.");

        // Unwrapped before the lock, so the other writers of the Circle
        // don't wait for it (ticket 28); checked again under the lock.
        final Early early = unwrapEarly(trustee);
        dao.lockCircle(trustee.getCircle());
        final EntryEntity entry = entry(circleFeature, entryId);
        final FeatureDeclaration.Kind kind = kind(circleFeature, entry.getKind());
        final SecretEDSKey circleSecret = circleSecret(trustee, early);
        allowed(kind.change(), kind, trustee, entry, circleSecret, circleId, "change");
        refuseClosed(entry);

        final MetadataEntity metadata = entry.getMetadata();
        final String name = name(body.getName());
        if (name != null) {
            metadata.setName(freeName(circleFeature, name, metadata));
            dao.save(metadata);
        }
        if (body.getContent() != null) {
            final byte[] content = content(body.getContent(), kind, true);
            dao.deleteContent(metadata);
            encryptAndSave(dao, trustee, circleSecret, metadata, content);
        }
        entry.setChanged(Utilities.newDate());
        dao.save(entry);

        final Entry changed = convert(entry, circleId, circleSecret, true);
        events.add(new FeatureEvent(FeatureEvent.Change.CHANGED, circleId, feature, changed));
        final EntryResponse response = new EntryResponse("The entry was successfully changed.");
        response.addEntry(changed);

        return response;
    }

    /**
     * Removes an entry, and with it the entries which are about it.
     *
     * @param token    Session token
     * @param circleId ID of the Circle
     * @param feature     Name of the feature
     * @param entryId  ID of the entry
     * @return The entries which were removed, without their content
     */
    public EntryResponse removeEntry(final byte[] token, final String circleId, final String feature, final String entryId) {
        verifyRequest(request(token), Permission.FETCH_DATA);
        final TrusteeEntity trustee = findTrustee(circleId);
        final CircleFeatureEntity circleFeature = writable(trustee, feature);

        // Unwrapped before the lock, so the other writers of the Circle
        // don't wait for it (ticket 28); checked again under the lock.
        final Early early = unwrapEarly(trustee);
        dao.lockCircle(trustee.getCircle());
        final EntryEntity entry = entry(circleFeature, entryId);
        final FeatureDeclaration.Kind kind = kind(circleFeature, entry.getKind());
        final SecretEDSKey circleSecret = circleSecret(trustee, early);
        allowed(kind.remove(), kind, trustee, entry, circleSecret, circleId, "remove");
        refuseClosed(entry);

        final EntryResponse response = new EntryResponse("The entry was successfully removed.");
        final List<Entry> removed = new ArrayList<>();
        remove(entry, circleId, circleSecret, removed, 0);
        response.setEntries(removed);

        return response;
    }

    /**
     * Closes an entry, or opens it again. A closed entry can't be changed
     * or removed, nothing new can be about it, and what is about it can't
     * be changed or removed either.
     *
     * @param token    Session token
     * @param circleId ID of the Circle
     * @param feature     Name of the feature
     * @param entryId  ID of the entry
     * @param closed   True to close, false to open again
     * @return The entry
     */
    public EntryResponse closeEntry(final byte[] token, final String circleId, final String feature, final String entryId, final Boolean closed) {
        verifyRequest(request(token), Permission.FETCH_DATA);
        final TrusteeEntity trustee = findTrustee(circleId);
        final CircleFeatureEntity circleFeature = writable(trustee, feature);
        throwConditionalNullException(closed,
                ReturnCode.VERIFICATION_WARNING, "Whether to close the entry or to open it again is missing.");
        final boolean close = closed;

        // Unwrapped before the lock, so the other writers of the Circle
        // don't wait for it (ticket 28); checked again under the lock.
        final Early early = unwrapEarly(trustee);
        dao.lockCircle(trustee.getCircle());
        final EntryEntity entry = entry(circleFeature, entryId);
        final FeatureDeclaration.Kind kind = kind(circleFeature, entry.getKind());
        final SecretEDSKey circleSecret = circleSecret(trustee, early);
        allowed(kind.close(), kind, trustee, entry, circleSecret, circleId, close ? "close" : "open");
        // An entry which is about a closed one stays as it is.
        final EntryEntity above = dao.findEntryById(entry.getAboutId());
        if (above != null) {
            refuseClosed(above);
        }

        entry.setClosed(closed);
        dao.save(entry);

        final Entry changed = convert(entry, circleId, circleSecret, true);
        events.add(new FeatureEvent(FeatureEvent.Change.CHANGED, circleId, feature, changed));
        final EntryResponse response = new EntryResponse("The entry was successfully " + (close ? "closed." : "opened again."));
        response.addEntry(changed);

        return response;
    }

    /**
     * Reads one entry.
     *
     * @param token    Session token
     * @param circleId ID of the Circle
     * @param feature     Name of the feature
     * @param entryId  ID of the entry
     * @return The entry
     */
    public EntryResponse entry(final byte[] token, final String circleId, final String feature, final String entryId) {
        verifyRequest(request(token), Permission.FETCH_DATA);
        final TrusteeEntity trustee = findTrustee(circleId);
        final CircleFeatureEntity circleFeature = readable(trustee, feature);
        final EntryEntity entry = entry(circleFeature, entryId);

        final EntryResponse response = new EntryResponse();
        response.addEntry(convert(entry, circleId, circleSecret(trustee), true));

        return response;
    }

    /**
     * A page of the entries of a feature in a Circle, the newest first unless
     * asked otherwise.
     *
     * @param token       Session token
     * @param circleId    ID of the Circle
     * @param feature        Name of the feature
     * @param query       Which entries, in which order, how many, and whether with content
     * @return The entries
     */
    public EntryResponse entries(final byte[] token, final String circleId, final String feature, final EntryQuery query) {
        verifyRequest(request(token), Permission.FETCH_DATA);
        final TrusteeEntity trustee = findTrustee(circleId);
        final CircleFeatureEntity circleFeature = readable(trustee, feature);
        final String kind = query.kind();
        final int size = (query.limit() != null) ? query.limit() : DEFAULT_PAGE;
        throwConditionalException((size < 1) || (size > Constants.MAX_ENTRY_PAGE),
                ReturnCode.VERIFICATION_WARNING, "The limit must be from 1 to " + Constants.MAX_ENTRY_PAGE + '.');
        final Long aboutId = (query.about() != null) ? entry(circleFeature, query.about()).getId() : null;
        final SecretEDSKey circleSecret = circleSecret(trustee);

        // Entries which are too old are gone before anyone reads them.
        final FeatureDeclaration declaration = declaration(circleFeature);
        for (final FeatureDeclaration.Kind declared : declaration.kinds().values()) {
            if ((declared.keep() == FeatureDeclaration.Keep.AGE) && ((kind == null) || kind.equals(declared.name())) && (circleFeature.getState() == FeatureState.ON)) {
                prune(circleFeature, declared, circleId, circleSecret);
            }
        }

        // The page is read without a flush before each query, and with one
        // query for what all its entries are about, one for their authors and
        // one for their content (ticket 28).
        final boolean withContent = query.content();
        final EntryResponse response = new EntryResponse();
        dao.reading(() -> convert(dao.findEntries(circleFeature, query, aboutId, size), circleId, circleSecret, withContent, true))
                .forEach(response::addEntry);

        return response;
    }

    /**
     * Counts the entries of a feature in a Circle, which is how a client finds
     * what a Member hasn't seen yet.
     *
     * @param token    Session token
     * @param circleId ID of the Circle
     * @param feature     Name of the feature
     * @param kind     Only this kind, or null
     * @param after    Only entries after this position, or null
     * @return The number of entries
     */
    public EntryResponse count(final byte[] token, final String circleId, final String feature, final String kind, final Long after) {
        verifyRequest(request(token), Permission.FETCH_DATA);
        final TrusteeEntity trustee = findTrustee(circleId);
        final CircleFeatureEntity circleFeature = readable(trustee, feature);

        final EntryResponse response = new EntryResponse();
        response.setCount(dao.countEntries(circleFeature, kind, after));

        return response;
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private void onlySystemAdministrator() {
        if (member.getMemberRole() != MemberRole.ADMIN) {
            throw new AuthorizationException("Only the System Administrator decides which features the stone offers.");
        }
    }

    private TrusteeEntity administrator(final String circleId) {
        final TrusteeEntity trustee = findTrustee(circleId);
        if (!has(trustee, TrustLevel.ADMIN)) {
            throw new AuthorizationException("Only an administrator of the Circle decides about its features.");
        }

        return trustee;
    }

    private static boolean has(final TrusteeEntity trustee, final TrustLevel level) {
        return TrustLevel.getLevels(level).contains(trustee.getTrustLevel());
    }

    private FeatureEntity offered(final String name) {
        final FeatureEntity feature = FeatureDeclaration.isName(name) ? dao.findFeature(name) : null;
        throwConditionalNullException(feature,
                ReturnCode.IDENTIFICATION_WARNING, "The stone doesn't offer a feature '" + name + "'.");

        return feature;
    }

    /** The feature of the Circle, if it isn't switched off. */
    private CircleFeatureEntity readable(final TrusteeEntity trustee, final String feature) {
        final CircleFeatureEntity circleFeature = FeatureDeclaration.isName(feature) ? dao.findCircleFeature(trustee.getCircle(), feature) : null;
        // A feature which is switched off is as if it weren't there.
        throwConditionalException((circleFeature == null) || (circleFeature.getState() == FeatureState.OFF),
                ReturnCode.IDENTIFICATION_WARNING, featureNamed(feature) + " isn't switched on in the Circle.");

        return circleFeature;
    }

    /** The feature of the Circle, if it is switched on. */
    private CircleFeatureEntity writable(final TrusteeEntity trustee, final String feature) {
        final CircleFeatureEntity circleFeature = readable(trustee, feature);
        throwConditionalException(circleFeature.getState() != FeatureState.ON,
                ReturnCode.ILLEGAL_ACTION, featureNamed(feature) + " is read-only in the Circle.");

        return circleFeature;
    }

    private static FeatureDeclaration declaration(final CircleFeatureEntity circleFeature) {
        return FeatureDeclaration.parse(circleFeature.getFeature().getDeclaration(), Integer.MAX_VALUE);
    }

    private static FeatureDeclaration.Kind kind(final CircleFeatureEntity circleFeature, final String name) {
        final FeatureDeclaration.Kind kind = declaration(circleFeature).kind(name);
        throwConditionalNullException(kind,
                ReturnCode.VERIFICATION_WARNING, featureNamed(circleFeature.getFeature().getName()) + " declares no kind '" + name + "'.");

        return kind;
    }

    private int maxSize() {
        return (int) Math.min(Integer.MAX_VALUE, settings.getDataMaxInlineSize());
    }

    private EntryEntity entry(final CircleFeatureEntity circleFeature, final String entryId) {
        final EntryEntity entry = (entryId != null) ? dao.findEntry(circleFeature, entryId) : null;
        throwConditionalNullException(entry,
                ReturnCode.IDENTIFICATION_WARNING, "The entry '" + entryId + "' is not an entry of the feature in the Circle.");

        return entry;
    }

    /** The entry which a new one is about, as the kind asks for it. */
    private EntryEntity about(final CircleFeatureEntity circleFeature, final FeatureDeclaration.Kind kind, final String aboutId) {
        if (kind.about() == null) {
            throwConditionalException(aboutId != null,
                    ReturnCode.VERIFICATION_WARNING, entryOfKind(kind) + " isn't about another entry.");
            return null;
        }
        throwConditionalNullException(aboutId,
                ReturnCode.VERIFICATION_WARNING, entryOfKind(kind) + " is about an entry of the kind '" + kind.about() + "', which is missing.");
        final EntryEntity about = entry(circleFeature, aboutId);
        throwConditionalException(!Objects.equals(kind.about(), about.getKind()),
                ReturnCode.VERIFICATION_WARNING, entryOfKind(kind) + " is about an entry of the kind '" + kind.about() + "', not '" + about.getKind() + "'.");
        refuseClosed(about);

        return about;
    }

    /** Refuses if the entry is closed, or what it is about, and so on upwards. */
    private void refuseClosed(final EntryEntity entry) {
        EntryEntity current = entry;
        for (int depth = 0; (current != null) && (depth < MAX_DEPTH); depth++) {
            // The first is the entry itself.
            throwConditionalException(Boolean.TRUE.equals(current.getClosed()),
                    ReturnCode.ILLEGAL_ACTION, (depth == 0) ? "The entry is closed." : "The entry is about a closed one.");
            current = dao.findEntryById(current.getAboutId());
        }
    }

    /** Refuses unless the requesting Member is one of those the rule names. */
    private void allowed(final Set<FeatureDeclaration.Who> who, final FeatureDeclaration.Kind kind, final TrusteeEntity trustee, final EntryEntity entry,
                         final SecretEDSKey circleSecret, final String circleId, final String what) {
        final boolean author = who.contains(FeatureDeclaration.Who.AUTHOR)
                && ownerTags(circleSecret, circleId).contains(entry.getOwnerTag());
        final boolean adder = who.contains(FeatureDeclaration.Who.ADDERS) && has(trustee, kind.add());
        final boolean administrator = who.contains(FeatureDeclaration.Who.ADMINISTRATORS) && has(trustee, TrustLevel.ADMIN);

        if (!author && !adder && !administrator) {
            if (who.isEmpty()) {
                throw new IllegalActionException("Nobody may " + what + " an entry of the kind '" + kind.name() + "'.");
            }
            throw new AuthorizationException("The requesting Account may not " + what + " this entry.");
        }
    }

    private static byte[] content(final String encoded, final FeatureDeclaration.Kind kind, final boolean required) {
        throwConditionalException(required && (encoded == null),
                ReturnCode.VERIFICATION_WARNING, "The content is missing.");
        final byte[] content;
        try {
            content = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException e) {
            final VerificationException exception = new VerificationException("The content must be Base64-encoded.");
            exception.initCause(e);
            throw exception;
        }
        throwConditionalException(content.length > kind.size(),
                ReturnCode.CONTENT_TOO_LARGE, "The content of an entry of the kind '" + kind.name() + "' may have at most " + kind.size() + " bytes.");

        return content;
    }

    private static String name(final String name) {
        if (name == null) {
            return null;
        }
        final String trimmed = name.trim();
        throwConditionalException(trimmed.isEmpty() || (trimmed.length() > Constants.MAX_NAME_LENGTH),
                ReturnCode.VERIFICATION_WARNING, "The name of an entry has 1 to " + Constants.MAX_NAME_LENGTH + " characters.");

        return trimmed;
    }

    /** The name for an entry: the given one if it is free, or one of the stone's making. */
    private String freeName(final CircleFeatureEntity circleFeature, final String name, final MetadataEntity self) {
        if (name == null) {
            final byte[] random = new byte[8];
            RANDOM.nextBytes(random);
            return Utilities.newDate().toString() + '-' + HexFormat.of().formatHex(random);
        }
        throwConditionalException(dao.checkIfNameIsUsed((self != null) ? self.getId() : -1L, name, circleFeature.getFolderId()),
                ReturnCode.IDENTIFICATION_WARNING, "The feature has an entry with the name '" + name + "' in the Circle already.");

        return name;
    }

    private long nextPosition(final CircleFeatureEntity circleFeature) {
        final long position = circleFeature.getNextPosition();
        circleFeature.setNextPosition(position + 1);
        dao.save(circleFeature);

        return position;
    }

    /** A Member's new entry takes the place of their old one of the kind. */
    private EntryResponse replace(final TrusteeEntity trustee, final CircleFeatureEntity circleFeature, final SecretEDSKey circleSecret,
                                  final EntryEntity own, final String name, final byte[] content) {
        final String circleId = trustee.getCircle().getExternalId();
        final MetadataEntity metadata = own.getMetadata();
        if (name != null) {
            metadata.setName(freeName(circleFeature, name, metadata));
            dao.save(metadata);
        }
        dao.deleteContent(metadata);
        encryptAndSave(dao, trustee, circleSecret, metadata, content);
        // The new position is taken before the old one is given up, as two
        // entries can't share one.
        own.setPosition(nextPosition(circleFeature));
        own.setChanged(Utilities.newDate());
        dao.save(own);

        final Entry replaced = convert(own, circleId, circleSecret, true);
        events.add(new FeatureEvent(FeatureEvent.Change.CHANGED, circleId, circleFeature.getFeature().getName(), replaced));
        final EntryResponse response = new EntryResponse("The entry was successfully replaced.");
        response.addEntry(replaced);

        return response;
    }

    /** Removes an entry with what is about it, the deepest first. */
    private void remove(final EntryEntity entry, final String circleId, final SecretEDSKey circleSecret, final List<Entry> removed, final int depth) {
        if (depth < MAX_DEPTH) {
            for (final EntryEntity child : dao.findEntriesAbout(entry.getId())) {
                remove(child, circleId, circleSecret, removed, depth + 1);
            }
        }
        final Entry gone = convert(entry, circleId, circleSecret, false);
        final MetadataEntity metadata = entry.getMetadata();
        dao.delete(entry);
        dao.deleteContent(metadata);
        dao.delete(metadata);
        removed.add(gone);
        events.add(new FeatureEvent(FeatureEvent.Change.REMOVED, circleId, gone.getFeature(), gone));
    }

    /** Removes what a kind no longer keeps: entries past their age, or beyond their number. */
    private void prune(final CircleFeatureEntity circleFeature, final FeatureDeclaration.Kind kind, final String circleId, final SecretEDSKey circleSecret) {
        final List<EntryEntity> gone;
        if (kind.keep() == FeatureDeclaration.Keep.AGE) {
            gone = dao.findEntriesOlderThan(circleFeature, kind.name(), Utilities.newDate().minusSeconds(kind.keepValue()));
        } else if (kind.keep() == FeatureDeclaration.Keep.COUNT) {
            gone = dao.findEntriesBeyond(circleFeature, kind.name(), (int) Math.min(Integer.MAX_VALUE, kind.keepValue()));
        } else {
            gone = List.of();
        }
        for (final EntryEntity entry : gone) {
            remove(entry, circleId, circleSecret, new ArrayList<>(), 0);
        }
    }

    private MetadataEntity createFolder(final String circleId, final String feature) {
        final MetadataEntity root = dao.findRootByMemberCircle(member.getId(), circleId);
        throwConditionalNullException(root,
                ReturnCode.INTEGRITY_ERROR, "No root folder could be found for the Circle '" + circleId + "', please contact the administrators.");
        final String name = FeatureDeclaration.folder(feature);
        // A folder of that name from before the stone offered the feature isn't
        // taken over: what is in it isn't the feature's.
        throwConditionalException(dao.checkIfNameIsUsed(-1L, name, root.getId()),
                ReturnCode.ILLEGAL_ACTION, "The Circle has something named '" + name + "' already, which the feature would need as its folder.");

        final MetadataEntity folder = new MetadataEntity();
        folder.setCircle(root.getCircle());
        folder.setParentId(root.getId());
        folder.setType(dao.findDataTypeByName(Constants.FOLDER_TYPENAME));
        folder.setName(name);
        dao.save(folder);

        return folder;
    }

    // -------------------------------------------------------------------------
    // Conversions
    // -------------------------------------------------------------------------

    private static Feature convert(final FeatureEntity entity) {
        final Feature feature = new Feature();
        feature.setName(entity.getName());
        feature.setVersion(entity.getVersion());
        feature.setDeclaration(entity.getDeclaration());

        return feature;
    }

    private static CircleFeature convert(final CircleFeatureEntity entity) {
        final CircleFeature feature = new CircleFeature();
        feature.setName(entity.getFeature().getName());
        feature.setVersion(entity.getFeature().getVersion());
        feature.setState(entity.getState());

        return feature;
    }

    /**
     * The Circle Secret, from the Trustee row as it is now: a rotation of the
     * secret (PLAN.md, Crypto C6) may have started since the row was read,
     * and authors and owner tags are sealed with the current secret. For a
     * write, the Circle is locked before, so it is the secret the entry is
     * stored with.
     *
     * @param trustee The requesting Member's Trustee row
     * @return The current Circle Secret
     */
    private SecretEDSKey circleSecret(final TrusteeEntity trustee) {
        dao.refresh(trustee);
        reader = trustee;
        retiring = null;
        return unwrapCircleKey(trustee);
    }

    /** The Circle Secret unwrapped before the Circle is locked, and the key row it is of. */
    private record Early(long keyId, SecretEDSKey secret) {
    }

    /**
     * Unwraps the Circle Secret before the Circle is locked (ticket 28): the
     * unwrap (ECDH and ML-KEM) takes the longest of a write, and every other
     * writer of the Circle would wait for it under the lock.
     *
     * @param trustee The requesting Member's Trustee row, as read before the lock
     * @return The secret, with the key row it is of
     */
    private Early unwrapEarly(final TrusteeEntity trustee) {
        return new Early(trustee.getKey().getId(), unwrapCircleKey(trustee));
    }

    /**
     * The current Circle Secret under the lock. The Trustee row is read
     * again, as a rotation may have installed a new key before the lock was
     * had; the secret unwrapped before the lock is used while it is still
     * the current one, else the new one is unwrapped.
     *
     * @param trustee The requesting Member's Trustee row
     * @param early   The secret unwrapped before the lock
     * @return The current Circle Secret
     */
    private SecretEDSKey circleSecret(final TrusteeEntity trustee, final Early early) {
        dao.refresh(trustee);
        reader = trustee;
        retiring = null;
        return (trustee.getKey().getId() == early.keyId()) ? early.secret() : unwrapCircleKey(trustee);
    }

    /** The retiring Circle Secret while it is rotated, else null. */
    private SecretEDSKey retiringSecret() {
        if ((retiring == null) && (reader != null) && (reader.getPreviousKey() != null)) {
            retiring = unwrapCircleKey(reader, reader.getPreviousKey().getId());
        }
        return retiring;
    }

    /** The secret an entry's content is stored with: the current one, or the retiring one while it is rotated. */
    private SecretEDSKey contentSecret(final SecretEDSKey circleSecret, final DataEntity data) {
        final long keyId = data.getKey().getId();
        if ((reader == null) || Objects.equals(reader.getKey().getId(), keyId)) {
            return circleSecret;
        }
        return ((reader.getPreviousKey() != null) && Objects.equals(reader.getPreviousKey().getId(), keyId))
                ? retiringSecret()
                : unwrapCircleKey(reader, keyId);
    }

    /**
     * The owner tags of the requesting Member: while the Circle Secret is
     * rotated, an entry may still have the one of the retiring secret, as the
     * entries are sealed anew one by one.
     */
    private List<String> ownerTags(final SecretEDSKey circleSecret, final String circleId) {
        final String current = seals.ownerTag(circleSecret, circleId, member.getExternalId());
        final SecretEDSKey previous = retiringSecret();
        return (previous != null) ? List.of(current, seals.ownerTag(previous, circleId, member.getExternalId())) : List.of(current);
    }

    private Entry convert(final EntryEntity entity, final String circleId, final SecretEDSKey circleSecret, final boolean withContent) {
        return convert(List.of(entity), circleId, circleSecret, withContent, false).getFirst();
    }

    /**
     * The entries as the API gives them. What they are about, their authors'
     * names and their content are each read with one query for all of them,
     * not one for each entry (ticket 28).
     *
     * @param entities      The entries
     * @param circleId      ID of the Circle
     * @param circleSecret  The current Circle Secret
     * @param withContent   Whether the content is given
     * @param onlyRead      Whether the content is only read here: it leaves
     *                      the persistence context afterwards, so it isn't
     *                      checked for changes at the commit
     * @return The entries, in the same order
     */
    private List<Entry> convert(final List<EntryEntity> entities, final String circleId, final SecretEDSKey circleSecret,
                                final boolean withContent, final boolean onlyRead) {
        final Map<Long, String> abouts = dao.findEntryExternalIds(entities.stream()
                .map(EntryEntity::getAboutId).filter(Objects::nonNull).collect(Collectors.toSet()));
        final List<String> authors = new ArrayList<>(entities.size());
        for (final EntryEntity entity : entities) {
            authors.add(seals.openAuthor(circleSecret, this::retiringSecret, circleId, entity));
        }
        final Map<String, String> names = dao.findMemberNames(authors.stream().filter(Objects::nonNull).collect(Collectors.toSet()));
        final Map<Long, DataEntity> contents = withContent
                ? dao.findDataByMetadata(entities.stream().map(EntryEntity::getMetadata).toList(), onlyRead)
                : Map.of();

        final List<Entry> entries = new ArrayList<>(entities.size());
        for (int i = 0; i < entities.size(); i++) {
            final EntryEntity entity = entities.get(i);
            final MetadataEntity metadata = entity.getMetadata();
            final Entry entry = new Entry();
            entry.setEntryId(metadata.getExternalId());
            entry.setCircleId(circleId);
            entry.setFeature(entity.getCircleFeature().getFeature().getName());
            entry.setKind(entity.getKind());
            entry.setPosition(entity.getPosition());
            entry.setAbout((entity.getAboutId() != null) ? abouts.get(entity.getAboutId()) : null);
            entry.setName(metadata.getName());
            entry.setAdded(metadata.getAdded());
            entry.setChanged(entity.getChanged());
            entry.setClosed(Boolean.TRUE.equals(entity.getClosed()));
            final String memberId = authors.get(i);
            entry.setMemberId(memberId);
            entry.setAccountName((memberId != null) ? names.get(memberId) : null);
            final DataEntity data = contents.get(metadata.getId());
            if (data != null) {
                entry.setContent(content(circleSecret, data, metadata));
            }
            entries.add(entry);
        }
        if (onlyRead) {
            dao.detach(contents.values());
        }

        return entries;
    }

    /** The decrypted content, Base64; null if it can't be read. */
    private String content(final SecretEDSKey circleSecret, final DataEntity data, final MetadataEntity metadata) {
        try {
            final byte[] content = DataFormat.decrypt(contentSecret(circleSecret, data), dataContext(data), data.getData());
            final String encoded = Base64.getEncoder().encodeToString(content);
            Arrays.fill(content, (byte) 0);
            return encoded;
        } catch (RuntimeException e) {
            // A damaged entry mustn't hide the others; the sanity check
            // reports the damage.
            LOGGER.warn("The entry {} can't be read: {}", metadata.getExternalId(), e.getMessage());
            return null;
        }
    }

    private static String describe(final FeatureState state) {
        return switch (state) {
            case ON -> "switched on";
            case READONLY -> "read-only";
            case OFF -> "switched off";
        };
    }

    private static Authentication request(final byte[] token) {
        throwConditionalException((token == null) || (token.length == 0),
                ReturnCode.AUTHENTICATION_WARNING, "The Session token is missing.");
        final Authentication request = new Authentication();
        request.setCredentialType(CredentialType.SESSION);
        request.setCredential(token);

        return request;
    }

    private static String featureNamed(final String name) {
        return "The feature '" + name + '\'';
    }

    private static String entryOfKind(final FeatureDeclaration.Kind kind) {
        return "An entry of the kind '" + kind.name() + '\'';
    }
}
