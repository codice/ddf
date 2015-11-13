/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.source.opensearch;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.util.ParameterParser;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opengis.filter.Filter;
import org.osgi.framework.InvalidSyntaxException;

import junit.framework.Assert;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.resource.impl.ResourceImpl;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.service.SecurityServiceException;

/**
 * Tests parts of the {@link OpenSearchSource}
 *
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 */
public class TestOpenSearchSource {

    private static final GeotoolsFilterAdapterImpl FILTER_ADAPTER = new GeotoolsFilterAdapterImpl();

    private static final String SAMPLE_ID = "abcdef12345678900987654321fedcba";

    private static final String SAMPLE_SEARCH_PHRASE = "foobar";

    private static final String BYTES_TO_SKIP = "BytesToSkip";

    private static final List<String> DEFAULT_PARAMETERS = Arrays.asList("q", "src", "mr", "start", "count", "mt", "dn",
            "lat", "lon", "radius", "bbox", "polygon", "dtstart", "dtend", "dateName", "filter", "sort");

    private static FilterBuilder filterBuilder = new GeotoolsFilterBuilder();

    private static InputStream getSampleAtomStream() {
        String response =
                "<feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:os=\"http://a9.com/-/spec/opensearch/1.1/\">\r\n"
                        + "    <title type=\"text\">Query Response</title>\r\n"
                        + "    <updated>2013-01-31T23:22:37.298Z</updated>\r\n"
                        + "    <id>urn:uuid:a27352c9-f935-45f0-9b8c-5803095164bb</id>\r\n"
                        + "    <link href=\"#\" rel=\"self\" />\r\n" + "    <author>\r\n"
                        + "        <name>Codice</name>\r\n" + "    </author>\r\n"
                        + "    <generator version=\"2.1.0.20130129-1341\">ddf123</generator>\r\n"
                        + "    <os:totalResults>1</os:totalResults>\r\n"
                        + "    <os:itemsPerPage>10</os:itemsPerPage>\r\n"
                        + "    <os:startIndex>1</os:startIndex>\r\n"
                        + "    <entry xmlns:relevance=\"http://a9.com/-/opensearch/extensions/relevance/1.0/\" xmlns:fs=\"http://a9.com/-/opensearch/extensions/federation/1.0/\"\r\n"
                        + "        xmlns:georss=\"http://www.georss.org/georss\">\r\n"
                        + "        <fs:resultSource fs:sourceId=\"ddf123\" />\r\n"
                        + "        <relevance:score>0.19</relevance:score>\r\n"
                        + "        <id>urn:catalog:id:ee7a161e01754b9db1872bfe39d1ea09</id>\r\n"
                        + "        <title type=\"text\">F-15 lands in Libya; Crew Picked Up</title>\r\n"
                        + "        <updated>2013-01-31T23:22:31.648Z</updated>\r\n"
                        + "        <published>2013-01-31T23:22:31.648Z</published>\r\n"
                        + "        <link href=\"http://123.45.67.123:8181/services/catalog/ddf123/ee7a161e01754b9db1872bfe39d1ea09\" rel=\"alternate\" title=\"View Complete Metacard\" />\r\n"
                        + "        <category term=\"Resource\" />\r\n"
                        + "        <georss:where xmlns:gml=\"http://www.opengis.net/gml\">\r\n"
                        + "            <gml:Point>\r\n"
                        + "                <gml:pos>32.8751900768792 13.1874561309814</gml:pos>\r\n"
                        + "            </gml:Point>\r\n" + "        </georss:where>\r\n"
                        + "        <content type=\"application/xml\">\r\n"
                        + "            <ns3:metacard xmlns:ns3=\"urn:catalog:metacard\" xmlns:ns2=\"http://www.w3.org/1999/xlink\" xmlns:ns1=\"http://www.opengis.net/gml\"\r\n"
                        + "                xmlns:ns4=\"http://www.w3.org/2001/SMIL20/\" xmlns:ns5=\"http://www.w3.org/2001/SMIL20/Language\" ns1:id=\"4535c53fc8bc4404a1d32a5ce7a29585\">\r\n"
                        + "                <ns3:type>ddf.metacard</ns3:type>\r\n"
                        + "                <ns3:source>ddf.distribution</ns3:source>\r\n"
                        + "                <ns3:geometry name=\"location\">\r\n"
                        + "                    <ns3:value>\r\n"
                        + "                        <ns1:Point>\r\n"
                        + "                            <ns1:pos>32.8751900768792 13.1874561309814</ns1:pos>\r\n"
                        + "                        </ns1:Point>\r\n"
                        + "                    </ns3:value>\r\n"
                        + "                </ns3:geometry>\r\n"
                        + "                <ns3:dateTime name=\"created\">\r\n"
                        + "                    <ns3:value>2013-01-31T16:22:31.648-07:00</ns3:value>\r\n"
                        + "                </ns3:dateTime>\r\n"
                        + "                <ns3:dateTime name=\"modified\">\r\n"
                        + "                    <ns3:value>2013-01-31T16:22:31.648-07:00</ns3:value>\r\n"
                        + "                </ns3:dateTime>\r\n"
                        + "                <ns3:stringxml name=\"metadata\">\r\n"
                        + "                    <ns3:value>\r\n"
                        + "                        <ns6:xml xmlns:ns6=\"urn:sample:namespace\" xmlns=\"urn:sample:namespace\">Example description.</ns6:xml>\r\n"
                        + "                    </ns3:value>\r\n"
                        + "                </ns3:stringxml>\r\n"
                        + "                <ns3:string name=\"metadata-content-type-version\">\r\n"
                        + "                    <ns3:value>myVersion</ns3:value>\r\n"
                        + "                </ns3:string>\r\n"
                        + "                <ns3:string name=\"metadata-content-type\">\r\n"
                        + "                    <ns3:value>myType</ns3:value>\r\n"
                        + "                </ns3:string>\r\n"
                        + "                <ns3:string name=\"title\">\r\n"
                        + "                    <ns3:value>Example title</ns3:value>\r\n"
                        + "                </ns3:string>\r\n" + "            </ns3:metacard>\r\n"
                        + "        </content>\r\n" + "    </entry>\r\n" + "</feed>";
        return new ByteArrayInputStream(response.getBytes());

    }

