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
package ddf.catalog.source.opensearch.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
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
import ddf.security.encryption.EncryptionService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opengis.filter.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Tests parts of the {@link OpenSearchSource}
 *
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 */
public class OpenSearchSourceTest {

  private static final String SOURCE_ID = "TEST-OS";

  private static final String RESOURCE_TAG = "Resource";

  private static final GeotoolsFilterAdapterImpl FILTER_ADAPTER = new GeotoolsFilterAdapterImpl();

  private static final String SAMPLE_ID = "abcdef12345678900987654321fedcba";

  private static final String SAMPLE_SEARCH_PHRASE = "foobar";
  private static final List<String> DEFAULT_PARAMETERS =
      Arrays.asList(
          "q",
          "src",
          "mr",
          "start",
          "count",
          "mt",
          "dn",
          "lat",
          "lon",
          "radius",
          "bbox",
          "polygon",
          "dtstart",
          "dtend",
          "dateName",
          "filter",
          "sort");
  private static FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
  private EncryptionService encryptionService = mock(EncryptionService.class);
  private OpenSearchParserImpl openSearchParserImpl = new OpenSearchParserImpl();
  private OpenSearchFilterVisitor openSearchFilterVisitor = new OpenSearchFilterVisitor();
  private Response response;
  private OverriddenOpenSearchSource source;

  private static InputStream getSampleAtomStreamWithForeignMarkup() {
    String response =
        "<feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:os=\"http://a9.com/-/spec/opensearch/1.1/\">\r\n"
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
            + "        <Resource:Resource xmlns=\"http://sample.com/resource\" xmlns:ns5=\"http://www.w3.org/1999/xlink\" xmlns:Resource=\"http://sample.com/resource\">"
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
            + "        </Resource:Resource>\r\n"
            + "    </entry>\r\n"
            + "</feed>";
    return new ByteArrayInputStream(response.getBytes());
  }

  private static InputStream getSampleAtomStream() {
    String response =
        "<feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:os=\"http://a9.com/-/spec/opensearch/1.1/\">\r\n"
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
            + "        </content>\r\n"
            + "    </entry>\r\n"
            + "</feed>";
    return new ByteArrayInputStream(response.getBytes());
  }

  private static InputStream getSampleRssStream() {
    String response =
        "<rss version=\"2.0\" xmlns:os=\"http://a9.com/-/spec/opensearch/1.1/\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\"><channel>\r\n"
            + "    <title type=\"text\">Query Response</title>\r\n"
            + "    <lastBuildDate>2013-01-31T23:22:37.298Z</lastBuildDate>\r\n"
            + "    <guid>urn:uuid:a27352c9-f935-45f0-9b8c-5803095164bb</guid>\r\n"
            + "    <link href=\"#\" rel=\"self\" />\r\n"
            + "    <managingEditor>\r\n"
            + "        Codice\r\n"
            + "    </managingEditor>\r\n"
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
            + "            </gml:Point>\r\n"
            + "        </georss:where>\r\n"
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
            + "                </ns3:string>\r\n"
            + "            </ns3:metacard>]]>\r\n"
            + "        </content:encoded>\r\n"
            + "    </item>\r\n"
            + "</channel></rss>";
    return new ByteArrayInputStream(response.getBytes());
  }

  private static InputStream getBinaryData() {

    byte[] sampleBytes = {80, 81, 82};

    return new ByteArrayInputStream(sampleBytes);
  }

  private static InputStream getSampleXmlStream() {

    String response =
        "\r\n"
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
            + "            </ns3:metacard>\r\n";

    return IOUtils.toInputStream(response);
  }

