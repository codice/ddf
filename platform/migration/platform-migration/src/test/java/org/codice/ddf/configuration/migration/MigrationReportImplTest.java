/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.configuration.migration;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang.ArrayUtils;
import org.codice.ddf.migration.MigrationCompoundException;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationInformation;
import org.codice.ddf.migration.MigrationMessage;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.test.util.ThrowableMatchers;
import org.codice.ddf.util.function.ERunnable;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class MigrationReportImplTest {
    private final static String[] MESSAGE_STRINGS =
            new String[] {"warning1", "info2", "error3", "info4", "info5", "warning6", "error7",
                    "error8", "warning9", "warning10", "info11"};

    private final static String[] POTENTIAL_WARNING_MESSAGE_STRINGS = Stream.of(MESSAGE_STRINGS)
            .filter(m -> !m.startsWith("info"))
            .toArray(String[]::new);

    private final static String[] ERRORS = Stream.of(MESSAGE_STRINGS)
            .filter(m -> m.startsWith("error"))
            .toArray(String[]::new);

    private final static String[] WARNINGS = Stream.of(MESSAGE_STRINGS)
            .filter(m -> m.startsWith("warning"))
            .toArray(String[]::new);

    private final static String[] INFOS = Stream.of(MESSAGE_STRINGS)
            .filter(m -> m.startsWith("info"))
            .toArray(String[]::new);

    private final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.EXPORT,
            Optional.empty());

    private final MigrationException[] EXCEPTIONS = new MigrationException[ERRORS.length];

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void before() {
        int i = 0;

        for (final String msg : MESSAGE_STRINGS) {
            if (msg.startsWith("info")) {
                REPORT.record(msg);
            } else if (msg.startsWith("warning")) {
                REPORT.record(new MigrationWarning(msg));
            } else {
                final MigrationException e = new MigrationException(msg);

                EXCEPTIONS[i++] = e;
                REPORT.record(e);
            }
        }
    }

    @Test
    public void testConstructorWithExportOperation() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.EXPORT,
                Optional.empty());

        Assert.assertThat(REPORT.getOperation(), Matchers.equalTo(MigrationOperation.EXPORT));
        Assert.assertThat(REPORT.getStartTime(), Matchers.greaterThan(0L));
        Assert.assertThat(REPORT.getEndTime(), Matchers.equalTo(-1L));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testConstructorWithImportOperation() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
                Optional.empty());

        Assert.assertThat(REPORT.getOperation(), Matchers.equalTo(MigrationOperation.IMPORT));
        Assert.assertThat(REPORT.getStartTime(), Matchers.greaterThan(0L));
        Assert.assertThat(REPORT.getEndTime(), Matchers.equalTo(-1L));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testConstructorWithNullOperation() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null operation"));

        new MigrationReportImpl(null, Optional.empty());
    }

    @Test
    public void testConstructorWithReport() throws Exception {
        final MigrationReportImpl REPORT2 = new MigrationReportImpl(MigrationOperation.IMPORT,
                REPORT,
                Optional.empty());

        Assert.assertThat(REPORT2.getOperation(), Matchers.equalTo(MigrationOperation.IMPORT));
        Assert.assertThat(REPORT2.getStartTime(), Matchers.equalTo(REPORT.getStartTime()));
        Assert.assertThat(REPORT.getEndTime(), Matchers.equalTo(-1L));
        Assert.assertThat(REPORT2.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT2.warnings()
                        .map(MigrationWarning::getMessage)
                        .toArray(String[]::new),
                Matchers.arrayContaining(POTENTIAL_WARNING_MESSAGE_STRINGS));
    }

    @Test
    public void testConstructorWithUncompletedReport() throws Exception {
        REPORT.doAfterCompletion(r -> r.record(new MigrationWarning("final warning")));

        final MigrationReportImpl REPORT2 = new MigrationReportImpl(MigrationOperation.IMPORT,
                REPORT,
                Optional.empty());

        Assert.assertThat(REPORT2.getOperation(), Matchers.equalTo(MigrationOperation.IMPORT));
        Assert.assertThat(REPORT2.getStartTime(), Matchers.equalTo(REPORT.getStartTime()));
        Assert.assertThat(REPORT.getEndTime(), Matchers.equalTo(-1L));
        Assert.assertThat(REPORT2.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT2.warnings()
                        .map(MigrationWarning::getMessage)
                        .toArray(String[]::new),
                Matchers.arrayContaining(ArrayUtils.add(POTENTIAL_WARNING_MESSAGE_STRINGS,
                        "final warning")));
    }

    @Test
    public void testRecordWithWithInfo() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
                Optional.empty());
        final MigrationInformation INFO = new MigrationInformation("info");

        REPORT.record(INFO);

        Assert.assertThat(REPORT.hasInfos(), Matchers.equalTo(true));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.messages()
                .toArray(), Matchers.arrayContaining(INFO));
    }

    @Test
    public void testRecordWithWarning() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
                Optional.empty());
        final MigrationWarning WARNING = new MigrationWarning("warning");

        REPORT.record(WARNING);

        Assert.assertThat(REPORT.hasInfos(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(true));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.messages()
                .toArray(), Matchers.arrayContaining(WARNING));
    }

    @Test
    public void testRecordWithError() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
                Optional.empty());
        final MigrationException ERROR = new MigrationException("error");

        REPORT.record(ERROR);

        Assert.assertThat(REPORT.hasInfos(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));
        Assert.assertThat(REPORT.messages()
                .toArray(), Matchers.arrayContaining(ERROR));
    }

    @Test
    public void testRecordWithNull() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null message"));

        REPORT.record((MigrationMessage) null);
    }

    @Test
    public void testDoAfterCompletion() throws Exception {
        final Consumer<MigrationReport> CODE = Mockito.mock(Consumer.class);

        REPORT.doAfterCompletion(CODE);

        Mockito.verify(CODE, Mockito.never())
                .accept(Mockito.any());
    }

    @Test
    public void testDoAfterCompletionWithNullCode() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null code"));

        REPORT.doAfterCompletion(null);
    }

    @Test
    public void testErrorsWhenNoneRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
                Optional.empty());

        Assert.assertThat(REPORT.errors()
                .count(), Matchers.equalTo(0L));
    }

    @Test
    public void testErrorsWhenSomeRecorded() throws Exception {
        Assert.assertThat(REPORT.errors()
                .toArray(), Matchers.arrayContaining(EXCEPTIONS));
    }

    @Test
    public void testErrorsDoesNotCallRegisteredCode() throws Exception {
        final Consumer<MigrationReport> CODE = Mockito.mock(Consumer.class);

        REPORT.doAfterCompletion(CODE);
        REPORT.errors();

        Mockito.verify(CODE, Mockito.never())
                .accept(Mockito.any());
    }

    @Test
    public void testWarningsWhenNoneRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
                Optional.empty());

        Assert.assertThat(REPORT.warnings()
                .count(), Matchers.equalTo(0L));
    }

    @Test
    public void testWarningsWhenSomeRecorded() throws Exception {
        Assert.assertThat(REPORT.warnings()
                .map(MigrationWarning::getMessage)
                .toArray(), Matchers.arrayContaining(WARNINGS));
    }

    @Test
    public void testWarningsDoesNotCallRegisteredCode() throws Exception {
        final Consumer<MigrationReport> CODE = Mockito.mock(Consumer.class);

        REPORT.doAfterCompletion(CODE);
        REPORT.warnings();

        Mockito.verify(CODE, Mockito.never())
                .accept(Mockito.any());
    }

    @Test
    public void testInfosWhenNoneRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
                Optional.empty());

        Assert.assertThat(REPORT.infos()
                .count(), Matchers.equalTo(0L));
    }

    @Test
    public void testInfosWhenSomeRecorded() throws Exception {
        Assert.assertThat(REPORT.infos()
                .map(MigrationInformation::getMessage)
                .toArray(), Matchers.arrayContaining(INFOS));
    }

    @Test
    public void testInfosDoesNotCallRegisteredCode() throws Exception {
        final Consumer<MigrationReport> CODE = Mockito.mock(Consumer.class);

        REPORT.doAfterCompletion(CODE);
        REPORT.infos();

        Mockito.verify(CODE, Mockito.never())
                .accept(Mockito.any());
    }

    @Test
    public void testWasSuccessfulWhenNoErrorsOrWarningsAreRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
                Optional.empty());

        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(true));
    }

    @Test
    public void testWasSuccessfulWhenBothWarningsAndErrorsAreRecorded() throws Exception {
        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(false));
    }

    @Test
    public void testWasSuccessfulWhenOnlyWarningsAreRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
                Optional.empty());

        REPORT.record(new MigrationWarning("warning"));

        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(true));
    }

    @Test
    public void testWasSuccessfulWhenOnlyErrorsRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
                Optional.empty());

        REPORT.record(new MigrationException("error"));

        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(false));
    }

    @Test
    public void testWasSuccessfulCallsRegisteredCode() throws Exception {
        final Consumer<MigrationReport> CODE = Mockito.mock(Consumer.class);

        REPORT.doAfterCompletion(CODE);
        REPORT.wasSuccessful();

        Mockito.verify(CODE, Mockito.only())
                .accept(Mockito.any());
    }

    @Test
    public void testWasSuccessfulWithRunnableWhenNoErrorsAreRecorded() throws Exception {
        final Runnable CODE = () -> {
        };

        Assert.assertThat(REPORT.wasSuccessful(CODE), Matchers.equalTo(true));
    }

    @Test
    public void testWasSuccessfulWithRunnableWhenErrorsAreRecorded() throws Exception {
        final Runnable CODE = () -> REPORT.record(new MigrationException("test"));

        Assert.assertThat(REPORT.wasSuccessful(CODE), Matchers.equalTo(false));
    }

    @Test
    public void testWasIOSuccessfulWithRunnableWhenNoErrorsAreRecorded() throws Exception {
        final ERunnable<IOException> CODE = () -> {
        };

        Assert.assertThat(REPORT.wasIOSuccessful(CODE), Matchers.equalTo(true));
    }

    @Test
    public void testWasIOSuccessfulWithRunnableWhenErrorsAreRecorded() throws Exception {
        final ERunnable<IOException> CODE = () -> REPORT.record(new MigrationException("test"));

        Assert.assertThat(REPORT.wasIOSuccessful(CODE), Matchers.equalTo(false));
    }

    @Test
    public void testWasIOSuccessfulWithRunnableThrowingException() throws Exception {
        final IOException E = new IOException("test");
        final ERunnable<IOException> CODE = () -> { throw E; };

        thrown.expect(Matchers.sameInstance(E));
        thrown.expectMessage(Matchers.containsString(""));

        REPORT.wasIOSuccessful(CODE);
    }

    @Test
    public void testHasErrorsWhenBothWarningsAndErrorsAreRecorded() throws Exception {
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));
    }

    @Test
    public void testHasErrorsWhenNoneRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
                Optional.empty());

        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
    }

    @Test
    public void testHasErrorsWhenOnlyWarningsAreRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
                Optional.empty());

        REPORT.record(new MigrationWarning("warning"));

        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
    }

    @Test
    public void testHasErrorsCallsRegisteredCode() throws Exception {
        final Consumer<MigrationReport> CODE = Mockito.mock(Consumer.class);

        REPORT.doAfterCompletion(CODE);
        REPORT.hasErrors();

        Mockito.verify(CODE, Mockito.only())
                .accept(Mockito.any());
    }

    @Test
    public void testHasWarningsWhenBothWarningsAndErrorsAreRecorded() throws Exception {
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(true));
    }

    @Test
    public void testHasWarningsWhenNoneRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
                Optional.empty());

        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testHasWarningsWhenOnlyErrorsAreRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
                Optional.empty());

        REPORT.record(new MigrationException("error"));

        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testHasWarningsCallsRegisteredCode() throws Exception {
        final Consumer<MigrationReport> CODE = Mockito.mock(Consumer.class);

        REPORT.doAfterCompletion(CODE);
        REPORT.hasWarnings();

        Mockito.verify(CODE, Mockito.only())
                .accept(Mockito.any());
    }

    @Test
    public void testVerifyCompletionWhenNoErrorsAreRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
                Optional.empty());

        REPORT.verifyCompletion();
    }

    @Test
    public void testVerifyCompletionWhenOneErrorIsRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
                Optional.empty());
        final MigrationException ERROR = new MigrationException("error");

        REPORT.record(ERROR);

        thrown.expect(MigrationException.class);
        thrown.expect(Matchers.sameInstance(ERROR));

        REPORT.verifyCompletion();
    }

    @Test
    public void testVerifyCompletionWhenMultipleErrorsAreRecorded() throws Exception {
        thrown.expect(MigrationCompoundException.class);
        thrown.expectMessage(Matchers.containsString(ERRORS[0]));
        thrown.expect(ThrowableMatchers.hasCauseMatching(Matchers.sameInstance(EXCEPTIONS[0])));
        thrown.expect(ThrowableMatchers.hasSuppressedMatching(Matchers.arrayContaining(Stream.of(
                EXCEPTIONS)
                .skip(1)
                .toArray(MigrationException[]::new))));

        REPORT.verifyCompletion();
    }

    @Test
    public void testVerifyCompletionCallsRegisteredCode() throws Exception {
        final Consumer<MigrationReport> CODE = Mockito.mock(Consumer.class);

        REPORT.doAfterCompletion(CODE);
        try {
            REPORT.verifyCompletion();
        } catch (MigrationException e) { // don't care here if it happens or not
        }

        Mockito.verify(CODE, Mockito.only())
                .accept(Mockito.any());
    }

    @Test
    public void testEnd() throws Exception {
        REPORT.end();

        Assert.assertThat(REPORT.getEndTime(), Matchers.greaterThan(0L));
    }

    @Test
    public void testEndCallsRegisteredCode() throws Exception {
        final Consumer<MigrationReport> CODE = Mockito.mock(Consumer.class);

        REPORT.doAfterCompletion(CODE);
        REPORT.end();

        Mockito.verify(CODE, Mockito.only())
                .accept(Mockito.any());
    }
}
