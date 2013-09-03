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
package ddf.metrics.reporting.internal.rrd4j;


import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDbPool;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;

import ddf.metrics.reporting.internal.Collector;
import ddf.metrics.reporting.internal.CollectorException;


/**
 * Periodically collects a metrics data from its associated JMX MBean attribute and
 * stores this data in an RRD (Round Robin Database) file.
 * 
 * NOTE: This is an internal class (as noted in its package name) that is not intended 
 * for external use by third party developers.
 * 
 * The configuration parameters for a JmxCollector are typically configured in the
 * {@code <config>} stanzas of the catalog-core-app features.xml file.
 * 
 * Sample config stanza for a JmxCollector:
 * <pre>
 * {@code
 *  <config name="MetricsJmxCollector-UsageThreshold">
 *          mbeanName = java.lang:type=MemoryPool,name=Code Cache
 *          mbeanAttributeName = UsageThreshold
 *          rrdPath = usageThreshold.rrd
 *          rrdDataSourceName = data
 *          rrdDataSourceType = COUNTER
 * </config>
 * }
 * </pre>
 *  
 * @author rodgersh
 * @author ddf.isgs@lmco.com
 *
 */
public class JmxCollector implements Collector
{
	private static final int MILLIS_PER_SECOND = 1000;
	
    private static final int FIVE_MINUTES_MILLIS = 300000;

    private static final Logger LOGGER = Logger.getLogger(JmxCollector.class);
    
    public static final String DEFAULT_METRICS_DIR = "data/metrics/";
    
    public static final String COUNTER_DATA_SOURCE_TYPE = "COUNTER";
    
    public static final String GAUGE_DATA_SOURCE_TYPE = "GAUGE";
    
    public static final int ONE_YEAR_IN_15_MINUTE_STEPS = 4 * 24 * 365;
    
    public static final int TEN_YEARS_IN_HOURS = ONE_YEAR_IN_15_MINUTE_STEPS * 10;
    
    
    /** 
     * Name of the JMX MBean that contains the metric being collected. 
     * (Should be set by <config> stanza in metrics-reporting-app features.xml file)
     */
    private String mbeanName;
    
    /** 
     * Name of the JMX MBean attribute that maps to the metric being collected. 
     * (Should be set by <config> stanza in metrics-reporting-app features.xml file)
     */
    private String mbeanAttributeName;
    
    /** 
     * Name of the RRD file to store the metric's data being collected. The
     * DDF metrics base directory will be prepended to this filename.
     * (Should be set by <config> stanza in metrics-reporting-app features.xml file)
     */    
    private String rrdPath;
    
    /** 
     * The name of the RRD data source to use for the metric being collected. This
     * can (and should) be the same for all metrics configured, e.g., "data", since
     * there should only be one data source per RRD file they do not need to be unique
     * data source names.
     * (Should be set by <config> stanza in metrics-reporting-app features.xml file)
     */    
    private String rrdDataSourceName;
    
    /** 
     * Type of RRD data source to use for the metric's data being collected. A
     * COUNTER type is used for metrics that always increment, e.g., query count.
     * A GAUGE is used for metrics whose value can vary up or down at any time, e.g.,
     * query response time.
     * (Should be set by <config> stanza in metrics-reporting-app features.xml file)
     */
    private String rrdDataSourceType;
    
    private String metricsDir;
    private int sampleRate;
    private long minimumUpdateTimeDelta;
    private long sampleSkipCount;
    private int rrdStep;
    private MBeanServer localMBeanServer;    
    private final RrdDbPool pool;
    private RrdDb rrdDb;
    private Sample sample = null;    
    private ScheduledThreadPoolExecutor executor;
    private long mbeanTimeoutMillis = FIVE_MINUTES_MILLIS;
    private ExecutorService executorPool;
    
    
    public JmxCollector()
    {
        metricsDir = DEFAULT_METRICS_DIR;
        localMBeanServer = getLocalMBeanServer();
        
        // Only expose these values via setter/getter methods for unit
        // testing purposes so that unit tests can run in seconds vs. minutes
        // by using a faster (lower value) sample rate and no (zero)
        // minimum update time delta so that sample updates always occur during
        // unit tests.
        this.sampleRate = 60;
        this.minimumUpdateTimeDelta = 1;
        this.sampleSkipCount = 0;
        
        // Should always be the same as the sample rate
        rrdStep = this.sampleRate;
        pool = RrdDbPool.getInstance();
        
        // Set to default - should be overridden by the <config> in the 
        // metrics-reporting-app features.xml file, which will call this
        // attribute's setter method.
        this.rrdDataSourceType = COUNTER_DATA_SOURCE_TYPE;
                
        LOGGER.trace("EXITING: JmxCollector default constructor");
    }
 

