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
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.exceptions.CryptoException;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.managers.RotationManager;
import net.haugr.eds.core.model.ConnectionSource;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.rotation.CircleRotations;
import net.haugr.eds.core.socket.RotationChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Rotates the Circle Secret of a Circle (PLAN.md, Crypto C6), with the
 * secrets a Trustee's request handed over: the new secret in one short
 * transaction, then every Data Object re-encrypted in a transaction of its
 * own, {@code eds.circle.secret.rotation.threads} at a time, and the end in
 * one more. The runtime runs it in the background (eds-quarkus installs it
 * as the runner of {@link CircleRotations}).</p>
 *
 * <p>An object which can't be re-encrypted, as its content failed its check,
 * is marked for the sanity check; the rotation then can't end, and keeps
 * the retiring secret, until it is deleted or replaced. A job which ends
 * early leaves everything in a state the next one continues from.</p>
 *
 * <p>Without a UserTransaction (the unit tests, with a resource-local
 * transaction), the steps run in the caller's transaction, one object after
 * the other.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ApplicationScoped
public class RotationBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotationBean.class);
    private static final int BATCH = 100;
    private static final int MAX_ROUNDS = 5;
    /**
     * How long the retiring secret is kept at least, after the rotation
     * started: a request which read the Trustee row just before may still
     * write with it. What it writes is then re-encrypted before the end.
     */
    private static final long GRACE_MILLIS = 5_000;

    private final EntityManager entityManager;
    private final UserTransaction userTransaction;
    private final ConnectionSource readers;
    private final Settings settings;
    private final Event<RotationChange> changes;

    public RotationBean() {
        this(null, null, null, Settings.getInstance(), null);
    }

    @Inject
    public RotationBean(final EntityManager entityManager, final UserTransaction userTransaction, final ConnectionSource readers, final Event<RotationChange> changes) {
        this(entityManager, userTransaction, readers, Settings.getInstance(), changes);
    }

    public RotationBean(final EntityManager entityManager, final Settings settings, final ConnectionSource readers) {
        this(entityManager, null, readers, settings, null);
    }

    public RotationBean(final EntityManager entityManager, final UserTransaction userTransaction, final ConnectionSource readers,
                        final Settings settings, final Event<RotationChange> changes) {
        this.entityManager = entityManager;
        this.userTransaction = userTransaction;
        this.readers = readers;
        this.settings = settings;
        this.changes = changes;
    }

    /**
     * Runs the rotation of a Circle with the secrets handed over, until it
     * has ended or can't go on; then the Circle is free for the next offer.
     *
     * @param handoff The secrets a Trustee's request unwrapped
     */
    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public void rotate(final CircleRotations.Handoff handoff) {
        try {
            CircleRotations.Handoff next = handoff;
            while (next != null) {
                next = rotateOnce(next);
            }
        } catch (RuntimeException e) {
            LOGGER.error("The rotation of the Circle Secret of the Circle {} stopped: {}", handoff.externalId(), e.getMessage(), e);
        } finally {
            CircleRotations.finished(handoff);
        }
    }

    private CircleRotations.Handoff rotateOnce(final CircleRotations.Handoff handoff) {
        final RotationManager.Rotation rotation = step(manager -> manager.begin(handoff));
        if (rotation == null) {
            return null;
        }
        announce(rotation.externalId(), false);

        final long started = System.currentTimeMillis();
        RotationManager.Outcome outcome = RotationManager.Outcome.MORE;
        for (int round = 0; (round < MAX_ROUNDS) && (outcome == RotationManager.Outcome.MORE); round++) {
            long failed = reencryptAll(rotation);
            if (graceUntil(started + GRACE_MILLIS)) {
                failed += reencryptAll(rotation);
            }
            resealEntries(rotation);
            outcome = endStep(rotation);
            if ((outcome == RotationManager.Outcome.MORE) && (failed > 0)) {
                break;
            }
        }
        if (outcome == RotationManager.Outcome.MORE) {
            LOGGER.warn("The rotation of the Circle Secret of the Circle {} can't end yet: Data Objects are left on the retiring secret, "
                    + "of which those which failed their check stay until they are deleted or replaced. It is tried again in an hour.", rotation.externalId());
            CircleRotations.retryLater(rotation.circleId());
            return null;
        }
        announce(rotation.externalId(), true);

        return (outcome == RotationManager.Outcome.AGAIN)
                ? new CircleRotations.Handoff(rotation.circleId(), rotation.externalId(), rotation.keyId(), rotation.current(), null, null)
                : null;
    }

    /**
     * Waits until the time, if the rotation runs in the background.
     *
     * @return True if it waited
     */
    private boolean graceUntil(final long until) {
        final long wait = until - System.currentTimeMillis();
        if ((userTransaction == null) || (wait <= 0)) {
            return false;
        }
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The rotation was interrupted.", e);
        }
        return true;
    }

    private RotationManager.Outcome endStep(final RotationManager.Rotation rotation) {
        try {
            return step(manager -> manager.end(rotation));
        } catch (RuntimeException e) {
            // An object written with the retiring secret while the end ran;
            // it is re-encrypted in the next round.
            LOGGER.debug("The rotation of the Circle {} couldn't end yet.", rotation.externalId(), e);
            return RotationManager.Outcome.MORE;
        }
    }

    /**
     * Re-encrypts the objects on the retiring secret, batch by batch.
     *
     * @return How many failed
     */
    private long reencryptAll(final RotationManager.Rotation rotation) {
        final AtomicLong failed = new AtomicLong();
        final List<Long> skipped = new ArrayList<>();
        List<Long> ids = step(manager -> manager.next(rotation, BATCH));
        while (!ids.isEmpty()) {
            final List<Long> batch = ids.stream().filter(id -> !skipped.contains(id)).toList();
            if (batch.isEmpty()) {
                break;
            }
            skipped.addAll(reencrypt(rotation, batch, failed));
            final int seen = skipped.size();
            ids = step(manager -> manager.next(rotation, BATCH + seen));
        }
        return failed.get();
    }

    /** Seals the authors and owner tags of all entries anew, batch by batch. */
    private void resealEntries(final RotationManager.Rotation rotation) {
        long last = step(manager -> manager.resealEntries(rotation, 0, BATCH));
        while (last >= 0) {
            final long after = last;
            last = step(manager -> manager.resealEntries(rotation, after, BATCH));
        }
    }

    /** Re-encrypts a batch, several at a time; returns those which failed. */
    private List<Long> reencrypt(final RotationManager.Rotation rotation, final List<Long> batch, final AtomicLong failed) {
        final List<Long> failures = Collections.synchronizedList(new ArrayList<>());
        final ConcurrentLinkedQueue<Long> queue = new ConcurrentLinkedQueue<>(batch);
        final Runnable worker = () -> {
            Long id = queue.poll();
            while (id != null) {
                reencryptOne(rotation, id, failures, failed);
                id = queue.poll();
            }
        };

        final int threads = (userTransaction == null) ? 1 : Math.min(settings.getCircleSecretRotationThreads(), batch.size());
        if (threads <= 1) {
            worker.run();
        } else {
            try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
                final List<Future<?>> running = new ArrayList<>(threads);
                for (int i = 0; i < threads; i++) {
                    running.add(pool.submit(worker));
                }
                for (final Future<?> future : running) {
                    waitFor(future);
                }
            }
        }
        return failures;
    }

    private void reencryptOne(final RotationManager.Rotation rotation, final long id, final List<Long> failures, final AtomicLong failed) {
        try {
            step(manager -> manager.reencrypt(rotation, id));
        } catch (RuntimeException e) {
            failures.add(id);
            if (damaged(e)) {
                failed.incrementAndGet();
                LOGGER.error("The Data Object with the content row {} failed its check, and couldn't be re-encrypted for the rotation of the Circle {}: {}",
                        id, rotation.externalId(), e.getMessage());
                step(manager -> {
                    manager.markFailed(id);
                    return null;
                });
            } else {
                // E.g. a deadlock with a request which changed the object
                // meanwhile: it is tried again in the next round.
                LOGGER.info("The Data Object with the content row {} is re-encrypted later for the rotation of the Circle {}: {}",
                        id, rotation.externalId(), e.getMessage());
            }
        }
    }

    /** Whether the content itself failed, not the database. */
    private static boolean damaged(final RuntimeException e) {
        return (e instanceof CryptoException)
                || ((e instanceof EDSException failure) && (failure.getReturnCode() == ReturnCode.INTEGRITY_ERROR));
    }

    private static void waitFor(final Future<?> future) {
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The rotation was interrupted.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("A worker of the rotation failed.", e);
        }
    }

    private <T> T step(final Function<RotationManager, T> work) {
        return StreamTransaction.run(userTransaction, settings.getStreamTransactionTimeout(),
                () -> work.apply(new RotationManager(settings, entityManager, readers)));
    }

    private void announce(final String circleId, final boolean ended) {
        if (changes != null) {
            changes.fire(new RotationChange(circleId, ended));
        }
    }
}
