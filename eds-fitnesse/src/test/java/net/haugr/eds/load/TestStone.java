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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * <p>A stone of its own for the load test: the runner JAR started on ports
 * of its own, with a fresh MasterKey file as its source, so it starts
 * unlocked (the MasterKey isn't what is tested here).</p>
 *
 * <p>Without {@code EDS_DB_URL} in the environment it runs on its in-memory
 * H2 database; with it, on the given PostgreSQL database, which must be
 * empty, since the test claims the stone as its first account.</p>
 *
 * <p>System properties: {@code eds.runner} (the JAR, default the stone
 * without Rune), {@code eds.load.http.port} (18090),
 * {@code eds.load.management.port} (19090) and {@code eds.load.jvm}
 * (options for its JVM, e.g. {@code -Xmx1g}).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class TestStone implements AutoCloseable {

    private static final Duration STARTUP = Duration.ofMinutes(3);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Process process;
    private final Path log;
    private final int httpPort;
    /** The Flight Recording of the stone, in the target directory. */
    static final String RECORDING = "load-stone.jfr";
    private final int managementPort;

    private TestStone(final Process process, final Path log, final int httpPort, final int managementPort) {
        this.process = process;
        this.log = log;
        this.httpPort = httpPort;
        this.managementPort = managementPort;
    }

    // Starts this JVM's java with the runner JAR; the JAR, the ports and the JVM options come from the
    // system properties the developer gives the test (see above), which is their purpose.
    @SuppressWarnings("findsecbugs:COMMAND_INJECTION")
    static TestStone start(final Path target) throws IOException, InterruptedException {
        return start(target, "load", 18090, 19090);
    }

    /**
     * Starts a stone for a test of the name: its MasterKey file and log are
     * {@code <name>-masterkey} and {@code <name>-stone.log} in the target
     * directory, its ports and JVM options are the properties
     * {@code eds.<name>.http.port}, {@code eds.<name>.management.port} and
     * {@code eds.<name>.jvm}.
     */
    // As above: the properties the developer gives the test.
    @SuppressWarnings("findsecbugs:COMMAND_INJECTION")
    static TestStone start(final Path target, final String name, final int defaultHttpPort, final int defaultManagementPort) throws IOException, InterruptedException {
        final Path runner = Path.of(System.getProperty("eds.runner", "../eds-quarkus/target/eds-stone.jar"));
        if (!Files.isRegularFile(runner)) {
            throw new IllegalStateException("The runner JAR " + runner.toAbsolutePath().normalize() + " is missing; build eds-quarkus first.");
        }
        final int httpPort = Integer.getInteger("eds." + name + ".http.port", defaultHttpPort);
        final int managementPort = Integer.getInteger("eds." + name + ".management.port", defaultManagementPort);
        Files.createDirectories(target);

        // The MasterKey: 48 random bytes, Base64, as "eds.sh masterkey generate" makes it.
        final Path masterKey = target.resolve(name + "-masterkey");
        final byte[] secret = new byte[48];
        RANDOM.nextBytes(secret);
        Files.deleteIfExists(masterKey);
        Files.createFile(masterKey, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        Files.writeString(masterKey, Base64.getEncoder().encodeToString(secret));

        final List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        final String jvm = System.getProperty("eds." + name + ".jvm", "").trim();
        if (!jvm.isEmpty()) {
            command.addAll(Arrays.asList(jvm.split("\\s+")));
        }
        if ("load".equals(name) && recording()) {
            // A Flight Recording of the whole run: threads, CPU, heap, GC, locks, socket I/O.
            command.add("-XX:StartFlightRecording=filename=" + target.resolve(RECORDING).toAbsolutePath() + ",settings=profile,dumponexit=true");
        }
        command.add("-Dquarkus.http.port=" + httpPort);
        command.add("-Dquarkus.http.ssl-port=" + (httpPort + 1));
        command.add("-Dquarkus.management.port=" + managementPort);
        command.add("-jar");
        command.add(runner.toString());

        final Path log = target.resolve(name + "-stone.log");
        final ProcessBuilder builder = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(log.toFile());
        builder.environment().put("EDS_MASTERKEY_URL", masterKey.toAbsolutePath().toUri().toString());
        final TestStone stone = new TestStone(builder.start(), log, httpPort, managementPort);
        stone.awaitReady();
        return stone;
    }

    /** Whether the stone keeps a Flight Recording, with {@code -Deds.load.jfr=true}. */
    static boolean recording() {
        return Boolean.getBoolean("eds.load.jfr");
    }

    String url() {
        return "http://localhost:" + httpPort + "/api/v1";
    }

    Path log() {
        return log;
    }

    boolean alive() {
        return process.isAlive();
    }

    /** Whether the stone says it is ready: its database reachable and the MasterKey loaded. */
    boolean ready() {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).proxy(HttpClient.Builder.NO_PROXY).build()) {
            final HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + managementPort + "/q/health/ready"))
                    .timeout(Duration.ofSeconds(10)).build();
            return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (IOException _) {
            return false;
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** The lines of the log at level ERROR, or with a stack trace of an exception. */
    List<String> errors() throws IOException {
        try (Stream<String> lines = Files.lines(log)) {
            return lines.filter(line -> line.contains(" ERROR ") || line.startsWith("Exception in thread")).toList();
        }
    }

    private void awaitReady() throws InterruptedException {
        final long deadline = System.nanoTime() + STARTUP.toNanos();
        while (System.nanoTime() < deadline) {
            if (ready()) {
                return;
            }
            // Half a second until the next try, or less if EDS stops meanwhile.
            if (process.waitFor(500, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("EDS stopped with exit code " + process.exitValue() + " during startup, see " + log.toAbsolutePath());
            }
        }
        throw new IllegalStateException("EDS wasn't ready within " + STARTUP + ", see " + log.toAbsolutePath());
    }

    @Override
    public void close() throws InterruptedException {
        process.destroy();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
        }
    }
}