    /**
     * Initialization when the JmxCollector is created. Called by blueprint.
     */
    public void init()
    {
        LOGGER.trace("ENTERING: init()");
        
        if (executorPool == null)
        {
			executorPool = Executors.newCachedThreadPool();        
        }
        
		// Creating JmxCollector can be time consuming,
		// so do this in a separate thread to prevent holding up creation
        // of Sources or the Catalog
        final Runnable jmxCollectorCreator = new Runnable() 
        {
            public void run()
            { 				
            	try {
					configureCollector();
				} catch (CollectorException e) {
					// Ignore, it has already been logged
				} catch (IOException e) {
					// Ignore, it has already been logged
				}
            }
        };
        
        LOGGER.debug("Start configureCollector thread for JmxCollector " + mbeanAttributeName);
        executorPool.execute(jmxCollectorCreator);
        
        LOGGER.trace("EXITING: init()");
    }
    
    void configureCollector() throws CollectorException, IOException
    {
    	LOGGER.trace("ENTERING: configureCollector() for collector " + mbeanAttributeName);
    	
        if (!isMbeanAccessible())
        {
            LOGGER.warn("MBean attribute " + mbeanAttributeName + " is not accessible - no collector will be configured for it.");
            throw new CollectorException("MBean attribute " + mbeanAttributeName + " is not accessible - no collector will be configured for it.");
        }
        
        if (rrdDataSourceType == null)
        {
        	LOGGER.warn("Unable to configure collector for MBean attribute " + mbeanAttributeName + "\nData Source type for the RRD file cannot be null - must be either COUNTER or GAUGE.");
        	throw new CollectorException("Unable to configure collector for MBean attribute " + mbeanAttributeName + "\nData Source type for the RRD file cannot be null - must be either COUNTER or GAUGE.");
        }
        
        createRrdFile(rrdPath, rrdDataSourceName, DsType.valueOf(rrdDataSourceType));
        
        updateSamples();      
        
        LOGGER.trace("EXITING: configureCollector() for collector " + mbeanAttributeName);
    }
    
