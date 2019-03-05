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
package org.codice.ddf.catalog.ui.forms;

import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static spark.Spark.stop;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Header;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.Subject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.codice.ddf.catalog.ui.config.ConfigurationApplication;
import org.codice.ddf.catalog.ui.forms.data.QueryTemplateMetacard;
import org.codice.ddf.catalog.ui.forms.filter.FilterWriter;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.opengis.filter.Filter;
import spark.Spark;

/**
 * Spin up the {@link SearchFormsApplication} spark application and verify the REST contract against
 * a mocked {@link CatalogFramework}.
 *
 * <p>This test suite uses HTTP to verify one of the data contracts for catalog-ui-search. By
 * testing the network directly, the tests remain stable regardless of the JSON solution used.
 *
 * <p>This particular test suite verifies both directions of JSON and XML transforms using
 * parameterized REST messages that are interpolated with actual test values at runtime. The
 * objective of this test suite is to ensure serialization can handle any special character.
 */
@RunWith(Parameterized.class)
public class SearchFormsSymbolsIT {
  /**
   * Set of symbols upon which to parameterize the tests; defined as they appear in a JSON document.
   */
  @Parameterized.Parameters(name = "Verify search form REST I/O for symbol: {0}")
  public static Iterable<?> data() {
    return Arrays.asList(
        "'", "\\\"", "\\\\", ">", "<", "&", "{", "}", "[", "]", ":", ";", ",", ".", "?", "/", "|",
        "-", "_", "+", "=", "*", "^", "%", "$", "#", "@", "!", "~", "`", "(", ")");
  }

  /**
   * Not all JSON representations are communitively validatable against XML. This mapping is what
   * all the tests use to ensure expected XML counterparts are built correctly.
   */
  private static final Map<String, String> JSON_TO_XML_SYMBOL_MAPPING =
      ImmutableMap.of(
          "\\\"", "\"",
          "\\\\", "\\",
          ">", "&gt;",
          "<", "&lt;",
          "&", "&amp;");

  private static final Header CONTENT_IS_JSON = new Header("Content-Type", "application/json");

  private static final String TEMPLATE_FORM_METACARD_JSON_SIMPLE =
      getContentsOfFile("/forms/app/form-simple.json");

  private static final String TEMPLATE_FORM_METACARD_JSON_SIMPLE_RESPONSE =
      removePrettyPrintingOnJson(getContentsOfFile("/forms/app/form-simple-response.json"));

  private static final String TEMPLATE_FORM_FILTER_XML_SIMPLE =
      removePrettyPrintingOnXml(getContentsOfFile("/forms/app/form-filter-simple.xml"));

  private static final String MSG_COULD_NOT_LOAD = "Could not load test resource";

  private static final String CANNED_TITLE = "MY_TITLE";

  private static final String CANNED_DESCRIPTION = "MY_DESCRIPTION";

  private static final String CANNED_ID = "abcdefg";

  private static final String CANNED_ISO_DATE = "2018-12-10T13:09:40Z";

  private static final FilterBuilder FILTER_BUILDER = new GeotoolsFilterBuilder();

  private static final CatalogFramework MOCK_FRAMEWORK = mock(CatalogFramework.class);

  private static final Subject MOCK_SUBJECT = mock(Subject.class);

  private static final ConfigurationApplication MOCK_CONFIG = mock(ConfigurationApplication.class);

  private static final TemplateTransformer TRANSFORMER =
      new TemplateTransformer(getWriter(), getRegistry());

  private static final EndpointUtil UTIL =
      new EndpointUtil(
          null, // No interaction
          MOCK_FRAMEWORK,
          FILTER_BUILDER,
          null, // No interaction
          null, // No interaction
          null, // No interaction
          null, // No interaction
          MOCK_CONFIG);

  private static final SearchFormsApplication APPLICATION =
      new SearchFormsApplication(
          MOCK_FRAMEWORK, FILTER_BUILDER, TRANSFORMER, UTIL, () -> MOCK_SUBJECT);

