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
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.Subject;
import java.util.concurrent.Callable;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.shiro.subject.ExecutionException;
import org.codice.ddf.security.impl.Security;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

/**
 * Tests the {@link CommandJob} class.
 *
 * @author Ashraf Barakat
 */
public class CommandJobTest {

  private static final String VALID_COMMAND = "info";

  /** Tests that there is no exception when command input is null */
  @Test
  public void testNullCommand() {
    // given
    CommandJob commandJob = new CommandJob(new Security());

    String command = null;

    // when
    commandJob.execute(createMockJobExecutionContext(command));
  }

  /** Tests that there is no exception when command is empty */
  @Test
  public void testEmptyCommand() {
    // given
    CommandJob commandJob = new CommandJob(new Security());

    String command = "";

    // when
    commandJob.execute(createMockJobExecutionContext(command));
  }

  /** Tests the simplest command will be executed */
  @Test
  public void testSimpleCommand() throws Exception {
    FirstArgumentAnswer captureInput = new FirstArgumentAnswer();
    Session session = mock(Session.class);
    when(session.execute(isA(CharSequence.class))).then(captureInput);

    CommandJob commandJob =
        new CommandJob(new Security()) {
          @Override
          public Subject getSystemSubject() {
            return createMockSubject();
          }

          @Override
          protected SessionFactory getSessionFactory() {
            SessionFactory sessionFactory = mock(SessionFactory.class);
            when(sessionFactory.create(notNull(), any(), any())).thenReturn(session);
            return sessionFactory;
          }
        };

    String command = VALID_COMMAND;

    // when
    commandJob.execute(createMockJobExecutionContext(command));

    // then
    assertThat(captureInput.getInputArg(), is(command));

    verify(session, times(1)).execute(command);
    verify(session, times(1)).close();
  }

  /** Tests that there is no exception when the {@link JobExecutionContext} is null */
  @Test
  public void testNullContext() {
    // given
    CommandJob commandJob = new CommandJob(new Security());

    // when
    commandJob.execute(null);
  }

  /**
   * Tests that there is no exception when the {@link JobDataMap} of the {@link JobExecutionContext}
   * is null
   */
  @Test
  public void testNullMergedJobDataMap() {
    // given
    CommandJob commandJob = new CommandJob(new Security());

    JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(null);

    // when
    commandJob.execute(jobExecutionContext);
  }

  /** Tests that there is no exception when unable to get the {@link SessionFactory} */
  @Test
  public void testUnableToGetSessionFactory() {
    // given
    CommandJob commandJob =
        new CommandJob(new Security()) {
          @Override
          public Subject getSystemSubject() {
            return createMockSubject();
          }

          @Override
          protected SessionFactory getSessionFactory() {
            return null;
          }
        };

    String command = VALID_COMMAND;

    // when
    commandJob.execute(createMockJobExecutionContext(command));
  }

  /**
   * Tests that there is no exception when unable to get the system's {@link Subject}. This might
   * happen when the system is very slow to start up where not all of the required security bundles
   * are started yet.
   */
  @Test
  public void testUnableToGetSystemSubject() {
    // given
    CommandJob commandJob =
        new CommandJob(new Security()) {
          @Override
          public Subject getSystemSubject() {
            return null;
          }
        };

    String command = VALID_COMMAND;

    // when
    commandJob.execute(createMockJobExecutionContext(command));
  }

  /**
   * Tests that there is no exception when {@link
   * org.codice.ddf.security.Security#getSystemSubject()} fails. This might happen when the system
   * is very slow to start up where not all of the required security bundles are started yet.
   */
  @Test
  public void testExceptionGettingSystemSubject() {
    // given
    CommandJob commandJob =
        new CommandJob(new Security()) {
          @Override
          public Subject getSystemSubject() {
            throw new RuntimeException();
          }
        };

    String command = VALID_COMMAND;

    // when
    commandJob.execute(createMockJobExecutionContext(command));
  }

  /** Tests that there is no exception when executing with the {@link Subject} fails. */
  @Test
  public void testUnableToExecuteWithSubject() {
    // given
    CommandJob commandJob =
        new CommandJob(new Security()) {
          @Override
          public Subject getSystemSubject() {
            Subject subject = mock(Subject.class);
            when(subject.execute(ArgumentMatchers.<Callable<Object>>any()))
                .thenThrow(ExecutionException.class);
            return subject;
          }
        };

    String command = VALID_COMMAND;

    // when
    commandJob.execute(createMockJobExecutionContext(command));
  }

  private JobExecutionContext createMockJobExecutionContext(String command) {
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put(CommandJob.COMMAND_KEY, command);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getMergedJobDataMap()).thenReturn(jobDataMap);

    return context;
  }

  private static Subject createMockSubject() {
    Subject subject = mock(Subject.class);
    when(subject.execute(ArgumentMatchers.<Callable<Object>>any()))
        .thenAnswer(
            invocation -> {
              Callable<Object> callable = (Callable<Object>) invocation.getArguments()[0];
              return callable.call();
            });
    return subject;
  }
}
