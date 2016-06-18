package ddf.catalog.history;

import static com.google.common.base.Strings.isNullOrEmpty;
import static ddf.catalog.Constants.CONTENT_PATHS;
import static ddf.catalog.core.versioning.MetacardVersion.HISTORY_METACARDS_PROPERTY;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;
import com.google.common.io.Files;

import ddf.catalog.CatalogFramework;
import ddf.catalog.content.StorageException;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.core.versioning.MetacardVersion;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.Operation;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;

public class Historian {
    public static final String ALREADY_VERSIONED = "already-versioned";

    private static final Logger LOGGER = LoggerFactory.getLogger(Historian.class);

    private static final String HISTORY_METACARDS = "history-metacards";

    private boolean historyEnabled = true;

    private List<StorageProvider> storageProviders;

    private List<CatalogProvider> catalogProviders;

    private CatalogFramework catalogFramework;

    public Historian() {
    }

    private final Map<String, List<Callable<Boolean>>> staged = new HashMap<>();

    public CreateRequest versionCreate(CreateRequest createRequest) throws IngestException {
        if (doSkip(createRequest)) {
            return createRequest;
        }

        List<Metacard> updatedMetacardList = createRequest.getMetacards()
                .stream()
                .filter(MetacardVersion::isNotVersion)
                .map(m -> new MetacardVersion(m,
                        MetacardVersion.Action.CREATED,
                        SecurityUtils.getSubject()))
                .collect(Collectors.toList());

        if (!updatedMetacardList.isEmpty()) {
            catalogProvider().create(new CreateRequestImpl(updatedMetacardList));
        }

        return createRequest;
    }

    public UpdateResponse versionUpdate(UpdateResponse updateResponse)
            throws SourceUnavailableException, IngestException {
        if (doSkip(updateResponse)) {
            return updateResponse;
        }

        List<Metacard> inputMetacards = updateResponse.getUpdatedMetacards()
                .stream()
                .map(Update::getNewMetacard)
                .collect(Collectors.toList());

        CreateResponse createdHistory = getVersionedMetacards(inputMetacards,
                MetacardVersion.Action.UPDATED);
        if (createdHistory == null) {
            return updateResponse;
        }

        if (updateResponse.getProperties() != null) {
            updateResponse.getProperties()
                    .put(HISTORY_METACARDS, new ArrayList<>(createdHistory.getCreatedMetacards()));
        }

        return updateResponse;
    }

    public CreateStorageRequest versionStorageCreate(CreateStorageRequest createStorageRequest)
            throws IngestException, StorageException {
        if (doSkip(createStorageRequest)) {
            return createStorageRequest;
        }

        @SuppressWarnings("unchecked")
        Map<String, Path> tmpContentPaths = (Map<String, Path>) createStorageRequest.getProperties()
                .getOrDefault(CONTENT_PATHS, new HashMap<>());

        List<ContentItem> newContentItems = createStorageRequest.getContentItems()
                .stream()
                .map(ContentItem::getMetacard)
                .distinct()
                .flatMap(mc -> getVersionedContent(mc,
                        createStorageRequest.getContentItems(),
                        MetacardVersion.Action.CREATED_CONTENT,
                        tmpContentPaths).stream())
                .collect(Collectors.toList());

        newContentItems.forEach(ci -> {
            if (ci.getMetacard()
                    .getResourceURI() == null) {
                ci.getMetacard()
                        .setAttribute(new AttributeImpl(Metacard.RESOURCE_URI, ci.getUri()));
                try {
                    ci.getMetacard()
                            .setAttribute(new AttributeImpl(Metacard.RESOURCE_SIZE, ci.getSize()));
                } catch (IOException e) {
                    LOGGER.warn("Could not get size of content item", e);
                }
            }
        });

        CreateStorageRequestImpl versionStorageRequest = new CreateStorageRequestImpl(
                newContentItems,
                createStorageRequest.getId(),
                createStorageRequest.getProperties());

        storageProvider().create(versionStorageRequest);
        storageProvider().commit(versionStorageRequest);
        catalogProvider().create(new CreateRequestImpl(newContentItems.stream()
                .map(ContentItem::getMetacard)
                .distinct()
                .collect(Collectors.toList())));

        createStorageRequest.getProperties()
                .put(ALREADY_VERSIONED, true);
        return createStorageRequest;
    }

