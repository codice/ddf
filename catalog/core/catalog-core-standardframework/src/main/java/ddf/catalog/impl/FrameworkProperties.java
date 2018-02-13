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
package ddf.catalog.impl;

import ddf.catalog.cache.solr.impl.ValidationQueryFactory;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.plugin.PostCreateStoragePlugin;
import ddf.catalog.content.plugin.PostUpdateStoragePlugin;
import ddf.catalog.content.plugin.PreCreateStoragePlugin;
import ddf.catalog.content.plugin.PreUpdateStoragePlugin;
import ddf.catalog.data.AttributeInjector;
import ddf.catalog.data.DefaultAttributeValueRegistry;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventPublisher;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.plugin.PreAuthorizationPlugin;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.PreResourcePlugin;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.resource.download.ReliableResourceDownloadManager;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.CatalogStore;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.util.impl.SourcePoller;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeToTransformerMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.codice.ddf.catalog.transform.Transform;
import org.osgi.framework.BundleContext;

/**
 * Properties relating to the {@link CatalogFrameworkImpl} class. This class is used to create a new
 * instance of {@link CatalogFrameworkImpl}.
 */
public class FrameworkProperties {

  private List<CatalogProvider> catalogProviders = new ArrayList<>();

  private Map<String, CatalogStore> catalogStoresMap = new HashMap<>();

  private BundleContext bundleContext;

  private List<PreIngestPlugin> preIngest = new ArrayList<>();

  private List<PostIngestPlugin> postIngest = new ArrayList<>();

  private List<PreQueryPlugin> preQuery = new ArrayList<>();

  private List<PostQueryPlugin> postQuery = new ArrayList<>();

  private List<PreResourcePlugin> preResource = new ArrayList<>();

  private List<PostResourcePlugin> postResource = new ArrayList<>();

  private List<PreAuthorizationPlugin> preAuthorizationPlugins = new ArrayList<>();

  private List<PolicyPlugin> policyPlugins = new ArrayList<>();

  private List<AccessPlugin> accessPlugins = new ArrayList<>();

  private List<ConnectedSource> connectedSources = new ArrayList<>();

  private Map<String, FederatedSource> federatedSources = new HashMap<>();

  private List<ResourceReader> resourceReaders = new ArrayList<>();

  private FederationStrategy federationStrategy;

  private QueryResponsePostProcessor queryResponsePostProcessor;

  private ExecutorService pool;

  private SourcePoller sourcePoller;

  private DownloadsStatusEventPublisher downloadsStatusEventPublisher;

  private ReliableResourceDownloadManager reliableResourceDownloadManager;

  private FilterBuilder filterBuilder;

  private MimeTypeToTransformerMapper mimeTypeToTransformerMapper;

  private Transform transform;

  private MimeTypeMapper mimeTypeMapper;

  private List<StorageProvider> storageProviders = new ArrayList<>();

  private List<PreCreateStoragePlugin> preCreateStoragePlugins = new ArrayList<>();

  private List<PostCreateStoragePlugin> postCreateStoragePlugins = new ArrayList<>();

  private List<PreUpdateStoragePlugin> preUpdateStoragePlugins = new ArrayList<>();

  private List<PostUpdateStoragePlugin> postUpdateStoragePlugins = new ArrayList<>();

  private ValidationQueryFactory validationQueryFactory;

  private DefaultAttributeValueRegistry defaultAttributeValueRegistry;

  private List<AttributeInjector> attributeInjectors = new ArrayList<>();

  public List<CatalogProvider> getCatalogProviders() {
    return catalogProviders;
  }

  public void setCatalogProviders(List<CatalogProvider> catalogProviders) {
    this.catalogProviders = catalogProviders;
  }

  public BundleContext getBundleContext() {
    return bundleContext;
  }

  public void setBundleContext(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }

  public List<PreIngestPlugin> getPreIngest() {
    return preIngest;
  }

  public void setPreIngest(List<PreIngestPlugin> preIngest) {
    this.preIngest = preIngest;
  }

  public List<PostIngestPlugin> getPostIngest() {
    return postIngest;
  }

  public void setPostIngest(List<PostIngestPlugin> postIngest) {
    this.postIngest = postIngest;
  }

  public List<PreQueryPlugin> getPreQuery() {
    return preQuery;
  }

  public void setPreQuery(List<PreQueryPlugin> preQuery) {
    this.preQuery = preQuery;
  }

  public List<PostQueryPlugin> getPostQuery() {
    return postQuery;
  }

  public void setPostQuery(List<PostQueryPlugin> postQuery) {
    this.postQuery = postQuery;
  }

  public List<PreResourcePlugin> getPreResource() {
    return preResource;
  }

  public void setPreResource(List<PreResourcePlugin> preResource) {
    this.preResource = preResource;
  }

  public List<PostResourcePlugin> getPostResource() {
    return postResource;
  }

  public void setPostResource(List<PostResourcePlugin> postResource) {
    this.postResource = postResource;
  }

  public List<PreAuthorizationPlugin> getPreAuthorizationPlugins() {
    return preAuthorizationPlugins;
  }

  public void setPreAuthorizationPlugins(List<PreAuthorizationPlugin> preAuthorizationPlugins) {
    this.preAuthorizationPlugins = preAuthorizationPlugins;
  }

  public List<PolicyPlugin> getPolicyPlugins() {
    return policyPlugins;
  }

  public void setPolicyPlugins(List<PolicyPlugin> policyPlugins) {
    this.policyPlugins = policyPlugins;
  }

  public List<AccessPlugin> getAccessPlugins() {
    return accessPlugins;
  }

  public void setAccessPlugins(List<AccessPlugin> accessPlugins) {
    this.accessPlugins = accessPlugins;
  }

