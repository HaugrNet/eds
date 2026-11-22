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
import java.util.Objects;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.CredentialType;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.jce.DataFormat;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.model.EncryptedDataStore;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.MetadataEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;
import net.haugr.eds.core.stream.ByteRange;
import net.haugr.eds.core.stream.ContentDownload;

/**
 * <p>Prepares a content download (PLAN.md, S3): authenticates the
 * Session, checks the access, reads the header and size of the stored
 * content (not the content), and resolves Range and If-Range. Everything
 * that can fail before the response starts fails here; the streaming itself
 * is {@link net.haugr.eds.core.stream.ContentStreamer}.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class DownloadDataManager extends AbstractManager<DataDao, EDSResponse, FetchDataRequest> {

    public DownloadDataManager(final Settings settings, final EntityManager entityManager) {
        super(settings, new DataDao(entityManager));
    }

    /**
     * Prepares the download.
     *
     * @param sessionToken The Session token
     * @param dataId       The Data Object
     * @param rangeHeader  Range header, or null
     * @param ifRange      If-Range header, or null
     * @return The prepared download
     */
    public ContentDownload prepare(final byte[] sessionToken, final String dataId, final String rangeHeader, final String ifRange) {
        if ((sessionToken == null) || (sessionToken.length == 0)) {
            throw new EDSException(ReturnCode.AUTHENTICATION_WARNING, "The Authorization header with a Session token is missing.");
        }
        final FetchDataRequest request = new FetchDataRequest();
        request.setCredentialType(CredentialType.SESSION);
        request.setCredential(sessionToken);
        request.setDataId(dataId);
        verifyRequest(request, Permission.FETCH_DATA);

        final MetadataEntity metadata = dao.findMetadataByMemberAndExternalId(member.getId(), dataId);
        // An entry of a feature which is switched off isn't read (FEATURES.md).
        if ((metadata == null) || FeatureFolder.isSwitchedOff(dao, metadata)) {
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING, "The Data Object could not be found.");
        }
        if (Objects.equals(Constants.FOLDER_TYPENAME, metadata.getType().getName())) {
            throw new EDSException(ReturnCode.ILLEGAL_ACTION, "Folders have no content.");
        }
        // Finding the Trustee checks the access, so it comes before anything
        // about the content is looked at or told.
        @SuppressWarnings("java:S1941")
        final TrusteeEntity trustee = findTrustee(metadata.getCircle().getExternalId());
        final Object[] info = dao.findContentInfo(metadata);
        if (info == null) {
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING, "The Data Object has no content.");
        }
        final long id = (Long) info[0];

        final long storedLength = dao.withConnection(connection -> EncryptedDataStore.length(connection, id));
        if (storedLength < DataFormat.HEADER_BYTES) {
            throw new EDSException(ReturnCode.INTEGRITY_ERROR, "The stored content is damaged.");
        }
        final byte[] headerBytes = dao.withConnection(connection -> EncryptedDataStore.read(connection, id, 0, DataFormat.HEADER_BYTES));
        final DataFormat.Header header = DataFormat.Header.read(headerBytes);
        final String etag = DataFormat.etag(header);
        final long size = DataFormat.plaintextLength(storedLength, header.chunkSize());

        // If-Range: only serve the range if the content hasn't changed.
        final boolean useRange = (ifRange == null) || Objects.equals(ifRange.trim(), etag);
        final ByteRange range = ByteRange.parse(useRange ? rangeHeader : null, size);
        if (range.kind() == ByteRange.Kind.UNSATISFIABLE) {
            final EDSResponse error = new EDSResponse(ReturnCode.RANGE_NOT_SATISFIABLE, "The range is outside the " + size + " bytes of the content.");
            return ContentDownload.unsatisfiable(error, header, storedLength, etag);
        }

        final long keyId = (Long) info[1];
        final DataFormat.Context context = new DataFormat.Context(metadata.getCircle().getExternalId(), metadata.getExternalId(), keyId);
        final ContentDownload download = ContentDownload.prepared(new ContentDownload.Stored(id, header, storedLength, etag),
                new ContentDownload.Described(metadata.getExternalId(), metadata.getMediaType(), metadata.getName()),
                context, range, unwrapCircleKey(trustee, keyId));
        // Content on the retiring secret may be re-encrypted before it is
        // streamed (PLAN.md, Crypto C6), so the new secret goes with it.
        return (trustee.getKey().getId() == keyId) ? download : download.rotating(new ContentDownload.Rotated(trustee.getKey().getId(), unwrapCircleKey(trustee)));
    }

    /**
     * Not used, the download has two steps; see {@link #prepare}.
     */
    @Override
    public EDSResponse perform(final FetchDataRequest request) {
        throw new EDSException(ReturnCode.ILLEGAL_SERVICE, "Use prepare for downloads.");
    }
}
