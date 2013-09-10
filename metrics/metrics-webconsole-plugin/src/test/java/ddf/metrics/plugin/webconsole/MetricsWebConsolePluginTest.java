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
package ddf.metrics.plugin.webconsole;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import junit.framework.TestCase;

import org.joda.time.DateTimeConstants;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;

public class MetricsWebConsolePluginTest extends TestCase {
    public void testTitle() throws Exception {
        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();
        assertEquals("Metrics", metricsPlugin.getTitle());
    }

    public void testLabel() throws Exception {
        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();
        assertEquals("metrics", metricsPlugin.getLabel());
    }

    public void testConvertCamelCase() throws Exception {
        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();
        assertEquals("Foo", metricsPlugin.convertCamelCase("foo"));
        assertEquals("Foo", metricsPlugin.convertCamelCase("Foo"));
        assertEquals("Foobar", metricsPlugin.convertCamelCase("foobar"));
        assertEquals("Foo Bar", metricsPlugin.convertCamelCase("fooBar"));
    }

    public void testCellHeader() throws Exception {
        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        metricsPlugin.addCellLabel(pw, "label1");
        assertEquals("<td>label1</td>\n", sw.toString());
    }

    public void testEndTableRow() throws Exception {
        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        metricsPlugin.endTableRow(pw);
        assertEquals("</tr>\n", sw.toString());
    }

    public void testAddPptLink() throws Exception {
        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        metricsPlugin.addPPTCellLink(pw, "2013-04-01", "2013-04-30", "http://www.aprilfools.local/test/ppt");
        assertEquals("<td><a class=\"ui-state-default ui-corner-all\" href=\"http://www.aprilfools.local/test/ppt/report.ppt?startDate=2013-04-01&endDate=2013-04-30\">PPT</a></td>\n", sw.toString());
    }

    public void testAddXlsLink() throws Exception {
        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        metricsPlugin.addXLSCellLink(pw, "2013-04-01", "2013-04-30", "http://www.aprilfools.local/test/xls");
        assertEquals("<td><a class=\"ui-state-default ui-corner-all\" href=\"http://www.aprilfools.local/test/xls/report.xls?startDate=2013-04-01&endDate=2013-04-30\">XLS</a></td>\n", sw.toString());
    }

    public void testAddCellLabelForRangeAustralia() throws Exception {
        Locale.setDefault(new Locale("en", "AU"));
        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        DateMidnight start = new DateMidnight(2013, 4, 1);
        DateTime end = start.plusDays(DateTimeConstants.DAYS_PER_WEEK).toDateTime().minus(1 /* millisecond */);
        metricsPlugin.addCellLabelForRange(pw, start, end);
        assertEquals("<td>1 April 2013 - 7 April 2013</td>\n", sw.toString());
    }

    public void testAddCellLabelForRangeUK() throws Exception {
        Locale.setDefault(new Locale("en", "GB"));
        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        DateMidnight start = new DateMidnight(2013, 4, 10);
        DateTime end = start.plusDays(DateTimeConstants.DAYS_PER_WEEK).toDateTime().minus(1 /* millisecond */);
        metricsPlugin.addCellLabelForRange(pw, start, end);
        assertEquals("<td>10 April 2013 - 16 April 2013</td>\n", sw.toString());
    }

    public void testAddCellLabelForRangeUS() throws Exception {
        Locale.setDefault(new Locale("en", "US"));
        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        DateMidnight start = new DateMidnight(2013, 4, 1);
        DateTime end = start.plusDays(DateTimeConstants.DAYS_PER_WEEK).toDateTime().minus(1 /* millisecond */);
        metricsPlugin.addCellLabelForRange(pw, start, end);
        assertEquals("<td>April 1, 2013 - April 7, 2013</td>\n", sw.toString());
    }

    public void testAddCellLabelForRangeFrance() throws Exception {
        Locale.setDefault(new Locale("fr", "FR"));
        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        DateMidnight start = new DateMidnight(2013, 4, 1);
        DateTime end = start.plusDays(DateTimeConstants.DAYS_PER_WEEK).toDateTime().minus(1 /* millisecond */);
        metricsPlugin.addCellLabelForRange(pw, start, end);
        assertEquals("<td>1 avril 2013 - 7 avril 2013</td>\n", sw.toString());
    }

