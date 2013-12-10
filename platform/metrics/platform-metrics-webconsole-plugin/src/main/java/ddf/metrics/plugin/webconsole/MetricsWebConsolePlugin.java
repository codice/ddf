/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.metrics.plugin.webconsole;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.log4j.Logger;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.simple.JSONValue;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.framework.BundleContext;

/**
 * Felix Web Console plugin to create a Metrics tab for interacting with the {@link MetricsEndpoint}
 * . This plugin displays a table of all of the monitored metrics and their associated hyperlinks to
 * display an each metrics collected data in various formats, including PNG graph, CSV, Excel
 * spreadsheet, and PowerPoint slides.
 * 
 * @author rodgersh
 * @author ddf.isgs@lmco.com
 * 
 */
public class MetricsWebConsolePlugin extends AbstractWebConsolePlugin {
    private static final long serialVersionUID = -3725252410686520419L;

    private static final Logger LOGGER = Logger.getLogger(MetricsWebConsolePlugin.class);

    // TODO: When DdfConfigurationManager and Watcher are moved from the catalog app to the platform
    // app
    // then this class should implement the DdfConfigurationWatcher interface and be able to
    // dynamically
    // get the services context root and build out this URI - for now it must be hard-coded
    private static final String METRICS_SERVICE_BASE_URL = "/services/internal/metrics";

    public static final String NAME = "metrics";

    public static final String LABEL = "Metrics";

    private static final String DATE_DISPLAY_FORMAT = "L-";

    private static final int NUMBER_OF_WEEKLY_REPORTS = 4;
    private static final int NUMBER_OF_MONTHLY_REPORTS = 12;
    private static final int NUMBER_OF_YEARLY_REPORTS = 1;

    private BundleContext bundleContext;


    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void start() {
        super.activate(bundleContext);
        LOGGER.debug(LABEL + " plugin activated");
    }

    public void stop() {
        LOGGER.debug(LABEL + " plugin deactivated");
        super.deactivate();
    }

    @Override
    public String getTitle() {
        return LABEL;
    }

    @Override
    public String getLabel() {
        return NAME;
    }

    /**
     * Renders the Metrics tab in the Admin GUI console. Retrieves the list of all metrics being
     * collected in the system by invoking the {@link MetricsEndpoint}, and then parsing this JSON
     * response to create the table in the Metrics tab that consists of the list of metric names and
     * hyperlinks for each format for each time range that a metric's data can be retrieved.
     */
    @SuppressWarnings("rawtypes")
    @Override
    protected void renderContent(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        LOGGER.debug("ENTERING: renderContent");

        final PrintWriter pw = response.getWriter();

        String metricsServiceUrl = request.getScheme() + "://" + request.getServerName() + ":"
                + request.getServerPort() + METRICS_SERVICE_BASE_URL;

        // Call Metrics Endpoint REST service to get list of all of the monitored
        // metrics and their associated hyperlinks to graph their historical data.
        // Response is a JSON-formatted string.
        LOGGER.debug("(NEW) Creating WebClient to URI " + metricsServiceUrl);
        WebClient client = WebClient.create(metricsServiceUrl);
        client.accept("application/json");
        Response metricsListResponse = client.get();
        InputStream is = (InputStream) metricsListResponse.getEntity();
        LOGGER.debug("Response has this many bytes in it: " + is.available());
        String metricsList = IOUtils.toString(is);
        LOGGER.debug("metricsList = " + metricsList);

        JSONParser parser = new JSONParser();

        // Useful class for simple JSON to handle collections when parsing
        // (Called internally by the simple JSON parser.parse(...) method)
        ContainerFactory containerFactory = new ContainerFactory() {
            public List creatArrayContainer() {
                return new LinkedList();
            }

            public Map createObjectContainer() {
                return new LinkedHashMap();
            }
        };

        try {
            LOGGER.debug("Parsing JSON metricsList response");

            // JSON-formatted text will map to a Map1<String1, Map2<String2, Map3<String3,String4>>>
            // (Numbers added to end of Map and String types so that each position could be referred
            // to)
            // String1 = metric name, e.g., "catalogQueries"
            // Map2 = mapping of time range to it list of hyperlinks
            // String2 = time range, e.g., "15m"
            // Map3 = hyperlink for each format type (e.g., PNG) for each time range for each metric
            // String3 = display text for hyperlink, e.g., PNG
            // String4 = hyperlink for metric data in specific format, e.g.,
            // http://host:port/.../catalogQueries.png?dateOffset=900
            Map<String, Map<String, Map<String, String>>> json = new TreeMap(
                    (Map<String, Map<String, Map<String, String>>>) parser.parse(metricsList,
                            containerFactory));
            Iterator iter = json.entrySet().iterator();

            // Create HTML table of Metric Name and hyperlinks to its associated
            // RRD graphs
            pw.println("<table class=\"nicetable\">");
            pw.println("<tr>");
            pw.println("<th>Metric</th>");

            // Column headers for the time ranges, e.g., 15m, 1h, 4h, etc.
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                Map<String, Map<String, String>> timeRangeData = (Map<String, Map<String, String>>) entry
                        .getValue();
                Set<String> timeRanges = timeRangeData.keySet();
                for (String timeRange : timeRanges) {
                    pw.println("<th>" + timeRange + "</th>");
                }
                break; // only need one set of time ranges for column headers
            }
            pw.println("</tr>");

            // List of metric names and associated hyperlinks per format per time range
            int rowCount = 1;
            iter = json.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String metricName = (String) entry.getKey();
                String tableStriping = "odd";
                if ((rowCount % 2) == 0) {
                    tableStriping = "even";
                }
                pw.println("<tr class=\"" + tableStriping + " ui-state-default\">");
                pw.println("<td>" + convertCamelCase(metricName) + "</td>");
                Map<String, Map<String, String>> timeRangeData = (Map<String, Map<String, String>>) entry
                        .getValue();

                Iterator metricDataIter = timeRangeData.entrySet().iterator();
                while (metricDataIter.hasNext()) {
                    Map.Entry entry2 = (Map.Entry) metricDataIter.next();
                    String timeRange = (String) entry2.getKey();

                    // For each time range (for each metric), a list of display text and its
                    // associated hyperlink
                    // is provided, e.g., "1h" and its associated hyperlink with the
                    // corresponding dateOffset (in seconds) from current time.
                    // Example:
                    // http://<host>:<port>/services/internal/metrics?dateOffset=3600
                    //
                    // key=display text
                    // value=URLs
                    Map<String, String> metricUrls = (Map<String, String>) entry2.getValue();
                    LOGGER.debug(timeRange + " -> " + metricUrls);
                    pw.println("<td>");

                    Iterator metricUrlsIter = metricUrls.entrySet().iterator();
                    while (metricUrlsIter.hasNext()) {
                        Map.Entry metricUrl = (Map.Entry) metricUrlsIter.next();
                        String metricUrlCell = "<a class=\"ui-state-default ui-corner-all\" href=\""
                                + metricUrl.getValue() + "\">" + metricUrl.getKey() + "</a>&nbsp;";
                        pw.println(metricUrlCell);
                    }
                    pw.println("</td>");
                }
                pw.println("</tr>");
                rowCount++;
            }