    private static InputStream getSampleRssStream() {
        String response =
                "<rss version=\"2.0\" xmlns:os=\"http://a9.com/-/spec/opensearch/1.1/\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\"><channel>\r\n"
                        + "    <title type=\"text\">Query Response</title>\r\n"
                        + "    <lastBuildDate>2013-01-31T23:22:37.298Z</lastBuildDate>\r\n"
                        + "    <guid>urn:uuid:a27352c9-f935-45f0-9b8c-5803095164bb</guid>\r\n"
                        + "    <link href=\"#\" rel=\"self\" />\r\n" + "    <managingEditor>\r\n"
                        + "        Codice\r\n" + "    </managingEditor>\r\n"
                        + "    <generator>ddf123</generator>\r\n"
                        + "    <os:totalResults>1</os:totalResults>\r\n"
                        + "    <os:itemsPerPage>10</os:itemsPerPage>\r\n"
                        + "    <os:startIndex>1</os:startIndex>\r\n"
                        + "    <item xmlns:relevance=\"http://a9.com/-/opensearch/extensions/relevance/1.0/\" xmlns:fs=\"http://a9.com/-/opensearch/extensions/federation/1.0/\"\r\n"
                        + "        xmlns:georss=\"http://www.georss.org/georss\">\r\n"
                        + "        <fs:resultSource fs:sourceId=\"ddf123\" />\r\n"
                        + "        <relevance:score>0.19</relevance:score>\r\n"
                        + "        <guid>urn:catalog:id:ee7a161e01754b9db1872bfe39d1ea09</guid>\r\n"
                        + "        <title type=\"text\">F-15 lands in Libya; Crew Picked Up</title>\r\n"
                        + "        <pubDate>2013-01-31T23:22:31.648Z</pubDate>\r\n"
                        + "        <link href=\"http://123.45.67.123:8181/services/catalog/ddf123/ee7a161e01754b9db1872bfe39d1ea09\" rel=\"alternate\" title=\"View Complete Metacard\" />\r\n"
                        + "        <category>Resource</category>\r\n"
                        + "        <georss:where xmlns:gml=\"http://www.opengis.net/gml\">\r\n"
                        + "            <gml:Point>\r\n"
                        + "                <gml:pos>32.8751900768792 13.1874561309814</gml:pos>\r\n"
                        + "            </gml:Point>\r\n" + "        </georss:where>\r\n"
                        + "        <content:encoded>\r\n"
                        + "            <![CDATA[<ns3:metacard xmlns:ns3=\"urn:catalog:metacard\" xmlns:ns2=\"http://www.w3.org/1999/xlink\" xmlns:ns1=\"http://www.opengis.net/gml\"\r\n"
                        + "                xmlns:ns4=\"http://www.w3.org/2001/SMIL20/\" xmlns:ns5=\"http://www.w3.org/2001/SMIL20/Language\" ns1:id=\"4535c53fc8bc4404a1d32a5ce7a29585\">\r\n"
                        + "                <ns3:type>ddf.metacard</ns3:type>\r\n"
                        + "                <ns3:source>ddf.distribution</ns3:source>\r\n"
                        + "                <ns3:geometry name=\"location\">\r\n"
                        + "                    <ns3:value>\r\n"
                        + "                        <ns1:Point>\r\n"
                        + "                            <ns1:pos>32.8751900768792 13.1874561309814</ns1:pos>\r\n"
                        + "                        </ns1:Point>\r\n"
                        + "                    </ns3:value>\r\n"
                        + "                </ns3:geometry>\r\n"
                        + "                <ns3:dateTime name=\"created\">\r\n"
                        + "                    <ns3:value>2013-01-31T16:22:31.648-07:00</ns3:value>\r\n"
                        + "                </ns3:dateTime>\r\n"
                        + "                <ns3:dateTime name=\"modified\">\r\n"
                        + "                    <ns3:value>2013-01-31T16:22:31.648-07:00</ns3:value>\r\n"
                        + "                </ns3:dateTime>\r\n"
                        + "                <ns3:stringxml name=\"metadata\">\r\n"
                        + "                    <ns3:value>\r\n"
                        + "                        <ns6:xml xmlns:ns6=\"urn:sample:namespace\" xmlns=\"urn:sample:namespace\">Example description.</ns6:xml>\r\n"
                        + "                    </ns3:value>\r\n"
                        + "                </ns3:stringxml>\r\n"
                        + "                <ns3:string name=\"metadata-content-type-version\">\r\n"
                        + "                    <ns3:value>myVersion</ns3:value>\r\n"
                        + "                </ns3:string>\r\n"
                        + "                <ns3:string name=\"metadata-content-type\">\r\n"
                        + "                    <ns3:value>myType</ns3:value>\r\n"
                        + "                </ns3:string>\r\n"
                        + "                <ns3:string name=\"title\">\r\n"
                        + "                    <ns3:value>Example title</ns3:value>\r\n"
                        + "                </ns3:string>\r\n" + "            </ns3:metacard>]]>\r\n"
                        + "        </content:encoded>\r\n" + "    </item>\r\n" + "</channel></rss>";
        return new ByteArrayInputStream(response.getBytes());

    }