    /**
     * Cleanup when the JmxCollector is destroyed. Called by blueprint.
     */
    public void destroy()
    {
        LOGGER.trace("ENTERING: destroy()");
        
        // Shutdown the scheduled threaded executor that is polling the MBean attribute (metric)
        if (executor != null)
        {
            List<Runnable> tasks = executor.shutdownNow();
            if (tasks != null)
            {
                LOGGER.debug("Num tasks awaiting execution = " + tasks.size());
            }
            else
            {
                LOGGER.debug("No tasks awaiting execution");
            }
        }
        
        // Close the RRD DB
        try
        {
            if (rrdDb != null)
            {
                rrdDb.close();
                pool.release(rrdDb);
            }
        }
        catch (IOException e)
        {
            LOGGER.warn("Unable to close RRD DB", e);
        }
        
        LOGGER.trace("EXITING: destroy()");
    }

    
    /**
     * Verify MBean and its attribute exists and can be collected, 
     * i.e., is numeric data (vs. CompositeData)
     * 
     * @return true if MBean can be accessed, false otherwise
     */
    private boolean isMbeanAccessible()
    {
        Object attr = null;
        long startTime = System.currentTimeMillis();
        while (attr == null && (System.currentTimeMillis() - startTime < mbeanTimeoutMillis)) {
            try
            {
                attr = localMBeanServer.getAttribute(new ObjectName(mbeanName), mbeanAttributeName);
                
                if (!isNumeric(attr))
                {
                    LOGGER.debug(mbeanAttributeName + " from MBean " + mbeanName + " has non-numeric data");
                    return false;
                }
                
                if (!(attr instanceof Integer) && !(attr instanceof Long) && !(attr instanceof Float) 
                    && !(attr instanceof Double))
                {
                    return false;
                }
            }
            catch (Exception e)
            {
                try {
                    LOGGER.trace("MBean [" + mbeanName +"] not found, sleeping...");
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    // Ignore this
                }
            }
        }

        return attr != null;
    }

    
    /**
     * Create an RRD file based on the metric's name (path) in the DDF metrics sub-directory.
     * An RRD DB instance is created from this RRD file's definition (if the RRD file did not already
     * exist, which can occur if the RRD file is created then DDF is restarted and this method is called).
     * If the RRD file already exists, then just create an RRD DB instance based on the existing
     * RRD file.
     * 
     * @param path path where the RRD file is to be created. This is required.
     * @param dsName data source name for the RRD file. This is required.
     * @param dsType data source type, i.e., COUNTER or GAUGE (This is required.)
     *       (DERIVE and ABSOLUTE are not currently supported)
     *       
     * @throws IOException
     * @throws CollectorException
     */
    private void createRrdFile(final String path, final String dsName, final DsType dsType) 
        throws IOException, CollectorException 
    {
        LOGGER.trace("ENTERING: createRrdFile");
        
        if (StringUtils.isEmpty(path))
        {
            throw new CollectorException("Path where RRD file is to be created must be specified.");
        }
        else
        {
            rrdPath = metricsDir + path;
        }
        
        if (StringUtils.isEmpty(dsName))
        {
            throw new CollectorException("The name of the data source used in the RRD file must be specified.");
        }
        
        if (!dsType.equals(DsType.COUNTER) && !dsType.equals(DsType.GAUGE))
        {
            throw new CollectorException("Data Source type for the RRD file must be either COUNTER or GAUGE.");
        }

        File file = new File(rrdPath);
        if (!file.exists()) 
        {
            // Create necessary parent directories
            if (!file.getParentFile().exists()) 
            {
                file.getParentFile().mkdirs();
            }

            LOGGER.debug("Creating new RRD file " + rrdPath);

            RrdDef def = new RrdDef(rrdPath, rrdStep);
            
            // NOTE: Currently restrict each RRD file to only have one data source
            // (even though RRD supports multiple data sources in a single RRD file)
            def.addDatasource(dsName, dsType, 90, 0, Double.NaN);

            // NOTE: Separate code segments based on dsType in case in future
            // we want more or less archivers based on data source type.
            
            // Use a COUNTER for continuous incrementing counters, e.g., number of queries
            if (dsType == DsType.COUNTER)
            {
                // 1 minute resolution for last 60 minutes
                def.addArchive(ConsolFun.TOTAL, 0.5, 1, 60);  
    
                // 15 minute resolution for the last year
                def.addArchive(ConsolFun.TOTAL, 0.5, 15, ONE_YEAR_IN_15_MINUTE_STEPS);
    
                // 1 minute resolution for last 60 minutes
                def.addArchive(ConsolFun.AVERAGE, 0.5, 1, 60);  
    
                // 15 minute resolution for the last year
                def.addArchive(ConsolFun.AVERAGE, 0.5, 15, ONE_YEAR_IN_15_MINUTE_STEPS);
                
                // 1 minute resolution for last 60 minutes
                def.addArchive(ConsolFun.MAX, 0.5, 1, 60);  
    
                // 15 minute resolution for the last year
                def.addArchive(ConsolFun.MAX, 0.5, 15, ONE_YEAR_IN_15_MINUTE_STEPS);
    
                // 1 minute resolution for last 60 minutes
                def.addArchive(ConsolFun.MIN, 0.5, 1, 60);  
    
                // 15 minute resolution for the last year
                def.addArchive(ConsolFun.MIN, 0.5, 15, ONE_YEAR_IN_15_MINUTE_STEPS);
            }
            
            // Use a GAUGE to store the values we measure directly as they are,
            // e.g., response time for an ingest or query
            else if (dsType == DsType.GAUGE)
            {
                // If you want to know the amount, look at the averages. 
                // If you want to know the rate, look at the maximum.
                
                // 1 minute resolution for last 60 minutes
                def.addArchive(ConsolFun.TOTAL, 0.5, 1, 60);  
    
                // 15 minute resolution for the last year
                def.addArchive(ConsolFun.TOTAL, 0.5, 15, ONE_YEAR_IN_15_MINUTE_STEPS);
                
                // 1 minute resolution for last 60 minutes
                def.addArchive(ConsolFun.AVERAGE, 0.5, 1, 60);  
    
                // 15 minute resolution for the last year
                def.addArchive(ConsolFun.AVERAGE, 0.5, 15, ONE_YEAR_IN_15_MINUTE_STEPS);
                
                // 1 minute resolution for last 60 minutes
                def.addArchive(ConsolFun.MAX, 0.5, 1, 60);  
    
                // 15 minute resolution for the last year
                def.addArchive(ConsolFun.MAX, 0.5, 15, ONE_YEAR_IN_15_MINUTE_STEPS);
    
                // 1 minute resolution for last 60 minutes
                def.addArchive(ConsolFun.MIN, 0.5, 1, 60);  
    
                // 15 minute resolution for the last year
                def.addArchive(ConsolFun.MIN, 0.5, 15, ONE_YEAR_IN_15_MINUTE_STEPS);
            }

            // Create RRD file based on the RRD file definition
            rrdDb = pool.requestRrdDb(def);
        }
        else
        {
            LOGGER.debug("rrd file " + path + " already exists - absolute path = " + file.getAbsolutePath());
            rrdDb = pool.requestRrdDb(rrdPath);
        }
        
        LOGGER.trace("EXITING: createRrdFile");
    }
    
    
    /**
     * Configures a scheduled threaded executor to poll the metric's MBean periodically
     * and add a sample to the RRD file with the metric's current value.
     * 
     * @throws CollectorException
     */
    public void updateSamples() throws CollectorException
    {
        LOGGER.trace("ENTERING: updateSamples");
        
        if (executor == null)
        {
            executor = new ScheduledThreadPoolExecutor(1);        
        }
        
        final Runnable updater = new Runnable() 
        {
            public void run() 
            {        
                Object attr = null;
                try
                {
                    attr = localMBeanServer.getAttribute(new ObjectName(mbeanName), mbeanAttributeName);
                    
                    LOGGER.trace("Sampling attribute " + mbeanAttributeName + " from MBean " + mbeanName);
                    
                    // Cast the metric's sampled value to the appropriate data type
                    double val = 0;
                    if (attr instanceof Integer) 
                    {
                        val = (Integer) attr;
                    } 
                    else if (attr instanceof Long) 
                    {
                        val = ((Long) attr).intValue();
                    } 
                    else if (attr instanceof Float) 
                    {
                        val = ((Float) attr);
                    } 
                    else if (attr instanceof Double) 
                    {
                        val = ((Double) attr);
                    } 
                    else 
                    {
                        throw new IllegalArgumentException("Unsupported type " + attr + " for attribute " + mbeanAttributeName);
                    }
                    
                    LOGGER.trace("MBean attribute " + mbeanAttributeName + " has value = " + val);
                    
                    // If first time this metric has been sampled, then need to create a
                    // sample in the RRD file
                    if (sample == null)
                    {
                        sample = rrdDb.createSample();
                    }
                    
                    try 
                    {
                    	long now = System.currentTimeMillis()/MILLIS_PER_SECOND;
                        long lastUpdateTime = rrdDb.getLastUpdateTime();
                        
                        // Add metric's sample to RRD file with current timestamp (i.e., "NOW")
                        //sample.setAndUpdate("NOW:" + val);
                        
                        if (now - rrdDb.getLastUpdateTime() >= minimumUpdateTimeDelta) {   
                        	LOGGER.debug("Sample time is " + now);
                            sample.setTime(now);
                            sample.setValue(rrdDataSourceName, val);
                            sample.update();
                        }
                        else
                        {
                        	LOGGER.debug("Skipping sample update because time between updates is less than " + minimumUpdateTimeDelta + " seconds");

                            sampleSkipCount++;
                            
                            LOGGER.debug("now = " + now + ",   lastUpdateTime = " + lastUpdateTime + 
                            		"   (sampleSkipCount = " + sampleSkipCount + ")");
                        }
                    } 
                    catch (IllegalArgumentException iae) 
                    {
                        LOGGER.error("Dropping sample of datasource " + rrdDataSourceName, iae);
                    }
                }
                catch (Exception e )
                {
                    LOGGER.warn("Problems getting MBean attribute " + mbeanAttributeName, e);
                }        
            }
        };
        
        // Setup threaded scheduler to retrieve this MBean attribute's value
        // at the specified sample rate
        LOGGER.debug("Setup ScheduledThreadPoolExecutor for MBean " + mbeanName);
        executor.scheduleWithFixedDelay(updater, 0, sampleRate, TimeUnit.SECONDS);
        
        LOGGER.trace("EXITING: updateSamples");
    }
            
