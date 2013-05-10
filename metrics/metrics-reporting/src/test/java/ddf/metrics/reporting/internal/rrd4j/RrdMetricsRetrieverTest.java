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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.poi.hslf.model.AutoShape;
import org.apache.poi.hslf.model.Picture;
import org.apache.poi.hslf.model.Shape;
import org.apache.poi.hslf.model.Slide;
import org.apache.poi.hslf.model.TextBox;
import org.apache.poi.hslf.usermodel.SlideShow;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.custommonkey.xmlunit.XMLTestCase;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;

import ddf.metrics.reporting.internal.MetricsGraphException;
import ddf.metrics.reporting.internal.MetricsRetriever;

public class RrdMetricsRetrieverTest extends XMLTestCase
{
    private static final transient Logger LOGGER = Logger.getLogger(RrdMetricsRetrieverTest.class);
    
    private static final String TEST_DIR = "target/";
    
    private static final String RRD_FILE_EXTENSION = ".rrd";
    
    private static final long ONE_DAY_IN_SECONDS = 24 * 60 * 60;
    
    private RrdDb rrdDb;
    
    
    private static final String months[] = { 
            "Jan", "Feb", "Mar", "Apr", 
            "May", "Jun", "Jul", "Aug", 
            "Sep", "Oct", "Nov", "Dec"}; 
    

    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
        // Format logger output
        BasicConfigurator.configure();
        ((PatternLayout) ((Appender) Logger.getRootLogger().getAllAppenders().nextElement()).getLayout())
                .setConversionPattern("[%30.30t] %-30.30c{1} %-5p %m%n");

