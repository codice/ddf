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
import ddf.catalog.source.solr.provider.SolrProviderXpath;
import java.nio.file.Paths;
import org.codice.solr.factory.impl.ConfigurationFileProxy;
import org.codice.solr.factory.impl.ConfigurationStore;
import org.codice.solr.factory.impl.EmbeddedSolrFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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
  SolrProviderUpdate.class,
  SolrProviderXpath.class
})
public class SolrProviderTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrProviderTest.class);

  private static String cipherSuites;

  private static String protocols;

  private static String threadPoolSize;

  private static SolrCatalogProvider provider = null;

  public static final String MASKED_ID = "scp";

  @BeforeClass
  public static void setup() {
    cipherSuites = System.getProperty("https.cipherSuites");
    protocols = System.getProperty("https.protocols");
    System.setProperty(
        "https.cipherSuites",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA");
    System.setProperty("https.protocols", "TLSv1.1, TLSv1.2");
    threadPoolSize = System.getProperty("org.codice.ddf.system.threadPoolSize");
    System.setProperty("org.codice.ddf.system.threadPoolSize", "128");
    LOGGER.info("RUNNING one-time setup.");
    ConfigurationStore store = ConfigurationStore.getInstance();
    store.setInMemory(true);
    store.setForceAutoCommit(true);
    String solrDataPath = Paths.get("target/surefire/solr").toString();
    System.getProperty("solr.data.dir", solrDataPath);
    store.setDataDirectoryPath(solrDataPath);
    ConfigurationFileProxy configurationFileProxy = new ConfigurationFileProxy(store);

    provider =
        new SolrCatalogProvider(
            EmbeddedSolrFactory.getEmbeddedSolrServer(
                "catalog", "solrconfig-inmemory.xml", "schema.xml", configurationFileProxy),
            new GeotoolsFilterAdapterImpl(),
            new SolrFilterDelegateFactoryImpl());

    // Mask the id, this is something that the CatalogFramework would
    // usually do
    provider.setId(MASKED_ID);
  }

  @AfterClass
  public static void teardown() {
    if (threadPoolSize != null) {
      System.setProperty("org.codice.ddf.system.threadPoolSize", threadPoolSize);
    } else {
      System.clearProperty("org.codice.ddf.system.threadPoolSize");
    }
    if (cipherSuites != null) {
      System.setProperty("https.cipherSuites", cipherSuites);
    } else {
      System.clearProperty("https.cipherSuites");
    }
    if (protocols != null) {
      System.setProperty("https.protocols", protocols);
    } else {
      System.clearProperty("https.protocols");
    }
  }

  public static SolrCatalogProvider getProvider() {
    return provider;
  }
}
