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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>Watches the PostgreSQL database of the load test while it runs, so a
 * slowdown can be told apart: running, waiting for a lock, or reading from
 * disk. Only for a database in a container, given with
 * {@code -Deds.load.pg.container=<name>}: the observer changes settings of
 * the database and resets its statistics, which it does only where the
 * whole server is a test's own. Before anything is changed it checks that
 * the container is running, that it publishes the port of
 * {@code EDS_DB_URL} on this host, that the server reached through
 * {@code EDS_DB_URL} is the one in the container (the time it started, read
 * both ways, is the same), and that its user is a superuser; if not, the
 * test stops before the load.</p>
 *
 * <ul>
 *   <li>Every second, the connections of the database
 *   ({@code pg_stat_activity}): their state, what they wait for, who blocks
 *   them ({@code pg_blocking_pids}), the age of their transaction and the
 *   start of their statement; {@code target/load-pg-activity.txt}.</li>
 *   <li>{@code log_lock_waits} with a {@code deadlock_timeout} of 200 ms:
 *   PostgreSQL logs every lock wait longer than that, with the holder and the
 *   statement; taken from the container's log at the end,
 *   {@code target/load-pg-lockwaits.log}.</li>
 *   <li>{@code track_io_timing}: the time spent reading and writing blocks,
 *   in {@code pg_stat_database} and in the plans of {@code EXPLAIN}.</li>
 *   <li>A snapshot of {@code pg_stat_user_tables},
 *   {@code pg_statio_user_tables}, {@code pg_stat_user_indexes} and
 *   {@code pg_stat_database} at the start, every
 *   {@code eds.load.pg.snapshot.minutes} (60) and at the end, after
 *   {@code pg_stat_reset()}; {@code target/load-pg-snapshots.txt}.</li>
 * </ul>
 *
 * <p>The three settings are set on the database ({@code ALTER DATABASE}),
 * so they apply to the stone's connections, which it opens after; at the end
 * they are set back to what they were, also when the test fails or the JVM
 * is stopped (a shutdown hook); only a JVM which is killed leaves them, see
 * the README. A summary by hour goes into the report.</p>
 *
 * <p>System properties: {@code eds.load.pg.container}, {@code eds.load.pg.user}
 * (the container's superuser, default {@code postgres}),
 * {@code eds.load.pg.command} (the container command: {@code podman} if it
 * is on the PATH, as it runs without root, else {@code docker}) and {@code eds.load.pg.snapshot.minutes}.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class PostgresObserver implements AutoCloseable {

    static final String ACTIVITY = "load-pg-activity.txt";
    static final String SNAPSHOTS = "load-pg-snapshots.txt";
    static final String LOCK_WAITS = "load-pg-lockwaits.log";
    static final String SUMMARY = "load-pg-summary.txt";

    private static final String OBSERVER = "eds-load-observer";
    private static final String SAMPLER = "eds-load-sampler";
    private static final Pattern DATABASE_URL = Pattern.compile("jdbc:postgresql://(\\[[^]]+]|[^/:?\\[]+)(?::(\\d+))?/(\\w+)(?:\\?.*)?");
    private static final Pattern CONTAINER = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.-]*");
    private static final Set<String> LOCAL = Set.of("localhost", "127.0.0.1", "[::1]");
    /** The addresses of this host a container may publish on. */
    private static final String PUBLISHED = "(127\\.0\\.0\\.1|0\\.0\\.0\\.0|\\[::]|\\[::1]):";
    private static final String START_TIME = "SELECT extract(epoch FROM pg_postmaster_start_time())::text";
    /** The settings, and the values the run gives them. */
    private static final Map<String, String> SETTINGS = settings();
    private static final Duration COMMAND = Duration.ofMinutes(2);
    private static final int TOP = 5;
    private static final int STATEMENT = 120;

    /** Runs a command; for the tests, which give their own. */
    interface Shell {
        Output run(List<String> command, String input) throws IOException, InterruptedException;
    }

    /** The exit code of a command, and what it wrote, out and errors together. */
    record Output(int code, String text) {}

    /** Starts the sampler, writing to the file; for the tests, which give their own. */
    interface Starter {
        Process start(List<String> command, Path output) throws IOException;
    }

    /** The time the server of EDS_DB_URL started, read the way the stone reads it; for the tests, which give their own. */
    interface Identity {
        String startTime() throws SQLException;
    }

    private final Shell shell;
    private final Starter starter;
    private final Identity identity;
    private final String command;
    private final String container;
    private final String user;
    private final String database;
    private final String port;
    private final Path target;
    private final Duration snapshotEvery;
    private final Map<String, String> previous = new LinkedHashMap<>();
    private ScheduledExecutorService snapshots;
    private Process sampler;
    private Instant started;
    private boolean changed;
    private Thread hook;
    private String summary;

    PostgresObserver(final Shell shell, final Starter starter, final Identity identity, final String command, final String container, final String user, final String databaseUrl,
                     final Path target, final Duration snapshotEvery) {
        if ((container == null) || !CONTAINER.matcher(container).matches()) {
            throw new IllegalStateException("eds.load.pg.container must name a container, not '" + container + "'.");
        }
        if (databaseUrl == null) {
            throw new IllegalStateException("eds.load.pg.container needs the stone on PostgreSQL: EDS_DB_URL isn't set.");
        }
        final Matcher url = DATABASE_URL.matcher(databaseUrl.trim());
        if (!url.matches()) {
            throw new IllegalStateException("EDS_DB_URL isn't a PostgreSQL URL of the form jdbc:postgresql://host:port/database.");
        }
        if (!LOCAL.contains(url.group(1).toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("EDS_DB_URL names the host " + url.group(1)
                    + "; the observer only watches a container on this host (localhost).");
        }
        if (snapshotEvery.toMinutes() < 1) {
            throw new IllegalStateException("eds.load.pg.snapshot.minutes must be at least 1.");
        }
        this.shell = shell;
        this.starter = starter;
        this.identity = identity;
        this.command = command;
        this.container = container;
        this.user = user;
        this.port = (url.group(2) == null) ? "5432" : url.group(2);
        this.database = url.group(3);
        this.target = target;
        this.snapshotEvery = snapshotEvery;
    }

    /**
     * Starts watching, if {@code eds.load.pg.container} is given, before the
     * stone is started.
     *
     * @return The observer, or null without the property
     */
    static PostgresObserver start(final Path target) throws IOException, InterruptedException {
        return start(target, System.getProperty("eds.load.pg.container"));
    }

    static PostgresObserver start(final Path target, final String container) throws IOException, InterruptedException {
        if ((container == null) || container.isBlank()) {
            return null;
        }
        return begun(new PostgresObserver(PostgresObserver::process, PostgresObserver::sampler, PostgresObserver::startTime,
                System.getProperty("eds.load.pg.command", containerCommand(System.getenv("PATH"))), container.trim(), System.getProperty("eds.load.pg.user", "postgres"),
                System.getenv("EDS_DB_URL"), target, Duration.ofMinutes(Long.getLong("eds.load.pg.snapshot.minutes", 60))));
    }

    /** Begins, and if that fails, sets back what was changed before the failure. */
    static PostgresObserver begun(final PostgresObserver observer) throws IOException, InterruptedException {
        try {
            observer.begin();
        } catch (IOException | InterruptedException | RuntimeException e) {
            // What was changed before the failure is set back.
            observer.close();
            throw e;
        }
        return observer;
    }

    String database() {
        return database;
    }

    /**
     * Checks that the database is in the container, then changes the
     * settings, resets the statistics, and starts the sampling and the
     * snapshots.
     */
    void begin() throws IOException, InterruptedException {
        verify();
        Files.createDirectories(target);
        for (final String line : psql("SELECT s FROM pg_db_role_setting r, unnest(r.setconfig) s "
                + "WHERE r.setdatabase = (SELECT oid FROM pg_database WHERE datname = current_database()) AND r.setrole = 0")) {
            final int equals = line.indexOf('=');
            if ((equals > 0) && SETTINGS.containsKey(line.substring(0, equals))) {
                previous.put(line.substring(0, equals), line.substring(equals + 1));
            }
        }
        markChanged();
        hook = Thread.ofPlatform().name("pg-observer-shutdown").unstarted(this::closeQuietly);
        Runtime.getRuntime().addShutdownHook(hook);
        final StringBuilder sql = new StringBuilder();
        SETTINGS.forEach((name, value) -> sql.append("ALTER DATABASE \"").append(database).append("\" SET ").append(name)
                .append(" = ").append(value).append(";\n"));
        sql.append("SELECT pg_stat_reset();\n");
        psql(sql.toString());
        started = Instant.now();
        for (final String file : List.of(ACTIVITY, SNAPSHOTS, LOCK_WAITS, SUMMARY)) {
            Files.deleteIfExists(target.resolve(file));
        }
        snapshot("start");
        // A sampler left by a run which was killed would go on, unseen by this one.
        terminateSamplers();
        startSampler();
        snapshots = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("pg-snapshots").daemon().factory());
        final long minutes = snapshotEvery.toMinutes();
        snapshots.scheduleAtFixedRate(new Runnable() {
            private int count;

            @Override
            public void run() {
                count++;
                try {
                    snapshot(count * minutes + "m");
                } catch (IOException | RuntimeException e) {
                    // The next snapshot is tried all the same.
                    System.err.println("The snapshot of PostgreSQL failed: " + e.getMessage());
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                }
            }
        }, minutes, minutes, TimeUnit.MINUTES);
    }

    /**
     * Ends the watching: a last snapshot, the sampling stopped, the lock waits
     * taken from the container's log, the settings set back, and the summary
     * written. The settings are set back whatever fails before.
     *
     * @return The summary, for the report
     */
    String finish() throws IOException, InterruptedException {
        if (summary != null) {
            return summary;
        }
        try {
            if (snapshots != null) {
                snapshots.shutdownNow();
                snapshots.awaitTermination(1, TimeUnit.MINUTES);
            }
            snapshot("end");
            stopSampler();
            final Output log = shell.run(List.of(command, "logs", "--since", started.toString(), container), "");
            Files.writeString(target.resolve(LOCK_WAITS), lockWaitLines(log.text()));
        } finally {
            close();
        }
        summary = summarize(Files.readAllLines(target.resolve(ACTIVITY)), Files.readAllLines(target.resolve(LOCK_WAITS)),
                Files.readAllLines(target.resolve(SNAPSHOTS)));
        Files.writeString(target.resolve(SUMMARY), summary);
        return summary;
    }

    /**
     * Stops the sampling and sets the settings back, if that isn't done yet;
     * after {@link #finish()}, or instead of it when the test fails.
     */
    @Override
    public void close() throws IOException, InterruptedException {
        if (snapshots != null) {
            snapshots.shutdownNow();
        }
        try {
            // Also when the podman command has ended: its psql may still run in the container.
            if (sampler != null) {
                stopSampler();
            }
        } finally {
            restore();
            if ((hook != null) && (Thread.currentThread() != hook)) {
                try {
                    Runtime.getRuntime().removeShutdownHook(hook);
                } catch (IllegalStateException _) {
                    // The JVM is stopping already; the hook does the same.
                }
            }
        }
    }

    private void closeQuietly() {
        try {
            close();
        } catch (IOException | RuntimeException e) {
            System.err.println("PostgreSQL's settings weren't set back: " + e.getMessage());
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    private void verify() throws IOException, InterruptedException {
        final Output inspect = shell.run(List.of(command, "inspect", "--format", "{{.State.Running}}", container), "");
        if ((inspect.code() != 0) || !"true".equals(inspect.text().trim())) {
            throw new IllegalStateException("The container " + container + " isn't running (" + command + " inspect: "
                    + inspect.text().trim() + "); the observer only watches a PostgreSQL in a container.");
        }
        final Output published = shell.run(List.of(command, "port", container, "5432/tcp"), "");
        final boolean publishes = (published.code() == 0) && published.text().lines().map(String::trim)
                .anyMatch(line -> line.matches(PUBLISHED + port));
        if (!publishes) {
            throw new IllegalStateException("The container " + container + " doesn't publish PostgreSQL on the port " + port
                    + " of EDS_DB_URL on this host (" + command + " port: " + published.text().trim() + "), so it may not be the stone's database.");
        }
        // The port alone could be another server's, e.g. with podman talking to another host; the time the
        // server started, to the microsecond, tells.
        final String inContainer = String.join("", psql(START_TIME));
        final String throughUrl;
        try {
            throughUrl = identity.startTime();
        } catch (SQLException e) {
            throw new IllegalStateException("The database of EDS_DB_URL can't be reached to compare it with the container: " + e.getMessage(), e);
        }
        if (!inContainer.equals(throughUrl)) {
            throw new IllegalStateException("The server of EDS_DB_URL (started " + throughUrl + ") isn't the one in the container "
                    + container + " (started " + inContainer + ").");
        }
        final List<String> superuser = psql("SELECT rolsuper FROM pg_roles WHERE rolname = current_user");
        if (!List.of("t").equals(superuser)) {
            throw new IllegalStateException("The user " + user + " isn't a superuser in the container " + container
                    + "; give it with eds.load.pg.user.");
        }
    }

    private synchronized void markChanged() {
        changed = true;
    }

    private synchronized void restore() throws IOException, InterruptedException {
        if (!changed) {
            return;
        }
        final StringBuilder sql = new StringBuilder();
        for (final String name : SETTINGS.keySet()) {
            sql.append("ALTER DATABASE \"").append(database).append('"');
            final String value = previous.get(name);
            if (value != null) {
                sql.append(" SET ").append(name).append(" = '").append(value.replace("'", "''")).append("';\n");
            } else {
                sql.append(" RESET ").append(name).append(";\n");
            }
        }
        psql(sql.toString());
        // Only once it is done; else the next close tries again.
        changed = false;
    }

    private synchronized void snapshot(final String label) throws IOException, InterruptedException {
        final String at = "'" + label + "', extract(epoch FROM date_trunc('second', now()))::bigint, ";
        final List<String> rows = psql(String.join("\n",
                "SELECT " + at + "'table', relname, seq_scan, seq_tup_read, coalesce(idx_scan, 0), coalesce(idx_tup_fetch, 0),"
                        + " n_tup_ins, n_tup_upd, n_tup_del, n_live_tup, n_dead_tup, autovacuum_count, autoanalyze_count"
                        + " FROM pg_stat_user_tables ORDER BY relname;",
                "SELECT " + at + "'io', relname, heap_blks_read, heap_blks_hit, coalesce(idx_blks_read, 0), coalesce(idx_blks_hit, 0)"
                        + " FROM pg_statio_user_tables ORDER BY relname;",
                "SELECT " + at + "'index', relname, indexrelname, idx_scan, idx_tup_read, idx_tup_fetch"
                        + " FROM pg_stat_user_indexes ORDER BY relname, indexrelname;",
                "SELECT " + at + "'database', xact_commit, xact_rollback, blks_read, blks_hit, temp_files, temp_bytes, deadlocks,"
                        + " round(blk_read_time::numeric, 1), round(blk_write_time::numeric, 1), tup_returned, tup_fetched"
                        + " FROM pg_stat_database WHERE datname = current_database();"));
        Files.write(target.resolve(SNAPSHOTS), rows, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private void startSampler() throws IOException {
        // One psql for the whole run, which repeats the query every second (\watch); a heartbeat row
        // (pid 0) marks each sample, also when no connection is open.
        final String query = "SELECT extract(epoch FROM date_trunc('second', now()))::bigint, 0, 'sample', '', '', '', -1, ''"
                + " UNION ALL SELECT extract(epoch FROM date_trunc('second', now()))::bigint, a.pid, coalesce(a.state, ''),"
                + " coalesce(a.wait_event_type, ''), coalesce(a.wait_event, ''), array_to_string(pg_blocking_pids(a.pid), ','),"
                + " coalesce((extract(epoch FROM now() - a.xact_start) * 1000)::bigint, -1),"
                + " regexp_replace(left(a.query, " + STATEMENT + "), '\\s+', ' ', 'g')"
                + " FROM pg_stat_activity a WHERE a.datname = current_database() AND a.backend_type = 'client backend'"
                + " AND a.application_name NOT IN ('" + OBSERVER + "', '" + SAMPLER + "')\n\\watch 1\n";
        sampler = starter.start(psqlCommand(SAMPLER), target.resolve(ACTIVITY));
        try (OutputStream input = sampler.getOutputStream()) {
            input.write(query.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void stopSampler() throws IOException, InterruptedException {
        // Ending the sampler's connection ends its psql in the container; stopping only the podman
        // command could leave it running there.
        final Process process = sampler;
        sampler = null;
        try {
            terminateSamplers();
        } finally {
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        }
    }

    private void terminateSamplers() throws IOException, InterruptedException {
        psql("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE application_name = '" + SAMPLER + "'");
    }

    private List<String> psql(final String sql) throws IOException, InterruptedException {
        // The server ends a statement which waits or runs too long, as stopping podman on a timeout
        // wouldn't stop psql in the container.
        final List<String> call = new ArrayList<>(psqlCommand(OBSERVER));
        call.addAll(3, List.of("-e", "PGOPTIONS=-c lock_timeout=30s -c statement_timeout=90s"));
        final Output output = shell.run(call, sql);
        if (output.code() != 0) {
            throw new IllegalStateException("psql in the container " + container + " failed: " + output.text().trim());
        }
        return output.text().lines().filter(line -> !line.isBlank()).toList();
    }

    private List<String> psqlCommand(final String application) {
        return List.of(command, "exec", "-i", "-e", "PGAPPNAME=" + application, container,
                "psql", "-U", user, "-d", database, "-X", "-A", "-t", "-q", "-v", "ON_ERROR_STOP=1", "-F", "|");
    }

    // The command is the podman or docker of the developer, with the container they gave (checked above).
    @SuppressWarnings("findsecbugs:COMMAND_INJECTION")
    private static Output process(final List<String> command, final String input) throws IOException, InterruptedException {
        // The output goes to a file, so a command which hangs can't hold the test past the deadline.
        final Path output = Files.createTempFile("eds-load-pg", ".txt");
        try {
            final Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(input.getBytes(StandardCharsets.UTF_8));
            }
            if (!process.waitFor(COMMAND.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("No answer within " + COMMAND + ": " + String.join(" ", command));
            }
            return new Output(process.exitValue(), Files.readString(output));
        } finally {
            Files.deleteIfExists(output);
        }
    }

    /** Podman if it is on the PATH, as it runs without root; else docker. */
    static String containerCommand(final String path) {
        return onPath("podman", path) ? "podman" : "docker";
    }

    /** Whether the program is in a directory of the PATH. */
    static boolean onPath(final String program, final String path) {
        if (path != null) {
            for (final String directory : path.split(File.pathSeparator)) {
                if (!directory.isEmpty() && Files.isExecutable(Path.of(directory, program))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String startTime() throws SQLException {
        try (Connection connection = DriverManager.getConnection(System.getenv("EDS_DB_URL"), System.getenv("EDS_DB_USERNAME"),
                System.getenv("EDS_DB_PASSWORD"));
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(START_TIME)) {
            return result.next() ? result.getString(1) : "";
        }
    }

    // As above.
    @SuppressWarnings("findsecbugs:COMMAND_INJECTION")
    private static Process sampler(final List<String> command, final Path output) throws IOException {
        return new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
    }

    private static Map<String, String> settings() {
        final Map<String, String> settings = new LinkedHashMap<>();
        settings.put("log_lock_waits", "on");
        settings.put("deadlock_timeout", "'200ms'");
        settings.put("track_io_timing", "on");
        return settings;
    }

    // ------------------------------------------------------------------
    // The summary, from the three files.
    // ------------------------------------------------------------------

    /** The lines of the server's log about lock waits and deadlocks, each with the lines which go with it. */
    static String lockWaitLines(final String log) {
        final StringBuilder lines = new StringBuilder();
        boolean inWait = false;
        for (final String line : log.split("\n")) {
            if (line.contains("still waiting for") || line.contains(" acquired ") || line.contains("deadlock detected")) {
                inWait = true;
                lines.append(line).append('\n');
            } else if (inWait && (line.contains("DETAIL:") || line.contains("CONTEXT:") || line.contains("STATEMENT:")
                    || line.startsWith("\t") || line.startsWith(" "))) {
                lines.append(line).append('\n');
            } else {
                inWait = false;
            }
        }
        return lines.toString();
    }

    static String summarize(final List<String> activity, final List<String> lockWaits, final List<String> snapshotRows) {
        return activity(activity) + lockWaits(lockWaits) + snapshots(snapshotRows);
    }

    /** One hour of samples: sums over its samples, so a mean is the sum by the samples. */
    private static final class Hour {
        long samples;
        long active;
        long lockWaiting;
        long idleInTransaction;
        long blocked;
        final Map<String, Long> waits = new TreeMap<>();
    }

    static String activity(final List<String> lines) {
        final Map<Long, Hour> hours = new TreeMap<>();
        final Map<String, Long> waitingStatements = new TreeMap<>();
        long first = -1;
        for (final String line : lines) {
            final String[] field = line.split("\\|", 8);
            if ((field.length < 8) || !field[0].matches("\\d+") || !field[1].matches("\\d+")) {
                continue;
            }
            final long second = Long.parseLong(field[0]);
            if (first < 0) {
                first = second;
            }
            final Hour hour = hours.computeIfAbsent((second - first) / 3600 + 1, _ -> new Hour());
            if ("0".equals(field[1])) {
                hour.samples++;
                continue;
            }
            if ("active".equals(field[2])) {
                hour.active++;
            } else if (field[2].startsWith("idle in transaction")) {
                hour.idleInTransaction++;
            }
            if (!field[5].isEmpty()) {
                hour.blocked++;
            }
            if (!field[3].isEmpty() && !"Client".equals(field[3]) && !"Activity".equals(field[3])) {
                hour.waits.merge(field[3] + "/" + field[4], 1L, Long::sum);
            }
            if ("Lock".equals(field[3])) {
                hour.lockWaiting++;
                waitingStatements.merge(field[7], 1L, Long::sum);
            }
        }
        final StringBuilder text = new StringBuilder(String.format(Locale.ROOT, 
                "%nPostgreSQL, sampled every second (pg_stat_activity, %s); connections at a time, as a mean:%n", ACTIVITY));
        text.append(String.format(Locale.ROOT, "  %-5s %8s %8s %10s %10s %10s  %s%n", "hour", "samples", "active", "on a lock", "blocked",
                "idle in tx", "waited for (not idle)"));
        hours.forEach((number, hour) -> {
            final long samples = Math.max(1, hour.samples);
            final String waits = hour.waits.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(3)
                    .map(e -> String.format(Locale.ROOT, "%s %.1f", e.getKey(), (double) e.getValue() / samples))
                    .collect(Collectors.joining(", "));
            text.append(String.format(Locale.ROOT, "  %-5d %8d %8.1f %10.1f %10.1f %10.1f  %s%n", number, hour.samples,
                    (double) hour.active / samples, (double) hour.lockWaiting / samples, (double) hour.blocked / samples,
                    (double) hour.idleInTransaction / samples, waits));
        });
        if (!waitingStatements.isEmpty()) {
            text.append("Statements seen waiting for a lock, in samples:\n");
            top(waitingStatements).forEach(e -> text.append(String.format(Locale.ROOT, "  %8d  %s%n", e.getValue(), e.getKey())));
        }
        return text.toString();
    }

    private static final Pattern WAITED = Pattern.compile("acquired .* after ([0-9.]+) ms");

    static String lockWaits(final List<String> lines) {
        long waits = 0;
        long deadlocks = 0;
        final List<Double> acquired = new ArrayList<>();
        final Map<String, Long> statements = new TreeMap<>();
        boolean waiting = false;
        for (final String line : lines) {
            if (line.contains("still waiting for")) {
                waits++;
                waiting = true;
            } else if (line.contains("deadlock detected")) {
                deadlocks++;
                waiting = false;
            } else if (line.contains(" acquired ")) {
                final Matcher matcher = WAITED.matcher(line);
                if (matcher.find()) {
                    acquired.add(Double.parseDouble(matcher.group(1)));
                }
                waiting = false;
            } else if (waiting && line.contains("STATEMENT:")) {
                final String statement = line.substring(line.indexOf("STATEMENT:") + 10).trim().replaceAll("\\s+", " ");
                statements.merge(statement.substring(0, Math.min(STATEMENT, statement.length())), 1L, Long::sum);
                waiting = false;
            }
        }
        final StringBuilder text = new StringBuilder(String.format(Locale.ROOT, 
                "%nLock waits over 200 ms (log_lock_waits, %s): %d, deadlocks %d", LOCK_WAITS, waits, deadlocks));
        if (!acquired.isEmpty()) {
            text.append(String.format(Locale.ROOT, "; of %d which got their lock, the wait was %.0f ms as a mean, %.0f at most",
                    acquired.size(), acquired.stream().mapToDouble(Double::doubleValue).average().orElse(0),
                    acquired.stream().mapToDouble(Double::doubleValue).max().orElse(0)));
        }
        text.append(".\n");
        top(statements).forEach(e -> text.append(String.format(Locale.ROOT, "  %8d  %s%n", e.getValue(), e.getKey())));
        return text.toString();
    }

    static String snapshots(final List<String> rows) {
        // label -> table -> rows read by seq scans; label -> the row of the database.
        final Map<String, Map<String, Long>> seqRows = new LinkedHashMap<>();
        final Map<String, String[]> databases = new LinkedHashMap<>();
        for (final String row : rows) {
            final String[] field = row.split("\\|");
            if ((field.length > 5) && "table".equals(field[2])) {
                seqRows.computeIfAbsent(field[0], _ -> new TreeMap<>()).put(field[3], Long.parseLong(field[5]));
            } else if ((field.length > 13) && "database".equals(field[2])) {
                databases.put(field[0], field);
            }
        }
        final StringBuilder text = new StringBuilder(String.format(Locale.ROOT, 
                "%nPostgreSQL between the snapshots (pg_stat_*, %s, track_io_timing):%n", SNAPSHOTS));
        text.append(String.format(Locale.ROOT, "  %-12s %10s %9s %6s %10s %9s %9s  %s%n", "interval", "commits", "rollbacks", "hit %",
                "read ms", "temp MiB", "deadlocks", "rows read by seq scans"));
        final List<String> labels = new ArrayList<>(databases.keySet());
        for (int i = 1; i < labels.size(); i++) {
            final String[] from = databases.get(labels.get(i - 1));
            final String[] to = databases.get(labels.get(i));
            final long read = delta(to, from, 5);
            final long hit = delta(to, from, 6);
            final double readMillis = Double.parseDouble(to[10]) - Double.parseDouble(from[10]);
            final Map<String, Long> before = seqRows.getOrDefault(labels.get(i - 1), Map.of());
            final Map<String, Long> scans = new TreeMap<>();
            seqRows.getOrDefault(labels.get(i), Map.of()).forEach((table, value) -> {
                final long added = value - before.getOrDefault(table, 0L);
                if (added > 0) {
                    scans.put(table, added);
                }
            });
            text.append(String.format(Locale.ROOT, "  %-12s %10d %9d %6.1f %10.0f %9.1f %9d  %s%n", labels.get(i - 1) + ".." + labels.get(i),
                    delta(to, from, 3), delta(to, from, 4), ((read + hit) == 0) ? 100.0 : 100.0 * hit / (read + hit), readMillis,
                    delta(to, from, 8) / (1024.0 * 1024), delta(to, from, 9),
                    scans.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(3)
                            .map(e -> e.getKey() + " " + e.getValue()).collect(Collectors.joining(", "))));
        }
        return text.toString();
    }

    private static long delta(final String[] to, final String[] from, final int column) {
        return Long.parseLong(to[column]) - Long.parseLong(from[column]);
    }

    private static List<Map.Entry<String, Long>> top(final Map<String, Long> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP).toList();
    }
}
