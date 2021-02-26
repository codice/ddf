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
package ddf.catalog.impl.operations;

import ddf.action.Action;
import ddf.action.ActionRegistry;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.data.ContentType;
import ddf.catalog.impl.FrameworkProperties;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.impl.SourceInfoResponseImpl;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.impl.SourceDescriptorImpl;
import ddf.catalog.util.Describable;
import ddf.catalog.util.impl.DescribableImpl;
import ddf.catalog.util.impl.SourceDescriptorComparator;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.lang.Validate;
import org.codice.ddf.catalog.sourcepoller.SourcePoller;
import org.codice.ddf.catalog.sourcepoller.SourceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support class for source delegate operations for the {@code CatalogFrameworkImpl}.
 *
 * <p>This class contains two delegated source methods and methods to support them. No
 * operations/support methods should be added to this class except in support of CFI source
 * operations.
 */
public class SourceOperations extends DescribableImpl {
  private static final Logger LOGGER = LoggerFactory.getLogger(SourceOperations.class);

  private static final String GET_SOURCE_EXCEPTION_MSG =
      "Exception during runtime while performing getSourceInfo";

  //
  // Injected properties
  //
  private final FrameworkProperties frameworkProperties;

  private final ActionRegistry sourceActionRegistry;

  private final SourcePoller<SourceStatus> sourceStatusCache;

  private final SourcePoller<Set<ContentType>> contentTypesCache;

  //
  // Bound objects
  //
  private CatalogProvider catalog;

  private StorageProvider storage;

  /** @throws IllegalArgumentException if any of the parameters are {@code null} */
  public SourceOperations(
      FrameworkProperties frameworkProperties,
      ActionRegistry sourceActionRegistry,
      SourcePoller<SourceStatus> sourceStatusCache,
      SourcePoller<Set<ContentType>> contentTypesCache) {
    Validate.notNull(frameworkProperties, "frameworkProperties must be non-null");
    Validate.notNull(sourceActionRegistry, "sourceActionRegistry must be non-null");
    Validate.notNull(sourceStatusCache, "sourceStatusSourcePoller must be non-null");
    Validate.notNull(contentTypesCache, "contentTypesSourcePoller must be non-null");

    this.frameworkProperties = frameworkProperties;
    this.sourceActionRegistry = sourceActionRegistry;
    this.sourceStatusCache = sourceStatusCache;
    this.contentTypesCache = contentTypesCache;
  }

  /**
   * Invoked by blueprint when a {@link CatalogProvider} is created and bound to this
   * CatalogFramework instance.
   *
   * <p>The local catalog provider will be set to the first item in the {@link java.util.List} of
   * {@link CatalogProvider}s bound to this CatalogFramework.
   *
   * @param catalogProvider the {@link CatalogProvider} being bound to this CatalogFramework
   *     instance
   */
  public void bind(CatalogProvider catalogProvider) {
    LOGGER.trace("ENTERING: bind");

    catalog = frameworkProperties.getCatalogProviders().stream().findFirst().orElse(null);

    LOGGER.trace("EXITING: bind with catalog = {}", catalog);
  }

  /**
   * Invoked by blueprint when a {@link CatalogProvider} is deleted and unbound from this
   * CatalogFramework instance.
   *
   * <p>The local catalog provider will be reset to the new first item in the {@link java.util.List}
   * of {@link CatalogProvider}s bound to this CatalogFramework. If this list of catalog providers
   * is currently empty, then the local catalog provider will be set to <code>null</code>.
   *
   * @param catalogProvider the {@link CatalogProvider} being unbound from this CatalogFramework
   *     instance
   */
  public void unbind(CatalogProvider catalogProvider) {
    LOGGER.trace("ENTERING: unbind");

    catalog = frameworkProperties.getCatalogProviders().stream().findFirst().orElse(null);

    LOGGER.trace("EXITING: unbind with catalog = {}", catalog);
  }

