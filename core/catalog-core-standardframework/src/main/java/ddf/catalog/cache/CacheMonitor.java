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
package ddf.catalog.cache;

import java.util.TimerTask;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * CacheMonitor monitors the caching of a product to the file system. If the caching
 * to the file system is taking too long, then the CacheMonitor will interrupt and
 * stop the caching.
 * 
 * CacheMonitor is a @TimerTask running in a separate thread. The @CallableCacheProduct, the class
 * responsible for caching the product to disk, is passed in as the @Callable to be
 * monitored by this class. The @Future that started the @CallableCacheProduct is also
 * passed in so that the CacheMonitor can cancel the @Future, thereby stopping the
 * caching of the product.
 * 
 * It is started by the @CachedResource, and
 * its initial delay startup time and frequency of monitoring (period) are both 
 * configurable.
 * 
 * @author rodgers
 *
 */
public class CacheMonitor extends TimerTask
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheMonitor.class);
    
    private long previousBytesRead = 0;
    private Future<?> future;
    private CallableCacheProduct callableCacheProduct;
    private long cachingMonitorPeriod;
    
    public CacheMonitor(Future<?> future, CallableCacheProduct callableCacheProduct, long cachingMonitorPeriod) {
        this.future = future;
        this.callableCacheProduct = callableCacheProduct;
        this.cachingMonitorPeriod = cachingMonitorPeriod;
    }        
    
    public long getBytesRead() {
        return previousBytesRead;
    }

    @Override
    public void run() {
        long chunkByteCount = callableCacheProduct.getBytesRead() - previousBytesRead;
        if (chunkByteCount > 0) {
            long transferSpeed = (chunkByteCount / cachingMonitorPeriod) * 1000;  // in bytes per second
            LOGGER.debug(
                    "Cached {} bytes in last {} ms. Total bytes read = {},  transfer speed = {}/second",
                    chunkByteCount, cachingMonitorPeriod, callableCacheProduct.getBytesRead(),
                    FileUtils.byteCountToDisplaySize(transferSpeed));
            previousBytesRead = callableCacheProduct.getBytesRead();
        } else {
            LOGGER.debug("No bytes cached in last {} ms - cancelling CacheMonitor and caching future (thread).", cachingMonitorPeriod);
            // Stop this CacheMonitor since the CallableCacheProduct being watched will be stopped now
            cancel();
            // Stop the caching thread
            // synchronized so that Callable can finish any writing to OutputStreams before being canceled
            synchronized(callableCacheProduct) {
                LOGGER.debug("Setting interruptCaching on CallableCacheProduct thread");
                callableCacheProduct.setInterruptCaching(true);
                
                // Without this the Future that is running the CallableCacheProduct will not stop
                boolean status = future.cancel(true);
                LOGGER.debug("future cancelling status = {}", status);
            }
        }
    }
}