    /**
     * @return local MBean server
     */
    private MBeanServer getLocalMBeanServer() 
    {
        if (localMBeanServer == null) 
        {
            localMBeanServer = ManagementFactory.getPlatformMBeanServer();
        }
        
        return localMBeanServer;
    }  

    
    /**
     * Determines whether an object's value is a numeric type or a String with
     * a numeric value.
     * 
     * @param value the Object to be tested whether it has a numeric value
     * 
     * @return true if object's value is numeric, false otherwise
     */
    public static boolean isNumeric(Object value) 
    {
        return ((value instanceof Number) || 
                ((value instanceof String) && NumberUtils.isNumber((String) value)));
    }
    
    
    public void setMbeanName(String mbeanName)
    {
        LOGGER.trace("Setting mbeanName to " + mbeanName);
        
        this.mbeanName = mbeanName;
    }
    
    
    public String getMbeanAttributeName()
    {
        return mbeanAttributeName;
    }


    public void setMbeanAttributeName( String mbeanAttributeName )
    {
        this.mbeanAttributeName = mbeanAttributeName;
    }


    public String getMetricsDir()
    {
        return metricsDir;
    }


    public void setMetricsDir( String metricsDir )
    {
        this.metricsDir = metricsDir;
    }


    public String getRrdPath()
    {
        return rrdPath;
    }