  /**
   * Invoked by blueprint when a {@link StorageProvider} is created and bound to this
   * CatalogFramework instance.
   *
   * <p>The local storage provider will be set to the first item in the {@link List} of {@link
   * StorageProvider}s bound to this CatalogFramework.
   *
   * @param storageProvider the {@link CatalogProvider} being bound to this CatalogFramework
   *     instance
   */
  public void bind(StorageProvider storageProvider) {
    List<StorageProvider> storageProviders = frameworkProperties.getStorageProviders();
    LOGGER.debug("storage providers list size = {}", storageProviders.size());

    // The list of storage providers is sorted by OSGi service ranking, hence should
    // always set the local storage provider to the first item in the list.
    this.storage = storageProviders.get(0);
  }

  /**
   * Invoked by blueprint when a {@link StorageProvider} is deleted and unbound from this
   * CatalogFramework instance.
   *
   * <p>The local storage provider will be reset to the new first item in the {@link List} of {@link
   * StorageProvider}s bound to this CatalogFramework. If this list of storage providers is
   * currently empty, then the local storage provider will be set to <code>null</code>.
   *
   * @param storageProvider the {@link StorageProvider} being unbound from this CatalogFramework
   *     instance
   */
  public void unbind(StorageProvider storageProvider) {
    List<StorageProvider> storageProviders = this.frameworkProperties.getStorageProviders();
    if (!storageProviders.isEmpty()) {
      LOGGER.debug("storage providers list size = {}", storageProviders.size());
      LOGGER.debug("Setting storage to first provider in list");

      // The list of storage providers is sorted by OSGi service ranking, hence should
      // always set the local storage provider to the first item in the list.
      this.storage = storageProviders.get(0);
    } else {
      LOGGER.debug("Setting storage = NULL");
      this.storage = null;
    }
  }

  public CatalogProvider getCatalog() {
    return catalog;
  }

  public StorageProvider getStorage() {
    return storage;
  }

  //
  // Delegate methods
  //
  public Set<String> getSourceIds(boolean fanoutEnabled) {
    Set<String> ids = new TreeSet<>();

    ids.add(getId());
    if (!fanoutEnabled) {
      frameworkProperties.getFederatedSources().stream()
          .map(Describable::getId)
          .forEach(
              e -> {
                if (!ids.add(e)) {
                  LOGGER.debug("Multiple FederatedSources found for id: {}", e);
                }
              });
    }
    return ids;
  }

  public SourceInfoResponse getSourceInfo(
      SourceInfoRequest sourceInfoRequest, boolean fanoutEnabled)
      throws SourceUnavailableException {
    SourceInfoResponse response;
    Set<SourceDescriptor> sourceDescriptors;

    if (fanoutEnabled) {
      return getFanoutSourceInfo(sourceInfoRequest);
    }

    boolean addCatalogProviderDescriptor = false;
    try {
      validateSourceInfoRequest(sourceInfoRequest);
      // Obtain the source information based on the sourceIds in the
      // request

      sourceDescriptors = new LinkedHashSet<>();
      Set<String> requestedSourceIds = sourceInfoRequest.getSourceIds();

      // If it is an enterprise request than add all source information for the enterprise
      if (sourceInfoRequest.isEnterprise()) {

        sourceDescriptors =
            getFederatedSourceDescriptors(frameworkProperties.getFederatedSources(), true);
        // If Ids are specified check if they are known sources
      } else if (requestedSourceIds != null) {
        LOGGER.debug("getSourceRequest contains requested source ids");
        Set<FederatedSource> discoveredSources = new HashSet<>();

        for (String requestedSourceId : requestedSourceIds) {
          // Check if the requestedSourceId can be found in the known federatedSources

          final List<FederatedSource> sources =
              frameworkProperties.getFederatedSources().stream()
                  .filter(e -> e.getId().equals(requestedSourceId))
                  .collect(Collectors.toList());

          if (!sources.isEmpty()) {
            String logMsg =
                (sources.size() == 1)
                    ? "Found federated source: {}"
                    : "Multiple FederatedSources found for id: {}";
            LOGGER.debug(logMsg, requestedSourceId);
            discoveredSources.add(sources.get(0));
          } else {
            LOGGER.debug("Unable to find source: {}", requestedSourceId);

            // Check for the local catalog provider, DDF sourceId represents this
            if (requestedSourceId.equals(getId())) {
              LOGGER.debug(
                  "adding CatalogSourceDescriptor since it was in sourceId list as: {}",
                  requestedSourceId);
              addCatalogProviderDescriptor = true;
            }
          }
        }

        sourceDescriptors =
            getFederatedSourceDescriptors(discoveredSources, addCatalogProviderDescriptor);

      } else {
        // only add the local catalogProviderDescriptor
        addCatalogSourceDescriptor(sourceDescriptors);
      }

      response = new SourceInfoResponseImpl(sourceInfoRequest, null, sourceDescriptors);

    } catch (RuntimeException re) {
      LOGGER.debug(GET_SOURCE_EXCEPTION_MSG, re);
      throw new SourceUnavailableException(GET_SOURCE_EXCEPTION_MSG);
    }

    return response;
  }

