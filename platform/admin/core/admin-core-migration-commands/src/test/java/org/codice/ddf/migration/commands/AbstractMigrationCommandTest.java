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

import java.io.PrintStream;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;
import org.codice.ddf.configuration.migration.ConfigurationMigrationService;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.security.common.Security;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** Base class for migration command test classes. */
// squid:S2187 - hopefully the doc above indicates this is a base class for test classes
@SuppressWarnings("squid:S2187")
public class AbstractMigrationCommandTest {
  protected final PrintStream console = Mockito.mock(PrintStream.class);

  protected final Path exportedPath = Paths.get("test-exported");

  protected final String exportedArg = exportedPath.toString();

  protected final ConfigurationMigrationService service =
      Mockito.mock(ConfigurationMigrationService.class);

  protected final Security security = Mockito.mock(Security.class);

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  protected Path root;

  protected Path ddfHome;

  @Before
  public void baseSetup() throws Exception {
    root = testFolder.getRoot().toPath().toRealPath(LinkOption.NOFOLLOW_LINKS);
    ddfHome = testFolder.newFolder("ddf").toPath().toRealPath(LinkOption.NOFOLLOW_LINKS);
    ddfHome.resolve(exportedPath).toFile().mkdirs();

    Mockito.doAnswer(
            AdditionalAnswers.<MigrationReport, Callable<MigrationReport>>answer(
                code -> code.call()))
        .when(security)
        .runWithSubjectOrElevate(Mockito.notNull());

    System.setProperty("ddf.home", ddfHome.toString());
  }

  protected <T extends MigrationCommand> T initCommand(T command) {
    final T cmd = Mockito.spy(command);

    Mockito.when(cmd.getConsole()).thenReturn(console);
    return cmd;
  }

  protected void verifyConsoleOutput(Matcher<String> messageMatcher, Ansi.Color color) {
    final ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

    Mockito.verify(console, Mockito.times(2)).print(argument.capture());
    final List<String> values = argument.getAllValues();

    Assert.assertThat(
        values.get(0), Matchers.equalTo(Ansi.ansi().a(Attribute.RESET).fg(color).toString()));
    Assert.assertThat(values.get(1), messageMatcher);
    Mockito.verify(console).println(Ansi.ansi().a(Attribute.RESET).toString());
  }

  protected void verifyConsoleOutput(String message, Ansi.Color color) {
    verifyConsoleOutput(Matchers.equalTo(message), color);
  }
}
