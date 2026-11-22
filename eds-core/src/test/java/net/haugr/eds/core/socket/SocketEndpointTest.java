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
package net.haugr.eds.core.socket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.websocket.CloseReason;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.haugr.eds.api.common.ReturnCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The WebSocket of the stone (FEATURES.md), which passes its sessions to the
 * hub. The sessions of these tests are in memory, and the hub is the one CDI
 * gives, here a hub without features: a connection which doesn't start with
 * AUTH is answered and closed without them.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class SocketEndpointTest {

    private static final long WAIT_SECONDS = 20;
    private static final String NOT_AUTH = "{\"type\":\"JOIN\"}";
    /** What CDI gives, while these tests run. */
    private static final AtomicReference<CDI<Object>> CURRENT = new AtomicReference<>();

    private static SocketHub hub = null;
    private final AtomicInteger ids = new AtomicInteger();

    @BeforeAll
    static void startCdi() {
        hub = new SocketHub(null, 200);
        CURRENT.set(new HubCdi(hub));
        CDI.setCDIProvider(CURRENT::get);
    }

    @AfterAll
    static void stopCdi() {
        // A provider which gives nothing is passed over.
        CURRENT.set(null);
        hub.shutdown();
    }

    @Test
    void testASessionIsAConnectionOfTheHub() throws InterruptedException, IOException {
        final SocketEndpoint endpoint = new SocketEndpoint();
        final FakeSession fake = new FakeSession(true, false);
        try (Session session = fake.session()) {
            endpoint.open(session);
            assertEquals(SocketEndpoint.MAX_FRAME_BYTES, fake.maxTextMessageBufferSize.get());
            endpoint.message(session, NOT_AUTH);

            final String refused = fake.sent.poll(WAIT_SECONDS, TimeUnit.SECONDS);
            assertNotNull(refused);
            assertTrue(refused.contains("\"returnCode\":" + ReturnCode.AUTHENTICATION_WARNING.getCode()), refused);
            assertTrue(fake.closing.await(WAIT_SECONDS, TimeUnit.SECONDS));
            assertEquals(SocketConnection.POLICY_VIOLATION, fake.closeReason.get().getCloseCode().getCode());

            // The hub had closed the connection already.
            assertDoesNotThrow(() -> endpoint.close(session));
        }
    }

    @Test
    void testAClosedSessionIsSentNothing() throws InterruptedException, IOException {
        final SocketEndpoint endpoint = new SocketEndpoint();
        final FakeSession fake = new FakeSession(false, false);
        try (Session session = fake.session()) {
            endpoint.open(session);
            endpoint.message(session, NOT_AUTH);

            // The answer and the close each ask whether the session is open, in turn.
            assertTrue(fake.asked.await(WAIT_SECONDS, TimeUnit.SECONDS));
            assertTrue(fake.sent.isEmpty());
            assertNull(fake.closeReason.get());
        }
    }

    @Test
    void testASessionWhichFailsToCloseIsLeft() throws InterruptedException, IOException {
        final SocketEndpoint endpoint = new SocketEndpoint();
        final FakeSession fake = new FakeSession(true, true);
        try (Session session = fake.session()) {
            endpoint.open(session);
            endpoint.message(session, NOT_AUTH);

            assertNotNull(fake.sent.poll(WAIT_SECONDS, TimeUnit.SECONDS));
            assertTrue(fake.closing.await(WAIT_SECONDS, TimeUnit.SECONDS));
            assertEquals(SocketConnection.POLICY_VIOLATION, fake.closeReason.get().getCloseCode().getCode());
            assertDoesNotThrow(() -> endpoint.close(session));
        }
    }

    @Test
    void testAnErrorOfASessionIsOnlyNoted() {
        final SocketEndpoint endpoint = new SocketEndpoint();
        final FakeSession fake = new FakeSession(true, false);

        assertDoesNotThrow(() -> endpoint.error(fake.session(), new IOException("The network is gone.")));
        assertTrue(fake.sent.isEmpty());
        assertNull(fake.closeReason.get());
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    /** A WebSocket session in memory, which keeps what it is sent. */
    private final class FakeSession {

        private final String id = "session-" + ids.incrementAndGet();
        private final boolean open;
        private final boolean failsToClose;
        private final BlockingQueue<String> sent = new LinkedBlockingQueue<>();
        private final AtomicInteger maxTextMessageBufferSize = new AtomicInteger();
        private final AtomicReference<CloseReason> closeReason = new AtomicReference<>();
        private final CountDownLatch closing = new CountDownLatch(1);
        private final CountDownLatch asked = new CountDownLatch(2);

        private FakeSession(final boolean open, final boolean failsToClose) {
            this.open = open;
            this.failsToClose = failsToClose;
        }

        private Session session() {
            final RemoteEndpoint.Basic remote = proxy(RemoteEndpoint.Basic.class, (method, arguments) -> {
                if ("sendText".equals(method.getName()) && (arguments.length == 1)) {
                    sent.add((String) arguments[0]);
                    return null;
                }
                throw new UnsupportedOperationException(method.getName());
            });

            return proxy(Session.class, (method, arguments) -> switch (method.getName()) {
                case "getId" -> id;
                case "isOpen" -> {
                    asked.countDown();
                    yield open;
                }
                case "getBasicRemote" -> remote;
                case "setMaxTextMessageBufferSize" -> {
                    maxTextMessageBufferSize.set((Integer) arguments[0]);
                    yield null;
                }
                // close() without a reason is only called by the tests' try-with-resources.
                case "close" -> ((arguments == null) || (arguments.length == 0)) ? null : close((CloseReason) arguments[0]);
                default -> throw new UnsupportedOperationException(method.getName());
            });
        }

        private Object close(final CloseReason reason) throws IOException {
            closeReason.set(reason);
            closing.countDown();
            if (failsToClose) {
                throw new IOException("The session can't be closed.");
            }
            return null;
        }
    }

    /** What a method of a proxy does. */
    @FunctionalInterface
    private interface Behaviour {

        Object invoke(Method method, Object[] arguments) throws IOException;
    }

    private static <T> T proxy(final Class<T> type, final Behaviour behaviour) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, arguments) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxy == arguments[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> type.getSimpleName();
                };
            }
            return behaviour.invoke(method, (arguments != null) ? arguments : new Object[0]);
        }));
    }

    /** CDI, which gives the hub of the tests and nothing else. */
    private static final class HubCdi extends CDI<Object> {

        private final SocketHub socketHub;

        private HubCdi(final SocketHub socketHub) {
            this.socketHub = socketHub;
        }

        @Override
        public <U> Instance<U> select(final Class<U> subtype, final Annotation... qualifiers) {
            return new Given<>(subtype.cast(socketHub));
        }

        @Override
        public BeanManager getBeanManager() {
            throw new UnsupportedOperationException("Not needed by the endpoint.");
        }

        @Override
        public Instance<Object> select(final Annotation... qualifiers) {
            throw new UnsupportedOperationException("Not needed by the endpoint.");
        }

        @Override
        public <U> Instance<U> select(final TypeLiteral<U> subtype, final Annotation... qualifiers) {
            throw new UnsupportedOperationException("Not needed by the endpoint.");
        }

        @Override
        public boolean isUnsatisfied() {
            return false;
        }

        @Override
        public boolean isAmbiguous() {
            return false;
        }

        @Override
        public void destroy(final Object instance) {
            throw new UnsupportedOperationException("Not needed by the endpoint.");
        }

        @Override
        public Handle<Object> getHandle() {
            throw new UnsupportedOperationException("Not needed by the endpoint.");
        }

        @Override
        public Iterable<? extends Handle<Object>> handles() {
            throw new UnsupportedOperationException("Not needed by the endpoint.");
        }

        @Override
        public Object get() {
            throw new UnsupportedOperationException("Not needed by the endpoint.");
        }

        @Override
        public Iterator<Object> iterator() {
            throw new UnsupportedOperationException("Not needed by the endpoint.");
        }
    }
}
