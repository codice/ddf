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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.karaf.features.FeatureState;
import org.codice.ddf.migration.ExportMigrationContext;
import org.codice.ddf.migration.ExportMigrationEntry;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.util.function.BiThrowingConsumer;
import org.hamcrest.Matchers;
import org.hamcrest.junit.internal.ThrowableMessageMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.skyscreamer.jsonassert.JSONAssert;

public class ProfileMigratableTest {
  private static final Path PROFILE_PATH = Paths.get("profile.json");

  private static final String FEATURE_NAME = "feature.test.name";

  private static final String FEATURE_ID = "feature.test.id";

  private static final FeatureState FEATURE_STATE = FeatureState.Installed;

  private static final boolean FEATURE_REQUIRED = true;
  private static final int FEATURE_START = 57;

  private static final String FEATURE_REGION = "feature.test.region";

  private static final String FEATURE_REPOSITORY = "feature.test.repo";

  private static final long BUNDLE_ID = 14235L;
  private static final String BUNDLE_NAME = "bundle.test.name";
  private static final Version BUNDLE_VERSION = new Version(1, 2, 3, "bundle");
  private static final int BUNDLE_STATE = Bundle.STARTING;
  private static final String BUNDLE_LOCATION = "bundle.test.location";

  private static final JsonFeature JFEATURE =
      new JsonFeature(
          FEATURE_NAME,
          FEATURE_ID,
          null,
          null,
          FEATURE_STATE,
          FEATURE_REQUIRED,
          FEATURE_REGION,
          FEATURE_REPOSITORY,
          FEATURE_START);

  private static final JsonBundle JBUNDLE =
      new JsonBundle(BUNDLE_NAME, BUNDLE_VERSION, BUNDLE_ID, BUNDLE_STATE, BUNDLE_LOCATION);

  private static final String JSON_PROFILE_STR =
      JsonUtils.toJson(new JsonProfile(JFEATURE, JBUNDLE));

  private static final String JSON_PROFILE_FROM_MAP =
      JsonUtils.toJson(
          ImmutableMap.of(
              "features",
                  ImmutableList.of(
                      ImmutableMap.builder()
                          .put("name", FEATURE_NAME)
                          .put("id", FEATURE_ID)
                          .put("state", FEATURE_STATE)
                          .put("required", FEATURE_REQUIRED)
                          .put("region", FEATURE_REGION)
                          .put("repository", FEATURE_REPOSITORY)
                          .put("startLevel", FEATURE_START)
                          .build()),
              "bundles",
                  ImmutableList.of(
                      ImmutableMap.of(
                          "name", BUNDLE_NAME,
                          "id", BUNDLE_ID,
                          "version", BUNDLE_VERSION,
                          "state", BUNDLE_STATE,
                          "location", BUNDLE_LOCATION))));

  private final FeatureMigrator featureMigrator = Mockito.mock(FeatureMigrator.class);
  private final BundleMigrator bundleMigrator = Mockito.mock(BundleMigrator.class);
  private final MigrationReport report = Mockito.mock(MigrationReport.class);

  private final ProfileMigratable migratable =
      new ProfileMigratable(featureMigrator, bundleMigrator);
  private final ImportMigrationContext context = Mockito.mock(ImportMigrationContext.class);
  private final ImportMigrationEntry entry = Mockito.mock(ImportMigrationEntry.class);

