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
import static ddf.catalog.source.solr.DynamicSchemaResolver.FIVE_MEGABYTES;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.deleteAll;

import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.source.solr.provider.SolrProviderContentTypes;
import ddf.catalog.source.solr.provider.SolrProviderCreate;
import ddf.catalog.source.solr.provider.SolrProviderDelete;
import ddf.catalog.source.solr.provider.SolrProviderExtensibleMetacards;
import ddf.catalog.source.solr.provider.SolrProviderQuery;
import ddf.catalog.source.solr.provider.SolrProviderSorting;
import ddf.catalog.source.solr.provider.SolrProviderSource;
import ddf.catalog.source.solr.provider.SolrProviderSpatial;
import ddf.catalog.source.solr.provider.SolrProviderTemporal;
import ddf.catalog.source.solr.provider.SolrProviderUpdate;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.codice.solr.factory.impl.SolrCloudClientFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  SolrProviderContentTypes.class,
  SolrProviderCreate.class,
  SolrProviderDelete.class,
  SolrProviderExtensibleMetacards.class,
  SolrProviderQuery.class,
  SolrProviderSorting.class,
  SolrProviderSource.class,
  SolrProviderSpatial.class,
  SolrProviderTemporal.class,
  SolrProviderUpdate.class
})
public class SolrProviderTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrProviderTest.class);

  private static final String COLLECTION = "catalog";

  @Rule @ClassRule public static TemporaryFolder baseDir = new TemporaryFolder();

  private static SolrClient solrClient;

  protected static SolrCatalogProviderImpl provider = null;

  private static MiniSolrCloudCluster miniSolrCloud;

  public static final String MASKED_ID = "scp";

  @BeforeClass
  public static void beforeClass() throws Exception {
    Path solrDataPath = Paths.get("target/test-classes/data");
    System.setProperty("ddf.home", solrDataPath.toString());

    ConfigurationStore store = ConfigurationStore.getInstance();
    store.setForceAutoCommit(true);
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

    miniSolrCloud =
        new MiniSolrCloudCluster.Builder(1, baseDir.getRoot().toPath())
            .withJettyConfig(jetty -> jetty.setContext("/solr"))
            .build();

    System.setProperty("solr.cloud.shardCount", "1");
    System.setProperty("solr.cloud.replicationFactor", "1");
    System.setProperty("solr.cloud.zookeeper.chroot", "/solr");
    System.setProperty("solr.cloud.zookeeper", miniSolrCloud.getZkServer().getZkHost());
    System.setProperty("metadata.size.limit", Integer.toString(FIVE_MEGABYTES));
    System.setProperty("solr.query.sort.caseInsensitive", "true");

    SolrCloudClientFactory solrClientFactory = new SolrCloudClientFactory();
    solrClient = solrClientFactory.newClient(COLLECTION);

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
    System.clearProperty("pkiHandlerPrivateKeyPath");
    System.clearProperty("pkiHandlerPublicKeyPath");
    System.clearProperty("jetty.testMode");
    System.clearProperty("jute.maxbuffer");
    System.clearProperty("solr.cloud.shardCount");
    System.clearProperty("solr.cloud.replicationFactor");
    System.clearProperty("solr.cloud.zookeeper.chroot");
    System.clearProperty("solr.cloud.zookeeper");
    System.clearProperty("metadata.size.limit");
    System.clearProperty("solr.query.sort.caseInsensitive");

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

  public static SolrCatalogProviderImpl getProvider() {
    return provider;
  }
}
