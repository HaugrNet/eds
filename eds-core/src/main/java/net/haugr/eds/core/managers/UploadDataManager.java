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
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.CredentialType;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.enums.SanityStatus;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.jce.DataFormat;
import net.haugr.eds.core.jce.DataStreams;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.model.EncryptedDataStore;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.DataEntity;
import net.haugr.eds.core.model.entities.MetadataEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;

/**
 * <p>Uploads the content of a Data Object as a stream (PLAN.md, S2).
 * The content is encrypted chunk by chunk in the §4 format while the
 * database reads it, with the SHA-512 of the ciphertext computed on the way.
 * Any existing content is replaced; the caller's transaction decides, so a
 * failed or interrupted upload leaves the previous content in place.</p>
 *
 * <p>The length of the content is normally given (Content-Length), which
 * allows refusing content that is too large before reading it, and writing
 * it to the database as it arrives. Without a length, e.g. from a pipe, the
 * content is read to its end; see {@link #writeToTheEnd}.</p>
 *
 * <p>Authentication is by Session only (the token from the Authorization
 * header), as the body is the raw content.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class UploadDataManager extends AbstractManager<DataDao, ProcessDataResponse, ProcessDataRequest> {

    private final InputStream content;
    private final long length;
    private final String ifMatch;

    public UploadDataManager(final Settings settings, final EntityManager entityManager, final InputStream content, final long length, final String ifMatch) {
        super(settings, new DataDao(entityManager));
        this.content = content;
        this.length = length;
        this.ifMatch = ifMatch;
    }

    /**
     * Builds the internal request for the upload.
     *
     * @param sessionToken The Session token
     * @param dataId       The Data Object
     * @return Request
     */
    public static ProcessDataRequest request(final byte[] sessionToken, final String dataId) {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setCredentialType(CredentialType.SESSION);
        request.setCredential(sessionToken);
        request.setAction(Action.UPDATE);
        request.setDataId(dataId);

        return request;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessDataResponse perform(final ProcessDataRequest request) {
        if ((request.getCredential() == null) || (request.getCredential().length == 0)) {
            throw new EDSException(ReturnCode.AUTHENTICATION_WARNING, "The Authorization header with a Session token is missing.");
        }
        // With a length, content that is too large is refused before any of
        // it is read; without one (a pipe), while it is read.
        final boolean lengthKnown = length >= 0;
        final long maxSize = settings.getStreamMaxObjectSize();
        if (lengthKnown && (length > maxSize)) {
            throw tooLarge(maxSize);
        }
        verifyRequest(request, Permission.PROCESS_DATA);

        final MetadataEntity metadata = dao.findMetadataByMemberAndExternalId(member.getId(), request.getDataId());
        if (metadata == null) {
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING, "The Data Object could not be found.");
        }
        if (Objects.equals(Constants.FOLDER_TYPENAME, metadata.getType().getName())) {
            throw new EDSException(ReturnCode.ILLEGAL_ACTION, "Folders have no content.");
        }
        FeatureFolder.refuseIf(FeatureFolder.isEntry(metadata));
        Thumbnails.refuseIf(Thumbnails.isThumbnail(metadata));
        final TrusteeEntity trustee = findTrustee(metadata.getCircle().getExternalId());
        // The thumbnail no longer matches the new content; if the upload
        // fails, the rollback restores it.
        dao.deleteThumbnails(metadata);

        // The existing content is never loaded, it may be large; only its
        // Id, and the Key is switched to the current Circle Secret.
        final Object[] existing = dao.findContentInfo(metadata);
        final long id;
        if (existing != null) {
            id = (Long) existing[0];
            checkIfMatch(id);
            dao.updateContentKey(id, trustee.getKey());
        } else {
            checkIfMatch(-1);
            final DataEntity entity = new DataEntity();
            entity.setMetadata(metadata);
            entity.setKey(trustee.getKey());
            // Replaced by the streamed write below, in the same transaction.
            entity.setData(new byte[0]);
            entity.setChecksum("pending");
            entity.setSanityStatus(SanityStatus.OK);
            entity.setSanityChecked(Utilities.newDate());
            dao.save(entity);
            id = entity.getId();
            // Detached, so a later flush can't overwrite the streamed content.
            dao.withConnection(_ -> {
                dao.detach(entity);
                return null;
            });
        }

        final SecretEDSKey circleSecret = unwrapCircleKey(trustee);
        final DataFormat.Context context = new DataFormat.Context(metadata.getCircle().getExternalId(), metadata.getExternalId(), trustee.getKey().getId());
        final String algorithm = settings.getHashAlgorithm().getAlgorithm();
        // The stream reads the body of the request, which the HTTP layer
        // owns and closes; closing it here would close that body.
        @SuppressWarnings("PMD.CloseResource")
        final DataStreams.EncryptingStream encrypting;
        if (lengthKnown) {
            encrypting = DataStreams.encrypt(circleSecret, context, content, length);
            // The content is replaced, so its current length doesn't count.
            StorageCeiling.check(settings, dao, id, encrypting.storedLength());
            try {
                dao.withConnection(connection -> EncryptedDataStore.write(connection, id, encrypting, encrypting.storedLength(), algorithm, settings.getStreamReadBatchSize()));
            } catch (EDSException e) {
                throw e;
            } catch (RuntimeException e) {
                // The body ended early, or the client or the database failed:
                // the transaction is rolled back, the previous content stays.
                throw rolledBack(e);
            }
        } else {
            encrypting = writeToTheEnd(id, circleSecret, context, maxSize, algorithm);
        }

        final ProcessDataResponse response = new ProcessDataResponse("The content of " + theDataObject(metadata) + " was successfully uploaded.");
        response.setDataId(metadata.getExternalId());
        response.setEtag(DataFormat.etag(encrypting.header()));

        return response;
    }

    /**
     * Stores content of unknown length. Neither the size limit nor the
     * storage ceiling can be checked up front, so the plaintext is cut off
     * where it passes the smaller of the two, and the stored length is
     * checked against the ceiling afterwards; either way the transaction is
     * rolled back, and the previous content stays. Nothing is spooled on
     * the way: each part is written as it has arrived.
     */
    private DataStreams.EncryptingStream writeToTheEnd(final long id, final SecretEDSKey circleSecret, final DataFormat.Context context, final long maxSize, final String algorithm) {
        final long room = StorageCeiling.remaining(settings, dao, id);
        if (room <= 0) {
            throw new EDSException(ReturnCode.INSUFFICIENT_STORAGE, StorageCeiling.REACHED);
        }
        // The stored content is longer than the plaintext, so plaintext
        // beyond the room can't fit either.
        // The stream reads the body of the request, which the HTTP layer
        // owns and closes; closing it here would close that body.
        @SuppressWarnings("PMD.CloseResource")
        final BoundedStream bounded = new BoundedStream(content, Math.min(maxSize, room));
        final DataStreams.EncryptingStream encrypting = DataStreams.encrypt(circleSecret, context, bounded);
        final long stored;
        try {
            stored = dao.withConnection(connection -> EncryptedDataStore.writeToTheEnd(connection, id, encrypting, algorithm, settings.getStreamReadBatchSize()));
        } catch (EDSException e) {
            throw e;
        } catch (RuntimeException e) {
            if (bounded.exceeded()) {
                throw (maxSize <= room) ? tooLarge(maxSize) : new EDSException(ReturnCode.INSUFFICIENT_STORAGE, StorageCeiling.REACHED, e);
            }
            throw rolledBack(e);
        }
        if (stored > room) {
            throw new EDSException(ReturnCode.INSUFFICIENT_STORAGE, StorageCeiling.REACHED);
        }

        return encrypting;
    }

    private static EDSException tooLarge(final long maxSize) {
        return new EDSException(ReturnCode.CONTENT_TOO_LARGE, "The content is larger than the allowed " + maxSize + " bytes.");
    }

    private static EDSException rolledBack(final RuntimeException e) {
        return new EDSException(ReturnCode.VERIFICATION_WARNING, "The upload failed and was rolled back: " + rootMessage(e), e);
    }

    /**
     * Passes on at most the given number of bytes; a source with more fails
     * the read, and remembers why.
     */
    private static final class BoundedStream extends FilterInputStream {

        private long left;
        private boolean exceeded = false;

        private BoundedStream(final InputStream source, final long limit) {
            super(source);
            this.left = limit;
        }

        private boolean exceeded() {
            return exceeded;
        }

        @Override
        public int read() throws IOException {
            final int value = super.read();
            if (value != -1) {
                count(1);
            }

            return value;
        }

        @Override
        public int read(final byte[] bytes, final int offset, final int length) throws IOException {
            final int read = super.read(bytes, offset, length);
            if (read > 0) {
                count(read);
            }

            return read;
        }

        private void count(final int read) throws IOException {
            left -= read;
            if (left < 0) {
                exceeded = true;
                throw new IOException("The content is larger than allowed.");
            }
        }
    }

    private void checkIfMatch(final long dataId) {
        if (ifMatch != null) {
            final String current = (dataId >= 0) ? etagOf(dataId) : null;
            final boolean matches = "*".equals(ifMatch.trim()) ? (current != null) : Objects.equals(ifMatch.trim(), current);
            if (!matches) {
                throw new EDSException(ReturnCode.PRECONDITION_FAILED, "The content has changed (If-Match).");
            }
        }
    }

    private String etagOf(final long id) {
        final byte[] header = dao.withConnection(connection -> EncryptedDataStore.read(connection, id, 0, DataFormat.HEADER_BYTES));
        return (header.length == DataFormat.HEADER_BYTES) ? DataFormat.etag(DataFormat.Header.read(header)) : null;
    }

    private static String rootMessage(final Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        return cause.getMessage();
    }

    private static String theDataObject(final MetadataEntity metadata) {
        return "the Data Object '" + metadata.getName() + '\'';
    }
}
