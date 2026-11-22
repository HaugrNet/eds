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

import jakarta.persistence.Query;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.dtos.Sanity;
import net.haugr.eds.api.requests.SanityRequest;
import net.haugr.eds.api.responses.SanityResponse;
import net.haugr.eds.core.enums.HashAlgorithm;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.enums.SanityStatus;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.model.EncryptedDataStore;
import net.haugr.eds.core.model.SanityDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.DataEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Business Logic implementation for the EDS Sanity request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class SanityManager extends AbstractManager<SanityDao, SanityResponse, SanityRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SanityManager.class);

    public SanityManager(final Settings settings, final EntityManager entityManager) {
        super(settings, new SanityDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SanityResponse perform(final SanityRequest request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.SANITY);
        Arrays.fill(request.getCredential(), (byte) 0);

        final List<DataEntity> found = findRecords(request);
        final List<Sanity> sanities = convertRecords(found);
        final SanityResponse response = new SanityResponse();
        response.setSanities(sanities);

        return response;
    }

    /**
     * The check is switched off with an interval of 0 days.
     *
     * @return True if the Data Objects are checked
     */
    public boolean isEnabled() {
        return settings.getSanityInterval() > 0;
    }

    /**
     * Whether a run has checked as many Data Objects as allowed
     * ({@code eds.sanity.check.max.objects}, 0 for no limit).
     *
     * @param checked Checked so far
     * @return True if the run must stop
     */
    public boolean reachedLimit(final long checked) {
        final int max = settings.getSanityMaxObjects();
        return (max > 0) && (checked >= max);
    }

    /**
     * Removes expired Sessions and Invitations.
     */
    public void clearExpired() {
        clearExpireSessions();
        clearExpiredInvitations();
    }

    /**
     * Counts the Data Objects marked as failed.
     *
     * @return Number of failed Data Objects
     */
    public long countFailed() {
        return (Long) dao.createNamedQuery("data.countWithStatus")
                .setParameter("status", SanityStatus.FAILED)
                .getSingleResult();
    }

    public List<Long> findNextBatch(final int maxResults) {
        final int days = settings.getSanityInterval();
        final LocalDateTime date = Utilities.newDate().minusDays(days);

        final Query query = dao.createNamedQuery("data.findIdsForSanityCheck")
                .setParameter("status", SanityStatus.OK)
                .setParameter("date", date)
                .setMaxResults(maxResults);

        return CommonDao.findList(query);
    }

    /**
     * <p>Checks one Data Object: the digest over the stored ciphertext is
     * computed while reading it in batches (never the whole value), and
     * compared with the stored checksum. No key is needed. Not a security
     * control, the GCM tags are (PLAN.md, Crypto §4); it finds silent corruption.</p>
     *
     * <p>The digest is made with the algorithm the checksum was stored with,
     * which is told by its length. So changing the hash algorithm setting
     * doesn't fail the content stored before the change.</p>
     *
     * @param id Internal Id of the content
     * @return The new Sanity status
     */
    public SanityStatus processEntity(final Long id) {
        SanityStatus status;

        try {
            final String stored = (String) dao.createNamedQuery("data.findChecksum").setParameter("id", id).getSingleResult();
            final String algorithm = checksumAlgorithm(stored, settings.getHashAlgorithm());
            final int batchSize = settings.getStreamReadBatchSize();
            final DataDao dataDao = new DataDao(dao.getEntityManager());
            final String checksum = dataDao.withConnection(connection -> digest(connection, id, algorithm, batchSize));

            status = Objects.equals(checksum, stored) ? SanityStatus.OK : SanityStatus.FAILED;
            if (status == SanityStatus.FAILED) {
                LOGGER.error("The Data Object with the internal Id {} failed the sanity check; its stored data is damaged.", id);
            }
            // Regardless, the check date is set, so the record is not checked
            // again for a while.
            dataDao.updateSanityStatus(id, status);
        } catch (RuntimeException e) {
            // Another EDS instance may be processing the record, or the
            // database failed; the record is checked again later.
            LOGGER.error("The sanity check of the Data Object with the internal Id {} failed.", id, e);
            status = SanityStatus.BLOCKED;
        }

        return status;
    }

    private static String digest(final java.sql.Connection connection, final long id, final String algorithm, final int batchSize) throws java.sql.SQLException {
        try {
            final java.security.MessageDigest digest = java.security.MessageDigest.getInstance(algorithm);
            final long length = EncryptedDataStore.length(connection, id);
            try (java.io.InputStream stream = EncryptedDataStore.open(connection, id, 0, Math.max(0, length), batchSize)) {
                final byte[] buffer = new byte[64 * 1024];
                int read = stream.read(buffer);
                while (read != -1) {
                    digest.update(buffer, 0, read);
                    read = stream.read(buffer);
                }
            }
            return java.util.Base64.getEncoder().encodeToString(digest.digest());
        } catch (java.security.NoSuchAlgorithmException | java.io.IOException e) {
            throw new net.haugr.eds.core.exceptions.EDSException(net.haugr.eds.api.common.ReturnCode.INTEGRITY_ERROR, e.getMessage(), e);
        }
    }

    /**
     * <p>The algorithm a stored checksum was made with. The checksums are
     * Base64 encoded digests, and the supported algorithms differ in the
     * length of their digest (SHA-384: 48 bytes, SHA-512: 64 bytes), so the
     * length of the checksum tells the algorithm. A checksum of any other
     * length is checked with the current algorithm, and fails.</p>
     *
     * @param stored  The stored checksum
     * @param current The hash algorithm of the settings
     * @return Name of the digest algorithm to check the content with
     */
    static String checksumAlgorithm(final String stored, final HashAlgorithm current) {
        if (stored != null) {
            for (final HashAlgorithm candidate : HashAlgorithm.values()) {
                if (stored.length() == checksumLength(candidate)) {
                    return candidate.getAlgorithm();
                }
            }
        }

        return current.getAlgorithm();
    }

    private static int checksumLength(final HashAlgorithm algorithm) {
        try {
            final int bytes = java.security.MessageDigest.getInstance(algorithm.getAlgorithm()).getDigestLength();
            return ((bytes + 2) / 3) * 4;
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new net.haugr.eds.core.exceptions.EDSException(net.haugr.eds.api.common.ReturnCode.INTEGRITY_ERROR, e.getMessage(), e);
        }
    }

    private void clearExpireSessions() {
        final Query query = dao.createNamedQuery("session.removeExpired");
        LOGGER.debug("expired {} sessions.", query.executeUpdate());
    }

    private void clearExpiredInvitations() {
        final Query query = dao.createNamedQuery("member.removeExpiredInvitations");
        LOGGER.debug("removed {} expired invitations.", query.executeUpdate());
    }

    private List<DataEntity> findRecords(final SanityRequest request) {
        final LocalDateTime since = (request.getSince() == null) ? Utilities.newDate(0L) : request.getSince();
        final List<DataEntity> found;

        if (request.getCircleId() != null) {
            // Find for specific Circle
            found = dao.findFailedRecords(request.getCircleId(), since);
        } else if (member.getMemberRole() == MemberRole.ADMIN) {
            // The System Administrator is allowed to retrieve all records for
            // all Circles.
            found = dao.findFailedRecords(since);
        } else {
            // Find for a specific Member, which will retrieve all records which
            // the member is Administrator for
            found = dao.findFailedRecords(member, since);
        }

        return found;
    }

    private static List<Sanity> convertRecords(final Collection<DataEntity> found) {
        final List<Sanity> sanities = new ArrayList<>(found.size());

        for (final DataEntity entity : found) {
            final Sanity sanity = new Sanity();
            sanity.setDataId(entity.getMetadata().getExternalId());
            sanity.setChanged(entity.getSanityChecked());
            sanities.add(sanity);
        }

        return sanities;
    }
}
