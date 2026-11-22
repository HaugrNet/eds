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
package net.haugr.eds.core.services.openapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.enums.StandardSetting;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;

/**
 * <p>Applies the per-operation documentation ({@link OperationDocs}) to the
 * specification: the Markdown text is appended to the description, the
 * permission becomes {@code x-eds-permission} and {@code x-eds-trust-level}
 * (derived from {@link Permission}), and the return codes, specific plus
 * common, become {@code x-eds-return-codes} and a table in the
 * description.</p>
 *
 * <p>Unknown permissions or return codes, and documentation for operations
 * that don't exist, stop the build, so the documentation can't silently
 * drift from the code.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class OperationDocumenter {

    static final String PERMISSION = "x-eds-permission";
    static final String TRUST_LEVEL = "x-eds-trust-level";
    static final String NO_AUTHENTICATION = "NONE";

    private static final String SETTINGS_OPERATION = "POST /settings";

    private OperationDocumenter() {
        // Utility class, no instances.
    }

    static void document(final OpenAPI openAPI) {
        if (openAPI.getPaths() == null) {
            return;
        }
        final Map<String, Map<String, OperationDocs.Section>> byTag = new HashMap<>();
        final Map<String, OperationDocs.Section> byKey = new HashMap<>();
        final Map<String, Operation> operations = new LinkedHashMap<>();
        final Set<String> documented = new HashSet<>();
        final Set<String> available = new HashSet<>();

        for (final Map.Entry<String, PathItem> path : openAPI.getPaths().getPathItems().entrySet()) {
            for (final Map.Entry<PathItem.HttpMethod, Operation> entry : path.getValue().getOperations().entrySet()) {
                final Operation operation = entry.getValue();
                if (Boolean.TRUE.equals(operation.getDeprecated()) || (operation.getTags() == null) || operation.getTags().isEmpty()) {
                    continue;
                }
                final String tag = operation.getTags().get(0);
                final String key = entry.getKey().name() + ' ' + path.getKey();
                final Map<String, OperationDocs.Section> sections = byTag.computeIfAbsent(tag, OperationDocs::load);
                sections.keySet().forEach(name -> available.add(tag + '|' + name));
                final OperationDocs.Section section = sections.get(key);
                if (section != null) {
                    byKey.put(key, section);
                    operations.put(key, operation);
                    documented.add(tag + '|' + key);
                }
            }
        }

        available.removeAll(documented);
        if (!available.isEmpty()) {
            throw new IllegalStateException("Documentation for operations that don't exist (or are filed under the wrong tag): " + available);
        }

        for (final Map.Entry<String, Operation> entry : operations.entrySet()) {
            apply(entry.getKey(), entry.getValue(), resolve(entry.getKey(), byKey.get(entry.getKey()), byKey, operations));
        }
    }

    /**
     * Resolves a {@code same-as:} reference: the permission and the return
     * codes are taken from the referenced operation (own return codes are
     * added), and the description points to it.
     */
    private static OperationDocs.Section resolve(final String key, final OperationDocs.Section section,
                                                 final Map<String, OperationDocs.Section> byKey, final Map<String, Operation> operations) {
        if (section.sameAs() == null) {
            return section;
        }
        final OperationDocs.Section original = byKey.get(section.sameAs());
        if ((original == null) || (original.sameAs() != null)) {
            throw new IllegalStateException(key + ": 'same-as: " + section.sameAs() + "' must name a documented operation without a 'same-as:' of its own.");
        }
        final Map<String, String> returnCodes = new LinkedHashMap<>(original.returnCodes());
        returnCodes.putAll(section.returnCodes());
        final String permission = (section.permission() != null) ? section.permission() : original.permission();
        final String reference = "The rules and fields of `" + section.sameAs() + "` (`" + operations.get(section.sameAs()).getOperationId()
                + "`) apply; values from the path replace those of the body.";
        final String body = section.body().isEmpty() ? reference : (section.body() + "\n\n" + reference);

        return new OperationDocs.Section(permission, returnCodes, body, null);
    }

    private static void apply(final String key, final Operation operation, final OperationDocs.Section section) {
        final String trustLevel = trustLevel(key, section.permission());
        final Map<String, String> returnCodes = returnCodes(key, trustLevel, section.returnCodes());

        operation.addExtension(PERMISSION, section.permission());
        operation.addExtension(TRUST_LEVEL, trustLevel);
        operation.addExtension(EDSOpenApiFilter.RETURN_CODES, returnCodeList(returnCodes));

        final StringBuilder description = new StringBuilder();
        if (operation.getDescription() != null) {
            description.append(operation.getDescription().strip()).append("\n\n");
        }
        if (!section.body().isEmpty()) {
            description.append(section.body()).append("\n\n");
        }
        if (SETTINGS_OPERATION.equals(key)) {
            description.append(settingsTable()).append("\n\n");
        }
        description.append("**Base permission:** ").append(permissionText(trustLevel)).append("\n\n");
        description.append("**Return codes:**\n\n").append(returnCodeTable(returnCodes));
        operation.setDescription(description.toString().strip());
    }

    private static String trustLevel(final String key, final String permission) {
        if (NO_AUTHENTICATION.equals(permission)) {
            return NO_AUTHENTICATION;
        }
        try {
            return Permission.valueOf(permission).getTrustLevel().name();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(key + ": unknown permission '" + permission + "'.", e);
        }
    }

    /**
     * SUCCESS, the operation-specific codes, then the common ones for
     * authenticated operations. A specific entry replaces the common text.
     */
    private static Map<String, String> returnCodes(final String key, final String trustLevel, final Map<String, String> specific) {
        final Map<String, String> codes = new LinkedHashMap<>();
        codes.put(ReturnCode.SUCCESS.name(), "The request was completed.");
        for (final Map.Entry<String, String> entry : specific.entrySet()) {
            try {
                ReturnCode.valueOf(entry.getKey());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(key + ": unknown return code '" + entry.getKey() + "'.", e);
            }
            codes.put(entry.getKey(), entry.getValue());
        }
        if (!NO_AUTHENTICATION.equals(trustLevel)) {
            codes.putIfAbsent(ReturnCode.AUTHENTICATION_WARNING.name(), "The account is unknown, the credentials are wrong, or the session is unknown or expired.");
            if (!TrustLevel.ALL.name().equals(trustLevel)) {
                codes.putIfAbsent(ReturnCode.AUTHORIZATION_WARNING.name(), "The requesting Member lacks the required trust level or role.");
            }
            codes.putIfAbsent(ReturnCode.VERIFICATION_WARNING.name(), "The request is invalid: required fields are missing, values are too long or IDs are invalid. returnMessage lists the fields.");
            codes.putIfAbsent(ReturnCode.CRYPTO_ERROR.name(), "A key could not be unlocked, or another cryptographic operation failed.");
            codes.putIfAbsent(ReturnCode.DATABASE_ERROR.name(), "EDS is not ready (e.g. the database is invalid), or a database operation failed.");
            codes.putIfAbsent(ReturnCode.ERROR.name(), "An unexpected error occurred.");
        }
        return codes;
    }

    private static List<Map<String, Object>> returnCodeList(final Map<String, String> returnCodes) {
        final List<Map<String, Object>> list = new ArrayList<>();
        for (final Map.Entry<String, String> entry : returnCodes.entrySet()) {
            final Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", entry.getKey());
            item.put("returnCode", ReturnCode.valueOf(entry.getKey()).getCode());
            item.put("when", entry.getValue());
            list.add(item);
        }
        return list;
    }

    private static String returnCodeTable(final Map<String, String> returnCodes) {
        final StringBuilder table = new StringBuilder("| returnCode | Name | When |\n|---|---|---|\n");
        for (final Map.Entry<String, String> entry : returnCodes.entrySet()) {
            table.append("| ").append(ReturnCode.valueOf(entry.getKey()).getCode())
                    .append(" | `").append(entry.getKey())
                    .append("` | ").append(entry.getValue())
                    .append(" |\n");
        }
        return table.toString().strip();
    }

    private static String permissionText(final String trustLevel) {
        final String inCircle = " in the Circle of the request; without a Circle, in at least one Circle. "
                + "The System Administrator passes this check, but operations using a Circle's key also require being its Trustee.";
        final String text = switch (trustLevel) {
            case NO_AUTHENTICATION -> "none, no authentication needed.";
            case "ALL" -> "any authenticated Member.";
            case "READ" -> "trust level `READ` or higher" + inCircle;
            case "WRITE" -> "trust level `WRITE` or higher" + inCircle;
            case "ADMIN" -> "trust level `ADMIN`" + inCircle;
            case "SYSOP" -> "the System Administrator only.";
            default -> "trust level `" + trustLevel + "`.";
        };
        return NO_AUTHENTICATION.equals(trustLevel) ? text : (text + " The rules above may restrict it further.");
    }

    private static String settingsTable() {
        final StringBuilder table = new StringBuilder("**Standard settings:**\n\n| Key | Default | Meaning |\n|---|---|---|\n");
        for (final StandardSetting setting : StandardSetting.values()) {
            table.append("| `").append(setting.getKey())
                    .append("` | `").append(setting.getValue())
                    .append("` | ").append(SettingDescriptions.of(setting))
                    .append(" |\n");
        }
        return table.toString().strip();
    }
}
