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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.Subject;
import java.io.InputStream;
import java.util.concurrent.Callable;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.stubbing.Answer;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the {@link CommandJob} class.
 *
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 */
public class CommandJobTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommandJobTest.class);

  private SessionFactory sessionFactory;

  @Before
  public void setup() {
    sessionFactory = mock(SessionFactory.class);
  }

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
  @SuppressWarnings("ConstantConditions")
  @Test
  public void testNullCommand() throws Exception {
    // given
    String command = null;

    FirstArgumentAnswer captureInput = new FirstArgumentAnswer();

    Session session = getSession(captureInput);

    when(sessionFactory.create(notNull(InputStream.class), any(), any())).thenReturn(session);

    CommandJob job = getCommandJob();

    // when
    job.execute(getJobExecutionContext(command));

    // then
    verifySessionCallsAndSessionClosed(command, session, 0);
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

    Session session = getSession(captureInput);

    when(sessionFactory.create(notNull(InputStream.class), any(), any())).thenReturn(session);

    CommandJob job = getCommandJob();

    // when
    job.execute(getJobExecutionContext(command));

    // then
    assertThat(captureInput.getInputArg(), is(command));

    verifySessionCallsAndSessionClosed(command, session, 1);
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

    Session session = getSession(captureInput);

    when(sessionFactory.create(notNull(InputStream.class), any(), any())).thenReturn(session);

    CommandJob job = getCommandJob();

    // when
    job.execute(getJobExecutionContext(command));

    // then

    assertThat(captureInput.getInputArg(), is(command));

    verifySessionCallsAndSessionClosed(command, session, 1);
  }

  @Test
  public void testNullContext() throws JobExecutionException {
    CommandJob job = getCommandJob();
    job.execute(null);
  }

  @Test
  public void testNullMergedJobDataMap() throws JobExecutionException {
    CommandJob job = getCommandJob();

    JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(null);

    job.execute(jobExecutionContext);
  }

  private CommandJob getCommandJob() {
    return new CommandJob() {
      @SuppressWarnings("unchecked")
      @Override
      public Subject getSystemSubject() {
        Subject subject = mock(Subject.class);
        when(subject.execute(Matchers.<Callable<Object>>any()))
            .thenAnswer(
                invocation -> {
                  Callable<Object> callable = (Callable<Object>) invocation.getArguments()[0];
                  return callable.call();
                });
        return subject;
      }

      @Override
      protected SessionFactory getSessionFactory() {
        return sessionFactory;
      }
    };
  }

  private void verifySessionCallsAndSessionClosed(
      String command, Session session, int expectedAmountOfCalls) throws Exception {
    verify(session, times(expectedAmountOfCalls)).execute(command);
    verify(session, times(1)).close();
  }

  private Session getSession(Answer<String> captureInput) {
    Session session = mock(Session.class);

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
}
