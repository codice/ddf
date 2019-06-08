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

import ddf.metrics.reporting.internal.MetricsEndpointException;
import ddf.metrics.reporting.internal.MetricsGraphException;
import ddf.metrics.reporting.internal.MetricsRetriever;
import ddf.metrics.reporting.internal.rrd4j.RrdMetricsRetriever;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides an endpoint for a client to access the historical metrics data collected by
 * DDF.
 *
 * <p>This endpoint provides a URL to retrieve the list of metrics collected by DDF, including their
 * associated URLs to access pre-defined time ranges of each metric's historical data, e.g., for the
 * past 15 minutes, 1 hour, 4 hours, 12 hours, 24 hours, 3 days, 1 week, 1 month, and 1 year. Each
 * of these hyperlinks will return a byte array containing a PNG graph of the metric's historical
 * data for the given time range.
 */
@Path("/")
public class MetricsEndpoint {
  public static final String DEFAULT_METRICS_DIR =
      new AbsolutePathResolver("data" + File.separator + "metrics" + File.separator).getPath();

  static final Map<String, Long> TIME_RANGES = new HashMap<>();

  private static final Logger LOGGER = LoggerFactory.getLogger(MetricsEndpoint.class);

  private static final String METRICS_SERVICE_BASE_URL = "/services/internal/metrics";

  private static final String RRD_FILE_EXTENSION = ".rrd";

  private static final String JSON_MIME_TYPE = "application/json";

  private static final String PNG_MIME_TYPE = "image/png";

  private static final String DATE_OFFSET_QUERY = "?dateOffset=";

  private static final int MILLISECONDS_PER_SECOND = 1000;

  private static final long FIFTEEN_MINUTES_IN_SECONDS = TimeUnit.MINUTES.toSeconds(15);

  private static final long ONE_HOUR_IN_SECONDS = 4 * FIFTEEN_MINUTES_IN_SECONDS;

  private static final long ONE_DAY_IN_SECONDS = 24 * ONE_HOUR_IN_SECONDS;

  private static final long ONE_WEEK_IN_SECONDS = 7 * ONE_DAY_IN_SECONDS;

  private static final long ONE_MONTH_IN_SECONDS = 30 * ONE_DAY_IN_SECONDS;

  private static final long THREE_MONTHS_IN_SECONDS = 90 * ONE_DAY_IN_SECONDS;

  private static final long SIX_MONTHS_IN_SECONDS = 180 * ONE_DAY_IN_SECONDS;

  private static final long ONE_YEAR_IN_SECONDS = 365 * ONE_DAY_IN_SECONDS;

  private static final String PNG_FORMAT = "png";

  static {
    TIME_RANGES.put("15m", FIFTEEN_MINUTES_IN_SECONDS);
    TIME_RANGES.put("1h", ONE_HOUR_IN_SECONDS);
    TIME_RANGES.put("1d", ONE_DAY_IN_SECONDS);
    TIME_RANGES.put("1w", ONE_WEEK_IN_SECONDS);
    TIME_RANGES.put("1M", ONE_MONTH_IN_SECONDS);
    TIME_RANGES.put("3M", THREE_MONTHS_IN_SECONDS);
    TIME_RANGES.put("6M", SIX_MONTHS_IN_SECONDS);
    TIME_RANGES.put("1y", ONE_YEAR_IN_SECONDS);
  }

  private final FastDateFormat dateFormatter = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ssZ");

  private static final String DEFAULTED_TO_ENDTIME = "Defaulted endTime to {}";

  private String metricsDir = DEFAULT_METRICS_DIR;

  private MetricsRetriever metricsRetriever = new RrdMetricsRetriever();