  @Before
  public void setUp() throws Exception {
    response = mock(Response.class);
    WebClient webClient = mock(WebClient.class);

    doReturn(response).when(webClient).get();
    doReturn(Response.Status.OK.getStatusCode()).when(response).getStatus();

    doReturn(getSampleXmlStream()).when(response).getEntity();
    when(response.getHeaderString(eq(OpenSearchSource.HEADER_ACCEPT_RANGES)))
        .thenReturn(OpenSearchSource.BYTES);
    when(webClient.replaceQueryParam(any(String.class), any(Object.class))).thenReturn(webClient);

    SecureCxfClientFactory factory = getMockFactory(webClient);

    source = new OverriddenOpenSearchSource(FILTER_ADAPTER, encryptionService);
    source.setShortname(SOURCE_ID);
    source.setInputTransformer(getMockInputTransformer());
    source.setEndpointUrl("http://localhost:8181/services/catalog/query");
    source.init();
    source.setParameters(DEFAULT_PARAMETERS);
    source.factory = factory;
  }

  /**
   * Tests the proper query is sent to the remote source for query by id.
   *
   * @throws UnsupportedQueryException
   * @throws IOException
   */
  @Test
  public void testQueryById() throws UnsupportedQueryException, IOException {
    Filter filter = filterBuilder.attribute(Metacard.ID).equalTo().text(SAMPLE_ID);

    // when
    SourceResponse response = source.query(new QueryRequestImpl(new QueryImpl(filter)));

    // then
    assertThat(response.getHits(), is(1L));
  }

  @Test
  public void testQueryBySearchPhrase()
      throws UnsupportedQueryException, URISyntaxException, IOException {
    when(response.getEntity()).thenReturn(getSampleAtomStream());

    Filter filter = filterBuilder.attribute(Metacard.METADATA).like().text(SAMPLE_SEARCH_PHRASE);

    // when
    QueryRequestImpl queryRequest = new QueryRequestImpl(new QueryImpl(filter));
    Map<String, Serializable> properties = new HashMap<>();
    properties.put(SecurityConstants.SECURITY_SUBJECT, mock(Subject.class));
    queryRequest.setProperties(properties);
    SourceResponse response = source.query(queryRequest);

    assertThat(response.getHits(), is(1L));
    List<Result> results = response.getResults();
    assertThat(results.size(), is(1));
    Result result = results.get(0);
    Metacard metacard = result.getMetacard();
    assertThat(metacard, notNullValue());
    assertThat(metacard.getContentTypeName(), is(RESOURCE_TAG));
  }

  @Test
  public void testQueryBySearchPhraseRss()
      throws UnsupportedQueryException, URISyntaxException, IOException {
    when(response.getEntity()).thenReturn(getSampleRssStream());

    Filter filter = filterBuilder.attribute(Metacard.METADATA).like().text(SAMPLE_SEARCH_PHRASE);

    // when
    QueryRequestImpl queryRequest = new QueryRequestImpl(new QueryImpl(filter));
    Map<String, Serializable> properties = new HashMap<>();
    properties.put(SecurityConstants.SECURITY_SUBJECT, mock(Subject.class));
    queryRequest.setProperties(properties);
    SourceResponse response = source.query(queryRequest);

    assertThat(response.getHits(), is(1L));
    List<Result> results = response.getResults();
    assertThat(results.size(), is(1));
    Result result = results.get(0);
    Metacard metacard = result.getMetacard();
    assertThat(metacard, notNullValue());
    assertThat(metacard.getContentTypeName(), is(RESOURCE_TAG));
  }

  @Test
  public void testQueryBySearchPhraseContentTypeSet()
      throws UnsupportedQueryException, URISyntaxException, IOException,
          CatalogTransformerException {
    when(response.getEntity()).thenReturn(getSampleAtomStream());
    InputTransformer inputTransformer = mock(InputTransformer.class);

    MetacardImpl generatedMetacard = new MetacardImpl();
    generatedMetacard.setMetadata(getSample());
    generatedMetacard.setId(SAMPLE_ID);
    generatedMetacard.setContentTypeName("myType");

    when(inputTransformer.transform(isA(InputStream.class))).thenReturn(generatedMetacard);
    when(inputTransformer.transform(isA(InputStream.class), isA(String.class)))
        .thenReturn(generatedMetacard);

    source.setInputTransformer(inputTransformer);

    Filter filter = filterBuilder.attribute(Metacard.METADATA).like().text(SAMPLE_SEARCH_PHRASE);
    SourceResponse response = source.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertThat(response.getHits(), is(1L));
    List<Result> results = response.getResults();
    assertThat(results.size(), is(1));
    Result result = results.get(0);
    Metacard metacard = result.getMetacard();
    assertThat(metacard, notNullValue());
    assertThat(metacard.getContentTypeName(), is("myType"));
  }

