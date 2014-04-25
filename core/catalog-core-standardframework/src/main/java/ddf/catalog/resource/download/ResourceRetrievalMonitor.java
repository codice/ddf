/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.resource.download;

import java.util.TimerTask;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors the @ReliableResourceCallable, detecting if no bytes have been read from the resource's @InputStream
 * for the monitor's period. If this is detected, then this monitor is canceled, the @ReliableResourceCallable's
 * interrupt flag is set, and the @Future that started it is canceled.
 *
 */
public class ResourceRetrievalMonitor extends TimerTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceRetrievalMonitor.class);

    private long previousBytesRead = 0;
    private Future<?> future;
    private ReliableResourceCallable reliableResourceCallable;
    private long monitorPeriod;


    /**
     * @param future the @Future that started the @ReliableResourceCallable doing the resource download
     * @param reliableResourceCallable the @Callable to interrupt if no bytes read in specified period
     * @param monitorPeriod the frequency (in ms) this monitor should check for bytes read
     */
    public ResourceRetrievalMonitor(Future<?> future, ReliableResourceCallable reliableResourceCallable, long monitorPeriod) {
        this.future = future;
        this.reliableResourceCallable = reliableResourceCallable;
        this.monitorPeriod = monitorPeriod;
    }

    /**
     * Returns the number of bytes read from the resource's @InputStream thus far.
     * 
     * @return
     */
    public long getBytesRead() {
        return previousBytesRead;
    }

    @Override
    public void run() {
        long bytesRead = reliableResourceCallable.getBytesRead();
        long chunkByteCount = bytesRead - previousBytesRead;
        if (chunkByteCount > 0) {
            long transferSpeed = (chunkByteCount / monitorPeriod) * 1000;  // in bytes per second
            LOGGER.debug(
                    "Downloaded {} bytes in last {} ms. Total bytes read = {},  transfer speed = {}/second",
                    chunkByteCount, monitorPeriod, bytesRead,
                    FileUtils.byteCountToDisplaySize(transferSpeed));
            previousBytesRead = reliableResourceCallable.getBytesRead();
        } else {
            LOGGER.debug("No bytes downloaded in last {} ms - cancelling ResourceRetrievalMonitor and ReliableResourceCallable future (thread).", monitorPeriod);
            // Stop this ResourceRetrievalMonitor since the ReliableResourceCallable being watched will be stopped now
            cancel();
            // Stop the download thread
            // synchronized so that Callable can finish any writing to OutputStreams before being canceled
            synchronized(reliableResourceCallable) {
                LOGGER.debug("Setting interruptCaching on ReliableResourceCallable thread");
                reliableResourceCallable.setInterruptDownload(true);

                // Without this the Future that is running the ReliableResourceCallable will not stop
                boolean status = future.cancel(true);
                LOGGER.debug("future cancelling status = {}", status);
            }
        }
    }

}
