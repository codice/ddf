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
package org.codice.ddf.migration.commands;

import ddf.security.service.SecurityServiceException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import org.apache.shiro.subject.ExecutionException;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationInformation;
import org.codice.ddf.migration.MigrationSuccessfulInformation;
import org.codice.ddf.migration.MigrationWarning;
import org.fusesource.jansi.Ansi;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MigrationCommandTest extends AbstractMigrationCommandSupport {

  private static final String MESSAGE = "test message.";

  @Before
  public void setup() throws Exception {
    initCommand(
        Mockito.mock(
            MigrationCommand.class,
            Mockito.withSettings()
                .useConstructor(service, security, eventAdmin, session)
                .defaultAnswer(Mockito.CALLS_REAL_METHODS)));
  }

  @Test
  public void testConstructor() throws Exception {
    Assert.assertThat(
        command.exportDirectory, Matchers.equalTo(ddfHome.resolve(MigrationCommand.EXPORTED)));
  }

  @Test
  public void testExecuteWithNoUser() throws Exception {
    Mockito.doReturn(null).when(command).executeWithSubject();

    command.execute();

    Mockito.verify(security).runWithSubjectOrElevate(Mockito.any());
    Mockito.verify(command).executeWithSubject();
  }

  @Test
  public void testExecuteWithNoUserWhenUnableToElevateSubject() throws Exception {
    final String msg = "Some error";

    Mockito.when(security.runWithSubjectOrElevate(Mockito.any()))
        .thenThrow(new SecurityServiceException(msg));

    command.execute();

    Mockito.verify(security).runWithSubjectOrElevate(Mockito.any());
    verifyConsoleOutput(
        Matchers.equalTo("An error was encountered while executing this command; " + msg + "."),
        Ansi.Color.RED);
  }

  @Test
  public void testExecuteWithNoUserWhenSubClassThrowsException() throws Exception {
    final String msg = "Some error";
    final Exception exception = new Exception(msg);

    Mockito.when(security.runWithSubjectOrElevate(Mockito.any()))
        .thenThrow(new InvocationTargetException(exception));

    command.execute();

    Mockito.verify(security).runWithSubjectOrElevate(Mockito.any());
    verifyConsoleOutput(
        Matchers.equalTo("An error was encountered while executing this command; " + msg + "."),
        Ansi.Color.RED);
  }

  @Test
  public void testExecuteWithUser() throws Exception {
    command.user = SUBJECT_NAME;

    command.execute();

    Mockito.verify(session).readLine(Mockito.anyString(), Mockito.anyChar());
    Mockito.verify(security).getSubject(SUBJECT_NAME, PASSWORD, "127.0.0.1");
    Mockito.verify(subject).execute(Mockito.<Callable<Object>>notNull());
    Mockito.verify(command).executeWithSubject();
  }

  @Test
  public void testExecuteWithUserWhenSubClassThrowsException() throws Exception {
    final String msg = "Some error";
    final Exception exception = new Exception(msg);

    command.user = SUBJECT_NAME;

    Mockito.when(subject.execute(Mockito.<Callable<Object>>notNull()))
        .thenThrow(new ExecutionException(exception));

    command.execute();

    Mockito.verify(session).readLine(Mockito.anyString(), Mockito.anyChar());
    Mockito.verify(security).getSubject(SUBJECT_NAME, PASSWORD, "127.0.0.1");
    Mockito.verify(subject).execute(Mockito.<Callable<Object>>notNull());
    verifyConsoleOutput(
        Matchers.equalTo("An error was encountered while executing this command; " + msg + "."),
        Ansi.Color.RED);
  }

  @Test
  public void testExecuteWithUserWhenFailedToGetPassword() throws Exception {
    final String msg = "Some error";
    final IOException exception = new IOException(msg);

    command.user = SUBJECT_NAME;

    Mockito.doThrow(exception).when(session).readLine(Mockito.anyString(), Mockito.anyChar());

    command.execute();

    Mockito.verify(session).readLine(Mockito.anyString(), Mockito.anyChar());
    Mockito.verify(security, Mockito.never()).getSubject(SUBJECT_NAME, PASSWORD, "127.0.0.1");
    Mockito.verify(subject, Mockito.never()).execute(Mockito.<Callable<Object>>notNull());
    verifyConsoleOutput(Matchers.equalTo("Failed to read password"), Ansi.Color.RED);
  }

  @Test
  public void testExecuteWithUserWhenSubjectNotFound() throws Exception {
    command.user = SUBJECT_NAME;

    Mockito.doReturn(null).when(security).getSubject(SUBJECT_NAME, PASSWORD, "127.0.0.1");

    command.execute();

    Mockito.verify(session).readLine(Mockito.anyString(), Mockito.anyChar());
    Mockito.verify(security).getSubject(SUBJECT_NAME, PASSWORD, "127.0.0.1");
    Mockito.verify(subject, Mockito.never()).execute(Mockito.<Callable<Object>>notNull());
    verifyConsoleOutput(Matchers.equalTo("Invalid username/password"), Ansi.Color.RED);
  }

  @Test
  public void testOutputErrorMessage() throws Exception {
    command.outputErrorMessage(MESSAGE);

    verifyConsoleOutput(MESSAGE, Ansi.Color.RED);
  }

  @Test
  public void testOutputMessageWithException() throws Exception {
    command.outputMessage(new MigrationException(MESSAGE));

    verifyConsoleOutput(MESSAGE, Ansi.Color.RED);
  }

  @Test
  public void testOutputMessageWithWarning() throws Exception {
    command.outputMessage(new MigrationWarning(MESSAGE));

    verifyConsoleOutput(MESSAGE, Ansi.Color.YELLOW);
  }

  @Test
  public void testOutputMessageWithSuccess() throws Exception {
    command.outputMessage(new MigrationSuccessfulInformation(MESSAGE));

    verifyConsoleOutput(MESSAGE, Ansi.Color.GREEN);
  }

  @Test
  public void testOutputMessageWithInfo() throws Exception {
    command.outputMessage(new MigrationInformation(MESSAGE));

    verifyConsoleOutput(MESSAGE, Ansi.Color.WHITE);
  }

  @Test
  public void testGetConsole() throws Exception {
    Mockito.doCallRealMethod().when(command).getConsole();

    Assert.assertThat(command.getConsole(), Matchers.sameInstance(System.out));
  }
}
