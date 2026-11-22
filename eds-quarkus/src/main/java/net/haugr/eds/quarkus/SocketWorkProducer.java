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
package net.haugr.eds.quarkus;

import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.concurrent.Executor;
import net.haugr.eds.core.socket.SocketHub;

/**
 * <p>Gives the socket hub the worker pool of Quarkus, the one which handles
 * the REST requests, to handle the frames of the socket on. Requests and
 * frames then share the CPU alike: on virtual threads of their own, the
 * frames got only as many carrier threads as there are CPUs, against the
 * many threads of the pool, and so far less of a busy CPU (the load test,
 * 2026-11-19).</p>
 *
 * <p>Quarkus makes its worker pool the default worker pool of Mutiny.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ApplicationScoped
public class SocketWorkProducer {

    /**
     * The Executor which handles the frames of the socket.
     *
     * @return The worker pool of Quarkus
     */
    @Produces
    // Singleton, not ApplicationScoped: the pool is Quarkus' own and lives as long as EDS, so a client
    // proxy would only add a call to every frame of the socket.
    @Singleton
    @Named(SocketHub.WORK)
    public Executor socketWork() {
        return Infrastructure.getDefaultWorkerPool();
    }
}
