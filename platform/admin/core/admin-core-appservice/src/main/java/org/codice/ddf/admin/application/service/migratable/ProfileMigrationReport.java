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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationInformation;
import org.codice.ddf.migration.MigrationMessage;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.util.function.ThrowingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfileMigrationReport implements MigrationReport {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProfileMigrationReport.class);

  private static final String ATTEMPTS_SUFFIX = " attempt)";

  private final MigrationReport report;

  /**
   * Keeps track of the number of times a particular operation was attempted on a particular
   * feature.
   */
  private final Map<Operation, Map<String, AtomicInteger>> featuresAttempts =
      new EnumMap<>(Operation.class);

  /**
   * Keeps track of the number of times a particular operation was attempted on a particular bundle.
   */
  private final Map<Operation, Map<String, AtomicInteger>> bundlesAttempts =
      new EnumMap<>(Operation.class);

  private final boolean finalAttempt;

  /** Tracks whether at least one recorded error was suppressed. */
  private boolean suppressedErrors = false;

  /** Tracks whether at least one task was recorded as part of this report. */
  private boolean recordedTasks = false;

  /**
   * Creates a new profile migration report with the provided migration report.
   *
   * @param report the migration report associated with this report
   * @param finalAttempt <code>true</code> if this report is associated with a final attempt; <code>
   *     false</code> otherwise
   * @throws IllegalArgumentException if <code>report</code> is <code>null</code>
   */
  public ProfileMigrationReport(MigrationReport report, boolean finalAttempt) {
    Validate.notNull(report, "invalid null report");
    this.report = report;
    this.finalAttempt = finalAttempt;
  }

  public static String ordinal(int i) {
    final String[] suffixes =
        new String[] {"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};

    switch (i % 100) {
      case 11:
      case 12:
      case 13:
        return i + "th";
      default:
        return i + suffixes[i % 10];
    }
  }

  /**
   * Checks if this report is associated with a final attempt in which case errors recorded via
   * {@link #recordOnFinalAttempt(MigrationException)} should never be suppressed.
   *
   * @return <code>true</code> if this report is associated with a final attempt; <code>false
   *     </code> otherwise
   */
  public boolean isFinalAttempt() {
    return finalAttempt;
  }

  /**
   * Checks if at least one recorded error was suppressed any errors because the report was not
   * associated with a final attempt.
   *
   * @return <code>true</code> if at least one recorded error was suppressed; <code>false</code>
   *     otherwise
   */
  public boolean hasSuppressedErrors() {
    return suppressedErrors;
  }

  /**
   * Checks if at least one task was recorded as part of this report.
   *
   * @return <code>true</code> if at least one task was executed as part of this report; <code>false
   *     </code> otherwise
   */
  public boolean hasRecordedTasks() {
    return recordedTasks;
  }

  /**
   * Indicates a task is being recorded as part of this report.
   *
   * @return this for chaining
   */
  @SuppressWarnings(
      "PMD.DefaultPackage" /* designed to be called by TaskList within this package */)
  ProfileMigrationReport recordTask() {
    this.recordedTasks = true;
    return this;
  }

  /**
   * Records the specified exception as an error only if this is the report is associated with a
   * final attempt otherwise suppress it.
   *
   * @param e the error to be recorded only if the report is associated with a final attempt
   * @return this for chaining
   */
  public ProfileMigrationReport recordOnFinalAttempt(MigrationException e) {
    if (finalAttempt) {
      return record(e);
    } // else - suppressed it
    this.suppressedErrors = true;
    LOGGER.debug("Suppressed error: ", e);
    return this;
  }

  /**
   * Gets a trace string representing the number of attempts for a given feature operation.
   *
   * @param op the operation for which to get an attempt trace string
   * @param id the attempt id for which to get an attempt trace string
   * @return the corresponding attempt trace string
   */
  public String getFeatureAttemptString(Operation op, String id) {
    final int attempt =
        featuresAttempts
            .computeIfAbsent(op, o -> new HashMap<>())
            .computeIfAbsent(id, n -> new AtomicInteger(0))
            .incrementAndGet();

    return (attempt > 1)
        ? " (" + ProfileMigrationReport.ordinal(attempt) + ProfileMigrationReport.ATTEMPTS_SUFFIX
        : "";
  }

  /**
   * Gets a trace string representing the number of attempts for a given bundle operation.
   *
   * @param op the operation for which to get an attempt trace string
   * @param id the attempt id for which to get an attempt trace string
   * @return the corresponding attempt trace string
   */
  public String getBundleAttemptString(Operation op, String id) {
    final int attempt =
        bundlesAttempts
            .computeIfAbsent(op, o -> new HashMap<>())
            .computeIfAbsent(id, n -> new AtomicInteger(0))
            .incrementAndGet();

    return (attempt > 1)
        ? " (" + ProfileMigrationReport.ordinal(attempt) + ProfileMigrationReport.ATTEMPTS_SUFFIX
        : "";
  }

  @Override
  public MigrationOperation getOperation() {
    return report.getOperation();
  }

  @Override
  public Instant getStartTime() {
    return report.getStartTime();
  }

  @Override
  public Optional<Instant> getEndTime() {
    return report.getEndTime();
  }

  @Override
  public ProfileMigrationReport record(String msg) {
    report.record(msg);
    return this;
  }

  @Override
  public MigrationReport record(String format, @Nullable Object... args) {
    report.record(format, args);
    return this;
  }

  @Override
  public ProfileMigrationReport record(MigrationMessage msg) {
    report.record(msg);
    return this;
  }

  @Override
  public ProfileMigrationReport doAfterCompletion(Consumer<MigrationReport> code) {
    report.doAfterCompletion(code);
    return this;
  }

  @Override
  public Stream<MigrationMessage> messages() {
    return report.messages();
  }

  @Override
  public Stream<MigrationException> errors() {
    return report.errors();
  }

  @Override
  public Stream<MigrationWarning> warnings() {
    return report.warnings();
  }

  @Override
  public Stream<MigrationInformation> infos() {
    return report.infos();
  }

  @Override
  public boolean wasSuccessful() {
    return report.wasSuccessful();
  }

  @Override
  public boolean wasSuccessful(@Nullable Runnable code) {
    return report.wasSuccessful(code);
  }

  @Override
  public boolean wasIOSuccessful(@Nullable ThrowingRunnable<IOException> code) throws IOException {
    return report.wasIOSuccessful(code);
  }

  @Override
  public boolean hasInfos() {
    return report.hasInfos();
  }

  @Override
  public boolean hasWarnings() {
    return report.hasWarnings();
  }

  @Override
  public boolean hasErrors() {
    return report.hasErrors();
  }

  @Override
  public void verifyCompletion() {
    report.verifyCompletion();
  }
}
