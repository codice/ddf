/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.impl;

import static ddf.catalog.Constants.CONTENT_PATHS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.tika.detect.DefaultProbDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.platform.util.InputValidation;
import org.opengis.filter.Filter;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

import ddf.catalog.CatalogFramework;
import ddf.catalog.Constants;
import ddf.catalog.content.StorageException;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.operation.DeleteStorageRequest;
import ddf.catalog.content.operation.StorageRequest;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.content.operation.impl.DeleteStorageRequestImpl;
import ddf.catalog.content.operation.impl.UpdateStorageRequestImpl;
import ddf.catalog.content.plugin.PostCreateStoragePlugin;
import ddf.catalog.content.plugin.PostUpdateStoragePlugin;
import ddf.catalog.content.plugin.PreCreateStoragePlugin;
import ddf.catalog.content.plugin.PreUpdateStoragePlugin;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.DefaultAttributeValueRegistry;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.impl.LiteralImpl;
import ddf.catalog.filter.impl.PropertyIsEqualToLiteral;
import ddf.catalog.filter.impl.PropertyNameImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.Request;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.OperationTransactionImpl;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.operation.impl.SourceInfoResponseImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.PreResourcePlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.resource.DataUsageLimitExceededException;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.resource.download.DownloadException;
import ddf.catalog.resourceretriever.LocalResourceRetriever;
import ddf.catalog.resourceretriever.RemoteResourceRetriever;
import ddf.catalog.resourceretriever.ResourceRetriever;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.CatalogStore;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.InternalIngestException;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.impl.SourceDescriptorImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transform.QueryResponseTransformer;
import ddf.catalog.util.impl.DescribableImpl;
import ddf.catalog.util.impl.Masker;
import ddf.catalog.util.impl.Requests;
import ddf.catalog.util.impl.SourceDescriptorComparator;
import ddf.mime.MimeTypeResolutionException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.SubjectUtils;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;

/**
 * CatalogFrameworkImpl is the core class of DDF. It is used for query, create, update, delete, and
 * resource retrieval operations.
 */
@SuppressWarnings("deprecation")
public class CatalogFrameworkImpl extends DescribableImpl implements CatalogFramework {

    protected static final String FAILED_BY_GET_RESOURCE_PLUGIN =
            "Error during Pre/PostResourcePlugin.";

    static final Logger INGEST_LOGGER = LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

    private static final String PRE_INGEST_ERROR =
            "Error during pre-ingest service invocation:\n\n";

