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

import net.haugr.eds.core.features.EntryQuery;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.SocketFrameType;
import net.haugr.eds.api.dtos.Entry;
import net.haugr.eds.api.dtos.SocketFrame;
import net.haugr.eds.api.requests.EntryRequest;
import net.haugr.eds.api.responses.CircleFeatureResponse;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.api.responses.EntryResponse;
import net.haugr.eds.core.FeatureBean;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.managers.FeatureManager;
import net.haugr.eds.core.features.FeatureDeclaration;
import net.haugr.eds.core.features.FeatureEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>The connections of the socket (FEATURES.md). A connection authenticates
 * with a Session token (AUTH), joins Circles, and then works with the
 * entries of their features: it adds, changes, removes and closes entries,
 * lists and counts them, and sends signals. What changed in a Circle is
 * told to every connection which joined it: an entry added, changed or
 * removed, by whichever connection or request, a feature switched, and the
 * files of the Circle.</p>
 *
 * <p>The hub also tells who is present in a Circle (FEATURES.md, "Presence,
 * and signals to one Member"): a Member with at least one connection which
 * joined it and is shown. Who joins gets the list, and the others are told
 * when a Member's first such connection comes and when the last one goes.
 * Nothing of it is stored.</p>
 *
 * <p>The hub knows nothing about what a feature is for. Every request is
 * authenticated and checked by the {@link FeatureBean}, like a request of the
 * resource API; the hub only passes it on, and passes on what happened once
 * the transaction succeeded.</p>
 *
 * <p>Access ends promptly: when a change of Members, Circles, Trustees or
 * the MasterKey is committed ({@link AccessChange}), the connections it may
 * touch are checked again, and when a Session has expired its connections
 * are closed. Until a connection is checked, nothing of the Circle is sent
 * to it: each Circle has an access version, raised right after the commit,
 * and a connection checked before it is checked again first (ticket 18).
 * What waits for a connection is limited, both ways; a connection over a
 * limit, whose client doesn't read or floods, is closed.</p>
 *
 * <p>Before AUTH a connection may do little: at most
 * {@link #MAX_WAITING_FOR_AUTH} wait for it at once, its first frame must
 * be a valid AUTH within {@link #AUTH_TIMEOUT_SECONDS} seconds, or it is
 * closed, and less may wait to be handled. After AUTH, its signals have a
 * budget of characters, and its changes of entries a budget of frames
 * (tickets 18 and 22).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ApplicationScoped
public class SocketHub {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketHub.class);

    /** The most frames which may wait for a connection, each way. */
    static final int MAX_PENDING_FRAMES = 4096;
    /** The most characters which may wait to be handled. */
    static final long MAX_PENDING_RECEIVED = 4L * 1024 * 1024;
    /** The most characters which may wait to be sent: some pages of entries. */
    static final long MAX_PENDING_SENT = 32L * 1024 * 1024;
    /** A connection which is closed for a reason gets this long to read why. */
    static final long CLOSE_TIMEOUT_MILLIS = 10_000;
    /**
     * The name of the Executor which handles the frames, if the runtime
     * gives one: the worker pool of its REST requests, so that frames and
     * requests share the CPU alike. Without one, each frame gets a virtual
     * thread.
     */
    public static final String WORK = "eds-socket-work";
    /** The first frame, AUTH, must come within this time. */
    static final long AUTH_TIMEOUT_SECONDS = 10;
    /** The most connections which may wait for their AUTH at once; one more is closed at once. */
    static final int MAX_WAITING_FOR_AUTH = 256;
    /** The most characters which may wait to be handled before AUTH: the AUTH frame is small. */
    static final long MAX_PENDING_RECEIVED_BEFORE_AUTH = 64L * 1024;
    /** The characters of signals a connection may send at once, and how many come back a second. */
    static final long SIGNAL_BURST = 256L * 1024;
    static final long SIGNAL_RATE = 32L * 1024;
    /** The changes of entries a connection may make at once, and how many come back a second. */
    static final long ENTRY_BURST = 100;
    static final long ENTRY_RATE = 20;

    private static final String NOT_AUTHENTICATED = "Not authenticated";
    private static final String TOO_MANY_WAITING = "Too many connections wait for AUTH";
    private static final String SESSION_EXPIRED = "The Session has expired.";

    private final FeatureBean features;
    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    /** Guards who is shown in which Circle, so that coming and going are told once and in order. */
    private final Object presence = new Object();
    private final ScheduledExecutorService timer;
    private final ExecutorService checker;
    private final AtomicBoolean checkPending = new AtomicBoolean(false);
    /** The connections which wait for their AUTH. */
    private final AtomicInteger waiting = new AtomicInteger(0);
    /** The access version of everyone: raised by a change which may touch every connection. */
    private final AtomicLong everyoneVersion = new AtomicLong(0);
    /** The access version of each Circle: raised by a change of the access to it. */
    private final Map<String, AtomicLong> circleVersions = new ConcurrentHashMap<>();
    private final long closeTimeoutMillis;
    private final Clock clock;
    private final long authTimeoutMillis;
    /** Where the frames are handled, each connection's in order. */
    private final Executor frameWork;
    /** The virtual threads of the frames, if no Executor was given. */
    private final ExecutorService ownWork;

    public SocketHub() {
        this.features = null;
        this.timer = null;
        this.checker = null;
        this.closeTimeoutMillis = CLOSE_TIMEOUT_MILLIS;
        this.clock = Clock.systemUTC();
        this.authTimeoutMillis = AUTH_TIMEOUT_SECONDS * 1000;
        this.frameWork = null;
        this.ownWork = null;
    }

    @Inject
    public SocketHub(final FeatureBean features, @Named(WORK) final Instance<Executor> work) {
        this(features, CLOSE_TIMEOUT_MILLIS, Clock.systemUTC(), AUTH_TIMEOUT_SECONDS * 1000, work.isResolvable() ? work.get() : null);
    }

    SocketHub(final FeatureBean features, final long closeTimeoutMillis) {
        this(features, closeTimeoutMillis, Clock.systemUTC(), AUTH_TIMEOUT_SECONDS * 1000, null);
    }

    /**
     * For the tests: the clock tells when a Session has expired, and a
     * connection must authenticate within the given time.
     */
    SocketHub(final FeatureBean features, final long closeTimeoutMillis, final Clock clock, final long authTimeoutMillis) {
        this(features, closeTimeoutMillis, clock, authTimeoutMillis, null);
    }

    private SocketHub(final FeatureBean features, final long closeTimeoutMillis, final Clock clock, final long authTimeoutMillis, final Executor work) {
        this.features = features;
        this.closeTimeoutMillis = closeTimeoutMillis;
        this.clock = clock;
        this.authTimeoutMillis = authTimeoutMillis;
        this.ownWork = (work == null) ? Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("eds-socket-", 0).factory()) : null;
        this.frameWork = (work == null) ? ownWork : work;
        this.timer = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("eds-socket-timer").daemon().factory());
        this.checker = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("eds-socket-check").factory());
        timer.scheduleAtFixedRate(this::closeExpired, 1, 1, TimeUnit.MINUTES);
    }

    @PreDestroy
    void shutdown() {
        if (timer != null) {
            timer.shutdownNow();
            checker.shutdownNow();
        }
        if (ownWork != null) {
            ownWork.shutdown();
        }
        for (final Client client : clients.values()) {
            client.dispose();
        }
        clients.clear();
    }

    /**
     * The number of open connections.
     *
     * @return Open connections
     */
    public int connections() {
        return clients.size();
    }

    /**
     * The Circles whose access version is kept, for the tests: only a change
     * of the access makes one.
     *
     * @return Circles with a version
     */
    int versionsKept() {
        return circleVersions.size();
    }

    // =========================================================================
    // Connections
    // =========================================================================

    public void open(final SocketConnection connection) {
        if (waiting.incrementAndGet() > MAX_WAITING_FOR_AUTH) {
            waiting.decrementAndGet();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Socket connection {} refused: {}.", connection.id(), TOO_MANY_WAITING);
            }
            Thread.ofVirtual().name("eds-socket-refuse-" + connection.id()).start(() -> connection.close(SocketConnection.TRY_AGAIN_LATER, TOO_MANY_WAITING));
            return;
        }
        final Client client = new Client(connection, this::overloaded, frameWork, clock);
        clients.put(connection.id(), client);
        timer.schedule(() -> {
            if ((client.identity() == null) && clients.containsKey(connection.id())) {
                drop(client, ReturnCode.AUTHENTICATION_WARNING, "No AUTH frame within " + (authTimeoutMillis / 1000) + " seconds.");
            }
        }, authTimeoutMillis, TimeUnit.MILLISECONDS);
    }

    public void receive(final String connectionId, final String text) {
        final Client client = clients.get(connectionId);
        if (client != null) {
            client.receive(text, () -> handle(client, text));
        }
    }

    public void closed(final String connectionId) {
        final Client client = clients.remove(connectionId);
        if (client != null) {
            stopWaiting(client);
            gone(client);
            client.dispose();
        }
    }

    // =========================================================================
    // What happened, once it is committed
    // =========================================================================

    /**
     * Something happened in a feature of a Circle. Every connection which
     * joined the Circle is told, also the one which asked for it: it gets
     * its answer as well, and tells the two apart by the entry.
     *
     * @param event What happened
     */
    public void featureChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) final FeatureEvent event) {
        final SocketFrame frame = new SocketFrame(switch (event.change()) {
            case ADDED -> SocketFrameType.ADDED;
            case CHANGED -> SocketFrameType.CHANGED;
            case REMOVED -> SocketFrameType.REMOVED;
            case FEATURE -> SocketFrameType.FEATURE;
        });
        frame.setCircleId(event.circleId());
        frame.setFeature(event.feature());
        frame.setEntry(announced(event.entry()));
        announce(event.circleId(), SocketCodec.write(frame), null);
    }

    /**
     * The files of a Circle changed.
     *
     * @param change Which Circle and Data Object
     */
    void dataChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) final DataChange change) {
        if (change.circleId() == null) {
            return;
        }
        final SocketFrame frame = new SocketFrame(SocketFrameType.FILES);
        frame.setCircleId(change.circleId());
        frame.setDataId(change.dataId());
        announce(change.circleId(), SocketCodec.write(frame), null);
    }

    /**
     * The rotation of a Circle's secret started or ended.
     *
     * @param change Which Circle, and whether it ended
     */
    void rotationChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) final RotationChange change) {
        final SocketFrame frame = new SocketFrame(change.ended() ? SocketFrameType.ROTATED : SocketFrameType.ROTATING);
        frame.setCircleId(change.circleId());
        announce(change.circleId(), SocketCodec.write(frame), null);
    }

    /**
     * Members, Circles, Trustees or the MasterKey changed. The access version
     * is raised at once, on the thread of the request, so nothing more of
     * the Circle is sent to a connection before it was checked again. The
     * checks themselves run off that thread: of the connections which joined
     * the Circle, or of every connection; a check of every connection which
     * is waiting already covers the later changes too.
     *
     * @param change What changed, committed
     */
    void accessChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) final AccessChange change) {
        if (!change.touchesEveryone()) {
            circleVersions.computeIfAbsent(change.circleId(), id -> new AtomicLong(0)).incrementAndGet();
            for (final Client client : clients.values()) {
                if ((client.identity() != null) && client.circles.contains(change.circleId())) {
                    client.run(() -> check(client));
                }
            }
            return;
        }
        everyoneVersion.incrementAndGet();
        if ((checker != null) && checkPending.compareAndSet(false, true)) {
            try {
                checker.execute(() -> {
                    checkPending.set(false);
                    checkAll();
                });
            } catch (RejectedExecutionException e) {
                // The checker is gone, as the stone stops.
                LOGGER.debug("The access check is not run.", e);
                checkPending.set(false);
            }
        }
    }

    private void announce(final String circleId, final String json, final Client except) {
        announce(circleId, json, except, null);
    }

    /**
     * Tells the connections which joined a Circle; only those of one Member,
     * if one is named. A connection whose access to the Circle may have
     * changed since it was last checked is checked first, in the order of
     * its frames: what it is told then waits behind the check, and so does
     * what comes after, until all of it is sent.
     */
    private void announce(final String circleId, final String json, final Client except, final String memberId) {
        for (final Client client : clients.values()) {
            if (!client.equals(except) && client.circles.contains(circleId) && ((memberId == null) || isOf(client, memberId))) {
                if (expired(client)) {
                    drop(client, ReturnCode.AUTHENTICATION_WARNING, SESSION_EXPIRED);
                } else if ((client.held.get() > 0) || stale(client, circleId)) {
                    sendChecked(client, circleId, json);
                } else {
                    client.sendText(json);
                }
            }
        }
    }

    /**
     * Sends behind a check of the access, in the order of the connection's
     * frames. If the check fails (the database can't be read), what was to
     * be sent is dropped, and the next announcement checks again.
     */
    private void sendChecked(final Client client, final String circleId, final String json) {
        client.held.incrementAndGet();
        client.run(() -> {
            try {
                if (stale(client, circleId)) {
                    check(client);
                }
                // Not if the check failed, so it is still stale: nothing is
                // sent which wasn't checked.
                if (client.circles.contains(circleId) && !stale(client, circleId)) {
                    client.sendText(json);
                }
            } finally {
                client.held.decrementAndGet();
            }
        });
    }

    // =========================================================================
    // Access versions
    // =========================================================================

    /**
     * The access version of a Circle; 0 for a Circle whose access never
     * changed, which has no entry: only a change of the access makes one, so
     * a client can't grow the map with the IDs it joins.
     */
    private long circleVersion(final String circleId) {
        final AtomicLong version = circleVersions.get(circleId);
        return (version != null) ? version.get() : 0;
    }

    /** The access versions now, of everyone and of a Circle; read before a check of the access. */
    private Checked versions(final String circleId) {
        return new Checked(everyoneVersion.get(), circleVersion(circleId));
    }

    /** Whether the access of a connection to a Circle may have changed since it was checked. */
    private boolean stale(final Client client, final String circleId) {
        final Checked checked = client.checked.get(circleId);
        return (checked == null) || (checked.everyone() < everyoneVersion.get()) || (checked.circle() < circleVersion(circleId));
    }

    /** The access versions a connection was checked against, for one Circle. */
    private record Checked(long everyone, long circle) {
    }

    /** An entry as it is announced: without a long content, which is fetched. */
    private static Entry announced(final Entry entry) {
        if ((entry != null) && (entry.getContent() != null) && (entry.getContent().length() > Constants.MAX_ANNOUNCED_CONTENT)) {
            entry.setContent(null);
        }
        return entry;
    }

    // =========================================================================
    // Frames
    // =========================================================================

    private void handle(final Client client, final String text) {
        if (!client.equals(clients.get(client.connection.id()))) {
            // Dropped meanwhile: the frames which still wait are not handled.
            return;
        }
        if (client.identity() == null) {
            handleFirst(client, text);
            return;
        }
        String ref = null;
        try {
            final SocketFrame frame = SocketCodec.parse(text);
            ref = frame.getRef();
            if (frame.getType() == SocketFrameType.AUTH) {
                authenticate(client, frame);
            } else if (expired(client)) {
                drop(client, ReturnCode.AUTHENTICATION_WARNING, SESSION_EXPIRED);
            } else {
                dispatch(client, frame);
            }
        } catch (EDSException e) {
            if ((e.getReturnCode().getClassification() == ReturnCode.Classification.CLASS_ERROR) && (e.getReturnCode() != ReturnCode.SERVICE_BUSY)) {
                // An error of the stone, not of the frame (as the REST endpoints
                // log it; a busy stone isn't one).
                LOGGER.warn("Socket frame failed with {}: {}", e.getReturnCode(), e.getMessage());
            } else {
                LOGGER.debug("Socket frame failed: {}", e.getMessage());
            }
            if (e.getReturnCode() == ReturnCode.AUTHENTICATION_WARNING) {
                drop(client, e.getReturnCode(), e.getMessage());
            } else {
                client.send(error(ref, e.getReturnCode(), e.getMessage()));
            }
        } catch (RuntimeException e) {
            LOGGER.error("Socket frame failed: {}", e.getMessage(), e);
            client.send(error(ref, ReturnCode.ERROR, e.getMessage()));
        }
    }

    /**
     * The first frame of a connection must be a valid AUTH: anything else,
     * or an AUTH which fails, closes the connection (tickets 18 and 22).
     * Nothing of it is logged above DEBUG: a locked stone fails every AUTH,
     * and an unknown client could fill the log.
     */
    private void handleFirst(final Client client, final String text) {
        // The answer names the frame it answers, so a client learns why
        // before the connection is closed.
        String ref = null;
        try {
            final SocketFrame frame = SocketCodec.parse(text);
            ref = frame.getRef();
            if (frame.getType() != SocketFrameType.AUTH) {
                drop(client, ref, ReturnCode.AUTHENTICATION_WARNING, "The first frame must be AUTH.");
                return;
            }
            authenticate(client, frame);
        } catch (EDSException e) {
            LOGGER.debug("The first socket frame failed: {}", e.getMessage());
            drop(client, ref, e.getReturnCode(), e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.debug("The first socket frame failed: {}", e.getMessage(), e);
            drop(client, ref, ReturnCode.ERROR, e.getMessage());
        }
    }

    /**
     * How a connection is closed for a failure: as a broken rule (1008), which
     * clients take for an ended Session, unless the stone failed (a locked
     * stone, the database), which a client may try again later (1013).
     */
    private static int closeCode(final ReturnCode returnCode) {
        return (returnCode.getClassification() == ReturnCode.Classification.CLASS_ERROR)
                ? SocketConnection.TRY_AGAIN_LATER : SocketConnection.POLICY_VIOLATION;
    }

    /** Runs a frame of an authenticated connection; a failure is answered by {@link #handle(Client, String)}. */
    private void dispatch(final Client client, final SocketFrame frame) {
        switch (frame.getType()) {
            case JOIN -> join(client, frame);
            case LEAVE -> leave(client, frame);
            case ADD -> add(client, frame);
            case CHANGE -> change(client, frame);
            case REMOVE -> remove(client, frame);
            case CLOSE -> close(client, frame);
            case GET -> get(client, frame);
            case LIST -> list(client, frame);
            case COUNT -> count(client, frame);
            case SIGNAL -> signal(client, frame);
            default -> throw new EDSException(ReturnCode.VERIFICATION_WARNING, "The frame type " + frame.getType() + " is sent by EDS, not by clients.");
        }
    }

    private void authenticate(final Client client, final SocketFrame frame) {
        if (client.identity() != null) {
            throw new EDSException(ReturnCode.ILLEGAL_ACTION, "The connection is already authenticated.");
        }
        final byte[] token = decode(frame.getToken());
        final FeatureManager.Identity identity = features.identify(token);
        client.token.set(token);
        client.identity.set(identity);
        stopWaiting(client);

        final SocketFrame ready = reply(SocketFrameType.READY, frame);
        ready.setMemberId(identity.memberId());
        ready.setAccountName(identity.accountName());
        client.send(ready);
    }

    private void join(final Client client, final SocketFrame frame) {
        final String circleId = checkId(frame.getCircleId(), Constants.FIELD_CIRCLE_ID);
        // The versions before the check: a change committed meanwhile is
        // checked again.
        final Checked versions = versions(circleId);
        // Reading the features of the Circle needs READ in it, which is what
        // joining it needs.
        final CircleFeatureResponse found = checked(features.circleFeatures(client.token(), circleId));

        final SocketFrame joined = reply(SocketFrameType.JOINED, frame);
        joined.setCircleId(circleId);
        joined.setFeatures(found.getFeatures());
        // The list and what is told afterwards are in order: nobody comes or
        // goes between the two.
        synchronized (presence) {
            // A connection which closed meanwhile joins nothing: it would stay present for good.
            if (!client.equals(clients.get(client.connection.id()))) {
                return;
            }
            client.checked.put(circleId, versions);
            client.circles.add(circleId);
            show(client, circleId, !Boolean.TRUE.equals(frame.getUnseen()));
            joined.setPresent(present(circleId));
            client.send(joined);
        }
    }

    private void leave(final Client client, final SocketFrame frame) {
        final String circleId = checkId(frame.getCircleId(), Constants.FIELD_CIRCLE_ID);
        synchronized (presence) {
            show(client, circleId, false);
            client.circles.remove(circleId);
            client.checked.remove(circleId);
        }

        final SocketFrame left = reply(SocketFrameType.LEFT, frame);
        left.setCircleId(circleId);
        client.send(left);
    }

    private void add(final Client client, final SocketFrame frame) {
        final String circleId = joined(client, frame);
        spend(client);
        final EntryRequest body = new EntryRequest();
        body.setKind(frame.getKind());
        body.setAbout((frame.getAbout() != null) ? checkId(frame.getAbout(), Constants.FIELD_ABOUT) : null);
        body.setName(frame.getName());
        body.setContent(frame.getContent());
        done(client, frame, circleId, features.addEntry(client.token(), circleId, feature(frame), body));
    }

    private void change(final Client client, final SocketFrame frame) {
        final String circleId = joined(client, frame);
        spend(client);
        final EntryRequest body = new EntryRequest();
        body.setName(frame.getName());
        body.setContent(frame.getContent());
        done(client, frame, circleId, features.changeEntry(client.token(), circleId, feature(frame), checkId(frame.getEntryId(), Constants.FIELD_ENTRY_ID), body));
    }

    private void remove(final Client client, final SocketFrame frame) {
        final String circleId = joined(client, frame);
        spend(client);
        done(client, frame, circleId, features.removeEntry(client.token(), circleId, feature(frame), checkId(frame.getEntryId(), Constants.FIELD_ENTRY_ID)));
    }

    private void close(final Client client, final SocketFrame frame) {
        final String circleId = joined(client, frame);
        spend(client);
        done(client, frame, circleId, features.closeEntry(client.token(), circleId, feature(frame), checkId(frame.getEntryId(), Constants.FIELD_ENTRY_ID), frame.getClosed()));
    }

    private void get(final Client client, final SocketFrame frame) {
        final String circleId = joined(client, frame);
        done(client, frame, circleId, features.entry(client.token(), circleId, feature(frame), checkId(frame.getEntryId(), Constants.FIELD_ENTRY_ID)));
    }

    private void list(final Client client, final SocketFrame frame) {
        final String circleId = joined(client, frame);
        final String about = (frame.getAbout() != null) ? checkId(frame.getAbout(), Constants.FIELD_ABOUT) : null;
        final EntryQuery query = EntryQuery.all().kind(frame.getKind()).about(about).namePrefix(frame.getName())
                .after(frame.getAfter()).before(frame.getBefore()).oldestFirst(frame.getOldestFirst())
                .limit(frame.getLimit()).content(!Boolean.TRUE.equals(frame.getBare()));
        final EntryResponse found = checked(features.entries(client.token(), circleId, feature(frame), query));

        final SocketFrame page = reply(SocketFrameType.ENTRIES, frame);
        page.setCircleId(circleId);
        page.setFeature(frame.getFeature());
        page.setEntries(found.getEntries());
        client.send(page);
    }

    private void count(final Client client, final SocketFrame frame) {
        final String circleId = joined(client, frame);
        final EntryResponse found = checked(features.count(client.token(), circleId, feature(frame), frame.getKind(), frame.getAfter()));

        final SocketFrame counted = reply(SocketFrameType.COUNTED, frame);
        counted.setCircleId(circleId);
        counted.setFeature(frame.getFeature());
        counted.setCount(found.getCount());
        client.send(counted);
    }

    /**
     * A signal is passed on to the others which joined the Circle, or to
     * the connections of the one Member it names, and neither stored nor
     * answered. One which comes too soon after the connection's last one
     * for the feature and Circle is dropped silently, and so is one over the
     * connection's budget for signals.
     */
    private void signal(final Client client, final SocketFrame frame) {
        // The checks run in this order: joined, feature, to, the signal, and
        // then the access to the feature.
        final String circleId = joined(client, frame);
        final String feature = feature(frame);
        final String to = (frame.getTo() != null) ? checkId(frame.getTo(), Constants.FIELD_TO) : null;
        final String signal = signalOf(frame);
        final long interval = features.signal(client.token(), circleId, feature);
        final String key = circleId + '|' + feature;
        if (due(client, key, interval) && client.signals.take(signal.length())) {
            client.signalled.put(key, clock.millis());
            final SocketFrame passed = new SocketFrame(SocketFrameType.SIGNAL);
            passed.setCircleId(circleId);
            passed.setFeature(feature);
            passed.setMemberId(client.identity().memberId());
            passed.setAccountName(client.identity().accountName());
            passed.setSignal(signal);
            passed.setTo(to);
            announce(circleId, SocketCodec.write(passed), client, to);
        }
    }

    /** The signal of a frame, empty without one; refused if it is too long. */
    private static String signalOf(final SocketFrame frame) {
        final String signal = (frame.getSignal() != null) ? frame.getSignal() : "";
        if (signal.length() > Constants.MAX_SIGNAL_LENGTH) {
            throw new EDSException(ReturnCode.VERIFICATION_WARNING, "A signal has at most " + Constants.MAX_SIGNAL_LENGTH + " characters.");
        }

        return signal;
    }

    /** A change of an entry takes one of the connection's budget; without one left, the stone is busy for it. */
    private static void spend(final Client client) {
        if (!client.entries.take(1)) {
            throw new EDSException(ReturnCode.SERVICE_BUSY, "Too many changes of entries at once; try again shortly.");
        }
    }

    /**
     * Whether a signal of the connection for the feature and Circle (the
     * key) may go now, as the interval has passed since its last one. The
     * time of the last one is noted when it goes, so a signal over the
     * budget doesn't use up the interval.
     */
    private boolean due(final Client client, final String key, final long interval) {
        final Long last = client.signalled.get(key);
        return (last == null) || ((clock.millis() - last) >= interval);
    }

    // =========================================================================
    // Who is present
    // =========================================================================

    /**
     * Sets whether a connection is shown in a Circle, and tells the others
     * who joined it when the Member came or went by that: with their first
     * shown connection, or their last.
     */
    private void show(final Client client, final String circleId, final boolean shown) {
        final FeatureManager.Identity identity = client.identity();
        if (identity == null) {
            return;
        }
        synchronized (presence) {
            if (shown ? !client.shown.add(circleId) : !client.shown.remove(circleId)) {
                return;
            }
            if (!shownElsewhere(identity.memberId(), circleId, client)) {
                final SocketFrame frame = new SocketFrame(shown ? SocketFrameType.PRESENT : SocketFrameType.ABSENT);
                frame.setCircleId(circleId);
                frame.setMemberId(identity.memberId());
                frame.setAccountName(identity.accountName());
                announce(circleId, SocketCodec.write(frame), client);
            }
        }
    }

    /** A connection went: its Member is no longer present where this was their last shown connection. */
    private void gone(final Client client) {
        synchronized (presence) {
            for (final String circleId : List.copyOf(client.shown)) {
                show(client, circleId, false);
            }
            client.circles.clear();
        }
    }

    private boolean shownElsewhere(final String memberId, final String circleId, final Client except) {
        for (final Client other : clients.values()) {
            if (!other.equals(except) && other.shown.contains(circleId) && isOf(other, memberId)) {
                return true;
            }
        }
        return false;
    }

    /** The Members who are present in a Circle, by their IDs, in order. */
    private List<String> present(final String circleId) {
        final Set<String> members = new TreeSet<>();
        for (final Client client : clients.values()) {
            final FeatureManager.Identity identity = client.identity();
            if ((identity != null) && client.shown.contains(circleId)) {
                members.add(identity.memberId());
            }
        }
        return List.copyOf(members);
    }

    private static boolean isOf(final Client client, final String memberId) {
        final FeatureManager.Identity identity = client.identity();
        return (identity != null) && memberId.equals(identity.memberId());
    }

    /** The answer to a request about entries; the others are told by the event of the change. */
    private static void done(final Client client, final SocketFrame frame, final String circleId, final EntryResponse response) {
        checked(response);
        final SocketFrame done = reply(SocketFrameType.DONE, frame);
        done.setCircleId(circleId);
        done.setFeature(frame.getFeature());
        done.setEntries(response.getEntries());
        done.setReturnMessage(response.getReturnMessage());
        client.send(done);
    }

    // =========================================================================
    // Access checks
    // =========================================================================

    private void checkAll() {
        for (final Client client : clients.values()) {
            if (client.identity() != null) {
                client.run(() -> check(client));
            }
        }
    }

    private void check(final Client client) {
        final byte[] token = client.token.get();
        if (token == null) {
            return;
        }
        try {
            // The versions before the check: a change committed meanwhile is
            // checked again.
            final Map<String, Checked> versions = new HashMap<>();
            for (final String circleId : client.circles) {
                versions.put(circleId, versions(circleId));
            }
            final Set<String> readable = features.readableCircles(token.clone());
            for (final String circleId : client.circles) {
                if (readable.contains(circleId)) {
                    final Checked checked = versions.get(circleId);
                    if (checked != null) {
                        client.checked.put(circleId, checked);
                    }
                } else {
                    synchronized (presence) {
                        show(client, circleId, false);
                        client.circles.remove(circleId);
                        client.checked.remove(circleId);
                    }
                    final SocketFrame left = new SocketFrame(SocketFrameType.LEFT);
                    left.setCircleId(circleId);
                    left.setReturnCode(ReturnCode.AUTHORIZATION_WARNING.getCode());
                    left.setReturnMessage("The access to the Circle has ended.");
                    client.send(left);
                }
            }
        } catch (EDSException e) {
            if ((e.getReturnCode() == ReturnCode.AUTHENTICATION_WARNING) || (e.getReturnCode() == ReturnCode.AUTHORIZATION_WARNING)) {
                drop(client, ReturnCode.AUTHENTICATION_WARNING, e.getMessage());
            } else {
                LOGGER.warn("The socket connection {} could not be checked: {}", client.connection.id(), e.getMessage());
            }
        } catch (RuntimeException e) {
            LOGGER.warn("The socket connection {} could not be checked: {}", client.connection.id(), e.getMessage(), e);
        }
    }

    /** Closes the connections whose Session has expired; the timer does so every minute. */
    void closeExpired() {
        for (final Client client : clients.values()) {
            if (expired(client)) {
                drop(client, ReturnCode.AUTHENTICATION_WARNING, SESSION_EXPIRED);
            }
        }
    }

    private boolean expired(final Client client) {
        final FeatureManager.Identity identity = client.identity();
        return (identity != null) && ((identity.sessionExpire() == null) || !LocalDateTime.now(clock).isBefore(identity.sessionExpire()));
    }

    private void drop(final Client client, final ReturnCode returnCode, final String message) {
        drop(client, null, returnCode, message);
    }

    /** Closes a connection, after telling it why; the answer names a frame, if one is given. */
    private void drop(final Client client, final String ref, final ReturnCode returnCode, final String message) {
        if (clients.remove(client.connection.id()) != null) {
            stopWaiting(client);
            gone(client);
            client.send(error(ref, returnCode, message));
            final int code = closeCode(returnCode);
            final String reason = (code == SocketConnection.POLICY_VIOLATION) ? NOT_AUTHENTICATED : "The stone failed, try again later";
            client.closeAfterSends(code, reason);
            timer.schedule(() -> client.closeNow(code, reason), closeTimeoutMillis, TimeUnit.MILLISECONDS);
        }
    }

    private void overloaded(final Client client) {
        if (clients.remove(client.connection.id()) != null) {
            stopWaiting(client);
            gone(client);
            final String id = client.connection.id();
            if (client.identity() == null) {
                // Before AUTH anyone could fill the log with it.
                LOGGER.debug("Too much waits for the socket connection {} before its AUTH, it is closed.", id);
            } else {
                LOGGER.warn("Too much waits for the socket connection {}, it is closed.", id);
            }
            client.closeNow(SocketConnection.TRY_AGAIN_LATER, "Too much is waiting");
        }
    }

    /** The connection no longer waits for its AUTH: it has it, or it is gone. */
    private void stopWaiting(final Client client) {
        if (client.waiting.compareAndSet(true, false)) {
            waiting.decrementAndGet();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** The Circle of a frame, which the connection must have joined. */
    private static String joined(final Client client, final SocketFrame frame) {
        final String circleId = checkId(frame.getCircleId(), Constants.FIELD_CIRCLE_ID);
        if (!client.circles.contains(circleId)) {
            throw new EDSException(ReturnCode.ILLEGAL_ACTION, "Join the Circle first.");
        }
        return circleId;
    }

    private static String feature(final SocketFrame frame) {
        if (!FeatureDeclaration.isName(frame.getFeature())) {
            throw new EDSException(ReturnCode.VERIFICATION_WARNING, "The field 'feature' must name a feature.");
        }
        return frame.getFeature();
    }

    /** A response of the bean which failed becomes the failure of the frame. */
    private static <R extends EDSResponse> R checked(final R response) {
        if (!response.isOk()) {
            throw new EDSException(ReturnCode.findReturnCode(response.getReturnCode()), response.getReturnMessage());
        }
        return response;
    }

    private static SocketFrame reply(final SocketFrameType type, final SocketFrame request) {
        final SocketFrame frame = new SocketFrame(type);
        frame.setRef(request.getRef());
        return frame;
    }

    private static SocketFrame error(final String ref, final ReturnCode returnCode, final String message) {
        final SocketFrame frame = new SocketFrame(SocketFrameType.ERROR);
        frame.setRef(ref);
        frame.setReturnCode(returnCode.getCode());
        frame.setReturnMessage(message);
        return frame;
    }

    private static String checkId(final String id, final String field) {
        if ((id == null) || !Constants.ID_PATTERN.matcher(id).matches()) {
            throw new EDSException(ReturnCode.VERIFICATION_WARNING, "The field '" + field + "' must be a valid ID.");
        }
        return id;
    }

    private static byte[] decode(final String token) {
        if ((token == null) || token.isBlank()) {
            throw new EDSException(ReturnCode.AUTHENTICATION_WARNING, "The AUTH frame has no token.");
        }
        final String value = token.trim();
        // The two alphabets differ only in '+' and '/' against '-' and '_';
        // each decoder refuses the characters of the other.
        final boolean urlSafe = (value.indexOf('-') >= 0) || (value.indexOf('_') >= 0);
        try {
            return (urlSafe ? Base64.getUrlDecoder() : Base64.getDecoder()).decode(value);
        } catch (IllegalArgumentException e) {
            throw new EDSException(ReturnCode.AUTHENTICATION_WARNING, "The token is not Base64.", e);
        }
    }

    // =========================================================================
    // A connection, with what waits for it
    // =========================================================================

    /**
     * One connection. Frames are handled by one thread and sent by another,
     * so a peer which doesn't read holds up neither the handling of its
     * frames nor anyone else.
     */
    private static final class Client {

        private final SocketConnection connection;
        private final Consumer<Client> overloaded;
        private final SerialExecutor handler;
        private final ExecutorService sender;
        private final Backlog received = new Backlog();
        private final Backlog sent = new Backlog();
        private final AtomicBoolean reported = new AtomicBoolean(false);
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final Set<String> circles = ConcurrentHashMap.newKeySet();
        /** The Circles in which the connection is shown: joined, and not unseen. */
        private final Set<String> shown = ConcurrentHashMap.newKeySet();
        private final Map<String, Long> signalled = new ConcurrentHashMap<>();
        /** The access versions each joined Circle was last checked against. */
        private final Map<String, Checked> checked = new ConcurrentHashMap<>();
        /** What is told to the connection behind a check of its access, not yet sent. */
        private final AtomicInteger held = new AtomicInteger(0);
        /** Whether the connection still waits for its AUTH. */
        private final AtomicBoolean waiting = new AtomicBoolean(true);
        private final Budget signals;
        private final Budget entries;
        private final AtomicReference<byte[]> token = new AtomicReference<>();
        private final AtomicReference<FeatureManager.Identity> identity = new AtomicReference<>();

        private Client(final SocketConnection connection, final Consumer<Client> overloaded, final Executor work, final Clock clock) {
            this.connection = connection;
            this.overloaded = overloaded;
            this.signals = new Budget(SIGNAL_BURST, SIGNAL_RATE, clock);
            this.entries = new Budget(ENTRY_BURST, ENTRY_RATE, clock);
            this.handler = new SerialExecutor(work);
            this.sender = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("eds-socket-send-" + connection.id()).factory());
        }

        /** A copy of the Session token, as each request clears the one it is given. */
        private byte[] token() {
            final byte[] secret = token.get();
            if (secret == null) {
                throw new EDSException(ReturnCode.AUTHENTICATION_WARNING, "The connection is closed.");
            }
            return secret.clone();
        }

        /** Who the connection is authenticated as, or null before the AUTH frame. */
        private FeatureManager.Identity identity() {
            return identity.get();
        }

        private void receive(final String text, final Runnable task) {
            final int length = text.length();
            // Before AUTH only a little may wait: the AUTH frame is small.
            final long limit = (identity() == null) ? MAX_PENDING_RECEIVED_BEFORE_AUTH : MAX_PENDING_RECEIVED;
            if (!received.add(length, limit)) {
                overflow();
            } else if (!execute(handler, received.counted(length, task))) {
                received.remove(length);
            }
        }

        private void run(final Runnable task) {
            execute(handler, task);
        }

        private void send(final SocketFrame frame) {
            sendText(SocketCodec.write(frame));
        }

        private void sendText(final String text) {
            final int length = text.length();
            if (!sent.add(length, MAX_PENDING_SENT)) {
                overflow();
            } else if (!execute(sender, sent.counted(length, () -> transmit(text)))) {
                sent.remove(length);
            }
        }

        private void transmit(final String text) {
            try {
                connection.send(text);
            } catch (IOException | RuntimeException e) {
                // The client is gone, or going.
                LOGGER.atDebug().setCause(e).log("Sending to the socket connection {} failed.", connection.id());
            }
        }

        private boolean execute(final Executor executor, final Runnable task) {
            try {
                executor.execute(task);
                return true;
            } catch (RejectedExecutionException _) {
                LOGGER.debug("The socket connection {} is closed.", connection.id());
                return false;
            }
        }

        private void overflow() {
            if (reported.compareAndSet(false, true)) {
                overloaded.accept(this);
            }
        }

        private void closeAfterSends(final int code, final String reason) {
            execute(sender, () -> close(code, reason));
        }

        private void closeNow(final int code, final String reason) {
            if (!closed.get()) {
                Thread.ofVirtual().name("eds-socket-close-" + connection.id()).start(() -> close(code, reason));
            }
        }

        private void close(final int code, final String reason) {
            if (closed.compareAndSet(false, true)) {
                connection.close(code, reason);
                dispose();
            }
        }

        private void dispose() {
            final byte[] secret = token.getAndSet(null);
            if (secret != null) {
                Arrays.fill(secret, (byte) 0);
            }
            circles.clear();
            shown.clear();
            checked.clear();
            handler.shutdown();
            sender.shutdownNow();
        }
    }

    /** What waits for a connection in one direction, counted in frames and characters. */
    private static final class Backlog {

        private final AtomicInteger frames = new AtomicInteger(0);
        private final AtomicLong characters = new AtomicLong(0);

        /** Adds a frame, unless the frames or the characters would pass their limit. */
        private boolean add(final int length, final long maxCharacters) {
            if (frames.incrementAndGet() > MAX_PENDING_FRAMES) {
                frames.decrementAndGet();
                return false;
            }
            if (characters.addAndGet(length) > maxCharacters) {
                remove(length);
                return false;
            }
            return true;
        }

        private void remove(final int length) {
            frames.decrementAndGet();
            characters.addAndGet(-length);
        }

        private Runnable counted(final int length, final Runnable task) {
            return () -> {
                try {
                    task.run();
                } finally {
                    remove(length);
                }
            };
        }
    }

    /**
     * A budget which is spent and comes back over time: at most its burst
     * at once, refilled at its rate a second (a token bucket).
     */
    private static final class Budget {

        private final long burst;
        private final long rate;
        private final Clock clock;
        private double left;
        private long last;

        private Budget(final long burst, final long rate, final Clock clock) {
            this.burst = burst;
            this.rate = rate;
            this.clock = clock;
            this.left = (double) burst;
            this.last = clock.millis();
        }

        /** Takes the amount, if that much is left. */
        private synchronized boolean take(final long amount) {
            final long now = clock.millis();
            left = Math.min((double) burst, left + ((double) Math.max(0, now - last) * rate / 1000.0));
            last = now;
            if ((double) amount > left) {
                return false;
            }
            left -= (double) amount;
            return true;
        }
    }

    /**
     * Runs the tasks of one connection one after the other, in order, on a
     * shared Executor: a frame is handled after those before it. One task
     * runs at a time, and then the turn goes back to the Executor, so a busy
     * connection holds no thread of the pool for itself.
     */
    private static final class SerialExecutor implements Executor {

        private final Executor pool;
        private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
        private final AtomicBoolean scheduled = new AtomicBoolean(false);
        private final AtomicBoolean shutdown = new AtomicBoolean(false);

        private SerialExecutor(final Executor pool) {
            this.pool = pool;
        }

        @Override
        public void execute(final Runnable task) {
            if (shutdown.get()) {
                throw new RejectedExecutionException("The connection is closed.");
            }
            tasks.add(task);
            schedule();
        }

        /** No new tasks; those waiting still run. */
        private void shutdown() {
            shutdown.set(true);
        }

        private void schedule() {
            if (!tasks.isEmpty() && scheduled.compareAndSet(false, true)) {
                try {
                    pool.execute(this::runNext);
                } catch (RejectedExecutionException e) {
                    // The pool is gone, as the stone stops.
                    LOGGER.debug("The tasks of a socket connection are not run.", e);
                    scheduled.set(false);
                    tasks.clear();
                }
            }
        }

        private void runNext() {
            try {
                final Runnable task = tasks.poll();
                if (task != null) {
                    task.run();
                }
            } finally {
                scheduled.set(false);
                schedule();
            }
        }
    }
}
