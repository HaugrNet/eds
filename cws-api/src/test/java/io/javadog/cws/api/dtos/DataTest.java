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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class DataTest {

    private static final String LONG_NAME = "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void testClass() {
        final String id = UUID.randomUUID().toString();
        final String circleId = UUID.randomUUID().toString();
        final String folderId = UUID.randomUUID().toString();
        final String dataName = "Data Name";
        final String typeName = "DataType Name";
        final byte[] bytes = { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5,
                               (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 0 };
        final Date added = new Date();

        final Data data = new Data();
        data.setId(id);
        data.setCircleId(circleId);
        data.setFolderId(folderId);
        data.setName(dataName);
        data.setTypeName(typeName);
        data.setBytes(bytes);
        data.setAdded(added);

        assertThat(data.getId(), is(id));
        assertThat(data.getCircleId(), is(circleId));
        assertThat(data.getFolderId(), is(folderId));
        assertThat(data.getName(), is(dataName));
        assertThat(data.getTypeName(), is(typeName));
        assertThat(data.getBytes(), is(bytes));
        assertThat(data.getAdded(), is(added));

        assertThat(data.validate().isEmpty(), is(true));
    }

    @Test
    public void testEmptyClass() {
        final Data data = new Data();
        final Map<String, String> errors = data.validate();

        assertThat(errors.isEmpty(), is(false));
        assertThat(errors.size(), is(2));
        assertThat(errors.get("circleId"), is("The Circle Id is required for new Data Objects."));
        assertThat(errors.get("typeName"), is("The DataType Name is required for new Data Objects."));
    }

    @Test
    public void testNullId() {
        final Data data = new Data();
        data.setId(UUID.randomUUID().toString());
        assertThat(data.getId(), is(not(nullValue())));

        data.setId(null);
        assertThat(data.getId(), is(nullValue()));
    }

    @Test
    public void testInvalidId() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("The value for 'id' is not matching the required pattern '[\\da-z]{8}-[\\da-z]{4}-[\\da-z]{4}-[\\da-z]{4}-[\\da-z]{12}'.");

        final Data data = new Data();

        data.setId("123");
    }

    @Test
    public void testNullCircleId() {
        final Data data = new Data();
        data.setCircleId(UUID.randomUUID().toString());
        assertThat(data.getCircleId(), is(not(nullValue())));

        data.setCircleId(null);
        assertThat(data.getCircleId(), is(nullValue()));
    }

    @Test
    public void testInvalidCircleId() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("The value for 'circleId' is not matching the required pattern '[\\da-z]{8}-[\\da-z]{4}-[\\da-z]{4}-[\\da-z]{4}-[\\da-z]{12}'.");

        final Data data = new Data();

        data.setCircleId("123");
    }

    @Test
    public void testNullFolderId() {
        final Data data = new Data();
        data.setFolderId(UUID.randomUUID().toString());
        assertThat(data.getFolderId(), is(not(nullValue())));

        data.setFolderId(null);
        assertThat(data.getFolderId(), is(nullValue()));
    }

    @Test
    public void testInvalidFolderId() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("The value for 'folderId' is not matching the required pattern '[\\da-z]{8}-[\\da-z]{4}-[\\da-z]{4}-[\\da-z]{4}-[\\da-z]{12}'.");

        final Data data = new Data();
        data.setFolderId("123");
    }

    @Test
    public void testEmptyName() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("The value for 'name' is shorter than the minimum length of 1.");

        final Data data = new Data();
        data.setName("");
    }

    @Test
    public void testTooLongName() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("The value for 'name' is longer than the maximum length of 256.");

        final Data data = new Data();
        data.setName(LONG_NAME);
    }

    @Test
    public void testEmptyTypeName() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("The value for 'typeName' is shorter than the minimum length of 1.");

        final Data data = new Data();
        data.setTypeName("");
    }

    @Test
    public void testTooLongTypeName() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("The value for 'typeName' is longer than the maximum length of 256.");

        final Data data = new Data();
        data.setTypeName(LONG_NAME);
    }

    @Test
    public void testValidationWithInvalidId() {
        final Data data = new Data();
        data.setId(UUID.randomUUID().toString());
        reflectiveCorrection(data, "id", "invalid Id");
        reflectiveCorrection(data, "name", LONG_NAME);
        reflectiveCorrection(data, "folderId", "invalid Folder Id");

        final Map<String, String> errors = data.validate();
        assertThat(errors.isEmpty(), is(false));
        assertThat(errors.size(), is(3));
        assertThat(errors.get("id"), is("The Data Id is invalid."));
        assertThat(errors.get("name"), is("The name of the Data Object may not exceed 256 characters."));
        assertThat(errors.get("folderId"), is("The Folder Id is invalid."));
    }

    @Test
    public void testValidationWithNullId1() {
        final Data data = new Data();
        data.setId(UUID.randomUUID().toString());
        reflectiveCorrection(data, "id", null);
        reflectiveCorrection(data, "circleId", null);
        reflectiveCorrection(data, "folderId", "invalid Folder Id");
        reflectiveCorrection(data, "name", LONG_NAME);
        reflectiveCorrection(data, "typeName", null);

        final Map<String, String> errors = data.validate();
        assertThat(errors.isEmpty(), is(false));
        assertThat(errors.size(), is(4));
        assertThat(errors.get("circleId"), is("The Circle Id is required for new Data Objects."));
        assertThat(errors.get("folderId"), is("The Folder Id is invalid."));
        assertThat(errors.get("name"), is("The name of the Data Object may not exceed 256 characters."));
        assertThat(errors.get("typeName"), is("The DataType Name is required for new Data Objects."));
    }

    @Test
    public void testValidationWithNullId2() {
        final Data data = new Data();
        data.setId(UUID.randomUUID().toString());
        reflectiveCorrection(data, "id", null);
        reflectiveCorrection(data, "circleId", "invalid Circle Id");
        reflectiveCorrection(data, "folderId", "invalid Folder Id");
        reflectiveCorrection(data, "name", LONG_NAME);
        reflectiveCorrection(data, "typeName", LONG_NAME);

        final Map<String, String> errors = data.validate();
        assertThat(errors.isEmpty(), is(false));
        assertThat(errors.size(), is(4));
        assertThat(errors.get("circleId"), is("The Circle Id is invalid."));
        assertThat(errors.get("folderId"), is("The Folder Id is invalid."));
        assertThat(errors.get("name"), is("The name of the Data Object may not exceed 256 characters."));
        assertThat(errors.get("typeName"), is("The name of the DataType may not exceed 256 characters."));
    }

    private static void reflectiveCorrection(final Object instance, final String fieldName, final Object newValue) {
        try {
            final Field field = instance.getClass().getDeclaredField(fieldName);
            final boolean isAccessible = field.isAccessible();
            field.setAccessible(true);
            field.set(instance, newValue);
            field.setAccessible(isAccessible);
        } catch (NoSuchFieldException | IllegalAccessException | SecurityException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
