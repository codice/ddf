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
package ddf.metrics.reporting.internal.rrd4j;

import ddf.metrics.reporting.internal.MetricsGraphException;
import ddf.metrics.reporting.internal.MetricsRetriever;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.codice.ddf.platform.util.XMLUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.Datasource;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdDb;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Retrieves metrics historical data from an RRD file and formats that data in a variety of formats
 * over a specified time range.
 *
 * <p>The supported formats include:
 *
 * <ul>
 *   <li>a PNG graph of the data (returned to the client as a byte array)
 *   <li>a CSV file which can be displayed in Microsoft Excel, OpenOffice Calc, etc.
 *   <li>a stream in XLS-format, which can be displayed in Microsoft Excel or OpenOffice Calc
 *   <li>a stream in PPT-format, which can be displayed in Microsoft PowerPoint or OpenOffice
 *       Impress
 *   <li>as XML (no schema provided)
 *   <li>a JSON-formatted string
 * </ul>
 *
 * <p>Aggregate reports, which include the data for all metrics, over a specified time range are
 * also supported in XLS (Excel) and PPT (PowerPoint) format. For example, if there are 10 metrics
 * that are having data collected, then an aggregate report in XLS would be a spreadsheet with a
 * separate worksheet for each of the 10 metrics. Similarly, this aggregate report in PPT format
 * would consist of a slide per metric, where each slide contains the metric's graph and total count
 * (if applicable).
 *
 * @author rodgersh
 */
public class RrdMetricsRetriever implements MetricsRetriever {

  private static final transient Logger LOGGER = LoggerFactory.getLogger(RrdMetricsRetriever.class);

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  private static final double DEFAULT_METRICS_MAX_THRESHOLD = 4000000000.0;

  private static final int RRD_STEP = 60;

  public static final String SUMMARY_TIMESTAMP = "dd-MM-yy HHmm";

  public static final int EXCEL_MAX_COLUMNS = 256;

  /** Used for formatting long timestamps into more readable calendar dates/times. */
  private static final String MONTHS[] = {
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
  };

  /**
   * Max threshold for a metric's sample value. Used to filter out spike data that typically has a
   * value of 4.2E+09 or higher.
   */
  private double metricsMaxThreshold;

  public enum SUMMARY_INTERVALS {
    minute,
    hour,
    day,
    week,
    month;
  }

  public RrdMetricsRetriever() {
    this(DEFAULT_METRICS_MAX_THRESHOLD);
  }

  public RrdMetricsRetriever(double metricsMaxThreshold) {
    LOGGER.trace("Setting metricsMaxThreshold = {}", metricsMaxThreshold);
    this.metricsMaxThreshold = metricsMaxThreshold;
  }

  /**
   * Formats timestamp (in seconds since Unix epoch) into human-readable format of MMM DD YYYY
   * hh:mm:ss.
   *
   * <p>Example: Apr 10 2013 09:14:43
   *
   * @param timestamp time in seconds since Unix epoch of Jan 1, 1970 12:00:00
   * @return formatted date/time string of the form MMM DD YYYY hh:mm:ss
   */
  static String getCalendarTime(long timestamp) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(TimeUnit.SECONDS.toMillis(timestamp));

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

  static String addLeadingZero(int value) {
    if (value < 10) {
      return "0" + String.valueOf(value);
    }

    return String.valueOf(value);
  }

  /**
   * Convert string, if it is in camelCase, to individual words with each word starting with a
   * capital letter
   */
  public static String convertCamelCase(String input) {
    String[] parts = StringUtils.splitByCharacterTypeCamelCase(input);
    String convertedStr = StringUtils.join(parts, " ");
    convertedStr = WordUtils.capitalizeFully(convertedStr).trim();

    return convertedStr;
  }

  @Override
  public byte[] createGraph(String metricName, String rrdFilename, long startTime, long endTime)
      throws IOException, MetricsGraphException {
    // Create default label and title for graph
    String displayableMetricName = convertCamelCase(metricName);
    String verticalAxisLabel = displayableMetricName;
    String title =
        displayableMetricName
            + " for "
            + getCalendarTime(startTime)
            + " to "
            + getCalendarTime(endTime);

    return createGraph(metricName, rrdFilename, startTime, endTime, verticalAxisLabel, title);
  }

