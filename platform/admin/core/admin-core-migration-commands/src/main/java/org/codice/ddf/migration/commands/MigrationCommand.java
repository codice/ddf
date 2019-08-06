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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import ddf.security.SubjectUtils;
import ddf.security.service.SecurityServiceException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.console.Session;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.ExecutionException;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.configuration.migration.ConfigurationMigrationService;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMessage;
import org.codice.ddf.migration.MigrationSuccessfulInformation;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.security.common.Security;
import org.codice.ddf.system.alerts.NoticePriority;
import org.codice.ddf.system.alerts.SystemNotice;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides common methods and instance variables that migration commands can use. */
public abstract class MigrationCommand implements Action {

  public static final String ERROR_MESSAGE =
      "An error was encountered while executing this command; %s.";

  public static final String NAMESPACE = "migration";

  protected static final String EXPORTED = "exported";

  private static final String ALERT_TITLE = "User is %sing configuration settings";

  private static final String USER_MESSAGE =
      "The user trying to %s configuration settings is [%s].";

  private static final Logger LOGGER = LoggerFactory.getLogger(MigrationCommand.class);

  protected final Security security;

  @Reference protected ConfigurationMigrationService configurationMigrationService;

  @Reference protected EventAdmin eventAdmin;

  @Reference protected Session session;

  @Option(
    name = "--user",
    required = false,
    aliases = {"-u"},
    multiValued = false,
    description = "Run command as a different user."
  )
  protected String user = null;

  protected Path exportDirectory =
      Paths.get(System.getProperty("ddf.home"), MigrationCommand.EXPORTED);

  @VisibleForTesting
  MigrationCommand(
      ConfigurationMigrationService service,
      Security security,
      EventAdmin eventAdmin,
      Session session) {
    this.configurationMigrationService = service;
    this.security = security;
    this.eventAdmin = eventAdmin;
    this.session = session;
  }

  public MigrationCommand() {
    this.security = Security.getInstance();
  }

  @Override
  public Object execute() throws Exception {
    try {
      if (StringUtils.isNotBlank(user)) {
        return runWithUserName();
      }
      return security.runWithSubjectOrElevate(this::executeWithSubject);
    } catch (SecurityServiceException e) {
      outputErrorMessage(String.format(MigrationCommand.ERROR_MESSAGE, e.getMessage()));
    } catch (ExecutionException | InvocationTargetException e) {
      outputErrorMessage(String.format(MigrationCommand.ERROR_MESSAGE, e.getCause().getMessage()));
    }
    return null;
  }

  /**
   * Executes the command once the user has been properly authorized.
   *
   * @return result of the command execution
   * @throws Exception if the command failed to run
   */
  @SuppressWarnings("squid:S00112" /* throwing Exception for consistency with execute() */)
  protected abstract Object executeWithSubject() throws Exception;

  protected void outputErrorMessage(String message) {
    outputMessageWithColor(message, Ansi.Color.RED);
  }

  protected void outputMessage(MigrationMessage msg) {
    if (msg instanceof MigrationException) {
      outputErrorMessage(msg.getMessage());
    } else if (msg instanceof MigrationWarning) {
      outputMessageWithColor(msg.getMessage(), Ansi.Color.YELLOW);
    } else if (msg instanceof MigrationSuccessfulInformation) {
      outputMessageWithColor(msg.getMessage(), Ansi.Color.GREEN);
    } else {
      outputMessageWithColor(msg.getMessage(), Ansi.Color.WHITE);
    }
  }

  @SuppressWarnings("squid:S106"
  /* we purposely need to output to the output stream for the info to get to the admin console */
  )
  @VisibleForTesting
  protected PrintStream getConsole() {
    return System.out;
  }

  @VisibleForTesting
  protected String getSubjectName() {
    return SubjectUtils.getName(SecurityUtils.getSubject(), null, true);
  }

  protected void postSystemNotice(String cmd) {
    final String subjectName = getSubjectName();
    final SystemNotice notice =
        new SystemNotice(
            getClass().getName() + '.' + System.currentTimeMillis(),
            // add a timestamp to make sure we generate a different one each time
            NoticePriority.IMPORTANT,
            String.format(MigrationCommand.ALERT_TITLE, cmd),
            ImmutableSet.of(String.format(MigrationCommand.USER_MESSAGE, cmd, subjectName)));

    eventAdmin.postEvent(
        new Event(
            SystemNotice.SYSTEM_NOTICE_BASE_TOPIC + MigrationCommand.NAMESPACE,
            notice.getProperties()));
  }

  private void outputMessageWithColor(String message, Ansi.Color color) {
    final String colorAsString = Ansi.ansi().a(Attribute.RESET).fg(color).toString();
    final PrintStream console = getConsole();

    console.print(colorAsString);
    console.print(message);
    console.println(Ansi.ansi().a(Attribute.RESET).toString());
  }

  private Object runWithUserName() throws ExecutionException {
    try {
      final String password = session.readLine("Password for " + user + ": ", '*');
      final Subject subject = security.getSubject(user, password, "127.0.0.1");

      if (subject != null) {
        return subject.execute(this::executeWithSubject);
      }
      outputErrorMessage("Invalid username/password");
    } catch (IOException e) {
      LOGGER.info("Failed to read password", e);
      outputErrorMessage("Failed to read password");
    }
    return null;
  }
}
