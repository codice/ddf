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
 **/
package org.codice.ddf.catalog.content.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.content.StorageException;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.operation.DeleteStorageRequest;
import ddf.catalog.content.operation.DeleteStorageResponse;
import ddf.catalog.content.operation.ReadStorageRequest;
import ddf.catalog.content.operation.ReadStorageResponse;
import ddf.catalog.content.operation.StorageRequest;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.content.operation.impl.CreateStorageResponseImpl;
import ddf.catalog.content.operation.impl.DeleteStorageResponseImpl;
import ddf.catalog.content.operation.impl.ReadStorageResponseImpl;
import ddf.catalog.content.operation.impl.UpdateStorageResponseImpl;
import ddf.catalog.data.Metacard;
import ddf.mime.MimeTypeMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * File system storage provider.
 */
public class FileSystemStorageProvider implements StorageProvider {

    public static final String CONTENT_URI_PREFIX = "content:";

    public static final String DEFAULT_CONTENT_REPOSITORY = "content";

    public static final String DEFAULT_CONTENT_STORE = "store";

    public static final String DEFAULT_TMP = "tmp";

    public static final String KARAF_HOME = "karaf.home";

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemStorageProvider.class);

    /**
     * Mapper for file extensions-to-mime types (and vice versa)
     */
    private MimeTypeMapper mimeTypeMapper;

    /**
     * Root directory for entire content repository
     */
    private Path baseContentDirectory;

    private Path baseContentTmpDirectory;

    private Map<String, List<Metacard>> deletionMap = new ConcurrentHashMap<>();

    /**
     * Default constructor, invoked by blueprint.
     */
    public FileSystemStorageProvider() {
        LOGGER.info("File System Provider initializing...");
    }

    @Override
    public CreateStorageResponse create(CreateStorageRequest createRequest)
            throws StorageException {
        LOGGER.trace("ENTERING: create");

        List<ContentItem> contentItems = createRequest.getContentItems();

        List<ContentItem> createdContentItems = new ArrayList<>(createRequest.getContentItems()
                .size());

        for (ContentItem contentItem : contentItems) {

            String id = contentItem.getId();

            Path contentIdDir = Paths.get(baseContentTmpDirectory.toAbsolutePath()
                    .toString(), createRequest.getId(), id);

            try {
                Path contentDirectory = Files.createDirectories(contentIdDir);

                createdContentItems.add(generateContentFile(contentItem, contentDirectory));
            } catch (IOException e) {
                throw new StorageException(e);
            }
        }

        CreateStorageResponse response = new CreateStorageResponseImpl(createRequest,
                createdContentItems);

        LOGGER.trace("EXITING: create");

        return response;
    }

    @Override
    public ReadStorageResponse read(ReadStorageRequest readRequest) throws StorageException {
        LOGGER.trace("ENTERING: read");

        URI uri = readRequest.getResourceUri();
        String id = uri.getSchemeSpecificPart();
        Path file = getContentFilePath(id);

        if (file == null) {
            throw new StorageException("Unable to find file for content ID: " + id);
        }

        String extension = FilenameUtils.getExtension(file.getFileName()
                .toString());

        String mimeType;

        try (InputStream fileInputStream = Files.newInputStream(file)) {
            mimeType = mimeTypeMapper.guessMimeType(fileInputStream, extension);
        } catch (Exception e) {
            LOGGER.warn("Could not determine mime type for file extension = {}; defaulting to {}",
                    extension, DEFAULT_MIME_TYPE);
            mimeType = DEFAULT_MIME_TYPE;
        }
        if (mimeType == null || DEFAULT_MIME_TYPE.equals(mimeType)) {
            try {
                mimeType = Files.probeContentType(file);
            } catch (IOException e) {
                LOGGER.warn("Unable to determine mime type using Java Files service.", e);
                mimeType = DEFAULT_MIME_TYPE;
            }
        }

        LOGGER.debug("mimeType = {}", mimeType);
        long size = 0;
        try {
            size = Files.size(file);
        } catch (IOException e) {
            LOGGER.warn("Unable to retrieve size of file: {}", file.toAbsolutePath().toString(), e);
        }
        ContentItem returnItem = new ContentItemImpl(id,
                com.google.common.io.Files.asByteSource(file.toFile()), mimeType,
                file.toAbsolutePath()
                        .toString(), size, null);
        return new ReadStorageResponseImpl(readRequest, returnItem);

    }

    @Override
    public UpdateStorageResponse update(UpdateStorageRequest updateRequest)
            throws StorageException {
        LOGGER.trace("ENTERING: update");

        List<ContentItem> contentItems = updateRequest.getContentItems();

        List<ContentItem> updatedItems = new ArrayList<>(updateRequest.getContentItems()
                .size());

        for (ContentItem contentItem : contentItems) {

            String id = contentItem.getId();

            Path contentIdDir = Paths.get(baseContentTmpDirectory.toAbsolutePath()
                    .toString(), updateRequest.getId(), id);

            try {
                updatedItems.add(generateContentFile(contentItem, contentIdDir));
            } catch (IOException e) {
                throw new StorageException(e);
            }
        }

        UpdateStorageResponse response = new UpdateStorageResponseImpl(updateRequest, updatedItems);

        LOGGER.trace("EXITING: update");

        return response;
    }

    @Override
    public synchronized DeleteStorageResponse delete(DeleteStorageRequest deleteRequest)
            throws StorageException {
        LOGGER.trace("ENTERING: delete");

        List<Metacard> itemsToBeDeleted = new ArrayList<>();

        List<ContentItem> deletedContentItems = new ArrayList<>(deleteRequest.getMetacards()
                .size());
        for (Metacard metacard : deleteRequest.getMetacards()) {
            LOGGER.debug("File to be deleted: {}", metacard.getId());

            String[] parts = getContentFilePathParts(metacard.getId());
            Path contentIdDir = Paths.get(baseContentDirectory.toAbsolutePath()
                    .toString(), parts[0], parts[1], parts[2]);
            if (Files.exists(contentIdDir)) {
                ContentItemImpl deletedContentItem = new ContentItemImpl(metacard.getId(), null, "", "",
                        0, metacard);
                String contentUri = CONTENT_URI_PREFIX + deletedContentItem.getId();
                LOGGER.debug("contentUri = {}", contentUri);
                deletedContentItem.setUri(contentUri);
                deletedContentItems.add(deletedContentItem);
                itemsToBeDeleted.add(metacard);
            }
        }

        deletionMap.put(deleteRequest.getId(), itemsToBeDeleted);

        DeleteStorageResponse response = new DeleteStorageResponseImpl(deleteRequest,
                deletedContentItems);

        LOGGER.trace("EXITING: delete");

        return response;
    }

    @Override
    public synchronized void commit(StorageRequest request) throws StorageException {
        String id = request.getId();
        Path requestIdDir = Paths.get(baseContentTmpDirectory.toAbsolutePath()
                .toString(), id);
        if (Files.exists(requestIdDir)) {
            commitUpdates(request, id, requestIdDir);
        } else if (deletionMap.containsKey(id)) {
            commitDeletes(request, id);
        } else {
            LOGGER.warn("Nothing to commit for request: {}", request.getId());
        }
    }

    private void commitDeletes(StorageRequest request, String id) throws StorageException {
        List<Metacard> itemsToBeDeleted = deletionMap.get(id);
        try {
            for (Metacard metacard : itemsToBeDeleted) {
                LOGGER.debug("File to be deleted: {}", metacard.getId());

                String metacardId = metacard.getId();

                String[] parts = getContentFilePathParts(metacardId);

                Path contentIdDir = Paths.get(baseContentDirectory.toAbsolutePath()
                        .toString(), parts[0], parts[1], parts[2]);

                if (!Files.exists(contentIdDir)) {
                    throw new StorageException("File doesn't exist for id: " + metacard.getId());
                }

                try {
                    List<Path> files = listPaths(contentIdDir);
                    for (Path file : files) {
                        Files.deleteIfExists(file);
                    }
                    Files.deleteIfExists(contentIdDir);
                    Path part1 = contentIdDir.getParent();
                    List<Path> part1Files = listPaths(part1);
                    if (part1Files.size() == 0) {
                        Files.deleteIfExists(part1);
                    }
                    Path part0 = part1.getParent();
                    List<Path> part0Files = listPaths(part0);
                    if (part0Files.size() == 0) {
                        Files.deleteIfExists(part0);
                    }
                } catch (IOException e) {
                    throw new StorageException("Could not delete file: " + metacard.getId(), e);
                }
            }
        } finally {
            rollback(request);
        }
    }

    private void commitUpdates(StorageRequest request, String id, Path requestIdDir)
            throws StorageException {
        List<Path> contentIdDirs;
        try {
            contentIdDirs = listPaths(requestIdDir);
        } catch (IOException e) {
            throw new StorageException(
                    "Unable to retrieve contents of temporary content storage for id: " + id, e);
        }
        try {
            for (Path contentIdDir : contentIdDirs) {
                String[] parts = getContentFilePathParts(contentIdDir.getFileName()
                        .toString());
                Path target = Paths.get(baseContentDirectory.toAbsolutePath()
                        .toString(), parts[0], parts[1], parts[2]);
                try {
                    if (Files.exists(target)) {
                        List<Path> files = listPaths(target);
                        for (Path file : files) {
                            Files.deleteIfExists(file);
                        }
                    }
                    if (Files.deleteIfExists(target)) {
                        LOGGER.debug("Remove existing content id directory for commit: " + id);
                    }
                    Files.createDirectories(target.getParent());
                    Files.move(contentIdDir, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    LOGGER.warn(
                            "Unable to move files by simple rename, resorting to copy. This will impact performance.",
                            e);
                    try {
                        if (Files.deleteIfExists(target)) {
                            LOGGER.debug("Remove existing content id directory for commit: " + id);
                        }
                        Path createdTarget = Files.createDirectories(target);
                        List<Path> files = listPaths(contentIdDir);
                        Files.copy(files.get(0), Paths.get(createdTarget.toAbsolutePath()
                                .toString(), files.get(0)
                                .getFileName()
                                .toString()));
                    } catch (IOException e1) {
                        throw new StorageException("Unable to commit changes for request: " + id,
                                e1);
                    }
                }
            }
        } finally {
            rollback(request);
        }
    }

    @Override
    public synchronized void rollback(StorageRequest request) throws StorageException {
        String id = request.getId();
        Path requestIdDir = Paths.get(baseContentTmpDirectory.toAbsolutePath()
                .toString(), id);
        deletionMap.remove(id);
        try {
            List<Path> items = listPaths(requestIdDir);
            for (Path item : items) {
                List<Path> files = listPaths(item);
                for (Path file : files) {
                    Files.deleteIfExists(file);
                }
                Files.deleteIfExists(item);
            }
            if (Files.deleteIfExists(requestIdDir)) {
                LOGGER.debug("Remove existing content id directory for commit: " + id);
            }
        } catch (IOException e) {
            throw new StorageException(
                    "Unable to remove temporary content storage for request: " + id, e);
        }
    }

    private List<Path> listPaths(Path dir) throws IOException {
        List<Path> result = new ArrayList<>();
        if (Files.exists(dir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry : stream) {
                    result.add(entry);
                }
            } catch (DirectoryIteratorException ex) {
                // I/O error encounted during the iteration, the cause is an IOException
                throw ex.getCause();
            }
        }
        return result;
    }

    //separating into 2 directories of 3 characters each allows us to
    //get to 361,000,000,000 records before we would run up against the
    //NTFS file system limits for a single directory
    String[] getContentFilePathParts(String id) {
        String partsId = id;
        if (id.length() < 6) {
            partsId = StringUtils.rightPad(id, 6, "0");
        }

        String[] parts = new String[3];
        parts[0] = partsId.substring(0, 3);
        parts[1] = partsId.substring(3, 6);
        parts[2] = id;
        return parts;
    }

    private Path getContentFilePath(String id) throws StorageException {
        String[] parts = getContentFilePathParts(id);
        Path contentIdDir = Paths.get(baseContentDirectory.toAbsolutePath()
                .toString(), parts[0], parts[1], parts[2]);
        List<Path> contentFiles;
        if (Files.exists(contentIdDir)) {
            try {
                contentFiles = listPaths(contentIdDir);
            } catch (IOException e) {
                throw new StorageException(e);
            }

            if (contentFiles.size() != 1) {
                throw new StorageException("Content ID: " + id + " storage folder is corrupted.");
            }

            //there should only be one file
            return contentFiles.get(0);
        }
        return null;
    }

    private ContentItem generateContentFile(ContentItem item, Path contentDirectory)
            throws IOException, StorageException {
        LOGGER.trace("ENTERING: generateContentFile");

        if (!Files.exists(contentDirectory)) {
            Files.createDirectories(contentDirectory);
        }

        Path contentItemPath = Paths.get(contentDirectory.toAbsolutePath()
                .toString(), item.getFilename());

        long copy = Files.copy(item.getInputStream(), contentItemPath);

        if (copy != item.getSize()) {
            LOGGER.warn("Created content item {} size {} does not match expected size {}",
                    item.getId(), copy, item.getSize());
        }

        ContentItemImpl contentItem = new ContentItemImpl(item.getId(),
                com.google.common.io.Files.asByteSource(contentItemPath.toFile()),
                item.getMimeType()
                        .toString(), contentItemPath.toAbsolutePath()
                .toString(), copy, item.getMetacard());
        String contentUri = CONTENT_URI_PREFIX + contentItem.getId();
        LOGGER.debug("contentUri = {}", contentUri);
        contentItem.setUri(contentUri);

        LOGGER.trace("EXITING: generateContentFile");

        return contentItem;
    }

    public MimeTypeMapper getMimeTypeMapper() {
        return mimeTypeMapper;
    }

    public void setMimeTypeMapper(MimeTypeMapper mimeTypeMapper) {
        this.mimeTypeMapper = mimeTypeMapper;
    }

    @SuppressFBWarnings
    public void setBaseContentDirectory(final String baseDirectory) throws IOException {

        Path directory;
        if (!baseDirectory.isEmpty()) {
            String path = FilenameUtils.normalize(baseDirectory);
            try {
                directory = Paths.get(path, DEFAULT_CONTENT_REPOSITORY, DEFAULT_CONTENT_STORE);
            } catch (InvalidPathException e) {
                path = System.getProperty(KARAF_HOME);
                directory = Paths.get(path, DEFAULT_CONTENT_REPOSITORY, DEFAULT_CONTENT_STORE);
            }
        } else {
            String path = System.getProperty("karaf.home");
            directory = Paths.get(path, DEFAULT_CONTENT_REPOSITORY, DEFAULT_CONTENT_STORE);
        }

        Path directories;
        if (!Files.exists(directory)) {
            directories = Files.createDirectories(directory);
            LOGGER.debug("Setting base content directory to: {}", directories.toAbsolutePath()
                    .toString());
        } else {
            directories = directory;
        }

        Path tmpDirectories;
        Path tmpDirectory = Paths.get(directories.toAbsolutePath()
                .toString(), DEFAULT_TMP);
        if (!Files.exists(tmpDirectory)) {
            tmpDirectories = Files.createDirectories(tmpDirectory);
            LOGGER.debug("Setting base content directory to: {}", tmpDirectory.toAbsolutePath()
                    .toString());
        } else {
            tmpDirectories = tmpDirectory;
        }

        this.baseContentDirectory = directories;
        this.baseContentTmpDirectory = tmpDirectories;
    }
}
