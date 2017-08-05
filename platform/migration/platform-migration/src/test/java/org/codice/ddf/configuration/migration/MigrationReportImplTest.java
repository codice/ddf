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

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang.ArrayUtils;
import org.codice.ddf.migration.MigrationCompoundException;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.test.util.ThrowableMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class MigrationReportImplTest {
    private final static String[] MESSAGES =
            new String[] {"warning1", "error2", "warning3", "error4", "error5", "warning6",
                    "warning7"};

    private final static String[] ERRORS = Stream.of(MESSAGES)
            .filter(m -> m.startsWith("error"))
            .toArray(String[]::new);

    private final static String[] WARNINGS = Stream.of(MESSAGES)
            .filter(m -> m.startsWith("warning"))
            .toArray(String[]::new);

    private final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.EXPORT);

    private final MigrationException[] EXCEPTIONS = new MigrationException[ERRORS.length];

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void before() {
        int i = 0;

        for (final String msg : MESSAGES) {
            if (msg.startsWith("warning")) {
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
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.EXPORT);

        Assert.assertThat(REPORT.getOperation(), Matchers.equalTo(MigrationOperation.EXPORT));
        Assert.assertThat(REPORT.getStartTime(), Matchers.greaterThan(0L));
        Assert.assertThat(REPORT.getEndTime(), Matchers.equalTo(-1L));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testConstructorWithImportOperation() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT);

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

        new MigrationReportImpl(null);
    }

    @Test
    public void testConstructorWithReport() throws Exception {
        final MigrationReportImpl REPORT2 = new MigrationReportImpl(MigrationOperation.IMPORT,
                REPORT);

        Assert.assertThat(REPORT2.getOperation(), Matchers.equalTo(MigrationOperation.IMPORT));
        Assert.assertThat(REPORT2.getStartTime(), Matchers.equalTo(REPORT.getStartTime()));
        Assert.assertThat(REPORT.getEndTime(), Matchers.equalTo(-1L));
        Assert.assertThat(REPORT2.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT2.warnings()
                .map(MigrationWarning::getMessage)
                .toArray(String[]::new), Matchers.arrayContaining(MESSAGES));
    }

    @Test
    public void testConstructorWithUncompletedReport() throws Exception {
        REPORT.doAfterCompletion(r -> r.record(new MigrationWarning("final warning")));

        final MigrationReportImpl REPORT2 = new MigrationReportImpl(MigrationOperation.IMPORT,
                REPORT);

        Assert.assertThat(REPORT2.getOperation(), Matchers.equalTo(MigrationOperation.IMPORT));
        Assert.assertThat(REPORT2.getStartTime(), Matchers.equalTo(REPORT.getStartTime()));
        Assert.assertThat(REPORT.getEndTime(), Matchers.equalTo(-1L));
        Assert.assertThat(REPORT2.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT2.warnings()
                        .map(MigrationWarning::getMessage)
                        .toArray(String[]::new),
                Matchers.arrayContaining(ArrayUtils.add(MESSAGES, "final warning")));
    }

    @Test
    public void testRecordWarning() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT);
        final MigrationWarning WARNING = new MigrationWarning("warning");

        REPORT.record(WARNING);

        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(true));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.getWarnings(), Matchers.contains(WARNING));
    }

    @Test
    public void testRecordWarningWithNull() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null warning"));

        REPORT.record((MigrationWarning) null);
    }

    @Test
    public void testRecordError() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT);
        final MigrationException ERROR = new MigrationException("error");

        REPORT.record(ERROR);

        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));
        Assert.assertThat(REPORT.errors()
                .toArray(), Matchers.arrayContaining(ERROR));
    }

    @Test
    public void testRecordErrorWithNull() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null error"));

        REPORT.record((MigrationException) null);
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
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT);

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
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT);

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
    public void testGetWarningsWhenNoneRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT);

        Assert.assertThat(REPORT.getWarnings()
                .isEmpty(), Matchers.equalTo(true));
    }

    @Test
    public void testGetWarningsWhenSomeRecorded() throws Exception {
        Assert.assertThat(REPORT.getWarnings()
                .stream()
                .map(MigrationWarning::getMessage)
                .toArray(), Matchers.arrayContaining(WARNINGS));
    }

    @Test
    public void testGetWarningsDoesNotCallRegisteredCode() throws Exception {
        final Consumer<MigrationReport> CODE = Mockito.mock(Consumer.class);

        REPORT.doAfterCompletion(CODE);
        REPORT.getWarnings();

        Mockito.verify(CODE, Mockito.never())
                .accept(Mockito.any());
    }

    @Test
    public void testWasSuccessfulWhenNoErrorsOrWarningsAreRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT);

        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(true));
    }

    @Test
    public void testWasSuccessfulWhenBothWarningsAndErrorsAreRecorded() throws Exception {
        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(false));
    }

    @Test
    public void testWasSuccessfulWhenOnlyWarningsAreRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT);

        REPORT.record(new MigrationWarning("warning"));

        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(true));
    }

    @Test
    public void testWasSuccessfulWhenOnlyErrorsRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT);

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
    public void testHasErrorsWhenBothWarningsAndErrorsAreRecorded() throws Exception {
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));
    }

    @Test
    public void testHasErrorsWhenNoneRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT);

        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
    }

    @Test
    public void testHasErrorsWhenOnlyWarningsAreRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT);

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
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT);

        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testHasWarningsWhenOnlyErrorsAreRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT);

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
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT);

        REPORT.verifyCompletion();
    }

    @Test
    public void testVerifyCompletionWhenOneErrorIsRecorded() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT);
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
