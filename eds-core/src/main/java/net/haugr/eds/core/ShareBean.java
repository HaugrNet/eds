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
package net.haugr.eds.core;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.io.InputStream;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional;
import jakarta.transaction.UserTransaction;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.FetchDataTypeRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.requests.ProcessDataTypeRequest;
import net.haugr.eds.api.responses.FetchDataResponse;
import net.haugr.eds.api.responses.FetchDataTypeResponse;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.api.responses.ProcessDataTypeResponse;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.managers.UploadDataManager;
import net.haugr.eds.core.managers.FetchDataManager;
import net.haugr.eds.core.managers.FetchDataTypeManager;
import net.haugr.eds.core.managers.ProcessDataManager;
import net.haugr.eds.core.managers.ProcessDataTypeManager;
import net.haugr.eds.core.model.Settings;
import jakarta.enterprise.event.Event;
import java.util.LinkedHashSet;
import java.util.Set;
import net.haugr.eds.core.socket.DataChange;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.sql.Connection;
import net.haugr.eds.api.responses.DownloadTokenResponse;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.core.enums.SanityStatus;
import net.haugr.eds.core.managers.DownloadDataManager;
import net.haugr.eds.core.managers.MediaManager;
import net.haugr.eds.core.model.ConnectionSource;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.stream.ContentDownload;
import net.haugr.eds.core.stream.ContentStreamer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>CDI Bean for the Share functionality and final Error handling layer.
 * This is also the layer where transactions are controlled: every business
 * method runs in a transaction (REQUIRED). Even reading requests may write,
 * as the first who comes to a stone without accounts becomes its System
 * Administrator.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@ApplicationScoped
