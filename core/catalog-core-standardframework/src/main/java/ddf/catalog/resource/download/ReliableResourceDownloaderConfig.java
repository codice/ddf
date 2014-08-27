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

import ddf.catalog.cache.impl.ResourceCache;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventListener;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventPublisher;

public class ReliableResourceDownloaderConfig {
    
    public static final int KB = 1024;

    public static final int MB = 1024 * KB;

    private static final int DEFAULT_CHUNK_SIZE = 1 * MB;

    private int maxRetryAttempts = 3;

    private int delayBetweenAttemptsMS = 10000;

    private long monitorPeriodMS = 5000;

    private int monitorInitialDelayMS = 1000;

    private boolean cacheEnabled = false;

    private boolean cacheWhenCanceled = false;

    private ResourceCache resourceCache;

    private DownloadsStatusEventPublisher eventPublisher;

    private DownloadsStatusEventListener eventListener;
    
    private int chunkSize = DEFAULT_CHUNK_SIZE;

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getMonitorInitialDelayMS() {
        return monitorInitialDelayMS;
    }

    public void setMonitorInitialDelayMS(int monitorInitialDelayMS) {
        this.monitorInitialDelayMS = monitorInitialDelayMS;
    }

    public DownloadsStatusEventListener getEventListener() {
        return eventListener;
    }

    public void setEventListener(DownloadsStatusEventListener eventListener) {
        this.eventListener = eventListener;
    }

    public DownloadsStatusEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    public void setEventPublisher(DownloadsStatusEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public ResourceCache getResourceCache() {
        return resourceCache;
    }

    public void setResourceCache(ResourceCache resourceCache) {
        this.resourceCache = resourceCache;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public void setDelayBetweenAttemptsMS(int delayBetweenAttempts) {
        this.delayBetweenAttemptsMS = delayBetweenAttempts;
    }

    public void setMonitorPeriodMS(long monitorPeriod) {
        this.monitorPeriodMS = monitorPeriod;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public void setCacheWhenCanceled(boolean cacheWhenCanceled) {
        this.cacheWhenCanceled = cacheWhenCanceled;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public int getDelayBetweenAttemptsMS() {
        return delayBetweenAttemptsMS;
    }

    public long getMonitorPeriodMS() {
        return monitorPeriodMS;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public boolean isCacheWhenCanceled() {
        return cacheWhenCanceled;
    }

}
