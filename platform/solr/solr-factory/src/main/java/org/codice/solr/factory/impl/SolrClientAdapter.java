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
package org.codice.solr.factory.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closeables;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.jodah.failsafe.AsyncFailsafe;
import net.jodah.failsafe.ExecutionContext;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.SyncFailsafe;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrException;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.codice.solr.client.solrj.UnavailableSolrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides an implementation for the {@link org.codice.solr.client.solrj.SolrClient}
 * interface that adapts to {@link SolrClient}.
 */
// final is required for security reasons as this class use the Access Controller to extends its
// privileges
public final class SolrClientAdapter extends SolrClientProxy
    implements org.codice.solr.client.solrj.SolrClient {
  /** Enumeration representing the various states. */
  private enum State {
    CLOSED,
    CREATING,
    CONNECTING,
    CONNECTED
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrClientAdapter.class);

  private static final String FAILED_TO_PING = "Solr({}): Proxy failed to ping; {}";

  private static final String FAILED_TO_PING_WITH_STATUS =
      "Solr({}): Proxy failed to ping Solr client; got status [{}]";

  private static final String OK_STATUS = "OK";

  private static final int THREAD_POOL_DEFAULT_SIZE = 128;

  private static final RetryPolicy
      ABORT_WHEN_INTERRUPTED_AND_RETRY_UNTIL_NO_ERROR_AND_A_CLIENT_IS_CREATED =
          new RetryPolicy()
              .retryIf(r -> !(r instanceof SolrClient))
              .retryOn(Throwable.class)
              .abortOn(InterruptedIOException.class, InterruptedException.class)
              .abortOn(VirtualMachineError.class)
              .withBackoff(10L, TimeUnit.MINUTES.toMillis(1L), TimeUnit.MILLISECONDS);

  private static final RetryPolicy ABORT_WHEN_INTERRUPTED_AND_RETRY_UNTIL_NO_ERROR =
      new RetryPolicy()
          .retryOn(Throwable.class)
          .abortOn(InterruptedIOException.class, InterruptedException.class)
          .abortOn(VirtualMachineError.class)
          .withBackoff(1L, TimeUnit.MINUTES.toSeconds(2L), TimeUnit.SECONDS);

  private static final long PING_MIN_FREQUENCY = TimeUnit.SECONDS.toMillis(10L);

  private static final long ERROR_MIN_FREQUENCY = TimeUnit.MINUTES.toMillis(1L);

  private static final ScheduledExecutorService SCHEDULED_EXECUTOR =
      SolrClientAdapter.createExecutor();

  private final transient Object lock = new Object();

  private final String core;

  private final transient Creator creator;

  private final transient AsyncFailsafe<SolrClient> createFailsafe;

  private final transient AsyncFailsafe<Void> pingFailsafe;

  private final transient Waiter waiter;

  private final transient Executor executor;

  private final transient Set<Listener> listeners = new CopyOnWriteArraySet<>();

  private final transient Queue<Initializer> initializers = new ConcurrentLinkedQueue<>();

  private final transient AtomicLong lastPing;

  private final transient AtomicLong lastCreateError;

  /**
   * The client to use when calling api methods.
   *
   * <p><i>Note:</i> writes are always protected by synchronization, reads are not.
   */
  private transient volatile SolrClient apiClient;

  /**
   * The client to use when calling pinging.
   *
   * <p><i>Note:</i> writes are always protected by synchronization, reads are not.
   */
  private transient volatile SolrClient pingClient;

  /**
   * The real created client. Will be <code>null</code> only while creating and after closing.
   *
   * <p><i>Note:</i> writes are always protected by synchronization, reads are not.
   */
  @Nullable private transient SolrClient realClient;

  /**
   * The current unavailable client. Will be <code>null</code> when we are available.
   *
   * <p><i>Note:</i> writes are always protected by synchronization, reads are not.
   */
  @Nullable private transient volatile UnavailableSolrClient unavailableClient;

  // writes are always protected by synchronization, reads are not
  private transient volatile State state;

  // writes and reads are always protected by synchronization
  @Nullable private transient Future<?> future;

  /**
   * Constructs a new client adapter for the specified code and using the specified creator to
   * create new Solr client instances.
   *
   * <p><i>Note:</i> There is no need to implement any retry behavior in the creator as this will be
   * handled by this class. Simply attempt the creation and fail fast. The creation of the client
   * can be interrupted (see {@link Thread#interrupted}) in which case it should attempt to stop the
   * creation as quickly as possible. It is acceptable for the creator to throw back any of the
   * following exceptions: {@link IOException}, {@link SolrServerException}, {@link SolrException},
   * {@link InterruptedException}, or {@link InterruptedIOException}. A retry will automatically be
   * triggered if returning <code>null</code> or any exceptions other than {@link
   * InterruptedException} or {@link InterruptedIOException} are thrown back.
   *
   * @param core the Solr core for which to create an adaptor
   * @param creator the creator to use for creating corresponding Solr clients
   * @throws IllegalArgumentException if <code>core</code> or <code>creator</code> is <code>null
   *     </code>
   */
  public SolrClientAdapter(String core, Creator creator) {
    this(core, creator, Failsafe::with, Failsafe::with);
  }

  @VisibleForTesting
  SolrClientAdapter(
      String core,
      Creator creator,
      Function<RetryPolicy, SyncFailsafe<SolrClient>> createFailsafeCreator,
      Function<RetryPolicy, SyncFailsafe<Void>> pingFailsafeCreator) {
    this(core, creator, createFailsafeCreator, pingFailsafeCreator, new Waiter(), new Executor());
  }

  @VisibleForTesting
  SolrClientAdapter(
      String core,
      Creator creator,
      Function<RetryPolicy, SyncFailsafe<SolrClient>> createFailsafeCreator,
      Function<RetryPolicy, SyncFailsafe<Void>> pingFailsafeCreator,
      Waiter waiter) {
    this(core, creator, createFailsafeCreator, pingFailsafeCreator, waiter, new Executor());
  }

  @VisibleForTesting
  SolrClientAdapter(
      String core,
      Creator creator,
      Function<RetryPolicy, SyncFailsafe<SolrClient>> createFailsafeCreator,
      Function<RetryPolicy, SyncFailsafe<Void>> pingFailsafeCreator,
      Executor executor) {
    this(core, creator, createFailsafeCreator, pingFailsafeCreator, new Waiter(), executor);
  }

  @VisibleForTesting
  SolrClientAdapter(
      String core,
      Creator creator,
      Function<RetryPolicy, SyncFailsafe<SolrClient>> createFailsafeCreator,
      Function<RetryPolicy, SyncFailsafe<Void>> pingFailsafeCreator,
      Waiter waiter,
      Executor executor) {
    Validate.notNull(core, "invalid null Solr core name");
    Validate.notNull(creator, "invalid null Solr creator");
    LOGGER.debug("Solr({}): Creating a Solr client adapter with creator [{}]", core, creator);
    this.core = core;
    this.creator = creator;
    this.createFailsafe =
        createFailsafeCreator
            .apply(
                SolrClientAdapter
                    .ABORT_WHEN_INTERRUPTED_AND_RETRY_UNTIL_NO_ERROR_AND_A_CLIENT_IS_CREATED)
            .with(SolrClientAdapter.SCHEDULED_EXECUTOR)
            .onRetry(this::logFailure)
            .onAbort(this::logInterruptionAndRecreate)
            .onFailure(this::logAndRecreateIfNotCancelled)
            .onSuccess(this::logAndSetConnecting);
    this.pingFailsafe =
        pingFailsafeCreator
            .apply(SolrClientAdapter.ABORT_WHEN_INTERRUPTED_AND_RETRY_UNTIL_NO_ERROR)
            .with(SolrClientAdapter.SCHEDULED_EXECUTOR)
            .onRetry(this::logFailure)
            .onAbort(this::logInterruptionAndReconnectIfStillConnecting)
            .onFailure(this::logAndReconnectIfNotCancelledAndStillConnecting)
            .onSuccess(this::logAndSetConnected);
    this.waiter = waiter;
    this.executor = executor;
    this.lastPing = new AtomicLong(System.currentTimeMillis());
    this.lastCreateError =
        new AtomicLong(); // set to beginning of time to make sure we log the first error
    this.unavailableClient =
        new UnavailableSolrClient(
            new UnavailableSolrException("initializing '" + core + "' client"));
    this.apiClient = unavailableClient;
    this.pingClient = unavailableClient;
    this.realClient = null;
    this.state = State.CREATING;
    this.future = null;
    setCreating(unavailableClient, false);
  }

  @Override
  protected SolrClient getProxiedClient() {
    if ((state != State.CONNECTED) && (state != State.CLOSED)) {
      // do a spot check to see if it suddenly became reachable
      try {
        checkIfReachable("from the API because it is currently unavailable");
        // if we get here then the ping was successful so make sure we move to connected
        setConnected(true);
        // fall-through to return the current one which should have been changed to the actual one
      } catch (UnavailableSolrException e) {
        // fall-through to return the current one which should still be an unavailable one
      }
    }
    return this.apiClient;
  }

  @Override
  protected <T> T handle(Code<T> code) throws SolrServerException, IOException {
    try {
      return code.invoke(getProxiedClient());
    } catch (UnavailableSolrException e) {
      throw e;
    } catch (SolrException e) {
      LOGGER.debug(
          "Solr({}): API failure with code [{}] and metadata [{}]; {}",
          core,
          e.code(),
          e.getMetadata(),
          e, // this will get the info logged on the first line
          e); // this one will get the stack trace logged after
      checkIfReachableAndChangeStateAccordingly(e, "from the API after an error was detected");
      throw e;
    } catch (SolrServerException | IOException e) {
      LOGGER.debug("Solr({}): API failure; {}", core, e, e);
      checkIfReachableAndChangeStateAccordingly(e, "from the API after an error was detected");
      throw e;
    }
  }

  @Override
  public final SolrClient getClient() {
    // returning this to make sure all calls from our SolrClient API will still be intercepted
    // when the client is retrieved and passed to a Solr request object
    // this should be temporary until we completely abstract out Solr with interfaces
    return this;
  }

  @Override
  public String getCore() {
    return core;
  }

  @Override
  @SuppressWarnings("squid:S2093" /* closing of real client handled by finalizeStateChange() */)
  public void close() throws IOException {
    try {
      if (state == State.CLOSED) {
        return;
      }
      final SolrClient previousClientToClose;
      final Future<?> futureToCancel;

      synchronized (lock) {
        if (state == State.CLOSED) { // already closed so bail
          return;
        }
        futureToCancel = future;
        previousClientToClose = realClient;
        LOGGER.debug("Solr({}): closing", core);
        this.unavailableClient =
            new UnavailableSolrClient(
                new UnavailableSolrException("'" + core + "' client was closed"));
        this.apiClient = unavailableClient;
        this.pingClient = unavailableClient;
        this.realClient = null;
        this.state = State.CLOSED;
        this.future = null;
        lock.notifyAll(); // wakeup those waiting for isAvailable(timeout)
      }
      finalizeStateChange(true, futureToCancel, previousClientToClose, false);
    } finally {
      listeners.clear();
      initializers.clear();
    }
  }

  @Override
  public boolean isAvailable() {
    // no need to account for state == State.CLOSED, since that would require synchronization which
    // we are trying to avoid here. Since by design, this class is dealing with background retries
    // when it is not available, this method doesn't have to do anything, As such, checking only for
    // CONNECTED would automatically yield false and return right away without doing anything else;
    // very close to a short-circuit.
    final boolean available = (state == State.CONNECTED);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "Solr({}): current availability is [{} = {}]",
          core,
          state,
          SolrClientAdapter.availableToString(available));
    }
    if (available && wasNotRecent(lastPing, SolrClientAdapter.PING_MIN_FREQUENCY)) {
      LOGGER.debug(
          "Solr({}): Proxy is starting a background task to ping the client because the last ping was too long ago",
          core);
      executor.submit(this::backgroundPing);
    }
    return available;
  }

  @Override
  public boolean isAvailable(long timeout, TimeUnit unit) throws InterruptedException {
    Validate.notNull(unit, "invalid null time unit");
    if (isAvailable()) { // quick check to avoid synchronization
      return true;
    }
    // letting now be recomputed by the timedWait() method allows us to better control testing
    long now = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
    final long end = now + unit.toNanos(timeout);

    synchronized (lock) {
      while (true) {
        if (state == State.CLOSED) {
          return false;
        }
        final boolean available = isAvailable();

        if (available) {
          return true;
        }
        final long timeRemaining = end - now;

        if (timeRemaining <= 0L) { // we timed out
          return false;
        }
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "Solr({}): waiting {} to become available",
              core,
              DurationFormatUtils.formatDurationHMS(TimeUnit.NANOSECONDS.toMillis(timeRemaining)));
        }
        now = waiter.timedWait(lock, now, timeRemaining, TimeUnit.NANOSECONDS);
      }
    }
  }

  @Override
  public boolean isAvailable(Listener listener) {
    Validate.notNull(listener, "invalid null listener");
    if (state != State.CLOSED) {
      LOGGER.debug("Solr({}): registering a new listener [{}]", core, listener);
      listeners.add(listener);
    } // else - notify the listener at least once
    final boolean available = isAvailable();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "Solr({}): starting a background task to notify a new listener [{}] that the availability is [{}]",
          core,
          listener,
          SolrClientAdapter.availableToString(available));
    }
    executor.submit(() -> notifyAvailability(listener, "is"));
    return available;
  }

  @Override
  public void whenAvailable(Initializer initializer) {
    Validate.notNull(initializer, "invalid null initializer");
    if (state == State.CLOSED) {
      return;
    }
    LOGGER.debug("Solr({}): registering a new initializer [{}]", core, initializer);
    // add the initializer to the list first to make sure we don't miss
    // the available state changes. We shall notify the initializer ourselves if
    // we are available and it was not yet removed from the list by another thread
    initializers.add(initializer);
    if (isAvailable() && initializers.remove(initializer)) {
      LOGGER.debug(
          "Solr({}): starting a background task to notify a new initializer [{}]",
          core,
          initializer);
      executor.submit(() -> notifyAvailability(initializer));
    }
  }

  @Override
  // overridden to always send the ping to the client; avoiding the intercept in handle()
  // which goes throw getProxiedClient() which would throw back an unavailable error instead of
  // returning the response
  public SolrPingResponse ping() throws SolrServerException, IOException {
    return ping("from the API");
  }

  @Override
  public String toString() {
    return "SolrClientAdapter(" + core + ", " + realClient + ")";
  }

  @VisibleForTesting
  State getState() {
    return state;
  }

  @VisibleForTesting
  Creator getCreator() {
    return creator;
  }

  @Nullable
  @VisibleForTesting
  SolrClient getRealClient() {
    return realClient;
  }

  @Nullable
  @VisibleForTesting
  UnavailableSolrClient getUnavailableClient() {
    return unavailableClient;
  }

  @VisibleForTesting
  SolrClient getApiClient() {
    return apiClient;
  }

  @VisibleForTesting
  SolrClient getPingClient() {
    return pingClient;
  }

  @VisibleForTesting
  @SuppressWarnings("squid:S1452" /* the future's value is never used internally */)
  Future<?> getFuture() {
    return future;
  }

  @VisibleForTesting
  boolean hasListeners() {
    return !listeners.isEmpty();
  }

  @VisibleForTesting
  boolean hasInitializers() {
    return !initializers.isEmpty();
  }

  /**
   * Checks if the Solr server is reachable by issuing a ping and awaiting the response.
   *
   * @param how how we got to checking if the server was reachable
   * @throws UnavailableSolrException if the server is not reachable for whatever reasons
   */
  @VisibleForTesting
  @SuppressWarnings("squid:S1181" /* bubbling out VirtualMachineError */)
  void checkIfReachable(String how) {
    LOGGER.debug("Solr({}): checking availability of the client {}", core, how);
    try {
      lastPing.set(System.currentTimeMillis());
      final SolrPingResponse response = pingClient.ping();

      if (response == null) {
        LOGGER.debug(SolrClientAdapter.FAILED_TO_PING, core, "null response");
        throw new UnavailableSolrException("ping failed with no response");
      }
      final Object status = response.getResponse().get("status");

      if (SolrClientAdapter.OK_STATUS.equals(status)) {
        return;
      }
      LOGGER.debug(SolrClientAdapter.FAILED_TO_PING_WITH_STATUS, core, status);
      throw new UnavailableSolrException("ping failed with " + status + " status");
    } catch (UnavailableSolrException | VirtualMachineError e) {
      throw e;
    } catch (Throwable t) {
      LOGGER.debug(SolrClientAdapter.FAILED_TO_PING, core, t, t);
      throw new UnavailableSolrException("ping failed", t);
    }
  }

  @VisibleForTesting
  void logFailure(@Nullable SolrClient returnedClient, Throwable t, ExecutionContext ctx) {
    if (wasNotRecent(lastCreateError, SolrClientAdapter.ERROR_MIN_FREQUENCY)) {
      LOGGER.warn("Solr client ({}) creation failed; retrying again: {}", core, t.getMessage());
    }
    LOGGER.debug(
        "Solr({}): retrying again after failed failsafe attempt #{} for client creation; got [{}] or [{}]",
        core,
        ctx.getExecutions(),
        returnedClient,
        t, // this will get the info logged on the first line
        t); // this one will get the stack trace logged after
  }

  @VisibleForTesting
  void logFailure(
      @SuppressWarnings({"unused", "squid:S1172"} /* failsafe api requirement */) @Nullable
          Void dummy,
      Throwable t,
      ExecutionContext ctx) {
    LOGGER.debug(
        "Solr({}): retrying again after failed failsage attempt #{} for client connection; got [{}]",
        core,
        ctx.getExecutions(),
        t, // this will get the info logged on the first line
        t); // this one will get the stack trace logged after
  }

  @VisibleForTesting
  void logInterruptionAndRecreate(Throwable t) {
    lastCreateError.set(0L); // reset it
    LOGGER.warn("Solr client ({}) creation interrupted", core);
    LOGGER.debug("Solr({}): client creation failsafe attempts were interrupted", core, t);
    Thread.currentThread().interrupt(); // propagate
    // should we actually close the whole thing as opposed to re-initializing?
    // ... and piggy back the exception as the cause
    // that is because normally we would get here if the thread was interrupted from the outside
    // which typically should happen if the executor was shutdown but we currently don't do that
    setCreating(unavailableClient, false);
  }

  @VisibleForTesting
  void logInterruptionAndReconnectIfStillConnecting(Throwable t) {
    LOGGER.warn("Solr client ({}) connection interrupted", core);
    LOGGER.debug("Solr({}): client connection failsafe attempts were interrupted", core, t);
    Thread.currentThread().interrupt(); // propagate
    // should we actually close the whole thing as opposed to re-connecting it?
    // ... and piggy back the exception as the cause
    // that is because normally we would get here if the thread was interrupted from the outside
    // which typically should happen if the executor was shutdown but we currently don't do that
    setConnecting(realClient, unavailableClient, false, State.CONNECTING);
  }

  @VisibleForTesting
  void logAndRecreateIfNotCancelled(Throwable t) {
    if (t instanceof CancellationException) { // don't restart if it was cancelled
      return;
    }
    LOGGER.debug(
        "Solr({}): failed all failsafe attempts for client creation; re-creating", core, t);
    setCreating(unavailableClient, false);
  }

  @VisibleForTesting
  void logAndReconnectIfNotCancelledAndStillConnecting(Throwable t) {
    if (t instanceof CancellationException) { // don't restart if it was cancelled
      return;
    }
    LOGGER.debug(
        "Solr({}): failed all failsafe attempts for client connection; re-connecting", core, t);
    setConnecting(realClient, unavailableClient, false, State.CONNECTING);
  }

  @VisibleForTesting
  void logAndSetConnecting(SolrClient newRealClient, ExecutionContext ctx) {
    lastCreateError.set(0L); // reset it
    LOGGER.info("Solr client ({}) creation was successful", core);
    LOGGER.debug(
        "Solr({}): client creation was successful after {} failsafe attempt(s): [{}]",
        core,
        ctx.getExecutions(),
        newRealClient);
    setConnecting(
        newRealClient, unavailableClient, false, State.CREATING, State.CONNECTING, State.CONNECTED);
  }

  @VisibleForTesting
  void logAndSetConnected(
      @SuppressWarnings({"unused", "squid:S1172"} /* failsafe api requirement */) Void dummy,
      ExecutionContext ctx) {
    LOGGER.info("Solr client ({}) connection was successful", core);
    LOGGER.debug(
        "Solr({}): client connection was successful after {} failsafe attempt(s)",
        core,
        ctx.getExecutions());
    setConnected(false);
  }

  // should only be called when a real client has been created
  @VisibleForTesting
  void checkIfReachableAndChangeStateAccordingly(Throwable error, String how) {
    if ((realClient == null) || (state == State.CLOSED)) { // quick check to avoid pinging
      return;
    }
    try {
      checkIfReachable(how);
      setConnected(true);
    } catch (UnavailableSolrException e) {
      // ignore the reason for the ping failure and continue with the one provided in parameter
      setConnecting(realClient, new UnavailableSolrClient(error), true, State.CONNECTED);
    }
  }

  /**
   * Changes the state to <i>creating</i> which will trigger a Solr client creation/re-creation.
   *
   * <p><i>Note:</i> Nothing will happen if the adapter is closed.
   *
   * @param newUnavailableClient the new unavailable client to use from now on indicating why we are
   *     creating/re-creating a new client
   * @param cancelFuture <code>true</code> to cancel the current future; <code>false</code> to only
   *     clear it
   */
  @VisibleForTesting
  void setCreating(UnavailableSolrClient newUnavailableClient, boolean cancelFuture) {
    if (state == State.CLOSED) { // quick check to avoid synchronization
      return;
    }
    final boolean notifyAvailability;
    final Future<?> futureToCancel;
    final SolrClient previousClientToClose;

    synchronized (lock) {
      if (state == State.CLOSED) { // already closed so bail
        return;
      }
      futureToCancel = cancelFuture ? future : null;
      previousClientToClose = realClient;
      // notify only if we were available
      notifyAvailability =
          shouldNotifyUnavailability(
              newUnavailableClient.getCause(), newUnavailableClient.getCause());
      LOGGER.debug("Solr({}): starting a failsafe client creation task", core);
      this.apiClient = newUnavailableClient;
      this.pingClient = newUnavailableClient;
      this.realClient = null;
      this.unavailableClient = newUnavailableClient;
      this.state = State.CREATING;
      this.future = createFailsafe.get(creator::create);
    }
    finalizeStateChangeWhileSwallowingIOExceptions(
        notifyAvailability, futureToCancel, previousClientToClose);
  }

  /**
   * Changes the state to <i>connecting</i> while updating the client and the cause if needed or if
   * requested.
   *
   * <p><i>Note:</i> Nothing will happen if the adapter is closed.
   *
   * @param newClient the new client to use
   * @param newUnavailableClient the new unavailable client to use from now on indicating why we are
   *     connecting/re-connecting to the client
   * @param cancelFuture <code>true</code> to cancel the current future; <code>false</code> to only
   *     clear it
   * @param onlyIf a set of states for which we should proceed with this state change
   *     <i>connected</i>; <code>false</code> to change it for any other states
   */
  @VisibleForTesting
  void setConnecting(
      SolrClient newClient,
      UnavailableSolrClient newUnavailableClient,
      boolean cancelFuture,
      State... onlyIf) {
    if (state == State.CLOSED) { // quick check to avoid synchronization
      return;
    }
    final boolean notifyAvailability;
    final Future<?> futureToCancel;
    final SolrClient previousClientToClose;

    synchronized (lock) {
      if (state == State.CLOSED) { // already closed so bail
        return;
      }
      if (!ArrayUtils.contains(onlyIf, state)) {
        return;
      }
      futureToCancel = cancelFuture ? future : null;
      previousClientToClose = realClient;
      // notify only if we were available
      notifyAvailability =
          shouldNotifyUnavailability("real client created as [" + newClient + "]", null);
      LOGGER.debug("Solr({}): starting a failsafe client connection task", core);
      lastPing.set(System.currentTimeMillis()); // since we are starting a background task
      this.apiClient = newUnavailableClient;
      this.pingClient = newClient;
      this.realClient = newClient;
      this.unavailableClient = newUnavailableClient;
      this.state = State.CONNECTING;
      this.future = pingFailsafe.run(this::checkIfReachable);
    }
    finalizeStateChangeWhileSwallowingIOExceptions(
        notifyAvailability, futureToCancel, previousClientToClose);
  }

  /**
   * Changes the state to <i>connected</i>.
   *
   * <p><i>Note:</i> Nothing will happen if the adapter is closed.
   *
   * @param cancelFuture <code>true</code> to cancel the current future; <code>false</code> to only
   *     clear it
   */
  @VisibleForTesting
  void setConnected(boolean cancelFuture) {
    if (state == State.CLOSED) { // quick check to avoid synchronization
      return;
    }
    final boolean notifyAvailability;
    final Future<?> futureToCancel;

    synchronized (lock) {
      if (state == State.CLOSED) { // already closed so bail
        return;
      }
      futureToCancel = cancelFuture ? future : null;
      // notify only if we were not available as we will now be
      notifyAvailability = shouldNotifyOfAvailability();
      this.apiClient = realClient;
      // keep pingClient and realClient as is
      this.unavailableClient = null;
      this.state = State.CONNECTED;
      this.future = null;
      lock.notifyAll(); // wakeup those waiting for isAvailable(timeout)
    }
    finalizeStateChangeWhileSwallowingIOExceptions(notifyAvailability, futureToCancel, null);
  }

  @VisibleForTesting
  boolean wasNotRecent(AtomicLong previous, long freq) {
    final long now = System.currentTimeMillis();

    if (now == previous.get()) {
      return false;
    }
    // update if not recent (i.e. if the last occurrence was older than the specified frequency
    return previous.accumulateAndGet(now, (last, n) -> ((now - last) >= freq) ? now : last) == now;
  }

  private SolrPingResponse backgroundPing() throws SolrServerException, IOException {
    return ping("in the background");
  }

  @SuppressWarnings("squid:S1181" /* bubbling out VirtualMachineError */)
  private SolrPingResponse ping(String how) throws SolrServerException, IOException {
    LOGGER.debug("Solr({}): pinging the client {}", core, how);
    try {
      lastPing.set(System.currentTimeMillis());
      final SolrPingResponse response = pingClient.ping();

      if (response == null) {
        LOGGER.debug(SolrClientAdapter.FAILED_TO_PING, core, "null response");
        setConnecting(
            realClient,
            new UnavailableSolrClient(new UnavailableSolrException("ping failed with no response")),
            true,
            State.CONNECTED);
        return response;
      }
      final Object status = response.getResponse().get("status");

      if (SolrClientAdapter.OK_STATUS.equals(status)) {
        setConnected(true);
      } else {
        LOGGER.debug(SolrClientAdapter.FAILED_TO_PING_WITH_STATUS, core, status);
        setConnecting(
            realClient,
            new UnavailableSolrClient(
                new UnavailableSolrException("ping failed with " + status + " status")),
            true,
            State.CONNECTED);
      }
      return response;
    } catch (UnavailableSolrException | VirtualMachineError e) {
      throw e;
    } catch (Throwable t) {
      LOGGER.debug(SolrClientAdapter.FAILED_TO_PING, core, t, t);
      setConnecting(realClient, new UnavailableSolrClient(t), true, State.CONNECTED);
      throw t;
    }
  }

  private void checkIfReachable(
      @SuppressWarnings({"unused", "squid:S1172"} /* required by failsafe's API */)
          ExecutionContext context) {
    checkIfReachable("from failsafe while trying to reconnect");
  }

  private void finalizeStateChangeWhileSwallowingIOExceptions(
      boolean notifyAvailability,
      @Nullable Future<?> futureToCancel,
      @Nullable SolrClient previousClientToClose) {
    try {
      finalizeStateChange(notifyAvailability, futureToCancel, previousClientToClose, true);
    } catch (IOException e) { // will never happen, exceptions are swallowed above
    }
  }

  @SuppressWarnings("PMD.CompareObjectsWithEquals" /* purposely testing previous client identity */)
  private void finalizeStateChange(
      boolean notifyAvailability,
      @Nullable Future<?> futureToCancel,
      @Nullable SolrClient previousClientToClose,
      boolean swallowIOExceptions)
      throws IOException {
    if (notifyAvailability) {
      notifyListenersAndInitializers();
    }
    if ((futureToCancel != null) && !futureToCancel.isDone()) {
      LOGGER.debug("Solr({}): cancelling its previous failsafe task", core);
      futureToCancel.cancel(true);
    }
    // don't close if we still use the same client
    if ((previousClientToClose != null) && (previousClientToClose != realClient)) {
      LOGGER.debug("Solr({}): closing its previous client [{}]", core, previousClientToClose);
      Closeables.close(previousClientToClose, swallowIOExceptions);
    }
  }

  private void notifyListenersAndInitializers() {
    final boolean available = (state == State.CONNECTED);
    final String availableString = SolrClientAdapter.availableToString(available);

    if (LOGGER.isInfoEnabled()) {
      if (state == State.CLOSED) {
        LOGGER.info("Solr client ({}) is closed", core);
      } else {
        LOGGER.info("Solr client ({}) is {}", core, availableString.toLowerCase());
      }
    }
    if (!listeners.isEmpty()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Solr({}): starting background task(s) to notify {} listener(s) that the availability changed to [{}]",
            core,
            listeners.size(),
            availableString);
      }
      listeners.forEach(l -> executor.submit(() -> notifyAvailability(l, "changed to")));
    }
    if (available && !initializers.isEmpty()) {
      Initializer i;

      LOGGER.debug(
          "Solr({}): starting background task(s) to notify {} initializer(s)",
          core,
          initializers.size());
      while ((i = initializers.poll()) != null) {
        final Initializer initializer = i;

        executor.submit(() -> notifyAvailability(initializer));
      }
    }
  }

  private void notifyAvailability(Listener listener, String how) {
    final boolean available = (state == State.CONNECTED);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "Solr({}): notifying a listener [{}] that the availability {} [{}]",
          core,
          listener,
          how,
          SolrClientAdapter.availableToString(available));
    }
    listener.changed(this, available);
  }

  private void notifyAvailability(Initializer initializer) {
    LOGGER.debug("Solr({}): notifying an initializer [{}]", core, initializer);
    initializer.initialized(this);
  }

  private boolean shouldNotifyUnavailability(Object reason, @Nullable Throwable cause) {
    final boolean notifyAvailability = state == State.CONNECTED;

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "Solr({}): {} unavailable because: {}",
          core,
          SolrClientAdapter.goingOrRemaining(notifyAvailability),
          reason, // this will get the reason logged on the first line
          cause); // this one will get the stack trace logged after (if any)
    }
    return notifyAvailability;
  }

  private boolean shouldNotifyOfAvailability() {
    final boolean notifyAvailability = (state != State.CONNECTED);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "Solr({}): {} available", core, SolrClientAdapter.goingOrRemaining(notifyAvailability));
    }
    return notifyAvailability;
  }

  private static ScheduledExecutorService createExecutor() throws NumberFormatException {
    return Executors.newScheduledThreadPool(
        NumberUtils.toInt(
            AccessController.doPrivileged(
                (PrivilegedAction<String>)
                    () -> System.getProperty("org.codice.ddf.system.threadPoolSize")),
            SolrClientAdapter.THREAD_POOL_DEFAULT_SIZE),
        StandardThreadFactoryBuilder.newThreadFactory("SolrClientAdapter"));
  }

  private static String availableToString(boolean available) {
    return available ? "AVAILABLE" : "NOT AVAILABLE";
  }

  private static String goingOrRemaining(boolean going) {
    return going ? "going" : "remaining";
  }

  /** Functional interface used to create Solr clients. */
  @FunctionalInterface
  public interface Creator {
    /**
     * Called to attempt to create a new Solr client.
     *
     * @return the corresponding client or <code>null</code> if unable to create one
     * @throws IOException if an I/O exception occurred while attempting to create the Solr client
     * @throws SolrServerException if an Solr server exception occurred while attempting to create
     *     the Solr client
     * @throws SolrException if an Solr exception occurred while attempting to create the Solr
     *     client
     * @throws InterruptedException if interrupted while attempting to create the Solr client
     */
    @Nullable
    public SolrClient create() throws SolrServerException, IOException, InterruptedException;
  }

  /** Useful class for intercepting waiting periods during testing. */
  @VisibleForTesting
  static class Waiter {
    /**
     * Waits on the specified lock for the specified amount of time in the given unit.
     *
     * <p><i>Note:</i> <code>lock</code> will be synchronized when this method is called.
     *
     * @param lock the lock to wait on (assumed the lock is already acquired)
     * @param now the current time in the given unit
     * @param time the amount of time in the given unit to wait for (always greater than 0)
     * @param unit the unit for the amount of time
     * @return the current time in the specified unit after it woke up (used to recompute the next
     *     delay if needed)
     * @throws InterruptedException if the wait is interrupted
     */
    public long timedWait(
        Object lock,
        @SuppressWarnings("unused" /* for testing */) long now,
        long time,
        TimeUnit unit)
        throws InterruptedException {
      unit.timedWait(lock, time);
      return unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }
  }

  /** Useful class for intercepting standalone background execution (not done via failsafe). */
  @VisibleForTesting
  static class Executor {
    public <T> Future<T> submit(Callable<T> task) {
      return SolrClientAdapter.SCHEDULED_EXECUTOR.submit(task);
    }

    @SuppressWarnings("squid:S1452" /* the future's value is never used internally */)
    public Future<?> submit(Runnable task) {
      return SolrClientAdapter.SCHEDULED_EXECUTOR.submit(task);
    }
  }
}
