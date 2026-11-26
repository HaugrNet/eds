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
package net.haugr.eds.testing.fitnesse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * <p>Runs the FitNesse suites against the EDS runner JAR (PLAN.md, L3),
 * with {@code mvn -Pfitnesse verify}. The runner is started on its own ports,
 * so it doesn't clash with an EDS running on the default ports, and stopped
 * afterwards.</p>
 *
 * <p>Without {@code EDS_DB_URL} in the environment, the runner uses its
 * in-memory H2 database; with it, the given PostgreSQL database, which the
 * suites leave as they found it (their setup removes all Circles, Members and
 * DataTypes).</p>
 *
 * <p>System properties: {@code eds.runner} (the runner JAR),
 * {@code eds.it.http.port} and {@code eds.it.management.port}, and
 * {@code eds.it.suite} (default {@code EDS}).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class FitNesseSuiteIT {

    private static final Path TARGET = Path.of("target");
    private static final Duration STARTUP = Duration.ofMinutes(3);

    @Test
    // Starts this JVM's java with the runner JAR and FitNesse, nothing else.
    @SuppressWarnings("findsecbugs:COMMAND_INJECTION")
    void testSuitesAgainstTheRunner() throws IOException, InterruptedException {
        final Path runner = Path.of(System.getProperty("eds.runner", "../eds-quarkus/target/eds-rune.jar"));
        final int httpPort = Integer.getInteger("eds.it.http.port", 18080);
        final int managementPort = Integer.getInteger("eds.it.management.port", 19000);
        final String suite = System.getProperty("eds.it.suite", "EDS");
        final String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        assertTrue(Files.isRegularFile(runner), "The runner JAR " + runner.toAbsolutePath().normalize() + " is missing; build eds-quarkus first.");
        assertTrue(Files.isRegularFile(TARGET.resolve("eds-testing.jar")), "The fixtures JAR target/eds-testing.jar is missing.");

        final Path serverLog = TARGET.resolve("eds-server.log");
        final Process server = new ProcessBuilder(java,
                "-Dquarkus.http.port=" + httpPort,
                "-Dquarkus.management.port=" + managementPort,
                "-jar", runner.toString())
                .redirectErrorStream(true)
                .redirectOutput(serverLog.toFile())
                .start();
        try {
            awaitReady(server, managementPort, serverLog);

            final Path results = TARGET.resolve("fitnesse-results.txt");
            final ProcessBuilder builder = new ProcessBuilder(java,
                    "-cp", System.getProperty("java.class.path"),
                    "fitnesseMain.FitNesseMain",
                    "-p", String.valueOf(freePort()),
                    "-o",
                    "-c", suite + "?suite&format=text")
                    .redirectErrorStream(true)
                    .redirectOutput(results.toFile());
            builder.environment().put("EDS_URL", "http://localhost:" + httpPort + "/api/v1");
            final Process fitnesse = builder.start();
            if (!fitnesse.waitFor(30, TimeUnit.MINUTES)) {
                fitnesse.destroyForcibly();
                fail("FitNesse didn't finish within 30 minutes, see " + results);
            }

            final List<String> lines = Files.readAllLines(results);
            lines.stream().filter(line -> line.matches("^[.FEX] .*|^\\d+ Tests,.*")).forEach(System.out::println);
            // FitNesse exits with the number of failed pages.
            assertEquals(0, fitnesse.exitValue(), "FitNesse reported failures, see " + results.toAbsolutePath()
                    + " and FitNesseRoot/files/testResults");
            assertTrue(lines.stream().anyMatch(line -> line.matches("^\\d+ Tests,\\s+0 Failures.*")),
                    "FitNesse ran no tests, see " + results.toAbsolutePath());
        } finally {
            server.destroy();
            if (!server.waitFor(30, TimeUnit.SECONDS)) {
                server.destroyForcibly();
            }
        }
    }

    private static void awaitReady(final Process server, final int managementPort, final Path log) throws InterruptedException {
        final URI ready = URI.create("http://localhost:" + managementPort + "/q/health/ready");
        final long deadline = System.nanoTime() + STARTUP.toNanos();
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()) {
            while (System.nanoTime() < deadline) {
                if (ready(client, ready)) {
                    return;
                }
                // Half a second until the next try, or less if EDS stops meanwhile.
                if (server.waitFor(500, TimeUnit.MILLISECONDS)) {
                    fail("EDS stopped with exit code " + server.exitValue() + " during startup, see " + log.toAbsolutePath());
                }
            }
        }
        fail("EDS wasn't ready within " + STARTUP + ", see " + log.toAbsolutePath());
    }

    private static boolean ready(final HttpClient client, final URI ready) throws InterruptedException {
        try {
            return client.send(HttpRequest.newBuilder(ready).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (IOException _) {
            // Not listening yet.
            return false;
        }
    }

    // Only asks the system for a free port; nothing is ever sent over it.
    @SuppressWarnings("findsecbugs:UNENCRYPTED_SERVER_SOCKET")
    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }
}
