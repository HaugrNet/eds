/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core;

import io.javadog.cws.core.model.Settings;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.logging.Logger;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Startup
@Singleton
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class StartupBean {

    private static final Logger log = Logger.getLogger(StartupBean.class.getName());

    @Inject private SettingBean settingBean;
    @Inject private SanitizerBean sanitizerBean;
    @Resource private TimerService timerService;

    @PostConstruct
    public void startup() {
        log.info("Initializing the CWS Sanitizer Service.");

        // If requested, then simply start the sanitize as a background job
        // now. The job will process small blocks of code and save these.
        if (settingBean.getSettings().getSanityStartup()) {
            sanitizerBean.sanitize();
        }

        // Registering the Timer Service. This will ensure that the Scheduler
        // is invoked at frequent intervals.
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo("CWS Sanitizer");

        // Once started, the next run should always occur as planned, regardless
        // of restarts, as it is not guaranteed that the sanitizing is performed
        // at startup.
        timerConfig.setPersistent(true);

        final ScheduleExpression expression = new ScheduleExpression();
        timerService.createCalendarTimer(expression, timerConfig);
        log.log(Settings.INFO, "First scheduled sanitizing will begin at {}", expression);
    }

    @Timeout
    public void timerservice(final Timer timer) {
        log.log(Settings.INFO, "Starting Timed Sanitizing Service.");
        sanitizerBean.sanitize();
    }
}