    public void setRrdPath( String rrdPath )
    {
        this.rrdPath = rrdPath;
    }


    public String getRrdDataSourceName()
    {
        return rrdDataSourceName;
    }


    public void setRrdDataSourceName( String rrdDataSourceName )
    {
        this.rrdDataSourceName = rrdDataSourceName;
    }


    public String getRrdDataSourceType()
    {
        return rrdDataSourceType;
    }


    public void setRrdDataSourceType( String rrdDataSourceType )
    {
        this.rrdDataSourceType = rrdDataSourceType;
        LOGGER.debug("rrdDataSourceType = " + rrdDataSourceType);
    }
    
    
    protected int getSampleRate()
    {
        return sampleRate;
    }

    
    protected void setSampleRate( int sampleRate )
    {
        this.sampleRate = sampleRate;
        this.rrdStep = this.sampleRate;
    }
    
    protected long getSampleSkipCount() {
    	return sampleSkipCount;
    }
    
    
    public void setMinimumUpdateTimeDelta(long minimumUpdateTimeDelta) 
    {
		this.minimumUpdateTimeDelta = minimumUpdateTimeDelta;
	}


	void setMbeanTimeoutMillis(long mbeanTimeoutMillis) {
        this.mbeanTimeoutMillis = mbeanTimeoutMillis;
    }

}
