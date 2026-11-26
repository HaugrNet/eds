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
package net.haugr.eds.core.features;

import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.io.StringReader;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.core.JsonFactories;
import net.haugr.eds.core.exceptions.VerificationException;

/**
 * <p>The declaration of a feature (FEATURES.md): its name, the kinds of its
 * entries, and for each kind the rules, picked from the fixed set which the
 * stone knows. What a feature is for, and what its entries hold, is known to
 * the clients only.</p>
 *
 * <p>A declaration is a JSON object:</p>
 *
 * <pre>
 * {
 *   "feature": "calendar",
 *   "version": 1,
 *   "signals": 2000,
 *   "kinds": {
 *     "event": {
 *       "add": "WRITE",
 *       "change": ["adders"],
 *       "remove": ["author", "administrators"],
 *       "close": [],
 *       "keep": "all",
 *       "about": null,
 *       "size": 65536
 *     }
 *   }
 * }
 * </pre>
 *
 * <ul>
 *   <li><b>feature</b>: the name, a lower case letter followed by lower case
 *   letters, digits or hyphens, at most 32 characters. The folder of the
 *   feature in a Circle is a dot and this name.</li>
 *   <li><b>version</b>: a number from 1, for the clients.</li>
 *   <li><b>signals</b>: optional; the least milliseconds between two
 *   signals of one connection. Without it, the feature has no signals.</li>
 *   <li><b>kinds</b>: at least one kind, named like a feature.</li>
 *   <li><b>add</b>: the trust level needed to add an entry: READ, WRITE or
 *   ADMIN.</li>
 *   <li><b>change</b>, <b>remove</b>, <b>close</b>: who may change,
 *   remove or close (and open again) an entry: any of "author", "adders"
 *   (everyone who may add) and "administrators"; empty or absent for
 *   nobody.</li>
 *   <li><b>keep</b>: "all"; "member" (one per Member, a new one replaces
 *   the old); "member-about" (one per Member about each entry);
 *   "age:&lt;seconds&gt;" (entries up to an age); "count:&lt;number&gt;"
 *   (the newest up to a number). Absent means "all".</li>
 *   <li><b>about</b>: optional; the kind of entry which an entry of this
 *   kind is about. Such an entry goes when the one it is about is
 *   removed.</li>
 *   <li><b>size</b>: the largest content of an entry, in bytes.</li>
 * </ul>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class FeatureDeclaration {

    /** Who may do something with an entry. */
    public enum Who {
        /** The Member who wrote the entry. */
        AUTHOR,
        /** Everyone who may add an entry of the kind. */
        ADDERS,
        /** The administrators of the Circle. */
        ADMINISTRATORS
    }

    /** What is kept of a kind. */
    public enum Keep {
        /** Everything. */
        ALL,
        /** One entry per Member; a new one replaces the old. */
        MEMBER,
        /** One entry per Member about each entry. */
        MEMBER_ABOUT,
        /** Entries up to an age, in seconds. */
        AGE,
        /** The newest entries up to a number. */
        COUNT
    }

    /**
     * The rules of one kind of entry.
     *
     * @param name      Name of the kind
     * @param add       Trust level needed to add
     * @param change    Who may change an entry
     * @param remove    Who may remove an entry
     * @param close     Who may close an entry, and open it again
     * @param keep      What is kept
     * @param keepValue Seconds for {@link Keep#AGE}, number for {@link Keep#COUNT}, otherwise 0
     * @param about     Kind of the entry which this kind is about, or null
     * @param size      Largest content in bytes
     */
    public record Kind(String name, TrustLevel add, Set<Who> change, Set<Who> remove, Set<Who> close, Keep keep, long keepValue, String about, int size) {
    }

    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z][a-z0-9-]{0," + (Constants.MAX_FEATURE_NAME_LENGTH - 1) + '}');
    private static final String FIELD_SIGNALS = "signals";
    private static final String FIELD_ABOUT = "about";
    private static final String THE_DECLARATION = "The declaration";
    private static final Set<String> TOP = Set.of("feature", "version", FIELD_SIGNALS, "kinds");
    private static final Set<String> RULES = Set.of("add", "change", "remove", "close", "keep", FIELD_ABOUT, "size");
    /** A feature has at most this many kinds. */
    public static final int MAX_KINDS = 32;

    private final String name;
    private final int version;
    private final long signals;
    private final Map<String, Kind> kinds;

    private FeatureDeclaration(final String name, final int version, final long signals, final Map<String, Kind> kinds) {
        this.name = name;
        this.version = version;
        this.signals = signals;
        this.kinds = Collections.unmodifiableMap(kinds);
    }

    /**
     * The name of the feature.
     *
     * @return Name of the feature
     */
    public String name() {
        return name;
    }

    /**
     * The version of the declaration.
     *
     * @return Version, from 1
     */
    public int version() {
        return version;
    }

    /**
     * True if the feature has signals.
     *
     * @return True if signals are allowed
     */
    public boolean hasSignals() {
        return signals >= 0;
    }

    /**
     * The least time between two signals of one connection.
     *
     * @return Milliseconds, or -1 without signals
     */
    public long signals() {
        return signals;
    }

    /**
     * The kinds of entries, by their names.
     *
     * @return The kinds
     */
    public Map<String, Kind> kinds() {
        return kinds;
    }

    /**
     * The rules of a kind.
     *
     * @param kind Name of the kind
     * @return The kind, or null if the feature doesn't declare it
     */
    public Kind kind(final String kind) {
        return (kind != null) ? kinds.get(kind) : null;
    }

    /**
     * The name of the folder of a feature in the root folder of a Circle.
     *
     * @param feature Name of the feature
     * @return A dot and the name
     */
    public static String folder(final String feature) {
        return '.' + feature;
    }

    /**
     * True if the text may name a feature or a kind.
     *
     * @param text Text to check
     * @return True for a valid name
     */
    public static boolean isName(final String text) {
        return (text != null) && NAME_PATTERN.matcher(text).matches();
    }

    /**
     * Reads and checks a declaration.
     *
     * @param text      The declaration, a JSON object as text
     * @param maxSize   The largest size a kind may allow, in bytes
     * @return The declaration
     * @throws VerificationException if it isn't a valid declaration
     */
    public static FeatureDeclaration parse(final String text, final int maxSize) {
        final JsonObject json = readObject(text);
        onlyKnown(json, TOP, THE_DECLARATION);

        final String name = string(json, "feature", THE_DECLARATION);
        if (!isName(name)) {
            throw new VerificationException("The name of a feature is a lower case letter, then lower case letters, digits or hyphens, at most " + Constants.MAX_FEATURE_NAME_LENGTH + " characters.");
        }
        final long version = number(json, "version", THE_DECLARATION, 1, Integer.MAX_VALUE);
        final long signals = json.containsKey(FIELD_SIGNALS) && (json.get(FIELD_SIGNALS).getValueType() != JsonValue.ValueType.NULL)
                ? number(json, FIELD_SIGNALS, THE_DECLARATION, 0, 3_600_000)
                : -1;
        final Map<String, Kind> kinds = kinds(json.get("kinds"), maxSize);
        checkAbout(kinds);

        return new FeatureDeclaration(name, (int) version, signals, kinds);
    }

    private static JsonObject readObject(final String text) {
        if ((text == null) || text.isBlank()) {
            throw new VerificationException("The declaration is missing.");
        }
        if (text.length() > Constants.MAX_DECLARATION_LENGTH) {
            throw new VerificationException("The declaration must be at most " + Constants.MAX_DECLARATION_LENGTH + " characters.");
        }

        try (JsonReader reader = JsonFactories.READERS.createReader(new StringReader(text))) {
            return reader.readObject();
        } catch (JsonException | IllegalStateException e) {
            throw invalid("The declaration must be a JSON object.", e);
        }
    }

    private static Map<String, Kind> kinds(final JsonValue declared, final int maxSize) {
        if ((declared == null) || (declared.getValueType() != JsonValue.ValueType.OBJECT) || declared.asJsonObject().isEmpty()) {
            throw new VerificationException("The declaration needs at least one kind in 'kinds'.");
        }
        if (declared.asJsonObject().size() > MAX_KINDS) {
            throw new VerificationException("A feature has at most " + MAX_KINDS + " kinds.");
        }
        final Map<String, Kind> kinds = new LinkedHashMap<>();
        for (final Map.Entry<String, JsonValue> entry : declared.asJsonObject().entrySet()) {
            if (!isName(entry.getKey())) {
                throw new VerificationException("The kind '" + entry.getKey() + "' has no valid name: a lower case letter, then lower case letters, digits or hyphens.");
            }
            if (entry.getValue().getValueType() != JsonValue.ValueType.OBJECT) {
                throw new VerificationException("The kind '" + entry.getKey() + "' must be a JSON object.");
            }
            kinds.put(entry.getKey(), kind(entry.getKey(), entry.getValue().asJsonObject(), maxSize));
        }

        return kinds;
    }

    private static void checkAbout(final Map<String, Kind> kinds) {
        for (final Kind kind : kinds.values()) {
            if ((kind.about() != null) && !kinds.containsKey(kind.about())) {
                throw new VerificationException("The kind '" + kind.name() + "' is about the kind '" + kind.about() + "', which the feature doesn't declare.");
            }
            if ((kind.keep() == Keep.MEMBER_ABOUT) && (kind.about() == null)) {
                throw new VerificationException("The kind '" + kind.name() + "' keeps one per Member about each entry, so it must say what it is about.");
            }
        }
    }

    private static Kind kind(final String name, final JsonObject json, final int maxSize) {
        final String what = "The kind '" + name + '\'';
        onlyKnown(json, RULES, what);

        final TrustLevel add = add(json, what);
        final KeepRule keep = present(json, "keep") ? keep(string(json, "keep", what), what) : new KeepRule(Keep.ALL, 0);
        final String about = about(json, what);
        final long size = number(json, "size", what, 0, maxSize);

        return new Kind(name, add, who(json, "change", what), who(json, "remove", what), who(json, "close", what), keep.keep(), keep.value(), about, (int) size);
    }

    private static String about(final JsonObject json, final String what) {
        final String about = present(json, FIELD_ABOUT) ? string(json, FIELD_ABOUT, what) : null;
        if ((about != null) && !isName(about)) {
            throw new VerificationException(what + " names no valid kind in 'about'.");
        }

        return about;
    }

    private static TrustLevel add(final JsonObject json, final String what) {
        final TrustLevel add;
        try {
            add = TrustLevel.valueOf(string(json, "add", what));
        } catch (IllegalArgumentException _) {
            throw new VerificationException(what + " needs 'add' to be READ, WRITE or ADMIN.");
        }
        if (!EnumSet.of(TrustLevel.READ, TrustLevel.WRITE, TrustLevel.ADMIN).contains(add)) {
            throw new VerificationException(what + " needs 'add' to be READ, WRITE or ADMIN.");
        }

        return add;
    }

    /** The keep rule of a kind, with its number for age and count. */
    private record KeepRule(Keep keep, long value) {
    }

    private static KeepRule keep(final String text, final String what) {
        final String invalid = what + " needs 'keep' to be all, member, member-about, age:<seconds> or count:<number>.";
        final int colon = text.indexOf(':');
        final String rule = (colon < 0) ? text : text.substring(0, colon);
        final Keep keep = switch (rule) {
            case "all" -> Keep.ALL;
            case "member" -> Keep.MEMBER;
            case "member-about" -> Keep.MEMBER_ABOUT;
            case "age" -> Keep.AGE;
            case "count" -> Keep.COUNT;
            default -> throw new VerificationException(invalid);
        };
        final boolean valued = (keep == Keep.AGE) || (keep == Keep.COUNT);
        if (valued != (colon >= 0)) {
            throw new VerificationException(invalid);
        }
        if (!valued) {
            return new KeepRule(keep, 0);
        }

        long value;
        try {
            value = Long.parseLong(text.substring(colon + 1));
        } catch (NumberFormatException _) {
            value = 0;
        }
        if (value < 1) {
            throw new VerificationException(what + " needs a number from 1 in 'keep'.");
        }

        return new KeepRule(keep, value);
    }

    private static Set<Who> who(final JsonObject json, final String field, final String what) {
        final Set<Who> who = EnumSet.noneOf(Who.class);

        if (present(json, field)) {
            final JsonValue value = json.get(field);
            if (value.getValueType() != JsonValue.ValueType.ARRAY) {
                throw new VerificationException(what + " needs '" + field + "' to be a list of author, adders and administrators.");
            }
            final JsonArray array = value.asJsonArray();
            for (final JsonValue item : array) {
                try {
                    who.add(Who.valueOf(((JsonString) item).getString().toUpperCase(Locale.ROOT)));
                } catch (ClassCastException | IllegalArgumentException e) {
                    throw invalid(what + " needs '" + field + "' to be a list of author, adders and administrators.", e);
                }
            }
        }

        return Collections.unmodifiableSet(who);
    }

    private static boolean present(final JsonObject json, final String field) {
        return json.containsKey(field) && (json.get(field).getValueType() != JsonValue.ValueType.NULL);
    }

    private static void onlyKnown(final JsonObject json, final Set<String> known, final String what) {
        for (final String field : json.keySet()) {
            if (!known.contains(field)) {
                throw new VerificationException(what + " has the unknown field '" + field + "'.");
            }
        }
    }

    private static String string(final JsonObject json, final String field, final String what) {
        final JsonValue value = json.get(field);
        if ((value == null) || (value.getValueType() != JsonValue.ValueType.STRING)) {
            throw new VerificationException(what + " needs '" + field + "' as text.");
        }

        return ((JsonString) value).getString();
    }

    private static long number(final JsonObject json, final String field, final String what, final long min, final long max) {
        final JsonValue value = json.get(field);
        if ((value == null) || (value.getValueType() != JsonValue.ValueType.NUMBER) || !((JsonNumber) value).isIntegral()) {
            throw new VerificationException(what + " needs '" + field + "' as a whole number.");
        }
        final long number;
        try {
            number = ((JsonNumber) value).longValueExact();
        } catch (ArithmeticException e) {
            throw invalid(what + " needs '" + field + "' from " + min + " to " + max + '.', e);
        }
        if ((number < min) || (number > max)) {
            throw new VerificationException(what + " needs '" + field + "' from " + min + " to " + max + '.');
        }

        return number;
    }

    /** The refusal of an invalid declaration, which keeps what was found wrong as its cause. */
    private static VerificationException invalid(final String message, final Exception cause) {
        final VerificationException exception = new VerificationException(message);
        exception.initCause(cause);

        return exception;
    }
}
