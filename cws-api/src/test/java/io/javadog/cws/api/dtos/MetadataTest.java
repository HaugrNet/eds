/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.util.Date;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class MetadataTest {

    @Test
    public void testClass() {
        final String id = UUID.randomUUID().toString();
        final String circleId = UUID.randomUUID().toString();
        final String folderId = UUID.randomUUID().toString();
        final String dataName = "Data Name";
        final Date added = new Date();

        final DataType dataType = new DataType();
        dataType.setName("DataType Name");
        dataType.setType("DataType Type");

        final Metadata metadata = new Metadata();
        metadata.setDataId(id);
        metadata.setCircleId(circleId);
        metadata.setFolderId(folderId);
        metadata.setName(dataName);
        metadata.setDataType(dataType);
        metadata.setAdded(added);

        assertThat(metadata.getDataId(), is(id));
        assertThat(metadata.getCircleId(), is(circleId));
        assertThat(metadata.getFolderId(), is(folderId));
        assertThat(metadata.getName(), is(dataName));
        assertThat(metadata.getDataType(), is(dataType));
        assertThat(metadata.getAdded(), is(added));
    }

    @Test
    public void testStandardMethods() {
        final DataType dataType = new DataType();
        dataType.setName("DataType Name");
        dataType.setType("DataType Type");

        final Metadata metadata = new Metadata();
        final Metadata sameMetadata = new Metadata();
        final Metadata emptyMetadata = new Metadata();

        metadata.setDataId(UUID.randomUUID().toString());
        metadata.setCircleId(UUID.randomUUID().toString());
        metadata.setFolderId(UUID.randomUUID().toString());
        metadata.setName("Data Record");
        metadata.setDataType(dataType);
        metadata.setAdded(new Date());
        sameMetadata.setDataId(metadata.getDataId());
        sameMetadata.setCircleId(metadata.getCircleId());
        sameMetadata.setFolderId(metadata.getFolderId());
        sameMetadata.setName(metadata.getName());
        sameMetadata.setDataType(metadata.getDataType());
        sameMetadata.setAdded(metadata.getAdded());

        assertThat(metadata.equals(null), is(false));
        assertThat(metadata.equals(metadata), is(true));
        assertThat(metadata.equals(sameMetadata), is(true));
        assertThat(metadata.equals(emptyMetadata), is(false));

        assertThat(metadata.hashCode(), is(sameMetadata.hashCode()));
        assertThat(metadata.hashCode(), is(not(emptyMetadata.hashCode())));

        assertThat(metadata.toString(), is(sameMetadata.toString()));
        assertThat(metadata.toString(), is(not(emptyMetadata.toString())));
    }
}
