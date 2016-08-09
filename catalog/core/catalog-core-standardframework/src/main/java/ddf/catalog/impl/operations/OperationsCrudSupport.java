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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.detect.DefaultProbDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.codice.ddf.platform.util.InputValidation;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.Constants;
import ddf.catalog.content.StorageException;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.StorageRequest;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.DefaultAttributeValueRegistry;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.history.Historian;
import ddf.catalog.impl.FrameworkProperties;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.Request;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.source.CatalogStore;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.util.impl.Requests;
import ddf.mime.MimeTypeResolutionException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.SubjectUtils;

public class OperationsCrudSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsCrudSupport.class);

    private static final Logger INGEST_LOGGER =
            LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

    // Inject properties
    private final FrameworkProperties frameworkProperties;

    private final QueryOperations queryOperations;

    private final SourceOperations sourceOperations;

    private Historian historian;

    public OperationsCrudSupport(FrameworkProperties frameworkProperties,
            QueryOperations queryOperations, SourceOperations sourceOperations) {
        this.frameworkProperties = frameworkProperties;
        this.queryOperations = queryOperations;
        this.sourceOperations = sourceOperations;
    }

    public void setHistorian(Historian historian) {
        this.historian = historian;
    }

    //
    // CRUD Helper methods
    //
    void prepareStorageRequest(StorageRequest storageRequest,
            Supplier<List<ContentItem>> getContentItems)
            throws IngestException, SourceUnavailableException {
        validateStorageRequest(storageRequest, getContentItems);

        queryOperations.setFlagsOnRequest(storageRequest);

        if (Requests.isLocal(storageRequest) && (!sourceOperations.isSourceAvailable(
                sourceOperations.getCatalog())
                || !isStorageAvailable(sourceOperations.getStorage()))) {
            String message = "Local provider is not available, cannot perform storage operation.";
            SourceUnavailableException sourceUnavailableException = new SourceUnavailableException(
                    message);
            if (INGEST_LOGGER.isWarnEnabled()) {
                INGEST_LOGGER.warn(message, sourceUnavailableException);
            }
            throw sourceUnavailableException;
        }
    }

    void handleStorageException(StorageRequest storageRequest, String id, Exception ex)
            throws IngestException {
        if (storageRequest != null) {
            try {
                sourceOperations.getStorage()
                        .rollback(storageRequest);
            } catch (StorageException e1) {
                LOGGER.error("Unable to remove temporary content for id: " + storageRequest.getId(),
                        e1);
            }
        }
        throw new IngestException("Unable to store products for request: " + id, ex);

    }

    void commitAndCleanup(StorageRequest storageRequest, Optional<String> historianTransactionKey,
            HashMap<String, Path> tmpContentPaths) {
        if (storageRequest != null) {
            try {
                sourceOperations.getStorage()
                        .commit(storageRequest);
                historianTransactionKey.ifPresent(historian::commit);
            } catch (StorageException e) {
                LOGGER.error("Unable to commit content changes for id: {}",
                        storageRequest.getId(),
                        e);
                try {
                    sourceOperations.getStorage()
                            .rollback(storageRequest);
                } catch (StorageException e1) {
                    LOGGER.error("Unable to remove temporary content for id: {}",
                            storageRequest.getId(),
                            e1);
                } finally {
                    try {
                        historianTransactionKey.ifPresent(historian::rollback);
                    } catch (RuntimeException re) {
                        LOGGER.error(
                                "Unable to commit versioned items for historian transaction: {}",
                                historianTransactionKey.orElseGet(String::new),
                                re);
                    }
                }
            }
        }

        tmpContentPaths.values()
                .forEach(path -> FileUtils.deleteQuietly(path.toFile()));
        tmpContentPaths.clear();

    }

    void setDefaultValues(Metacard metacard) {
        MetacardType metacardType = metacard.getMetacardType();
        DefaultAttributeValueRegistry registry =
                frameworkProperties.getDefaultAttributeValueRegistry();

        metacardType.getAttributeDescriptors()
                .stream()
                .map(AttributeDescriptor::getName)
                .filter(attributeName -> hasNoValue(metacard.getAttribute(attributeName)))
                .forEach(attributeName -> {
                    registry.getDefaultValue(metacardType.getName(), attributeName)
                            .ifPresent(defaultValue -> metacard.setAttribute(new AttributeImpl(
                                    attributeName,
                                    defaultValue)));
                });
    }

    void generateMetacardAndContentItems(StorageRequest storageRequest,
            List<ContentItem> incomingContentItems, Map<String, Metacard> metacardMap,
            List<ContentItem> contentItems, Map<String, Path> tmpContentPaths)
            throws IngestException {
        for (ContentItem contentItem : incomingContentItems) {
            try {
                Path tmpPath = null;
                long size;
                try (InputStream inputStream = contentItem.getInputStream()) {
                    if (inputStream == null) {
                        throw new IngestException(
                                "Could not copy bytes of content message.  Message was NULL.");
                    }

                    String sanitizedFilename =
                            InputValidation.sanitizeFilename(contentItem.getFilename());
                    tmpPath = Files.createTempFile(FilenameUtils.getBaseName(sanitizedFilename),
                            FilenameUtils.getExtension(sanitizedFilename));
                    Files.copy(inputStream, tmpPath, StandardCopyOption.REPLACE_EXISTING);
                    size = Files.size(tmpPath);
                    tmpContentPaths.put(contentItem.getId(), tmpPath);
                } catch (IOException e) {
                    if (tmpPath != null) {
                        FileUtils.deleteQuietly(tmpPath.toFile());
                    }
                    throw new IngestException("Could not copy bytes of content message.", e);
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
                        .forEach(path -> FileUtils.deleteQuietly(path.toFile()));
                tmpContentPaths.clear();
                throw new IngestException("Could not create metacard.", e);
            }
        }
    }

    List<CatalogStore> getCatalogStoresForRequest(Request request,
            Set<ProcessingDetails> exceptions) {
        if (!isCatalogStoreRequest(request)) {
            return Collections.emptyList();
        }

        List<CatalogStore> results = new ArrayList<>(request.getStoreIds()
                .size());
        for (String destination : request.getStoreIds()) {
            if (frameworkProperties.getCatalogStoresMap()
                    .containsKey(destination)) {
                results.add(frameworkProperties.getCatalogStoresMap()
                        .get(destination));
            } else if (sourceOperations.getCatalog() == null
                    || !destination.equals(sourceOperations.getCatalog()
                    .getId())) {
                exceptions.add(new ProcessingDetailsImpl(destination,
                        null,
                        "CatalogStore does not exist"));
            }
        }
        return results;
    }

    boolean isCatalogStoreRequest(Request request) {
        return request != null && CollectionUtils.isNotEmpty(request.getStoreIds()) && (
                request.getStoreIds()
                        .size() > 1 || sourceOperations.getCatalog() == null
                        || !request.getStoreIds()
                        .contains(sourceOperations.getCatalog()
                                .getId()));
    }

    boolean isStorageAvailable(StorageProvider storageProvider) {
        if (storageProvider == null) {
            LOGGER.warn("storageProvider is null, therefore not available");
            return false;
        }
        return true;
    }

    Filter getFilterWithAdditionalFilters(List<Filter> originalFilter) {
        return frameworkProperties.getFilterBuilder()
                .allOf(getTagsQueryFilter(),
                        frameworkProperties.getValidationQueryFactory()
                                .getFilterWithValidationFilter(),
                        frameworkProperties.getFilterBuilder()
                                .anyOf(originalFilter));
    }

    String getAttributeStringValue(Metacard mcard, String attribute) {
        Attribute attr = mcard.getAttribute(attribute);
        if (attr != null && attr.getValue() != null) {
            return attr.getValue()
                    .toString();
        }
        return "";
    }

    //
    // Private helper methods
    //
    private boolean hasNoValue(Attribute attribute) {
        return attribute == null || attribute.getValue() == null;
    }

    private Metacard generateMetacard(String mimeTypeRaw, String id, String fileName,
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

        String name = Optional.ofNullable(SubjectUtils.getName(subject))
                .orElse("");

        generatedMetacard.setAttribute(new AttributeImpl(Metacard.POINT_OF_CONTACT, name));

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
                    String line = bufferedReader.lines()
                            .map(String::trim)
                            .filter(StringUtils::isNotEmpty)
                            .findFirst()
                            .orElse("");

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

    /**
     * Validates that the {@link StorageRequest} is non-null and has a non-empty list of
     * {@link ContentItem}s in it.
     *
     * @param request the {@link StorageRequest}
     * @throws IngestException if the {@link StorageRequest} is null, or request has a null or empty list of
     *                         {@link ContentItem}s
     */
    private void validateStorageRequest(StorageRequest request,
            Supplier<List<ContentItem>> getContentItems) throws IngestException {
        if (request == null) {
            throw new IngestException("StorageRequest was null.");
        }
        List<ContentItem> contentItems = getContentItems.get();
        if (CollectionUtils.isEmpty(contentItems)) {
            throw new IngestException("Cannot perform ingest or update with null/empty entry list.");
        }
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

}
