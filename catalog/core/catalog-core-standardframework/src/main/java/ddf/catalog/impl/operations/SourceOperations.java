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
import org.codice.ddf.platform.services.common.Describable;
import org.osgi.service.blueprint.container.ServiceUnavailableException;
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

  //
  // Injected properties
  //
  private FrameworkProperties frameworkProperties;

  //
  // Bound objects
  //
  private CatalogProvider catalog;

  private StorageProvider storage;

  private ActionRegistry sourceActionRegistry;

  public SourceOperations(
      FrameworkProperties frameworkProperties, ActionRegistry sourceActionRegistry) {
    Validate.notNull(frameworkProperties, "frameworkProperties must be non-null");
    Validate.notNull(sourceActionRegistry, "sourceActionRegistry must be non-null");

    this.frameworkProperties = frameworkProperties;
    this.sourceActionRegistry = sourceActionRegistry;
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
      ids.addAll(
          frameworkProperties
              .getFederatedSources()
              .stream()
              .map(Describable::getId)
              .collect(Collectors.toSet()));
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

          final Optional<FederatedSource> source =
              frameworkProperties
                  .getFederatedSources()
                  .stream()
                  .filter(e -> e.getId().equals(requestedSourceId))
                  .findFirst();

          source.ifPresent(
              e -> {
                LOGGER.debug("Found federated source: {}", requestedSourceId);
                discoveredSources.add(e);
              });

          if (!source.isPresent()) {
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
        // only add the local catalogProviderdescriptor
        addCatalogSourceDescriptor(sourceDescriptors);
      }

      response = new SourceInfoResponseImpl(sourceInfoRequest, null, sourceDescriptors);

    } catch (RuntimeException re) {
      LOGGER.debug("Exception during runtime while performing getSourceInfo", re);
      throw new SourceUnavailableException(
          "Exception during runtime while performing getSourceInfo");
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
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Checking if source \"{}\" is available...", source.getId());
      }

      // source is considered available unless we have checked and seen otherwise
      boolean available = true;
      Source cachedSource = frameworkProperties.getSourcePoller().getCachedSource(source);
      if (cachedSource != null) {
        available = cachedSource.isAvailable();
      }

      if (!available) {
        LOGGER.info("source \"{}\" is not available", source.getId());
      }
      return available;
    } catch (ServiceUnavailableException e) {
      LOGGER.info("Caught ServiceUnavaiableException", e);
      return false;
    } catch (Exception e) {
      LOGGER.info("Caught Exception", e);
      return false;
    }
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
      Set<Source> availableSources =
          frameworkProperties
              .getFederatedSources()
              .stream()
              .map(source -> frameworkProperties.getSourcePoller().getCachedSource(source))
              .filter(source -> source != null && source.isAvailable())
              .collect(Collectors.toSet());

      Set<ContentType> contentTypes =
          availableSources
              .stream()
              .map(source -> frameworkProperties.getSourcePoller().getCachedSource(source))
              .filter(source -> source.getContentTypes() != null)
              .map(Source::getContentTypes)
              .flatMap(Collection::stream)
              .collect(Collectors.toSet());

      Source localSource = null;

      if (catalog != null) {
        localSource = frameworkProperties.getSourcePoller().getCachedSource(catalog);
        if (localSource != null && localSource.isAvailable()) {
          availableSources.add(localSource);
          contentTypes.addAll(localSource.getContentTypes());
        }
      }

      List<Action> actions = getSourceActions(localSource);

      // only reveal this sourceDescriptor, not the federated sources
      sourceDescriptor = new SourceDescriptorImpl(this.getId(), contentTypes, actions);
      if (this.getVersion() != null) {
        sourceDescriptor.setVersion(this.getVersion());
      }
      sourceDescriptor.setAvailable(availableSources.size() > 0);
      sourceDescriptors.add(sourceDescriptor);

      response = new SourceInfoResponseImpl(sourceInfoRequest, null, sourceDescriptors);

    } catch (RuntimeException re) {
      throw new SourceUnavailableException(
          "Exception during runtime while performing getSourceInfo", re);
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

          Source cachedSource = null;
          // check the poller for cached information
          if (frameworkProperties.getSourcePoller() != null
              && frameworkProperties.getSourcePoller().getCachedSource(source) != null) {
            cachedSource = frameworkProperties.getSourcePoller().getCachedSource(source);
          }

          sourceDescriptor =
              new SourceDescriptorImpl(
                  sourceId, source.getContentTypes(), sourceActionRegistry.list(source));
          sourceDescriptor.setVersion(source.getVersion());
          sourceDescriptor.setAvailable((cachedSource != null) && cachedSource.isAvailable());

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
      Source cachedSource = null;
      if (catalog != null) {
        // check the poller for cached information
        if (frameworkProperties.getSourcePoller() != null
            && frameworkProperties.getSourcePoller().getCachedSource(catalog) != null) {
          cachedSource = frameworkProperties.getSourcePoller().getCachedSource(catalog);
        }
        if (cachedSource != null) {
          SourceDescriptorImpl descriptor =
              new SourceDescriptorImpl(
                  this.getId(),
                  cachedSource.getContentTypes(),
                  sourceActionRegistry.list(cachedSource));
          if (this.getVersion() != null) {
            descriptor.setVersion(this.getVersion());
          }
          descriptor.setAvailable(cachedSource.isAvailable());
          descriptors.add(descriptor);
        }
      }
    }
  }
}
