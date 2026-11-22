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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import net.haugr.eds.api.dtos.StatusSample;
import net.haugr.eds.api.dtos.StatusSeries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

/**
 * The gathering for the status of a stone (STATUS.md): what is counted,
 * what a sample holds, and the three series.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class StatusCollectorTest {

    private static final long SECOND = 1_000_000_000L;
    private final StatusCollector collector = StatusCollector.getInstance();

    @BeforeEach
    @AfterEach
    void forget() {
        collector.reset();
    }

    @Test
    void testASampleHoldsWhatHappenedSinceTheLast() {
        assertTrue(collector.loadDue(SECOND, 5));
        collector.request(2_000_000, false, 100, 1000);
        collector.request(4_000_000, true, -1, 500);
        collector.request(0, false, 0, -1);
        collector.active(7L);
        collector.active(7L);
        collector.active(9L);
        collector.active(null);
        collector.database(1.5);
        collector.connections(() -> 3);

        final Map<String, Double> values = collector.sampleLoad(SECOND).getValues();

        assertEquals(3.0, values.get("requests"));
        assertEquals(1.0, values.get("failed"));
        assertEquals(100.0, values.get("bytesIn"));
        assertEquals(1500.0, values.get("bytesOut"));
        assertEquals(2.0, values.get("requestMillis"));
        assertEquals(4.0, values.get("requestMillisMax"));
        assertEquals(1.5, values.get("databaseMillis"));
        assertEquals(2.0, values.get("accounts"));
        assertEquals(3.0, values.get("connections"));
        assertTrue(values.get("processors") >= 1);
        assertTrue(values.get("memoryUsed") > 0);
        assertTrue(values.get("memoryMax") >= values.get("memoryUsed"));
        assertTrue(values.get("threads") >= 1);
        // The share of the processors needs two samples.
        assertFalse(values.containsKey("processShare"));

        // The next sample starts from nothing; who was active is forgotten.
        final Map<String, Double> next = collector.sampleLoad(6 * SECOND).getValues();
        assertEquals(0.0, next.get("requests"));
        assertEquals(0.0, next.get("failed"));
        assertEquals(0.0, next.get("requestMillis"));
        assertEquals(0.0, next.get("requestMillisMax"));
        assertEquals(0.0, next.get("accounts"));
        final Double processShare = next.get("processShare");
        if (processShare != null) {
            assertTrue((processShare >= 0) && (processShare <= 1));
        }
    }

    @Test
    void testWhenASampleIsDue() {
        // Switched off.
        assertFalse(collector.loadDue(SECOND, 0));
        // The first one at once, the next after the interval.
        assertTrue(collector.loadDue(SECOND, 5));
        collector.sampleLoad(SECOND);
        assertFalse(collector.loadDue(3 * SECOND, 5));
        assertTrue(collector.loadDue(6 * SECOND, 5));

        assertTrue(collector.inventoryDue(SECOND, 3600));
        collector.sampleInventory(SECOND, Map.of("files", 1.0));
        assertFalse(collector.inventoryDue(3599 * SECOND, 3600));
        assertTrue(collector.inventoryDue(3601 * SECOND, 3600));
        // After a failure it is tried again in a minute, not in an hour.
        collector.inventoryFailed(3601 * SECOND);
        assertFalse(collector.inventoryDue(3630 * SECOND, 3600));
        assertTrue(collector.inventoryDue(3662 * SECOND, 3600));
    }

    @Test
    void testOnlyWhatIsNewIsTold() {
        collector.loadDue(SECOND, 1);
        for (int i = 1; i <= 5; i++) {
            collector.request(1_000_000, false, 0, 0);
            collector.sampleLoad(i * SECOND);
        }

        final StatusSeries all = collector.load(null);
        assertEquals(1L, all.getInterval());
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L), all.getSamples().stream().map(StatusSample::getSequence).toList());
        assertEquals(List.of(4L, 5L), collector.load(3L).getSamples().stream().map(StatusSample::getSequence).toList());
        assertTrue(collector.load(5L).getSamples().isEmpty());
        assertNotNull(all.getSamples().getFirst().getTime());
    }

    @Test
    void testTheRecentLoadIsBounded() {
        collector.loadDue(SECOND, 1);
        for (int i = 1; i <= (StatusCollector.RECENT_SAMPLES + 10); i++) {
            collector.sampleLoad(i * SECOND);
        }

        final List<StatusSample> samples = collector.load(null).getSamples();
        assertEquals(StatusCollector.RECENT_SAMPLES, samples.size());
        assertEquals(11L, samples.getFirst().getSequence());
    }

    @Test
    void testAnotherIntervalStartsTheRecentLoadAnew() {
        collector.loadDue(SECOND, 5);
        collector.sampleLoad(SECOND);
        collector.sampleLoad(6 * SECOND);
        assertEquals(2, collector.load(null).getSamples().size());

        assertTrue(collector.loadDue(7 * SECOND, 1));
        assertTrue(collector.load(null).getSamples().isEmpty());
        assertEquals(1L, collector.load(null).getInterval());
        // The sequence goes on, so a client isn't told an old number twice.
        assertEquals(3L, collector.sampleLoad(7 * SECOND).getSequence());
    }

    @Test
    void testTheDayAddsUpItsSamples() {
        collector.loadDue(SECOND, 1);
        collector.connections(() -> 2);
        collector.request(2_000_000, false, 10, 20);
        collector.request(6_000_000, true, 10, 20);
        collector.active(1L);
        collector.database(1.0);
        collector.sampleLoad(SECOND);
        collector.connections(() -> 5);
        collector.request(1_000_000, false, 10, 20);
        collector.database(3.0);
        collector.sampleLoad(2 * SECOND);

        final StatusSeries day = collector.day();
        assertEquals(StatusCollector.DAY_STEP_SECONDS, day.getInterval());
        // Both samples fall into the step which is being filled, unless the
        // clock just passed a step.
        final Map<String, Double> sum = new java.util.HashMap<>();
        double samples = 0;
        for (final StatusSample step : day.getSamples()) {
            samples += step.getValues().get("samples");
            sum.merge("requests", step.getValues().get("requests"), Double::sum);
            sum.merge("failed", step.getValues().get("failed"), Double::sum);
            sum.merge("bytesOut", step.getValues().get("bytesOut"), Double::sum);
            sum.merge("connections", step.getValues().get("connections"), Math::max);
            sum.merge("requestMillisMax", step.getValues().get("requestMillisMax"), Math::max);
            sum.merge("databaseMillisMax", step.getValues().get("databaseMillisMax"), Math::max);
        }
        // A count of whole samples, which a double holds exactly.
        assertEquals(2.0, samples, 0.0);
        assertEquals(3.0, sum.get("requests"));
        assertEquals(1.0, sum.get("failed"));
        assertEquals(60.0, sum.get("bytesOut"));
        assertEquals(5.0, sum.get("connections"));
        assertEquals(6.0, sum.get("requestMillisMax"));
        assertEquals(3.0, sum.get("databaseMillisMax"));
        if (day.getSamples().size() == 1) {
            final Map<String, Double> step = day.getSamples().getFirst().getValues();
            // The average of the requests, not of the samples: (2 + 6 + 1) / 3.
            assertEquals(3.0, step.get("requestMillis"), 0.0001);
            assertEquals(2.0, step.get("databaseMillis"), 0.0001);
            assertEquals(1.0, step.get("accounts"));
        }
    }

    /**
     * Gathering must be cheap enough for a step of one second (STATUS.md):
     * counting a request and taking a sample are measured here, with a
     * generous bound, and the numbers are printed for the record.
     */
    @Test
    void testGatheringIsCheap(final TestReporter reporter) {
        collector.loadDue(SECOND, 1);
        collector.connections(() -> 100);
        // Warm up, then measure.
        for (int i = 0; i < 20_000; i++) {
            collector.request(1_000_000, (i % 10) == 0, 500, 5000);
            collector.active((long) (i % 500));
        }
        for (int i = 0; i < 2_000; i++) {
            collector.sampleLoad((i + 1) * SECOND);
        }

        // Each cost is measured in rounds, and the cheapest round counts:
        // a round slowed by something else on the machine (another build, a
        // garbage collection) says nothing about the cost itself.
        final int rounds = 5;
        final int counted = 200_000;
        double perRequest = Double.MAX_VALUE;
        for (int round = 0; round < rounds; round++) {
            final long start = System.nanoTime();
            for (int i = 0; i < counted; i++) {
                collector.request(1_000_000, (i % 10) == 0, 500, 5000);
                collector.active((long) (i % 500));
            }
            perRequest = Math.min(perRequest, (System.nanoTime() - start) / (double) counted);
        }

        final int sampled = 1_000;
        double perSample = Double.MAX_VALUE;
        long second = 2_001;
        for (int round = 0; round < rounds; round++) {
            final long start = System.nanoTime();
            for (int i = 0; i < sampled; i++) {
                for (int j = 0; j < 200; j++) {
                    collector.active((long) j);
                }
                collector.sampleLoad(second++ * SECOND);
            }
            perSample = Math.min(perSample, (System.nanoTime() - start) / (double) sampled);
        }
        reporter.publishEntry("Status cost", String.format("%.0f ns per counted request, %.1f microseconds per sample of the load", perRequest, perSample / 1000));

        // A request is counted on every call: a few counters and the set of
        // active accounts, about a microsecond on a slow virtual machine. 10
        // microseconds would be a hundredth of the cheapest request (a
        // millisecond), so more means something went wrong.
        // A sample reads the figures of the operating system (the process'
        // processor time, the load, the threads), which take about a
        // millisecond on a slow virtual machine; samples come at most once a
        // second, so 10 milliseconds would be a hundredth of one processor.
        assertTrue(perRequest < 10_000, "per request: " + perRequest + " ns");
        assertTrue(perSample < 10_000_000, "per sample: " + perSample + " ns");
    }

    @Test
    void testTheInventoryKeepsItsSamples() {
        assertTrue(collector.inventory(false).getSamples().isEmpty());
        collector.inventoryDue(SECOND, 3600);
        collector.sampleInventory(SECOND, Map.of("files", 1.0));
        collector.sampleInventory(2 * SECOND, Map.of("files", 2.0));

        assertEquals(3600L, collector.inventory(false).getInterval());
        assertEquals(1, collector.inventory(false).getSamples().size());
        assertEquals(2.0, collector.inventory(false).getSamples().getFirst().getValues().get("files"));
        assertEquals(2, collector.inventory(true).getSamples().size());
        assertEquals(1.0, collector.inventory(true).getSamples().getFirst().getValues().get("files"));
        assertNotNull(collector.started());
    }

    @Test
    void testTheInventoryIsBounded() {
        collector.inventoryDue(SECOND, 60);
        for (int i = 0; i <= StatusCollector.INVENTORY_SAMPLES; i++) {
            collector.sampleInventory((i + 1) * SECOND, Map.of("files", (double) i));
        }

        final List<StatusSample> samples = collector.inventory(true).getSamples();
        assertEquals(StatusCollector.INVENTORY_SAMPLES, samples.size());
        // The oldest went.
        assertEquals(2L, samples.getFirst().getSequence());
        assertEquals((double) StatusCollector.INVENTORY_SAMPLES, samples.getLast().getValues().get("files"));
    }

    @Test
    void testWithoutASourceThereAreNoConnections() {
        collector.loadDue(SECOND, 1);
        collector.connections(() -> 3);
        collector.connections(null);

        final StatusSample first = collector.sampleLoad(SECOND);
        assertEquals(0.0, first.getValues().get("connections"));
        // The share of the processors needs time between two samples.
        assertFalse(first.getValues().containsKey("processShare"));
        assertFalse(collector.sampleLoad(SECOND).getValues().containsKey("processShare"));
    }
}