  /**
   * Retrieve data for the specified metric over the given time range. The URL to access this method
   * is of the form http://<host>:<port>/<metricName>.<outputFormat> So the desired metric filename
   * is specified in the URL, e.g., catalogQueryCount.png, where the filename extension defines the
   * desired output format returned for the metric's data. Currently supported formats are png, csv,
   * xls, ppt, xml, and json.
   *
   * <p>Note that the time range can be specified as either a start and end date (in RFC3339 format,
   * i.e., YYYY-MM-DD'T'hh:mm:ssZ), or as an offset in seconds from the current time. These 2 time
   * range mechanisms cannot be combined, e.g., you cannot specify an end date and an offset to be
   * applied from that end date.
   *
   * <p>By default, the metric's name will be used for the y-axis label on the PNG graph, and the
   * metric name and time range will be used for the graph's title. Both of these can be optionally
   * specified with the yAxisLabel and title parameters. These 2 parameters do not apply for the
   * other formats.
   *
   * @param metricName Name of the metric being graphed, e.g., queryCount
   * @param outputFormat output format of the metric, e.g. csv
   * @param startDate Specifies the start of the time range of the search on the metric's data
   *     (RFC-3339 - Date and Time format, i.e. YYYY-MM-DDTHH:mm:ssZ). Cannot be used with
   *     dateOffset parameter.
   * @param endDate Specifies the end of the time range of the search on the metric's data (RFC-3339
   *     - Date and Time format, i.e. YYYY-MM-DDTHH:mm:ssZ). Cannot be used with dateOffset
   *     parameter.
   * @param dateOffset Specifies an offset, backwards from the current time, to search on the
   *     modified time field for entries. Defined in seconds. Cannot be used with startDate and
   *     endDate parameters.
   * @param yAxisLabel (optional) the label to apply to the graph's y-axis
   * @param title (optional) the title to be applied to the graph
   * @param uriInfo
   * @return Response containing the metric's data in the specified outputFormat
   * @throws MetricsEndpointException
   */
  @SuppressWarnings({"squid:S3776", "squid:S00107"})
  @GET
  @Path("/{metricName}.{outputFormat}")
  public Response getMetricsData(
      @PathParam("metricName") String metricName,
      @PathParam("outputFormat") String outputFormat,
      @QueryParam("startDate") String startDate,
      @QueryParam("endDate") String endDate,
      @QueryParam("dateOffset") String dateOffset,
      @QueryParam("yAxisLabel") String yAxisLabel,
      @QueryParam("title") String title,
      @Context UriInfo uriInfo)
      throws MetricsEndpointException {
    LOGGER.trace(
        "ENTERING: getMetricsData  -  metricName = {},    outputFormat = {}",
        metricName,
        outputFormat);
    LOGGER.trace("request url: {}", uriInfo.getRequestUri());
    LOGGER.trace("startDate = {},     endDate = {}", startDate, endDate);
    LOGGER.trace("dateOffset = {}", dateOffset);

    Response response = null;

    // Client must specify *either* startDate and/or endDate *OR* dateOffset
    if (!StringUtils.isBlank(dateOffset)
        && (!StringUtils.isBlank(startDate) || !StringUtils.isBlank(endDate))) {
      throw new MetricsEndpointException(
          "Cannot specify dateOffset and startDate or endDate, must specify either dateOffset only or startDate and/or endDate",
          Response.Status.BAD_REQUEST);
    }

    long endTime;
    if (!StringUtils.isBlank(endDate)) {
      endTime = parseDate(endDate);
      LOGGER.trace("Parsed endTime = {}", endTime);
    } else {
      // Default end time for metrics graphing to now (in seconds)
      Calendar now = getCalendar();
      endTime = now.getTimeInMillis() / MILLISECONDS_PER_SECOND;
      LOGGER.trace(DEFAULTED_TO_ENDTIME, endTime);

      // Set endDate to new calculated endTime (so that endDate is displayed properly
      // in graph's title)
      endDate = dateFormatter.format(now.getTime());
    }

    long startTime;
    if (!StringUtils.isBlank(startDate)) {
      startTime = parseDate(startDate);
      LOGGER.trace("Parsed startTime = {}", startTime);
    } else if (!StringUtils.isBlank(dateOffset)) {
      startTime = endTime - Long.parseLong(dateOffset);
      LOGGER.trace("Offset-computed startTime = {}", startTime);

      // Set startDate to new calculated startTime (so that startDate is displayed properly
      // in graph's title)
      Calendar cal = getCalendar();
      cal.setTimeInMillis(startTime * MILLISECONDS_PER_SECOND);
      startDate = dateFormatter.format(cal.getTime());
    } else {
      // Default start time for metrics graphing to end time last 24 hours (in seconds)
      startTime = endTime - ONE_DAY_IN_SECONDS;
      LOGGER.trace("Defaulted startTime to {}", startTime);

      // Set startDate to new calculated startTime (so that startDate is displayed properly
      // in graph's title)
      Calendar cal = getCalendar();
      cal.setTimeInMillis(startTime * MILLISECONDS_PER_SECOND);
      startDate = dateFormatter.format(cal.getTime());
    }

    LOGGER.trace("startDate = {},   endDate = {}", startDate, endDate);

    if (StringUtils.isBlank(yAxisLabel)) {
      yAxisLabel = RrdMetricsRetriever.convertCamelCase(metricName);
    }

    if (StringUtils.isBlank(title)) {
      title =
          RrdMetricsRetriever.convertCamelCase(metricName) + " for " + startDate + " to " + endDate;
    }

    // Convert metric filename to rrd filename (because RRD file required by MetricRetriever to
    // generate graph)
    String rrdFilename = metricsDir + metricName + RRD_FILE_EXTENSION;

    if (outputFormat.equalsIgnoreCase(PNG_FORMAT)) {
      LOGGER.trace("Retrieving PNG-formatted data for metric {}", metricName);
      try {
        byte[] metricsGraphBytes =
            metricsRetriever.createGraph(
                metricName, rrdFilename, startTime, endTime, yAxisLabel, title);
        ByteArrayInputStream bis = new ByteArrayInputStream(metricsGraphBytes);
        response = Response.ok(bis, PNG_MIME_TYPE).build();
      } catch (IOException | MetricsGraphException e) {
        LOGGER.info("Could not create graph for specified metric");
        throw new MetricsEndpointException(
            "Cannot create metrics graph for specified metric.", Response.Status.BAD_REQUEST);
      }
    } else if ("csv".equalsIgnoreCase(outputFormat)) {
      try {
        String csv = metricsRetriever.createCsvData(rrdFilename, startTime, endTime);
        ResponseBuilder responseBuilder = Response.ok(csv);
        responseBuilder.type("text/csv");
        response = responseBuilder.build();
      } catch (IOException | MetricsGraphException e) {
        LOGGER.info("Could not create CSV data for specified metric");
        throw new MetricsEndpointException(
            "Cannot create CSV data for specified metric.", Response.Status.BAD_REQUEST);
      }
    } else if ("xls".equalsIgnoreCase(outputFormat)) {
      LOGGER.trace("Retrieving XLS-formatted data for metric {}", metricName);
      try (OutputStream os =
          metricsRetriever.createXlsData(metricName, rrdFilename, startTime, endTime)) {
        InputStream is = new ByteArrayInputStream(((ByteArrayOutputStream) os).toByteArray());
        ResponseBuilder responseBuilder = Response.ok(is);
        responseBuilder.type("application/vnd.ms-excel");
        response = responseBuilder.build();
      } catch (IOException | MetricsGraphException e) {
        LOGGER.info("Could not create XLS data for specified metric");
        throw new MetricsEndpointException(
            "Cannot create XLS data for specified metric.", Response.Status.BAD_REQUEST);
      }
    } else if ("xml".equalsIgnoreCase(outputFormat)) {
      LOGGER.trace("Retrieving XML-formatted data for metric {}", metricName);
      try {
        String xmlData =
            metricsRetriever.createXmlData(metricName, rrdFilename, startTime, endTime);
        ResponseBuilder responseBuilder = Response.ok(xmlData);
        responseBuilder.type("text/xml");
        response = responseBuilder.build();
      } catch (IOException | MetricsGraphException e) {
        LOGGER.info("Could not create XML data for specified metric");
        throw new MetricsEndpointException(
            "Cannot create XML data for specified metric.", Response.Status.BAD_REQUEST);
      }
    } else if ("json".equalsIgnoreCase(outputFormat)) {
      LOGGER.trace("Retrieving JSON-formatted data for metric {}", metricName);
      try {
        String jsonData =
            metricsRetriever.createJsonData(metricName, rrdFilename, startTime, endTime);
        ResponseBuilder responseBuilder = Response.ok(jsonData);
        responseBuilder.type(JSON_MIME_TYPE);
        response = responseBuilder.build();
      } catch (IOException | MetricsGraphException e) {
        LOGGER.info("Could not create JSON data for specified metric");
        throw new MetricsEndpointException(
            "Cannot create JSON data for specified metric.", Response.Status.BAD_REQUEST);
      }
    }

    LOGGER.trace("EXITING: getMetricsData");

    return response;
  }

