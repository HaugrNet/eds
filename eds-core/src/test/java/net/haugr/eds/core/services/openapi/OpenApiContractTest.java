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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * <p>The API contract (PLAN.md, L1): the checked-in specification
 * {@code eds-api/src/main/resources/openapi.json} must be exactly the one
 * generated from the code. Any API change therefore shows up as a change of
 * the contract file, which is reviewed with the code; the clients are
 * generated from that file.</p>
 *
 * <p>After a deliberate API change, update the contract with
 * {@code mvn install -Deds.contract.update=true} and commit it.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class OpenApiContractTest {

    private static final Path GENERATED = Path.of("target", "generated", "openapi.json");
    private static final Path CONTRACT = Path.of("..", "eds-api", "src", "main", "resources", "openapi.json");
    private static final String UPDATE = "eds.contract.update";
    private static final int MAX_REPORTED = 20;

    @Test
    void testGeneratedSpecificationIsTheContract() throws IOException {
        assertTrue(Files.exists(GENERATED), "The OpenAPI specification " + GENERATED + " was not generated.");

        if (Boolean.getBoolean(UPDATE)) {
            Files.copy(GENERATED, CONTRACT, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        assertTrue(Files.exists(CONTRACT), "The contract " + CONTRACT + " is missing; create it with -D" + UPDATE + "=true.");
        final List<String> differences = new ArrayList<>();
        compare("", read(CONTRACT), read(GENERATED), differences);

        if (!differences.isEmpty()) {
            fail("The API differs from the contract " + CONTRACT.normalize() + " in " + differences.size()
                    + " place(s). If the change is intended, update the contract with -D" + UPDATE + "=true and commit it:\n  "
                    + String.join("\n  ", differences.subList(0, Math.min(MAX_REPORTED, differences.size()))));
        }
    }

    static JsonValue read(final Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             JsonReader jsonReader = Json.createReader(reader)) {
            return jsonReader.readValue();
        }
    }

    /**
     * Collects the JSON Pointers where the two values differ.
     */
    static void compare(final String pointer, final JsonValue contract, final JsonValue generated, final List<String> differences) {
        if (contract.equals(generated)) {
            return;
        }

        if ((contract instanceof JsonObject expected) && (generated instanceof JsonObject actual)) {
            final Set<String> keys = new TreeSet<>(expected.keySet());
            keys.addAll(actual.keySet());
            for (final String key : keys) {
                final String child = pointer + '/' + key.replace("~", "~0").replace("/", "~1");
                if (!actual.containsKey(key)) {
                    differences.add(child + ": removed from the API");
                } else if (!expected.containsKey(key)) {
                    differences.add(child + ": added to the API");
                } else {
                    compare(child, expected.get(key), actual.get(key), differences);
                }
            }
        } else if ((contract instanceof JsonArray expected) && (generated instanceof JsonArray actual) && (expected.size() == actual.size())) {
            for (int i = 0; i < expected.size(); i++) {
                compare(pointer + '/' + i, expected.get(i), actual.get(i), differences);
            }
        } else {
            differences.add(pointer + ": changed");
        }
    }
}