    private static InputStream getBinaryData() {

        byte[] sampleBytes = {80, 81, 82};

        return new ByteArrayInputStream(sampleBytes);
    }

    private static InputStream getSampleXmlStream() {

        String response = "\r\n"
                + "            <ns3:metacard xmlns:ns3=\"urn:catalog:metacard\" xmlns:ns2=\"http://www.w3.org/1999/xlink\" xmlns:ns1=\"http://www.opengis.net/gml\"\r\n"
                + "                xmlns:ns4=\"http://www.w3.org/2001/SMIL20/\" xmlns:ns5=\"http://www.w3.org/2001/SMIL20/Language\" ns1:id=\"4535c53fc8bc4404a1d32a5ce7a29585\">\r\n"
                + "                <ns3:type>ddf.metacard</ns3:type>\r\n"
                + "                <ns3:source>ddf.distribution</ns3:source>\r\n"
                + "                <ns3:geometry name=\"location\">\r\n"
                + "                    <ns3:value>\r\n" + "                        <ns1:Point>\r\n"
                + "                            <ns1:pos>32.8751900768792 13.1874561309814</ns1:pos>\r\n"
                + "                        </ns1:Point>\r\n"
                + "                    </ns3:value>\r\n" + "                </ns3:geometry>\r\n"
                + "                <ns3:dateTime name=\"created\">\r\n"
                + "                    <ns3:value>2013-01-31T16:22:31.648-07:00</ns3:value>\r\n"
                + "                </ns3:dateTime>\r\n"
                + "                <ns3:dateTime name=\"modified\">\r\n"
                + "                    <ns3:value>2013-01-31T16:22:31.648-07:00</ns3:value>\r\n"
                + "                </ns3:dateTime>\r\n"
                + "                <ns3:stringxml name=\"metadata\">\r\n"
                + "                    <ns3:value>\r\n"
                + "                        <ns6:xml xmlns:ns6=\"urn:sample:namespace\" xmlns=\"urn:sample:namespace\">Example description.</ns6:xml>\r\n"
                + "                    </ns3:value>\r\n" + "                </ns3:stringxml>\r\n"
                + "                <ns3:string name=\"metadata-content-type-version\">\r\n"
                + "                    <ns3:value>myVersion</ns3:value>\r\n"
                + "                </ns3:string>\r\n"
                + "                <ns3:string name=\"metadata-content-type\">\r\n"
                + "                    <ns3:value>myType</ns3:value>\r\n"
                + "                </ns3:string>\r\n"
                + "                <ns3:string name=\"title\">\r\n"
                + "                    <ns3:value>Example title</ns3:value>\r\n"
                + "                </ns3:string>\r\n" + "            </ns3:metacard>\r\n";

        return IOUtils.toInputStream(response);
    }

