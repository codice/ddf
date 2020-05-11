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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

public class ScheduledCommandTaskTest {

  private static final String INFO = "info";

  private Scheduler scheduler;

  private ScheduledCommandTask scheduledCommandTask;

  @Before
  public void setUp() {
    scheduler = mock(Scheduler.class);
    scheduledCommandTask = new ScheduledCommandTask(scheduler, CommandJob.class);
    scheduledCommandTask.setCommand(INFO);
  }

  @Test
  public void testGettersAndSetters() {
    assertThat(scheduledCommandTask.getIntervalString(), is(ScheduledCommandTask.ONE_DAY));
    assertThat(scheduledCommandTask.getIntervalType(), is(ScheduledCommandTask.CRON_STRING));
    assertThat(scheduledCommandTask.getCommand(), is(INFO));
  }

  @Test
  public void testSetIntervalString() {
    String cronString = "0 0 0 0 0 0";
    scheduledCommandTask.setIntervalString(cronString);
    assertThat(scheduledCommandTask.getIntervalString(), is(cronString));
  }

  @Test
  public void testSetIntervalType() {
    scheduledCommandTask.setIntervalType(ScheduledCommandTask.SECOND_INTERVAL);
    assertThat(scheduledCommandTask.getIntervalType(), is(ScheduledCommandTask.SECOND_INTERVAL));
  }

  @Test
  public void testEmptyIntervalString() {
    scheduledCommandTask.setIntervalString("");
    assertThat(scheduledCommandTask.getIntervalString(), is(ScheduledCommandTask.ONE_DAY));
  }

  @Test
  public void testEmptyIntervalType() {
    scheduledCommandTask.setIntervalType("");
    assertThat(scheduledCommandTask.getIntervalType(), is(ScheduledCommandTask.CRON_STRING));
  }

  @Test
  public void testCreateSimpleTrigger() {
    scheduledCommandTask.setIntervalString("1");
    scheduledCommandTask.setIntervalType(ScheduledCommandTask.SECOND_INTERVAL);
    scheduledCommandTask.newTask();
  }

  @Test
  public void testCreateSimpleTriggerNonInteger() {
    scheduledCommandTask.setIntervalString("1s");
    scheduledCommandTask.setIntervalType(ScheduledCommandTask.SECOND_INTERVAL);
    scheduledCommandTask.newTask();
  }

  @Test
  public void testNewTaskScheduleException() throws Exception {
    when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class)))
        .thenThrow(new SchedulerException());
    scheduledCommandTask.newTask();
  }

  @Test
  public void testDeleteTaskScheduleException() throws Exception {
    when(scheduler.deleteJob(any(JobKey.class))).thenThrow(new SchedulerException());
    scheduledCommandTask.deleteTask(2);
  }

  @Test
  public void testNewTaskSecondInterval() {
    scheduledCommandTask.newTask();
  }

  @Test
  public void testBadIntervalType() {
    scheduledCommandTask.setIntervalType("badType");
    scheduledCommandTask.newTask();
  }

  @Test
  public void testUpdateNullPropertiesMap() {
    scheduledCommandTask.updateTask(null);
    assertThat(scheduledCommandTask.getIntervalString(), is(ScheduledCommandTask.ONE_DAY));
    assertThat(scheduledCommandTask.getIntervalType(), is(ScheduledCommandTask.CRON_STRING));
  }

  @Test
  public void testUpdateEmptyProperties() {
    scheduledCommandTask.updateTask(new HashMap<>());
    assertThat(scheduledCommandTask.getIntervalString(), is(ScheduledCommandTask.ONE_DAY));
    assertThat(scheduledCommandTask.getIntervalType(), is(ScheduledCommandTask.CRON_STRING));
  }

  @Test
  public void testDeleteJob() {
    scheduledCommandTask.deleteTask(1);
  }

  @Test
  public void testUpdate() {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put(CommandJob.COMMAND_KEY, "info2");
    properties.put(ScheduledCommandTask.INTERVAL_STRING, "1");
    properties.put(ScheduledCommandTask.INTERVAL_TYPE, ScheduledCommandTask.SECOND_INTERVAL);
    scheduledCommandTask.updateTask(properties);
    assertThat(scheduledCommandTask.getIntervalString(), is("1"));
    assertThat(scheduledCommandTask.getIntervalType(), is(ScheduledCommandTask.SECOND_INTERVAL));
  }

  @Test
  public void testUpdateNullProperties() {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put(CommandJob.COMMAND_KEY, null);
    properties.put(ScheduledCommandTask.INTERVAL_STRING, null);
    properties.put(ScheduledCommandTask.INTERVAL_TYPE, null);
    scheduledCommandTask.updateTask(properties);
    assertThat(scheduledCommandTask.getIntervalString(), is(ScheduledCommandTask.ONE_DAY));
    assertThat(scheduledCommandTask.getIntervalType(), is(ScheduledCommandTask.CRON_STRING));
    assertThat(scheduledCommandTask.getCommand(), is(INFO));
  }

  @Test
  public void testUpdatePropertiesSchedulerException() throws SchedulerException {
    when(scheduler.rescheduleJob(any(TriggerKey.class), any(Trigger.class)))
        .thenThrow(new SchedulerException());
    HashMap<String, Object> properties = new HashMap<>();
    properties.put(CommandJob.COMMAND_KEY, INFO);
    properties.put(ScheduledCommandTask.INTERVAL_STRING, "1");
    properties.put(ScheduledCommandTask.INTERVAL_TYPE, ScheduledCommandTask.SECOND_INTERVAL);
    scheduledCommandTask.updateTask(properties);
    assertThat(scheduledCommandTask.getIntervalString(), is("1"));
    assertThat(scheduledCommandTask.getIntervalType(), is(ScheduledCommandTask.SECOND_INTERVAL));
    assertThat(scheduledCommandTask.getCommand(), is(INFO));
  }

  @Test
  public void testUpdatePropertiesInvalidConfig() throws SchedulerException {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put(CommandJob.COMMAND_KEY, INFO);
    properties.put(ScheduledCommandTask.INTERVAL_STRING, "1s");
    properties.put(ScheduledCommandTask.INTERVAL_TYPE, ScheduledCommandTask.SECOND_INTERVAL);
    scheduledCommandTask.updateTask(properties);
    assertThat(scheduledCommandTask.getIntervalString(), is("1s"));
    assertThat(scheduledCommandTask.getIntervalType(), is(ScheduledCommandTask.SECOND_INTERVAL));
    assertThat(scheduledCommandTask.getCommand(), is(INFO));
  }
}