  /**
   * Get list of available metrics and the associated URLs to their historical data.
   *
   * @param uriInfo
   * @return JSON-formatted response where each metric has a list of URLs (and the display text for
   *     them), where each URL links to a graph of the metric's data for a specific time range from
   *     current time (e.g., for last 4 hours).
   */
  @GET
  @Path("/")
  @Produces({JSON_MIME_TYPE})
  public Response getMetricsList(@Context UriInfo uriInfo) {
    Response response = null;

    List<String> metricNames = getMetricsNames();

    Map<String, Map<String, Map<String, String>>> metrics =
        new LinkedHashMap<String, Map<String, Map<String, String>>>();
    for (String metricName : metricNames) {
      generateMetricsUrls(metrics, metricName, uriInfo);
    }

    String jsonText = JSONValue.toJSONString(metrics);
    LOGGER.trace(jsonText);

    response = Response.ok(jsonText).build();

    return response;
  }

  /**
   * Retrieve data for the all metrics over the given time range. The URL to access this method is
   * of the form http://<host>:<port>/report.<outputFormat> The filename extension defines the
   * desired output format returned for the report's data. Currently supported formats are xls and
   * ppt.
   *
   * <p>The XLS-formatted report will be one spreadsheet (workbook) with a worksheet per metric. The
   * PPT-formatted report will be one PowerPoint slide deck with a slide per metric. Each slide will
   * contain the metric's PNG graph.
   *
   * <p>If a summary interval is requested, the XSL report will instead contain a single table, with
   * the summarized values for each interval and metric. Cannot be used with PPT format.
   *
   * <p>Note that the time range can be specified as either a start and end date (in RFC3339 format,
   * i.e., YYYY-MM-DD'T'hh:mm:ssZ), or as an offset in seconds from the current time. These 2 time
   * range mechanisms cannot be combined, e.g., you cannot specify an end date and an offset to be
   * applied from that end date.
   *
   * <p>By default, the metric's name will be used for the y-axis label, and the metric name and
   * time range will be used for the graph's title for the report in PPT format.
   *
   * @param startDate Specifies the start of the time range of the search on the metric's data
   *     (RFC-3339 - Date and Time format, i.e. YYYY-MM-DDTHH:mm:ssZ). Cannot be used with
   *     dateOffset parameter.
   * @param endDate Specifies the end of the time range of the search on the metric's data (RFC-3339
   *     - Date and Time format, i.e. YYYY-MM-DDTHH:mm:ssZ). Cannot be used with dateOffset
   *     parameter.
   * @param dateOffset Specifies an offset, backwards from the current time, to search on the
   *     modified time field for entries. Defined in seconds. Cannot be used with startDate or
   *     endDate parameters.
   * @param summaryInterval One of {@link
   *     ddf.metrics.reporting.internal.rrd4j.RrdMetricsRetriever.SUMMARY_INTERVALS}
   * @param uriInfo
   * @return Response containing the report as a stream in either XLS or PPT format
   * @throws MetricsEndpointException
   */
  @GET
  @Path("/report.{outputFormat}")
  public Response getMetricsReport(
      @PathParam("outputFormat") String outputFormat,
      @QueryParam("startDate") String startDate,
      @QueryParam("endDate") String endDate,
      @QueryParam("dateOffset") String dateOffset,
      @QueryParam("summaryInterval") String summaryInterval,
      @Context UriInfo uriInfo)
      throws MetricsEndpointException {
    LOGGER.debug("ENTERING: getMetricsReport  -  outputFormat = {}", outputFormat);
    LOGGER.debug("request url: {}", uriInfo.getRequestUri());
    LOGGER.debug("startDate = {},     endDate = {}", startDate, endDate);
    LOGGER.debug("dateOffset = {}", dateOffset);

    Response response = null;

    // Client must specify *either* startDate and/or endDate *OR* dateOffset
    if (!StringUtils.isBlank(dateOffset)
        && (!StringUtils.isBlank(startDate) || !StringUtils.isBlank(endDate))) {
      throw new MetricsEndpointException(
          "Cannot specify dateOffset and startDate or endDate, must specify either dateOffset only or startDate and/or endDate",
          Response.Status.BAD_REQUEST);
    }

    long endTime;
    if (!StringUtils.isBlank(endDate)) {
      endTime = parseDate(endDate);
      LOGGER.debug("Parsed endTime = {}", endTime);
    } else {
      // Default end time for metrics graphing to now (in seconds)
      Calendar now = getCalendar();
      endTime = now.getTimeInMillis() / MILLISECONDS_PER_SECOND;
      LOGGER.debug(DEFAULTED_TO_ENDTIME, endTime);

      // Set endDate to new calculated endTime (so that endDate is displayed properly
      // in graph's title)
      endDate = dateFormatter.format(now.getTime());
    }

    long startTime;
    if (!StringUtils.isBlank(startDate)) {
      startTime = parseDate(startDate);
      LOGGER.debug("Parsed startTime = {}", startTime);
    } else if (!StringUtils.isBlank(dateOffset)) {
      startTime = endTime - Long.parseLong(dateOffset);
      LOGGER.debug("Offset-computed startTime = {}", startTime);

      // Set startDate to new calculated startTime (so that startDate is displayed properly
      // in graph's title)
      Calendar cal = getCalendar();
      cal.setTimeInMillis(startTime * MILLISECONDS_PER_SECOND);
      startDate = dateFormatter.format(cal.getTime());
    } else {
      // Default start time for metrics graphing to end time last 24 hours (in seconds)
      startTime = endTime - ONE_DAY_IN_SECONDS;
      LOGGER.debug("Defaulted startTime to {}", startTime);

      // Set startDate to new calculated startTime (so that startDate is displayed properly
      // in graph's title)
      Calendar cal = getCalendar();
      cal.setTimeInMillis(startTime * MILLISECONDS_PER_SECOND);
      startDate = dateFormatter.format(cal.getTime());
    }

    LOGGER.debug("startDate = {},   endDate = {}", startDate, endDate);

    List<String> metricNames = getMetricsNames();

    // Generated name for metrics file (<DDF Sitename>_<Startdate>_<EndDate>.outputFormat)
    String dispositionString =
        "attachment; filename="
            + SystemInfo.getSiteName()
            + "_"
            + startDate.substring(0, 10)
            + "_"
            + endDate.substring(0, 10)
            + "."
            + outputFormat;

    try {
      if ("xls".equalsIgnoreCase(outputFormat)) {
        try (OutputStream os =
            metricsRetriever.createXlsReport(
                metricNames, metricsDir, startTime, endTime, summaryInterval)) {
          InputStream is = new ByteArrayInputStream(((ByteArrayOutputStream) os).toByteArray());
          ResponseBuilder responseBuilder = Response.ok(is);
          responseBuilder.type("application/vnd.ms-excel");
          responseBuilder.header("Content-Disposition", dispositionString);
          response = responseBuilder.build();
        }
      }
    } catch (IOException | MetricsGraphException e) {
      LOGGER.debug("Could not create {} report", outputFormat, e);
      throw new MetricsEndpointException(
          "Could not create report in specified output format.", Response.Status.BAD_REQUEST);
    }

    LOGGER.debug("EXITING: getMetricsReport");

    return response;
  }

