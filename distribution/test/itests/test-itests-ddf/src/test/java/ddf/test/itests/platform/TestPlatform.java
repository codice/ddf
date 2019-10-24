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
package ddf.test.itests.platform;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import java.util.List;
import java.util.Map;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.platform.logging.LogEvent;
import org.codice.ddf.test.common.LoggingUtils;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestPlatform extends AbstractIntegrationTest {

  private static final DynamicUrl LOGGING_SERVICE_JOLOKIA_URL =
      new DynamicUrl(
          DynamicUrl.SECURE_ROOT,
          HTTPS_PORT,
          "/admin/jolokia/exec/org.codice.ddf.platform.logging.LoggingService:service=logging-service/retrieveLogEvents");

  private static final DynamicUrl REPORT_GENERATION_URL =
      new DynamicUrl(DynamicUrl.SECURE_ROOT, HTTPS_PORT, "/services/internal/metrics/report.xls");

  public static final String ADMIN = "admin";

  @BeforeExam
  public void beforeTest() throws Exception {
    try {
      // Must explicitly add basic auth to log in with a username and password
      getSecurityPolicy().configureRestForBasic();
    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @BeforeExam: ");
    }
  }

  @Test
  public void testLoggingServiceEndpoint() throws Exception {
    try {
      getSecurityPolicy().configureRestForBasic();

      Response response =
          given()
              .auth()
              .preemptive()
              .basic(ADMIN, ADMIN)
              .header("X-Requested-With", "XMLHttpRequest")
              .header("Origin", LOGGING_SERVICE_JOLOKIA_URL.getUrl())
              .expect()
              .statusCode(200)
              .when()
              .get(LOGGING_SERVICE_JOLOKIA_URL.getUrl());

      String bodyString = checkResponseBody(response, LOGGING_SERVICE_JOLOKIA_URL);

      final List events = JsonPath.given(bodyString).get("value");
      final Map firstEvent = (Map) events.get(0);
      final String levelOfFirstEvent = firstEvent.get("level").toString();
      final String unknownLevel = LogEvent.Level.UNKNOWN.getLevel();
      assertThat(
          String.format(
              "The level of an event returned by %s should not be %s",
              LOGGING_SERVICE_JOLOKIA_URL, unknownLevel),
          levelOfFirstEvent,
          not(equalTo(unknownLevel)));

    } finally {
      getSecurityPolicy().configureRestForGuest();
    }
  }

  @Test
  public void testPlatformMetricsReportGeneration() throws Exception {
    try {
      getSecurityPolicy().configureRestForBasic();

      Response response =
          given()
              .auth()
              .preemptive()
              .basic(ADMIN, ADMIN)
              .header("X-Requested-With", "XMLHttpRequest")
              .header("Origin", REPORT_GENERATION_URL.getUrl())
              .expect()
              .statusCode(200)
              .when()
              .get(REPORT_GENERATION_URL.getUrl());

      checkResponseBody(response, REPORT_GENERATION_URL);

    } finally {
      getSecurityPolicy().configureRestForGuest();
    }
  }

  private String checkResponseBody(Response response, DynamicUrl url) {
    final String bodyString = response.getBody().asString();
    assertThat(
        String.format("The response body from %s should not be empty", url),
        bodyString,
        not(isEmptyString()));
    return bodyString;
  }
}
