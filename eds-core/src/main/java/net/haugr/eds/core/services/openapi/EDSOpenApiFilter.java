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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.haugr.eds.api.common.ReturnCode;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;

/**
 * <p>Adds the parts of the OpenAPI specification that are derived from the
 * code or kept outside the annotations, so they cannot drift from it:</p>
 * <ul>
 *   <li>the API overview ({@code overview.md} next to this class) as
 *   {@code info.description},</li>
 *   <li>the return-code table (from {@link ReturnCode}) on the
 *   {@code EDSResponse} schema,</li>
 *   <li>the shared responses for requests that are rejected before they reach
 *   EDS (malformed JSON, wrong method or content type),</li>
 *   <li>the per-operation documentation, trust levels and return codes, see
 *   {@link OperationDocumenter}.</li>
 * </ul>
 *
 * <p>The filter runs when the specification is generated at build time, see
 * the {@code smallrye-open-api-maven-plugin} configuration in the eds-core
 * POM. It is not used at runtime.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class EDSOpenApiFilter implements OASFilter {

    /** Extension holding the return codes, on EDSResponse and on operations. */
    public static final String RETURN_CODES = "x-eds-return-codes";
    /** Name of the base response schema. */
    public static final String RESPONSE_SCHEMA = "EDSResponse";

    static final String BAD_REQUEST = "BadRequest";
    static final String METHOD_NOT_ALLOWED = "MethodNotAllowed";
    static final String UNSUPPORTED_MEDIA_TYPE = "UnsupportedMediaType";
    static final String CONSTRAINT_VIOLATION_SCHEMA = "ConstraintViolationReport";

    private static final String OVERVIEW = "overview.md";
    private static final String PROJECT_URL = "https://github.com/HaugrNet/eds";
    private static final String RESPONSES_PREFIX = "#/components/responses/";
    private static final String SCHEMAS_PREFIX = "#/components/schemas/";
    private static final String JSON = "application/json";

    @Override
    public void filterOpenAPI(final OpenAPI openAPI) {
        documentInfo(openAPI);
        final Components components = components(openAPI);
        documentResponseModel(components);
        documentFrameworkErrors(openAPI, components);
        removeHiddenParents(components);
        hoistInheritedDescriptions(components);
        // Operation IDs first, so the documentation can refer to them.
        assignOperationIds(openAPI);
        OperationDocumenter.document(openAPI);
    }

    // =========================================================================
    // Operation IDs
    // =========================================================================

    /**
     * Generated clients name their methods after the operationId. EDS paths
     * end in a unique, readable name (e.g. /circles/createCircle), which is
     * used unless an operationId is set explicitly. Duplicates stop the build.
     */
    private static void assignOperationIds(final OpenAPI openAPI) {
        if (openAPI.getPaths() == null) {
            return;
        }
        final Map<String, String> used = new LinkedHashMap<>();
        for (final Map.Entry<String, PathItem> path : openAPI.getPaths().getPathItems().entrySet()) {
            for (final Operation operation : path.getValue().getOperations().values()) {
                if (operation.getOperationId() == null) {
                    operation.setOperationId(path.getKey().substring(path.getKey().lastIndexOf('/') + 1));
                }
                final String previous = used.put(operation.getOperationId(), path.getKey());
                if (previous != null) {
                    throw new IllegalStateException("The operationId '" + operation.getOperationId() + "' is used by both "
                            + previous + " and " + path.getKey() + "; set an explicit operationId on one of them.");
                }
            }
        }
    }

    // =========================================================================
    // Schemas
    // =========================================================================

    /**
     * A parent class hidden from the schema (e.g. AbstractRequest, which has
     * no fields) still leaves an {@code allOf} reference behind. Such dangling
     * references are removed, and a single remaining part is merged into the
     * schema itself.
     */
    private static void removeHiddenParents(final Components components) {
        if (components.getSchemas() == null) {
            return;
        }
        final Map<String, Schema> schemas = components.getSchemas();
        for (final Schema schema : schemas.values()) {
            if (schema.getAllOf() != null) {
                removeHiddenParent(schema, schemas);
            }
        }
    }

    private static void removeHiddenParent(final Schema schema, final Map<String, Schema> schemas) {
        final List<Schema> allOf = schema.getAllOf();
        final List<Schema> kept = new ArrayList<>();
        for (final Schema part : allOf) {
            final String ref = part.getRef();
            if ((ref == null) || !ref.startsWith(SCHEMAS_PREFIX) || schemas.containsKey(ref.substring(SCHEMAS_PREFIX.length()))) {
                kept.add(part);
            }
        }
        if (kept.size() == allOf.size()) {
            return;
        }
        if ((kept.size() == 1) && (kept.get(0).getRef() == null)) {
            final Schema own = kept.get(0);
            schema.setAllOf(null);
            schema.setType(own.getType());
            schema.setProperties(own.getProperties());
            if (schema.getDescription() == null) {
                schema.setDescription(own.getDescription());
            }
        } else {
            schema.setAllOf(kept);
        }
    }

    /**
     * Subclasses are rendered as {@code allOf: [parent, own part]}, and the
     * class description ends up in the own part. Viewers show the description
     * of the schema itself, so it is moved there.
     */
    private static void hoistInheritedDescriptions(final Components components) {
        if (components.getSchemas() == null) {
            return;
        }
        for (final Schema schema : components.getSchemas().values()) {
            if ((schema.getDescription() == null) && (schema.getAllOf() != null)) {
                for (final Schema part : schema.getAllOf()) {
                    if ((part.getRef() == null) && (part.getDescription() != null)) {
                        schema.setDescription(part.getDescription());
                        part.setDescription(null);
                        break;
                    }
                }
            }
        }
    }

    // =========================================================================
    // Overview
    // =========================================================================

    private static void documentInfo(final OpenAPI openAPI) {
        // The title, version, contact and license come from the plugin
        // configuration in the eds-core POM; only the description is set here.
        if (openAPI.getInfo() == null) {
            openAPI.setInfo(OASFactory.createInfo());
        }
        openAPI.getInfo().setDescription(readOverview());
        openAPI.setExternalDocs(OASFactory.createExternalDocumentation()
                .description("EDS project, with the source code and documentation")
                .url(PROJECT_URL));
    }

    private static String readOverview() {
        try (InputStream stream = EDSOpenApiFilter.class.getResourceAsStream(OVERVIEW)) {
            if (stream == null) {
                throw new IllegalStateException("The OpenAPI overview '" + OVERVIEW + "' is missing.");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // =========================================================================
    // Response model and return codes
    // =========================================================================

    private static void documentResponseModel(final Components components) {
        Schema response = (components.getSchemas() == null) ? null : components.getSchemas().get(RESPONSE_SCHEMA);
        if (response == null) {
            response = OASFactory.createSchema().addType(Schema.SchemaType.OBJECT);
            components.addSchema(RESPONSE_SCHEMA, response);
        }

        response.setDescription("Common part of every EDS response; the outcome of the request is in returnCode and " +
                "returnMessage. The JSON API answers HTTP 200 for every request that reaches EDS; the content " +
                "endpoints and the ID-based resource API answer with the returnCode as HTTP status. See " +
                RETURN_CODES + " for the complete list of return codes.");
        response.addProperty("returnCode", OASFactory.createSchema()
                .addType(Schema.SchemaType.INTEGER)
                .format("int32")
                .description("Outcome of the request: 200 is success, 4xx values are warnings the caller can correct, " +
                        "5xx values are errors in EDS.\n\n" + returnCodeTable())
                .examples(List.of(ReturnCode.SUCCESS.getCode())));
        response.addProperty("returnMessage", OASFactory.createSchema()
                .addType(Schema.SchemaType.STRING)
                .description("Human-readable explanation of the outcome, 'Ok' on success. For invalid requests, it " +
                        "lists the invalid fields and why.")
                .examples(List.of("Ok")));
        response.addProperty("ok", OASFactory.createSchema()
                .addType(Schema.SchemaType.BOOLEAN)
                // Not readOnly: EDSResponse is only ever a response, and generated
                // clients (openapi-generator java) break on a readOnly property
                // in the parent of the responses.
                .description("True if the request was successful, i.e. returnCode is 200.")
                .examples(List.of(true)));
        response.addExtension(RETURN_CODES, returnCodeList());
    }

    /**
     * The return codes as a list of maps, for tools reading the specification.
     *
     * @return One entry per ReturnCode, in declaration order
     */
    static List<Map<String, Object>> returnCodeList() {
        final List<Map<String, Object>> list = new ArrayList<>();
        for (final ReturnCode returnCode : ReturnCode.values()) {
            final Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", returnCode.name());
            entry.put("returnCode", returnCode.getCode());
            entry.put("classification", classification(returnCode));
            entry.put("description", returnCode.getDescription());
            entry.put("when", ReturnCodeCauses.of(returnCode));
            list.add(entry);
        }
        return list;
    }

    private static String returnCodeTable() {
        final StringBuilder table = new StringBuilder("| returnCode | Name | Class | Meaning | When |\n|---|---|---|---|---|\n");
        for (final ReturnCode returnCode : ReturnCode.values()) {
            table.append("| ").append(returnCode.getCode())
                    .append(" | `").append(returnCode.name())
                    .append("` | ").append(classification(returnCode))
                    .append(" | ").append(returnCode.getDescription())
                    .append(" | ").append(ReturnCodeCauses.of(returnCode))
                    .append(" |\n");
        }
        return table.toString().strip();
    }

    private static String classification(final ReturnCode returnCode) {
        return switch (returnCode.getClassification()) {
            case CLASS_INFO -> "success";
            case CLASS_WARNING -> "warning";
            case CLASS_ERROR -> "error";
        };
    }

    // =========================================================================
    // Requests rejected before they reach EDS
    // =========================================================================

    private static void documentFrameworkErrors(final OpenAPI openAPI, final Components components) {
        components.addSchema(CONSTRAINT_VIOLATION_SCHEMA, constraintViolationSchema());
        components.addResponse(BAD_REQUEST, OASFactory.createAPIResponse()
                .description("The request was rejected before it reached EDS: the body is not valid JSON, a field has " +
                        "an invalid value (e.g. an unknown enum value or invalid Base64), or the body is missing. " +
                        "A missing body is reported as a constraint violation (JSON body); the other cases have " +
                        "an empty body.")
                .content(OASFactory.createContent().addMediaType(JSON, OASFactory.createMediaType()
                        .schema(OASFactory.createSchema().ref(SCHEMAS_PREFIX + CONSTRAINT_VIOLATION_SCHEMA)))));
        components.addResponse(METHOD_NOT_ALLOWED, OASFactory.createAPIResponse()
                .description("The HTTP method is not supported for this path; EDS operations use POST. Empty body."));
        components.addResponse(UNSUPPORTED_MEDIA_TYPE, OASFactory.createAPIResponse()
                .description("The request content type is not application/json. Empty body."));

        if (openAPI.getPaths() != null) {
            for (final PathItem pathItem : openAPI.getPaths().getPathItems().values()) {
                for (final Operation operation : pathItem.getOperations().values()) {
                    addFrameworkErrors(operation);
                }
            }
        }
    }

    private static void addFrameworkErrors(final Operation operation) {
        if (operation.getResponses() == null) {
            operation.setResponses(OASFactory.createAPIResponses());
        }
        // Only operations taking a body can fail on its content or type.
        if (operation.getRequestBody() != null) {
            addReference(operation, "400", BAD_REQUEST);
            addReference(operation, "415", UNSUPPORTED_MEDIA_TYPE);
        }
        addReference(operation, "405", METHOD_NOT_ALLOWED);
    }

    private static void addReference(final Operation operation, final String status, final String response) {
        if (operation.getResponses().getAPIResponse(status) == null) {
            final APIResponse reference = OASFactory.createAPIResponse().ref(RESPONSES_PREFIX + response);
            operation.getResponses().addAPIResponse(status, reference);
        }
    }

    private static Schema constraintViolationSchema() {
        final Schema violation = OASFactory.createSchema()
                .addType(Schema.SchemaType.OBJECT)
                .description("One violated constraint.")
                .addProperty("field", OASFactory.createSchema().addType(Schema.SchemaType.STRING)
                        .description("The violating element, e.g. 'authenticated.arg0' for a missing request body.")
                        .examples(List.of("authenticated.arg0")))
                .addProperty("message", OASFactory.createSchema().addType(Schema.SchemaType.STRING)
                        .description("Why the value is invalid.")
                        .examples(List.of("must not be null")));

        return OASFactory.createSchema()
                .addType(Schema.SchemaType.OBJECT)
                .description("Report for requests rejected by Bean Validation before they reach EDS, e.g. a missing " +
                        "request body.")
                .addProperty("title", OASFactory.createSchema().addType(Schema.SchemaType.STRING)
                        .description("Always 'Constraint Violation'.")
                        .examples(List.of("Constraint Violation")))
                .addProperty("status", OASFactory.createSchema().addType(Schema.SchemaType.INTEGER).format("int32")
                        .description("The HTTP status, 400.")
                        .examples(List.of(400)))
                .addProperty("violations", OASFactory.createSchema().addType(Schema.SchemaType.ARRAY)
                        .description("The violated constraints.")
                        .items(violation));
    }

    private static Components components(final OpenAPI openAPI) {
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createComponents());
        }
        return openAPI.getComponents();
    }
}
