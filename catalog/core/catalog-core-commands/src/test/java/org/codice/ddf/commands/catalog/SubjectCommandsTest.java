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
package org.codice.ddf.commands.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.Subject;
import ddf.security.service.SecurityServiceException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import org.apache.karaf.shell.api.console.Session;
import org.apache.shiro.subject.ExecutionException;
import org.codice.ddf.security.Security;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SubjectCommandsTest extends ConsoleOutputCommon {

  private static final String ERROR = "Error!";

  private static final String SUCCESS = "Success!";

  private static final String USERNAME = "username";

  private static final String PASSWORD = "password";

  @Mock Session session;

  @Mock Subject subject;

  @Mock private Security security;

  @Test
  public void execute() throws Exception {
    SubjectCommands subjectCommands = new SubjectCommandsUnderTest();
    when(security.runWithSubjectOrElevate(any(Callable.class)))
        .thenAnswer(invocationOnMock -> subjectCommands.executeWithSubject());

    Object result = subjectCommands.execute();

    assertThat(result, is(SUCCESS));
  }

  @Test
  public void doExecuteWithUserName() throws Exception {
    SubjectCommands subjectCommands = new SubjectCommandsUnderTest();
    subjectCommands.user = USERNAME;

    when(session.readLine(anyString(), eq('*'))).thenReturn(PASSWORD);
    when(security.getSubject(USERNAME, PASSWORD, "127.0.0.1")).thenReturn(subject);
    when(subject.execute(any(Callable.class)))
        .thenAnswer(
            invocation -> {
              try {
                return invocation.<Callable>getArgument(0).call();
              } catch (Exception e) {
                throw new ExecutionException(e);
              }
            });

    Object result = subjectCommands.execute();

    assertThat(result, is(SUCCESS));
    verify(session).readLine("Password for " + USERNAME + ": ", '*');
    verify(security).getSubject(USERNAME, PASSWORD, "127.0.0.1");
  }

  @Test
  public void doExecuteWithInvalidUserName() throws Exception {
    SubjectCommands subjectCommands = new SubjectCommandsUnderTest();
    subjectCommands.user = USERNAME;

    when(session.readLine(anyString(), eq('*'))).thenReturn(PASSWORD);
    when(security.getSubject(USERNAME, PASSWORD, "127.0.0.1")).thenReturn(null);

    subjectCommands.execute();

    assertThat(consoleOutput.getOutput(), containsString("Invalid username/password"));
    verify(security).getSubject(USERNAME, PASSWORD, "127.0.0.1");
  }

  @Test
  public void doExecuteWithUserNameFailsToReadPassword() throws Exception {
    SubjectCommands subjectCommands = new SubjectCommandsUnderTest();
    subjectCommands.user = USERNAME;

    when(session.readLine(anyString(), eq('*'))).thenThrow(new IOException());

    subjectCommands.execute();

    assertThat(consoleOutput.getOutput(), containsString("Failed to read password"));
  }

  @Test
  public void doExecuteWithUsernameWhenSubjectExecuteThrowsExecutionException() throws Exception {
    SubjectCommands subjectCommands =
        new SubjectCommandsUnderTest() {
          @Override
          protected Object executeWithSubject() throws Exception {
            throw new IllegalStateException(ERROR);
          }
        };
    subjectCommands.user = USERNAME;

    when(session.readLine(anyString(), eq('*'))).thenReturn(PASSWORD);
    when(security.getSubject(USERNAME, PASSWORD, "127.0.0.1")).thenReturn(subject);
    when(subject.execute(any(Callable.class)))
        .thenAnswer(
            invocation -> {
              try {
                return invocation.<Callable>getArgument(0).call();
              } catch (Exception e) {
                throw new ExecutionException(e);
              }
            });

    subjectCommands.execute();

    verify(security).getSubject(USERNAME, PASSWORD, "127.0.0.1");
    assertThat(consoleOutput.getOutput(), containsString(ERROR));
  }

  @Test
  public void doExecuteWithoutUsernameWhenRunWithSubjectOrElevateThrowsSecurityServiceException()
      throws Exception {
    SubjectCommands subjectCommands = new SubjectCommandsUnderTest();
    when(security.runWithSubjectOrElevate(any(Callable.class)))
        .thenThrow(new SecurityServiceException(ERROR));

    subjectCommands.execute();

    assertThat(consoleOutput.getOutput(), containsString(ERROR));
  }

  @Test
  public void doExecuteWithoutUsernameWhenExecuteWithSubjectThrowsRuntimeException()
      throws Exception {
    SubjectCommands subjectCommands =
        new SubjectCommandsUnderTest() {
          @Override
          protected Object executeWithSubject() {
            throw new IllegalStateException(ERROR);
          }
        };
    when(security.runWithSubjectOrElevate(any(Callable.class)))
        .thenAnswer(
            invocation -> {
              try {
                return invocation.<Callable>getArgument(0).call();
              } catch (RuntimeException e) {
                throw new InvocationTargetException(e);
              }
            });

    subjectCommands.execute();

    assertThat(consoleOutput.getOutput(), containsString(ERROR));
  }

  @Test
  public void doExecuteWithoutUsernameWhenExecuteWithSubjectThrowsCheckedException()
      throws Exception {
    SubjectCommands subjectCommands =
        new SubjectCommandsUnderTest() {
          @Override
          protected Object executeWithSubject() throws IOException {
            throw new IOException(ERROR);
          }
        };
    when(security.runWithSubjectOrElevate(any(Callable.class)))
        .thenAnswer(
            invocation -> {
              try {
                return invocation.<Callable>getArgument(0).call();
              } catch (RuntimeException e) {
                fail("Expected checked Exception");
                throw new InvocationTargetException(e);
              } catch (Exception e) {
                throw new InvocationTargetException(e);
              }
            });

    subjectCommands.execute();

    assertThat(consoleOutput.getOutput(), containsString(ERROR));
  }

  private class SubjectCommandsUnderTest extends SubjectCommands {

    SubjectCommandsUnderTest() {
      session = SubjectCommandsTest.this.session;
      security = SubjectCommandsTest.this.security;
    }

    @Override
    protected Object executeWithSubject() throws Exception {
      return SUCCESS;
    }
  }
}
