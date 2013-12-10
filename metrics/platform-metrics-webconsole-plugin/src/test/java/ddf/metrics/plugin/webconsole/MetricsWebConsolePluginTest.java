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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.custommonkey.xmlunit.HTMLDocumentBuilder;
import org.custommonkey.xmlunit.TolerantSaxDocumentBuilder;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.w3c.dom.Document;

public class MetricsWebConsolePluginTest extends XMLTestCase {
    
    private static final String REPORT_FIRST_ROW_DATE_RANGE_XPATH = "//tr[1]/td[1]";
    private static final String REPORT_FIRST_ROW_XLS_XPATH = "//tr[1]/td[2]";
    private static final String REPORT_FIRST_ROW_PPT_XPATH = "//tr[1]/td[3]";
    private static final String REPORT_FIRST_ROW_XLS_HYPERLINK_XPATH = 
            REPORT_FIRST_ROW_XLS_XPATH + "/a";
    private static final String REPORT_FIRST_ROW_XLS_HREF_XPATH = 
            REPORT_FIRST_ROW_XLS_HYPERLINK_XPATH + "/@href";
    private static final String REPORT_FIRST_ROW_PPT_HYPERLINK_XPATH = 
            REPORT_FIRST_ROW_PPT_XPATH + "/a";
    private static final String REPORT_FIRST_ROW_PPT_HREF_XPATH = 
            REPORT_FIRST_ROW_PPT_HYPERLINK_XPATH + "/@href";
    private static final String XLS_FILE_EXTENSION = ".xls";
    private static final String PPT_FILE_EXTENSION = ".ppt";
    
    private static XpathEngine xpathEngine;
    
    
    @Test
    public void testTitle() throws Exception {
        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();
        assertEquals("Metrics", metricsPlugin.getTitle());
    }

    @Test
    public void testLabel() throws Exception {
        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();
        assertEquals("metrics", metricsPlugin.getLabel());
    }

    @Test
    public void testConvertCamelCase() throws Exception {
        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();
        assertEquals("Foo", metricsPlugin.convertCamelCase("foo"));
        assertEquals("Foo", metricsPlugin.convertCamelCase("Foo"));
        assertEquals("Foobar", metricsPlugin.convertCamelCase("foobar"));
        assertEquals("Foo Bar", metricsPlugin.convertCamelCase("fooBar"));
    }

    @Test
    public void testAddWeeklyReportUrlsAU() throws Exception {
        String html = getWeeklyReportUrls("Etc/GMT-8", "en", "AU");
        Document wellFormedDocument = getDocument(html);
        
        verifyWeeklyReportContent(wellFormedDocument, "3", "4 March 2013 - 10 March 2013",
            "startDate=2013-03-04T00%3A00%3A00%2B08%3A00",
            "endDate=2013-03-10T23%3A59%3A59%2B08%3A00");
    }

    @Test
    public void testAddWeeklyReportUrlsUS() throws Exception {
        String html = getWeeklyReportUrls("Etc/GMT+7", "en", "US");
        Document wellFormedDocument = getDocument(html);
        
        verifyWeeklyReportContent(wellFormedDocument, "3", "March 4, 2013 - March 10, 2013",
                "startDate=2013-03-04T00%3A00%3A00-07%3A00",
                "endDate=2013-03-10T23%3A59%3A59-07%3A00");
    }

    @Test
    public void testAddWeeklyReportUrlsDE() throws Exception {
        String html = getWeeklyReportUrls("Etc/GMT-1", "de", "DE");
        Document wellFormedDocument = getDocument(html);
        
        verifyWeeklyReportContent(wellFormedDocument, "3", "4. März 2013 - 10. März 2013",
                "startDate=2013-03-04T00%3A00%3A00%2B01%3A00",
                "endDate=2013-03-10T23%3A59%3A59%2B01%3A00");
    }

    @Test
    public void testAddMonthlyReportUrlsAU() throws Exception {
        String html = getMonthlyReportUrls("Etc/GMT-8", "en", "AU");
        Document wellFormedDocument = getDocument(html);
        
        verifyMonthlyReportContent(wellFormedDocument, "4", "1 March 2013 - 31 March 2013",
                "startDate=2013-03-01T00%3A00%3A00%2B08%3A00",
                "endDate=2013-03-31T23%3A59%3A59%2B08%3A00");
    }

    @Test
    public void testAddMonthlyReportUrlsDE() throws Exception {
        String html = getMonthlyReportUrls("Etc/GMT-1", "de", "DE");
        Document wellFormedDocument = getDocument(html);
        
        verifyMonthlyReportContent(wellFormedDocument, "4", "1. März 2013 - 31. März 2013",
                "startDate=2013-03-01T00%3A00%3A00%2B01%3A00",
                "endDate=2013-03-31T23%3A59%3A59%2B01%3A00");
    }

    @Test
    public void testAddMonthlyReportUrlsUS() throws Exception {
        String html = getMonthlyReportUrls("Etc/GMT+7", "en", "US");
        Document wellFormedDocument = getDocument(html);
        
        verifyMonthlyReportContent(wellFormedDocument, "4", "March 1, 2013 - March 31, 2013",
                "startDate=2013-03-01T00%3A00%3A00-07%3A00",
                "endDate=2013-03-31T23%3A59%3A59-07%3A00");
    }
    
/**************************************************************************************************/    
    
