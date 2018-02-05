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

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesService;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.Mockito;

public class FeatureMigratorTest {

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
  private static final boolean REQUIRED = true;
  private static final boolean REQUIRED2 = false;
  private static final boolean REQUIRED3 = false;
  private static final boolean REQUIRED4 = true;
  private static final int START = 51;
  private static final int START2 = 52;
  private static final int START3 = 51;
  private static final int START4 = 54;

  private static final String REPOSITORY = "test.repo";
  private static final String REPOSITORY2 = "teat.repo2";
  private static final String REPOSITORY3 = "test.repo3";
  private static final String REPOSITORY4 = null;

  private final Feature feature = Mockito.mock(Feature.class);

  private final Feature feature2 = Mockito.mock(Feature.class);

  private final Feature feature3 = Mockito.mock(Feature.class);

  private final Feature feature4 = Mockito.mock(Feature.class);

  // use null for region since we currently do not extract that info from anywhere in JFeature ctor

  private final JsonFeature jfeature =
      new JsonFeature(NAME, ID, VERSION, DESCRIPTION, STATE, REQUIRED, null, REPOSITORY, START);

  private final JsonFeature jfeature2 =
      new JsonFeature(
          NAME2, ID2, VERSION2, DESCRIPTION2, STATE2, REQUIRED2, null, REPOSITORY2, START2);

  private final JsonFeature jfeature3 =
      new JsonFeature(
          NAME3, ID3, VERSION3, DESCRIPTION3, STATE3, REQUIRED3, null, REPOSITORY3, START3);

  private final TaskList tasks = Mockito.mock(TaskList.class);

  private final FeaturesService featuresService = Mockito.mock(FeaturesService.class);

  private final FeatureProcessor featureProcessor = Mockito.mock(FeatureProcessor.class);

  private final FeatureMigrator featureMigrator =
      Mockito.mock(
          FeatureMigrator.class,
          Mockito.withSettings()
              .useConstructor(featuresService, featureProcessor)
              .defaultAnswer(Answers.CALLS_REAL_METHODS));

  private final ProfileMigrationReport report = Mockito.mock(ProfileMigrationReport.class);

  private final JsonProfile jprofile = Mockito.mock(JsonProfile.class);

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() throws Exception {
    Mockito.when(feature.getId()).thenReturn(ID);
    Mockito.when(feature.getName()).thenReturn(NAME);
    Mockito.when(feature.getVersion()).thenReturn(VERSION);
    Mockito.when(feature.getDescription()).thenReturn(DESCRIPTION);
    Mockito.when(feature.getRepositoryUrl()).thenReturn(REPOSITORY);
    Mockito.when(featuresService.getState(ID)).thenReturn(STATE);
    Mockito.when(featuresService.isRequired(feature)).thenReturn(REQUIRED);
    Mockito.when(feature.getStartLevel()).thenReturn(START);

    Mockito.when(feature2.getId()).thenReturn(ID2);
    Mockito.when(feature2.getName()).thenReturn(NAME2);
    Mockito.when(feature2.getVersion()).thenReturn(VERSION2);
    Mockito.when(feature2.getDescription()).thenReturn(DESCRIPTION2);
    Mockito.when(feature2.getRepositoryUrl()).thenReturn(REPOSITORY2);
    Mockito.when(featuresService.getState(ID2)).thenReturn(STATE2);
    Mockito.when(featuresService.isRequired(feature2)).thenReturn(REQUIRED2);
    Mockito.when(feature2.getStartLevel()).thenReturn(START2);

    Mockito.when(feature3.getId()).thenReturn(ID3);
    Mockito.when(feature3.getName()).thenReturn(NAME3);
    Mockito.when(feature3.getVersion()).thenReturn(VERSION3);
    Mockito.when(feature3.getDescription()).thenReturn(DESCRIPTION3);
    Mockito.when(feature3.getRepositoryUrl()).thenReturn(REPOSITORY3);
    Mockito.when(featuresService.getState(ID3)).thenReturn(STATE3);
    Mockito.when(featuresService.isRequired(feature3)).thenReturn(REQUIRED3);
    Mockito.when(feature3.getStartLevel()).thenReturn(START3);

    Mockito.when(feature4.getId()).thenReturn(ID4);
    Mockito.when(feature4.getName()).thenReturn(NAME4);
    Mockito.when(feature4.getVersion()).thenReturn(VERSION4);
    Mockito.when(feature4.getDescription()).thenReturn(DESCRIPTION4);
    Mockito.when(feature4.getRepositoryUrl()).thenReturn(REPOSITORY4);
    Mockito.when(featuresService.isRequired(feature4)).thenReturn(REQUIRED4);
    Mockito.when(featuresService.getState(ID4)).thenReturn(STATE4);
    Mockito.when(feature4.getStartLevel()).thenReturn(START4);

    Mockito.when(featureMigrator.newTaskList(report)).thenReturn(tasks);
  }

  @Test
  public void testConstructorWithNullFeatureService() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null features service"));

    new FeatureMigrator(null, featureProcessor);
  }

  @Test
  public void testConstructorWithNullProcessor() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null feature processor"));

    new FeatureMigrator(featuresService, null);
  }

  @Test
  public void testExportFeatures() throws Exception {
    Mockito.when(featureProcessor.listFeatures(Mockito.anyString()))
        .thenReturn(new Feature[] {feature, feature2, feature3});

    Assert.assertThat(
        featureMigrator.exportFeatures(), Matchers.contains(jfeature, jfeature2, jfeature3));
  }

  @Test
  public void testImportFeaturesWithTasksAndSucceeds() throws Exception {
    Mockito.doNothing().when(featureProcessor).processFeaturesAndPopulateTaskList(jprofile, tasks);
    Mockito.doReturn(false, true).when(tasks).isEmpty();
    Mockito.doReturn(true).when(tasks).execute();

    Assert.assertThat(featureMigrator.importFeatures(report, jprofile), Matchers.equalTo(true));

    Mockito.verify(featureProcessor, Mockito.times(2))
        .processFeaturesAndPopulateTaskList(jprofile, tasks);
    Mockito.verify(tasks, Mockito.times(2)).isEmpty();
    Mockito.verify(tasks).execute();
  }

  @Test
  public void testImportFeaturesWithNoTasks() throws Exception {
    Mockito.doNothing().when(featureProcessor).processFeaturesAndPopulateTaskList(jprofile, tasks);
    Mockito.doReturn(true).when(tasks).isEmpty();

    Assert.assertThat(featureMigrator.importFeatures(report, jprofile), Matchers.equalTo(true));

    Mockito.verify(featureProcessor).processFeaturesAndPopulateTaskList(jprofile, tasks);
    Mockito.verify(tasks).isEmpty();
    Mockito.verify(tasks, Mockito.never()).execute();
  }

  @Test
  public void testImportFeaturesWhenFailedToExecuteTasks() throws Exception {
    Mockito.doNothing().when(featureProcessor).processFeaturesAndPopulateTaskList(jprofile, tasks);
    Mockito.doReturn(false).when(tasks).isEmpty();
    Mockito.doReturn(false).when(tasks).execute();

    Assert.assertThat(featureMigrator.importFeatures(report, jprofile), Matchers.equalTo(false));

    Mockito.verify(featureProcessor).processFeaturesAndPopulateTaskList(jprofile, tasks);
    Mockito.verify(tasks).isEmpty();
    Mockito.verify(tasks).execute();
  }
}
