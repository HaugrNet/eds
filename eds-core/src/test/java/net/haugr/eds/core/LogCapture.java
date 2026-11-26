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
package net.haugr.eds.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 * Captures what a class logs at WARN and above, for the tests which check
 * that a failure is logged. The logging of the tests is off otherwise, and
 * nothing captured goes on to the console. Only what the capturing thread
 * logs is kept. The tests of a JVM run one after another; two captures of
 * one logger at the same time are not supported.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class LogCapture implements AutoCloseable {

    private final Logger logger;
    private final Level level;
    private final boolean additive;
    private final String thread = Thread.currentThread().getName();
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    private LogCapture(final Class<?> type) {
        logger = (Logger) LoggerFactory.getLogger(type);
        level = logger.getLevel();
        additive = logger.isAdditive();
        appender.start();
        logger.addAppender(appender);
        logger.setAdditive(false);
        logger.setLevel(Level.WARN);
    }

    /**
     * Starts capturing what the given class logs at WARN and above.
     *
     * @param type The class whose logger is captured
     * @return The capture, to close when done
     */
    public static LogCapture of(final Class<?> type) {
        return new LogCapture(type);
    }

    /**
     * The messages logged as WARN, formatted.
     *
     * @return The messages, in order
     */
    public List<String> warnings() {
        return appender.list.stream()
                .filter(event -> (event.getLevel() == Level.WARN) && thread.equals(event.getThreadName()))
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        logger.setLevel(level);
        logger.setAdditive(additive);
        appender.stop();
    }
}
