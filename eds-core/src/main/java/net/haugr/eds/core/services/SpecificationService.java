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
package net.haugr.eds.core.services;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import net.haugr.eds.api.common.Constants;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * <p>Serves the API contract (PLAN.md, L2): the OpenAPI specification
 * {@code openapi.json} from the eds-api jar, byte for byte as checked in. The
 * build ensures that it describes the running code (OpenApiContractTest), so
 * nothing is derived at runtime.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Path(Constants.REST_SPECIFICATION)
@Tag(name = "Specification", description = "The OpenAPI specification of this API.")
public class SpecificationService {

    private static final String CONTRACT = "/openapi.json";
    private static final byte[] SPECIFICATION = load();

    /**
     * Default Constructor.
     */
    public SpecificationService() {
        // Generating JavaDoc requires an explicit Constructor, SonarQube
        // requires explicit comment in empty methods, hence this comment
        // for the default, empty, constructor.
    }

    /**
     * Returns the OpenAPI specification.
     *
     * @return The specification as JSON
     */
    @Operation(
            operationId = "specification",
            summary = "Get the OpenAPI specification",
            description = "Returns this OpenAPI specification (the API contract), as JSON.")
    @APIResponse(responseCode = "200", description = "The OpenAPI specification.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.OBJECT, description = "OpenAPI 3.0 document.")))
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response specification() {
        return Response.ok(SPECIFICATION, MediaType.APPLICATION_JSON_TYPE)
                .header("Cache-Control", "no-cache")
                .header("X-Content-Type-Options", "nosniff")
                .build();
    }

    private static byte[] load() {
        try (InputStream stream = Constants.class.getResourceAsStream(CONTRACT)) {
            if (stream == null) {
                throw new IllegalStateException("The API contract " + CONTRACT + " is missing from the eds-api jar.");
            }
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
