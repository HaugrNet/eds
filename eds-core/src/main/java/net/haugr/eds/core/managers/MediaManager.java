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
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.CredentialType;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.responses.DownloadTokenResponse;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.MetadataEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;
import net.haugr.eds.core.stream.ContentPolicy;

/**
 * <p>Media support for the PWA (PLAN.md, L5): download tokens and
 * thumbnails.</p>
 *
 * <p>A download token lets a client without custom headers, like the
 * {@code <img>} and {@code <video>} elements of a browser, read the content
 * of one Data Object. It is the Session token and an expiry, sealed with the
 * MasterKey wrap key (PLAN.md, Crypto §2a), with the ID of the Data Object in the
 * AAD. Nothing is stored: the token ends at its expiry, when the Session ends
 * (the Session must still open when the token is used), or when the
 * MasterKey changes. Only the server can open it, and it reads only the one
 * content.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class MediaManager extends AbstractManager<DataDao, EDSResponse, Authentication> {

    private static final String DOWNLOAD_AAD = "eds/download|";
    private static final String INVALID_TOKEN = "The download token is invalid or has expired.";

    public MediaManager(final Settings settings, final EntityManager entityManager) {
        super(settings, new DataDao(entityManager));
    }

    /**
     * Not used; see the methods for the single requests.
     */
    @Override
    public EDSResponse perform(final Authentication request) {
        throw new EDSException(ReturnCode.ILLEGAL_SERVICE, "Use the media methods.");
    }

    /**
     * Creates a download token for the content of a Data Object, which the
     * Member may read.
     *
     * @param sessionToken The Session token
     * @param dataId       The Data Object
     * @return The token and its expiry
     */
    public DownloadTokenResponse downloadToken(final byte[] sessionToken, final String dataId) {
        verifyRequest(request(sessionToken), Permission.FETCH_DATA);
        final MetadataEntity metadata = findDataObject(dataId);
        findTrustee(metadata.getCircle().getExternalId());

        final LocalDateTime lifetime = Utilities.newDate().plusMinutes(settings.getDownloadTokenLifetime());
        final LocalDateTime sessionEnd = member.getSessionExpire();
        final LocalDateTime expires = ((sessionEnd != null) && sessionEnd.isBefore(lifetime)) ? sessionEnd : lifetime;

        final byte[] payload = ByteBuffer.allocate(Long.BYTES + sessionToken.length)
                .putLong(expires.toEpochSecond(ZoneOffset.UTC))
                .put(sessionToken)
                .array();
        final byte[] sealed = Base64.getDecoder().decode(crypto.sealWithMasterKey(DOWNLOAD_AAD + metadata.getExternalId(), payload));
        Arrays.fill(payload, (byte) 0);

        final DownloadTokenResponse response = new DownloadTokenResponse();
        response.setToken(Base64.getUrlEncoder().withoutPadding().encodeToString(sealed));
        response.setExpires(expires.withNano(0));

        return response;
    }

    /**
     * Opens a download token for the given Data Object.
     *
     * @param downloadToken The token, URL-safe Base64
     * @param dataId        The Data Object of the request
     * @return The Session token inside
     * @throws EDSException with AUTHENTICATION_WARNING, if the token is
     *                      invalid, for another Data Object, or expired
     */
    public byte[] openDownloadToken(final String downloadToken, final String dataId) {
        final byte[] payload;
        try {
            final byte[] sealed = Base64.getUrlDecoder().decode(downloadToken.trim());
            payload = crypto.openWithMasterKey(DOWNLOAD_AAD + dataId, Base64.getEncoder().encodeToString(sealed));
        } catch (RuntimeException e) {
            throw new EDSException(ReturnCode.AUTHENTICATION_WARNING, INVALID_TOKEN, e);
        }

        final ByteBuffer buffer = ByteBuffer.wrap(payload);
        final long expires = buffer.getLong();
        final byte[] sessionToken = new byte[buffer.remaining()];
        buffer.get(sessionToken);
        Arrays.fill(payload, (byte) 0);
        if (Utilities.newDate().toEpochSecond(ZoneOffset.UTC) >= expires) {
            Arrays.fill(sessionToken, (byte) 0);
            throw new EDSException(ReturnCode.AUTHENTICATION_WARNING, INVALID_TOKEN);
        }

        return sessionToken;
    }

    /**
     * Stores the thumbnail of a Data Object, replacing an existing one. The
     * client makes the image; it is stored encrypted like any content, and
     * the server never reads it.
     *
     * @param sessionToken The Session token
     * @param dataId       The Data Object
     * @param mediaType    Media type of the image
     * @param image        The image
     * @return The response, with the ID of the thumbnail
     */
    public ProcessDataResponse storeThumbnail(final byte[] sessionToken, final String dataId, final String mediaType, final byte[] image) {
        verifyRequest(request(sessionToken), Permission.PROCESS_DATA);
        final MetadataEntity metadata = findDataObject(dataId);
        FeatureFolder.refuseIf(FeatureFolder.isEntry(metadata));
        Thumbnails.refuseIf(Thumbnails.isThumbnail(metadata));
        final TrusteeEntity trustee = findTrustee(metadata.getCircle().getExternalId());
        final String type = (mediaType != null) ? mediaType.trim().toLowerCase(Locale.ROOT) : null;
        throwConditionalException((type == null) || !ContentPolicy.THUMBNAIL_TYPES.contains(type),
                ReturnCode.VERIFICATION_WARNING, "A thumbnail must be one of " + String.join(", ", ContentPolicy.THUMBNAIL_TYPES.stream().sorted().toList()) + '.');
        throwConditionalException((image == null) || (image.length == 0),
                ReturnCode.VERIFICATION_WARNING, "The thumbnail is empty.");
        throwConditionalException(image.length > Constants.MAX_THUMBNAIL_SIZE,
                ReturnCode.CONTENT_TOO_LARGE, "A thumbnail may have at most " + Constants.MAX_THUMBNAIL_SIZE + " bytes.");

        dao.deleteThumbnails(metadata);
        final MetadataEntity thumbnail = new MetadataEntity();
        thumbnail.setCircle(metadata.getCircle());
        thumbnail.setParentId(metadata.getId());
        thumbnail.setType(dao.findDataTypeByName(Constants.THUMBNAIL_TYPENAME));
        thumbnail.setMediaType(type);
        thumbnail.setName(Constants.THUMBNAIL_PREFIX + metadata.getExternalId());
        dao.save(thumbnail);
        encryptAndSave(dao, trustee, thumbnail, image);

        final ProcessDataResponse response = new ProcessDataResponse("The thumbnail of '" + metadata.getName() + "' was stored.");
        response.setDataId(thumbnail.getExternalId());

        return response;
    }

    private MetadataEntity findDataObject(final String dataId) {
        final MetadataEntity metadata = dao.findMetadataByMemberAndExternalId(member.getId(), dataId);
        throwConditionalNullException(metadata,
                ReturnCode.IDENTIFICATION_WARNING, "The Data Object could not be found.");
        throwConditionalException(Objects.equals(Constants.FOLDER_TYPENAME, metadata.getType().getName()),
                ReturnCode.ILLEGAL_ACTION, "Folders have no content.");

        return metadata;
    }

    private static Authentication request(final byte[] token) {
        throwConditionalException((token == null) || (token.length == 0),
                ReturnCode.AUTHENTICATION_WARNING, "The Authorization header with a Session token is missing.");
        final Authentication request = new Authentication();
        request.setCredentialType(CredentialType.SESSION);
        request.setCredential(token);
        return request;
    }
}
