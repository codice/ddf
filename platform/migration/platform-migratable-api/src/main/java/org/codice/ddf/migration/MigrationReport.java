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
package org.codice.ddf.migration;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * The migration report provides information about the execution of a migration operation.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public interface MigrationReport {
    /**
     * Gets the type of migration operation this report is associated with.
     *
     * @return the type of migration operation this report is associated with
     */
    public MigrationOperation getOperation();

    /**
     * Records a warning that occurred during the migration report.
     *
     * @param w the warning to record
     * @return this for chaining
     * @throws IllegalArgumentException if <code>w</code> is <code>null</code>
     */
    public MigrationReport record(MigrationWarning w);

    /**
     * Records an error that occurred during the migration report.
     * <p>
     * Recorded errors will be thrown back when {@link #verifyCompletion()} is called at the end of
     * the operation.
     *
     * @param e the error to record
     * @return this for chaining
     * @throws IllegalArgumentException if <code>e</code> is <code>null</code>
     */
    public MigrationReport record(MigrationException e);

    /**
     * Registers code to be invoked at the completion of the migration operation.
     *
     * @param code the code to execute which will receive this report in parameter where
     *          additional errors and/or warnings can be registered
     * @return this for chaining
     * @throws IllegalArgumentException if <code>code</code> is <code>null</code>
     */
    public MigrationReport doAfterCompletion(Consumer<MigrationReport> code);

    /**
     * Retrieves all errors recorded by the operation that generated this migration report.
     *
     * @return a stream of all recorded errors (may be empty)
     */
    public Stream<MigrationException> errors();

    /**
     * Retrieves all warnings recorded by the operation that generated this migration report.
     *
     * @return a stream of all recorded warnings (may be empty)
     */
    public Stream<MigrationWarning> warnings();

    /**
     * Retrieves all warnings recorded by the operation that generated this migration report.
     *
     * @return an unmodifiable collections of all recorded warnings (may be empty)
     */
    public Collection<MigrationWarning> getWarnings();

    /**
     * Checks if the operation that generated this migration report was successful or not.
     * <p>
     * <i>Note:</i> A successful operation might still report warnings.
     * <p>
     * Invoking this method will call all registered verifiers first.
     *
     * @return <code>true</code> if the operation was successfull; <code>false</code> if not
     */
    public boolean wasSuccessful();

    /**
     * Checks if the operation that generated this migration recorded any warnings.
     * <p>
     * Invoking this method will call all registered verifiers first.
     *
     * @return <code>true</code> if the operation recorded at least one warning; <code>false</code> if not
     */
    public boolean hasWarnings();

    /**
     * Checks if the operation that generated this migration recorded any errors.
     * <p>
     * Invoking this method will call all registered verifiers first.
     *
     * @return <code>true</code> if the operation recorded at least one error; <code>false</code> if not
     */
    public boolean hasErrors();

    /**
     * Verifies if the operation that generated this migration report completed successfully.
     * <p>
     * Invoking this method will call all registered verifiers first.
     *
     * @throws MigrationException         if a single error occurred during the operation (throws that
     *                                    error back)
     * @throws MigrationCompoundException if more than one error occurred during the operation
     */
    public void verifyCompletion() throws MigrationException;
}
