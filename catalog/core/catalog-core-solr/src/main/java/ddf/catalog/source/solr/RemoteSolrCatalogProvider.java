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
import javax.annotation.Nullable;
import org.codice.solr.client.solrj.SolrClient;
import org.codice.solr.factory.impl.ConfigurationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common base class for all remote Solr Catalog providers. */
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

  private final CatalogProvider provider;

  private SolrClient client;

  private FilterAdapter filterAdapter;

  private SolrFilterDelegateFactory solrFilterDelegateFactory;

  private DynamicSchemaResolver resolver;

  /**
   * Constructor.
   *
   * @param filterAdapter filter adaptor this provider will use
   * @param client client this provider will use to connect to Solr
   * @param solrFilterDelegateFactory Solr filter delegate factory this provider will use
   * @param resolver schema resolver this provider will use. A default schema resolver will be used
   *     if this parameter is {@code null}.
   */
  public RemoteSolrCatalogProvider(
      FilterAdapter filterAdapter,
      SolrClient client,
      SolrFilterDelegateFactory solrFilterDelegateFactory,
      @Nullable DynamicSchemaResolver resolver) {
    notNull(filterAdapter, "FilterAdapter cannot be null");
    notNull(solrFilterDelegateFactory, "SolrFilterDelegateFactory cannot be null");

    this.filterAdapter = filterAdapter;
    this.client = client;
    this.solrFilterDelegateFactory = solrFilterDelegateFactory;
    this.resolver = (resolver == null) ? new DynamicSchemaResolver() : resolver;
    this.provider =
        new SolrCatalogProvider(client, filterAdapter, solrFilterDelegateFactory, this.resolver);
    provider.maskId(getId());
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
    LOGGER.debug("Closing connection to Solr client.");
    final SolrClient c = client;
    try {
      c.close();
    } catch (IOException e) {
      LOGGER.info("Unable to close Solr client", e);
    }
    LOGGER.debug("Finished closing connection to Solr client.");
  }

  private CatalogProvider getProvider() {
    return provider;
  }
}
