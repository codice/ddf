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


import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Wrapper used for logging any errors associated with ingesting to the catalog
 * 
 * Format similar to the DDF SecurityLogger class
 * 
 * <p><b>
 * This code is internally used in the catalog framework, and it is experimental 
 * and may change or be removed in a future version of the library.
 * </b></p>
 * 
 * @author Alex Lamar
 *
 */
public final class IngestLogger {
	
	private static final String LOGGER_NAME = "ingestLogger";
	
	private static final Logger INGEST_LOGGER = Logger.getLogger(LOGGER_NAME);

    private IngestLogger()
    {

    }

    public static void trace(String log, Throwable throwable)
    {
        INGEST_LOGGER.trace(log, throwable);
    }

    public static void trace(String log)
    {
        INGEST_LOGGER.trace(log);
    }

    public static void debug(String log, Throwable throwable)
    {
        INGEST_LOGGER.debug(log, throwable);
    }

    public static void debug(String log)
    {
        INGEST_LOGGER.debug(log);
    }

    public static void info(String log, Throwable throwable)
    {
        INGEST_LOGGER.info(log, throwable);
    }

    public static void info(String log)
    {
        INGEST_LOGGER.info(log);
    }

    public static void warn(String log, Throwable throwable)
    {
        INGEST_LOGGER.warn(log, throwable);
    }

    public static void warn(String log)
    {
        INGEST_LOGGER.warn(log);
    }

    public static void error(String log, Throwable throwable)
    {
        INGEST_LOGGER.error(log, throwable);
    }

    public static void error(String log)
    {
        INGEST_LOGGER.error(log);
    }
    
    
    public static boolean isTraceEnabled() {
    	return INGEST_LOGGER.isEnabledFor(Level.TRACE);
    }
    
    public static boolean isDebugEnabled() {
    	return INGEST_LOGGER.isEnabledFor(Level.DEBUG);
    }
    
    public static boolean isInfoEnabled() {
    	return INGEST_LOGGER.isEnabledFor(Level.INFO);
    }
    
    public static boolean isWarnEnabled() {
    	return INGEST_LOGGER.isEnabledFor(Level.WARN);
    }
    
    public static boolean isErrorEnabled() {
    	return INGEST_LOGGER.isEnabledFor(Level.ERROR);
    }
}
