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

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import net.haugr.eds.core.RotationBean;
import net.haugr.eds.core.rotation.CircleRotations.Handoff;
import net.haugr.eds.core.rotation.CircleRotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Runs the rotations of Circle Secrets in the background (PLAN.md, Crypto
 * C6): when a Trustee's request hands over the secret of a Circle whose
 * rotation is due or under way, the rotation runs on a thread of its own,
 * and the request goes on. The job starts when the request's transaction
 * has ended, so what the request writes with the secret it read is there
 * for the job to find. Each job re-encrypts several objects at a time, by
 * {@code eds.circle.secret.rotation.threads}.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ApplicationScoped
public class RotationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotationRunner.class);

    private final RotationBean rotationBean;
    private final TransactionSynchronizationRegistry transactions;
    private final ExecutorService jobs = Executors.newVirtualThreadPerTaskExecutor();

    @Inject
    public RotationRunner(final RotationBean rotationBean, final TransactionSynchronizationRegistry transactions) {
        this.rotationBean = rotationBean;
        this.transactions = transactions;
    }

    void start(@Observes final StartupEvent event) {
        CircleRotations.install(this::afterTheRequest);
    }

    private void afterTheRequest(final Handoff handoff) {
        if (transactions.getTransactionStatus() == Status.STATUS_ACTIVE) {
            transactions.registerInterposedSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {
                    // Nothing to do before.
                }

                @Override
                public void afterCompletion(final int status) {
                    // The secret is the Circle's whether the request
                    // succeeded or not.
                    run(handoff);
                }
            });
        } else {
            run(handoff);
        }
    }

    private void run(final Handoff handoff) {
        try {
            jobs.execute(() -> rotationBean.rotate(handoff));
        } catch (RejectedExecutionException e) {
            // The stone stops; the rotation continues after its start.
            LOGGER.debug("The rotation of the Circle {} isn't started, as the stone stops.", handoff.externalId(), e);
            CircleRotations.finished(handoff);
        }
    }

    void stop(@Observes final ShutdownEvent event) {
        CircleRotations.install(null);
        jobs.shutdownNow();
    }
}