  @Test
  public void testQueryBySearchPhraseContentTypeSetRss()
      throws UnsupportedQueryException, URISyntaxException, IOException,
          CatalogTransformerException {
    when(response.getEntity()).thenReturn(getSampleRssStream());

    InputTransformer inputTransformer = mock(InputTransformer.class);

    MetacardImpl generatedMetacard = new MetacardImpl();
    generatedMetacard.setMetadata(getSample());
    generatedMetacard.setId(SAMPLE_ID);
    generatedMetacard.setContentTypeName("myType");

    when(inputTransformer.transform(isA(InputStream.class))).thenReturn(generatedMetacard);
    when(inputTransformer.transform(isA(InputStream.class), isA(String.class)))
        .thenReturn(generatedMetacard);

    source.setInputTransformer(inputTransformer);

    Filter filter = filterBuilder.attribute(Metacard.METADATA).like().text(SAMPLE_SEARCH_PHRASE);
    SourceResponse response = source.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertThat(response.getHits(), is(1L));
    List<Result> results = response.getResults();
    assertThat(results.size(), is(1));
    Result result = results.get(0);
    Metacard metacard = result.getMetacard();
    assertThat(metacard, notNullValue());
    assertThat(metacard.getContentTypeName(), is("myType"));
  }

  @Test
  public void testQueryAnyText() throws UnsupportedQueryException, URISyntaxException, IOException {
    when(response.getEntity()).thenReturn(getSampleAtomStream());

    Filter filter = filterBuilder.attribute(Metacard.ANY_TEXT).like().text(SAMPLE_SEARCH_PHRASE);

    // when
    SourceResponse response = source.query(new QueryRequestImpl(new QueryImpl(filter)));
    assertThat(response.getHits(), is(1L));
  }

  @Test(expected = UnsupportedQueryException.class)
  public void testQueryBadResponse() throws UnsupportedQueryException, IOException {
    doReturn(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).when(response).getStatus();

    Filter filter = filterBuilder.attribute(Metacard.ANY_TEXT).like().text(SAMPLE_SEARCH_PHRASE);

    source.query(new QueryRequestImpl(new QueryImpl(filter)));
  }

  @Test
  public void testQueryResponseWithForeignMarkup() throws UnsupportedQueryException, IOException {
    source.setMarkUpSet(Collections.singletonList(RESOURCE_TAG));
    when(response.getEntity()).thenReturn(getSampleAtomStreamWithForeignMarkup());

    Filter filter = filterBuilder.attribute(Metacard.ANY_TEXT).like().text(SAMPLE_SEARCH_PHRASE);

    SourceResponse response = source.query(new QueryRequestImpl(new QueryImpl(filter)));
    assertThat(response.getHits(), is(1L));
    List<Result> results = response.getResults();
    assertThat(results, hasSize(1));
    Metacard metacard = results.get(0).getMetacard();
    assertThat(metacard.getId(), is(SAMPLE_ID));
    assertThat(metacard.getContentTypeName(), is(RESOURCE_TAG));
  }

  /**
   * Basic retrieve product case. Tests the url sent to the connection is correct.
   *
   * @throws ResourceNotSupportedException
   * @throws IOException
   * @throws ResourceNotFoundException
   */
  @Test
  public void testRetrieveResource() throws Exception {

    // given
    ResourceReader mockReader = mock(ResourceReader.class);
    when(response.getEntity()).thenReturn(getBinaryData());
    when(mockReader.retrieveResource(any(URI.class), any(Map.class)))
        .thenReturn(new ResourceResponseImpl(new ResourceImpl(getBinaryData(), "")));
    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, Arrays.asList("application/octet-stream"));
    when(response.getHeaders()).thenReturn(headers);

    source.setLocalQueryOnly(true);
    source.setInputTransformer(getMockInputTransformer());
    source.setResourceReader(mockReader);

