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
package net.haugr.eds.load;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;

/**
 * <p>What the load test measured, per operation: how often it ran, how it
 * ended, how long it took and how many bytes went each way.</p>
 *
 * <p>An outcome is one of three: <b>ok</b>; <b>expected</b>, a refusal the
 * test provoked on purpose or a race it can't avoid (another user removed
 * what was reacted to); and <b>failed</b>, everything else, also any 5xx and
 * any lost connection. A run is good when nothing failed.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class Metrics {

    enum Outcome { OK, EXPECTED, FAILED }

    /** The kinds of timed operations, as the timeline tells them apart. */
    enum Kind { REST, SOCKET, DELIVERY }

    /** One line of the table of operations. */
    record Row(String name, long count, double perSecond, long expected, long failed, boolean timed,
               double p50, double p90, double p99, double max, double mean, double sentMiB, double receivedMiB) {
    }

    /** The timeline is kept in steps of this length. */
    static final long STEP_SECONDS = 5;
    private static final long STEP_NANOS = STEP_SECONDS * 1_000_000_000L;

    private static final int SAMPLES_PER_OPERATION = 5;

    private final Map<String, Operation> operations = new ConcurrentHashMap<>(128);
    private final Queue<String> notes = new ConcurrentLinkedQueue<>();
    private final Map<Integer, Step> timeline = new ConcurrentHashMap<>();
    private final Step whole = new Step();
    private volatile long started = System.nanoTime();
    private volatile long stopped = 0;
    private volatile boolean timing = false;
    private volatile long loadEnded = 0;

    void start() {
        operations.clear();
        timeline.clear();
        whole.clear();
        started = System.nanoTime();
        stopped = 0;
        loadEnded = 0;
        timing = true;
    }

    void stop() {
        stopped = System.nanoTime();
        timing = false;
    }

    /**
     * The time of the load is up: what the users finish afterwards still
     * counts in the table, but no longer on the timeline, which would show
     * the winding down as a fall of the load.
     */
    void endLoad() {
        loadEnded = System.nanoTime();
        timing = false;
    }

    /** How long the timeline is, in seconds: until the time of the load was up. */
    double loadSeconds() {
        return Math.max(1, ((loadEnded == 0) ? ((stopped == 0) ? System.nanoTime() : stopped) : loadEnded) - started) / 1e9;
    }

    /** Counted, but not on the timeline of latencies: a check, a frame heard, a frame which gets no answer. */
    void tally(final String operation, final Outcome outcome, final long nanos, final long sent, final long received, final String detail) {
        tally(null, operation, outcome, nanos, sent, received, detail);
    }

    /** A request or delivery of the given kind, also on the timeline. */
    void tally(final Kind kind, final String operation, final Outcome outcome, final long nanos, final long sent, final long received, final String detail) {
        final Operation op = operations.computeIfAbsent(operation, Operation::new);
        op.add(outcome, nanos, sent, received, detail);
        if (timing) {
            final Step step = timeline.computeIfAbsent((int) ((System.nanoTime() - started) / STEP_NANOS), index -> new Step());
            if ((kind != null) && (nanos > 0)) {
                step.histograms[kind.ordinal()].add(nanos);
                whole.histograms[kind.ordinal()].add(nanos);
            }
            if (outcome == Outcome.FAILED) {
                step.failed.increment();
            }
        }
    }

    /** A measurement which isn't a request, such as the time a chat message took to reach another user. */
    void latency(final String operation, final long nanos) {
        tally(Kind.DELIVERY, operation, Outcome.OK, nanos, 0, 0, null);
    }

    /**
     * The timeline of the load, a step of {@link #STEP_SECONDS} each: the
     * seconds since the start at its end, then per kind the count, p50 and
     * p90 in milliseconds, the failures of the step, and its length in
     * seconds: the last one ends with the load, so it may be shorter.
     */
    List<double[]> timeline() {
        final double seconds = loadSeconds();
        final List<double[]> steps = new ArrayList<>();
        for (final Map.Entry<Integer, Step> entry : new TreeMap<>(timeline).entrySet()) {
            final Step step = entry.getValue();
            final double begin = entry.getKey() * (double) STEP_SECONDS;
            final double length = Math.clamp(seconds - begin, 0.001, STEP_SECONDS);
            final double[] values = new double[3 + (Kind.values().length * 3)];
            values[0] = begin + length;
            int i = 1;
            for (final Histogram histogram : step.histograms) {
                values[i++] = (double) histogram.total.sum();
                values[i++] = histogram.percentile(0.50) / 1e3;
                values[i++] = histogram.percentile(0.90) / 1e3;
            }
            values[i++] = (double) step.failed.sum();
            values[i] = length;
            steps.add(values);
        }
        return steps;
    }

    /**
     * The latency of a kind over the whole load.
     *
     * @return The count, p50 and p90 in milliseconds
     */
    double[] overall(final Kind kind) {
        final Histogram histogram = whole.histograms[kind.ordinal()];
        return new double[] { (double) histogram.total.sum(), histogram.percentile(0.50) / 1e3, histogram.percentile(0.90) / 1e3 };
    }

    /** The table of operations, by name. */
    List<Row> rows() {
        final double seconds = seconds();
        final List<Row> rows = new ArrayList<>();
        for (final Operation op : new TreeMap<>(operations).values()) {
            final long n = op.count.sum();
            final boolean timed = op.nanos.sum() > 0;
            rows.add(new Row(op.name, n, n / seconds, op.expected.sum(), op.failed.sum(), timed,
                    timed ? op.histogram.percentile(0.50) / 1e3 : 0, timed ? op.histogram.percentile(0.90) / 1e3 : 0,
                    timed ? op.histogram.percentile(0.99) / 1e3 : 0, timed ? op.histogram.max() / 1e3 : 0,
                    timed ? op.nanos.sum() / 1e6 / n : 0, op.sent.sum() / 1048576.0, op.received.sum() / 1048576.0));
        }
        return rows;
    }

    /** How long the load was measured, in seconds. */
    double seconds() {
        return Math.max(1, ((stopped == 0) ? System.nanoTime() : stopped) - started) / 1e9;
    }

    /** The failures and expected outcomes kept as examples, per operation. */
    List<String> samples() {
        final List<String> samples = new ArrayList<>();
        for (final Operation op : new TreeMap<>(operations).values()) {
            for (final String sample : op.samples) {
                samples.add(op.name + ": " + sample);
            }
        }
        return samples;
    }

    /** A finding outside the operations, such as a consistency check. */
    void note(final String text) {
        notes.add(text);
    }

    long failed() {
        return operations.values().stream().mapToLong(op -> op.failed.sum()).sum();
    }

    long total() {
        return operations.values().stream().mapToLong(op -> op.count.sum()).sum();
    }

    List<String> notes() {
        return new ArrayList<>(notes);
    }

    String report() {
        final double seconds = seconds();
        final StringBuilder out = new StringBuilder(8192);
        out.append(String.format(Locale.ROOT, "%-34s %8s %7s %6s %6s %8s %8s %8s %8s %8s %9s %9s%n",
                "operation", "count", "per s", "exp", "FAILED", "p50 ms", "p90 ms", "p99 ms", "max ms", "mean ms", "sent MiB", "recv MiB"));
        long count = 0;
        long expected = 0;
        long failed = 0;
        for (final Row row : rows()) {
            count += row.count();
            expected += row.expected();
            failed += row.failed();
            if (!row.timed()) {
                // A check of an answer, or a frame heard: counted, not timed.
                out.append(String.format(Locale.ROOT, "%-34s %8d %7.1f %6d %6d %8s %8s %8s %8s %8s %9s %9.2f%n",
                        row.name(), row.count(), row.perSecond(), row.expected(), row.failed(), "-", "-", "-", "-", "-", "-", row.receivedMiB()));
                continue;
            }
            out.append(String.format(Locale.ROOT, "%-34s %8d %7.1f %6d %6d %8.1f %8.1f %8.1f %8.1f %8.1f %9.2f %9.2f%n",
                    row.name(), row.count(), row.perSecond(), row.expected(), row.failed(),
                    row.p50(), row.p90(), row.p99(), row.max(), row.mean(), row.sentMiB(), row.receivedMiB()));
        }
        out.append(String.format(Locale.ROOT, "%-34s %8d %7.1f %6d %6d%n", "all", count, count / seconds, expected, failed));
        out.append(String.format(Locale.ROOT, "%nMeasured over %.1f s.%n", seconds));

        final List<String> samples = samples();
        if (!samples.isEmpty()) {
            out.append(String.format("%nExamples of failed and expected outcomes (at most %d per operation):%n", SAMPLES_PER_OPERATION * 2));
            samples.forEach(sample -> out.append("  ").append(sample).append('\n'));
        }
        if (!notes.isEmpty()) {
            out.append(String.format("%nNotes:%n"));
            notes.forEach(note -> out.append("  ").append(note).append('\n'));
        }
        return out.toString();
    }

    /** One step of the timeline: the latencies of each kind, and the failures. */
    private static final class Step {

        private final Histogram[] histograms = new Histogram[Kind.values().length];
        private final LongAdder failed = new LongAdder();

        private Step() {
            clear();
        }

        private void clear() {
            for (int i = 0; i < histograms.length; i++) {
                histograms[i] = new Histogram();
            }
            failed.reset();
        }
    }

    private static final class Operation {

        private final String name;
        private final LongAdder count = new LongAdder();
        private final LongAdder expected = new LongAdder();
        private final LongAdder failed = new LongAdder();
        private final LongAdder nanos = new LongAdder();
        private final LongAdder sent = new LongAdder();
        private final LongAdder received = new LongAdder();
        private final Histogram histogram = new Histogram();
        private final Queue<String> samples = new ConcurrentLinkedQueue<>();
        private final AtomicInteger failedSamples = new AtomicInteger();
        private final AtomicInteger expectedSamples = new AtomicInteger();

        private Operation(final String name) {
            this.name = name;
        }

        private void add(final Outcome outcome, final long time, final long bytesSent, final long bytesReceived, final String detail) {
            count.increment();
            nanos.add(time);
            sent.add(bytesSent);
            received.add(bytesReceived);
            histogram.add(time);
            switch (outcome) {
                case EXPECTED -> {
                    expected.increment();
                    if ((detail != null) && (expectedSamples.getAndIncrement() < SAMPLES_PER_OPERATION)) {
                        samples.add("expected: " + detail);
                    }
                }
                case FAILED -> {
                    failed.increment();
                    if (failedSamples.getAndIncrement() < SAMPLES_PER_OPERATION) {
                        samples.add("FAILED: " + detail);
                    }
                }
                default -> {
                    // Nothing more to note.
                }
            }
        }
    }

    /**
     * Latencies in microseconds, in buckets of about 6 % width: 16 linear
     * buckets per power of two, so percentiles need no list of every value.
     */
    private static final class Histogram {

        private static final int SUB = 16;
        private static final int SIZE = SUB + (64 - 4) * SUB;
        private final AtomicLongArray buckets = new AtomicLongArray(SIZE);
        private final LongAdder total = new LongAdder();
        private volatile long highest = 0;

        private void add(final long nanos) {
            final long micros = Math.max(0, nanos / 1000);
            buckets.incrementAndGet(index(micros));
            total.increment();
            if (nanos > highest) {
                synchronized (this) {
                    highest = Math.max(highest, nanos);
                }
            }
        }

        private static int index(final long value) {
            if (value < SUB) {
                return (int) value;
            }
            final int exponent = 63 - Long.numberOfLeadingZeros(value);
            final int sub = (int) ((value >>> (exponent - 4)) & (SUB - 1));
            return SUB + ((exponent - 4) * SUB) + sub;
        }

        /** The upper end of a bucket, in microseconds. */
        private static long upper(final int index) {
            if (index < SUB) {
                return index;
            }
            final int exponent = ((index - SUB) / SUB) + 4;
            final long sub = (index - SUB) % SUB;
            return ((SUB + sub + 1) << (exponent - 4)) - 1;
        }

        /** The percentile, in microseconds. */
        private double percentile(final double fraction) {
            final long n = total.sum();
            if (n == 0) {
                return 0;
            }
            final long rank = (long) Math.ceil(fraction * n);
            long seen = 0;
            for (int i = 0; i < SIZE; i++) {
                seen += buckets.get(i);
                if (seen >= rank) {
                    return Math.min((double) upper(i), max());
                }
            }
            return max();
        }

        private double max() {
            return highest / 1e3;
        }
    }
}