    public void testAddCellLabelForRangeGermany() throws Exception {
        Locale.setDefault(new Locale("de", "DE"));
        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        DateMidnight start = new DateMidnight(2013, 4, 1);
        DateTime end = start.plusDays(DateTimeConstants.DAYS_PER_WEEK).toDateTime().minus(1 /* millisecond */);
        metricsPlugin.addCellLabelForRange(pw, start, end);
        assertEquals("<td>1. April 2013 - 7. April 2013</td>\n", sw.toString());
    }

    public void testAddCellLabelForRangeGermanyMai() throws Exception {
        Locale.setDefault(new Locale("de", "DE"));
        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        DateMidnight start = new DateMidnight(2013, 5, 1);
        DateTime end = start.plusDays(DateTimeConstants.DAYS_PER_WEEK).toDateTime().minus(1 /* millisecond */);
        metricsPlugin.addCellLabelForRange(pw, start, end);
        assertEquals("<td>1. Mai 2013 - 7. Mai 2013</td>\n", sw.toString());
    }

    public void testAddCellLabelForRangeGermanyMarch() throws Exception {
        Locale.setDefault(new Locale("de", "DE"));
        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        DateMidnight start = new DateMidnight(2013, 3, 1);
        DateTime end = start.plusDays(DateTimeConstants.DAYS_PER_WEEK).toDateTime().minus(1 /* millisecond */);
        metricsPlugin.addCellLabelForRange(pw, start, end);
        assertEquals("<td>1. März 2013 - 7. März 2013</td>\n", sw.toString());
    }

    static String threeWeeklyReportsFor12March_en_AU = "<tr class=\"odd ui-state-default\">\n"
        + "<td>4 March 2013 - 10 March 2013</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/weekly/report.xls?startDate=2013-03-04T00%3A00%3A00%2B08%3A00&endDate=2013-03-10T23%3A59%3A59%2B08%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/weekly/report.ppt?startDate=2013-03-04T00%3A00%3A00%2B08%3A00&endDate=2013-03-10T23%3A59%3A59%2B08%3A00\">PPT</a></td>\n"
        + "</tr>\n"
        + "<tr class=\"even ui-state-default\">\n"
        + "<td>25 February 2013 - 3 March 2013</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/weekly/report.xls?startDate=2013-02-25T00%3A00%3A00%2B08%3A00&endDate=2013-03-03T23%3A59%3A59%2B08%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/weekly/report.ppt?startDate=2013-02-25T00%3A00%3A00%2B08%3A00&endDate=2013-03-03T23%3A59%3A59%2B08%3A00\">PPT</a></td>\n"
        + "</tr>\n"
        + "<tr class=\"odd ui-state-default\">\n"
        + "<td>18 February 2013 - 24 February 2013</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/weekly/report.xls?startDate=2013-02-18T00%3A00%3A00%2B08%3A00&endDate=2013-02-24T23%3A59%3A59%2B08%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/weekly/report.ppt?startDate=2013-02-18T00%3A00%3A00%2B08%3A00&endDate=2013-02-24T23%3A59%3A59%2B08%3A00\">PPT</a></td>\n"
        + "</tr>\n";

    static String threeWeeklyReportsFor12March_en_US = "<tr class=\"odd ui-state-default\">\n"
        + "<td>March 4, 2013 - March 10, 2013</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/weekly/report.xls?startDate=2013-03-04T00%3A00%3A00-07%3A00&endDate=2013-03-10T23%3A59%3A59-07%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/weekly/report.ppt?startDate=2013-03-04T00%3A00%3A00-07%3A00&endDate=2013-03-10T23%3A59%3A59-07%3A00\">PPT</a></td>\n"
        + "</tr>\n"
        + "<tr class=\"even ui-state-default\">\n"
        + "<td>February 25, 2013 - March 3, 2013</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/weekly/report.xls?startDate=2013-02-25T00%3A00%3A00-07%3A00&endDate=2013-03-03T23%3A59%3A59-07%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/weekly/report.ppt?startDate=2013-02-25T00%3A00%3A00-07%3A00&endDate=2013-03-03T23%3A59%3A59-07%3A00\">PPT</a></td>\n"
        + "</tr>\n"
        + "<tr class=\"odd ui-state-default\">\n"
        + "<td>February 18, 2013 - February 24, 2013</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/weekly/report.xls?startDate=2013-02-18T00%3A00%3A00-07%3A00&endDate=2013-02-24T23%3A59%3A59-07%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/weekly/report.ppt?startDate=2013-02-18T00%3A00%3A00-07%3A00&endDate=2013-02-24T23%3A59%3A59-07%3A00\">PPT</a></td>\n"
        + "</tr>\n";