  // Will be initialized by setUpClass() when the port is known
  private static String localhostFormsUrl = null;

  private final ArgumentCaptor<CreateRequest> requestCaptor;

  private final String formFilterXml;

  private final String formRequestJson;

  private final String formResponseJson;

  // Ctor necessary for parameterization
  public SearchFormsSymbolsIT(String symbolUnderTest) {
    this.requestCaptor = ArgumentCaptor.forClass(CreateRequest.class);
    StrSubstitutor substitutor =
        new StrSubstitutor(ImmutableMap.of("value", "hello" + symbolUnderTest));

    this.formRequestJson = substitutor.replace(TEMPLATE_FORM_METACARD_JSON_SIMPLE);
    this.formResponseJson = substitutor.replace(TEMPLATE_FORM_METACARD_JSON_SIMPLE_RESPONSE);

    final String xmlVariation = JSON_TO_XML_SYMBOL_MAPPING.get(symbolUnderTest);
    if (xmlVariation == null) {
      this.formFilterXml = substitutor.replace(TEMPLATE_FORM_FILTER_XML_SIMPLE);
    } else {
      StrSubstitutor xmlSubstitutor =
          new StrSubstitutor(ImmutableMap.of("value", "hello" + xmlVariation));
      this.formFilterXml = xmlSubstitutor.replace(TEMPLATE_FORM_FILTER_XML_SIMPLE);
    }
  }

  @BeforeClass
  public static void setUpClass() {
    Spark.port(getAvailablePort());
    APPLICATION.init();
    Spark.awaitInitialization();
    localhostFormsUrl = format("http://localhost:%d/forms/query", Spark.port());
  }

  @AfterClass
  public static void tearDownClass() {
    stop();
  }

  @Before
  public void setUp() {
    when(MOCK_SUBJECT.isGuest()).thenReturn(false);
    when(MOCK_CONFIG.getMaximumUploadSize()).thenReturn(1024);
  }

  @After
  public void tearDown() {
    reset(MOCK_FRAMEWORK, MOCK_SUBJECT, MOCK_CONFIG);
  }

  @Test
  public void testJsonToXml() throws IngestException, SourceUnavailableException {
    // Prepare
    MetacardImpl metacardWithIdAndCreatedDate = new MetacardImpl();
    metacardWithIdAndCreatedDate.setId(CANNED_ID);
    metacardWithIdAndCreatedDate.setCreatedDate(new Date());

    doReturn(
            new CreateResponseImpl(
                new CreateRequestImpl(Collections.emptyList()),
                Collections.emptyMap(),
                Collections.singletonList(metacardWithIdAndCreatedDate)))
        .when(MOCK_FRAMEWORK)
        .create(requestCaptor.capture());

    // Execute
    int statusCode =
        RestAssured.given()
            .header(CONTENT_IS_JSON)
            .content(formRequestJson)
            .post(localhostFormsUrl)
            .statusCode();
    assertThat(statusCode, is(200));
    assertThat(getCapturedXml(), is(formFilterXml));
  }

  @Test
  public void testXmlToJson()
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    // Prepare
    QueryTemplateMetacard queryTemplateMetacard =
        new QueryTemplateMetacard(CANNED_TITLE, CANNED_DESCRIPTION, CANNED_ID);
    queryTemplateMetacard.setFormsFilter(formFilterXml);
    queryTemplateMetacard.setCreatedDate(Date.from(Instant.parse(CANNED_ISO_DATE)));
    queryTemplateMetacard.setModifiedDate(Date.from(Instant.parse(CANNED_ISO_DATE)));

    QueryResponseImpl response =
        new QueryResponseImpl(new QueryRequestImpl(new QueryImpl(Filter.INCLUDE)));
    response.addResult(new ResultImpl(queryTemplateMetacard), true);

