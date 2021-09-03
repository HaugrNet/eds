/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2021, haugr.net
 * mailto: cws AT haugr DOT net
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.cws.core.setup.fakes;

import javax.ejb.EJBException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

/**
 * This test Stub was extracted from the SanitizerBeanTest.
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class FakeTimerService implements TimerService {

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(final long duration, final Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createSingleActionTimer(final long duration, final TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(final long initialDuration, final long intervalDuration, final Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createIntervalTimer(final long initialDuration, final long intervalDuration, final TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(final Date expiration, final Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createSingleActionTimer(final Date expiration, final TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(final Date initialExpiration, final long intervalDuration, final Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createIntervalTimer(final Date initialExpiration, final long intervalDuration, final TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createCalendarTimer(final ScheduleExpression schedule) throws IllegalArgumentException, IllegalStateException, EJBException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createCalendarTimer(final ScheduleExpression schedule, final TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Timer> getTimers() throws IllegalStateException, EJBException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Timer> getAllTimers() throws IllegalStateException, EJBException {
        return null;
    }
}
