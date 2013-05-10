/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.platform.scheduler;

import java.util.Map;

import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

/**
 * Schedules a Command task
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 *
 */
public class ScheduledCommandTask implements ScheduledTask {

    private Class<? extends Job> classObject;

    private static int ONE_DAY = 60 * 60 * 24;

    private int intervalInSeconds = ONE_DAY;

    private String command;

    public void setCommand(String command) {
        this.command = command;
    }

    public void setIntervalInSeconds(int intervalInSeconds) {
        this.intervalInSeconds = intervalInSeconds;
    }

    private Scheduler scheduler;

    private JobKey jobKey;

    private TriggerKey triggerKey;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ScheduledCommandTask.class);

    /**
     * 
     * @param scheduler
     * @param classObject
     */
    public ScheduledCommandTask(Scheduler scheduler, Class classObject) {

        this.scheduler = scheduler;
        this.classObject = classObject;
    }

    @Override
    public void newTask() {

        LOGGER.debug("Creating new Task.");

        long identifier = System.currentTimeMillis();

        this.jobKey = new JobKey("job" + identifier,
                classObject.getSimpleName());

        this.triggerKey = new TriggerKey("trigger" + identifier,
                classObject.getSimpleName());

        JobDetail jobDetail = createJob();

        Trigger trigger = createTrigger();

        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            LOGGER.info("Error with scheduling of task.", e);
        }

    }

    @Override
    public void deleteTask() {
        try {
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            LOGGER.info("Error with deletion of task.", e);
        }
    }

    @Override
    public void updateTask(Map properties) {

        if (properties == null) {
            LOGGER.info("No properties detected. No action taken.");
            return;
        }
        Object commandValue = properties.get(CommandJob.COMMAND_KEY);

        Object intervalValue = properties.get("intervalInSeconds");

        if (commandValue != null) {
            this.command = commandValue.toString();
        }

        if (intervalValue != null) {
            this.intervalInSeconds = Integer.parseInt(intervalValue.toString());
        }

        JobDetail newJob = createJob();

        Trigger newTrigger = createTrigger();

        boolean overWritePreviousJob = true;

        try {
            scheduler.addJob(newJob, overWritePreviousJob);

            scheduler.rescheduleJob(triggerKey, newTrigger);
        } catch (SchedulerException e) {
            LOGGER.info("Error with rescheduling of task.", e);
        }

    }

    private JobDetail createJob() {
        return newJob().ofType(classObject).withIdentity(jobKey).storeDurably()
                .usingJobData(CommandJob.COMMAND_KEY, command).build();
    }

    private Trigger createTrigger() {
        return newTrigger()
                .withIdentity(triggerKey)
                .startNow()
                .withSchedule(
                        simpleSchedule().withIntervalInSeconds(
                                intervalInSeconds).repeatForever()).build();
    }
}