    private static final String DEFAULT_RESOURCE_NOT_FOUND_MESSAGE = "Unknown resource request";

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogFrameworkImpl.class);

    private static final String FANOUT_MESSAGE =
            "Fanout proxy does not support create, update, and delete operations";

    protected boolean notificationEnabled = true;

    protected boolean activityEnabled = true;

    // The local catalog provider, which is set to the first item in the {@link List} of
    // {@link CatalogProvider}s.
    // Keep this private to make sure subclasses don't use it.
    private CatalogProvider catalog;

    private Masker masker;

    private boolean fanoutEnabled = false;

    private StorageProvider storage;

    private FrameworkProperties frameworkProperties;

    /**
     * Instantiates a new CatalogFrameworkImpl
     *
     * @param frameworkProperties - collection of properties to be set on the CatalogFramework instance
     */
    public CatalogFrameworkImpl(FrameworkProperties frameworkProperties) {
        this.frameworkProperties = frameworkProperties;

        setId(SystemInfo.getSiteName());
        setVersion(SystemInfo.getVersion());
        setOrganization(SystemInfo.getOrganization());
        registerBasicMetacard();
    }

    private void registerBasicMetacard() {
        Bundle bundle = FrameworkUtil.getBundle(CatalogFrameworkImpl.class);
        if (bundle != null && bundle.getBundleContext() != null) {
            Dictionary<String, Object> properties = new Hashtable<>();
            properties.put("name", BasicTypes.BASIC_METACARD.getName());
            bundle.getBundleContext()
                    .registerService(MetacardType.class, BasicTypes.BASIC_METACARD, properties);
        }
    }

    public void setFanoutEnabled(boolean fanoutEnabled) {
        this.fanoutEnabled = fanoutEnabled;
    }

    public void setNotificationEnabled(boolean notificationEnabled) {
        LOGGER.debug("Setting notificationEnabled = {}", notificationEnabled);
        this.notificationEnabled = notificationEnabled;
        frameworkProperties.getDownloadsStatusEventPublisher()
                .setNotificationEnabled(notificationEnabled);
    }

    public void setActivityEnabled(boolean activityEnabled) {
        LOGGER.debug("Setting activityEnabled = {}", activityEnabled);
        this.activityEnabled = activityEnabled;
        frameworkProperties.getDownloadsStatusEventPublisher()
                .setActivityEnabled(activityEnabled);
    }


    /**
     * Invoked by blueprint when a {@link CatalogProvider} is created and bound to this
     * CatalogFramework instance.
     * <p>
     * The local catalog provider will be set to the first item in the {@link List} of
     * {@link CatalogProvider}s bound to this CatalogFramework.
     *
     * @param catalogProvider the {@link CatalogProvider} being bound to this CatalogFramework instance
     */
    public void bind(CatalogProvider catalogProvider) {
        LOGGER.trace("ENTERING: bind with CatalogProvider arg");

        List<CatalogProvider> catalogProviders = frameworkProperties.getCatalogProviders();
        LOGGER.info("catalog providers list size = {}", catalogProviders.size());

        // The list of catalog providers is sorted by OSGi service ranking, hence should
        // always set the local catalog provider to the first item in the list.
        this.catalog = catalogProviders.get(0);

        LOGGER.trace("EXITING: bind with CatalogProvider arg");
    }

    /**
     * Invoked by blueprint when a {@link CatalogProvider} is deleted and unbound from this
     * CatalogFramework instance.
     * <p>
     * The local catalog provider will be reset to the new first item in the {@link List} of
     * {@link CatalogProvider}s bound to this CatalogFramework. If this list of catalog providers is
     * currently empty, then the local catalog provider will be set to <code>null</code>.
     *
     * @param catalogProvider the {@link CatalogProvider} being unbound from this CatalogFramework instance
     */
    public void unbind(CatalogProvider catalogProvider) {
        LOGGER.trace("ENTERING: unbind with CatalogProvider arg");

        List<CatalogProvider> catalogProviders = this.frameworkProperties.getCatalogProviders();
        if (catalogProviders.size() > 0) {
            LOGGER.info("catalog providers list size = {}", catalogProviders.size());
            LOGGER.info("Setting catalog to first provider in list");

            // The list of catalog providers is sorted by OSGi service ranking, hence should
            // always set the local catalog provider to the first item in the list.
            this.catalog = catalogProviders.get(0);
        } else {
            LOGGER.info("Setting catalog = NULL");
            this.catalog = null;
        }

        LOGGER.trace("EXITING: unbind with CatalogProvider arg");
    }

    /**
     * Invoked by blueprint when a {@link StorageProvider} is created and bound to this
     * CatalogFramework instance.
     * <p>
     * The local storage provider will be set to the first item in the {@link List} of
     * {@link StorageProvider}s bound to this CatalogFramework.
     *
     * @param storageProvider the {@link CatalogProvider} being bound to this CatalogFramework instance
     */
    public void bind(StorageProvider storageProvider) {
        List<StorageProvider> storageProviders = frameworkProperties.getStorageProviders();
        LOGGER.info("storage providers list size = {}", storageProviders.size());

        // The list of storage providers is sorted by OSGi service ranking, hence should
        // always set the local storage provider to the first item in the list.
        this.storage = storageProviders.get(0);
    }

    /**
     * Invoked by blueprint when a {@link StorageProvider} is deleted and unbound from this
     * CatalogFramework instance.
     * <p>
     * The local storage provider will be reset to the new first item in the {@link List} of
     * {@link StorageProvider}s bound to this CatalogFramework. If this list of storage providers is
     * currently empty, then the local storage provider will be set to <code>null</code>.
     *
     * @param storageProvider the {@link StorageProvider} being unbound from this CatalogFramework instance
     */
    public void unbind(StorageProvider storageProvider) {
        List<StorageProvider> storageProviders = this.frameworkProperties.getStorageProviders();
        if (storageProviders.size() > 0) {
            LOGGER.info("storage providers list size = {}", storageProviders.size());
            LOGGER.info("Setting storage to first provider in list");

            // The list of storage providers is sorted by OSGi service ranking, hence should
            // always set the local storage provider to the first item in the list.
            this.storage = storageProviders.get(0);
        } else {
            LOGGER.info("Setting storage = NULL");
            this.storage = null;
        }
    }

    /**
     * Sets the {@link Masker}
     *
     * @param masker the {@link Masker} this framework will use
     */
    public void setMasker(Masker masker) {
        synchronized (this) {

            this.masker = masker;
            if (this.getId() != null) {
                masker.setId(getId());
            }
        }

    }

    /**
     * Sets the source id to identify this framework (DDF). This is also referred to as the site
     * name.
     *
     * @param sourceId the sourceId to set
     */
    @Override
    public void setId(String sourceId) {
        LOGGER.debug("Setting id = {}", sourceId);
        synchronized (this) {
            super.setId(sourceId);
            if (masker != null) {
                masker.setId(sourceId);
            }
        }
    }

    @Override
    public SourceInfoResponse getSourceInfo(SourceInfoRequest sourceInfoRequest)
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

    /**
     * Retrieves the {@link SourceDescriptor} info for all {@link FederatedSource}s in the fanout
     * configuration, but the all of the source info, e.g., content types, for all of the available
     * {@link FederatedSource}s is packed into one {@link SourceDescriptor} for the
     * fanout configuration with the fanout's site name in it. This keeps the individual
     * {@link FederatedSource}s' source info hidden from the external client.
     */
    public SourceInfoResponse getFanoutSourceInfo(SourceInfoRequest sourceInfoRequest)
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
    protected void addCatalogSourceDescriptor(Set<SourceDescriptor> descriptors) {
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
            if (catalog != null) {
                contentTypes = catalog.getContentTypes();
            }
            SourceDescriptorImpl descriptor = new SourceDescriptorImpl(this.getId(), contentTypes);
            descriptor.setVersion(this.getVersion());
            descriptors.add(descriptor);
        }
    }

    private void buildPolicyMap(HashMap<String, Set<String>> policyMap,
            Set<Entry<String, Set<String>>> policy) {
        if (policy != null) {
            for (Entry<String, Set<String>> entry : policy) {
                if (policyMap.containsKey(entry.getKey())) {
                    policyMap.get(entry.getKey())
                            .addAll(entry.getValue());
                } else {
                    policyMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
                }
            }
        }
    }

    private Metacard generateMetacard(String mimeTypeRaw, String id, String fileName, long size,
            Subject subject, Path tmpContentPath)
            throws MetacardCreationException, MimeTypeParseException {

        Metacard generatedMetacard = null;
        InputTransformer transformer = null;
        StringBuilder causeMessage = new StringBuilder("Could not create metacard with mimeType ");
        try {
            MimeType mimeType = new MimeType(mimeTypeRaw);

            List<InputTransformer> listOfCandidates =
                    frameworkProperties.getMimeTypeToTransformerMapper()
                            .findMatches(InputTransformer.class, mimeType);

            LOGGER.debug("List of matches for mimeType [{}]: {}", mimeType, listOfCandidates);

            for (InputTransformer candidate : listOfCandidates) {
                transformer = candidate;

                try (InputStream transformerStream = com.google.common.io.Files.asByteSource(
                        tmpContentPath.toFile())
                        .openStream()) {
                    generatedMetacard = transformer.transform(transformerStream);
                }
                if (generatedMetacard != null) {
                    break;
                }
            }
        } catch (CatalogTransformerException | IOException e) {
            causeMessage.append(mimeTypeRaw)
                    .append(". Reason: ")
                    .append(System.lineSeparator())
                    .append(e.getMessage());

            // The caught exception more than likely does not have the root cause message
            // that is needed to inform the caller as to why things have failed.  Therefore
            // we need to iterate through the chain of cause exceptions and gather up
            // all of their message details.
            Throwable cause = e.getCause();
            while (cause != null && cause != cause.getCause()) {
                causeMessage.append(System.lineSeparator())
                        .append(cause.getMessage());
                cause = cause.getCause();
            }
            LOGGER.debug("Transformer [{}] could not create metacard.", transformer, e);
        }

        if (generatedMetacard == null) {
            throw new MetacardCreationException(causeMessage.toString());
        }

        if (id != null) {
            generatedMetacard.setAttribute(new AttributeImpl(Metacard.ID, id));
        } else {
            generatedMetacard.setAttribute(new AttributeImpl(Metacard.ID,
                    UUID.randomUUID()
                            .toString()
                            .replaceAll("-", "")));
        }

        if (StringUtils.isBlank(generatedMetacard.getTitle())) {
            generatedMetacard.setAttribute(new AttributeImpl(Metacard.TITLE, fileName));
        }

        String name = SubjectUtils.getName(subject);

        generatedMetacard.setAttribute(new AttributeImpl(Metacard.POINT_OF_CONTACT,
                name == null ? "" : name));

        return generatedMetacard;

    }

    private String updateFileExtension(String mimeTypeRaw, String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        if (ContentItem.DEFAULT_FILE_NAME.equals(fileName) && !ContentItem.DEFAULT_MIME_TYPE.equals(
                mimeTypeRaw) || StringUtils.isEmpty(extension)) {
            try {
                extension = frameworkProperties.getMimeTypeMapper()
                        .getFileExtensionForMimeType(mimeTypeRaw);
                if (StringUtils.isNotEmpty(extension)) {
                    fileName = FilenameUtils.removeExtension(fileName);
                    fileName += extension;
                }
            } catch (MimeTypeResolutionException e) {
                LOGGER.debug("Unable to guess file extension for mime type.", e);
            }
        }
        return fileName;
    }

    private String guessMimeType(String mimeTypeRaw, String fileName, Path tmpContentPath)
            throws IOException {
        if (ContentItem.DEFAULT_MIME_TYPE.equals(mimeTypeRaw)) {
            try (InputStream inputStreamMessageCopy = com.google.common.io.Files.asByteSource(
                    tmpContentPath.toFile())
                    .openStream()) {
                String mimeTypeGuess = frameworkProperties.getMimeTypeMapper()
                        .guessMimeType(inputStreamMessageCopy,
                                FilenameUtils.getExtension(fileName));
                if (StringUtils.isNotEmpty(mimeTypeGuess)) {
                    mimeTypeRaw = mimeTypeGuess;
                }
            } catch (MimeTypeResolutionException e) {
                LOGGER.debug("Unable to guess mime type for file.", e);
            }
            if (ContentItem.DEFAULT_MIME_TYPE.equals(mimeTypeRaw)) {
                Detector detector = new DefaultProbDetector();
                try (InputStream inputStreamMessageCopy = com.google.common.io.Files.asByteSource(
                        tmpContentPath.toFile())
                        .openStream()) {
                    MediaType mediaType = detector.detect(inputStreamMessageCopy, new Metadata());
                    mimeTypeRaw = mediaType.toString();
                } catch (IOException e) {
                    LOGGER.debug("Unable to guess mime type for file.", e);
                }
            }
            if (mimeTypeRaw.equals("text/plain")) {
                try (InputStream inputStreamMessageCopy = com.google.common.io.Files.asByteSource(
                        tmpContentPath.toFile())
                        .openStream();
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                                inputStreamMessageCopy,
                                Charset.forName("UTF-8")))) {
                    String line = "";
                    while (bufferedReader.ready()) {
                        line = bufferedReader.readLine();
                        line = line.trim();
                        if (!StringUtils.isEmpty(line)) {
                            break;
                        }
                    }

                    if (line.startsWith("<")) {
                        mimeTypeRaw = "text/xml";
                    } else if (line.startsWith("{") || line.startsWith("[")) {
                        mimeTypeRaw = "application/json";
                    }
                } catch (IOException e) {
                    LOGGER.debug("Unable to guess mime type for file.", e);
                }
            }
        }
        return mimeTypeRaw;
    }

    private void generateMetacardAndContentItems(StorageRequest storageRequest,
            List<ContentItem> incomingContentItems, Map<String, Metacard> metacardMap,
            List<ContentItem> contentItems, Map<String, Path> tmpContentPaths)
            throws IngestException {
        for (ContentItem contentItem : incomingContentItems) {
            try {
                Path tmpPath = null;
                long size;
                try {
                    String sanitizedFilename = InputValidation.sanitizeFilename(
                            contentItem.getFilename());
                    if (contentItem.getInputStream() != null) {
                        tmpPath =
                                Files.createTempFile(FilenameUtils.getBaseName(sanitizedFilename),
                                        FilenameUtils.getExtension(sanitizedFilename));
                        Files.copy(contentItem.getInputStream(),
                                tmpPath,
                                StandardCopyOption.REPLACE_EXISTING);
                        size = Files.size(tmpPath);
                        tmpContentPaths.put(contentItem.getId(), tmpPath);
                    } else {
                        throw new IngestException(
                                "Could not copy bytes of content message.  Message was NULL.");
                    }
                } catch (IOException e) {
                    if (tmpPath != null) {
                        FileUtils.deleteQuietly(tmpPath.toFile());
                    }
                    throw new IngestException("Could not copy bytes of content message.", e);
                } finally {
                    IOUtils.closeQuietly(contentItem.getInputStream());
                }
                String mimeTypeRaw = contentItem.getMimeTypeRawData();
                mimeTypeRaw = guessMimeType(mimeTypeRaw, contentItem.getFilename(), tmpPath);

                if (!InputValidation.checkForClientSideVulnerableMimeType(mimeTypeRaw)) {
                    throw new IngestException("Unsupported mime type.");
                }

                String fileName = updateFileExtension(mimeTypeRaw, contentItem.getFilename());
                Metacard metacard = generateMetacard(mimeTypeRaw,
                        contentItem.getId(),
                        fileName,
                        size,
                        (Subject) storageRequest.getProperties()
                                .get(SecurityConstants.SECURITY_SUBJECT),
                        tmpPath);
                metacardMap.put(metacard.getId(), metacard);

                ContentItem generatedContentItem = new ContentItemImpl(metacard.getId(),
                        com.google.common.io.Files.asByteSource(tmpPath.toFile()),
                        mimeTypeRaw,
                        fileName,
                        size,
                        metacard);
                contentItems.add(generatedContentItem);
            } catch (Exception e) {
                tmpContentPaths.values()
                        .stream()
                        .forEach(path -> FileUtils.deleteQuietly(path.toFile()));
                tmpContentPaths.clear();
                throw new IngestException("Could not create metacard.", e);
            }
        }
    }

    @Override
    public CreateResponse create(CreateStorageRequest streamCreateRequest)
            throws IngestException, SourceUnavailableException {
        validateCreateStorageRequest(streamCreateRequest);

        setFlagsOnRequest(streamCreateRequest);

        if (fanoutEnabled) {
            throw new IngestException(FANOUT_MESSAGE);
        }

        if (Requests.isLocal(streamCreateRequest) && (!sourceIsAvailable(catalog)
                || !storageIsAvailable(storage))) {
            SourceUnavailableException sourceUnavailableException = new SourceUnavailableException(
                    "Local provider is not available, cannot perform create operation.");
            if (INGEST_LOGGER.isWarnEnabled()) {
                INGEST_LOGGER.warn("Error on create operation, local provider not available.",
                        sourceUnavailableException);
            }
            throw sourceUnavailableException;
        }

        Map<String, Metacard> metacardMap = new HashMap<>();
        List<ContentItem> contentItems = new ArrayList<>(streamCreateRequest.getContentItems()
                .size());
        HashMap<String, Path> tmpContentPaths = new HashMap<>(streamCreateRequest.getContentItems()
                .size());
        generateMetacardAndContentItems(streamCreateRequest,
                streamCreateRequest.getContentItems(),
                metacardMap,
                contentItems,
                tmpContentPaths);
        streamCreateRequest.getProperties()
                .put(CONTENT_PATHS, tmpContentPaths);

        // Get attributeOverrides, apply them and then remove them from the streamCreateRequest so they are not exposed to plugins
        Map<String, String> attributeOverrideHeaders =
                (HashMap<String, String>) streamCreateRequest.getProperties()
                        .get(Constants.ATTRIBUTE_OVERRIDES_KEY);
        applyAttributeOverridesToMetacardMap(attributeOverrideHeaders, metacardMap);
        streamCreateRequest.getProperties()
                .remove(Constants.ATTRIBUTE_OVERRIDES_KEY);

        CreateStorageRequest createStorageRequest = null;
        CreateResponse createResponse;
        try {
            if (contentItems.size() > 0) {
                createStorageRequest = new CreateStorageRequestImpl(contentItems,
                        streamCreateRequest.getId(),
                        streamCreateRequest.getProperties());
                for (final PreCreateStoragePlugin plugin : frameworkProperties.getPreCreateStoragePlugins()) {
                    try {
                        createStorageRequest = plugin.process(createStorageRequest);
                    } catch (PluginExecutionException e) {
                        LOGGER.warn(
                                "Plugin processing failed. This is allowable. Skipping to next plugin.",
                                e);
                    }
                }

                CreateStorageResponse createStorageResponse;
                try {
                    createStorageResponse = storage.create(createStorageRequest);
                    createStorageResponse.getProperties()
                            .put(CONTENT_PATHS, tmpContentPaths);
                } catch (StorageException e) {
                    throw new IngestException("Could not store content items.", e);
                }

                for (final PostCreateStoragePlugin plugin : frameworkProperties.getPostCreateStoragePlugins()) {
                    try {
                        createStorageResponse = plugin.process(createStorageResponse);
                    } catch (PluginExecutionException e) {
                        LOGGER.warn(
                                "Plugin processing failed. This is allowable. Skipping to next plugin.",
                                e);
                    }
                }

                for (ContentItem contentItem : createStorageResponse.getCreatedContentItems()) {
                    if (contentItem.getMetacard()
                            .getResourceURI() == null) {
                        contentItem.getMetacard()
                                .setAttribute(new AttributeImpl(Metacard.RESOURCE_URI,
                                        contentItem.getUri()));
                        contentItem.getMetacard()
                                .setAttribute(new AttributeImpl(Metacard.RESOURCE_SIZE,
                                        String.valueOf(contentItem.getSize())));
                    }
                    metacardMap.put(contentItem.getId(), contentItem.getMetacard());
                }
            }

            CreateRequest createRequest =
                    new CreateRequestImpl(new ArrayList<>(metacardMap.values()),
                            streamCreateRequest.getProperties());

            createResponse = create(createRequest);
        } catch (Exception e) {
            if (createStorageRequest != null) {
                try {
                    storage.rollback(createStorageRequest);
                } catch (StorageException e1) {
                    LOGGER.error("Unable to remove temporary content for id: "
                            + createStorageRequest.getId(), e1);
                }
            }
            throw new IngestException(
                    "Unable to store products for request: " + streamCreateRequest.getId(), e);
        } finally {
            if (createStorageRequest != null) {
                try {
                    storage.commit(createStorageRequest);
                } catch (StorageException e) {
                    LOGGER.error("Unable to commit content changes for id: "
                            + createStorageRequest.getId(), e);
                    try {
                        storage.rollback(createStorageRequest);
                    } catch (StorageException e1) {
                        LOGGER.error("Unable to remove temporary content for id: "
                                + createStorageRequest.getId(), e1);
                    }
                }
            }
            tmpContentPaths.values()
                    .stream()
                    .forEach(path -> FileUtils.deleteQuietly(path.toFile()));
            tmpContentPaths.clear();
        }

        return createResponse;
    }

    @Override
    public CreateResponse create(CreateRequest createRequest)
            throws IngestException, SourceUnavailableException {

        boolean catalogStoreRequest = isCatalogStoreRequest(createRequest);
        setFlagsOnRequest(createRequest);

        if (fanoutEnabled) {
            throw new IngestException(FANOUT_MESSAGE);
        }

        validateCreateRequest(createRequest);

        if (Requests.isLocal(createRequest) && !sourceIsAvailable(catalog)) {
            SourceUnavailableException sourceUnavailableException = new SourceUnavailableException(
                    "Local provider is not available, cannot perform create operation.");
            if (INGEST_LOGGER.isWarnEnabled()) {
                INGEST_LOGGER.warn(
                        "Error on create operation, local provider not available. {} metacards failed to ingest. {}",
                        createRequest.getMetacards()
                                .size(),
                        buildIngestLog(createRequest),
                        sourceUnavailableException);
            }
            throw sourceUnavailableException;
        }

        setDefaultValues(createRequest);

        CreateResponse createResponse = null;

        Exception ingestError = null;
        try {
            Map<String, Serializable> unmodifiablePropertiesMap = Collections.unmodifiableMap(
                    createRequest.getProperties());
            HashMap<String, Set<String>> requestPolicyMap = new HashMap<>();
            for (Metacard metacard : createRequest.getMetacards()) {
                HashMap<String, Set<String>> itemPolicyMap = new HashMap<>();
                for (PolicyPlugin plugin : frameworkProperties.getPolicyPlugins()) {
                    PolicyResponse policyResponse = plugin.processPreCreate(metacard,
                            unmodifiablePropertiesMap);
                    buildPolicyMap(itemPolicyMap,
                            policyResponse.itemPolicy()
                                    .entrySet());
                    buildPolicyMap(requestPolicyMap,
                            policyResponse.operationPolicy()
                                    .entrySet());
                }
                metacard.setAttribute(new AttributeImpl(Metacard.SECURITY, itemPolicyMap));
            }
            createRequest.getProperties()
                    .put(PolicyPlugin.OPERATION_SECURITY, requestPolicyMap);

            for (AccessPlugin plugin : frameworkProperties.getAccessPlugins()) {
                createRequest = plugin.processPreCreate(createRequest);
            }

            createRequest.getProperties()
                    .put(Constants.OPERATION_TRANSACTION_KEY,
                            new OperationTransactionImpl(OperationTransaction.OperationType.CREATE,
                                    new ArrayList<>()));

            for (PreIngestPlugin plugin : frameworkProperties.getPreIngest()) {
                try {
                    createRequest = plugin.process(createRequest);
                } catch (PluginExecutionException e) {
                    LOGGER.info(
                            "Plugin processing failed. This is allowable. Skipping to next plugin.",
                            e);
                }
            }
            validateCreateRequest(createRequest);

            // Call the create on the catalog
            LOGGER.debug("Calling catalog.create() with {} entries.",
                    createRequest.getMetacards()
                            .size());

            if (Requests.isLocal(createRequest)) {
                createResponse = catalog.create(createRequest);
            }

            if (catalogStoreRequest) {
                CreateResponse remoteCreateResponse = doRemoteCreate(createRequest);
                if (createResponse == null) {
                    createResponse = remoteCreateResponse;
                } else {
                    createResponse.getProperties()
                            .putAll(remoteCreateResponse.getProperties());
                    createResponse.getProcessingErrors()
                            .addAll(remoteCreateResponse.getProcessingErrors());
                }
            }

        } catch (IngestException iee) {
            INGEST_LOGGER.warn("Ingest error", iee);
            ingestError = iee;
            throw iee;
        } catch (StopProcessingException see) {
            LOGGER.warn(PRE_INGEST_ERROR, see);
            ingestError = see;
            throw new IngestException(PRE_INGEST_ERROR + see.getMessage());
        } catch (RuntimeException re) {
            LOGGER.warn("Exception during runtime while performing create", re);
            ingestError = re;
            throw new InternalIngestException("Exception during runtime while performing create");
        } finally {
            if (ingestError != null && INGEST_LOGGER.isWarnEnabled()) {
                INGEST_LOGGER.warn("Error on create operation. {} metacards failed to ingest. {}",
                        createRequest.getMetacards()
                                .size(),
                        buildIngestLog(createRequest),
                        ingestError);
            }
        }

        try {
            createResponse = validateFixCreateResponse(createResponse, createRequest);
            for (final PostIngestPlugin plugin : frameworkProperties.getPostIngest()) {
                try {
                    createResponse = plugin.process(createResponse);
                } catch (PluginExecutionException e) {
                    LOGGER.info(
                            "Plugin processing failed. This is allowable. Skipping to next plugin.",
                            e);
                }
            }
        } catch (RuntimeException re) {
            LOGGER.warn(
                    "Exception during runtime while performing doing post create operations (plugins and pubsub)",
                    re);

        }

        // if debug is enabled then catalog might take a significant performance hit w/r/t string
        // building
        if (INGEST_LOGGER.isDebugEnabled()) {
            INGEST_LOGGER.debug("{} metacards were successfully ingested. {}",
                    createRequest.getMetacards()
                            .size(),
                    buildIngestLog(createRequest));
        }
        return createResponse;
    }

    private void applyAttributeOverridesToMetacardMap(Map<String, String> attributeOverrideMap,
            Map<String, Metacard> metacardMap) {

        if (MapUtils.isEmpty(attributeOverrideMap) || MapUtils.isEmpty(metacardMap)) {
            return;
        }

        metacardMap.values()
                .forEach(metacard -> attributeOverrideMap.keySet()
                        .stream()
                        .map(attributeName -> metacard.getMetacardType()
                                .getAttributeDescriptor(attributeName))
                        .filter(Objects::nonNull)
                        .map(attributeDescriptor -> {
                            String overrideValue =
                                    attributeOverrideMap.get(attributeDescriptor.getName());
                            try {
                                Serializable newValue;
                                switch (attributeDescriptor.getType()
                                        .getAttributeFormat()) {
                                case INTEGER:
                                    newValue = Integer.parseInt(overrideValue);
                                    break;
                                case FLOAT:
                                    newValue = Float.parseFloat(overrideValue);
                                    break;
                                case DOUBLE:
                                    newValue = Double.parseDouble(overrideValue);
                                    break;
                                case SHORT:
                                    newValue = Short.parseShort(overrideValue);
                                    break;
                                case LONG:
                                    newValue = Long.parseLong(overrideValue);
                                    break;
                                case DATE:
                                    Calendar calendar = DatatypeConverter.parseDateTime(
                                            overrideValue);
                                    newValue = calendar.getTime();
                                    break;
                                case BOOLEAN:
                                    newValue = Boolean.parseBoolean(overrideValue);
                                    break;
                                case BINARY:
                                    newValue = overrideValue.getBytes();
                                    break;
                                case OBJECT:
                                case STRING:
                                case GEOMETRY:
                                case XML:
                                    newValue = overrideValue;
                                    break;

                                default:
                                    return null;
                                }
                                return new AttributeImpl(attributeDescriptor.getName(), newValue);
                            } catch (IllegalArgumentException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .forEach(metacard::setAttribute));
    }

    private void setDefaultValues(CreateRequest createRequest) {
        createRequest.getMetacards()
                .stream()
                .filter(Objects::nonNull)
                .forEach(this::setDefaultValues);
    }

    private void setDefaultValues(UpdateRequest updateRequest) {
        updateRequest.getUpdates()
                .stream()
                .filter(Objects::nonNull)
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .forEach(this::setDefaultValues);
    }

    private boolean hasNoValue(Attribute attribute) {
        return attribute == null || attribute.getValue() == null;
    }

    private void setDefaultValues(Metacard metacard) {
        Map<String, Serializable> defaults = new HashMap<>();
        MetacardType metacardType = metacard.getMetacardType();
        DefaultAttributeValueRegistry registry =
                frameworkProperties.getDefaultAttributeValueRegistry();

        metacardType.getAttributeDescriptors()
                .stream()
                .map(AttributeDescriptor::getName)
                .forEach(attributeName -> {
                    registry.getDefaultValue(metacardType.getName(), attributeName)
                            .ifPresent(defaultValue -> defaults.put(attributeName, defaultValue));
                });

        defaults.forEach((attributeName, defaultValue) -> {
            Attribute attribute = metacard.getAttribute(attributeName);
            if (hasNoValue(attribute)) {
                metacard.setAttribute(new AttributeImpl(attributeName, defaultValue));
            }
        });
    }

    @Override
    public UpdateResponse update(UpdateStorageRequest streamUpdateRequest)
            throws IngestException, SourceUnavailableException {

        validateUpdateStorageRequest(streamUpdateRequest);

        setFlagsOnRequest(streamUpdateRequest);

        if (fanoutEnabled) {
            throw new IngestException(FANOUT_MESSAGE);
        }

        if (Requests.isLocal(streamUpdateRequest) && (!sourceIsAvailable(catalog)
                || !storageIsAvailable(storage))) {
            SourceUnavailableException sourceUnavailableException = new SourceUnavailableException(
                    "Local provider is not available, cannot perform create operation.");
            if (INGEST_LOGGER.isWarnEnabled()) {
                INGEST_LOGGER.warn("Error on create operation, local provider not available.",
                        sourceUnavailableException);
            }
            throw sourceUnavailableException;
        }

        Map<String, Metacard> metacardMap = new HashMap<>();
        List<ContentItem> contentItems = new ArrayList<>(streamUpdateRequest.getContentItems()
                .size());
        HashMap<String, Path> tmpContentPaths = new HashMap<>(streamUpdateRequest.getContentItems()
                .size());
        generateMetacardAndContentItems(streamUpdateRequest,
                streamUpdateRequest.getContentItems(),
                metacardMap,
                contentItems,
                tmpContentPaths);
        streamUpdateRequest.getProperties()
                .put(CONTENT_PATHS, tmpContentPaths);

        UpdateResponse updateResponse;
        UpdateStorageRequest updateStorageRequest = null;
        try {
            if (contentItems.size() > 0) {
                updateStorageRequest = new UpdateStorageRequestImpl(contentItems,
                        streamUpdateRequest.getId(),
                        streamUpdateRequest.getProperties());

                for (final PreUpdateStoragePlugin plugin : frameworkProperties.getPreUpdateStoragePlugins()) {
                    try {
                        updateStorageRequest = plugin.process(updateStorageRequest);
                    } catch (PluginExecutionException e) {
                        LOGGER.warn(
                                "Plugin processing failed. This is allowable. Skipping to next plugin.",
                                e);
                    }
                }

                UpdateStorageResponse updateStorageResponse;
                try {
                    updateStorageResponse = storage.update(updateStorageRequest);
                    updateStorageResponse.getProperties()
                            .put(CONTENT_PATHS, tmpContentPaths);
                } catch (StorageException e) {
                    throw new IngestException(
                            "Could not store content items. Removed created metacards.",
                            e);
                }

                for (final PostUpdateStoragePlugin plugin : frameworkProperties.getPostUpdateStoragePlugins()) {
                    try {
                        updateStorageResponse = plugin.process(updateStorageResponse);
                    } catch (PluginExecutionException e) {
                        LOGGER.warn(
                                "Plugin processing failed. This is allowable. Skipping to next plugin.",
                                e);
                    }
                }

                for (ContentItem contentItem : updateStorageResponse.getUpdatedContentItems()) {
                    metacardMap.put(contentItem.getId(), contentItem.getMetacard());
                }
            }

            UpdateRequestImpl updateRequest =
                    new UpdateRequestImpl(Iterables.toArray(metacardMap.values()
                            .stream()
                            .map(Metacard::getId)
                            .collect(Collectors.toList()), String.class),
                            new ArrayList<>(metacardMap.values()));
            updateRequest.setProperties(streamUpdateRequest.getProperties());
            updateResponse = update(updateRequest);
        } catch (Exception e) {
            if (updateStorageRequest != null) {
                try {
                    storage.rollback(updateStorageRequest);
                } catch (StorageException e1) {
                    LOGGER.error("Unable to remove temporary content for id: "
                            + streamUpdateRequest.getId(), e1);
                }
            }
            throw new IngestException(
                    "Unable to store products for request: " + streamUpdateRequest.getId(), e);
        } finally {
            if (updateStorageRequest != null) {
                try {
                    storage.commit(updateStorageRequest);
                } catch (StorageException e) {
                    LOGGER.error("Unable to commit content changes for id: "
                            + updateStorageRequest.getId(), e);
                    try {
                        storage.rollback(updateStorageRequest);
                    } catch (StorageException e1) {
                        LOGGER.error("Unable to remove temporary content for id: "
                                + updateStorageRequest.getId(), e1);
                    }
                }
            }
            tmpContentPaths.values()
                    .stream()
                    .forEach(path -> FileUtils.deleteQuietly(path.toFile()));
            tmpContentPaths.clear();
        }

        return updateResponse;
    }

    @Override
    public UpdateResponse update(UpdateRequest updateRequest)
            throws IngestException, SourceUnavailableException {

        boolean catalogStoreRequest = isCatalogStoreRequest(updateRequest);
        setFlagsOnRequest(updateRequest);

        if (fanoutEnabled) {
            throw new IngestException(FANOUT_MESSAGE);
        }

        UpdateRequest updateReq = updateRequest;
        validateUpdateRequest(updateReq);

        if (Requests.isLocal(updateRequest) && !sourceIsAvailable(catalog)) {
            throw new SourceUnavailableException(
                    "Local provider is not available, cannot perform update operation.");
        }

        setDefaultValues(updateRequest);

        UpdateResponse updateResponse = null;
        try {
            List<Filter> idFilters = new ArrayList<>();
            for (Entry<Serializable, Metacard> update : updateReq.getUpdates()) {
                idFilters.add(frameworkProperties.getFilterBuilder()
                        .attribute(updateRequest.getAttributeName())
                        .is()
                        .equalTo()
                        .text(update.getKey()
                                .toString()));
            }

            QueryImpl queryImpl = new QueryImpl(getFilterWithAdditionalFilters(idFilters));
            queryImpl.setStartIndex(1);
            queryImpl.setPageSize(updateReq.getUpdates()
                    .size());
            QueryRequestImpl queryRequest = new QueryRequestImpl(queryImpl,
                    updateReq.getStoreIds());

            QueryResponse query;
            Map<String, Metacard> metacardMap = new HashMap<>(updateReq.getUpdates()
                    .size());
            if (!frameworkProperties.getPolicyPlugins()
                    .isEmpty()) {
                try {
                    query = doQuery(queryRequest, frameworkProperties.getFederationStrategy());
                    for (Result result : query.getResults()) {
                        metacardMap.put(getAttributeStringValue(result.getMetacard(),
                                updateRequest.getAttributeName()), result.getMetacard());
                    }
                } catch (FederationException e) {
                    LOGGER.warn("Unable to complete query for updated metacards.", e);
                }
            }
            HashMap<String, Set<String>> requestPolicyMap = new HashMap<>();
            for (Entry<Serializable, Metacard> update : updateReq.getUpdates()) {
                HashMap<String, Set<String>> itemPolicyMap = new HashMap<>();
                HashMap<String, Set<String>> oldItemPolicyMap = new HashMap<>();
                Metacard oldMetacard = metacardMap.get(getAttributeStringValue(update.getValue(),
                        updateRequest.getAttributeName()));
                for (PolicyPlugin plugin : frameworkProperties.getPolicyPlugins()) {
                    PolicyResponse updatePolicyResponse = plugin.processPreUpdate(update.getValue(),
                            Collections.unmodifiableMap(updateReq.getProperties()));
                    PolicyResponse oldPolicyResponse = plugin.processPreUpdate(oldMetacard,
                            Collections.unmodifiableMap(updateReq.getProperties()));
                    buildPolicyMap(itemPolicyMap,
                            updatePolicyResponse.itemPolicy()
                                    .entrySet());
                    buildPolicyMap(oldItemPolicyMap,
                            oldPolicyResponse.itemPolicy()
                                    .entrySet());
                    buildPolicyMap(requestPolicyMap,
                            updatePolicyResponse.operationPolicy()
                                    .entrySet());
                }
                update.getValue()
                        .setAttribute(new AttributeImpl(Metacard.SECURITY, itemPolicyMap));
                if (oldMetacard != null) {
                    oldMetacard.setAttribute(new AttributeImpl(Metacard.SECURITY,
                            oldItemPolicyMap));
                }
            }
            updateReq.getProperties()
                    .put(PolicyPlugin.OPERATION_SECURITY, requestPolicyMap);

            for (AccessPlugin plugin : frameworkProperties.getAccessPlugins()) {
                updateReq = plugin.processPreUpdate(updateReq, metacardMap);
            }

            updateReq.getProperties()
                    .put(Constants.OPERATION_TRANSACTION_KEY,
                            new OperationTransactionImpl(OperationTransaction.OperationType.UPDATE,
                                    metacardMap.values()));

            for (PreIngestPlugin plugin : frameworkProperties.getPreIngest()) {
                try {
                    updateReq = plugin.process(updateReq);
                } catch (PluginExecutionException e) {
                    LOGGER.warn("error processing update in PreIngestPlugin", e);
                }
            }
            validateUpdateRequest(updateReq);

            // Call the create on the catalog
            LOGGER.debug("Calling catalog.update() with {} updates.",
                    updateRequest.getUpdates()
                            .size());

            if (Requests.isLocal(updateReq)) {
                updateResponse = catalog.update(updateReq);
            }

            if (catalogStoreRequest) {
                UpdateResponse remoteUpdateResponse = doRemoteUpdate(updateReq);
                if (updateResponse == null) {
                    updateResponse = remoteUpdateResponse;
                } else {
                    updateResponse.getProperties()
                            .putAll(remoteUpdateResponse.getProperties());
                    updateResponse.getProcessingErrors()
                            .addAll(remoteUpdateResponse.getProcessingErrors());
                }
            }

            // Handle the posting of messages to pubsub
            updateResponse = validateFixUpdateResponse(updateResponse, updateReq);
            for (final PostIngestPlugin plugin : frameworkProperties.getPostIngest()) {
                try {
                    updateResponse = plugin.process(updateResponse);
                } catch (PluginExecutionException e) {
                    LOGGER.info("Plugin exception", e);
                }
            }

        } catch (StopProcessingException see) {
            LOGGER.warn(PRE_INGEST_ERROR, see);
            throw new IngestException(PRE_INGEST_ERROR + see.getMessage());

        } catch (RuntimeException re) {
            LOGGER.warn("Exception during runtime while performing update", re);
            throw new InternalIngestException("Exception during runtime while performing update");

        }

        return updateResponse;
    }

    @Override
    public DeleteResponse delete(DeleteRequest deleteRequest)
            throws IngestException, SourceUnavailableException {

        boolean catalogStoreRequest = isCatalogStoreRequest(deleteRequest);
        setFlagsOnRequest(deleteRequest);

        if (fanoutEnabled) {
            throw new IngestException(FANOUT_MESSAGE);
        }

        validateDeleteRequest(deleteRequest);

        if (Requests.isLocal(deleteRequest) && (!sourceIsAvailable(catalog) || !storageIsAvailable(
                storage))) {
            throw new SourceUnavailableException(
                    "Local provider is not available, cannot perform delete operation.");
        }

        DeleteStorageRequest deleteStorageRequest = null;

        DeleteResponse deleteResponse = null;
        try {
            List<Filter> idFilters = new ArrayList<>();
            for (Serializable serializable : deleteRequest.getAttributeValues()) {
                idFilters.add(frameworkProperties.getFilterBuilder()
                        .attribute(deleteRequest.getAttributeName())
                        .is()
                        .equalTo()
                        .text(serializable.toString()));
            }

            QueryImpl queryImpl = new QueryImpl(getFilterWithAdditionalFilters(idFilters));
            queryImpl.setStartIndex(1);
            queryImpl.setPageSize(deleteRequest.getAttributeValues()
                    .size());
            QueryRequestImpl queryRequest = new QueryRequestImpl(queryImpl,
                    deleteRequest.getStoreIds());

            QueryResponse query;
            List<Metacard> metacards = new ArrayList<>(deleteRequest.getAttributeValues()
                    .size());
            if (!frameworkProperties.getPolicyPlugins()
                    .isEmpty()) {
                try {
                    query = doQuery(queryRequest, frameworkProperties.getFederationStrategy());
                    metacards.addAll(query.getResults()
                            .stream()
                            .map(Result::getMetacard)
                            .collect(Collectors.toList()));
                } catch (FederationException e) {
                    LOGGER.warn("Unable to complete query for updated metacards.", e);
                    throw new IngestException("Exception during runtime while performing delete");
                }

                if (metacards.size() < deleteRequest.getAttributeValues()
                        .size()) {
                    throw new StopProcessingException(
                            "Unable to remove all metacards contained in request.");
                }
            }

            deleteStorageRequest = new DeleteStorageRequestImpl(metacards,
                    deleteRequest.getProperties());

            HashMap<String, Set<String>> requestPolicyMap = new HashMap<>();
            Map<String, Serializable> unmodifiableProperties = Collections.unmodifiableMap(
                    deleteRequest.getProperties());
            for (PolicyPlugin plugin : frameworkProperties.getPolicyPlugins()) {
                PolicyResponse policyResponse = plugin.processPreDelete(metacards,
                        unmodifiableProperties);
                buildPolicyMap(requestPolicyMap,
                        policyResponse.operationPolicy()
                                .entrySet());
            }
            deleteRequest.getProperties()
                    .put(PolicyPlugin.OPERATION_SECURITY, requestPolicyMap);

            for (AccessPlugin plugin : frameworkProperties.getAccessPlugins()) {
                deleteRequest = plugin.processPreDelete(deleteRequest);
            }

            deleteRequest.getProperties()
                    .put(Constants.OPERATION_TRANSACTION_KEY,
                            new OperationTransactionImpl(OperationTransaction.OperationType.DELETE,
                                    metacards));

            for (PreIngestPlugin plugin : frameworkProperties.getPreIngest()) {
                try {
                    deleteRequest = plugin.process(deleteRequest);
                } catch (PluginExecutionException e) {
                    LOGGER.info(
                            "Plugin processing failed. This is allowable. Skipping to next plugin.",
                            e);
                }
            }
            validateDeleteRequest(deleteRequest);

            // Call the Provider delete method
            LOGGER.debug("Calling catalog.delete() with {} entries.",
                    deleteRequest.getAttributeValues()
                            .size());

            if (Requests.isLocal(deleteRequest)) {
                try {
                    storage.delete(deleteStorageRequest);
                } catch (StorageException e) {
                    LOGGER.error(
                            "Unable to delete stored content items. Not removing stored metacards",
                            e);
                    throw new InternalIngestException(
                            "Unable to delete stored content items. Not removing stored metacards.",
                            e);
                }
                deleteResponse = catalog.delete(deleteRequest);
            }

            if (catalogStoreRequest) {
                DeleteResponse remoteDeleteResponse = doRemoteDelete(deleteRequest);
                if (deleteResponse == null) {
                    deleteResponse = remoteDeleteResponse;
                } else {
                    deleteResponse.getProperties()
                            .putAll(remoteDeleteResponse.getProperties());
                    deleteResponse.getProcessingErrors()
                            .addAll(remoteDeleteResponse.getProcessingErrors());
                }
            }

            HashMap<String, Set<String>> responsePolicyMap = new HashMap<>();
            unmodifiableProperties = Collections.unmodifiableMap(deleteRequest.getProperties());
            if (deleteResponse != null && deleteResponse.getDeletedMetacards() != null) {
                for (Metacard metacard : deleteResponse.getDeletedMetacards()) {
                    HashMap<String, Set<String>> itemPolicyMap = new HashMap<>();
                    for (PolicyPlugin plugin : frameworkProperties.getPolicyPlugins()) {
                        PolicyResponse policyResponse = plugin.processPostDelete(metacard,
                                unmodifiableProperties);
                        buildPolicyMap(itemPolicyMap,
                                policyResponse.itemPolicy()
                                        .entrySet());
                        buildPolicyMap(responsePolicyMap,
                                policyResponse.operationPolicy()
                                        .entrySet());
                    }
                    metacard.setAttribute(new AttributeImpl(Metacard.SECURITY, itemPolicyMap));
                }
            }
            deleteRequest.getProperties()
                    .put(PolicyPlugin.OPERATION_SECURITY, responsePolicyMap);

            for (AccessPlugin plugin : frameworkProperties.getAccessPlugins()) {
                deleteResponse = plugin.processPostDelete(deleteResponse);
            }

            // Post results to be available for pubsub
            deleteResponse = validateFixDeleteResponse(deleteResponse, deleteRequest);
            for (final PostIngestPlugin plugin : frameworkProperties.getPostIngest()) {
                try {
                    deleteResponse = plugin.process(deleteResponse);
                } catch (PluginExecutionException e) {
                    LOGGER.info("Plugin exception", e);
                }
            }

        } catch (StopProcessingException see) {
            LOGGER.warn(PRE_INGEST_ERROR + see.getMessage(), see);
            throw new IngestException(PRE_INGEST_ERROR + see.getMessage());

        } catch (RuntimeException re) {
            LOGGER.warn("Exception during runtime while performing delete", re);
            throw new InternalIngestException("Exception during runtime while performing delete");

        } finally {
            if (deleteStorageRequest != null) {
                try {
                    storage.commit(deleteStorageRequest);
                } catch (StorageException e) {
                    LOGGER.error("Unable to remove stored content items.", e);
                }
            }
        }

        return deleteResponse;
    }

    @Override
    public QueryResponse query(QueryRequest fedQueryRequest)
            throws UnsupportedQueryException, FederationException {
        return query(fedQueryRequest, null);
    }

    /**
     * Determines if this catalog framework has any {@link ConnectedSource}s configured.
     *
     * @return true if this framework has any connected sources configured, false otherwise
     */
    protected boolean connectedSourcesExist() {
        return frameworkProperties.getConnectedSources() != null &&
                frameworkProperties.getConnectedSources()
                        .size() > 0;
    }

    @Override
    public QueryResponse query(QueryRequest queryRequest, FederationStrategy strategy)
            throws UnsupportedQueryException, FederationException {
        return query(queryRequest, strategy, false);
    }

    public QueryResponse query(QueryRequest queryRequest, FederationStrategy strategy,
            boolean overrideFanoutRename) throws UnsupportedQueryException, FederationException {

        FederationStrategy fedStrategy = strategy;
        QueryResponse queryResponse;

        setFlagsOnRequest(queryRequest);

        QueryRequest queryReq = queryRequest;

        try {
            validateQueryRequest(queryReq);

            if (fanoutEnabled) {
                // Force an enterprise query
                queryReq = new QueryRequestImpl(queryRequest.getQuery(),
                        true,
                        null,
                        queryRequest.getProperties());
            }

            HashMap<String, Set<String>> requestPolicyMap = new HashMap<>();
            Map<String, Serializable> unmodifiableProperties =
                    Collections.unmodifiableMap(queryReq.getProperties());
            for (PolicyPlugin plugin : frameworkProperties.getPolicyPlugins()) {
                try {
                    PolicyResponse policyResponse = plugin.processPreQuery(queryReq.getQuery(),
                            unmodifiableProperties);
                    buildPolicyMap(requestPolicyMap,
                            policyResponse.operationPolicy()
                                    .entrySet());
                } catch (StopProcessingException e) {
                    throw new FederationException("Query could not be executed.", e);
                }
            }
            queryReq.getProperties()
                    .put(PolicyPlugin.OPERATION_SECURITY, requestPolicyMap);

            for (AccessPlugin plugin : frameworkProperties.getAccessPlugins()) {
                try {
                    queryReq = plugin.processPreQuery(queryReq);
                } catch (StopProcessingException e) {
                    throw new FederationException("Query could not be executed.", e);
                }
            }

            for (PreQueryPlugin service : frameworkProperties.getPreQuery()) {
                try {
                    queryReq = service.process(queryReq);
                } catch (PluginExecutionException see) {
                    LOGGER.warn("Error executing PreQueryPlugin: {}", see.getMessage(), see);
                } catch (StopProcessingException e) {
                    throw new FederationException("Query could not be executed.", e);
                }
            }

            validateQueryRequest(queryReq);

            if (fedStrategy == null) {
                if (frameworkProperties.getFederationStrategy() == null) {
                    throw new FederationException(
                            "No Federation Strategies exist.  Cannot execute federated query.");
                } else {
                    LOGGER.debug("FederationStrategy was not specified, using default strategy: "
                            + frameworkProperties.getFederationStrategy()
                            .getClass());
                    fedStrategy = frameworkProperties.getFederationStrategy();
                }
            }

            queryResponse = doQuery(queryReq, fedStrategy);

            validateFixQueryResponse(queryResponse, queryReq, overrideFanoutRename);

            HashMap<String, Set<String>> responsePolicyMap = new HashMap<>();
            unmodifiableProperties = Collections.unmodifiableMap(queryResponse.getProperties());
            for (Result result : queryResponse.getResults()) {
                HashMap<String, Set<String>> itemPolicyMap = new HashMap<>();
                for (PolicyPlugin plugin : frameworkProperties.getPolicyPlugins()) {
                    try {
                        PolicyResponse policyResponse = plugin.processPostQuery(result,
                                unmodifiableProperties);
                        buildPolicyMap(itemPolicyMap,
                                policyResponse.itemPolicy()
                                        .entrySet());
                        buildPolicyMap(responsePolicyMap,
                                policyResponse.operationPolicy()
                                        .entrySet());
                    } catch (StopProcessingException e) {
                        throw new FederationException("Query could not be executed.", e);
                    }
                }
                result.getMetacard()
                        .setAttribute(new AttributeImpl(Metacard.SECURITY, itemPolicyMap));
            }
            queryResponse.getProperties()
                    .put(PolicyPlugin.OPERATION_SECURITY, responsePolicyMap);

            for (AccessPlugin plugin : frameworkProperties.getAccessPlugins()) {
                try {
                    queryResponse = plugin.processPostQuery(queryResponse);
                } catch (StopProcessingException e) {
                    throw new FederationException("Query could not be executed.", e);
                }
            }

            for (PostQueryPlugin service : frameworkProperties.getPostQuery()) {
                try {
                    queryResponse = service.process(queryResponse);
                } catch (PluginExecutionException see) {
                    LOGGER.warn("Error executing PostQueryPlugin: {}", see.getMessage(), see);
                } catch (StopProcessingException e) {
                    throw new FederationException("Query could not be executed.", e);
                }
            }

        } catch (RuntimeException re) {
            LOGGER.warn("Exception during runtime while performing query", re);
            throw new UnsupportedQueryException("Exception during runtime while performing query");
        }

        return queryResponse;

    }

    private Filter getTagsQueryFilter() {
        return frameworkProperties.getFilterBuilder()
                .anyOf(frameworkProperties.getFilterBuilder()
                                .attribute(Metacard.TAGS)
                                .is()
                                .like()
                                .text(FilterDelegate.WILDCARD_CHAR),
                        frameworkProperties.getFilterBuilder()
                                .attribute(Metacard.TAGS)
                                .empty());
    }

    private Filter getFilterWithAdditionalFilters(List<Filter> originalFilter) {
        return frameworkProperties.getFilterBuilder()
                .allOf(getTagsQueryFilter(),
                        frameworkProperties.getValidationQueryFactory()
                                .getFilterWithValidationFilter(),
                        frameworkProperties.getFilterBuilder()
                                .anyOf(originalFilter));
    }

    private void setFlagsOnRequest(Request request) {
        if (request != null) {
            Set<String> ids = getCombinedIdSet(request);

            request.getProperties()
                    .put(Constants.LOCAL_DESTINATION_KEY,
                            ids.isEmpty() || (catalog != null && ids.contains(catalog.getId())));
            request.getProperties()
                    .put(Constants.REMOTE_DESTINATION_KEY,
                            (Requests.isLocal(request) && ids.size() > 1) || (!Requests.isLocal(
                                    request) && !ids.isEmpty()));
        }
    }

    private boolean isCatalogStoreRequest(Request request) {
        return request != null && request.getStoreIds() != null && !request.getStoreIds()
                .isEmpty() && (request.getStoreIds()
                .size() > 1 || catalog == null || !request.getStoreIds()
                .contains(catalog.getId()));
    }

    private Set<String> getCombinedIdSet(Request request) {
        Set<String> ids = new HashSet<>();
        if (request != null) {
            if (request.getStoreIds() != null) {
                ids.addAll(request.getStoreIds());
            }
            if (request instanceof QueryRequest
                    && ((QueryRequest) request).getSourceIds() != null) {
                ids.addAll(((QueryRequest) request).getSourceIds());
            }
        }
        return ids;
    }

    private List<CatalogStore> getCatalogStoresForRequest(Request request,
            Set<ProcessingDetails> exceptions) {
        List<CatalogStore> results = new ArrayList<>();

        if (isCatalogStoreRequest(request)) {
            for (String destination : request.getStoreIds()) {
                if (frameworkProperties.getCatalogStoresMap()
                        .containsKey(destination)) {
                    results.add(frameworkProperties.getCatalogStoresMap()
                            .get(destination));
                } else if (!hasCatalogProvider() || !destination.equals(catalog.getId())) {
                    exceptions.add(new ProcessingDetailsImpl(destination,
                            null,
                            "CatalogStore does not exist"));
                }
            }
        }
        return results;
    }

    private CreateResponse doRemoteCreate(CreateRequest createRequest) {
        HashSet<ProcessingDetails> exceptions = new HashSet<>();
        Map<String, Serializable> properties = new HashMap<>();

        List<CatalogStore> stores = getCatalogStoresForRequest(createRequest, exceptions);

        for (CatalogStore store : stores) {
            try {
                if (!store.isAvailable()) {
                    exceptions.add(new ProcessingDetailsImpl(store.getId(),
                            null,
                            "CatalogStore is not available"));
                } else {
                    CreateResponse response = store.create(createRequest);
                    properties.put(store.getId(), new ArrayList<>(response.getCreatedMetacards()));
                }
            } catch (IngestException e) {
                INGEST_LOGGER.error("Error creating metacards for CatalogStore {}",
                        store.getId(),
                        e);
                exceptions.add(new ProcessingDetailsImpl(store.getId(), e));
            }
        }

        return new CreateResponseImpl(createRequest,
                properties,
                createRequest.getMetacards(),
                exceptions);
    }

    private UpdateResponse doRemoteUpdate(UpdateRequest updateRequest) {
        HashSet<ProcessingDetails> exceptions = new HashSet<>();
        Map<String, Serializable> properties = new HashMap<>();

        List<CatalogStore> stores = getCatalogStoresForRequest(updateRequest, exceptions);

        List<Update> updates = new ArrayList<>();

        for (CatalogStore store : stores) {
            try {
                if (!store.isAvailable()) {
                    exceptions.add(new ProcessingDetailsImpl(store.getId(),
                            null,
                            "CatalogStore is not available"));
                } else {
                    UpdateResponse response = store.update(updateRequest);
                    properties.put(store.getId(), new ArrayList<>(response.getUpdatedMetacards()));
                    updates = response.getUpdatedMetacards();
                }
            } catch (IngestException e) {
                INGEST_LOGGER.error("Error updating metacards for CatalogStore {}",
                        store.getId(),
                        e);
                exceptions.add(new ProcessingDetailsImpl(store.getId(), e));
            }
        }

        return new UpdateResponseImpl(updateRequest, properties, updates, exceptions);
    }

    private DeleteResponse doRemoteDelete(DeleteRequest deleteRequest) {
        HashSet<ProcessingDetails> exceptions = new HashSet<>();
        Map<String, Serializable> properties = new HashMap<>();

        List<CatalogStore> stores = getCatalogStoresForRequest(deleteRequest, exceptions);

        List<Metacard> metacards = new ArrayList<>();
        for (CatalogStore store : stores) {
            try {
                if (!store.isAvailable()) {
                    exceptions.add(new ProcessingDetailsImpl(store.getId(),
                            null,
                            "CatalogStore is not available"));
                } else {
                    DeleteResponse response = store.delete(deleteRequest);
                    properties.put(store.getId(), new ArrayList<>(response.getDeletedMetacards()));
                    metacards = response.getDeletedMetacards();
                }
            } catch (IngestException e) {
                INGEST_LOGGER.error("Error deleting metacards for CatalogStore {}",
                        store.getId(),
                        e);
                exceptions.add(new ProcessingDetailsImpl(store.getId(), e));
            }
        }

        return new DeleteResponseImpl(deleteRequest, properties, metacards, exceptions);
    }

    /**
     * Executes a query using the specified {@link QueryRequest} and {@link FederationStrategy}.
     * Based on the isEnterprise and sourceIds list in the query request, the federated query may
     * include the local provider and {@link ConnectedSource}s.
     *
     * @param queryRequest the {@link QueryRequest}
     * @param strategy     the {@link FederationStrategy}
     * @return the {@link QueryResponse}
     * @throws FederationException
     */
    private QueryResponse doQuery(QueryRequest queryRequest, FederationStrategy strategy)
            throws FederationException {

        Set<ProcessingDetails> exceptions = new HashSet<>();
        Set<String> sourceIds = getCombinedIdSet(queryRequest);
        LOGGER.debug("source ids: {}", sourceIds);
        List<Source> sourcesToQuery = new ArrayList<>();
        boolean addConnectedSources = false;
        boolean addCatalogProvider = false;
        boolean sourceFound;

        if (queryRequest.isEnterprise()) { // Check if it's an enterprise query
            addConnectedSources = true;
            addCatalogProvider = hasCatalogProvider();

            if (sourceIds != null && !sourceIds.isEmpty()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(
                            "Enterprise Query also included specific sites which will now be ignored");
                }
                sourceIds.clear();
            }

            // add all the federated sources
            for (FederatedSource source : frameworkProperties.getFederatedSources()
                    .values()) {
                if (sourceIsAvailable(source) && canAccessSource(source, queryRequest)) {
                    sourcesToQuery.add(source);
                } else {
                    exceptions.add(createUnavailableProcessingDetails(source));
                }
            }

        } else if (sourceIds != null && !sourceIds.isEmpty()) {
            // it's a targeted federated query
            if (includesLocalSources(sourceIds)) {
                LOGGER.debug("Local source is included in sourceIds");
                addConnectedSources = connectedSourcesExist();
                addCatalogProvider = hasCatalogProvider();
                sourceIds.remove(getId());
                sourceIds.remove(null);
                sourceIds.remove("");
            }

            // See if we still have sources to look up by name
            if (sourceIds.size() > 0) {
                for (String id : sourceIds) {
                    LOGGER.debug("Looking up source ID = {}", id);
                    sourceFound = false;
                    if (frameworkProperties.getFederatedSources()
                            .containsKey(id)) {
                        sourceFound = true;
                        if (frameworkProperties.getFederatedSources()
                                .get(id)
                                .isAvailable()
                                && canAccessSource(frameworkProperties.getFederatedSources()
                                .get(id), queryRequest)) {
                            sourcesToQuery.add(frameworkProperties.getFederatedSources()
                                    .get(id));
                        } else {
                            exceptions.add(createUnavailableProcessingDetails(frameworkProperties.getFederatedSources()
                                    .get(id)));
                        }
                    }

                    if (!sourceFound) {
                        exceptions.add(new ProcessingDetailsImpl(id, new SourceUnavailableException(
                                "Source id is not found")));
                    }
                }
            }
        } else {
            // default to local sources
            addConnectedSources = connectedSourcesExist();
            addCatalogProvider = hasCatalogProvider();
        }

        if (addConnectedSources) {
            // add Connected Sources
            for (ConnectedSource source : frameworkProperties.getConnectedSources()) {
                if (sourceIsAvailable(source)) {
                    sourcesToQuery.add(source);
                } else {
                    // do nothing -- we don't care if a connected source is
                    // unavailable.
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Connected Source {} is unavailable and will not be queried.",
                                source.getId());
                    }
                }
            }
        }

        if (addCatalogProvider) {
            if (sourceIsAvailable(catalog)) {
                sourcesToQuery.add(catalog);
            } else {
                exceptions.add(createUnavailableProcessingDetails(catalog));
            }
        }

        if (sourcesToQuery.isEmpty()) {
            // We have nothing to query at all.
            // TODO change to SourceUnavailableException
            throw new FederationException(
                    "SiteNames could not be resolved due to  invalid site names, none of the sites were available, or the current subject doesn't have permission to access the sites.");
        }

        LOGGER.debug("Calling strategy.federate()");

        QueryResponse response = strategy.federate(sourcesToQuery, queryRequest);
        frameworkProperties.getQueryResponsePostProcessor()
                .processResponse(response);
        return addProcessingDetails(exceptions, response);
    }

    private boolean canAccessSource(FederatedSource source, QueryRequest request) {
        Map<String, Set<String>> securityAttributes = source.getSecurityAttributes();
        if (securityAttributes.isEmpty()) {
            return true;
        }

        Object requestSubject = request.getProperties()
                .get(SecurityConstants.SECURITY_SUBJECT);
        if (requestSubject instanceof ddf.security.Subject) {
            Subject subject = (Subject) requestSubject;

            KeyValueCollectionPermission kvCollection = new KeyValueCollectionPermission(
                    CollectionPermission.READ_ACTION,
                    securityAttributes);
            boolean isPermitted = subject.isPermitted(kvCollection);
            if (isPermitted) {
                SecurityLogger.audit("Subject is permitted to access source {}", source.getId());
            } else {
                SecurityLogger.audit("Subject is not permitted to access source {}",
                        source.getId());
            }
            return isPermitted;
        }
        return false;
    }

    /**
     * Adds any exceptions to the query response's processing details.
     *
     * @param exceptions the set of exceptions to include in the response's {@link ProcessingDetails}. Can
     *                   be empty, but cannot be null.
     * @param response   the {@link QueryResponse} to add the exceptions to
     * @return the modified {@link QueryResponse}
     */
    protected QueryResponse addProcessingDetails(Set<ProcessingDetails> exceptions,
            QueryResponse response) {

        if (!exceptions.isEmpty()) {
            // we have exceptions to merge in
            if (response == null) {
                LOGGER.error(
                        "Could not add Query exceptions to a QueryResponse because the list of ProcessingDetails was null -- according to the API this should not happen");
            } else {
                // need to merge them together.
                Set<ProcessingDetails> sourceDetails = response.getProcessingDetails();
                sourceDetails.addAll(exceptions);
            }
        }
        return response;
    }

    /**
     * Determines if the local catlog provider's source ID is included in the list of source IDs. A
     * source ID in the list of null or an empty string are treated the same as the local source's
     * actual ID being in the list.
     *
     * @param sourceIds the list of source IDs to examine
     * @return true if the list includes the local source's ID, false otherwise
     */
    private boolean includesLocalSources(Set<String> sourceIds) {
        return sourceIds != null && (sourceIds.contains(getId()) || sourceIds.contains("")
                || sourceIds.contains(null));
    }

    /**
     * Whether this {@link CatalogFramework} is configured with a {@link CatalogProvider}.
     *
     * @return true if this {@link CatalogFrameworkImpl} has a {@link CatalogProvider} configured,
     * false otherwise
     */
    protected boolean hasCatalogProvider() {
        if (!this.fanoutEnabled && this.catalog != null) {
            LOGGER.trace("hasCatalogProvider() returning true");
            return true;
        }

        LOGGER.trace("hasCatalogProvider() returning false");
        return false;
    }

    private ProcessingDetailsImpl createUnavailableProcessingDetails(Source source) {
        ProcessingDetailsImpl exception = new ProcessingDetailsImpl();
        SourceUnavailableException sue = new SourceUnavailableException(
                "Source \"" + source.getId() + "\" is unavailable and will not be queried");
        exception.setException(sue);
        exception.setSourceId(source.getId());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Source Unavailable", sue);
        }
        return exception;
    }

    @Override
    public BinaryContent transform(Metacard metacard, String transformerShortname,
            Map<String, Serializable> arguments) throws CatalogTransformerException {

        ServiceReference[] refs;
        try {
            // TODO replace shortname with id
            refs = frameworkProperties.getBundleContext()
                    .getServiceReferences(MetacardTransformer.class.getName(),
                            "(|" + "(" + Constants.SERVICE_SHORTNAME + "=" + transformerShortname
                                    + ")" + "(" + Constants.SERVICE_ID + "=" + transformerShortname
                                    + ")" + ")");
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException(
                    "Invalid transformer shortName: " + transformerShortname, e);
        }
        if (refs == null || refs.length == 0) {
            throw new IllegalArgumentException(
                    "Transformer " + transformerShortname + " not found");
        } else {
            MetacardTransformer transformer =
                    (MetacardTransformer) frameworkProperties.getBundleContext()
                            .getService(refs[0]);
            if (metacard != null) {
                return transformer.transform(metacard, arguments);
            } else {
                throw new IllegalArgumentException("Metacard is null.");
            }
        }
    }

    @Override
    public BinaryContent transform(SourceResponse response, String transformerShortname,
            Map<String, Serializable> arguments) throws CatalogTransformerException {

        ServiceReference[] refs;
        try {
            refs = frameworkProperties.getBundleContext()
                    .getServiceReferences(QueryResponseTransformer.class.getName(),
                            "(|" + "(" + Constants.SERVICE_SHORTNAME + "=" + transformerShortname
                                    + ")" + "(" + Constants.SERVICE_ID + "=" + transformerShortname
                                    + ")" + ")");
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid transformer id: " + transformerShortname,
                    e);
        }

        if (refs == null || refs.length == 0) {
            throw new IllegalArgumentException(
                    "Transformer " + transformerShortname + " not found");
        } else {
            QueryResponseTransformer transformer =
                    (QueryResponseTransformer) frameworkProperties.getBundleContext()
                            .getService(refs[0]);
            if (response != null) {
                return transformer.transform(response, arguments);
            } else {
                throw new IllegalArgumentException("QueryResponse is null.");
            }
        }
    }

    @Override
    public ResourceResponse getLocalResource(ResourceRequest resourceRequest)
            throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        String methodName = "getLocalResource";
        LOGGER.debug("ENTERING: {}", methodName);
        ResourceResponse resourceResponse;
        if (fanoutEnabled) {
            LOGGER.debug("getLocalResource call received, fanning it out to all sites.");
            resourceResponse = getEnterpriseResource(resourceRequest);
        } else {
            resourceResponse = getResource(resourceRequest, false, getId());
        }
        LOGGER.debug("EXITING: {} ", methodName);
        return resourceResponse;
    }

    @Override
    public ResourceResponse getResource(ResourceRequest resourceRequest, String resourceSiteName)
            throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        String methodName = "getResource";
        LOGGER.debug("ENTERING: {}", methodName);
        ResourceResponse resourceResponse;
        if (fanoutEnabled) {
            LOGGER.debug("getResource call received, fanning it out to all sites.");
            resourceResponse = getEnterpriseResource(resourceRequest);
        } else {
            resourceResponse = getResource(resourceRequest, false, resourceSiteName);
        }
        LOGGER.debug("EXITING: {}", methodName);
        return resourceResponse;
    }

    @Override
    public ResourceResponse getEnterpriseResource(ResourceRequest resourceRequest)
            throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        String methodName = "getEnterpriseResource";
        LOGGER.debug("ENTERING: {}", methodName);
        ResourceResponse resourceResponse = getResource(resourceRequest, true, null);
        LOGGER.debug("EXITING: {}", methodName);
        return resourceResponse;
    }

    @Override
    public Set<String> getSourceIds() {
        Set<String> ids = new TreeSet<>();
        ids.add(getId());
        if (!fanoutEnabled) {
            ids.addAll(frameworkProperties.getFederatedSources()
                    .keySet());
        }
        return ids;
    }

    @SuppressWarnings("javadoc")
    protected ResourceResponse getResource(ResourceRequest resourceRequest, boolean isEnterprise,
            String resourceSiteName)
            throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        ResourceResponse resourceResponse = null;
        ResourceRequest resourceReq = resourceRequest;
        String resourceSourceName = resourceSiteName;
        ResourceRetriever retriever = null;

        if (fanoutEnabled) {
            isEnterprise = true;
        }

        if (resourceSourceName == null && !isEnterprise) {
            throw new ResourceNotFoundException(
                    "resourceSiteName cannot be null when obtaining resource.");
        }

        validateGetResourceRequest(resourceReq);
        try {
            HashMap<String, Set<String>> requestPolicyMap = new HashMap<>();
            for (PolicyPlugin plugin : frameworkProperties.getPolicyPlugins()) {
                PolicyResponse policyResponse = plugin.processPreResource(resourceReq);
                buildPolicyMap(requestPolicyMap,
                        policyResponse.operationPolicy()
                                .entrySet());
            }
            resourceReq.getProperties()
                    .put(PolicyPlugin.OPERATION_SECURITY, requestPolicyMap);

            for (AccessPlugin plugin : frameworkProperties.getAccessPlugins()) {
                resourceReq = plugin.processPreResource(resourceReq);
            }

            for (PreResourcePlugin plugin : frameworkProperties.getPreResource()) {
                try {
                    resourceReq = plugin.process(resourceReq);
                } catch (PluginExecutionException e) {
                    LOGGER.info(
                            "Plugin processing failed. This is allowable. Skipping to next plugin.",
                            e);
                }
            }

            Map<String, Serializable> requestProperties = resourceReq.getProperties();
            LOGGER.debug("Attempting to get resource from siteName: {}", resourceSourceName);
            // At this point we pull out the properties and use them.
            Serializable sourceIdProperty = requestProperties.get(ResourceRequest.SOURCE_ID);
            if (sourceIdProperty != null) {
                resourceSourceName = sourceIdProperty.toString();
            }

            Serializable enterpriseProperty = requestProperties.get(ResourceRequest.IS_ENTERPRISE);
            if (enterpriseProperty != null) {
                if (Boolean.parseBoolean(enterpriseProperty.toString())) {
                    isEnterprise = true;
                }
            }

            // check if the resourceRequest has an ID only
            // If so, the metacard needs to be found and the Resource URI
            StringBuilder resolvedSourceIdHolder = new StringBuilder();

            ResourceInfo resourceInfo = getResourceInfo(resourceReq,
                    resourceSourceName,
                    isEnterprise,
                    resolvedSourceIdHolder,
                    requestProperties);
            if (resourceInfo == null) {
                throw new ResourceNotFoundException(
                        "Resource could not be found for the given attribute value: "
                                + resourceReq.getAttributeValue());
            }
            URI responseURI = resourceInfo.getResourceUri();
            Metacard metacard = resourceInfo.getMetacard();

            String resolvedSourceId = resolvedSourceIdHolder.toString();
            LOGGER.debug("resolvedSourceId = {}", resolvedSourceId);
            LOGGER.debug("ID = {}", getId());

            if (isEnterprise) {
                // since resolvedSourceId specifies what source the product
                // metacard resides on, we can just
                // change resourceSiteName to be that value, and then the
                // following if-else statements will
                // handle retrieving the product on the correct source
                resourceSourceName = resolvedSourceId;
            }

            // retrieve product from specified federated site if not in cache
            if (!resourceSourceName.equals(getId())) {
                LOGGER.debug("Searching federatedSource {} for resource.", resourceSourceName);
                LOGGER.debug("metacard for product found on source: {}", resolvedSourceId);

                FederatedSource source = frameworkProperties.getFederatedSources()
                        .get(resourceSourceName);

                if (source != null) {
                    LOGGER.debug("Adding federated site to federated query: {}",
                            source.getId());
                    LOGGER.debug("Retrieving product from remote source {}", source.getId());
                    retriever = new RemoteResourceRetriever(source,
                            responseURI,
                            requestProperties);
                } else {
                    LOGGER.warn("Could not find federatedSource: {}", resourceSourceName);
                }
            } else {
                LOGGER.debug("Retrieving product from local source {}", resourceSourceName);
                retriever =
                        new LocalResourceRetriever(frameworkProperties.getResourceReaders(),
                                responseURI,
                                requestProperties);
            }

            try {
                resourceResponse = frameworkProperties.getReliableResourceDownloadManager()
                        .download(resourceRequest, metacard, retriever);
            } catch (DownloadException e) {
                LOGGER.info("Unable to download resource", e);
            }

            resourceResponse = validateFixGetResourceResponse(resourceResponse, resourceReq);

            HashMap<String, Set<String>> responsePolicyMap = new HashMap<>();
            for (PolicyPlugin plugin : frameworkProperties.getPolicyPlugins()) {
                PolicyResponse policyResponse = plugin.processPostResource(resourceResponse,
                        metacard);
                buildPolicyMap(responsePolicyMap,
                        policyResponse.operationPolicy()
                                .entrySet());
            }
            resourceResponse.getProperties()
                    .put(PolicyPlugin.OPERATION_SECURITY, responsePolicyMap);

            for (AccessPlugin plugin : frameworkProperties.getAccessPlugins()) {
                resourceResponse = plugin.processPostResource(resourceResponse, metacard);
            }

            for (PostResourcePlugin plugin : frameworkProperties.getPostResource()) {
                try {
                    resourceResponse = plugin.process(resourceResponse);
                } catch (PluginExecutionException e) {
                    LOGGER.info(
                            "Plugin processing failed. This is allowable. Skipping to next plugin.",
                            e);
                }
            }
            resourceResponse.getProperties()
                    .put(Constants.METACARD_PROPERTY, metacard);
        } catch (DataUsageLimitExceededException e) {
            LOGGER.error("RuntimeException caused by: ", e);
            throw e;
        } catch (RuntimeException e) {
            LOGGER.error("RuntimeException caused by: ", e);
            throw new ResourceNotFoundException("Unable to find resource");
        } catch (StopProcessingException e) {
            LOGGER.error("Resource not supported", e);
            throw new ResourceNotSupportedException(FAILED_BY_GET_RESOURCE_PLUGIN + e.getMessage());
        }

        return resourceResponse;
    }

    /**
     * String representation of this {@code CatalogFrameworkImpl}.
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    /**
     * Retrieves a resource by URI.
     * <p>
     * The {@link ResourceRequest} can specify either the product's URI or ID. If the product ID is
     * specified, then the matching {@link Metacard} must first be retrieved and the product URI
     * extracted from this {@link Metacard}.
     *
     * @param resourceRequest
     * @param site
     * @param isEnterprise
     * @param federatedSite
     * @param requestProperties
     * @return
     * @throws ResourceNotSupportedException
     * @throws ResourceNotFoundException
     */
    protected ResourceInfo getResourceInfo(ResourceRequest resourceRequest, String site,
            boolean isEnterprise, StringBuilder federatedSite,
            Map<String, Serializable> requestProperties)
            throws ResourceNotSupportedException, ResourceNotFoundException {

        Metacard metacard;
        URI resourceUri;
        String name = resourceRequest.getAttributeName();
        try {
            if (ResourceRequest.GET_RESOURCE_BY_PRODUCT_URI.equals(name)) {
                // because this is a get resource by product uri, we already
                // have the product uri to return
                LOGGER.debug("get resource by product uri");
                Object value = resourceRequest.getAttributeValue();

                if (value instanceof URI) {
                    resourceUri = (URI) value;
                    if (StringUtils.isNotBlank(resourceUri.getFragment())) {
                        resourceRequest.getProperties()
                                .put(ContentItem.QUALIFIER, resourceUri.getFragment());
                        try {
                            resourceUri = new URI(resourceUri.getScheme(),
                                    resourceUri.getSchemeSpecificPart(),
                                    null);
                        } catch (URISyntaxException e) {
                            throw new ResourceNotFoundException(
                                    "Could not resolve URI by doing a URI based query: " + value);
                        }
                    }

                    Query propertyEqualToUriQuery =
                            createPropertyIsEqualToQuery(Metacard.RESOURCE_URI,
                                    resourceUri.toString());

                    // if isEnterprise, go out and obtain the actual source
                    // where the product's metacard is stored.
                    QueryRequest queryRequest = new QueryRequestImpl(propertyEqualToUriQuery,
                            isEnterprise,
                            Collections.singletonList(site == null ? this.getId() : site),
                            resourceRequest.getProperties());

                    QueryResponse queryResponse = query(queryRequest, null, true);
                    if (queryResponse.getResults()
                            .size() > 0) {
                        metacard = queryResponse.getResults()
                                .get(0)
                                .getMetacard();
                        federatedSite.append(metacard.getSourceId());
                        LOGGER.debug("Trying to lookup resource URI {} for metacardId: {}",
                                resourceUri,
                                resourceUri);

                        if (!requestProperties.containsKey(Metacard.ID)) {
                            requestProperties.put(Metacard.ID, metacard.getId());
                        }
                        if (!requestProperties.containsKey(Metacard.RESOURCE_URI)) {
                            requestProperties.put(Metacard.RESOURCE_URI, metacard.getResourceURI());
                        }
                    } else {
                        throw new ResourceNotFoundException(
                                "Could not resolve source id for URI by doing a URI based query: "
                                        + resourceUri);
                    }
                } else {
                    throw new ResourceNotSupportedException(
                            "The GetResourceRequest with attribute value of class '"
                                    + value.getClass()
                                    + "' is not supported by this instance of the CatalogFramework.");
                }
            } else if (ResourceRequest.GET_RESOURCE_BY_ID.equals(name)) {
                // since this is a get resource by id, we need to obtain the
                // product URI
                LOGGER.debug("get resource by id");
                Object value = resourceRequest.getAttributeValue();
                if (value instanceof String) {
                    String metacardId = (String) value;
                    LOGGER.debug("metacardId = {},   site = {}", metacardId, site);
                    QueryRequest queryRequest = new QueryRequestImpl(createMetacardIdQuery(
                            metacardId),
                            isEnterprise,
                            Collections.singletonList(site == null ? this.getId() : site),
                            resourceRequest.getProperties());

                    QueryResponse queryResponse = query(queryRequest, null, true);
                    if (queryResponse.getResults()
                            .size() > 0) {
                        metacard = queryResponse.getResults()
                                .get(0)
                                .getMetacard();
                        resourceUri = metacard.getResourceURI();
                        federatedSite.append(metacard.getSourceId());
                        LOGGER.debug("Trying to lookup resource URI {} for metacardId: {}",
                                resourceUri,
                                metacardId);
                    } else {
                        throw new ResourceNotFoundException(
                                "Could not resolve source id for URI by doing an id based query: "
                                        + metacardId);
                    }

                    if (!requestProperties.containsKey(Metacard.ID)) {
                        requestProperties.put(Metacard.ID, metacardId);
                    }
                    if (!requestProperties.containsKey(Metacard.RESOURCE_URI)) {
                        requestProperties.put(Metacard.RESOURCE_URI, resourceUri);
                    }
                } else {
                    throw new ResourceNotSupportedException(
                            "The GetResourceRequest with attribute value of class '"
                                    + value.getClass()
                                    + "' is not supported by this instance of the CatalogFramework.");
                }
            } else {
                throw new ResourceNotSupportedException(
                        "The GetResourceRequest with attribute name '" + name
                                + "' is not supported by this instance of the CatalogFramework.");
            }
        } catch (UnsupportedQueryException | FederationException e) {

            throw new ResourceNotFoundException(DEFAULT_RESOURCE_NOT_FOUND_MESSAGE, e);
        }

        LOGGER.debug("Returning resourceURI: {}", resourceUri);
        if (resourceUri == null) {
            throw new ResourceNotFoundException(DEFAULT_RESOURCE_NOT_FOUND_MESSAGE);
        }

        return new ResourceInfo(metacard, resourceUri);
    }

    protected Query createMetacardIdQuery(String metacardId) {
        return createPropertyIsEqualToQuery(Metacard.ID, metacardId);
    }

    protected Query createPropertyIsEqualToQuery(String propertyName, String literal) {
        return new QueryImpl(new PropertyIsEqualToLiteral(new PropertyNameImpl(propertyName),
                new LiteralImpl(literal)));
    }

    /**
     * Checks that the specified source is valid and available.
     *
     * @param source the {@link Source} to check availability of
     * @return true if the {@link Source} is available, false otherwise
     */
    protected boolean sourceIsAvailable(Source source) {
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

    protected boolean storageIsAvailable(StorageProvider storageProvider) {
        if (storageProvider == null) {
            LOGGER.warn("storageProvider is null, therefore not available");
            return false;
        }
        return true;
    }

    /**
     * Validates that the {@link CreateResponse} has one or more {@link Metacard}s in it that were
     * created in the catalog, and that the original {@link CreateRequest} is included in the
     * response.
     *
     * @param createResponse the original {@link CreateResponse} returned from the catalog provider
     * @param createRequest  the original {@link CreateRequest} sent to the catalog provider
     * @return the updated {@link CreateResponse}
     * @throws IngestException if original {@link CreateResponse} passed in is null or the {@link Metacard}s
     *                         list in the response is null
     */
    protected CreateResponse validateFixCreateResponse(CreateResponse createResponse,
            CreateRequest createRequest) throws IngestException {
        if (createResponse != null) {
            if (createResponse.getCreatedMetacards() == null) {
                throw new IngestException(
                        "CatalogProvider returned null list of results from create method.");
            }
            if (createResponse.getRequest() == null) {
                createResponse = new CreateResponseImpl(createRequest,
                        createResponse.getProperties(),
                        createResponse.getCreatedMetacards());
            }
        } else {
            throw new IngestException("CatalogProvider returned null CreateResponse Object.");
        }
        return createResponse;
    }

    /**
     * Validates that the {@link UpdateResponse} has one or more {@link Metacard}s in it that were
     * updated in the catalog, and that the original {@link UpdateRequest} is included in the
     * response.
     *
     * @param updateResponse the original {@link UpdateResponse} returned from the catalog provider
     * @param updateRequest  the original {@link UpdateRequest} sent to the catalog provider
     * @return the updated {@link UpdateResponse}
     * @throws IngestException if original {@link UpdateResponse} passed in is null or the {@link Metacard}s
     *                         list in the response is null
     */
    protected UpdateResponse validateFixUpdateResponse(UpdateResponse updateResponse,
            UpdateRequest updateRequest) throws IngestException {
        UpdateResponse updateResp = updateResponse;
        if (updateResp != null) {
            if (updateResp.getUpdatedMetacards() == null) {
                throw new IngestException(
                        "CatalogProvider returned null list of results from update method.");
            }
            if (updateResp.getRequest() == null) {
                updateResp = new UpdateResponseImpl(updateRequest,
                        updateResponse.getProperties(),
                        updateResponse.getUpdatedMetacards());
            }
        } else {
            throw new IngestException("CatalogProvider returned null UpdateResponse Object.");
        }
        return updateResp;
    }

    /**
     * Validates that the {@link DeleteResponse} has one or more {@link Metacard}s in it that were
     * deleted in the catalog, and that the original {@link DeleteRequest} is included in the
     * response.
     *
     * @param deleteResponse the original {@link DeleteResponse} returned from the catalog provider
     * @param deleteRequest  the original {@link DeleteRequest} sent to the catalog provider
     * @return the updated {@link DeleteResponse}
     * @throws IngestException if original {@link DeleteResponse} passed in is null or the {@link Metacard}s
     *                         list in the response is null
     */
    protected DeleteResponse validateFixDeleteResponse(DeleteResponse deleteResponse,
            DeleteRequest deleteRequest) throws IngestException {
        DeleteResponse delResponse = deleteResponse;
        if (delResponse != null) {
            if (delResponse.getDeletedMetacards() == null) {
                throw new IngestException(
                        "CatalogProvider returned null list of results from delete method.");
            }
            if (delResponse.getRequest() == null) {
                delResponse = new DeleteResponseImpl(deleteRequest,
                        delResponse.getProperties(),
                        delResponse.getDeletedMetacards());
            }
        } else {
            throw new IngestException("CatalogProvider returned null DeleteResponse Object.");
        }
        return delResponse;
    }

    /**
     * Validates that the {@link ResourceResponse} has a {@link ddf.catalog.resource.Resource} in it that was retrieved,
     * and that the original {@link ResourceRequest} is included in the response.
     *
     * @param getResourceResponse the original {@link ResourceResponse} returned from the source
     * @param getResourceRequest  the original {@link ResourceRequest} sent to the source
     * @return the updated {@link ResourceResponse}
     * @throws ResourceNotFoundException if the original {@link ResourceResponse} is null or the resource could not be
     *                                   found
     */
    protected ResourceResponse validateFixGetResourceResponse(ResourceResponse getResourceResponse,
            ResourceRequest getResourceRequest) throws ResourceNotFoundException {
        ResourceResponse resourceResponse = getResourceResponse;
        if (getResourceResponse != null) {
            if (getResourceResponse.getResource() == null) {
                throw new ResourceNotFoundException(
                        "Resource was returned as null, meaning it could not be found.");
            }
            if (getResourceResponse.getRequest() == null) {
                resourceResponse = new ResourceResponseImpl(getResourceRequest,
                        getResourceResponse.getProperties(),
                        getResourceResponse.getResource());
            }
        } else {
            throw new ResourceNotFoundException(
                    "CatalogProvider returned null ResourceResponse Object.");
        }
        return resourceResponse;
    }

    /**
     * Validates that the {@link QueryResponse} has a non-null list of {@link Result}s in it, and
     * that the original {@link QueryRequest} is included in the response.
     *
     * @param sourceResponse       the original {@link ddf.catalog.operation.SourceResponse} returned from the source
     * @param queryRequest         the original {@link ddf.catalog.operation.QueryRequest} sent to the source
     * @param overrideFanoutRename
     * @return the updated {@link QueryResponse}
     * @throws UnsupportedQueryException if the original {@link QueryResponse} is null or the results list is null
     */
    protected SourceResponse validateFixQueryResponse(SourceResponse sourceResponse,
            QueryRequest queryRequest, boolean overrideFanoutRename)
            throws UnsupportedQueryException {
        SourceResponse sourceResp = sourceResponse;
        if (fanoutEnabled && !overrideFanoutRename) {
            sourceResp = replaceSourceId((QueryResponse) sourceResponse);
        }
        if (sourceResp != null) {
            if (sourceResp.getResults() == null) {
                throw new UnsupportedQueryException(
                        "CatalogProvider returned null list of results from query method.");
            }
            if (sourceResp.getRequest() == null) {
                sourceResp = new SourceResponseImpl(queryRequest,
                        sourceResp.getProperties(),
                        sourceResp.getResults());
            }
        } else {
            throw new UnsupportedQueryException(
                    "CatalogProvider returned null QueryResponse Object.");
        }
        return sourceResp;
    }

    /**
     * Replaces the site name(s) of {@link FederatedSource}s in the {@link QueryResponse} with the
     * fanout's site name to keep info about the {@link FederatedSource}s hidden from the external
     * client.
     *
     * @param queryResponse the original {@link QueryResponse} from the query request
     * @return the updated {@link QueryResponse} with all site names replaced with fanout's site
     * name
     */
    protected QueryResponse replaceSourceId(QueryResponse queryResponse) {
        LOGGER.debug("ENTERING: replaceSourceId()");
        List<Result> results = queryResponse.getResults();
        QueryResponseImpl newResponse = new QueryResponseImpl(queryResponse.getRequest(),
                queryResponse.getProperties());
        for (Result result : results) {
            MetacardImpl newMetacard = new MetacardImpl(result.getMetacard());
            newMetacard.setSourceId(this.getId());
            ResultImpl newResult = new ResultImpl(newMetacard);
            // Copy over scores
            newResult.setDistanceInMeters(result.getDistanceInMeters());
            newResult.setRelevanceScore(result.getRelevanceScore());
            newResponse.addResult(newResult, false);
        }
        newResponse.setHits(queryResponse.getHits());
        newResponse.closeResultQueue();
        LOGGER.debug("EXITING: replaceSourceId()");
        return newResponse;
    }

    /**
     * Validates that the {@link CreateRequest} is non-null and has a non-empty list of
     * {@link Metacard}s in it.
     *
     * @param createRequest the {@link CreateRequest}
     * @throws IngestException if the {@link CreateRequest} is null, or request has a null or empty list of
     *                         {@link Metacard}s
     */
    protected void validateCreateRequest(CreateRequest createRequest) throws IngestException {
        if (createRequest == null) {
            throw new IngestException(
                    "CreateRequest was null, either passed in from endpoint, or as output from PreIngestPlugins");
        }
        List<Metacard> entries = createRequest.getMetacards();
        if (entries == null || entries.size() == 0) {
            throw new IngestException(
                    "Cannot perform ingest with null/empty entry list, either passed in from endpoint, or as output from PreIngestPlugins");
        }
    }

    /**
     * Validates that the {@link UpdateRequest} is non-null, has a non-empty list of
     * {@link Metacard}s in it, and a non-null attribute name (which specifies if the update is
     * being done by product URI or ID).
     *
     * @param updateRequest the {@link UpdateRequest}
     * @throws IngestException if the {@link UpdateRequest} is null, or has null or empty {@link Metacard} list,
     *                         or a null attribute name.
     */
    protected void validateUpdateRequest(UpdateRequest updateRequest) throws IngestException {
        if (updateRequest == null) {
            throw new IngestException(
                    "UpdateRequest was null, either passed in from endpoint, or as output from PreIngestPlugins");
        }
        List<Entry<Serializable, Metacard>> entries = updateRequest.getUpdates();
        if (entries == null || entries.size() == 0 || updateRequest.getAttributeName() == null) {
            throw new IngestException(
                    "Cannot perform update with null/empty attribute value list or null attributeName, either passed in from endpoint, or as output from PreIngestPlugins");
        }
    }

    /**
     * Validates that the {@link CreateStorageRequest} is non-null and has a non-empty list of
     * {@link ContentItem}s in it.
     *
     * @param createRequest the {@link CreateStorageRequest}
     * @throws IngestException if the {@link CreateStorageRequest} is null, or request has a null or empty list of
     *                         {@link ContentItem}s
     */
    protected void validateCreateStorageRequest(CreateStorageRequest createRequest)
            throws IngestException {
        if (createRequest == null) {
            throw new IngestException("CreateStorageRequest was null.");
        }
        List<ContentItem> entries = createRequest.getContentItems();
        if (entries == null || entries.size() == 0) {
            throw new IngestException("Cannot perform ingest with null/empty entry list.");
        }
    }

    /**
     * Validates that the {@link UpdateStorageRequest} is non-null, has a non-empty list of
     * {@link ContentItem}s in it.
     *
     * @param updateRequest the {@link UpdateStorageRequest}
     * @throws IngestException if the {@link UpdateStorageRequest} is null, or has null or empty {@link ContentItem} list.
     */
    protected void validateUpdateStorageRequest(UpdateStorageRequest updateRequest)
            throws IngestException {
        if (updateRequest == null) {
            throw new IngestException("UpdateStorageRequest was null.");
        }
        List<ContentItem> entries = updateRequest.getContentItems();
        if (entries == null || entries.size() == 0) {
            throw new IngestException("Cannot perform update with null/empty entry list.");
        }
    }

    /**
     * Validates that the {@link DeleteRequest} is non-null, has a non-empty list of
     * {@link Metacard}s in it, and a non-null attribute name (which specifies if the delete is
     * being done by product URI or ID).
     *
     * @param deleteRequest the {@link DeleteRequest}
     * @throws IngestException if the {@link DeleteRequest} is null, or has null or empty {@link Metacard} list,
     *                         or a null attribute name
     */
    protected void validateDeleteRequest(DeleteRequest deleteRequest) throws IngestException {
        if (deleteRequest == null) {
            throw new IngestException(
                    "DeleteRequest was null, either passed in from endpoint, or as output from PreIngestPlugins");
        }
        List<?> entries = deleteRequest.getAttributeValues();
        if (entries == null || entries.size() == 0 || deleteRequest.getAttributeName() == null) {
            throw new IngestException(
                    "Cannot perform delete with null/empty attribute value list or null attributeName, either passed in from endpoint, or as output from PreIngestPlugins");
        }
    }

    /**
     * Validates that the {@link ResourceRequest} is non-null, a non-null attribute name (which
     * specifies if the retrieval is being done by product URI or ID), and a non-null attribute
     * value.
     *
     * @param getResourceRequest the {@link ResourceRequest}
     * @throws ResourceNotSupportedException if the {@link ResourceRequest} is null, or has a null attribute value or name
     */
    protected void validateGetResourceRequest(ResourceRequest getResourceRequest)
            throws ResourceNotSupportedException {
        if (getResourceRequest == null) {
            throw new ResourceNotSupportedException(
                    "GetResourceRequest was null, either passed in from endpoint, or as output from PreResourcePlugin");
        }
        Object value = getResourceRequest.getAttributeValue();
        if (value == null || getResourceRequest.getAttributeName() == null) {
            throw new ResourceNotSupportedException(
                    "Cannot perform getResource with null attribute value or null attributeName, either passed in from endpoint, or as output from PreResourcePlugin");
        }
    }

    /**
     * Validates that the {@link QueryRequest} is non-null and that the query in it is non-null.
     *
     * @param queryRequest the {@link QueryRequest}
     * @throws UnsupportedQueryException if the {@link QueryRequest} is null or the query in it is null
     */
    protected void validateQueryRequest(QueryRequest queryRequest)
            throws UnsupportedQueryException {
        if (queryRequest == null) {
            throw new UnsupportedQueryException(
                    "QueryRequest was null, either passed in from endpoint, or as output from a PreQuery Plugin");
        }

        if (queryRequest.getQuery() == null) {
            throw new UnsupportedQueryException(
                    "Cannot perform query with null query, either passed in from endpoint, or as output from a PreQuery Plugin");
        }

        if (fanoutEnabled) {
            Set<String> sources = queryRequest.getSourceIds();
            if (sources != null) {
                for (String querySourceId : sources) {
                    LOGGER.debug("validating requested sourceId {}", querySourceId);
                    if (!querySourceId.equals(this.getId())) {
                        UnsupportedQueryException unsupportedQueryException =
                                new UnsupportedQueryException("Unknown source: " + querySourceId);
                        LOGGER.debug(
                                "Throwing unsupportedQueryException due to unknown sourceId: {}",
                                querySourceId,
                                unsupportedQueryException);
                        throw unsupportedQueryException;
                    }
                }
            }
        }
    }

    /**
     * Helper method to build ingest log strings
     */
    private String buildIngestLog(CreateRequest createReq) {
        StringBuilder strBuilder = new StringBuilder();
        List<Metacard> metacards = createReq.getMetacards();
        final String newLine = System.getProperty("line.separator");

        for (int i = 0; i < metacards.size(); i++) {
            Metacard card = metacards.get(i);
            strBuilder.append(newLine)
                    .append("Batch #: ")
                    .append(i + 1)
                    .append(" | ");
            if (card != null) {
                if (card.getTitle() != null) {
                    strBuilder.append("Metacard Title: ")
                            .append(card.getTitle())
                            .append(" | ");
                }
                if (card.getId() != null) {
                    strBuilder.append("Metacard ID: ")
                            .append(card.getId())
                            .append(" | ");
                }
            } else {
                strBuilder.append("Null Metacard");
            }
        }
        return strBuilder.toString();
    }

    @Deprecated
    @Override
    public Map<String, Set<String>> getLocalResourceOptions(String metacardId)
            throws ResourceNotFoundException {
        LOGGER.trace("ENTERING: getLocalResourceOptions");

        Map<String, Set<String>> optionsMap;
        try {
            QueryRequest queryRequest = new QueryRequestImpl(createMetacardIdQuery(metacardId),
                    false,
                    Collections.singletonList(getId()),
                    null);
            QueryResponse queryResponse = query(queryRequest);
            List<Result> results = queryResponse.getResults();

            if (results.size() > 0) {
                Metacard metacard = results.get(0)
                        .getMetacard();
                optionsMap = Collections.singletonMap(ResourceRequest.OPTION_ARGUMENT,
                        getOptionsFromLocalProvider(metacard));
            } else {

                String message = "Could not find metacard " + metacardId + " on local source";
                ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                        message);
                LOGGER.trace("EXITING: getLocalResourceOptions");
                throw resourceNotFoundException;
            }
        } catch (UnsupportedQueryException e) {
            LOGGER.warn("Error finding metacard {}", metacardId, e);
            LOGGER.trace("EXITING: getLocalResourceOptions");
            throw new ResourceNotFoundException("Error finding metacard due to Unsuppported Query",
                    e);
        } catch (FederationException e) {
            LOGGER.warn("Error federating query for metacard {}", metacardId, e);
            LOGGER.trace("EXITING: getLocalResourceOptions");
            throw new ResourceNotFoundException("Error finding metacard due to Federation issue",
                    e);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Metacard couldn't be found {}", metacardId, e);
            LOGGER.trace("EXITING: getLocalResourceOptions");
            throw new ResourceNotFoundException("Query returned null metacard", e);
        }

        LOGGER.trace("EXITING: getLocalResourceOptions");

        return optionsMap;
    }

    @Deprecated
    @Override
    public Map<String, Set<String>> getEnterpriseResourceOptions(String metacardId)
            throws ResourceNotFoundException {
        LOGGER.trace("ENTERING: getEnterpriseResourceOptions");
        Set<String> supportedOptions = Collections.emptySet();

        try {
            QueryRequest queryRequest = new QueryRequestImpl(createMetacardIdQuery(metacardId),
                    true,
                    null,
                    null);
            QueryResponse queryResponse = query(queryRequest);
            List<Result> results = queryResponse.getResults();

            if (results.size() > 0) {
                Metacard metacard = results.get(0)
                        .getMetacard();
                String sourceIdOfResult = metacard.getSourceId();

                if (sourceIdOfResult != null && sourceIdOfResult.equals(getId())) {
                    // found entry on local source
                    supportedOptions = getOptionsFromLocalProvider(metacard);
                } else if (sourceIdOfResult != null && !sourceIdOfResult.equals(getId())) {
                    // found entry on federated source
                    supportedOptions = getOptionsFromFederatedSource(metacard, sourceIdOfResult);
                }
            } else {
                String message = "Unable to find metacard " + metacardId + " on enterprise.";
                LOGGER.debug(message);
                LOGGER.trace("EXITING: getEnterpriseResourceOptions");
                throw new ResourceNotFoundException(message);
            }

        } catch (UnsupportedQueryException e) {
            LOGGER.warn("Error finding metacard {}", metacardId, e);
            LOGGER.trace("EXITING: getEnterpriseResourceOptions");
            throw new ResourceNotFoundException("Error finding metacard due to Unsuppported Query",
                    e);
        } catch (FederationException e) {
            LOGGER.warn("Error federating query for metacard {}", metacardId, e);
            LOGGER.trace("EXITING: getEnterpriseResourceOptions");
            throw new ResourceNotFoundException("Error finding metacard due to Federation issue",
                    e);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Metacard couldn't be found {}", metacardId, e);
            LOGGER.trace("EXITING: getEnterpriseResourceOptions");
            throw new ResourceNotFoundException("Query returned null metacard", e);
        }

        LOGGER.trace("EXITING: getEnterpriseResourceOptions");
        return Collections.singletonMap(ResourceRequest.OPTION_ARGUMENT, supportedOptions);
    }

    @Deprecated
    @Override
    public Map<String, Set<String>> getResourceOptions(String metacardId, String sourceId)
            throws ResourceNotFoundException {
        LOGGER.trace("ENTERING: getResourceOptions");
        Map<String, Set<String>> optionsMap;
        try {
            LOGGER.debug("source id to get options from: {}", sourceId);
            QueryRequest queryRequest = new QueryRequestImpl(createMetacardIdQuery(metacardId),
                    false,
                    Collections.singletonList(sourceId == null ? this.getId() : sourceId),
                    null);
            QueryResponse queryResponse = query(queryRequest);
            List<Result> results = queryResponse.getResults();

            if (results.size() > 0) {
                Metacard metacard = results.get(0)
                        .getMetacard();
                // DDF-1763: Check if the source ID passed in is null, empty,
                // or the local provider.
                if (StringUtils.isEmpty(sourceId) || sourceId.equals(getId())) {
                    optionsMap = Collections.singletonMap(ResourceRequest.OPTION_ARGUMENT,
                            getOptionsFromLocalProvider(metacard));
                } else {
                    optionsMap = Collections.singletonMap(ResourceRequest.OPTION_ARGUMENT,
                            getOptionsFromFederatedSource(metacard, sourceId));
                }
            } else {

                String message = "Could not find metacard " + metacardId + " on source " + sourceId;
                throw new ResourceNotFoundException(message);
            }
        } catch (UnsupportedQueryException e) {
            LOGGER.warn("Error finding metacard {}", metacardId, e);
            throw new ResourceNotFoundException("Error finding metacard due to Unsuppported Query",
                    e);
        } catch (FederationException e) {
            LOGGER.warn("Error federating query for metacard {}", metacardId, e);
            throw new ResourceNotFoundException("Error finding metacard due to Federation issue",
                    e);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Metacard couldn't be found {}", metacardId, e);
            throw new ResourceNotFoundException("Query returned null metacard", e);
        } finally {
            LOGGER.trace("EXITING: getResourceOptions");
        }

        return optionsMap;
    }

    /**
     * Get the supported options from the {@link ResourceReader} that matches the scheme in the
     * specified {@link Metacard}'s URI. Only look in the local provider for the specified
     * {@link Metacard}.
     *
     * @param metacard the {@link Metacard} to get the supported options for
     * @return the {@link Set} of supported options for the metacard
     */
    @Deprecated
    private Set<String> getOptionsFromLocalProvider(Metacard metacard) {
        LOGGER.trace("ENTERING: getOptionsFromLocalProvider");
        Set<String> supportedOptions = Collections.emptySet();
        URI resourceUri = metacard.getResourceURI();
        for (ResourceReader reader : frameworkProperties.getResourceReaders()) {
            LOGGER.debug("reader id: {}", reader.getId());
            Set<String> rrSupportedSchemes = reader.getSupportedSchemes();
            String metacardScheme = resourceUri.getScheme();
            if (metacardScheme != null && rrSupportedSchemes.contains(metacardScheme)) {
                supportedOptions = reader.getOptions(metacard);
            }
        }

        LOGGER.trace("EXITING: getOptionsFromLocalProvider");
        return supportedOptions;
    }

    /**
     * Get the supported options from the {@link ResourceReader} that matches the scheme in the
     * specified {@link Metacard}'s URI. Only look in the specified source for the {@link Metacard}.
     *
     * @param metacard the {@link Metacard} to get the supported options for
     * @param sourceId the ID of the federated source to look for the {@link Metacard}
     * @return the {@link Set} of supported options for the metacard
     * @throws ResourceNotFoundException if the {@link Source} cannot be found for the source ID
     */
    @Deprecated
    private Set<String> getOptionsFromFederatedSource(Metacard metacard, String sourceId)
            throws ResourceNotFoundException {
        LOGGER.trace("ENTERING: getOptionsFromFederatedSource");

        FederatedSource source = frameworkProperties.getFederatedSources()
                .get(sourceId);

        if (source != null) {
            LOGGER.trace("EXITING: getOptionsFromFederatedSource");

            return source.getOptions(metacard);
        } else {
            String message = "Unable to find source corresponding to given site name: " + sourceId;
            LOGGER.trace("EXITING: getOptionsFromFederatedSource");

            throw new ResourceNotFoundException(message);
        }
    }

    private String getAttributeStringValue(Metacard mcard, String attribute) {
        Attribute attr = mcard.getAttribute(attribute);
        if (attr != null && attr.getValue() != null) {
            return attr.getValue()
                    .toString();
        }
        return "";
    }

    protected static class ResourceInfo {
        private Metacard metacard;

        private URI resourceUri;

        public ResourceInfo(Metacard metacard, URI uri) {
            this.metacard = metacard;
            this.resourceUri = uri;
        }

        public Metacard getMetacard() {
            return metacard;
        }

        public URI getResourceUri() {
            return resourceUri;
        }
    }
}
