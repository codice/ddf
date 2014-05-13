/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.platform.scheduler;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ITestScheduledTask {

    private static final int MILLISECONDS_IN_ONE_SECOND = 1000;

    private static Scheduler scheduler;

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandJob.class);

    @BeforeClass
    public static void setupClass() {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            LOGGER.error("Error while starting scheduler", e);
        }

    }

    @AfterClass
    public static void destroy() throws SchedulerException {
        scheduler.shutdown();
    }

    @Test
    public void testSimpleCase() throws Exception {

        FirstArgumentAnswer captureInput = new FirstArgumentAnswer();

        CommandSession session = givenSession(captureInput);

        setCommandProcessor(session);

        int numberOfSeconds = 1;

        ScheduledCommandTask task = new ScheduledCommandTask(scheduler, CommandJob.class);

        task.setIntervalInSeconds(numberOfSeconds);

        String command = "info";

        task.setCommand(command);
        // CREATE
        int expectedRuns = 3;

        int buffer = 500;

        task.newTask();

        Thread.sleep((expectedRuns - 1) * numberOfSeconds * MILLISECONDS_IN_ONE_SECOND + buffer);

        LOGGER.info("Verifying {} command was run", command);

        verify(session, times(expectedRuns)).execute(command);

        // UPDATE
        String newCommand = "newInfo";
        task.setCommand(newCommand);
        numberOfSeconds = 2;
        task.setIntervalInSeconds(numberOfSeconds);
        int updatedExpectedRuns = 2;

        task.updateTask(new HashMap<String, Object>());

        Thread.sleep((updatedExpectedRuns - 1) * numberOfSeconds * MILLISECONDS_IN_ONE_SECOND
                + buffer);

        LOGGER.debug("Verifying {} command was run", newCommand);
        verify(session, times(updatedExpectedRuns)).execute(newCommand);

        assertThat(captureInput.getInputArg(), is(newCommand));

        // DELETE
        task.deleteTask();

        Thread.sleep((updatedExpectedRuns - 1) * numberOfSeconds * MILLISECONDS_IN_ONE_SECOND
                + buffer);

        verify(session, times(updatedExpectedRuns)).execute(newCommand);
        verify(session, times(expectedRuns)).execute(command);
    }

    private CommandSession givenSession(Answer<String> captureInput) {
        CommandSession session = mock(CommandSession.class);

        try {
            when(session.execute(isA(CharSequence.class))).then(captureInput);

        } catch (Exception e) {
            LOGGER.error("Exception occurred during command session", e);
        }
        return session;
    }

    private CommandProcessor getCommandProcessor(CommandSession session) {
        CommandProcessor processor = mock(CommandProcessor.class);

        when(
                processor.createSession(isNull(InputStream.class), isA(PrintStream.class),
                        isA(PrintStream.class))).thenReturn(session);
        return processor;

    }

    private void setCommandProcessor(CommandSession session) {

        ServiceStore store = ServiceStore.getInstance();

        store.setObject(getCommandProcessor(session));
    }

}
