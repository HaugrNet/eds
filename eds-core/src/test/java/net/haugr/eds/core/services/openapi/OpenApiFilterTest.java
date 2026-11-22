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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.openapi.api.SmallRyeOpenAPI;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import net.haugr.eds.api.common.Constants;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * <p>Runs the OpenAPI filter as the build does: the classes of eds-core and
 * eds-api are scanned by SmallRye OpenAPI, the scanner of the
 * smallrye-open-api-maven-plugin, with the plugin's settings from
 * eds-core/pom.xml, and {@link EDSOpenApiFilter} is applied. The result must
 * be the contract, {@code eds-api/src/main/resources/openapi.json}.</p>
 *
 * <p>The build runs the filter inside Maven, where coverage isn't measured;
 * here it runs through its own contract, the {@code OASFilter} interface,
 * as SmallRye OpenAPI calls it.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class OpenApiFilterTest {

    private static final Path CONTRACT = Path.of("..", "eds-api", "src", "main", "resources", "openapi.json");
    private static final int MAX_REPORTED = 20;

    @Test
    void testTheFilterGivesTheContract() throws IOException, URISyntaxException {
        final JsonValue contract = OpenApiContractTest.read(CONTRACT);
        final JsonValue generated = generate(contract.asJsonObject().getJsonObject("info"));

        final List<String> differences = new ArrayList<>();
        OpenApiContractTest.compare("", contract, generated, differences);
        assertTrue(differences.isEmpty(), differences.size() + " differences between the filtered scan and the contract:\n  "
                + String.join("\n  ", differences.subList(0, Math.min(MAX_REPORTED, differences.size()))));
    }

    @Test
    void testAnEmptyDocumentGetsTheFrameOfTheContract() {
        final OpenAPI openApi = OASFactory.createOpenAPI();

        new EDSOpenApiFilter().filterOpenAPI(openApi);

        assertNotNull(openApi.getInfo());
        assertNotNull(openApi.getInfo().getDescription());
        assertNotNull(openApi.getComponents().getSchemas().get(EDSOpenApiFilter.RESPONSE_SCHEMA));
    }

    @Test
    void testAnOperationIdIsUsedOnce() {
        final OpenAPI openApi = OASFactory.createOpenAPI().paths(OASFactory.createPaths()
                .addPathItem("/circles/list", OASFactory.createPathItem().GET(OASFactory.createOperation()))
                .addPathItem("/members/list", OASFactory.createPathItem().GET(OASFactory.createOperation())));
        final EDSOpenApiFilter filter = new EDSOpenApiFilter();

        final IllegalStateException cause = assertThrows(IllegalStateException.class, () -> filter.filterOpenAPI(openApi));
        assertEquals("The operationId 'list' is used by both /circles/list and /members/list; set an explicit operationId on one of them.", cause.getMessage());
    }

    @Test
    void testDocumentationOfMissingOperationsIsRefused() {
        // The documentation of the Circles names operations which this
        // document hasn't; an operation without tags isn't documented.
        final OpenAPI openApi = OASFactory.createOpenAPI().paths(OASFactory.createPaths()
                .addPathItem("/circles/unknown", OASFactory.createPathItem().GET(OASFactory.createOperation().addTag("Circles")))
                .addPathItem("/untagged", OASFactory.createPathItem().GET(OASFactory.createOperation())));
        final EDSOpenApiFilter filter = new EDSOpenApiFilter();

        final IllegalStateException cause = assertThrows(IllegalStateException.class, () -> filter.filterOpenAPI(openApi));
        assertTrue(cause.getMessage().startsWith("Documentation for operations that don't exist"), cause.getMessage());
    }

    @Test
    void testOnlyTheHiddenParentIsRemoved() {
        final Schema child = OASFactory.createSchema()
                .addAllOf(OASFactory.createSchema().ref("#/components/schemas/Hidden"))
                .addAllOf(OASFactory.createSchema().ref("#/components/schemas/Base"))
                .addAllOf(OASFactory.createSchema().addType(Schema.SchemaType.OBJECT).description("The own part"));
        final OpenAPI openApi = OASFactory.createOpenAPI().components(OASFactory.createComponents()
                .addSchema("Base", OASFactory.createSchema().addType(Schema.SchemaType.OBJECT))
                .addSchema("Child", child));

        new EDSOpenApiFilter().filterOpenAPI(openApi);

        // The own part stays, and its description is moved to the schema.
        final Schema filtered = openApi.getComponents().getSchemas().get("Child");
        assertEquals(2, filtered.getAllOf().size());
        assertEquals("#/components/schemas/Base", filtered.getAllOf().get(0).getRef());
        assertEquals("The own part", filtered.getDescription());
    }

    @Test
    void testASingleOwnPartIsMergedIntoTheSchema() {
        final Schema described = OASFactory.createSchema().description("The schema itself")
                .addAllOf(OASFactory.createSchema().ref("#/components/schemas/Hidden"))
                .addAllOf(ownPart("The own part"));
        final Schema undescribed = OASFactory.createSchema()
                .addAllOf(OASFactory.createSchema().ref("#/components/schemas/Hidden"))
                .addAllOf(ownPart("The own part"));
        final OpenAPI openApi = OASFactory.createOpenAPI().components(OASFactory.createComponents()
                .addSchema("Described", described)
                .addSchema("Undescribed", undescribed));

        new EDSOpenApiFilter().filterOpenAPI(openApi);

        // A description of the schema itself is kept, else the own part's is taken.
        final Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        for (final String name : List.of("Described", "Undescribed")) {
            assertNull(schemas.get(name).getAllOf(), name);
            assertEquals(List.of(Schema.SchemaType.OBJECT), schemas.get(name).getType(), name);
            assertTrue(schemas.get(name).getProperties().containsKey("name"), name);
        }
        assertEquals("The schema itself", schemas.get("Described").getDescription());
        assertEquals("The own part", schemas.get("Undescribed").getDescription());
    }

    @Test
    void testParentsWhichArentHiddenStay() {
        final String external = "https://example.com/schemas/External.json";
        final Schema withExternal = OASFactory.createSchema()
                .addAllOf(OASFactory.createSchema().ref(external))
                .addAllOf(OASFactory.createSchema().ref("#/components/schemas/Hidden"))
                .addAllOf(ownPart(null));
        final Schema withBase = OASFactory.createSchema()
                .addAllOf(OASFactory.createSchema().ref("#/components/schemas/Hidden"))
                .addAllOf(OASFactory.createSchema().ref("#/components/schemas/Base"));
        final OpenAPI openApi = OASFactory.createOpenAPI().components(OASFactory.createComponents()
                .addSchema("Base", OASFactory.createSchema().addType(Schema.SchemaType.OBJECT))
                .addSchema("WithExternal", withExternal)
                .addSchema("WithBase", withBase));

        new EDSOpenApiFilter().filterOpenAPI(openApi);

        // A reference outside the schemas of the document is no hidden parent;
        // an own part without a description leaves the schema without one.
        final Schema filteredExternal = openApi.getComponents().getSchemas().get("WithExternal");
        assertEquals(2, filteredExternal.getAllOf().size());
        assertEquals(external, filteredExternal.getAllOf().get(0).getRef());
        assertNull(filteredExternal.getDescription());
        // A single parent stays a reference.
        final Schema filteredBase = openApi.getComponents().getSchemas().get("WithBase");
        assertEquals(1, filteredBase.getAllOf().size());
        assertEquals("#/components/schemas/Base", filteredBase.getAllOf().get(0).getRef());
    }

    @Test
    void testAnOwnResponseOfAnOperationIsKept() {
        final Operation operation = OASFactory.createOperation()
                .requestBody(OASFactory.createRequestBody())
                .responses(OASFactory.createAPIResponses().addAPIResponse("400", OASFactory.createAPIResponse().description("Own.")));
        final OpenAPI openApi = OASFactory.createOpenAPI().paths(OASFactory.createPaths()
                .addPathItem("/untagged", OASFactory.createPathItem().POST(operation)));

        new EDSOpenApiFilter().filterOpenAPI(openApi);

        final APIResponses responses = operation.getResponses();
        assertEquals("Own.", responses.getAPIResponse("400").getDescription());
        assertNull(responses.getAPIResponse("400").getRef());
        assertEquals("#/components/responses/" + EDSOpenApiFilter.UNSUPPORTED_MEDIA_TYPE, responses.getAPIResponse("415").getRef());
    }

    @Test
    void testOperationsAreDocumentedFromTheirTagsFile() {
        final Operation original = documented();
        final Operation copy = documented();
        final Operation plain = documented().description("  From the annotation.  ");
        final Operation stricter = documented();
        // Neither a deprecated operation nor one without tags is documented;
        // the documentation of this tag would stop the build.
        final Operation deprecated = OASFactory.createOperation().deprecated(true).addTag("Test Unknown Permission");
        final Operation untagged = OASFactory.createOperation().tags(List.of());
        final OpenAPI openApi = OASFactory.createOpenAPI().paths(OASFactory.createPaths()
                .addPathItem("/test/original", OASFactory.createPathItem().POST(original))
                .addPathItem("/test/copy", OASFactory.createPathItem().GET(copy))
                .addPathItem("/test/plain", OASFactory.createPathItem().POST(plain))
                .addPathItem("/test/stricter", OASFactory.createPathItem().POST(stricter))
                .addPathItem("/test/deprecated", OASFactory.createPathItem().POST(deprecated))
                .addPathItem("/test/untagged", OASFactory.createPathItem().POST(untagged)));

        new EDSOpenApiFilter().filterOpenAPI(openApi);

        assertEquals("FETCH_DATA", original.getExtensions().get(OperationDocumenter.PERMISSION));
        assertTrue(original.getDescription().startsWith("The original operation.\n\n**Base permission:** trust level `READ`"), original.getDescription());
        // A copy takes the permission and the return codes of the original, and points to it.
        assertEquals("FETCH_DATA", copy.getExtensions().get(OperationDocumenter.PERMISSION));
        assertEquals("READ", copy.getExtensions().get(OperationDocumenter.TRUST_LEVEL));
        assertTrue(copy.getDescription().startsWith("The rules and fields of `POST /test/original` (`original`) apply"), copy.getDescription());
        assertTrue(copy.getDescription().contains("`IDENTIFICATION_WARNING`"), copy.getDescription());
        // A copy may have a permission and a text of its own.
        assertEquals("PROCESS_DATA", stricter.getExtensions().get(OperationDocumenter.PERMISSION));
        assertEquals("WRITE", stricter.getExtensions().get(OperationDocumenter.TRUST_LEVEL));
        assertTrue(stricter.getDescription().startsWith("A copy which asks for more.\n\nThe rules and fields of `POST /test/original`"), stricter.getDescription());
        // The description of the annotation comes first.
        assertEquals("SYSOP", plain.getExtensions().get(OperationDocumenter.TRUST_LEVEL));
        assertTrue(plain.getDescription().startsWith("From the annotation.\n\n**Base permission:** the System Administrator only."), plain.getDescription());
        assertNull(deprecated.getDescription());
        assertNull(deprecated.getExtensions());
        assertNull(untagged.getExtensions());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Test Unknown Permission | /test/strange | POST /test/strange: unknown permission 'STRANGE'.",
            "Test Unknown Return Code | /test/odd | POST /test/odd: unknown return code 'ODD_CODE'.",
            "Test Same As Unknown | /test/lost | POST /test/lost: 'same-as: POST /test/nowhere' must name a documented operation without a 'same-as:' of its own."
    })
    void testFaultyDocumentationIsRefused(final String tag, final String path, final String message) {
        final OpenAPI openApi = OASFactory.createOpenAPI().paths(OASFactory.createPaths()
                .addPathItem(path, OASFactory.createPathItem().POST(OASFactory.createOperation().addTag(tag))));
        final EDSOpenApiFilter filter = new EDSOpenApiFilter();

        final IllegalStateException cause = assertThrows(IllegalStateException.class, () -> filter.filterOpenAPI(openApi));
        assertEquals(message, cause.getMessage());
    }

    @Test
    void testASameAsOfASameAsIsRefused() {
        final String tag = "Test Same As Chained";
        final OpenAPI openApi = OASFactory.createOpenAPI().paths(OASFactory.createPaths()
                .addPathItem("/test/first", OASFactory.createPathItem().POST(OASFactory.createOperation().addTag(tag)))
                .addPathItem("/test/second", OASFactory.createPathItem().POST(OASFactory.createOperation().addTag(tag)))
                .addPathItem("/test/third", OASFactory.createPathItem().POST(OASFactory.createOperation().addTag(tag))));
        final EDSOpenApiFilter filter = new EDSOpenApiFilter();

        final IllegalStateException cause = assertThrows(IllegalStateException.class, () -> filter.filterOpenAPI(openApi));
        assertEquals("POST /test/third: 'same-as: POST /test/second' must name a documented operation without a 'same-as:' of its own.", cause.getMessage());
    }

    private static Schema ownPart(final String description) {
        return OASFactory.createSchema().addType(Schema.SchemaType.OBJECT).description(description)
                .addProperty("name", OASFactory.createSchema().addType(Schema.SchemaType.STRING));
    }

    /** An operation of the tag whose documentation is a test resource. */
    private static Operation documented() {
        return OASFactory.createOperation().addTag("Test Documented");
    }

    /**
     * Scans and filters as the plugin does. Title, version, contact and
     * license are settings of the plugin, not work of the filter, so they
     * are taken from the contract.
     */
    private static JsonValue generate(final JsonObject info) throws IOException, URISyntaxException {
        final Map<String, String> settings = new HashMap<>();
        settings.put("mp.openapi.filter", EDSOpenApiFilter.class.getName());
        settings.put("mp.openapi.extensions.smallrye.openapi", "3.0.3");
        settings.put("mp.openapi.extensions.smallrye.application-path.disable", "true");
        settings.put("mp.openapi.extensions.smallrye.auto-inheritance", "BOTH");
        settings.put("mp.openapi.extensions.smallrye.info.title", info.getString("title"));
        settings.put("mp.openapi.extensions.smallrye.info.version", info.getString("version"));
        settings.put("mp.openapi.extensions.smallrye.info.contact.name", info.getJsonObject("contact").getString("name"));
        settings.put("mp.openapi.extensions.smallrye.info.contact.url", info.getJsonObject("contact").getString("url"));
        settings.put("mp.openapi.extensions.smallrye.info.license.name", info.getJsonObject("license").getString("name"));
        settings.put("mp.openapi.extensions.smallrye.info.license.url", info.getJsonObject("license").getString("url"));
        final Config config = new SmallRyeConfigBuilder()
                .withSources(new PropertiesConfigSource(settings, "OpenApiFilterTest", 500))
                .build();

        final SmallRyeOpenAPI openApi = SmallRyeOpenAPI.builder()
                .withConfig(config)
                .withApplicationClassLoader(OpenApiFilterTest.class.getClassLoader())
                .withIndex(index())
                .build();

        try (JsonReader reader = Json.createReader(new StringReader(openApi.toJSON()))) {
            return reader.readValue();
        }
    }

    /** The classes of eds-core and of eds-api, a directory or a JAR each. */
    private static Index index() throws IOException, URISyntaxException {
        final Indexer indexer = new Indexer();
        for (final Class<?> type : List.of(EDSOpenApiFilter.class, Constants.class)) {
            final Path location = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isDirectory(location)) {
                indexDirectory(indexer, location);
            } else {
                indexJar(indexer, location);
            }
        }

        return indexer.complete();
    }

    private static void indexDirectory(final Indexer indexer, final Path directory) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            for (final Path file : files.filter(path -> path.toString().endsWith(".class")).toList()) {
                try (InputStream stream = Files.newInputStream(file)) {
                    indexer.index(stream);
                }
            }
        }
    }

    private static void indexJar(final Indexer indexer, final Path jar) throws IOException {
        try (JarFile file = new JarFile(jar.toFile())) {
            for (final JarEntry entry : file.stream().filter(e -> e.getName().endsWith(".class")).toList()) {
                try (InputStream stream = file.getInputStream(entry)) {
                    indexer.index(stream);
                }
            }
        }
    }
}