            LOGGER.debug("==  toJSONString()  ==");
            LOGGER.debug(JSONValue.toJSONString(json));

            // blank line for spacing between tables
            pw.println("<tr><td colspan=\"3\">&nbsp;</td></tr>");

            // Create HTML table for aggregate reports
            pw.println("<tr>");
            pw.println("<th colspan=\"3\">Weekly Reports</th>");
            pw.println("</tr>");
            addWeeklyReportUrls(pw, NUMBER_OF_WEEKLY_REPORTS, metricsServiceUrl);

            // blank line for spacing between tables
            pw.println("<tr><td colspan=\"3\">&nbsp;</td></tr>");

            pw.println("<tr>");
            pw.println("<th colspan=\"3\">Monthly Reports</th>");
            pw.println("</tr>");

            addMonthlyReportUrls(pw, NUMBER_OF_MONTHLY_REPORTS, metricsServiceUrl);

            // blank line for spacing between tables
            pw.println("<tr><td colspan=\"3\">&nbsp;</td></tr>");

            pw.println("<tr>");
            pw.println("<th colspan=\"3\">Yearly Reports</th>");
            pw.println("</tr>");

            addYearlyReportUrls(pw, NUMBER_OF_YEARLY_REPORTS, metricsServiceUrl);

            // blank line for spacing between tables
            pw.println("<tr><td colspan=\"3\">&nbsp;</td></tr>");