  private Calendar getCalendar() {
    return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
  }

  /**
   * Parse date in ISO8601 format into seconds since Unix epoch.
   *
   * @param date
   * @return
   */
  protected long parseDate(String date) {
    DateTimeFormatter dateFormatter = ISODateTimeFormat.dateTimeNoMillis();
    Date formattedDate = dateFormatter.parseDateTime(date).toDate();

    return formattedDate.getTime() / 1000;
  }

  /**
   * Generates the URLs for each time range, e.g., 15m, 1h, etc. for the specified metric.
   *
   * <p>The metric's URL info will be put in the {@code metrics} Maps passed in to this method. The
   * structure of these nested Maps are: {@code Map1<String1, Map2<String2, Map3<String3,String4>>>}
   * (Numbers added to end of Map and String types so that each position could be referred to)
   * where:
   *
   * <ul>
   *   <li>String1 = metric name, e.g., "catalogQueries"
   *   <li>Map2 = mapping of time range to its list of hyperlinks
   *   <li>String2 = time range, e.g., "15m"
   *   <li>Map3 = hyperlink for each format type (e.g., PNG) for each time range for each metric
   *   <li>String3 = display text for hyperlink, e.g., PNG
   *   <li>String4 = hyperlink for metric data in specific format, e.g.,
   *       http://host:port/services/internal/metrics/catalogQueries.png?dateOffset=900
   * </ul>
   *
   * @param metrics nested Maps that will be populated with the metric's URL info
   * @param metricsName name of the metric to generate URLs for
   * @param uriInfo used to extract the base URL and append the metric's path to it
   */
  protected void generateMetricsUrls(
      Map<String, Map<String, Map<String, String>>> metrics, String metricsName, UriInfo uriInfo) {
    // Generate text and hyperlink for single metric for 15 minute, 1 hour, 4 hours,
    // 12 hours, 1 day, 3 days, 1 week, 1 month, and 1 year
    Calendar cal = getCalendar();
    long endTime = cal.getTimeInMillis() / 1000;
    LOGGER.trace(DEFAULTED_TO_ENDTIME, endTime);

    String[] supportedFormats = new String[] {"png", "csv", "xls"};

    // key=time range
    // value=list of hyperlinks (and their display text) for each supported format
    // Example:
    // key="15m"
    // value=[("PNG", "http://host:port/.../catalogQueries.png?dateOffset=900),("CSV", ...)]
    SortedMap<String, Map<String, String>> metricTimeRangeLinks =
        new TreeMap<String, Map<String, String>>(new MetricsTimeRangeComparator());
    Iterator timeRangesIter = TIME_RANGES.entrySet().iterator();
    while (timeRangesIter.hasNext()) {
      Map.Entry entry = (Map.Entry) timeRangesIter.next();
      String timeRange = (String) entry.getKey();
      long timeRangeInSeconds = (Long) entry.getValue();
      Map<String, String> metricsUrls = new LinkedHashMap<String, String>();
      for (String format : supportedFormats) {
        // CXF bug: getAbsolutePath() caches the path that was used the very first time
        // to access this REST service. For example, if client used localhost:8181/... to
        // access this service and then later a different client uses the IP address,
        // 172.18.14.169:8181/... to access this service, getAbsolutePath() will still
        // return localhost:8181 (which will not work for someone connecting remotely to
        // this service since they are probably not running DDF too).

        /*
         * START: CXF bug with getAbsolutePath()
         *
         * String uriAbsolutePath = uriInfo.getAbsolutePath().toASCIIString(); UriBuilder
         * uriBuilder = UriBuilder.fromPath(uriAbsolutePath); uriBuilder.path(metricsName +
         * "." + format); URI uri = uriBuilder.build(); String baseMetricsUrl =
         * uri.toASCIIString(); String metricsUrl = baseMetricsUrl + DATE_OFFSET_QUERY +
         * timeRangeInSeconds;
         *
         * END: CXF bug
         */

        String metricsUrl =
            SystemBaseUrl.EXTERNAL.constructUrl(
                METRICS_SERVICE_BASE_URL
                    + "/"
                    + metricsName
                    + "."
                    + format
                    + DATE_OFFSET_QUERY
                    + timeRangeInSeconds);

        // key=format
        // value=url for format with specified time range in seconds
        // Example:
        // "PNG", "http://host:port/.../catalogQueries.png?dateOffset=900
        metricsUrls.put(format.toUpperCase(), metricsUrl);
      }
      metricTimeRangeLinks.put(timeRange, metricsUrls);
    }

    metrics.put(metricsName, metricTimeRangeLinks);
  }