@Transactional
public class ShareBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShareBean.class);

    private final EntityManager entityManager;
    private final Settings settings;
    private final TransactionSynchronizationRegistry transactions;
    private final UserTransaction userTransaction;
    private final ConnectionSource readers;
    // Committed changes of a Circle's files, for the socket; null outside CDI.
    private Event<DataChange> dataChanges = null;

    public ShareBean() {
        this(null, (TransactionSynchronizationRegistry) null, null, null, null);
    }

    @Inject
    public ShareBean(final EntityManager entityManager, final TransactionSynchronizationRegistry transactions, final UserTransaction userTransaction,
                     final ConnectionSource readers, final Event<DataChange> dataChanges) {
        this(entityManager, Settings.getInstance(), transactions, userTransaction, readers);
        this.dataChanges = dataChanges;
    }

    public ShareBean(final EntityManager entityManager, final Settings settings) {
        this(entityManager, settings, null, null);
    }

    public ShareBean(final EntityManager entityManager, final Settings settings, final TransactionSynchronizationRegistry transactions) {
        this(entityManager, settings, transactions, null);
    }

    public ShareBean(final EntityManager entityManager, final Settings settings, final TransactionSynchronizationRegistry transactions, final UserTransaction userTransaction) {
        this(entityManager, settings, transactions, userTransaction, null);
    }

    public ShareBean(final EntityManager entityManager, final Settings settings, final ConnectionSource readers) {
        this(entityManager, settings, null, null, readers);
    }

    public ShareBean(final EntityManager entityManager, final Settings settings, final TransactionSynchronizationRegistry transactions,
                     final UserTransaction userTransaction, final ConnectionSource readers) {
        this.entityManager = entityManager;
        this.settings = settings;
        this.transactions = transactions;
        this.userTransaction = userTransaction;
        this.readers = readers;
    }

    /**
     * <p>Prepares a content download (PLAN.md, S3), in a short
     * transaction: everything which can fail before the response starts.</p>
     *
     * @param sessionToken The Session token from the Authorization header
     * @param dataId       The Data Object
     * @param range        Range header, or null
     * @param ifRange      If-Range header, or null
     * @return The prepared download, or the error
     */
    public ContentDownload prepareContent(final byte[] sessionToken, final String dataId, final String range, final String ifRange) {
        try {
            return new DownloadDataManager(settings, entityManager).prepare(sessionToken, dataId, range, ifRange);
        } catch (EDSException e) {
            Transactions.markRollbackOnly(transactions);
            LOGGER.debug("The request failed.", e);
            return ContentDownload.failed(new EDSResponse(e.getReturnCode(), e.getMessage()));
        }
    }

    /**
     * <p>Prepares a content download with a download token (PLAN.md, L5)
     * instead of the Authorization header; the token holds the Session.</p>
     *
     * @param downloadToken The token from the query
     * @param dataId        The Data Object
     * @param range         Range header, or null
     * @param ifRange       If-Range header, or null
     * @return The prepared download, or the error
     */
    public ContentDownload prepareContentWithToken(final String downloadToken, final String dataId, final String range, final String ifRange) {
        byte[] sessionToken = null;
        try {
            sessionToken = new MediaManager(settings, entityManager).openDownloadToken(downloadToken, dataId);
            return new DownloadDataManager(settings, entityManager).prepare(sessionToken, dataId, range, ifRange);
        } catch (EDSException e) {
            Transactions.markRollbackOnly(transactions);
            LOGGER.debug("The request failed.", e);
            return ContentDownload.failed(new EDSResponse(e.getReturnCode(), e.getMessage()));
        } finally {
            if (sessionToken != null) {
                java.util.Arrays.fill(sessionToken, (byte) 0);
            }
        }
    }

    /**
     * Creates a download token for the content of one Data Object
     * (PLAN.md, L5).
     *
     * @param sessionToken The Session token
     * @param dataId       The Data Object
     * @return The token, or the error
     */
    public DownloadTokenResponse downloadToken(final byte[] sessionToken, final String dataId) {
        try {
            return new MediaManager(settings, entityManager).downloadToken(sessionToken, dataId);
        } catch (EDSException e) {
            Transactions.markRollbackOnly(transactions);
            LOGGER.debug("The request failed.", e);
            return new DownloadTokenResponse(e.getReturnCode(), e.getMessage());
        }
    }

    /**
     * Stores the thumbnail of a Data Object (PLAN.md, L5).
     *
     * @param sessionToken The Session token
     * @param dataId       The Data Object
     * @param mediaType    Media type of the image
     * @param image        The image
     * @return The response with the thumbnail's ID, or the error
     */
    public ProcessDataResponse storeThumbnail(final byte[] sessionToken, final String dataId, final String mediaType, final byte[] image) {
        try {
            final ProcessDataResponse response = new MediaManager(settings, entityManager).storeThumbnail(sessionToken, dataId, mediaType, image);
            // Delivered after the commit (an AFTER_SUCCESS observer).
            dataChanged(response, Set.of(new DataDao(entityManager).findCircleIdOfData(dataId)), dataId);
            return response;
        } catch (EDSException e) {
            Transactions.markRollbackOnly(transactions);
            LOGGER.debug("The request failed.", e);
            return new ProcessDataResponse(e.getReturnCode(), e.getMessage());
        }
    }

    /**
     * <p>Streams a prepared download to the response (PLAN.md, S3), in
     * a read-only transaction of its own with the stream timeout. If a chunk
     * fails its check, the object is marked as failed and an IOException is
     * thrown, which aborts the response: the client never receives
     * unverified data.</p>
     *
     * @param download The prepared download
     * @param output   The response body
     * @throws IOException if the stream failed, or the client went away
     */
    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public void streamContent(final ContentDownload download, final OutputStream output) throws IOException {
        final DataDao dao = new DataDao(entityManager);
        try {
            StreamTransaction.run(userTransaction, settings.getStreamTransactionTimeout(),
                    () -> dao.withConnection(connection -> stream(connection, download, output)));
        } catch (UncheckedIOException e) {
            if ((e.getCause() instanceof ContentStreamer.ContentException content) && content.isDamaged()) {
                LOGGER.error("The content of the Data Object {} failed its check while streaming: {}", download.objectId(), content.getMessage());
                StreamTransaction.run(userTransaction, settings.getStreamTransactionTimeout(), () -> {
                    dao.updateSanityStatus(download.dataId(), SanityStatus.FAILED);
                    return null;
                });
                SanityReport.contentFailed();
            }
            throw e.getCause();
        }
    }

    /** Streams within the transaction; an IOException is carried out of it unchecked, and unwrapped above. */
    private Void stream(final Connection connection, final ContentDownload download, final OutputStream output) {
        try {
            ContentStreamer.stream(connection, download, output, settings.getStreamReadBatchSize());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    /**
     * <p>Uploads the content of a Data Object as a stream (PLAN.md, S2).
     * It runs in its own transaction with the stream timeout
     * ({@code eds.stream.transaction.timeout.seconds}), not the default one,
     * so the container's transaction is suspended. Any failure, including an
     * interrupted upload, rolls the transaction back, so the previous content
     * stays.</p>
     *
     * @param sessionToken The Session token from the Authorization header
     * @param dataId       The Data Object
     * @param content      The raw content
     * @param length       Content-Length, or -1 if not given
     * @param ifMatch      If-Match header, or null
     * @return Response, with the new ETag
     */
    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public ProcessDataResponse uploadContent(final byte[] sessionToken, final String dataId, final InputStream content, final long length, final String ifMatch) {
        final Set<String> circles = new LinkedHashSet<>();
        final ProcessDataResponse result = StreamTransaction.run(userTransaction, settings.getStreamTransactionTimeout(), () -> {
            ProcessDataResponse response;

            try {
                circles.add(new DataDao(entityManager).findCircleIdOfData(dataId));
                final UploadDataManager manager = new UploadDataManager(settings, entityManager, content, length, ifMatch);
                response = manager.perform(UploadDataManager.request(sessionToken, dataId));
            } catch (EDSException e) {
                Transactions.markRollbackOnly(transactions);
                LOGGER.debug("The request failed.", e);
                response = new ProcessDataResponse(e.getReturnCode(), e.getMessage());
            }

            return response;
        });
        dataChanged(result, circles, dataId);

        return result;
    }

    public ProcessDataTypeResponse processDataType(final ProcessDataTypeRequest request) {
        ProcessDataTypeResponse response;

        try {
            final ProcessDataTypeManager manager = new ProcessDataTypeManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // The Exception doesn't reach the transaction interceptor, so
            // anything written before the failure must be rolled back here.
            Transactions.markRollbackOnly(transactions);
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new ProcessDataTypeResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    public FetchDataTypeResponse fetchDataTypes(final FetchDataTypeRequest request) {
        FetchDataTypeResponse response;

        try {
            final FetchDataTypeManager manager = new FetchDataTypeManager(settings, entityManager);
            response = manager.perform(request);
        } catch (EDSException e) {
            // The Exception doesn't reach the transaction interceptor, so
            // anything written before the failure must be rolled back here.
            Transactions.markRollbackOnly(transactions);
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new FetchDataTypeResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    /**
     * <p>Processes a Data Object. It runs in its own transaction with the
     * stream timeout ({@code eds.stream.transaction.timeout.seconds}), as a
     * copy or move of large content re-encrypts all of it (PLAN.md, S4).</p>
     *
     * @param request The request
     * @return Response
     */
    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public ProcessDataResponse processData(final ProcessDataRequest request) {
        final Set<String> circles = new LinkedHashSet<>();
        final ProcessDataResponse result = StreamTransaction.run(userTransaction, settings.getStreamTransactionTimeout(), () -> {
            ProcessDataResponse response;

            try {
                // The Circles are read before the change; a deleted or moved
                // Data Object is gone afterwards.
                circles.addAll(circlesOf(request));
                final ProcessDataManager manager = new ProcessDataManager(settings, entityManager, readers);
                response = manager.perform(request);
            } catch (EDSException e) {
                // The Exception doesn't reach the transaction interceptor, so
                // anything written before the failure must be rolled back here.
                Transactions.markRollbackOnly(transactions);
                // Any Warning or Error thrown by the EDS contain enough
                // information, so it can be dealt with by the requesting
                // System. Logging the error is thus not needed, as all
                // information is provided in the response.
                LOGGER.debug(e.getMessage(), e);
                response = new ProcessDataResponse(e.getReturnCode(), e.getMessage());
            }

            return response;
        });
        dataChanged(result, circles, (result.getDataId() != null) ? result.getDataId() : request.getDataId());

        return result;
    }

    public FetchDataResponse fetchData(final FetchDataRequest request) {
        return fetchData(request, false);
    }

    /**
     * Fetches a Data Object as {@link #fetchData(FetchDataRequest)}, but
     * optionally only its metadata, without reading its content; used by
     * {@code GET /data/{dataId}} (PLAN.md, L1).
     *
     * @param request      The request
     * @param metadataOnly True for the metadata only
     * @return The response
     */
    public FetchDataResponse fetchData(final FetchDataRequest request, final boolean metadataOnly) {
        FetchDataResponse response;

        try {
            final FetchDataManager manager = new FetchDataManager(settings, entityManager, metadataOnly);
            response = manager.perform(request);
        } catch (EDSException e) {
            // The Exception doesn't reach the transaction interceptor, so
            // anything written before the failure must be rolled back here.
            Transactions.markRollbackOnly(transactions);
            // Any Warning or Error thrown by the EDS contain enough
            // information, so it can be dealt with by the requesting
            // System. Logging the error is thus not needed, as all
            // information is provided in the response.
            LOGGER.debug(e.getMessage(), e);
            response = new FetchDataResponse(e.getReturnCode(), e.getMessage());
        }

        return response;
    }

    // =========================================================================
    // Changes of the files of a Circle, for the socket (Rune, Tolk)
    // =========================================================================

    private Set<String> circlesOf(final ProcessDataRequest request) {
        final Set<String> circles = new LinkedHashSet<>();
        if (request.getCircleId() != null) {
            circles.add(request.getCircleId());
        }
        if (request.getDataId() != null) {
            final String circleId = new DataDao(entityManager).findCircleIdOfData(request.getDataId());
            if (circleId != null) {
                circles.add(circleId);
            }
        }
        if (request.getTargetCircleId() != null) {
            circles.add(request.getTargetCircleId());
        }
        return circles;
    }

    private void dataChanged(final EDSResponse response, final Set<String> circles, final String dataId) {
        if ((dataChanges != null) && response.isOk()) {
            for (final String circleId : circles) {
                if (circleId != null) {
                    dataChanges.fire(new DataChange(circleId, dataId));
                }
            }
        }
    }
}