    /**
     * Tests the proper query is sent to the remote source for query by id.
     *
     * @throws UnsupportedQueryException
     * @throws IOException
     * @throws MalformedURLException
     */
    @Test
    public void testQueryById() throws UnsupportedQueryException, IOException {
        Response clientResponse = mock(Response.class);
        WebClient client = mock(WebClient.class);

        //ClientResponse
        doReturn(clientResponse).when(client).get();
        doReturn(Response.Status.OK.getStatusCode()).when(clientResponse).getStatus();

        //Client functions
        doReturn(getSampleXmlStream()).when(clientResponse).getEntity();
        when(clientResponse.getHeaderString(eq(OpenSearchSource.HEADER_ACCEPT_RANGES)))
                .thenReturn(OpenSearchSource.BYTES);
        when(client.replaceQueryParam(any(String.class), any(Object.class))).thenReturn(client);

        SecureCxfClientFactory factory = getMockFactory(client);

        OverriddenOpenSearchSource source = new OverriddenOpenSearchSource(FILTER_ADAPTER);
        source.setInputTransformer(getMockInputTransformer());
        source.setEndpointUrl("http://localhost:8181/services/catalog/query");
        source.init();
        source.setParameters(DEFAULT_PARAMETERS);
        source.factory = factory;

        Filter filter = filterBuilder.attribute(Metacard.ID).equalTo().text(SAMPLE_ID);

        // when
        SourceResponse response = source.query(new QueryRequestImpl(new QueryImpl(filter)));

        // then
        Assert.assertEquals(1, response.getHits());
    }

    @Test
    @Ignore
    // Ignored because Content Type support has yet to be added.
    public void testQueryByContentType()
            throws UnsupportedQueryException, IOException, URISyntaxException,
            ResourceNotFoundException, ResourceNotSupportedException {

        // given
        FirstArgumentCapture answer = new FirstArgumentCapture(getSampleAtomStream());

        OpenSearchSource source = givenSource(answer);

        Filter filter = filterBuilder.attribute(Metacard.CONTENT_TYPE).equalTo().text(SAMPLE_ID);

        // when
        SourceResponse response = source.query(new QueryRequestImpl(new QueryImpl(filter)));

        // then
        List<NameValuePair> pairs = extractQueryParams(answer);

        verifyOpenSearchUrl(pairs, pair("type", SAMPLE_ID));

    }

