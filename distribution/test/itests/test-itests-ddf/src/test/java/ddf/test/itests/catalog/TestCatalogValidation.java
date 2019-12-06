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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.TRANSFORMER_XML;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingest;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingestXmlFromResourceAndWait;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.query;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.update;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureMetacardValidityFilterPlugin;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureValidationMarkerPlugin;
import static org.codice.ddf.itests.common.csw.CswQueryBuilder.PROPERTY_IS_LIKE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import com.jayway.restassured.response.ValidatableResponse;
import ddf.catalog.data.types.Validation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.http.HttpStatus;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.catalog.CatalogTestCommons;
import org.codice.ddf.itests.common.csw.CswQueryBuilder;
import org.codice.ddf.test.common.LoggingUtils;
import org.codice.ddf.test.common.annotations.AfterExam;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.junit.After;
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

  private static final String SAMPLE_VALIDATOR = "sample-validator";

  private static final String CLEAN_METACARD =
      XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml";

  private static final String WARNING_METACARD =
      XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml";

  private static final String ERROR_METACARD =
      XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml";

  private Dictionary<String, Object> oldValidationMarkerPluginProps;

  private Dictionary<String, Object> oldValidityFilterPluginProps;

  @BeforeExam
  public void beforeExam() {
    try {
      getServiceManager()
          .installBundle(
              "mvn:org.codice.ddf.sdk.validation.metacard/sdk-sample-metacard-validator/2.19.0");
      getServiceManager().startBundle("sdk-sample-metacard-validator");

      // start test with validation errors/warnings allowed in catalog/search results
      oldValidationMarkerPluginProps =
          configureValidationMarkerPlugin(
              Collections.singletonList(""), false, false, getAdminConfig());
      oldValidityFilterPluginProps =
          configureMetacardValidityFilterPlugin(
              Collections.singletonList("invalid-state=guest"), false, false, getAdminConfig());
      clearCatalogAndWait();
    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @BeforeExam: ");
    }
  }

  @After
  public void tearDown() {
    clearCatalogAndWait();
  }

  @AfterExam
  public void afterExam() {
    try {
      if (oldValidationMarkerPluginProps != null) {
        configureValidationMarkerPlugin(oldValidationMarkerPluginProps, getAdminConfig());
      }
      if (oldValidityFilterPluginProps != null) {
        configureMetacardValidityFilterPlugin(oldValidityFilterPluginProps, getAdminConfig());
      }
      getServiceManager().uninstallBundle("sdk-sample-metacard-validator");
    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @AfterExam: ");
    }
  }

  /* ***************** TEST ENFORCE VALIDATION ON INGEST ***************** */
  @Test
  public void testEnforceValidityErrorsOnly() throws Exception {
    // Configure marker plugin to enforce validator errors but not warnings
    Dictionary<String, Object> markerPluginProps =
        configureValidationMarkerPlugin(
            Collections.singletonList(SAMPLE_VALIDATOR), true, false, getAdminConfig());

    try {
      String warningId = ingestXmlFromResourceAndWait(WARNING_METACARD);
      String cleanId = ingestXmlFromResourceAndWait(CLEAN_METACARD);
      ingestXmlFromResourceWaitForFailure(ERROR_METACARD);

      // verify the clean and warning metacard can be queried
      query(cleanId, TRANSFORMER_XML, HttpStatus.SC_OK);
      query(warningId, TRANSFORMER_XML, HttpStatus.SC_OK);

      // Test updating
      String warningData = getFileContent(WARNING_METACARD);
      String errorData = getFileContent(ERROR_METACARD);
      update(cleanId, warningData, MediaType.APPLICATION_XML, HttpStatus.SC_OK);
      update(cleanId, errorData, MediaType.APPLICATION_XML, HttpStatus.SC_BAD_REQUEST);
    } finally {
      // Reset marker plugin
      configureValidationMarkerPlugin(markerPluginProps, getAdminConfig());
    }
  }

  @Test
  public void testEnforceValidityWarningsOnly() throws Exception {
    // Configure marker plugin to enforce warnings but not errors
    Dictionary<String, Object> markerPluginProps =
        configureValidationMarkerPlugin(
            Collections.singletonList(SAMPLE_VALIDATOR), false, true, getAdminConfig());

    try {
      ingestXmlFromResourceWaitForFailure(WARNING_METACARD);
      String cleanId = ingestXmlFromResourceAndWait(CLEAN_METACARD);
      String errorId = ingestXmlFromResourceAndWait(ERROR_METACARD);

      // verify the clean and error metacard can be queried
      query(cleanId, TRANSFORMER_XML, HttpStatus.SC_OK);
      query(errorId, TRANSFORMER_XML, HttpStatus.SC_OK);

      // Test updating
      String warningData = getFileContent(WARNING_METACARD);
      String errorData = getFileContent(ERROR_METACARD);
      update(cleanId, warningData, MediaType.APPLICATION_XML, HttpStatus.SC_BAD_REQUEST);
      update(cleanId, errorData, MediaType.APPLICATION_XML, HttpStatus.SC_OK);
    } finally {
      // Reset marker plugin
      configureValidationMarkerPlugin(markerPluginProps, getAdminConfig());
    }
  }

  @Test
  public void testEnforceValidityErrorsAndWarnings() throws Exception {
    // Configure marker plugin to enforce errors and warnings
    Dictionary<String, Object> markerPluginProps =
        configureValidationMarkerPlugin(
            Collections.singletonList(SAMPLE_VALIDATOR), true, true, getAdminConfig());

    try {
      ingestXmlFromResourceWaitForFailure(WARNING_METACARD);
      String cleanId = ingestXmlFromResourceAndWait(CLEAN_METACARD);
      ingestXmlFromResourceWaitForFailure(ERROR_METACARD);

      // verify the clean metacard can be queried
      query(cleanId, TRANSFORMER_XML, HttpStatus.SC_OK);

      // Test updating with invalid data
      String warningData = getFileContent(WARNING_METACARD);
      String errorData = getFileContent(ERROR_METACARD);
      update(cleanId, warningData, MediaType.APPLICATION_XML, HttpStatus.SC_BAD_REQUEST);
      update(cleanId, errorData, MediaType.APPLICATION_XML, HttpStatus.SC_BAD_REQUEST);
    } finally {
      // Reset marker plugin
      configureValidationMarkerPlugin(markerPluginProps, getAdminConfig());
    }
  }

  @Test
  public void testNoEnforceValidityErrorsOrWarnings() throws Exception {
    // Configure marker plugin to enforce neither errors nor warnings
    Dictionary<String, Object> markerPluginProps =
        configureValidationMarkerPlugin(
            Collections.singletonList(SAMPLE_VALIDATOR), false, false, getAdminConfig());

    try {
      String warningId = ingestXmlFromResourceAndWait(WARNING_METACARD);
      String cleanId = ingestXmlFromResourceAndWait(CLEAN_METACARD);
      String errorId = ingestXmlFromResourceAndWait(ERROR_METACARD);

      // verify the clean and warning metacard can be queried
      query(cleanId, TRANSFORMER_XML, HttpStatus.SC_OK);
      query(warningId, TRANSFORMER_XML, HttpStatus.SC_OK);
      query(errorId, TRANSFORMER_XML, HttpStatus.SC_OK);

      // Test updating
      String warningData = getFileContent(WARNING_METACARD);
      String errorData = getFileContent(ERROR_METACARD);
      update(cleanId, warningData, MediaType.APPLICATION_XML, HttpStatus.SC_OK);
      update(cleanId, errorData, MediaType.APPLICATION_XML, HttpStatus.SC_OK);
    } finally {
      // Reset marker plugin
      configureValidationMarkerPlugin(markerPluginProps, getAdminConfig());
    }
  }

  /* ***************** TEST VALIDATION FILTERING ON QUERY ***************** */
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

  /* ***************** TEST POST-QUERY VALIDITY FILTERING ***************** */
  @Test
  public void testFilterPluginWarningsOnly() throws Exception {
    String warningId = ingestXmlFromResourceAndWait(WARNING_METACARD);
    String cleanId = ingestXmlFromResourceAndWait(CLEAN_METACARD);
    String errorId = ingestXmlFromResourceAndWait(ERROR_METACARD);

    // Configure to filter metacards with validation warnings but not validation errors
    Dictionary<String, Object> filterPluginProps =
        configureMetacardValidityFilterPlugin(
            Collections.singletonList("invalid-state=data-manager"), false, true, getAdminConfig());

    try {
      query(warningId, TRANSFORMER_XML, HttpStatus.SC_NOT_FOUND);
      query(cleanId, TRANSFORMER_XML, HttpStatus.SC_OK);
      query(errorId, TRANSFORMER_XML, HttpStatus.SC_OK);
    } finally {
      // Reset plugin
      configureMetacardValidityFilterPlugin(filterPluginProps, getAdminConfig());
    }
  }

  @Test
  public void testFilterPluginErrorsOnly() throws Exception {
    String warningId = ingestXmlFromResourceAndWait(WARNING_METACARD);
    String cleanId = ingestXmlFromResourceAndWait(CLEAN_METACARD);
    String errorId = ingestXmlFromResourceAndWait(ERROR_METACARD);

    // Configure to filter metacards with validation errors but not validation warnings
    Dictionary<String, Object> filterPluginProps =
        configureMetacardValidityFilterPlugin(
            Collections.singletonList("invalid-state=data-manager"), true, false, getAdminConfig());

    try {
      query(warningId, TRANSFORMER_XML, HttpStatus.SC_OK);
      query(cleanId, TRANSFORMER_XML, HttpStatus.SC_OK);
      query(errorId, TRANSFORMER_XML, HttpStatus.SC_NOT_FOUND);
    } finally {
      // Reset plugin
      configureMetacardValidityFilterPlugin(filterPluginProps, getAdminConfig());
    }
  }

  @Test
  public void testFilterPluginWarningsAndErrors() throws Exception {
    String warningId = ingestXmlFromResourceAndWait(WARNING_METACARD);
    String cleanId = ingestXmlFromResourceAndWait(CLEAN_METACARD);
    String errorId = ingestXmlFromResourceAndWait(ERROR_METACARD);

    // Configure to filter metacards with validation errors and validation warnings
    Dictionary<String, Object> filterPluginProps =
        configureMetacardValidityFilterPlugin(
            Collections.singletonList("invalid-state=data-manager"), true, true, getAdminConfig());

    try {
      query(warningId, TRANSFORMER_XML, HttpStatus.SC_NOT_FOUND);
      query(cleanId, TRANSFORMER_XML, HttpStatus.SC_OK);
      query(errorId, TRANSFORMER_XML, HttpStatus.SC_NOT_FOUND);
    } finally {
      // Reset plugin
      configureMetacardValidityFilterPlugin(filterPluginProps, getAdminConfig());
    }
  }

  @Test
  public void testFilterPluginNoFiltering() throws Exception {
    String warningId = ingestXmlFromResourceAndWait(WARNING_METACARD);
    String cleanId = ingestXmlFromResourceAndWait(CLEAN_METACARD);
    String errorId = ingestXmlFromResourceAndWait(ERROR_METACARD);

    // Configure to not filter metacards with validation errors nor validation warnings
    Dictionary<String, Object> filterPluginProps =
        configureMetacardValidityFilterPlugin(
            Collections.singletonList("invalid-state=data-manager"),
            false,
            false,
            getAdminConfig());

    try {
      query(warningId, TRANSFORMER_XML, HttpStatus.SC_OK);
      query(cleanId, TRANSFORMER_XML, HttpStatus.SC_OK);
      query(errorId, TRANSFORMER_XML, HttpStatus.SC_OK);
    } finally {
      // Reset plugin
      configureMetacardValidityFilterPlugin(filterPluginProps, getAdminConfig());
    }
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
}
