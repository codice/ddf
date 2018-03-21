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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesService;
import org.codice.ddf.admin.application.service.migratable.TaskList.CompoundTask;
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
  private static final boolean REQUIRED = true;
  private static final boolean REQUIRED2 = true;
  private static final boolean REQUIRED3 = false;
  private static final boolean REQUIRED4 = true;
  private static final int START = 51;
  private static final int START2 = 52;
  private static final int START3 = 51;
  private static final int START4 = 54;
  private static final String REGION = "REGION";
  private static final String REGION2 = "REGION2";
  private static final String REGION3 = "REGION3";
  private static final String REPOSITORY = "test.repo";
  private static final String REPOSITORY2 = "teat.repo2";
  private static final String REPOSITORY3 = "test.repo3";
  private static final String REPOSITORY4 = null;
  private static final String REQUIREMENT =
      "feature:" + NAME + "/[" + VERSION + "," + VERSION + "]";
  private static final String REQUIREMENT2 =
      "feature:" + NAME2 + "/[" + VERSION2 + "," + VERSION2 + "]";
  private static final String REQUIREMENT3 =
      "feature:" + NAME3 + "/[" + VERSION3 + "," + VERSION3 + "]";
  private static final String REQUIREMENT4 =
      "feature:" + NAME4 + "/[" + VERSION4 + "," + VERSION4 + "]";

  private static final Set<String> IDS14 = FeatureProcessorTest.toLinkedSet(ID, ID4);
  private static final Set<String> IDS2 = FeatureProcessorTest.toLinkedSet(ID2);
  private static final Set<String> IDS3 = FeatureProcessorTest.toLinkedSet(ID3);

  private static final Map<String, Set<String>> REQUIREMENTS14 =
      ImmutableMap.of(REGION, FeatureProcessorTest.toLinkedSet(REQUIREMENT, REQUIREMENT4));
  private static final Map<String, Set<String>> REQUIREMENTS2 =
      ImmutableMap.of(REGION2, FeatureProcessorTest.toLinkedSet(REQUIREMENT2));
  private static final Map<String, Set<String>> REQUIREMENTS3 =
      ImmutableMap.of(REGION3, FeatureProcessorTest.toLinkedSet(REQUIREMENT3));

  private final Feature feature = Mockito.mock(Feature.class);
  private final Feature feature2 = Mockito.mock(Feature.class);
  private final Feature feature3 = Mockito.mock(Feature.class);
  private final Feature feature4 = Mockito.mock(Feature.class);

  private final Map<String, Set<Feature>> featuresPerRegion = new LinkedHashMap<>();
  private final Map<String, Set<Feature>> featurePerRegion = new LinkedHashMap<>();
  private final Map<String, Set<Feature>> feature2PerRegion = new LinkedHashMap<>();
  private final Map<String, Set<Feature>> feature3PerRegion = new LinkedHashMap<>();
  private final Map<String, Set<Feature>> feature4PerRegion = new LinkedHashMap<>();

  private final JsonFeature jfeature =
      new JsonFeature(NAME, ID, VERSION, null, STATE, REQUIRED, REGION, REPOSITORY, START);
  private final JsonFeature jfeature2 =
      new JsonFeature(NAME2, ID2, VERSION2, null, STATE2, REQUIRED2, REGION2, REPOSITORY2, START2);
  private final JsonFeature jfeature3 =
      new JsonFeature(NAME3, ID3, VERSION3, null, STATE3, REQUIRED3, REGION3, REPOSITORY3, START3);
  private final JsonFeature jfeature4 =
      new JsonFeature(NAME4, ID4, VERSION4, null, STATE4, REQUIRED4, REGION, REPOSITORY4, START4);

  private final Map<String, Set<JsonFeature>> jfeaturesPerRegion = new LinkedHashMap<>();
  private final Map<String, Set<JsonFeature>> jfeaturePerRegion = new LinkedHashMap<>();
  private final Map<String, Set<JsonFeature>> jfeature2PerRegion = new LinkedHashMap<>();
  private final Map<String, Set<JsonFeature>> jfeature3PerRegion = new LinkedHashMap<>();
  private final Map<String, Set<JsonFeature>> jfeature4PerRegion = new LinkedHashMap<>();

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

  public FeatureProcessorTest() {
    featuresPerRegion.computeIfAbsent(REGION, r -> new LinkedHashSet<>()).add(feature);
    featuresPerRegion.computeIfAbsent(REGION2, r -> new LinkedHashSet<>()).add(feature2);
    featuresPerRegion.computeIfAbsent(REGION3, r -> new LinkedHashSet<>()).add(feature3);
    featuresPerRegion.computeIfAbsent(REGION, r -> new LinkedHashSet<>()).add(feature4);
    featurePerRegion.computeIfAbsent(REGION, r -> new LinkedHashSet<>()).add(feature);
    feature2PerRegion.computeIfAbsent(REGION2, r -> new LinkedHashSet<>()).add(feature2);
    feature3PerRegion.computeIfAbsent(REGION3, r -> new LinkedHashSet<>()).add(feature3);
    feature4PerRegion.computeIfAbsent(REGION, r -> new LinkedHashSet<>()).add(feature4);
    jfeaturesPerRegion.computeIfAbsent(REGION, r -> new LinkedHashSet<>()).add(jfeature);
    jfeaturesPerRegion.computeIfAbsent(REGION2, r -> new LinkedHashSet<>()).add(jfeature2);
    jfeaturesPerRegion.computeIfAbsent(REGION3, r -> new LinkedHashSet<>()).add(jfeature3);
    jfeaturesPerRegion.computeIfAbsent(REGION, r -> new LinkedHashSet<>()).add(jfeature4);
    jfeaturePerRegion.computeIfAbsent(REGION, r -> new LinkedHashSet<>()).add(jfeature);
    jfeature2PerRegion.computeIfAbsent(REGION2, r -> new LinkedHashSet<>()).add(jfeature2);
    jfeature3PerRegion.computeIfAbsent(REGION3, r -> new LinkedHashSet<>()).add(jfeature3);
    jfeature4PerRegion.computeIfAbsent(REGION, r -> new LinkedHashSet<>()).add(jfeature4);
  }

  @Before
  @SuppressWarnings(
      "ReturnValueIgnored" /* only called for testing and we do not care about the result here */)
  public void setup() throws Exception {
    // execute the tasks added right away
    Mockito.doAnswer(
            AdditionalAnswers.<Boolean, Operation, String, Predicate<ProfileMigrationReport>>answer(
                (o, i, t) -> t.test(report)))
        .when(tasks)
        .add(Mockito.any(), Mockito.anyString(), Mockito.any());

    // execute the compound task, the factory, and the accumulator added later right away
    Mockito.doAnswer(
            AdditionalAnswers
                .<CompoundTask<Object>, Operation, Supplier<Object>,
                    BiPredicate<Object, ProfileMigrationReport>>
                    answer(
                        (o, s, t) -> {
                          final Object container = s.get();
                          final CompoundTask<Object> compoundTask =
                              Mockito.mock(CompoundTask.class);

                          Mockito.doAnswer(
                                  AdditionalAnswers
                                      .<CompoundTask<Object>, String, Consumer<Object>>answer(
                                          (id, a) -> {
                                            a.accept(container);
                                            return compoundTask;
                                          }))
                              .when(compoundTask)
                              .add(Mockito.any(), Mockito.any());

                          t.test(container, report);
                          return compoundTask;
                        }))
        .when(tasks)
        .addIfAbsent(Mockito.any(), Mockito.any(), Mockito.any());

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
    Mockito.when(feature.getRepositoryUrl()).thenReturn(REPOSITORY2);
    Mockito.when(featuresService.getState(ID2)).thenReturn(STATE2);
    Mockito.when(featuresService.isRequired(feature2)).thenReturn(REQUIRED2);
    Mockito.when(feature.getStartLevel()).thenReturn(START2);

    Mockito.when(feature3.getId()).thenReturn(ID3);
    Mockito.when(feature3.getName()).thenReturn(NAME3);
    Mockito.when(feature3.getVersion()).thenReturn(VERSION3);
    Mockito.when(feature3.getDescription()).thenReturn(DESCRIPTION3);
    Mockito.when(feature.getRepositoryUrl()).thenReturn(REPOSITORY3);
    Mockito.when(featuresService.getState(ID3)).thenReturn(STATE3);
    Mockito.when(featuresService.isRequired(feature3)).thenReturn(REQUIRED3);
    Mockito.when(feature.getStartLevel()).thenReturn(START3);

    Mockito.when(feature4.getId()).thenReturn(ID4);
    Mockito.when(feature4.getName()).thenReturn(NAME4);
    Mockito.when(feature4.getVersion()).thenReturn(VERSION4);
    Mockito.when(feature4.getDescription()).thenReturn(DESCRIPTION4);
    Mockito.when(feature.getRepositoryUrl()).thenReturn(REPOSITORY4);
    Mockito.when(featuresService.getState(ID4)).thenReturn(STATE4);
    Mockito.when(featuresService.isRequired(feature4)).thenReturn(REQUIRED4);
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
  public void testInstallFeaturesForMultipleRegions() throws Exception {
    Mockito.doNothing().when(featuresService).installFeatures(IDS14, REGION, NO_AUTO_REFRESH);
    Mockito.doNothing().when(featuresService).installFeatures(IDS2, REGION2, NO_AUTO_REFRESH);
    Mockito.doNothing().when(featuresService).installFeatures(IDS3, REGION3, NO_AUTO_REFRESH);

    Assert.assertThat(
        featureProcessor.installFeatures(report, jfeaturesPerRegion), Matchers.equalTo(true));

    Mockito.verify(featuresService).installFeatures(IDS14, REGION, NO_AUTO_REFRESH);
    Mockito.verify(featuresService).installFeatures(IDS2, REGION2, NO_AUTO_REFRESH);
    Mockito.verify(featuresService).installFeatures(IDS3, REGION3, NO_AUTO_REFRESH);
    Mockito.verify(report, Mockito.never()).recordOnFinalAttempt(Mockito.any());
  }

  @Test
  public void testInstallFeaturesForMultipleRegionsWhenFailWithException() throws Exception {
    final Exception e = new Exception();

    Mockito.doNothing().when(featuresService).installFeatures(IDS14, REGION, NO_AUTO_REFRESH);
    Mockito.doThrow(e).when(featuresService).installFeatures(IDS2, REGION2, NO_AUTO_REFRESH);
    Mockito.doNothing().when(featuresService).installFeatures(IDS3, REGION3, NO_AUTO_REFRESH);

    Assert.assertThat(
        featureProcessor.installFeatures(report, jfeaturesPerRegion), Matchers.equalTo(false));

    Mockito.verify(featuresService).installFeatures(IDS14, REGION, NO_AUTO_REFRESH);
    Mockito.verify(featuresService).installFeatures(IDS2, REGION2, NO_AUTO_REFRESH);
    Mockito.verify(featuresService, Mockito.never())
        .installFeatures(IDS3, REGION3, NO_AUTO_REFRESH);
    verifyRecordOnFinalAttempt(IDS2, "install", e);
  }

  @Test
  public void testInstallFeaturesForOneRegion() throws Exception {
    Mockito.doNothing().when(featuresService).installFeatures(IDS14, REGION, NO_AUTO_REFRESH);

    Assert.assertThat(
        featureProcessor.installFeatures(report, REGION, jfeaturesPerRegion.get(REGION)),
        Matchers.equalTo(true));

    Mockito.verify(featuresService).installFeatures(IDS14, REGION, NO_AUTO_REFRESH);
    Mockito.verify(report, Mockito.never()).recordOnFinalAttempt(Mockito.any());
  }

  @Test
  public void testInstallFeaturesForOneRegionWhenFailWithException() throws Exception {
    final Exception e = new Exception();

    Mockito.doThrow(e).when(featuresService).installFeatures(IDS2, REGION2, NO_AUTO_REFRESH);

    Assert.assertThat(
        featureProcessor.installFeatures(report, REGION2, jfeaturesPerRegion.get(REGION2)),
        Matchers.equalTo(false));

    Mockito.verify(featuresService).installFeatures(IDS2, REGION2, NO_AUTO_REFRESH);
    verifyRecordOnFinalAttempt(IDS2, "install", e);
  }

  @Test
  public void testUninstallFeaturesForMultipleRegions() throws Exception {
    Mockito.doNothing().when(featuresService).addRequirements(REQUIREMENTS14, NO_AUTO_REFRESH);
    Mockito.doNothing().when(featuresService).uninstallFeatures(IDS14, REGION, NO_AUTO_REFRESH);
    Mockito.doNothing().when(featuresService).addRequirements(REQUIREMENTS2, NO_AUTO_REFRESH);
    Mockito.doNothing().when(featuresService).uninstallFeatures(IDS2, REGION2, NO_AUTO_REFRESH);
    Mockito.doNothing().when(featuresService).addRequirements(REQUIREMENTS3, NO_AUTO_REFRESH);
    Mockito.doNothing().when(featuresService).uninstallFeatures(IDS3, REGION3, NO_AUTO_REFRESH);

    Assert.assertThat(
        featureProcessor.uninstallFeatures(report, featuresPerRegion), Matchers.equalTo(true));

    Mockito.verify(featuresService).addRequirements(REQUIREMENTS14, NO_AUTO_REFRESH);
    Mockito.verify(featuresService).uninstallFeatures(IDS14, REGION, NO_AUTO_REFRESH);
    Mockito.verify(featuresService).addRequirements(REQUIREMENTS2, NO_AUTO_REFRESH);
    Mockito.verify(featuresService).uninstallFeatures(IDS2, REGION2, NO_AUTO_REFRESH);
    Mockito.verify(featuresService).addRequirements(REQUIREMENTS3, NO_AUTO_REFRESH);
    Mockito.verify(featuresService).uninstallFeatures(IDS3, REGION3, NO_AUTO_REFRESH);
    Mockito.verify(report, Mockito.never()).recordOnFinalAttempt(Mockito.any());
  }

  @Test
  public void testUninstallFeaturesForMultipleRegionsWhenFailWithException() throws Exception {
    final Exception e = new Exception();

    Mockito.doNothing().when(featuresService).addRequirements(REQUIREMENTS14, NO_AUTO_REFRESH);
    Mockito.doNothing().when(featuresService).uninstallFeatures(IDS14, REGION, NO_AUTO_REFRESH);
    Mockito.doNothing().when(featuresService).addRequirements(REQUIREMENTS2, NO_AUTO_REFRESH);
    Mockito.doThrow(e).when(featuresService).uninstallFeatures(IDS2, REGION2, NO_AUTO_REFRESH);
    Mockito.doNothing().when(featuresService).addRequirements(REQUIREMENTS3, NO_AUTO_REFRESH);
    Mockito.doNothing().when(featuresService).uninstallFeatures(IDS3, REGION3, NO_AUTO_REFRESH);

    Assert.assertThat(
        featureProcessor.uninstallFeatures(report, featuresPerRegion), Matchers.equalTo(false));

    Mockito.verify(featuresService).addRequirements(REQUIREMENTS14, NO_AUTO_REFRESH);
    Mockito.verify(featuresService).uninstallFeatures(IDS14, REGION, NO_AUTO_REFRESH);
    Mockito.verify(featuresService).addRequirements(REQUIREMENTS2, NO_AUTO_REFRESH);
    Mockito.verify(featuresService).uninstallFeatures(IDS2, REGION2, NO_AUTO_REFRESH);
    Mockito.verify(featuresService, Mockito.never())
        .addRequirements(REQUIREMENTS3, NO_AUTO_REFRESH);
    Mockito.verify(featuresService, Mockito.never())
        .uninstallFeatures(IDS3, REGION3, NO_AUTO_REFRESH);
    verifyRecordOnFinalAttempt(IDS2, "uninstall", e);
  }

  @Test
  public void testUninstallFeaturesForOneRegion() throws Exception {
    Mockito.doNothing().when(featuresService).addRequirements(REQUIREMENTS14, NO_AUTO_REFRESH);
    Mockito.doNothing().when(featuresService).uninstallFeatures(IDS14, REGION, NO_AUTO_REFRESH);

    Assert.assertThat(
        featureProcessor.uninstallFeatures(report, REGION, featuresPerRegion.get(REGION)),
        Matchers.equalTo(true));

    Mockito.verify(featuresService).addRequirements(REQUIREMENTS14, NO_AUTO_REFRESH);
    Mockito.verify(featuresService).uninstallFeatures(IDS14, REGION, NO_AUTO_REFRESH);
    Mockito.verify(report, Mockito.never()).recordOnFinalAttempt(Mockito.any());
  }

  @Test
  public void testUninstallFeaturesForOneRegionWhenFailWithException() throws Exception {
    final Exception e = new Exception();

    Mockito.doNothing().when(featuresService).addRequirements(REQUIREMENTS2, NO_AUTO_REFRESH);
    Mockito.doThrow(e).when(featuresService).uninstallFeatures(IDS2, REGION2, NO_AUTO_REFRESH);

    Assert.assertThat(
        featureProcessor.uninstallFeatures(report, REGION2, featuresPerRegion.get(REGION2)),
        Matchers.equalTo(false));

    Mockito.verify(featuresService).addRequirements(REQUIREMENTS2, NO_AUTO_REFRESH);
    Mockito.verify(featuresService).uninstallFeatures(IDS2, REGION2, NO_AUTO_REFRESH);
    verifyRecordOnFinalAttempt(IDS2, "uninstall", e);
  }

  @Test
  public void testUpdateFeaturesRequirementsForMultipleRegions() throws Exception {
    Mockito.doNothing().when(featuresService).addRequirements(REQUIREMENTS14, NO_AUTO_REFRESH);
    Mockito.doNothing().when(featuresService).addRequirements(REQUIREMENTS2, NO_AUTO_REFRESH);
    Mockito.doNothing().when(featuresService).removeRequirements(REQUIREMENTS3, NO_AUTO_REFRESH);

    Assert.assertThat(
        featureProcessor.updateFeaturesRequirements(report, jfeaturesPerRegion),
        Matchers.equalTo(true));

    Mockito.verify(featuresService).addRequirements(REQUIREMENTS14, NO_AUTO_REFRESH);
    Mockito.verify(featuresService).addRequirements(REQUIREMENTS2, NO_AUTO_REFRESH);
    Mockito.verify(featuresService).removeRequirements(REQUIREMENTS3, NO_AUTO_REFRESH);
    Mockito.verify(report, Mockito.never()).recordOnFinalAttempt(Mockito.any());
  }

  @Test
  public void testUpdateFeaturesRequirementsForMultipleRegionsWhenFailWithException()
      throws Exception {
    final Exception e = new Exception();

    Mockito.doNothing().when(featuresService).addRequirements(REQUIREMENTS14, NO_AUTO_REFRESH);
    Mockito.doThrow(e).when(featuresService).addRequirements(REQUIREMENTS2, NO_AUTO_REFRESH);
    Mockito.doNothing().when(featuresService).removeRequirements(REQUIREMENTS3, NO_AUTO_REFRESH);

    Assert.assertThat(
        featureProcessor.updateFeaturesRequirements(report, jfeaturesPerRegion),
        Matchers.equalTo(false));

    Mockito.verify(featuresService).addRequirements(REQUIREMENTS14, NO_AUTO_REFRESH);
    Mockito.verify(featuresService).addRequirements(REQUIREMENTS2, NO_AUTO_REFRESH);
    Mockito.verify(featuresService, Mockito.never())
        .removeRequirements(REQUIREMENTS3, NO_AUTO_REFRESH);
    verifyRecordOnFinalAttempt(IDS2, "update", e);
  }

  @Test
  public void testUpdateFeaturesRequirementsForOneRegionAndRequired() throws Exception {
    Mockito.doNothing().when(featuresService).addRequirements(REQUIREMENTS14, NO_AUTO_REFRESH);

    Assert.assertThat(
        featureProcessor.updateFeaturesRequirements(report, REGION, jfeaturesPerRegion.get(REGION)),
        Matchers.equalTo(true));

    Mockito.verify(featuresService).addRequirements(REQUIREMENTS14, NO_AUTO_REFRESH);
    Mockito.verify(report, Mockito.never()).recordOnFinalAttempt(Mockito.any());
  }

  @Test
  public void testUpdateFeaturesRequirementsForOneRegionAndNotRequired() throws Exception {
    Mockito.doNothing().when(featuresService).addRequirements(REQUIREMENTS3, NO_AUTO_REFRESH);

    Assert.assertThat(
        featureProcessor.updateFeaturesRequirements(
            report, REGION3, jfeaturesPerRegion.get(REGION3)),
        Matchers.equalTo(true));

    Mockito.verify(featuresService).removeRequirements(REQUIREMENTS3, NO_AUTO_REFRESH);
    Mockito.verify(report, Mockito.never()).recordOnFinalAttempt(Mockito.any());
  }

  @Test
  public void testUpdateFeaturesRequirementsForOneRegionAndRequiredWhenFailWithException()
      throws Exception {
    final Exception e = new Exception();

    Mockito.doThrow(e).when(featuresService).addRequirements(REQUIREMENTS2, NO_AUTO_REFRESH);

    Assert.assertThat(
        featureProcessor.updateFeaturesRequirements(
            report, REGION2, jfeaturesPerRegion.get(REGION2)),
        Matchers.equalTo(false));

    Mockito.verify(featuresService).addRequirements(REQUIREMENTS2, NO_AUTO_REFRESH);
    verifyRecordOnFinalAttempt(IDS2, "update", e);
  }

  @Test
  public void testUpdateFeaturesRequirementsForOneRegionAndNotRequiredWhenFailWithException()
      throws Exception {
    final Exception e = new Exception();

    Mockito.doThrow(e).when(featuresService).removeRequirements(REQUIREMENTS3, NO_AUTO_REFRESH);

    Assert.assertThat(
        featureProcessor.updateFeaturesRequirements(
            report, REGION3, jfeaturesPerRegion.get(REGION3)),
        Matchers.equalTo(false));

    Mockito.verify(featuresService).removeRequirements(REQUIREMENTS3, NO_AUTO_REFRESH);
    verifyRecordOnFinalAttempt(IDS3, "update", e);
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
  public void testProcessMissingFeatureAndPopulateTaskList() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).installFeatures(report, jfeaturePerRegion);

    featureProcessor.processMissingFeatureAndPopulateTaskList(jfeature, tasks);

    verifyTaskListAndExecution(jfeature, feature, 1, 0, 0, 0, 0);
  }

  @Test
  public void testProcessMissingFeatureAndPopulateTaskListWhenItWasUninstalled() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).installFeatures(report, jfeature2PerRegion);

    featureProcessor.processMissingFeatureAndPopulateTaskList(jfeature2, tasks);

    verifyTaskListAndExecution(jfeature2, feature2, 0, 0, 0, 0, 0);
  }

  @Test
  public void testProcessInstalledFeatureAndPopulateTaskListWhenUninstalled() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).installFeatures(report, jfeature2PerRegion);

    featureProcessor.processInstalledFeatureAndPopulateTaskList(jfeature2, feature2, tasks);

    verifyTaskListAndExecution(jfeature2, feature2, 1, 0, 0, 0, 0);
  }

  @Test
  public void testProcessInstalledFeatureAndPopulateTaskListWhenInstalledWithSameRequirements()
      throws Exception {
    featureProcessor.processInstalledFeatureAndPopulateTaskList(jfeature3, feature3, tasks);

    verifyTaskListAndExecution(jfeature3, feature3, 0, 0, 0, 0, 0);
  }

  @Test
  public void testProcessInstalledFeatureAndPopulateTaskListWhenInstalledWithDifferentRequirements()
      throws Exception {
    final JsonFeature jfeature3 =
        new JsonFeature(
            NAME3, ID3, VERSION3, null, STATE3, !REQUIRED3, REGION3, REPOSITORY3, START3);

    featureProcessor.processInstalledFeatureAndPopulateTaskList(jfeature3, feature3, tasks);

    verifyTaskListAndExecution(jfeature3, feature3, 0, 0, 0, 0, 1);
  }

  @Test
  public void testProcessInstalledFeatureAndPopulateTaskListWhenStartedWithSameRequirements()
      throws Exception {
    Mockito.doReturn(true).when(featureProcessor).stopFeature(report, feature4, REGION);

    featureProcessor.processInstalledFeatureAndPopulateTaskList(jfeature4, feature4, tasks);

    verifyTaskListAndExecution(jfeature4, feature4, 0, 0, 0, 1, 0);
  }

  @Test
  public void testProcessInstalledFeatureAndPopulateTaskListWhenStartedWithDifferentRequirements()
      throws Exception {
    final JsonFeature jfeature4 =
        new JsonFeature(
            NAME4, ID4, VERSION4, null, STATE4, !REQUIRED4, REGION, REPOSITORY4, START4);

    Mockito.doReturn(true).when(featureProcessor).stopFeature(report, feature4, REGION);

    featureProcessor.processInstalledFeatureAndPopulateTaskList(jfeature4, feature4, tasks);

    verifyTaskListAndExecution(jfeature4, feature4, 0, 0, 0, 1, 1);
  }

  @Test
  public void testProcessInstalledFeatureAndPopulateTaskListWhenResolvedWithSameRequirements()
      throws Exception {
    featureProcessor.processInstalledFeatureAndPopulateTaskList(jfeature, feature, tasks);

    verifyTaskListAndExecution(jfeature, feature, 0, 0, 0, 0, 0);
  }

  @Test
  public void testProcessInstalledFeatureAndPopulateTaskListWhenResolvedWithDifferentRequirements()
      throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, VERSION, null, STATE, !REQUIRED, REGION, REPOSITORY, START);

    featureProcessor.processInstalledFeatureAndPopulateTaskList(jfeature, feature, tasks);

    verifyTaskListAndExecution(jfeature, feature, 0, 0, 0, 0, 1);
  }

  @Test
  public void testProcessResolvedFeatureAndPopulateTaskListWhenUninstalled() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).installFeatures(report, jfeature2PerRegion);

    featureProcessor.processResolvedFeatureAndPopulateTaskList(jfeature2, feature2, tasks);

    verifyTaskListAndExecution(jfeature2, feature2, 1, 0, 0, 0, 0);
  }

  @Test
  public void testProcessResolvedFeatureAndPopulateTaskListWhenInstalledWithSameRequirements()
      throws Exception {
    Mockito.doReturn(true).when(featureProcessor).stopFeature(report, feature3, REGION);

    featureProcessor.processResolvedFeatureAndPopulateTaskList(jfeature3, feature3, tasks);

    verifyTaskListAndExecution(jfeature3, feature3, 0, 0, 0, 1, 0);
  }

  @Test
  public void testProcessResolvedFeatureAndPopulateTaskListWhenInstalledWithDifferentRequirements()
      throws Exception {
    final JsonFeature jfeature3 =
        new JsonFeature(
            NAME3, ID3, VERSION3, null, STATE3, !REQUIRED3, REGION3, REPOSITORY3, START3);

    Mockito.doReturn(true).when(featureProcessor).stopFeature(report, feature3, REGION);

    featureProcessor.processResolvedFeatureAndPopulateTaskList(jfeature3, feature3, tasks);

    verifyTaskListAndExecution(jfeature3, feature3, 0, 0, 0, 1, 1);
  }

  @Test
  public void testProcessResolvedFeatureAndPopulateTaskListWhenResolvedWithSameRequirements()
      throws Exception {
    Mockito.doReturn(FeatureState.Resolved).when(featuresService).getState(ID);

    featureProcessor.processResolvedFeatureAndPopulateTaskList(jfeature, feature, tasks);

    verifyTaskListAndExecution(jfeature, feature, 0, 0, 0, 0, 0);
  }

  @Test
  public void testProcessResolvedFeatureAndPopulateTaskListWhenResolvedWithDifferentRequirements()
      throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, VERSION, null, STATE, !REQUIRED, REGION, REPOSITORY, START);

    Mockito.doReturn(FeatureState.Resolved).when(featuresService).getState(ID);

    featureProcessor.processResolvedFeatureAndPopulateTaskList(jfeature, feature, tasks);

    verifyTaskListAndExecution(jfeature, feature, 0, 0, 0, 0, 1);
  }

  @Test
  public void testProcessResolvedFeatureAndPopulateTaskListWhenStartedWithSameRequirements()
      throws Exception {
    Mockito.doReturn(true).when(featureProcessor).stopFeature(report, feature4, REGION);

    featureProcessor.processResolvedFeatureAndPopulateTaskList(jfeature4, feature4, tasks);

    verifyTaskListAndExecution(jfeature4, feature4, 0, 0, 0, 1, 0);
  }

  @Test
  public void testProcessResolvedFeatureAndPopulateTaskListWhenStartedWithDifferentRequirements()
      throws Exception {
    final JsonFeature jfeature4 =
        new JsonFeature(
            NAME4, ID4, VERSION4, null, STATE4, !REQUIRED4, REGION, REPOSITORY4, START4);

    Mockito.doReturn(true).when(featureProcessor).stopFeature(report, feature4, REGION);

    featureProcessor.processResolvedFeatureAndPopulateTaskList(jfeature4, feature4, tasks);

    verifyTaskListAndExecution(jfeature4, feature4, 0, 0, 0, 1, 1);
  }

  @Test
  public void testProcessUninstalledFeatureAndPopulateTaskListWhenUninstalled() throws Exception {
    Mockito.doReturn(FeatureState.Uninstalled).when(featuresService).getState(ID2);

    featureProcessor.processUninstalledFeatureAndPopulateTaskList(feature2, tasks);

    verifyTaskListAndExecution(jfeature2, feature2, 0, 0, 0, 0, 0);
  }

  @Test
  public void testProcessUninstalledFeatureAndPopulateTaskListWhenInstalled() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).uninstallFeatures(report, feature3PerRegion);

    featureProcessor.processUninstalledFeatureAndPopulateTaskList(feature3, tasks);

    verifyTaskListAndExecution(jfeature3, feature3, 0, 1, 0, 0, 0);
  }

  @Test
  public void testProcessUninstalledFeatureAndPopulateTaskListWhenResolved() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).uninstallFeatures(report, featurePerRegion);

    featureProcessor.processUninstalledFeatureAndPopulateTaskList(feature, tasks);

    verifyTaskListAndExecution(jfeature, feature, 0, 1, 0, 0, 0);
  }

  @Test
  public void testProcessUninstalledFeatureAndPopulateTaskListWhenStarted() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).uninstallFeatures(report, feature4PerRegion);

    featureProcessor.processUninstalledFeatureAndPopulateTaskList(feature4, tasks);

    verifyTaskListAndExecution(jfeature4, feature4, 0, 1, 0, 0, 0);
  }

  @Test
  public void testProcessStartedFeatureAndPopulateTaskListWhenUninstalled() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).installFeatures(report, jfeature2PerRegion);

    featureProcessor.processStartedFeatureAndPopulateTaskList(jfeature2, feature2, tasks);

    verifyTaskListAndExecution(jfeature2, feature2, 1, 0, 0, 0, 0);
  }

  @Test
  public void testProcessStartedFeatureAndPopulateTaskListWhenInstalledWithSameRequirements()
      throws Exception {
    Mockito.doReturn(true).when(featureProcessor).startFeature(report, feature3, REGION);

    featureProcessor.processStartedFeatureAndPopulateTaskList(jfeature3, feature3, tasks);

    verifyTaskListAndExecution(jfeature3, feature3, 0, 0, 1, 0, 0);
  }

  @Test
  public void testProcessStartedFeatureAndPopulateTaskListWhenInstalledWithDifferentRequirements()
      throws Exception {
    final JsonFeature jfeature3 =
        new JsonFeature(
            NAME3, ID3, VERSION3, null, STATE3, !REQUIRED3, REGION3, REPOSITORY3, START3);

    Mockito.doReturn(true).when(featureProcessor).startFeature(report, feature3, REGION);

    featureProcessor.processStartedFeatureAndPopulateTaskList(jfeature3, feature3, tasks);

    verifyTaskListAndExecution(jfeature3, feature3, 0, 0, 1, 0, 1);
  }

  @Test
  public void testProcessStartedFeatureAndPopulateTaskListWhenResolvedWithSameRequirements()
      throws Exception {
    Mockito.doReturn(true).when(featureProcessor).startFeature(report, feature, REGION);

    featureProcessor.processStartedFeatureAndPopulateTaskList(jfeature, feature, tasks);

    verifyTaskListAndExecution(jfeature, feature, 0, 0, 1, 0, 0);
  }

  @Test
  public void testProcessStartedFeatureAndPopulateTaskListWhenResolvedWithDifferentRequirements()
      throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, VERSION, null, STATE, !REQUIRED, REGION, REPOSITORY, START);

    Mockito.doReturn(true).when(featureProcessor).startFeature(report, feature, REGION);

    featureProcessor.processStartedFeatureAndPopulateTaskList(jfeature, feature, tasks);

    verifyTaskListAndExecution(jfeature, feature, 0, 0, 1, 0, 1);
  }

  @Test
  public void testProcessFeatureAndPopulateTaskListWhenItIsMissing() throws Exception {
    Mockito.doReturn(true).when(featureProcessor).installFeatures(report, jfeaturePerRegion);

    featureProcessor.processFeatureAndPopulateTaskList(jfeature, null, tasks);

    verifyTaskListAndExecution(jfeature, feature, 1, 0, 0, 0, 0);
  }

  @Test
  public void testProcessFeatureWhenItWasInstalledAndItIsUninstalled() throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(
            NAME2,
            ID2,
            VERSION2,
            null,
            FeatureState.Installed,
            REQUIRED2,
            REGION2,
            REPOSITORY2,
            START2);

    Mockito.doReturn(true)
        .when(featureProcessor)
        .installFeatures(report, ImmutableMap.of(REGION2, ImmutableSet.of(jfeature2)));

    featureProcessor.processFeatureAndPopulateTaskList(jfeature2, feature2, tasks);

    verifyTaskListAndExecution(jfeature2, feature2, 1, 0, 0, 0, 0);
  }

  @Test
  public void testProcessFeatureAndPopulateTaskListWhenItWasInstalledAndItIsInstalled()
      throws Exception {
    final JsonFeature jfeature3 =
        new JsonFeature(
            NAME3,
            ID3,
            VERSION3,
            null,
            FeatureState.Installed,
            REQUIRED3,
            REGION3,
            REPOSITORY3,
            START3);

    featureProcessor.processFeatureAndPopulateTaskList(jfeature3, feature3, tasks);

    verifyTaskListAndExecution(jfeature3, feature3, 0, 0, 0, 0, 0);
  }

  @Test
  public void testProcessFeatureAndPopulateTaskListWhenItWasInstalledAndItIsStarted()
      throws Exception {
    final JsonFeature jfeature4 =
        new JsonFeature(
            NAME4,
            ID4,
            VERSION4,
            null,
            FeatureState.Installed,
            REQUIRED4,
            REGION,
            REPOSITORY4,
            START4);

    Mockito.doReturn(true).when(featureProcessor).stopFeature(report, feature4, REGION);

    featureProcessor.processFeatureAndPopulateTaskList(jfeature4, feature4, tasks);

    verifyTaskListAndExecution(jfeature4, feature4, 0, 0, 0, 1, 0);
  }

  @Test
  public void testProcessFeatureAndPopulateTaskListWhenItWasInstalledAndItIsResolved()
      throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(
            NAME, ID, VERSION, null, FeatureState.Installed, REQUIRED, REGION, REPOSITORY, START);

    Mockito.doReturn(true).when(featureProcessor).stopFeature(report, feature, REGION);

    featureProcessor.processFeatureAndPopulateTaskList(jfeature, feature, tasks);

    verifyTaskListAndExecution(jfeature, feature, 0, 0, 0, 0, 0);
  }

  @Test
  public void testProcessFeatureAndPopulateTaskListWhenItWasStartedAndItIsUninstalled()
      throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(
            NAME2,
            ID2,
            VERSION2,
            null,
            FeatureState.Started,
            REQUIRED2,
            REGION2,
            REPOSITORY2,
            START2);

    Mockito.doReturn(true)
        .when(featureProcessor)
        .installFeatures(report, ImmutableMap.of(REGION2, ImmutableSet.of(jfeature2)));

    featureProcessor.processFeatureAndPopulateTaskList(jfeature2, feature2, tasks);

    verifyTaskListAndExecution(jfeature2, feature2, 1, 0, 0, 0, 0);
  }

  @Test
  public void testProcessFeatureAndPopulateTaskListWhenItWasStartedAndItIsInstalled()
      throws Exception {
    final JsonFeature jfeature3 =
        new JsonFeature(
            NAME3,
            ID3,
            VERSION3,
            null,
            FeatureState.Started,
            REQUIRED3,
            REGION3,
            REPOSITORY3,
            START3);

    Mockito.doReturn(true).when(featureProcessor).startFeature(report, feature3, REGION3);

    featureProcessor.processFeatureAndPopulateTaskList(jfeature3, feature3, tasks);

    verifyTaskListAndExecution(jfeature3, feature3, 0, 0, 1, 0, 0);
  }

  @Test
  public void testProcessFeatureAndPopulateTaskListWhenItWasStartedAndItIsStarted()
      throws Exception {
    final JsonFeature jfeature4 =
        new JsonFeature(
            NAME4,
            ID4,
            VERSION4,
            null,
            FeatureState.Started,
            REQUIRED4,
            REGION,
            REPOSITORY4,
            START4);

    featureProcessor.processFeatureAndPopulateTaskList(jfeature4, feature4, tasks);

    verifyTaskListAndExecution(jfeature4, feature4, 0, 0, 0, 0, 0);
  }

  @Test
  public void testProcessFeatureAndPopulateTaskListWhenItWasStartedAndItIsResolved()
      throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(
            NAME, ID, VERSION, null, FeatureState.Started, REQUIRED, REGION, REPOSITORY, START);

    Mockito.doReturn(true).when(featureProcessor).startFeature(report, feature, REGION);

    featureProcessor.processFeatureAndPopulateTaskList(jfeature, feature, tasks);

    verifyTaskListAndExecution(jfeature, feature, 0, 0, 1, 0, 0);
  }

  @Test
  public void testProcessFeatureAndPopulateTaskListWhenItWasUninstalledAndItIsUninstalled()
      throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(
            NAME2,
            ID2,
            VERSION2,
            null,
            FeatureState.Uninstalled,
            REQUIRED2,
            REGION2,
            REPOSITORY2,
            START2);

    featureProcessor.processFeatureAndPopulateTaskList(jfeature2, feature2, tasks);

    verifyTaskListAndExecution(jfeature2, feature2, 0, 0, 0, 0, 0);
  }

  @Test
  public void testProcessFeatureAndPopulateTaskListWhenItWasUninstalledAndItIsInstalled()
      throws Exception {
    final JsonFeature jfeature3 =
        new JsonFeature(
            NAME3,
            ID3,
            VERSION3,
            null,
            FeatureState.Uninstalled,
            REQUIRED3,
            REGION3,
            REPOSITORY3,
            START3);

    Mockito.doReturn(true).when(featureProcessor).uninstallFeatures(report, feature3PerRegion);

    featureProcessor.processFeatureAndPopulateTaskList(jfeature3, feature3, tasks);

    verifyTaskListAndExecution(jfeature3, feature3, 0, 1, 0, 0, 0);
  }

  @Test
  public void testProcessFeatureAndPopulateTaskListWhenItWasUninstalledAndItIsStarted()
      throws Exception {
    final JsonFeature jfeature4 =
        new JsonFeature(
            NAME4,
            ID4,
            VERSION4,
            null,
            FeatureState.Uninstalled,
            REQUIRED4,
            REGION,
            REPOSITORY4,
            START4);

    Mockito.doReturn(true).when(featureProcessor).uninstallFeatures(report, feature4PerRegion);

    featureProcessor.processFeatureAndPopulateTaskList(jfeature4, feature4, tasks);

    verifyTaskListAndExecution(jfeature4, feature4, 0, 1, 0, 0, 0);
  }

  @Test
  public void testProcessFeatureAndPopulateTaskListWhenItWasUninstalledAndItIsResolved()
      throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(
            NAME, ID, VERSION, null, FeatureState.Uninstalled, REQUIRED, REGION, REPOSITORY, START);

    Mockito.doReturn(true).when(featureProcessor).uninstallFeatures(report, featurePerRegion);

    featureProcessor.processFeatureAndPopulateTaskList(jfeature, feature, tasks);

    verifyTaskListAndExecution(jfeature, feature, 0, 1, 0, 0, 0);
  }

  @Test
  public void testProcessFeatureAndPopulateTaskListWhenItWasResolvedAndItIsUninstalled()
      throws Exception {
    final JsonFeature jfeature2 =
        new JsonFeature(
            NAME2,
            ID2,
            VERSION2,
            null,
            FeatureState.Resolved,
            REQUIRED2,
            REGION2,
            REPOSITORY2,
            START2);

    featureProcessor.processFeatureAndPopulateTaskList(jfeature2, feature2, tasks);

    verifyTaskListAndExecution(jfeature2, feature2, 1, 0, 0, 0, 0);
  }

  @Test
  public void testProcessFeatureAndPopulateTaskListWhenItWasResolvedAndItIsInstalled()
      throws Exception {
    final JsonFeature jfeature3 =
        new JsonFeature(
            NAME3,
            ID3,
            VERSION3,
            null,
            FeatureState.Resolved,
            REQUIRED3,
            REGION3,
            REPOSITORY3,
            START3);

    Mockito.doReturn(true).when(featureProcessor).stopFeature(report, feature3, REGION3);

    featureProcessor.processFeatureAndPopulateTaskList(jfeature3, feature3, tasks);

    verifyTaskListAndExecution(jfeature3, feature3, 0, 0, 0, 1, 0);
  }

  @Test
  public void testProcessFeatureAndPopulateTaskListWhenItWasResolvedAndItIsStarted()
      throws Exception {
    final JsonFeature jfeature4 =
        new JsonFeature(
            NAME4,
            ID4,
            VERSION4,
            null,
            FeatureState.Resolved,
            REQUIRED4,
            REGION,
            REPOSITORY4,
            START4);

    Mockito.doReturn(true).when(featureProcessor).stopFeature(report, feature4, REGION);

    featureProcessor.processFeatureAndPopulateTaskList(jfeature4, feature4, tasks);

    verifyTaskListAndExecution(jfeature4, feature4, 0, 0, 0, 1, 0);
  }

  @Test
  public void testProcessFeatureAndPopulateTaskListWhenItWasResolvedAndItIsResolved()
      throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(
            NAME, ID, VERSION, null, FeatureState.Resolved, REQUIRED, REGION, REPOSITORY, START);

    Mockito.doReturn(true).when(featureProcessor).uninstallFeatures(report, featurePerRegion);

    featureProcessor.processFeatureAndPopulateTaskList(jfeature, feature, tasks);

    verifyTaskListAndExecution(jfeature, feature, 0, 0, 0, 0, 0);
  }

  @Test
  public void testProcessLeftoverFeaturesAndPopulateTaskList() throws Exception {
    final Map<String, Feature> features =
        ImmutableMap.of(ID, feature, ID2, feature2, ID3, feature3, ID4, feature4);

    Mockito.doReturn(true)
        .when(featureProcessor)
        .processUninstalledFeatureAndPopulateTaskList(feature, tasks);
    Mockito.doReturn(true)
        .when(featureProcessor)
        .processUninstalledFeatureAndPopulateTaskList(feature2, tasks);
    Mockito.doReturn(false)
        .when(featureProcessor)
        .processUninstalledFeatureAndPopulateTaskList(feature3, tasks);
    Mockito.doReturn(true)
        .when(featureProcessor)
        .processUninstalledFeatureAndPopulateTaskList(feature4, tasks);

    featureProcessor.processLeftoverFeaturesAndPopulateTaskList(features, tasks);

    Mockito.verify(featureProcessor).processUninstalledFeatureAndPopulateTaskList(feature, tasks);
    Mockito.verify(featureProcessor).processUninstalledFeatureAndPopulateTaskList(feature2, tasks);
    Mockito.verify(featureProcessor).processUninstalledFeatureAndPopulateTaskList(feature3, tasks);
    Mockito.verify(featureProcessor).processUninstalledFeatureAndPopulateTaskList(feature4, tasks);
  }

  @Test
  public void testProcessExportedFeaturesAndPopulateTaskListWhenFoundInMemory() throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, VERSION, null, STATE, REQUIRED, REGION, REPOSITORY, START);
    final Map<String, Feature> features = new HashMap<>();

    features.put(ID, feature);

    Mockito.doReturn(Stream.of(jfeature)).when(jprofile).features();
    Mockito.doNothing()
        .when(featureProcessor)
        .processFeatureAndPopulateTaskList(jfeature, feature, tasks);

    featureProcessor.processExportedFeaturesAndPopulateTaskList(jprofile, features, tasks);

    Assert.assertThat(features.isEmpty(), Matchers.equalTo(true));

    Mockito.verify(featureProcessor).processFeatureAndPopulateTaskList(jfeature, feature, tasks);
  }

  @Test
  public void testProcessExportedFeaturesAndPopulateTaskListWhenNotFoundInMemory()
      throws Exception {
    final JsonFeature jfeature =
        new JsonFeature(NAME, ID, VERSION, null, STATE, REQUIRED, REGION, REPOSITORY, START);
    final Map<String, Feature> features = new HashMap<>();

    features.put(ID2, feature2);

    Mockito.doReturn(Stream.of(jfeature)).when(jprofile).features();
    Mockito.doNothing()
        .when(featureProcessor)
        .processFeatureAndPopulateTaskList(jfeature, null, tasks);

    featureProcessor.processExportedFeaturesAndPopulateTaskList(jprofile, features, tasks);

    Assert.assertThat(features.size(), Matchers.equalTo(1));

    Mockito.verify(featureProcessor).processFeatureAndPopulateTaskList(jfeature, null, tasks);
  }

  @Test
  public void testProcessFeaturesAndPopulateTaskList() throws Exception {
    // because the processExportedFeaturesAndPopulateTaskList has a side effect of modifying the
    // provided
    // map and because Mockito ArgumentCapture only captures references to arguments and not a clone
    // of them we are forced to clone the features received in the
    // processExportedFeaturesAndPopulateTaskList() in the doAnswer() so we can verify that exact
    // content
    // at the end
    final AtomicReference<Map<String, Feature>> featuresAtExported = new AtomicReference<>();

    Mockito.doReturn(new Feature[] {feature4, feature, feature2})
        .when(featureProcessor)
        .listFeatures(Mockito.notNull());
    Mockito.doAnswer(
            AdditionalAnswers.<JsonProfile, Map<String, Feature>, TaskList>answerVoid(
                (jprofile, featuresMap, tasks) -> {
                  // capture a clone of the args for later verification
                  featuresAtExported.set(new LinkedHashMap<>(featuresMap));
                  // simulate removing entries from the map
                  featuresMap.remove(ID);
                  featuresMap.remove(ID2);
                }))
        .when(featureProcessor)
        .processExportedFeaturesAndPopulateTaskList(
            Mockito.same(jprofile), Mockito.notNull(), Mockito.same(tasks));
    Mockito.doNothing()
        .when(featureProcessor)
        .processLeftoverFeaturesAndPopulateTaskList(Mockito.notNull(), Mockito.same(tasks));

    featureProcessor.processFeaturesAndPopulateTaskList(jprofile, tasks);

    final ArgumentCaptor<Map<String, Feature>> featuresAtLeftover =
        ArgumentCaptor.forClass(Map.class);

    Mockito.verify(featureProcessor)
        .processExportedFeaturesAndPopulateTaskList(
            Mockito.same(jprofile), Mockito.notNull(), Mockito.same(tasks));
    Mockito.verify(featureProcessor)
        .processLeftoverFeaturesAndPopulateTaskList(
            featuresAtLeftover.capture(), Mockito.same(tasks));

    Assert.assertThat(
        featuresAtExported.get(),
        Matchers.allOf(
            Matchers.aMapWithSize(3),
            Matchers.hasEntry(ID, feature),
            Matchers.hasEntry(ID2, feature2),
            Matchers.hasEntry(ID4, feature4)));
    Assert.assertThat(
        featuresAtLeftover.getValue(),
        Matchers.allOf(Matchers.aMapWithSize(1), Matchers.hasEntry(ID4, feature4)));
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

  private void verifyRecordOnFinalAttempt(Object ids, String operation, Exception e) {
    final ArgumentCaptor<MigrationException> captor =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(report).recordOnFinalAttempt(captor.capture());

    final MigrationException me = captor.getValue();

    if (ids instanceof Set) {
      Assert.assertThat(
          me.getMessage(), Matchers.containsString("failed to " + operation + " features"));
    } else {
      Assert.assertThat(
          me.getMessage(),
          Matchers.containsString("failed to " + operation + " feature [" + ids + "]"));
    }
    Assert.assertThat(me.getCause(), Matchers.sameInstance(e));
  }

  private void verifyTaskListAndExecution(
      JsonFeature jfeature,
      Feature feature,
      int installCount,
      int uninstallCount,
      int startCount,
      int stopCount,
      int updateCount) {
    Mockito.verify(featureProcessor, Mockito.times(installCount))
        .installFeatures(report, ImmutableMap.of(jfeature.getRegion(), ImmutableSet.of(jfeature)));
    Mockito.verify(tasks, Mockito.times(installCount))
        .addIfAbsent(
            Mockito.eq(Operation.INSTALL),
            // Mockito.eq(jfeature.getId()),
            Mockito.notNull(),
            Mockito.notNull());
    // should be using the region from feature but we don't know how to get it yet
    Mockito.verify(featureProcessor, Mockito.times(uninstallCount))
        .uninstallFeatures(
            report, ImmutableMap.of(FeaturesService.ROOT_REGION, ImmutableSet.of(feature)));
    Mockito.verify(tasks, Mockito.times(uninstallCount))
        .addIfAbsent(
            Mockito.eq(Operation.UNINSTALL),
            // Mockito.eq(feature.getId()),
            Mockito.notNull(),
            Mockito.notNull());
    Mockito.verify(featureProcessor, Mockito.times(startCount))
        .startFeature(report, feature, jfeature.getRegion());
    Mockito.verify(tasks, Mockito.times(startCount))
        .add(Mockito.eq(Operation.START), Mockito.eq(jfeature.getId()), Mockito.notNull());
    Mockito.verify(featureProcessor, Mockito.times(stopCount))
        .stopFeature(report, feature, jfeature.getRegion());
    Mockito.verify(tasks, Mockito.times(stopCount))
        .add(Mockito.eq(Operation.STOP), Mockito.eq(jfeature.getId()), Mockito.notNull());
    Mockito.verify(featureProcessor, Mockito.times(updateCount))
        .updateFeaturesRequirements(
            report, ImmutableMap.of(jfeature.getRegion(), ImmutableSet.of(jfeature)));
    Mockito.verify(tasks, Mockito.times(updateCount))
        .addIfAbsent(
            Mockito.eq(Operation.UPDATE),
            // Mockito.eq(jfeature.getId()),
            Mockito.notNull(),
            Mockito.notNull());
  }

  public static <T> Set<T> toLinkedSet(T... values) {
    final LinkedHashSet<T> set = new LinkedHashSet<>();

    for (T t : values) {
      set.add(t);
    }
    return set;
  }
}
