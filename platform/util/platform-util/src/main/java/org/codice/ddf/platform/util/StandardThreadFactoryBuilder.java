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
package org.codice.ddf.platform.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class StandardThreadFactoryBuilder implements ThreadFactory {

  public static ThreadFactory newThreadFactory(String threadName) {
    return newThreadFactory(threadName, true, Thread.NORM_PRIORITY);
  }

  public static ThreadFactory newThreadFactory(String threadName, boolean isDaemon, int priority) {
    if (threadName == null || threadName.isEmpty()) {
      threadName = "thread";
    }
    return build(threadName, isDaemon, priority);
  }

  private static ThreadFactory build(String threadName, boolean isDaemon, int priority) {
    final AtomicLong count = new AtomicLong(0);
    return r -> {
      Thread thread = new Thread(r);
      thread.setName(threadName + " " + count.getAndIncrement());
      thread.setDaemon(isDaemon);
      thread.setPriority(priority);
      return thread;
    };
  }

  @Override
  public Thread newThread(Runnable r) {
    return new Thread(r);
  }
}
