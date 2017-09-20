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
import java.lang.reflect.InvocationTargetException;
import org.fusesource.jansi.Ansi;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ImportCommandTest extends AbstractMigrationCommandTest {
  private ImportCommand command;

  @Before
  public void setup() throws Exception {
    command = initCommand(new ImportCommand(service, security, exportedArg));
  }

  @Test
  public void testExecuteWhenDirectorySpecified() throws Exception {
    command.execute();

    Mockito.verify(service).doImport(Mockito.eq(exportedPath), Mockito.notNull());
  }

  @Test
  public void testExecuteWhenDirectoryNotSpecified() throws Exception {
    command = initCommand(Mockito.spy(new ImportCommand(service, security, "")));

    command.execute();

    Mockito.verify(service)
        .doImport(Mockito.eq(ddfHome.resolve(MigrationCommand.EXPORTED)), Mockito.notNull());
  }

  @Test
  public void testExecuteWhenDirectoryIsInvalid() throws Exception {
    command =
        initCommand(Mockito.spy(new ImportCommand(service, security, "invalid path \"*?<> \0")));

    command.execute();

    Mockito.verify(service, Mockito.never()).doImport(Mockito.any(), Mockito.notNull());

    verifyConsoleOutput(
        Matchers.matchesPattern(
            "An error was encountered while executing this command; .*invalid path.*\\."),
        Ansi.Color.RED);
  }

  @Test
  public void testExecuteWhenUnableToElevateSubject() throws Exception {
    final String msg = "Some error";

    Mockito.when(security.runWithSubjectOrElevate(Mockito.any()))
        .thenThrow(new SecurityServiceException(msg));

    command.execute();

    verifyConsoleOutput(
        Matchers.equalTo("An error was encountered while executing this command; " + msg + "."),
        Ansi.Color.RED);
  }

  @Test
  public void testExecuteWhenServiceThrowsError() throws Exception {
    final String msg = "Some error";
    final Exception exeption = new Exception(msg);

    Mockito.when(security.runWithSubjectOrElevate(Mockito.any()))
        .thenThrow(new InvocationTargetException(exeption));

    command.execute();

    verifyConsoleOutput(
        Matchers.equalTo("An error was encountered while executing this command; " + msg + "."),
        Ansi.Color.RED);
  }
}
