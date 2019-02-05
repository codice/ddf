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
import static org.junit.Assert.fail;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import javax.xml.bind.JAXBException;
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
public class SearchFormsApplicationTest {
  @Parameterized.Parameters(name = "Verify search form REST I/O for symbol: {0}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          // Following symbols may have different escaped representations depending on data flow
          //          {"\""}, {"\\"}, {"<"}, {">"}, {"&"},
          {"'"},
          {"{"},
          {"}"},
          {"["},
          {"]"},
          {":"},
          {";"},
          {","},
          {"."},
          {"?"},
          {"/"},
          {"|"},
          {"-"},
          {"_"},
          {"+"},
          {"="},
          {"*"},
          {"^"},
          {"%"},
          {"$"},
          {"#"},
          {"@"},
          {"!"},
          {"~"},
          {"`"},
          {"("},
          {")"}
        });
  }

  private static final URL FILTER_RESOURCES_DIR =
      SearchFormsLoaderTest.class.getResource("/forms/app");

  private static final Header CONTENT_IS_JSON = new Header("Content-Type", "application/json");

  private static final String LOCALHOST_FORMS_QUERY = "http://localhost:4567/forms/query";

  private static final String TEMPLATE_FORM_METACARD_JSON_SIMPLE =
      getContentsOfFile("form-simple.json");

  private static final String TEMPLATE_FORM_METACARD_JSON_SIMPLE_RESPONSE =
      removePrettyPrintingOnJson(getContentsOfFile("form-simple-response.json"));

  private static final String TEMPLATE_FORM_FILTER_XML_SIMPLE =
      getContentsOfFile("form-filter-simple.xml");

  private static final String CANNED_TITLE = "MY_TITLE";

  private static final String CANNED_DESCRIPTION = "MY_DESCRIPTION";

  private static final String CANNED_ID = "abcdefg";

  private static final String CANNED_ISO_DATE = "2018-12-10T13:09:40Z";

  private static final FilterBuilder MOCK_BUILDER = new GeotoolsFilterBuilder();

  private static final CatalogFramework MOCK_FRAMEWORK = mock(CatalogFramework.class);

  private static final Subject MOCK_SUBJECT = mock(Subject.class);

  private static final ConfigurationApplication MOCK_CONFIG = mock(ConfigurationApplication.class);

  private static final TemplateTransformer TRANSFORMER =
      new TemplateTransformer(getWriter(), getRegistry());

  private static final EndpointUtil UTIL =
      new EndpointUtil(
          null, // No interaction
          MOCK_FRAMEWORK,
          MOCK_BUILDER,
          null, // No interaction
          null, // No interaction
          null, // No interaction
          null, // No interaction
          MOCK_CONFIG);

  private static final SearchFormsApplication APPLICATION =
      new SearchFormsApplication(
          MOCK_FRAMEWORK, MOCK_BUILDER, TRANSFORMER, UTIL, () -> MOCK_SUBJECT);

  private final ArgumentCaptor<CreateRequest> requestCaptor;

  private final String formFilterXml;

  private final String formRequestJson;

  private final String formResponseJson;

  // Ctor necessary for parameterization
  public SearchFormsApplicationTest(String symbolUnderTest) {
    this.requestCaptor = ArgumentCaptor.forClass(CreateRequest.class);
    StrSubstitutor substitutor =
        new StrSubstitutor(ImmutableMap.of("value", "hello" + symbolUnderTest));

    this.formFilterXml = substitutor.replace(TEMPLATE_FORM_FILTER_XML_SIMPLE);
    this.formRequestJson = substitutor.replace(TEMPLATE_FORM_METACARD_JSON_SIMPLE);
    this.formResponseJson = substitutor.replace(TEMPLATE_FORM_METACARD_JSON_SIMPLE_RESPONSE);
  }

  @BeforeClass
  public static void setUpClass() {
    APPLICATION.init();
    Spark.awaitInitialization();
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
            .post(LOCALHOST_FORMS_QUERY)
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
        RestAssured.given().header(CONTENT_IS_JSON).get(LOCALHOST_FORMS_QUERY).body().asString();
    assertThat(json, is(formResponseJson));
  }

  private String getCapturedXml() {
    Metacard searchForm = requestCaptor.getValue().getMetacards().get(0);
    return ((QueryTemplateMetacard) searchForm).getFormsFilter();
  }

  private static String removePrettyPrintingOnJson(String json) {
    return json.replaceAll("\\h|\\v", "");
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

  private static String getContentsOfFile(String... resourceRoute) {
    try {
      File dir = new File(FILTER_RESOURCES_DIR.toURI());
      if (!dir.exists()) {
        fail(
            format(
                "Invalid setup parameter '%s', the directory does not exist",
                FILTER_RESOURCES_DIR.toString()));
      }

      Path route = Arrays.stream(resourceRoute).map(Paths::get).reduce(Path::resolve).orElse(null);
      if (route == null) {
        fail("Could not reduce resource route to a single path");
      }

      File resourceFile = dir.toPath().resolve(route).toFile();
      if (!resourceFile.exists()) {
        fail("File was not found " + resourceFile.getAbsolutePath());
      }

      try (FileInputStream fis = new FileInputStream(resourceFile)) {
        return IOUtils.toString(fis, StandardCharsets.UTF_8);
      }
    } catch (IOException | URISyntaxException e) {
      throw new AssertionError("Could not complete test setup due to exception", e);
    }
  }
}
