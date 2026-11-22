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

import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.QueryParam;
import net.haugr.eds.api.responses.EDSResponse;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import net.haugr.eds.api.responses.FetchDataResponse;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.stream.ContentDownload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.responses.DownloadTokenResponse;
import net.haugr.eds.core.stream.ContentPolicy;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.core.ShareBean;
import net.haugr.eds.core.model.Settings;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;

/**
 * <p>REST interface for the Data functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Path(Constants.REST_DATA_BASE)
@Tag(name = "Data", description = "Operations for managing data objects within circles.")
public class DataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataService.class);

    private static final String TOKEN = "A download token for this Data Object (POST /data/{dataId}/token), "
            + "for clients which can't send the Authorization header, like the media elements of a browser.";
    private static final String HIDDEN = "List the hidden entries too, whose names start with a dot, like the folder of a feature; default false.";

    private final ShareBean shareBean;
    private final Settings settings;

    public DataService() {
        this(null);
    }

    @Inject
    public DataService(final ShareBean shareBean) {
        this.shareBean = shareBean;
        this.settings = Settings.getInstance();
    }

    /**
     * The REST Add Data Endpoint.
     *
     * @param addDataRequest Add Data Request
     * @return Add Data Response
     */
    @Operation(
            summary = "Add data",
            description = "Adds a Data Object or folder to a Circle; the content is stored encrypted with the Circle's key.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProcessDataResponse.class)))
    @POST
    @Path(Constants.REST_DATA_ADD)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response add(@NotNull final ProcessDataRequest addDataRequest) {
        addDataRequest.setAction(Action.ADD);
        return CommonService.runRequest(settings, () -> shareBean.processData(addDataRequest), Constants.REST_DATA_BASE + Constants.REST_DATA_ADD);
    }

    /**
     * The REST Upload Content Endpoint (PLAN.md, S2).
     *
     * @param dataId        The Data Object
     * @param authorization Authorization header with the Session token
     * @param contentLength Content-Length header
     * @param ifMatch       Optional If-Match header
     * @param content       The raw content
     * @return Response with the new ETag
     */
    @Operation(
            operationId = "uploadContent",
            summary = "Upload content",
            description = "Uploads the content of a Data Object as a raw stream, replacing any existing content.")
    @APIResponse(responseCode = "200", description = "The content was stored; the ETag header has its new ETag.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProcessDataResponse.class)))
    @APIResponse(responseCode = "4xx", description = "A warning; the HTTP status is the returnCode of the JSON body.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProcessDataResponse.class)))
    @PUT
    @Path(Constants.REST_DATA_CONTENT)
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response uploadContent(
            @PathParam(Constants.PATH_DATA_ID) final String dataId,
            @Parameter(hidden = true)
            @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            // Content-Length is a transport header, which HTTP clients set from
            // the body; as a declared parameter, generated clients would try to
            // set it themselves (java.net.http refuses that). Documented in the
            // header table of the operation instead.
            @Parameter(hidden = true)
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final String contentLength,
            @Parameter(description = "Only replace the content if its current ETag matches, or any content exists for '*'.")
            @HeaderParam(HttpHeaders.IF_MATCH) final String ifMatch,
            @RequestBody(description = "The raw content.", required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM,
                            schema = @Schema(type = SchemaType.STRING, format = "binary")))
            final InputStream content) {
        final long length = parseLength(contentLength);
        final Response response = CommonService.runStreamRequest(settings,
                () -> shareBean.uploadContent(CommonService.bearerToken(authorization), dataId, content, length, ifMatch),
                Constants.REST_DATA_BASE + Constants.REST_DATA_CONTENT);
        final ProcessDataResponse entity = (ProcessDataResponse) response.getEntity();

        return (entity.getEtag() != null) ? Response.fromResponse(response).header(HttpHeaders.ETAG, entity.getEtag()).build() : response;
    }

    /**
     * The REST Download Content Endpoint (PLAN.md, S3).
     *
     * @param dataId        The Data Object
     * @param authorization Authorization header with the Session token
     * @param range         Optional Range header
     * @param ifRange       Optional If-Range header
     * @return The content, or a JSON error
     */
    @Operation(
            operationId = "downloadContent",
            summary = "Download content",
            description = "Downloads the content of a Data Object as a raw stream, optionally a byte range.")
    @APIResponse(responseCode = "200", description = "The full content.",
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM,
                    schema = @Schema(type = SchemaType.STRING, format = "binary")))
    @APIResponse(responseCode = "206", description = "The requested range, see Content-Range.",
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM,
                    schema = @Schema(type = SchemaType.STRING, format = "binary")))
    @APIResponse(responseCode = "4xx", description = "A warning; the HTTP status is the returnCode of the JSON body. 416 has Content-Range: bytes */size.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = EDSResponse.class)))
    @GET
    @Path(Constants.REST_DATA_CONTENT)
    @Produces({ MediaType.WILDCARD, CommonService.PRODUCES })
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response downloadContent(
            @PathParam(Constants.PATH_DATA_ID) final String dataId,
            @Parameter(hidden = true)
            @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "A single byte range: bytes=first-last, bytes=first- or bytes=-suffix. Multiple ranges give the full content.")
            @HeaderParam("Range") final String range,
            @Parameter(description = "Only serve the Range if the content still has this ETag; otherwise the full content.")
            @HeaderParam("If-Range") final String ifRange,
            @Parameter(description = TOKEN)
            @QueryParam(Constants.QUERY_TOKEN) final String token) {
        return content(dataId, authorization, token, range, ifRange, true);
    }

    /**
     * The REST Content Headers Endpoint: as the download, without the body.
     *
     * @param dataId        The Data Object
     * @param authorization Authorization header with the Session token
     * @param range         Optional Range header
     * @param ifRange       Optional If-Range header
     * @return The headers of the content
     */
    @Operation(
            operationId = "contentHeaders",
            summary = "Content headers",
            description = "Returns the headers of a content download (size, ETag, type) without the content.")
    @APIResponse(responseCode = "200", description = "The headers of the full content; no body.",
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM,
                    schema = @Schema(type = SchemaType.STRING, format = "binary")))
    @APIResponse(responseCode = "206", description = "The headers of the requested range; no body.",
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM,
                    schema = @Schema(type = SchemaType.STRING, format = "binary")))
    @APIResponse(responseCode = "4xx", description = "A warning; the HTTP status is the returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = EDSResponse.class)))
    @HEAD
    @Path(Constants.REST_DATA_CONTENT)
    @Produces({ MediaType.WILDCARD, CommonService.PRODUCES })
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response contentHeaders(
            @PathParam(Constants.PATH_DATA_ID) final String dataId,
            @Parameter(hidden = true)
            @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "A single byte range, as for the download.")
            @HeaderParam("Range") final String range,
            @Parameter(description = "As for the download.")
            @HeaderParam("If-Range") final String ifRange,
            @Parameter(description = TOKEN)
            @QueryParam(Constants.QUERY_TOKEN) final String token) {
        return content(dataId, authorization, token, range, ifRange, false);
    }

    private Response content(final String dataId, final String authorization, final String token, final String range, final String ifRange, final boolean withBody) {
        final long start = System.nanoTime();
        // The Authorization header wins; the download token is for clients
        // which can't send it (PLAN.md, L5).
        final ContentDownload download = ((authorization == null) && (token != null))
                ? shareBean.prepareContentWithToken(token, dataId, range, ifRange)
                : shareBean.prepareContent(CommonService.bearerToken(authorization), dataId, range, ifRange);
        LOGGER.info("{} {} prepared in {} ms", withBody ? "GET" : "HEAD", Constants.REST_DATA_BASE + Constants.REST_DATA_CONTENT, (System.nanoTime() - start) / 1_000_000);

        if (!download.isOk()) {
            final ReturnCode returnCode = ReturnCode.findReturnCode(download.error().getReturnCode());
            final Response.ResponseBuilder builder = Response.status((returnCode != null) ? returnCode.getHttpCode() : 500)
                    .type(CommonService.PRODUCES)
                    .entity(download.error());
            if (returnCode == ReturnCode.RANGE_NOT_SATISFIABLE) {
                builder.header("Content-Range", download.contentRange());
            }
            return builder.build();
        }

        final Response.ResponseBuilder builder = Response.status(download.status())
                .header("Accept-Ranges", "bytes")
                .header(HttpHeaders.CONTENT_LENGTH, download.contentLength())
                .header(HttpHeaders.CONTENT_TYPE, download.contentType())
                .header("Content-Disposition", download.contentDisposition())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                // Content is untrusted: opened directly, it gets no scripts,
                // no origin, and is never embedded by other sites.
                .header("Content-Security-Policy", ContentPolicy.CONTENT_SECURITY_POLICY)
                .header("Cross-Origin-Resource-Policy", "same-origin")
                .header("Referrer-Policy", "no-referrer")
                .header(HttpHeaders.ETAG, download.etag());
        if (download.isPartial()) {
            builder.header("Content-Range", download.contentRange());
        }
        if (withBody) {
            builder.entity((StreamingOutput) output -> shareBean.streamContent(download, output));
        }

        return builder.build();
    }

    /**
     * Create a download token: {@code POST /data/{dataId}/token}
     * (PLAN.md, L5).
     *
     * @param authorization Authorization header with the Session token
     * @param dataId        The Data Object
     * @return DownloadTokenResponse
     */
    @Operation(operationId = "createDownloadToken", summary = "Create a download token",
            description = "Creates a short-lived token, which reads the content of this one Data Object without an Authorization header: "
                    + "GET or HEAD /data/{dataId}/content?token=... For media elements of a browser, like img and video, which can't send headers. "
                    + "The token holds the Session, sealed with the MasterKey, and ends at its expiry (setting eds.download.token.lifetime.minutes, "
                    + "at most the end of the Session), at logout, or when the MasterKey changes.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DownloadTokenResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @POST
    @Path(Constants.REST_DATA_TOKEN)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response createDownloadToken(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Data Object.", required = true) @PathParam(Constants.PATH_DATA_ID) final String dataId) {
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> shareBean.downloadToken(r.getCredential(), dataId), "POST " + Constants.REST_DATA_BASE + Constants.REST_DATA_TOKEN);
    }

    /**
     * Store a thumbnail: {@code PUT /data/{dataId}/thumbnail} (PLAN.md, L5).
     *
     * @param authorization Authorization header with the Session token
     * @param dataId        The Data Object
     * @param contentType   Media type of the image
     * @param image         The image
     * @return ProcessDataResponse, with the ID of the thumbnail
     */
    @Operation(operationId = "putThumbnail", summary = "Store a thumbnail",
            description = "Stores a thumbnail of the content, made by the client (JPEG, PNG or WebP, at most " + Constants.MAX_THUMBNAIL_SIZE + " bytes), "
                    + "encrypted like any content; an existing thumbnail is replaced. The Metadata of the Data Object has its ID as thumbnailId; "
                    + "download it as content. It goes away with the Data Object, or when the content is replaced or moved; copies don't keep it. "
                    + "The server never reads the image.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessDataResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @PUT
    @Path(Constants.REST_DATA_THUMBNAIL)
    @Consumes({ "image/jpeg", "image/png", "image/webp" })
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response putThumbnail(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Data Object.", required = true) @PathParam(Constants.PATH_DATA_ID) final String dataId,
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.CONTENT_TYPE) final String contentType,
            @RequestBody(description = "The image.", required = true,
                    content = {
                            @Content(mediaType = "image/jpeg", schema = @Schema(type = SchemaType.STRING, format = "binary")),
                            @Content(mediaType = "image/png", schema = @Schema(type = SchemaType.STRING, format = "binary")),
                            @Content(mediaType = "image/webp", schema = @Schema(type = SchemaType.STRING, format = "binary")) })
            final InputStream image) {
        final byte[] bytes = readLimited(image, Constants.MAX_THUMBNAIL_SIZE + 1);
        final String mediaType = (contentType != null) ? contentType.split(";", 2)[0] : null;
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> shareBean.storeThumbnail(r.getCredential(), dataId, mediaType, bytes), "PUT " + Constants.REST_DATA_BASE + Constants.REST_DATA_THUMBNAIL);
    }

    /**
     * Reads at most the given number of bytes; a longer body is cut there,
     * which the size check then refuses.
     */
    private static byte[] readLimited(final InputStream input, final int limit) {
        try {
            return (input != null) ? input.readNBytes(limit) : new byte[0];
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static long parseLength(final String contentLength) {
        try {
            return (contentLength != null) ? Long.parseLong(contentLength.trim()) : -1;
        } catch (NumberFormatException _) {
            return -1;
        }
    }

    /**
     * The REST Copy Data Endpoint.
     *
     * @param copyDataRequest Copy Data Request
     * @return Copy Data Response
     */
    @Operation(
            summary = "Copy data",
            description = "Copies a Data Object to another Circle, re-encrypted with that Circle's key.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProcessDataResponse.class)))
    @POST
    @Path(Constants.REST_DATA_COPY)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response copy(@NotNull final ProcessDataRequest copyDataRequest) {
        copyDataRequest.setAction(Action.COPY);
        return CommonService.runRequest(settings, () -> shareBean.processData(copyDataRequest), Constants.REST_DATA_BASE + Constants.REST_DATA_COPY);
    }

    /**
     * The REST Move Data Endpoint.
     *
     * @param moveDataRequest Move Data Request
     * @return Move Data Response
     */
    @Operation(
            summary = "Move data",
            description = "Moves a Data Object to another Circle; the moved object gets a new ID.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProcessDataResponse.class)))
    @POST
    @Path(Constants.REST_DATA_MOVE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response move(@NotNull final ProcessDataRequest moveDataRequest) {
        moveDataRequest.setAction(Action.MOVE);
        return CommonService.runRequest(settings, () -> shareBean.processData(moveDataRequest), Constants.REST_DATA_BASE + Constants.REST_DATA_MOVE);
    }

    /**
     * The REST Update Data Endpoint.
     *
     * @param updateDataRequest Update Data Request
     * @return Update Data Response
     */
    @Operation(
            summary = "Update data",
            description = "Renames a Data Object, moves it to another folder in the same Circle, and/or replaces its content.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProcessDataResponse.class)))
    @POST
    @Path(Constants.REST_DATA_UPDATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response update(@NotNull final ProcessDataRequest updateDataRequest) {
        updateDataRequest.setAction(Action.UPDATE);
        return CommonService.runRequest(settings, () -> shareBean.processData(updateDataRequest), Constants.REST_DATA_BASE + Constants.REST_DATA_UPDATE);
    }

    /**
     * The REST Delete Data Endpoint.
     *
     * @param deleteDataRequest Delete Data Request
     * @return Delete Data Response
     */
    @Operation(
            summary = "Delete data",
            description = "Deletes a Data Object or an empty folder.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProcessDataResponse.class)))
    @POST
    @Path(Constants.REST_DATA_DELETE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response delete(@NotNull final ProcessDataRequest deleteDataRequest) {
        deleteDataRequest.setAction(Action.DELETE);
        return CommonService.runRequest(settings, () -> shareBean.processData(deleteDataRequest), Constants.REST_DATA_BASE + Constants.REST_DATA_DELETE);
    }

    /**
     * The REST Fetch Data Endpoint.
     *
     * @param fetchDataRequest Fetch Data Request
     * @return Fetch Data Response
     */
    @Operation(
            summary = "Fetch data",
            description = "Returns a Data Object with its decrypted content, or a page of a folder's metadata.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = FetchDataResponse.class)))
    @POST
    @Path(Constants.REST_DATA_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchDataRequest fetchDataRequest) {
        return CommonService.runRequest(settings, () -> shareBean.fetchData(fetchDataRequest), Constants.REST_DATA_BASE + Constants.REST_DATA_FETCH);
    }

    // =========================================================================
    // ID-based resource API (PLAN.md, L1)
    // =========================================================================

    /**
     * List circle data: {@code GET /data} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle; required.
     * @param pageNumber Page number, from 1; default 1.
     * @param pageSize Page size, 1 to 100; default 100.
     * @param includeHidden True to list the hidden entries too.
     * @return FetchDataResponse
     */
    @Operation(operationId = "getData", summary = "List circle data", description = "Lists the root folder of a Circle; as fetchData with a circleId.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FetchDataResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @GET
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response getData(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle; required.", required = true) @QueryParam("circleId") final String circleId,
            @Parameter(description = "Page number, from 1; default 1.") @QueryParam("pageNumber") final Integer pageNumber,
            @Parameter(description = "Page size, 1 to 100; default 100.") @QueryParam("pageSize") final Integer pageSize,
            @Parameter(description = HIDDEN) @QueryParam(Constants.FIELD_INCLUDE_HIDDEN) final Boolean includeHidden) {
        final FetchDataRequest request = new FetchDataRequest();
        request.setCircleId(circleId);
        request.setIncludeHidden(includeHidden);
        if (pageNumber != null) {
            request.setPageNumber(pageNumber);
        }
        if (pageSize != null) {
            request.setPageSize(pageSize);
        }
        return CommonService.runResourceRequest(settings, authorization, request, shareBean::fetchData, "GET /data");
    }

    /**
     * Get data object: {@code GET /data/{dataId}} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param dataId ID of the Data Object.
     * @param pageNumber Page number of a folder listing, from 1; default 1.
     * @param pageSize Page size of a folder listing, 1 to 100; default 100.
     * @param includeHidden True to list the hidden entries too.
     * @return FetchDataResponse
     */
    @Operation(operationId = "getDataById", summary = "Get data object", description = "Returns the metadata of a Data Object without its content, or lists a folder.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FetchDataResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @GET
    @Path("/{dataId}")
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response getDataById(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Data Object.", required = true) @PathParam("dataId") final String dataId,
            @Parameter(description = "Page number of a folder listing, from 1; default 1.") @QueryParam("pageNumber") final Integer pageNumber,
            @Parameter(description = "Page size of a folder listing, 1 to 100; default 100.") @QueryParam("pageSize") final Integer pageSize,
            @Parameter(description = HIDDEN) @QueryParam(Constants.FIELD_INCLUDE_HIDDEN) final Boolean includeHidden) {
        final FetchDataRequest request = new FetchDataRequest();
        request.setDataId(dataId);
        request.setIncludeHidden(includeHidden);
        if (pageNumber != null) {
            request.setPageNumber(pageNumber);
        }
        if (pageSize != null) {
            request.setPageSize(pageSize);
        }
        return CommonService.runResourceRequest(settings, authorization, request, r -> shareBean.fetchData(r, true), "GET /data/{dataId}");
    }

    /**
     * Add data object: {@code POST /data} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param body          The request body
     * @return ProcessDataResponse
     */
    @Operation(operationId = "postData", summary = "Add data object", description = "Adds a Data Object or folder; as addData.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessDataResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @POST
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response postData(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            final ProcessDataRequest body) {
        final ProcessDataRequest request = (body != null) ? body : new ProcessDataRequest();
        request.setAction(Action.ADD);
        return CommonService.runResourceRequest(settings, authorization, request, shareBean::processData, "POST /data");
    }

    /**
     * Update data object: {@code PUT /data/{dataId}} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param dataId ID of the Data Object.
     * @param body          The request body
     * @return ProcessDataResponse
     */
    @Operation(operationId = "putDataById", summary = "Update data object", description = "Renames, moves within the Circle, or replaces the content of a Data Object; as updateData.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessDataResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @PUT
    @Path("/{dataId}")
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response putDataById(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Data Object.", required = true) @PathParam("dataId") final String dataId,
            final ProcessDataRequest body) {
        final ProcessDataRequest request = (body != null) ? body : new ProcessDataRequest();
        request.setAction(Action.UPDATE);
        request.setDataId(dataId);
        return CommonService.runResourceRequest(settings, authorization, request, shareBean::processData, "PUT /data/{dataId}");
    }

    /**
     * Delete data object: {@code DELETE /data/{dataId}} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param dataId ID of the Data Object.
     * @return ProcessDataResponse
     */
    @Operation(operationId = "deleteDataById", summary = "Delete data object", description = "Deletes a Data Object, or an empty folder; as deleteData.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessDataResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @DELETE
    @Path("/{dataId}")
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response deleteDataById(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Data Object.", required = true) @PathParam("dataId") final String dataId) {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setAction(Action.DELETE);
        request.setDataId(dataId);
        return CommonService.runResourceRequest(settings, authorization, request, shareBean::processData, "DELETE /data/{dataId}");
    }

    /**
     * Copy data object: {@code POST /data/{dataId}/copy} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param dataId ID of the Data Object.
     * @param body          The request body
     * @return ProcessDataResponse
     */
    @Operation(operationId = "postDataCopy", summary = "Copy data object", description = "Copies a Data Object to another Circle; as copyData.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessDataResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @POST
    @Path("/{dataId}/copy")
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response postDataCopy(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Data Object.", required = true) @PathParam("dataId") final String dataId,
            final ProcessDataRequest body) {
        final ProcessDataRequest request = (body != null) ? body : new ProcessDataRequest();
        request.setAction(Action.COPY);
        request.setDataId(dataId);
        return CommonService.runResourceRequest(settings, authorization, request, shareBean::processData, "POST /data/{dataId}/copy");
    }

    /**
     * Move data object: {@code POST /data/{dataId}/move} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param dataId ID of the Data Object.
     * @param body          The request body
     * @return ProcessDataResponse
     */
    @Operation(operationId = "postDataMove", summary = "Move data object", description = "Moves a Data Object to another Circle; as moveData.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessDataResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @POST
    @Path("/{dataId}/move")
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response postDataMove(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Data Object.", required = true) @PathParam("dataId") final String dataId,
            final ProcessDataRequest body) {
        final ProcessDataRequest request = (body != null) ? body : new ProcessDataRequest();
        request.setAction(Action.MOVE);
        request.setDataId(dataId);
        return CommonService.runResourceRequest(settings, authorization, request, shareBean::processData, "POST /data/{dataId}/move");
    }
}