  private final AtomicInteger importFeaturesAttempts = new AtomicInteger();
  private final AtomicInteger importBundlesAttempts = new AtomicInteger();

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    Mockito.doReturn(entry).when(context).getEntry(PROFILE_PATH);
    Mockito.doAnswer(callWithJson(report))
        .when(entry)
        .restore(
            Mockito
                .<BiThrowingConsumer<MigrationReport, Optional<InputStream>, IOException>>
                    notNull());
  }

  @Test
  public void testConstructorWithNullFeatureMigrator() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null feature migrator"));

    new ProfileMigratable(null, bundleMigrator);
  }

  @Test
  public void testConstructorWithNullBundleMigrator() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null bundle migrator"));

    new ProfileMigratable(featureMigrator, null);
  }

  @Test
  public void testGetVersion() throws Exception {
    Assert.assertThat(migratable.getVersion(), Matchers.notNullValue());
  }

  @Test
  public void testGetId() throws Exception {
    Assert.assertThat(migratable.getId(), Matchers.equalTo("ddf.profile"));
  }

  @Test
  public void testGetTitle() throws Exception {
    Assert.assertThat(migratable.getTitle(), Matchers.notNullValue());
  }

  @Test
  public void testGetDescription() throws Exception {
    Assert.assertThat(migratable.getDescription(), Matchers.notNullValue());
  }

  @Test
  public void testGetOrganization() throws Exception {
    Assert.assertThat(migratable.getOrganization(), Matchers.notNullValue());
  }

  @Test
  public void testDoExport() throws Exception {
    final ExportMigrationContext context = Mockito.mock(ExportMigrationContext.class);
    final ExportMigrationEntry entry = Mockito.mock(ExportMigrationEntry.class);
    final StringWriter sw = new StringWriter();

    Mockito.doReturn(entry).when(context).getEntry(PROFILE_PATH);
    Mockito.doReturn(Collections.singletonList(JFEATURE)).when(featureMigrator).exportFeatures();
    Mockito.doReturn(Collections.singletonList(JBUNDLE)).when(bundleMigrator).exportBundles();
    Mockito.doAnswer(
            AdditionalAnswers
                .<Boolean, BiThrowingConsumer<MigrationReport, OutputStream, IOException>>answer(
                    c -> { // callback the consumer
                      c.accept(
                          report, new WriterOutputStream(sw, Charset.defaultCharset(), 1024, true));
                      return true;
                    }))
        .when(entry)
        .store(Mockito.<BiThrowingConsumer<MigrationReport, OutputStream, IOException>>notNull());

    migratable.doExport(context);

    JSONAssert.assertEquals(JSON_PROFILE_FROM_MAP, sw.toString(), true);

    Mockito.verify(context).getEntry(PROFILE_PATH);
    Mockito.verify(entry)
        .store(Mockito.<BiThrowingConsumer<MigrationReport, OutputStream, IOException>>notNull());
    Mockito.verify(featureMigrator).exportFeatures();
    Mockito.verify(bundleMigrator).exportBundles();
  }

  @Test
  public void testDoImportWhenAllSucceedsInOnePass() throws Exception {
    initImportAttempts(ProfileMigratable.ATTEMPT_COUNT, ProfileMigratable.ATTEMPT_COUNT);

    Mockito.doAnswer(succeedsWasSuccessful()).when(report).wasSuccessful(Mockito.notNull());
    Mockito.doAnswer(
            succeedsImportAndStopRecordingTasksAtAttempt(
                importFeaturesAttempts, ProfileMigratable.ATTEMPT_COUNT - 1))
        .when(featureMigrator)
        .importFeatures(Mockito.notNull(), Mockito.notNull());
    Mockito.doAnswer(
            succeedsImportAndStopRecordingTasksAtAttempt(
                importBundlesAttempts, ProfileMigratable.ATTEMPT_COUNT - 1))
        .when(bundleMigrator)
        .importBundles(Mockito.notNull(), Mockito.notNull());

    migratable.doImport(context);

    verifyMigratorsImport(2, 2, true);
  }

  @Test
  public void testDoImportWhenFeaturesSucceedOnlyOnBeforeToFinalAttempt() throws Exception {
    initImportAttempts(ProfileMigratable.ATTEMPT_COUNT - 1, ProfileMigratable.ATTEMPT_COUNT - 1);

    Mockito.doAnswer(succeedsWasSuccessful()).when(report).wasSuccessful(Mockito.notNull());
    Mockito.doAnswer(succeedsImportOnLastAttempt(importFeaturesAttempts))
        .when(featureMigrator)
        .importFeatures(Mockito.notNull(), Mockito.notNull());
    Mockito.doAnswer(succeedsImportAndStopRecordingTasksAtAttempt(importBundlesAttempts, 0))
        .when(bundleMigrator)
        .importBundles(Mockito.notNull(), Mockito.notNull());

    migratable.doImport(context);

    verifyMigratorsImport(ProfileMigratable.ATTEMPT_COUNT, ProfileMigratable.ATTEMPT_COUNT, false);
  }

  @Test
  public void testDoImportWhenBundlesSucceedOnlyOnBeforeToFinalAttempt() throws Exception {
    initImportAttempts(1, ProfileMigratable.ATTEMPT_COUNT - 1);

    Mockito.doAnswer(succeedsWasSuccessful()).when(report).wasSuccessful(Mockito.notNull());
    Mockito.doAnswer(succeedsImportAndStopRecordingTasksAtAttempt(importFeaturesAttempts, 0))
        .when(featureMigrator)
        .importFeatures(Mockito.notNull(), Mockito.notNull());
    Mockito.doAnswer(succeedsImportOnLastAttempt(importBundlesAttempts))
        .when(bundleMigrator)
        .importBundles(Mockito.notNull(), Mockito.notNull());

    migratable.doImport(context);

    // importFeatures() will be called on last attempt and verification attempt only
    verifyMigratorsImport(2, ProfileMigratable.ATTEMPT_COUNT, false);
  }

  @Test
  public void testDoImportWhenFeaturesFailOnFirstPass() throws Exception {
    Mockito.doAnswer(failsWasSuccessful()).when(report).wasSuccessful(Mockito.notNull());
    Mockito.doReturn(false)
        .when(featureMigrator)
        .importFeatures(Mockito.notNull(), Mockito.notNull());
    Mockito.doReturn(true).when(bundleMigrator).importBundles(Mockito.notNull(), Mockito.notNull());

    migratable.doImport(context);

    verifyMigratorsImport(1, 1, true);
  }

  @Test
  public void testDoImportWhenBundlesFailOnFirstPass() throws Exception {
    Mockito.doAnswer(failsWasSuccessful()).when(report).wasSuccessful(Mockito.notNull());
    Mockito.doReturn(false)
        .when(featureMigrator)
        .importFeatures(Mockito.notNull(), Mockito.notNull());
    Mockito.doReturn(false)
        .when(bundleMigrator)
        .importBundles(Mockito.notNull(), Mockito.notNull());

    migratable.doImport(context);

    verifyMigratorsImport(0, 1, true);
  }

  @Test
  public void testDoImportWhenFeaturesNeverSucceed() throws Exception {
    initImportAttempts(0, ProfileMigratable.ATTEMPT_COUNT - 1);

    Mockito.doAnswer(succeedsWasSuccessful()).when(report).wasSuccessful(Mockito.notNull());
    Mockito.doAnswer(neverSucceedsImport())
        .when(featureMigrator)
        .importFeatures(Mockito.notNull(), Mockito.notNull());
    Mockito.doAnswer(succeedsImportAndStopRecordingTasksAtAttempt(importBundlesAttempts, 0))
        .when(bundleMigrator)
        .importBundles(Mockito.notNull(), Mockito.notNull());

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage(Matchers.containsString("too many attempts"));

    migratable.doImport(context);
  }

  @Test
  public void testDoImportWhenBundlesNeverSucceed() throws Exception {
    initImportAttempts(1, 0);

    Mockito.doAnswer(succeedsWasSuccessful()).when(report).wasSuccessful(Mockito.notNull());
    Mockito.doAnswer(succeedsImportAndStopRecordingTasksAtAttempt(importFeaturesAttempts, 0))
        .when(featureMigrator)
        .importFeatures(Mockito.notNull(), Mockito.notNull());
    Mockito.doAnswer(neverSucceedsImport())
        .when(bundleMigrator)
        .importBundles(Mockito.notNull(), Mockito.notNull());

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage(Matchers.containsString("too many attempts"));

    migratable.doImport(context);
  }

  @Test
  public void testDoImportWhenProfileWasNotExported() throws Exception {
    Mockito.doAnswer(callWithoutJson(report))
        .when(entry)
        .restore(
            Mockito
                .<BiThrowingConsumer<MigrationReport, Optional<InputStream>, IOException>>
                    notNull());
    Mockito.doReturn(report).when(context).getReport();
    Mockito.doReturn(report).when(report).record(Mockito.<MigrationException>notNull());

    migratable.doImport(context);

    final ArgumentCaptor<MigrationException> error =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(report).record(error.capture());

    Assert.assertThat(
        error.getValue(), // last one should be the too many attempts one
        ThrowableMessageMatcher.hasMessage(
            Matchers.containsString("missing exported profile information")));
  }

  private void initImportAttempts(int features, int bundles) {
    importFeaturesAttempts.set(features);
    importBundlesAttempts.set(bundles);
  }

  private void verifyMigratorsImport(int features, int bundles, boolean nofinals) {
    Mockito.verify(context).getEntry(PROFILE_PATH);
    Mockito.verify(entry)
        .restore(
            Mockito
                .<BiThrowingConsumer<MigrationReport, Optional<InputStream>, IOException>>
                    notNull());
    Mockito.verify(report, Mockito.times(Math.max(features, bundles)))
        .wasSuccessful(Mockito.notNull());

    final ArgumentCaptor<JsonProfile> jprofiles = ArgumentCaptor.forClass(JsonProfile.class);
    final ArgumentCaptor<ProfileMigrationReport> featureReports =
        ArgumentCaptor.forClass(ProfileMigrationReport.class);
    final ArgumentCaptor<ProfileMigrationReport> bundleReports =
        ArgumentCaptor.forClass(ProfileMigrationReport.class);

    Mockito.verify(featureMigrator, Mockito.times(features))
        .importFeatures(featureReports.capture(), jprofiles.capture());
    Mockito.verify(bundleMigrator, Mockito.times(bundles))
        .importBundles(bundleReports.capture(), jprofiles.capture());

    verifyReports(featureReports, features, nofinals);
    verifyReports(bundleReports, bundles, nofinals);

    jprofiles
        .getAllValues()
        .forEach(
            p -> {
              Assert.assertThat(
                  p.features().toArray(JsonFeature[]::new), Matchers.arrayContaining(JFEATURE));
              Assert.assertThat(
                  p.bundles().toArray(JsonBundle[]::new), Matchers.arrayContaining(JBUNDLE));
            });
  }

  private void verifyReports(
      ArgumentCaptor<ProfileMigrationReport> reports, int total, boolean nofinals) {
    final List<ProfileMigrationReport> rs = reports.getAllValues();

    Assert.assertThat(rs.size(), Matchers.equalTo(total));
    for (int i = 0; i < total; i++) {
      Assert.assertThat(
          rs.get(i).isFinalAttempt(), Matchers.equalTo(!nofinals && (i == total - 1)));
    }
  }

  private static Answer callWithJson(MigrationReport report) {
    return AdditionalAnswers
        .<Boolean, BiThrowingConsumer<MigrationReport, Optional<InputStream>, IOException>>answer(
            c -> {
              // callback the consumer
              c.accept(
                  report,
                  Optional.of(
                      new ReaderInputStream(
                          new StringReader(JSON_PROFILE_STR), Charset.defaultCharset())));
              return true;
            });
  }

  private static Answer callWithoutJson(MigrationReport report) {
    return AdditionalAnswers
        .<Boolean, BiThrowingConsumer<MigrationReport, Optional<InputStream>, IOException>>answer(
            c -> {
              // callback the consumer
              c.accept(report, Optional.empty());
              return true;
            });
  }

  private static Answer succeedsWasSuccessful() {
    return AdditionalAnswers.<Boolean, Runnable>answer(
        r -> {
          r.run();
          return true;
        });
  }

  private static Answer failsWasSuccessful() {
    return AdditionalAnswers.<Boolean, Runnable>answer(
        r -> {
          r.run();
          return false;
        });
  }

  private static Answer succeedsImportAndStopRecordingTasksAtAttempt(
      AtomicInteger attempts, int attempt) {
    return AdditionalAnswers.<Boolean, ProfileMigrationReport, JsonProfile>answer(
        (r, p) -> {
          if (attempts.getAndDecrement() > attempt) {
            r.recordTask();
          }
          return true;
        });
  }

  private static Answer succeedsImportOnLastAttempt(AtomicInteger attempts) {
    return AdditionalAnswers.<Boolean, ProfileMigrationReport, JsonProfile>answer(
        (r, p) -> {
          final int attempt = attempts.decrementAndGet();

          if (attempt <= 0) { // succeeds on last attempt only
            if (attempt == 0) {
              r.recordTask();
            } // else - don't record tasks on the verification attempt that will follow the last
            //          attempt
            return true;
          }
          r.recordTask();
          r.recordOnFinalAttempt(new MigrationException("testing import #" + (attempt + 1)));
          return false;
        });
  }

  private static Answer neverSucceedsImport() {
    return AdditionalAnswers.<Boolean, ProfileMigrationReport, JsonProfile>answer(
        (r, p) -> {
          r.recordTask();
          r.recordOnFinalAttempt(new MigrationException("testing import ..."));
          return false;
        });
  }
}
