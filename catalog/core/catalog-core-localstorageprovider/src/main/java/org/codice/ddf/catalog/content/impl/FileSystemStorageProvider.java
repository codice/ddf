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
package org.codice.ddf.catalog.content.impl;

import com.google.common.io.ByteSource;
import ddf.catalog.Constants;
import ddf.catalog.content.StorageException;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.data.impl.ContentItemValidator;
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
import ddf.catalog.data.impl.AttributeImpl;
import ddf.mime.MimeTypeMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.activation.MimeType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** File system storage provider. */
public class FileSystemStorageProvider implements StorageProvider {

  public static final String DEFAULT_CONTENT_REPOSITORY = "content";

  public static final String DEFAULT_CONTENT_STORE = "store";

  public static final String DEFAULT_TMP = "tmp";

  public static final String KARAF_HOME = "karaf.home";

  private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

  private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemStorageProvider.class);

  public static final String REF_EXT = "external-reference";

  /** Mapper for file extensions-to-mime types (and vice versa) */
  private MimeTypeMapper mimeTypeMapper;

  /** Root directory for entire content repository */
  private Path baseContentDirectory;

  private Path baseContentTmpDirectory;

  private Map<String, List<Metacard>> deletionMap = new ConcurrentHashMap<>();

  private Map<String, Set<String>> updateMap = new ConcurrentHashMap<>();

  /** Default constructor, invoked by blueprint. */
  public FileSystemStorageProvider() {
    LOGGER.debug("File System Provider initializing...");
  }

  @Override
  public CreateStorageResponse create(CreateStorageRequest createRequest) throws StorageException {
    LOGGER.trace("ENTERING: create");

    List<ContentItem> contentItems = createRequest.getContentItems();

    List<ContentItem> createdContentItems = new ArrayList<>(createRequest.getContentItems().size());

    for (ContentItem contentItem : contentItems) {
      try {
        if (!ContentItemValidator.validate(contentItem)) {
          LOGGER.warn("Item is not valid: {}", contentItem);
          continue;
        }
        Path contentIdDir =
            getTempContentItemDir(createRequest.getId(), new URI(contentItem.getUri()));

        Path contentDirectory = Files.createDirectories(contentIdDir);

        createdContentItems.add(
            generateContentFile(
                contentItem,
                contentDirectory,
                (String) createRequest.getPropertyValue(Constants.STORE_REFERENCE_KEY)));
      } catch (IOException | URISyntaxException | IllegalArgumentException e) {
        throw new StorageException(e);
      }
    }

    CreateStorageResponse response =
        new CreateStorageResponseImpl(createRequest, createdContentItems);
    updateMap.put(
        createRequest.getId(),
        createdContentItems.stream().map(ContentItem::getUri).collect(Collectors.toSet()));

    LOGGER.trace("EXITING: create");

    return response;
  }

  @Override
  public ReadStorageResponse read(ReadStorageRequest readRequest) throws StorageException {
    LOGGER.trace("ENTERING: read");

    if (readRequest.getResourceUri() == null) {
      return new ReadStorageResponseImpl(readRequest);
    }

    URI uri = readRequest.getResourceUri();
    ContentItem returnItem = readContent(uri);
    return new ReadStorageResponseImpl(readRequest, returnItem);
  }

  @Override
  public UpdateStorageResponse update(UpdateStorageRequest updateRequest) throws StorageException {
    LOGGER.trace("ENTERING: update");

    List<ContentItem> contentItems = updateRequest.getContentItems();

    List<ContentItem> updatedItems = new ArrayList<>(updateRequest.getContentItems().size());

    for (ContentItem contentItem : contentItems) {
      try {
        if (!ContentItemValidator.validate(contentItem)) {
          LOGGER.warn("Item is not valid: {}", contentItem);
          continue;
        }

        ContentItem updateItem = contentItem;
        if (StringUtils.isBlank(contentItem.getFilename())
            || StringUtils.equals(contentItem.getFilename(), ContentItem.DEFAULT_FILE_NAME)) {
          ContentItem existingItem = readContent(new URI(contentItem.getUri()));
          updateItem = new ContentItemDecorator(contentItem, existingItem);
        }

        Path contentIdDir =
            getTempContentItemDir(updateRequest.getId(), new URI(updateItem.getUri()));

        updatedItems.add(
            generateContentFile(
                updateItem,
                contentIdDir,
                (String) updateRequest.getPropertyValue(Constants.STORE_REFERENCE_KEY)));
      } catch (IOException | URISyntaxException | IllegalArgumentException e) {
        throw new StorageException(e);
      }
    }

    for (ContentItem contentItem : updatedItems) {
      if (contentItem.getMetacard().getResourceURI() == null
          && StringUtils.isBlank(contentItem.getQualifier())) {
        contentItem
            .getMetacard()
            .setAttribute(new AttributeImpl(Metacard.RESOURCE_URI, contentItem.getUri()));
        try {
          contentItem
              .getMetacard()
              .setAttribute(new AttributeImpl(Metacard.RESOURCE_SIZE, contentItem.getSize()));
        } catch (IOException e) {
          LOGGER.info(
              "Could not set size of content item [{}] on metacard [{}]",
              contentItem.getId(),
              contentItem.getMetacard().getId(),
              e);
        }
      }
    }

    UpdateStorageResponse response = new UpdateStorageResponseImpl(updateRequest, updatedItems);
    updateMap.put(
        updateRequest.getId(),
        updatedItems.stream().map(ContentItem::getUri).collect(Collectors.toSet()));

    LOGGER.trace("EXITING: update");

    return response;
  }

  @Override
  public DeleteStorageResponse delete(DeleteStorageRequest deleteRequest) throws StorageException {
    LOGGER.trace("ENTERING: delete");

    List<Metacard> itemsToBeDeleted = new ArrayList<>();

    List<ContentItem> deletedContentItems = new ArrayList<>(deleteRequest.getMetacards().size());
    for (Metacard metacard : deleteRequest.getMetacards()) {
      LOGGER.debug("File to be deleted: {}", metacard.getId());

      ContentItem deletedContentItem =
          new ContentItemImpl(metacard.getId(), "", null, "", "", 0, metacard);

      if (!ContentItemValidator.validate(deletedContentItem)) {
        LOGGER.warn("Cannot delete invalid content item ({})", deletedContentItem);
        continue;
      }
      try {
        // For deletion we can ignore the qualifier and assume everything under a given ID is
        // to be removed.
        Path contentIdDir = getContentItemDir(new URI(deletedContentItem.getUri()));
        if (Files.exists(contentIdDir)) {
          List<Path> paths = new ArrayList<>();
          if (Files.isDirectory(contentIdDir)) {
            paths = listPaths(contentIdDir);
          } else {
            paths.add(contentIdDir);
          }

          for (Path path : paths) {
            if (Files.exists(path)) {
              deletedContentItems.add(deletedContentItem);
            }
          }
          itemsToBeDeleted.add(metacard);
        }
      } catch (IOException | URISyntaxException e) {
        throw new StorageException("Could not delete file: " + metacard.getId(), e);
      }
    }

    deletionMap.put(deleteRequest.getId(), itemsToBeDeleted);

    DeleteStorageResponse response =
        new DeleteStorageResponseImpl(deleteRequest, deletedContentItems);
    LOGGER.trace("EXITING: delete");

    return response;
  }

  @Override
  public void commit(StorageRequest request) throws StorageException {
    if (deletionMap.containsKey(request.getId())) {
      commitDeletes(request);
    } else if (updateMap.containsKey(request.getId())) {
      commitUpdates(request);
    } else {
      LOGGER.info("Nothing to commit for request: {}", request.getId());
    }
  }

  private void commitDeletes(StorageRequest request) throws StorageException {
    List<Metacard> itemsToBeDeleted = deletionMap.get(request.getId());
    try {
      for (Metacard metacard : itemsToBeDeleted) {
        LOGGER.debug("File to be deleted: {}", metacard.getId());

        String metacardId = metacard.getId();

        List<String> parts = getContentFilePathParts(metacardId, "");

        Path contentIdDir =
            Paths.get(baseContentDirectory.toString(), parts.toArray(new String[parts.size()]));

        if (!Files.exists(contentIdDir)) {
          throw new StorageException("File doesn't exist for id: " + metacard.getId());
        }

        try {
          FileUtils.deleteDirectory(contentIdDir.toFile());

          Path part1 = contentIdDir.getParent();
          if (Files.isDirectory(part1) && isDirectoryEmpty(part1)) {
            FileUtils.deleteDirectory(part1.toFile());
            Path part0 = part1.getParent();
            if (Files.isDirectory(part0) && isDirectoryEmpty(part0)) {
              FileUtils.deleteDirectory(part0.toFile());
            }
          }

        } catch (IOException e) {
          throw new StorageException("Could not delete file: " + metacard.getId(), e);
        }
      }
    } finally {
      rollback(request);
    }
  }

  private boolean isDirectoryEmpty(Path dir) throws IOException {
    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
      return !dirStream.iterator().hasNext();
    } catch (IOException e) {
      LOGGER.debug("Unable to open directory stream for {}", dir.toString(), e);
      throw e;
    }
  }

  private void commitUpdates(StorageRequest request) throws StorageException {
    try {
      for (String contentUri : updateMap.get(request.getId())) {
        Path contentIdDir = getTempContentItemDir(request.getId(), new URI(contentUri));
        Path target = getContentItemDir(new URI(contentUri));
        try {
          if (Files.exists(contentIdDir)) {
            if (Files.exists(target)) {
              List<Path> files = listPaths(target);
              for (Path file : files) {
                if (!Files.isDirectory(file)) {
                  Files.deleteIfExists(file);
                }
              }
            }
            Files.createDirectories(target.getParent());
            Files.move(contentIdDir, target, StandardCopyOption.REPLACE_EXISTING);
          }
        } catch (IOException e) {
          LOGGER.debug(
              "Unable to move files by simple rename, resorting to copy. This will impact performance.",
              e);
          try {
            Path createdTarget = Files.createDirectories(target);
            List<Path> files = listPaths(contentIdDir);
            Files.copy(
                files.get(0),
                Paths.get(
                    createdTarget.toAbsolutePath().toString(),
                    files.get(0).getFileName().toString()));
          } catch (IOException e1) {
            throw new StorageException(
                "Unable to commit changes for request: " + request.getId(), e1);
          }
        }
      }
    } catch (URISyntaxException e) {
      throw new StorageException(e);
    } finally {
      rollback(request);
    }
  }

  @Override
  public void rollback(StorageRequest request) throws StorageException {
    String id = request.getId();
    Path requestIdDir = Paths.get(baseContentTmpDirectory.toAbsolutePath().toString(), id);
    deletionMap.remove(id);
    updateMap.remove(id);
    try {
      FileUtils.deleteDirectory(requestIdDir.toFile());
    } catch (IOException e) {
      throw new StorageException(
          "Unable to remove temporary content storage for request: " + id, e);
    }
  }

  private ContentItem readContent(URI uri) throws StorageException {
    Path file = getContentFilePath(uri);

    if (file == null) {
      throw new StorageException(
          "Unable to find file for content ID: " + uri.getSchemeSpecificPart());
    }

    String filename = file.getFileName().toString();
    String extension = FilenameUtils.getExtension(filename);
    URI reference = null;
    // if the file is an external reference, remove the reference extension
    // so we can get the real extension
    if (REF_EXT.equals(extension)) {
      extension = FilenameUtils.getExtension(FilenameUtils.removeExtension(filename));
      try {
        reference = new URI(new String(Files.readAllBytes(file), Charset.forName("UTF-8")));
        if (reference.getScheme().equalsIgnoreCase("file")) {
          file = Paths.get(reference);
        } else {
          file = null;
        }
      } catch (IOException | URISyntaxException e) {
        throw new StorageException(e);
      }
    }

    if (reference != null && file != null && !file.toFile().exists()) {
      throw new StorageException("Cannot read " + uri + ".");
    }

    String mimeType = DEFAULT_MIME_TYPE;
    long size = 0;
    ByteSource byteSource;

    try (InputStream fileInputStream =
        file != null ? Files.newInputStream(file) : reference.toURL().openStream()) {
      mimeType = mimeTypeMapper.guessMimeType(fileInputStream, extension);
    } catch (Exception e) {
      LOGGER.info(
          "Could not determine mime type for file extension = {}; defaulting to {}",
          extension,
          DEFAULT_MIME_TYPE);
    }

    if (file != null) {
      if (mimeType == null || DEFAULT_MIME_TYPE.equals(mimeType)) {
        try {
          mimeType = Files.probeContentType(file);
        } catch (IOException e) {
          LOGGER.info("Unable to determine mime type using Java Files service.", e);
        }
      }

      LOGGER.debug("mimeType = {}", mimeType);
      try {
        size = Files.size(file);
      } catch (IOException e) {
        LOGGER.info("Unable to retrieve size of file: {}", file.toAbsolutePath().toString(), e);
      }
      byteSource = com.google.common.io.Files.asByteSource(file.toFile());
    } else {
      URI finalReference = reference;
      byteSource =
          new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
              return finalReference.toURL().openStream();
            }
          };
    }

    return new ContentItemImpl(
        uri.getSchemeSpecificPart(), uri.getFragment(), byteSource, mimeType, filename, size, null);
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

  private Path getTempContentItemDir(String requestId, URI contentUri) {
    List<String> pathParts = new ArrayList<>();
    pathParts.add(requestId);
    pathParts.addAll(
        getContentFilePathParts(contentUri.getSchemeSpecificPart(), contentUri.getFragment()));

    return Paths.get(
        baseContentTmpDirectory.toAbsolutePath().toString(),
        pathParts.toArray(new String[pathParts.size()]));
  }

  private Path getContentItemDir(URI contentUri) {
    List<String> pathParts =
        getContentFilePathParts(contentUri.getSchemeSpecificPart(), contentUri.getFragment());
    try {
      return Paths.get(
          baseContentDirectory.toString(), pathParts.toArray(new String[pathParts.size()]));
    } catch (InvalidPathException e) {
      LOGGER.debug(
          "Invalid path: [{}/{}]",
          baseContentDirectory.toString(),
          pathParts.stream().collect(Collectors.joining()));
      return null;
    }
  }

  // separating into 2 directories of 3 characters each allows us to
  // get to 361,000,000,000 records before we would run up against the
  // NTFS file system limits for a single directory
  List<String> getContentFilePathParts(String id, String qualifier) {
    String partsId = id;
    if (id.length() < 6) {
      partsId = StringUtils.rightPad(id, 6, "0");
    }
    List<String> parts = new ArrayList<>();
    parts.add(partsId.substring(0, 3));
    parts.add(partsId.substring(3, 6));
    parts.add(partsId);
    if (StringUtils.isNotBlank(qualifier)) {
      parts.add(qualifier);
    }
    return parts;
  }

  private Path getContentFilePath(URI uri) throws StorageException {
    Path contentIdDir = getContentItemDir(uri);
    List<Path> contentFiles;
    if (contentIdDir != null && Files.exists(contentIdDir)) {
      try {
        contentFiles = listPaths(contentIdDir);
      } catch (IOException e) {
        throw new StorageException(e);
      }

      contentFiles.removeIf(Files::isDirectory);

      if (contentFiles.size() != 1) {
        throw new StorageException(
            "Content ID: " + uri.getSchemeSpecificPart() + " storage folder is corrupted.");
      }

      // there should only be one file
      return contentFiles.get(0);
    }
    return null;
  }

  private ContentItem generateContentFile(
      ContentItem item, Path contentDirectory, String storeReference)
      throws IOException, StorageException {
    LOGGER.trace("ENTERING: generateContentFile");

    if (!Files.exists(contentDirectory)) {
      Files.createDirectories(contentDirectory);
    }

    Path contentItemPath =
        Paths.get(contentDirectory.toAbsolutePath().toString(), item.getFilename());
    ByteSource byteSource;

    long copy;

    if (storeReference != null) {
      copy = item.getSize();
      Files.write(
          Paths.get(contentItemPath.toString() + "." + REF_EXT),
          storeReference.getBytes(Charset.forName("UTF-8")));
      byteSource =
          new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
              return new URL(storeReference).openStream();
            }
          };
    } else {
      try (InputStream inputStream = item.getInputStream()) {
        copy = Files.copy(inputStream, contentItemPath);
      }
      byteSource = com.google.common.io.Files.asByteSource(contentItemPath.toFile());

      if (copy != item.getSize()) {
        LOGGER.warn(
            "Created content item {} size {} does not match expected size {}"
                + System.lineSeparator()
                + "Verify filesystem and/or network integrity.",
            item.getId(),
            copy,
            item.getSize());
      }
    }

    ContentItemImpl contentItem =
        new ContentItemImpl(
            item.getId(),
            item.getQualifier(),
            byteSource,
            item.getMimeType().toString(),
            contentItemPath.getFileName().toString(),
            copy,
            item.getMetacard());

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
      String path = tryCanonicalizeDirectory(baseDirectory);
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
      LOGGER.debug(
          "Setting base content directory to: {}", directories.toAbsolutePath().toString());
    } else {
      directories = directory;
    }

    Path tmpDirectories;
    Path tmpDirectory = Paths.get(directories.toAbsolutePath().toString(), DEFAULT_TMP);
    if (!Files.exists(tmpDirectory)) {
      tmpDirectories = Files.createDirectories(tmpDirectory);
      LOGGER.debug(
          "Setting base content directory to: {}", tmpDirectory.toAbsolutePath().toString());
    } else {
      tmpDirectories = tmpDirectory;
    }

    this.baseContentDirectory = directories;
    this.baseContentTmpDirectory = tmpDirectories;
  }

  private String tryCanonicalizeDirectory(String directory) {
    String normalized = FilenameUtils.normalize(directory);
    try {
      return Paths.get(normalized).toFile().getCanonicalPath();
    } catch (IOException e) {
      LOGGER.debug("Could not get canonical path for ({})", directory, e);
    }
    return normalized;
  }

  private static class ContentItemDecorator implements ContentItem {

    private final ContentItem updateContentItem;

    private final ContentItem existingItem;

    public ContentItemDecorator(ContentItem contentItem, ContentItem existingItem) {
      this.updateContentItem = contentItem;
      this.existingItem = existingItem;
    }

    @Override
    public String getId() {
      return updateContentItem.getId();
    }

    @Override
    public String getUri() {
      return updateContentItem.getUri();
    }

    @Override
    public String getQualifier() {
      return updateContentItem.getQualifier();
    }

    @Override
    public String getFilename() {
      return existingItem.getFilename();
    }

    @Override
    public MimeType getMimeType() {
      return existingItem.getMimeType();
    }

    @Override
    public String getMimeTypeRawData() {
      return existingItem.getMimeTypeRawData();
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return updateContentItem.getInputStream();
    }

    @Override
    public long getSize() throws IOException {
      return updateContentItem.getSize();
    }

    @Override
    public Metacard getMetacard() {
      return updateContentItem.getMetacard();
    }
  }
}
