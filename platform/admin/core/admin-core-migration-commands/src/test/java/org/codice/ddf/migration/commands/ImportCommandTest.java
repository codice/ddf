/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.migration.commands;

import java.lang.reflect.InvocationTargetException;

import org.fusesource.jansi.Ansi;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ddf.security.service.SecurityServiceException;

public class ImportCommandTest extends AbstractMigrationCommandTest {
    private ImportCommand COMMAND;

    @Before
    public void before() throws Exception {
        COMMAND = initCommand(new ImportCommand(SERVICE, SECURITY, EXPORTED_ARG));
    }

    @Test
    public void testExecuteWhenDirectorySpecified() throws Exception {
        COMMAND.execute();

        Mockito.verify(SERVICE)
                .doImport(Mockito.eq(EXPORTED_PATH), Mockito.notNull());
    }

    @Test
    public void testExecuteWhenDirectoryNotSpecified() throws Exception {
        COMMAND = initCommand(Mockito.spy(new ImportCommand(SERVICE, SECURITY, "")));

        COMMAND.execute();

        Mockito.verify(SERVICE)
                .doImport(Mockito.eq(DDF_HOME.resolve(MigrationCommand.EXPORTED)),
                        Mockito.notNull());
    }

    @Test
    public void testExecuteWhenDirectoryIsInvalid() throws Exception {
        COMMAND = initCommand(Mockito.spy(new ImportCommand(SERVICE,
                SECURITY,
                "invalid path \"*?<> \0")));

        COMMAND.execute();

        Mockito.verify(SERVICE, Mockito.never())
                .doImport(Mockito.any(), Mockito.notNull());

        verifyConsoleOutput(Matchers.matchesPattern(".*error.*encountered.*command;.*invalid path.*"),
                Ansi.Color.RED);
    }

    @Test
    public void testExecuteWhenUnableToElevateSubject() throws Exception {
        final String MSG = "Some error";

        Mockito.when(SECURITY.runWithSubjectOrElevate(Mockito.any()))
                .thenThrow(new SecurityServiceException(MSG));

        COMMAND.execute();

        verifyConsoleOutput(Matchers.matchesPattern(".*error.*encountered.*command; " + MSG),
                Ansi.Color.RED);
    }

    @Test
    public void testExecuteWhenServiceThrowsError() throws Exception {
        final String MSG = "Some error";
        final Exception EXCEPTION = new Exception(MSG);

        Mockito.when(SECURITY.runWithSubjectOrElevate(Mockito.any()))
                .thenThrow(new InvocationTargetException(EXCEPTION));

        COMMAND.execute();

        verifyConsoleOutput(Matchers.matchesPattern(".*error.*encountered.*command; " + MSG),
                Ansi.Color.RED);
    }
}
