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

import jakarta.enterprise.inject.spi.CDI;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import net.haugr.eds.api.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>The WebSocket of the stone (FEATURES.md), {@code /api/v1/socket}, e.g.
 * {@code ws://localhost:8080/api/v1/socket}. It sits beside the REST API,
 * not below its application path, so its path names the API root itself.
 * Frames are JSON text frames, see
 * {@link net.haugr.eds.api.dtos.SocketFrame}. The connection is
 * authenticated by its first frame, AUTH with the Session token, as
 * browsers can't add an Authorization header to a WebSocket; a cross-site
 * page therefore can't use a browser's connection without the token, so the
 * Origin isn't checked.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ServerEndpoint(Constants.WS_SOCKET)
public class SocketEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketEndpoint.class);

    /**
     * Largest frame, in bytes. An entry whose content doesn't fit, Base64
     * and all, is written with the resource API.
     */
    static final int MAX_FRAME_BYTES = 256 * 1024;

    private SocketHub hub = null;

    @OnOpen
    public void open(final Session session) {
        session.setMaxTextMessageBufferSize(MAX_FRAME_BYTES);
        hub().open(new SessionConnection(session));
    }

    @OnMessage(maxMessageSize = MAX_FRAME_BYTES)
    public void message(final Session session, final String text) {
        hub().receive(session.getId(), text);
    }

    @OnClose
    public void close(final Session session) {
        hub().closed(session.getId());
    }

    @OnError
    public void error(final Session session, final Throwable cause) {
        LOGGER.debug("Socket connection {} failed: {}", session.getId(), cause.getMessage());
    }

    private SocketHub hub() {
        if (hub == null) {
            hub = CDI.current().select(SocketHub.class).get();
        }
        return hub;
    }

    private record SessionConnection(Session session) implements SocketConnection {

        @Override
        public String id() {
            return session.getId();
        }

        @Override
        public void send(final String text) throws IOException {
            if (session.isOpen()) {
                session.getBasicRemote().sendText(text);
            }
        }

        @Override
        public void close(final int code, final String reason) {
            try {
                if (session.isOpen()) {
                    session.close(new CloseReason(CloseReason.CloseCodes.getCloseCode(code), reason));
                }
            } catch (IOException e) {
                LOGGER.debug("Closing socket connection {} failed: {}", session.getId(), e.getMessage());
            }
        }
    }
}