  @Override
  public byte[] createGraph(
      String metricName,
      String rrdFilename,
      long startTime,
      long endTime,
      String verticalAxisLabel,
      String title)
      throws IOException, MetricsGraphException {
    // Create RRD DB in read-only mode for the specified RRD file
    try (RrdDb rrdDb = new RrdDb(rrdFilename, true)) {

      // Extract the data source (should always only be one data source per RRD file - otherwise
      // we have a problem)
      if (rrdDb.getDsCount() != 1) {
        throw new MetricsGraphException(
            "Only one data source per RRD file is supported - RRD file "
                + rrdFilename
                + " has "
                + rrdDb.getDsCount()
                + " data sources.");
      }

      // Define attributes of the graph to be created for this metric
      RrdGraphDef graphDef = new RrdGraphDef();
      graphDef.setTimeSpan(startTime, endTime);
      graphDef.setImageFormat("PNG");
      graphDef.setShowSignature(false);
      graphDef.setStep(RRD_STEP);
      graphDef.setVerticalLabel(verticalAxisLabel);
      graphDef.setHeight(500);
      graphDef.setWidth(1000);
      graphDef.setTitle(title);

      // Since we have verified only one datasource in RRD file/RRDb, then know
      // that we can index by zero safely and get the metric's data
      Datasource dataSource = rrdDb.getDatasource(0);
      DsType dataSourceType = dataSource.getType();

      // Determine the type of Data Source for this RRD file
      // (Need to know this because COUNTER and DERIVE data is averaged across samples and the
      // vertical axis of the
      // generated graph by default will show data per rrdStep interval)
      if (dataSourceType == DsType.COUNTER || dataSourceType == DsType.DERIVE) {
        if (LOGGER.isTraceEnabled()) {
          dumpData(ConsolFun.TOTAL, "TOTAL", rrdDb, dataSourceType.name(), startTime, endTime);
        }

        // If we ever needed to adjust the metric's data collected by RRD by the archive step
        // (which is the rrdStep * archiveSampleCount) this is how to do it.
        // FetchRequest fetchRequest = rrdDb.createFetchRequest(ConsolFun.AVERAGE, startTime,
        // endTime);
        // Archive archive = rrdDb.findMatchingArchive(fetchRequest);
        // long archiveStep = archive.getArcStep();
        // LOGGER.debug("archiveStep = " + archiveStep);
        long rrdStep = rrdDb.getRrdDef().getStep();
        LOGGER.debug("rrdStep = {}", rrdStep);

        // Still TBD if we want to graph the AVERAGE data on the same graph
        // graphDef.comment(metricName + "   ");
        // graphDef.datasource("myAverage", rrdFilename, "data", ConsolFun.AVERAGE);
        // graphDef.datasource("realAverage", "myAverage," + rrdStep + ",*");
        // graphDef.line("realAverage", Color.GREEN, "Average", 2);

        // Multiplied by the rrdStep to "undo" the automatic averaging that RRD does
        // when it collects TOTAL data - we want the actual totals for the step, not
        // the average of the totals.
        graphDef.datasource("myTotal", rrdFilename, dataSource.getName(), ConsolFun.TOTAL);
        graphDef.datasource("realTotal", "myTotal," + rrdStep + ",*");

        // If real total exceeds the threshold value used to constrain/filter spike data out,
        // then set total to UNKNOWN, which means this sample will not be graphed. This prevents
        // spike data that is typically 4.2E+09 (graphed as 4.3G) from corrupting the RRD graph.
        graphDef.datasource(
            "constrainedTotal", "realTotal," + metricsMaxThreshold + ",GT,UNKN,realTotal,IF");
        graphDef.line("constrainedTotal", Color.BLUE, convertCamelCase(metricName), 2);

        // Add some spacing between the graph and the summary stats shown beneath the graph
        graphDef.comment("\\s");
        graphDef.comment("\\s");
        graphDef.comment("\\c");

        // Average, Min, and Max over all of the TOTAL data - displayed at bottom of the graph
        graphDef.gprint("constrainedTotal", ConsolFun.AVERAGE, "Average = %.3f%s");
        graphDef.gprint("constrainedTotal", ConsolFun.MIN, "Min = %.3f%s");
        graphDef.gprint("constrainedTotal", ConsolFun.MAX, "Max = %.3f%s");
      } else if (dataSourceType == DsType.GAUGE) {
        if (LOGGER.isTraceEnabled()) {
          dumpData(ConsolFun.AVERAGE, "AVERAGE", rrdDb, dataSourceType.name(), startTime, endTime);
        }

        graphDef.datasource("myAverage", rrdFilename, dataSource.getName(), ConsolFun.AVERAGE);
        graphDef.line("myAverage", Color.RED, convertCamelCase(metricName), 2);

        // Add some spacing between the graph and the summary stats shown beneath the graph
        graphDef.comment("\\s");
        graphDef.comment("\\s");
        graphDef.comment("\\c");

        // Average, Min, and Max over all of the AVERAGE data - displayed at bottom of the graph
        graphDef.gprint("myAverage", ConsolFun.AVERAGE, "Average = %.3f%s");
        graphDef.gprint("myAverage", ConsolFun.MIN, "Min = %.3f%s");
        graphDef.gprint("myAverage", ConsolFun.MAX, "Max = %.3f%s");
      } else {
        rrdDb.close();
        throw new MetricsGraphException(
            "Unsupported data source type "
                + dataSourceType.name()
                + " in RRD file "
                + rrdFilename
                + ", only DERIVE, COUNTER and GAUGE data source types supported.");
      }

      rrdDb.close();

      // Use "-" as filename so that RRD creates the graph only in memory (no file is
      // created, hence no file locking problems due to race conditions between multiple clients)
      graphDef.setFilename("-");
      RrdGraph graph = new RrdGraph(graphDef);

      return graph.getRrdGraphInfo().getBytes();
    }
  }

