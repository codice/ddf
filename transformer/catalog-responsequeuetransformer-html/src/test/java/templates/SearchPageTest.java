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
package templates;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.SourceInfoRequestEnterprise;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

public class SearchPageTest {

    private static final String SCHEME = "http";

    private static final String HOST = "ddf.test.host";

    private static final int PORT = 8181;

    private static final String SCHEME_SPECIFIC_PART = SCHEME + "://" + HOST + ":" + PORT;

    private static final long HIT_COUNT = 1004;

    private static final long PAGE_SIZE = 5;

    private static final String URL = SCHEME_SPECIFIC_PART
            + "/services/catalog/query?format=querypage&count="
            + PAGE_SIZE
            + "&start=1&q=mace&temporalCriteria=absoluteTime&dtend=&dtend=&dtoffset=&offsetTime=&offsetTimeUnits=minutes&radius=14000&lat=45.2&lon=31.2&latitude=45.2&longitude=31.2&radiusValue=14&radiusUnits=kilometers&bbox=&west=&south=&east=&north=&type=Resource&typeList=Resource&src=ddf&federationSources=ddf";

    private static final String HTML_ACTION = "/someAction/html";

    private static final String METACARD_ACTION = "/someAction/metacard";

    private static final String RESOURCE_ACTION = "/someAction/resource";

    private static final String HEADER = "Test Header";

    private static final String FOOTER = "Test Footer";

    private static final String TITLE = "Test Title";

    private static final String LOCAL_ID = "localDDF";

    private static final String FED_SOURCE_1_ID = "fed1";

    private static final String FED_SOURCE_2_ID = "fed2";

    private static final String CONTENT_TYPE_1 = "contentType 1";

    private static final String CONTENT_TYPE_2 = "contentType 2";

    private static final String CONTENT_TYPE_3 = "contentType 3";

    private static final String METACARD_LOCATION = "POINT(1 2)";

    private static final String RESOURCE_SIZE = "10240";

