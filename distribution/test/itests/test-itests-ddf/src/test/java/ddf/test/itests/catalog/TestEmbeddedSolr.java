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
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswInsertRequest;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.xml.HasXPath.hasXPath;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;
import java.io.IOException;
import java.util.UUID;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.xpath.XPathExpressionException;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.catalog.CatalogTestCommons;
import org.codice.ddf.itests.common.config.UrlResourceReaderConfigurator;
import org.codice.ddf.test.common.LoggingUtils;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

/** Tests the Catalog framework component when backed by the Embedded Solr Provider. */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestEmbeddedSolr extends AbstractIntegrationTest {

  private static final String DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS = "data/products";

  @Rule public TestName testName = new TestName();

  private UrlResourceReaderConfigurator urlResourceReaderConfigurator;

  @BeforeExam
  public void beforeExam() throws Exception {
    try {
      basePort = getBasePort();
      getServiceManager().startFeature(true, getDefaultRequiredApps());
      getServiceManager().waitForAllBundles();
      getCatalogBundle().waitForCatalogProvider();
      getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query");

      configureRestForGuest();
      getSecurityPolicy().waitForGuestAuthReady(REST_PATH.getUrl() + "?_wadl");

    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @BeforeExam: ");
    }
  }

  @Before
  public void setup() {
    urlResourceReaderConfigurator = getUrlResourceReaderConfigurator();
  }

  @After
  public void tearDown() throws IOException {
    urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
        DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS);
    clearCatalog();
  }

  @Test
  public void testCswIngest() {
    Response response = ingestCswRecord();
    ValidatableResponse validatableResponse = response.then();

    validatableResponse.body(
        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("1")),
        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
        hasXPath(
            "//TransactionResponse/InsertResult/BriefRecord/title",
            is("Aliquam fermentum purus quis arcu")),
        hasXPath("//TransactionResponse/InsertResult/BriefRecord/BoundingBox"));

    try {
      CatalogTestCommons.deleteMetacardUsingCswResponseId(response);
    } catch (IOException | XPathExpressionException e) {
      fail("Could not retrieve the ingested record's ID from the response.");
    }
  }

  @Override
  protected Option[] configureCustom() {
    return options(
        editConfigurationFilePut(
            "etc/custom.system.properties", "solr.client", "EmbeddedSolrServer"),
        editConfigurationFilePut("etc/custom.system.properties", "solr.http.url", ""),
        editConfigurationFilePut(
            "etc/custom.system.properties", "solr.data.dir", "${karaf.home}/data/solr"),
        editConfigurationFilePut("etc/custom.system.properties", "solr.cloud.zookeeper", ""));
  }

  private Response ingestCswRecord() {

    String uuid = UUID.randomUUID().toString().replaceAll("-", "");

    return given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
        .body(
            getCswInsertRequest(
                "csw:Record",
                getFileContent(
                    CSW_RECORD_RESOURCE_PATH + "/CswRecord", ImmutableMap.of("id", uuid))))
        .post(CSW_PATH.getUrl());
  }
}
