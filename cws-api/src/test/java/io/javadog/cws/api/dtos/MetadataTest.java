/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Utilities;
import org.junit.Test;

import java.util.Date;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class MetadataTest {

    @Test
    public void testClassflow() {
        final String id = UUID.randomUUID().toString();
        final String circleId = UUID.randomUUID().toString();
        final String folderId = UUID.randomUUID().toString();
        final String dataName = "Data Name";
        final String typeName = "Data Type";
        final Date added = new Date();

        final Metadata metadata = new Metadata();
        metadata.setDataId(id);
        metadata.setCircleId(circleId);
        metadata.setFolderId(folderId);
        metadata.setDataName(dataName);
        metadata.setTypeName(typeName);
        metadata.setAdded(added);

        assertThat(metadata.getDataId(), is(id));
        assertThat(metadata.getCircleId(), is(circleId));
        assertThat(metadata.getFolderId(), is(folderId));
        assertThat(metadata.getDataName(), is(dataName));
        assertThat(metadata.getTypeName(), is(typeName));
        assertThat(metadata.getAdded(), is(added));
    }

    @Test
    public void testStandardMethods() {
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

        assertThat(metadata.toString(), is(sameMetadata.toString()));
        assertThat(metadata.toString(), is(not(emptyMetadata.toString())));
    }
}