  /**
   * Returns the list of all of the RRD files in the metrics directory.
   *
   * @return
   */
  private String[] getRrdFiles() {
    FilenameFilter rrdFilter =
        new FilenameFilter() {
          public boolean accept(File dir, String name) {
            return name.endsWith(RRD_FILE_EXTENSION);
          }
        };

    File dir = new File(metricsDir);
    String[] rrdFiles = dir.list(rrdFilter);

    return rrdFiles;
  }

  /**
   * Returns a list of all of the metrics' names based on the list of RRD files found in the metrics
   * directory.
   *
   * @return
   */
  private List<String> getMetricsNames() {
    String[] rrdFiles = getRrdFiles();
    List<String> metricNames = new ArrayList<String>();
    if (rrdFiles != null) {
      for (String rrdFile : rrdFiles) {
        String metricsName =
            FilenameUtils.getFullPath(rrdFile) + FilenameUtils.getBaseName(rrdFile);
        metricNames.add(metricsName);
      }
    }

    Collections.sort(metricNames);

    LOGGER.trace("Returning {} metrics", metricNames.size());

    return metricNames;
  }

  void setMetricsDir(String metricsDir) {
    this.metricsDir = metricsDir;
  }

  void setMetricsRetriever(MetricsRetriever metricsRetriever) {
    this.metricsRetriever = metricsRetriever;
  }

  public void setMetricsMaxThreshold(double metricsMaxThreshold) {
    LOGGER.debug(
        "Creating new RrdMetricsRetriever with metricsMaxThreshold = {}", metricsMaxThreshold);
    metricsRetriever = new RrdMetricsRetriever(metricsMaxThreshold);
  }

  /**
   * Comparator used to sort metric time ranges by chronological order rather than the default
   * lexigraphical order.
   */
  static class MetricsTimeRangeComparator implements Comparator, Serializable {
    private static final long serialVersionUID = 1L;

    public int compare(Object o1, Object o2) {
      String timeRange1 = (String) o1;
      String timeRange2 = (String) o2;

      Long dateOffset1 = TIME_RANGES.get(timeRange1);
      Long dateOffset2 = TIME_RANGES.get(timeRange2);

      return dateOffset1.compareTo(dateOffset2);
    }
  }
}
