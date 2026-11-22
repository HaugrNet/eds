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

import java.util.Objects;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.FeatureState;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.model.FeatureDao;
import net.haugr.eds.core.model.entities.CircleFeatureEntity;
import net.haugr.eds.core.model.entities.MetadataEntity;

/**
 * <p>Rules for the folders of the features of a Circle (FEATURES.md), which hold
 * the entries: Data Objects of the type {@code entry}. Folder and entries
 * are written by the feature only, so the rules of its declaration hold; the
 * data requests may read them, but not add, change, move, copy or delete
 * them. While a feature is switched off in a Circle, they can't read them
 * either.</p>
 *
 * <p>The name of a feature's folder, a dot and the name of a feature which the
 * stone offers, is reserved in the root folder of every Circle, ignoring
 * case; elsewhere it is an ordinary name.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class FeatureFolder {

    /** Message of the refusals. */
    public static final String RESERVED = "The folder of a feature and its entries are written by the feature only.";

    private FeatureFolder() {
        // Utility class, no instances.
    }

    /**
     * True if the folder is the root folder of a Circle.
     *
     * @param folder Metadata of a folder
     * @return True for a root folder
     */
    public static boolean isRoot(final MetadataEntity folder) {
        return (folder != null) && Objects.equals(0L, folder.getParentId());
    }

    /**
     * True if the Metadata is an entry of a feature.
     *
     * @param metadata Metadata
     * @return True for an entry
     */
    public static boolean isEntry(final MetadataEntity metadata) {
        return (metadata != null) && Objects.equals(Constants.ENTRY_TYPENAME, metadata.getType().getName());
    }

    /**
     * True if the Metadata is the folder of a feature of its Circle.
     *
     * @param dao      DAO, to look the feature up
     * @param metadata Metadata
     * @return True for the folder of a feature
     */
    public static boolean isFeatureFolder(final DataDao dao, final MetadataEntity metadata) {
        return (metadata != null)
                && Objects.equals(Constants.FOLDER_TYPENAME, metadata.getType().getName())
                && (metadata.getName() != null)
                && metadata.getName().startsWith(".")
                && (features(dao).findCircleFeatureByFolder(metadata.getId()) != null);
    }

    /**
     * True if the Metadata belongs to a feature: its folder or an entry.
     *
     * @param dao      DAO, to look the feature up
     * @param metadata Metadata
     * @return True if written by the feature only
     */
    public static boolean isFeature(final DataDao dao, final MetadataEntity metadata) {
        return isEntry(metadata) || isFeatureFolder(dao, metadata);
    }

    /**
     * True if the name is that of the folder of a feature which the stone
     * offers, and the parent is the root folder of a Circle.
     *
     * @param dao    DAO, to look the feature up
     * @param parent The folder in which the name is to be used
     * @param name   The name
     * @return True if the name is reserved there
     */
    public static boolean isReservedName(final DataDao dao, final MetadataEntity parent, final String name) {
        if (!isRoot(parent) || (name == null)) {
            return false;
        }
        final String trimmed = name.trim().toLowerCase(java.util.Locale.ROOT);

        return trimmed.startsWith(".") && (features(dao).findFeature(trimmed.substring(1)) != null);
    }

    /**
     * True if the Metadata belongs to a feature which is switched off in its
     * Circle, so nothing of it is read.
     *
     * @param dao      DAO, to look the feature up
     * @param metadata Metadata
     * @return True if hidden
     */
    public static boolean isSwitchedOff(final DataDao dao, final MetadataEntity metadata) {
        if (metadata == null) {
            return false;
        }
        CircleFeatureEntity feature = null;
        if (isEntry(metadata)) {
            feature = features(dao).findCircleFeatureByFolder(metadata.getParentId());
        } else if (Objects.equals(Constants.FOLDER_TYPENAME, metadata.getType().getName()) && (metadata.getName() != null) && metadata.getName().startsWith(".")) {
            feature = features(dao).findCircleFeatureByFolder(metadata.getId());
        }

        return (feature != null) && (feature.getState() == FeatureState.OFF);
    }

    /**
     * Refuses a data request which would write a feature's folder or entry.
     *
     * @param refused True to refuse
     */
    public static void refuseIf(final boolean refused) {
        AbstractManager.throwConditionalException(refused, ReturnCode.ILLEGAL_ACTION, RESERVED);
    }

    private static FeatureDao features(final DataDao dao) {
        return (dao instanceof FeatureDao feature) ? feature : new FeatureDao(dao.entityManager());
    }
}
