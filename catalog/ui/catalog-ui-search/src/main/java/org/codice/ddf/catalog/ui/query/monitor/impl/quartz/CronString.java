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

import static org.apache.commons.lang3.Validate.notNull;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.function.Supplier;

import org.quartz.Trigger;

/**
 * Supply a Quartz {@code Trigger} that is configured with a CRON string.
 */
public class CronString implements Supplier<Trigger> {

    private final String cronString;

    private final String identity;

    /**
     * @param cronString must be non-null
     * @param identity   must be non-null
     */
    public CronString(String cronString, String identity) {
        notNull(cronString, "cronString must be non-null");
        notNull(identity, "identity must be non-null");
        this.cronString = cronString;
        this.identity = identity;
    }

    @Override
    public String toString() {
        return "CronString{" +
                "cronString='" + cronString + '\'' +
                ", identity='" + identity + '\'' +
                ", =" + get() +
                '}';
    }

    @Override
    public Trigger get() {
        return newTrigger().withIdentity(identity)
                .withSchedule(cronSchedule(cronString))
                .build();
    }
}
