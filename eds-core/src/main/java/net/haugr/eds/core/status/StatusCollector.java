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
package net.haugr.eds.core.status;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.IntSupplier;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.dtos.StatusSample;
import net.haugr.eds.api.dtos.StatusSeries;

/**
 * <p>Gathers what a stone tells about itself (STATUS.md): its load, how
 * busy it is, and its inventory, what it holds. Everything is kept in
 * memory, in three series: the recent load at the stone's load interval,
 * the load of the last 24 hours in steps of five minutes, and the
 * inventory. A restart starts them anew.</p>
 *
 * <p>Gathering the load is cheap: requests are counted where they are
 * handled, and a sample only reads the counters, the JDK's management
 * interface and the processor time of the own process. A request for the
 * status never gathers anything; it is answered from what is kept here, so
 * many watchers cost what one costs.</p>
 *
 * <p>Only general numbers are kept: no names, and nothing about a single
 * Member, Circle or file. Which accounts were active in a step is counted
 * and forgotten with the step.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class StatusCollector {

    /** The samples of the recent load which are kept. */
    public static final int RECENT_SAMPLES = 720;
    /** The seconds of one step of the day. */
    public static final long DAY_STEP_SECONDS = 300;
    /** The steps of the day which are kept: 24 hours. */
    public static final int DAY_STEPS = 288;
    /** The samples of the inventory which are kept. */
    public static final int INVENTORY_SAMPLES = 720;

    private static final StatusCollector INSTANCE = new StatusCollector();
    private static final double NANOS_PER_MILLI = 1_000_000.0;
    // The names of the values of a sample, as the status gives them.
    private static final String KEY_REQUESTS = "requests";
    private static final String KEY_FAILED = "failed";
    private static final String KEY_BYTES_IN = "bytesIn";
    private static final String KEY_BYTES_OUT = "bytesOut";
    private static final String KEY_REQUEST_MILLIS = "requestMillis";
    private static final String KEY_REQUEST_MILLIS_MAX = "requestMillisMax";
    private static final String KEY_DATABASE_MILLIS = "databaseMillis";
    private static final String KEY_ACCOUNTS = "accounts";
    private static final String KEY_CONNECTIONS = "connections";
    private static final String KEY_PROCESS_SHARE = "processShare";
    private static final String KEY_SYSTEM_LOAD = "systemLoad";
    private static final String KEY_MEMORY_USED = "memoryUsed";
    private static final String KEY_THREADS = "threads";

    // What the requests of the current step add up to.
    private final LongAdder requests = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final LongAdder bytesIn = new LongAdder();
    private final LongAdder bytesOut = new LongAdder();
    private final LongAdder requestNanos = new LongAdder();
    private final AtomicLong longestNanos = new AtomicLong();
    // A private lock, so no other code can hold the lock of the collector.
    private final Object lock = new Object();
    private final Set<Long> accounts = ConcurrentHashMap.newKeySet();

    private final Deque<StatusSample> recent = new ArrayDeque<>();
    private final Deque<StatusSample> day = new ArrayDeque<>();
    private final Deque<StatusSample> inventory = new ArrayDeque<>();
    private final LocalDateTime started = Utilities.newDate();

    private long loadSequence = 0;
    private long inventorySequence = 0;
    private long loadInterval = 0;
    private long inventoryInterval = 0;
    private long lastLoadNanos = 0;
    private long lastInventoryNanos = 0;
    private long lastProcessorNanos = -1;
    private long lastProcessorAt = 0;
    private volatile double databaseMillis = Double.NaN;
    private final AtomicReference<IntSupplier> connections = new AtomicReference<>(() -> 0);
    private Step step = null;

    private StatusCollector() {
        // A singleton, like the Settings.
    }

    /**
     * The collector of this stone.
     *
     * @return The collector
     */
    public static StatusCollector getInstance() {
        return INSTANCE;
    }

    // =========================================================================
    // What is counted as it happens
    // =========================================================================

    /**
     * A request was handled.
     *
     * @param nanos   How long it took
     * @param failure True if it was refused or failed
     * @param in      Bytes received with it, as far as known
     * @param out     Bytes sent with its answer, as far as known
     */
    public void request(final long nanos, final boolean failure, final long in, final long out) {
        requests.increment();
        if (failure) {
            failed.increment();
        }
        if (in > 0) {
            bytesIn.add(in);
        }
        if (out > 0) {
            bytesOut.add(out);
        }
        if (nanos > 0) {
            requestNanos.add(nanos);
            longestNanos.accumulateAndGet(nanos, Math::max);
        }
    }

    /**
     * An account made an authenticated request. Only how many did in a step
     * is told; which ones is forgotten with the step.
     *
     * @param account The internal ID of the account
     */
    public void active(final Long account) {
        if (account != null) {
            accounts.add(account);
        }
    }

    /**
     * How long a trivial query to the database took. The stone can't read
     * the load of the database, but this rises with it.
     *
     * @param millis Milliseconds
     */
    public void database(final double millis) {
        databaseMillis = millis;
    }

    /**
     * Where the number of open socket connections is read.
     *
     * @param supplier Gives the number of open connections
     */
    public void connections(final IntSupplier supplier) {
        connections.set((supplier != null) ? supplier : () -> 0);
    }

    // =========================================================================
    // Gathering
    // =========================================================================

    /**
     * True if a sample of the load is due.
     *
     * @param nanos    The time, as of {@link System#nanoTime()}
     * @param interval The load interval in seconds; 0 for no gathering
     * @return True if a sample is to be taken
     */
    public boolean loadDue(final long nanos, final long interval) {
        synchronized (lock) {
            if (interval != loadInterval) {
                // The interval changed: the samples so far were of another one.
                loadInterval = interval;
                recent.clear();
                lastLoadNanos = 0;
            }
            return (interval > 0) && ((lastLoadNanos == 0) || ((nanos - lastLoadNanos) >= (interval * 1_000_000_000L) - 50_000_000L));
        }
    }

    /**
     * True if a sample of the inventory is due.
     *
     * @param nanos    The time, as of {@link System#nanoTime()}
     * @param interval The inventory interval in seconds
     * @return True if a sample is to be taken
     */
    public boolean inventoryDue(final long nanos, final long interval) {
        synchronized (lock) {
            inventoryInterval = interval;
            return (lastInventoryNanos == 0) || ((nanos - lastInventoryNanos) >= (interval * 1_000_000_000L));
        }
    }

    /**
     * Takes a sample of the load: what the requests since the last one add
     * up to, and how the process and its host are doing now.
     *
     * @param nanos The time, as of {@link System#nanoTime()}
     * @return The sample
     */
    public StatusSample sampleLoad(final long nanos) {
        synchronized (lock) {
            final long count = requests.sumThenReset();
            final long nanosSum = requestNanos.sumThenReset();
            final int active = accounts.size();
            accounts.clear();

            final Map<String, Double> values = new LinkedHashMap<>();
            values.put(KEY_REQUESTS, (double) count);
            values.put(KEY_FAILED, (double) failed.sumThenReset());
            values.put(KEY_BYTES_IN, (double) bytesIn.sumThenReset());
            values.put(KEY_BYTES_OUT, (double) bytesOut.sumThenReset());
            values.put(KEY_REQUEST_MILLIS, (count > 0) ? (nanosSum / NANOS_PER_MILLI / count) : 0.0);
            values.put(KEY_REQUEST_MILLIS_MAX, longestNanos.getAndSet(0) / NANOS_PER_MILLI);
            final double database = databaseMillis;
            if (!Double.isNaN(database)) {
                values.put(KEY_DATABASE_MILLIS, database);
            }
            values.put(KEY_ACCOUNTS, (double) active);
            values.put(KEY_CONNECTIONS, (double) connections.get().getAsInt());

            final int processors = Runtime.getRuntime().availableProcessors();
            values.put("processors", (double) processors);
            final Optional<Duration> used = ProcessHandle.current().info().totalCpuDuration();
            if (used.isPresent()) {
                // The share of all processors which the stone's own process
                // used since the last sample: 1 would be all of them.
                final long now = used.get().toNanos();
                if ((lastProcessorNanos >= 0) && (nanos > lastProcessorAt)) {
                    values.put(KEY_PROCESS_SHARE, Math.clamp((now - lastProcessorNanos) / ((double) (nanos - lastProcessorAt) * processors), 0.0, 1.0));
                }
                lastProcessorNanos = now;
                lastProcessorAt = nanos;
            }
            final double systemLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
            if (systemLoad >= 0) {
                values.put(KEY_SYSTEM_LOAD, systemLoad);
            }
            final Runtime runtime = Runtime.getRuntime();
            values.put(KEY_MEMORY_USED, (double) (runtime.totalMemory() - runtime.freeMemory()));
            values.put("memoryMax", (double) runtime.maxMemory());
            values.put(KEY_THREADS, (double) ManagementFactory.getThreadMXBean().getThreadCount());

            final StatusSample sample = new StatusSample();
            sample.setSequence(++loadSequence);
            sample.setTime(Utilities.newDate());
            sample.setValues(values);
            recent.addLast(sample);
            while (recent.size() > RECENT_SAMPLES) {
                recent.removeFirst();
            }
            lastLoadNanos = nanos;
            fold(sample);

            return sample;
        }
    }

    /**
     * Keeps a sample of the inventory, which was gathered with queries.
     *
     * @param nanos  The time, as of {@link System#nanoTime()}
     * @param values The numbers by their names
     */
    public void sampleInventory(final long nanos, final Map<String, Double> values) {
        synchronized (lock) {
            final StatusSample sample = new StatusSample();
            sample.setSequence(++inventorySequence);
            sample.setTime(Utilities.newDate());
            sample.setValues(values);
            inventory.addLast(sample);
            while (inventory.size() > INVENTORY_SAMPLES) {
                inventory.removeFirst();
            }
            lastInventoryNanos = nanos;
        }
    }

    /**
     * The inventory couldn't be gathered; it is tried again after a minute.
     *
     * @param nanos The time, as of {@link System#nanoTime()}
     */
    public void inventoryFailed(final long nanos) {
        synchronized (lock) {
            lastInventoryNanos = nanos - Math.max(0, (inventoryInterval - 60)) * 1_000_000_000L;
        }
    }

    // =========================================================================
    // What is told
    // =========================================================================

    /**
     * The recent load.
     *
     * @param since Only samples after this sequence; null for all kept
     * @return The series
     */
    public StatusSeries load(final Long since) {
        synchronized (lock) {
            final StatusSeries series = new StatusSeries();
            series.setInterval(loadInterval);
            final List<StatusSample> samples = new ArrayList<>();
            for (final StatusSample sample : recent) {
                if ((since == null) || (sample.getSequence() > since)) {
                    samples.add(sample);
                }
            }
            series.setSamples(samples);

            return series;
        }
    }

    /**
     * The load of the last 24 hours in steps, with the step which is still
     * being filled as the last.
     *
     * @return The series
     */
    public StatusSeries day() {
        synchronized (lock) {
            final StatusSeries series = new StatusSeries();
            series.setInterval(DAY_STEP_SECONDS);
            final List<StatusSample> samples = new ArrayList<>(day);
            if (step != null) {
                samples.add(step.sample());
            }
            series.setSamples(samples);

            return series;
        }
    }

    /**
     * The inventory.
     *
     * @param history True for all samples kept, false for the latest only
     * @return The series
     */
    public StatusSeries inventory(final boolean history) {
        synchronized (lock) {
            final StatusSeries series = new StatusSeries();
            series.setInterval(inventoryInterval);
            final List<StatusSample> samples = new ArrayList<>();
            if (history) {
                samples.addAll(inventory);
            } else if (!inventory.isEmpty()) {
                samples.add(inventory.getLast());
            }
            series.setSamples(samples);

            return series;
        }
    }

    /**
     * When the stone started.
     *
     * @return The time, in UTC
     */
    public LocalDateTime started() {
        return started;
    }

    /**
     * Forgets everything gathered, for the tests.
     */
    public void reset() {
        synchronized (lock) {
            requests.reset();
            failed.reset();
            bytesIn.reset();
            bytesOut.reset();
            requestNanos.reset();
            longestNanos.set(0);
            accounts.clear();
            recent.clear();
            day.clear();
            inventory.clear();
            loadSequence = 0;
            inventorySequence = 0;
            loadInterval = 0;
            inventoryInterval = 0;
            lastLoadNanos = 0;
            lastInventoryNanos = 0;
            lastProcessorNanos = -1;
            databaseMillis = Double.NaN;
            connections.set(() -> 0);
            step = null;
        }
    }

    // =========================================================================
    // The day, in steps
    // =========================================================================

    /** Adds a sample of the load to the step of the day it falls into. */
    private void fold(final StatusSample sample) {
        final long key = sample.getTime().toEpochSecond(ZoneOffset.UTC) / DAY_STEP_SECONDS;
        if ((step != null) && (step.key != key)) {
            day.addLast(step.sample());
            while (day.size() > DAY_STEPS) {
                day.removeFirst();
            }
            step = null;
        }
        if (step == null) {
            step = new Step(key);
        }
        step.add(sample.getValues());
    }

    /**
     * One step of the day: what its samples add up to. Counts are summed;
     * of what is measured, the average and, as {@code <name>Max}, the peak
     * are kept; of what stands, the highest.
     */
    private static final class Step {

        private static final Set<String> SUMMED = Set.of(KEY_REQUESTS, KEY_FAILED, KEY_BYTES_IN, KEY_BYTES_OUT);
        private static final Set<String> AVERAGED = Set.of(KEY_DATABASE_MILLIS, KEY_PROCESS_SHARE, KEY_SYSTEM_LOAD);
        private static final Set<String> HIGHEST = Set.of(KEY_ACCOUNTS, KEY_CONNECTIONS, KEY_MEMORY_USED, KEY_THREADS, KEY_REQUEST_MILLIS_MAX);

        private final long key;
        private final Map<String, Double> sums = new LinkedHashMap<>();
        private final Map<String, Double> peaks = new LinkedHashMap<>();
        private final Map<String, Integer> counts = new LinkedHashMap<>();
        private double requestMillisTotal = 0;
        private int samples = 0;

        private Step(final long key) {
            this.key = key;
        }

        private void add(final Map<String, Double> values) {
            samples++;
            requestMillisTotal += values.getOrDefault(KEY_REQUEST_MILLIS, 0.0) * values.getOrDefault(KEY_REQUESTS, 0.0);
            for (final Map.Entry<String, Double> value : values.entrySet()) {
                final String name = value.getKey();
                if (SUMMED.contains(name) || AVERAGED.contains(name)) {
                    sums.merge(name, value.getValue(), Double::sum);
                    counts.merge(name, 1, Integer::sum);
                }
                if (AVERAGED.contains(name) || HIGHEST.contains(name)) {
                    peaks.merge(name, value.getValue(), Math::max);
                }
            }
        }

        private StatusSample sample() {
            final Map<String, Double> values = new LinkedHashMap<>();
            for (final String name : List.of(KEY_REQUESTS, KEY_FAILED, KEY_BYTES_IN, KEY_BYTES_OUT)) {
                values.put(name, sums.getOrDefault(name, 0.0));
            }
            final double count = sums.getOrDefault(KEY_REQUESTS, 0.0);
            values.put(KEY_REQUEST_MILLIS, (count > 0) ? (requestMillisTotal / count) : 0.0);
            putPeak(values, KEY_REQUEST_MILLIS_MAX, KEY_REQUEST_MILLIS_MAX);
            for (final String name : List.of(KEY_DATABASE_MILLIS, KEY_PROCESS_SHARE, KEY_SYSTEM_LOAD)) {
                final Double sum = sums.get(name);
                if (sum != null) {
                    values.put(name, sum / counts.get(name));
                    putPeak(values, name + "Max", name);
                }
            }
            putPeak(values, KEY_ACCOUNTS, KEY_ACCOUNTS);
            putPeak(values, KEY_CONNECTIONS, KEY_CONNECTIONS);
            putPeak(values, KEY_MEMORY_USED, KEY_MEMORY_USED);
            putPeak(values, KEY_THREADS, KEY_THREADS);
            values.put("samples", (double) samples);

            final StatusSample sample = new StatusSample();
            sample.setSequence(key);
            sample.setTime(LocalDateTime.ofEpochSecond(key * DAY_STEP_SECONDS, 0, ZoneOffset.UTC));
            sample.setValues(values);

            return sample;
        }

        private void putPeak(final Map<String, Double> values, final String name, final String source) {
            final Double peak = peaks.get(source);
            if (peak != null) {
                values.put(name, peak);
            }
        }
    }
}
