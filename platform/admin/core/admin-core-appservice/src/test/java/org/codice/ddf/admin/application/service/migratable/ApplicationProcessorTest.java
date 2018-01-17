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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.migration.MigrationException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.AdditionalAnswers;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class ApplicationProcessorTest {

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

  private final Application app = Mockito.mock(Application.class);

  private final Application app2 = Mockito.mock(Application.class);

  private final Application app3 = Mockito.mock(Application.class);

  private final Application app4 = Mockito.mock(Application.class);

  private final ApplicationService appService = Mockito.mock(ApplicationService.class);

  private final ApplicationProcessor appProcessor =
      Mockito.mock(
          ApplicationProcessor.class,
          Mockito.withSettings()
              .useConstructor(appService)
              .defaultAnswer(Answers.CALLS_REAL_METHODS));

  private final TaskList tasks = Mockito.mock(TaskList.class);

  private final ProfileMigrationReport report = Mockito.mock(ProfileMigrationReport.class);

  private final JsonProfile jprofile = Mockito.mock(JsonProfile.class);

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() throws Exception {
    // execute the tasks added right away
    Mockito.doAnswer(
            AdditionalAnswers.<Boolean, Operation, String, Predicate<ProfileMigrationReport>>answer(
                (o, i, t) -> t.test(report)))
        .when(tasks)
        .add(Mockito.any(), Mockito.anyString(), Mockito.any());
    final URI uri = new URI("https://test.com/test");
    final URI uri2 = new URI("https://test.com/test2");
    final URI uri3 = new URI("https://test.com/test3");
    final URI uri4 = new URI("https://test.com/test4");

    Mockito.when(app.getName()).thenReturn(NAME);
    Mockito.when(app.getVersion()).thenReturn(VERSION);
    Mockito.when(app.getDescription()).thenReturn(DESCRIPTION);
    Mockito.when(app.getURI()).thenReturn(uri);
    Mockito.when(appService.isApplicationStarted(app)).thenReturn(STARTED);

    Mockito.when(app2.getName()).thenReturn(NAME2);
    Mockito.when(app2.getVersion()).thenReturn(VERSION2);
    Mockito.when(app2.getDescription()).thenReturn(DESCRIPTION2);
    Mockito.when(app2.getURI()).thenReturn(uri2);
    Mockito.when(appService.isApplicationStarted(app2)).thenReturn(STARTED2);

    Mockito.when(app3.getName()).thenReturn(NAME3);
    Mockito.when(app3.getVersion()).thenReturn(VERSION3);
    Mockito.when(app3.getDescription()).thenReturn(DESCRIPTION3);
    Mockito.when(app3.getURI()).thenReturn(uri3);
    Mockito.when(appService.isApplicationStarted(app3)).thenReturn(STARTED3);

    Mockito.when(app4.getName()).thenReturn(NAME4);
    Mockito.when(app4.getVersion()).thenReturn(VERSION4);
    Mockito.when(app4.getDescription()).thenReturn(DESCRIPTION4);
    Mockito.when(app4.getURI()).thenReturn(uri4);
    Mockito.when(appService.isApplicationStarted(app4)).thenReturn(STARTED4);
  }

  @Test
  public void testStartApplication() throws Exception {
    Mockito.doNothing().when(appService).startApplication(app);

    Assert.assertThat(appProcessor.startApplication(report, app), Matchers.equalTo(true));

    Mockito.verify(appService).startApplication(app);
    Mockito.verify(report, Mockito.never()).recordOnFinalAttempt(Mockito.any());
  }

  @Test
  public void testStartApplicationWhenFail() throws Exception {
    final ApplicationServiceException e = new ApplicationServiceException();

    Mockito.doThrow(e).when(appService).startApplication(app);

    Assert.assertThat(appProcessor.startApplication(report, app), Matchers.equalTo(false));

    final ArgumentCaptor<MigrationException> captor =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(appService).startApplication(app);
    Mockito.verify(report).recordOnFinalAttempt(captor.capture());

    final MigrationException me = captor.getValue();

    Assert.assertThat(
        me.getMessage(), Matchers.containsString("failed to start application [" + NAME + "]"));
    Assert.assertThat(me.getCause(), Matchers.sameInstance(e));
  }

  @Test
  public void testGetApplication() throws Exception {
    Mockito.when(appService.getApplication(NAME)).thenReturn(app);

    Assert.assertThat(appProcessor.getApplication(report, NAME), Matchers.sameInstance(app));

    Mockito.verify(appService).getApplication(NAME);
    Mockito.verify(report, Mockito.never()).record(Mockito.any(MigrationException.class));
  }

  @Test
  public void testGetApplicationWhenNotInstalled() throws Exception {
    Mockito.when(appService.getApplication(NAME)).thenReturn(null);

    Assert.assertThat(appProcessor.getApplication(report, NAME), Matchers.equalTo(null));

    final ArgumentCaptor<MigrationException> captor =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(appService).getApplication(NAME);
    Mockito.verify(report).record(captor.capture());

    Assert.assertThat(
        captor.getValue().getMessage(),
        Matchers.containsString("failed to start application [" + NAME + "]"));
  }

  @Test
  public void testProcessStartedApplicationWhenNotStarted() throws Exception {
    Mockito.doReturn(true).when(appProcessor).startApplication(report, app);

    Assert.assertThat(
        appProcessor.processStartedApplication(app, NAME, false, tasks), Matchers.equalTo(true));

    Mockito.verify(tasks).add(Mockito.eq(Operation.START), Mockito.eq(NAME), Mockito.notNull());
    Mockito.verify(appProcessor).startApplication(report, app);
  }

  @Test
  public void testProcessStartedApplicationWhenAlreadyStarted() throws Exception {
    Assert.assertThat(
        appProcessor.processStartedApplication(app, NAME, true, tasks), Matchers.equalTo(false));

    Mockito.verify(tasks, Mockito.never()).add(Mockito.any(), Mockito.eq(NAME), Mockito.notNull());
    Mockito.verify(appProcessor, Mockito.never()).startApplication(report, app);
  }

  @Test
  public void testProcessApplicationWhenItWasStartedAndItIsStarted() throws Exception {
    final JsonApplication japp = new JsonApplication(NAME, STARTED);

    appProcessor.processApplication(japp, app, tasks);

    Mockito.verify(tasks, Mockito.never()).add(Mockito.any(), Mockito.eq(NAME), Mockito.notNull());
    Mockito.verify(appProcessor, Mockito.never()).startApplication(report, app);
  }

  @Test
  public void testProcessApplicationWhenItWasNotStartedAndItIsStarted() throws Exception {
    final JsonApplication japp = new JsonApplication(NAME, !STARTED);

    appProcessor.processApplication(japp, app, tasks);

    Mockito.verify(tasks, Mockito.never()).add(Mockito.any(), Mockito.eq(NAME), Mockito.notNull());
    Mockito.verify(appProcessor, Mockito.never()).startApplication(report, app);
  }

  @Test
  public void testProcessApplicationWhenItWasStartedAndItIsNotStarted() throws Exception {
    final JsonApplication japp = new JsonApplication(NAME2, !STARTED2);

    Mockito.doReturn(true).when(appProcessor).startApplication(report, app2);

    appProcessor.processApplication(japp, app2, tasks);

    Mockito.verify(tasks).add(Mockito.eq(Operation.START), Mockito.eq(NAME2), Mockito.notNull());
    Mockito.verify(appProcessor).startApplication(report, app2);
  }

  @Test
  public void testProcessApplicationWhenItWasNotStartedAndItIsNotStarted() throws Exception {
    final JsonApplication japp = new JsonApplication(NAME2, STARTED2);

    appProcessor.processApplication(japp, app2, tasks);

    Mockito.verify(tasks, Mockito.never()).add(Mockito.any(), Mockito.eq(NAME), Mockito.notNull());
    Mockito.verify(appProcessor, Mockito.never()).startApplication(report, app2);
  }

  @Test
  public void testProcessApplicationWhenItWasNotInstalled() throws Exception {
    appProcessor.processApplication(null, app, tasks);

    Mockito.verify(tasks, Mockito.never()).add(Mockito.any(), Mockito.eq(NAME), Mockito.notNull());
    Mockito.verify(appProcessor, Mockito.never()).startApplication(report, app);
  }

  @Test
  public void testProcessLeftOverExportedApplicationsWhenWasStarted() throws Exception {
    final JsonApplication japp = new JsonApplication(NAME, STARTED);
    final Map<String, JsonApplication> japps = ImmutableMap.of(NAME, japp);

    Mockito.doReturn(app).when(appProcessor).getApplication(report, NAME);
    Mockito.doNothing().when(appProcessor).processApplication(japp, app, tasks);

    Assert.assertThat(
        appProcessor.processLeftoverExportedApplications(report, japps, tasks),
        Matchers.equalTo(true));

    Mockito.verify(appProcessor).getApplication(report, NAME);
    Mockito.verify(appProcessor).processApplication(japp, app, tasks);
  }

  @Test
  public void testProcessLeftOverExportedApplicationsWhenNotFoundInMemory() throws Exception {
    final JsonApplication japp = new JsonApplication(NAME, STARTED);
    final Map<String, JsonApplication> japps = ImmutableMap.of(NAME, japp);

    Mockito.doReturn(null).when(appProcessor).getApplication(report, NAME);

    Assert.assertThat(
        appProcessor.processLeftoverExportedApplications(report, japps, tasks),
        Matchers.equalTo(false));

    Mockito.verify(appProcessor).getApplication(report, NAME);
    Mockito.verify(appProcessor, Mockito.never())
        .processApplication(Mockito.same(japp), Mockito.any(), Mockito.same(tasks));
  }

  @Test
  public void testProcessLeftOverExportedApplicationsWhenWasNotStarted() throws Exception {
    final JsonApplication japp2 = new JsonApplication(NAME2, STARTED2);
    final Map<String, JsonApplication> japps = ImmutableMap.of(NAME2, japp2);

    Assert.assertThat(
        appProcessor.processLeftoverExportedApplications(report, japps, tasks),
        Matchers.equalTo(true));

    Mockito.verify(appProcessor, Mockito.never()).getApplication(report, NAME2);
    Mockito.verify(appProcessor, Mockito.never())
        .processApplication(Mockito.same(japp2), Mockito.any(), Mockito.same(tasks));
  }

  @Test
  public void testProcessLeftOverExportedApplicationsWhenNothingLeft() throws Exception {
    final Map<String, JsonApplication> japps = Collections.emptyMap();

    Assert.assertThat(
        appProcessor.processLeftoverExportedApplications(report, japps, tasks),
        Matchers.equalTo(true));

    Mockito.verify(appProcessor, Mockito.never())
        .getApplication(Mockito.same(report), Mockito.any());
    Mockito.verify(appProcessor, Mockito.never())
        .processApplication(Mockito.any(), Mockito.any(), Mockito.same(tasks));
  }

  @Test
  public void testProcessMemoryApplicationsWhenFoundInExport() throws Exception {
    final JsonApplication japp = new JsonApplication(NAME, STARTED);
    final Map<String, JsonApplication> japps = new HashMap<>();

    japps.put(NAME, japp);

    Mockito.doReturn(ImmutableSet.of(app)).when(appService).getApplications();
    Mockito.doNothing().when(appProcessor).processApplication(japp, app, tasks);

    appProcessor.processMemoryApplications(japps, tasks);

    Assert.assertThat(japps.isEmpty(), Matchers.equalTo(true));

    Mockito.verify(appService).getApplications();
    Mockito.verify(appProcessor).processApplication(japp, app, tasks);
  }

  @Test
  public void testProcessMemoryApplicationsWhenNotFoundInExport() throws Exception {
    final JsonApplication japp = new JsonApplication(NAME, STARTED);
    final Map<String, JsonApplication> japps = new HashMap<>();

    japps.put(NAME, japp);

    Mockito.doReturn(ImmutableSet.of(app2)).when(appService).getApplications();
    Mockito.doNothing().when(appProcessor).processApplication(null, app2, tasks);

    appProcessor.processMemoryApplications(japps, tasks);

    Assert.assertThat(
        japps, Matchers.allOf(Matchers.aMapWithSize(1), Matchers.hasEntry(NAME, japp)));

    Mockito.verify(appService).getApplications();
    Mockito.verify(appProcessor).processApplication(null, app2, tasks);
  }

  @Test
  public void testProcessMemoryApplicationsWhenNothingsFoundInMemory() throws Exception {
    final JsonApplication japp = new JsonApplication(NAME, STARTED);
    final Map<String, JsonApplication> japps = new HashMap<>();

    japps.put(NAME, japp);

    Mockito.doReturn(Collections.emptySet()).when(appService).getApplications();
    Mockito.doNothing()
        .when(appProcessor)
        .processApplication(Mockito.any(), Mockito.any(), Mockito.same(tasks));

    appProcessor.processMemoryApplications(japps, tasks);

    Assert.assertThat(
        japps, Matchers.allOf(Matchers.aMapWithSize(1), Matchers.hasEntry(NAME, japp)));

    Mockito.verify(appService).getApplications();
    Mockito.verify(appProcessor, Mockito.never())
        .processApplication(Mockito.any(), Mockito.any(), Mockito.same(tasks));
  }

  @Test
  public void testProcessApplications() throws Exception {
    final JsonApplication japp = new JsonApplication(NAME, STARTED);
    final JsonApplication japp2 = new JsonApplication(NAME2, STARTED2);
    final JsonApplication japp4 = new JsonApplication(NAME4, STARTED4);
    // because the processMemoryApplications has a side effect of modifying the provided map and
    // because Mockito ArgumentCapture only captures references to arguments and not a clone of
    // them we are forced to clone the japps received in the processMemoryApplications() in the
    // doAnswer() so we can verify that exact content at the end
    final AtomicReference<Map<String, JsonApplication>> jappsAtMemory = new AtomicReference<>();

    Mockito.doReturn(Stream.of(japp, japp2, japp4)).when(jprofile).applications();
    Mockito.doAnswer(
            AdditionalAnswers.<Map<String, JsonApplication>, TaskList>answerVoid(
                (jappsMap, tasks) -> {
                  // capture a clone of the args for later verification
                  jappsAtMemory.set(new LinkedHashMap<>(jappsMap));
                  jappsMap.remove(NAME2); // simulate removing entries from the map
                }))
        .when(appProcessor)
        .processMemoryApplications(Mockito.notNull(), Mockito.same(tasks));
    Mockito.doReturn(true)
        .when(appProcessor)
        .processLeftoverExportedApplications(
            Mockito.same(report), Mockito.notNull(), Mockito.same(tasks));

    Assert.assertThat(
        appProcessor.processApplications(report, jprofile, tasks), Matchers.equalTo(true));

    final ArgumentCaptor<Map<String, JsonApplication>> jappsAtLeftover =
        ArgumentCaptor.forClass(Map.class);

    Mockito.verify(appProcessor).processMemoryApplications(Mockito.notNull(), Mockito.same(tasks));
    Mockito.verify(appProcessor)
        .processLeftoverExportedApplications(
            Mockito.same(report), jappsAtLeftover.capture(), Mockito.same(tasks));
    Mockito.verify(jprofile).applications();

    Assert.assertThat(
        jappsAtMemory.get(),
        Matchers.allOf(
            Matchers.aMapWithSize(3),
            Matchers.hasEntry(NAME, japp),
            Matchers.hasEntry(NAME2, japp2),
            Matchers.hasEntry(NAME4, japp4)));
    Assert.assertThat(
        jappsAtLeftover.getValue(),
        Matchers.allOf(
            Matchers.aMapWithSize(2),
            Matchers.hasEntry(NAME, japp),
            Matchers.hasEntry(NAME4, japp4)));
  }

  @Test
  public void testProcessApplicationsWhenUnableToFindInstalledApps() throws Exception {
    final JsonApplication japp = new JsonApplication(NAME, STARTED);
    final JsonApplication japp2 = new JsonApplication(NAME2, STARTED2);
    final JsonApplication japp4 = new JsonApplication(NAME4, STARTED4);
    // because the processMemoryApplications has a side effect of modifying the provided map and
    // because Mockito ArgumentCapture only captures references to arguments and not a clone of
    // them we are forced to clone the japps received in the processMemoryApplications() in the
    // doAnswer() so we can verify that exact content at the end
    final AtomicReference<Map<String, JsonApplication>> jappsAtMemory = new AtomicReference<>();

    Mockito.doReturn(Stream.of(japp, japp2, japp4)).when(jprofile).applications();
    Mockito.doAnswer(
            AdditionalAnswers.<Map<String, JsonApplication>, TaskList>answerVoid(
                (jappsMap, tasks) -> {
                  // capture a clone of the args for later verification
                  jappsAtMemory.set(new LinkedHashMap<>(jappsMap));
                  jappsMap.remove(NAME2); // simulate removing entries from the map
                }))
        .when(appProcessor)
        .processMemoryApplications(Mockito.notNull(), Mockito.same(tasks));
    Mockito.doReturn(false)
        .when(appProcessor)
        .processLeftoverExportedApplications(
            Mockito.same(report), Mockito.notNull(), Mockito.same(tasks));

    Assert.assertThat(
        appProcessor.processApplications(report, jprofile, tasks), Matchers.equalTo(false));

    final ArgumentCaptor<Map<String, JsonApplication>> jappsAtLeftover =
        ArgumentCaptor.forClass(Map.class);

    Mockito.verify(appProcessor).processMemoryApplications(Mockito.notNull(), Mockito.same(tasks));
    Mockito.verify(appProcessor)
        .processLeftoverExportedApplications(
            Mockito.same(report), jappsAtLeftover.capture(), Mockito.same(tasks));
    Mockito.verify(jprofile).applications();

    Assert.assertThat(
        jappsAtMemory.get(),
        Matchers.allOf(
            Matchers.aMapWithSize(3),
            Matchers.hasEntry(NAME, japp),
            Matchers.hasEntry(NAME2, japp2),
            Matchers.hasEntry(NAME4, japp4)));
    Assert.assertThat(
        jappsAtLeftover.getValue(),
        Matchers.allOf(
            Matchers.aMapWithSize(2),
            Matchers.hasEntry(NAME, japp),
            Matchers.hasEntry(NAME4, japp4)));
  }
}
