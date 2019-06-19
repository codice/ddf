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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteSource;
import com.google.common.io.FileBackedOutputStream;
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
import ddf.mime.MimeTypeResolutionException;
import ddf.security.encryption.crypter.Crypter;
import ddf.security.encryption.crypter.Crypter.CrypterException;
import java.io.FileInputStream;
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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.activation.MimeType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
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

  @VisibleForTesting static final String CRYPTER_NAME = "file-system";

  public static final String REF_EXT = "external-reference";

  /** Mapper for file extensions-to-mime types (and vice versa) */
  private MimeTypeMapper mimeTypeMapper;

  /** Root directory for entire content repository */
  private Path baseContentDirectory;

  private Path baseContentTmpDirectory;

  private Map<String, List<Metacard>> deletionMap = new ConcurrentHashMap<>();

  private Map<String, Set<String>> updateMap = new ConcurrentHashMap<>();

  private Crypter crypter;

  /** Default constructor, invoked by blueprint. */
  public FileSystemStorageProvider() {
    LOGGER.debug("File System Provider initializing...");
    crypter =
        AccessController.doPrivileged((PrivilegedAction<Crypter>) () -> new Crypter(CRYPTER_NAME));
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
        if (contentIdDir != null && contentIdDir.toFile().exists()) {
          List<Path> paths = new ArrayList<>();
          if (contentIdDir.toFile().isDirectory()) {
            paths = listPaths(contentIdDir);
          } else {
            paths.add(contentIdDir);
          }

          for (Path path : paths) {
            if (path.toFile().exists()) {
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

        if (!contentIdDir.toFile().exists()) {
          throw new StorageException("File doesn't exist for id: " + metacard.getId());
        }

        try {
          FileUtils.deleteDirectory(contentIdDir.toFile());

          Path part1 = contentIdDir.getParent();
          if (part1.toFile().isDirectory() && isDirectoryEmpty(part1)) {
            FileUtils.deleteDirectory(part1.toFile());
            Path part0 = part1.getParent();
            if (part0.toFile().isDirectory() && isDirectoryEmpty(part0)) {
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
        if (target == null) {
          LOGGER.debug(
              "Unable to get content item directory. Unable to commit all changes for request: {} with content uri {}",
              request.getId(),
              contentUri);
          continue;
        }
        try {
          if (contentIdDir.toFile().exists()) {
            if (target.toFile().exists()) {
              List<Path> files = listPaths(target);
              for (Path file : files) {
                if (!file.toFile().isDirectory()) {
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
    Path path = getContentFilePath(uri);

    if (path == null) {
      throw new StorageException(
          "Unable to find file for content ID: " + uri.getSchemeSpecificPart());
    }

    String filename = path.getFileName().toString();

    // resolve external reference if necessary, determine the extension, and retrieve an
    // InputStream to the content
    InputStream contentInputStream;
    String extension;

    try {
      if (REF_EXT.equals(FilenameUtils.getExtension(filename))) {
        // remove the external reference extension so we can get the real extension
        extension =
            FilenameUtils.getExtension(
                FilenameUtils.removeExtension(path.getFileName().toString()));
        contentInputStream = getInputStreamFromReference(path);
      } else {
        extension = FilenameUtils.getExtension(path.getFileName().toString());
        contentInputStream = getInputStreamFromResource(path);
      }
    } catch (IOException e) {
      throw new StorageException(
          String.format("Unable to resolve InputStream given URI of %s", uri), e);
    }

    // decrypt the content and return as a ByteSource
    ByteSource byteSource = decryptStream(contentInputStream);

    // determine the size of the content
    long size = 0;

    try {
      size = byteSource.size();
    } catch (IOException e) {
      LOGGER.debug("Problem determining size of resource; defaulting to {}.", size, e);
    }

    // determine the MimeType of the content
    String mimeType = determineMimeType(extension, path, byteSource);

    return new ContentItemImpl(
        uri.getSchemeSpecificPart(), uri.getFragment(), byteSource, mimeType, filename, size, null);
  }

  private InputStream getInputStreamFromReference(Path externalReferencePath) throws IOException {
    URI reference;

    try {
      byte[] encryptedRefBytes = Files.readAllBytes(externalReferencePath);
      String encryptedRefString = new String(encryptedRefBytes, Charset.forName("UTF-8"));

      reference = new URI(crypter.decrypt(encryptedRefString));
    } catch (IOException | URISyntaxException e) {
      throw new IOException(e);
    }

    // try and represent the reference as a path
    Path newPath = null;
    if (reference.getScheme() == null) {
      newPath = Paths.get(reference.toASCIIString());
    } else if (reference.getScheme().equalsIgnoreCase("file")) {
      newPath = Paths.get(reference);
    }

    // if the reference can be represented as a path
    if (newPath != null) {
      if (!newPath.toFile().exists()) {
        throw new IOException("Cannot read " + reference + ".");
      }
      return getInputStreamFromResource(newPath);
    }

    return reference.toURL().openStream();
  }

  private InputStream getInputStreamFromResource(Path path) throws IOException {
    return Files.newInputStream(path);
  }

  private String determineMimeType(String extension, Path path, ByteSource byteSource) {
    String mimeType = DEFAULT_MIME_TYPE;

    // guess MimeType
    try {
      mimeType = mimeTypeMapper.guessMimeType(byteSource.openStream(), extension);
    } catch (IOException | MimeTypeResolutionException e) {
      LOGGER.debug(
          "Could not determine mime type for file extension = {}; defaulting to {}.",
          extension,
          mimeType);
    }

    // probe for MimeType
    if (mimeType == null || DEFAULT_MIME_TYPE.equals(mimeType)) {
      try {
        mimeType = Files.probeContentType(path);
      } catch (IOException e) {
        LOGGER.debug(
            "Could not determine mime type from file {}; defaulting to {}.", path, mimeType);
      }
    }

    return mimeType;
  }

  private ByteSource decryptStream(InputStream contentInputStream) throws StorageException {
    InputStream decryptedInputStream = null;
    FileBackedOutputStream decryptedOutputStream = null;

    try {
      // do not use try with resources in order to have these InputStreams in the finally block
      decryptedInputStream = crypter.decrypt(contentInputStream);
      decryptedOutputStream = new FileBackedOutputStream(128);
      IOUtils.copy(decryptedInputStream, decryptedOutputStream);
    } catch (CrypterException | IOException e) {
      LOGGER.debug(
          "Error decrypting InputStream {}. Failing StorageProvider read.", contentInputStream, e);
      throw new StorageException(
          String.format("Cannot decrypt InputStream %s.", contentInputStream), e);
    } finally {
      // need to close both streams in order for IOUtils to copy properly
      IOUtils.closeQuietly(decryptedInputStream);
      IOUtils.closeQuietly(decryptedOutputStream);
    }

    return decryptedOutputStream.asByteSource();
  }

  private List<Path> listPaths(Path dir) throws IOException {
    List<Path> result = new ArrayList<>();
    if (dir.toFile().exists()) {
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
    if (contentIdDir != null && contentIdDir.toFile().exists()) {
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
      ContentItem item, Path contentDirectory, String storeReference) throws IOException {
    LOGGER.trace("ENTERING: generateContentFile");

    if (!contentDirectory.toFile().exists()) {
      Files.createDirectories(contentDirectory);
    }

    long itemSize = item.getSize();
    long copySize;
    Path contentItemPath =
        Paths.get(contentDirectory.toAbsolutePath().toString(), item.getFilename());
    ByteSource byteSource;

    if (storeReference != null) {
      String encryptedReference = crypter.encrypt(storeReference);
      Files.write(
          Paths.get(contentItemPath.toString() + "." + REF_EXT),
          encryptedReference.getBytes(Charset.forName("UTF-8")));
      byteSource =
          new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
              InputStream referenceInputStream = new URL(storeReference).openStream();
              return crypter.decrypt(referenceInputStream);
            }
          };
    } else {
      try (InputStream plainInputStream = item.getInputStream();
          InputStream encryptedInputStream = crypter.encrypt(plainInputStream)) {
        copySize = Files.copy(encryptedInputStream, contentItemPath);
      }
      byteSource =
          new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
              try (InputStream fileInputStream = new FileInputStream(contentItemPath.toFile())) {
                return crypter.decrypt(fileInputStream);
              }
            }
          };

      if (copySize < itemSize && LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "Created content item {} encrypted size {} is not greater than plain size {}.{}"
                + "Verify filesystem and/or network integrity.",
            item.getId(),
            copySize,
            item.getSize(),
            System.lineSeparator());
      }
    }

    ContentItemImpl contentItem =
        new ContentItemImpl(
            item.getId(),
            item.getQualifier(),
            byteSource,
            item.getMimeType().toString(),
            contentItemPath.getFileName().toString(),
            itemSize,
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
    if (!directory.toFile().exists()) {
      directories = Files.createDirectories(directory);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Setting base content directory to: {}", directories.toAbsolutePath().toString());
      }
    } else {
      directories = directory;
    }

    Path tmpDirectories;
    Path tmpDirectory = Paths.get(directories.toAbsolutePath().toString(), DEFAULT_TMP);
    if (!tmpDirectory.toFile().exists()) {
      tmpDirectories = Files.createDirectories(tmpDirectory);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Setting base content directory to: {}", tmpDirectory.toAbsolutePath().toString());
      }
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
