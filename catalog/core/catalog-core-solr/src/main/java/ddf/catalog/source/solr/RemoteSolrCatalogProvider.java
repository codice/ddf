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

import static org.apache.commons.lang.Validate.notNull;

import ddf.catalog.data.ContentType;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.MaskableImpl;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.codice.ddf.configuration.PropertyResolver;
import org.codice.solr.factory.impl.ConfigurationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common base class for all remote Solr Catalog providers. Sub-classes need to implement the {@link
 * #createClient()} method and return a new {@code SolrClient}.
 */
public abstract class RemoteSolrCatalogProvider extends MaskableImpl implements CatalogProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(RemoteSolrCatalogProvider.class);

  private static final String PING_ERROR_MESSAGE = "Solr ping failed.";

  private static final String OK_STATUS = "OK";

  private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

  private static Properties describableProperties = new Properties();

  static {
    try (InputStream inputStream =
        RemoteSolrCatalogProvider.class.getResourceAsStream(DESCRIBABLE_PROPERTIES_FILE)) {
      describableProperties.load(inputStream);
    } catch (IOException e) {
      LOGGER.info("Did not load properties properly.", e);
    }
  }

  protected static final String SOLR_CATALOG_CORE_NAME = "catalog";

  private String url;

  private CatalogProvider provider = new UnavailableSolrCatalogProvider();

  private SolrClient client;

  private FilterAdapter filterAdapter;

  private SolrFilterDelegateFactory solrFilterDelegateFactory;

  private DynamicSchemaResolver resolver;

  private Future<SolrClient> clientFuture;

  /**
   * Constructor.
   *
   * @param filterAdapter filter adaptor this provider will use
   * @param client client this provider will use to connect to Solr. If set to {@code null}, a new
   *     client will be created using {@link #createClient()} when needed.
   * @param solrFilterDelegateFactory Solr filter delegate factory this provider will use
   * @param resolver schema resolver this provider will use. A default schema resolver will be used
   *     if this parameter is {@code null}.
   */
  public RemoteSolrCatalogProvider(
      FilterAdapter filterAdapter,
      @Nullable SolrClient client,
      SolrFilterDelegateFactory solrFilterDelegateFactory,
      @Nullable DynamicSchemaResolver resolver) {
    notNull(filterAdapter, "FilterAdapter cannot be null");
    notNull(solrFilterDelegateFactory, "SolrFilterDelegateFactory cannot be null");

    this.filterAdapter = filterAdapter;
    this.client = client;
    this.solrFilterDelegateFactory = solrFilterDelegateFactory;
    this.resolver = (resolver == null) ? new DynamicSchemaResolver() : resolver;
  }

  /**
   * Constructor. Uses a default {@link DynamicSchemaResolver}.
   *
   * @param filterAdapter filter adaptor this provider will use
   * @param client client this provider will use to connect to Solr. If set to {@code null}, a new
   *     client will be created using {@link #createClient()} when needed.
   * @param solrFilterDelegateFactory Solr filter delegate factory this provider will use
   */
  public RemoteSolrCatalogProvider(
      FilterAdapter filterAdapter,
      @Nullable SolrClient client,
      SolrFilterDelegateFactory solrFilterDelegateFactory) {
    this(filterAdapter, client, solrFilterDelegateFactory, null);
  }

  /**
   * Constructor. Creates and uses a default {@code SolrClient} and {@link DynamicSchemaResolver}.
   *
   * @param filterAdapter filter adaptor this provider will use
   * @param solrFilterDelegateFactory Solr filter delegate factory this provider will use
   */
  public RemoteSolrCatalogProvider(
      FilterAdapter filterAdapter, SolrFilterDelegateFactory solrFilterDelegateFactory) {
    this(filterAdapter, null, solrFilterDelegateFactory, null);
    updateClient();
  }

  @Override
  public void maskId(String id) {
    super.maskId(id);
    provider.maskId(id);
  }

  /**
   * Used to signal to the Solr client to commit on every transaction. Updates the underlying {@link
   * ConfigurationStore} so that the property is propagated throughout the Solr Catalog Provider
   * code.
   *
   * @param forceAutoCommit {@code true} to force auto-commits
   */
  public void setForceAutoCommit(boolean forceAutoCommit) {
    ConfigurationStore.getInstance().setForceAutoCommit(forceAutoCommit);
  }

  /**
   * Disables text path indexing for every subsequent update or insert.
   *
   * @param disableTextPath {@code true} to turn off text path indexing
   */
  public void setDisableTextPath(boolean disableTextPath) {
    ConfigurationStore.getInstance().setDisableTextPath(disableTextPath);
  }

  @Override
  public Set<ContentType> getContentTypes() {
    return getProvider().getContentTypes();
  }

  @Override
  public boolean isAvailable() {
    return getProvider().isAvailable();
  }

  @Override
  public boolean isAvailable(SourceMonitor callback) {
    return getProvider().isAvailable(callback);
  }

  @Override
  public SourceResponse query(QueryRequest queryRequest) throws UnsupportedQueryException {
    return getProvider().query(queryRequest);
  }

  @Override
  public String getDescription() {
    return describableProperties.getProperty("description", "");
  }

  @Override
  public String getOrganization() {
    return describableProperties.getProperty("organization", "");
  }

  @Override
  public String getTitle() {
    return describableProperties.getProperty("name", "");
  }

  @Override
  public String getVersion() {
    return describableProperties.getProperty("version", "");
  }

  @Override
  public CreateResponse create(CreateRequest createRequest) throws IngestException {
    return getProvider().create(createRequest);
  }

  @Override
  public DeleteResponse delete(DeleteRequest deleteRequest) throws IngestException {
    return getProvider().delete(deleteRequest);
  }

  @Override
  public UpdateResponse update(UpdateRequest updateRequest) throws IngestException {
    return getProvider().update(updateRequest);
  }

  /** Shuts down the connection to Solr and releases resources. */
  public void shutdown() {
    closeSolrClient();
  }

  /**
   * Returns the current Solr server URL. The format of the URL may vary based on the current Solr
   * configuration (embedded, external or cloud).
   *
   * @return current Solr URL
   */
  @Nullable
  public String getUrl() {
    return url;
  }

  /**
   * Sets Solr's URL. The format of the URL may vary based on the current Solr configuration, e.g.,
   * embedded, external or cloud. See the {@code system.properties} configuration file for the
   * different options and formats. <br>
   * Changing the URL will trigger the creation of a new Solr client using {@link #createClient()}.
   *
   * @param url new Solr URL
   */
  public void setUrl(@Nullable String url) {
    updateClient(PropertyResolver.resolveProperties(url));
  }

  /** Forces an update of the Solr client. */
  protected void updateClient() {
    clientFuture = createClient();
    client = null;
  }

  /**
   * Request the creation of a new {@code SolrClient}.
   *
   * @return {@code Future} used to retrieve the new {@code SolrClient} created
   */
  protected abstract Future<SolrClient> createClient();

  private void updateClient(@Nullable String urlValue) {
    LOGGER.debug("New url {}", urlValue);

    if (urlValue != null) {
      if (!StringUtils.equalsIgnoreCase(urlValue.trim(), url) || getClient() == null) {
        url = urlValue.trim();

        if (getClient() != null) {
          LOGGER.debug(
              "Shutting down the connection manager to Solr and releasing allocated resources.");
          closeSolrClient();
          LOGGER.debug("Shutdown complete.");
        }

        updateClient();
      }

    } else {
      url = null;
    }
  }

  private void closeSolrClient() {
    LOGGER.debug("Closing connection to Solr client.");

    if (getClient() != null) {
      try {
        getClient().close();
      } catch (IOException e) {
        LOGGER.info("Unable to close Solr client", e);
      }
    } else if (clientFuture != null && !clientFuture.isDone() && !clientFuture.isCancelled()) {
      clientFuture.cancel(true);
    }

    LOGGER.debug("Finished closing connection to Solr client.");
  }

  private CatalogProvider getProvider() {

    if (!isClientConnected(getClient())) {
      return new UnavailableSolrCatalogProvider();
    }

    if (provider instanceof UnavailableSolrCatalogProvider) {
      provider =
          new SolrCatalogProvider(getClient(), filterAdapter, solrFilterDelegateFactory, resolver);
    }

    provider.maskId(getId());
    return provider;
  }

  private boolean isClientConnected(SolrClient solr) {

    if (solr == null) {
      return false;
    }

    try {
      return OK_STATUS.equals(solr.ping().getResponse().get("status"));
    } catch (Exception e) {
      /*
       * if we get any type of exception, whether declared by Solr or not, we do not want to
       * fail, we just want to return false
       */
      LOGGER.info(PING_ERROR_MESSAGE);
      LOGGER.debug(PING_ERROR_MESSAGE, e);
    }
    return false;
  }

  private SolrClient getClient() {
    if (client == null && clientFuture != null) {
      try {
        SolrClient solrClient = clientFuture.get(5, TimeUnit.SECONDS);

        if (solrClient == null) {
          // If we fail to get a SolrClient after all potential retries have been
          // exhausted, call updateClient() to keep trying.
          // See SolrClientFactory.newClient() for details.
          updateClient();
        }

        return solrClient;
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        LOGGER.debug("Failed to get client from future", e);
      }
    }
    return client;
  }
}
