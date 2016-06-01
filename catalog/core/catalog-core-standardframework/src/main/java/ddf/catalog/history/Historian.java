package ddf.catalog.history;

import static ddf.catalog.core.versioning.MetacardVersion.HISTORY_METACARDS_PROPERTY;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
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
                .filter(m -> !MetacardVersion.getMetacardVersionType()
                        .equals(m.getMetacardType()))
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
                .getOrDefault(ALREADY_VERSIONED, "")
                .equals(true)) {
            return updateResponse;
        }
        List<Metacard> inputMetacards = updateResponse.getUpdatedMetacards()
                .stream()
                .map(Update::getNewMetacard)
                .collect(Collectors.toList());
        CreateResponse createdHistory = null;

        createdHistory = getVersionedMetacards(inputMetacards,
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

    public CreateStorageRequest versionStorageCreate(CreateStorageRequest createStorageRequest,
            List<ContentItem> contentItems, Map<String, MetacardVersion> versions)
            throws IngestException {
        if (!historyEnabled) {
            return createStorageRequest;
        }
        if (createStorageRequest.getProperties()
                .getOrDefault(ALREADY_VERSIONED, "")
                .equals(true)) {
            return createStorageRequest;
        }

        List<ContentItem> newContentItems = new ArrayList<>();
        for (ContentItem contentItem : contentItems) {
            MetacardVersion versionedMetacard = versions.get(contentItem.getId());
            try {
                newContentItems.add(new ContentItemImpl(versionedMetacard.getId(),
                        new WrappedByteSource(contentItem),
                        contentItem.getMimeTypeRawData(),
                        contentItem.getFilename(),
                        contentItem.getSize(),
                        versionedMetacard));
            } catch (IOException e) {
                throw new IngestException(e);
            }
        }
        newContentItems.addAll(createStorageRequest.getContentItems());
        return new CreateStorageRequestImpl(newContentItems,
                createStorageRequest.getId(),
                createStorageRequest.getProperties());
    }

    // TODO (RCZ) - should i throw the IOException or catch and rethrow as storage
    public String versionUpdateStorage(UpdateStorageResponse updateStorageResponse,
            HashMap<String, Path> tmpContentPaths, CatalogFramework catalogFramework,
            StorageProvider storage) throws IOException {
        String transactionKey = UUID.randomUUID()
                .toString();
        List<Callable<Boolean>> preStaged = new ArrayList<>();
        staged.put(transactionKey, preStaged);
        List<Metacard> historyMetacards = new ArrayList<>();
        List<ContentItem> versionedContentItems = new ArrayList<>();

        for (ContentItem contentItem : updateStorageResponse.getUpdatedContentItems()) {
            MetacardVersion updatedMetacard = new MetacardVersion(contentItem.getMetacard(),
                    MetacardVersion.Action.UPDATED_CONTENT,
                    SecurityUtils.getSubject());
            historyMetacards.add(updatedMetacard);
            ContentItem versionedContent = new ContentItemImpl(updatedMetacard.getId(),
                    Files.asByteSource(tmpContentPaths.get(contentItem.getId()).toFile()),
                    contentItem.getMimeTypeRawData(),
                    contentItem.getFilename(),
                    contentItem.getSize(),
                    updatedMetacard);
            try {
                updatedMetacard.setResourceURI(new URI(versionedContent.getUri()));
                updatedMetacard.setResourceSize(String.valueOf(versionedContent.getSize()));
            } catch (URISyntaxException e) {
                LOGGER.warn("Could not set resource uri for metacard", e);
            }
            versionedContentItems.add(versionedContent);
        }
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
                // TODO (RCZ) - do anything special?
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








