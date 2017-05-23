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
package org.codice.ddf.commands.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

import org.apache.karaf.shell.api.console.Session;
import org.apache.shiro.subject.ExecutionException;
import org.codice.ddf.security.common.Security;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ddf.security.Subject;
import ddf.security.service.SecurityServiceException;

@RunWith(MockitoJUnitRunner.class)
public class SubjectCommandsTest extends ConsoleOutputCommon {

    private static final String ERROR = "Error!";

    private static final String SUCCESS = "Success!";

    private static final String USERNAME = "username";

    private static final String PASSWORD = "password";

    @Mock
    Session session;

    @Mock
    Subject subject;

    @Mock
    private Security security;

    private SubjectCommandsUnderTest subjectCommands;

    @Before
    public void setup() {
        subjectCommands = new SubjectCommandsUnderTest();
    }

    @Test
    public void execute() throws Exception {
        when(security.runWithSubjectOrElevate(any(Callable.class))).thenAnswer(invocationOnMock -> executeCommand());

        Object result = subjectCommands.execute();

        assertThat(result, is(SUCCESS));
    }

    @Test
    public void doExecuteWithUserName() throws Exception {
        subjectCommands.user = USERNAME;

        when(session.getKeyboard()).thenReturn(new ByteArrayInputStream(PASSWORD.getBytes()));
        when(security.getSubject(USERNAME, PASSWORD)).thenReturn(subject);
        when(subject.execute(any(Callable.class))).thenAnswer(invocationOnMock -> executeCommand());

        Object result = subjectCommands.execute();

        assertThat(result, is(SUCCESS));
        assertThat(consoleOutput.getOutput(), containsString("Password for " + USERNAME));
        verify(security).getSubject(USERNAME, PASSWORD);
    }

    @Test
    public void doExecuteWithInvalidUserName() throws Exception {
        subjectCommands.user = USERNAME;

        when(session.getKeyboard()).thenReturn(new ByteArrayInputStream(PASSWORD.getBytes()));
        when(security.getSubject(USERNAME, PASSWORD)).thenReturn(null);

        subjectCommands.execute();

        assertThat(consoleOutput.getOutput(), containsString("Invalid username/password"));
        verify(security).getSubject(USERNAME, PASSWORD);
    }

    @Test
    public void doExecuteWithUserNameFailsToReadPassword() throws Exception {
        subjectCommands.user = USERNAME;

        InputStream keyboard = mock(InputStream.class);
        when(session.getKeyboard()).thenReturn(keyboard);
        when(keyboard.read()).thenThrow(new IOException());

        subjectCommands.execute();

        assertThat(consoleOutput.getOutput(), containsString("Failed to read password"));
    }

    @Test
    public void doExecuteWhenSubjectExecuteThrowsExecutionException() throws Exception {
        subjectCommands.user = USERNAME;

        when(session.getKeyboard()).thenReturn(new ByteArrayInputStream(PASSWORD.getBytes()));
        when(security.getSubject(USERNAME, PASSWORD)).thenReturn(subject);
        when(subject.execute(any(Callable.class))).thenThrow(new ExecutionException(new IllegalStateException(
                ERROR)));

        subjectCommands.execute();

        verify(security).getSubject(USERNAME, PASSWORD);
        assertThat(consoleOutput.getOutput(), containsString(ERROR));
    }

    @Test
    public void doExecuteWhenRunWithSubjectOrElevateThrowsSecurityServiceException()
            throws Exception {
        when(security.runWithSubjectOrElevate(any(Callable.class))).thenThrow(new SecurityServiceException(
                ERROR));

        subjectCommands.execute();

        assertThat(consoleOutput.getOutput(), containsString(ERROR));
    }

    @Test
    public void doExecuteWhenRunWithSubjectOrElevateThrowsInvocationTargetException()
            throws Exception {
        when(security.runWithSubjectOrElevate(any(Callable.class))).thenThrow(new InvocationTargetException(
                new IllegalStateException(ERROR)));

        subjectCommands.execute();

        assertThat(consoleOutput.getOutput(), containsString(ERROR));
    }

    private Object executeCommand() throws Exception {
        return subjectCommands.executeWithSubject();
    }

    private class SubjectCommandsUnderTest extends SubjectCommands {

        SubjectCommandsUnderTest() {
            super(security);
            this.session = SubjectCommandsTest.this.session;
        }

        @Override
        protected Object executeWithSubject() throws Exception {
            return SUCCESS;
        }
    }
}