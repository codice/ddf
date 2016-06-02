package ddf.catalog.history;

import static com.google.common.base.Strings.isNullOrEmpty;
import static ddf.catalog.Constants.CONTENT_PATHS;
import static ddf.catalog.core.versioning.MetacardVersion.HISTORY_METACARDS_PROPERTY;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.shiro.SecurityUtils;
import org.codice.ddf.security.common.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;

import ddf.catalog.CatalogFramework;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.core.versioning.MetacardVersion;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import ddf.catalog.source.IngestException;
import ddf.security.Subject;

public class Historian {
    public static final String ALREADY_VERSIONED = "already-versioned";

    private static final Logger LOGGER = LoggerFactory.getLogger(Historian.class);

    private static final String HISTORY_METACARDS = "history-metacards";

    private boolean historyEnabled = true;

    public Historian() {
    }

    private final Cache<String, List<Callable<Boolean>>> staged = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    public CreateRequest versionCreate(CreateRequest createRequest) {
        if (!historyEnabled) {
            return createRequest;
        }

        if (createRequest.getProperties()
                .getOrDefault(ALREADY_VERSIONED, "")
                .equals(true)) {
            return createRequest;
        }

        List<Metacard> updatedMetacardList = createRequest.getMetacards()
                .stream()
                .filter(MetacardVersion::isNotVersion)
                .map(m -> new MetacardVersion(m,
                        MetacardVersion.Action.CREATED,
                        SecurityUtils.getSubject()))
                .collect(Collectors.toList());
        updatedMetacardList.addAll(createRequest.getMetacards());

        return new CreateRequestImpl(updatedMetacardList, createRequest.getProperties());
    }

    public UpdateResponse versionUpdate(UpdateResponse updateResponse,
            CatalogFramework catalogFramework, StorageProvider storage) {
        if (!historyEnabled) {
            return updateResponse;
        }

        if (updateResponse.getProperties()
                .getOrDefault(ALREADY_VERSIONED, false)
                .equals(true)) {
            return updateResponse;
        }

        List<Metacard> inputMetacards = updateResponse.getUpdatedMetacards()
                .stream()
                .map(Update::getNewMetacard)
                .collect(Collectors.toList());

        CreateResponse createdHistory = getVersionedMetacards(inputMetacards,
                MetacardVersion.Action.UPDATED,
                catalogFramework);
        if (createdHistory == null) {
            return updateResponse;
        }

        Map<String, Serializable> properties = updateResponse.getProperties() != null ?
                updateResponse.getProperties() :
                new HashMap<>();
        properties.put(HISTORY_METACARDS,
                new ArrayList<Serializable>(createdHistory.getCreatedMetacards()));

        return new UpdateResponseImpl(updateResponse.getRequest(),
                properties,
                updateResponse.getUpdatedMetacards(),
                updateResponse.getProcessingErrors());
    }

    public CreateStorageRequest versionStorageCreate(CreateStorageRequest createStorageRequest) throws IngestException {
        if (!historyEnabled) {
            return createStorageRequest;
        }

        if (createStorageRequest.getProperties()
                .getOrDefault(ALREADY_VERSIONED, "")
                .equals(true)) {
            return createStorageRequest;
        }

        Map<String, Path> tmpContentPaths = (Map<String, Path>) createStorageRequest.getProperties()
                .getOrDefault(CONTENT_PATHS, new HashMap<>());
        List<ContentItem> contentItems = createStorageRequest.getContentItems();

        List<ContentItem> newContentItems = createStorageRequest.getContentItems()
                .stream()
                .map(ContentItem::getMetacard)
                .distinct()
                .flatMap(mc -> getVersionedContent(mc,
                        contentItems,
                        MetacardVersion.Action.CREATED_CONTENT,
                        tmpContentPaths).stream())
                .collect(Collectors.toList());

        newContentItems.addAll(createStorageRequest.getContentItems());
        createStorageRequest.getProperties().put(ALREADY_VERSIONED, true);

        return new CreateStorageRequestImpl(newContentItems,
                createStorageRequest.getId(),
                createStorageRequest.getProperties());
    }

    // TODO (RCZ) - should i throw the IOException or catch and rethrow as storage
    public String versionUpdateStorage(UpdateStorageResponse updateStorageResponse,
            HashMap<String, Path> tmpContentPaths, CatalogFramework catalogFramework,
            StorageProvider storage) throws IOException {
        if (!historyEnabled) {
            return null;
        }

        String transactionKey = UUID.randomUUID()
                .toString();

        List<Callable<Boolean>> preStaged = new ArrayList<>(2);
        staged.put(transactionKey, preStaged);

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
            CreateStorageResponse createStorageResponse = storage.create(createStorageRequest);
            storage.commit(createStorageRequest);
            return createStorageResponse.getProcessingErrors() != null
                    && createStorageResponse.getProcessingErrors()
                    .isEmpty();
        });

        preStaged.add(() -> {
            Map<String, Serializable> props = new HashMap<>();
            props.put(ALREADY_VERSIONED, true);
            CreateResponse createResponse = catalogFramework.create(new CreateRequestImpl(
                    historyMetacards,
                    props));
            return createResponse.getProcessingErrors() != null
                    && createResponse.getProcessingErrors()
                    .isEmpty();
        });

        return transactionKey;
    }

    public List<Exception> commit(String historianTransactionKey) {
        List<Callable<Boolean>> ops = staged.getIfPresent(historianTransactionKey);
        if (ops == null) {
            return null;
        }

        staged.invalidate(historianTransactionKey);

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
        staged.invalidate(historianTransactionKey);
    }

    public boolean isHistoryEnabled() {
        return historyEnabled;
    }

    public void setHistoryEnabled(boolean historyEnabled) {
        this.historyEnabled = historyEnabled;
    }

    private CreateResponse getVersionedMetacards(List<Metacard> metacards,
            final MetacardVersion.Action action, CatalogFramework catalogFramework) {
        final List<Metacard> versionedMetacards = metacards.stream()
                .filter(metacard -> !metacard.getMetacardType()
                        .equals(MetacardVersion.getMetacardVersionType()))
                .map(metacard -> new MetacardVersion(metacard, action, SecurityUtils.getSubject()))
                .collect(Collectors.toList());

        if (versionedMetacards.isEmpty()) {
            return null;
        }

        Subject system = Security.getInstance()
                .getSystemSubject();
        if (system == null) {
            LOGGER.warn("Could not get system subject to create versioned metacards.");
            return null;
        }

        return system.execute(() -> catalogFramework.create(new CreateRequestImpl(metacards)));
    }

    public CreateResponse removeHistoryItems(CreateResponse input) {
        if (!historyEnabled) {
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

        // TODO (RCZ) - this slightly scares me, I shouldn't be coding against implementation detail
        // And yet you still did it ryan... Hmm good point. ¯\_(ツ)_/¯
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
