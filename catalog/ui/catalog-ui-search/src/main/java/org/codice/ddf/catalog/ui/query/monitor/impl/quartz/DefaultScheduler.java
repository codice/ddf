/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.ui.query.monitor.impl.quartz;

import java.util.Optional;
import java.util.function.Supplier;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultScheduler implements Supplier<Optional<Scheduler>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultScheduler.class);

    @Override
    public Optional<Scheduler> get() {
        try {
            return Optional.of(StdSchedulerFactory.getDefaultScheduler());
        } catch (SchedulerException e) {
            LOGGER.warn("unable to get default quartz scheduler", e);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "DefaultScheduler{}";
    }
}
