/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.platform.scheduler;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schedules a Command task
 *
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 */
public class ScheduledCommandTask implements ScheduledTask {

  public static final String SECOND_INTERVAL = "secondInterval";

  public static final String CRON_STRING = "cronString";

  public static final String INTERVAL_STRING = "intervalString";

  public static final String INTERVAL_TYPE = "intervalType";

  public static final String ONE_DAY = "0 0 0 1/1 * ? *";

  private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledCommandTask.class);

  private Class<? extends Job> jobClass;

  private String intervalString = ONE_DAY;

  private String intervalType = CRON_STRING;

  private String command;

  private Scheduler scheduler;

  private JobKey jobKey;

  private TriggerKey triggerKey;

  /**
   * @param scheduler - A reference to the Quartz Scheduler
   * @param jobClass - A reference to the Quartz Job Implementing Class
   */
  public ScheduledCommandTask(Scheduler scheduler, Class jobClass) {
    this.scheduler = scheduler;
    this.jobClass = jobClass;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public String getCommand() {
    return command;
  }

  public void setIntervalString(String intervalString) {
    if (StringUtils.isNotEmpty(intervalString)) {
      this.intervalString = intervalString;
    }
  }

  public String getIntervalString() {
    return intervalString;
  }

  public void setIntervalType(String intervalType) {
    if (StringUtils.isNotEmpty(intervalType)) {
      this.intervalType = intervalType;
    }
  }

  public String getIntervalType() {
    return intervalType;
  }

  @Override
  public void newTask() {
    LOGGER.trace("Creating new Task.");

    long identifier = System.currentTimeMillis();

    this.jobKey = new JobKey("job" + identifier, jobClass.getSimpleName());

    this.triggerKey = new TriggerKey("trigger" + identifier, jobClass.getSimpleName());

    JobDetail jobDetail = createJob();

    Trigger trigger = createTrigger();

    if (trigger == null) {
      return;
    }

    try {
      scheduler.scheduleJob(jobDetail, trigger);
    } catch (SchedulerException e) {
      LOGGER.info("Error with scheduling of task.", e);
    }
  }

  @Override
  public void deleteTask(int code) {
    try {
      scheduler.deleteJob(jobKey);
    } catch (SchedulerException e) {
      LOGGER.info("Error with deletion of task.", e);
    }
  }

  @Override
  public void updateTask(Map<String, Object> properties) {
    if (MapUtils.isEmpty(properties)) {
      LOGGER.info("Empty or null properties map. No action taken.");
      return;
    }

    Object commandValue = properties.get(CommandJob.COMMAND_KEY);
    if (commandValue != null) {
      this.command = commandValue.toString();
    }

    Object intervalString = properties.get(INTERVAL_STRING);
    if (intervalString != null) {
      LOGGER.debug("Updating intervalString : {}", intervalString);
      this.intervalString = (String) intervalString;
    }

    Object intervalType = properties.get(INTERVAL_TYPE);
    if (intervalType != null) {
      LOGGER.debug("Updating intervalType : {}", intervalType);
      this.intervalType = (String) intervalType;
    }

    JobDetail newJob = createJob();
    Trigger newTrigger = createTrigger();

    if (newTrigger == null) {
      return;
    }

    try {
      scheduler.addJob(newJob, true);
      scheduler.rescheduleJob(triggerKey, newTrigger);
    } catch (SchedulerException e) {
      LOGGER.info("Error with rescheduling of task.", e);
    }
  }

  private JobDetail createJob() {
    return newJob()
        .ofType(jobClass)
        .withIdentity(jobKey)
        .storeDurably()
        .usingJobData(CommandJob.COMMAND_KEY, command)
        .build();
  }

  private Trigger createTrigger() {
    Trigger newTrigger = null;
    if (CRON_STRING.equals(this.intervalType)) {
      newTrigger = createCronTrigger();
    } else if (SECOND_INTERVAL.equals(this.intervalType)) {
      try {
        Integer secondsInterval = Integer.parseInt(this.intervalString);
        newTrigger = createSimpleTrigger(secondsInterval);
      } catch (NumberFormatException e) {
        LOGGER.warn(
            "Unable to update platform scheduler.  Invalid second interval specified : {}",
            this.intervalString,
            e);
      }
    } else {
      LOGGER.warn(
          "Unable to update platform scheduler with given interval type : {}", this.intervalType);
    }
    return newTrigger;
  }

  private Trigger createSimpleTrigger(int secondsInterval) {
    LOGGER.debug("Creating trigger with {} second interval", secondsInterval);
    return newTrigger()
        .withIdentity(triggerKey)
        .startNow()
        .withSchedule(simpleSchedule().withIntervalInSeconds(secondsInterval).repeatForever())
        .build();
  }

  private Trigger createCronTrigger() {
    LOGGER.debug("Creating trigger with cron string : {}", intervalString);
    return newTrigger()
        .withIdentity(triggerKey)
        .startNow()
        .withSchedule(cronSchedule(intervalString))
        .build();
  }
}
