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
package org.codice.ddf.admin.application.service.command.completers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.apache.karaf.shell.api.console.CommandLine;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationStatus;
import org.codice.ddf.admin.application.service.impl.ApplicationImpl;
import org.codice.ddf.admin.application.service.impl.ApplicationServiceImpl;
import org.codice.ddf.admin.application.service.impl.ApplicationStatusImpl;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotActiveApplicationsCompleterTest {

  private static final String NO_APP_SERV = "No application service";

  private static CommandLine commandLine = mock(CommandLine.class);

  /** Tests the {@link NotActiveApplicationsCompleter#complete(String, int, List)} method */
  @Test
  public void testNotActiveApplicationsCompleter() {
    Application testApp = mock(ApplicationImpl.class);
    ApplicationService testAppService = mock(ApplicationServiceImpl.class);
    Set<Application> appSet = new HashSet<>();
    appSet.add(testApp);
    ApplicationStatus testStatus = mock(ApplicationStatusImpl.class);
    ApplicationStatus.ApplicationState testState = ApplicationStatus.ApplicationState.INACTIVE;

    when(testAppService.getApplications()).thenReturn(appSet);
    when(testAppService.getApplicationStatus(testApp)).thenReturn(testStatus);
    when(testStatus.getState()).thenReturn(testState);
    when(testApp.getName()).thenReturn("TestApp");

    NotActiveApplicationsCompleter activeApplicationsCompleter =
        new NotActiveApplicationsCompleter();
    activeApplicationsCompleter.setApplicationService(testAppService);

    assertThat(
        "If the return value is -1, then the expected match was not found.",
        activeApplicationsCompleter.complete(null, commandLine, new ArrayList<>()),
        is(not(-1)));
  }

  /**
   * Tests the {@link NotActiveApplicationsCompleter#complete(String, int, List)} method for the
   * case where the ApplicationService given to it is null
   */
  // TODO RAP 29 Aug 16: DDF-2443 - Fix test to not depend on specific log output
  @Test
  public void testNotActiveApplicationsCompleterNullAppService() {
    ch.qos.logback.classic.Logger root =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    final Appender mockAppender = mock(Appender.class);
    when(mockAppender.getName()).thenReturn("MOCK");
    root.addAppender(mockAppender);
    root.setLevel(Level.ALL);

    ApplicationService testAppService = null;

    NotActiveApplicationsCompleter notActiveApplicationsCompleter =
        new NotActiveApplicationsCompleter();
    notActiveApplicationsCompleter.setApplicationService(testAppService);

    notActiveApplicationsCompleter.complete(null, commandLine, new ArrayList<>());

    verify(mockAppender)
        .doAppend(
            argThat(
                new ArgumentMatcher() {
                  @Override
                  public boolean matches(final Object argument) {
                    return ((LoggingEvent) argument).getFormattedMessage().contains(NO_APP_SERV);
                  }
                }));
  }
}
