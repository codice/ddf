/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.source.Source;
import ddf.catalog.source.SourceMonitor;

/**
 * The poller to check the availability of all configured sources. This class is instantiated
 * by the CatalogFramework's blueprint and is scheduled by the {@link SourcePoller} to execute
 * at a fixed rate, i.e., the polling interval. 
 * 
 * This class maintains a list of all of the sources
 * to be polled for their availability. Sources are added to this list when they come online and
 * when they are deleted. A cached map is maintained of all the sources and their last availability
 * states.
 * 
 * @author ddf.isgs@lmco.com
 *
 */
public class SourcePollerRunner implements Runnable {

	private List<Source> sources;
	private Map<Source, SourceStatus> status = new ConcurrentHashMap<Source, SourceStatus>();
	private static XLogger logger = new XLogger(LoggerFactory.getLogger(SourcePollerRunner.class));

    private ExecutorService pool;
    private Map<Source, Lock> sourceStatusThreadLocks = new ConcurrentHashMap<Source, Lock>();


	
	/**
	 * Creates an empty list of {@link Source} sources to be polled for availability.
	 * This constructor is invoked by the CatalogFramework's blueprint.
	 */
	public SourcePollerRunner() {

		logger.info("Creating source poller runner.");
        sources = new CopyOnWriteArrayList<Source>();
	}

	
	/**
	 * Checks the availability of each source in the list of sources
	 * to be polled.
	 */
	@Override
	public void run() {

		logger.trace("RUNNER checking source statuses");

		for (Source source : sources) {

			if (source != null) {

				checkStatus(source);

			}

		}

	}

	
	/**
	 * Checks if the specified source is available, updating the internally maintained
	 * map of sources and their status.  Lock ensures only one status thread is running
	 * per source.
	 * 
	 * @param source the source to check if it is available
	 */
    private void checkStatus(final Source source) {

        if (pool == null) {
            pool = Executors.newCachedThreadPool();

        }
        final Runnable statusRunner = new Runnable() {

            public void run() {

                Lock sourceStatusThreadLock = sourceStatusThreadLocks.get(source);
                if (sourceStatusThreadLock.tryLock()) {
                    logger.debug("Acquired lock for Source [" + source
                            + "] with id [" + source.getId() + "] ");
                    boolean available = false;

                    try {
                        logger.debug("Checking Source [" + source
                                + "] with id [" + source.getId()
                                + "] availability.");
                        SourceMonitor monitor = new SourceMonitor() {

                            @Override
                            public void setAvailable() {
                                status.put(source, SourceStatus.AVAILABLE);
                                logger.info("Source [" + source + "] with id ["
                                        + source.getId() + "] is AVAILABLE");
                            }

                            @Override
                            public void setUnavailable() {
                                status.put(source, SourceStatus.UNAVAILABLE);
                                logger.info("Source [" + source + "] with id ["
                                        + source.getId() + "] is UNAVAILABLE");
                            }
                        };

                        available = source.isAvailable(monitor);

                        logger.debug("Source [" + source + "] with id ["
                                + source.getId() + "] isAvailable? "
                                + available);
                        
                        if (available) {
                            status.put(source, SourceStatus.AVAILABLE);
                        } else {
                            status.put(source, SourceStatus.UNAVAILABLE);
                        }
                        
                    } catch (Exception e) {
                        status.put(source, SourceStatus.UNAVAILABLE);
                        logger.debug(
                                "Source ["
                                        + source
                                        + "] did not return properly with availability.",
                                e);
                    } finally {
                        // release the lock acquired initially
                        sourceStatusThreadLock.unlock();
                        logger.debug("Released lock for Source [" + source
                                + "] with id [" + source.getId() + "] ");

                    }
                } else {
                    logger.debug("Unable to get lock for Source [" + source
                            + "] with id [" + source.getId() + "].  A status thread is already running.");
                }
            }
        };
        pool.execute(statusRunner);
    }
	
	
	/**
	 * Adds the {@link Source} instance to the list and sets its current status
	 * to UNCHECKED, indicating it will checked at the next polling interval.
	 * 
	 * @param source the source to add to the list
	 */
	public void bind(Source source) {
		
		logger.info("Binding source: " + source);
		if (source != null) {
			logger.debug("Marking new source " + source+ " as UNCHECKED.") ;
			sources.add(source);
			sourceStatusThreadLocks.put(source, new ReentrantLock());
			status.put(source, SourceStatus.UNCHECKED);
            checkStatus(source);

		}
	}
	
	
	/**
	 * Removes the {@link Source} instance from the list of references
	 * so that its availability is no longer polled.
	 * 
	 * @param source the source to remove from the list
	 */
	public void unbind(Source source) {
		logger.info("Unbinding source [" + source +"]");
		if(source != null) {
			status.remove(source) ;
			sources.remove(source);
			sourceStatusThreadLocks.remove(source);
		}
	}

	
	/**
	 * Retrieve the current availability of the specified source.
	 * 
	 * @param source the source to retrieve the availability for
	 * 
	 * @return the source's status
	 */
	public SourceStatus getStatus(Source source) {
		return status.get(source);
	}
	
    /**
     * 
     * Calls the @link ExecutorService to shutdown immediately
     */
    public void shutdown() {
        logger.trace("Shutting down status threads");
        if (pool != null) {
            pool.shutdownNow();
        }
        logger.trace("Status threads shut down");
    }
	
}
