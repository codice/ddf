package ddf.catalog.util.impl;

import static org.apache.commons.lang.Validate.notNull;

import com.google.common.util.concurrent.Futures;
import ddf.catalog.source.Source;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Callable;
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

  private Future<Boolean> isAvailableFuture;

  private Future watcherFuture;

  public CachedSourceAvailability() {
    sourceAvailabilityOptional = Optional.empty();
    isAvailableFuture = Futures.immediateCancelledFuture();
    watcherFuture = Futures.immediateCancelledFuture();
  }

  public void cancel() {
    watcherFuture.cancel(true);
    isAvailableFuture.cancel(true);
    sourceAvailabilityOptional = Optional.empty();
  }

  /**
   * Starts a process to recheck the availability if it is not currently being checked and if has
   * not been checked within the last {@code timeoutMinutes} minutes.
   */
  public void recheckAvailability(
      final Source source,
      final long timeout,
      final TimeUnit timeoutTimeUnit,
      final ExecutorService executorService) {
    notNull(source);
    final String sourceId = source.getId();

    if (!watcherFuture.isDone()) {
      LOGGER.debug(
          "A process has already been started to check the availability of source id={}. This is expected when the source is recently modified or bound. Not starting a new check",
          sourceId);
      return;
    }

    if (!isAvailableFuture.isDone()) {
      isAvailableFuture.cancel(true);
    }

    if (sourceAvailabilityOptional.isPresent()) {
      final Instant sourceStatusTimeStamp = sourceAvailabilityOptional.get().getSourceStatusTimeStamp();
      final Instant nextEarliestRecheckTime = sourceStatusTimeStamp.plusMillis(timeout);
      final Instant now = Instant.now();

      if (now.isBefore(nextEarliestRecheckTime)) {
        // Not rechecking the availability of source id={} because it has been checked in the last
        // {} minutes
        // Don't log anything here because this happens really often.
        return;
      }
    }

    LOGGER.trace("Starting a process to check the availability of source id={}", sourceId);
    isAvailableFuture = executorService.submit((Callable<Boolean>) source::isAvailable);
    watcherFuture =
        executorService.submit(
            () ->
                watchAndUpdateAvailability(isAvailableFuture, sourceId, timeout, timeoutTimeUnit));
  }

  /**
   * Availability checks will be cancelled if they take longer than the poll interval, and the
   * {@link SourceStatus} will be updated to {@link SourceStatus#TIMEOUT}.
   */
  private void watchAndUpdateAvailability(
      final Future<Boolean> isAvailableFuture,
      final String sourceId,
      final long timeout,
      final TimeUnit timeoutTimeUnit) {
    SourceStatus newSourceStatus;

    try {
      final boolean isAvailable = isAvailableFuture.get(timeout, timeoutTimeUnit);
      LOGGER.trace("Successfully checked the availability of source id={}", sourceId);
      newSourceStatus = isAvailable ? SourceStatus.AVAILABLE : SourceStatus.UNAVAILABLE;
    } catch (TimeoutException e) {
      isAvailableFuture.cancel(true);
      LOGGER.debug(
          "Unable to check the availability of source id={} within {} {}. Cancelling the check",
          sourceId,
          timeout,
          timeoutTimeUnit);
      newSourceStatus = SourceStatus.TIMEOUT;
    } catch (CancellationException e) {
      isAvailableFuture.cancel(true);
      LOGGER.debug("Unable to check the availability of source id={}", sourceId, e);
      return;
    } catch (RuntimeException | ExecutionException e) {
      isAvailableFuture.cancel(true);
      LOGGER.debug("Exception checking the availability of source id={}", sourceId, e);
      newSourceStatus = SourceStatus.EXCEPTION;
    } catch (InterruptedException e) {
      isAvailableFuture.cancel(true);
      LOGGER.debug("Interrupted while checking the availability of source id={}", sourceId);
      Thread.currentThread().interrupt();
      return;
    }

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
