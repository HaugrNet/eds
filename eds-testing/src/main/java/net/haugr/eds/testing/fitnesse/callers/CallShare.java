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
package net.haugr.eds.testing.fitnesse.callers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.haugr.eds.testing.client.ApiClient;
import net.haugr.eds.testing.client.ApiException;
import net.haugr.eds.testing.client.ApiResponse;
import net.haugr.eds.testing.client.api.DataApi;
import net.haugr.eds.testing.client.api.DataTypesApi;
import net.haugr.eds.testing.client.model.EDSResponse;
import net.haugr.eds.testing.client.model.FetchDataRequest;
import net.haugr.eds.testing.client.model.FetchDataResponse;
import net.haugr.eds.testing.client.model.FetchDataTypeRequest;
import net.haugr.eds.testing.client.model.FetchDataTypeResponse;
import net.haugr.eds.testing.client.model.ProcessDataRequest;
import net.haugr.eds.testing.client.model.ProcessDataResponse;
import net.haugr.eds.testing.client.model.ProcessDataTypeRequest;
import net.haugr.eds.testing.client.model.ProcessDataTypeResponse;
import net.haugr.eds.testing.fitnesse.exceptions.StopTestException;

/**
 * <p>Calls of the share operations and the content endpoints, with the
 * client generated from the API contract.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.1
 */
public final class CallShare {

    private static final String UNSUPPORTED = "Unsupported action: ";

    private CallShare() {
        // Private Constructor, this is a utility Class.
    }

    public static ProcessDataTypeResponse processDataType(final String url, final ProcessDataTypeRequest request) {
        final DataTypesApi api = Calls.api(url, DataTypesApi.class, DataTypesApi::new);
        return Calls.run(url, ProcessDataTypeResponse.class, () -> switch (request.getAction()) {
            case PROCESS -> api.processDataType(request);
            case DELETE -> api.deleteDataType(request);
            default -> throw new StopTestException(UNSUPPORTED + request.getAction());
        });
    }

    public static FetchDataTypeResponse fetchDataTypes(final String url, final FetchDataTypeRequest request) {
        return Calls.run(url, FetchDataTypeResponse.class, () -> Calls.api(url, DataTypesApi.class, DataTypesApi::new).fetchDataTypes(request));
    }

    public static ProcessDataResponse processData(final String url, final ProcessDataRequest request) {
        final DataApi api = Calls.api(url, DataApi.class, DataApi::new);
        return Calls.run(url, ProcessDataResponse.class, () -> switch (request.getAction()) {
            case ADD -> api.addData(request);
            case UPDATE -> api.updateData(request);
            case COPY -> api.copyData(request);
            case MOVE -> api.moveData(request);
            case DELETE -> api.deleteData(request);
            default -> throw new StopTestException(UNSUPPORTED + request.getAction());
        });
    }

    public static FetchDataResponse fetchData(final String url, final FetchDataRequest request) {
        return Calls.run(url, FetchDataResponse.class, () -> Calls.api(url, DataApi.class, DataApi::new).fetchData(request));
    }

    // =========================================================================
    // Streaming of content (PLAN.md, Streaming)
    // =========================================================================

    /**
     * <p>Uploads content. The generated operation takes a file as body, so
     * the content is written to a temporary file first.</p>
     *
     * <p>The {@code native} library of openapi-generator (7.26.0) serializes
     * every body that isn't a string as JSON, also a binary body, so it would
     * send the file name as a JSON string. A request interceptor, on a client
     * only for this call, replaces the body with the file content; everything
     * else (path, headers, authorization, response) is the generated code.</p>
     */
    public static ProcessDataResponse uploadContent(final String url, final byte[] session, final String dataId, final byte[] content, final String ifMatch) {
        Path file = null;
        try {
            file = Files.createTempFile("eds-upload-", ".bin");
            Files.write(file, content);
            final Path path = file;
            final ApiClient client = new ApiClient();
            client.updateBaseUri(url);
            client.setRequestInterceptor(builder -> {
                try {
                    builder.method("PUT", HttpRequest.BodyPublishers.ofFile(path));
                } catch (FileNotFoundException e) {
                    throw new StopTestException("The upload file is missing", e);
                }
            });
            return Calls.run(url, ProcessDataResponse.class, () -> new DataApi(client).uploadContent(dataId, path.toFile(), ifMatch, Calls.bearer(session)));
        } catch (IOException e) {
            throw new StopTestException("Cannot prepare the upload", e);
        } finally {
            delete(file);
        }
    }