        Logger.getRootLogger().setLevel(Level.DEBUG);
    }

    @After
    public void tearDown() throws Exception
    {        
        if (rrdDb != null && !rrdDb.isClosed())
        {
            rrdDb.close();
        }
        
        // Delete all .png files in test directory to ensure starting with clean directory
        FilenameFilter pngFilter = new FilenameFilter() 
        {
            public boolean accept(File dir, String name) 
            {
                if (name.endsWith(".rrd") || name.endsWith(".png"))
                    return true;
                
                return false;
            }
        };
        
        File testDir = new File(TEST_DIR);
        File[] fileList = testDir.listFiles(pngFilter);
        for (File file : fileList)
        {
            if (file.isFile())
            {
                file.delete();
            }
        }
    }
    
    
    @Test
    public void testMetricsGraphWithGauge() throws Exception
    {
        String rrdFilename = TEST_DIR + "queryCount_Gauge" + RRD_FILE_EXTENSION;
        long startTime = 900000000L;
        long endTime = createRrdFileWithGauge(rrdFilename, startTime);
               
        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        byte[] metricsGraph = metricsRetriever.createGraph("Query Reponse Time", rrdFilename, startTime, endTime);
                
        assertThat(metricsGraph, not(nullValue()));
        assertThat(metricsGraph.length, is(greaterThan(0)));
        
        // NOTE: RrdGraph provides no way to programmatically verify the title and axis label were set, i.e., no getters
        // All we can verify is that this alternate createGraph() method returns a byte array of the graph image. Only
        // visual inspection can verify the graph's accurracy, title, and axis labels.
        metricsGraph = metricsRetriever.createGraph("Query Reponse Time", rrdFilename, startTime, endTime, "My Vertical Axis Label", "My Title");
        
        assertThat(metricsGraph, not(nullValue()));
        assertThat(metricsGraph.length, is(greaterThan(0)));
    }
    
    
    @Test
    public void testMetricsGraphWithCounter() throws Exception
    {
        String rrdFilename = TEST_DIR + "queryCount_Counter" + RRD_FILE_EXTENSION;
        long startTime = 900000000L;
        long endTime = createRrdFileWithCounter(rrdFilename, startTime);
        
        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        byte[] metricsGraph = metricsRetriever.createGraph("Query Count", rrdFilename, startTime, endTime);
        
        assertThat(metricsGraph, not(nullValue()));
        assertThat(metricsGraph.length, is(greaterThan(0)));
        
        // NOTE: RrdGraph provides no way to programmatically verify the title and axis label were set, i.e., no getters
        // All we can verify is that this alternate createGraph() method returns a byte array of the graph image. Only
        // visual inspection can verify the graph's accurracy, title, and axis labels.        
        metricsGraph = metricsRetriever.createGraph("Query Count", rrdFilename, startTime, endTime, "My Vertical Axis Label", "My Title");
        
        assertThat(metricsGraph, not(nullValue()));
        assertThat(metricsGraph.length, is(greaterThan(0)));
    }
    
    
    @Test
    public void testMetricsJsonDataWithCounter() throws Exception
    {
        String rrdFilename = TEST_DIR + "queryCount_Counter" + RRD_FILE_EXTENSION;
        long startTime = 900000000L;
        long endTime = createRrdFileWithCounter(rrdFilename, startTime);
        
        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        String json = metricsRetriever.createJsonData("queryCount", rrdFilename, startTime, endTime);
        
        JSONParser parser = new JSONParser();
        JSONObject jsonObj = (JSONObject) parser.parse(json);
        
        // Verify the title, totalCount, and data (i.e., samples) are present
        assertThat(jsonObj.size(), equalTo(3));
        assertThat(jsonObj.get("title"), not(nullValue()));
        assertThat(jsonObj.get("totalCount"), not(nullValue()));
        
        // Verify 2 samples were retrieved from the RRD file and put in the JSON fetch results
        JSONArray samples = (JSONArray) jsonObj.get("data");
        assertThat(samples.size(), equalTo(6));  // 6 because that's the max num rows configured for the RRD file
        
        // Verify each retrieved sample has a timestamp and value
        for (int i=0; i < samples.size(); i++)
        {
            JSONObject sample = (JSONObject) samples.get(i);
            LOGGER.debug("timestamp = " + (String) sample.get("timestamp") + ",   value = " + (Long) sample.get("value"));
            assertThat(sample.get("timestamp"), not(nullValue()));
            assertThat(sample.get("value"), not(nullValue()));
        }  
    }
    
    
    @Test
    public void testMetricsJsonDataWithGauge() throws Exception
    {
        String rrdFilename = TEST_DIR + "queryCount_Gauge" + RRD_FILE_EXTENSION;
        long startTime = 900000000L;
        long endTime = createRrdFileWithGauge(rrdFilename, startTime);
        
        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        String json = metricsRetriever.createJsonData("queryCount", rrdFilename, startTime, endTime);
        
        JSONParser parser = new JSONParser();
        JSONObject jsonObj = (JSONObject) parser.parse(json);
        
        // Verify the title, and data (i.e., samples) are present
        assertThat(jsonObj.size(), equalTo(2));
        assertThat(jsonObj.get("title"), not(nullValue()));
        
        // Verify 2 samples were retrieved from the RRD file and put in the JSON fetch results
        JSONArray samples = (JSONArray) jsonObj.get("data");
        assertThat(samples.size(), equalTo(6));  // 6 because that's the max num rows configured for the RRD file
        
        // Verify each retrieved sample has a timestamp and value
        for (int i=0; i < samples.size(); i++)
        {
            JSONObject sample = (JSONObject) samples.get(i);
            LOGGER.debug("timestamp = " + (String) sample.get("timestamp") + ",   value = " + sample.get("value"));
            assertThat(sample.get("timestamp"), not(nullValue()));
            assertThat(sample.get("value"), not(nullValue()));
        }  
    }   
    
    
    @Test
    public void testMetricsCsvDataWithCounter() throws Exception
    {
        String rrdFilename = TEST_DIR + "queryCount_Counter" + RRD_FILE_EXTENSION;
        long startTime = 900000000L;
        long endTime = createRrdFileWithCounter(rrdFilename, startTime);
        
        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        String csv = metricsRetriever.createCsvData(rrdFilename, startTime, endTime);    
        
        // Break up CSV data into its individual lines
        // Each line should have 2 parts (cells)
        // The first line should be the column headers
        String[] csvLines = csv.split("\n");
        
        // Verify that number of lines should be the column headers + (number of samples - 1)
        // Since 3 samples were taken, but only 2 are added to the RRD file due to averaging,
        // expect 2 lines of data and the one line of column headers.
        assertThat(csvLines.length, equalTo(7));
        assertThat(csvLines[0], equalTo("Timestamp,Value"));  // column headers
        String[] cells = csvLines[1].split(",");
        assertThat(cells.length, equalTo(2));  // each line of data has the 2 values
    }
    
    
    @Test
    public void testMetricsCsvDataWithGauge() throws Exception
    {
        String rrdFilename = TEST_DIR + "queryCount_Gauge" + RRD_FILE_EXTENSION;
        long startTime = 900000000L;
        long endTime = createRrdFileWithGauge(rrdFilename, startTime);
        
        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        String csv = metricsRetriever.createCsvData(rrdFilename, startTime, endTime);       
        
        // Break up CSV data into its individual lines
        // Each line should have 2 parts (cells)
        // The first line should be the column headers
        String[] csvLines = csv.split("\n");
        
        // Verify that number of lines should be the column headers + (number of samples - 1)
        // Since 3 samples were taken, but only 2 are added to the RRD file due to averaging,
        // expect 2 lines of data and the one line of column headers.
        assertThat(csvLines.length, equalTo(7));
        assertThat(csvLines[0], equalTo("Timestamp,Value"));  // column headers
        String[] cells = csvLines[1].split(",");
        assertThat(cells.length, equalTo(2));  // each line of data has the 2 values
    }
    
    
    @Test
    public void testMetricsXlsDataWithCounter() throws Exception
    {
        String rrdFilename = TEST_DIR + "queryCount_Counter" + RRD_FILE_EXTENSION;
        long startTime = 900000000L;
        long endTime = createRrdFileWithCounter(rrdFilename, startTime);
        
        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        OutputStream os = metricsRetriever.createXlsData("queryCount", rrdFilename, startTime, endTime);
        InputStream xls = new ByteArrayInputStream(((ByteArrayOutputStream) os).toByteArray());
        assertThat(xls, not(nullValue()));
        
        HSSFWorkbook wb = new HSSFWorkbook(xls);
        assertThat(wb.getNumberOfSheets(), equalTo(1));

        HSSFSheet sheet = wb.getSheet("Query Count");
        assertThat(sheet, not(nullValue()));
        verifyWorksheet(sheet, "Query Count", 6, true);
    }
    
    
    @Test
    public void testMetricsXlsDataWithGauge() throws Exception
    {
        String rrdFilename = TEST_DIR + "queryCount_Gauge" + RRD_FILE_EXTENSION;
        long startTime = 900000000L;
        long endTime = createRrdFileWithGauge(rrdFilename, startTime);
        
        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        OutputStream os = metricsRetriever.createXlsData("queryCount", rrdFilename, startTime, endTime);
        InputStream xls = new ByteArrayInputStream(((ByteArrayOutputStream) os).toByteArray());
        assertThat(xls, not(nullValue()));
        
        HSSFWorkbook wb = new HSSFWorkbook(xls);
        assertThat(wb.getNumberOfSheets(), equalTo(1));
        HSSFSheet sheet = wb.getSheet("Query Count");
        assertThat(sheet, not(nullValue()));
        verifyWorksheet(sheet, "Query Count", 6, false);
    }
    
    
    @Test
    public void testMetricsXmlDataWithCounter() throws Exception
    {
        String rrdFilename = TEST_DIR + "queryCount_Counter" + RRD_FILE_EXTENSION;
        long startTime = 900000000L;
        long endTime = createRrdFileWithCounter(rrdFilename, startTime);
        
        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        String xml = metricsRetriever.createXmlData("queryCount", rrdFilename, startTime, endTime);
        assertXpathExists("/queryCount", xml);
        assertXpathExists("/queryCount/title", xml);
        assertXpathExists("/queryCount/data", xml);
        assertXpathExists("/queryCount/data/sample", xml);
        assertXpathExists("/queryCount/data/sample/timestamp", xml);
        assertXpathExists("/queryCount/data/sample/value", xml);
        assertXpathExists("/queryCount/data/totalCount", xml);
    }
    
    
    @Test
    public void testMetricsXmlDataWithGauge() throws Exception
    {
        String rrdFilename = TEST_DIR + "queryCount_Gauge" + RRD_FILE_EXTENSION;
        long startTime = 900000000L;
        long endTime = createRrdFileWithGauge(rrdFilename, startTime);
        
        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        String xml = metricsRetriever.createXmlData("queryCount", rrdFilename, startTime, endTime);
        assertXpathExists("/queryCount", xml);
        assertXpathExists("/queryCount/title", xml);
        assertXpathExists("/queryCount/data", xml);
        assertXpathExists("/queryCount/data/sample", xml);
        assertXpathExists("/queryCount/data/sample/timestamp", xml);
        assertXpathExists("/queryCount/data/sample/value", xml);
        assertXpathNotExists("/queryCount/data/totalCount", xml);
    }
    
    
    @Test
    public void testMetricsXlsReport() throws Exception
    {
        String rrdFilename = TEST_DIR + "queryCount_Counter" + RRD_FILE_EXTENSION;
        long startTime = 900000000L;
        long endTime = createRrdFileWithCounter(rrdFilename, startTime);
        
        rrdFilename = TEST_DIR + "queryCount_Gauge" + RRD_FILE_EXTENSION;
        endTime = createRrdFileWithGauge(rrdFilename, startTime);
        
        List<String> metricNames = new ArrayList<String>();
        metricNames.add("queryCount_Counter");
        metricNames.add("queryCount_Gauge");
        
        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        OutputStream os = metricsRetriever.createXlsReport(metricNames, TEST_DIR, startTime, endTime);
        InputStream xls = new ByteArrayInputStream(((ByteArrayOutputStream) os).toByteArray());
        assertThat(xls, not(nullValue()));
        
        HSSFWorkbook wb = new HSSFWorkbook(xls);
        assertThat(wb.getNumberOfSheets(), equalTo(2));
        
        HSSFSheet sheet = wb.getSheetAt(0);
        assertThat(sheet, not(nullValue()));
        verifyWorksheet(sheet, wb.getSheetName(0), 6, true);
        
        sheet = wb.getSheetAt(1);
        assertThat(sheet, not(nullValue()));
        verifyWorksheet(sheet, wb.getSheetName(1), 6, false);
    }    
    
    
    @Test
    public void testMetricsPptDataWithCounter() throws Exception
    {
        String rrdFilename = TEST_DIR + "queryCount_Counter" + RRD_FILE_EXTENSION;
        long startTime = 900000000L;
        long endTime = createRrdFileWithCounter(rrdFilename, startTime);
        
        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        OutputStream os = metricsRetriever.createPptData("queryCount", rrdFilename, startTime, endTime);
        InputStream is = new ByteArrayInputStream(((ByteArrayOutputStream) os).toByteArray());
        assertThat(is, not(nullValue()));
        
        SlideShow ppt = new SlideShow(is);
        Slide[] slides = ppt.getSlides();
        assertThat(slides.length, equalTo(1));
        verifySlide(slides[0], "queryCount", true);
    }
    
    
    @Test
    public void testMetricsPptDataWithGauge() throws Exception
    {
        String rrdFilename = TEST_DIR + "queryCount_Gauge" + RRD_FILE_EXTENSION;
        long startTime = 900000000L;
        long endTime = createRrdFileWithGauge(rrdFilename, startTime);
        
        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        OutputStream os = metricsRetriever.createPptData("queryCount", rrdFilename, startTime, endTime);
        InputStream is = new ByteArrayInputStream(((ByteArrayOutputStream) os).toByteArray());
        assertThat(is, not(nullValue()));
        
        SlideShow ppt = new SlideShow(is);
        Slide[] slides = ppt.getSlides();
        assertThat(slides.length, equalTo(1));
        verifySlide(slides[0], "queryCount", false);
    }
    
    
    
    @Test
    public void testMetricsPptReport() throws Exception
    {
        String rrdFilename = TEST_DIR + "queryCount_Counter" + RRD_FILE_EXTENSION;
        long startTime = 900000000L;
        long endTime = createRrdFileWithCounter(rrdFilename, startTime);
        
        rrdFilename = TEST_DIR + "queryCount_Gauge" + RRD_FILE_EXTENSION;
        endTime = createRrdFileWithGauge(rrdFilename, startTime);
        
        List<String> metricNames = new ArrayList<String>();
        metricNames.add("queryCount_Counter");
        metricNames.add("queryCount_Gauge");
        
        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        OutputStream os = metricsRetriever.createPptReport(metricNames, TEST_DIR, startTime, endTime);
        InputStream is = new ByteArrayInputStream(((ByteArrayOutputStream) os).toByteArray());
        assertThat(is, not(nullValue()));
        
        SlideShow ppt = new SlideShow(is);
        Slide[] slides = ppt.getSlides();
        assertThat(slides.length, equalTo(2));
        
        verifySlide(slides[0], "queryCount_Counter", true);
        verifySlide(slides[1], "queryCount_Gauge", false);
    }     
    
    
    @Test //(expected = MetricsGraphException.class)
    public void testInvalidDataSourceType() throws Exception
    {
        String dataSourceName = "data";
        String rrdFilename = TEST_DIR + "dummy_Derive" + RRD_FILE_EXTENSION;
        int rrdStep = 60;  // in seconds
        long startTime = 900000000L;
        
        RrdDef rrdDef = new RrdDef(rrdFilename, rrdStep);
        rrdDef.setStartTime(startTime - 1);
        rrdDef.addDatasource(dataSourceName, DsType.DERIVE, 90, 0, Double.NaN);
        
        // 1 step, 60 seconds per step, for 5 minutes  
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 1, 5);  
        
        rrdDb = new RrdDb(rrdDef);
     
        long endTime = loadCounterSamples(dataSourceName, 4, rrdStep, startTime);
        
        rrdDb.close();
        
        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        try
        {
            metricsRetriever.createGraph("Dummy", rrdFilename, startTime, endTime);
            fail();
        }
        catch (MetricsGraphException e)
        {            
        }
    }
    
    
    @Test //(expected = MetricsGraphException.class)
    public void testRrdFileWithMultipleDataSources() throws Exception
    {
        String dataSourceName = "data";
        String rrdFilename = TEST_DIR + "dummy" + RRD_FILE_EXTENSION;
        int rrdStep = 60;  // in seconds
        long startTime = 900000000L;
        
        RrdDef rrdDef = new RrdDef(rrdFilename, rrdStep);
        rrdDef.setStartTime(startTime - 1);
        rrdDef.addDatasource(dataSourceName, DsType.COUNTER, 90, 0, Double.NaN);
        rrdDef.addDatasource("ds2", DsType.GAUGE, 90, 0, Double.NaN);
        
        // 1 step, 60 seconds per step, for 5 minutes  
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 1, 5);
        
        rrdDb = new RrdDb(rrdDef);
     
        long endTime = loadCounterSamples(dataSourceName, 4, rrdStep, startTime);
        
        rrdDb.close();
        
        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        try
        {
            metricsRetriever.createGraph("Dummy", rrdFilename, startTime, endTime);
            fail();
        }
        catch (MetricsGraphException e)
        {            
        }
    }
    