  @Override
  public String createCsvData(String rrdFilename, long startTime, long endTime)
      throws IOException, MetricsGraphException {
    LOGGER.trace("ENTERING: createCsvData");

    MetricData metricData = getMetricData(rrdFilename, startTime, endTime);

    StringBuilder csv = new StringBuilder();

    csv.append("Timestamp,Value\n");

    List<Long> timestamps = metricData.getTimestamps();
    List<Double> values = metricData.getValues();

    for (int i = 0; i < timestamps.size(); i++) {
      String timestamp = getCalendarTime(timestamps.get(i));
      csv.append(timestamp + "," + values.get(i) + "\n");
    }

    LOGGER.trace("csv = {}", csv);

    LOGGER.trace("EXITING: createCsvData");

    return csv.toString();
  }

  @Override
  public String createXmlData(String metricName, String rrdFilename, long startTime, long endTime)
      throws IOException, MetricsGraphException {
    LOGGER.trace("ENTERING: createXmlData");

    MetricData metricData = getMetricData(rrdFilename, startTime, endTime);

    String displayableMetricName = convertCamelCase(metricName);

    String title =
        displayableMetricName
            + " for "
            + getCalendarTime(startTime)
            + " to "
            + getCalendarTime(endTime);

    String xmlString = null;

    try {
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

      // root elements
      Document doc = docBuilder.newDocument();
      Element rootElement = doc.createElement(metricName);
      doc.appendChild(rootElement);

      Element titleElement = doc.createElement("title");
      titleElement.appendChild(doc.createTextNode(title));
      rootElement.appendChild(titleElement);

      Element dataElement = doc.createElement("data");
      rootElement.appendChild(dataElement);

      List<Long> timestamps = metricData.getTimestamps();
      List<Double> values = metricData.getValues();

      for (int i = 0; i < timestamps.size(); i++) {
        Element sampleElement = doc.createElement("sample");
        dataElement.appendChild(sampleElement);

        String timestamp = getCalendarTime(timestamps.get(i));
        Element timestampElement = doc.createElement("timestamp");
        timestampElement.appendChild(doc.createTextNode(timestamp));
        sampleElement.appendChild(timestampElement);

        Element valueElement = doc.createElement("value");
        valueElement.appendChild(doc.createTextNode(String.valueOf(values.get(i))));
        sampleElement.appendChild(valueElement);
      }

      if (metricData.hasTotalCount()) {
        Element totalCountElement = doc.createElement("totalCount");
        totalCountElement.appendChild(
            doc.createTextNode(Long.toString(metricData.getTotalCount())));
        dataElement.appendChild(totalCountElement);
      }

      // Write the content into xml stringwriter
      xmlString = XML_UTILS.prettyFormat(doc);
    } catch (ParserConfigurationException pce) {
      LOGGER.debug("Parsing error while creating xml data", pce);
    }

    LOGGER.trace("xml = {}", xmlString);

    LOGGER.trace("EXITING: createXmlData");

    return xmlString;
  }

