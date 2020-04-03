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

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.codice.ddf.itests.common.csw.CswTestCommons.CSW_FEDERATED_SOURCE_FACTORY_PID;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswSourceProperties;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import com.jayway.restassured.path.json.JsonPath;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.Source;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.MediaType;
import org.apache.http.HttpStatus;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.catalog.CatalogTestCommons;
import org.codice.ddf.test.common.LoggingUtils;
import org.codice.ddf.test.common.annotations.AfterExam;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.framework.InvalidSyntaxException;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestFanout extends AbstractIntegrationTest {

  private static final String LOCAL_SOURCE_ID = "ddf.distribution";

  // Using default resource tag as the one to blacklist
  private static final List<String> TAG_BLACKLIST = Collections.singletonList("resource");

  private static Map<String, Object> oldPolicyManagerProps;

  @BeforeExam
  public void beforeExam() throws Exception {
    try {
      getCatalogBundle().setFanout(true);
      getCatalogBundle().waitForCatalogProvider();
      LOGGER.info("Source status: \n{}", get(REST_PATH.getUrl() + "sources").body().prettyPrint());
    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @BeforeExam: ");
    }
  }

  @AfterExam
  public void afterExam() throws Exception {
    if (oldPolicyManagerProps != null) {
      getSecurityPolicy().updateWebContextPolicy(oldPolicyManagerProps);
    }
    getCatalogBundle().setFanout(false);
    getCatalogBundle().setFanoutTagBlacklist(TAG_BLACKLIST);
    getCatalogBundle().waitForCatalogProvider();
  }

  @Before
  public void setup() throws IOException, InterruptedException {
    // Start with empty blacklist
    getCatalogBundle().setFanoutTagBlacklist(Collections.emptyList());
    // Start with fanout enabled
    getCatalogBundle().setFanout(true);
    try {
      getCatalogBundle().waitForCatalogProvider();
    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @Before: ");
    }
  }

  @After
  public void tearDown() throws Exception {
    clearCatalog();
  }

  private void startCswSource() throws Exception {
    getServiceManager().waitForHttpEndpoint(CSW_PATH + "?_wadl");
    getServiceManager()
        .createManagedService(
            CSW_FEDERATED_SOURCE_FACTORY_PID,
            getCswSourceProperties(CSW_SOURCE_ID, CSW_PATH.getUrl(), getServiceManager()));

    await("Waiting for CSW source to initialize")
        .atMost(10, TimeUnit.MINUTES)
        .pollDelay(1, TimeUnit.SECONDS)
        .until(this::areSourcesReady);
  }

  private boolean areSourcesReady() throws InvalidSyntaxException {
    return getServiceManager().getServiceReferences(FederatedSource.class, null).stream()
        .map(getServiceManager()::getService)
        .filter(src -> CSW_SOURCE_ID.equals(src.getId()))
        .anyMatch(Source::isAvailable);
  }

  @Test
  public void fanoutQueryReturnsOnlyOneSource() throws Exception {
    startCswSource();
    try {
      String queryUrl = REST_PATH.getUrl() + "sources";

      String jsonBody = given().when().get(queryUrl).getBody().asString();
      JsonPath json = JsonPath.from(jsonBody);

      assertThat(json.getInt("size()"), equalTo(1));
      assertThat(json.getList("id", String.class), hasItem(LOCAL_SOURCE_ID));
    } finally {
      getServiceManager().stopManagedService(CSW_FEDERATED_SOURCE_FACTORY_PID);
    }
  }

  @Test
  public void nonFanoutQueryReturnsMultipleSources() throws Exception {
    startCswSource();
    try {
      getCatalogBundle().setFanout(false);
      getCatalogBundle().waitForCatalogProvider();

      String queryUrl = REST_PATH.getUrl() + "sources";

      String jsonBody = given().when().get(queryUrl).getBody().asString();
      JsonPath json = JsonPath.from(jsonBody);

      assertThat(json.getInt("size()"), equalTo(2));
      assertThat(json.getList("id", String.class), hasItem(LOCAL_SOURCE_ID));
      assertThat(json.getList("id", String.class), hasItem(CSW_SOURCE_ID));
    } finally {
      getServiceManager().stopManagedService(CSW_FEDERATED_SOURCE_FACTORY_PID);
    }
  }

  @Test
  public void testCswIngestWithFanoutEnabledAndEmptyBlacklist() throws Exception {
    String id =
        CatalogTestCommons.ingest(
            "Some data to ingest", MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
    CatalogTestCommons.delete(id);
  }

  @Test
  public void testCswIngestFailsWithFanoutEnabledAndBlacklistSet() throws Exception {
    getCatalogBundle().setFanoutTagBlacklist(TAG_BLACKLIST);
    CatalogTestCommons.ingest(
        "Some data to ingest. This should fail.", MediaType.TEXT_PLAIN, HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void testCswUpdateWorksWithFanoutEnabledAndEmptyBlacklist()
      throws IOException, InterruptedException {
    getCatalogBundle().setFanoutTagBlacklist(Collections.emptyList());
    String id =
        CatalogTestCommons.ingest(
            "Some data to ingest", MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
    CatalogTestCommons.update(id, "Some data to update", MediaType.TEXT_PLAIN, HttpStatus.SC_OK);
    CatalogTestCommons.delete(id);
  }

  @Test
  public void testCswUpdateFailsWithFanoutEnabledAndBlacklistSet()
      throws IOException, InterruptedException {
    getCatalogBundle().setFanoutTagBlacklist(Collections.emptyList());
    String id =
        CatalogTestCommons.ingest(
            "Some data to ingest", MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

    // Set blacklist so update will fail
    getCatalogBundle().setFanoutTagBlacklist(TAG_BLACKLIST);
    CatalogTestCommons.update(
        id, "Some data to update", MediaType.TEXT_PLAIN, HttpStatus.SC_BAD_REQUEST);

    getServiceManager().waitForAllBundles();
    // Set blacklist to empty list so the delete will succeed
    getCatalogBundle().setFanoutTagBlacklist(new ArrayList<>());
    CatalogTestCommons.delete(id);
  }

  @Test
  public void testCswDeleteFailsWithFanoutEnabledAndBlacklistSet()
      throws IOException, InterruptedException {
    // The case where delete works with fanout on and empty blacklist is tested as clean up in the
    // other tests.
    getCatalogBundle().setFanoutTagBlacklist(Collections.emptyList());
    String id =
        CatalogTestCommons.ingest(
            "Some data to ingest", MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

    // Set blacklist so update will fail
    getCatalogBundle().setFanoutTagBlacklist(TAG_BLACKLIST);
    CatalogTestCommons.delete(id, HttpStatus.SC_BAD_REQUEST);

    // Set blacklist to empty list so the delete will succeed
    getCatalogBundle().setFanoutTagBlacklist(new ArrayList<>());
    CatalogTestCommons.delete(id);
  }
}
