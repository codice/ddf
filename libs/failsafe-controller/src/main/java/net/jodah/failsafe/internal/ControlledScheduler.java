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
package net.jodah.failsafe.internal;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.jodah.failsafe.FailsafeController;
import net.jodah.failsafe.util.concurrent.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class wraps around another scheduler in order to keep track of all allocated threads. */
public class ControlledScheduler implements Scheduler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ControlledScheduler.class);

  private final FailsafeController<?> controller;

  private final int id;

  private final Scheduler scheduler;

  private boolean shutdown = false;

  /**
   * Keeps track of all threads that are currently allocated to help failsafe perform a specific
   * execution. These threads are either asynchronously executing the retry attempt or
   * asynchronously notifying registered listeners.
   */
  private final Deque<Thread> threads = new LinkedList<>();

  public ControlledScheduler(FailsafeController<?> controller, int id, Scheduler scheduler) {
    this.controller = controller;
    this.id = id;
    this.scheduler = scheduler;
  }

  /**
   * Checks if this scheduler is currently actively executing something.
   *
   * @return <code>true</code> if at least one thread is currently executing something; <code>false
   *     </code> otherwise
   */
  public boolean isActive() {
    synchronized (controller) {
      return !threads.isEmpty();
    }
  }

  /**
   * Checks if a given thread is currently actively executing something for this scheduler.
   *
   * @param thread the thread to check for
   * @return <code>true</code> if the specified thread is currently executing something for this
   *     scheduler; <code>false</code> otherwise
   */
  public boolean isActive(Thread thread) {
    synchronized (controller) {
      return threads.contains(thread);
    }
  }

  /** Shuts down this scheduler by interrupting all active threads. */
  public void shutdown() {
    synchronized (controller) {
      this.shutdown = true;
      threads.forEach(Thread::interrupt);
    }
  }

  @Override
  public ScheduledFuture<?> schedule(Callable<?> callable, long delay, TimeUnit unit) {
    return scheduler.schedule(
        () -> {
          final Thread thread = Thread.currentThread();

          try {
            synchronized (controller) {
              if (shutdown) {
                throw new InterruptedException("failsafe controller was shutdown: " + controller);
              }
              threads.add(thread);
              LOGGER.debug(
                  "FailsafeController({} - {}): allocating thread '{}', total: {}",
                  controller,
                  controller.getId(),
                  thread.getName(),
                  threads.size());
            }
            return callable.call();
          } finally {
            synchronized (controller) {
              threads.remove(thread);
              LOGGER.debug(
                  "FailsafeController({} - {}): releasing thread: '{}', total: {}",
                  controller,
                  id,
                  thread.getName(),
                  threads.size());
              // wakeup anybody that might be waiting on this thread to no longer be
              // active
              controller.notifyAll();
            }
          }
        },
        delay,
        unit);
  }
}
