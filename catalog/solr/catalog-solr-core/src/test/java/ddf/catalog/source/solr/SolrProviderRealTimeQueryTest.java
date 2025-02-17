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
package ddf.catalog.source.solr;

import static ddf.catalog.data.impl.MetacardImpl.BASIC_METACARD;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.create;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.deleteAll;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.getFilterBuilder;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.queryAndVerifyCount;
import static junit.framework.TestCase.assertTrue;
import static org.awaitility.Awaitility.await;

import com.google.common.collect.Lists;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.solr.provider.Library;
import ddf.catalog.source.solr.provider.MockMetacard;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.codice.solr.factory.impl.SolrCloudClientFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrProviderRealTimeQueryTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrProviderRealTimeQueryTest.class);

  private static final String COMMIT_NRT_TYPE = "CommitNrtType";

  @Rule @ClassRule public static TemporaryFolder baseDir = new TemporaryFolder();

  private static SolrClient solrClient;

  private static MiniSolrCloudCluster miniSolrCloud;

  private static SolrCatalogProviderImpl provider;

  public static final String MASKED_ID = "scp";

  @BeforeClass
  public static void beforeClass() throws Exception {
    Path solrDataPath = Paths.get("target/test-classes/realtime");
    System.setProperty("ddf.home", solrDataPath.toString());
    System.setProperty("solr.install.dir", solrDataPath.toString());

    ConfigurationStore store = ConfigurationStore.getInstance();
    // Set to false for real time query by ID tests
    store.setForceAutoCommit(false);
    store.setDataDirectoryPath(solrDataPath.toString());

    System.clearProperty("https.protocols");
    System.clearProperty("https.cipherSuites");
    System.clearProperty("javax.net.ssl.keyStore");
    System.clearProperty("javax.net.ssl.keyStorePassword");
    System.clearProperty("javax.net.ssl.keyStoreType");
    System.clearProperty("javax.net.ssl.trustStore");
    System.clearProperty("javax.net.ssl.trustStorePassword");
    System.clearProperty("javax.net.ssl.trustStoreType");

    System.setProperty(
        "pkiHandlerPrivateKeyPath",
        SolrTestCaseJ4.class
            .getClassLoader()
            .getResource("cryptokeys/priv_key512_pkcs8.pem")
            .toExternalForm());
    System.setProperty(
        "pkiHandlerPublicKeyPath",
        SolrTestCaseJ4.class
            .getClassLoader()
            .getResource("cryptokeys/pub_key512.der")
            .toExternalForm());

    System.setProperty("jetty.testMode", "true");
    System.setProperty("jute.maxbuffer", "20000000"); // windows solution

    System.setProperty("zookeeper.4lw.commands.whitelist", "*");
    System.setProperty("solr.allowPaths", "*");

    miniSolrCloud =
        new MiniSolrCloudCluster.Builder(1, baseDir.getRoot().toPath())
            .withJettyConfig(jetty -> jetty.setContext("/solr"))
            .build();

    System.setProperty("solr.cloud.shardCount", "1");
    System.setProperty("solr.cloud.replicationFactor", "1");
    System.setProperty("solr.cloud.zookeeper.chroot", "/solr");
    System.setProperty("solr.cloud.zookeeper", miniSolrCloud.getZkServer().getZkHost());
    // Set soft commit and hard commit times high, so they will not impact the tests.
    System.setProperty(
        "solr.autoSoftCommit.maxTime", String.valueOf(TimeUnit.MINUTES.toMillis(30)));
    System.setProperty("solr.autoCommit.maxTime", String.valueOf(TimeUnit.MINUTES.toMillis((45))));
    System.setProperty("solr.commit.nrt.metacardTypes", COMMIT_NRT_TYPE);
    System.setProperty("solr.commit.nrt.commitWithinMs", "1");

    SolrCloudClientFactory solrClientFactory = new SolrCloudClientFactory();
    solrClient = solrClientFactory.newClient("catalog");

    DynamicSchemaResolver dynamicSchemaResolver = new DynamicSchemaResolver();
    dynamicSchemaResolver.addMetacardType(BASIC_METACARD);

    provider =
        new SolrCatalogProviderImpl(
            solrClient,
            new GeotoolsFilterAdapterImpl(),
            new SolrFilterDelegateFactoryImpl(),
            dynamicSchemaResolver);

    // Mask the id, this is something that the CatalogFramework would usually do
    provider.setId(MASKED_ID);

    RetryPolicy<Boolean> retryPolicy =
        RetryPolicy.<Boolean>builder()
            .handleResult(false)
            .withBackoff(Duration.ofMillis(10), Duration.ofMinutes(1))
            .withMaxDuration(Duration.ofMinutes(5))
            .withMaxRetries(-1)
            .build();

    Failsafe.with(retryPolicy).get(() -> provider.isAvailable());
    Failsafe.with(retryPolicy).run(() -> deleteAll(provider));
  }

  @AfterClass
  public static void afterClass() throws Exception {
    System.clearProperty("ddf.home");
    System.clearProperty("solr.install.dir");
    System.clearProperty("pkiHandlerPrivateKeyPath");
    System.clearProperty("pkiHandlerPublicKeyPath");
    System.clearProperty("jetty.testMode");
    System.clearProperty("jute.maxbuffer");
    System.clearProperty("zookeeper.4lw.commands.whitelist");
    System.clearProperty("solr.allowPaths");
    System.clearProperty("solr.cloud.shardCount");
    System.clearProperty("solr.cloud.replicationFactor");
    System.clearProperty("solr.cloud.zookeeper.chroot");
    System.clearProperty("solr.cloud.zookeeper");
    System.clearProperty("solr.autoSoftCommit.maxTime");
    System.clearProperty("solr.autoCommit.maxTime");
    System.clearProperty("solr.commit.nrt.metacardTypes");
    System.clearProperty("solr.commit.nrt.commitWithinMs");

    if (miniSolrCloud != null) {
      miniSolrCloud.shutdown();
    }

    if (solrClient != null) {
      solrClient.close();
    }

    if (provider != null) {
      provider.shutdown();
    }
  }

  @Test
  public void testRealTimeQueryById() throws Exception {

    final String metacardTitle = "testRealTimeQueryById";

    deleteAll(provider);

    MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());
    metacard.setTitle(metacardTitle);

    CreateResponse createResponse = create(metacard, provider);
    Optional<String> id =
        createResponse.getCreatedMetacards().stream().map(m -> m.getId()).findFirst();

    if (!id.isPresent()) {
      Assert.fail("Metacard creation failed for metacard with title: " + metacardTitle);
    }

    // Real time queries only work when querying by ID, so a real time query by title only will
    // return
    // 0 results.
    queryAndVerifyCount(
        0,
        getFilterBuilder().attribute(Metacard.TITLE).is().equalTo().text(metacardTitle),
        provider);

    // When performing a real time query by ID, we get the result.
    queryAndVerifyCount(
        1, getFilterBuilder().attribute(Metacard.ID).is().equalTo().text(id.get()), provider);
  }

  @Test
  public void testRealTimeQueryByIdAndTitle() throws Exception {

    final String metacardTitle = "testRealTimeQueryByIdAndTitle";

    deleteAll(provider);

    MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());
    metacard.setTitle(metacardTitle);

    CreateResponse createResponse = create(metacard, provider);
    Optional<String> id =
        createResponse.getCreatedMetacards().stream().map(m -> m.getId()).findFirst();

    if (!id.isPresent()) {
      Assert.fail("Metacard creation failed for metacard with title: " + metacardTitle);
    }

    // Real time queries only work when querying by ID, so a real time query by title only will
    // return
    // 0 results.
    queryAndVerifyCount(
        0,
        getFilterBuilder().attribute(Metacard.TITLE).is().equalTo().text(metacardTitle),
        provider);

    // When performing a real time query by ID and title, we get the result.
    queryAndVerifyCount(
        1,
        getFilterBuilder()
            .allOf(
                getFilterBuilder().attribute(Metacard.ID).is().equalTo().text(id.get()),
                getFilterBuilder().attribute(Metacard.TITLE).is().equalTo().text(metacardTitle)),
        provider);
  }

  @Test
  public void testRealTimeQueryMultipleIds() throws Exception {
    final String metacardTitle1 = "testRealTimeQueryMultipleIds1";
    final String metacardTitle2 = "testRealTimeQueryMultipleIds2";

    deleteAll(provider);

    MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    metacard1.setTitle(metacardTitle1);

    MockMetacard metacard2 = new MockMetacard(Library.getFlagstaffRecord());
    metacard1.setTitle(metacardTitle2);

    List<Metacard> metacards = new ArrayList<>(2);
    metacards.add(metacard1);
    metacards.add(metacard2);

    CreateResponse createResponse = create(metacards, provider);
    List<String> ids =
        createResponse.getCreatedMetacards().stream()
            .map(m -> m.getId())
            .collect(Collectors.toList());

    // Real time queries only work when querying by ID, so a real time query by title only will
    // return
    // 0 results.
    queryAndVerifyCount(
        0,
        getFilterBuilder().attribute(Metacard.TITLE).is().equalTo().text(metacardTitle1),
        provider);
    queryAndVerifyCount(
        0,
        getFilterBuilder().attribute(Metacard.TITLE).is().equalTo().text(metacardTitle2),
        provider);

    Filter filter =
        getFilterBuilder()
            .anyOf(
                getFilterBuilder().attribute(Metacard.ID).is().equalTo().text(ids.get(0)),
                getFilterBuilder().attribute(Metacard.ID).is().equalTo().text(ids.get(1)));

    // When performing a real time query by ID, we get the results.
    queryAndVerifyCount(2, filter, provider);
  }

  @Test
  public void testRealTimeQueryNotId() throws Exception {

    final String metacardTitle = "testRealTimeQueryNotId";

    deleteAll(provider);

    MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());
    metacard.setTitle(metacardTitle);

    CreateResponse createResponse = create(metacard, provider);
    Optional<String> id =
        createResponse.getCreatedMetacards().stream().map(m -> m.getId()).findFirst();

    assertTrue(
        "Metacard creation failed for metacard with title: " + metacardTitle, id.isPresent());

    // Verify the result is actually visible (searchable) in Solr using a real time query by ID.
    queryAndVerifyCount(
        1, getFilterBuilder().attribute(Metacard.ID).is().equalTo().text(id.get()), provider);

    Filter filter =
        getFilterBuilder().not(getFilterBuilder().attribute(Metacard.ID).equalTo().text(id.get()));

    // Verify a "not ID query" does not return the result.
    queryAndVerifyCount(0, filter, provider);
  }

  @Test
  public void testCommitNrt() throws Exception {

    final String nrtTitle = "testCommitNrt";
    final String nonNrtTitle = "testCommitNonNrt";

    deleteAll(provider);

    MetacardType nrtMetacardType =
        new MetacardTypeImpl(COMMIT_NRT_TYPE, BASIC_METACARD.getAttributeDescriptors());

    MockMetacard nrtMetacard = new MockMetacard(Library.getFlagstaffRecord(), nrtMetacardType);
    nrtMetacard.setTitle(nrtTitle);

    MockMetacard nonNrtMetacard = new MockMetacard(Library.getFlagstaffRecord());
    nonNrtMetacard.setTitle(nonNrtTitle);

    create(Lists.newArrayList(nrtMetacard, nonNrtMetacard), provider);

    QueryRequest request =
        new QueryRequestImpl(
            new QueryImpl(
                getFilterBuilder().attribute(Metacard.TITLE).is().like().text("testCommit*Nrt")));

    await()
        .pollInterval(100, TimeUnit.MILLISECONDS)
        .atMost(30, TimeUnit.SECONDS)
        .until(() -> provider.query(request).getResults().size() == 2);
  }

  /*similar to testRealTimeQueryNotId but this time we'll use a random ID for the filter. In this case
  the created metacard will match the filter but should not be returned because we shouldn't be doing a
  real time query.*/
  @Test
  public void testRealTimeQueryNotId2() throws Exception {

    final String metacardTitle = "testRealTimeQueryNotId";

    deleteAll(provider);

    MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());
    metacard.setTitle(metacardTitle);

    CreateResponse createResponse = create(metacard, provider);
    Optional<String> id =
        createResponse.getCreatedMetacards().stream().map(m -> m.getId()).findFirst();

    assertTrue(
        "Metacard creation failed for metacard with title: " + metacardTitle, id.isPresent());

    // Verify the result is actually visible (searchable) in Solr using a real time query by ID.
    queryAndVerifyCount(
        1, getFilterBuilder().attribute(Metacard.ID).is().equalTo().text(id.get()), provider);

    Filter filter =
        getFilterBuilder()
            .not(getFilterBuilder().attribute(Metacard.ID).equalTo().text("can't match this"));

    // Verify a "not ID query" does not return the result.
    queryAndVerifyCount(0, filter, provider);
  }

  @Test
  public void testRealTimeQueryByIdOrTitle() throws Exception {

    final String metacardTitle = "testRealTimeQueryById";

    deleteAll(provider);

    MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());
    metacard.setTitle(metacardTitle);

    CreateResponse createResponse = create(metacard, provider);
    Optional<String> id =
        createResponse.getCreatedMetacards().stream().map(m -> m.getId()).findFirst();

    if (!id.isPresent()) {
      Assert.fail("Metacard creation failed for metacard with title: " + metacardTitle);
    }

    // Real time queries only happen when querying by ID, so a query by title only will
    // return 0 results.
    queryAndVerifyCount(
        0,
        getFilterBuilder().attribute(Metacard.TITLE).is().equalTo().text(metacardTitle),
        provider);

    Filter filter =
        getFilterBuilder()
            .anyOf(
                getFilterBuilder().attribute(Metacard.ID).is().equalTo().text(id.get()),
                getFilterBuilder().attribute(Metacard.TITLE).is().equalTo().text(metacardTitle));

    // Verify an "ID or Title" query does not perform a real time get, and therefore,
    // does not return a result.
    queryAndVerifyCount(0, filter, provider);
  }

  @Test
  public void testRealTimeQueryMultipleByIdAndTitle() throws Exception {
    final String metacardTitle1 = "testRealTimeQueryMultipleIds1";
    final String metacardTitle2 = "testRealTimeQueryMultipleIds2";

    deleteAll(provider);

    MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    metacard1.setTitle(metacardTitle1);

    MockMetacard metacard2 = new MockMetacard(Library.getFlagstaffRecord());
    metacard1.setTitle(metacardTitle2);

    List<Metacard> metacards = new ArrayList<>(2);
    metacards.add(metacard1);
    metacards.add(metacard2);

    CreateResponse createResponse = create(metacards, provider);
    List<Metacard> createdMetacards = createResponse.getCreatedMetacards();

    // Real time queries only work when querying by ID, so a query by title only will
    // return 0 results.
    queryAndVerifyCount(
        0,
        getFilterBuilder().attribute(Metacard.TITLE).is().equalTo().text(metacardTitle1),
        provider);
    queryAndVerifyCount(
        0,
        getFilterBuilder().attribute(Metacard.TITLE).is().equalTo().text(metacardTitle2),
        provider);

    // this filter will look like ((id = <id0> AND title = <title0>) OR (id = <id1> AND title =
    // <title1>))
    Filter filter =
        getFilterBuilder()
            .anyOf(
                getFilterBuilder()
                    .allOf(
                        getFilterBuilder()
                            .attribute(Metacard.ID)
                            .is()
                            .equalTo()
                            .text(createdMetacards.get(0).getId()),
                        getFilterBuilder()
                            .attribute(Metacard.TITLE)
                            .is()
                            .equalTo()
                            .text(createdMetacards.get(0).getTitle())),
                getFilterBuilder()
                    .allOf(
                        getFilterBuilder()
                            .attribute(Metacard.ID)
                            .is()
                            .equalTo()
                            .text(createdMetacards.get(1).getId()),
                        getFilterBuilder()
                            .attribute(Metacard.TITLE)
                            .is()
                            .equalTo()
                            .text(createdMetacards.get(1).getTitle())));

    // When performing a real time query by ID, we get the results.
    queryAndVerifyCount(2, filter, provider);
  }
}
