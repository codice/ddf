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
package ddf.catalog.source.opensearch;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import junit.framework.Assert;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.util.ParameterParser;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opengis.filter.Filter;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests parts of the {@link OpenSearchSource}
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class TestOpenSearchSource {

    private static final GeotoolsFilterAdapterImpl FILTER_ADAPTER = new GeotoolsFilterAdapterImpl();

    private static final String SAMPLE_ID = "abcdef12345678900987654321fedcba";

    private static final String SAMPLE_SEARCH_PHRASE = "foobar";

    private static final String BYTES_TO_SKIP = "BytesToSkip";

    private static FilterBuilder filterBuilder = new GeotoolsFilterBuilder();

    /**
     * Tests the proper query is sent to the remote source for query by id.
     * 
     * @throws UnsupportedQueryException
     * @throws IOException
     * @throws MalformedURLException
     */
    @Test
    public void testQuery_ById() throws UnsupportedQueryException, MalformedURLException,
        IOException {
        WebClient client = mock(WebClient.class);

        Response clientResponse = mock(Response.class);

        OpenSearchConnection openSearchConnection = mock(OpenSearchConnection.class);

        when(openSearchConnection.getOpenSearchWebClient()).thenReturn(client);

        when(client.get()).thenReturn(clientResponse);

        Client proxy = mock(Client.class);

        when(openSearchConnection.newRestClient(any(String.class), any(Query.class), any(String.class), any(Boolean.class))).thenReturn(proxy);

        when(openSearchConnection.getWebClientFromClient(proxy)).thenReturn(client);

        when(clientResponse.getEntity()).thenReturn(
                new BinaryContentImpl(getSampleXmlStream()).getInputStream());

        OpenSearchSource source = new OpenSearchSource(FILTER_ADAPTER);
        source.setInputTransformer(getMockInputTransformer());
        source.setEndpointUrl("http://localhost:8181/services/catalog/query");
        source.init();
        source.setParameters("q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort");

        source.openSearchConnection = openSearchConnection;

        Filter filter = filterBuilder.attribute(Metacard.ID).equalTo().text(SAMPLE_ID);

        // when
        SourceResponse response = source.query(new QueryRequestImpl(new QueryImpl(filter)));

        // then
        Assert.assertEquals(1, response.getHits());

    }

    @Test
    @Ignore
    // Ignored because Content Type support has yet to be added.
    public void testQuery_ByContentType() throws UnsupportedQueryException, MalformedURLException,
        IOException, URISyntaxException {

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
    public void testQuery_BySearchPhrase() throws UnsupportedQueryException, URISyntaxException,
        IOException {
        WebClient client = mock(WebClient.class);

        Response clientResponse = mock(Response.class);

        OpenSearchConnection openSearchConnection = mock(OpenSearchConnection.class);

        when(openSearchConnection.getOpenSearchWebClient()).thenReturn(client);

        when(client.get()).thenReturn(clientResponse);

        OpenSearchSource source = new OpenSearchSource(FILTER_ADAPTER);
        source.setInputTransformer(getMockInputTransformer());
        source.setEndpointUrl("http://localhost:8181/services/catalog/query");
        source.init();
        source.setParameters("q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort");

        source.openSearchConnection = openSearchConnection;

        Filter filter = filterBuilder.attribute(Metacard.METADATA).like()
                .text(SAMPLE_SEARCH_PHRASE);

        // when
        SourceResponse response = source.query(new QueryRequestImpl(new QueryImpl(filter)));

        Assert.assertEquals(0, response.getHits());

    }

    @Test
    public void testQueryAnyText() throws UnsupportedQueryException, URISyntaxException,
        IOException {
        WebClient client = mock(WebClient.class);

        Response clientResponse = mock(Response.class);

        OpenSearchConnection openSearchConnection = mock(OpenSearchConnection.class);

        when(openSearchConnection.getOpenSearchWebClient()).thenReturn(client);

        when(client.get()).thenReturn(clientResponse);

        OpenSearchSource source = new OpenSearchSource(FILTER_ADAPTER);
        source.setInputTransformer(getMockInputTransformer());
        source.setEndpointUrl("http://localhost:8181/services/catalog/query");
        source.init();
        source.setParameters("q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort");

        source.openSearchConnection = openSearchConnection;

        Filter filter = filterBuilder.attribute(Metacard.ANY_TEXT).like()
                .text(SAMPLE_SEARCH_PHRASE);

        // when
        SourceResponse response = source.query(new QueryRequestImpl(new QueryImpl(filter)));

        Assert.assertEquals(0, response.getHits());

    }

    /**
     * Basic retrieve product case. Tests the url sent to the connection is correct.
     * 
     * @throws ResourceNotSupportedException
     * @throws IOException
     * @throws ResourceNotFoundException
     */
    @Test
    public void testRetrieveResource() throws ResourceNotSupportedException, IOException,
        ResourceNotFoundException {

        // given
        FirstArgumentCapture answer = new FirstArgumentCapture(getBinaryData());

        OpenSearchSource source = givenSource(answer);

        Map<String, Serializable> requestProperties = new HashMap<String, Serializable>();

        requestProperties.put(Metacard.ID, SAMPLE_ID);

        // when
        ResourceResponse response = source.retrieveResource(null, requestProperties);

        Assert.assertEquals(3, response.getResource().getByteArray().length);
    }


    @Test
    public void testRetrieveResourceWithBytesToSkip() throws ResourceNotSupportedException, IOException,
            ResourceNotFoundException {

        // given
        FirstArgumentCapture answer = new FirstArgumentCapture(getBinaryData());

        OpenSearchSource source = givenSource(answer);

        Map<String, Serializable> requestProperties = new HashMap<String, Serializable>();

        requestProperties.put(Metacard.ID, SAMPLE_ID);

        requestProperties.put(OpenSearchSource.BYTES_TO_SKIP, "2");

        // when
        ResourceResponse response = source.retrieveResource(null, requestProperties);

        Assert.assertEquals(1, response.getResource().getByteArray().length);
    }

    /**
     * Given all null params, nothing will be returned, expect an exception.
     * 
     * @throws ResourceNotSupportedException
     */
    @Test
    public void testRetrieveNullProduct() throws ResourceNotSupportedException, IOException {
        WebClient client = mock(WebClient.class);

        Response clientResponse = mock(Response.class);

        OpenSearchConnection openSearchConnection = mock(OpenSearchConnection.class);

        when(openSearchConnection.getOpenSearchWebClient()).thenReturn(client);

        when(client.get()).thenReturn(clientResponse);

        OpenSearchSource source = new OpenSearchSource(FILTER_ADAPTER);

        source.openSearchConnection = openSearchConnection;

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

    @Test
    public void testRetrieveProductUriSyntaxException() throws ResourceNotSupportedException, IOException {
        WebClient client = mock(WebClient.class);

        Response clientResponse = mock(Response.class);

        OpenSearchConnection openSearchConnection = mock(OpenSearchConnection.class);

        when(openSearchConnection.getOpenSearchWebClient()).thenReturn(client);

        when(client.get()).thenReturn(clientResponse);

        OpenSearchSource source = new OpenSearchSource(FILTER_ADAPTER);

        source.openSearchConnection = openSearchConnection;

        source.setEndpointUrl("http://example.com/q?s=^LMT");

        Map<String, Serializable> requestProperties = new HashMap<String, Serializable>();

        requestProperties.put(Metacard.ID, SAMPLE_ID);

        // when
        try {
            source.retrieveResource(null, requestProperties);

            // then
            fail("Should have thrown " + ResourceNotFoundException.class.getName()
                    + " because of null uri.");
        } catch (ResourceNotFoundException e) {
            /*
             * this exception should have been thrown.
             */
            assertThat(e.getMessage(), containsString(OpenSearchSource.COULD_NOT_RETRIEVE_RESOURCE_MESSAGE));
        }

    }

    @Test
    public void testRetrieveProductMalformedUrlException() throws ResourceNotSupportedException, IOException {
        WebClient client = mock(WebClient.class);

        Response clientResponse = mock(Response.class);

        OpenSearchConnection openSearchConnection = mock(OpenSearchConnection.class);

        when(openSearchConnection.getOpenSearchWebClient()).thenReturn(client);

        when(client.get()).thenReturn(clientResponse);

        OpenSearchSource source = new OpenSearchSource(FILTER_ADAPTER);

        source.openSearchConnection = openSearchConnection;

        source.setEndpointUrl("unknownProtocol://localhost:8181/services/catalog/query");

        Map<String, Serializable> requestProperties = new HashMap<String, Serializable>();

        requestProperties.put(Metacard.ID, SAMPLE_ID);

        // when
        try {
            source.retrieveResource(null, requestProperties);

            // then
            fail("Should have thrown " + ResourceNotFoundException.class.getName()
                    + " because of null uri.");
        } catch (ResourceNotFoundException e) {
            /*
             * this exception should have been thrown.
             */
            assertThat(e.getMessage(), containsString(OpenSearchSource.COULD_NOT_RETRIEVE_RESOURCE_MESSAGE));
        }

    }

    // DDF-161
    @Test
    public void testQuery_QueryByMetacardIdFollowedByAnyTextQuery() throws Exception {

        WebClient client = mock(WebClient.class);

        Response clientResponse = mock(Response.class);

        OpenSearchConnection openSearchConnection = mock(OpenSearchConnection.class);

        when(openSearchConnection.getOpenSearchWebClient()).thenReturn(client);

        when(client.get()).thenReturn(clientResponse);

        Client proxy = mock(Client.class);

        when(openSearchConnection.newRestClient(any(String.class), any(Query.class), any(String.class), any(Boolean.class))).thenReturn(proxy);

        when(openSearchConnection.getWebClientFromClient(proxy)).thenReturn(client);

        when(clientResponse.getEntity()).thenReturn(
                new BinaryContentImpl(getSampleXmlStream()).getInputStream()).thenReturn(
                new BinaryContentImpl(getSampleAtomStream()).getInputStream());
        OpenSearchSource source = new OpenSearchSource(FILTER_ADAPTER);
        source.setLocalQueryOnly(true);
        source.setInputTransformer(getMockInputTransformer());
        source.setEndpointUrl("http://localhost:8181/services/catalog/query");
        source.init();
        source.setParameters("q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort");

        source.openSearchConnection = openSearchConnection;

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

            String errorMessage = "[" + entry.getKey() + "]"
                    + " should not have a corresponding value.";

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
        throws IOException {
        WebClient client = mock(WebClient.class);

        OpenSearchConnection openSearchConnection = mock(OpenSearchConnection.class);

        Client proxy = mock(Client.class);

        when(openSearchConnection.newRestClient(any(String.class), any(Query.class), any(String.class), any(Boolean.class))).thenReturn(proxy);

        when(openSearchConnection.getWebClientFromClient(proxy)).thenReturn(client);

        Response clientResponse = mock(Response.class);

        when(client.get()).thenReturn(clientResponse);

        when(clientResponse.getEntity()).thenReturn(getBinaryData());

        when(openSearchConnection.getOpenSearchWebClient()).thenReturn(client);

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<String, Object>();
        headers.put(HttpHeaders.CONTENT_TYPE, Arrays.<Object>asList("application/octet-stream"));

        when(clientResponse.getHeaders()).thenReturn(headers);

        OpenSearchSource source = new OpenSearchSource(FILTER_ADAPTER);
        source.setEndpointUrl("http://localhost:8181/services/catalog/query");
        source.setParameters("q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort");
        source.init();
        source.setLocalQueryOnly(true);
        source.setInputTransformer(getMockInputTransformer());
        source.openSearchConnection = openSearchConnection;
        return source;
    }

    protected InputTransformer getMockInputTransformer() {
        InputTransformer inputTransformer = mock(InputTransformer.class);

        Metacard generatedMetacard = getSimpleMetacard();

        try {
            when(inputTransformer.transform(isA(InputStream.class))).thenReturn(generatedMetacard);
            when(inputTransformer.transform(isA(InputStream.class), isA(String.class))).thenReturn(
                    generatedMetacard);
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

    private static InputStream getSampleAtomStream() {
        String response = "<feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:os=\"http://a9.com/-/spec/opensearch/1.1/\">\r\n"
                + "    <title type=\"text\">Query Response</title>\r\n"
                + "    <updated>2013-01-31T23:22:37.298Z</updated>\r\n"
                + "    <id>urn:uuid:a27352c9-f935-45f0-9b8c-5803095164bb</id>\r\n"
                + "    <link href=\"#\" rel=\"self\" />\r\n"
                + "    <author>\r\n"
                + "        <name>Codice</name>\r\n"
                + "    </author>\r\n"
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
                + "            </gml:Point>\r\n"
                + "        </georss:where>\r\n"
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
                + "                </ns3:string>\r\n"
                + "            </ns3:metacard>\r\n"
                + "        </content>\r\n" + "    </entry>\r\n" + "</feed>";
        return new ByteArrayInputStream(response.getBytes());

    }

    private static InputStream getBinaryData() {

        byte[] sampleBytes = {80, 81, 82};

        return new ByteArrayInputStream(sampleBytes);
    }

    private static InputStream getSampleXmlStream() {

        String response = "\r\n"
                + "<ns3:metacard ns1:id=\"ac0c6917d5ee45bfb3c2bf8cd2ebaa67\" xmlns:ns1=\"http://www.opengis.net/gml\" xmlns:ns3=\"urn:catalog:metacard\">\r\n"
                + "   <ns3:type>ddf.metacard</ns3:type>\r\n"
                + "   <ns3:source>ddf</ns3:source>\r\n"
                + "   <ns3:dateTime name=\"modified\">\r\n"
                + "      <ns3:value>2013-01-29T17:09:19.980-07:00</ns3:value>\r\n"
                + "   </ns3:dateTime>\r\n"
                + "   <ns3:stringxml name=\"metadata\">\r\n"
                + "      <ns3:value>\r\n"
                + "         <ddms:Resource xmlns:ddms=\"http://metadata.dod.mil/mdr/ns/DDMS/2.0/\" xmlns:ICISM=\"urn:us:gov:ic:ism:v2\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
                + "            <ddms:identifier ddms:qualifier=\"http://example.test#URI\" ddms:value=\"http://example.test.html\"/>\r\n"
                + "            <ddms:title ICISM:classification=\"U\" ICISM:ownerProducer=\"USA\">Example Title</ddms:title>\r\n"
                + "            <ddms:description ICISM:classification=\"U\" ICISM:ownerProducer=\"USA\">Example description.</ddms:description>\r\n"
                + "            <ddms:dates ddms:posted=\"2013-01-29\"/>\r\n"
                + "            <ddms:rights ddms:copyright=\"true\" ddms:intellectualProperty=\"true\" ddms:privacyAct=\"false\"/>\r\n"
                + "            <ddms:creator ICISM:classification=\"U\" ICISM:ownerProducer=\"USA\">\r\n"
                + "               <ddms:Person>\r\n"
                + "                  <ddms:name>John Doe</ddms:name>\r\n"
                + "                  <ddms:surname>Doe</ddms:surname>\r\n"
                + "               </ddms:Person>\r\n"
                + "            </ddms:creator>\r\n"
                + "            <ddms:subjectCoverage>\r\n"
                + "               <ddms:Subject>\r\n"
                + "                  <ddms:category ddms:code=\"nitf\" ddms:label=\"nitf\" ddms:qualifier=\"SubjectCoverageQualifier\"/>\r\n"
                + "                  <ddms:keyword ddms:value=\"schematypesearch\"/>\r\n"
                + "               </ddms:Subject>\r\n"
                + "            </ddms:subjectCoverage>\r\n"
                + "            <ddms:temporalCoverage>\r\n"
                + "               <ddms:TimePeriod>\r\n"
                + "                  <ddms:start>2013-01-29</ddms:start>\r\n"
                + "                  <ddms:end>2013-01-29</ddms:end>\r\n"
                + "               </ddms:TimePeriod>\r\n"
                + "            </ddms:temporalCoverage>\r\n"
                + "            <ddms:security ICISM:classification=\"U\" ICISM:ownerProducer=\"USA\"/>\r\n"
                + "         </ddms:Resource>\r\n" + "      </ns3:value>\r\n"
                + "   </ns3:stringxml>\r\n" + "   <ns3:string name=\"resource-size\">\r\n"
                + "      <ns3:value>N/A</ns3:value>\r\n" + "   </ns3:string>\r\n"
                + "   <ns3:geometry name=\"location\">\r\n" + "      <ns3:value>\r\n"
                + "         <ns1:Point>\r\n" + "            <ns1:pos>2.0 1.0</ns1:pos>\r\n"
                + "         </ns1:Point>\r\n" + "      </ns3:value>\r\n" + "   </ns3:geometry>\r\n"
                + "   <ns3:dateTime name=\"created\">\r\n"
                + "      <ns3:value>2013-01-29T17:09:19.980-07:00</ns3:value>\r\n"
                + "   </ns3:dateTime>\r\n" + "   <ns3:string name=\"resource-uri\">\r\n"
                + "      <ns3:value>http://example.com</ns3:value>\r\n" + "   </ns3:string>\r\n"
                + "   <ns3:string name=\"metadata-content-type-version\">\r\n"
                + "      <ns3:value>v2.0</ns3:value>\r\n" + "   </ns3:string>\r\n"
                + "   <ns3:string name=\"title\">\r\n"
                + "      <ns3:value>Example Title</ns3:value>\r\n" + "   </ns3:string>\r\n"
                + "   <ns3:string name=\"metadata-content-type\">\r\n"
                + "      <ns3:value>Resource</ns3:value>\r\n" + "   </ns3:string>\r\n"
                + "   <ns3:dateTime name=\"effective\">\r\n"
                + "      <ns3:value>2013-01-29T17:09:19.980-07:00</ns3:value>\r\n"
                + "   </ns3:dateTime>\r\n" + "</ns3:metacard>";

        return new ByteArrayInputStream(response.getBytes());
    }

    private class FirstArgumentCapture implements Answer<BinaryContent> {

        public FirstArgumentCapture() {
            this.returnInputStream = getSampleXmlStream();
        }

        public FirstArgumentCapture(InputStream inputStream) {
            this.returnInputStream = inputStream;
        }

        private InputStream returnInputStream;

        private String inputArg;

        public String getInputArg() {
            return inputArg;
        }

        @Override
        public BinaryContent answer(InvocationOnMock invocation) throws Throwable {
            this.inputArg = (String) invocation.getArguments()[0];

            return new BinaryContentImpl(returnInputStream);

        }

    }

}