    static String threeWeeklyReportsFor12March_de_DE = "<tr class=\"odd ui-state-default\">\n"
        + "<td>4. März 2013 - 10. März 2013</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/weekly/report.xls?startDate=2013-03-04T00%3A00%3A00%2B01%3A00&endDate=2013-03-10T23%3A59%3A59%2B01%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/weekly/report.ppt?startDate=2013-03-04T00%3A00%3A00%2B01%3A00&endDate=2013-03-10T23%3A59%3A59%2B01%3A00\">PPT</a></td>\n"
        + "</tr>\n"
        + "<tr class=\"even ui-state-default\">\n"
        + "<td>25. Februar 2013 - 3. März 2013</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/weekly/report.xls?startDate=2013-02-25T00%3A00%3A00%2B01%3A00&endDate=2013-03-03T23%3A59%3A59%2B01%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/weekly/report.ppt?startDate=2013-02-25T00%3A00%3A00%2B01%3A00&endDate=2013-03-03T23%3A59%3A59%2B01%3A00\">PPT</a></td>\n"
        + "</tr>\n"
        + "<tr class=\"odd ui-state-default\">\n"
        + "<td>18. Februar 2013 - 24. Februar 2013</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/weekly/report.xls?startDate=2013-02-18T00%3A00%3A00%2B01%3A00&endDate=2013-02-24T23%3A59%3A59%2B01%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/weekly/report.ppt?startDate=2013-02-18T00%3A00%3A00%2B01%3A00&endDate=2013-02-24T23%3A59%3A59%2B01%3A00\">PPT</a></td>\n"
        + "</tr>\n";

    public void testAddWeeklyReportUrls() throws Exception {
        DateTimeZone defaultDTZ = DateTimeZone.getDefault();
        DateTimeZone.setDefault(DateTimeZone.forID("Etc/GMT-8"));
        Locale.setDefault(new Locale("en", "AU"));

        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        int numWeeklyReports = 3;
        String url = "http://ddf.example.local/metrics/weekly";

        org.joda.time.DateTime testDate = new org.joda.time.DateTime(2013, 3, 12, 9, 45, 39, 0);
        org.joda.time.DateTimeUtils.setCurrentMillisFixed(testDate.getMillis());
        metricsPlugin.addWeeklyReportUrls(pw, numWeeklyReports, url);
        DateTimeZone.setDefault(defaultDTZ);
        org.joda.time.DateTimeUtils.setCurrentMillisSystem();

        assertEquals(threeWeeklyReportsFor12March_en_AU, sw.toString());
    }

    public void testAddWeeklyReportUrls_US() throws Exception {
        DateTimeZone defaultDTZ = DateTimeZone.getDefault();
        DateTimeZone.setDefault(DateTimeZone.forID("Etc/GMT+7"));
        Locale.setDefault(new Locale("en", "US"));

        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        int numWeeklyReports = 3;
        String url = "http://ddf.example.local/metrics/weekly";

        org.joda.time.DateTime testDate = new org.joda.time.DateTime(2013, 3, 12, 9, 45, 39, 0);
        org.joda.time.DateTimeUtils.setCurrentMillisFixed(testDate.getMillis());
        metricsPlugin.addWeeklyReportUrls(pw, numWeeklyReports, url);
        DateTimeZone.setDefault(defaultDTZ);
        org.joda.time.DateTimeUtils.setCurrentMillisSystem();

        assertEquals(threeWeeklyReportsFor12March_en_US, sw.toString());
    }

    public void testAddWeeklyReportUrls_DE() throws Exception {
        DateTimeZone defaultDTZ = DateTimeZone.getDefault();
        DateTimeZone.setDefault(DateTimeZone.forID("Etc/GMT-1"));
        Locale.setDefault(new Locale("de", "DE"));

        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        int numWeeklyReports = 3;
        String url = "http://ddf.example.local/metrics/weekly";

        org.joda.time.DateTime testDate = new org.joda.time.DateTime(2013, 3, 12, 9, 45, 39, 0);
        org.joda.time.DateTimeUtils.setCurrentMillisFixed(testDate.getMillis());
        metricsPlugin.addWeeklyReportUrls(pw, numWeeklyReports, url);
        DateTimeZone.setDefault(defaultDTZ);
        org.joda.time.DateTimeUtils.setCurrentMillisSystem();

        assertEquals(threeWeeklyReportsFor12March_de_DE, sw.toString());
    }