  @Override
  public OutputStream createXlsData(
      String metricName, String rrdFilename, long startTime, long endTime)
      throws IOException, MetricsGraphException {
    LOGGER.trace("ENTERING: createXlsData");

    Workbook wb = new HSSFWorkbook();
    createSheet(wb, metricName, rrdFilename, startTime, endTime);

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    wb.write(bos);
    bos.close();

    LOGGER.trace("EXITING: createXlsData");

    return bos;
  }

  @Override
  public String createJsonData(String metricName, String rrdFilename, long startTime, long endTime)
      throws IOException, MetricsGraphException {
    LOGGER.trace("ENTERING: createJsonData");

    JSONObject obj = new JSONObject();

    String displayableMetricName = convertCamelCase(metricName);

    MetricData metricData = getMetricData(rrdFilename, startTime, endTime);

    String title =
        displayableMetricName
            + " for "
            + getCalendarTime(startTime)
            + " to "
            + getCalendarTime(endTime);
    obj.put("title", title);

    List<Long> timestamps = metricData.getTimestamps();
    List<Double> values = metricData.getValues();
    JSONArray samples = new JSONArray();

    for (int i = 0; i < timestamps.size(); i++) {
      String timestamp = getCalendarTime(timestamps.get(i));
      JSONObject sample = new JSONObject();
      sample.put("timestamp", timestamp);
      sample.put("value", values.get(i));
      samples.add(sample);
    }
    obj.put("data", samples);

    if (metricData.hasTotalCount()) {
      obj.put("totalCount", metricData.getTotalCount());
    }

    JsonWriter writer = new JsonWriter();
    obj.writeJSONString(writer);
    String jsonText = writer.toString();

    LOGGER.trace("jsonText = {}", jsonText);

    LOGGER.trace("EXITING: createJsonData");

    return jsonText;
  }

  @Override
  public OutputStream createXlsReport(
      List<String> metricNames,
      String metricsDir,
      long startTime,
      long endTime,
      String summaryInterval)
      throws IOException, MetricsGraphException {
    LOGGER.trace("ENTERING: createXlsReport");

    try (Workbook wb = new HSSFWorkbook()) {
      Collections.sort(metricNames);

      if (StringUtils.isNotEmpty(summaryInterval)) {
        createSummary(
            wb,
            metricNames,
            metricsDir,
            startTime,
            endTime,
            SUMMARY_INTERVALS.valueOf(summaryInterval));
      } else {
        for (int i = 0; i < metricNames.size(); i++) {
          String metricName = metricNames.get(i);
          String rrdFilename = getRrdFilename(metricsDir, metricName);
          String displayName = i + metricName;

          createSheet(wb, displayName, rrdFilename, startTime, endTime);
        }
      }

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      wb.write(bos);
      bos.close();

      LOGGER.trace("EXITING: createXlsReport");

      return bos;
    }
  }

