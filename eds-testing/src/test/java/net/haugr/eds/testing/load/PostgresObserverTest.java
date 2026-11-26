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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The checks of {@link PostgresObserver} before it changes anything, the
 * settings it sets and sets back, and its summary; with a shell of the test
 * instead of podman.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class PostgresObserverTest {

    private static final String URL = "jdbc:postgresql://127.0.0.1:55432/eds_load?sslmode=disable";

    @TempDir
    Path target;

    /** Answers like podman with a running PostgreSQL container, and remembers every command and its input. */
    private static final class FakeDocker implements PostgresObserver.Shell {
        final List<String> calls = new ArrayList<>();
        String running = "true";
        String port = "0.0.0.0:55432\n[::]:55432\n";
        String superuser = "t";
        String previous = "";
        String startTime = "1795560000.123456";
        Predicate<String> failing = _ -> false;

        @Override
        public PostgresObserver.Output run(final List<String> command, final String input) {
            calls.add(String.join(" ", command) + " <<< " + input);
            if (command.contains("inspect")) {
                return new PostgresObserver.Output("true".equals(running) ? 0 : 1, running + "\n");
            }
            if (command.contains("port")) {
                return new PostgresObserver.Output(port.isEmpty() ? 1 : 0, port);
            }
            if (command.contains("logs")) {
                return new PostgresObserver.Output(0, """
                        2026-11-25 10:00:01.000 UTC [77] LOG:  process 77 still waiting for ShareLock on transaction 9 after 200.1 ms
                        2026-11-25 10:00:01.000 UTC [77] DETAIL:  Process holding the lock: 78. Wait queue: 77.
                        2026-11-25 10:00:01.000 UTC [77] STATEMENT:  select ce1_0.id from eds_circles ce1_0 where ce1_0.id=$1 for no key update of ce1_0
                        2026-11-25 10:00:01.500 UTC [77] LOG:  process 77 acquired ShareLock on transaction 9 after 700.5 ms
                        2026-11-25 10:00:01.500 UTC [77] STATEMENT:  select ce1_0.id from eds_circles ce1_0 where ce1_0.id=$1 for no key update of ce1_0
                        2026-11-25 10:00:02.000 UTC [80] LOG:  checkpoint starting: time
                        """);
            }
            if (failing.test(input)) {
                return new PostgresObserver.Output(3, "ERROR:  it failed");
            }
            if (input.contains("pg_postmaster_start_time")) {
                return new PostgresObserver.Output(0, startTime + "\n");
            }
            if (input.contains("rolsuper")) {
                return new PostgresObserver.Output(0, superuser + "\n");
            }
            if (input.contains("pg_db_role_setting")) {
                return new PostgresObserver.Output(0, previous);
            }
            if (input.contains("'table'")) {
                final String label = input.substring(input.indexOf("SELECT '") + 8, input.indexOf("', extract"));
                final long n = "start".equals(label) ? 0 : 1;
                return new PostgresObserver.Output(0, label + "|1000|table|eds_data|" + (2 * n) + "|" + (500 * n) + "|1|1|0|0|0|0|0|0|0\n"
                        + label + "|1000|database|" + (100 * n) + "|" + n + "|" + (10 * n) + "|" + (90 * n) + "|0|0|0|" + (12.5 * n) + "|0.0|0|0\n");
            }
            return new PostgresObserver.Output(0, "");
        }

        boolean ran(final String text) {
            return calls.stream().anyMatch(call -> call.contains(text));
        }
    }

    private PostgresObserver observer(final FakeDocker docker, final String url) {
        return observer(docker, url, "1795560000.123456");
    }

    private PostgresObserver observer(final FakeDocker docker, final String url, final String startTimeThroughUrl) {
        return new PostgresObserver(docker, (_, output) -> {
            Files.writeString(output, """
                    1000|0|sample||||-1|
                    1000|41|active|Lock|tuple|42|350|select ce1_0.id from eds_circles ce1_0 where ce1_0.id=$1 for no key update of ce1_0
                    1000|42|idle in transaction|Client|ClientRead||900|UPDATE eds_circle_storage SET stored_bytes = stored_bytes + $1
                    """);
            return new ProcessBuilder("cat").redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        }, () -> startTimeThroughUrl, "podman", "eds-load-pg", "postgres", url, target, Duration.ofMinutes(60));
    }

    /** The start is refused, and nothing was changed before. */
    private static void assertRefused(final FakeDocker docker, final PostgresObserver observer) {
        assertThrows(IllegalStateException.class, () -> PostgresObserver.begun(observer));
        assertFalse(docker.ran("ALTER DATABASE"));
    }

    @Test
    void testWithoutAContainerNothingIsWatched() throws IOException, InterruptedException {
        assertNull(PostgresObserver.start(target, null));
        assertNull(PostgresObserver.start(target, " "));
    }

    @Test
    void testPodmanIsPreferredToDocker() throws IOException {
        final Path withPodman = Files.createDirectories(target.resolve("bin"));
        final Path podman = Files.createFile(withPodman.resolve("podman"));
        assertTrue(podman.toFile().setExecutable(true));
        assertEquals("podman", PostgresObserver.containerCommand("/nowhere" + java.io.File.pathSeparator + withPodman));
        assertEquals("docker", PostgresObserver.containerCommand("/nowhere"));
        assertEquals("docker", PostgresObserver.containerCommand(null));
    }

    @Test
    void testOnlyALocalPostgreSQLIsTaken() {
        final FakeDocker docker = new FakeDocker();
        assertThrows(IllegalStateException.class, () -> observer(docker, null));
        assertThrows(IllegalStateException.class, () -> observer(docker, "jdbc:h2:mem:eds"));
        assertThrows(IllegalStateException.class, () -> observer(docker, "jdbc:postgresql://db.example.com:5432/eds"));
        final Duration hour = Duration.ofMinutes(60);
        final Duration lessThanAMinute = Duration.ofSeconds(59);
        assertThrows(IllegalStateException.class, () -> new PostgresObserver(docker, null, null, "podman", "--privileged", "postgres", URL,
                target, hour));
        assertThrows(IllegalStateException.class, () -> new PostgresObserver(docker, null, null, "podman", "eds-load-pg", "postgres", URL,
                target, lessThanAMinute));
        assertEquals("eds_load", observer(docker, URL).database());
        assertEquals("eds", observer(docker, "jdbc:postgresql://localhost/eds").database());
        assertTrue(docker.calls.isEmpty());
    }

    @Test
    void testNothingIsChangedUnlessTheContainerRunsAndPublishesThePort() {
        final FakeDocker stopped = new FakeDocker();
        stopped.running = "false";
        assertRefused(stopped, observer(stopped, URL));

        final FakeDocker otherPort = new FakeDocker();
        otherPort.port = "0.0.0.0:5432\n";
        assertRefused(otherPort, observer(otherPort, URL));

        final FakeDocker notPublished = new FakeDocker();
        notPublished.port = "";
        assertRefused(notPublished, observer(notPublished, URL));

        final FakeDocker otherAddress = new FakeDocker();
        otherAddress.port = "192.168.1.5:55432\n";
        assertRefused(otherAddress, observer(otherAddress, URL));

        final FakeDocker otherServer = new FakeDocker();
        assertRefused(otherServer, observer(otherServer, URL, "1795569999.5"));

        final FakeDocker noSuperuser = new FakeDocker();
        noSuperuser.superuser = "f";
        assertRefused(noSuperuser, observer(noSuperuser, URL));
    }

    @Test
    void testTheSettingsAreSetAndSetBack() throws IOException, InterruptedException {
        final FakeDocker docker = new FakeDocker();
        docker.previous = "deadlock_timeout=1s\nwork_mem=64MB\n";
        final PostgresObserver observer = PostgresObserver.begun(observer(docker, URL));
        assertTrue(docker.ran("podman exec -i -e PGOPTIONS=-c lock_timeout=30s -c statement_timeout=90s -e PGAPPNAME=eds-load-observer"
                + " eds-load-pg psql -U postgres -d eds_load"));
        assertTrue(docker.ran("ALTER DATABASE \"eds_load\" SET log_lock_waits = on;"));
        assertTrue(docker.ran("ALTER DATABASE \"eds_load\" SET deadlock_timeout = '200ms';"));
        assertTrue(docker.ran("ALTER DATABASE \"eds_load\" SET track_io_timing = on;"));
        assertTrue(docker.ran("SELECT pg_stat_reset();"));
        assertFalse(docker.ran("RESET"));

        final String summary = observer.finish();
        assertTrue(docker.ran("ALTER DATABASE \"eds_load\" RESET log_lock_waits;"));
        assertTrue(docker.ran("ALTER DATABASE \"eds_load\" SET deadlock_timeout = '1s';"));
        assertTrue(docker.ran("ALTER DATABASE \"eds_load\" RESET track_io_timing;"));
        assertFalse(docker.ran("work_mem"));
        assertTrue(docker.ran("pg_terminate_backend(pid) FROM pg_stat_activity WHERE application_name = 'eds-load-sampler'"));
        final int calls = docker.calls.size();
        observer.close();
        assertEquals(calls, docker.calls.size(), "set back once");

        assertTrue(summary.contains("Lock waits over 200 ms (log_lock_waits, load-pg-lockwaits.log): 1, deadlocks 0"), summary);
        assertTrue(summary.contains("eds_data 500"), summary);
        assertEquals(summary, Files.readString(target.resolve(PostgresObserver.SUMMARY)));
        assertFalse(Files.readString(target.resolve(PostgresObserver.LOCK_WAITS)).contains("checkpoint"));
    }

    @Test
    void testTheSettingsAreSetBackWhenTheStartFails() {
        final FakeDocker docker = new FakeDocker();
        docker.failing = input -> input.contains("pg_stat_reset");
        final PostgresObserver observer = observer(docker, URL);
        assertThrows(IllegalStateException.class, () -> PostgresObserver.begun(observer));
        assertTrue(docker.ran("ALTER DATABASE \"eds_load\" SET log_lock_waits = on;"));
        assertTrue(docker.ran("ALTER DATABASE \"eds_load\" RESET log_lock_waits;"));
        assertTrue(docker.ran("ALTER DATABASE \"eds_load\" RESET deadlock_timeout;"));
        assertTrue(docker.ran("ALTER DATABASE \"eds_load\" RESET track_io_timing;"));
    }

    @Test
    void testAFailedTestStopsTheSamplerAndSetsTheSettingsBack() throws IOException, InterruptedException {
        final FakeDocker docker = new FakeDocker();
        final PostgresObserver observer = PostgresObserver.begun(observer(docker, URL));
        final long terminated = docker.calls.stream().filter(call -> call.contains("pg_terminate_backend")).count();
        observer.close();
        assertEquals(terminated + 1, docker.calls.stream().filter(call -> call.contains("pg_terminate_backend")).count());
        assertTrue(docker.ran("ALTER DATABASE \"eds_load\" RESET log_lock_waits;"));
    }

    @Test
    void testTheSettingsAreTriedAgainWhenSettingThemBackFails() throws IOException, InterruptedException {
        final FakeDocker docker = new FakeDocker();
        final PostgresObserver observer = PostgresObserver.begun(observer(docker, URL));
        docker.failing = input -> input.contains("RESET");
        assertThrows(IllegalStateException.class, observer::close);
        docker.failing = _ -> false;
        docker.calls.clear();
        observer.close();
        assertTrue(docker.ran("ALTER DATABASE \"eds_load\" RESET log_lock_waits;"));
    }

    @Test
    void testTheActivityIsSummedByHour() {
        final List<String> lines = new ArrayList<>(List.of("garbage", ""));
        // Two samples in the first hour, one in the second.
        lines.add("1000|0|sample||||-1|");
        lines.add("1000|41|active|Lock|tuple|42|350|select a for update");
        lines.add("1000|42|active|||||update b");
        lines.add("1001|0|sample||||-1|");
        lines.add("1001|41|active|Lock|tuple|42|350|select a for update");
        lines.add("1001|43|idle in transaction|Client|ClientRead||10|commit");
        lines.add("4600|0|sample||||-1|");
        lines.add("4600|44|active|IO|DataFileRead||5|select c | d");
        final String text = PostgresObserver.activity(lines);
        assertTrue(text.contains("  1            2      1.5        1.0        1.0        0.5  Lock/tuple 1.0"), text);
        assertTrue(text.contains("  2            1      1.0        0.0        0.0        0.0  IO/DataFileRead 1.0"), text);
        assertTrue(text.contains("         2  select a for update"), text);
    }

    @Test
    void testTheSnapshotsAreTakenApart() {
        final String text = PostgresObserver.snapshots(List.of(
                "start|1|table|eds_data|0|0|0|0|0|0|0|0|0|0|0",
                "start|1|table|eds_keys|0|0|0|0|0|0|0|0|0|0|0",
                "start|1|database|0|0|0|0|0|0|0|0.0|0.0|0|0",
                "60m|2|table|eds_data|4|4000|0|0|0|0|0|0|0|0|0",
                "60m|2|table|eds_keys|9|90|0|0|0|0|0|0|0|0|0",
                "60m|2|database|1000|2|25|75|1|2097152|1|30.0|0.0|0|0",
                "end|3|table|eds_data|4|4000|0|0|0|0|0|0|0|0|0",
                "end|3|table|eds_keys|10|100|0|0|0|0|0|0|0|0|0",
                "end|3|database|1500|2|25|175|1|2097152|1|30.0|0.0|0|0"));
        assertTrue(text.contains("  start..60m         1000         2   75.0         30       2.0         1  eds_data 4000, eds_keys 90"), text);
        assertTrue(text.contains("  60m..end            500         0  100.0          0       0.0         0  eds_keys 10"), text);
    }

    @Test
    void testTheLockWaitsAreCounted() {
        final String text = PostgresObserver.lockWaits(List.of(
                "LOG:  process 1 still waiting for ShareLock on transaction 9 after 200.1 ms",
                "STATEMENT:  select a\tfor update",
                "LOG:  process 2 still waiting for ShareLock on transaction 9 after 200.2 ms",
                "STATEMENT:  select a for update",
                "LOG:  process 1 acquired ShareLock on transaction 9 after 300.0 ms",
                "STATEMENT:  select a for update",
                "LOG:  process 2 acquired ShareLock on transaction 9 after 900.0 ms",
                "ERROR:  deadlock detected"));
        assertTrue(text.contains(": 2, deadlocks 1; of 2 which got their lock, the wait was 600 ms as a mean, 900 at most."), text);
        assertTrue(text.contains("         2  select a for update"), text);
    }
}
