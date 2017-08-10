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
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.MigrationCompoundException;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationInformation;
import org.codice.ddf.migration.MigrationMessage;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.util.function.ERunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The migration report provides ways to aggregate warnings and errors related to migration operations.
 */
public class MigrationReportImpl implements MigrationReport {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationReportImpl.class);

    private final Optional<Consumer<MigrationMessage>> consumer;

    private final Set<MigrationMessage> messages;

    private final Deque<Consumer<MigrationReport>> codes = new LinkedList<>();

    private final MigrationOperation operation;

    private final long start;

    private int numInfos = 0;

    private int numWarnings = 0;

    private int numErrors = 0;

    private long end = -1L;

    /**
     * Creates a new migration report.
     *
     * @param operation the type of migration operation for this report
     * @param consumer  an optional consumer to call whenever a new migration message is recorded
     *                  during the operation
     * @throws IllegalArgumentException if <code>operation</code> is <code>null</code>
     */
    public MigrationReportImpl(MigrationOperation operation,
            Optional<Consumer<MigrationMessage>> consumer) {
        Validate.notNull(operation, "invalid null operation");
        this.operation = operation;
        this.consumer = consumer;
        this.start = System.currentTimeMillis();
        this.messages =
                new LinkedHashSet<>(); // LinkedHashSet to prevent duplicate and maintain order
    }

    /**
     * Creates a new migration report by transferring the errors and warnings from the provided report
     * as warnings for this report. Transfer also the start time.
     *
     * @param operation the type of migration operation for this report
     * @param report    the report to be transferred over
     * @param consumer  an optional consumer to call whenever a new migration message is recorded
     *                  during the operation
     * @throws IllegalArgumentException if <code>operation</code> or <code>report</code> is <code>null</code>
     */
    public MigrationReportImpl(MigrationOperation operation, MigrationReportImpl report,
            Optional<Consumer<MigrationMessage>> consumer) {
        Validate.notNull(operation, "invalid null operation");
        Validate.notNull(report, "invalid null report");
        report.runCodes(); // to get all errors and warnings recorded
        this.messages = report.messages.stream()
                .map(MigrationMessage::downgradeToWarning)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toCollection(LinkedHashSet::new)); // LinkedHashSet to prevent duplicate and maintain order
        this.numWarnings = messages.size();
        this.operation = operation;
        this.consumer = consumer;
        this.start = report.getStartTime();
    }

    @Override
    public MigrationOperation getOperation() {
        return operation;
    }

    @Override
    public long getStartTime() {
        return start;
    }

    @Override
    public long getEndTime() {
        return end;
    }

    @Override
    public MigrationReportImpl record(MigrationMessage msg) {
        Validate.notNull(msg, "invalid null message");
        final String level;

        if (msg instanceof MigrationException) {
            this.numErrors++;
            level = "error";
        } else if (msg instanceof MigrationWarning) {
            this.numWarnings++;
            level = "warning";
        } else if (msg instanceof MigrationInformation) {
            this.numInfos++;
            level = "info";
        } else {
            level = "message";
        }
        LOGGER.debug("migration {}: {}", level, msg);
        messages.add(msg);
        consumer.ifPresent(c -> c.accept(msg));
        return this;
    }

    @Override
    public MigrationReport doAfterCompletion(Consumer<MigrationReport> code) {
        Validate.notNull(code, "invalid null code");
        codes.add(code);
        return this;
    }

    @Override
    public Stream<MigrationMessage> messages() {
        return messages.stream();
    }

    @Override
    public boolean wasSuccessful() {
        runCodes();
        return (numErrors == 0);
    }

    @Override
    public boolean wasSuccessful(Runnable code) {
        final int numErrors = this.numErrors;

        if (code != null) {
            code.run();
        }
        return (this.numErrors == numErrors);
    }

    @Override
    public boolean wasIOSuccessful(ERunnable<IOException> code) throws IOException {
        final int numErrors = this.numErrors;

        if (code != null) {
            code.run();
        }
        return (this.numErrors == numErrors);
    }

    public boolean hasInfos() {
        runCodes();
        return (numInfos > 0);
    }

    public boolean hasWarnings() {
        runCodes();
        return (numWarnings > 0);
    }

    public boolean hasErrors() {
        runCodes();
        return (numErrors > 0);
    }

    @Override
    public void verifyCompletion() throws MigrationException {
        runCodes();
        if (numErrors == 0) {
            return;
        } else if (numErrors == 1) {
            throw errors().findAny()
                    .get(); // will never be null since there is 1
        }
        throw new MigrationCompoundException(errors().collect(Collectors.toList())); // preserve order
    }

    MigrationReportImpl end() {
        runCodes();
        this.end = System.currentTimeMillis();
        return this;
    }

    private void runCodes() {
        while (!codes.isEmpty()) {
            codes.removeFirst()
                    .accept(this);
        }
    }
}