    @Test
    public void testQueryBySearchPhrase()
            throws UnsupportedQueryException, URISyntaxException, IOException {

        Response clientResponse = mock(Response.class);
        when(clientResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(clientResponse.getEntity()).thenReturn(getSampleAtomStream());

        WebClient client = mock(WebClient.class);
        when(client.get()).thenReturn(clientResponse);

        SecureCxfClientFactory factory = getMockFactory(client);

        OverriddenOpenSearchSource source = new OverriddenOpenSearchSource(FILTER_ADAPTER);
        source.setInputTransformer(getMockInputTransformer());
        source.setEndpointUrl("http://localhost:8181/services/catalog/query");
        source.init();
        source.setParameters(DEFAULT_PARAMETERS);
        source.factory = factory;

        Filter filter = filterBuilder.attribute(Metacard.METADATA).like()
                .text(SAMPLE_SEARCH_PHRASE);

        // when
        QueryRequestImpl queryRequest = new QueryRequestImpl(new QueryImpl(filter));
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(SecurityConstants.SECURITY_SUBJECT, mock(Subject.class));
        queryRequest.setProperties(properties);
        SourceResponse response = source.query(queryRequest);

        Assert.assertEquals(1, response.getHits());
        List<Result> results = response.getResults();
        Assert.assertTrue(results.size() == 1);
        Result result = results.get(0);
        Metacard metacard = result.getMetacard();
        Assert.assertNotNull(metacard);
        Assert.assertEquals("Resource", metacard.getContentTypeName());
    }

    @Test
    public void testQueryBySearchPhraseRss()
            throws UnsupportedQueryException, URISyntaxException, IOException {
        WebClient client = mock(WebClient.class);
        Response clientResponse = mock(Response.class);
        when(client.get()).thenReturn(clientResponse);
        when(clientResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(clientResponse.getEntity()).thenReturn(getSampleRssStream());

        SecureCxfClientFactory factory = getMockFactory(client);

        OverriddenOpenSearchSource source = new OverriddenOpenSearchSource(FILTER_ADAPTER);
        source.setInputTransformer(getMockInputTransformer());
        source.setEndpointUrl("http://localhost:8181/services/catalog/query");
        source.init();
        source.setParameters(DEFAULT_PARAMETERS);

        source.factory = factory;

        Filter filter = filterBuilder.attribute(Metacard.METADATA).like()
                .text(SAMPLE_SEARCH_PHRASE);

        // when
        QueryRequestImpl queryRequest = new QueryRequestImpl(new QueryImpl(filter));
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(SecurityConstants.SECURITY_SUBJECT, mock(Subject.class));
        queryRequest.setProperties(properties);
        SourceResponse response = source.query(queryRequest);

        Assert.assertEquals(1, response.getHits());
        List<Result> results = response.getResults();
        Assert.assertTrue(results.size() == 1);
        Result result = results.get(0);
        Metacard metacard = result.getMetacard();
        Assert.assertNotNull(metacard);
        Assert.assertEquals("Resource", metacard.getContentTypeName());
    }

    @Test
    public void testQueryBySearchPhraseContentTypeSet()
            throws UnsupportedQueryException, URISyntaxException, IOException {
        WebClient client = mock(WebClient.class);
        Response clientResponse = mock(Response.class);
        when(client.get()).thenReturn(clientResponse);
        when(clientResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(clientResponse.getEntity()).thenReturn(getSampleAtomStream());

        SecureCxfClientFactory factory = getMockFactory(client);

        OverriddenOpenSearchSource source = new OverriddenOpenSearchSource(FILTER_ADAPTER);
        InputTransformer inputTransformer = mock(InputTransformer.class);

        MetacardImpl generatedMetacard = new MetacardImpl();
        generatedMetacard.setMetadata(getSample());
        generatedMetacard.setId(SAMPLE_ID);
        generatedMetacard.setContentTypeName("myType");

        try {
            when(inputTransformer.transform(isA(InputStream.class))).thenReturn(generatedMetacard);
            when(inputTransformer.transform(isA(InputStream.class), isA(String.class)))
                    .thenReturn(generatedMetacard);
        } catch (IOException e) {
            fail();
        } catch (CatalogTransformerException e) {
            fail();
        }
        source.setInputTransformer(inputTransformer);
        source.setEndpointUrl("http://localhost:8181/services/catalog/query");
        source.init();
        source.setParameters(DEFAULT_PARAMETERS);

        source.factory = factory;

        Filter filter = filterBuilder.attribute(Metacard.METADATA).like()
                .text(SAMPLE_SEARCH_PHRASE);
        SourceResponse response = source.query(new QueryRequestImpl(new QueryImpl(filter)));

        Assert.assertEquals(1, response.getHits());
        List<Result> results = response.getResults();
        Assert.assertTrue(results.size() == 1);
        Result result = results.get(0);
        Metacard metacard = result.getMetacard();
        Assert.assertNotNull(metacard);
        Assert.assertEquals("myType", metacard.getContentTypeName());
    }

    @Test
    public void testQueryBySearchPhraseContentTypeSetRss()
            throws UnsupportedQueryException, URISyntaxException, IOException {
        WebClient client = mock(WebClient.class);
        Response clientResponse = mock(Response.class);
        when(client.get()).thenReturn(clientResponse);
        when(clientResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(clientResponse.getEntity()).thenReturn(getSampleRssStream());

        SecureCxfClientFactory factory = getMockFactory(client);

        OverriddenOpenSearchSource source = new OverriddenOpenSearchSource(FILTER_ADAPTER);
        InputTransformer inputTransformer = mock(InputTransformer.class);

        MetacardImpl generatedMetacard = new MetacardImpl();
        generatedMetacard.setMetadata(getSample());
        generatedMetacard.setId(SAMPLE_ID);
        generatedMetacard.setContentTypeName("myType");

        try {
            when(inputTransformer.transform(isA(InputStream.class))).thenReturn(generatedMetacard);
            when(inputTransformer.transform(isA(InputStream.class), isA(String.class)))
                    .thenReturn(generatedMetacard);
        } catch (IOException e) {
            fail();
        } catch (CatalogTransformerException e) {
            fail();
        }
        source.setInputTransformer(inputTransformer);
        source.setEndpointUrl("http://localhost:8181/services/catalog/query");
        source.init();
        source.setParameters(DEFAULT_PARAMETERS);

        source.factory = factory;

        Filter filter = filterBuilder.attribute(Metacard.METADATA).like()
                .text(SAMPLE_SEARCH_PHRASE);
        SourceResponse response = source.query(new QueryRequestImpl(new QueryImpl(filter)));

        Assert.assertEquals(1, response.getHits());
        List<Result> results = response.getResults();
        Assert.assertTrue(results.size() == 1);
        Result result = results.get(0);
        Metacard metacard = result.getMetacard();
        Assert.assertNotNull(metacard);
        Assert.assertEquals("myType", metacard.getContentTypeName());
    }

    @Test
    public void testQueryAnyText()
            throws UnsupportedQueryException, URISyntaxException, IOException {
        Response clientResponse = mock(Response.class);
        doReturn(getSampleAtomStream()).when(clientResponse).getEntity();

        WebClient client = mock(WebClient.class);
        doReturn(Response.Status.OK.getStatusCode()).when(clientResponse).getStatus();
        doReturn(clientResponse).when(client).get();

        SecureCxfClientFactory factory = getMockFactory(client);

        OverriddenOpenSearchSource source = new OverriddenOpenSearchSource(FILTER_ADAPTER);
        source.setInputTransformer(getMockInputTransformer());
        source.setEndpointUrl("http://localhost:8181/services/catalog/query");
        source.init();
        source.setParameters(DEFAULT_PARAMETERS);
        source.factory = factory;

        Filter filter = filterBuilder.attribute(Metacard.ANY_TEXT).like()
                .text(SAMPLE_SEARCH_PHRASE);

        // when
        SourceResponse response = source.query(new QueryRequestImpl(new QueryImpl(filter)));
        Assert.assertEquals(1, response.getHits());
    }

    @Test(expected = UnsupportedQueryException.class)
    public void testQueryBadResponse() throws UnsupportedQueryException, IOException {
        Response clientResponse = mock(Response.class);
        WebClient client = mock(WebClient.class);

        //ClientResponse
        doReturn(clientResponse).when(client).get();
        doReturn(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).when(clientResponse)
                .getStatus();

        SecureCxfClientFactory factory = getMockFactory(client);

        OverriddenOpenSearchSource source = new OverriddenOpenSearchSource(FILTER_ADAPTER);
        source.setInputTransformer(getMockInputTransformer());
        source.setEndpointUrl("http://localhost:8181/services/catalog/query");
        source.init();
        source.setParameters(DEFAULT_PARAMETERS);
        source.factory = factory;

        Filter filter = filterBuilder.attribute(Metacard.ANY_TEXT).like()
                .text(SAMPLE_SEARCH_PHRASE);

        source.query(new QueryRequestImpl(new QueryImpl(filter)));

    }

    /**
     * Basic retrieve product case. Tests the url sent to the connection is correct.
     *
     * @throws ResourceNotSupportedException
     * @throws IOException
     * @throws ResourceNotFoundException
     */
    @Test
    public void testRetrieveResource()
            throws ResourceNotSupportedException, IOException, ResourceNotFoundException {

        // given
        FirstArgumentCapture answer = new FirstArgumentCapture(getBinaryData());

        OpenSearchSource source = givenSource(answer);

        Map<String, Serializable> requestProperties = new HashMap<String, Serializable>();

        requestProperties.put(Metacard.ID, SAMPLE_ID);

        // when
        ResourceResponse response = source.retrieveResource(null, requestProperties);

        Assert.assertEquals(3, response.getResource().getByteArray().length);
    }

    /**
     * Given all null params, nothing will be returned, expect an exception.
     *
     * @throws ResourceNotSupportedException
     */
    @Test
    public void testRetrieveNullProduct() throws ResourceNotSupportedException, IOException {
        OverriddenOpenSearchSource source = new OverriddenOpenSearchSource(FILTER_ADAPTER);
        // when
        try {
            source.retrieveResource(null, null);

            // then
            fail("Should have thrown " + ResourceNotFoundException.class.getName()
                    + " because of null uri.");
        } catch (ResourceNotFoundException e) {
            /*
             * this exception should have been thrown.
             */
            assertThat(e.getMessage(),
                    containsString(OpenSearchSource.COULD_NOT_RETRIEVE_RESOURCE_MESSAGE));
            assertThat(e.getMessage(), containsString("null"));
        }

    }

    // DDF-161
    @Test
    public void testQueryQueryByMetacardIdFollowedByAnyTextQuery() throws Exception {
        WebClient client = mock(WebClient.class);
        Response clientResponse = mock(Response.class);
        when(clientResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(client.get()).thenReturn(clientResponse);
        when(clientResponse.getEntity()).thenReturn(getSampleXmlStream())
                .thenReturn(getSampleAtomStream());

        SecureCxfClientFactory factory = getMockFactory(client);

        OverriddenOpenSearchSource source = new OverriddenOpenSearchSource(FILTER_ADAPTER);
        source.setLocalQueryOnly(true);
        source.setInputTransformer(getMockInputTransformer());
        source.setEndpointUrl("http://localhost:8181/services/catalog/query");
        source.init();
        source.setParameters(DEFAULT_PARAMETERS);

        source.factory = factory;

        // Metacard ID filter
        Filter idFilter = filterBuilder.attribute(Metacard.ID).equalTo().text(SAMPLE_ID);

        // Any text filter
        Filter anyTextFilter = filterBuilder.attribute(Metacard.ANY_TEXT).like()
                .text(SAMPLE_SEARCH_PHRASE);

        // Perform Test (Query by ID followed by Any Text Query)
        SourceResponse response1 = source.query(new QueryRequestImpl(new QueryImpl(idFilter)));
        SourceResponse response2 = source.query(new QueryRequestImpl(new QueryImpl(anyTextFilter)));

        // Verification - Verify that we don't see any exceptions when
        // processing the input stream from the endpoint.
        // Verify 1 metacard is in the results
        assertThat(response1.getResults().size(), is(1));

        // Verify that the atom feed is converted into 1 metacard result
        assertThat(response2.getResults().size(), is(1));
    }

    // DDF-161
    @Test
    public void testQueryQueryByMetacardIdFollowedByAnyTextQueryRss() throws Exception {
        WebClient client = mock(WebClient.class);
        Response clientResponse = mock(Response.class);
        when(clientResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(clientResponse.getEntity()).thenReturn(getSampleXmlStream())
                .thenReturn(getSampleRssStream());
        when(client.get()).thenReturn(clientResponse);

        SecureCxfClientFactory factory = getMockFactory(client);

        OverriddenOpenSearchSource source = new OverriddenOpenSearchSource(FILTER_ADAPTER);
        source.setLocalQueryOnly(true);
        source.setInputTransformer(getMockInputTransformer());
        source.setEndpointUrl("http://localhost:8181/services/catalog/query");
        source.init();
        source.setParameters(DEFAULT_PARAMETERS);

        source.factory = factory;

        // Metacard ID filter
        Filter idFilter = filterBuilder.attribute(Metacard.ID).equalTo().text(SAMPLE_ID);

        // Any text filter
        Filter anyTextFilter = filterBuilder.attribute(Metacard.ANY_TEXT).like()
                .text(SAMPLE_SEARCH_PHRASE);

        // Perform Test (Query by ID followed by Any Text Query)
        SourceResponse response1 = source.query(new QueryRequestImpl(new QueryImpl(idFilter)));
        SourceResponse response2 = source.query(new QueryRequestImpl(new QueryImpl(anyTextFilter)));

        // Verification - Verify that we don't see any exceptions when
        // processing the input stream from the endpoint.
        // Verify 1 metacard is in the results
        assertThat(response1.getResults().size(), is(1));

        // Verify that the atom feed is converted into 1 metacard result
        assertThat(response2.getResults().size(), is(1));
    }

    private NameValuePair pair(String name, String value) {
        return new NameValuePair(name, value);
    }

    private List<NameValuePair> extractQueryParams(FirstArgumentCapture answer)
            throws MalformedURLException, URISyntaxException {
        URL url = new URI(answer.getInputArg()).toURL();
        ParameterParser paramParser = new ParameterParser();
        List<NameValuePair> pairs = paramParser.parse(url.getQuery(), '&');
        return pairs;
    }

    private void verifyOpenSearchUrl(List<NameValuePair> pairs, NameValuePair... answers) {

        ConcurrentHashMap<String, String> nvpMap = createMapFor(pairs);

        for (NameValuePair answerPair : answers) {
            assertThat(nvpMap.get(answerPair.getName()), is(answerPair.getValue()));
            nvpMap.remove(answerPair.getName());
        }

        assertThat(nvpMap.get("count"), is("20"));
        nvpMap.remove("count");
        assertThat(nvpMap.get("mt"), is("0"));
        nvpMap.remove("mt");
        assertThat(nvpMap.get("src"), is("local"));
        nvpMap.remove("src");

        verifyAllEntriesBlank(nvpMap);

    }

    /**
     * Verifies that the rest of the entries don't have a corresponding value
     *
     * @param nvpMap
     */
    private void verifyAllEntriesBlank(ConcurrentHashMap<String, String> nvpMap) {
        for (Entry<String, String> entry : nvpMap.entrySet()) {

            String errorMessage =
                    "[" + entry.getKey() + "]" + " should not have a corresponding value.";

            assertThat(errorMessage, entry.getValue(), is(""));
        }
    }

    private ConcurrentHashMap<String, String> createMapFor(List<NameValuePair> pairs) {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();

        for (NameValuePair pair : pairs) {
            map.put(pair.getName(), pair.getValue());
        }
        return map;
    }

    private OpenSearchSource givenSource(Answer<BinaryContent> answer)
            throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        WebClient client = mock(WebClient.class);
        ResourceReader mockReader = mock(ResourceReader.class);

        Response clientResponse = mock(Response.class);
        when(clientResponse.getEntity()).thenReturn(getBinaryData());
        when(clientResponse.getHeaderString(eq(OpenSearchSource.HEADER_ACCEPT_RANGES)))
                .thenReturn(OpenSearchSource.BYTES);
        when(client.get()).thenReturn(clientResponse);
        SecureCxfClientFactory factory = getMockFactory(client);
        when(mockReader.retrieveResource(any(URI.class), any(Map.class)))
                .thenReturn(new ResourceResponseImpl(new ResourceImpl(getBinaryData(), "")));

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<String, Object>();
        headers.put(HttpHeaders.CONTENT_TYPE, Arrays.<Object>asList("application/octet-stream"));

        when(clientResponse.getHeaders()).thenReturn(headers);

        OverriddenOpenSearchSource source = new OverriddenOpenSearchSource(FILTER_ADAPTER);
        source.setEndpointUrl("http://localhost:8181/services/catalog/query");
        source.setParameters(DEFAULT_PARAMETERS);
        source.init();
        source.setLocalQueryOnly(true);
        source.setInputTransformer(getMockInputTransformer());
        source.factory = factory;
        source.setResourceReader(mockReader);

        return source;
    }

    protected InputTransformer getMockInputTransformer() {
        InputTransformer inputTransformer = mock(InputTransformer.class);

        Metacard generatedMetacard = getSimpleMetacard();

        try {
            when(inputTransformer.transform(isA(InputStream.class))).thenReturn(generatedMetacard);
            when(inputTransformer.transform(isA(InputStream.class), isA(String.class)))
                    .thenReturn(generatedMetacard);
        } catch (IOException e) {
            fail();
        } catch (CatalogTransformerException e) {
            fail();
        }
        return inputTransformer;
    }

    protected Metacard getSimpleMetacard() {
        MetacardImpl generatedMetacard = new MetacardImpl();
        generatedMetacard.setMetadata(getSample());
        generatedMetacard.setId(SAMPLE_ID);

        return generatedMetacard;
    }

    private String getSample() {
        return "<xml></xml>";
    }

    private class FirstArgumentCapture implements Answer<BinaryContent> {

        private InputStream returnInputStream;

        private String inputArg;

        public FirstArgumentCapture() {
            this.returnInputStream = getSampleXmlStream();
        }

        public FirstArgumentCapture(InputStream inputStream) {
            this.returnInputStream = inputStream;
        }

        public String getInputArg() {
            return inputArg;
        }

        @Override
        public BinaryContent answer(InvocationOnMock invocation) throws Throwable {
            this.inputArg = (String) invocation.getArguments()[0];

            return new BinaryContentImpl(returnInputStream);

        }

    }

    private class OverriddenOpenSearchSource extends OpenSearchSource {

        private InputTransformer transformer;

        /**
         * Creates an OpenSearch Site instance. Sets an initial default endpointUrl that can be
         * overwritten using the setter methods.
         *
         * @param filterAdapter
         * @throws UnsupportedQueryException
         */
        public OverriddenOpenSearchSource(FilterAdapter filterAdapter) {
            super(filterAdapter);
        }

        public OverriddenOpenSearchSource(FilterAdapter filterAdapter,
                SecureCxfClientFactory factory) {
            super(filterAdapter);
            this.factory = factory;
        }

        protected void setInputTransformer(InputTransformer inputTransformer) {
            transformer = inputTransformer;
        }

        @Override
        protected InputTransformer lookupTransformerReference(String namespaceUri)
                throws InvalidSyntaxException {
            return transformer;
        }

        @Override
        protected SecureCxfClientFactory tempFactory(String url) {
            return this.factory;
        }
    }

    protected SecureCxfClientFactory getMockFactory(WebClient client) {
        SecureCxfClientFactory factory = mock(SecureCxfClientFactory.class);

        try {
            doReturn(client).when(factory)
                    .getClientForBasicAuth(any(String.class), any(String.class));
            doReturn(client).when(factory)
                    .getWebClientForSubject(any(org.apache.shiro.subject.Subject.class));
            doReturn(client).when(factory).getUnsecuredWebClient();
        } catch (SecurityServiceException sse) {
            fail("Could not get client");
        }

        return factory;
    }
}
