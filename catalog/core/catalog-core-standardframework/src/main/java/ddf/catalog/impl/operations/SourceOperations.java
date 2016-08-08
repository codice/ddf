/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.impl.operations;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.osgi.service.blueprint.container.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class SourceOperations extends DescribableImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceOperations.class);

    private Supplier<CatalogProvider> catalogSupplier;

    //
    // Injected properties
    //
    private FrameworkProperties frameworkProperties;

    public SourceOperations(FrameworkProperties frameworkProperties) {
        this.frameworkProperties = frameworkProperties;
    }

    public void setCatalogSupplier(Supplier<CatalogProvider> catalogSupplier) {
        this.catalogSupplier = catalogSupplier;
    }

    //
    // Delegate methods
    //
    public Set<String> getSourceIds(boolean fanoutEnabled) {
        Set<String> ids = new TreeSet<>();
        ids.add(getId());
        if (!fanoutEnabled) {
            ids.addAll(frameworkProperties.getFederatedSources()
                    .keySet());
        }
        return ids;
    }

    public SourceInfoResponse getSourceInfo(SourceInfoRequest sourceInfoRequest,
            boolean fanoutEnabled)
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
                        getFederatedSourceDescriptors(frameworkProperties.getFederatedSources()
                                .values(), true);
                // If Ids are specified check if they are known sources
            } else if (requestedSourceIds != null) {
                LOGGER.debug("getSourceRequest contains requested source ids");
                Set<FederatedSource> discoveredSources = new HashSet<>();
                boolean containsId = false;

                for (String requestedSourceId : requestedSourceIds) {
                    // Check if the requestedSourceId can be found in the known federatedSources

                    if (frameworkProperties.getFederatedSources()
                            .containsKey(requestedSourceId)) {
                        containsId = true;
                        LOGGER.debug("Found federated source: {}", requestedSourceId);
                        discoveredSources.add(frameworkProperties.getFederatedSources()
                                .get(requestedSourceId));
                    }
                    if (!containsId) {
                        LOGGER.debug("Unable to find source: {}", requestedSourceId);

                        // Check for the local catalog provider, DDF sourceId represents this
                        if (requestedSourceId.equals(getId())) {
                            LOGGER.debug(
                                    "adding CatalogSourceDescriptor since it was in sourceId list as: {}",
                                    requestedSourceId);
                            addCatalogProviderDescriptor = true;
                        }
                    }
                    containsId = false;

                }

                sourceDescriptors = getFederatedSourceDescriptors(discoveredSources,
                        addCatalogProviderDescriptor);

            } else {
                // only add the local catalogProviderdescriptor
                addCatalogSourceDescriptor(sourceDescriptors);
            }

            response = new SourceInfoResponseImpl(sourceInfoRequest, null, sourceDescriptors);

        } catch (RuntimeException re) {
            LOGGER.warn("Exception during runtime while performing getSourceInfo: {}",
                    re.getMessage());
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
            LOGGER.warn("source is null, therefore not available");
            return false;
        }
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Checking if source \"{}\" is available...", source.getId());
            }

            // source is considered available unless we have checked and seen otherwise
            boolean available = true;
            Source cachedSource = frameworkProperties.getSourcePoller()
                    .getCachedSource(source);
            if (cachedSource != null) {
                available = cachedSource.isAvailable();
            }

            if (!available) {
                LOGGER.warn("source \"{}\" is not available", source.getId());
            }
            return available;
        } catch (ServiceUnavailableException e) {
            LOGGER.warn("Caught ServiceUnavaiableException", e);
            return false;
        } catch (Exception e) {
            LOGGER.warn("Caught Exception", e);
            return false;
        }
    }

    /**
     * Retrieves the {@link SourceDescriptor} info for all {@link FederatedSource}s in the fanout
     * configuration, but the all of the source info, e.g., content types, for all of the available
     * {@link FederatedSource}s is packed into one {@link SourceDescriptor} for the
     * fanout configuration with the fanout's site name in it. This keeps the individual
     * {@link FederatedSource}s' source info hidden from the external client.
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
            if (ids != null && !ids.isEmpty()) {
                for (String id : ids) {
                    if (!id.equals(this.getId())) {
                        SourceUnavailableException sourceUnavailableException =
                                new SourceUnavailableException("Unknown source: " + id);
                        LOGGER.warn("Throwing SourceUnavailableException for unknown source: {}",
                                id,
                                sourceUnavailableException);
                        throw sourceUnavailableException;

                    }
                }

            }
            // Fanout will only add one source descriptor with all the contents
            Set<ContentType> contentTypes = frameworkProperties.getFederatedSources()
                    .values()
                    .stream()
                    .filter(source -> source != null && source.isAvailable()
                            && source.getContentTypes() != null)
                    .map(Source::getContentTypes)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());

            // only reveal this sourceDescriptor, not the federated sources
            sourceDescriptor = new SourceDescriptorImpl(this.getId(), contentTypes);
            sourceDescriptor.setVersion(this.getVersion());
            sourceDescriptors.add(sourceDescriptor);

            response = new SourceInfoResponseImpl(sourceInfoRequest, null, sourceDescriptors);

        } catch (RuntimeException re) {
            LOGGER.warn("Exception during runtime while performing create", re);
            throw new SourceUnavailableException(
                    "Exception during runtime while performing getSourceInfo",
                    re);

        }
        return response;

    }

    /**
     * Creates a {@link Set} of {@link SourceDescriptor} based on the incoming list of
     * {@link Source}.
     *
     * @param sources {@link Collection} of {@link Source} to obtain descriptor information from
     * @return new {@link Set} of {@link SourceDescriptor}
     */
    private Set<SourceDescriptor> getFederatedSourceDescriptors(Collection<FederatedSource> sources,
            boolean addCatalogProviderDescriptor) {
        SourceDescriptorImpl sourceDescriptor;
        Set<SourceDescriptor> sourceDescriptors = new HashSet<>();
        if (sources != null) {
            for (Source source : sources) {
                if (source != null) {
                    String sourceId = source.getId();
                    LOGGER.debug("adding sourceId: {}", sourceId);

                    // check the poller for cached information
                    if (frameworkProperties.getSourcePoller() != null &&
                            frameworkProperties.getSourcePoller()
                                    .getCachedSource(source) != null) {
                        source = frameworkProperties.getSourcePoller()
                                .getCachedSource(source);
                    }

                    sourceDescriptor = new SourceDescriptorImpl(sourceId, source.getContentTypes());
                    sourceDescriptor.setVersion(source.getVersion());
                    sourceDescriptor.setAvailable(source.isAvailable());

                    sourceDescriptors.add(sourceDescriptor);
                }
            }
        }
        if (addCatalogProviderDescriptor) {
            addCatalogSourceDescriptor(sourceDescriptors);
        }

        Set<SourceDescriptor> orderedDescriptors = new TreeSet<>(new SourceDescriptorComparator());

        orderedDescriptors.addAll(sourceDescriptors);
        return orderedDescriptors;

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
     * @param descriptors the set of {@link SourceDescriptor}s to add the local catalog's descriptor to
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
            Set<ContentType> contentTypes = new HashSet<>();
            if (catalogSupplier.get() != null) {
                contentTypes = catalogSupplier.get()
                        .getContentTypes();
            }
            SourceDescriptorImpl descriptor = new SourceDescriptorImpl(this.getId(), contentTypes);
            descriptor.setVersion(this.getVersion());
            descriptors.add(descriptor);
        }
    }
}
