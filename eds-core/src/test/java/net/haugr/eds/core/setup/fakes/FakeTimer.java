/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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
package net.haugr.eds.core.setup.fakes;

import jakarta.ejb.EJBException;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerHandle;
import java.io.Serializable;
import java.util.Date;

/**
 * This test Stub was extracted from the SanitizerBeanTest.
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class FakeTimer implements Timer {

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel() throws IllegalStateException, EJBException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTimeRemaining() throws IllegalStateException, EJBException {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getNextTimeout() throws IllegalStateException, EJBException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduleExpression getSchedule() throws IllegalStateException, EJBException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPersistent() throws IllegalStateException, EJBException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCalendarTimer() throws IllegalStateException, EJBException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Serializable getInfo() throws IllegalStateException, EJBException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimerHandle getHandle() throws IllegalStateException, EJBException {
        return null;
    }
}
