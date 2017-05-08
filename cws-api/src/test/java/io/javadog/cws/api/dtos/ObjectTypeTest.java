/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ObjectTypeTest {

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void testClass() {
        final String name = "name1";
        final String type = "type1";

        final DataType objectType = new DataType();
        objectType.setName(name);
        objectType.setType(type);

        assertThat(objectType.getName(), is(name));
        assertThat(objectType.getType(), is(type));

        final Map<String, String> errors = objectType.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testNullName() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("The value for 'name' may not be null.");

        final DataType type = new DataType();
        type.setName(null);
    }

    @Test
    public void testEmptyName() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("The value for 'name' is outside of the allowed boundaries.");

        final DataType type = new DataType();
        type.setName("");
    }

    @Test
    public void testLongName() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("The value for 'name' is outside of the allowed boundaries.");

        final String data = "12345678901234567890123456789012345678901234567890";
        final String value = data + data + data + data + data + data;

        final DataType type = new DataType();
        type.setName(value);
    }

    @Test
    public void testNullType() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("The value for 'type' may not be null.");

        final DataType type = new DataType();
        type.setType(null);
    }

    @Test
    public void testEmptyType() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("The value for 'type' is outside of the allowed boundaries.");

        final DataType type = new DataType();
        type.setType("");
    }

    @Test
    public void testLongType() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("The value for 'type' is outside of the allowed boundaries.");

        final String data = "12345678901234567890123456789012345678901234567890";
        final String value = data + data + data + data + data + data;

        final DataType type = new DataType();
        type.setType(value);
    }

    @Test
    public void testEmptyObject() {
        final DataType type = new DataType();

        final Map<String, String> errors = type.validate();
        assertThat(errors.size(), is(2));
        assertThat(errors.get("name"), is("The Name is not defined."));
        assertThat(errors.get("type"), is("The Type is not defined."));
    }

    @Test
    public void testForcingEmptyData() throws NoSuchFieldException, IllegalAccessException {
        final DataType type = new DataType();
        final Field fieldName = type.getClass().getDeclaredField("name");
        fieldName.setAccessible(true);
        fieldName.set(type, "");
        final Field fieldType = type.getClass().getDeclaredField("type");
        fieldType.setAccessible(true);
        fieldType.set(type, "");

        final Map<String, String> errors = type.validate();
        assertThat(errors.size(), is(2));
        assertThat(errors.get("name"), is("The Name may not be empty."));
        assertThat(errors.get("type"), is("The Type may not be empty."));
    }

    @Test
    public void testForcingLongData() throws NoSuchFieldException, IllegalAccessException {
        final String data = "12345678901234567890123456789012345678901234567890";
        final String value = data + data + data + data + data + data;
        final DataType type = new DataType();
        final Field fieldName = type.getClass().getDeclaredField("name");
        fieldName.setAccessible(true);
        fieldName.set(type, value);
        final Field fieldType = type.getClass().getDeclaredField("type");
        fieldType.setAccessible(true);
        fieldType.set(type, value);

        final Map<String, String> errors = type.validate();
        assertThat(errors.size(), is(2));
        assertThat(errors.get("name"), is("The Name is longer than the allowed 256 characters."));
        assertThat(errors.get("type"), is("The Type is longer than the allowed 256 characters."));
    }
}