    static String fourMonthlyReports13April2013 = "<tr class=\"odd ui-state-default\">\n"
        + "<td>1 March 2013 - 31 March 2013</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.xls?startDate=2013-03-01T00%3A00%3A00%2B08%3A00&endDate=2013-03-31T23%3A59%3A59%2B08%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.ppt?startDate=2013-03-01T00%3A00%3A00%2B08%3A00&endDate=2013-03-31T23%3A59%3A59%2B08%3A00\">PPT</a></td>\n"
        + "</tr>\n"
        + "<tr class=\"even ui-state-default\">\n"
        + "<td>1 February 2013 - 28 February 2013</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.xls?startDate=2013-02-01T00%3A00%3A00%2B08%3A00&endDate=2013-02-28T23%3A59%3A59%2B08%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.ppt?startDate=2013-02-01T00%3A00%3A00%2B08%3A00&endDate=2013-02-28T23%3A59%3A59%2B08%3A00\">PPT</a></td>\n"
        + "</tr>\n"
        + "<tr class=\"odd ui-state-default\">\n"
        + "<td>1 January 2013 - 31 January 2013</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.xls?startDate=2013-01-01T00%3A00%3A00%2B08%3A00&endDate=2013-01-31T23%3A59%3A59%2B08%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.ppt?startDate=2013-01-01T00%3A00%3A00%2B08%3A00&endDate=2013-01-31T23%3A59%3A59%2B08%3A00\">PPT</a></td>\n"
        + "</tr>\n"
        + "<tr class=\"even ui-state-default\">\n"
        + "<td>1 December 2012 - 31 December 2012</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.xls?startDate=2012-12-01T00%3A00%3A00%2B08%3A00&endDate=2012-12-31T23%3A59%3A59%2B08%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.ppt?startDate=2012-12-01T00%3A00%3A00%2B08%3A00&endDate=2012-12-31T23%3A59%3A59%2B08%3A00\">PPT</a></td>\n"
        + "</tr>\n";

    static String fourMonthlyReports13April2013_de = "<tr class=\"odd ui-state-default\">\n"
        + "<td>1. März 2013 - 31. März 2013</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.xls?startDate=2013-03-01T00%3A00%3A00%2B01%3A00&endDate=2013-03-31T23%3A59%3A59%2B01%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.ppt?startDate=2013-03-01T00%3A00%3A00%2B01%3A00&endDate=2013-03-31T23%3A59%3A59%2B01%3A00\">PPT</a></td>\n"
        + "</tr>\n"
        + "<tr class=\"even ui-state-default\">\n"
        + "<td>1. Februar 2013 - 28. Februar 2013</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.xls?startDate=2013-02-01T00%3A00%3A00%2B01%3A00&endDate=2013-02-28T23%3A59%3A59%2B01%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.ppt?startDate=2013-02-01T00%3A00%3A00%2B01%3A00&endDate=2013-02-28T23%3A59%3A59%2B01%3A00\">PPT</a></td>\n"
        + "</tr>\n"
        + "<tr class=\"odd ui-state-default\">\n"
        + "<td>1. Januar 2013 - 31. Januar 2013</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.xls?startDate=2013-01-01T00%3A00%3A00%2B01%3A00&endDate=2013-01-31T23%3A59%3A59%2B01%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.ppt?startDate=2013-01-01T00%3A00%3A00%2B01%3A00&endDate=2013-01-31T23%3A59%3A59%2B01%3A00\">PPT</a></td>\n"
        + "</tr>\n"
        + "<tr class=\"even ui-state-default\">\n"
        + "<td>1. Dezember 2012 - 31. Dezember 2012</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.xls?startDate=2012-12-01T00%3A00%3A00%2B01%3A00&endDate=2012-12-31T23%3A59%3A59%2B01%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.ppt?startDate=2012-12-01T00%3A00%3A00%2B01%3A00&endDate=2012-12-31T23%3A59%3A59%2B01%3A00\">PPT</a></td>\n"
        + "</tr>\n";

