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
package ddf.metrics.reporting.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface MetricsRetriever {
  /**
   * Retrieves the PNG-formatted graph of the metric's data from the specified RRD file over the
   * specified time range. The vertical axis label defaults to the metric's name, and the title for
   * the graph defaults to the metric's name and the time range (e.g., catalogQueries for Apr 10
   * 2013 09:14:43 to Apr 10 2013 09:29:43).
   *
   * @param metricName name of the metric to be graphed
   * @param rrdFilename name of the RRD file to retrieve the metric's data from
   * @param startTime start time, in seconds since Unix epoch, to retrieve metric's data
   * @param endTime end time, in seconds since Unix epoch, to retrieve metric's data
   * @return PNG-formatted graph of metric's data
   * @throws IOException
   * @throws MetricsGraphException
   */
  public byte[] createGraph(String metricName, String rrdFilename, long startTime, long endTime)
      throws IOException, MetricsGraphException;

  /**
   * Retrieves the PNG-formatted graph of the metric's data from the specified RRD file over the
   * specified time range, using the input y-axis label and title on the graph that is generated.
   *
   * @param metricName name of the metric to be graphed
   * @param rrdFilename name of the RRD file to retrieve the metric's data from
   * @param startTime start time, in seconds since Unix epoch, to retrieve metric's data
   * @param endTime end time, in seconds since Unix epoch, to retrieve metric's data
   * @param verticalAxisLabel label to use for the vertical (y) axis
   * @param title the title to use for the graph
   * @return
   * @throws IOException
   * @throws MetricsGraphException
   */
  public byte[] createGraph(
      String metricName,
      String rrdFilename,
      long startTime,
      long endTime,
      String verticalAxisLabel,
      String title)
      throws IOException, MetricsGraphException;

  /**
   * Retrieves the metric's data as a CSV (Comma Separated Values) formatted string from the
   * specified RRD file over the specified time range. This CSV string will consist of the first row
   * being the column headers (namely Timestamp,Value), and the remaining lines will each contain
   * the timestamp and value for a single sample of the metric's data.
   *
   * @param rrdFilename name of the RRD file to retrieve the metric's data from
   * @param startTime start time, in seconds since Unix epoch, to retrieve metric's data
   * @param endTime end time, in seconds since Unix epoch, to retrieve metric's data
   * @return CSV-formatted string of the metric's data
   * @throws IOException
   * @throws MetricsGraphException
   */
  public String createCsvData(String rrdFilename, long startTime, long endTime)
      throws IOException, MetricsGraphException;

  /**
   * Retrieves the metric's data as an XML formatted string from the specified RRD file over the
   * specified time range. No XML schema is defined or referenced in the returned XML.
   *
   * <p>The format of this XML string will be:
   *
   * <pre>{@code
   * <metricName>
   *     <title>metricName for startTime to endTime</title>
   *     <data>
   *         <sample>
   *             <timestamp>MMM DD YYY hh:mm:ss</timestamp>
   *             <value>12345</value>
   *         </sample>
   *         <sample>
   *         ...
   *         </sample>
   *         <totalCount>99999</totalCount>
   *     </data>
   * </metricName>
   * }</pre>
   *
   * Note that <totalCount> is only present for metrics that are counters, i.e., values that would
   * always increment and never decrement, e.g., catalogQueries metric.
   *
   * @param metricName name of the metric to retrieve data for
   * @param rrdFilename name of the RRD file to retrieve the metric's data from
   * @param startTime start time, in seconds since Unix epoch, to retrieve metric's data
   * @param endTime end time, in seconds since Unix epoch, to retrieve metric's data
   * @return XML-formatted string of the metric's data
   * @throws IOException
   * @throws MetricsGraphException
   */
  public String createXmlData(String metricName, String rrdFilename, long startTime, long endTime)
      throws IOException, MetricsGraphException;

  /**
   * Retrieves the metric's data as an XLS (Excel spreadsheet) formatted stream from the specified
   * RRD file over the specified time range. This XLS stream will consist of:
   *
   * <ul>
   *   <li>the first row being the title of the spreadsheet
   *   <li>a blank row for spacing/readability
   *   <li>column headers (namely Timestamp,Value)
   *   <li>the remaining rows will each contain the timestamp and value for a single sample of the
   *       metric's data
   *   <li>if the metric has a total count, then this will be in the last row of the spreadsheet
   * </ul>
   *
   * @param metricName name of the metric to retrieve data for
   * @param rrdFilename name of the RRD file to retrieve the metric's data from
   * @param startTime start time, in seconds since Unix epoch, to retrieve metric's data
   * @param endTime end time, in seconds since Unix epoch, to retrieve metric's data
   * @return XLS-formatted stream of the metric's data
   * @throws IOException
   * @throws MetricsGraphException
   */
  public OutputStream createXlsData(
      String metricName, String rrdFilename, long startTime, long endTime)
      throws IOException, MetricsGraphException;

  /**
   * Retrieves the metric's data as a JSON formatted string from the specified RRD file over the
   * specified time range.
   *
   * <p>The format of this JSON string will be (data values in this sample are notional):
   *
   * <pre>{@code
   * {
   *     "title":"catalogQueries for Apr 10 2013 10:18:05 to Apr 10 2013 10:33:05",
   *     "totalCount":5348,
   *     "data":[
   *       {
   *           "timestamp":"Apr 10 2013 10:18:00",
   *           "value":351
   *       },
   *       {
   *           "timestamp":"Apr 10 2013 10:19:00",
   *           "value":358
   *       },
   *     ]
   * }
   * }</pre>
   *
   * Note that <totalCount> is only present for metrics that are counters, i.e., values that would
   * always increment and never decrement, e.g., catalogQueries metric.
   *
   * @param metricName name of the metric to retrieve data for
   * @param rrdFilename name of the RRD file to retrieve the metric's data from
   * @param startTime start time, in seconds since Unix epoch, to retrieve metric's data
   * @param endTime end time, in seconds since Unix epoch, to retrieve metric's data
   * @return JSON-formatted string of the metric's data
   * @throws IOException
   * @throws MetricsGraphException
   */
  public String createJsonData(String metricName, String rrdFilename, long startTime, long endTime)
      throws IOException, MetricsGraphException;

  /**
   * Returns an XLS (Excel spreadsheet) formatted stream over the specified time range that contains
   * one worksheet for each metric. Refer to the createXlsData() method for a description of what
   * each worksheet would contain.
   *
   * @param metricNames names of the metrics to retrieve data for
   * @param metricsDir directory containing all of the metrics' RRD files, typically
   *     <DDF_INSTALL_DIR>/data/metrics
   * @param startTime start time, in seconds since Unix epoch, to retrieve metric's data
   * @param endTime end time, in seconds since Unix epoch, to retrieve metric's data
   * @param summaryInterval unit of time to group metrics by
   * @return XLS-formatted stream of all metrics' data
   * @throws IOException
   * @throws MetricsGraphException
   */
  public OutputStream createXlsReport(
      List<String> metricNames,
      String metricsDir,
      long startTime,
      long endTime,
      String summaryInterval)
      throws IOException, MetricsGraphException;
}
