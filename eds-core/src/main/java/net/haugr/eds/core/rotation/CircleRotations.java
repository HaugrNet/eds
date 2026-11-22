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
package net.haugr.eds.core.rotation;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;

/**
 * <p>Where a request hands an unwrapped Circle Secret to the rotation of
 * the Circle's secret (PLAN.md, Crypto C6). The stone alone can't read a
 * Circle Secret; only a Trustee's request unwraps it. So when a request
 * unwraps the secret of a Circle whose rotation is due, or which is being
 * rotated, it offers the secret here, and the runtime starts the rotation in
 * the background (eds-quarkus installs the runner). The request isn't held
 * up by it.</p>
 *
 * <p>A Circle is rotated by one job of this stone at a time; an offer for a
 * Circle with a job is ignored. Without a runner (a test which runs the
 * rotation itself), offers are kept until they are taken.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class CircleRotations {

    private static final Map<Long, Handoff> RUNNING = new ConcurrentHashMap<>();
    /** Circles whose rotation couldn't end, and when the next try may start. */
    private static final Map<Long, LocalDateTime> WAITING = new ConcurrentHashMap<>();
    /** How long a Circle whose rotation couldn't end waits for the next try. */
    private static final long WAIT_HOURS = 1;
    private static final AtomicReference<Consumer<Handoff>> RUNNER = new AtomicReference<>();

    private CircleRotations() {
        // Utility class.
    }

    /**
     * What a rotation needs from a request: the Circle, and its secrets,
     * unwrapped. The previous secret is there while a rotation is under way,
     * and is needed to read the objects which aren't re-encrypted yet.
     *
     * @param circleId       Internal ID of the Circle
     * @param externalId     External ID of the Circle
     * @param keyId          Key row of the current secret
     * @param current        The current Circle Secret
     * @param previousKeyId  Key row of the retiring secret, or null
     * @param previous       The retiring Circle Secret, or null
     */
    public record Handoff(long circleId, String externalId, long keyId, SecretEDSKey current, Long previousKeyId, SecretEDSKey previous) {
    }

    /**
     * Installs the runner, which starts a rotation in the background; null
     * removes it.
     *
     * @param starter Starts the rotation of a Handoff
     */
    public static void install(final Consumer<Handoff> starter) {
        RUNNER.set(starter);
    }

    /**
     * Offers the Circle Secret which a request has unwrapped. Nothing
     * happens unless the Circle's rotation is due or under way, and no job
     * of this stone has the Circle already.
     *
     * @param trustee  The Trustee row the secret was unwrapped from
     * @param current  The current Circle Secret
     * @param previous Unwraps the retiring secret, if there is one
     */
    public static void offer(final TrusteeEntity trustee, final SecretEDSKey current, final Supplier<SecretEDSKey> previous) {
        final CircleEntity circle = trustee.getCircle();
        final LocalDateTime now = Utilities.newDate();
        final LocalDateTime waiting = WAITING.get(circle.getId());
        if ((waiting != null) && waiting.isAfter(now)) {
            return;
        }
        final boolean underWay = trustee.getPreviousKey() != null;
        if ((underWay || circle.isRotationDue(now)) && !RUNNING.containsKey(circle.getId())) {
            final Handoff handoff = new Handoff(circle.getId(), circle.getExternalId(), trustee.getKey().getId(), current,
                    underWay ? trustee.getPreviousKey().getId() : null, underWay ? previous.get() : null);
            if (RUNNING.putIfAbsent(circle.getId(), handoff) == null) {
                final Consumer<Handoff> start = RUNNER.get();
                if (start != null) {
                    start.accept(handoff);
                }
            }
        }
    }

    /**
     * Takes the offer kept for a Circle, for a caller which runs the
     * rotation itself (the tests). The Circle counts as having a job until
     * {@link #finished} is called.
     *
     * @param circleId Internal ID of the Circle
     * @return The offer, or null
     */
    public static Handoff pending(final long circleId) {
        return RUNNING.get(circleId);
    }

    /**
     * The job of a Circle has ended, successfully or not; the next offer
     * starts another.
     *
     * @param handoff The offer the job had
     */
    public static void finished(final Handoff handoff) {
        RUNNING.remove(handoff.circleId(), handoff);
    }

    /**
     * The rotation of a Circle couldn't end, as an object failed its check:
     * the next try waits an hour, instead of every request starting one
     * which fails again.
     *
     * @param circleId Internal ID of the Circle
     */
    public static void retryLater(final long circleId) {
        WAITING.put(circleId, Utilities.newDate().plusHours(WAIT_HOURS));
    }

    /**
     * Whether a job of this stone has the Circle.
     *
     * @param circleId Internal ID of the Circle
     * @return True while a job has it
     */
    public static boolean isRunning(final long circleId) {
        return RUNNING.containsKey(circleId);
    }

    /**
     * Forgets the offer of a Circle, and that it waits: as a stone which
     * stops forgets them.
     *
     * @param circleId Internal ID of the Circle
     */
    public static void forget(final long circleId) {
        RUNNING.remove(circleId);
        WAITING.remove(circleId);
    }
}