  private void createSummary(
      Workbook wb,
      List<String> metricNames,
      String metricsDir,
      long startTime,
      long endTime,
      SUMMARY_INTERVALS summaryInterval)
      throws IOException, MetricsGraphException {
    // convert seconds to milliseconds
    startTime = TimeUnit.SECONDS.toMillis(startTime);
    endTime = TimeUnit.SECONDS.toMillis(endTime);
    DateTime reportStart = new DateTime(startTime, DateTimeZone.UTC);
    DateTime reportEnd = new DateTime(endTime, DateTimeZone.UTC);

    Sheet sheet = wb.createSheet();
    wb.setSheetName(
        0,
        reportStart.toString(SUMMARY_TIMESTAMP) + " to " + reportEnd.toString(SUMMARY_TIMESTAMP));
    Row headingRow = sheet.createRow(0);

    int columnMax = 1;
    for (String metricName : metricNames) {
      MutableDateTime chunkStart = new MutableDateTime(reportStart);
      MutableDateTime chunkEnd = new MutableDateTime(chunkStart);
      Row row = sheet.createRow(metricNames.indexOf(metricName) + 1);
      int columnCounter = 1;
      Boolean isSum = null;

      while (reportEnd.compareTo(chunkEnd) > 0 && columnCounter < EXCEL_MAX_COLUMNS) {
        increment(chunkEnd, summaryInterval);
        if (chunkEnd.isAfter(reportEnd)) {
          chunkEnd.setMillis(reportEnd);
        }

        // offset range by one millisecond so rrd will calculate granularity correctly
        chunkEnd.addMillis(-1);
        MetricData metricData =
            getMetricData(
                getRrdFilename(metricsDir, metricName),
                TimeUnit.MILLISECONDS.toSeconds(chunkStart.getMillis()),
                TimeUnit.MILLISECONDS.toSeconds(chunkEnd.getMillis()));
        isSum = metricData.hasTotalCount();
        chunkEnd.addMillis(1);

        if (headingRow.getCell(columnCounter) == null) {
          Cell headingRowCell = headingRow.createCell(columnCounter);
          headingRowCell.getCellStyle().setWrapText(true);
          headingRowCell.setCellValue(
              getTimestamp(chunkStart, chunkEnd, columnCounter, summaryInterval));
        }

        Cell sumOrAvg = row.createCell(columnCounter);
        if (isSum) {
          sumOrAvg.setCellValue((double) metricData.getTotalCount());
        } else {
          sumOrAvg.setCellValue(cumulativeRunningAverage(metricData.getValues()));
        }

        chunkStart.setMillis(chunkEnd);
        columnCounter++;
      }
      columnMax = columnCounter;

      if (isSum != null) {
        row.createCell(0)
            .setCellValue(convertCamelCase(metricName) + " (" + (isSum ? "sum" : "avg") + ")");
      }
    }
    for (int i = 0; i < columnMax; i++) {
      sheet.autoSizeColumn(i);
    }
  }

  private double cumulativeRunningAverage(List<Double> values) {
    if (values.isEmpty()) {
      return 0;
    }
    SummaryStatistics summaryStatistics = new SummaryStatistics();
    for (Double value : values) {
      summaryStatistics.addValue(value);
    }
    return summaryStatistics.getMean();
  }

  private String getTimestamp(
      MutableDateTime chunkStart,
      MutableDateTime chunkEnd,
      int columnCounter,
      SUMMARY_INTERVALS summaryInterval) {
    StringBuilder title = new StringBuilder();
    title
        .append(StringUtils.capitalize(summaryInterval.toString()))
        .append(" ")
        .append(columnCounter)
        .append("\n");
    String timestamp = SUMMARY_TIMESTAMP;
    switch (summaryInterval) {
      case minute:
      case hour:
        break;
      case day:
      case week:
        timestamp = "dd-MM-y";
        break;
      case month:
        timestamp = "MM-y";
        break;
    }
    title
        .append(chunkStart.toDateTime(DateTimeZone.getDefault()).toString(timestamp))
        .append(" to ")
        .append(chunkEnd.toDateTime(DateTimeZone.getDefault()).toString(timestamp));
    return title.toString();
  }

  private String getRrdFilename(String metricsDir, String metricName) {
    return metricsDir + metricName + ".rrd";
  }

  private void increment(MutableDateTime chunkStart, SUMMARY_INTERVALS summaryInterval) {
    switch (summaryInterval) {
      case minute:
        chunkStart.addMinutes(1);
        break;
      case hour:
        chunkStart.addHours(1);
        break;
      case day:
        chunkStart.addDays(1);
        break;
      case week:
        chunkStart.addWeeks(1);
        break;
      case month:
        chunkStart.addMonths(1);
        break;
    }
  }

