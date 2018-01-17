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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesService;
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

public class FeatureProcessorTest {
  private static final EnumSet NO_AUTO_REFRESH =
      EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles);

  private static final String ID = "test.id";
  private static final String ID2 = "test.id2";
  private static final String ID3 = "test.id3";
  private static final String ID4 = "test.id4";
  private static final String NAME = "test.name";
  private static final String NAME2 = "test.name2";
  private static final String NAME3 = "test.name3";
  private static final String NAME4 = "test.name4";
  private static final String VERSION = "1.1";
  private static final String VERSION2 = "2.1";
  private static final String VERSION3 = "3.1";
  private static final String VERSION4 = "4.1";
  private static final String DESCRIPTION = "test.description";
  private static final String DESCRIPTION2 = "test.description2";
  private static final String DESCRIPTION3 = "test.description3";
  private static final String DESCRIPTION4 = "test.description4";
  private static final FeatureState STATE = FeatureState.Resolved;
  private static final FeatureState STATE2 = FeatureState.Uninstalled;
  private static final FeatureState STATE3 = FeatureState.Installed;
  private static final FeatureState STATE4 = FeatureState.Started;
  private static final int START = 51;
  private static final int START2 = 52;
  private static final int START3 = 51;
  private static final int START4 = 54;
  private static final String REGION = "REGION";
  private static final String REGION2 = "REGION2";
  private static final String REGION3 = "REGION3";
  private static final String REGION4 = "REGION4";
  private static final String REPOSITORY = "test.repo";
  private static final String REPOSITORY2 = "teat.repo2";
  private static final String REPOSITORY3 = "test.repo3";
  private static final String REPOSITORY4 = null;

  private final Feature feature = Mockito.mock(Feature.class);

  private final Feature feature2 = Mockito.mock(Feature.class);

  private final Feature feature3 = Mockito.mock(Feature.class);

  private final Feature feature4 = Mockito.mock(Feature.class);

  private final JsonFeature jfeature = new JsonFeature(NAME, ID, STATE, REGION, REPOSITORY, START);
  private final JsonFeature jfeature2 =
      new JsonFeature(NAME2, ID2, STATE2, REGION2, REPOSITORY2, START2);
  private final JsonFeature jfeature4 =
      new JsonFeature(NAME4, ID4, STATE4, REGION4, REPOSITORY4, START4);

  private final FeaturesService featuresService = Mockito.mock(FeaturesService.class);

  private final FeatureProcessor featureProcessor =
      Mockito.mock(
          FeatureProcessor.class,
          Mockito.withSettings()
              .useConstructor(featuresService)
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

    Mockito.when(feature.getId()).thenReturn(ID);
    Mockito.when(feature.getName()).thenReturn(NAME);
    Mockito.when(feature.getVersion()).thenReturn(VERSION);
    Mockito.when(feature.getDescription()).thenReturn(DESCRIPTION);
    Mockito.when(feature.getRepositoryUrl()).thenReturn(REPOSITORY);
    Mockito.when(featuresService.getState(ID)).thenReturn(STATE);
    Mockito.when(feature.getStartLevel()).thenReturn(START);

    Mockito.when(feature2.getId()).thenReturn(ID2);
    Mockito.when(feature2.getName()).thenReturn(NAME2);
    Mockito.when(feature2.getVersion()).thenReturn(VERSION2);
    Mockito.when(feature2.getDescription()).thenReturn(DESCRIPTION2);
    Mockito.when(feature.getRepositoryUrl()).thenReturn(REPOSITORY2);
    Mockito.when(featuresService.getState(ID2)).thenReturn(STATE2);
    Mockito.when(feature.getStartLevel()).thenReturn(START2);

    Mockito.when(feature3.getId()).thenReturn(ID3);
    Mockito.when(feature3.getName()).thenReturn(NAME3);
    Mockito.when(feature3.getVersion()).thenReturn(VERSION3);
    Mockito.when(feature3.getDescription()).thenReturn(DESCRIPTION3);
    Mockito.when(feature.getRepositoryUrl()).thenReturn(REPOSITORY3);
    Mockito.when(featuresService.getState(ID3)).thenReturn(STATE3);
    Mockito.when(feature.getStartLevel()).thenReturn(START3);

    Mockito.when(feature4.getId()).thenReturn(ID4);
    Mockito.when(feature4.getName()).thenReturn(NAME4);
    Mockito.when(feature4.getVersion()).thenReturn(VERSION4);
    Mockito.when(feature4.getDescription()).thenReturn(DESCRIPTION4);
    Mockito.when(feature.getRepositoryUrl()).thenReturn(REPOSITORY4);
    Mockito.when(featuresService.getState(ID4)).thenReturn(STATE4);
    Mockito.when(feature.getStartLevel()).thenReturn(START4);
  }

  @Test
  public void testConstructorWithNullService() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null features service"));

    new FeatureProcessor(null);
  }

  @Test
  public void testGetFeature() throws Exception {
    Mockito.doReturn(feature).when(featuresService).getFeature(ID);

    Assert.assertThat(featureProcessor.getFeature(report, ID), Matchers.sameInstance(feature));

    Mockito.verify(featuresService).getFeature(ID);
  }

  @Test
  public void testGetFeatureWhenNotFound() throws Exception {
    Mockito.doReturn(null).when(featuresService).getFeature(ID);

    Assert.assertThat(featureProcessor.getFeature(report, ID), Matchers.nullValue());

    Mockito.verify(featuresService).getFeature(ID);
    verifyRecord(ID, "retrieve", null);
  }

  @Test
  public void testGetFeatureWhenFailedToRetrieve() throws Exception {
    final Exception error = new Exception();

    Mockito.doThrow(error).when(featuresService).getFeature(ID);

    Assert.assertThat(featureProcessor.getFeature(report, ID), Matchers.nullValue());

    Mockito.verify(featuresService).getFeature(ID);
    verifyRecord(ID, "retrieve", error);
  }

  @Test
  public void testListFeatures() throws Exception {
    final Feature[] features = new Feature[] {};

    Mockito.doReturn(features).when(featuresService).listFeatures();

    Assert.assertThat(featureProcessor.listFeatures("TEST"), Matchers.sameInstance(features));

    Mockito.verify(featuresService).listFeatures();
  }

  @Test
  public void testListFeaturesWhenFailedToRetrieve() throws Exception {
    final Exception error = new Exception();

    Mockito.doThrow(error).when(featuresService).listFeatures();

    thrown.expect(MigrationException.class);
    thrown.expectMessage(
        Matchers.allOf(
            Matchers.startsWith("TEST"), Matchers.containsString("failed to retrieve features")));

    Assert.assertThat(featureProcessor.listFeatures("TEST"), Matchers.nullValue());

    Mockito.verify(featuresService).listFeatures();
  }

  @Test
  public void testInstallFeature() throws Exception {
    Mockito.doNothing().when(featuresService).installFeature(ID, NO_AUTO_REFRESH);

    Assert.assertThat(featureProcessor.installFeature(report, feature), Matchers.equalTo(true));

    Mockito.verify(featuresService).installFeature(ID, NO_AUTO_REFRESH);
    Mockito.verify(report, Mockito.never()).recordOnFinalAttempt(Mockito.any());
  }

  @Test
  public void testInstallFeatureWhenFailWithException() throws Exception {
    final Exception e = new Exception();

    Mockito.doThrow(e).when(featuresService).installFeature(ID, NO_AUTO_REFRESH);

    Assert.assertThat(featureProcessor.installFeature(report, feature), Matchers.equalTo(false));

    Mockito.verify(featuresService).installFeature(ID, NO_AUTO_REFRESH);
    verifyRecordOnFinalAttempt(ID, "install", e);
  }

  @Test
  public void testUninstallFeature() throws Exception {
    Mockito.doNothing().when(featuresService).uninstallFeature(ID, NO_AUTO_REFRESH);

    Assert.assertThat(featureProcessor.uninstallFeature(report, feature), Matchers.equalTo(true));

    Mockito.verify(featuresService).uninstallFeature(ID, NO_AUTO_REFRESH);
    Mockito.verify(report, Mockito.never()).recordOnFinalAttempt(Mockito.any());
  }

  @Test
  public void testUninstallFeatureWhenFailWithException() throws Exception {
    final Exception e = new Exception();

    Mockito.doThrow(e).when(featuresService).uninstallFeature(ID, NO_AUTO_REFRESH);

    Assert.assertThat(featureProcessor.uninstallFeature(report, feature), Matchers.equalTo(false));

    Mockito.verify(featuresService).uninstallFeature(ID, NO_AUTO_REFRESH);
    verifyRecordOnFinalAttempt(ID, "uninstall", e);
  }

  @Test
  public void testStartFeature() throws Exception {
    Mockito.doNothing()
        .when(featuresService)
        .updateFeaturesState(Mockito.notNull(), Mockito.eq(NO_AUTO_REFRESH));

    Assert.assertThat(
        featureProcessor.startFeature(report, feature, REGION), Matchers.equalTo(true));

    verifyFeatureStateUpdated(ID, FeatureState.Started);

    Mockito.verify(report, Mockito.never()).recordOnFinalAttempt(Mockito.any());
  }

  @Test
  public void testStartFeatureWhenFailWithException() throws Exception {
    final Exception e = new Exception();

    Mockito.doThrow(e)
        .when(featuresService)
        .updateFeaturesState(Mockito.notNull(), Mockito.eq(NO_AUTO_REFRESH));

    Assert.assertThat(
        featureProcessor.startFeature(report, feature, REGION), Matchers.equalTo(false));

    verifyFeatureStateUpdated(ID, FeatureState.Started);
    verifyRecordOnFinalAttempt(ID, "start", e);
  }

  @Test
  public void testStopFeature() throws Exception {
    Mockito.doNothing()
        .when(featuresService)
        .updateFeaturesState(Mockito.notNull(), Mockito.eq(NO_AUTO_REFRESH));

    Assert.assertThat(
        featureProcessor.stopFeature(report, feature, REGION), Matchers.equalTo(true));

    verifyFeatureStateUpdated(ID, FeatureState.Resolved);

    Mockito.verify(report, Mockito.never()).recordOnFinalAttempt(Mockito.any());
  }

  @Test
  public void testStopFeatureWhenFailWithException() throws Exception {
    final Exception e = new Exception();

    Mockito.doThrow(e)
        .when(featuresService)
        .updateFeaturesState(Mockito.notNull(), Mockito.eq(NO_AUTO_REFRESH));

    Assert.assertThat(
        featureProcessor.stopFeature(report, feature, REGION), Matchers.equalTo(false));

    verifyFeatureStateUpdated(ID, FeatureState.Resolved);
    verifyRecordOnFinalAttempt(ID, "stop", e);
  }

  @Test
  public void testProcessInstalledFeatureWhenUninstalled() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).installFeature(report, feature);

    featureProcessor.processInstalledFeature(feature, ID, FeatureState.Uninstalled, REGION, tasks);

    verifyTaskListAndExecution(feature, ID, REGION, 1, 0, 0, 0);
  }

  @Test
  public void testProcessInstalledFeatureWhenInstalled() throws Exception {
    featureProcessor.processInstalledFeature(feature, ID, FeatureState.Installed, REGION, tasks);

    verifyTaskListAndExecution(feature, ID, REGION, 0, 0, 0, 0);
  }

  @Test
  public void testProcessInstalledFeatureWhenStarted() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).stopFeature(report, feature, REGION);

    featureProcessor.processInstalledFeature(feature, ID, FeatureState.Started, REGION, tasks);

    verifyTaskListAndExecution(feature, ID, REGION, 0, 0, 0, 1);
  }

  @Test
  public void testProcessInstalledFeatureWhenResolved() throws Exception {
    featureProcessor.processInstalledFeature(feature, ID, FeatureState.Resolved, REGION, tasks);

    verifyTaskListAndExecution(feature, ID, REGION, 0, 0, 0, 0);
  }

  @Test
  public void testProcessResolvedFeatureWhenUninstalled() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).installFeature(report, feature);

    featureProcessor.processResolvedFeature(feature, ID, FeatureState.Uninstalled, REGION, tasks);

    verifyTaskListAndExecution(feature, ID, REGION, 1, 0, 0, 0);
    Mockito.verify(tasks).increaseAttemptsFor(Operation.STOP);
  }

  @Test
  public void testProcessResolvedFeatureWhenInstalled() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).stopFeature(report, feature, REGION);

    featureProcessor.processResolvedFeature(feature, ID, FeatureState.Installed, REGION, tasks);

    verifyTaskListAndExecution(feature, ID, REGION, 0, 0, 0, 1);
  }

  @Test
  public void testProcessResolvedFeatureWhenResolved() throws Exception {
    featureProcessor.processResolvedFeature(feature, ID, FeatureState.Resolved, REGION, tasks);

    verifyTaskListAndExecution(feature, ID, REGION, 0, 0, 0, 0);
  }

  @Test
  public void testProcessResolvedFeatureWhenStarted() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).stopFeature(report, feature, REGION);

    featureProcessor.processResolvedFeature(feature, ID, FeatureState.Started, REGION, tasks);

    verifyTaskListAndExecution(feature, ID, REGION, 0, 0, 0, 1);
  }

  @Test
  public void testProcessUninstalledFeatureWhenUninstalled() throws Exception {
    featureProcessor.processUninstalledFeature(feature, ID, FeatureState.Uninstalled, tasks);

    verifyTaskListAndExecution(feature, ID, REGION, 0, 0, 0, 0);
  }

  @Test
  public void testProcessUninstalledFeatureWhenInstalled() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).uninstallFeature(report, feature);

    featureProcessor.processUninstalledFeature(feature, ID, FeatureState.Installed, tasks);

    verifyTaskListAndExecution(feature, ID, REGION, 0, 1, 0, 0);
  }

  @Test
  public void testProcessUninstalledFeatureWhenResolved() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).uninstallFeature(report, feature);

    featureProcessor.processUninstalledFeature(feature, ID, FeatureState.Resolved, tasks);

    verifyTaskListAndExecution(feature, ID, REGION, 0, 1, 0, 0);
  }

  @Test
  public void testProcessUninstalledFeatureWhenStarted() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).uninstallFeature(report, feature);

    featureProcessor.processUninstalledFeature(feature, ID, FeatureState.Started, tasks);

    verifyTaskListAndExecution(feature, ID, REGION, 0, 1, 0, 0);
  }

  @Test
  public void testProcessStartedFeatureWhenUninstalled() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).installFeature(report, feature);

    featureProcessor.processStartedFeature(feature, ID, FeatureState.Uninstalled, REGION, tasks);

    verifyTaskListAndExecution(feature, ID, REGION, 1, 0, 0, 0);
    Mockito.verify(tasks).increaseAttemptsFor(Operation.START);
  }

  @Test
  public void testProcessStartedFeatureWhenInstalled() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).startFeature(report, feature, REGION);

    featureProcessor.processStartedFeature(feature, ID, FeatureState.Installed, REGION, tasks);

    verifyTaskListAndExecution(feature, ID, REGION, 0, 0, 1, 0);
  }

  @Test
  public void testProcessStartedFeatureWhenResolved() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).startFeature(report, feature, REGION);

    featureProcessor.processStartedFeature(feature, ID, FeatureState.Resolved, REGION, tasks);

    verifyTaskListAndExecution(feature, ID, REGION, 0, 0, 1, 0);
  }

  @Test
  public void testProcessFeatureWhenItWasInstalledAndItIsUninstalled() throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(NAME2, ID2, FeatureState.Installed, REGION2, REPOSITORY2, START2);

    Mockito.doReturn(true).when(featureProcessor).installFeature(report, feature2);

    featureProcessor.processFeature(jfeature2, feature2, tasks);

    verifyTaskListAndExecution(feature2, ID2, jfeature2.getRegion(), 1, 0, 0, 0);
  }

  @Test
  public void testProcessFeatureWhenItWasInstalledAndItIsInstalled() throws Exception {
    final JsonFeature jfeature3 =
        new JsonFeature(NAME3, ID3, FeatureState.Installed, REGION3, REPOSITORY3, START3);

    featureProcessor.processFeature(jfeature3, feature3, tasks);

    verifyTaskListAndExecution(feature3, ID3, jfeature3.getRegion(), 0, 0, 0, 0);
  }

  @Test
  public void testProcessFeatureWhenItWasInstalledAndItIsStarted() throws Exception {
    final JsonFeature jfeature4 =
        new JsonFeature(NAME4, ID4, FeatureState.Installed, REGION4, REPOSITORY4, START4);

    Mockito.doReturn(true).when(featureProcessor).stopFeature(report, feature4, REGION);

    featureProcessor.processFeature(jfeature4, feature4, tasks);

    verifyTaskListAndExecution(feature4, ID4, jfeature4.getRegion(), 0, 0, 0, 1);
  }

  @Test
  public void testProcessFeatureWhenItWasInstalledAndItIsResolved() throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, FeatureState.Installed, REGION, REPOSITORY, START);

    Mockito.doReturn(true).when(featureProcessor).stopFeature(report, feature, REGION);

    featureProcessor.processFeature(jfeature, feature, tasks);

    verifyTaskListAndExecution(feature, ID, jfeature.getRegion(), 0, 0, 0, 0);
  }

  @Test
  public void testProcessFeatureWhenItWasStartedAndItIsUninstalled() throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(NAME2, ID2, FeatureState.Started, REGION2, REPOSITORY2, START2);

    Mockito.doReturn(true).when(featureProcessor).installFeature(report, feature2);

    featureProcessor.processFeature(jfeature2, feature2, tasks);

    verifyTaskListAndExecution(feature2, ID2, jfeature2.getRegion(), 1, 0, 0, 0);
    Mockito.verify(tasks).increaseAttemptsFor(Operation.START);
  }

  @Test
  public void testProcessFeatureWhenItWasStartedAndItIsInstalled() throws Exception {
    final JsonFeature jfeature3 =
        new JsonFeature(NAME3, ID3, FeatureState.Started, REGION3, REPOSITORY3, START3);

    Mockito.doReturn(true).when(featureProcessor).startFeature(report, feature3, REGION3);

    featureProcessor.processFeature(jfeature3, feature3, tasks);

    verifyTaskListAndExecution(feature3, ID3, jfeature3.getRegion(), 0, 0, 1, 0);
  }

  @Test
  public void testProcessFeatureWhenItWasStartedAndItIsStarted() throws Exception {
    final JsonFeature jfeature4 =
        new JsonFeature(NAME4, ID4, FeatureState.Started, REGION4, REPOSITORY4, START4);

    featureProcessor.processFeature(jfeature4, feature4, tasks);

    verifyTaskListAndExecution(feature4, ID4, jfeature4.getRegion(), 0, 0, 0, 0);
  }

  @Test
  public void testProcessFeatureWhenItWasStartedAndItIsResolved() throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, FeatureState.Started, REGION, REPOSITORY, START);

    Mockito.doReturn(true).when(featureProcessor).startFeature(report, feature, REGION);

    featureProcessor.processFeature(jfeature, feature, tasks);

    verifyTaskListAndExecution(feature, ID, jfeature.getRegion(), 0, 0, 1, 0);
  }

  @Test
  public void testProcessFeatureWhenItWasUninstalledAndItIsUninstalled() throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(NAME2, ID2, FeatureState.Uninstalled, REGION2, REPOSITORY2, START2);

    featureProcessor.processFeature(jfeature2, feature2, tasks);

    verifyTaskListAndExecution(feature2, ID2, jfeature2.getRegion(), 0, 0, 0, 0);
  }

  @Test
  public void testProcessFeatureWhenItWasUninstalledAndItIsInstalled() throws Exception {
    final JsonFeature jfeature3 =
        new JsonFeature(NAME3, ID3, FeatureState.Uninstalled, REGION3, REPOSITORY3, START3);

    Mockito.doReturn(true).when(featureProcessor).uninstallFeature(report, feature3);

    featureProcessor.processFeature(jfeature3, feature3, tasks);

    verifyTaskListAndExecution(feature3, ID3, jfeature3.getRegion(), 0, 1, 0, 0);
  }

  @Test
  public void testProcessFeatureItWasUninstalledAndItIsStarted() throws Exception {
    final JsonFeature jfeature4 =
        new JsonFeature(NAME4, ID4, FeatureState.Uninstalled, REGION4, REPOSITORY4, START4);

    Mockito.doReturn(true).when(featureProcessor).uninstallFeature(report, feature4);

    featureProcessor.processFeature(jfeature4, feature4, tasks);

    verifyTaskListAndExecution(feature4, ID4, jfeature4.getRegion(), 0, 1, 0, 0);
  }

  @Test
  public void testProcessFeatureItWasUninstalledAndItIsResolved() throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, FeatureState.Uninstalled, REGION, REPOSITORY, START);

    Mockito.doReturn(true).when(featureProcessor).uninstallFeature(report, feature);

    featureProcessor.processFeature(jfeature, feature, tasks);

    verifyTaskListAndExecution(feature, ID, jfeature.getRegion(), 0, 1, 0, 0);
  }

  @Test
  public void testProcessFeatureWhenItWasResolvedAndItIsUninstalled() throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(NAME2, ID2, FeatureState.Resolved, REGION2, REPOSITORY2, START2);

    featureProcessor.processFeature(jfeature2, feature2, tasks);

    verifyTaskListAndExecution(feature2, ID2, jfeature2.getRegion(), 1, 0, 0, 0);
    Mockito.verify(tasks).increaseAttemptsFor(Operation.STOP);
  }

  @Test
  public void testProcessFeatureWhenItWasResolvedAndItIsInstalled() throws Exception {
    final JsonFeature jfeature3 =
        new JsonFeature(NAME3, ID3, FeatureState.Resolved, REGION3, REPOSITORY3, START3);

    Mockito.doReturn(true).when(featureProcessor).stopFeature(report, feature3, REGION3);

    featureProcessor.processFeature(jfeature3, feature3, tasks);

    verifyTaskListAndExecution(feature3, ID3, jfeature3.getRegion(), 0, 0, 0, 1);
  }

  @Test
  public void testProcessFeatureItWasResolvedAndItIsStarted() throws Exception {
    final JsonFeature jfeature4 =
        new JsonFeature(NAME4, ID4, FeatureState.Resolved, REGION4, REPOSITORY4, START4);

    Mockito.doReturn(true).when(featureProcessor).stopFeature(report, feature4, REGION4);

    featureProcessor.processFeature(jfeature4, feature4, tasks);

    verifyTaskListAndExecution(feature4, ID4, jfeature4.getRegion(), 0, 0, 0, 1);
  }

  @Test
  public void testProcessFeatureItWasResolvedAndItIsResolved() throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, FeatureState.Resolved, REGION, REPOSITORY, START);

    Mockito.doReturn(true).when(featureProcessor).uninstallFeature(report, feature);

    featureProcessor.processFeature(jfeature, feature, tasks);

    verifyTaskListAndExecution(feature, ID, jfeature.getRegion(), 0, 0, 0, 0);
  }

  @Test
  public void testProcessFeatureWhenItWasNotInstalledAndItIsUninstalled() throws Exception {
    featureProcessor.processFeature(null, feature2, tasks);

    verifyTaskListAndExecution(feature2, ID2, REGION, 0, 0, 0, 0);
  }

  @Test
  public void testProcessFeatureWhenItWasNotInstalledAndItIsInstalled() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).uninstallFeature(report, feature3);

    featureProcessor.processFeature(null, feature3, tasks);

    verifyTaskListAndExecution(feature3, ID3, REGION, 0, 1, 0, 0);
  }

  @Test
  public void testProcessFeatureItWasNotInstalledAndItIsStarted() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).uninstallFeature(report, feature4);

    featureProcessor.processFeature(null, feature4, tasks);

    verifyTaskListAndExecution(feature4, ID4, REGION, 0, 1, 0, 0);
  }

  @Test
  public void testProcessFeatureItWasNotInstalledAndItIsResolved() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).uninstallFeature(report, feature);

    featureProcessor.processFeature(null, feature, tasks);

    verifyTaskListAndExecution(feature, ID, REGION, 0, 1, 0, 0);
  }

  @Test
  public void testProcessLeftOverExportedFeaturesWhenWasUninstalled() throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(NAME2, ID2, FeatureState.Uninstalled, REGION2, REPOSITORY2, START2);
    final Map<String, JsonFeature> jfeatures = ImmutableMap.of(NAME, jfeature2);

    Mockito.doReturn(feature2).when(featuresService).getFeature(ID2);
    Mockito.doNothing().when(featureProcessor).processFeature(jfeature2, feature2, tasks);

    Assert.assertThat(
        featureProcessor.processLeftoverExportedFeatures(report, jfeatures, tasks),
        Matchers.equalTo(true));

    Mockito.verify(featureProcessor, Mockito.never()).getFeature(report, ID2);
    Mockito.verify(featureProcessor, Mockito.never()).processFeature(jfeature2, feature2, tasks);
  }

  @Test
  public void testProcessLeftOverExportedFeaturesWhenWasInstalled() throws Exception {
    final JsonFeature jfeature3 =
        new JsonFeature(NAME3, ID3, FeatureState.Installed, REGION3, REPOSITORY3, START3);
    final Map<String, JsonFeature> jfeatures = ImmutableMap.of(NAME, jfeature3);

    Mockito.doReturn(feature3).when(featuresService).getFeature(ID3);
    Mockito.doNothing().when(featureProcessor).processFeature(jfeature3, feature3, tasks);

    Assert.assertThat(
        featureProcessor.processLeftoverExportedFeatures(report, jfeatures, tasks),
        Matchers.equalTo(true));

    Mockito.verify(featuresService).getFeature(ID3);
    Mockito.verify(featureProcessor).processFeature(jfeature3, feature3, tasks);
  }

  @Test
  public void testProcessLeftOverExportedFeaturesWhenWasStarted() throws Exception {
    final JsonFeature jfeature4 =
        new JsonFeature(NAME4, ID4, FeatureState.Started, REGION4, REPOSITORY4, START4);
    final Map<String, JsonFeature> jfeatures = ImmutableMap.of(NAME, jfeature4);

    Mockito.doReturn(feature4).when(featuresService).getFeature(ID4);
    Mockito.doNothing().when(featureProcessor).processFeature(jfeature4, feature4, tasks);

    Assert.assertThat(
        featureProcessor.processLeftoverExportedFeatures(report, jfeatures, tasks),
        Matchers.equalTo(true));

    Mockito.verify(featuresService).getFeature(ID4);
    Mockito.verify(featureProcessor).processFeature(jfeature4, feature4, tasks);
  }

  @Test
  public void testProcessLeftOverExportedFeaturesWhenWasResolved() throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, FeatureState.Resolved, REGION, REPOSITORY, START);
    final Map<String, JsonFeature> jfeatures = ImmutableMap.of(NAME, jfeature);

    Mockito.doReturn(feature).when(featuresService).getFeature(ID);
    Mockito.doNothing().when(featureProcessor).processFeature(jfeature, feature, tasks);

    Assert.assertThat(
        featureProcessor.processLeftoverExportedFeatures(report, jfeatures, tasks),
        Matchers.equalTo(true));

    Mockito.verify(featuresService).getFeature(ID);
    Mockito.verify(featureProcessor).processFeature(jfeature, feature, tasks);
  }

  @Test
  public void testProcessLeftOverExportedFeaturesWhenNotFoundInMemory() throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, FeatureState.Resolved, REGION, REPOSITORY, START);
    final Map<String, JsonFeature> jfeatures = ImmutableMap.of(NAME, jfeature);

    Mockito.doReturn(null).when(featuresService).getFeature(ID);
    Mockito.doNothing().when(featureProcessor).processFeature(jfeature, feature, tasks);

    Assert.assertThat(
        featureProcessor.processLeftoverExportedFeatures(report, jfeatures, tasks),
        Matchers.equalTo(false));

    Mockito.verify(featuresService).getFeature(ID);
    Mockito.verify(featureProcessor, Mockito.never())
        .processFeature(Mockito.same(jfeature), Mockito.any(), Mockito.same(tasks));
  }

  @Test
  public void testProcessLeftOverExportedFeaturesWhenNothingLeft() throws Exception {
    final Map<String, JsonFeature> jfeatures = Collections.emptyMap();

    Assert.assertThat(
        featureProcessor.processLeftoverExportedFeatures(report, jfeatures, tasks),
        Matchers.equalTo(true));

    Mockito.verify(featuresService, Mockito.never()).getFeature(Mockito.anyString());
    Mockito.verify(featureProcessor, Mockito.never())
        .processFeature(Mockito.any(), Mockito.any(), Mockito.same(tasks));
  }

  @Test
  public void testProcessMemoryFeaturesWhenFoundInExport() throws Exception {
    final JsonFeature jfeature = new JsonFeature(NAME, ID, STATE, REGION, REPOSITORY, START);
    final Map<String, JsonFeature> jfeatures = new HashMap<>();

    jfeatures.put(jfeature.getId(), jfeature);

    Mockito.doReturn(new Feature[] {feature})
        .when(featureProcessor)
        .listFeatures(Mockito.anyString());
    Mockito.doNothing().when(featureProcessor).processFeature(jfeature, feature, tasks);

    featureProcessor.processMemoryFeatures(jfeatures, tasks);

    Assert.assertThat(jfeatures.isEmpty(), Matchers.equalTo(true));

    Mockito.verify(featureProcessor).listFeatures(Mockito.anyString());
    Mockito.verify(featureProcessor).processFeature(jfeature, feature, tasks);
  }

  @Test
  public void testProcessMemoryFeaturesWhenNotFoundInExport() throws Exception {
    final JsonFeature jfeature = new JsonFeature(NAME, ID, STATE, REGION, REPOSITORY, START);
    final Map<String, JsonFeature> jfeatures = new HashMap<>();

    jfeatures.put(jfeature.getId(), jfeature);

    Mockito.doReturn(new Feature[] {feature2})
        .when(featureProcessor)
        .listFeatures(Mockito.anyString());
    Mockito.doNothing().when(featureProcessor).processFeature(null, feature2, tasks);

    featureProcessor.processMemoryFeatures(jfeatures, tasks);

    Assert.assertThat(
        jfeatures,
        Matchers.allOf(Matchers.aMapWithSize(1), Matchers.hasEntry(jfeature.getId(), jfeature)));

    Mockito.verify(featureProcessor).listFeatures(Mockito.anyString());
    Mockito.verify(featureProcessor).processFeature(null, feature2, tasks);
  }

  @Test
  public void testProcessMemoryFeaturesWhenNothingsFoundInMemory() throws Exception {
    final JsonFeature jfeature = new JsonFeature(NAME, ID, STATE, REGION, REPOSITORY, START);
    final Map<String, JsonFeature> jfeatures = new HashMap<>();

    jfeatures.put(jfeature.getId(), jfeature);

    Mockito.doReturn(new Feature[] {}).when(featureProcessor).listFeatures(Mockito.anyString());
    Mockito.doNothing()
        .when(featureProcessor)
        .processFeature(Mockito.any(), Mockito.any(), Mockito.same(tasks));

    featureProcessor.processMemoryFeatures(jfeatures, tasks);

    Assert.assertThat(
        jfeatures,
        Matchers.allOf(Matchers.aMapWithSize(1), Matchers.hasEntry(jfeature.getId(), jfeature)));

    Mockito.verify(featureProcessor).listFeatures(Mockito.anyString());
    Mockito.verify(featureProcessor, Mockito.never())
        .processFeature(Mockito.any(), Mockito.any(), Mockito.same(tasks));
  }

  @Test
  public void testProcessFeatures() throws Exception {
    // because the processMemoryFeatures has a side effect of modifying the provided map and
    // because Mockito ArgumentCapture only captures references to arguments and not a clone of
    // them we are forced to clone the jfeatures received in the processMemoryFeatures() in the
    // doAnswer() so we can verify that exact content at the end
    final AtomicReference<Map<String, JsonFeature>> jfeaturesAtMemory = new AtomicReference<>();

    Mockito.doReturn(Stream.of(jfeature, jfeature2, jfeature4)).when(jprofile).features();
    Mockito.doAnswer(
            AdditionalAnswers.<Map<String, JsonFeature>, TaskList>answerVoid(
                (jfeaturesMap, tasks) -> {
                  // capture a clone of the args for later verification
                  jfeaturesAtMemory.set(new LinkedHashMap<>(jfeaturesMap));
                  jfeaturesMap.remove(jfeature2.getId()); // simulate removing entries fromthe map
                }))
        .when(featureProcessor)
        .processMemoryFeatures(Mockito.notNull(), Mockito.same(tasks));
    Mockito.doReturn(true)
        .when(featureProcessor)
        .processLeftoverExportedFeatures(
            Mockito.same(report), Mockito.notNull(), Mockito.same(tasks));

    Assert.assertThat(
        featureProcessor.processFeatures(report, jprofile, tasks), Matchers.equalTo(true));

    final ArgumentCaptor<Map<String, JsonFeature>> jfeaturesAtLeftover =
        ArgumentCaptor.forClass(Map.class);

    Mockito.verify(featureProcessor).processMemoryFeatures(Mockito.notNull(), Mockito.same(tasks));
    Mockito.verify(featureProcessor)
        .processLeftoverExportedFeatures(
            Mockito.same(report), jfeaturesAtLeftover.capture(), Mockito.same(tasks));
    Mockito.verify(jprofile).features();

    Assert.assertThat(
        jfeaturesAtMemory.get(),
        Matchers.allOf(
            Matchers.aMapWithSize(3),
            Matchers.hasEntry(jfeature.getId(), jfeature),
            Matchers.hasEntry(jfeature2.getId(), jfeature2),
            Matchers.hasEntry(jfeature4.getId(), jfeature4)));
    Assert.assertThat(
        jfeaturesAtLeftover.getValue(),
        Matchers.allOf(
            Matchers.aMapWithSize(2),
            Matchers.hasEntry(jfeature.getId(), jfeature),
            Matchers.hasEntry(jfeature4.getId(), jfeature4)));
  }

  @Test
  public void testProcessFeaturesWhenUnableToFindInstalledFeatures() throws Exception {
    // because the processMemoryFeatures has a side effect of modifying the provided map and
    // because Mockito ArgumentCapture only captures references to arguments and not a clone of
    // them we are forced to clone the jfeatures received in the processMemoryFeatures() in the
    // doAnswer() so we can verify that exact content at the end
    final AtomicReference<Map<String, JsonFeature>> jfeaturesAtMemory = new AtomicReference<>();

    Mockito.doReturn(Stream.of(jfeature, jfeature2, jfeature4)).when(jprofile).features();
    Mockito.doAnswer(
            AdditionalAnswers.<Map<String, JsonFeature>, TaskList>answerVoid(
                (jfeaturesMap, tasks) -> {
                  // capture a clone of the args for later verification
                  jfeaturesAtMemory.set(new LinkedHashMap<>(jfeaturesMap));
                  jfeaturesMap.remove(jfeature2.getId()); // simulate removing entries from the map
                }))
        .when(featureProcessor)
        .processMemoryFeatures(Mockito.notNull(), Mockito.same(tasks));
    Mockito.doReturn(false)
        .when(featureProcessor)
        .processLeftoverExportedFeatures(
            Mockito.same(report), Mockito.notNull(), Mockito.same(tasks));

    Assert.assertThat(
        featureProcessor.processFeatures(report, jprofile, tasks), Matchers.equalTo(false));

    final ArgumentCaptor<Map<String, JsonFeature>> jfeaturesAtLeftover =
        ArgumentCaptor.forClass(Map.class);

    Mockito.verify(featureProcessor).processMemoryFeatures(Mockito.notNull(), Mockito.same(tasks));
    Mockito.verify(featureProcessor)
        .processLeftoverExportedFeatures(
            Mockito.same(report), jfeaturesAtLeftover.capture(), Mockito.same(tasks));
    Mockito.verify(jprofile).features();

    Assert.assertThat(
        jfeaturesAtMemory.get(),
        Matchers.allOf(
            Matchers.aMapWithSize(3),
            Matchers.hasEntry(jfeature.getId(), jfeature),
            Matchers.hasEntry(jfeature2.getId(), jfeature2),
            Matchers.hasEntry(jfeature4.getId(), jfeature4)));
    Assert.assertThat(
        jfeaturesAtLeftover.getValue(),
        Matchers.allOf(
            Matchers.aMapWithSize(2),
            Matchers.hasEntry(jfeature.getId(), jfeature),
            Matchers.hasEntry(jfeature4.getId(), jfeature4)));
  }

  private void verifyFeatureStateUpdated(String id, FeatureState state) throws Exception {
    final ArgumentCaptor<Map<String, Map<String, FeatureState>>> captor =
        ArgumentCaptor.forClass(Map.class);

    Mockito.verify(featuresService)
        .updateFeaturesState(captor.capture(), Mockito.eq(NO_AUTO_REFRESH));

    Assert.assertThat(
        captor.getValue(),
        Matchers.allOf(
            Matchers.aMapWithSize(1),
            Matchers.hasEntry(
                Matchers.equalTo(REGION),
                Matchers.allOf(Matchers.aMapWithSize(1), Matchers.hasEntry(id, state)))));
  }

  private void verifyRecord(String id, String operation, Exception e) {
    final ArgumentCaptor<MigrationException> captor =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(report).record(captor.capture());

    final MigrationException me = captor.getValue();

    Assert.assertThat(
        me.getMessage(),
        Matchers.containsString("failed to " + operation + " feature [" + id + "]"));
    if (e != null) {
      Assert.assertThat(me.getCause(), Matchers.sameInstance(e));
    }
  }

  private void verifyRecordOnFinalAttempt(String id, String operation, Exception e) {
    final ArgumentCaptor<MigrationException> captor =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(report).recordOnFinalAttempt(captor.capture());

    final MigrationException me = captor.getValue();

    Assert.assertThat(
        me.getMessage(),
        Matchers.containsString("failed to " + operation + " feature [" + id + "]"));
    Assert.assertThat(me.getCause(), Matchers.sameInstance(e));
  }

  private void verifyTaskListAndExecution(
      Feature feature,
      String id,
      String region,
      int installCount,
      int uninstallCount,
      int startCount,
      int stopCount) {
    Mockito.verify(featureProcessor, Mockito.times(installCount)).installFeature(report, feature);
    Mockito.verify(tasks, Mockito.times(installCount))
        .add(Mockito.eq(Operation.INSTALL), Mockito.eq(id), Mockito.notNull());
    Mockito.verify(featureProcessor, Mockito.times(uninstallCount))
        .uninstallFeature(report, feature);
    Mockito.verify(tasks, Mockito.times(uninstallCount))
        .add(Mockito.eq(Operation.UNINSTALL), Mockito.eq(id), Mockito.notNull());
    Mockito.verify(featureProcessor, Mockito.times(startCount))
        .startFeature(report, feature, region);
    Mockito.verify(tasks, Mockito.times(startCount))
        .add(Mockito.eq(Operation.START), Mockito.eq(id), Mockito.notNull());
    Mockito.verify(featureProcessor, Mockito.times(stopCount)).stopFeature(report, feature, region);
    Mockito.verify(tasks, Mockito.times(stopCount))
        .add(Mockito.eq(Operation.STOP), Mockito.eq(id), Mockito.notNull());
  }
}
