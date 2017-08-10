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

import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.codice.ddf.util.function.ERunnable;

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
     * Gets the time the corresponding migration operation started.
     *
     * @return the time from the epoch the report migration operation started
     */
    public long getStartTime();

    /**
     * Gets the time the corresponding migration operation ended.
     *
     * @return the time from the epoch the report migration operation ended or <code>-1L</code> if it
     * is still in operation
     */
    public long getEndTime();

    /**
     * Records an informational message that occurred during the migration report.
     * <p>
     * <i>Note:</i> This is a short for <code>record(new MigrationInformation(msg))</code>.
     *
     * @param msg the information message to record
     * @return this for chaining
     * @throws IllegalArgumentException if <code>msg</code> is <code>null</code>
     */
    public default MigrationReport record(String msg) {
        Validate.notNull(msg, "invalid null message");
        return record(new MigrationInformation(msg));
    }

    /**
     * Records a message that occurred during the migration report.
     * <p>
     * Recorded errors (i.e. MigrationException} will be thrown back when {@link #verifyCompletion()}
     * is called at the end of the operation.
     *
     * @param msg the message to record
     * @return this for chaining
     * @throws IllegalArgumentException if <code>msg</code> is <code>null</code>
     */
    public MigrationReport record(MigrationMessage msg);

    /**
     * Registers code to be invoked at the completion of the migration operation.
     *
     * @param code the code to execute which will receive this report in parameter where
     *             additional errors and/or warnings can be registered
     * @return this for chaining
     * @throws IllegalArgumentException if <code>code</code> is <code>null</code>
     */
    public MigrationReport doAfterCompletion(Consumer<MigrationReport> code);

    /**
     * Retrieves all messages recorded by the operation that generated this migration report.
     *
     * @return a stream of all recorded messages (may be empty)
     */
    public Stream<MigrationMessage> messages();

    /**
     * Retrieves all errors recorded by the operation that generated this migration report.
     *
     * @return a stream of all recorded errors (may be empty)
     */
    public default Stream<MigrationException> errors() {
        return messages().filter(MigrationException.class::isInstance)
                .map(MigrationException.class::cast);
    }

    /**
     * Retrieves all warnings recorded by the operation that generated this migration report.
     *
     * @return a stream of all recorded warnings (may be empty)
     */
    public default Stream<MigrationWarning> warnings() {
        return messages().filter(MigrationWarning.class::isInstance)
                .map(MigrationWarning.class::cast);
    }

    /**
     * Retrieves all informational messages recorded by the operation that generated this migration
     * report.
     *
     * @return a stream of all recorded info messages (may be empty)
     */
    public default Stream<MigrationInformation> infos() {
        return messages().filter(MigrationInformation.class::isInstance)
                .map(MigrationInformation.class::cast);
    }

    /**
     * Checks if the operation that generated this migration report was successful or not.
     * <p>
     * <i>Note:</i> A successful operation might still report warnings.
     * <p>
     * Invoking this method will call all registered code via {@link #doAfterCompletion(Consumer)} first.
     *
     * @return <code>true</code> if the operation was successfull; <code>false</code> if not
     */
    public boolean wasSuccessful();

    /**
     * Runs the specified code and report whether or not it was successful.
     * <p>
     * <i>Note:</i> This method will only account for errors generated from the point where the
     * provided code is called to the moment it terminates.
     *
     * @param code the code to run
     * @return <code>true</code> if no new errors were recorded while running the provided code;
     * <code>false</code> if at least one error was recorded
     */
    public boolean wasSuccessful(@Nullable Runnable code);

    /**
     * Runs the specified code and report whether or not it was successful.
     * <p>
     * <i>Note:</i> This method will only account for errors generated from the point where the
     * provided code is called to the moment it terminates.
     *
     * @param code the code to run
     * @return <code>true</code> if no new errors were recorded while running the provided code;
     * <code>false</code> if at least one error was recorded
     * @throws IOException if the code executed throws it
     */
    public boolean wasIOSuccessful(@Nullable ERunnable<IOException> code) throws IOException;

    /**
     * Checks if the operation that generated this migration recorded any information messages.
     * <p>
     * Invoking this method will call all registered code via {@link #doAfterCompletion(Consumer)} first.
     *
     * @return <code>true</code> if the operation recorded at least one informational message;
     * <code>false</code> if not
     */
    public boolean hasInfos();

    /**
     * Checks if the operation that generated this migration recorded any warnings.
     * <p>
     * Invoking this method will call all registered code via {@link #doAfterCompletion(Consumer)} first.
     *
     * @return <code>true</code> if the operation recorded at least one warning; <code>false</code> if not
     */
    public boolean hasWarnings();

    /**
     * Checks if the operation that generated this migration recorded any errors.
     * <p>
     * Invoking this method will call all registered code via {@link #doAfterCompletion(Consumer)} first.
     *
     * @return <code>true</code> if the operation recorded at least one error; <code>false</code> if not
     */
    public boolean hasErrors();

    /**
     * Verifies if the operation that generated this migration report completed successfully.
     * <p>
     * Invoking this method will call all registered code via {@link #doAfterCompletion(Consumer)} first.
     *
     * @throws MigrationException         if a single error occurred during the operation (throws that
     *                                    error back)
     * @throws MigrationCompoundException if more than one error occurred during the operation
     */
    public void verifyCompletion() throws MigrationException;
}
