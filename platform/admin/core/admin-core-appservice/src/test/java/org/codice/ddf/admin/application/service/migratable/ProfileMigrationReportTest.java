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

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationInformation;
import org.codice.ddf.migration.MigrationMessage;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.util.function.ThrowingRunnable;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class ProfileMigrationReportTest {
  private final MigrationReport report = Mockito.mock(MigrationReport.class);

  private final ProfileMigrationReport profileReport = new ProfileMigrationReport(report, false);

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testOrdinalFirst() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(1), Matchers.equalTo("1st"));
  }

  @Test
  public void testOrdinalSecond() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(2), Matchers.equalTo("2nd"));
  }

  @Test
  public void testOrdinalThird() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(3), Matchers.equalTo("3rd"));
  }

  @Test
  public void testOrdinalFourth() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(4), Matchers.equalTo("4th"));
  }

  @Test
  public void testOrdinalFifth() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(5), Matchers.equalTo("5th"));
  }

  @Test
  public void testOrdinalTenth() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(10), Matchers.equalTo("10th"));
  }

  @Test
  public void testOrdinalEleventh() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(11), Matchers.equalTo("11th"));
  }

  @Test
  public void testOrdinalTwelveth() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(12), Matchers.equalTo("12th"));
  }

  @Test
  public void testOrdinalThirtheenth() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(13), Matchers.equalTo("13th"));
  }

  @Test
  public void testOrdinalTwentyth() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(20), Matchers.equalTo("20th"));
  }

  @Test
  public void testOrdinalTwentyFirst() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(21), Matchers.equalTo("21st"));
  }

  @Test
  public void testOrdinalTwentySecond() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(22), Matchers.equalTo("22nd"));
  }

  @Test
  public void testOrdinalTwentyThird() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(23), Matchers.equalTo("23rd"));
  }

  @Test
  public void testOrdinalTwentyFourth() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(24), Matchers.equalTo("24th"));
  }

  @Test
  public void testOrdinalOneHunderThirtyth() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(130), Matchers.equalTo("130th"));
  }

  @Test
  public void testOrdinalOneHunderThirtyFirst() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(131), Matchers.equalTo("131st"));
  }

  @Test
  public void testOrdinalOneHunderThirtySecond() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(132), Matchers.equalTo("132nd"));
  }

  @Test
  public void testOrdinalOneHunderThirtyThird() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(133), Matchers.equalTo("133rd"));
  }

  @Test
  public void testOrdinalOneHunderThirtyFourth() throws Exception {
    Assert.assertThat(ProfileMigrationReport.ordinal(134), Matchers.equalTo("134th"));
  }

  @Test
  public void testConstructor() throws Exception {
    Assert.assertThat(profileReport.isFinalAttempt(), Matchers.equalTo(false));
    Assert.assertThat(profileReport.hasSuppressedErrors(), Matchers.equalTo(false));
    Assert.assertThat(profileReport.hasRecordedTasks(), Matchers.equalTo(false));
  }

  @Test
  public void testConstructorWithNullReport() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null report"));

    new ProfileMigrationReport(null, false);
  }

  @Test
  public void testIsFinalAttempt() throws Exception {
    final ProfileMigrationReport profileReport = new ProfileMigrationReport(report, true);

    Assert.assertThat(profileReport.isFinalAttempt(), Matchers.equalTo(true));
  }

  @Test
  public void testHasSuppressedErrors() throws Exception {
    profileReport.recordOnFinalAttempt(new MigrationException("testing"));

    Assert.assertThat(profileReport.hasSuppressedErrors(), Matchers.equalTo(true));
  }

  @Test
  public void testRecordTask() throws Exception {
    profileReport.recordTask();

    Assert.assertThat(profileReport.hasRecordedTasks(), Matchers.equalTo(true));
  }

  @Test
  public void testRecordTaskMultipleTimes() throws Exception {
    profileReport.recordTask();
    profileReport.recordTask();

    Assert.assertThat(profileReport.hasRecordedTasks(), Matchers.equalTo(true));
  }

  @Test
  public void testRecordOnFinalAttemptWhenReportIsNotForAFinalAttempt() throws Exception {
    Mockito.doReturn(report).when(report).record(Mockito.notNull(MigrationException.class));

    Assert.assertThat(
        profileReport.recordOnFinalAttempt(new MigrationException("testing")),
        Matchers.sameInstance(profileReport));
    Assert.assertThat(profileReport.hasSuppressedErrors(), Matchers.equalTo(true));

    Mockito.verify(report, Mockito.never()).record(Mockito.notNull(MigrationException.class));
  }

  @Test
  public void testRecordOnFinalAttemptWhenReportIsForAFinalAttempt() throws Exception {
    final MigrationException e = new MigrationException("testing");
    final ProfileMigrationReport profileReport = new ProfileMigrationReport(report, true);

    Mockito.doReturn(report).when(report).record(e);

    Assert.assertThat(profileReport.recordOnFinalAttempt(e), Matchers.sameInstance(profileReport));
    Assert.assertThat(profileReport.hasSuppressedErrors(), Matchers.equalTo(false));

    Mockito.verify(report).record(e);
  }

  @Test
  public void testGetFeatureAttemptStringTheFirstTime() throws Exception {
    profileReport.getFeatureAttemptString(Operation.UNINSTALL, "feature");

    Assert.assertThat(
        profileReport.getFeatureAttemptString(Operation.INSTALL, "feature"), Matchers.equalTo(""));

    // should not be impacted
    Assert.assertThat(
        profileReport.getFeatureAttemptString(Operation.START, "feature2"), Matchers.equalTo(""));
  }

  @Test
  public void testGetFeatureAttemptStringTheSecondTime() throws Exception {
    profileReport.getFeatureAttemptString(Operation.UNINSTALL, "feature");

    Assert.assertThat(
        profileReport.getFeatureAttemptString(Operation.UNINSTALL, "feature"),
        Matchers.equalTo(" (2nd attempt)"));

    // should not be impacted
    Assert.assertThat(
        profileReport.getFeatureAttemptString(Operation.STOP, "feature2"), Matchers.equalTo(""));
    Assert.assertThat(
        profileReport.getFeatureAttemptString(Operation.INSTALL, "feature"), Matchers.equalTo(""));
  }

  @Test
  public void testGetBundleAttemptStringTheFirstTime() throws Exception {
    profileReport.getBundleAttemptString(Operation.UNINSTALL, "bundle");

    Assert.assertThat(
        profileReport.getBundleAttemptString(Operation.INSTALL, "bundle"), Matchers.equalTo(""));

    // should not be impacted
    Assert.assertThat(
        profileReport.getBundleAttemptString(Operation.START, "bundle2"), Matchers.equalTo(""));
  }

  @Test
  public void testGetBundleAttemptStringTheSecondTime() throws Exception {
    profileReport.getBundleAttemptString(Operation.UNINSTALL, "bundle");

    Assert.assertThat(
        profileReport.getBundleAttemptString(Operation.UNINSTALL, "bundle"),
        Matchers.equalTo(" (2nd attempt)"));

    // should not be impacted
    Assert.assertThat(
        profileReport.getBundleAttemptString(Operation.STOP, "bundle2"), Matchers.equalTo(""));
    Assert.assertThat(
        profileReport.getBundleAttemptString(Operation.INSTALL, "bundle"), Matchers.equalTo(""));
  }

  @Test
  public void testGetOperation() throws Exception {
    final MigrationOperation op = MigrationOperation.EXPORT;

    Mockito.doReturn(op).when(report).getOperation();

    Assert.assertThat(profileReport.getOperation(), Matchers.equalTo(op));

    Mockito.verify(report).getOperation();
  }

  @Test
  public void testGetStartTime() throws Exception {
    final Instant instant = Instant.now();

    Mockito.doReturn(instant).when(report).getStartTime();

    Assert.assertThat(profileReport.getStartTime(), Matchers.equalTo(instant));

    Mockito.verify(report).getStartTime();
  }

  @Test
  public void testGetEndTime() throws Exception {
    final Optional<Instant> instant = Optional.of(Instant.now());

    Mockito.doReturn(instant).when(report).getEndTime();

    Assert.assertThat(profileReport.getEndTime(), Matchers.equalTo(instant));

    Mockito.verify(report).getEndTime();
  }

  @Test
  public void testRecordWithString() throws Exception {
    final String msg = "test";

    Mockito.doReturn(report).when(report).record(msg);

    Assert.assertThat(profileReport.record(msg), Matchers.sameInstance(profileReport));

    Mockito.verify(report).record(msg);
  }

  @Test
  public void testRecordWithStringFormat() throws Exception {
    final String msg = "test";

    Mockito.doReturn(report).when(report).record(msg, 1);

    Assert.assertThat(profileReport.record(msg, 1), Matchers.sameInstance(profileReport));

    Mockito.verify(report).record(msg, 1);
  }

  @Test
  public void testRecordWithMessage() throws Exception {
    final MigrationMessage msg = Mockito.mock(MigrationMessage.class);

    Mockito.doReturn(report).when(report).record(msg);

    Assert.assertThat(profileReport.record(msg), Matchers.sameInstance(profileReport));

    Mockito.verify(report).record(msg);
  }

  @Test
  public void testDoAfterCompletion() throws Exception {
    final Consumer<MigrationReport> consumer = Mockito.mock(Consumer.class);

    Mockito.doReturn(report).when(report).doAfterCompletion(consumer);

    Assert.assertThat(
        profileReport.doAfterCompletion(consumer), Matchers.sameInstance(profileReport));

    Mockito.verify(report).doAfterCompletion(consumer);
  }

  @Test
  public void testMessages() throws Exception {
    final Stream<MigrationMessage> stream = Mockito.mock(Stream.class);

    Mockito.doReturn(stream).when(report).messages();

    Assert.assertThat(profileReport.messages(), Matchers.sameInstance(stream));

    Mockito.verify(report).messages();
  }

  @Test
  public void testErrors() throws Exception {
    final Stream<MigrationException> stream = Mockito.mock(Stream.class);

    Mockito.doReturn(stream).when(report).errors();

    Assert.assertThat(profileReport.errors(), Matchers.sameInstance(stream));

    Mockito.verify(report).errors();
  }

  @Test
  public void testWarnings() throws Exception {
    final Stream<MigrationWarning> stream = Mockito.mock(Stream.class);

    Mockito.doReturn(stream).when(report).warnings();

    Assert.assertThat(profileReport.warnings(), Matchers.sameInstance(stream));

    Mockito.verify(report).warnings();
  }

  @Test
  public void testInfos() throws Exception {
    final Stream<MigrationInformation> stream = Mockito.mock(Stream.class);

    Mockito.doReturn(stream).when(report).infos();

    Assert.assertThat(profileReport.infos(), Matchers.sameInstance(stream));

    Mockito.verify(report).infos();
  }

  @Test
  public void testWasSuccesfulWhenTrue() throws Exception {
    Mockito.doReturn(true).when(report).wasSuccessful();

    Assert.assertThat(profileReport.wasSuccessful(), Matchers.equalTo(true));

    Mockito.verify(report).wasSuccessful();
  }

  @Test
  public void testWasSuccesfulWhenFalse() throws Exception {
    Mockito.doReturn(false).when(report).wasSuccessful();

    Assert.assertThat(profileReport.wasSuccessful(), Matchers.equalTo(false));

    Mockito.verify(report).wasSuccessful();
  }

  @Test
  public void testWasSuccesfulWithRunnableWhenTrue() throws Exception {
    final Runnable runnable = Mockito.mock(Runnable.class);

    Mockito.doReturn(true).when(report).wasSuccessful(runnable);

    Assert.assertThat(profileReport.wasSuccessful(runnable), Matchers.equalTo(true));

    Mockito.verify(report).wasSuccessful(runnable);
  }

  @Test
  public void testWasSuccesfulWithRunnableWhenFalse() throws Exception {
    final Runnable runnable = Mockito.mock(Runnable.class);

    Mockito.doReturn(false).when(report).wasSuccessful(runnable);

    Assert.assertThat(profileReport.wasSuccessful(runnable), Matchers.equalTo(false));

    Mockito.verify(report).wasSuccessful(runnable);
  }

  @Test
  public void testWasIOSuccesfulWhenTrue() throws Exception {
    final ThrowingRunnable<IOException> code = Mockito.mock(ThrowingRunnable.class);

    Mockito.doReturn(true).when(report).wasIOSuccessful(code);

    Assert.assertThat(profileReport.wasIOSuccessful(code), Matchers.equalTo(true));

    Mockito.verify(report).wasIOSuccessful(code);
  }

  @Test
  public void testWasIOSuccesfulWhenFalse() throws Exception {
    final ThrowingRunnable<IOException> code = Mockito.mock(ThrowingRunnable.class);

    Mockito.doReturn(false).when(report).wasIOSuccessful(code);

    Assert.assertThat(profileReport.wasIOSuccessful(code), Matchers.equalTo(false));

    Mockito.verify(report).wasIOSuccessful(code);
  }

  @Test
  public void testWasIOSuccesfulWhenFailedWithIOException() throws Exception {
    final ThrowingRunnable<IOException> code = Mockito.mock(ThrowingRunnable.class);
    final IOException e = new IOException("testing");

    thrown.expect(Matchers.sameInstance(e));

    Mockito.doThrow(e).when(report).wasIOSuccessful(code);

    profileReport.wasIOSuccessful(code);

    Mockito.verify(report).wasIOSuccessful(code);
  }

  @Test
  public void testHasInfos() throws Exception {
    Mockito.doReturn(true).when(report).hasInfos();

    Assert.assertThat(profileReport.hasInfos(), Matchers.equalTo(true));

    Mockito.verify(report).hasInfos();
  }

  @Test
  public void testHasWarnings() throws Exception {
    Mockito.doReturn(true).when(report).hasWarnings();

    Assert.assertThat(profileReport.hasWarnings(), Matchers.equalTo(true));

    Mockito.verify(report).hasWarnings();
  }

  @Test
  public void testHasErrors() throws Exception {
    Mockito.doReturn(true).when(report).hasErrors();

    Assert.assertThat(profileReport.hasErrors(), Matchers.equalTo(true));

    Mockito.verify(report).hasErrors();
  }

  @Test
  public void testVerifyCompletion() throws Exception {
    Mockito.doNothing().when(report).verifyCompletion();

    profileReport.verifyCompletion();

    Mockito.verify(report).verifyCompletion();
  }

  @Test
  public void testVerifyCompletionWhenFailedWithMigrationException() throws Exception {
    final MigrationException e = new MigrationException("test");

    Mockito.doThrow(e).when(report).verifyCompletion();

    thrown.expect(Matchers.sameInstance(e));

    profileReport.verifyCompletion();
  }
}
