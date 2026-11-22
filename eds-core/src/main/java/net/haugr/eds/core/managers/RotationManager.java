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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.core.enums.SanityStatus;
import net.haugr.eds.core.enums.Status;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.jce.DataFormat;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.ConnectionSource;
import net.haugr.eds.core.model.EncryptedDataStore;
import net.haugr.eds.core.model.RotationDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.model.entities.EntryEntity;
import net.haugr.eds.core.model.entities.KeyEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;
import net.haugr.eds.core.rotation.CircleRotations;
import net.haugr.eds.core.stream.ContentCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>The steps of the rotation of a Circle Secret (PLAN.md, Crypto C6), each
 * run in a transaction of its own by the RotationBean:</p>
 *
 * <ol>
 *   <li>{@link #begin}: a new Circle Secret, wrapped for every Trustee to
 *   their public keys, beside the retiring one; the external Circle key and
 *   the Trustees' secrets sealed anew; pending invitations into the Circle
 *   deleted. It is short, as it holds the lock of the Circle. Or, for a
 *   rotation under way, the check that the offered secrets are still the
 *   Circle's.</li>
 *   <li>{@link #reencrypt}: one Data Object re-encrypted from the retiring
 *   secret to the new one, chunk by chunk, in its own content row.</li>
 *   <li>{@link #resealEntries}: the authors and owner tags of a batch of
 *   entries of features sealed anew, under a short lock of the Circle.</li>
 *   <li>{@link #end}: when no object is left on the retiring secret, it goes
 *   with its wrapped copies, and the next rotation is set due.</li>
 * </ol>
 *
 * <p>Nothing of a rotation is kept in memory but the unwrapped secrets; the
 * rest is in the database, so a rotation which was interrupted continues
 * when a Trustee's request hands the secrets over again.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class RotationManager extends AbstractManager<RotationDao, EDSResponse, Authentication> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotationManager.class);

    private final ConnectionSource readers;
    private final EntrySeals seals;

    /**
     * The rotation under way.
     *
     * @param circleId       Internal ID of the Circle
     * @param externalId     External ID of the Circle
     * @param keyId          Key row of the new secret
     * @param current        The new Circle Secret
     * @param previousKeyId  Key row of the retiring secret
     * @param previous       The retiring Circle Secret
     * @param started        When the rotation started
     */
    public record Rotation(long circleId, String externalId, long keyId, SecretEDSKey current, long previousKeyId, SecretEDSKey previous, LocalDateTime started) {
    }

    /** What the end step found. */
    public enum Outcome {
        /** Objects are left on the retiring secret. */
        MORE,
        /** The rotation is complete. */
        DONE,
        /** It is complete, and another rotation is due already, as a Trustee left meanwhile. */
        AGAIN
    }

    public RotationManager(final Settings settings, final EntityManager entityManager, final ConnectionSource readers) {
        super(settings, new RotationDao(entityManager));
        this.readers = readers;
        this.seals = new EntrySeals(crypto);
    }

    /**
     * Not used; the rotation has its own steps.
     */
    @Override
    public EDSResponse perform(final Authentication request) {
        throw new EDSException(ReturnCode.ILLEGAL_SERVICE, "The rotation has its own steps.");
    }

    /**
     * Starts the rotation, or takes up one under way.
     *
     * @param handoff The secrets a Trustee's request unwrapped
     * @return The rotation, or null if there is nothing to do with them
     */
    public Rotation begin(final CircleRotations.Handoff handoff) {
        final CircleEntity circle = dao.find(CircleEntity.class, handoff.circleId());
        if (circle == null) {
            return null;
        }
        dao.lockCircle(circle);
        dao.refresh(circle);
        final KeyEntity active = dao.findKey(circle, Status.ACTIVE);
        if ((active == null) || !Objects.equals(active.getId(), handoff.keyId())) {
            // The secret was rotated since the request read it.
            return null;
        }
        final KeyEntity retiring = dao.findKey(circle, Status.RETIRING);

        final Rotation rotation;
        if (retiring != null) {
            rotation = Objects.equals(retiring.getId(), handoff.previousKeyId())
                    ? new Rotation(circle.getId(), circle.getExternalId(), active.getId(), handoff.current(), retiring.getId(), handoff.previous(), active.getAdded())
                    : null;
        } else if (circle.isRotationDue(Utilities.newDate())) {
            rotation = start(circle, active, handoff.current());
        } else {
            rotation = null;
        }

        return rotation;
    }

    private Rotation start(final CircleEntity circle, final KeyEntity active, final SecretEDSKey previous) {
        final SecretEDSKey secret = Crypto.generateSymmetricKey(settings.getSymmetricAlgorithm());
        final KeyEntity next = new KeyEntity();
        next.setCircle(circle);
        next.setAlgorithm(settings.getSymmetricAlgorithm());
        next.setStatus(Status.ACTIVE);
        dao.save(next);
        active.setStatus(Status.RETIRING);
        dao.save(active);

        for (final TrusteeEntity trustee : dao.findTrustees(circle)) {
            final String own = (trustee.getExternalKey() != null) ? openTrusteeSecret(previous, trustee) : null;
            trustee.setPreviousKey(active);
            trustee.setPreviousCircleKey(trustee.getCircleKey());
            trustee.setKey(next);
            trustee.setCircleKey(wrapCircleKey(trustee.getMember(), circle, next, secret));
            trustee.setExternalKey(sealTrusteeSecret(secret, circle, trustee.getMember(), own));
            dao.save(trustee);
        }
        circle.setCircleKey(encryptExternalKey(secret, circle, openExternalKey(previous, circle)));
        dao.save(circle);
        final int invitations = dao.deleteInvitations(circle);
        LOGGER.info("The Circle Secret of the Circle {} is rotated; {} pending invitations into it were deleted.", circle.getExternalId(), invitations);

        final LocalDateTime started = (next.getAdded() != null) ? next.getAdded() : Utilities.newDate();
        return new Rotation(circle.getId(), circle.getExternalId(), next.getId(), secret, active.getId(), previous, started);
    }

    /**
     * The next Data Objects still on the retiring secret.
     *
     * @param rotation The rotation
     * @param limit    How many at most
     * @return Internal IDs of their content rows
     */
    public List<Long> next(final Rotation rotation, final int limit) {
        return dao.findDataOnKey(dao.getReference(KeyEntity.class, rotation.previousKeyId()), limit);
    }

    /**
     * Re-encrypts one Data Object with the new secret, in its own content row:
     * the parts are replaced, and the row is switched to the new secret, in
     * the same transaction. Only the content row is locked, as everything
     * else which changes or deletes the object comes to it last; so the job
     * and the requests never wait for each other in a circle. A large
     * object's old parts are read on a connection of their own, which sees
     * them as committed.
     *
     * @param rotation The rotation
     * @param dataId   Internal ID of the content row
     * @return True if it was re-encrypted, false if it was gone or done
     */
    public boolean reencrypt(final Rotation rotation, final long dataId) {
        final Long keyId = dao.lockData(dataId);
        if ((keyId == null) || (keyId != rotation.previousKeyId())) {
            return false;
        }

        final String objectId = dao.findObjectId(dataId);
        final long length = dao.withConnection(connection -> EncryptedDataStore.length(connection, dataId));
        final ContentCopier.Source from = new ContentCopier.Source(dataId, length, rotation.previous(),
                new DataFormat.Context(rotation.externalId(), objectId, rotation.previousKeyId()));
        final ContentCopier.Target to = new ContentCopier.Target(dataId, rotation.current(),
                new DataFormat.Context(rotation.externalId(), objectId, rotation.keyId()), true);
        final String algorithm = settings.getHashAlgorithm().getAlgorithm();
        dao.withConnection(connection ->
                ContentCopier.copy(connection, readers, from, to, settings.getStreamReadBatchSize(), algorithm, settings.getDataMaxInlineSize()));
        dao.updateContentKey(dataId, dao.getReference(KeyEntity.class, rotation.keyId()));

        return true;
    }

    /**
     * Seals the authors and owner tags of a batch of entries of features
     * anew with the new secret, under the lock of the Circle, as a change of
     * an entry by its author writes the same rows. Entries sealed with the
     * new secret already are left as they are.
     *
     * @param rotation The rotation
     * @param afterId  The last entry of the batch before, 0 for the first
     * @param limit    How many at most
     * @return The last entry of this batch, or -1 if there were none
     */
    public long resealEntries(final Rotation rotation, final long afterId, final int limit) {
        final CircleEntity circle = dao.find(CircleEntity.class, rotation.circleId());
        if (circle == null) {
            return -1;
        }
        dao.lockCircle(circle);
        long last = -1;
        for (final EntryEntity entry : dao.findEntries(circle, afterId, limit)) {
            dao.refresh(entry);
            if (seals.reseal(rotation.previous(), rotation.current(), rotation.externalId(), entry)) {
                dao.save(entry);
            }
            last = entry.getId();
        }
        return last;
    }

    /**
     * Marks a Data Object which couldn't be re-encrypted, as its content
     * failed its check; the sanity check reports it.
     *
     * @param dataId Internal ID of the content row
     */
    public void markFailed(final long dataId) {
        dao.updateSanityStatus(dataId, SanityStatus.FAILED);
    }

    /**
     * Ends the rotation, if no object is left on the retiring secret: the
     * retiring secret goes, with its wrapped copies, and the next rotation is
     * due a period from now. A Trustee who joined with the retiring secret
     * only, while the rotation started, gets the new one.
     *
     * @param rotation The rotation
     * @return What was found
     */
    public Outcome end(final Rotation rotation) {
        final CircleEntity circle = dao.find(CircleEntity.class, rotation.circleId());
        final KeyEntity retiring = dao.find(KeyEntity.class, rotation.previousKeyId());
        if ((circle == null) || (retiring == null)) {
            return Outcome.DONE;
        }
        dao.lockCircle(circle);
        dao.refresh(circle);
        if (dao.countDataOnKey(retiring) > 0) {
            return Outcome.MORE;
        }

        final KeyEntity next = dao.find(KeyEntity.class, rotation.keyId());
        for (final TrusteeEntity trustee : dao.findTrustees(circle)) {
            if (Objects.equals(trustee.getKey().getId(), retiring.getId())) {
                final String own = (trustee.getExternalKey() != null) ? openTrusteeSecret(rotation.previous(), trustee) : null;
                trustee.setKey(next);
                trustee.setCircleKey(wrapCircleKey(trustee.getMember(), circle, next, rotation.current()));
                trustee.setExternalKey(sealTrusteeSecret(rotation.current(), circle, trustee.getMember(), own));
            }
            trustee.setPreviousKey(null);
            trustee.setPreviousCircleKey(null);
            dao.save(trustee);
        }
        // The content rows were switched to the new secret by statements, so
        // entities still in this context may name the retiring one; they are
        // let go before it is deleted.
        dao.entityManager().flush();
        dao.entityManager().clear();
        dao.deleteKey(rotation.previousKeyId());

        // A Trustee who left since the rotation started set it due again.
        final CircleEntity rotated = dao.find(CircleEntity.class, rotation.circleId());
        final LocalDateTime due = rotated.getRotateCircleKeyDue();
        // An extra rotation is better than one too few, so the same moment counts.
        final boolean again = (due != null) && !due.isBefore(rotation.started());
        if (!again) {
            rotated.setRotateCircleKeyDue(settings.nextCircleSecretRotation());
        }
        dao.save(rotated);
        LOGGER.info("The Circle Secret of the Circle {} is rotated, every Data Object is on the new secret.", rotation.externalId());

        return again ? Outcome.AGAIN : Outcome.DONE;
    }
}
