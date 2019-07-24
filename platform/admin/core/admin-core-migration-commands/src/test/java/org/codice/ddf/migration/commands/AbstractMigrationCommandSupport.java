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

import ddf.security.Subject;
import java.io.PrintStream;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.karaf.shell.api.console.Session;
import org.codice.ddf.configuration.migration.ConfigurationMigrationService;
import org.codice.ddf.security.common.Security;
import org.codice.ddf.system.alerts.NoticePriority;
import org.codice.ddf.system.alerts.SystemNotice;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/** Base class for migration command test classes. */
public class AbstractMigrationCommandSupport {

  protected static final String SUBJECT_NAME = "test.subject";

  protected static final String PASSWORD = "test.password";

  protected final PrintStream console = Mockito.mock(PrintStream.class);

  protected final ConfigurationMigrationService service =
      Mockito.mock(ConfigurationMigrationService.class);

  protected final Security security = Mockito.mock(Security.class);

  protected final EventAdmin eventAdmin = Mockito.mock(EventAdmin.class);

  protected final Session session = Mockito.mock(Session.class);

  protected final Subject subject = Mockito.mock(Subject.class);

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  protected Path root;

  protected Path ddfHome;

  protected Path exportedPath;

  protected MigrationCommand command;

  @Before
  public void baseSetup() throws Exception {
    root = testFolder.getRoot().toPath().toRealPath(LinkOption.NOFOLLOW_LINKS);
    ddfHome = testFolder.newFolder("ddf").toPath().toRealPath(LinkOption.NOFOLLOW_LINKS);
    exportedPath = ddfHome.resolve("exported");

    exportedPath.toFile().mkdirs();

    Mockito.doAnswer(AdditionalAnswers.<Object, Callable<Object>>answer(code -> code.call()))
        .when(security)
        .runWithSubjectOrElevate(Mockito.notNull());

    Mockito.doNothing().when(eventAdmin).postEvent(Mockito.notNull());
    Mockito.doReturn(PASSWORD).when(session).readLine(Mockito.anyString(), Mockito.anyChar());

    Mockito.doReturn(subject).when(security).getSubject(SUBJECT_NAME, PASSWORD, "127.0.0.1");
    Mockito.doAnswer(AdditionalAnswers.<Object, Callable<Object>>answer(code -> code.call()))
        .when(subject)
        .execute(Mockito.<Callable<Object>>notNull());

    System.setProperty("ddf.home", ddfHome.toString());
  }

  protected void initCommand(MigrationCommand cmd) throws Exception {
    this.command = Mockito.spy(cmd);

    Mockito.doReturn(console).when(command).getConsole();
    Mockito.doReturn(SUBJECT_NAME).when(command).getSubjectName();
  }

  protected void verifyPostedEvent(String cmd) {
    final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

    Mockito.verify(eventAdmin).postEvent(eventCaptor.capture());
    final Event event = eventCaptor.getValue();

    Assert.assertThat(
        event.getTopic(), Matchers.equalTo(SystemNotice.SYSTEM_NOTICE_BASE_TOPIC + "migration"));
    Assert.assertThat(
        (String) event.getProperty(SystemNotice.SYSTEM_NOTICE_SOURCE_KEY),
        Matchers.startsWith(command.getClass().getName() + '.'));
    Assert.assertThat(
        event.getProperty(SystemNotice.SYSTEM_NOTICE_PRIORITY_KEY),
        Matchers.equalTo(NoticePriority.IMPORTANT.value()));
    Assert.assertThat(
        event.getProperty(SystemNotice.SYSTEM_NOTICE_TITLE_KEY),
        Matchers.equalTo("User is " + cmd + "ing configuration settings"));
    Assert.assertThat(
        (Collection<String>) event.getProperty(SystemNotice.SYSTEM_NOTICE_DETAILS_KEY),
        Matchers.contains(
            Matchers.equalTo(
                "The user trying to "
                    + cmd
                    + " configuration settings is ["
                    + SUBJECT_NAME
                    + "].")));
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