    private String getWeeklyReportUrls(String dateTimeZoneId, String language, String country) {
        
        DateTimeZone defaultDTZ = DateTimeZone.getDefault();
        DateTimeZone.setDefault(DateTimeZone.forID(dateTimeZoneId));
        Locale.setDefault(new Locale(language, country));
        StringWriter sw = new StringWriter();
        
        MetricsWebConsolePlugin metricsPlugin = new MetricsWebConsolePlugin();
        PrintWriter pw = new PrintWriter(sw);

        int numWeeklyReports = 3;
        String url = "http://ddf.example.local/metrics/weekly";

        org.joda.time.DateTime testDate = new org.joda.time.DateTime(2013, 3, 12, 9, 45, 39, 0);
        org.joda.time.DateTimeUtils.setCurrentMillisFixed(testDate.getMillis());
        metricsPlugin.addWeeklyReportUrls(pw, numWeeklyReports, url);
        DateTimeZone.setDefault(defaultDTZ);
        org.joda.time.DateTimeUtils.setCurrentMillisSystem();
        
        return sw.toString();
    }
    
    private String getMonthlyReportUrls(String dateTimeZoneId, String language, String country) {
        
        DateTimeZone defaultDTZ = DateTimeZone.getDefault();
        DateTimeZone.setDefault(DateTimeZone.forID(dateTimeZoneId));
        Locale.setDefault(new Locale(language, country));

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

        return sw.toString();
    }
    
    private Document getDocument(String html) throws Exception {

        // Convert generated HTML into well-formed XML Document
        TolerantSaxDocumentBuilder tolerantSaxDocumentBuilder =
            new TolerantSaxDocumentBuilder(XMLUnit.newTestParser());
        HTMLDocumentBuilder htmlDocumentBuilder =
            new HTMLDocumentBuilder(tolerantSaxDocumentBuilder);
        Document wellFormedDocument =
            htmlDocumentBuilder.parse(html);
        
        return wellFormedDocument;
    }
    
    private String getXml(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        String xml = writer.getBuffer().toString();
        
        return xml;
    }
    
    private void verifyWeeklyReportContent(Document doc, String expectedRowCount,
            String dateRange, String startDate, String endDate) throws Exception {
        
        xpathEngine = XMLUnit.newXpathEngine();
        
        assertThat(xpathEngine.evaluate("count(//tr)", doc), is(expectedRowCount));

        // Verify date formatting (if we check one row's date formatting we are assuming
        // any remaining rows are correctly formatted without adding assertions for each row)
        assertXpathEvaluatesTo(dateRange, 
                REPORT_FIRST_ROW_DATE_RANGE_XPATH, doc);
        assertThat(xpathEngine.evaluate(REPORT_FIRST_ROW_XLS_HREF_XPATH, 
                doc), 
                containsString(startDate));
        assertThat(xpathEngine.evaluate(REPORT_FIRST_ROW_XLS_HREF_XPATH, 
                doc), 
                containsString(endDate));
        
        // Verify text displayed for hyperlinks
        assertXpathEvaluatesTo("XLS", REPORT_FIRST_ROW_XLS_HYPERLINK_XPATH, 
                doc);
        assertXpathEvaluatesTo("PPT", REPORT_FIRST_ROW_PPT_HYPERLINK_XPATH, 
                doc);
        
        // Verify file extensions used in hyperlinks
        assertThat(xpathEngine.evaluate(REPORT_FIRST_ROW_XLS_HREF_XPATH, 
                doc), 
                containsString(XLS_FILE_EXTENSION));
        assertThat(xpathEngine.evaluate(REPORT_FIRST_ROW_PPT_HREF_XPATH,
                doc), 
                containsString(PPT_FILE_EXTENSION));
    }
    
    private void verifyMonthlyReportContent(Document doc, String expectedRowCount,
            String dateRange, String startDate, String endDate) throws Exception {
        
        xpathEngine = XMLUnit.newXpathEngine();
        
        assertThat(xpathEngine.evaluate("count(//tr)", doc), is(expectedRowCount));

        // Verify date formatting (if we check one row's date formatting we are assuming
        // any remaining rows are correctly formatted without adding assertions for each row)
        assertXpathEvaluatesTo(dateRange, 
                REPORT_FIRST_ROW_DATE_RANGE_XPATH, doc);
        assertThat(xpathEngine.evaluate(REPORT_FIRST_ROW_XLS_HREF_XPATH, 
                doc), 
                containsString(startDate));
        assertThat(xpathEngine.evaluate(REPORT_FIRST_ROW_XLS_HREF_XPATH, 
                doc), 
                containsString(endDate));
        
        // Verify text displayed for hyperlinks
        assertXpathEvaluatesTo("XLS", REPORT_FIRST_ROW_XLS_HYPERLINK_XPATH, 
                doc);
        assertXpathEvaluatesTo("PPT", REPORT_FIRST_ROW_PPT_HYPERLINK_XPATH, 
                doc);
        
        // Verify file extensions used in hyperlinks
        assertThat(xpathEngine.evaluate(REPORT_FIRST_ROW_XLS_HREF_XPATH, 
                doc), 
                containsString(XLS_FILE_EXTENSION));
        assertThat(xpathEngine.evaluate(REPORT_FIRST_ROW_PPT_HREF_XPATH,
                doc), 
                containsString(PPT_FILE_EXTENSION));
    }

}
