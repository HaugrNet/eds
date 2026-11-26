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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The commands with which {@link TestStone} runs the stone in a container
 * (ticket 29), and when it does.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class TestStoneTest {

    @TempDir
    Path target;

    @Test
    void testTheStoneRunsInAContainerWhenPodmanIsThere() throws IOException {
        final Path bin = Files.createDirectories(target.resolve("bin"));
        assertTrue(Files.createFile(bin.resolve("podman")).toFile().setExecutable(true));
        final String path = "/nowhere" + File.pathSeparator + bin;
        assertTrue(TestStone.inContainer(null, path));
        assertTrue(TestStone.inContainer("true", path));
        assertFalse(TestStone.inContainer("false", path));
        assertFalse(TestStone.inContainer(null, "/nowhere"));
        assertFalse(TestStone.inContainer(null, null));
    }

    @Test
    void testALeftOverContainerIsRemovedBeforeTheStart() {
        final Path directory = target.resolve("eds-quarkus");
        final Path jar = directory.resolve("eds-stone.jar");
        final List<List<String>> commands = TestStone.startCommands("eds-load-stone", Path.of("../accessories/podman/Containerfile"),
                jar, 1795560000L, target, "1000:1000", "file:///eds/target/load-masterkey",
                List.of("-Xmx6g", "-Dquarkus.http.port=18090"), Set.of());
        assertEquals(3, commands.size());
        assertEquals(List.of("podman", "build", "--quiet", "--format", "docker", "--file", "../accessories/podman/Containerfile",
                "--tag", "eds-test-stone:local", "--build-arg", "JAR=eds-stone.jar", "--label", "net.haugr.eds.jar-modified=1795560000",
                directory.toString()), commands.get(0));
        assertEquals(List.of("podman", "rm", "--force", "--ignore", "eds-load-stone"), commands.get(1));
        assertEquals("run", commands.get(2).get(1));
    }

    @Test
    void testTheStoneRunsOnTheHostsNetworkAsTheOwnerOfTheTarget() {
        final List<String> run = TestStone.runCommand("eds-load-stone", target, "1000:1001",
                "file:///eds/target/load-masterkey", List.of("-Xmx6g", "-Dquarkus.http.port=18090"),
                Set.of("PATH", "HOME", "EDS_DB_URL", "EDS_DB_PASSWORD", "EDS_MASTERKEY_URL", "QUARKUS_LOG_LEVEL"));
        assertEquals(List.of("podman", "run", "--detach", "--name", "eds-load-stone", "--network", "host", "--userns", "keep-id",
                "--user", "1000:1001", "--no-healthcheck", "--security-opt", "label=disable", "--volume", target + ":/eds/target",
                "--env", "EDS_MASTERKEY_URL=file:///eds/target/load-masterkey",
                "--env", "EDS_DB_PASSWORD", "--env", "EDS_DB_URL", "--env", "QUARKUS_LOG_LEVEL",
                "--entrypoint", "java", "eds-test-stone:local", "-Xmx6g", "-Dquarkus.http.port=18090", "-jar", "/app/eds.jar"), run);
        // The password is passed by name only; variables which aren't set, or aren't EDS's or Quarkus', aren't passed.
        assertFalse(run.stream().anyMatch(argument -> argument.startsWith("EDS_DB_PASSWORD=")));
        assertFalse(run.contains("EDS_DB_USERNAME"));
        assertFalse(run.contains("PATH"));
        assertFalse(run.contains("HOME"));
    }

    @Test
    void testTheRecordingKeepsTheWholeRun() {
        // JFR's default would keep 250 MB, the last 15 minutes of a heavy run (ticket 37).
        assertEquals("-XX:StartFlightRecording=filename=/eds/target/load-stone.jfr,settings=profile,maxsize=8g,dumponexit=true",
                TestStone.recordingOption("/eds/target"));
    }

    @Test
    void testAnInterruptedCloseLeavesNoShutdownHook() throws IOException, InterruptedException {
        final Process process = new ProcessBuilder("sleep", "60").start();
        // Closed once more at the end, which finds the process stopped.
        try (TestStone stone = new TestStone(process, null, target.resolve("stone.log"), 0, 0)) {
            Thread.currentThread().interrupt();
            assertThrows(InterruptedException.class, stone::close);
            assertFalse(Runtime.getRuntime().removeShutdownHook(stone.hook()), "the hook stayed registered");
        } finally {
            Thread.interrupted();
            process.destroyForcibly();
        }
    }
}