  /**
   * Creates an Excel worksheet containing the metric's data (timestamps and values) for the
   * specified time range. This worksheet is titled with the trhe metric's name and added to the
   * specified Workbook.
   *
   * @param wb the workbook to add this worksheet to
   * @param metricName the name of the metric whose data is being rendered in this worksheet
   * @param rrdFilename the name of the RRD file to retrieve the metric's data from
   * @param startTime start time, in seconds since Unix epoch, to fetch metric's data
   * @param endTime end time, in seconds since Unix epoch, to fetch metric's data
   * @throws IOException
   * @throws MetricsGraphException
   */
  private void createSheet(
      Workbook wb, String metricName, String rrdFilename, long startTime, long endTime)
      throws IOException, MetricsGraphException {
    LOGGER.trace("ENTERING: createSheet");

    MetricData metricData = getMetricData(rrdFilename, startTime, endTime);

    String displayableMetricName = convertCamelCase(metricName);

    String title =
        displayableMetricName
            + " for "
            + getCalendarTime(startTime)
            + " to "
            + getCalendarTime(endTime);

    Sheet sheet = wb.createSheet(displayableMetricName);

    Font headerFont = wb.createFont();
    headerFont.setBold(true);
    CellStyle columnHeadingsStyle = wb.createCellStyle();
    columnHeadingsStyle.setFont(headerFont);

    CellStyle bannerStyle = wb.createCellStyle();
    bannerStyle.setFont(headerFont);
    bannerStyle.setFillForegroundColor(HSSFColor.HSSFColorPredefined.PALE_BLUE.getIndex());
    bannerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

    int rowCount = 0;

    Row row = sheet.createRow((short) rowCount);
    Cell cell = row.createCell(0);
    cell.setCellValue(title);
    cell.setCellStyle(bannerStyle);
    rowCount++;

    // Blank row for spacing/readability
    row = sheet.createRow((short) rowCount);
    cell = row.createCell(0);
    cell.setCellValue("");
    rowCount++;

    row = sheet.createRow((short) rowCount);
    cell = row.createCell(0);
    cell.setCellValue("Timestamp");
    cell.setCellStyle(columnHeadingsStyle);
    cell = row.createCell(1);
    cell.setCellValue("Value");
    cell.setCellStyle(columnHeadingsStyle);
    rowCount++;

    List<Long> timestamps = metricData.getTimestamps();
    List<Double> values = metricData.getValues();

    for (int i = 0; i < timestamps.size(); i++) {
      String timestamp = getCalendarTime(timestamps.get(i));
      row = sheet.createRow((short) rowCount);
      row.createCell(0).setCellValue(timestamp);
      row.createCell(1).setCellValue(values.get(i));
      rowCount++;
    }

    if (metricData.hasTotalCount()) {
      // Blank row for spacing/readability
      row = sheet.createRow((short) rowCount);
      cell = row.createCell(0);
      cell.setCellValue("");
      rowCount++;

      row = sheet.createRow((short) rowCount);
      cell = row.createCell(0);
      cell.setCellValue("Total Count: ");
      cell.setCellStyle(columnHeadingsStyle);
      row.createCell(1).setCellValue(metricData.getTotalCount());
    }

    sheet.autoSizeColumn(0);
    sheet.autoSizeColumn(1);

    LOGGER.trace("EXITING: createSheet");
  }

