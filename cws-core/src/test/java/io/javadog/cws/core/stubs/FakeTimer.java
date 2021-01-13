/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
 * mailto: cws AT JavaDog DOT io
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
package io.javadog.cws.core.stubs;

import javax.ejb.EJBException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerHandle;
import java.io.Serializable;
import java.util.Date;

/**
 * This test Stub was extracted from the SanitizerBeanTest.
 *
 * @author Kim Jensen
 * @since CWS 1.0
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
