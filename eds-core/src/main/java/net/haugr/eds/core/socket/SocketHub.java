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
 * the MasterKey is committed ({@link AccessChange}), the connections are
 * checked again, and when a Session has expired its connections are closed.
 * What waits for a connection is limited, both ways; a connection over a
 * limit, whose client doesn't read or floods, is closed.</p>
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
    static final long AUTH_TIMEOUT_SECONDS = 30;

    private static final String NOT_AUTHENTICATED = "Not authenticated";
    private static final String SESSION_EXPIRED = "The Session has expired.";

    private final FeatureBean features;
    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    /** Guards who is shown in which Circle, so that coming and going are told once and in order. */
    private final Object presence = new Object();
    private final ScheduledExecutorService timer;
    private final ExecutorService checker;
    private final AtomicBoolean checkPending = new AtomicBoolean(false);
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

    // =========================================================================
    // Connections
    // =========================================================================

    public void open(final SocketConnection connection) {
        final Client client = new Client(connection, this::overloaded, frameWork);
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
     * Members, Circles, Trustees or the MasterKey changed: every connection
     * is checked again, off the thread of the request. A check which is
     * waiting already covers the later changes too.
     *
     * @param change The marker of a committed change
     */
    void accessChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) final AccessChange change) {
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

    /** Tells the connections which joined a Circle; only those of one Member, if one is named. */
    private void announce(final String circleId, final String json, final Client except, final String memberId) {
        for (final Client client : clients.values()) {
            if (!client.equals(except) && client.circles.contains(circleId) && ((memberId == null) || isOf(client, memberId))) {
                if (expired(client)) {
                    drop(client, ReturnCode.AUTHENTICATION_WARNING, SESSION_EXPIRED);
                } else {
                    client.sendText(json);
                }
            }
        }
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
        String ref = null;
        try {
            final SocketFrame frame = SocketCodec.parse(text);
            ref = frame.getRef();
            if (frame.getType() == SocketFrameType.AUTH) {
                authenticate(client, frame);
            } else if (client.identity() == null) {
                drop(client, ReturnCode.AUTHENTICATION_WARNING, "The first frame must be AUTH.");
            } else if (expired(client)) {
                drop(client, ReturnCode.AUTHENTICATION_WARNING, SESSION_EXPIRED);
            } else {
                dispatch(client, frame);
            }
        } catch (EDSException e) {
            LOGGER.debug("Socket frame failed: {}", e.getMessage());
            if ((e.getReturnCode() == ReturnCode.AUTHENTICATION_WARNING) && (client.identity() != null)) {
                drop(client, e.getReturnCode(), e.getMessage());
            } else {
                client.send(error(ref, e.getReturnCode(), e.getMessage()));
            }
        } catch (RuntimeException e) {
            LOGGER.error("Socket frame failed: {}", e.getMessage(), e);
            client.send(error(ref, ReturnCode.ERROR, e.getMessage()));
        }
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

        final SocketFrame ready = reply(SocketFrameType.READY, frame);
        ready.setMemberId(identity.memberId());
        ready.setAccountName(identity.accountName());
        client.send(ready);
    }

    private void join(final Client client, final SocketFrame frame) {
        final String circleId = checkId(frame.getCircleId(), Constants.FIELD_CIRCLE_ID);
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
        }

        final SocketFrame left = reply(SocketFrameType.LEFT, frame);
        left.setCircleId(circleId);
        client.send(left);
    }

    private void add(final Client client, final SocketFrame frame) {
        final String circleId = joined(client, frame);
        final EntryRequest body = new EntryRequest();
        body.setKind(frame.getKind());
        body.setAbout((frame.getAbout() != null) ? checkId(frame.getAbout(), Constants.FIELD_ABOUT) : null);
        body.setName(frame.getName());
        body.setContent(frame.getContent());
        done(client, frame, circleId, features.addEntry(client.token(), circleId, feature(frame), body));
    }

    private void change(final Client client, final SocketFrame frame) {
        final String circleId = joined(client, frame);
        final EntryRequest body = new EntryRequest();
        body.setName(frame.getName());
        body.setContent(frame.getContent());
        done(client, frame, circleId, features.changeEntry(client.token(), circleId, feature(frame), checkId(frame.getEntryId(), Constants.FIELD_ENTRY_ID), body));
    }

    private void remove(final Client client, final SocketFrame frame) {
        final String circleId = joined(client, frame);
        done(client, frame, circleId, features.removeEntry(client.token(), circleId, feature(frame), checkId(frame.getEntryId(), Constants.FIELD_ENTRY_ID)));
    }

    private void close(final Client client, final SocketFrame frame) {
        final String circleId = joined(client, frame);
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
     * for the feature and Circle is dropped silently.
     */
    private void signal(final Client client, final SocketFrame frame) {
        // The checks run in this order: joined, feature, to, the signal, and
        // then the access to the feature.
        final String circleId = joined(client, frame);
        final String feature = feature(frame);
        final String to = (frame.getTo() != null) ? checkId(frame.getTo(), Constants.FIELD_TO) : null;
        final String signal = signalOf(frame);
        final long interval = features.signal(client.token(), circleId, feature);
        if (due(client, circleId + '|' + feature, interval)) {
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

    /**
     * Whether a signal of the connection for the feature and Circle (the
     * key) may go now, as the interval has passed since its last one; if
     * so, now is noted as the time of its last one.
     */
    private boolean due(final Client client, final String key, final long interval) {
        final long now = clock.millis();
        final Long last = client.signalled.get(key);
        if ((last != null) && ((now - last) < interval)) {
            return false;
        }
        client.signalled.put(key, now);

        return true;
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
            final Set<String> readable = features.readableCircles(token.clone());
            for (final String circleId : client.circles) {
                if (!readable.contains(circleId)) {
                    synchronized (presence) {
                        show(client, circleId, false);
                        client.circles.remove(circleId);
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
        if (clients.remove(client.connection.id()) != null) {
            gone(client);
            client.send(error(null, returnCode, message));
            client.closeAfterSends(SocketConnection.POLICY_VIOLATION, NOT_AUTHENTICATED);
            timer.schedule(() -> client.closeNow(SocketConnection.POLICY_VIOLATION, NOT_AUTHENTICATED), closeTimeoutMillis, TimeUnit.MILLISECONDS);
        }
    }

    private void overloaded(final Client client) {
        if (clients.remove(client.connection.id()) != null) {
            gone(client);
            final String id = client.connection.id();
            LOGGER.warn("Too much waits for the socket connection {}, it is closed.", id);
            client.closeNow(SocketConnection.TRY_AGAIN_LATER, "Too much is waiting");
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
        private final Backlog received = new Backlog(MAX_PENDING_RECEIVED);
        private final Backlog sent = new Backlog(MAX_PENDING_SENT);
        private final AtomicBoolean reported = new AtomicBoolean(false);
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final Set<String> circles = ConcurrentHashMap.newKeySet();
        /** The Circles in which the connection is shown: joined, and not unseen. */
        private final Set<String> shown = ConcurrentHashMap.newKeySet();
        private final Map<String, Long> signalled = new ConcurrentHashMap<>();
        private final AtomicReference<byte[]> token = new AtomicReference<>();
        private final AtomicReference<FeatureManager.Identity> identity = new AtomicReference<>();

        private Client(final SocketConnection connection, final Consumer<Client> overloaded, final Executor work) {
            this.connection = connection;
            this.overloaded = overloaded;
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
            if (!received.add(length)) {
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
            if (!sent.add(length)) {
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
            handler.shutdown();
            sender.shutdownNow();
        }
    }

    /** What waits for a connection in one direction, counted in frames and characters. */
    private static final class Backlog {

        private final long maxCharacters;
        private final AtomicInteger frames = new AtomicInteger(0);
        private final AtomicLong characters = new AtomicLong(0);

        private Backlog(final long maxCharacters) {
            this.maxCharacters = maxCharacters;
        }

        private boolean add(final int length) {
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
