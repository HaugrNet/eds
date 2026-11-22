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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.dtos.Metadata;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.model.entities.MetadataEntity;

/**
 * <p>Rules of the thumbnails (PLAN.md, L5). A thumbnail is a Data Object
 * of the type {@link Constants#THUMBNAIL_TYPENAME}, made by the client and
 * stored encrypted like any other content. Its parent is the Data Object it
 * shows, not a folder, so listings never show it; the Metadata of the Data
 * Object carries its ID instead. Only the thumbnail endpoint writes it, and
 * it goes away with its Data Object or when the content is replaced. The
 * server never reads the image.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class Thumbnails {

    /** Message of the refusals. */
    public static final String RESERVED = "Thumbnails are written with PUT " + Constants.REST_DATA_BASE + Constants.REST_DATA_THUMBNAIL + " only.";

    private Thumbnails() {
        // Utility class, no instances.
    }

    /**
     * True if the Metadata is a thumbnail.
     *
     * @param metadata Metadata
     * @return True for a thumbnail
     */
    public static boolean isThumbnail(final MetadataEntity metadata) {
        return (metadata != null) && Objects.equals(Constants.THUMBNAIL_TYPENAME, metadata.getType().getName());
    }

    /**
     * Refuses a data request which would change a thumbnail.
     *
     * @param refused True to refuse
     */
    public static void refuseIf(final boolean refused) {
        AbstractManager.throwConditionalException(refused, ReturnCode.ILLEGAL_ACTION, RESERVED);
    }

    /**
     * Sets the thumbnail IDs of the converted Metadata, with one query.
     *
     * @param dao      DAO
     * @param entities The Metadata entities
     * @param converted The converted Metadata, in the same order
     */
    public static void fill(final DataDao dao, final List<MetadataEntity> entities, final List<Metadata> converted) {
        final List<Long> ids = new ArrayList<>(entities.size());
        for (final MetadataEntity entity : entities) {
            if (!Objects.equals(Constants.FOLDER_TYPENAME, entity.getType().getName())) {
                ids.add(entity.getId());
            }
        }

        final Map<Long, String> thumbnails = new HashMap<>();
        for (final MetadataEntity thumbnail : dao.findThumbnails(ids)) {
            thumbnails.put(thumbnail.getParentId(), thumbnail.getExternalId());
        }
        for (int i = 0; i < entities.size(); i++) {
            converted.get(i).setThumbnailId(thumbnails.get(entities.get(i).getId()));
        }
    }
}