/*************************************************************************/
 
    
    private long createRrdFileWithCounter(String rrdFilename, long startTime) throws Exception
    {
        int rrdStep = 60;  // in seconds
        
        RrdDef rrdDef = new RrdDef(rrdFilename, rrdStep);
        rrdDef.setStartTime(startTime - 1);
        rrdDef.addDatasource("data", DsType.COUNTER, 90, 0, Double.NaN);
        
        // 1 step, 60 seconds per step, for 5 minutes
        rrdDef.addArchive(ConsolFun.TOTAL, 0.5, 1, 5);  
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 1, 5);  
        rrdDef.addArchive(ConsolFun.MAX, 0.5, 1, 5);  
        rrdDef.addArchive(ConsolFun.MIN, 0.5, 1, 5);
        
        // 5 steps, 60 seconds per step, for 30 minutes
        rrdDef.addArchive(ConsolFun.TOTAL, 0.5, 5, 6);  
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 5, 6);  
        rrdDef.addArchive(ConsolFun.MAX, 0.5, 5, 6);  
        rrdDef.addArchive(ConsolFun.MIN, 0.5, 5, 6);
        
        rrdDb = new RrdDb(rrdDef);
        
        // Enough samples to make sure both archivers get samples and the lower resolution archiver
        // is used by the RrdGraph
        int numSamples = 50; 
        
        // Small number of samples so that not enough data for second archiver to get any samples
        //int numSamples = 4;
        
        long endTime = loadCounterSamples("data", numSamples, rrdStep, startTime);
        
        dumpData("COUNTER", startTime, endTime);
        
        rrdDb.close();
        
        return endTime;
    }
    
    
    private long createRrdFileWithGauge(String rrdFilename, long startTime) throws Exception
    {
        int rrdStep = 60;  // in seconds
        RrdDef rrdDef = new RrdDef(rrdFilename, rrdStep);
        rrdDef.setStartTime(startTime - 1);
        rrdDef.addDatasource("data", DsType.GAUGE, 90, 0, Double.NaN);
        
        rrdDef.addArchive(ConsolFun.TOTAL, 0.5, 1, 5);  
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 1, 5);  
        rrdDef.addArchive(ConsolFun.MAX, 0.5, 1, 5);  
        rrdDef.addArchive(ConsolFun.MIN, 0.5, 1, 5);
                
        // 5 steps, 60 seconds per step, for 30 minutes
        rrdDef.addArchive(ConsolFun.TOTAL, 0.5, 5, 6);  
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 5, 6);  
        rrdDef.addArchive(ConsolFun.MAX, 0.5, 5, 6);  
        rrdDef.addArchive(ConsolFun.MIN, 0.5, 5, 6);
        
        rrdDb = new RrdDb(rrdDef);
        
        // Enough samples to make sure both archivers get samples and the lower resolution archiver
        // is used by the RrdGraph
        int numSamples = 50; 
        
        // Small number of samples so that not enough data for second archiver to get any samples
        //int numSamples = 4;
        
        long endTime = loadGaugeSamples("data", numSamples, rrdStep, startTime);
        
        dumpData("GAUGE", startTime, endTime);
        rrdDb.close();
        
        return endTime;
    }
    
    
    private long loadCounterSamples(String dataSourceName, int numSamples, int rrdStep, long startTime) 
        throws IOException
    {
        Sample sample = rrdDb.createSample();
        
        double queryCount = 0.0;
        int min = 0;
        int max = 20;
        Random random = new Random();
        long timestamp = startTime;

        for (int i=1; i <= numSamples; i++ )
        {
            int randomQueryCount = random.nextInt(max - min + 1) + min;
            queryCount += randomQueryCount;
            LOGGER.debug("timestamp: " + getCalendarTime(timestamp * 1000) + "  (" + timestamp + "),   queryCount = " + queryCount);
            sample.setTime(timestamp);
            sample.setValue(dataSourceName, queryCount);
            sample.update();
            
            // Increment by RRD step 
            // (can only add samples to data source on its step interval, not in between steps)
            timestamp += rrdStep;
        }
        
        return timestamp;
    }

    
    private long loadGaugeSamples(String dataSourceName, int numSamples, int rrdStep, long startTime) 
        throws IOException
    {
        Sample sample = rrdDb.createSample();
        
        int min = 200;
        int max = 2000;
        Random random = new Random();
        long timestamp = startTime;

        for (int i=1; i <= numSamples; i++ )
        {
            int randomQueryResponseTime = random.nextInt(max - min + 1) + min; // in ms
            LOGGER.debug("timestamp: " + getCalendarTime(timestamp * 1000) + "  (" + timestamp + "),   queryResponseTime = " + randomQueryResponseTime);
            sample.setTime(timestamp);
            sample.setValue(dataSourceName, randomQueryResponseTime);
            sample.update();
            
            // Increment by RRD step 
            // (can only add samples to data source on its step interval, not in between steps)
            timestamp += rrdStep;
        }
        
        return timestamp;
    }
    
    
    private String getCalendarTime(long timestamp)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp * 1000);
        
        String calTime = months[calendar.get(Calendar.MONTH)] +
            " " + calendar.get(Calendar.DATE) + " " + 
            calendar.get(Calendar.YEAR) + " ";
        
        calTime += addLeadingZero(calendar.get(Calendar.HOUR_OF_DAY)) + ":";
        calTime += addLeadingZero(calendar.get(Calendar.MINUTE)) + ":";
        calTime += addLeadingZero(calendar.get(Calendar.SECOND)); 
        
        return calTime;
    }
    
    
    private String addLeadingZero(int value)
    {
        if (value < 10)
        {
            return "0" + String.valueOf(value);
        }       
        
        return String.valueOf(value);
    }
    
    
    private void verifyWorksheet(HSSFSheet sheet, String metricName, int expectedNumberOfDataRows, boolean hasTotalCount)
    {
          // 3 = title + blank row + column headers
          int expectedTotalRows = 3 + expectedNumberOfDataRows;
          if (hasTotalCount) expectedTotalRows += 2;
          assertThat(sheet.getPhysicalNumberOfRows(), equalTo(expectedTotalRows));
          
          // first row should have title in first cell
          HSSFRow row = sheet.getRow(0);
          assertThat(row, not(nullValue()));
          assertThat(row.getCell(0).getStringCellValue(), startsWith(metricName + " for"));
          
          // third row should have column headers in first and second cells
          row = sheet.getRow(2);
          assertThat(row.getCell(0).getStringCellValue(), equalTo("Timestamp"));
          assertThat(row.getCell(1).getStringCellValue(), equalTo("Value"));
          
          // verify rows with the sample data, i.e., timestamps and values
          int endRow = 3 + expectedNumberOfDataRows;
          for (int i=3; i < endRow; i++)
          {
              row = sheet.getRow(i);
              assertThat(row.getCell(0).getStringCellValue(), not(nullValue()));
              assertThat(row.getCell(1).getNumericCellValue(), not(nullValue()));
          }
          
          row = sheet.getRow(sheet.getLastRowNum());
          if (hasTotalCount)
          {             
              assertThat(row.getCell(0).getStringCellValue(), startsWith("Total Count:"));
              assertThat(row.getCell(1).getNumericCellValue(), not(nullValue()));
          }
          else
          {
              assertThat(row.getCell(0).getStringCellValue(), not(startsWith("Total Count:")));
          }
    }
    
    
    private void verifySlide(Slide slide, String metricName, boolean hasTotalCount)
    {
        assertThat(slide, not(nullValue()));
        
        Shape[] shapes = slide.getShapes();
        assertThat(shapes, not(nullValue()));
        
        // expected shapes: title text box, metric's graph, metric's total count text box
        int numExpectedShapes = 2;
        int numExpectedTextBoxes = 1;
        if (hasTotalCount) 
        {
            numExpectedShapes++;
            numExpectedTextBoxes++;
        }
        assertThat(shapes.length, equalTo(numExpectedShapes));
        
        Picture picture = null;
        int numTextBoxes = 0;
        
        for (int i=0; i < numExpectedShapes; i++)
        {
            if (shapes[i] instanceof Picture)
            {
                picture = (Picture) shapes[i];
            }
            // title text box is actually an AutoShape
            else if (shapes[i] instanceof TextBox || shapes[i] instanceof AutoShape)
            {
                numTextBoxes++;
            }
        }
        
        assertThat(picture, not(nullValue()));
        assertThat(picture.getPictureData().getType(), equalTo(Picture.PNG));
        assertThat(numTextBoxes, equalTo(numExpectedTextBoxes));
    }
    
    
    private void dumpData(String dsType, long startTime, long endTime) throws IOException
    {      
        LOGGER.debug(rrdDb.dump());
        
        FetchRequest fetchRequest = rrdDb.createFetchRequest(ConsolFun.AVERAGE, startTime, endTime);
        FetchData fetchData = fetchRequest.fetchData();
        LOGGER.debug("************  " + dsType + ": AVERAGE  **************");
//        LOGGER.debug(fetchData.dump());
        long[] timestamps = fetchData.getTimestamps();
        double[] values = fetchData.getValues(0);
        for (int i=0; i < timestamps.length; i++)
        {
            LOGGER.debug(getCalendarTime(timestamps[i]) + ":  " + values[i]);
        }
        
        fetchRequest = rrdDb.createFetchRequest(ConsolFun.TOTAL, startTime, endTime);
        fetchData = fetchRequest.fetchData();
        LOGGER.debug("************  " + dsType + ": TOTAL  **************");
//        LOGGER.debug(fetchData.dump());
        timestamps = fetchData.getTimestamps();
        values = fetchData.getValues(0);
        for (int i=0; i < timestamps.length; i++)
        {
            LOGGER.debug(getCalendarTime(timestamps[i]) + ":  " + values[i]);
        }
        
//        fetchRequest = rrdDb.createFetchRequest(ConsolFun.MIN, startTime, endTime);
//        fetchData = fetchRequest.fetchData();
//        LOGGER.debug("************  " + dsType + ": MIN  **************");
//        LOGGER.debug(fetchData.dump());
//        
//        fetchRequest = rrdDb.createFetchRequest(ConsolFun.MAX, startTime, endTime);
//        fetchData = fetchRequest.fetchData();
//        LOGGER.debug("************  " + dsType + ": MAX  **************");
//        LOGGER.debug(fetchData.dump());
    }
}