    public static ContentResult downloadContent(final String url, final byte[] session, final String dataId, final String range, final String ifRange) {
        return content(url, false, session, null, dataId, range, ifRange);
    }

    /**
     * Downloads with a download token in the query instead of the
     * Authorization header, as media elements of a browser do (Rune).
     */
    public static ContentResult downloadContent(final String url, final byte[] session, final String token, final String dataId, final String range, final String ifRange) {
        return content(url, false, session, token, dataId, range, ifRange);
    }

    public static ContentResult contentHeaders(final String url, final byte[] session, final String dataId) {
        return content(url, true, session, null, dataId, null, null);
    }

    /**
     * Stores a thumbnail, as raw image bytes with their media type. The
     * generated client would send the file as JSON and always as image/jpeg,
     * so the body and the Content-Type are set by an interceptor, as for the
     * content upload.
     */
    public static ApiResponse<ProcessDataResponse> putThumbnail(final String url, final byte[] session, final String dataId, final byte[] image, final String mediaType) throws ApiException {
        Path file = null;
        try {
            file = Files.createTempFile("eds-thumbnail-", ".bin");
            Files.write(file, image);
            final Path path = file;
            final ApiClient client = new ApiClient();
            client.updateBaseUri(url);
            client.setRequestInterceptor(builder -> {
                try {
                    builder.method("PUT", HttpRequest.BodyPublishers.ofFile(path));
                    builder.setHeader("Content-Type", (mediaType != null) ? mediaType : "image/jpeg");
                } catch (FileNotFoundException e) {
                    throw new StopTestException("The thumbnail file is missing", e);
                }
            });
            return new DataApi(client).putThumbnailWithHttpInfo(dataId, path.toFile(), Calls.bearer(session));
        } catch (IOException e) {
            throw new StopTestException("Cannot prepare the thumbnail", e);
        } finally {
            delete(file);
        }
    }

    private static ContentResult content(final String url, final boolean head, final byte[] session, final String token, final String dataId, final String range, final String ifRange) {
        final DataApi api = Calls.api(url, DataApi.class, DataApi::new);
        final Map<String, String> headers = Calls.bearer(session);
        try {
            final ApiResponse<File> response = head
                    ? api.contentHeadersWithHttpInfo(dataId, token, ifRange, range, headers)
                    : api.downloadContentWithHttpInfo(dataId, token, ifRange, range, headers);
            final File file = response.getData();
            try {
                final byte[] content = (!head && (file != null)) ? Files.readAllBytes(file.toPath()) : null;
                return result(response.getStatusCode(), response.getHeaders(), content, null);
            } finally {
                delete((file != null) ? file.toPath() : null);
            }
        } catch (ApiException e) {
            // A HEAD response has no body, so its error has no return code.
            final EDSResponse error = head || (e.getResponseBody() == null) || e.getResponseBody().isBlank()
                    ? new EDSResponse().returnCode(0)
                    : Calls.readError(url, EDSResponse.class, e);
            if (e.getCode() == 0) {
                throw new StopTestException("Request failed: " + e.getMessage(), e);
            }
            final HttpHeaders headersOfError = e.getResponseHeaders();
            return result(e.getCode(), (headersOfError != null) ? headersOfError.map() : Map.of(), null, error);
        } catch (IOException e) {
            throw new StopTestException("Cannot read the download", e);
        }
    }

    private static ContentResult result(final int status, final Map<String, List<String>> headers, final byte[] content, final EDSResponse error) {
        final String length = header(headers, "Content-Length");
        return new ContentResult(status, (length != null) ? Long.parseLong(length) : -1,
                header(headers, "Content-Range"), header(headers, "Content-Type"),
                header(headers, "Content-Disposition"), header(headers, "ETag"), content, error);
    }

    private static String header(final Map<String, List<String>> headers, final String name) {
        return headers.entrySet().stream()
                .filter(entry -> name.equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .findFirst().orElse(null);
    }

    private static void delete(final Path file) {
        if (file != null) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                // Best effort, the file is temporary.
            }
        }
    }
}
