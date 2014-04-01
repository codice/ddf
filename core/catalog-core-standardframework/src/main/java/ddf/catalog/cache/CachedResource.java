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

import com.google.common.io.ByteSource;
import com.google.common.io.CountingOutputStream;
import com.google.common.io.FileBackedOutputStream;

import ddf.cache.CacheException;
import ddf.catalog.event.retrievestatus.RetrievalStatusEventPublisher;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.impl.ResourceImpl;
import ddf.catalog.resourceretriever.ResourceRetriever;

/**
 * Contains the details of a Resource including where it is stored and how to retrieve that Resource
 * locally.
 */
public class CachedResource extends InputStream implements Resource, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedResource.class);
    
    private enum CachingState {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    };

    /**
     * Initial delay, in ms, before the Caching Monitor TimerTask is started
     */
    static final int DEFAULT_CACHING_MONITOR_INITIAL_DELAY = 1000;  // 1 second
    
    private static final int KB = 1024;
    private static final int MB = 1024 * KB;
    
    /**
     * Size of byte buffer (chunk) for each read from the source InputStream
     */
    private static int DEFAULT_CHUNK_SIZE = 1 * MB;
    
    private static int DEFAULT_FILE_BACKED_OUTPUT_STREAM_THRESHOLD = 32 * KB;

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
     * Maximum number of attempts to try and retrieve product
     */
    private int maxRetryAttempts;
    
    /**
     * Delay, in ms, between attempts to cache resource to disk
     */
    private int delayBetweenAttempts;
    
    private int cachingMonitorInitialDelay;
    
    private boolean cacheWhenCanceled = false;
    
    /**
     * Transient because these attributes are not Serializable or relevant to being cached
     */
    private transient Future<CachedResourceStatus> cachingFuture;
    
    private transient CallableCacheProduct callableCacheProduct;
    
    private transient ExecutorService executor;
    
    private transient FileBackedOutputStream fbos;
    
    private transient CountingOutputStream countingFbos;
    
    private transient ByteSource fbosByteSource;
    
    private transient long fbosBytesRead = 0;
    
    private transient CachingState cachingState;
    
    private transient RetrievalStatusEventPublisher eventPublisher;
    

    public CachedResource(String productCacheDirectory, int maxRetryAttempts,
            int delayBetweenAttempts, long cacheMonitoringPeriod,
            RetrievalStatusEventPublisher eventPublisher) {
        this.productCacheDirectory = productCacheDirectory;
        this.cachingMonitorPeriod = cacheMonitoringPeriod;
        this.cachingMonitorInitialDelay = DEFAULT_CACHING_MONITOR_INITIAL_DELAY;
        this.maxRetryAttempts = maxRetryAttempts;
        this.delayBetweenAttempts = delayBetweenAttempts;
        this.chunkSize = DEFAULT_CHUNK_SIZE;
        this.executor = Executors.newCachedThreadPool();
        this.cachingState = CachingState.NOT_STARTED;
        this.eventPublisher = eventPublisher;
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
    
    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }
    
    /**
     * @param chunkSize how many bytes to read at a time from the resource's @InputStream
     */
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
    
    /**
     * Store (cache) the resource to a file in the product cache directory.
     * 
     * @param key
     * @param resourceResponse
     * @param resourceCache
     * @param retriever
     * @return
     * @throws CacheException
     */
    public Resource store(String key, final ResourceResponse resourceResponse,
            final ResourceCache resourceCache, final ResourceRetriever retriever, final boolean cacheWhenCanceled) throws CacheException {

        LOGGER.info("ENTERING: store()");
        
        if (key == null) {
            throw new CacheException("Cannot cache file because cache key is null");
        }
        
        this.key = key;
        this.cacheWhenCanceled = cacheWhenCanceled;
        
        if (resourceResponse == null) {
            throw new CacheException("Cannot cache file because ResourceResponse is null");
        }
        
        eventPublisher.postRetrievalStatus(resourceResponse,
                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_STARTED, 0L);
        
        Resource resource = resourceResponse.getResource();

        if (resource == null) {
            throw new CacheException("Cannot cache file because Resource is null");
        }

        // Fully qualified path to cache file that will be written to.
        // Example:
        //     <INSTALL-DIR>/data/product-cache/<source-id>-<metacard-id>
        //     <INSTALL-DIR>/data/product-cache/ddf.distribution-abc123
        filePath = FilenameUtils.concat(productCacheDirectory, key);
        
        fbos = new FileBackedOutputStream(DEFAULT_FILE_BACKED_OUTPUT_STREAM_THRESHOLD);
        fbosByteSource = fbos.asByteSource();
        countingFbos = new CountingOutputStream(fbos);
        
        mimeType = resource.getMimeType();
        resourceName = resource.getName();
        
        // Create new Resource to return that will encapsulate the PipedInputStream
        // that will be read by the client simultaneously as the product is cached
        // to disk
        ResourceImpl newResource = new ResourceImpl(this, mimeType, resourceName);
        
        LOGGER.debug("Copying resource to filepath = {}", filePath);
        
        // Get handle to retrieved product's InputStream
        final InputStream source = resource.getInputStream();
        
        Runnable cacheThread = new Runnable() {
            @Override
            public void run() {
                cacheFile(source, countingFbos, filePath, resourceCache, retriever, resourceResponse);
            }
        };
        
        executor.submit(cacheThread);
        
        LOGGER.debug("EXITING: store()");

        return newResource;
    }
    
    /**
     * Executes in a separate thread, reading the resource's InputStream and concurrently caching it to
     * a file in the product cache directory and the PipedOutputStream being read by the client (endpoint).
     * 
     * @param source
     * @param pos
     * @param filePath
     * @param resourceCache
     * @param retriever
     */
    private void cacheFile(InputStream source, CountingOutputStream countingFbos,            
            String filePath, ResourceCache resourceCache, ResourceRetriever retriever,
            ResourceResponse resourceResponse) {
                
        long bytesRead = 0;
        CachedResourceStatus cachedResourceStatus = null;
        int retryAttempts = 0;
        FileOutputStream fos = null;

        try {
            fos = FileUtils.openOutputStream(new File(filePath));
            
            // Must cache file in separate thread (Callable) because PipedOutputStream must write
            // in a separate thread from the thread that PipedInputStream will read - 
            // otherwise deadlock will occur after the pipie's circular buffer fills up,
            // i.e., after chunkSize amount of bytes is read.            
            callableCacheProduct = new CallableCacheProduct(source, countingFbos, fos, chunkSize);
            cachingFuture = null;
            CacheMonitor cacheMonitor = null;
            this.cachingState = CachingState.IN_PROGRESS;
            
            while (retryAttempts < maxRetryAttempts) {
                if (callableCacheProduct == null) {
                    // This usually occurs on retry attempts to cache and the CallableCacheProduct cannot be
                    // successfully created. In this case, a partial cache file may have been created from the
                    // previous caching attempt(s) and needs to be deleted from the product cache directory.
                    LOGGER.debug("CallableCacheProduct is null - can't do any caching, delete partially cached file");
                    IOUtils.closeQuietly(fos);
                    FileUtils.deleteQuietly(new File(filePath));
                    break;
                }
                retryAttempts++;
                LOGGER.debug("Caching attempt " + retryAttempts);
                ExecutorService cachingExecutor = null;
                try {
                    cachingExecutor = Executors.newCachedThreadPool();
                    cachingFuture = cachingExecutor.submit(callableCacheProduct);
                    
                    // Monitor to watch that bytes are continually being read from the resource's InputStream.
                    // This monitor is used to detect if there are long pauses or network connection loss during
                    // the product retrieval. If such a "gap" is detected, the Callable will be canceled and a
                    // new caching attempt (retry) will be started.
                    final Timer cacheTimer = new Timer();
                    cacheMonitor = new CacheMonitor(cachingFuture, callableCacheProduct, cachingMonitorPeriod);
                    LOGGER.debug("Configuring Caching Monitor to run every {} ms", cachingMonitorPeriod);
                    cacheTimer.scheduleAtFixedRate(cacheMonitor, cachingMonitorInitialDelay, cachingMonitorPeriod);
                    cachedResourceStatus = cachingFuture.get();
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
                
                LOGGER.debug("cachedResourceStatus = {}", cachedResourceStatus);
                
                if (cachedResourceStatus.getCachingStatus() == CachingStatus.RESOURCE_CACHING_COMPLETE) {
                    try {
                        LOGGER.debug("Cancelling cacheMonitor");
                        cacheMonitor.cancel();
                        LOGGER.debug("Sending event");
                        eventPublisher.postRetrievalStatus(resourceResponse,
                                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_COMPLETE,
                                cachedResourceStatus.getBytesRead());
                        LOGGER.debug("Adding caching key = {} to cache map", key);
                        resourceCache.put(this);
                    } catch (CacheException e) {
                        LOGGER.error("Unable to add cached resource to cache with key = {}", key, e);
                    }
                    break;
                } else { 
                    bytesRead = cachedResourceStatus.getBytesRead();
                    LOGGER.debug("Cached file {} not complete, only read {} bytes", filePath, bytesRead);
                    fos.flush();
                    
                    // Synchronized so that the Callable is not shutdown while in the middle of writing to the
                    // FileBackedOutputStream and cache file (need to keep both of these in sync with number of bytes
                    // written to each of them).
                    synchronized(callableCacheProduct) {
                        if (!cachingFuture.isCancelled()) {
                            LOGGER.debug("Canceling future");
                            cachingFuture.cancel(true);
                        }
                        
                        // Need to do shutdownNow() so that any blocking reads still active at the OS
                        // (native) level by FileOutputStream are shutdown. (The shutdown() method will
                        // not suffice here, nor did the future.cancel() suffice).
                        cachingExecutor.shutdownNow();
                    }                    
                    
                    if (cachedResourceStatus.getCachingStatus() == CachingStatus.PRODUCT_INPUT_STREAM_EXCEPTION) {
                        
                        // Detected exception when reading from product's InputStream - re-retrieve product from the
                        // Source and retry caching it
                        LOGGER.info("Handling product InputStream exception");
                        eventPublisher.postRetrievalStatus(resourceResponse,
                                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_RETRYING,
                                cachedResourceStatus.getBytesRead());
                        IOUtils.closeQuietly(source);
                        source = null;
                        delay(delayBetweenAttempts);
                        callableCacheProduct = retrieveResource(bytesRead, retriever, countingFbos, fos);           
                    } else if (cachedResourceStatus.getCachingStatus() == CachingStatus.CACHED_FILE_OUTPUT_STREAM_EXCEPTION) {
                        
                        // Detected exception when writing the product data to the product cache directory - 
                        // assume this OutputStream cannot be fixed (e.g., disk full) and just continue streaming
                        // product to the client, i.e., writing to the PipedOutputStream
                        LOGGER.info("Handling FileOutputStream exception");
                        eventPublisher.postRetrievalStatus(resourceResponse,
                                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_RETRYING,
                                cachedResourceStatus.getBytesRead());
                        deleteCacheFile(fos);
                        callableCacheProduct = new CallableCacheProduct(source, countingFbos, null, chunkSize);
                    } else if (cachedResourceStatus.getCachingStatus() == CachingStatus.CLIENT_OUTPUT_STREAM_EXCEPTION) {
                        
                        // Detected exception when writing product data to the output stream (FileBackedOutputStream) that 
                        // is being read by the client - assume client cancelled the product retrieval 
                        // and just continue to cache the file
                        LOGGER.info("Handling FileBackedOutputStream exception");
                        eventPublisher.postRetrievalStatus(resourceResponse,
                                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_CANCELLED, 0L);
                        IOUtils.closeQuietly(fbos);
                        IOUtils.closeQuietly(countingFbos);
                        LOGGER.debug("Cancelling cacheMonitor");
                        cacheMonitor.cancel();
                        callableCacheProduct = new CallableCacheProduct(source, null, fos, chunkSize);
                        
                    } else if (cachedResourceStatus.getCachingStatus() == CachingStatus.RESOURCE_CACHING_CANCELED) {
                        LOGGER.info("Handling client cancellation of product download");
                        LOGGER.debug("Cancelling cacheMonitor");
                        cacheMonitor.cancel();
                        eventPublisher.postRetrievalStatus(resourceResponse,
                                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_CANCELLED, 0L);
                        deleteCacheFile(fos);
                        break;
                        
                    } else if (cachedResourceStatus.getCachingStatus() == CachingStatus.RESOURCE_CACHING_INTERRUPTED) {
                        
                        // Caching has been interrupted (possibly CacheMonitor detected too much time being taken to
                        // retrieve a chunk of product data from the InputStream) - re-retrieve product from the Source,
                        // skip forward in the product InputStream the number of bytes already read successfully,
                        // and retry caching it
                        LOGGER.info("Handling interrupt of product caching - closing source InputStream");
                        
                        // Set InputStream used on previous attempt to null so that any attempt to close it
                        // will not fail (CXF's DelegatingInputStream, which is the underlying InputStream being used,
                        // does a consume() which is a read() as part of its close() operation and this will result 
                        // in a blocking read)
                        source = null;
                        eventPublisher.postRetrievalStatus(resourceResponse,
                                RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_RETRYING,
                                cachedResourceStatus.getBytesRead());
                        delay(delayBetweenAttempts);
                        callableCacheProduct = retrieveResource(bytesRead, retriever, countingFbos, fos);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Unable to store product file {}", filePath, e);
            eventPublisher.postRetrievalStatus(resourceResponse, RetrievalStatusEventPublisher.PRODUCT_RETRIEVAL_FAILED,
                    cachedResourceStatus.getBytesRead());
        } finally {         
            // If caching was not successful, then remove this product from the pending cache list
            // (Otherwise partially cached files will remain in pending list and returned to subsequent clients)
            if (cachedResourceStatus.getCachingStatus() != CachingStatus.RESOURCE_CACHING_COMPLETE) {
                resourceCache.removePendingCacheEntry(key);
                this.cachingState = CachingState.FAILED;
            } else {
                this.cachingState = CachingState.COMPLETED;
            }
            // Closes FileBackedOutputStream and deletes its underlying tmp file (if any)
            try {
                fbos.reset();
            } catch (IOException e) {
                LOGGER.info("Unable to reset FileBackedOutputStream - its tmp file may still be in <INSTALL_DIR>/data/tmp");
            }
            IOUtils.closeQuietly(countingFbos);
            IOUtils.closeQuietly(fos);
            LOGGER.debug("Closing source InputStream");
            IOUtils.closeQuietly(source);
            LOGGER.debug("Closed source InputStream");
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
    
    private CallableCacheProduct retrieveResource(long bytesRead,
            ResourceRetriever retriever, CountingOutputStream countingFbos, FileOutputStream fos) {
        CallableCacheProduct callableCacheProduct = null;
        try {
            LOGGER.debug("Attempting to re-retrieve resource"); 
            // Re-fetch product from the Source
            ResourceResponse resourceResponse = retriever.retrieveResource();
            LOGGER.debug("Name of re-retrieved resource = {}", resourceResponse.getResource().getName());
            InputStream source = resourceResponse.getResource().getInputStream();
            
            // Skip forward in the product's InputStream the amount of bytes already successfully
            // cached by CallableCacheProduct. This prevents the same bytes being read again and
            // put in the PipedOutputStream that the client is still reading from and in the
            // file being cached to.
            LOGGER.debug("Skipping {} bytes in re-retrieved source InputStream", bytesRead);
            long bytesSkipped = source.skip(bytesRead);
            LOGGER.debug("Actually skipped {} bytes in source InputStream", bytesSkipped);
            callableCacheProduct = new CallableCacheProduct(source, countingFbos, fos, chunkSize);
            
            // So that Callable can account for bytes read in previous download attempt(s)
            callableCacheProduct.setBytesRead(bytesRead);
        } catch (ResourceNotFoundException e) {
            LOGGER.warn("Unable to re-retrieve product; cannot cache product file {}", filePath);
        } catch (ResourceNotSupportedException e) {
            LOGGER.warn("Unable to re-retrieve product; cannot cache product file {}", filePath);
        } catch (IOException e) {
            LOGGER.warn("Unable to re-retrieve product; cannot cache product file {}", filePath);
        }
        
        return callableCacheProduct;
    }
    
    private void deleteCacheFile(FileOutputStream fos) {
        LOGGER.debug("Deleting partially cached file {}", filePath);
        IOUtils.closeQuietly(fos);
        
        // Delete the cache file since it will no longer be written to and it currently has
        // incomplete or corrupted data in it
        boolean result = FileUtils.deleteQuietly(new File(filePath));
        LOGGER.debug("result of deleting partial cache file = {}", result);
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
    
    @Override
    public void close() throws IOException {
        LOGGER.debug("ENTERING: close()");
        InputStream is = fbosByteSource.openStream();
        is.close();
        
        // Stop caching the product unless admin specifically
        // set option to continue caching even if client cancels
        // the product download
        if (!cacheWhenCanceled) {
            // Stop the caching thread
            // synchronized so that Callable can finish any writing to OutputStreams before being canceled
            synchronized(callableCacheProduct) {
                LOGGER.debug("Setting cancelCaching on CallableCacheProduct thread");
                callableCacheProduct.setCancelCaching(true);
                boolean status = cachingFuture.cancel(true);
                LOGGER.debug("cachingFuture cancelling status = {}", status);
            }
            
            // Resetting the FileBackedOutputStream should delete the tmp file
            // it created.
            LOGGER.debug("Resetting FBOS");
            fbos.reset();
        }
    }

    @Override
    public int read() throws IOException {
        LOGGER.trace("ENTERING: read()");
        int byteRead = 0;
        InputStream is = fbosByteSource.openStream();
        if (countingFbos.getCount() > fbosBytesRead) {
            is.skip(fbosBytesRead);
            byteRead = is.read();
            fbosBytesRead++;
        }
        is.close();
        return byteRead;
    }
    
    @Override
    public int read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }
        
        int numBytesRead = 0;
        
        long fbosCount = countingFbos.getCount();
        if (fbosCount != fbosBytesRead) {
            LOGGER.trace("fbos count = {}, fbosBytesRead = {}", fbosCount, fbosBytesRead);
        }
        
        // More bytes written to FileBackedOutputStream than have been read by the client -
        // ok to skip and do a read
        if (fbosCount > fbosBytesRead) {
            numBytesRead = readFromFbosInputStream(b, off, len);
        } else if (fbosCount > 0) {
            // bytes have been written to the FileBackedOutputStream
            numBytesRead = readFromFbosInputStream(b, off, len);
            if (numBytesRead == -1 && fbosCount == fbosBytesRead && 
               (cachingState == CachingState.COMPLETED || cachingState == CachingState.FAILED)) {
                LOGGER.debug("Sending EOF");
                // Client is done reading from this FileBackedOutputStream, so can
                // delete the backing file it created from the temp directory
                fbos.reset();
            } else if (numBytesRead <= 0) {
                LOGGER.debug("numBytesRead <= 0 but client hasn't read all of the data from FBOS - block and read");
                while (cachingState == CachingState.IN_PROGRESS || (fbosCount >= fbosBytesRead && cachingState != CachingState.FAILED)) {
                    numBytesRead = readFromFbosInputStream(b, off, len);
                    if (numBytesRead > 0) {
                        LOGGER.debug("retry: numBytesRead = {}", numBytesRead);
                        break;
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                }
                if (cachingState == CachingState.FAILED) {
                    LOGGER.debug("Throwing IOException because caching failed - cannot retrieve product");
                    throw new IOException("caching failed - cannot retrieve product");
                }
            }
        }

        return numBytesRead;
    }
    
    private int readFromFbosInputStream(byte[] b, int off, int len) throws IOException {
        InputStream is = fbosByteSource.openStream();
        is.skip(fbosBytesRead);
        int numBytesRead = is.read(b, off, len);
        LOGGER.trace("numBytesRead = {}", numBytesRead);
        if (numBytesRead > 0) {
            fbosBytesRead += numBytesRead;
        }
        
        is.close();
        
        return numBytesRead;
    }
}
