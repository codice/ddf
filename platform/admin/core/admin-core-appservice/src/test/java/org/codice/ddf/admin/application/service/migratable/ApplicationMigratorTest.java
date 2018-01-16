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
package org.codice.ddf.admin.application.service.migratable;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.Mockito;

public class ApplicationMigratorTest {

  private static final String NAME = "test.name";
  private static final String NAME2 = "test.name2";
  private static final String NAME3 = "test.name3";
  private static final String NAME4 = "test.name4";
  private static final String VERSION = "test.version";
  private static final String VERSION2 = "test.version2";
  private static final String VERSION3 = "test.version3";
  private static final String VERSION4 = "test.version4";
  private static final String DESCRIPTION = "test.description";
  private static final String DESCRIPTION2 = "test.description2";
  private static final String DESCRIPTION3 = "test.description3";
  private static final String DESCRIPTION4 = "test.description4";
  private static final Boolean STARTED = true;
  private static final Boolean STARTED2 = false;
  private static final Boolean STARTED3 = true;
  private static final Boolean STARTED4 = true;
  private static final String URI = "https://test.com/test";
  private static final String URI2 = "https://test.com/test2";
  private static final String URI3 = "https://test.com/test3";
  private static final String URI4 = "https://test.com/test4";

  private final Application app = Mockito.mock(Application.class);

  private final Application app2 = Mockito.mock(Application.class);

  private final Application app3 = Mockito.mock(Application.class);

  private final Application app4 = Mockito.mock(Application.class);

  private final JsonApplication japp =
      new JsonApplication(NAME, VERSION, DESCRIPTION, URI, STARTED);
  private final JsonApplication japp2 =
      new JsonApplication(NAME2, VERSION2, DESCRIPTION2, URI2, STARTED2);
  private final JsonApplication japp3 =
      new JsonApplication(NAME3, VERSION3, DESCRIPTION3, URI3, STARTED3);

  private final TaskList tasks = Mockito.mock(TaskList.class);

  private final ApplicationService appService = Mockito.mock(ApplicationService.class);

  private final ApplicationProcessor appProcessor = Mockito.mock(ApplicationProcessor.class);

  private final ApplicationMigrator appMigrator =
      Mockito.mock(
          ApplicationMigrator.class,
          Mockito.withSettings()
              .useConstructor(appService, appProcessor)
              .defaultAnswer(Answers.CALLS_REAL_METHODS));

  private final ProfileMigrationReport report = Mockito.mock(ProfileMigrationReport.class);

  private final JsonProfile jprofile = Mockito.mock(JsonProfile.class);

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() throws Exception {
    Mockito.when(app.getName()).thenReturn(NAME);
    Mockito.when(app.getVersion()).thenReturn(VERSION);
    Mockito.when(app.getDescription()).thenReturn(DESCRIPTION);
    Mockito.when(app.getURI()).thenReturn(new URI(URI));
    Mockito.when(appService.isApplicationStarted(app)).thenReturn(STARTED);

    Mockito.when(app2.getName()).thenReturn(NAME2);
    Mockito.when(app2.getVersion()).thenReturn(VERSION2);
    Mockito.when(app2.getDescription()).thenReturn(DESCRIPTION2);
    Mockito.when(app2.getURI()).thenReturn(new URI(URI2));
    Mockito.when(appService.isApplicationStarted(app2)).thenReturn(STARTED2);

    Mockito.when(app3.getName()).thenReturn(NAME3);
    Mockito.when(app3.getVersion()).thenReturn(VERSION3);
    Mockito.when(app3.getDescription()).thenReturn(DESCRIPTION3);
    Mockito.when(app3.getURI()).thenReturn(new URI(URI3));
    Mockito.when(appService.isApplicationStarted(app3)).thenReturn(STARTED3);

    Mockito.when(app4.getName()).thenReturn(NAME4);
    Mockito.when(app4.getVersion()).thenReturn(VERSION4);
    Mockito.when(app4.getDescription()).thenReturn(DESCRIPTION4);
    Mockito.when(app4.getURI()).thenReturn(new URI(URI4));
    Mockito.when(appService.isApplicationStarted(app4)).thenReturn(STARTED4);

    Mockito.when(appMigrator.newTaskList(report)).thenReturn(tasks);
  }

  @Test
  public void testConstructorWithNullAppService() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null application service"));

    new ApplicationMigrator(null, appProcessor);
  }

  @Test
  public void testExportApplications() throws Exception {
    final Set<Application> apps = new LinkedHashSet<>(); // to verify order

    apps.add(app2);
    apps.add(app);
    apps.add(app3);

    Mockito.when(appService.getApplications()).thenReturn(apps);

    Assert.assertThat(appMigrator.exportApplications(), Matchers.contains(japp2, japp, japp3));
  }

  @Test
  public void testImportApplicationsWithTasksAndSucceeds() throws Exception {
    Mockito.doReturn(true).when(appProcessor).processApplications(report, jprofile, tasks);
    Mockito.doReturn(false, true).when(tasks).isEmpty();
    Mockito.doReturn(true).when(tasks).execute();

    Assert.assertThat(appMigrator.importApplications(report, jprofile), Matchers.equalTo(true));

    Mockito.verify(appProcessor, Mockito.times(2)).processApplications(report, jprofile, tasks);
    Mockito.verify(tasks, Mockito.times(2)).isEmpty();
    Mockito.verify(tasks).execute();
  }

  @Test
  public void testImportApplicationsWhenFailsToProcess() throws Exception {
    Mockito.doReturn(false).when(appProcessor).processApplications(report, jprofile, tasks);

    Assert.assertThat(appMigrator.importApplications(report, jprofile), Matchers.equalTo(false));

    Mockito.verify(appProcessor).processApplications(report, jprofile, tasks);
    Mockito.verify(tasks, Mockito.never()).isEmpty();
    Mockito.verify(tasks, Mockito.never()).execute();
  }

  @Test
  public void testImportApplicationsWithNoTasks() throws Exception {
    Mockito.doReturn(true).when(appProcessor).processApplications(report, jprofile, tasks);
    Mockito.doReturn(true).when(tasks).isEmpty();

    Assert.assertThat(appMigrator.importApplications(report, jprofile), Matchers.equalTo(true));

    Mockito.verify(appProcessor).processApplications(report, jprofile, tasks);
    Mockito.verify(tasks).isEmpty();
    Mockito.verify(tasks, Mockito.never()).execute();
  }

  @Test
  public void testImportApplicationsWhenFailedToExecuteTasks() throws Exception {
    Mockito.doReturn(true).when(appProcessor).processApplications(report, jprofile, tasks);
    Mockito.doReturn(false).when(tasks).isEmpty();
    Mockito.doReturn(false).when(tasks).execute();

    Assert.assertThat(appMigrator.importApplications(report, jprofile), Matchers.equalTo(false));

    Mockito.verify(appProcessor).processApplications(report, jprofile, tasks);
    Mockito.verify(tasks).isEmpty();
    Mockito.verify(tasks).execute();
  }
}
