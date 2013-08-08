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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.Archive;
import org.rrd4j.core.Datasource;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.Header;
import org.rrd4j.core.RrdDb;

import ddf.metrics.reporting.internal.CollectorException;


public class JmxCollectorTest
{
    private static final transient Logger LOGGER = Logger.getLogger(JmxCollectorTest.class);
    
    private static final String TEST_DIR = "target/";
    
    public JmxCollector jmxCollector;
    public RrdDb rrdDb;
    public String rrdPath;
    public String dataSourceName;
    
    
    @BeforeClass
    static public void oneTimeSetup() {
        // Format logger output
        BasicConfigurator.configure();
        ((PatternLayout) ((Appender) Logger.getRootLogger().getAllAppenders().nextElement()).getLayout())
                .setConversionPattern("[%30.30t] %-30.30c{1} %-5p %m%n");

        Logger.getRootLogger().setLevel(Level.INFO);
        
        // To add appender that logs to a log file in addition to Console
//        Layout layout = (PatternLayout)((Appender) Logger.getRootLogger().getAllAppenders().nextElement()).getLayout();
//        FileAppender appender;
//        try
//        {
//            appender = new FileAppender(layout, "test.log", true);
//            Logger.getRootLogger().addAppender(appender);
//        }
//        catch (IOException e)
//        {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }
    
    
    @After
    public void tearDown() throws Exception
    {
        LOGGER.debug("Doing tearDown");
        
        // Stops the threaded executor JmxCollector is using to poll MBean attribute
        if (jmxCollector != null)
        {
            jmxCollector.destroy();
        }
        
        if (rrdDb == null) return;
        
        if (!rrdDb.isClosed())
        {
            rrdDb.close();
        }
        String path = rrdDb.getPath();
        File rrdFile = new File(path);
        if (rrdFile.exists())
        {
            boolean status = rrdFile.delete();
            if (status)
            {
                LOGGER.debug("Successfully deleted rrdFile " + path);
            }
            else
            {
                LOGGER.debug("Unable to delete rrdFile " + path);
            }
        }
        else
        {
            LOGGER.debug("rrdFile " + path + " does not exist - cannot delete");
        }
    }
    
    
    @Test
    public void testConstruction()
    {
        JmxCollector jmxCollector = new JmxCollector();
        assertThat(jmxCollector, not(nullValue()));
        assertThat(jmxCollector.getRrdDataSourceType(), is(JmxCollector.COUNTER_DATA_SOURCE_TYPE));
    }
    
    
    @Test
    public void testRrdFileCreationForCounterDataSource() throws Exception
    {
        // Set sample rate to 1 sec (default is 60 seconds) so that unit test runs quickly
        int sampleRate = 1;
        createJmxCollector( "Uptime", JmxCollector.COUNTER_DATA_SOURCE_TYPE, sampleRate);
        
        String rrdFilename = jmxCollector.getRrdPath();
        assertThat(rrdFilename, is(TEST_DIR + rrdPath));
        
        rrdDb = new RrdDb(rrdFilename);
        assertThat(rrdDb, not(nullValue()));
        assertThat(rrdDb.isClosed(), is(false));
        
        Header header = rrdDb.getHeader();
        assertThat(header, not(nullValue()));
        assertThat(header.getStep(), is((long) sampleRate));
        
        assertThat(rrdDb.getDsCount(), is(1));
        Datasource dataSource = rrdDb.getDatasource(dataSourceName);
        assertThat(dataSource, not(nullValue()));
        DsType dataSourceType = dataSource.getType();
        assertThat(dataSourceType, is(DsType.COUNTER));
        
        assertThat(rrdDb.getArcCount(), is(8));
        
        Archive archive = rrdDb.getArchive(ConsolFun.AVERAGE, 1);
        assertThat(archive, not(nullValue()));
        assertThat(archive.getRows(), is(60));
        
        archive = rrdDb.getArchive(ConsolFun.AVERAGE, 15);
        assertThat(archive, not(nullValue()));
        assertThat(archive.getRows(), is(JmxCollector.ONE_YEAR_IN_15_MINUTE_STEPS));
        
        archive = rrdDb.getArchive(ConsolFun.TOTAL, 1);
        assertThat(archive, not(nullValue()));
        assertThat(archive.getRows(), is(60));
        
        archive = rrdDb.getArchive(ConsolFun.TOTAL, 15);
        assertThat(archive, not(nullValue()));
        assertThat(archive.getRows(), is(JmxCollector.ONE_YEAR_IN_15_MINUTE_STEPS));
        
        //LOGGER.debug(rrdDb.dump());
    }
    
    
    @Test
    public void testRrdFileCreationForGaugeDataSource() throws Exception
    {
        // Set sample rate to 1 sec (default is 60 seconds) so that unit test runs quickly
        int sampleRate = 1;
        createJmxCollector( "Uptime", JmxCollector.GAUGE_DATA_SOURCE_TYPE, sampleRate);
        
        String rrdFilename = jmxCollector.getRrdPath();
        assertThat(rrdFilename, is(TEST_DIR + rrdPath));
        
        rrdDb = new RrdDb(rrdFilename);
        assertThat(rrdDb, not(nullValue()));
        assertThat(rrdDb.isClosed(), is(false));
        
        Header header = rrdDb.getHeader();
        assertThat(header, not(nullValue()));
        assertThat(header.getStep(), is((long) sampleRate));  // verify 60 second sample rate
        
        assertThat(rrdDb.getDsCount(), is(1));
        Datasource dataSource = rrdDb.getDatasource(dataSourceName);
        assertThat(dataSource, not(nullValue()));
        DsType dataSourceType = dataSource.getType();
        assertThat(dataSourceType, is(DsType.GAUGE));
        
        assertThat(rrdDb.getArcCount(), is(8));
        
        Archive archive = rrdDb.getArchive(ConsolFun.MIN, 1);
        assertThat(archive, not(nullValue()));
        assertThat(archive.getRows(), is(60));
        
        archive = rrdDb.getArchive(ConsolFun.MIN, 15);
        assertThat(archive, not(nullValue()));
        assertThat(archive.getRows(), is(JmxCollector.ONE_YEAR_IN_15_MINUTE_STEPS));
        
        archive = rrdDb.getArchive(ConsolFun.MAX, 1);
        assertThat(archive, not(nullValue()));
        assertThat(archive.getRows(), is(60));
        
        archive = rrdDb.getArchive(ConsolFun.MAX, 15);
        assertThat(archive, not(nullValue()));
        assertThat(archive.getRows(), is(JmxCollector.ONE_YEAR_IN_15_MINUTE_STEPS));
        
        archive = rrdDb.getArchive(ConsolFun.AVERAGE, 1);
        assertThat(archive, not(nullValue()));
        assertThat(archive.getRows(), is(60));
        
        archive = rrdDb.getArchive(ConsolFun.AVERAGE, 15);
        assertThat(archive, not(nullValue()));
        assertThat(archive.getRows(), is(JmxCollector.ONE_YEAR_IN_15_MINUTE_STEPS));        
    }
    
    
    @Test
    @Ignore
    public void testRrdFileCreationWhenRrdFileAlreadyExists() throws Exception
    {
        // Set sample rate to 1 sec (default is 60 seconds) so that unit test runs quickly
        int sampleRate = 1;
        createJmxCollector( "Uptime", JmxCollector.COUNTER_DATA_SOURCE_TYPE, sampleRate);
        
        String rrdFilename1 = jmxCollector.getRrdPath();
        assertThat(rrdFilename1, is(TEST_DIR + rrdPath));
        
        rrdDb = new RrdDb(rrdFilename1);
        assertThat(rrdDb, not(nullValue()));
        assertThat(rrdDb.isClosed(), is(false));
        
        // Attempt to create again
        LOGGER.debug("Creating JmxCollector again ...");
        dataSourceName = "uptime";
        rrdPath = dataSourceName + ".rrd";
        
        JmxCollector jmxCollector2 = new JmxCollector();
        jmxCollector2.setMbeanName("java.lang:type=Runtime");
        jmxCollector2.setMbeanAttributeName("Uptime");
        jmxCollector2.setRrdPath(rrdPath);
        jmxCollector2.setRrdDataSourceName(dataSourceName);
        jmxCollector2.setRrdDataSourceType(JmxCollector.COUNTER_DATA_SOURCE_TYPE);
        jmxCollector2.setSampleRate(sampleRate);
        jmxCollector2.setMetricsDir(TEST_DIR);
        
        // Simulates what Spring beans container would do
        jmxCollector2.configureCollector();
        
        // Verify the 2 JMX Collectors are using the same RRD file
        String rrdFilename2 = jmxCollector2.getRrdPath();
        assertThat(rrdFilename2, is(TEST_DIR + rrdPath));
        assertThat(rrdFilename1, equalTo(rrdFilename2));
        
        jmxCollector2.destroy();
    }
    
    
    @Test(expected = CollectorException.class)
    public void testInaccessibleMbeanAttribute() throws IOException, CollectorException
    {    
        String mbeanAttributeName = "Invalid";
        dataSourceName = mbeanAttributeName.toLowerCase();
        rrdPath = dataSourceName + ".rrd";
        
        jmxCollector = new JmxCollector();
        jmxCollector.setMbeanName("java.lang:type=Runtime");
        jmxCollector.setMbeanAttributeName(mbeanAttributeName);
        jmxCollector.setRrdPath(rrdPath);
        jmxCollector.setRrdDataSourceName(dataSourceName);
        jmxCollector.setRrdDataSourceType(JmxCollector.COUNTER_DATA_SOURCE_TYPE);
        jmxCollector.setSampleRate(1);
        jmxCollector.setMetricsDir(TEST_DIR);
        jmxCollector.setMbeanTimeoutMillis(50);
        
        // Simulates what Spring beans container would do
        jmxCollector.configureCollector();
    }
    
    
    @Test(expected = CollectorException.class)
    public void testNonNumericMbeanAttribute() throws IOException, CollectorException
    {    
        String mbeanAttributeName = "ClassPath";
        dataSourceName = mbeanAttributeName.toLowerCase();
        rrdPath = dataSourceName + ".rrd";
        
        jmxCollector = new JmxCollector();
        jmxCollector.setMbeanName("java.lang:type=Runtime");
        jmxCollector.setMbeanAttributeName(mbeanAttributeName);
        jmxCollector.setRrdPath(rrdPath);
        jmxCollector.setRrdDataSourceName(dataSourceName);
        jmxCollector.setRrdDataSourceType(JmxCollector.COUNTER_DATA_SOURCE_TYPE);
        jmxCollector.setSampleRate(1);
        jmxCollector.setMetricsDir(TEST_DIR);
        
        // Simulates what Spring beans container would do
        jmxCollector.configureCollector();
    }    
    
    
    @Test(expected = CollectorException.class)
    public void testNullRrdPath() throws IOException, CollectorException
    {    
        String mbeanAttributeName = "Uptime";
        dataSourceName = mbeanAttributeName.toLowerCase();
        rrdPath = null;
        
        jmxCollector = new JmxCollector();
        jmxCollector.setMbeanName("java.lang:type=Runtime");
        jmxCollector.setMbeanAttributeName(mbeanAttributeName);
        jmxCollector.setRrdPath(rrdPath);
        jmxCollector.setRrdDataSourceName(dataSourceName);
        jmxCollector.setRrdDataSourceType(JmxCollector.COUNTER_DATA_SOURCE_TYPE);
        jmxCollector.setSampleRate(1);
        jmxCollector.setMetricsDir(TEST_DIR);
        
        // Simulates what Spring beans container would do
        jmxCollector.configureCollector();
    }
    
    
    @Test(expected = CollectorException.class)
    public void testNullDataSourceName() throws IOException, CollectorException
    {    
        String mbeanAttributeName = "Uptime";
        dataSourceName = null;
        rrdPath = "uptime.rrd";
        
        jmxCollector = new JmxCollector();
        jmxCollector.setMbeanName("java.lang:type=Runtime");
        jmxCollector.setMbeanAttributeName(mbeanAttributeName);
        jmxCollector.setRrdPath(rrdPath);
        jmxCollector.setRrdDataSourceName(dataSourceName);
        jmxCollector.setRrdDataSourceType(JmxCollector.COUNTER_DATA_SOURCE_TYPE);
        jmxCollector.setSampleRate(1);
        jmxCollector.setMetricsDir(TEST_DIR);
        
        // Simulates what Spring beans container would do
        jmxCollector.configureCollector();
    }

    
    @Test(expected = CollectorException.class)
    public void testNullDataSourceType() throws IOException, CollectorException
    {    
        String mbeanAttributeName = "Uptime";
        dataSourceName = mbeanAttributeName.toLowerCase();
        rrdPath = dataSourceName + ".rrd";
        
        jmxCollector = new JmxCollector();
        jmxCollector.setMbeanName("java.lang:type=Runtime");
        jmxCollector.setMbeanAttributeName(mbeanAttributeName);
        jmxCollector.setRrdPath(rrdPath);
        jmxCollector.setRrdDataSourceName(dataSourceName);
        jmxCollector.setRrdDataSourceType(null);
        jmxCollector.setSampleRate(1);
        jmxCollector.setMetricsDir(TEST_DIR);
        
        // Simulates what Spring beans container would do
        jmxCollector.configureCollector();
    }

    
    @Test(expected = CollectorException.class)
    public void testUnsupportedDataSourceType() throws IOException, CollectorException
    {    
        String mbeanAttributeName = "Uptime";
        dataSourceName = mbeanAttributeName.toLowerCase();
        rrdPath = dataSourceName + ".rrd";
        
        jmxCollector = new JmxCollector();
        jmxCollector.setMbeanName("java.lang:type=Runtime");
        jmxCollector.setMbeanAttributeName(mbeanAttributeName);
        jmxCollector.setRrdPath(rrdPath);
        jmxCollector.setRrdDataSourceName(dataSourceName);
        jmxCollector.setRrdDataSourceType("DERIVE");
        jmxCollector.setSampleRate(1);
        jmxCollector.setMetricsDir(TEST_DIR);
        
        // Simulates what Spring beans container would do
        jmxCollector.configureCollector();
    }    
    
    
    @Test
    public void testCounterDataSourceCollection() throws Exception
    {
        // Set sample rate to 1 sec (default is 60 seconds) so that unit test runs quickly
        createJmxCollector( "Uptime", JmxCollector.COUNTER_DATA_SOURCE_TYPE, 1);
        
        // Wait for "n" iterations of RRDB's sample rate, then see if MBean value was collected
        collectData(4);
    }
    
    
    @Test
    public void testGaugeDataSourceCollection() throws Exception
    {
        // Set sample rate to 1 sec (default is 60 seconds) so that unit test runs quickly
        createJmxCollector( "Uptime", JmxCollector.GAUGE_DATA_SOURCE_TYPE, 1);
        
        // Wait for "n" iterations of RRDB's sample rate, then see if MBean value was collected
        collectData(4);       
    }
    
    @Test
    public void testManyUpdatesInRapidSuccession() throws Exception {
    	createJmxCollector( "Uptime", JmxCollector.COUNTER_DATA_SOURCE_TYPE, 1);
    	
    	// Set high update delta time so that samples will be skipped
    	jmxCollector.setMinimumUpdateTimeDelta(3);
    	
    	// Sleep long enough for some data to be collected
    	long numRrdSamples = 4;
    	Thread.sleep(numRrdSamples * 1000);
    	
    	// Expected skip count is 2 because first sample is always accepted and
    	// num samples is one more than minimum update time delta
    	long expectedSampleSkipCount = numRrdSamples - 2;
        assertThat(jmxCollector.getSampleSkipCount(), is(expectedSampleSkipCount));
    }
    
/******************************************************************************************/
    
    private void createJmxCollector(String mbeanAttributeName, String dataSourceType, int sampleRate) 
        throws Exception
    {
        dataSourceName = mbeanAttributeName.toLowerCase();
        rrdPath = dataSourceName + ".rrd";
        
        jmxCollector = new JmxCollector();
        jmxCollector.setMbeanName("java.lang:type=Runtime");
        jmxCollector.setMbeanAttributeName(mbeanAttributeName);
        jmxCollector.setRrdPath(rrdPath);
        jmxCollector.setRrdDataSourceName(dataSourceName);
        jmxCollector.setRrdDataSourceType(dataSourceType);
        jmxCollector.setSampleRate(sampleRate);
        jmxCollector.setMinimumUpdateTimeDelta(0);
        jmxCollector.setMetricsDir(TEST_DIR);
        
        // Simulates what Spring beans container would do
        jmxCollector.configureCollector();
    }
    
    
    private void collectData(int numRrdStepIterations) throws Exception
    {
        String rrdFilename = jmxCollector.getRrdPath();        
        rrdDb = new RrdDb(rrdFilename);
        Header header = rrdDb.getHeader();
        
        // Wait for "n" iterations of RRDB's sample rate, then see if MBean value was collected
        LOGGER.debug("Sleeping for " + (header.getStep()*numRrdStepIterations) + " seconds");
        Thread.sleep((header.getStep()*numRrdStepIterations) * 1000);
        
        //LOGGER.debug(rrdDb.dump());
        
        long endTime = Calendar.getInstance().getTimeInMillis()/1000;
        
        // +1 because the fetch gets data for times inclusively, e.g.,
        // endTime=12345, so startTime=12345-4=12341, 
        // then fetch data for timestamps 12341, 12342, 12343, 12344, 12345 (which is 5 values)
        long startTime = endTime - numRrdStepIterations + 1;
        LOGGER.debug("startTime = " + startTime + ",   endTime = " + endTime);
        
        FetchRequest fetchRequest = rrdDb.createFetchRequest(ConsolFun.TOTAL, startTime, endTime);
        FetchData fetchData = fetchRequest.fetchData();
        double[] values = fetchData.getValues(dataSourceName);
        assertThat(values.length, is(numRrdStepIterations));
        logFetchData(fetchData, "TOTAL");
        
        fetchRequest = rrdDb.createFetchRequest(ConsolFun.AVERAGE, startTime, endTime);
        fetchData = fetchRequest.fetchData();
        values = fetchData.getValues(dataSourceName);
        assertThat(values.length, is(numRrdStepIterations));
        logFetchData(fetchData, "AVERAGE");
        
        fetchRequest = rrdDb.createFetchRequest(ConsolFun.MIN, startTime, endTime);
        fetchData = fetchRequest.fetchData();
        values = fetchData.getValues(dataSourceName);
        assertThat(values.length, is(numRrdStepIterations));
        logFetchData(fetchData, "MIN");
        
        fetchRequest = rrdDb.createFetchRequest(ConsolFun.MAX, startTime, endTime);
        fetchData = fetchRequest.fetchData();
        values = fetchData.getValues(dataSourceName);
        assertThat(values.length, is(numRrdStepIterations));
        logFetchData(fetchData, "MAX");     
    }
    
    
    private void logFetchData(FetchData fetchData, String dataType) throws Exception
    {
        LOGGER.debug("*************  " + dataType + "  **************");
        
        long[] timestamps = fetchData.getTimestamps();
        double[] values = fetchData.getValues(dataSourceName);
        
        int i=0;
        for (double val : values)
        {
            LOGGER.debug("timestamp[" + i + "]: " + timestamps[i] + ",   val = " + val);
            i++;
        }
        
        LOGGER.debug(fetchData.exportXml());
    }
}
