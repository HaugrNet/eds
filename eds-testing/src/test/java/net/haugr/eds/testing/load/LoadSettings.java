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
package net.haugr.eds.testing.load;

import java.time.Duration;

/**
 * <p>How much load, and for how long; all from system properties, so a run
 * is shaped on the command line, e.g. {@code -Deds.load.users=50
 * -Deds.load.duration=PT10M}.</p>
 *
 * <table>
 *   <caption>Properties</caption>
 *   <tr><td>eds.load.url</td><td>A running stone to load, e.g. {@code http://localhost:8080/api/v1}; without it, a stone is started</td></tr>
 *   <tr><td>eds.load.admin, eds.load.passphrase</td><td>Its System Administrator; on an empty stone the test claims it with these</td></tr>
 *   <tr><td>eds.load.users</td><td>Users working at once (default 16)</td></tr>
 *   <tr><td>eds.load.rooms</td><td>Rooms they share (default 6); each user is in up to three</td></tr>
 *   <tr><td>eds.load.duration</td><td>How long the load lasts, ISO-8601 (default PT2M)</td></tr>
 *   <tr><td>eds.load.rampup</td><td>Over which the users start (default PT10S)</td></tr>
 *   <tr><td>eds.load.think.millis</td><td>Longest pause between two actions of a user, 0 for none (default 20)</td></tr>
 *   <tr><td>eds.load.content.max</td><td>Largest common upload in bytes (default 1 MiB); one in 50 is up to 6 MiB, across the stone's 4 MiB parts</td></tr>
 *   <tr><td>eds.load.held.max</td><td>Bytes of content a user keeps before deleting (default 24 MiB)</td></tr>
 *   <tr><td>eds.load.rotate.every</td><td>How often the administrator has the Circle Secret of a room rotated, ISO-8601: the first at once, then one each period (default PT20M); a stone rotates each Circle once a year by default</td></tr>
 *   <tr><td>eds.load.churn</td><td>Whether an administrator makes and removes users and rooms meanwhile (default true)</td></tr>
 *   <tr><td>eds.load.cleanup</td><td>Whether the users and rooms of the test are deleted at the end (default true)</td></tr>
 *   <tr><td>eds.load.seed</td><td>Seed of the choices, to repeat a run (default random)</td></tr>
 *   <tr><td>eds.load.jfr</td><td>Whether the started stone keeps a Flight Recording, target/load-stone.jfr (default false)</td></tr>
 *   <tr><td>eds.load.pg.container</td><td>Watch the started stone's PostgreSQL database in this container, see {@link PostgresObserver} (default none)</td></tr>
 *   <tr><td>eds.load.max.failures</td><td>Failures the run may have and still pass (default 0)</td></tr>
 * </table>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
record LoadSettings(String url, String admin, String passphrase, int users, int rooms, Duration duration, Duration rampUp,
                    int thinkMillis, int contentMax, long heldMax, Duration rotateEvery, boolean churn, boolean cleanup, long seed, long maxFailures) {

    static LoadSettings fromSystemProperties() {
        return new LoadSettings(
                System.getProperty("eds.load.url"),
                System.getProperty("eds.load.admin", "admin"),
                System.getProperty("eds.load.passphrase", "the load test keeps the stone busy, 2026"),
                Integer.getInteger("eds.load.users", 16),
                Integer.getInteger("eds.load.rooms", 6),
                Duration.parse(System.getProperty("eds.load.duration", "PT2M")),
                Duration.parse(System.getProperty("eds.load.rampup", "PT10S")),
                Integer.getInteger("eds.load.think.millis", 20),
                Integer.getInteger("eds.load.content.max", 1 << 20),
                Long.getLong("eds.load.held.max", 24L << 20),
                Duration.parse(System.getProperty("eds.load.rotate.every", "PT20M")),
                Boolean.parseBoolean(System.getProperty("eds.load.churn", "true")),
                Boolean.parseBoolean(System.getProperty("eds.load.cleanup", "true")),
                Long.getLong("eds.load.seed", System.nanoTime()),
                Long.getLong("eds.load.max.failures", 0));
    }

    LoadSettings withUrl(final String stoneUrl) {
        return new LoadSettings(stoneUrl, admin, passphrase, users, rooms, duration, rampUp, thinkMillis, contentMax, heldMax, rotateEvery, churn, cleanup, seed, maxFailures);
    }

    String describe() {
        return String.format("%d users in %d rooms for %s (ramp-up %s), pauses up to %d ms, uploads up to %d bytes, a rotation every %s, churn %s, seed %d",
                users, rooms, duration, rampUp, thinkMillis, contentMax, rotateEvery, churn ? "on" : "off", seed);
    }
}