    private static final String METACARD_TITLE = "Metacard";

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchPageTest.class);

    private static List<Metacard> metacards;

    private static Date runTimeDate = new Date();

    private static String generatedHtml;

    @BeforeClass
    public static void setUp() throws Exception {
        Configuration cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(new File("src/main/resources/templates"));
        cfg.setObjectWrapper(new DefaultObjectWrapper());

        Template temp = cfg.getTemplate("searchpage.ftl");

        Map<String, Object> root = new HashMap<String, Object>();
        root.put("request", createRequest());
        root.put("response", createResponse());
        root.put("exchange", createExchange());
        root.put("headers", createHeaders());

        Writer out = new StringWriter();
        temp.process(root, out);
        out.flush();

        generatedHtml = out.toString();
        System.out.println(generatedHtml);
    }

    @Test
    public void testHeader() {
        assertTrue(generatedHtml.contains("<div class=\"banner\">" + HEADER + "</div>"));
    }

    @Test
    public void testFooter() {
        assertTrue(generatedHtml.contains("<div class=\"navbar-fixed-bottom banner\">" + FOOTER
                + "</div>"));
    }

    @Test
    public void testSites() {
        // contains each site once and only once
        assertTrue(containsExactlyOnce(generatedHtml, "<option title=\"" + LOCAL_ID + "\">"
                + LOCAL_ID + "</option>"));
        assertTrue(containsExactlyOnce(generatedHtml, "<option title=\"" + FED_SOURCE_1_ID + "\">"
                + FED_SOURCE_1_ID + "</option>"));
        assertTrue(containsExactlyOnce(generatedHtml,
                "<option disabled=\"disabled\" class=\"disabled_option\" title=\""
                        + FED_SOURCE_2_ID + "\">" + FED_SOURCE_2_ID + "</option>"));
    }

    @Test
    public void testContentTypes() {
        assertTrue(containsExactlyOnce(generatedHtml, "<option>" + CONTENT_TYPE_1 + "</option>"));
        assertTrue(containsExactlyOnce(generatedHtml, "<option>" + CONTENT_TYPE_2 + "</option>"));
        assertTrue(containsExactlyOnce(generatedHtml, "<option>" + CONTENT_TYPE_3 + "</option>"));
    }

    @Test
    public void testHitCount() {
        assertTrue(generatedHtml
                .contains("<div class=\"resultsCount pull-left span6\" ><p class=\"lead\">Total Results: "
                        + NumberFormat.getIntegerInstance().format(HIT_COUNT) + " </p></div>"));
    }

    @Test
    public void testMetacardActionProvider() {
        int count = 0;
        for (Metacard metacard : getMetacards()) {
            if (count < PAGE_SIZE) {
                assertTrue(generatedHtml.contains("<a class=\"metacard-modal\" href=\""
                        + generateActionUrl(HTML_ACTION, metacard) + "\">" + metacard.getTitle()
                        + "</a>"));
            } else {
                assertFalse(generatedHtml.contains("<a href=\""
                        + generateActionUrl(HTML_ACTION, metacard) + "\">" + metacard.getTitle()
                        + "</a>"));
            }
            count++;
        }
    }

    @Test
    public void testResourceActionProvider() {
        int count = 0;
        for (Metacard metacard : getMetacards()) {
            if (null == metacard.getResourceURI() || count >= PAGE_SIZE && hasResource(metacard)) {
                assertFalse(generatedHtml.contains("<a href=\""
                        + generateActionUrl(RESOURCE_ACTION, metacard) + "\">"));
                assertFalse(generatedHtml
                        .contains("<div style=\"visibility: hidden;\" class=\"resourceSize\">"
                                + RESOURCE_SIZE + count + "</div>"));
            } else {
                assertTrue(generatedHtml.contains("<a href=\""
                        + generateActionUrl(RESOURCE_ACTION, metacard) + "\" target=\"_blank\">"));
                assertTrue(generatedHtml
                        .contains("<div style=\"visibility: hidden;\" class=\"resourceSize\">"
                                + RESOURCE_SIZE + count + "</div>"));
            }
            count++;
        }
    }

    @Test
    public void testDates() {
        String timeAsString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz").format(runTimeDate);

        int lastIndexCreated = -1;
        int lastIndexEffective = -1;
        int count = 0;
        for (Iterator<Metacard> itr = getMetacards().iterator(); itr.hasNext() && count < PAGE_SIZE; itr
                .next()) {
            count++;
            lastIndexCreated = generatedHtml.indexOf("Effective: " + timeAsString + "<br>",
                    lastIndexCreated + 1);
            lastIndexEffective = generatedHtml.indexOf("Received: " + timeAsString + "</td>",
                    lastIndexEffective + 1);

            assertTrue(lastIndexCreated != -1);
            assertTrue(lastIndexEffective != -1);
        }
    }

    @Test
    public void testPageCount() {
        boolean hasPages = (PAGE_SIZE < HIT_COUNT);

        if (hasPages) {
            assertTrue(generatedHtml.contains("<div class=\"pagination pull-right span6\">"));
            assertTrue(generatedHtml.contains("<li class=\"disabled\"><a href=\"" + URL
                    + "\">Prev</a></li>"));
            assertTrue(generatedHtml.contains("<li class=\"active\"><a href=\"" + URL
                    + "\">1</a></li>"));
            assertTrue(generatedHtml.contains(">Next</a></li>"));

            int pages = (int) Math.ceil(((double) HIT_COUNT) / ((double) PAGE_SIZE));

            for (int i = 2; i <= pages && i <= 4; i++) {
                assertTrue(generatedHtml.contains(">" + i + "</a></li>"));
            }

        } else {
            assertFalse(generatedHtml.contains("<div class=\"pagination pull-right span6\">"));
        }
    }

    private boolean containsExactlyOnce(String string, String fragment) {
        return string.contains(fragment)
                && (string.indexOf(fragment) == string.lastIndexOf(fragment));
    }

    private static Message createRequest() {
        Message message = mock(Message.class);
        SourceResponse sourceResponse = mock(SourceResponse.class);
        when(sourceResponse.getHits()).thenReturn(new Long(HIT_COUNT));

        List<Result> results = new ArrayList<Result>();

        int count = 0;
        for (Metacard metacard : getMetacards()) {
            Result result = mock(Result.class);
            when(result.getMetacard()).thenReturn(metacard);
            results.add(result);
            count++;
            if (count >= PAGE_SIZE) {
                break;
            }
        }

        when(sourceResponse.getResults()).thenReturn(results);

        when(message.getBody()).thenReturn(sourceResponse);
        return message;
    }

    private static Message createResponse() {
        Message message = mock(Message.class);
        return message;
    }

    private static Map<String, String> createHeaders() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("url", URL);
        return headers;
    }

    private static boolean hasResource(Metacard metacard) {
        return hasResource(metacard.getTitle());
    }

    private static boolean hasResource(String title) {
        int index = Integer.parseInt(title.split(" ")[1]);
        return hasResource(index);
    }

    private static boolean hasResource(int i) {
        return 0 == (i % 2);
    }

    private static List<Metacard> getMetacards() {
        if (null == metacards) {
            metacards = new ArrayList<Metacard>();
            for (int i = 0; i < HIT_COUNT; i++) {
                Metacard metacard = mock(Metacard.class);
                when(metacard.getSourceId()).thenReturn(LOCAL_ID);
                when(metacard.getLocation()).thenReturn(METACARD_LOCATION);
                when(metacard.getTitle()).thenReturn(METACARD_TITLE + " " + i);
                when(metacard.getEffectiveDate()).thenReturn(runTimeDate);
                when(metacard.getCreatedDate()).thenReturn(runTimeDate);
                if (hasResource(i)) { // just give even numbered metacards resources, any uri is
                                      // fine
                    try {
                        URI uri = generateActionUrl(RESOURCE_ACTION, metacard).toURI();
                        when(metacard.getResourceURI()).thenReturn(uri);
                        when(metacard.getResourceSize()).thenReturn(RESOURCE_SIZE + i);
                    } catch (URISyntaxException e) {
                    }
                }

                metacards.add(metacard);
            }
        }
        return metacards;
    }

    private static Exchange createExchange() {

        Set<ContentType> contentTypes = new HashSet<ContentType>();
        ContentType a = mock(ContentType.class);
        when(a.getName()).thenReturn(CONTENT_TYPE_1);
        ContentType b = mock(ContentType.class);
        when(b.getName()).thenReturn(CONTENT_TYPE_2);
        ContentType c = mock(ContentType.class);
        when(c.getName()).thenReturn(CONTENT_TYPE_3);
        ContentType d = mock(ContentType.class);
        when(d.getName()).thenReturn(CONTENT_TYPE_3); // duplicate should be
                                                      // filtered

        contentTypes.add(a);
        contentTypes.add(b);
        contentTypes.add(c);
        contentTypes.add(d);

        List<ActionProvider> htmlActionProviderList = new ArrayList<ActionProvider>();
        htmlActionProviderList.add(buildActionProvider(HTML_ACTION));
        List<ActionProvider> metacardActionProviderList = new ArrayList<ActionProvider>();
        metacardActionProviderList.add(buildActionProvider(METACARD_ACTION));
        List<ActionProvider> thumbnailActionProviderList = new ArrayList<ActionProvider>();
        List<ActionProvider> resourceActionProviderList = new ArrayList<ActionProvider>();
        resourceActionProviderList.add(buildActionProvider(RESOURCE_ACTION, true));

        SourceInfoRequestEnterprise sourceInfoRequest = mock(SourceInfoRequestEnterprise.class);
        CatalogFramework catalog = mock(CatalogFramework.class);
        SourceInfoResponse sourceInfoResponse = mock(SourceInfoResponse.class);

        Set<SourceDescriptor> srcSet = new HashSet<SourceDescriptor>();
        when(catalog.getId()).thenReturn(LOCAL_ID);
        SourceDescriptor localSrc = mock(SourceDescriptor.class);
        when(localSrc.getSourceId()).thenReturn(LOCAL_ID);
        when(localSrc.isAvailable()).thenReturn(true);
        when(localSrc.getContentTypes()).thenReturn(contentTypes);
        srcSet.add(localSrc);
        SourceDescriptor fedSource1 = mock(SourceDescriptor.class);
        when(fedSource1.getSourceId()).thenReturn(FED_SOURCE_1_ID);
        when(fedSource1.isAvailable()).thenReturn(true);
        srcSet.add(fedSource1);
        srcSet.add(fedSource1); // add it twice, should be filtered
        SourceDescriptor fedSource2 = mock(SourceDescriptor.class);
        when(fedSource2.getSourceId()).thenReturn(FED_SOURCE_2_ID);
        when(fedSource2.getContentTypes()).thenReturn(new HashSet<ContentType>());
        when(fedSource2.isAvailable()).thenReturn(false);
        srcSet.add(fedSource2);

        when(sourceInfoResponse.getSourceInfo()).thenReturn(srcSet);
        try {
            when(catalog.getSourceInfo(sourceInfoRequest)).thenReturn(sourceInfoResponse);
        } catch (SourceUnavailableException e) {
        }

        BeansWrapper beansWrapper = new BeansWrapper();

        Exchange exchange = mock(Exchange.class);
        when(exchange.getProperty("title")).thenReturn(TITLE);
        when(exchange.getProperty("header")).thenReturn(HEADER);
        when(exchange.getProperty("footer")).thenReturn(FOOTER);
        when(exchange.getProperty("catalog")).thenReturn(catalog);
        when(exchange.getProperty("beansWrapper")).thenReturn(beansWrapper);
        when(exchange.getProperty("sourceInfoReqEnterprise")).thenReturn(sourceInfoRequest);
        when(exchange.getProperty("htmlActionProviderList")).thenReturn(htmlActionProviderList);
        when(exchange.getProperty("metacardActionProviderList")).thenReturn(
                metacardActionProviderList);
        when(exchange.getProperty("thumbnailActionProviderList")).thenReturn(
                thumbnailActionProviderList);
        when(exchange.getProperty("resourceActionProviderList")).thenReturn(
                resourceActionProviderList);

        return exchange;
    }

    private static ActionProvider buildActionProvider(String actionStr) {
        return buildActionProvider(actionStr, false);
    }

    private static ActionProvider buildActionProvider(String actionStr, boolean halfNull) {
        ActionProvider metacardAction = mock(ActionProvider.class);
        boolean returnNull = false;
        for (Metacard metacard : getMetacards()) {
            Action action = mock(Action.class);
            URL url = generateActionUrl(actionStr, metacard);
            when(action.getUrl()).thenReturn((halfNull && returnNull) ? null : url);
            when(metacardAction.getAction(metacard)).thenReturn(action);
            returnNull = !returnNull;
        }
        return metacardAction;
    }

    private static java.net.URL generateActionUrl(String actionStr, Metacard metacard) {
        try {
            return new URI(SCHEME, null, HOST, PORT, actionStr, null, metacard.getTitle()).toURL();
        } catch (MalformedURLException e) {
            LOGGER.error("Malformed URL", e);
            return null;
        } catch (URISyntaxException e) {
            LOGGER.error("URI Syntax error", e);
            return null;
        }
    }
}
