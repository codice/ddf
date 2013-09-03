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

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.CatalogFramework;
import ddf.catalog.source.Source;

/**
 * The SourcePoller is the scheduler of the task to poll all configured sources at a fixed
 * interval to determine their availability. It is created by the CatalogFramework's blueprint.
 * 
 * An isAvailable() method is included in this class so that the caller, nominally the CatalogFramework,
 * can retrieve the cached availability of a specific source, or have it polled on demand if there is
 * no availability status cached.
 * 
 * @author ddf.isgs@lmco.com
 *
 */
public class SourcePoller {

	private static final int INTERVAL = 60;
	private static final int INITIAL_DELAY = 0;
	private ScheduledExecutorService scheduler;
	
	private ScheduledFuture<?> handle;
	
	private static XLogger logger = new XLogger(LoggerFactory.getLogger(SourcePoller.class));
	private SourcePollerRunner runner;

	
	/**
	 * Constructor to schedule the SourcePollerRunner to execute immediately and at a fixed interval,
	 * currently set at every 60 seconds. This constructor is invoked by the CatalogFramework's blueprint.
	 * 
	 * @param incomingRunner the SourcePollerRunner to use for polling
	 */
	public SourcePoller(SourcePollerRunner incomingRunner)
	{

		this.runner = incomingRunner ;
		
		scheduler = Executors.newScheduledThreadPool(1);

		handle = scheduler.scheduleAtFixedRate(runner, INITIAL_DELAY, INTERVAL, TimeUnit.SECONDS);

	}
	
	
	/**
	 * Checks the availability of the specified source. Returns the last source status
	 * retrieved during the last polling interval. If the specified source has never been polled
	 * (i.e., UNCHECKED status, this method runs the poller immediately for the source and gets 
	 * its current availability.
	 * 
	 * @param source the source to check the availability for
	 * 
	 * @return true if source is available, false otherwise
	 */
	public boolean isAvailable(Source source) {
		boolean result = false;
		logger.trace("Checking source."	) ;
		
		if (source != null) {
			SourceStatus sourceStatus = runner.getStatus(source);
			
			if(sourceStatus == null) {
				
				logger.debug("Unrecognized source [" + source +"/id="+source.getId() +"]" );
				
				/*
				 * If the sourceStatus equals null, then we don't have information
				 * as to whether that source exists, therefore we return true so as
				 * to not stop threads from doing their own check.
				 */
				result = true;
			} else if (sourceStatus == SourceStatus.AVAILABLE) {
				result = true;
			} else if (sourceStatus == SourceStatus.UNAVAILABLE
                || sourceStatus == SourceStatus.UNCHECKED) {
                result = false;
            }
		} else {
			logger.debug(" Source is null. Returning false. ");
			result = false;
		}
		return result;
	}
	

	/**
	 * Cancels the {@link SourcePollerRunner} thread that had been previously scheduled to run at specific intervals.
	 * Invoked by the CatalogFramework's blueprint when the framework is unregistered/uninstalled.
	 * 
	 * @param framework unused, but required by blueprint
	 * @param properties unused, but required by blueprint
	 */
	public void cancel(CatalogFramework framework, Map properties) {
		
		logger.info("Cancelling scheduled polling.");
		
        runner.shutdown();

		handle.cancel(true) ;
		
		scheduler.shutdownNow() ;
		
	}
	
	
	/** 
	 * Start method for this poller, invoked by the CatalogFramework's blueprint when the framework
	 * is registered/installed. No logic is executed except for logging the framework name.
	 * 
	 * @param framework the catalog framework being started
	 * @param properties unused, but required by blueprint
	 */
	public void start(CatalogFramework framework, Map properties)	 {
		String frameworkString = "" ;
		if(framework != null) {
			frameworkString = framework.toString();
		}
		logger.debug("Framework started for ["+frameworkString+"]") ;
		
	}
	
}


// States that a source (Catalog Provider, Federated Source, or Connected Source) can be in
enum SourceStatus {

	UNCHECKED, AVAILABLE, UNAVAILABLE
}


