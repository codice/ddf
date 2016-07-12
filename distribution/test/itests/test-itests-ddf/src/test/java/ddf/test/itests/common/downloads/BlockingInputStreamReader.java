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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special async stream reader for synchronizing with other async readers of the same type.
 * Will block-wait for the barrier to fire on the specified index until it continues processing.
 */
public class BlockingInputStreamReader extends AbstractIndexBlocker implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockingInputStreamReader.class);

    private final AtomicInteger currentByteConsumptionIndex = new AtomicInteger(0);

    private final StringBuilder data = new StringBuilder();

    private InputStream inputStream;

    /**
     * Default constructor.
     *
     * @param inputStream The stream to read from.
     * @param barrier     The barrier to synchronize against.
     */
    public BlockingInputStreamReader(String metacardId, InputStream inputStream,
            CyclicBarrier barrier) {
        super(metacardId, barrier);
        this.inputStream = inputStream;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
        executorService.shutdownNow();
    }

    /**
     * Retrieve the current byte that the stream is working on obtaining. This method is thread-safe
     * but its information may go out-of-date quickly if producers and consumers are still working.
     *
     * @return The next index pending a read.
     */
    public int getCurrentByteConsumptionIndex() {
        return currentByteConsumptionIndex.get();
    }

    /**
     * Gets the current set of bytes read as a String in a thread-safe fashion. This information will
     * go out of date quickly (as of this method returning) if producers and consumers are not in a
     * blocked-waiting state.
     *
     * @return A string of characters representing the current set of read bytes.
     */
    public String getData() {
        synchronized (data) {
            return data.toString();
        }
    }

    public Future<String> begin() {
        return executorService.submit(() -> {
            try (InputStream stream = this.inputStream) {
                int nextByte = stream.read();
                while (nextByte > -1) {
                    synchronized (data) {
                        data.append(nextByte);
                    }
                    int currentIndex = currentByteConsumptionIndex.incrementAndGet();
                    if (currentIndex == waitAt) {
                        barrier.await();
                        // If this was triggered, then the next stream.read() should have no
                        // data available to read. It should block.
                    }
                    nextByte = stream.read();
                }
            } catch (IOException e) {
                /* We only warn if the IOException was from a prematurely closed connection
                    This is an expected use case for the client
                    Any other cause is an error and a rethrow */
                e.printStackTrace();
                LOGGER.warn("BlockingInputStreamReader encountered an error during execution: {}",
                        "It might have been closed prematurely.");
            }
            synchronized (data) {
                return data.toString();
            }
        });
    }
}