            pw.println("</table>");
        } catch (ParseException pe) {
            LOGGER.warn(pe);
        }

        LOGGER.debug("EXITING: renderContent");
    }

    /**
     * Convert string, if it is in camelCase, to individual words with each word starting with a
     * capital letter
     */
    String convertCamelCase(String input) {
        String[] parts = StringUtils.splitByCharacterTypeCamelCase(input);
        String convertedStr = StringUtils.join(parts, " ");
        convertedStr = StringUtils.capitalize(convertedStr);

        return convertedStr;
    }

    void addWeeklyReportUrls(PrintWriter pw, int numWeeklyReports, String metricsServiceUrl) {
        DateTime input = new DateTime();
        LOGGER.debug("NOW:  " + input);

        for (int i = 1; i <= numWeeklyReports; i++) {
            try {
                DateMidnight startOfLastWeek = new DateMidnight(input.minusWeeks(i).withDayOfWeek(
                        DateTimeConstants.MONDAY));
                String startDate = urlEncodeDate(startOfLastWeek);
                LOGGER.debug("Previous Week " + i + "(start):  " + startDate);

                DateTime endOfLastWeek = startOfLastWeek.plusDays(DateTimeConstants.DAYS_PER_WEEK)
                        .toDateTime().minus(1 /* millisecond */);
                String endDate = urlEncodeDate(endOfLastWeek);
                LOGGER.debug("Previous Week " + i + " (end):  " + endDate);

                startTableRow(pw, i);
                addCellLabelForRange(pw, startOfLastWeek, endOfLastWeek);
                addXLSCellLink(pw, startDate, endDate, metricsServiceUrl);
                addPPTCellLink(pw, startDate, endDate, metricsServiceUrl);
                endTableRow(pw);
            } catch (UnsupportedEncodingException e) {
                LOGGER.info(e);
            }
        }
    }

    void addMonthlyReportUrls(PrintWriter pw, int numMonthlyReports, String metricsServiceUrl) {
        DateTime input = new DateTime();
        LOGGER.debug("NOW:  " + input);

        for (int i = 1; i <= numMonthlyReports; i++) {
            try {
                DateMidnight startOfLastMonth = new DateMidnight(input.minusMonths(i)
                        .withDayOfMonth(1));
                String startDate = urlEncodeDate(startOfLastMonth);
                LOGGER.debug("Previous Month (start):  " + startDate + "   (ms = "
                        + startOfLastMonth.getMillis() + ")");

                DateTime endOfLastMonth = startOfLastMonth.plusMonths(1).toDateTime().minus(1 /* millisecond */);
                String endDate = urlEncodeDate(endOfLastMonth);
                LOGGER.debug("Previous Month (end):  " + endOfLastMonth + "   (ms = "
                        + endOfLastMonth.getMillis() + ")");

                startTableRow(pw, i);
                addCellLabelForRange(pw, startOfLastMonth, endOfLastMonth);
                addXLSCellLink(pw, startDate, endDate, metricsServiceUrl);
                addPPTCellLink(pw, startDate, endDate, metricsServiceUrl);
                endTableRow(pw);
            } catch (UnsupportedEncodingException e) {
                LOGGER.info(e);
            }
        }
    }

    void addYearlyReportUrls(PrintWriter pw, int numYearlyReports, String metricsServiceUrl) {
        DateTime input = new DateTime();
        LOGGER.debug("NOW:  " + input);

        for (int i = 1; i <= numYearlyReports; i++) {
            try {
                DateMidnight startOfLastYear = new DateMidnight(input.minusYears(1)
                        .withDayOfYear(1));
                String startDate = urlEncodeDate(startOfLastYear);
                LOGGER.debug("Previous Year (start):  " + startOfLastYear + "   (ms = "
                        + startOfLastYear.getMillis() + ")");

                DateTime endOfLastYear = startOfLastYear.plusYears(1).toDateTime().minus(1 /* millisecond */);
                String endDate = urlEncodeDate(endOfLastYear);
                LOGGER.debug("Previous Year (end):  " + endOfLastYear + "   (ms = "
                        + endOfLastYear.getMillis() + ")");

                String urlText = startOfLastYear.toString("yyyy");
                LOGGER.debug("URL text = [" + urlText + "]");

                startTableRow(pw, i);
                addCellLabel(pw, urlText);
                addXLSCellLink(pw, startDate, endDate, metricsServiceUrl);
                addPPTCellLink(pw, startDate, endDate, metricsServiceUrl);
                endTableRow(pw);
            } catch (UnsupportedEncodingException e) {
                LOGGER.info(e);
            }
        }
    }

    private void startTableRow(PrintWriter pw, int rowNumber) {
        String tableStriping = "odd";
        if ((rowNumber % 2) == 0) {
            tableStriping = "even";
        }
        pw.println("<tr class=\"" + tableStriping + " ui-state-default\">");
    }

    private void addCellLabelForRange(PrintWriter pw, DateMidnight startDate, DateTime endDate) {
        DateTimeFormatter dateFormatter = DateTimeFormat.forStyle(DATE_DISPLAY_FORMAT);
        String urlText = dateFormatter.print(startDate) + " - " + dateFormatter.print(endDate);
        LOGGER.debug("URL text = [" + urlText + "]");
        addCellLabel(pw, urlText);
    }

    private void addCellLabel(PrintWriter pw, String cellLabel) {
        pw.println("<td>" + cellLabel + "</td>");
    }

    private void addXLSCellLink(PrintWriter pw, String startDate, String endDate, String metricsServiceUrl) {
        addCellLink(pw, startDate, endDate, metricsServiceUrl, "XLS");
    }

    private void addPPTCellLink(PrintWriter pw, String startDate, String endDate, String metricsServiceUrl) {
        addCellLink(pw, startDate, endDate, metricsServiceUrl, "PPT");
    }

    private void addCellLink(PrintWriter pw, String startDate, String endDate, String metricsServiceUrl, String extension) {
        String reportUrl = metricsServiceUrl + "/report." + extension.toLowerCase() + "?startDate=" + startDate + "&endDate=" + endDate;
        pw.println("<td><a class=\"ui-state-default ui-corner-all\" href=\"" + reportUrl + "\">" + extension + "</a></td>");
    }

    private void endTableRow(PrintWriter pw) {
        pw.println("</tr>");
    }

    private static String urlEncodeDate(DateMidnight date) throws UnsupportedEncodingException {
        return urlEncodeDate(date.toDateTime());
    }

    private static String urlEncodeDate(DateTime date) throws UnsupportedEncodingException {
        return URLEncoder.encode(date.toString(ISODateTimeFormat.dateTimeNoMillis()), CharEncoding.UTF_8);
    }
}
