/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
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
import java.util.concurrent.Callable;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.Subject;

/**
 * Tests the {@link CommandJob} class.
 *
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 */
public class TestCommandJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestCommandJob.class);

    /**
     * Do no execution when no command processor available
     *
     * @throws Exception
     */
    @Test
    public void testNoCommandProcessor() throws Exception {

        // given
        String command = "info";

        CommandJob job = getCommandJob();

        // when
        job.execute(getJobExecutionContext(command));

        // then
        /* we should not have a problem */
    }

    /**
     * Do not execute command on null input
     *
     * @throws Exception
     */
    @Test
    public void testNullCommand() throws Exception {

        // given
        String command = null;

        FirstArgumentAnswer captureInput = new FirstArgumentAnswer();

        CommandSession session = getSession(captureInput);

        setCommandProcessor(session);

        CommandJob job = getCommandJob();

        // when
        job.execute(getJobExecutionContext(command));

        // then
        verifySessionCalls(command, session, 0, 1);

    }

    /**
     * An empty command will be executed
     *
     * @throws Exception
     */
    @Test
    public void testEmptyCommand() throws Exception {
        // given
        String command = "";

        FirstArgumentAnswer captureInput = new FirstArgumentAnswer();

        CommandSession session = getSession(captureInput);

        setCommandProcessor(session);

        CommandJob job = getCommandJob();

        // when
        job.execute(getJobExecutionContext(command));

        // then
        assertThat(captureInput.getInputArg(), is(command));

        verifySessionCalls(command, session, 1, 1);

    }

    /**
     * Tests the simplest command will be executed.
     *
     * @throws Exception
     */
    @Test
    public void testSimpleCommand() throws Exception {
        // given
        String command = "info";

        FirstArgumentAnswer captureInput = new FirstArgumentAnswer();

        CommandSession session = getSession(captureInput);

        setCommandProcessor(session);

        CommandJob job = getCommandJob();

        // when
        job.execute(getJobExecutionContext(command));

        // then

        assertThat(captureInput.getInputArg(), is(command));

        verifySessionCalls(command, session, 1, 1);

    }

    @Test
    public void testNullContext() throws JobExecutionException {
        CommandJob job = getCommandJob();
        job.doExecute(null);
    }

    @Test
    public void testNullMergedJobDataMap() throws JobExecutionException {
        CommandJob job = getCommandJob();

        JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(null);

        job.doExecute(jobExecutionContext);
    }

    private CommandJob getCommandJob() {
        CommandJob job = new CommandJob() {
            @Override
            public Subject getSystemSubject() {
                Subject subject = mock(Subject.class);
                when(subject.execute(Matchers.<Callable<Object>>any()))
                        .thenAnswer(new Answer<Object>() {
                            @Override
                            public Object answer(InvocationOnMock invocation) throws Throwable {
                                Callable<Object> callable = (Callable<Object>) invocation
                                        .getArguments()[0];
                                return callable.call();
                            }
                        });
                return subject;
            }
        };
        return job;
    }

    /**
     * @param command
     * @param session
     * @param expectedAmountOfCalls
     * @param expectedTimesToClose  TODO
     * @throws Exception
     */
    void verifySessionCalls(String command, CommandSession session, int expectedAmountOfCalls,
            int expectedTimesToClose) throws Exception {
        verify(session, times(expectedAmountOfCalls)).execute(command);
        verify(session, times(expectedTimesToClose)).close();
    }

    private CommandProcessor getCommandProcessor(CommandSession session) {
        CommandProcessor processor = mock(CommandProcessor.class);

        when(processor.createSession(isNull(InputStream.class), isA(PrintStream.class),
                isA(PrintStream.class))).thenReturn(session);
        return processor;
    }

    private CommandSession getSession(Answer<String> captureInput) {
        CommandSession session = mock(CommandSession.class);

        try {
            when(session.execute(isA(CharSequence.class))).then(captureInput);

        } catch (Exception e) {
            LOGGER.error("Exception occurred during command session", e);
        }
        return session;
    }

    private JobExecutionContext getJobExecutionContext(String command) {

        JobExecutionContext context = mock(JobExecutionContext.class);

        JobDataMap jobDataMap = new JobDataMap();

        jobDataMap.put(CommandJob.COMMAND_KEY, command);

        when(context.getMergedJobDataMap()).thenReturn(jobDataMap);

        return context;
    }

    private void setCommandProcessor(CommandSession session) {

        ServiceStore store = ServiceStore.getInstance();

        store.setObject(getCommandProcessor(session));
    }
}
