/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.metrics.reporting.internal.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.metrics.reporting.internal.MetricsEndpointException;
import ddf.metrics.reporting.internal.MetricsGraphException;
import ddf.metrics.reporting.internal.rrd4j.RrdMetricsRetriever;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.custommonkey.xmlunit.XMLTestCase;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDbPool;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsEndpointTest extends XMLTestCase {
  private static final Logger LOGGER = LoggerFactory.getLogger(MetricsEndpointTest.class);

  private static final String TEST_DIR = "target/";

  private static final String ENDPOINT_ADDRESS = "http://localhost:8181/services/internal/metrics";

  private static final String MONTHS[] = {
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
  };

  private RrdDb rrdDb;

  private String rrdPath;

  @Test
  public void testParseDate() {
    long time = getEndpoint().parseDate("1998-07-09T09:00:00-07:00");
    assertThat(time, equalTo(900000000L));
  }

  @SuppressWarnings("rawtypes")
  @Test
  // @Ignore
  public void testGetMetricsList() throws Exception {
    // Delete all .rrd files in test directory to ensure starting with clean directory
    File testDir = new File(TEST_DIR);
    File[] fileList = testDir.listFiles();
    if (fileList != null) {
      for (File file : fileList) {
        if (file.isFile()) {
          file.delete();
        }
      }
    }

    // Create RRD file that Metrics Endpoint will detect
    rrdPath = TEST_DIR + "uptime.rrd";
    RrdDef def = new RrdDef(rrdPath, 1);
    def.addDatasource("uptime", DsType.COUNTER, 90, 0, Double.NaN);
    def.addArchive(ConsolFun.TOTAL, 0.5, 1, 60);
    rrdDb = RrdDbPool.getInstance().requestRrdDb(def);

    UriInfo uriInfo = createUriInfo();

    // Get the metrics list from the endpoint
    MetricsEndpoint endpoint = getEndpoint();
    endpoint.setMetricsDir(TEST_DIR);
    Response response = endpoint.getMetricsList(uriInfo);
    String metricsList = (String) response.getEntity();
    LOGGER.debug("metricsList = {}", metricsList);

    cleanupRrd();

    // Useful class for simple JSON to handle collections when parsing
    // (Called internally by the simple JSON parser.parse(...) method)
    ContainerFactory containerFactory =
        new ContainerFactory() {
          public List creatArrayContainer() {
            return new LinkedList();
          }

          public Map createObjectContainer() {
            return new LinkedHashMap();
          }
        };

    // Parse the returned JSON text
    JSONParser parser = new JSONParser();
    Map json = (Map) parser.parse(metricsList, containerFactory);
    Set<String> metricNames = (Set<String>) json.keySet();
    assertThat(metricNames.size(), equalTo(1));
    assertThat(metricNames, hasItem("uptime"));

    Iterator metricsIter = json.entrySet().iterator();

    // For each metric, a list of associated hyperlinks and their display
    // text is provided, e.g., "1h" and its associated hyperlink with the
    // corresponding dateOffset (in seconds) from current time
    // http://<host>:<port>/services/internal/metrics?dateOffset=3600
    while (metricsIter.hasNext()) {
      Map.Entry entry = (Map.Entry) metricsIter.next();
      Map metricTimeRangeLinks = (Map) entry.getValue();
      LOGGER.debug("metricTimeRangeLinks = {}", metricTimeRangeLinks);

      // Verify each metric name, e.g., "uptime", has all of the time ranges represented
      assertThat(metricTimeRangeLinks.containsKey("15m"), is(true));
      assertThat(metricTimeRangeLinks.containsKey("1h"), is(true));
      // assertThat(metricTimeRangeLinks.containsKey("4h"), is(true));
      // assertThat(metricTimeRangeLinks.containsKey("12h"), is(true));
      assertThat(metricTimeRangeLinks.containsKey("1d"), is(true));
      // assertThat(metricTimeRangeLinks.containsKey("3d"), is(true));
      assertThat(metricTimeRangeLinks.containsKey("1w"), is(true));
      assertThat(metricTimeRangeLinks.containsKey("1M"), is(true));
      assertThat(metricTimeRangeLinks.containsKey("3M"), is(true));
      assertThat(metricTimeRangeLinks.containsKey("6M"), is(true));
      assertThat(metricTimeRangeLinks.containsKey("1y"), is(true));

      Iterator timeRangeLinksIter = metricTimeRangeLinks.entrySet().iterator();

      // Verify for each time range that the associated hyperlinks are present for all
      // supported formats and that the correct dateOffset is specified in the hyperlinks
      while (timeRangeLinksIter.hasNext()) {
        Map.Entry timeRangeLinkEntry = (Map.Entry) timeRangeLinksIter.next();
        String timeRange = (String) timeRangeLinkEntry.getKey();
        Map<String, String> metricHyperlinks = (Map<String, String>) timeRangeLinkEntry.getValue();
        Long dateOffset = MetricsEndpoint.TIME_RANGES.get(timeRange);
        assertThat(metricHyperlinks.containsKey("PNG"), is(true));
        assertThat(metricHyperlinks.get("PNG"), endsWith("dateOffset=" + dateOffset));
        assertThat(metricHyperlinks.containsKey("CSV"), is(true));
        assertThat(metricHyperlinks.get("CSV"), endsWith("dateOffset=" + dateOffset));
        assertThat(metricHyperlinks.containsKey("XLS"), is(true));
        assertThat(metricHyperlinks.get("XLS"), endsWith("dateOffset=" + dateOffset));
      }
    }
  }

  @Test
  public void testGetMetricsGraphPositiveCase() throws Exception {
    // Create RRD file that Metrics Endpoint will detect
    int dateOffset = 900; // 15 minutes in seconds
    createRrdFile(dateOffset);

    UriInfo uriInfo = createUriInfo();

    // Get the metrics graph from the endpoint
    MetricsEndpoint endpoint = getEndpoint();
    endpoint.setMetricsDir(TEST_DIR);
    Response response =
        endpoint.getMetricsData(
            "uptime",
            "png",
            "2013-03-25T06:00:00-07:00",
            "2013-03-25T07:10:00-07:00",
            null,
            "my label",
            "my title",
            uriInfo);

    cleanupRrd();

    assertThat(response.getEntity(), not(nullValue()));

    // NOTE: There are other branches in MetricsEndpoint that could/should be tested, e.g.,
    // when startDate specified and no endDate is specified (which is valid) - unfortunately,
    // RRD provides no methods to get info on the graph other than the byte[] it returns.
    // Hence, only could verify that a non null graph is returned. These tests seem not
    // to be worth writing at this time.
  }

  @Test
  public void testGetMetricsGraphPositiveCaseWithDefaults() throws Exception {
    // Create RRD file that Metrics Endpoint will detect
    int dateOffset = 900; // 15 minutes in seconds
    createRrdFile(dateOffset);

    UriInfo uriInfo = createUriInfo();

    // Get the metrics graph from the endpoint
    MetricsEndpoint endpoint = getEndpoint();
    endpoint.setMetricsDir(TEST_DIR);
    Response response =
        endpoint.getMetricsData("uptime", "png", null, null, null, null, null, uriInfo);

    cleanupRrd();

    assertThat(response.getEntity(), not(nullValue()));
  }

  // NOTE: "expected" annotation does not work when test case extends XMLTestCase,
  // hence the usage of the try/catch/fail approach for the expected exception
  @Test
  // (expected = MetricsEndpointException.class)
  public void testGetMetricsGraphWithDateOffsetAndDates() throws Exception {
    UriInfo uriInfo = createUriInfo();

    MetricsEndpoint endpoint = getEndpoint();
    endpoint.setMetricsDir(TEST_DIR);
    try {
      endpoint.getMetricsData(
          "uptime",
          "png",
          "2013-03-25T06:00:00-07:00",
          "2013-03-25T07:10:00-07:00",
          "3600",
          "my label",
          "my title",
          uriInfo);
      fail();
    } catch (MetricsEndpointException e) {
    }
  }

  // NOTE: "expected" annotation does not work when test case extends XMLTestCase,
  // hence the usage of the try/catch/fail approach for the expected exception
  @Test
  // (expected = MetricsEndpointException.class)
  public void testGetMetricsGraphWithIOException() throws Exception {
    UriInfo uriInfo = createUriInfo();

    RrdMetricsRetriever metricsRetriever = mock(RrdMetricsRetriever.class);
    when(metricsRetriever.createGraph(
            anyString(), anyString(), anyLong(), anyLong(), anyString(), anyString()))
        .thenThrow(IOException.class);

    MetricsEndpoint endpoint = getEndpoint();
    endpoint.setMetricsDir(TEST_DIR);
    endpoint.setMetricsRetriever(metricsRetriever);

    try {
      endpoint.getMetricsData(
          "uptime",
          "png",
          "2013-03-25T06:00:00-07:00",
          "2013-03-25T07:10:00-07:00",
          null,
          "my label",
          "my title",
          uriInfo);
      fail();
    } catch (MetricsEndpointException e) {
    }
  }

  // NOTE: "expected" annotation does not work when test case extends XMLTestCase,
  // hence the usage of the try/catch/fail approach for the expected exception
  @Test
  // (expected = MetricsEndpointException.class)
  public void testGetMetricsGraphWithMetricsGraphException() throws Exception {
    UriInfo uriInfo = createUriInfo();

    RrdMetricsRetriever metricsRetriever = mock(RrdMetricsRetriever.class);
    when(metricsRetriever.createGraph(
            anyString(), anyString(), anyLong(), anyLong(), anyString(), anyString()))
        .thenThrow(MetricsGraphException.class);

    MetricsEndpoint endpoint = getEndpoint();
    endpoint.setMetricsDir(TEST_DIR);
    endpoint.setMetricsRetriever(metricsRetriever);

    try {
      endpoint.getMetricsData(
          "uptime",
          "png",
          "2013-03-25T06:00:00-07:00",
          "2013-03-25T07:10:00-07:00",
          null,
          "my label",
          "my title",
          uriInfo);
      fail();
    } catch (MetricsEndpointException e) {
    }
  }

  @Test
  public void testGetMetricsDataAsXml() throws Exception {
    // Create RRD file that Metrics Endpoint will detect
    int dateOffset = 900; // 15 minutes in seconds
    createRrdFile(dateOffset);

    UriInfo uriInfo = createUriInfo();

    // Get the metrics data from the endpoint
    MetricsEndpoint endpoint = getEndpoint();
    endpoint.setMetricsDir(TEST_DIR);

    Response response =
        endpoint.getMetricsData(
            "uptime",
            "xml",
            null,
            null,
            Integer.toString(dateOffset),
            "my label",
            "my title",
            uriInfo);

    cleanupRrd();

    String xml = (String) response.getEntity();
    LOGGER.debug("xml = {}", xml);

    // Requires XmlUnit, but when this class extends XMLTestCase causes test case
    // testGetMetricsGraphWithDateOffsetAndDates to fail (exception is thrown but
    // unit test does not see it)
    assertXpathExists("/uptime", xml);
    assertXpathExists("/uptime/title", xml);
    assertXpathExists("/uptime/data", xml);
    assertXpathExists("/uptime/data/sample", xml);
    assertXpathExists("/uptime/data/sample/timestamp", xml);
    assertXpathExists("/uptime/data/sample/value", xml);
    assertXpathExists("/uptime/data/totalCount", xml);
  }

  @Test
  public void testGetMetricsDataAsCsv() throws Exception {
    // Create RRD file that Metrics Endpoint will detect
    int dateOffset = 900; // 15 minutes in seconds
    createRrdFile(dateOffset);

    UriInfo uriInfo = createUriInfo();

    // Get the metrics data from the endpoint
    MetricsEndpoint endpoint = getEndpoint();
    endpoint.setMetricsDir(TEST_DIR);

    Response response =
        endpoint.getMetricsData(
            "uptime",
            "csv",
            null,
            null,
            Integer.toString(dateOffset),
            "my label",
            "my title",
            uriInfo);

    cleanupRrd();

    String csv = (String) response.getEntity();
    LOGGER.debug("csv = {}", csv);

    // Break up CSV data into its individual lines
    // Each line should have 2 parts (cells)
    // The first line should be the column headers
    String[] csvLines = csv.split("\n");

    // Verify that number of lines should be the column headers + (number of samples - 1)
    // Since 3 samples were taken, but only 2 are added to the RRD file due to averaging,
    // expect 2 lines of data and the one line of column headers.
    assertThat(csvLines.length, equalTo(3));
    assertThat(csvLines[0], equalTo("Timestamp,Value")); // column headers
    String[] cells = csvLines[1].split(",");
    assertThat(cells.length, equalTo(2)); // each line of data has the 2 values
  }

  @Test
  public void testGetMetricsDataAsXls() throws Exception {
    // Create RRD file that Metrics Endpoint will detect
    int dateOffset = 900; // 15 minutes in seconds
    createRrdFile(dateOffset);

    UriInfo uriInfo = createUriInfo();

    // Get the metrics data from the endpoint
    MetricsEndpoint endpoint = getEndpoint();
    endpoint.setMetricsDir(TEST_DIR);

    Response response =
        endpoint.getMetricsData(
            "uptime",
            "xls",
            null,
            null,
            Integer.toString(dateOffset),
            "my label",
            "my title",
            uriInfo);

    cleanupRrd();

    InputStream xls = (InputStream) response.getEntity();
    assertThat(xls, not(nullValue()));

    HSSFWorkbook wb = new HSSFWorkbook(xls);
    assertThat(wb.getNumberOfSheets(), equalTo(1));
    HSSFSheet sheet = wb.getSheet("Uptime");
    assertThat(sheet, not(nullValue()));

    // Expect 7 rows: title + blank + column headers + 2 rows of samples + blank +
    // totalQueryCount
    assertThat(sheet.getPhysicalNumberOfRows(), equalTo(7));

    // first row should have title in first cell
    HSSFRow row = sheet.getRow(0);
    assertThat(row, not(nullValue()));
    assertThat(row.getCell(0).getStringCellValue(), startsWith("Uptime for"));

    // third row should have column headers in first and second cells
    row = sheet.getRow(2);
    assertThat(row.getCell(0).getStringCellValue(), equalTo("Timestamp"));
    assertThat(row.getCell(1).getStringCellValue(), equalTo("Value"));

    // should have 2 rows of samples' data
    row = sheet.getRow(3);
    assertThat(row.getCell(0).getStringCellValue(), not(nullValue()));
    assertThat(row.getCell(1).getNumericCellValue(), not(nullValue()));
    row = sheet.getRow(4);
    assertThat(row.getCell(0).getStringCellValue(), not(nullValue()));
    assertThat(row.getCell(1).getNumericCellValue(), not(nullValue()));

    // last row should have totalQueryCount in first cell
    row = sheet.getRow(sheet.getLastRowNum());
    assertThat(row.getCell(0).getStringCellValue(), startsWith("Total Count:"));
    assertThat(row.getCell(1).getNumericCellValue(), not(nullValue()));
  }

  @Test
  public void testGetMetricsDataAsJson() throws Exception {
    // Create RRD file that Metrics Endpoint will detect
    int dateOffset = 900; // 15 minutes in seconds
    createRrdFile(dateOffset);

    UriInfo uriInfo = createUriInfo();

    // Get the metrics data from the endpoint
    MetricsEndpoint endpoint = getEndpoint();
    endpoint.setMetricsDir(TEST_DIR);

    Response response =
        endpoint.getMetricsData(
            "uptime",
            "json",
            null,
            null,
            Integer.toString(dateOffset),
            "my label",
            "my title",
            uriInfo);

    cleanupRrd();

    String json = (String) response.getEntity();
    LOGGER.debug("json = {}", json);

    JSONParser parser = new JSONParser();
    JSONObject jsonObj = (JSONObject) parser.parse(json);

    // Verify the title, totalCount, and data (i.e., samples) are present
    assertThat(jsonObj.size(), equalTo(3));
    assertThat(jsonObj.get("title"), not(nullValue()));
    assertThat(jsonObj.get("totalCount"), not(nullValue()));

    // Verify 2 samples were retrieved from the RRD file and put in the JSON fetch results
    JSONArray samples = (JSONArray) jsonObj.get("data");
    assertThat(samples.size(), equalTo(2));

    // Verify each retrieved sample has a timestamp and value
    for (int i = 0; i < samples.size(); i++) {
      JSONObject sample = (JSONObject) samples.get(i);
      LOGGER.debug(
          "timestamp = {},   value = {}", (String) sample.get("timestamp"), sample.get("value"));
      assertThat(sample.get("timestamp"), not(nullValue()));
      assertThat(sample.get("value"), not(nullValue()));
    }
  }

  @Test
  public void testGetMetricsReportAsXls() throws Exception {
    // Create RRD file that Metrics Endpoint will detect
    int dateOffset = 900; // 15 minutes in seconds
    createRrdFile(dateOffset, "uptime");

    UriInfo uriInfo = createUriInfo();

    // Get the metrics data from the endpoint
    MetricsEndpoint endpoint = getEndpoint();
    endpoint.setMetricsDir(TEST_DIR);

    Response response =
        endpoint.getMetricsReport(
            "xls", null, null, Integer.toString(dateOffset), "minute", uriInfo);

    cleanupRrd();

    MultivaluedMap<String, Object> headers = response.getHeaders();
    assertTrue(
        headers.getFirst("Content-Disposition").toString().contains("attachment; filename="));

    InputStream is = (InputStream) response.getEntity();
    assertThat(is, not(nullValue()));

    HSSFWorkbook wb = new HSSFWorkbook(is);
    assertThat(wb.getNumberOfSheets(), equalTo(1));
    HSSFSheet sheet = wb.getSheetAt(0);
    assertThat(sheet, not(nullValue()));
  }

  // NOTE: "expected" annotation does not work when test case extends XMLTestCase,
  // hence the usage of the try/catch/fail approach for the expected exception
  @Test
  // (expected=MetricsEndpointException.class)
  public void testGetMetricsDataAsJsonWithIOException() throws Exception {
    verifyException("json", IOException.class);
  }

  // NOTE: "expected" annotation does not work when test case extends XMLTestCase,
  // hence the usage of the try/catch/fail approach for the expected exception
  @Test
  // (expected=MetricsEndpointException.class)
  public void testGetMetricsDataAsJsonWithMetricsGraphException() throws Exception {
    verifyException("json", MetricsGraphException.class);
  }

  // NOTE: "expected" annotation does not work when test case extends XMLTestCase,
  // hence the usage of the try/catch/fail approach for the expected exception
  @Test
  // (expected=MetricsEndpointException.class)
  public void testGetMetricsDataAsCsvWithIOException() throws Exception {
    verifyException("csv", IOException.class);
  }

  // NOTE: "expected" annotation does not work when test case extends XMLTestCase,
  // hence the usage of the try/catch/fail approach for the expected exception
  @Test
  // (expected=MetricsEndpointException.class)
  public void testGetMetricsDataAsCsvWithMetricsGraphException() throws Exception {
    verifyException("csv", MetricsGraphException.class);
  }

  // NOTE: "expected" annotation does not work when test case extends XMLTestCase,
  // hence the usage of the try/catch/fail approach for the expected exception
  @Test
  // (expected=MetricsEndpointException.class)
  public void testGetMetricsDataAsXmlWithIOException() throws Exception {
    verifyException("xml", IOException.class);
  }

  // NOTE: "expected" annotation does not work when test case extends XMLTestCase,
  // hence the usage of the try/catch/fail approach for the expected exception
  @Test
  // (expected=MetricsEndpointException.class)
  public void testGetMetricsDataAsXmlWithMetricsGraphException() throws Exception {
    verifyException("xml", MetricsGraphException.class);
  }

  // NOTE: "expected" annotation does not work when test case extends XMLTestCase,
  // hence the usage of the try/catch/fail approach for the expected exception
  @Test
  // (expected=MetricsEndpointException.class)
  public void testGetMetricsDataAsXlsWithIOException() throws Exception {
    verifyException("xls", IOException.class);
  }

  // NOTE: "expected" annotation does not work when test case extends XMLTestCase,
  // hence the usage of the try/catch/fail approach for the expected exception
  @Test
  // (expected=MetricsEndpointException.class)
  public void testGetMetricsDataAsXlsWithMetricsGraphException() throws Exception {
    verifyException("xls", MetricsGraphException.class);
  }

  /** **************************************************************************** */
  private void createRrdFile(int dateOffset) throws Exception {
    createRrdFile(dateOffset, "uptime");
  }

  private void createRrdFile(int dateOffset, String metricName) throws Exception {
    // Create RRD file that Metrics Endpoint will detect
    rrdPath = TEST_DIR + metricName + ".rrd";
    int rrdStep = 60;
    RrdDef def = new RrdDef(rrdPath, rrdStep);
    long startTime = System.currentTimeMillis() / 1000 - dateOffset;
    def.setStartTime(startTime - rrdStep);
    def.addDatasource("data", DsType.COUNTER, 90, 0, Double.NaN);
    def.addArchive(ConsolFun.TOTAL, 0.5, 1, 5);
    rrdDb = RrdDbPool.getInstance().requestRrdDb(def);

    // Add enough samples to get one averaged sample stored into the RRD file
    long endTime = startTime;
    Sample sample = rrdDb.createSample();
    sample.setTime(endTime);
    sample.setValue("data", 100);
    sample.update();
    endTime += rrdStep;
    sample.setTime(endTime);
    sample.setValue("data", 200);
    sample.update();
    endTime += rrdStep;
    sample.setTime(endTime);
    sample.setValue("data", 100);
    sample.update();
    endTime += rrdStep;

    LOGGER.debug(rrdDb.dump());

    FetchRequest fetchRequest = rrdDb.createFetchRequest(ConsolFun.TOTAL, startTime, endTime);
    FetchData fetchData = fetchRequest.fetchData();

    LOGGER.debug(fetchData.dump());
    long[] timestamps = fetchData.getTimestamps();
    double[] values = fetchData.getValues(0);
    for (int i = 0; i < timestamps.length; i++) {
      LOGGER.debug("{}:  {}", getCalendarTime(timestamps[i]), values[i]);
    }

    rrdDb.close();
  }

  protected UriInfo createUriInfo() throws URISyntaxException {
    UriInfo info = mock(UriInfo.class);
    when(info.getBaseUri()).thenReturn(new URI(ENDPOINT_ADDRESS));
    when(info.getRequestUri()).thenReturn(new URI(ENDPOINT_ADDRESS));
    when(info.getAbsolutePath()).thenReturn(new URI(ENDPOINT_ADDRESS));

    UriBuilder builder = mock(UriBuilder.class);

    when(info.getAbsolutePathBuilder()).thenReturn(builder);

    when(builder.path("/uptime.png")).thenReturn(builder);

    when(builder.build()).thenReturn(new URI(ENDPOINT_ADDRESS + "/uptime.png"));

    return info;
  }

  private void verifyException(String format, Class exceptionClass) throws Exception {
    UriInfo uriInfo = createUriInfo();

    RrdMetricsRetriever metricsRetriever = mock(RrdMetricsRetriever.class);
    when(metricsRetriever.createJsonData(anyString(), anyString(), anyLong(), anyLong()))
        .thenThrow(exceptionClass);
    when(metricsRetriever.createCsvData(anyString(), anyLong(), anyLong()))
        .thenThrow(exceptionClass);
    when(metricsRetriever.createXlsData(anyString(), anyString(), anyLong(), anyLong()))
        .thenThrow(exceptionClass);
    when(metricsRetriever.createXmlData(anyString(), anyString(), anyLong(), anyLong()))
        .thenThrow(exceptionClass);

    // Get the metrics data from the endpoint
    MetricsEndpoint endpoint = getEndpoint();
    endpoint.setMetricsDir(TEST_DIR);
    endpoint.setMetricsRetriever(metricsRetriever);

    try {
      endpoint.getMetricsData("uptime", format, null, null, "900", "my label", "my title", uriInfo);
      fail();
    } catch (MetricsEndpointException e) {
    }
  }

  private void cleanupRrd() throws IOException {
    if (!rrdDb.isClosed()) {
      rrdDb.close();
    }
    RrdDbPool.getInstance().release(rrdDb);

    File rrdFile = new File(rrdPath);
    if (rrdFile.exists()) {
      boolean status = rrdFile.delete();
      if (status) {
        LOGGER.debug("Successfully deleted rrdFile {}", rrdPath);
      } else {
        LOGGER.debug("Unable to delete rrdFile {}", rrdPath);
      }
    } else {
      LOGGER.debug("rrdFile {} does not exist - cannot delete", rrdPath);
    }
  }

  private String getCalendarTime(long timestamp) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(timestamp * 1000);

    String calTime =
        MONTHS[calendar.get(Calendar.MONTH)]
            + " "
            + calendar.get(Calendar.DATE)
            + " "
            + calendar.get(Calendar.YEAR)
            + " ";

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

  private MetricsEndpoint getEndpoint() {
    MetricsEndpoint me = new MetricsEndpoint();
    System.setProperty(SystemBaseUrl.INTERNAL_ROOT_CONTEXT, "/services");
    System.setProperty(SystemInfo.SITE_NAME, "siteName");
    return me;
  }
}