    Map<String, Serializable> requestProperties = new HashMap<>();
    requestProperties.put(Metacard.ID, SAMPLE_ID);

    // when
    ResourceResponse response = source.retrieveResource(null, requestProperties);
    assertThat(response.getResource().getByteArray().length, is(3));
  }

  /**
   * Retrieve Product case using Basic Authentication. Test that the properties map passed to the
   * resource reader includes a username and password.
   *
   * @throws Exception
   */
  @Test
  public void testRetrieveResourceBasicAuth() throws Exception {

    ResourceReader mockReader = mock(ResourceReader.class);
    when(response.getEntity()).thenReturn(getBinaryData());
    when(mockReader.retrieveResource(
            any(URI.class),
            argThat(
                allOf(
                    hasEntry("username", (Serializable) "user"),
                    hasEntry("password", (Serializable) "secret")))))
        .thenReturn(new ResourceResponseImpl(new ResourceImpl(getBinaryData(), "")));
    when(encryptionService.decryptValue("secret")).thenReturn("secret");
    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, Arrays.asList("application/octet-stream"));
    when(response.getHeaders()).thenReturn(headers);

    source.setLocalQueryOnly(true);
    source.setInputTransformer(getMockInputTransformer());
    source.setResourceReader(mockReader);
    source.setUsername("user");
    source.setPassword("secret");

    Map<String, Serializable> requestProperties = new HashMap<>();
    requestProperties.put(Metacard.ID, SAMPLE_ID);

    ResourceResponse response = source.retrieveResource(null, requestProperties);
    assertThat(response.getResource().getByteArray().length, is(3));
  }

  /**
   * Given all null params, nothing will be returned, expect an exception.
   *
   * @throws ResourceNotSupportedException
   */
  @Test(expected = ResourceNotFoundException.class)
  public void testRetrieveNullProduct()
      throws ResourceNotFoundException, ResourceNotSupportedException, IOException {
    source.retrieveResource(null, null);
  }

  @Test
  public void testQueryQueryByMetacardIdFollowedByAnyTextQuery() throws Exception {
    when(response.getEntity()).thenReturn(getSampleXmlStream()).thenReturn(getSampleAtomStream());

    // Metacard ID filter
    Filter idFilter = filterBuilder.attribute(Metacard.ID).equalTo().text(SAMPLE_ID);

    // Any text filter
    Filter anyTextFilter =
        filterBuilder.attribute(Metacard.ANY_TEXT).like().text(SAMPLE_SEARCH_PHRASE);

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

  @Test
  public void testQueryQueryByMetacardIdFollowedByAnyTextQueryRss() throws Exception {
    when(response.getEntity()).thenReturn(getSampleXmlStream()).thenReturn(getSampleRssStream());

    // Metacard ID filter
    Filter idFilter = filterBuilder.attribute(Metacard.ID).equalTo().text(SAMPLE_ID);

    // Any text filter
    Filter anyTextFilter =
        filterBuilder.attribute(Metacard.ANY_TEXT).like().text(SAMPLE_SEARCH_PHRASE);

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

  protected InputTransformer getMockInputTransformer() throws Exception {
    InputTransformer inputTransformer = mock(InputTransformer.class);

    Metacard generatedMetacard = getSimpleMetacard();

    when(inputTransformer.transform(isA(InputStream.class))).thenReturn(generatedMetacard);
    when(inputTransformer.transform(isA(InputStream.class), isA(String.class)))
        .thenReturn(generatedMetacard);

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

  /** This test is to demonstrate a real-world example of using a foreign markup consumer. */
  @Test
  public void testForeignMarkupExample() throws UnsupportedQueryException {
    ForeignMarkupConsumerExample foreignMarkupConsumer = new ForeignMarkupConsumerExample();

    source.setForeignMarkupBiConsumer(foreignMarkupConsumer);

    source.setMarkUpSet(Collections.singletonList(RESOURCE_TAG));
    when(response.getEntity()).thenReturn(getSampleAtomStreamWithForeignMarkup());

    Filter filter = filterBuilder.attribute(Metacard.ANY_TEXT).like().text(SAMPLE_SEARCH_PHRASE);

    SourceResponse response = source.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertThat(foreignMarkupConsumer.getTotalResults(), is(1L));
  }

  /** Test to make sure the foreign markup consumer is called as expected. */
  @Test
  public void testForeignMarkupConsumer() throws UnsupportedQueryException, IOException {

    BiConsumer<List<Element>, SourceResponse> foreignMarkupConsumer = mock(BiConsumer.class);

    source.setForeignMarkupBiConsumer(foreignMarkupConsumer);

    source.setMarkUpSet(Collections.singletonList(RESOURCE_TAG));
    when(response.getEntity()).thenReturn(getSampleAtomStreamWithForeignMarkup());

    Filter filter = filterBuilder.attribute(Metacard.ANY_TEXT).like().text(SAMPLE_SEARCH_PHRASE);

    SourceResponse response = source.query(new QueryRequestImpl(new QueryImpl(filter)));

    ArgumentCaptor<List> elementListCaptor = ArgumentCaptor.forClass(List.class);

    verify(foreignMarkupConsumer).accept(elementListCaptor.capture(), any());

    assertThat(elementListCaptor.getAllValues().size(), is(1));

    List<String> names =
        ((List<?>) elementListCaptor.getAllValues().get(0))
            .stream()
            .filter(Element.class::isInstance)
            .map(Element.class::cast)
            .map(Element::getName)
            .collect(Collectors.toList());

    assertThat(names, is(Arrays.asList("totalResults", "itemsPerPage", "startIndex")));
  }

  @Test
  public void testSourceId() throws UnsupportedQueryException, IOException {

    BiConsumer<List<Element>, SourceResponse> foreignMarkupConsumer = mock(BiConsumer.class);

    source.setForeignMarkupBiConsumer(foreignMarkupConsumer);

    source.setMarkUpSet(Collections.singletonList(RESOURCE_TAG));
    when(response.getEntity()).thenReturn(getSampleAtomStreamWithForeignMarkup());

    Filter filter = filterBuilder.attribute(Metacard.ANY_TEXT).like().text(SAMPLE_SEARCH_PHRASE);

    SourceResponse response = source.query(new QueryRequestImpl(new QueryImpl(filter)));
    assertSourceId(response);
  }

  private void assertSourceId(SourceResponse sourceResponse) {
    if (sourceResponse != null && sourceResponse.getResults() != null) {
      sourceResponse
          .getResults()
          .stream()
          .filter(Objects::nonNull)
          .map(Result::getMetacard)
          .filter(Objects::nonNull)
          .forEach(metacard -> assertThat(metacard.getSourceId(), is(SOURCE_ID)));
    }
  }

  protected SecureCxfClientFactory getMockFactory(WebClient client) {
    SecureCxfClientFactory factory = mock(SecureCxfClientFactory.class);

    doReturn(client).when(factory).getClient();
    doReturn(client)
        .when(factory)
        .getWebClientForSubject(any(org.apache.shiro.subject.Subject.class));
    doReturn(client).when(factory).getWebClient();

    return factory;
  }

  /** Example of a real-world foreign markup consumer. */
  private static class ForeignMarkupConsumerExample
      implements BiConsumer<List<Element>, SourceResponse> {

    private Long totalResults;

    @Override
    public void accept(List<Element> elements, SourceResponse sourceResponse) {
      for (Element element : elements) {
        if (element.getName().equals("totalResults")) {
          try {
            totalResults = Long.parseLong(element.getContent(0).getValue());
          } catch (NumberFormatException | IndexOutOfBoundsException e) {
            // ignore
          }
        }
      }
    }

    public Long getTotalResults() {
      return totalResults;
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
    public OverriddenOpenSearchSource(
        FilterAdapter filterAdapter, EncryptionService encryptionService) {
      super(filterAdapter, openSearchParserImpl, openSearchFilterVisitor, encryptionService);
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
    protected SecureCxfClientFactory createClientFactory(
        String url, String username, String password) {
      return this.factory;
    }
  }
}
