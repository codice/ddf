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

import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.create;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.deleteAll;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.getFilterBuilder;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.queryAndVerifyCount;
import static junit.framework.TestCase.assertTrue;
import static org.awaitility.Awaitility.await;

import com.google.common.collect.Lists;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.solr.provider.Library;
import ddf.catalog.source.solr.provider.MockMetacard;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.codice.solr.client.solrj.SolrClient;
import org.codice.solr.factory.impl.SolrCloudClientFactory;
import org.hamcrest.Matchers;
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

  private static SolrCatalogProvider provider;

  public static final String MASKED_ID = "scp";

  @BeforeClass
  public static void beforeClass() throws Exception {
    ConfigurationStore store = ConfigurationStore.getInstance();
    // Set to false for real time query by ID tests
    store.setForceAutoCommit(false);
    String solrDataPath = Paths.get("target/surefire/solr/realtime").toString();
    System.setProperty("solr.data.dir", solrDataPath);
    store.setDataDirectoryPath(solrDataPath);

    System.setProperty("jute.maxbuffer", "20000000"); // windows solution

    miniSolrCloud =
        new MiniSolrCloudCluster(
            1, baseDir.getRoot().toPath(), JettyConfig.builder().setContext("/solr").build());

    System.setProperty("solr.cloud.shardCount", "1");
    System.setProperty("solr.cloud.replicationFactor", "1");
    System.setProperty("solr.cloud.maxShardPerNode", "1");
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

    Assert.assertThat(
        "Solr client is not available for testing",
        solrClient.isAvailable(30L, TimeUnit.SECONDS),
        Matchers.equalTo(true));

    provider =
        new SolrCatalogProvider(
            solrClient, new GeotoolsFilterAdapterImpl(), new SolrFilterDelegateFactoryImpl());

    // Mask the id, this is something that the CatalogFramework would usually do
    provider.setId(MASKED_ID);
  }

  @AfterClass
  public static void afterClass() throws Exception {
    System.clearProperty("solr.data.dir");
    System.clearProperty("solr.cloud.shardCount");
    System.clearProperty("solr.cloud.replicationFactor");
    System.clearProperty("solr.cloud.maxShardPerNode");
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
        createResponse
            .getCreatedMetacards()
            .stream()
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
        new MetacardTypeImpl(
            COMMIT_NRT_TYPE, MetacardImpl.BASIC_METACARD.getAttributeDescriptors());

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
