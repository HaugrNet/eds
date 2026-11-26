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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * <p>The load test against a stone of its own, with {@code mvn -Pload
 * verify} in eds-testing (build eds-quarkus first). The stone is started
 * unlocked from a MasterKey file, loaded, checked and stopped; the report
 * is in {@code target/load-report.txt}, the stone's log in
 * {@code target/load-stone.log}.</p>
 *
 * <p>It passes when no request failed, the stone holds exactly what the
 * users left in it, it is still alive and ready, and its log has no ERROR.
 * The size of the run is set with the properties of {@link LoadSettings};
 * with {@code eds.load.url} it loads that stone instead. With
 * {@code eds.load.pg.container} the PostgreSQL database in that container
 * is watched meanwhile ({@link PostgresObserver}).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class StoneLoadIT {

    private static final Path TARGET = Path.of("target");

    @Test
    void testTheStoneUnderLoad() throws Exception {
        final LoadSettings settings = LoadSettings.fromSystemProperties();
        if (settings.url() != null) {
            final LoadRun run = new LoadRun(settings);
            final LoadRun.Result result = run.run();
            LoadRun.save(result, TARGET.resolve("load-report.txt"));
            Files.writeString(TARGET.resolve("load-report.html"), run.html(result, List.of(verdict(result))));
            assertTrue(result.failed() <= settings.maxFailures(), result.failed() + " requests failed, see target/load-report.txt");
            return;
        }

        // The observer first, as the settings it gives the database apply to connections opened after.
        try (PostgresObserver database = PostgresObserver.start(TARGET); TestStone stone = TestStone.start(TARGET)) {
            final LoadRun run = new LoadRun(settings.withUrl(stone.url()));
            final LoadRun.Result result = run.run();
            final List<String> errors = stone.errors();
            final StringBuilder report = new StringBuilder(result.report());
            // If the observer fails, the report of the load is still saved, and the test fails after.
            Exception observerFailure = null;
            if (database != null) {
                try {
                    report.append(database.finish());
                } catch (IOException | RuntimeException e) {
                    observerFailure = e;
                    report.append(String.format("%nWatching PostgreSQL failed: %s%n", e));
                }
            }
            report.append(String.format("%nThe stone: %s, %s; %d lines at ERROR in %s%n", stone.alive() ? "alive" : "DEAD",
                    stone.ready() ? "ready" : "NOT READY", errors.size(), stone.log().toAbsolutePath()));
            errors.stream().limit(20).forEach(line -> report.append("  ").append(line).append('\n'));
            if (database != null) {
                report.append("PostgreSQL's database ").append(database.database()).append(" was watched: ")
                        .append(TARGET.resolve(PostgresObserver.SUMMARY).toAbsolutePath()).append(" and the files beside it.\n");
            }
            print(report.substring(result.report().length()));
            final boolean alive = stone.alive();
            final boolean ready = stone.ready();
            final List<String> verdict = new ArrayList<>(List.of(verdict(result),
                    String.format("The stone: %s, %s, %d lines at ERROR.", alive ? "alive" : "DEAD", ready ? "ready" : "NOT READY", errors.size())));
            if (TestStone.recording()) {
                // Written when the stone stops, at the end of this block.
                final String recording = "Its Flight Recording: " + TARGET.resolve(TestStone.RECORDING).toAbsolutePath() + " (JDK Mission Control).";
                report.append(recording).append('\n');
                verdict.add(recording);
            }
            final LoadRun.Result whole = new LoadRun.Result(report.toString(), result.failed(), result.total());
            LoadRun.save(whole, TARGET.resolve("load-report.txt"));
            Files.writeString(TARGET.resolve("load-report.html"), run.html(whole, verdict));

            assertTrue(alive, "The stone died under the load, see " + stone.log().toAbsolutePath());
            assertTrue(ready, "The stone isn't ready after the load, see " + stone.log().toAbsolutePath());
            assertTrue(result.failed() <= settings.maxFailures(), result.failed() + " requests failed, see target/load-report.txt");
            assertEquals(List.of(), errors.stream().limit(5).toList(), "The stone logged errors, see " + stone.log().toAbsolutePath());
            assertNull(observerFailure, "Watching PostgreSQL failed, see target/load-report.txt");
        }
    }

    private static void print(final String text) {
        System.out.println(text);
    }

    private static String verdict(final LoadRun.Result result) {
        return (result.failed() == 0) ? "No failures." : result.failed() + " of " + result.total() + " requests failed.";
    }
}
