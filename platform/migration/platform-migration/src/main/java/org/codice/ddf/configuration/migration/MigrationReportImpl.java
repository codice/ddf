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

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.MigrationCompoundException;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The migration report provides ways to aggregate warnings and errors related to migration operations.
 */
public class MigrationReportImpl implements MigrationReport {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(MigrationReportImpl.class);

    private final Set<MigrationWarning> warnings = new LinkedHashSet<>(); // to prevent duplicated and maintain order

    private final Set<MigrationException> errors = new LinkedHashSet<>(); // to prevent duplicated and maintain order

    private final Deque<Consumer<MigrationReport>> codes = new LinkedList<>();

    private final MigrationOperation operation;

    public MigrationReportImpl(MigrationOperation operation) {
        Validate.notNull(operation, "invalid null operation");
        this.operation = operation;
    }

    @Override
    public MigrationOperation getOperation() {
        return operation;
    }

    @Override
    public MigrationReportImpl record(MigrationWarning w) {
        Validate.notNull(w, "invalid null warning");
        warnings.add(w);
        LOGGER.debug("migration warning: {}", w);
        return this;
    }

    @Override
    public MigrationReportImpl record(MigrationException e) {
        Validate.notNull(e, "invalid null error");
        errors.add(e);
        LOGGER.info("migration error: ", e);
        return this;
    }

    @Override
    public MigrationReport doAfterCompletion(Consumer<MigrationReport> code) {
        Validate.notNull(code, "invalid null code");
        codes.add(code);
        return this;
    }

    @Override
    public Stream<MigrationException> errors() {
        return errors.stream();
    }

    @Override
    public Stream<MigrationWarning> warnings() {
        return warnings.stream();
    }

    @Override
    public Collection<MigrationWarning> getWarnings() {
        return Collections.unmodifiableCollection(warnings);
    }

    @Override
    public boolean wasSuccessful() {
        runCodes();
        return errors.isEmpty();
    }

    public boolean hasWarnings() {
        runCodes();
        return !warnings.isEmpty();
    }

    public boolean hasErrors() {
        runCodes();
        return !errors.isEmpty();
    }

    @Override
    public void verifyCompletion() throws MigrationException {
        runCodes();
        if (errors.isEmpty()) {
            return;
        } else if (errors.size() == 1) {
            throw errors.iterator().next();
        }
        throw new MigrationCompoundException(errors);
    }

    private void runCodes() {
        while (codes.isEmpty()) {
            codes.removeFirst().accept(this);
        }
    }
}
