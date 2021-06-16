/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.api.dtos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.javadog.cws.api.common.Utilities;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class MetadataTest {

    @Test
    void testClassFlow() {
        final String id = UUID.randomUUID().toString();
        final String circleId = UUID.randomUUID().toString();
        final String folderId = UUID.randomUUID().toString();
        final String dataName = "Data Name";
        final String typeName = "Data Type";
        final LocalDateTime added = Utilities.newDate();

        final Metadata metadata = new Metadata();
        metadata.setDataId(id);
        metadata.setCircleId(circleId);
        metadata.setFolderId(folderId);
        metadata.setDataName(dataName);
        metadata.setTypeName(typeName);
        metadata.setAdded(added);

        assertEquals(id, metadata.getDataId());
        assertEquals(circleId, metadata.getCircleId());
        assertEquals(folderId, metadata.getFolderId());
        assertEquals(dataName, metadata.getDataName());
        assertEquals(typeName, metadata.getTypeName());
        assertEquals(added, metadata.getAdded());
    }

    @Test
    void testStandardMethods() {
        final Metadata metadata = new Metadata();
        final Metadata sameMetadata = new Metadata();
        final Metadata emptyMetadata = new Metadata();

        metadata.setDataId(UUID.randomUUID().toString());
        metadata.setCircleId(UUID.randomUUID().toString());
        metadata.setFolderId(UUID.randomUUID().toString());
        metadata.setDataName("Data Record");
        metadata.setTypeName("Data Type");
        metadata.setAdded(Utilities.newDate());
        sameMetadata.setDataId(metadata.getDataId());
        sameMetadata.setCircleId(metadata.getCircleId());
        sameMetadata.setFolderId(metadata.getFolderId());
        sameMetadata.setDataName(metadata.getDataName());
        sameMetadata.setTypeName(metadata.getTypeName());
        sameMetadata.setAdded(metadata.getAdded());

        assertEquals(sameMetadata.toString(), metadata.toString());
        assertNotEquals(emptyMetadata.toString(), metadata.toString());
    }
}
