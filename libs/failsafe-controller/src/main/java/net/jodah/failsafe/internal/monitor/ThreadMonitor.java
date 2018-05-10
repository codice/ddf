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
package net.jodah.failsafe.internal.monitor;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.jodah.failsafe.FailsafeController;
import net.jodah.failsafe.util.concurrent.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper class to monitor threads. */
public class ThreadMonitor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ThreadMonitor.class);

  private static final ThreadLocal<ThreadMonitor> MONITORS = new ThreadLocal<>();

  /**
   * Fake thread monitor that doesn't monitor anything and it cannot be shutdown, it acts as a
   * pass-thru invoking code as is.
   */
  private static final ThreadMonitor FAKE =
      new ThreadMonitor() {
        @Override
        public void shutdown() { // shutdown is disabled
        }

        @Override
        public <T> T monitor(Callable<T> callable) throws Exception {
          // hopefully we should never get here, but if we do, simply call the code normally
          final Thread thread = Thread.currentThread();

          LOGGER.warn("FailsafeController({}): not monitoring thread '{}'", info, thread.getName());
          return callable.call();
        }
      };

  protected final FailsafeController<?> controller;

  protected final String info;

  private boolean shutdown = false;

  /** Keeps track of all threads that are currently being monitored. */
  private final Deque<Thread> threads = new LinkedList<>();

  private final Set<Object> locks = new HashSet<>();

  public ThreadMonitor(FailsafeController<?> controller) {
    this.controller = controller;
    this.info = controller.toString();
  }

  public ThreadMonitor(FailsafeController<?> controller, int id) {
    this.controller = controller;
    this.info = controller.toString() + " - " + id;
  }

  private ThreadMonitor() {
    this.controller = null;
    this.info = "?";
  }

  /**
   * Checks if this monitor is currently actively monitoring at least one thread or if at least one
   * lock was allocated and monitoring for it as not yet started.
   *
   * @return <code>true</code> if at least one thread is currently being monitored; <code>false
   *     </code> otherwise
   */
  public boolean isMonitoring() {
    synchronized (controller) {
      return !threads.isEmpty() || !locks.isEmpty();
    }
  }

  /**
   * Checks if a given thread is currently being monitored by this monitor.
   *
   * @param thread the thread to check for
   * @return <code>true</code> if the specified thread is currently being monitored by this monitor;
   *     <code>false</code> otherwise
   */
  public boolean isMonitoring(Thread thread) {
    synchronized (controller) {
      return threads.contains(thread);
    }
  }

  /** Shuts down this monitor by interrupting all threads currently being monitored. */
  public void shutdown() {
    synchronized (controller) {
      this.shutdown = true;
      threads.forEach(Thread::interrupt);
    }
  }

  /**
   * Monitors the current thread while calling the specified callable.
   *
   * @param <T> the result type
   * @param callable the callable to be monitored
   * @return the result from the callable
   * @throws Exception if thrown by the callable
   * @throws InterruptedException if the monitor is shutdown (the callable won't be called
   */
  public <T> T monitor(Callable<T> callable) throws Exception {
    final Thread thread = Thread.currentThread();
    final ThreadMonitor parent = ThreadMonitor.MONITORS.get();

    try {
      // register us as the current thread local
      // if a monitor was already registered then make sure we compound it with this one
      ThreadMonitor.MONITORS.set((parent != null) ? new CompoundThreadMonitor(this, parent) : this);
      synchronized (controller) {
        if (shutdown) {
          throw new InterruptedException("failsafe controller was shutdown: " + controller);
        }
        threads.add(thread);
        LOGGER.debug(
            "FailsafeController({}): starting to monitor thread '{}', total: {}",
            info,
            thread.getName(),
            threads.size());
      }
      return callable.call();
    } finally {
      // re-register the original thread monitor (even if none as that would simply deregister)
      ThreadMonitor.MONITORS.set(parent);
      synchronized (controller) {
        threads.remove(thread);
        LOGGER.debug(
            "FailsafeController({}): stopping to monitor thread: '{}', total: {}",
            info,
            thread.getName(),
            threads.size());
        // wakeup anybody that might be waiting on this thread to no longer be active
        controller.notifyAll();
      }
    }
  }

  /**
   * Submits a value-returning task for execution by the specified executor and returns a {@link
   * Future} representing the pending results of the task.
   *
   * @param <T> the type of the task's result
   * @param callable the callable to submit and monitor
   * @return a future representing pending completion of the task
   */
  @SuppressWarnings("squid:S1181" /* bubbling up VirtualMachineError first */)
  public <T> Future<T> monitor(Callable<T> callable, ExecutorService executor) {
    final Object lock = register(new Object());

    try {
      return executor.submit(() -> monitor(lock, callable));
    } catch (VirtualMachineError e) {
      throw e;
    } catch (Error | RuntimeException e) { // task could not be submitted
      synchronized (controller) {
        locks.remove(lock);
      }
      throw e;
    }
  }

  /**
   * Schedules the {@code callable} to be called by the speified scheduler after the {@code delay}
   * for the {@code unit} while being monitored.
   *
   * @param callable the callable to be scheduled and monitored
   * @param scheduler the scheduler to schedule the execution with
   * @param delay the amount of time to wait before executing the callable
   * @param unit the unit for the amount of time to wait before executing the callable
   */
  @SuppressWarnings({
    "squid:S1181", /* bubbling up VirtualMachineError first */
    "squid:S1452" /* forced by Failsafe's API */
  })
  public ScheduledFuture<?> monitor(
      Callable<?> callable, Scheduler scheduler, long delay, TimeUnit unit) {
    final Object lock = register(new Object());

    try {
      return scheduler.schedule(() -> monitor(lock, callable), delay, unit);
    } catch (VirtualMachineError e) {
      throw e;
    } catch (Error | RuntimeException e) { // task could not be submitted
      deregister(lock);
      throw e;
    }
  }

  /**
   * Creates a new scheduler capable of monitoring all scheduled tasks.
   *
   * @param scheduler the scheduler to use for actually scheduling tasks
   * @return a new scheduler that uses this thread monitor to monitor all scheduled tasks
   */
  public Scheduler monitor(Scheduler scheduler) {
    return (callable, delay, unit) -> monitor(callable, scheduler, delay, unit);
  }

  protected Object register(Object lock) {
    synchronized (controller) {
      locks.add(lock);
      LOGGER.debug(
          "FailsafeController({}): registering monitor lock '{}', total: {}",
          info,
          lock,
          locks.size());
    }
    return lock;
  }

  protected Object deregister(Object lock) {
    synchronized (controller) {
      locks.remove(lock);
      LOGGER.debug(
          "FailsafeController({}): deregistering monitor lock '{}', total: {}",
          info,
          lock,
          locks.size());
      // wakeup anybody that might be waiting on this thread monitor to no longer be active
      controller.notifyAll();
    }
    return lock;
  }

  protected <T> T monitor(Object lock, Callable<T> callable) throws Exception {
    try {
      return monitor(callable);
    } finally {
      deregister(lock);
    }
  }

  /**
   * Gets the thread monitor monitoring the current thread.
   *
   * @return the thread monitor monitoring the current thread
   */
  public static ThreadMonitor current() {
    final ThreadMonitor current = ThreadMonitor.MONITORS.get();

    if (current != null) {
      return current;
    }
    // else - return the fake monitor
    return ThreadMonitor.FAKE;
  }

  /** Thread monitor that combines 2 thread monitors */
  private class CompoundThreadMonitor extends ThreadMonitor {
    private final ThreadMonitor current;
    private final ThreadMonitor parent;

    CompoundThreadMonitor(ThreadMonitor current, ThreadMonitor parent) {
      this.current = current;
      this.parent = parent;
    }

    @Override
    public <T> T monitor(Callable<T> callable) throws Exception {
      return parent.monitor(() -> current.monitor(callable));
    }

    @Override
    protected Object register(Object lock) {
      return current.register(parent.register(lock));
    }

    @Override
    protected Object deregister(Object lock) {
      return parent.deregister(current.deregister(lock));
    }

    @Override
    protected <T> T monitor(Object lock, Callable<T> callable) throws Exception {
      return parent.monitor(lock, () -> current.monitor(lock, callable));
    }
  }
}
