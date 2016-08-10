/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.test.itests.common.downloads;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Allows an index to be specified for stream consumers to wait at.
 */
public abstract class AbstractIndexBlocker {

    final ExecutorService executorService = Executors.newSingleThreadExecutor();

    CyclicBarrier barrier;

    String metacardId;

    int waitAt;

    public AbstractIndexBlocker(String metacardId, CyclicBarrier barrier) {
        // Default waitAt to zero - which means we will never wait on the barrier
        this.waitAt = 0;
        this.metacardId = metacardId;
        this.barrier = barrier;
    }

    /**
     * Specify the next index that will trigger a block-wait on the barrier. Be careful when using
     * this setter. Ensure that all production and consumption has come to rest before invoking.
     *
     * @param waitAt The next index to wait at the barrier for other consumers to catch up.
     */
    public void setWaitAt(int waitAt) {
        this.waitAt = waitAt;
    }

    /**
     * Start asynchronous processing.
     *
     * @return A future representing the final result of the entire synchronized operation.
     */
    public abstract Future<String> begin();
}