    doReturn(response).when(MOCK_FRAMEWORK).query(any());

    // Execute
    String json =
        RestAssured.given().header(CONTENT_IS_JSON).get(localhostFormsUrl).body().asString();
    assertThat(json, is(formResponseJson));
  }

  private String getCapturedXml() {
    Metacard searchForm = requestCaptor.getValue().getMetacards().get(0);
    return ((QueryTemplateMetacard) searchForm).getFormsFilter();
  }

  /**
   * Given a pretty JSON string, flatten out the string to enable string comparison operations
   * regardless of formatting.
   *
   * @param json the input JSON string to flatten to a single line.
   * @return a single line string of the given JSON.
   */
  private static String removePrettyPrintingOnJson(String json) {
    return json.replaceAll("\\h|\\v", "");
  }

  /**
   * Given a pretty XML string, flatten out the string to enable string comparison operations
   * regardless of formatting.
   *
   * @param xml the input XML string to flatten to a single line.
   * @return a single line string of the given XML.
   */
  private static String removePrettyPrintingOnXml(String xml) {
    try {
      TransformerFactory factory = TransformerFactory.newInstance();
      Source xslt = new StreamSource(getResourceFile("/forms/app/inline-xml.xslt"));
      Transformer transformer = factory.newTransformer(xslt);

      Source text = new StreamSource(new ByteArrayInputStream(xml.getBytes()));
      StringWriter destination = new StringWriter();

      transformer.transform(text, new StreamResult(destination));

      // Replace the header that the XSLT transform removed
      return destination
          .toString()
          .replace(
              "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
              "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
    } catch (TransformerException e) {
      throw new AssertionError("Could not remove pretty printing from XML, " + xml, e);
    }
  }

  private static FilterWriter getWriter() {
    try {
      return new FilterWriter(true);
    } catch (JAXBException e) {
      throw new AssertionError("Could not make filter writer, " + e.getMessage());
    }
  }

  private static AttributeRegistry getRegistry() {
    AttributeRegistry registry = mock(AttributeRegistry.class);
    doReturn(Optional.empty()).when(registry).lookup(any());
    return registry;
  }

  private static File getResourceFile(String resourceRoute) {
    try {
      URL url = SearchFormsSymbolsIT.class.getResource(resourceRoute);
      if (url == null) {
        throw new AssertionError(MSG_COULD_NOT_LOAD + ", the URL came back null");
      }
      return new File(url.toURI());
    } catch (URISyntaxException e) {
      throw new AssertionError(MSG_COULD_NOT_LOAD + ", URI syntax not valid", e);
    }
  }

  private static String getContentsOfFile(String resourceRoute) {
    File resourceFile = getResourceFile(resourceRoute);
    try (FileInputStream fis = new FileInputStream(resourceFile)) {
      return IOUtils.toString(fis, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new AssertionError("Could not complete test setup due to exception", e);
    }
  }

  /**
   * Return an available port number after binding and releasing it.
   *
   * <p>The discovered port should be available for another client to bind to by the time this
   * function has returned. Given this detail, running unit tests on environments that are in the
   * process of being provisioned or are otherwise in a state of flux may cause erroneous failures
   * due to port binding race conditions.
   *
   * @return a port number the caller can <b>reasonably</b> assume is available to bind to.
   * @throws AssertionError if no port was available to bind to or the binding operation failed.
   */
  private static int getAvailablePort() {
    ServerSocket socket = null;
    try {
      socket = new ServerSocket(0);
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new AssertionError("Could not autobind to available port", e);
    } finally {
      tryCloseSocket(socket);
    }
  }

  private static void tryCloseSocket(@Nullable ServerSocket socket) {
    try {
      if (socket != null) {
        socket.close();
      }
    } catch (IOException e) {
      throw new AssertionError(
          "Problem while enumerating ports (specifically, port " + socket.getLocalPort() + ")", e);
    }
  }
}