  public List<ConnectedSource> getConnectedSources() {
    return connectedSources;
  }

  public void setConnectedSources(List<ConnectedSource> connectedSources) {
    this.connectedSources = connectedSources;
  }

  public Map<String, FederatedSource> getFederatedSources() {
    return federatedSources;
  }

  public void setFederatedSources(Map<String, FederatedSource> federatedSources) {
    this.federatedSources = federatedSources;
  }

  public List<ResourceReader> getResourceReaders() {
    return resourceReaders;
  }

  public void setResourceReaders(List<ResourceReader> resourceReaders) {
    this.resourceReaders = resourceReaders;
  }

  public FederationStrategy getFederationStrategy() {
    return federationStrategy;
  }

  public void setFederationStrategy(FederationStrategy federationStrategy) {
    this.federationStrategy = federationStrategy;
  }

  public QueryResponsePostProcessor getQueryResponsePostProcessor() {
    return queryResponsePostProcessor;
  }

  public void setQueryResponsePostProcessor(QueryResponsePostProcessor queryResponsePostProcessor) {
    this.queryResponsePostProcessor = queryResponsePostProcessor;
  }

  public ExecutorService getPool() {
    return pool;
  }

  public void setPool(ExecutorService pool) {
    this.pool = pool;
  }

  public SourcePoller getSourcePoller() {
    return sourcePoller;
  }

  public void setSourcePoller(SourcePoller sourcePoller) {
    this.sourcePoller = sourcePoller;
  }

  public DownloadsStatusEventPublisher getDownloadsStatusEventPublisher() {
    return downloadsStatusEventPublisher;
  }

  public void setDownloadsStatusEventPublisher(
      DownloadsStatusEventPublisher downloadsStatusEventPublisher) {
    this.downloadsStatusEventPublisher = downloadsStatusEventPublisher;
  }

  public ReliableResourceDownloadManager getReliableResourceDownloadManager() {
    return reliableResourceDownloadManager;
  }

  public void setReliableResourceDownloadManager(
      ReliableResourceDownloadManager reliableResourceDownloadManager) {
    this.reliableResourceDownloadManager = reliableResourceDownloadManager;
  }

  public Map<String, CatalogStore> getCatalogStoresMap() {
    return catalogStoresMap;
  }

  public void setCatalogStoresMap(Map<String, CatalogStore> catalogStoresMap) {
    this.catalogStoresMap = catalogStoresMap;
  }

  public FilterBuilder getFilterBuilder() {
    return filterBuilder;
  }

  public void setFilterBuilder(FilterBuilder filterBuilder) {
    this.filterBuilder = filterBuilder;
  }

  public MimeTypeToTransformerMapper getMimeTypeToTransformerMapper() {
    return mimeTypeToTransformerMapper;
  }

  public void setMimeTypeToTransformerMapper(
      MimeTypeToTransformerMapper mimeTypeToTransformerMapper) {
    this.mimeTypeToTransformerMapper = mimeTypeToTransformerMapper;
  }

  public MimeTypeMapper getMimeTypeMapper() {
    return mimeTypeMapper;
  }

  public void setMimeTypeMapper(MimeTypeMapper mimeTypeMapper) {
    this.mimeTypeMapper = mimeTypeMapper;
  }

  public List<StorageProvider> getStorageProviders() {
    return storageProviders;
  }

  public void setStorageProviders(List<StorageProvider> storageProviders) {
    this.storageProviders = storageProviders;
  }

  public List<PreCreateStoragePlugin> getPreCreateStoragePlugins() {
    return preCreateStoragePlugins;
  }

  public void setPreCreateStoragePlugins(List<PreCreateStoragePlugin> preCreateStoragePlugins) {
    this.preCreateStoragePlugins = preCreateStoragePlugins;
  }

  public List<PostCreateStoragePlugin> getPostCreateStoragePlugins() {
    return postCreateStoragePlugins;
  }

  public void setPostCreateStoragePlugins(List<PostCreateStoragePlugin> postCreateStoragePlugins) {
    this.postCreateStoragePlugins = postCreateStoragePlugins;
  }

  public List<PreUpdateStoragePlugin> getPreUpdateStoragePlugins() {
    return preUpdateStoragePlugins;
  }

  public void setPreUpdateStoragePlugins(List<PreUpdateStoragePlugin> preUpdateStoragePlugins) {
    this.preUpdateStoragePlugins = preUpdateStoragePlugins;
  }

  public List<PostUpdateStoragePlugin> getPostUpdateStoragePlugins() {
    return postUpdateStoragePlugins;
  }

  public void setPostUpdateStoragePlugins(List<PostUpdateStoragePlugin> postUpdateStoragePlugins) {
    this.postUpdateStoragePlugins = postUpdateStoragePlugins;
  }

  public void setValidationQueryFactory(ValidationQueryFactory validationQueryFactory) {
    this.validationQueryFactory = validationQueryFactory;
  }

  public ValidationQueryFactory getValidationQueryFactory() {
    return this.validationQueryFactory;
  }

  public void setDefaultAttributeValueRegistry(
      DefaultAttributeValueRegistry defaultAttributeValueRegistry) {
    this.defaultAttributeValueRegistry = defaultAttributeValueRegistry;
  }

  public DefaultAttributeValueRegistry getDefaultAttributeValueRegistry() {
    return defaultAttributeValueRegistry;
  }

  public void setAttributeInjectors(List<AttributeInjector> attributeInjectors) {
    this.attributeInjectors = attributeInjectors;
  }

  public List<AttributeInjector> getAttributeInjectors() {
    return attributeInjectors;
  }

  public Transform getTransform() {
    return transform;
  }

  public void setTransform(Transform transform) {
    this.transform = transform;
  }
}
