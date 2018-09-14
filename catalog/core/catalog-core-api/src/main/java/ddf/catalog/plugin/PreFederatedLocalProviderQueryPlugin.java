package ddf.catalog.plugin;

import ddf.catalog.operation.QueryRequest;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceCache;
import java.util.List;

public abstract class PreFederatedLocalProviderQueryPlugin implements PreFederatedQueryPlugin {

  protected final List<CatalogProvider> catalogProviders;

  public PreFederatedLocalProviderQueryPlugin(List<CatalogProvider> catalogProviders) {
    this.catalogProviders = catalogProviders;
  }

  protected boolean isCacheSource(Source source) {
    return source instanceof SourceCache;
  }

  protected boolean isCatalogProvider(Source source) {
    return source instanceof CatalogProvider
        && catalogProviders.stream().map(CatalogProvider::getId).anyMatch(source.getId()::equals);
  }

  /** Given a source, determine if it is a registered catalog provider or a cache. */
  protected boolean isLocalSource(Source source) {
    return isCacheSource(source) || isCatalogProvider(source);
  }

  public abstract QueryRequest process(Source source, QueryRequest input)
      throws StopProcessingException;
}