  /**
   * Retrieves the RRD stored data for the specified metric over the specified time range.
   *
   * @param rrdFilename the name of the RRD file containing the metric's data
   * @param startTime start time, in seconds since Unix epoch, to fetch metric's data
   * @param endTime end time, in seconds since Unix epoch, to fetch metric's data
   * @return domain object containing the metric's sampled data, which consists of the timestamps
   *     and their associated values, and the total count of the sampled data
   * @throws IOException
   * @throws MetricsGraphException
   */
  public MetricData getMetricData(String rrdFilename, long startTime, long endTime)
      throws IOException, MetricsGraphException {
    LOGGER.trace("ENTERING: getMetricData");

    // Create RRD DB in read-only mode for the specified RRD file
    try (RrdDb rrdDb = new RrdDb(rrdFilename, true)) {

      // Extract the data source (should always only be one data source per RRD file - otherwise
      // we have a problem)
      if (rrdDb.getDsCount() != 1) {
        throw new MetricsGraphException(
            "Only one data source per RRD file is supported - RRD file "
                + rrdFilename
                + " has "
                + rrdDb.getDsCount()
                + " data sources.");
      }

      // The step (sample) interval that determines how often RRD collects the metric's data
      long rrdStep = rrdDb.getRrdDef().getStep();

      // Retrieve the RRD file's data source type to determine how (later)
      // to store the metric's data for presentation.
      DsType dataSourceType = rrdDb.getDatasource(0).getType();

      // Fetch the metric's data from the RRD file for the specified time range
      FetchRequest fetchRequest = rrdDb.createFetchRequest(ConsolFun.TOTAL, startTime, endTime);
      FetchData fetchData = fetchRequest.fetchData();
      long[] timestamps = fetchData.getTimestamps();
      double[] values = fetchData.getValues(0);

      // Done retrieving data from the RRD database - close it, otherwise no one else will
      // be able to access it later.
      rrdDb.close();

      // The lists of the metric's timestamps and their associated values that have non-"NaN"
      // values
      List<Long> validTimestamps = new ArrayList<Long>();
      List<Double> validValues = new ArrayList<Double>();

      long totalCount = 0;
      MetricData metricData = new MetricData();

      if (dataSourceType == DsType.COUNTER || dataSourceType == DsType.DERIVE) {
        // Counters are for constantly incrementing data, hence they can
        // have a summation of their totals
        metricData.setHasTotalCount(true);
        for (int i = 0; i < timestamps.length; i++) {
          long timestamp = timestamps[i];
          // Filter out the RRD values that have not yet been sampled (they will
          // have been set to NaN as a placeholder when the RRD file was created)
          if (timestamp >= startTime
              && timestamp <= endTime
              && !Double.toString(values[i]).equals("NaN")) {
            // RRD averages the collected samples over the step interval.
            // To "undo" this averaging and get the actual count, need to
            // multiply the sampled data value by the RRD step interval.
            double nonAveragedValue = (double) (values[i] * rrdStep);
            validTimestamps.add(timestamp);
            validValues.add(nonAveragedValue);
            totalCount += (long) nonAveragedValue;
          }
        }
      } else if (dataSourceType == DsType.GAUGE) {
        // Gauges are for data that waxes and wanes, hence no total count
        metricData.setHasTotalCount(false);
        for (int i = 0; i < timestamps.length; i++) {
          long timestamp = timestamps[i];
          // Filter out the RRD values that have not yet been sampled (they will
          // have been set to NaN as a placeholder when the RRD file was created)
          if (timestamp >= startTime
              && timestamp <= endTime
              && !Double.toString(values[i]).equals("NaN")) {
            validTimestamps.add(timestamp);
            validValues.add(values[i]);
          }
        }
      }

      metricData.setTimestamps(validTimestamps);
      metricData.setValues(validValues);
      metricData.setTotalCount(totalCount);

      LOGGER.trace("EXITING: getMetricData");

      return metricData;
    }
  }

  private void dumpData(
      ConsolFun consolFun,
      String dataType,
      RrdDb rrdDb,
      String dsType,
      long startTime,
      long endTime) {
    String rrdFilename = rrdDb.getPath();
    LOGGER.trace("***********  START Dump of RRD file:  [{}]  ***************", rrdFilename);
    LOGGER.trace("metricsMaxThreshold = {}", metricsMaxThreshold);

    FetchRequest fetchRequest = rrdDb.createFetchRequest(consolFun, startTime, endTime);
    try {
      FetchData fetchData = fetchRequest.fetchData();
      LOGGER.trace("************  {}: {}  **************", dsType, dataType);

      int rrdStep = RRD_STEP; // in seconds
      long[] timestamps = fetchData.getTimestamps();
      double[] values = fetchData.getValues(0);
      double[] adjustedValues = new double[values.length];
      for (int i = 0; i < values.length; i++) {
        double adjustedValue = values[i] * rrdStep;
        adjustedValues[i] = adjustedValue;

        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace(
              "{}: {} (adjusted value = {}, floor = {}, round =  {})",
              getCalendarTime(timestamps[i]),
              values[i],
              adjustedValue,
              Math.floor(adjustedValue),
              Math.round(adjustedValue));
        }
      }

      LOGGER.trace("adjustedValues.length = {}", adjustedValues.length);

      for (int i = 0; i < adjustedValues.length; i++) {
        if (adjustedValues[i] > metricsMaxThreshold) {
          LOGGER.trace("Value [{}] is an OUTLIER", adjustedValues[i]);
        }
      }

    } catch (IOException e) {
    }

    LOGGER.trace("***********  END Dump of RRD file:  [{}]  ***************", rrdFilename);
  }
}