    public Optional<String> versionUpdateStorage(UpdateStorageRequest streamUpdateRequest,
            UpdateStorageResponse updateStorageResponse, HashMap<String, Path> tmpContentPaths)
            throws IOException {
        if (doSkip(updateStorageResponse)) {
            return Optional.empty();
        }

        String transactionKey = UUID.randomUUID()
                .toString();

        List<Callable<Boolean>> preStaged = new ArrayList<>(2);
        staged.put(transactionKey, preStaged);

        streamUpdateRequest.getProperties()
                .put(ALREADY_VERSIONED, true);
        updateStorageResponse.getProperties()
                .put(ALREADY_VERSIONED, true);

        List<ContentItem> versionedContentItems = updateStorageResponse.getUpdatedContentItems()
                .stream()
                .map(ContentItem::getMetacard)
                .distinct()
                .flatMap(mc -> getVersionedContent(mc,
                        updateStorageResponse.getUpdatedContentItems(),
                        MetacardVersion.Action.UPDATED_CONTENT,
                        tmpContentPaths).stream())
                .collect(Collectors.toList());

        List<Metacard> historyMetacards = versionedContentItems.stream()
                .map(ContentItem::getMetacard)
                .distinct()
                .collect(Collectors.toList());

        // These are being staged for delayed execution, to be ran after the storage/catalog has
        // successfully committed/updated
        preStaged.add(() -> {
            Map<String, Serializable> props = new HashMap<>();
            props.put(ALREADY_VERSIONED, true);
            CreateStorageRequestImpl createStorageRequest = new CreateStorageRequestImpl(
                    versionedContentItems,
                    props);
            CreateStorageResponse createStorageResponse = storageProvider().create(
                    createStorageRequest);
            storageProvider().commit(createStorageRequest);
            createStorageResponse.getCreatedContentItems()
                    .forEach((ci) -> {
                        // TODO (RCZ) - Should this be moved into FileSystemStorageProvider (with the update)
                        // or should FSSP update be moved out?
                        try {
                            ci.getMetacard()
                                    .setAttribute(new AttributeImpl(Metacard.RESOURCE_URI,
                                            ci.getUri()));
                            ci.getMetacard()
                                    .setAttribute(new AttributeImpl(Metacard.RESOURCE_SIZE,
                                            ci.getSize()));
                        } catch (IOException e) {
                            LOGGER.warn("Could not get size", e);
                        }

                    });
            return createStorageResponse.getProcessingErrors() != null
                    && createStorageResponse.getProcessingErrors()
                    .isEmpty();
        });

        preStaged.add(() -> {
            Map<String, Serializable> props = new HashMap<>();
            props.put(ALREADY_VERSIONED, true);
            CreateResponse createResponse = catalogProvider().create(new CreateRequestImpl(
                    historyMetacards,
                    props));
            return createResponse.getProcessingErrors() != null
                    && createResponse.getProcessingErrors()
                    .isEmpty();
        });

        return Optional.of(transactionKey);
    }

    public CreateResponse removeHistoryItems(CreateResponse input) {
        if (doSkip(input)) {
            return input;
        }

        List<Metacard> historyMetacards = input.getCreatedMetacards()
                .stream()
                .filter(m -> MetacardVersion.getMetacardVersionType()
                        .getName()
                        .equals(m.getMetacardType()
                                .getName()))
                .collect(Collectors.toList());

        if (historyMetacards.isEmpty()) {
            return input;
        }

        List<Metacard> resultMetacards = new ArrayList<>(input.getCreatedMetacards());
        resultMetacards.removeAll(historyMetacards);

        Map<String, Serializable> properties =
                input.getProperties() != null ? input.getProperties() : new HashMap<>();
        properties.put(HISTORY_METACARDS_PROPERTY, new ArrayList<Serializable>(historyMetacards));

        return new CreateResponseImpl(input.getRequest(),
                properties,
                resultMetacards,
                input.getProcessingErrors());
    }

