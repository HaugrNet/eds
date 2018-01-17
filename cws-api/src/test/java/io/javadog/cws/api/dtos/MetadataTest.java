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
        metadata.setAdded(new Date());
        sameMetadata.setDataId(metadata.getDataId());
        sameMetadata.setCircleId(metadata.getCircleId());
        sameMetadata.setFolderId(metadata.getFolderId());
        sameMetadata.setDataName(metadata.getDataName());
        sameMetadata.setTypeName(metadata.getTypeName());
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

    @Test
    public void testEquality() {
        final String id1 = UUID.randomUUID().toString();
        final String id2 = UUID.randomUUID().toString();
        final String typeName1 = "First DataType";
        final String typeName2 = "Second DataType";
        final String dataName1 = "First Data Object";
        final String dataName2 = "Second Data Object";
        final Date added1 = new Date(1212121212L);
        final Date added2 = new Date(2121212121L);

        final Metadata metadata1 = prepareMetadata(id1, id1, id1, dataName1, typeName1, added1);
        final Metadata metadata2 = prepareMetadata(id1, id2, id1, dataName1, typeName1, added1);
        final Metadata metadata3 = prepareMetadata(id1, id1, id2, dataName1, typeName1, added1);
        final Metadata metadata4 = prepareMetadata(id1, id1, id1, dataName2, typeName1, added1);
        final Metadata metadata5 = prepareMetadata(id1, id1, id1, dataName1, typeName2, added1);
        final Metadata metadata6 = prepareMetadata(id1, id1, id1, dataName1, typeName1, added2);

        assertThat(metadata1.equals(metadata2), is(false));
        assertThat(metadata2.equals(metadata1), is(false));
        assertThat(metadata1.equals(metadata3), is(false));
        assertThat(metadata3.equals(metadata1), is(false));
        assertThat(metadata1.equals(metadata4), is(false));
        assertThat(metadata4.equals(metadata1), is(false));
        assertThat(metadata1.equals(metadata5), is(false));
        assertThat(metadata5.equals(metadata1), is(false));
        assertThat(metadata1.equals(metadata6), is(false));
        assertThat(metadata6.equals(metadata1), is(false));
    }

    // =========================================================================
    // Internal Helper Method
    // =========================================================================

    private static Metadata prepareMetadata(final String dataId, final String circleId, final String folderId, final String dataName, final String typeName, final Date added) {
        final Metadata metadata = new Metadata();
        metadata.setDataId(dataId);
        metadata.setCircleId(circleId);
        metadata.setFolderId(folderId);
        metadata.setDataName(dataName);
        metadata.setTypeName(typeName);
        metadata.setAdded(added);

        return metadata;
    }
}
