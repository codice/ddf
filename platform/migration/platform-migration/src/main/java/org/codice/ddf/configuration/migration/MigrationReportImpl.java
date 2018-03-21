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

import java.io.IOException;
import java.time.Instant;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
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

/**
 * The migration report provides ways to aggregate warnings and errors related to migration
 * operations.
 */
public class MigrationReportImpl implements MigrationReport {
  private static final Logger LOGGER = LoggerFactory.getLogger(MigrationReportImpl.class);

  private final Optional<Consumer<MigrationMessage>> consumer;

  private final Set<MigrationMessage> messages;

  private final Deque<Consumer<MigrationReport>> codes = new LinkedList<>();

  private final MigrationOperation operation;

  private final Instant start;

  private int numInfos = 0;

  private int numWarnings = 0;

  private int numErrors = 0;

  private Optional<Instant> end = Optional.empty();

  /**
   * Creates a new migration report.
   *
   * @param operation the type of migration operation for this report
   * @param consumer an optional consumer to call whenever a new migration message is recorded
   *     during the operation
   * @throws IllegalArgumentException if <code>operation</code> is <code>null</code>
   */
  public MigrationReportImpl(
      MigrationOperation operation, Optional<Consumer<MigrationMessage>> consumer) {
    Validate.notNull(operation, "invalid null operation");
    this.operation = operation;
    this.consumer = consumer;
    this.start = Instant.now();
    this.messages = new LinkedHashSet<>(); // LinkedHashSet to prevent duplicate and maintain order
  }

  @Override
  public MigrationOperation getOperation() {
    return operation;
  }

  @Override
  public Instant getStartTime() {
    return start;
  }

  @Override
  public Optional<Instant> getEndTime() {
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
    LOGGER.debug("migration {}: {}", level, msg, msg); // 2nd 'msg' is for stack trace
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
    final int nerrs = this.numErrors;

    if (code != null) {
      code.run();
    }
    return (this.numErrors == nerrs);
  }

  @Override
  public boolean wasIOSuccessful(ThrowingRunnable<IOException> code) throws IOException {
    final int nerrs = this.numErrors;

    if (code != null) {
      code.run();
    }
    return (this.numErrors == nerrs);
  }

  @Override
  public boolean hasInfos() {
    runCodes();
    return (numInfos > 0);
  }

  @Override
  public boolean hasWarnings() {
    runCodes();
    return (numWarnings > 0);
  }

  @Override
  public boolean hasErrors() {
    runCodes();
    return (numErrors > 0);
  }

  @Override
  public void verifyCompletion() {
    runCodes();
    if (numErrors == 0) {
      return;
    }
    final Iterator<MigrationException> i = errors().iterator(); // preserve order
    final MigrationException e = i.next(); // will always be there since numErrors is not 0

    i.forEachRemaining(e::addSuppressed);
    throw e;
  }

  @SuppressWarnings(
      "PMD.DefaultPackage" /* designed to be called from ConfigurationMigrationManager within this package */)
  MigrationReportImpl end() {
    runCodes();
    this.end = Optional.of(Instant.now());
    return this;
  }

  private void runCodes() {
    while (!codes.isEmpty()) {
      codes.removeFirst().accept(this);
    }
  }
}
