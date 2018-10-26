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
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingest;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingestXmlFromResourceAndWait;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureEnforceValidityErrorsAndWarnings;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureEnforcedMetacardValidators;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureFilterInvalidMetacards;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureMetacardValidityFilterPlugin;
import static org.codice.ddf.itests.common.csw.CswQueryBuilder.NOT;
import static org.codice.ddf.itests.common.csw.CswQueryBuilder.OR;
import static org.codice.ddf.itests.common.csw.CswQueryBuilder.PROPERTY_IS_EQUAL_TO;
import static org.codice.ddf.itests.common.csw.CswQueryBuilder.PROPERTY_IS_LIKE;
import static org.codice.ddf.itests.common.opensearch.OpenSearchTestCommons.getOpenSearch;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.xml.HasXPath.hasXPath;

import com.jayway.restassured.response.ValidatableResponse;
import ddf.catalog.data.types.Validation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.http.HttpStatus;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.catalog.CatalogTestCommons;
import org.codice.ddf.itests.common.csw.CswQueryBuilder;
import org.codice.ddf.test.common.LoggingUtils;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

/**
 * Tests catalog validation. This test was created to pull out 16 tests in TestCatalog that were
 * starting/stopping the sample-validator each time. Since there is almost no overhead now for a new
 * class it is faster to just start the feature once for all 16 of the tests instead of toggling it
 * for each one.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestCatalogValidation extends AbstractIntegrationTest {

  private static final String METACARD_X_PATH = "/metacards/metacard[@id='%s']";

  private static final String SAMPLE_VALIDATOR = "sample-validator";

  private static final String CLEAN_METACARD =
      XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml";

  private static final String WARNING_METACARD =
      XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml";

  private static final String ERROR_METACARD =
      XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml";

  private static final String CSW_RECORD_XPATH =
      "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]";

  @BeforeExam
  public void beforeExam() {
    try {
      waitForSystemReady();
      getServiceManager().startFeature(true, SAMPLE_VALIDATOR);
    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @BeforeExam: ");
    }
  }

  @Before
  public void setup() throws Exception {
    configureEnforceValidityErrorsAndWarnings("false", "false", getAdminConfig());
    configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());
    configureFilterInvalidMetacards("false", "false", getAdminConfig());
    configureMetacardValidityFilterPlugin(
        Collections.singletonList("invalid-state=guest"), getAdminConfig());
    clearCatalogAndWait();
  }

  @Test
  public void testEnforceValidityErrorsOnly() throws Exception {
    // Configure to enforce validator
    configureEnforcedMetacardValidators(
        Collections.singletonList(SAMPLE_VALIDATOR), getAdminConfig());

    // Configure to enforce errors but not warnings
    configureEnforceValidityErrorsAndWarnings("true", "false", getAdminConfig());

    ingestXmlFromResourceAndWait(WARNING_METACARD);
    ingestXmlFromResourceAndWait(CLEAN_METACARD);
    ingestXmlFromResourceWaitForFailure(ERROR_METACARD);

    configureFilterInvalidMetacards("true", "false", getAdminConfig());

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
  }

  @Test
  public void testEnforceValidityWarningsOnly() throws Exception {
    // Configure to enforce validator
    configureEnforcedMetacardValidators(
        Collections.singletonList(SAMPLE_VALIDATOR), getAdminConfig());

    // Configure to enforce warnings but not errors
    configureEnforceValidityErrorsAndWarnings("false", "true", getAdminConfig());

    ingestXmlFromResourceWaitForFailure(WARNING_METACARD);
    ingestXmlFromResourceAndWait(CLEAN_METACARD);
    ingestXmlFromResourceAndWait(ERROR_METACARD);

    configureFilterInvalidMetacards("true", "false", getAdminConfig());

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
  }

  @Test
  public void testEnforceValidityErrorsAndWarnings() throws Exception {
    // Configure to enforce validator
    configureEnforcedMetacardValidators(
        Collections.singletonList(SAMPLE_VALIDATOR), getAdminConfig());

    // Configure to enforce errors and warnings
    configureEnforceValidityErrorsAndWarnings("true", "true", getAdminConfig());

    ingestXmlFromResourceWaitForFailure(WARNING_METACARD);
    ingestXmlFromResourceAndWait(CLEAN_METACARD);
    ingestXmlFromResourceWaitForFailure(ERROR_METACARD);

    configureFilterInvalidMetacards("true", "false", getAdminConfig());

    testWithRetry(
        () -> {
          String query =
              new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*").getQuery();
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
  }

  @Test
  public void testNoEnforceValidityErrorsOrWarnings() throws Exception {
    // Configure to enforce validator
    configureEnforcedMetacardValidators(
        Collections.singletonList(SAMPLE_VALIDATOR), getAdminConfig());

    // Configure to enforce neither errors nor warnings
    configureEnforceValidityErrorsAndWarnings("false", "false", getAdminConfig());

    ingestXmlFromResourceAndWait(WARNING_METACARD);
    ingestXmlFromResourceAndWait(CLEAN_METACARD);
    ingestXmlFromResourceAndWait(ERROR_METACARD);

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
  }

  @Test
  public void testQueryByErrorFailedValidators() {
    ingestXmlFromResourceAndWait(WARNING_METACARD);
    ingestXmlFromResourceAndWait(CLEAN_METACARD);
    ingestXmlFromResourceAndWait(ERROR_METACARD);

    String query =
        new CswQueryBuilder()
            .addAttributeFilter(
                PROPERTY_IS_LIKE, Validation.FAILED_VALIDATORS_ERRORS, SAMPLE_VALIDATOR)
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
  }

  @Test
  public void testQueryByWarningFailedValidators() {
    ingestXmlFromResourceAndWait(WARNING_METACARD);
    ingestXmlFromResourceAndWait(CLEAN_METACARD);
    ingestXmlFromResourceAndWait(ERROR_METACARD);

    String query =
        new CswQueryBuilder()
            .addAttributeFilter(
                PROPERTY_IS_LIKE, Validation.FAILED_VALIDATORS_WARNINGS, SAMPLE_VALIDATOR)
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
  }

  @Test
  public void testFilterPluginWarningsOnly() throws Exception {
    ingestXmlFromResourceAndWait(WARNING_METACARD);
    ingestXmlFromResourceAndWait(CLEAN_METACARD);
    ingestXmlFromResourceAndWait(ERROR_METACARD);

    // Configure invalid filtering
    configureMetacardValidityFilterPlugin(
        Collections.singletonList("invalid-state=not-any-users-roles"), getAdminConfig());

    // Configure to filter metacards with validation warnings but not validation errors
    configureFilterInvalidMetacards("false", "true", getAdminConfig());

    testWithRetry(
        () -> {
          String query =
              new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*").getQuery();
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
  }

  @Test
  public void testFilterPluginErrorsOnly() throws Exception {
    ingestXmlFromResourceAndWait(ERROR_METACARD);
    ingestXmlFromResourceAndWait(CLEAN_METACARD);
    ingestXmlFromResourceAndWait(WARNING_METACARD);

    configureMetacardValidityFilterPlugin(
        Collections.singletonList("invalid-state=not-any-users-roles"), getAdminConfig());
    configureFilterInvalidMetacards("true", "false", getAdminConfig());

    testWithRetry(
        () -> {
          String query =
              new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*").getQuery();
          ValidatableResponse response =
              given()
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                  .body(query)
                  .post(CSW_PATH.getUrl())
                  .then();

          response.body(not(containsString("error metacard")));
          response.body(containsString("clean metacard"));
          response.body(containsString("warning metacard"));
        });
  }

  @Test
  public void testFilterPluginWarningsAndErrors() throws Exception {
    ingestXmlFromResourceAndWait(ERROR_METACARD);
    ingestXmlFromResourceAndWait(CLEAN_METACARD);
    ingestXmlFromResourceAndWait(WARNING_METACARD);

    // Configure invalid filtering
    configureMetacardValidityFilterPlugin(
        Collections.singletonList("invalid-state=not-any-users-roles"), getAdminConfig());

    // configure to filter both metacards with validation errors and validation warnings
    configureFilterInvalidMetacards("true", "true", getAdminConfig());

    testWithRetry(
        () -> {
          String query =
              new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*").getQuery();
          ValidatableResponse response =
              given()
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                  .body(query)
                  .post(CSW_PATH.getUrl())
                  .then();

          response.body(not(containsString("error metacard")));
          response.body(not(containsString("warning metacard")));
          response.body(containsString("clean metacard"));
        });
  }

  @Test
  public void testFilterPluginNoFiltering() throws Exception {
    ingestXmlFromResourceAndWait(ERROR_METACARD);
    ingestXmlFromResourceAndWait(CLEAN_METACARD);
    ingestXmlFromResourceAndWait(WARNING_METACARD);

    configureFilterInvalidMetacards("false", "false", getAdminConfig());

    // Configure invalid filtering
    configureMetacardValidityFilterPlugin(
        Collections.singletonList("invalid-state=not-any-users-roles"), getAdminConfig());

    testWithRetry(
        () -> {
          String query =
              new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*").getQuery();
          ValidatableResponse response =
              given()
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                  .body(query)
                  .post(CSW_PATH.getUrl())
                  .then();

          response.body(containsString("error metacard"));
          response.body(containsString("warning metacard"));
          response.body(containsString("clean metacard"));
        });
  }

  @Test
  public void testValidationEnforcedUpdate() throws Exception {
    String warningId = ingestXmlFromResourceAndWait(WARNING_METACARD);
    String errorId = ingestXmlFromResourceAndWait(ERROR_METACARD);

    testWithRetry(
        () -> {
          String query =
              new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*").getQuery();
          ValidatableResponse response =
              given()
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                  .body(query)
                  .post(CSW_PATH.getUrl())
                  .then();

          response.body(containsString("error metacard"));
          response.body(containsString("warning metacard"));
        });

    configureEnforceValidityErrorsAndWarnings("true", "true", getAdminConfig());
    configureFilterInvalidMetacards("true", "true", getAdminConfig());

    configureEnforcedMetacardValidators(
        Collections.singletonList(SAMPLE_VALIDATOR), getAdminConfig());

    String errorMetacardXml = getFileContent(ERROR_METACARD);
    given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
        .body(errorMetacardXml)
        .put(new DynamicUrl(REST_PATH, errorId).getUrl())
        .then()
        .assertThat()
        .statusCode(HttpStatus.SC_BAD_REQUEST);

    String warningMetacardXml = getFileContent(WARNING_METACARD);
    given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
        .body(warningMetacardXml)
        .put(new DynamicUrl(REST_PATH, warningId).getUrl())
        .then()
        .assertThat()
        .statusCode(HttpStatus.SC_BAD_REQUEST);

    String warningIdXpath = format(METACARD_X_PATH, warningId);
    String errorIdXpath = format(METACARD_X_PATH, errorId);

    getOpenSearch("xml", null, null, "q=*")
        .log()
        .all()
        .assertThat()
        .body(hasXPath(warningIdXpath))
        .body(hasXPath(warningIdXpath + "/string[@name='title']/value", is("warning metacard")))
        .body(hasXPath(warningIdXpath + "/string[@name='validation-warnings']"))
        .body(not(hasXPath(warningIdXpath + "/string[@name='validation-errors']")))
        .body(hasXPath(errorIdXpath))
        .body(hasXPath(errorIdXpath + "/string[@name='title']/value", is("error metacard")))
        .body(not(hasXPath(errorIdXpath + "/string[@name='validation-warnings']")))
        .body(hasXPath(errorIdXpath + "/string[@name='validation-errors']"));
  }

  @Test
  public void testQueryingByValidationWarnings() throws Exception {
    String warningId = ingestXmlFromResourceAndWait(WARNING_METACARD);
    String errorId = ingestXmlFromResourceAndWait(ERROR_METACARD);

    String query =
        new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*").getQuery();
    ValidatableResponse response =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(query)
            .post(CSW_PATH.getUrl())
            .then();

    response.body(hasXPath(format(CSW_RECORD_XPATH, warningId)));
    response.body(hasXPath(format(CSW_RECORD_XPATH, errorId)));

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

    response.body(not(hasXPath(format(CSW_RECORD_XPATH, warningId))));
    response.body(hasXPath(format(CSW_RECORD_XPATH, errorId)));

    query =
        new CswQueryBuilder()
            .addAttributeFilter(PROPERTY_IS_EQUAL_TO, Validation.VALIDATION_WARNINGS, "*")
            .addLogicalOperator(OR)
            .addPropertyIsNullAttributeFilter(Validation.VALIDATION_WARNINGS)
            .getQuery();

    response =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(query)
            .post(CSW_PATH.getUrl())
            .then();

    response.body(not(hasXPath(format(CSW_RECORD_XPATH, warningId))));
    response.body(hasXPath(format(CSW_RECORD_XPATH, errorId)));

    query =
        new CswQueryBuilder()
            .addLogicalOperator(NOT)
            .addPropertyIsNullAttributeFilter(Validation.VALIDATION_WARNINGS)
            .getQuery();

    response =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(query)
            .post(CSW_PATH.getUrl())
            .then();

    response.body(not(hasXPath(format(CSW_RECORD_XPATH, warningId))));
    response.body(hasXPath(format(CSW_RECORD_XPATH, errorId)));
  }

  @Test
  public void testQueryingByValidationErrors() throws Exception {
    String warningId = ingestXmlFromResourceAndWait(WARNING_METACARD);
    String errorId = ingestXmlFromResourceAndWait(ERROR_METACARD);

    String query =
        new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*").getQuery();
    ValidatableResponse response =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(query)
            .post(CSW_PATH.getUrl())
            .then();

    response.body(hasXPath(format(CSW_RECORD_XPATH, warningId)));
    response.body(hasXPath(format(CSW_RECORD_XPATH, errorId)));

    query =
        new CswQueryBuilder()
            .addPropertyIsNullAttributeFilter(Validation.VALIDATION_ERRORS)
            .getQuery();
    response =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(query)
            .post(CSW_PATH.getUrl())
            .then();

    response.body(not(hasXPath(format(CSW_RECORD_XPATH, errorId))));
    response.body(hasXPath(format(CSW_RECORD_XPATH, warningId)));

    query =
        new CswQueryBuilder()
            .addAttributeFilter(PROPERTY_IS_EQUAL_TO, Validation.VALIDATION_ERRORS, "*")
            .addLogicalOperator(OR)
            .addPropertyIsNullAttributeFilter(Validation.VALIDATION_ERRORS)
            .getQuery();

    response =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(query)
            .post(CSW_PATH.getUrl())
            .then();

    response.body(not(hasXPath(format(CSW_RECORD_XPATH, errorId))));
    response.body(hasXPath(format(CSW_RECORD_XPATH, warningId)));

    query =
        new CswQueryBuilder()
            .addLogicalOperator(NOT)
            .addPropertyIsNullAttributeFilter(Validation.VALIDATION_ERRORS)
            .getQuery();

    response =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(query)
            .post(CSW_PATH.getUrl())
            .then();

    response.body(not(hasXPath(format(CSW_RECORD_XPATH, errorId))));
    response.body(hasXPath(format(CSW_RECORD_XPATH, warningId)));
  }

  /**
   * This method tries to ingest the given resource until it fails. This is needed because of the
   * async nature of setting configurations that would restrict/reject an ingest request.
   */
  private void ingestXmlFromResourceWaitForFailure(String resourcePath) {
    String content = getFileContent(resourcePath);
    List<String> ids = new ArrayList<>();
    with()
        .pollInterval(1, SECONDS)
        .await()
        .atMost(30, SECONDS)
        .ignoreExceptions()
        .until(
            () -> {
              try {
                ids.add(ingest(content, "text/xml", true));
              } catch (AssertionError ae) {
                return true;
              }
              return false;
            });
    ids.forEach(CatalogTestCommons::deleteMetacardAndWait);
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
}