    public List<Exception> commit(String historianTransactionKey) {
        List<Callable<Boolean>> ops = staged.remove(historianTransactionKey);
        if (ops == null) {
            LOGGER.warn("There was no operations staged for historian transaction key [{}]",
                    historianTransactionKey);
            return null;
        }

        ArrayList<Exception> exceptions = new ArrayList<>();
        for (Callable<Boolean> op : ops) {
            try {
                op.call();
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        return exceptions;
    }

    public void rollback(String historianTransactionKey) {
        staged.remove(historianTransactionKey);
    }

    public boolean isHistoryEnabled() {
        return historyEnabled;
    }

    public void setHistoryEnabled(boolean historyEnabled) {
        this.historyEnabled = historyEnabled;
    }

    public List<StorageProvider> getStorageProviders() {
        return storageProviders;
    }

    public void setStorageProviders(List<StorageProvider> storageProviders) {
        this.storageProviders = storageProviders;
    }

    public List<CatalogProvider> getCatalogProviders() {
        return catalogProviders;
    }

    public void setCatalogProviders(List<CatalogProvider> catalogProviders) {
        this.catalogProviders = catalogProviders;
    }

    public CatalogFramework getCatalogFramework() {
        return catalogFramework;
    }

    public void setCatalogFramework(CatalogFramework catalogFramework) {
        this.catalogFramework = catalogFramework;
    }

    private StorageProvider storageProvider() {
        return storageProviders.stream()
                .findFirst()
                .orElse(null);
    }

    private CatalogProvider catalogProvider() {
        return catalogProviders.stream()
                .findFirst()
                .orElse(null);
    }

    private CreateResponse getVersionedMetacards(List<Metacard> metacards,
            final MetacardVersion.Action action)
            throws SourceUnavailableException, IngestException {
        final List<Metacard> versionedMetacards = metacards.stream()
                .filter(MetacardVersion::isNotVersion)
                .map(metacard -> new MetacardVersion(metacard, action, SecurityUtils.getSubject()))
                .collect(Collectors.toList());

        if (versionedMetacards.isEmpty()) {
            return null;
        }

        return catalogProvider().create(new CreateRequestImpl(versionedMetacards));
    }

    private List<ContentItem> getVersionedContent(Metacard root, List<ContentItem> contentItems,
            MetacardVersion.Action versionAction, Map<String, Path> tmpContentPaths) {
        String id = root.getId();
        MetacardVersion rootVersion = new MetacardVersion(root,
                versionAction,
                SecurityUtils.getSubject());

        List<ContentItem> relatedContent = contentItems.stream()
                .filter(ci -> ci.getId()
                        .equals(id))
                .collect(Collectors.toList());

        ContentItem rootItem = relatedContent.stream()
                .filter(ci -> !isNullOrEmpty(ci.getUri()))
                .filter(ci -> !ci.getUri()
                        .contains("#"))
                .findAny()
                .orElseThrow(() -> new RuntimeException(
                        "Could not find root content item for: " + id));

        List<ContentItem> derivedContent = relatedContent.stream()
                .filter(ci -> !ci.equals(rootItem))
                .collect(Collectors.toList());

        List<ContentItem> resultItems = new ArrayList<>();

        { // version root content item
            long size = 0;
            try {
                size = rootItem.getSize();
            } catch (IOException e) {
                LOGGER.warn("Could not get size of item [{}].", rootItem.getId(), e);
            }
            resultItems.add(new ContentItemImpl(rootVersion.getId(),
                    Files.asByteSource(tmpContentPaths.get(rootItem.getId())
                            .toFile()),
                    rootItem.getMimeTypeRawData(),
                    rootItem.getFilename(),
                    size,
                    rootVersion));
        }

        { // version derived content items
            for (ContentItem contentItem : derivedContent) {
                long size = 0;
                try {
                    size = contentItem.getSize();
                } catch (IOException e) {
                    LOGGER.warn("Could not get size of item [{}].", rootItem.getId(), e);
                }
                resultItems.add(new ContentItemImpl(rootVersion.getId(),
                        contentItem.getQualifier(),
                        new WrappedByteSource(contentItem),
                        contentItem.getMimeTypeRawData(),
                        contentItem.getFilename(),
                        size,
                        rootVersion));
            }
        }

        return resultItems;
    }

    private boolean doSkip(@Nullable Operation op) {
        return doSkip(Optional.ofNullable(op.getProperties())
                .orElse(Collections.emptyMap()));
    }

    private boolean doSkip(Map<String, Serializable> properties) {
        return !historyEnabled || properties.getOrDefault(ALREADY_VERSIONED, false)
                .equals(true);
    }

    private static class WrappedByteSource extends ByteSource {
        private ContentItem contentItem;

        private WrappedByteSource(ContentItem contentItem) {
            this.contentItem = contentItem;
        }

        @Override
        public InputStream openStream() throws IOException {
            return contentItem.getInputStream();
        }
    }
}
