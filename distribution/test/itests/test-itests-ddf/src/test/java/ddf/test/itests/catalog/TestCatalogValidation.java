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
package ddf.test.itests.catalog;

import static com.jayway.restassured.RestAssured.given;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.deleteMetacardAndWait;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingest;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingestXmlFromResourceAndWait;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureAuthZRealm;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureEnforceValidityErrorsAndWarnings;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureEnforcedMetacardValidators;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureFilterInvalidMetacards;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureMetacardValidityFilterPlugin;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureShowInvalidMetacards;
import static org.codice.ddf.itests.common.csw.CswQueryBuilder.NOT;
import static org.codice.ddf.itests.common.csw.CswQueryBuilder.OR;
import static org.codice.ddf.itests.common.csw.CswQueryBuilder.PROPERTY_IS_EQUAL_TO;
import static org.codice.ddf.itests.common.csw.CswQueryBuilder.PROPERTY_IS_LIKE;
import static org.codice.ddf.itests.common.opensearch.OpenSearchTestCommons.getOpenSearch;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.xml.HasXPath.hasXPath;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.ValidatableResponse;
import ddf.catalog.data.types.Validation;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.csw.CswQueryBuilder;
import org.codice.ddf.test.common.LoggingUtils;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

/**
 * Tests catalog validation This test was created to pull out 16 tests in TestCatalog that were
 * starting/stopping the sample-validator each time. Since there is almost no overhead now for a new
 * class it is faster to just start the feature once for all 16 of the tests instead of toggling it
 * for each one.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestCatalogValidation extends AbstractIntegrationTest {

  private static final String METACARD_X_PATH = "/metacards/metacard[@id='%s']";

  @Rule public TestName testName = new TestName();

  public static String getGetRecordByIdProductRetrievalUrl() {
    return "?service=CSW&version=2.0.2&request=GetRecordById&NAMESPACE=xmlns="
        + "http://www.opengis.net/cat/csw/2.0.2&"
        + "outputFormat=application/octet-stream&outputSchema="
        + "http://www.iana.org/assignments/media-types/application/octet-stream&"
        + "id=placeholder_id";
  }

  public static String getSimpleXml(String uri) {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        + getFileContent(
            XML_RECORD_RESOURCE_PATH + "/SimpleXmlNoDecMetacard", ImmutableMap.of("uri", uri));
  }

  @BeforeExam
  public void beforeExam() throws Exception {
    try {
      waitForSystemReady();
      getServiceManager().startFeature(true, "sample-validator");
    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @BeforeExam: ");
    }
  }

  @Before
  public void setup() throws IOException {
    // to allow for waiting ingest
    configureShowInvalidMetacards("true", "true", getAdminConfig());
    configureFilterInvalidMetacards("false", "false", getAdminConfig());
    configureEnforceValidityErrorsAndWarnings("true", "false", getAdminConfig());
    configureMetacardValidityFilterPlugin(Arrays.asList(""), getAdminConfig());
    clearCatalogAndWait();
  }

  @Test
  public void testEnforceValidityErrorsOnly() throws Exception {
    // Configure to enforce validator
    Dictionary<String, Object> enforcedValidatorsProperties =
        configureEnforcedMetacardValidators(
            Collections.singletonList("sample-validator"), getAdminConfig());

    // Configure to enforce errors but not warnings
    Dictionary<String, Object> validityEnforcementProperties =
        configureEnforceValidityErrorsAndWarnings("true", "false", getAdminConfig());

    try {
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml");
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
      ingestXmlFromResourceWaitForFailure(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml");

      String query =
          new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*").getQuery();
      ValidatableResponse response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();

      // clean metacard and warning metacard should be in results but not error one
      response.body(containsString("warning metacard"));
      response.body(containsString("clean metacard"));
      response.body(not(containsString("error metacard")));
    } finally {
      configureEnforcedMetacardValidators(enforcedValidatorsProperties, getAdminConfig());
      configureEnforceValidityErrorsAndWarnings(validityEnforcementProperties, getAdminConfig());
    }
  }

  @Test
  public void testEnforceValidityWarningsOnly() throws Exception {
    // Configure to enforce validator
    Dictionary<String, Object> enforcedValidatorsProperties =
        configureEnforcedMetacardValidators(
            Collections.singletonList("sample-validator"), getAdminConfig());

    // Configure to enforce warnings but not errors
    Dictionary<String, Object> validityEnforcementProperties =
        configureEnforceValidityErrorsAndWarnings("false", "true", getAdminConfig());

    try {
      ingestXmlFromResourceWaitForFailure(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml");
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml");

      String query =
          new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*").getQuery();
      ValidatableResponse response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();

      // clean metacard and error metacard should be in results but not warning one
      response.body(not(containsString("warning metacard")));
      response.body(containsString("clean metacard"));
      response.body(containsString("error metacard"));
    } finally {
      configureEnforcedMetacardValidators(enforcedValidatorsProperties, getAdminConfig());
      configureEnforceValidityErrorsAndWarnings(validityEnforcementProperties, getAdminConfig());
    }
  }

  @Test
  public void testEnforceValidityErrorsAndWarnings() throws Exception {
    // Configure to enforce validator
    Dictionary<String, Object> enforcedValidatorsProperties =
        configureEnforcedMetacardValidators(
            Collections.singletonList("sample-validator"), getAdminConfig());

    // Configure to enforce errors and warnings
    Dictionary<String, Object> validityEnforcementProperties =
        configureEnforceValidityErrorsAndWarnings("true", "true", getAdminConfig());

    try {
      ingestXmlFromResourceWaitForFailure(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml");
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
      ingestXmlFromResourceWaitForFailure(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml");

      testWithRetry(
          () -> {
            String query =
                new CswQueryBuilder()
                    .addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*")
                    .getQuery();
            ValidatableResponse response =
                given()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();

            // clean metacard should be in results but not invalid ones
            response.body(not(containsString("warning metacard")));
            response.body(containsString("clean metacard"));
            response.body(not(containsString("error metacard")));
          });
    } finally {
      configureEnforcedMetacardValidators(enforcedValidatorsProperties, getAdminConfig());
      configureEnforceValidityErrorsAndWarnings(validityEnforcementProperties, getAdminConfig());
    }
  }

  @Test
  public void testNoEnforceValidityErrorsOrWarnings() throws Exception {
    // Configure to enforce validator
    Dictionary<String, Object> enforcedValidatorsProperties =
        configureEnforcedMetacardValidators(
            Collections.singletonList("sample-validator"), getAdminConfig());

    // Configure to enforce neither errors nor warnings
    Dictionary<String, Object> validityEnforcementProperties =
        configureEnforceValidityErrorsAndWarnings("false", "false", getAdminConfig());

    try {
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml");
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml");

      String query =
          new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*").getQuery();
      ValidatableResponse response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();

      response.body(containsString("warning metacard"));
      response.body(containsString("clean metacard"));
      response.body(containsString("error metacard"));
    } finally {
      configureEnforcedMetacardValidators(enforcedValidatorsProperties, getAdminConfig());
      configureEnforceValidityErrorsAndWarnings(validityEnforcementProperties, getAdminConfig());
    }
  }

  @Test
  public void testQueryByErrorFailedValidators() throws Exception {
    // Don't enforce the validator, so that it will be marked but ingested
    Dictionary<String, Object> enforcedValidatorsProperties =
        configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());

    try {
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml");
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml");

      String query =
          new CswQueryBuilder()
              .addAttributeFilter(
                  PROPERTY_IS_LIKE, Validation.FAILED_VALIDATORS_ERRORS, "sample-validator")
              .getQuery();
      ValidatableResponse response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();

      response.body(not(containsString("warning metacard")));
      response.body(not(containsString("clean metacard")));
      response.body(containsString("error metacard"));
    } finally {
      configureEnforcedMetacardValidators(enforcedValidatorsProperties, getAdminConfig());
    }
  }

  @Test
  public void testQueryByWarningFailedValidators() throws Exception {
    // Don't enforce the validator, so that it will be marked but ingested
    Dictionary<String, Object> enforcedValidatorsProperties =
        configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());

    try {
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml");
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml");

      String query =
          new CswQueryBuilder()
              .addAttributeFilter(
                  PROPERTY_IS_LIKE, Validation.FAILED_VALIDATORS_WARNINGS, "sample-validator")
              .getQuery();
      ValidatableResponse response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();

      // clean metacard and warning metacard should be in results but not error one
      response.body(not(containsString("error metacard")));
      response.body(not(containsString("clean metacard")));
      response.body(containsString("warning metacard"));
    } finally {
      configureEnforcedMetacardValidators(enforcedValidatorsProperties, getAdminConfig());
    }
  }

  @Test
  public void testFilterPluginWarningsOnly() throws Exception {
    // Configure not enforcing validators so invalid metacards can ingest
    Dictionary<String, Object> enforcedValidatorsProperties =
        configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());

    // Configure to filter metacards with validation warnings but not validation errors
    Dictionary<String, Object> filterProperties =
        configureFilterInvalidMetacards("false", "true", getAdminConfig());

    // Configure invalid filtering
    Dictionary<String, Object> validityProperties =
        configureMetacardValidityFilterPlugin(
            Arrays.asList("invalid-state=system-admin"), getAdminConfig());
    try {
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml");
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml");

      testWithRetry(
          () -> {
            String query =
                new CswQueryBuilder()
                    .addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*")
                    .getQuery();
            ValidatableResponse response =
                given()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();

            // clean metacard should be in results but not invalid one
            response.body(not(containsString("warning metacard")));
            response.body(containsString("clean metacard"));
            response.body(containsString("error metacard"));
          });
    } finally {
      configureEnforcedMetacardValidators(enforcedValidatorsProperties, getAdminConfig());
      configureFilterInvalidMetacards(filterProperties, getAdminConfig());
      configureMetacardValidityFilterPlugin(validityProperties, getAdminConfig());
    }
  }

  @Test
  public void testFilterPluginErrorsOnly() throws Exception {
    // Configure not enforcing validators so invalid metacards can ingest
    Dictionary<String, Object> enforcedValidatorsProperties =
        configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());

    // Configure invalid filtering
    Dictionary<String, Object> validityProperties =
        configureMetacardValidityFilterPlugin(
            Arrays.asList("invalid-state=system-admin"), getAdminConfig());

    // Configure to filter metacards with validation errors but not validation warnings
    Dictionary<String, Object> filterProperties =
        configureFilterInvalidMetacards("true", "false", getAdminConfig());

    try {
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml");
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml");

      testWithRetry(
          () -> {
            String query =
                new CswQueryBuilder()
                    .addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*")
                    .getQuery();
            ValidatableResponse response =
                given()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();

            // clean metacard should be in results but not invalid one
            response.body(not(containsString("error metacard")));
            response.body(containsString("clean metacard"));
            response.body(containsString("warning metacard"));
          });
    } finally {
      configureEnforcedMetacardValidators(enforcedValidatorsProperties, getAdminConfig());
      configureFilterInvalidMetacards(filterProperties, getAdminConfig());
      configureMetacardValidityFilterPlugin(validityProperties, getAdminConfig());
    }
  }

  @Test
  public void testFilterPluginWarningsAndErrors() throws Exception {
    // Configure not enforcing validators so invalid metacards can ingest
    Dictionary<String, Object> enforcedValidatorsProperties =
        configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());

    // Configure invalid filtering
    Dictionary<String, Object> validityProperties =
        configureMetacardValidityFilterPlugin(
            Arrays.asList("invalid-state=system-admin"), getAdminConfig());

    // configure to filter both metacards with validation errors and validation warnings
    Dictionary<String, Object> filterProperties =
        configureFilterInvalidMetacards("true", "true", getAdminConfig());

    try {
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml");
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml");

      testWithRetry(
          () -> {
            String query =
                new CswQueryBuilder()
                    .addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*")
                    .getQuery();
            ValidatableResponse response =
                given()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();

            // clean metacard should be in results but not invalid one
            response.body(not(containsString("error metacard")));
            response.body(not(containsString("warning metacard")));
            response.body(containsString("clean metacard"));
          });
    } finally {
      configureEnforcedMetacardValidators(enforcedValidatorsProperties, getAdminConfig());
      configureFilterInvalidMetacards(filterProperties, getAdminConfig());
      configureMetacardValidityFilterPlugin(validityProperties, getAdminConfig());
    }
  }

  @Test
  public void testFilterPluginNoFiltering() throws Exception {
    // Configure not enforcing validators so invalid metacards can ingest
    Dictionary<String, Object> enforcedValidatorsProperties =
        configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());

    // Configure invalid filtering
    Dictionary<String, Object> validityProperties =
        configureMetacardValidityFilterPlugin(
            Arrays.asList("invalid-state=system-admin"), getAdminConfig());
    try {
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml");
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
      ingestXmlFromResourceAndWait(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml");

      testWithRetry(
          () -> {
            String query =
                new CswQueryBuilder()
                    .addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*")
                    .getQuery();
            ValidatableResponse response =
                given()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();

            // clean metacard should be in results but not invalid one
            response.body(containsString("error metacard"));
            response.body(containsString("warning metacard"));
            response.body(containsString("clean metacard"));
          });
    } finally {
      configureEnforcedMetacardValidators(enforcedValidatorsProperties, getAdminConfig());
      configureMetacardValidityFilterPlugin(validityProperties, getAdminConfig());
    }
  }

  @Test
  public void testValidationEnforced() throws Exception {
    // Update metacardMarkerPlugin config with enforcedMetacardValidators
    Dictionary<String, Object> enforcedValidatorsProperties =
        configureEnforcedMetacardValidators(
            Collections.singletonList("sample-validator"), getAdminConfig());

    try {
      String id1 = ingestXmlFromResourceAndWait("/metacard1.xml");
      ingestXmlFromResourceWaitForFailure("/metacard2.xml");
      // Search for all entries, implicit "validation-warnings is null" and "validation-errors is
      // null"
      // should get added by ValidationQueryFactory
      String query =
          new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*").getQuery();
      ValidatableResponse response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();
      // Assert Metacard1 is in results AND not Metacard2
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));

      // Search for all entries that have no validation warnings or errors
      query =
          new CswQueryBuilder()
              .addPropertyIsNullAttributeFilter(Validation.VALIDATION_WARNINGS)
              .getQuery();
      response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();
      // Assert Metacard1 is in results AND not Metacard2
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));

      // Search for all entries that have validation-warnings from sample-validator or no validation
      // warnings
      // Only search that will actually return all entries

      query =
          new CswQueryBuilder()
              .addAttributeFilter(PROPERTY_IS_EQUAL_TO, Validation.VALIDATION_WARNINGS, "*")
              .addPropertyIsNullAttributeFilter(Validation.VALIDATION_WARNINGS)
              .addLogicalOperator(OR)
              .getQuery();

      response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();
      // Assert Metacard1 and NOT metacard2 is in results
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));

      // Search for all metacards that have validation-warnings
      query =
          new CswQueryBuilder()
              .addAttributeFilter(PROPERTY_IS_EQUAL_TO, Validation.VALIDATION_WARNINGS, "*")
              .getQuery();

      response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();
      // Assert Metacard1 and metacard2 are NOT in results
      response.body(
          not(
              hasXPath(
                  format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1))));
    } finally {
      configureEnforcedMetacardValidators(enforcedValidatorsProperties, getAdminConfig());
    }
  }

  @Test
  public void testValidationUnenforced() throws Exception {
    getServiceManager().stopBundle("catalog-security-filter");

    String id1 = ingestXmlFromResourceAndWait("/metacard1.xml");
    String id2 = ingestXmlFromResourceAndWait("/metacard2.xml");

    try {
      // metacardMarkerPlugin has no enforcedMetacardValidators
      // Search for all entries, implicit "validation-warnings is null" and "validation-errors is
      // null"
      // should get added by ValidationQueryFactory
      String query =
          new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*").getQuery();
      ValidatableResponse response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();
      // Assert Metacard1 is in results AND not Metacard2
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));
      response.body(
          not(
              hasXPath(
                  format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id2))));

      // Search for all entries that have no validation warnings
      query =
          new CswQueryBuilder()
              .addPropertyIsNullAttributeFilter(Validation.VALIDATION_WARNINGS)
              .getQuery();
      response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();
      // Assert Metacard1 is in results AND not Metacard2
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));
      response.body(
          not(
              hasXPath(
                  format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id2))));

      // Search for all entries that have validation-warnings or no validation warnings
      // Only search that will actually return all entries
      query =
          new CswQueryBuilder()
              .addAttributeFilter(
                  PROPERTY_IS_LIKE, Validation.VALIDATION_WARNINGS, "sampleWarnings")
              .addPropertyIsNullAttributeFilter(Validation.VALIDATION_WARNINGS)
              .addLogicalOperator(OR)
              .getQuery();

      response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();
      // Assert Metacard1 AND Metacard2 are in results
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id2)));

      // Search for all entries that are invalid
      query =
          new CswQueryBuilder()
              .addAttributeFilter(PROPERTY_IS_LIKE, Validation.VALIDATION_WARNINGS, "*")
              .getQuery();

      response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();
      // Assert Metacard2 is in results AND not Metacard1
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id2)));
      response.body(
          not(
              hasXPath(
                  format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1))));

      query =
          new CswQueryBuilder()
              .addPropertyIsNullAttributeFilter(Validation.VALIDATION_WARNINGS)
              .addLogicalOperator(NOT)
              .getQuery();

      response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();
      // Assert Metacard2 is in results AND not Metacard1
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id2)));
      response.body(
          not(
              hasXPath(
                  format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1))));
    } finally {
      getServiceManager().startBundle("catalog-security-filter");
    }
  }

  @Test
  public void testValidationEnforcedUpdate() throws Exception {
    // metacardMarkerPlugin has no enforced validators so both metacards can be ingested
    final String id1 = ingestXmlFromResourceAndWait("/metacard1.xml");
    final String id2 = ingestXmlFromResourceAndWait("/metacard2.xml");

    // Enforce the sample metacard validator
    Dictionary<String, Object> enforcedValidatorsProperties =
        configureEnforcedMetacardValidators(
            Collections.singletonList("sample-validator"), getAdminConfig());

    try {
      String metacard2Xml = getFileContent("metacard2.xml");
      given()
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
          .body(metacard2Xml)
          .put(new DynamicUrl(REST_PATH, id1).getUrl())
          .then()
          .assertThat()
          .statusCode(HttpStatus.SC_BAD_REQUEST);

      String metacard1Xml = getFileContent("metacard1.xml");
      given()
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
          .body(metacard1Xml)
          .put(new DynamicUrl(REST_PATH, id2).getUrl())
          .then()
          .assertThat()
          .statusCode(HttpStatus.SC_OK);

      String metacard1Path = format(METACARD_X_PATH, id1);
      String metacard2Path = format(METACARD_X_PATH, id2);

      getOpenSearch("xml", null, null, "q=*")
          .log()
          .all()
          .assertThat()
          .body(hasXPath(metacard1Path))
          .body(hasXPath(metacard1Path + "/string[@name='title']/value", is("Metacard-1")))
          .body(not(hasXPath(metacard1Path + "/string[@name='validation-errors']")))
          .body(not(hasXPath(metacard1Path + "/string[@name='validation-warnings']")))
          .body(hasXPath(metacard2Path))
          .body(hasXPath(metacard2Path + "/string[@name='title']/value", is("Metacard-1")))
          .body(not(hasXPath(metacard2Path + "/string[@name='validation-errors']")))
          .body(not(hasXPath(metacard2Path + "/string[@name='validation-warnings']")));
    } finally {
      configureEnforcedMetacardValidators(enforcedValidatorsProperties, getAdminConfig());
    }
  }

  @Test
  public void testValidationUnenforcedUpdate() throws Exception {
    Dictionary<String, Object> showInvalidProperties =
        configureShowInvalidMetacards("true", "true", getAdminConfig());

    try {
      // metacardMarkerPlugin has no enforced validators so both metacards can be ingested
      final String id1 = ingestXmlFromResourceAndWait("/metacard1.xml");
      final String id2 = ingestXmlFromResourceAndWait("/metacard2.xml");

      String metacard2Xml = getFileContent("metacard2.xml");
      given()
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
          .body(metacard2Xml)
          .put(new DynamicUrl(REST_PATH, id1).getUrl())
          .then()
          .assertThat()
          .statusCode(HttpStatus.SC_OK);

      String metacard1Xml = getFileContent("metacard1.xml");
      given()
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
          .body(metacard1Xml)
          .put(new DynamicUrl(REST_PATH, id2).getUrl())
          .then()
          .assertThat()
          .statusCode(HttpStatus.SC_OK);

      String metacard1Path = format(METACARD_X_PATH, id1);
      String metacard2Path = format(METACARD_X_PATH, id2);

      getOpenSearch("xml", null, null, "q=*")
          .log()
          .all()
          .assertThat()
          .body(hasXPath(metacard1Path))
          .body(hasXPath(metacard1Path + "/string[@name='title']/value", is("Metacard-2")))
          .body(
              hasXPath(
                  "count(" + metacard1Path + "/string[@name='validation-errors']/value)", is("1")))
          .body(
              hasXPath(
                  "count(" + metacard1Path + "/string[@name='validation-warnings']/value)",
                  is("1")))
          .body(hasXPath(metacard2Path))
          .body(hasXPath(metacard2Path + "/string[@name='title']/value", is("Metacard-1")))
          .body(not(hasXPath(metacard2Path + "/string[@name='validation-errors']")))
          .body(not(hasXPath(metacard2Path + "/string[@name='validation-warnings']")));
    } finally {
      configureShowInvalidMetacards(showInvalidProperties, getAdminConfig());
    }
  }

  @Test
  public void testValidationFiltering() throws Exception {
    // Update metacardMarkerPlugin config with no enforcedMetacardValidators
    Dictionary<String, Object> enforcedValidatorsProperties =
        configureEnforcedMetacardValidators(Arrays.asList(""), getAdminConfig());

    // Configure the PDP
    Map<String, Object> pdpProperties = new HashMap<>();
    pdpProperties.putAll(
        getServiceManager()
            .getMetatypeDefaults("security-pdp-authzrealm", "ddf.security.pdp.realm.AuthzRealm"));
    pdpProperties.put(
        "matchOneMappings",
        Arrays.asList("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=invalid-state"));

    Dictionary<String, Object> authZProps = new Hashtable<>(pdpProperties);
    Dictionary<String, Object> oldAuthZProps = configureAuthZRealm(authZProps, getAdminConfig());

    // Configure invalid filtering
    Dictionary<String, Object> validityProperties =
        configureMetacardValidityFilterPlugin(
            Arrays.asList("invalid-state=system-admin"), getAdminConfig());

    try {
      String id1 = ingestXmlFromResourceAndWait("/metacard1.xml");
      String id2 = ingestXmlFromResourceAndWait("/metacard2.xml");

      String query =
          new CswQueryBuilder()
              .addAttributeFilter(PROPERTY_IS_LIKE, Validation.VALIDATION_WARNINGS, "*")
              .addPropertyIsNullAttributeFilter(Validation.VALIDATION_WARNINGS)
              .addLogicalOperator(OR)
              .getQuery();

      ValidatableResponse response =
          given()
              .auth()
              .preemptive()
              .basic("admin", "admin")
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();
      // Assert Metacard2 is in results AND Metacard1
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id2)));

      response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();
      // Assert Metacard2 is in results Metacard1
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));
      response.body(
          not(
              hasXPath(
                  format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id2))));

      // Configure invalid filtering
      configureMetacardValidityFilterPlugin(
          Arrays.asList("invalid-state=system-admin,guest"), getAdminConfig());

      response =
          given()
              .auth()
              .preemptive()
              .basic("admin", "admin")
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();
      // Assert Metacard2 is in results AND Metacard1
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id2)));

      response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();
      // Assert Metacard2 is in results Metacard1
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id2)));

    } finally {
      configureAuthZRealm(oldAuthZProps, getAdminConfig());
      configureEnforcedMetacardValidators(enforcedValidatorsProperties, getAdminConfig());
      configureMetacardValidityFilterPlugin(validityProperties, getAdminConfig());
    }
  }

  @Test
  public void testValidationChecker() throws Exception {
    Dictionary<String, Object> enforcedValidatorsProperties =
        configureEnforcedMetacardValidators(Arrays.asList(""), getAdminConfig());

    try {
      String id1 = ingestXmlFromResourceAndWait("/metacard1.xml");
      String id2 = ingestXmlFromResourceAndWait("/metacard2.xml");

      // Search for all entries, implicit "validation-warnings is null" and "validation-errors is
      // null"
      // should get added by ValidationQueryFactory
      String query =
          new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*").getQuery();
      ValidatableResponse response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();
      // Assert Metacard1 is in results AND Metacard2 because showInvalidMetacards is true
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));
      response.body(
          (hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id2))));

      // Search for all entries that have no validation warnings or errors
      query =
          new CswQueryBuilder()
              .addPropertyIsNullAttributeFilter(Validation.VALIDATION_WARNINGS)
              .getQuery();
      response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();
      // Assert Metacard1 is in results AND not Metacard2
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));
      response.body(
          not(
              hasXPath(
                  format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id2))));

      // Search for all entries that have validation-warnings from sample-validator or no validation
      // warnings

      query =
          new CswQueryBuilder()
              .addAttributeFilter(PROPERTY_IS_EQUAL_TO, Validation.VALIDATION_WARNINGS, "*")
              .addPropertyIsNullAttributeFilter(Validation.VALIDATION_WARNINGS)
              .addLogicalOperator(OR)
              .getQuery();

      response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();
      // Assert Metacard1 and NOT metacard2 is in results
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));
      response.body(
          not(
              hasXPath(
                  format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id2))));

      // Search for all metacards that have validation-warnings
      query =
          new CswQueryBuilder()
              .addAttributeFilter(PROPERTY_IS_EQUAL_TO, Validation.VALIDATION_WARNINGS, "*")
              .getQuery();

      response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();
      // Assert Metacard1 and metacard2 are NOT in results
      response.body(
          not(
              hasXPath(
                  format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1))));
      response.body(
          not(
              hasXPath(
                  format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id2))));
    } finally {
      configureEnforcedMetacardValidators(enforcedValidatorsProperties, getAdminConfig());
    }
  }

  /**
   * This method tries to ingest the given resource until it fails. This is needed because of the
   * async nature of setting configurations that would restrict/reject an ingest request.
   */
  protected String ingestXmlFromResourceWaitForFailure(String resourceName) throws IOException {
    StringWriter writer = new StringWriter();
    IOUtils.copy(IOUtils.toInputStream(getFileContent(resourceName)), writer);
    List<String> ids = new ArrayList<>();
    with()
        .pollInterval(1, SECONDS)
        .await()
        .atMost(30, SECONDS)
        .ignoreExceptions()
        .until(
            () -> {
              try {
                ids.add(ingest(writer.toString(), "text/xml", true));
              } catch (AssertionError ae) {
                return true;
              }
              return false;
            });
    ids.stream().forEach(mcardId -> deleteMetacardAndWait(mcardId));
    return null;
  }

  /**
   * Setting configurations is performed asynchronously and there is no way to check if the
   * configured bean has received a configuration update. This method provides a best effort
   * workaround by retrying the test/assertions with a slight delay in between tries in an attempt
   * to let the configuration thread catch up. The Runnable.run() method will be called in each
   * attempt and all exceptions including AssertionErrors will be treated as a failed run and
   * retried.
   */
  private void testWithRetry(Runnable runnable) {

    with()
        .pollInterval(1, SECONDS)
        .await()
        .atMost(30, SECONDS)
        .ignoreExceptions()
        .until(
            () -> {
              runnable.run();
              return true;
            });
  }

  public class CatalogPolicyProperties extends HashMap<String, Object> {

    public static final String SYMBOLIC_NAME = "catalog-security-policyplugin";

    public static final String FACTORY_PID = "org.codice.ddf.catalog.security.CatalogPolicy";

    public CatalogPolicyProperties() {
      this.putAll(getServiceManager().getMetatypeDefaults(SYMBOLIC_NAME, FACTORY_PID));
    }
  }
}
