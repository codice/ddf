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
package ddf.catalog.util.impl;

import static org.apache.commons.lang.Validate.notNull;

import com.google.common.util.concurrent.Futures;
import ddf.catalog.source.Source;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the last known {@link SourceAvailability} of a {@link Source} and can be used to recheck
 * the availability and cancel the current availability check
 */
public class CachedSourceAvailability {

  private static final Logger LOGGER = LoggerFactory.getLogger(CachedSourceAvailability.class);

  /** {@link Optional#empty()} indicates that an availability check has not yet completed */
  private Optional<SourceAvailability> sourceAvailabilityOptional;

  private volatile Future isAvailableFuture;

  public CachedSourceAvailability() {
    sourceAvailabilityOptional = Optional.empty();
    isAvailableFuture = Futures.immediateCancelledFuture();
  }

  public void cancel() {
    isAvailableFuture.cancel(true);
    sourceAvailabilityOptional = Optional.empty();
  }

  /**
   * Starts a process to recheck the availability if it is not currently being checked and if has
   * not been checked within the last {@code timeoutMinutes} minutes.
   */
  public void recheckAvailability(final Source source, final ExecutorService executorService) {
    notNull(source);
    final String sourceId = source.getId();

    handleLastAvailabilityCheckResult(sourceId);
    LOGGER.trace("Starting a process to check the availability of source id={}", sourceId);
    isAvailableFuture = executorService.submit(() -> updateAvailability(source));
  }

  private void handleLastAvailabilityCheckResult(String sourceId) {
    try {
      isAvailableFuture.get(0, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      isAvailableFuture.cancel(true);
      LOGGER.debug(
          "Unable to check the availability of source id={} within the polling interval", sourceId);
      sourceAvailabilityOptional = Optional.of(new SourceAvailability(SourceStatus.TIMEOUT));
    } catch (CancellationException e) {
      isAvailableFuture.cancel(true);
      LOGGER.debug("Unable to check the availability of source id={}", sourceId, e);
    } catch (RuntimeException | ExecutionException e) {
      isAvailableFuture.cancel(true);
      LOGGER.debug("Exception checking the availability of source id={}", sourceId, e);
      sourceAvailabilityOptional = Optional.of(new SourceAvailability(SourceStatus.EXCEPTION));
    } catch (InterruptedException e) {
      isAvailableFuture.cancel(true);
      LOGGER.debug("Interrupted while checking the availability of source id={}", sourceId);
      Thread.currentThread().interrupt();
    }
  }

  private void updateAvailability(final Source source) {
    final String sourceId = source.getId();
    SourceStatus newSourceStatus =
        source.isAvailable() ? SourceStatus.AVAILABLE : SourceStatus.UNAVAILABLE;
    LOGGER.trace("Successfully checked the availability of source id={}", sourceId);

    if (sourceAvailabilityOptional.isPresent()) {
      final SourceAvailability oldSourceAvailability = sourceAvailabilityOptional.get();

      if (oldSourceAvailability.getSourceStatus() != newSourceStatus) {
        LOGGER.info(
            "The source status of source id={} has changed to {}. The last known source status was {} at {}.",
            sourceId,
            newSourceStatus,
            oldSourceAvailability.getSourceStatus(),
            oldSourceAvailability.getSourceStatusTimeStamp());
      }

      final SourceAvailability newSourceAvailability = new SourceAvailability(newSourceStatus);
      sourceAvailabilityOptional = Optional.of(newSourceAvailability);
    } else {
      final SourceAvailability initialSourceAvailability = new SourceAvailability(newSourceStatus);
      LOGGER.info(
          "Checked the availability of source id={} for the first time. Setting the source status to {}",
          sourceId,
          initialSourceAvailability.getSourceStatus());
      sourceAvailabilityOptional = Optional.of(initialSourceAvailability);
    }
  }

  public Optional<SourceAvailability> getSourceAvailability() {
    return sourceAvailabilityOptional;
  }
}
