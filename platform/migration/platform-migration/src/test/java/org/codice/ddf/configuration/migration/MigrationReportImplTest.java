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
package org.codice.ddf.configuration.migration;

import com.github.npathai.hamcrestopt.OptionalMatchers;
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
import org.codice.ddf.test.common.matchers.MappingMatchers;
import org.codice.ddf.test.common.matchers.ThrowableMatchers;
import org.codice.ddf.util.function.ThrowingRunnable;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class MigrationReportImplTest extends AbstractMigrationReportSupport {
  public MigrationReportImplTest() {
    super(MigrationOperation.EXPORT);
  }

  @Test
  public void testConstructorWithExportOperation() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.EXPORT, Optional.empty());

    Assert.assertThat(report.getOperation(), Matchers.equalTo(MigrationOperation.EXPORT));
    Assert.assertThat(report.getStartTime().toEpochMilli(), Matchers.greaterThan(0L));
    Assert.assertThat(report.getEndTime(), OptionalMatchers.isEmpty());
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testConstructorWithImportOperation() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());

    Assert.assertThat(report.getOperation(), Matchers.equalTo(MigrationOperation.IMPORT));
    Assert.assertThat(report.getStartTime().toEpochMilli(), Matchers.greaterThan(0L));
    Assert.assertThat(report.getEndTime(), OptionalMatchers.isEmpty());
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testConstructorWithNullOperation() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null operation"));

    new MigrationReportImpl(null, Optional.empty());
  }

  @Test
  public void testRecordWithInfo() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());
    final MigrationInformation info = new MigrationInformation("info");

    report.record(info);

    Assert.assertThat(report.hasInfos(), Matchers.equalTo(true));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.messages().toArray(), Matchers.arrayContaining(info));
  }

  @Test
  public void testRecordWithWarning() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());
    final MigrationWarning warning = new MigrationWarning("warning");

    report.record(warning);

    Assert.assertThat(report.hasInfos(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(true));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.messages().toArray(), Matchers.arrayContaining(warning));
  }

  @Test
  public void testRecordWithError() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());
    final MigrationException error = new MigrationException("error");

    report.record(error);

    Assert.assertThat(report.hasInfos(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));
    Assert.assertThat(report.messages().toArray(), Matchers.arrayContaining(error));
  }

  @Test
  public void testRecordWithNull() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null message"));

    report.record((MigrationMessage) null);
  }

  @Test
  public void testDoAfterCompletion() throws Exception {
    final Consumer<MigrationReport> code = Mockito.mock(Consumer.class);

    report.doAfterCompletion(code);

    Mockito.verify(code, Mockito.never()).accept(Mockito.any());
  }

  @Test
  public void testDoAfterCompletionWithNullCode() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null code"));

    report.doAfterCompletion(null);
  }

  @Test
  public void testErrorsWhenNoneRecorded() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());

    Assert.assertThat(report.errors().count(), Matchers.equalTo(0L));
  }

  @Test
  public void testErrorsWhenSomeRecorded() throws Exception {
    Assert.assertThat(report.errors().toArray(), Matchers.arrayContaining(exceptions));
  }

  @Test
  public void testErrorsDoesNotCallRegisteredCode() throws Exception {
    final Consumer<MigrationReport> code = Mockito.mock(Consumer.class);

    report.doAfterCompletion(code);
    report.errors();

    Mockito.verify(code, Mockito.never()).accept(Mockito.any());
  }

  @Test
  public void testWarningsWhenNoneRecorded() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());

    Assert.assertThat(report.warnings().count(), Matchers.equalTo(0L));
  }

  @Test
  public void testWarningsWhenSomeRecorded() throws Exception {
    Assert.assertThat(
        report.warnings().map(MigrationWarning::getMessage).toArray(),
        Matchers.arrayContaining(WARNINGS));
  }

  @Test
  public void testWarningsDoesNotCallRegisteredCode() throws Exception {
    final Consumer<MigrationReport> code = Mockito.mock(Consumer.class);

    report.doAfterCompletion(code);
    report.warnings();

    Mockito.verify(code, Mockito.never()).accept(Mockito.any());
  }

  @Test
  public void testInfosWhenNoneRecorded() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());

    Assert.assertThat(report.infos().count(), Matchers.equalTo(0L));
  }

  @Test
  public void testInfosWhenSomeRecorded() throws Exception {
    Assert.assertThat(
        report.infos().map(MigrationInformation::getMessage).toArray(),
        Matchers.arrayContaining(INFOS));
  }

  @Test
  public void testInfosDoesNotCallRegisteredCode() throws Exception {
    final Consumer<MigrationReport> code = Mockito.mock(Consumer.class);

    report.doAfterCompletion(code);
    report.infos();

    Mockito.verify(code, Mockito.never()).accept(Mockito.any());
  }

  @Test
  public void testWasSuccessfulWhenNoErrorsOrWarningsAreRecorded() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());

    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));
  }

  @Test
  public void testWasSuccessfulWhenBothWarningsAndErrorsAreRecorded() throws Exception {
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));
  }

  @Test
  public void testWasSuccessfulWhenOnlyWarningsAreRecorded() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());

    report.record(new MigrationWarning("warning"));

    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));
  }

  @Test
  public void testWasSuccessfulWhenOnlyErrorsRecorded() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());

    report.record(new MigrationException("error"));

    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));
  }

  @Test
  public void testWasSuccessfulCallsRegisteredCode() throws Exception {
    final Consumer<MigrationReport> code = Mockito.mock(Consumer.class);

    report.doAfterCompletion(code);
    report.wasSuccessful();

    Mockito.verify(code, Mockito.only()).accept(Mockito.any());
  }

  @Test
  public void testWasSuccessfulWithRunnableWhenNoErrorsAreRecorded() throws Exception {
    final Runnable code = () -> {};

    Assert.assertThat(report.wasSuccessful(code), Matchers.equalTo(true));
  }

  @Test
  public void testWasSuccessfulWithRunnableWhenErrorsAreRecorded() throws Exception {
    final Runnable code = () -> report.record(new MigrationException("test"));

    Assert.assertThat(report.wasSuccessful(code), Matchers.equalTo(false));
  }

  @Test
  public void testWasIOSuccessfulWithRunnableWhenNoErrorsAreRecorded() throws Exception {
    final ThrowingRunnable<IOException> code = () -> {};

    Assert.assertThat(report.wasIOSuccessful(code), Matchers.equalTo(true));
  }

  @Test
  public void testWasIOSuccessfulWithRunnableWhenErrorsAreRecorded() throws Exception {
    final ThrowingRunnable<IOException> code = () -> report.record(new MigrationException("test"));

    Assert.assertThat(report.wasIOSuccessful(code), Matchers.equalTo(false));
  }

  @Test
  public void testWasIOSuccessfulWithRunnableThrowingException() throws Exception {
    final IOException e = new IOException("test");
    final ThrowingRunnable<IOException> code =
        () -> {
          throw e;
        };

    thrown.expect(Matchers.sameInstance(e));
    thrown.expectMessage(Matchers.containsString(""));

    report.wasIOSuccessful(code);
  }

  @Test
  public void testHasErrorsWhenBothWarningsAndErrorsAreRecorded() throws Exception {
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));
  }

  @Test
  public void testHasErrorsWhenNoneRecorded() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());

    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
  }

  @Test
  public void testHasErrorsWhenOnlyWarningsAreRecorded() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());

    report.record(new MigrationWarning("warning"));

    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
  }

  @Test
  public void testHasErrorsCallsRegisteredCode() throws Exception {
    final Consumer<MigrationReport> code = Mockito.mock(Consumer.class);

    report.doAfterCompletion(code);
    report.hasErrors();

    Mockito.verify(code, Mockito.only()).accept(Mockito.any());
  }

  @Test
  public void testHasWarningsWhenBothWarningsAndErrorsAreRecorded() throws Exception {
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(true));
  }

  @Test
  public void testHasWarningsWhenNoneRecorded() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());

    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testHasWarningsWhenOnlyErrorsAreRecorded() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());

    report.record(new MigrationException("error"));

    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testHasWarningsCallsRegisteredCode() throws Exception {
    final Consumer<MigrationReport> code = Mockito.mock(Consumer.class);

    report.doAfterCompletion(code);
    report.hasWarnings();

    Mockito.verify(code, Mockito.only()).accept(Mockito.any());
  }

  @Test
  public void testVerifyCompletionWhenNoErrorsAreRecorded() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());

    report.verifyCompletion();
  }

  @Test
  public void testVerifyCompletionWhenOneErrorIsRecorded() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());
    final MigrationException error = new MigrationException("error");

    report.record(error);

    thrown.expect(MigrationException.class);
    thrown.expect(Matchers.sameInstance(error));

    report.verifyCompletion();
  }

  @Test
  public void testVerifyCompletionWhenMultipleErrorsAreRecorded() throws Exception {
    thrown.expect(Matchers.sameInstance(exceptions[0]));
    thrown.expectMessage(Matchers.containsString(ERRORS[0]));
    thrown.expect(ThrowableMatchers.hasCauseMatching(Matchers.nullValue(Throwable.class)));
    thrown.expect(
        ThrowableMatchers.hasSuppressedMatching(
            Matchers.arrayContaining(
                Stream.of(exceptions).skip(1).toArray(MigrationException[]::new))));

    report.verifyCompletion();
  }

  @Test
  public void testVerifyCompletionCallsRegisteredCode() throws Exception {
    final Consumer<MigrationReport> code = Mockito.mock(Consumer.class);

    report.doAfterCompletion(code);
    try {
      report.verifyCompletion();
    } catch (MigrationException e) { // don't care here if it happens or not
    }

    Mockito.verify(code, Mockito.only()).accept(Mockito.any());
  }

  @Test
  public void testEnd() throws Exception {
    report.end();

    Assert.assertThat(
        report.getEndTime(),
        OptionalMatchers.hasValue(
            MappingMatchers.map(Instant::toEpochMilli, Matchers.greaterThan(0L))));
  }

  @Test
  public void testEndCallsRegisteredCode() throws Exception {
    final Consumer<MigrationReport> code = Mockito.mock(Consumer.class);

    report.doAfterCompletion(code);
    report.end();

    Mockito.verify(code, Mockito.only()).accept(Mockito.any());
  }
}