    static String fourMonthlyReports13April2013_us = "<tr class=\"odd ui-state-default\">\n"
        + "<td>March 1, 2013 - March 31, 2013</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.xls?startDate=2013-03-01T00%3A00%3A00-07%3A00&endDate=2013-03-31T23%3A59%3A59-07%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.ppt?startDate=2013-03-01T00%3A00%3A00-07%3A00&endDate=2013-03-31T23%3A59%3A59-07%3A00\">PPT</a></td>\n"
        + "</tr>\n"
        + "<tr class=\"even ui-state-default\">\n"
        + "<td>February 1, 2013 - February 28, 2013</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.xls?startDate=2013-02-01T00%3A00%3A00-07%3A00&endDate=2013-02-28T23%3A59%3A59-07%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.ppt?startDate=2013-02-01T00%3A00%3A00-07%3A00&endDate=2013-02-28T23%3A59%3A59-07%3A00\">PPT</a></td>\n"
        + "</tr>\n"
        + "<tr class=\"odd ui-state-default\">\n"
        + "<td>January 1, 2013 - January 31, 2013</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.xls?startDate=2013-01-01T00%3A00%3A00-07%3A00&endDate=2013-01-31T23%3A59%3A59-07%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.ppt?startDate=2013-01-01T00%3A00%3A00-07%3A00&endDate=2013-01-31T23%3A59%3A59-07%3A00\">PPT</a></td>\n"
        + "</tr>\n"
        + "<tr class=\"even ui-state-default\">\n"
        + "<td>December 1, 2012 - December 31, 2012</td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.xls?startDate=2012-12-01T00%3A00%3A00-07%3A00&endDate=2012-12-31T23%3A59%3A59-07%3A00\">XLS</a></td>\n"
        + "<td><a class=\"ui-state-default ui-corner-all\" href=\"http://ddf.example.local/metrics/monthly/report.ppt?startDate=2012-12-01T00%3A00%3A00-07%3A00&endDate=2012-12-31T23%3A59%3A59-07%3A00\">PPT</a></td>\n"
        + "</tr>\n";

    public void testAddMonthlyReportUrls() throws Exception {
        DateTimeZone defaultDTZ = DateTimeZone.getDefault();
        DateTimeZone.setDefault(DateTimeZone.forID("Etc/GMT-8"));
        Locale.setDefault(new Locale("en", "AU"));

        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        int numMonthlyReports = 4;
        String url = "http://ddf.example.local/metrics/monthly";

        org.joda.time.DateTime testDate = new org.joda.time.DateTime(2013, 4, 13, 9, 45, 39, 0);
        org.joda.time.DateTimeUtils.setCurrentMillisFixed(testDate.getMillis());
        metricsPlugin.addMonthlyReportUrls(pw, numMonthlyReports, url);
        DateTimeZone.setDefault(defaultDTZ);
        org.joda.time.DateTimeUtils.setCurrentMillisSystem();

        assertEquals(fourMonthlyReports13April2013, sw.toString());
    }

    public void testAddMonthlyReportUrls_DE() throws Exception {
        DateTimeZone defaultDTZ = DateTimeZone.getDefault();
        DateTimeZone.setDefault(DateTimeZone.forID("Etc/GMT-1"));
        Locale.setDefault(new Locale("de", "DE"));

        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        int numMonthlyReports = 4;
        String url = "http://ddf.example.local/metrics/monthly";

        org.joda.time.DateTime testDate = new org.joda.time.DateTime(2013, 4, 13, 9, 45, 39, 0);
        org.joda.time.DateTimeUtils.setCurrentMillisFixed(testDate.getMillis());
        metricsPlugin.addMonthlyReportUrls(pw, numMonthlyReports, url);
        DateTimeZone.setDefault(defaultDTZ);
        org.joda.time.DateTimeUtils.setCurrentMillisSystem();

        assertEquals(fourMonthlyReports13April2013_de, sw.toString());
    }

    public void testAddMonthlyReportUrls_US() throws Exception {
        DateTimeZone defaultDTZ = DateTimeZone.getDefault();
        DateTimeZone.setDefault(DateTimeZone.forID("Etc/GMT+7"));
        Locale.setDefault(new Locale("en", "US"));

        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        int numMonthlyReports = 4;
        String url = "http://ddf.example.local/metrics/monthly";

        org.joda.time.DateTime testDate = new org.joda.time.DateTime(2013, 4, 13, 9, 45, 39, 0);
        org.joda.time.DateTimeUtils.setCurrentMillisFixed(testDate.getMillis());
        metricsPlugin.addMonthlyReportUrls(pw, numMonthlyReports, url);
        DateTimeZone.setDefault(defaultDTZ);
        org.joda.time.DateTimeUtils.setCurrentMillisSystem();

        assertEquals(fourMonthlyReports13April2013_us, sw.toString());
    }

}
