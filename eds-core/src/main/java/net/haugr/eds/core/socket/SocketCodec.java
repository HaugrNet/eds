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
package net.haugr.eds.core.socket;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.io.StringReader;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.SocketFrameType;
import net.haugr.eds.api.dtos.CircleFeature;
import net.haugr.eds.api.dtos.Entry;
import net.haugr.eds.api.dtos.SocketFrame;
import net.haugr.eds.core.exceptions.EDSException;

/**
 * <p>Reads and writes the JSON of the frames of the socket (FEATURES.md).
 * JSON-P is used, as it is part of Jakarta EE, so no JSON binding is
 * needed. Fields which a frame doesn't use are left out.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class SocketCodec {

    private SocketCodec() {
        // Utility class, no instances.
    }

    /**
     * Reads a frame which a client sent.
     *
     * @param text The frame
     * @return The frame
     * @throws EDSException with VERIFICATION_WARNING if it isn't a frame
     */
    public static SocketFrame parse(final String text) {
        final JsonObject json;
        try (JsonReader reader = Json.createReader(new StringReader(text))) {
            json = reader.readObject();
        } catch (JsonException | IllegalStateException e) {
            throw invalid("The frame must be a JSON object.", e);
        }

        final String type = string(json, Constants.FIELD_TYPE);
        if (type == null) {
            throw invalid("The frame has no known type.");
        }
        final SocketFrame frame = new SocketFrame();
        try {
            frame.setType(SocketFrameType.valueOf(type));
        } catch (IllegalArgumentException e) {
            throw invalid("The frame has no known type.", e);
        }
        frame.setRef(string(json, Constants.FIELD_REF));
        frame.setToken(string(json, Constants.FIELD_TOKEN));
        frame.setCircleId(string(json, Constants.FIELD_CIRCLE_ID));
        frame.setFeature(string(json, Constants.FIELD_FEATURE));
        frame.setKind(string(json, Constants.FIELD_KIND));
        frame.setEntryId(string(json, Constants.FIELD_ENTRY_ID));
        frame.setAbout(string(json, Constants.FIELD_ABOUT));
        frame.setName(string(json, Constants.FIELD_NAME));
        frame.setContent(string(json, Constants.FIELD_CONTENT));
        frame.setClosed(bool(json, Constants.FIELD_CLOSED));
        frame.setAfter(number(json, Constants.FIELD_AFTER));
        frame.setBefore(number(json, Constants.FIELD_BEFORE));
        frame.setOldestFirst(bool(json, Constants.FIELD_OLDEST_FIRST));
        final Long limit = number(json, Constants.FIELD_LIMIT);
        frame.setLimit((limit != null) ? Math.clamp(limit, Integer.MIN_VALUE, Integer.MAX_VALUE) : null);
        frame.setBare(bool(json, Constants.FIELD_BARE));
        frame.setSignal(string(json, Constants.FIELD_SIGNAL));
        frame.setTo(string(json, Constants.FIELD_TO));
        frame.setUnseen(bool(json, Constants.FIELD_UNSEEN));

        return frame;
    }

    /**
     * Writes a frame which the stone sends.
     *
     * @param frame The frame
     * @return The frame as JSON
     */
    public static String write(final SocketFrame frame) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(Constants.FIELD_TYPE, frame.getType().name());
        add(builder, Constants.FIELD_REF, frame.getRef());
        add(builder, Constants.FIELD_MEMBER_ID, frame.getMemberId());
        add(builder, Constants.FIELD_ACCOUNT_NAME, frame.getAccountName());
        add(builder, Constants.FIELD_CIRCLE_ID, frame.getCircleId());
        add(builder, Constants.FIELD_FEATURE, frame.getFeature());
        add(builder, Constants.FIELD_SIGNAL, frame.getSignal());
        add(builder, Constants.FIELD_TO, frame.getTo());
        if (frame.getPresent() != null) {
            final JsonArrayBuilder present = Json.createArrayBuilder();
            frame.getPresent().forEach(present::add);
            builder.add(Constants.FIELD_PRESENT, present);
        }
        if (frame.getEntry() != null) {
            builder.add(Constants.FIELD_ENTRY, entry(frame.getEntry()));
        }
        if (frame.getEntries() != null) {
            final JsonArrayBuilder entries = Json.createArrayBuilder();
            for (final Entry entry : frame.getEntries()) {
                entries.add(entry(entry));
            }
            builder.add(Constants.FIELD_ENTRIES, entries);
        }
        if (frame.getCount() != null) {
            builder.add(Constants.FIELD_COUNT, frame.getCount());
        }
        if (frame.getFeatures() != null) {
            final JsonArrayBuilder features = Json.createArrayBuilder();
            for (final CircleFeature feature : frame.getFeatures()) {
                features.add(feature(feature));
            }
            builder.add(Constants.FIELD_FEATURES, features);
        }
        add(builder, Constants.FIELD_DATA_ID, frame.getDataId());
        if (frame.getReturnCode() != null) {
            builder.add(Constants.FIELD_RETURN_CODE, frame.getReturnCode());
        }
        add(builder, Constants.FIELD_RETURN_MESSAGE, frame.getReturnMessage());

        return builder.build().toString();
    }

    private static JsonObjectBuilder feature(final CircleFeature feature) {
        final JsonObjectBuilder object = Json.createObjectBuilder();
        add(object, Constants.FIELD_NAME, feature.getName());
        if (feature.getVersion() != null) {
            object.add(Constants.FIELD_VERSION, feature.getVersion());
        }
        add(object, Constants.FIELD_STATE, (feature.getState() != null) ? feature.getState().name() : null);

        return object;
    }

    private static JsonObjectBuilder entry(final Entry entry) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        add(builder, Constants.FIELD_ENTRY_ID, entry.getEntryId());
        add(builder, Constants.FIELD_CIRCLE_ID, entry.getCircleId());
        add(builder, Constants.FIELD_FEATURE, entry.getFeature());
        add(builder, Constants.FIELD_KIND, entry.getKind());
        if (entry.getPosition() != null) {
            builder.add(Constants.FIELD_POSITION, entry.getPosition());
        }
        add(builder, Constants.FIELD_ABOUT, entry.getAbout());
        add(builder, Constants.FIELD_NAME, entry.getName());
        add(builder, Constants.FIELD_MEMBER_ID, entry.getMemberId());
        add(builder, Constants.FIELD_ACCOUNT_NAME, entry.getAccountName());
        add(builder, Constants.FIELD_ADDED, (entry.getAdded() != null) ? entry.getAdded().toString() : null);
        add(builder, Constants.FIELD_CHANGED, (entry.getChanged() != null) ? entry.getChanged().toString() : null);
        if (entry.getClosed() != null) {
            builder.add(Constants.FIELD_CLOSED, entry.getClosed());
        }
        add(builder, Constants.FIELD_CONTENT, entry.getContent());

        return builder;
    }

    private static void add(final JsonObjectBuilder builder, final String name, final String value) {
        if (value != null) {
            builder.add(name, value);
        }
    }

    private static String string(final JsonObject json, final String name) {
        final JsonValue value = json.get(name);
        if ((value == null) || (value.getValueType() == JsonValue.ValueType.NULL)) {
            return null;
        }
        if (value.getValueType() != JsonValue.ValueType.STRING) {
            throw invalid(field(name) + " must be text.");
        }

        return ((JsonString) value).getString();
    }

    private static String field(final String name) {
        return "The field '" + name + '\'';
    }

    // Three states on purpose: a missing field is null, as for the closed
    // flag "neither close nor open" is refused, which false would hide.
    @SuppressWarnings("java:S2447")
    private static Boolean bool(final JsonObject json, final String name) {
        final JsonValue value = json.get(name);
        if ((value == null) || (value.getValueType() == JsonValue.ValueType.NULL)) {
            return null;
        }
        if (value.getValueType() == JsonValue.ValueType.TRUE) {
            return Boolean.TRUE;
        }
        if (value.getValueType() == JsonValue.ValueType.FALSE) {
            return Boolean.FALSE;
        }
        throw invalid(field(name) + " must be true or false.");
    }

    private static Long number(final JsonObject json, final String name) {
        final JsonValue value = json.get(name);
        if ((value == null) || (value.getValueType() == JsonValue.ValueType.NULL)) {
            return null;
        }
        if ((value.getValueType() != JsonValue.ValueType.NUMBER) || !((JsonNumber) value).isIntegral()) {
            throw invalid(field(name) + " must be a whole number.");
        }
        try {
            return ((JsonNumber) value).longValueExact();
        } catch (ArithmeticException e) {
            throw invalid(field(name) + " must be a whole number.", e);
        }
    }

    private static EDSException invalid(final String message) {
        return new EDSException(ReturnCode.VERIFICATION_WARNING, message);
    }

    private static EDSException invalid(final String message, final Exception cause) {
        return new EDSException(ReturnCode.VERIFICATION_WARNING, message, cause);
    }
}
