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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * <p>A stone of its own for the load test: the runner JAR started on ports
 * of its own, with a fresh MasterKey file as its source, so it starts
 * unlocked (the MasterKey isn't what is tested here).</p>
 *
 * <p>With rootless Podman on the PATH the stone runs in a container (ticket
 * 29): an image built from the runner JAR with
 * {@code accessories/podman/Containerfile}, as {@code eds.sh build stone}
 * builds it, run under the name {@code eds-<name>-stone} on the host's
 * network, so its ports and {@code EDS_DB_URL} are those of this host. A
 * container of that name left by a run which was killed is removed before
 * the start, and a shutdown hook stops the stone when the JVM is stopped.
 * Without Podman, or with {@code -Deds.stone.container=false}, the JAR runs
 * as a process of this JVM's java.</p>
 *
 * <p>Without {@code EDS_DB_URL} in the environment it runs on its in-memory
 * H2 database; with it, on the given PostgreSQL database, which must be
 * empty, since the test claims the stone as its first account.</p>
 *
 * <p>System properties: {@code eds.runner} (the JAR, default the stone
 * without Rune), {@code eds.stone.container} ({@code false} for the JAR as a
 * process), {@code eds.load.http.port} (18090),
 * {@code eds.load.management.port} (19090) and {@code eds.load.jvm}
 * (options for its JVM, e.g. {@code -Xmx1g}).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class TestStone implements AutoCloseable {

    /** The Flight Recording of the stone, in the target directory. */
    static final String RECORDING = "load-stone.jfr";
    /** The most a Flight Recording keeps: a 4-hour run with 160 users makes some 4 GB. */
    static final String RECORDING_SIZE = "8g";
    /** The image of the test's stone; built anew for every run, from the JAR just built. */
    static final String IMAGE = "eds-test-stone:local";
    /** Where the target directory is in the container. */
    static final String MOUNT = "/eds/target";

    private static final Duration STARTUP = Duration.ofMinutes(3);
    private static final Duration STOP = Duration.ofMinutes(1);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PODMAN = "podman";
    // The same label as eds.sh gives its images, so its clean-up knows the old ones as EDS's.
    private static final String JAR_LABEL = "net.haugr.eds.jar-modified";
    /** The prefixes of the variables passed on to the container, by name; the MasterKey's is set apart. */
    private static final List<String> PASSED = List.of("EDS_", "QUARKUS_");
    private static final String MASTERKEY_VARIABLE = "EDS_MASTERKEY_URL";
    private static final Duration LOG_END = Duration.ofSeconds(10);

    /** The stone's process, or in a container the process which follows its log. */
    private final Process process;
    /** The name of its container, or null for a process. */
    private final String container;
    private final Path log;
    private final int httpPort;
    private final int managementPort;
    private final Thread hook;

    TestStone(final Process process, final String container, final Path log, final int httpPort, final int managementPort) {
        this.process = process;
        this.container = container;
        this.log = log;
        this.httpPort = httpPort;
        this.managementPort = managementPort;
        this.hook = Thread.ofPlatform().name("test-stone-shutdown").unstarted(this::stopQuietly);
        Runtime.getRuntime().addShutdownHook(hook);
    }

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

        final List<String> options = new ArrayList<>();
        final String jvm = System.getProperty("eds." + name + ".jvm", "").trim();
        if (!jvm.isEmpty()) {
            options.addAll(Arrays.asList(jvm.split("\\s+")));
        }
        final boolean inContainer = inContainer(System.getProperty("eds.stone.container"), System.getenv("PATH"));
        // The stone's own view of the target directory: the mount in a container.
        final String files = inContainer ? MOUNT : target.toAbsolutePath().toString();
        if ("load".equals(name) && recording()) {
            // A Flight Recording of the whole run: threads, CPU, heap, GC, locks, socket I/O.
            options.add(recordingOption(files));
        }
        options.add("-Dquarkus.http.port=" + httpPort);
        options.add("-Dquarkus.http.ssl-port=" + (httpPort + 1));
        options.add("-Dquarkus.management.port=" + managementPort);
        final String masterKeyUrl = inContainer
                ? "file://" + MOUNT + "/" + masterKey.getFileName()
                : masterKey.toAbsolutePath().toUri().toString();

        final Path log = target.resolve(name + "-stone.log");
        final TestStone stone = inContainer
                ? inContainer(target, name, runner, options, masterKeyUrl, log, httpPort, managementPort)
                : asProcess(runner, options, masterKeyUrl, log, httpPort, managementPort);
        try {
            stone.awaitReady();
        } catch (IllegalStateException | InterruptedException e) {
            stone.close();
            throw e;
        }
        return stone;
    }

    /** Whether the stone runs in a container: unless told not to, when Podman is on the PATH. */
    static boolean inContainer(final String property, final String path) {
        return !"false".equalsIgnoreCase(property) && PostgresObserver.onPath(PODMAN, path);
    }

    // The JAR, the ports and the JVM options come from the system properties the developer gives the test
    // (see above), which is their purpose.
    @SuppressWarnings("findsecbugs:COMMAND_INJECTION")
    private static TestStone asProcess(final Path runner, final List<String> options, final String masterKeyUrl, final Path log,
                                       final int httpPort, final int managementPort) throws IOException {
        final List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.addAll(options);
        command.add("-jar");
        command.add(runner.toString());
        final ProcessBuilder builder = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(log.toFile());
        builder.environment().put(MASTERKEY_VARIABLE, masterKeyUrl);
        return new TestStone(builder.start(), null, log, httpPort, managementPort);
    }

    // As above, and the container's name is made here.
    @SuppressWarnings("findsecbugs:COMMAND_INJECTION")
    private static TestStone inContainer(final Path target, final String name, final Path runner, final List<String> options,
                                         final String masterKeyUrl, final Path log, final int httpPort, final int managementPort)
            throws IOException, InterruptedException {
        final String container = "eds-" + name + "-stone";
        final Path containerFile = Path.of(System.getProperty("eds.containerfile", "../accessories/podman/Containerfile"));
        final String user = Files.getAttribute(target, "unix:uid") + ":" + Files.getAttribute(target, "unix:gid");
        for (final List<String> command : startCommands(container, containerFile, runner, Files.getLastModifiedTime(runner).toMillis() / 1000,
                target.toAbsolutePath(), user, masterKeyUrl, options, System.getenv().keySet())) {
            run(command);
        }
        final Process follower = new ProcessBuilder(PODMAN, "logs", "--follow", container)
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
        return new TestStone(follower, container, log, httpPort, managementPort);
    }

    /**
     * The image built, a container of the name left by a run which was killed
     * removed (it would hold the ports), and the stone started.
     */
    static List<List<String>> startCommands(final String container, final Path containerFile, final Path runner, final long modified,
                                            final Path target, final String user, final String masterKeyUrl, final List<String> options,
                                            final Set<String> environment) {
        return List.of(buildCommand(containerFile, runner, modified),
                List.of(PODMAN, "rm", "--force", "--ignore", container),
                runCommand(container, target, user, masterKeyUrl, options, environment));
    }

    /** Builds the image from the JAR, as eds.sh does. */
    static List<String> buildCommand(final Path containerFile, final Path runner, final long modified) {
        final Path directory = Objects.requireNonNull(runner.toAbsolutePath().getParent(), "The JAR has no directory");
        return List.of(PODMAN, "build", "--quiet", "--format", "docker", "--file", containerFile.toString(), "--tag", IMAGE,
                "--build-arg", "JAR=" + runner.getFileName(), "--label", JAR_LABEL + "=" + modified,
                directory.toString());
    }

    /**
     * Runs the stone: on the host's network, as the owner of the target
     * directory (its user namespace kept, so the MasterKey file can be read
     * and the recording is the developer's), with the target directory at
     * {@link #MOUNT}, not relabelled for SELinux (its labels are left alone,
     * as eds.sh's builder does). The variables of EDS and Quarkus which are
     * set (the database's among them) are passed by name, as {@code java
     * -jar} gets them, so the password is on no command line. The image's
     * health check is left out: it asks port 9000, the test moves the ports,
     * and checks readiness itself.
     */
    static List<String> runCommand(final String container, final Path target, final String user, final String masterKeyUrl,
                                   final List<String> options, final Set<String> environment) {
        final List<String> command = new ArrayList<>(List.of(PODMAN, "run", "--detach", "--name", container, "--network", "host",
                "--userns", "keep-id", "--user", user, "--no-healthcheck", "--security-opt", "label=disable",
                "--volume", target + ":" + MOUNT, "--env", MASTERKEY_VARIABLE + "=" + masterKeyUrl));
        environment.stream()
                .filter(variable -> PASSED.stream().anyMatch(variable::startsWith) && !MASTERKEY_VARIABLE.equals(variable))
                .sorted()
                .forEach(variable -> command.addAll(List.of("--env", variable)));
        command.addAll(List.of("--entrypoint", "java", IMAGE));
        command.addAll(options);
        command.addAll(List.of("-jar", "/app/eds.jar"));
        return command;
    }

    private static void run(final List<String> command) throws IOException, InterruptedException {
        final Outcome outcome = execute(command);
        if (outcome.code() != 0) {
            throw new IllegalStateException(String.join(" ", command.subList(0, 2)) + " failed: " + outcome.output()
                    + " (-Deds.stone.container=false runs the stone as java -jar instead)");
        }
    }

    private record Outcome(int code, String output) {}

    // As above.
    @SuppressWarnings("findsecbugs:COMMAND_INJECTION")
    private static Outcome execute(final List<String> command) throws IOException, InterruptedException {
        final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        return new Outcome(process.waitFor(), output);
    }

    /**
     * The Flight Recording of the whole run (ticket 37): JFR's default keeps
     * at most 250 MB, which with {@code profile} and 160 users is only the
     * last 15 minutes. It keeps up to {@link #RECORDING_SIZE}; no age is set,
     * as {@code maxage=0} kept only the last chunk.
     *
     * @param files The stone's view of the target directory
     * @return The option of the JVM
     */
    static String recordingOption(final String files) {
        return "-XX:StartFlightRecording=filename=" + files + "/" + RECORDING + ",settings=profile,maxsize=" + RECORDING_SIZE
                + ",dumponexit=true";
    }

    /** Whether the stone keeps a Flight Recording, with {@code -Deds.load.jfr=true}. */
    static boolean recording() {
        return Boolean.getBoolean("eds.load.jfr");
    }

    /** The hook which stops the stone when the JVM stops, for the test of {@link #close()}. */
    Thread hook() {
        return hook;
    }

    String url() {
        return "http://localhost:" + httpPort + "/api/v1";
    }

    Path log() {
        return log;
    }

    boolean alive() {
        if (container == null) {
            return process.isAlive();
        }
        try {
            final Outcome inspect = execute(List.of(PODMAN, "inspect", "--format", "{{.State.Running}}", container));
            return (inspect.code() == 0) && "true".equals(inspect.output());
        } catch (IOException _) {
            return false;
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return false;
        }
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
            if (process.waitFor(500, TimeUnit.MILLISECONDS) || ((container != null) && !alive())) {
                throw new IllegalStateException("EDS stopped during startup, see " + log.toAbsolutePath());
            }
        }
        throw new IllegalStateException("EDS wasn't ready within " + STARTUP + ", see " + log.toAbsolutePath());
    }

    @Override
    public void close() throws InterruptedException {
        try {
            stop();
        } finally {
            if (Thread.currentThread() != hook) {
                try {
                    Runtime.getRuntime().removeShutdownHook(hook);
                } catch (IllegalStateException _) {
                    // The JVM is stopping already; the hook stops the stone as well.
                }
            }
        }
    }

    // As above: the container's name is made here.
    @SuppressWarnings("findsecbugs:COMMAND_INJECTION")
    private synchronized void stop() throws InterruptedException {
        if (container != null) {
            // Stopping lets the JVM write its recording; the container goes with it, and its log ends.
            try {
                run(List.of(PODMAN, "stop", "--time", String.valueOf(STOP.toSeconds()), "--ignore", container));
                // The log's last lines, which tell why a stone stopped, are copied before the container goes.
                process.waitFor(LOG_END.toSeconds(), TimeUnit.SECONDS);
                run(List.of(PODMAN, "rm", "--force", "--ignore", container));
            } catch (IOException | IllegalStateException e) {
                System.err.println("The container " + container + " wasn't removed: " + e.getMessage());
            }
        } else {
            process.destroy();
        }
        if (!process.waitFor(STOP.toSeconds(), TimeUnit.SECONDS)) {
            process.destroyForcibly();
        }
    }

    private void stopQuietly() {
        try {
            stop();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
