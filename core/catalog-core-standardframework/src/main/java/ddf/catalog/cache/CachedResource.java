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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.util.Timer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.activation.MimeType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.cache.CacheException;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.impl.ResourceImpl;
import ddf.catalog.resourceretriever.ResourceRetriever;

/**
 * Contains the details of a Resource including where it is stored and how to retrieve that Resource
 * locally.
 */
public class CachedResource implements Resource, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedResource.class);

    /**
     * Indicates Resource has been completely cached, i.e., all bytes read for Resource's
     * InputStream and written to disk in the product cache directory.
     */
    private static final long RESOURCE_CACHING_COMPLETE = -1L;
    
    private static final int END_OF_FILE = -1;

    /**
     * Initial delay, in ms, before the Caching Monitor TimerTask is started
     */
    private static final int DEFAULT_CACHING_MONITOR_INITIAL_DELAY = 1000;  // 1 second
    
    /**
     * Frequency, in ms, that Caching Monitor checks that more bytes have been read
     * from the source InputStream
     */
    private static final long DEFAULT_CACHING_MONITOR_PERIOD = 5000;  // 5 seconds
    
    /**
     * Size of byte buffer (chunk) for each read from the source InputStream
     */
    private static int DEFAULT_CHUNK_SIZE = 1024 * 1024;  // 1 MB

    /**
     * Delay, in ms, between attempts to retrieve and cache the product
     */
    private static int DEFAULT_DELAY_BETWEEN_ATTEMPTS = 1000;  // 1 second

    /** Directory for products cached to file system */
    private String productCacheDirectory;

    private String filePath;

    private MimeType mimeType;

    private String resourceName;

    private long size = -1L;

    private String key;
    
    private long cachingMonitorPeriod;
    
    private int chunkSize;
    
    /**
     * Delay, in ms, between attempts to cache resource to disk
     */
    private int delayBetweenAttempts;
    
    private int cachingMonitorInitialDelay;
    
    /**
     * Transient because ExecutorService is not Serializable
     */
    private transient ExecutorService executor;
    

    public CachedResource(String productCacheDirectory) {
        this.productCacheDirectory = productCacheDirectory;
        this.cachingMonitorPeriod = DEFAULT_CACHING_MONITOR_PERIOD;
        this.cachingMonitorInitialDelay = DEFAULT_CACHING_MONITOR_INITIAL_DELAY;
        this.delayBetweenAttempts= DEFAULT_DELAY_BETWEEN_ATTEMPTS;
        this.chunkSize = DEFAULT_CHUNK_SIZE;
        this.executor = Executors.newCachedThreadPool();
    }
    
    /**
     * Set the frequency (period) of how often to verify that caching of the file is still
     * proceeding.
     * 
     * @param period
     *            frequency, in ms, to check that more bytes from the resource's @InputStream have
     *            been cached
     */
    public void setCachingMonitorPeriod(long period) {
        this.cachingMonitorPeriod = period;
    }
    
    /**
     * Set the frequency (period) of how often to verify that caching of the file is still
     * proceeding.
     * 
     * @param period
     *            frequency, in ms, to check that more bytes from the resource's @InputStream have
     *            been cached
     */
    public void setCachingMonitorInitialDelay(int delay) {
        this.cachingMonitorInitialDelay = delay;
    }
    
    /**
     * Amount of time, in ms, to wait between retry attempts
     * 
     * @param delay
     */
    public void setDelayBetweenAttempts(int delay) {
        this.delayBetweenAttempts = delay;
    }
    
    /**
     * @param chunkSize how many bytes to read at a time from the resource's @InputStream
     */
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
    
    public Resource store(String key, final ResourceResponse resourceResponse,
            final ResourceCache resourceCache, final ResourceRetriever retriever) throws CacheException {

        LOGGER.info("ENTERING: store()");
        
        if (key == null) {
            throw new CacheException("Cannot cache file because cache key is null");
        }
        
        this.key = key;
        
        if (resourceResponse == null) {
            throw new CacheException("Cannot cache file because ResourceResponse is null");
        }
        
        Resource resource = resourceResponse.getResource();

        if (resource == null) {
            throw new CacheException("Cannot cache file because Resource is null");
        }

        filePath = FilenameUtils.concat(productCacheDirectory, key);

        // Create a piped input stream for the endpoint/client to read from
        final PipedInputStream pis = new PipedInputStream(chunkSize);
        
        // Create a piped output stream to write product to and that will be read
        // by client using piped input stream
        final PipedOutputStream pos;
        try {
            pos = new PipedOutputStream(pis);
        } catch (IOException e) {
            LOGGER.error("Unable to open PipedOutputStream for caching file {}", filePath, e);
            throw new CacheException("Unable to open PipedOutputStream for caching file " + filePath, e);
        }
        
        mimeType = resource.getMimeType();
        resourceName = resource.getName();
        
        // Create new Resource to return that will encapsulate the PipedInputStream
        // that will be read by the client simultaneously as the product is cached
        // to disk
        ResourceImpl newResource = new ResourceImpl(pis, mimeType, resourceName);
        
        LOGGER.debug("Copying resource to filepath = {}", filePath);
        final InputStream source = resource.getInputStream();
        
        // PipedOutputStream must run in separate thread from PipedInputStream
        Runnable cacheThread = new Runnable() {
            @Override
            public void run() {
                cacheFile(source, pos, filePath, resourceCache, retriever, 3);
            }
        };
        
        executor.submit(cacheThread);
        
        LOGGER.debug("EXITING: store()");

        return newResource;
    }
    
    private void cacheFile(InputStream source, PipedOutputStream pos,
            String filePath, ResourceCache resourceCache, ResourceRetriever retriever, int retryAttempts) {

        // Copy product's bytes from InputStream to product cache file.
        // Resource cache file will be created if it doesn't exist, or
        // overwritten if it already exists.
        // The product's InputStream will be closed when copy completes.        
        long bytesRead = 0;
        CachedResourceStatus cachedResourceStatus = null;
        int attempts = 0;
        FileOutputStream output = null;

        try {
            output = FileUtils.openOutputStream(new File(filePath));
            
            // Must cache file in separate thread because PipedOutputStream must write
            // in a separate thread from the thread that PipedInputStream will read - 
            // otherwise deadlock will occur after the pipie's circular buffer fills up,
            // i.e., after chunkSize amount of bytes is read.
            CallableCacheProduct callableCacheProduct = new CallableCacheProduct(source, pos, output, chunkSize);
            Future<CachedResourceStatus> future = null;
            CacheMonitor cacheMonitor = null;
            while (attempts < retryAttempts) {
                attempts++;
                LOGGER.debug("Caching attempt " + attempts);
                try {
                    future = executor.submit(callableCacheProduct);
                    final Timer cacheTimer = new Timer();
                    cacheMonitor = new CacheMonitor(future, callableCacheProduct, cachingMonitorPeriod);
                    LOGGER.debug("Configuring Caching Monitor to run every {} ms", cachingMonitorPeriod);
                    cacheTimer.scheduleAtFixedRate(cacheMonitor, cachingMonitorInitialDelay, cachingMonitorPeriod);
                    cachedResourceStatus = future.get();
                } catch (InterruptedException e) {
                    LOGGER.error("InterruptedException - Unable to store product file {}", filePath, e);
                    cachedResourceStatus = callableCacheProduct.getCachedResourceStatus();
                } catch (ExecutionException e) {
                    LOGGER.error("ExecutionException - Unable to store product file {}", filePath, e);
                    cachedResourceStatus = callableCacheProduct.getCachedResourceStatus();
                } catch (CancellationException e) {
                    LOGGER.error("CancellationException - Unable to store product file {}", filePath, e);
                    cachedResourceStatus = callableCacheProduct.getCachedResourceStatus();
                }
                
                if (cachedResourceStatus.getCachingStatus() == CachingStatus.RESOURCE_CACHING_COMPLETE) {
                    try {
                        LOGGER.debug("Cancelling cacheMonitor");
                        cacheMonitor.cancel();
                        LOGGER.debug("Adding caching key = {} to cache map", key);
                        resourceCache.put(this);
                    } catch (CacheException e) {
                        LOGGER.error("Unable to add cached resource to cache with key = {}", key, e);
                    }
                    break;
                } else { 
                    bytesRead = cachedResourceStatus.getBytesRead();
                    LOGGER.debug("Cached file {} not complete, only read {} bytes", filePath, bytesRead);
                    if (!future.isCancelled()) {
                        LOGGER.debug("Canceling future");
                        future.cancel(true);
                    }
                    output.flush();
                    
                    if (cachedResourceStatus.getCachingStatus() == CachingStatus.PRODUCT_INPUT_STREAM_EXCEPTION) {
                        
                        // Detected exception when reading from original source InputStream - re-retrieve product from the
                        // Source and retry caching it
                        LOGGER.info("Handling product InputStream exception");
                        IOUtils.closeQuietly(source);
                        delay(delayBetweenAttempts);
                        callableCacheProduct = retrieveResource(source, bytesRead, retriever, pos, output);
                        
                    } else if (cachedResourceStatus.getCachingStatus() == CachingStatus.CACHED_FILE_OUTPUT_STREAM_EXCEPTION) {
                        
                        // Detected exception when writing the product data to the product cache directory - 
                        // assume this OutputStream cannot be fixed (e.g., disk full) and just continue streaming
                        // product to the client, i.e., writing to the PipedOutputStream
                        LOGGER.info("Handling FileOutputStream exception");
                        IOUtils.closeQuietly(output);
                        //TODO - delete the cache file?
                        callableCacheProduct = new CallableCacheProduct(source, pos, chunkSize);
                        
                    } else if (cachedResourceStatus.getCachingStatus() == CachingStatus.PIPED_OUTPUT_STREAM_EXCEPTION) {
                        
                        // Detected exception when writing product data to the PipedOutputStream that is being read by
                        // the client - assume client canceled the product retrieval and just continue to cache the file
                        LOGGER.info("Handling PipedOutputStream exception");
                        IOUtils.closeQuietly(pos);
                        callableCacheProduct = new CallableCacheProduct(source, output, chunkSize);
                        
                    } else if (cachedResourceStatus.getCachingStatus() == CachingStatus.RESOURCE_CACHING_INTERRUPTED) {
                        
                        // Caching has been interrupted (possibly CacheMonitor detected too much time being taken to
                        // retrieve a chunk of product data from the InputStream) - re-retrieve product from the Source,
                        // skip forward in the product InputStream the number of bytes already read successfully,
                        // and retry caching it
                        LOGGER.info("Handling interrupt of product caching");
                        IOUtils.closeQuietly(source);
                        delay(delayBetweenAttempts);
                        callableCacheProduct = retrieveResource(source, bytesRead, retriever, pos, output);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Unable to store product file {}", filePath, e);
        } finally {
            IOUtils.closeQuietly(source);
            IOUtils.closeQuietly(pos);
            IOUtils.closeQuietly(output);
        }
    }
    
    private void delay(int delayAmount) {
        try {
            LOGGER.debug(
                    "Waiting {} ms before attempting to re-retrieve and cache product {}",
                    this.delayBetweenAttempts, filePath);
            Thread.sleep(delayBetweenAttempts);
        } catch (InterruptedException e1) {
        }
    }
    
    private CallableCacheProduct retrieveResource(InputStream source, long bytesRead,
            ResourceRetriever retriever, PipedOutputStream pos, OutputStream output) throws IOException {
        CallableCacheProduct callableCacheProduct = null;
        try {
            // Re-fetch product from the Source
            ResourceResponse resourceResponse = retriever.retrieveResource();
            LOGGER.debug("Name of re-retrieved resource = {}", resourceResponse.getResource().getName());
            source = resourceResponse.getResource().getInputStream();
            
            // Skip forward in the product's InputStream the amount of bytes already successfully
            // cached by CallableCacheProduct. This prevents the same bytes being read again and
            // put in the PipedOutputStream that the client is still reading from and in the
            // file being cached to.
            LOGGER.debug("Skipping {} bytes in re-retrieved source InputStream", bytesRead);
            long bytesSkipped = source.skip(bytesRead);
            LOGGER.debug("Actually skipped {} bytes in source InputStream", bytesSkipped);
            callableCacheProduct = new CallableCacheProduct(source, pos, output, chunkSize);
        } catch (ResourceNotFoundException e) {
            LOGGER.warn("Unable to re-retrieve product; cannot cache product file {}", filePath);
        }
        
        return callableCacheProduct;
    }

    @Override
    public byte[] getByteArray() throws IOException {

        return IOUtils.toByteArray(getProduct());
    }

    /**
     * {@inheritDoc} Creates a new inputStream upon request.
     * 
     * @return InputStream of the product, otherwise {@code null} if could not be retrieved.
     */
    public InputStream getInputStream() {
        try {
            return getProduct();
        } catch (IOException e) {
            LOGGER.info("Could not retrieve file [{}]", filePath, e);
            return null;
        }
    }

    public InputStream getProduct() throws IOException {
    	if (filePath == null) {
    		return null;
    	}
    	LOGGER.info("filePath = {}", filePath);
        return FileUtils.openInputStream(new File(filePath));
    }
    
    /**
     * Returns true if this CachedResource's product file exists in the product cache directory.
     * 
     * @return
     */
    public boolean hasProduct() {
    	if (filePath == null) {
    		return false;
    	}
    	return new File(filePath).exists();
    }

    @Override
    public MimeType getMimeType() {
        return mimeType;
    }

    @Override
    public String getMimeTypeValue() {
        return mimeType != null ? mimeType.getBaseType() : null;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public String getName() {
        return resourceName;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    /**
     * Key is also filename of where file is stored.
     * 
     * @return key
     */
    public String getKey() {

        return key;
    }

}
