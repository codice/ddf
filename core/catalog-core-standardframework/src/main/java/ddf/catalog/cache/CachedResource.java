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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.activation.MimeType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.cache.CacheException;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.impl.ResourceImpl;

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

    private static final long DEFAULT_CACHING_MONITOR_PERIOD = 5000;  // 5 seconds
    
    private static int DEFAULT_CHUNK_SIZE = 1024 * 1024;  // 1 MB
    
    private static int TIMEOUT_PER_CHUNK_IN_MS = 10000;  // 10 seconds

    private static int DEFAULT_DELAY_BETWEEN_ATTEMPTS = 1000;

    /** Directory for products cached to file system */
    private String productCacheDirectory;

    private String filePath;

    private MimeType mimeType;

    private String resourceName;

    private long size = -1L;

    private String key;
    
    private long cachingMonitorPeriod;
    
    /**
     * Delay, in ms, between attempts to cache resource to disk
     */
    private int delayBetweenAttempts;
    
    private transient ExecutorService executor;
    

    public CachedResource(String productCacheDirectory) {
        this.productCacheDirectory = productCacheDirectory;
        this.cachingMonitorPeriod = DEFAULT_CACHING_MONITOR_PERIOD;
        this.delayBetweenAttempts= DEFAULT_DELAY_BETWEEN_ATTEMPTS;
        this.executor = Executors.newCachedThreadPool();
    }
    
    public void setCachinigMonitorPeriod(long period) {
        this.cachingMonitorPeriod = period;
    }
    
    public void setDelayBetweenAttempts(int delay) {
        this.delayBetweenAttempts = delay;
    }
    
    public Resource store(Metacard metacard, ResourceResponse resourceResponse, final ResourceCache resourceCache) throws CacheException {

        LOGGER.info("ENTERING: store() with TeeInputStream");
        Resource resource = resourceResponse.getResource();

        if (resource == null) {
            throw new CacheException("Cannot cache file because Resource is null");
        }

        CacheKey keyMaker = new CacheKey(metacard, resourceResponse.getRequest());

        key = keyMaker.generateKey();

        LOGGER.debug("Cache file with key {} ", key);

        filePath = FilenameUtils.concat(productCacheDirectory, key);

        // Create a piped input stream for the endpoint/client to read from
        final PipedInputStream pis = new PipedInputStream(DEFAULT_CHUNK_SIZE);
        
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
                long size = cacheFile(source, pos, filePath, resourceCache, 3);
                LOGGER.info("Done caching - size = {}", size);
            }
        };
        
        // ExecutorService is local because this class (CachedResource) must be
        // Serializable in order for hazelcast to be able to cache it.
        //TODO: or should it be private transient???
        //ExecutorService executor = Executors.newCachedThreadPool();
        executor.submit(cacheThread);
        
        LOGGER.debug("EXITING: store()");

        return newResource;
    }
    
    private long cacheFile(InputStream source, PipedOutputStream pos,
            String filePath, ResourceCache resourceCache, int retryAttempts) {

        // Copy product's bytes from InputStream to product cache file.
        // Resource cache file will be created if it doesn't exist, or
        // overwritten if it already exists.
        // The product's InputStream will be closed when copy completes.        
        long bytesRead = 0;
        int attempts = 0;
        TeeInputStream tee = null;
        FileOutputStream output = null;
        //ExecutorService executor = Executors.newCachedThreadPool();
        try {
            // Since product's input stream needs to be copied to both the cache
            // directory (FileOutputStream) and to the client (PipedOutputStream),
            // need to tee the input stream.
            tee = new TeeInputStream(source, pos, true);

            output = FileUtils.openOutputStream(new File(filePath));
            
            // Must cache file in separate thread because PipedOutputStream must write
            // in a separate thread from the thread that PipedInputStream will read - 
            // otherwise deadlock will occur after the pipie's circular buffer fills up,
            // i.e., after chunkSize amount of bytes is read.
            CallableCacheProduct callableCacheProduct = new CallableCacheProduct(tee, output);
            //ExecutorService executor = Executors.newCachedThreadPool();
            Future<Long> future = null;
            CacheMonitor cacheMonitor = null;
            while (attempts < retryAttempts) {
                attempts++;
                LOGGER.info("attempt " + attempts);
                try {
                    future = executor.submit(callableCacheProduct);
                    final Timer cachingMonitor = new Timer();
                    cacheMonitor = new CacheMonitor(future, callableCacheProduct);
                    LOGGER.info("Configuring Caching Monitor to run every {} seconds", cachingMonitorPeriod);
                    cachingMonitor.scheduleAtFixedRate(cacheMonitor, 1000, cachingMonitorPeriod);
                    bytesRead = future.get();
                } catch (InterruptedException e) {
                    LOGGER.error("InterruptedException - Unable to store product file {}", filePath, e);
                } catch (ExecutionException e) {
                    LOGGER.error("ExecutionException - Unable to store product file {}", filePath, e);
                } catch (CancellationException e) {
                    LOGGER.error("CancellationException - Unable to store product file {}", filePath, e);
                    bytesRead = cacheMonitor.getBytesRead();
                }
                if (bytesRead != RESOURCE_CACHING_COMPLETE) {
                    LOGGER.info("Cached file {} not complete, only read {} bytes", filePath, bytesRead);
                    LOGGER.info("Cancelling future");
                    future.cancel(true);
                    output.flush();
//                    IOUtils.closeQuietly(output);
                    LOGGER.info("Skipping {} bytes in source InputStream and tee InputStream", bytesRead);
                    long bytesSkipped = source.skip(bytesRead);
                    LOGGER.info("Actually skipped {} bytes in source InputStream", bytesSkipped);
                    bytesSkipped = tee.skip(bytesRead);
                    LOGGER.info("Actually skipped {} bytes in tee InputStream", bytesSkipped);
                } else {
                    try {
                        LOGGER.info("Cancelling cacheMonitor");
                        cacheMonitor.cancel();
                        LOGGER.info("Adding caching key = {} to cache map", key);
                        resourceCache.put(this);
                    } catch (CacheException e) {
                        LOGGER.error("Unable to add cached resource to cache with key = {}", key, e);
                    }
                    break;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Unable to store product file {}", filePath, e);
        } finally {
            IOUtils.closeQuietly(tee);
            IOUtils.closeQuietly(output);
            //executor.shutdown();
        }
        
        return bytesRead;
    }
    
    private void delay(int amount) {
        try {
            Thread.sleep(amount);
        } catch (InterruptedException e) {
        }
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

////////////////////////////////////////////////////////////////////////////////
    
    private class CallableCacheProduct implements Callable<Long> {

        protected static final long CACHING_MONITOR_PERIOD = 5000;  // 5 seconds

        private TeeInputStream tee = null;

        private FileOutputStream output = null;

//        private int timeoutPerChunk;

        private long bytesRead = 0;
        
        private boolean interruptCaching = false;

        public CallableCacheProduct(TeeInputStream tee, FileOutputStream output) {  //, int timeoutPerChunk) {
            this.tee = tee;
            this.output = output;
//            this.timeoutPerChunk = timeoutPerChunk;
        }

        public void skip(int numBytes) {
            LOGGER.info("Skipping {} bytes in product input stream", numBytes);
            try {
                long numBytesSkipped = tee.skip(numBytes);
                LOGGER.info("Actually skipped {} bytes", numBytesSkipped);
            } catch (IOException e) {
                LOGGER.error("Unable to skip {} bytes", numBytes, e);
            }
        }
        
        public long getBytesRead() {
            return bytesRead;
        }
        
//        public void setInterruptCaching(boolean interruptCaching) {
//            LOGGER.info("Setting interruptCaching = " + interruptCaching);
//            this.interruptCaching = interruptCaching;
//        }

        @Override
        public Long call() throws IOException {
            int chunkCount = 0;
            
            byte[] buffer = new byte[DEFAULT_CHUNK_SIZE];
            int n = 0;

            while (!interruptCaching) {
                chunkCount++;
                
                // This read will block if the PipedOutputStream's circular buffer is filled
                // before the client reads from it via the PipedInputStream
                //LOGGER.info("BEFORE tee read()");
                n = tee.read(buffer);
                //LOGGER.info("AFTER tee read() - n = {}", n);
                if (n == END_OF_FILE) {
                    break;
                }
                output.write(buffer, 0, n);
                bytesRead += n;
                //Prints LOTS of log stmts   LOGGER.info("chunkCount = " + chunkCount + ",  bytesRead = " + bytesRead);
            }

//            if (interruptCaching) {
//                LOGGER.info("Caching interrupted - returning " + bytesRead + " bytes read.");
//                output.flush();
//                IOUtils.closeQuietly(output);
//                return bytesRead;
//            }
            LOGGER.info("Returning -1 to indicate entire file cached successfully");
            return RESOURCE_CACHING_COMPLETE;
        }
    }
    
    private class CacheMonitor extends TimerTask
    {
        private long previousBytesRead = 0;
        private Future<?> future;
        private CallableCacheProduct callableCacheProduct;
        
        public CacheMonitor(Future<?> future, CallableCacheProduct callableCacheProduct) {
            this.future = future;
            this.callableCacheProduct = callableCacheProduct;
        }        
        
        public long getBytesRead() {
            return previousBytesRead;
        }

        @Override
        public void run() {
            long chunkByteCount = callableCacheProduct.getBytesRead() - previousBytesRead;
            if (chunkByteCount > 0) {
                LOGGER.info("Cached {} bytes in last {} seconds.", chunkByteCount, cachingMonitorPeriod);
                previousBytesRead = callableCacheProduct.getBytesRead();
            } else {
                LOGGER.info("No bytes cached in last {} seconds - cancelling CacheMonitor and caching future (thread).", cachingMonitorPeriod);
                cancel();
                // Stop the caching thread
                boolean status = future.cancel(true);
                LOGGER.info("future cancelling status = {}", status);
//                callableCacheProduct.setInterruptCaching(true);
            }
        }
    }
}