  //
  // Helper methods - check visibility
  //

  /**
   * Checks that the specified source is valid and available.
   *
   * @param source the {@link Source} to check availability of
   * @return true if the {@link Source} is available, false otherwise
   */
  boolean isSourceAvailable(Source source) {
    if (source == null) {
      LOGGER.debug("source is null, therefore not available");
      return false;
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Checking if source \"{}\" is available...", source.getId());
    }

    // source is considered available unless we have checked and seen otherwise
    return sourceStatusCache
        .getCachedValueForSource(source)
        .map(
            sourceStatus -> {
              final boolean available = sourceStatus == SourceStatus.AVAILABLE;
              if (!available) {
                LOGGER.debug("source \"{}\" is not available", source.getId());
              }
              return available;
            })
        .orElseGet(
            () -> {
              LOGGER.debug("Unknown availability for source id={}", source.getId());
              return false;
            });
  }

  /**
   * Retrieves the {@link SourceDescriptor} info for all {@link FederatedSource}s in the fanout
   * configuration, but the all of the source info, e.g., content types, for all of the available
   * {@link FederatedSource}s is packed into one {@link SourceDescriptor} for the fanout
   * configuration with the fanout's site name in it. This keeps the individual {@link
   * FederatedSource}s' source info hidden from the external client.
   */
  private SourceInfoResponse getFanoutSourceInfo(SourceInfoRequest sourceInfoRequest)
      throws SourceUnavailableException {

    SourceInfoResponse response;
    SourceDescriptorImpl sourceDescriptor;
    try {

      // request
      if (sourceInfoRequest == null) {
        throw new IllegalArgumentException("SourceInfoRequest was null");
      }

      Set<SourceDescriptor> sourceDescriptors = new LinkedHashSet<>();
      Set<String> ids = sourceInfoRequest.getSourceIds();

      // Only return source descriptor information if this sourceId is
      // specified
      if (ids != null) {
        Optional<String> notLocal = ids.stream().filter(s -> !s.equals(getId())).findFirst();
        if (notLocal.isPresent()) {
          SourceUnavailableException sourceUnavailableException =
              new SourceUnavailableException("Unknown source: " + notLocal.get());
          LOGGER.debug(
              "Throwing SourceUnavailableException for unknown source: {}",
              notLocal.get(),
              sourceUnavailableException);
          throw sourceUnavailableException;
        }
      }

      // Fanout will only add one source descriptor with all the contents
      // Using a List here instead of a Set because we should not rely on how Sources are compared
      final List<Source> availableSources =
          frameworkProperties.getFederatedSources().stream()
              .filter(this::isSourceAvailable)
              .collect(Collectors.toList());

      final Set<ContentType> contentTypes =
          availableSources.stream()
              .map(
                  source ->
                      contentTypesCache
                          .getCachedValueForSource(source)
                          .orElseGet(
                              () -> {
                                LOGGER.debug(
                                    "Unknown content types for source id={}", source.getId());
                                return Collections.emptySet();
                              }))
              .flatMap(Collection::stream)
              .collect(Collectors.toSet());

      if (isSourceAvailable(catalog)) {
        availableSources.add(catalog);
        final Optional<Set<ContentType>> catalogContentTypes =
            contentTypesCache.getCachedValueForSource(catalog);
        if (catalogContentTypes.isPresent()) {
          contentTypes.addAll(catalogContentTypes.get());
        } else {
          LOGGER.debug("Unknown content types for the localSource");
        }
      }

      List<Action> actions = getSourceActions(catalog);

      // only reveal this sourceDescriptor, not the federated sources
      sourceDescriptor = new SourceDescriptorImpl(this.getId(), contentTypes, actions);
      if (this.getVersion() != null) {
        sourceDescriptor.setVersion(this.getVersion());
      }
      sourceDescriptor.setAvailable(!availableSources.isEmpty());
      sourceDescriptors.add(sourceDescriptor);

      response = new SourceInfoResponseImpl(sourceInfoRequest, null, sourceDescriptors);

    } catch (RuntimeException re) {
      throw new SourceUnavailableException(GET_SOURCE_EXCEPTION_MSG, re);
    }
    return response;
  }

  private List<Action> getSourceActions(Source localSource) {
    return localSource != null ? sourceActionRegistry.list(localSource) : Collections.emptyList();
  }

  /**
   * Creates a {@link Set} of {@link SourceDescriptor} based on the incoming list of {@link Source}.
   *
   * @param sources {@link Collection} of {@link Source} to obtain descriptor information from
   * @return new {@link Set} of {@link SourceDescriptor}
   */
  private Set<SourceDescriptor> getFederatedSourceDescriptors(
      Collection<FederatedSource> sources, boolean addCatalogProviderDescriptor) {
    SourceDescriptorImpl sourceDescriptor;
    Set<SourceDescriptor> sourceDescriptors = new TreeSet<>(new SourceDescriptorComparator());
    if (sources != null) {
      for (Source source : sources) {
        if (source != null) {
          String sourceId = source.getId();
          LOGGER.debug("adding sourceId: {}", sourceId);

          sourceDescriptor =
              new SourceDescriptorImpl(
                  sourceId,
                  contentTypesCache
                      .getCachedValueForSource(source)
                      .orElseGet(
                          () -> {
                            LOGGER.debug("Unknown content types for source id={}", source.getId());
                            return Collections.emptySet();
                          }),
                  sourceActionRegistry.list(source));
          sourceDescriptor.setVersion(source.getVersion());
          sourceDescriptor.setAvailable(isSourceAvailable(source));

          sourceDescriptors.add(sourceDescriptor);
        }
      }
    }
    if (addCatalogProviderDescriptor) {
      addCatalogSourceDescriptor(sourceDescriptors);
    }
    return sourceDescriptors;
  }

  private void validateSourceInfoRequest(SourceInfoRequest sourceInfoRequest) {
    if (sourceInfoRequest == null) {
      throw new IllegalArgumentException("SourceInfoRequest was null");
    }
  }

  /**
   * Adds the local catalog's {@link SourceDescriptor} to the set of {@link SourceDescriptor}s for
   * this framework.
   *
   * @param descriptors the set of {@link SourceDescriptor}s to add the local catalog's descriptor
   *     to
   */
  private void addCatalogSourceDescriptor(Set<SourceDescriptor> descriptors) {
    /*
     * DDF-1614 if (catalog != null && descriptors != null ) { SourceDescriptorImpl descriptor =
     * new SourceDescriptorImpl(getId(), catalog.getContentTypes());
     * descriptor.setVersion(this.getVersion()); descriptors.add(descriptor); }
     */
    // DDF-1614: Even when no local catalog provider is configured should still
    // return a local site with the framework's ID and version (and no content types
    // since there is no catalog provider).
    // But when a local catalog provider is configured, include its content types in the
    // local site info.
    if (descriptors != null) {
      if (catalog != null) {
        final SourceDescriptorImpl descriptor =
            new SourceDescriptorImpl(
                this.getId(),
                contentTypesCache
                    .getCachedValueForSource(catalog)
                    .orElseGet(
                        () -> {
                          LOGGER.debug("Unknown content types for the localSource");
                          return Collections.emptySet();
                        }),
                sourceActionRegistry.list(catalog));
        if (this.getVersion() != null) {
          descriptor.setVersion(this.getVersion());
        }
        descriptor.setAvailable(isSourceAvailable(catalog));
        descriptors.add(descriptor);
      }
    }
  }
}
