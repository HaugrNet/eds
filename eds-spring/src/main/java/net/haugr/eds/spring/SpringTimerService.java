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
package net.haugr.eds.spring;

import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import org.springframework.stereotype.Component;

/**
 * <p>Spring is not supporting the Jakarta EE TimerService, so to avoid
 * making too many changes in the core module, this dummy class is provided
 * as a producer for the places where the TimerService is injected.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Component
public class SpringTimerService implements TimerService {

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createCalendarTimer(ScheduleExpression schedule) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createCalendarTimer(ScheduleExpression schedule, TimerConfig timerConfig) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createIntervalTimer(long initialDuration, long intervalDuration, TimerConfig timerConfig) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createIntervalTimer(Date initialExpiration, long intervalDuration, TimerConfig timerConfig) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createSingleActionTimer(long duration, TimerConfig timerConfig) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createSingleActionTimer(Date expiration, TimerConfig timerConfig) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(long duration, Serializable info) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(long initialDuration, long intervalDuration, Serializable info) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(Date expiration, Serializable info) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(Date expiration, long intervalDuration, Serializable info) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Timer> getTimers() {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Timer> getAllTimers() {
        return Collections.emptyList();
    }
}
