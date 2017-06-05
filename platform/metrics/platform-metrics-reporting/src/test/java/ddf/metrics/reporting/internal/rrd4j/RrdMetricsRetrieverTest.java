/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.metrics.reporting.internal.rrd4j;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
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

import org.apache.poi.hslf.model.AutoShape;
import org.apache.poi.hslf.model.Picture;
import org.apache.poi.hslf.model.Shape;
import org.apache.poi.hslf.model.Slide;
import org.apache.poi.hslf.model.TextBox;
import org.apache.poi.hslf.usermodel.PictureData;
import org.apache.poi.hslf.usermodel.SlideShow;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.custommonkey.xmlunit.XMLTestCase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.metrics.reporting.internal.MetricsGraphException;
import ddf.metrics.reporting.internal.MetricsRetriever;

public class RrdMetricsRetrieverTest extends XMLTestCase {
    private static final transient Logger LOGGER =
            LoggerFactory.getLogger(RrdMetricsRetrieverTest.class);

    private static final String TEST_DIR = "target/";

    private static final String RRD_FILE_EXTENSION = ".rrd";

    private static final long START_TIME = 900000000L;

    private static final int RRD_STEP = 60;

    private static final String MONTHS[] =
            {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    @After
    public void tearDown() throws Exception {

        // Delete all .png files in test directory to ensure starting with clean directory
        FilenameFilter pngFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.endsWith(".rrd") || name.endsWith(".png")) {
                    return true;
                }

                return false;
            }
        };

        File testDir = new File(TEST_DIR);
        File[] fileList = testDir.listFiles(pngFilter);
        if (null != fileList) {
            for (File file : fileList) {
                if (file.isFile()) {
                    file.delete();
                }
            }
        }
    }

    @Test
    public void testMetricsGraphWithGauge() throws Exception {
        String rrdFilename = TEST_DIR + "queryCount_Gauge" + RRD_FILE_EXTENSION;
        long endTime = new RrdFileBuilder().rrdFileName(rrdFilename)
                .dsType(DsType.GAUGE)
                .build();

        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        byte[] metricsGraph = metricsRetriever.createGraph("Query Reponse Time",
                rrdFilename,
                START_TIME,
                endTime);

        assertThat(metricsGraph, not(nullValue()));
        assertThat(metricsGraph.length, is(greaterThan(0)));

        // NOTE: RrdGraph provides no way to programmatically verify the title and axis label were
        // set, i.e., no getters
        // All we can verify is that this alternate createGraph() method returns a byte array of the
        // graph image. Only
        // visual inspection can verify the graph's accurracy, title, and axis labels.
        metricsGraph = metricsRetriever.createGraph("Query Reponse Time",
                rrdFilename,
                START_TIME,
                endTime,
                "My Vertical Axis Label",
                "My Title");

        assertThat(metricsGraph, not(nullValue()));
        assertThat(metricsGraph.length, is(greaterThan(0)));
    }

    @Test
    public void testMetricsGraphWithCounter() throws Exception {
        String rrdFilename = TEST_DIR + "queryCount_Counter" + RRD_FILE_EXTENSION;
        long endTime = new RrdFileBuilder().rrdFileName(rrdFilename)
                .build();

        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        byte[] metricsGraph = metricsRetriever.createGraph("Query Count",
                rrdFilename,
                START_TIME,
                endTime);

        assertThat(metricsGraph, not(nullValue()));
        assertThat(metricsGraph.length, is(greaterThan(0)));

        // NOTE: RrdGraph provides no way to programmatically verify the title and axis label were
        // set, i.e., no getters
        // All we can verify is that this alternate createGraph() method returns a byte array of the
        // graph image. Only
        // visual inspection can verify the graph's accurracy, title, and axis labels.
        metricsGraph = metricsRetriever.createGraph("Query Count",
                rrdFilename,
                START_TIME,
                endTime,
                "My Vertical Axis Label",
                "My Title");

        assertThat(metricsGraph, not(nullValue()));
        assertThat(metricsGraph.length, is(greaterThan(0)));
    }

    @Test
    public void testMetricsJsonDataWithCounter() throws Exception {
        String rrdFilename = TEST_DIR + "queryCount_Counter" + RRD_FILE_EXTENSION;
        long endTime = new RrdFileBuilder().rrdFileName(rrdFilename)
                .build();

        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        String json = metricsRetriever.createJsonData("queryCount",
                rrdFilename,
                START_TIME,
                endTime);

        JSONParser parser = new JSONParser();
        JSONObject jsonObj = (JSONObject) parser.parse(json);

        // Verify the title, totalCount, and data (i.e., samples) are present
        assertThat(jsonObj.size(), equalTo(3));
        assertThat(jsonObj.get("title"), not(nullValue()));
        assertThat(jsonObj.get("totalCount"), not(nullValue()));

        // Verify 2 samples were retrieved from the RRD file and put in the JSON fetch results
        JSONArray samples = (JSONArray) jsonObj.get("data");
        assertThat(samples.size(), equalTo(6)); // 6 because that's the max num rows configured for
        // the RRD file

        // Verify each retrieved sample has a timestamp and value
        for (int i = 0; i < samples.size(); i++) {
            JSONObject sample = (JSONObject) samples.get(i);
            LOGGER.debug("timestamp = {},   value= {}",
                    (String) sample.get("timestamp"),
                    sample.get("value"));
            assertThat(sample.get("timestamp"), not(nullValue()));
            assertThat(sample.get("value"), not(nullValue()));
        }
    }

    @Test
    public void testMetricsJsonDataWithGauge() throws Exception {
        String rrdFilename = TEST_DIR + "queryCount_Gauge" + RRD_FILE_EXTENSION;
        long endTime = new RrdFileBuilder().rrdFileName(rrdFilename)
                .dsType(DsType.GAUGE)
                .build();

        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        String json = metricsRetriever.createJsonData("queryCount",
                rrdFilename,
                START_TIME,
                endTime);

        JSONParser parser = new JSONParser();
        JSONObject jsonObj = (JSONObject) parser.parse(json);

        // Verify the title, and data (i.e., samples) are present
        assertThat(jsonObj.size(), equalTo(2));
        assertThat(jsonObj.get("title"), not(nullValue()));

        // Verify 2 samples were retrieved from the RRD file and put in the JSON fetch results
        JSONArray samples = (JSONArray) jsonObj.get("data");
        assertThat(samples.size(), equalTo(6)); // 6 because that's the max num rows configured for
        // the RRD file

        // Verify each retrieved sample has a timestamp and value
        for (int i = 0; i < samples.size(); i++) {
            JSONObject sample = (JSONObject) samples.get(i);
            LOGGER.debug("timestamp = {}, value = {}",
                    (String) sample.get("timestamp"),
                    sample.get("value"));
            assertThat(sample.get("timestamp"), not(nullValue()));
            assertThat(sample.get("value"), not(nullValue()));
        }
    }

    @Test
    public void testMetricsCsvDataWithCounter() throws Exception {
        String rrdFilename = TEST_DIR + "queryCount_Counter" + RRD_FILE_EXTENSION;
        long endTime = new RrdFileBuilder().rrdFileName(rrdFilename)
                .build();

        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        String csv = metricsRetriever.createCsvData(rrdFilename, START_TIME, endTime);

        // Break up CSV data into its individual lines
        // Each line should have 2 parts (cells)
        // The first line should be the column headers
        String[] csvLines = csv.split("\n");

        // Verify that number of lines should be the column headers + (number of samples - 1)
        // Since 3 samples were taken, but only 2 are added to the RRD file due to averaging,
        // expect 2 lines of data and the one line of column headers.
        assertThat(csvLines.length, equalTo(7));
        assertThat(csvLines[0], equalTo("Timestamp,Value")); // column headers
        String[] cells = csvLines[1].split(",");
        assertThat(cells.length, equalTo(2)); // each line of data has the 2 values
    }

    @Test
    public void testMetricsCsvDataWithGauge() throws Exception {
        String rrdFilename = TEST_DIR + "queryCount_Gauge" + RRD_FILE_EXTENSION;
        long endTime = new RrdFileBuilder().rrdFileName(rrdFilename)
                .dsType(DsType.GAUGE)
                .build();

        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        String csv = metricsRetriever.createCsvData(rrdFilename, START_TIME, endTime);

        // Break up CSV data into its individual lines
        // Each line should have 2 parts (cells)
        // The first line should be the column headers
        String[] csvLines = csv.split("\n");

        // Verify that number of lines should be the column headers + (number of samples - 1)
        // Since 3 samples were taken, but only 2 are added to the RRD file due to averaging,
        // expect 2 lines of data and the one line of column headers.
        assertThat(csvLines.length, equalTo(7));
        assertThat(csvLines[0], equalTo("Timestamp,Value")); // column headers
        String[] cells = csvLines[1].split(",");
        assertThat(cells.length, equalTo(2)); // each line of data has the 2 values
    }

    @Test
    public void testMetricsXlsDataWithCounter() throws Exception {
        String rrdFilename = TEST_DIR + "queryCount_Counter" + RRD_FILE_EXTENSION;
        long endTime = new RrdFileBuilder().rrdFileName(rrdFilename)
                .build();

        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        OutputStream os = metricsRetriever.createXlsData("queryCount",
                rrdFilename,
                START_TIME,
                endTime);
        InputStream xls = new ByteArrayInputStream(((ByteArrayOutputStream) os).toByteArray());
        assertThat(xls, not(nullValue()));

        HSSFWorkbook wb = new HSSFWorkbook(xls);
        assertThat(wb.getNumberOfSheets(), equalTo(1));

        HSSFSheet sheet = wb.getSheet("Query Count");
        if (null != sheet) {
            assertThat(sheet, not(nullValue()));
            verifyWorksheet(sheet, "Query Count", 6, true);
        } else {
            fail();
        }
    }

    @Test
    public void testGetMetricDataTimeRangeFiltering() throws Exception {
        String rrdFilename = TEST_DIR + "queryCount_Counter_TimeRange" + RRD_FILE_EXTENSION;
        long startTime = 1430463600L; // May 1st, 2015 00:00:00
        long endTime = 1433141999L;  // May 31st, 2015 23:59:59
        int minutes = 31 * 24 * 60;  // minutes between startTime and endTime
        int numSamples = minutes + 10; // go into the next month
        new RrdFileBuilder().startTime(startTime - RRD_STEP * 2)
                .rrdFileName(rrdFilename)
                .numRows(numSamples)
                .numSamples(numSamples)
                .dsType(DsType.GAUGE)
                .build();

        RrdMetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        MetricData metricData = metricsRetriever.getMetricData(rrdFilename, startTime, endTime);
        assertThat(metricData.getTimestamps()
                .size(), equalTo(minutes));
        assertThat(metricData.getTimestamps()
                .get(minutes - 1), lessThanOrEqualTo(endTime));
        assertThat(metricData.getTimestamps()
                .get(0), greaterThanOrEqualTo(startTime));
        metricData = metricsRetriever.getMetricData(rrdFilename,
                startTime - RRD_STEP * 2,
                endTime + RRD_STEP * 8);
        assertThat(metricData.getTimestamps()
                .get(0), lessThan(startTime));
        assertThat(metricData.getTimestamps()
                .get(numSamples - 1), greaterThan(endTime));
        assertThat(metricData.getTimestamps()
                .size(), equalTo(numSamples));
    }

    @Test
    public void testMetricsXlsDataWithGauge() throws Exception {
        String rrdFilename = TEST_DIR + "queryCount_Gauge" + RRD_FILE_EXTENSION;
        long endTime = new RrdFileBuilder().rrdFileName(rrdFilename)
                .dsType(DsType.GAUGE)
                .build();

        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        OutputStream os = metricsRetriever.createXlsData("queryCount",
                rrdFilename,
                START_TIME,
                endTime);
        InputStream xls = new ByteArrayInputStream(((ByteArrayOutputStream) os).toByteArray());
        assertThat(xls, not(nullValue()));

        HSSFWorkbook wb = new HSSFWorkbook(xls);
        assertThat(wb.getNumberOfSheets(), equalTo(1));
        HSSFSheet sheet = wb.getSheet("Query Count");
        if (null != sheet) {
            assertThat(sheet, not(nullValue()));
            verifyWorksheet(sheet, "Query Count", 6, false);
        } else {
            fail();
        }
    }

    @Test
    public void testMetricsXmlDataWithCounter() throws Exception {
        String rrdFilename = TEST_DIR + "queryCount_Counter" + RRD_FILE_EXTENSION;
        long endTime = new RrdFileBuilder().rrdFileName(rrdFilename)
                .build();

        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        String xml = metricsRetriever.createXmlData("queryCount", rrdFilename, START_TIME, endTime);
        assertXpathExists("/queryCount", xml);
        assertXpathExists("/queryCount/title", xml);
        assertXpathExists("/queryCount/data", xml);
        assertXpathExists("/queryCount/data/sample", xml);
        assertXpathExists("/queryCount/data/sample/timestamp", xml);
        assertXpathExists("/queryCount/data/sample/value", xml);
        assertXpathExists("/queryCount/data/totalCount", xml);
    }

    @Test
    public void testMetricsXmlDataWithGauge() throws Exception {
        String rrdFilename = TEST_DIR + "queryCount_Gauge" + RRD_FILE_EXTENSION;
        long endTime = new RrdFileBuilder().rrdFileName(rrdFilename)
                .dsType(DsType.GAUGE)
                .build();

        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        String xml = metricsRetriever.createXmlData("queryCount", rrdFilename, START_TIME, endTime);
        assertXpathExists("/queryCount", xml);
        assertXpathExists("/queryCount/title", xml);
        assertXpathExists("/queryCount/data", xml);
        assertXpathExists("/queryCount/data/sample", xml);
        assertXpathExists("/queryCount/data/sample/timestamp", xml);
        assertXpathExists("/queryCount/data/sample/value", xml);
        assertXpathNotExists("/queryCount/data/totalCount", xml);
    }

    @Test
    public void testMetricsXlsReport() throws Exception {
        String rrdFilename = TEST_DIR + "queryCount_Counter" + RRD_FILE_EXTENSION;
        new RrdFileBuilder().rrdFileName(rrdFilename)
                .build();

        rrdFilename = TEST_DIR + "queryCount_Gauge" + RRD_FILE_EXTENSION;
        long endTime = new RrdFileBuilder().rrdFileName(rrdFilename)
                .dsType(DsType.GAUGE)
                .build();

        List<String> metricNames = new ArrayList<String>();
        metricNames.add("queryCount_Counter");
        metricNames.add("queryCount_Gauge");

        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        OutputStream os = metricsRetriever.createXlsReport(metricNames,
                TEST_DIR,
                START_TIME,
                endTime,
                null);
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
    public void testMetricsXlsReportSummary() throws Exception {
        String metricName = "queryCount_Gauge";
        long endTime = new DateTime(DateTimeZone.UTC).getMillis();
        List<String> metricNames = new ArrayList<String>();
        metricNames.add(metricName);
        for (RrdMetricsRetriever.SUMMARY_INTERVALS interval : RrdMetricsRetriever.SUMMARY_INTERVALS.values()) {
            long startTime = 0L;
            switch (interval) {
            case minute:
                startTime = new DateTime(DateTimeZone.UTC).minusHours(1)
                        .getMillis();
                break;
            case hour:
                startTime = new DateTime(DateTimeZone.UTC).minusDays(1)
                        .getMillis();
                break;
            case day:
                startTime = new DateTime(DateTimeZone.UTC).minusWeeks(1)
                        .getMillis();
                break;
            case week:
                startTime = new DateTime(DateTimeZone.UTC).minusMonths(1)
                        .getMillis();
                break;
            case month:
                startTime = new DateTime(DateTimeZone.UTC).minusYears(1)
                        .getMillis();
                break;
            }
            int sampleSize = (int) ((endTime - startTime) / (RRD_STEP * 1000));
            new RrdFileBuilder().rrdFileName(TEST_DIR + metricName + RRD_FILE_EXTENSION)
                    .dsType(DsType.GAUGE)
                    .numSamples(sampleSize)
                    .numRows(sampleSize)
                    .startTime(startTime)
                    .build();
            MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
            OutputStream os = metricsRetriever.createXlsReport(metricNames,
                    TEST_DIR,
                    startTime,
                    endTime,
                    interval.toString());
            InputStream xls = new ByteArrayInputStream(((ByteArrayOutputStream) os).toByteArray());
            assertThat(xls, not(nullValue()));

            HSSFWorkbook wb = new HSSFWorkbook(xls);
            assertThat(wb.getNumberOfSheets(), equalTo(1));

            HSSFSheet sheet = wb.getSheetAt(0);
            assertThat(sheet, not(nullValue()));
        }
    }

    @Test
    public void testMetricsPptDataWithCounter() throws Exception {
        String rrdFilename = TEST_DIR + "queryCount_Counter" + RRD_FILE_EXTENSION;
        long endTime = new RrdFileBuilder().rrdFileName(rrdFilename)
                .build();

        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        OutputStream os = metricsRetriever.createPptData("queryCount",
                rrdFilename,
                START_TIME,
                endTime);
        InputStream is = new ByteArrayInputStream(((ByteArrayOutputStream) os).toByteArray());
        assertThat(is, not(nullValue()));

        SlideShow ppt = new SlideShow(is);
        Slide[] slides = ppt.getSlides();
        assertThat(slides.length, equalTo(1));
        verifySlide(slides[0], true);
    }

    @Test
    public void testMetricsPptDataWithGauge() throws Exception {
        String rrdFilename = TEST_DIR + "queryCount_Gauge" + RRD_FILE_EXTENSION;
        long endTime = new RrdFileBuilder().rrdFileName(rrdFilename)
                .dsType(DsType.GAUGE)
                .build();

        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        OutputStream os = metricsRetriever.createPptData("queryCount",
                rrdFilename,
                START_TIME,
                endTime);
        InputStream is = new ByteArrayInputStream(((ByteArrayOutputStream) os).toByteArray());
        assertThat(is, not(nullValue()));

        SlideShow ppt = new SlideShow(is);
        Slide[] slides = ppt.getSlides();
        assertThat(slides.length, equalTo(1));
        verifySlide(slides[0], false);
    }

    @Test
    public void testMetricsPptReport() throws Exception {
        String rrdFilename = TEST_DIR + "queryCount_Counter" + RRD_FILE_EXTENSION;
        new RrdFileBuilder().rrdFileName(rrdFilename)
                .build();

        rrdFilename = TEST_DIR + "queryCount_Gauge" + RRD_FILE_EXTENSION;
        long endTime = new RrdFileBuilder().rrdFileName(rrdFilename)
                .dsType(DsType.GAUGE)
                .build();

        List<String> metricNames = new ArrayList<String>();
        metricNames.add("queryCount_Counter");
        metricNames.add("queryCount_Gauge");

        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        OutputStream os = metricsRetriever.createPptReport(metricNames,
                TEST_DIR,
                START_TIME,
                endTime);
        InputStream is = new ByteArrayInputStream(((ByteArrayOutputStream) os).toByteArray());
        assertThat(is, not(nullValue()));

        SlideShow ppt = new SlideShow(is);
        Slide[] slides = ppt.getSlides();
        assertThat(slides.length, equalTo(2));

        verifySlide(slides[0], true);
        verifySlide(slides[1], false);
    }

    @Test
    // (expected = MetricsGraphException.class)
    public void testInvalidDataSourceType() throws Exception {
        String rrdFilename = TEST_DIR + "dummy_Absolute" + RRD_FILE_EXTENSION;
        long endTime = new RrdFileBuilder().rrdFileName(rrdFilename)
                .numSamples(4)
                .dsType(DsType.ABSOLUTE)
                .build();

        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        try {
            metricsRetriever.createGraph("Dummy", rrdFilename, START_TIME, endTime);
            fail();
        } catch (MetricsGraphException e) {
        }
    }

    @Test
    // (expected = MetricsGraphException.class)
    public void testRrdFileWithMultipleDataSources() throws Exception {
        String rrdFilename = TEST_DIR + "dummy" + RRD_FILE_EXTENSION;

        long endTime = new RrdFileBuilder().rrdFileName(rrdFilename)
                .numSamples(4)
                .secondDataSource(true)
                .build();

        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
        try {
            metricsRetriever.createGraph("Dummy", rrdFilename, START_TIME, endTime);
            fail();
        } catch (MetricsGraphException e) {
        }
    }

    private void verifyWorksheet(HSSFSheet sheet, String metricName, int expectedNumberOfDataRows,
            boolean hasTotalCount) {
        // 3 = title + blank row + column headers
        int expectedTotalRows = 3 + expectedNumberOfDataRows;
        if (hasTotalCount) {
            expectedTotalRows += 2;
        }
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(expectedTotalRows));

        // first row should have title in first cell
        HSSFRow row = sheet.getRow(0);
        assertThat(row, not(nullValue()));
        assertThat(row.getCell(0)
                .getStringCellValue(), startsWith(metricName + " for"));

        // third row should have column headers in first and second cells
        row = sheet.getRow(2);
        assertThat(row.getCell(0)
                .getStringCellValue(), equalTo("Timestamp"));
        assertThat(row.getCell(1)
                .getStringCellValue(), equalTo("Value"));

        // verify rows with the sample data, i.e., timestamps and values
        int endRow = 3 + expectedNumberOfDataRows;
        for (int i = 3; i < endRow; i++) {
            row = sheet.getRow(i);
            assertThat(row.getCell(0)
                    .getStringCellValue(), not(nullValue()));
            assertThat(row.getCell(1)
                    .getNumericCellValue(), not(nullValue()));
        }

        row = sheet.getRow(sheet.getLastRowNum());
        if (hasTotalCount) {
            assertThat(row.getCell(0)
                    .getStringCellValue(), startsWith("Total Count:"));
            assertThat(row.getCell(1)
                    .getNumericCellValue(), not(nullValue()));
        } else {
            assertThat(row.getCell(0)
                    .getStringCellValue(), not(startsWith("Total Count:")));
        }
    }

    private void verifySlide(Slide slide, boolean hasTotalCount) {
        assertThat(slide, not(nullValue()));

        Shape[] shapes = slide.getShapes();
        assertThat(shapes, not(nullValue()));

        // expected shapes: title text box, metric's graph, metric's total count text box
        int numExpectedShapes = 2;
        int numExpectedTextBoxes = 1;
        if (hasTotalCount) {
            numExpectedShapes++;
            numExpectedTextBoxes++;
        }
        assertThat(shapes.length, equalTo(numExpectedShapes));

        Picture picture = null;
        int numTextBoxes = 0;

        for (int i = 0; i < numExpectedShapes; i++) {
            if (shapes[i] instanceof Picture) {
                picture = (Picture) shapes[i];
                // title text box is actually an AutoShape
            } else if (shapes[i] instanceof TextBox || shapes[i] instanceof AutoShape) {
                numTextBoxes++;
            }
        }

        assertThat(picture, not(nullValue()));
        PictureData picData = picture.getPictureData();
        assertThat(picData, not(nullValue()));
        assertThat(picData.getType(), equalTo(Picture.PNG));
        assertThat(numTextBoxes, equalTo(numExpectedTextBoxes));
    }

    /**
     * *********************************************************************
     */

    public static class RrdFileBuilder {
        private RrdDb rrdDb;

        private RrdDef rrdDef;

        private int rrdStep;

        private String rrdFileName;

        private DsType dsType;

        private long startTime;

        private long endTime;

        private int numRows;

        private int numSamples;

        private boolean secondDataSource;

        private boolean dumpData;

        public RrdFileBuilder() {
            secondDataSource = false;
            dsType = DsType.COUNTER;
            startTime = START_TIME;
            rrdStep = RRD_STEP;
            numRows = 5;
            numSamples = 50;
            dumpData = false;
        }

        public RrdFileBuilder rrdFileName(String rrdFileName) {
            this.rrdFileName = rrdFileName;
            return this;
        }

        public RrdFileBuilder dsType(DsType dsType) {
            this.dsType = dsType;
            return this;
        }

        public RrdFileBuilder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }

        public RrdFileBuilder numRows(int numRows) {
            this.numRows = numRows;
            return this;
        }

        public RrdFileBuilder numSamples(int numSamples) {
            this.numSamples = numSamples;
            return this;
        }

        public RrdFileBuilder secondDataSource(boolean secondDataSource) {
            this.secondDataSource = secondDataSource;
            return this;
        }

        public long build() throws Exception {
            rrdDef = new RrdDef(rrdFileName, rrdStep);
            rrdDef.setStartTime(startTime - 1);
            rrdDef.addDatasource("data", dsType, 90, 0, Double.NaN);
            if (secondDataSource) {
                rrdDef.addDatasource("ds2", DsType.GAUGE, 90, 0, Double.NaN);
            }

            // 1 step, 60 seconds per step, for 5 minutes
            rrdDef.addArchive(ConsolFun.TOTAL, 0.5, 1, numRows);
            rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 1, numRows);
            rrdDef.addArchive(ConsolFun.MAX, 0.5, 1, numRows);
            rrdDef.addArchive(ConsolFun.MIN, 0.5, 1, numRows);

            // 5 steps, 60 seconds per step, for 30 minutes
            rrdDef.addArchive(ConsolFun.TOTAL, 0.5, 5, numRows + 1);
            rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 5, numRows + 1);
            rrdDef.addArchive(ConsolFun.MAX, 0.5, 5, numRows + 1);
            rrdDef.addArchive(ConsolFun.MIN, 0.5, 5, numRows + 1);

            rrdDb = new RrdDb(rrdDef);

            endTime = (dsType == DsType.GAUGE) ? loadGaugeSamples() : loadCounterSamples();

            if (dumpData) {
                dumpData();
            }
            rrdDb.close();

            return endTime;
        }

        private long loadCounterSamples() throws IOException {
            Sample sample = rrdDb.createSample();

            double queryCount = 0.0;
            int min = 0;
            int max = 20;
            Random random = new Random();
            long timestamp = startTime;

            for (int i = 1; i <= numSamples; i++) {
                int randomQueryCount = random.nextInt(max - min + 1) + min;
                queryCount += randomQueryCount;
                LOGGER.debug("timestamp: {}   ({}),   queryCount = {}",
                        getCalendarTime(timestamp * 1000),
                        timestamp,
                        queryCount);
                sample.setTime(timestamp);
                sample.setValue("data", queryCount);
                sample.update();

                // Increment by RRD step
                // (can only add samples to data source on its step interval, not in between steps)
                timestamp += rrdStep;
            }

            return timestamp;
        }

        private long loadGaugeSamples() throws IOException {
            Sample sample = rrdDb.createSample();

            int min = 200;
            int max = 2000;
            Random random = new Random();
            long timestamp = startTime;

            for (int i = 1; i <= numSamples; i++) {
                int randomQueryResponseTime = random.nextInt(max - min + 1) + min; // in ms
                LOGGER.debug("timestamp: {},  ({}),   queryResponseTime = {}",
                        getCalendarTime(timestamp * 1000),
                        timestamp,
                        randomQueryResponseTime);
                sample.setTime(timestamp);
                sample.setValue("data", randomQueryResponseTime);
                sample.update();

                // Increment by RRD step
                // (can only add samples to data source on its step interval, not in between steps)
                timestamp += rrdStep;
            }

            return timestamp;
        }

        private String getCalendarTime(long timestamp) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timestamp * 1000);

            String calTime =
                    MONTHS[calendar.get(Calendar.MONTH)] + " " + calendar.get(Calendar.DATE) + " "
                            + calendar.get(Calendar.YEAR) + " ";

            calTime += addLeadingZero(calendar.get(Calendar.HOUR_OF_DAY)) + ":";
            calTime += addLeadingZero(calendar.get(Calendar.MINUTE)) + ":";
            calTime += addLeadingZero(calendar.get(Calendar.SECOND));

            return calTime;
        }

        private String addLeadingZero(int value) {
            if (value < 10) {
                return "0" + String.valueOf(value);
            }

            return String.valueOf(value);
        }

        private void dumpData() throws IOException {
            LOGGER.debug(rrdDb.dump());

            FetchRequest fetchRequest = rrdDb.createFetchRequest(ConsolFun.AVERAGE,
                    startTime,
                    endTime);
            FetchData fetchData = fetchRequest.fetchData();
            LOGGER.debug("************  {}: AVERAGE  **************", dsType.name());
            LOGGER.debug(fetchData.dump());
            long[] timestamps = fetchData.getTimestamps();
            double[] values = fetchData.getValues(0);
            for (int i = 0; i < timestamps.length; i++) {
                LOGGER.debug("{}:  {}", getCalendarTime(timestamps[i]), values[i]);
            }

            fetchRequest = rrdDb.createFetchRequest(ConsolFun.TOTAL, startTime, endTime);
            fetchData = fetchRequest.fetchData();
            LOGGER.debug("************  {}: TOTAL  **************", dsType.name());
            LOGGER.debug(fetchData.dump());
            timestamps = fetchData.getTimestamps();
            values = fetchData.getValues(0);
            for (int i = 0; i < timestamps.length; i++) {
                LOGGER.debug("{}:  {}", getCalendarTime(timestamps[i]), values[i]);
            }

            fetchRequest = rrdDb.createFetchRequest(ConsolFun.MIN, startTime, endTime);
            fetchData = fetchRequest.fetchData();
            LOGGER.debug("************  {}: MIN  **************", dsType);
            LOGGER.debug(fetchData.dump());

            fetchRequest = rrdDb.createFetchRequest(ConsolFun.MAX, startTime, endTime);
            fetchData = fetchRequest.fetchData();
            LOGGER.debug("************  {}: MAX  **************", dsType);
            LOGGER.debug(fetchData.dump());
        }
    }
}
