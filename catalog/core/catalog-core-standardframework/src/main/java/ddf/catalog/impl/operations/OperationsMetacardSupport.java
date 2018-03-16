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

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeInjector;
import ddf.catalog.data.DefaultAttributeValueRegistry;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.impl.FrameworkProperties;
import ddf.catalog.source.IngestException;
import ddf.mime.MimeTypeResolutionException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.activation.MimeType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.detect.DefaultProbDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.codice.ddf.catalog.transform.TransformResponse;
import org.codice.ddf.platform.util.InputValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support class for working with {@code Metacard}s for the {@code CatalogFrameworkImpl}.
 *
 * <p>This class contains methods for management/manipulation for metacards for the CFI and its
 * support classes. No operations/support methods should be added to this class except in support of
 * CFI, specific to metacards.
 */
public class OperationsMetacardSupport {
  private static final Logger LOGGER = LoggerFactory.getLogger(OperationsMetacardSupport.class);

  //
  // Injected properties
  //
  private final FrameworkProperties frameworkProperties;

  public OperationsMetacardSupport(FrameworkProperties frameworkProperties) {
    this.frameworkProperties = frameworkProperties;
  }

  /**
   * Processes input metacard, injecting attributes as defined by the {@code injectors}.
   *
   * @param original the input metacard
   * @param injectors the list of injectors to apply
   * @return the metacard, updated with any extra attributes as defined by the {@code injectors}
   */
  Metacard applyInjectors(Metacard original, List<AttributeInjector> injectors) {
    Metacard metacard = original;
    for (AttributeInjector injector : injectors) {
      metacard = injector.injectAttributes(metacard);
    }
    return metacard;
  }

  void generateMetacardAndContentItems(
      List<ContentItem> incomingContentItems,
      Map<String, Metacard> metacardMap,
      List<ContentItem> contentItems,
      Map<String, Map<String, Path>> tmpContentPaths,
      Map<String, ? extends Serializable> arguments)
      throws IngestException {
    for (ContentItem contentItem : incomingContentItems) {
      try {
        Path tmpPath = null;
        String fileName;
        long size;
        try (InputStream inputStream = contentItem.getInputStream()) {
          fileName = contentItem.getFilename();
          if (inputStream == null) {
            throw new IngestException(
                "Could not copy bytes of content message.  Message was NULL.");
          }

          if (!InputValidation.isFileNameClientSideSafe(fileName)) {
            throw new IngestException("Ignored filename found.");
          }

          String sanitizedFilename = InputValidation.sanitizeFilename(fileName);
          tmpPath =
              Files.createTempFile(
                  FilenameUtils.getBaseName(sanitizedFilename),
                  FilenameUtils.getExtension(sanitizedFilename));
          Files.copy(inputStream, tmpPath, StandardCopyOption.REPLACE_EXISTING);
          size = Files.size(tmpPath);

          final String key = contentItem.getId();
          Map<String, Path> pathAndQualifiers = tmpContentPaths.get(key);

          if (pathAndQualifiers == null) {
            pathAndQualifiers = new HashMap<>();
            pathAndQualifiers.put(contentItem.getQualifier(), tmpPath);
            tmpContentPaths.put(key, pathAndQualifiers);
          } else {
            pathAndQualifiers.put(contentItem.getQualifier(), tmpPath);
          }

        } catch (IOException e) {
          if (tmpPath != null) {
            FileUtils.deleteQuietly(tmpPath.toFile());
          }
          throw new IngestException("Could not copy bytes of content message.", e);
        }
        String mimeTypeRaw = contentItem.getMimeTypeRawData();
        mimeTypeRaw = guessMimeType(mimeTypeRaw, fileName, tmpPath);

        if (!InputValidation.isMimeTypeClientSideSafe(mimeTypeRaw)) {
          throw new IngestException("Unsupported mime type.");
        }

        // If any sanitization was done, rename file name to sanitized file name.
        if (!InputValidation.sanitizeFilename(fileName).equals(fileName)) {
          fileName = InputValidation.sanitizeFilename(fileName);
        } else {
          fileName = updateFileExtension(mimeTypeRaw, fileName);
        }

        TransformResponse transformResponse =
            frameworkProperties
                .getTransform()
                .transform(
                    new MimeType(mimeTypeRaw),
                    contentItem.getId(),
                    null,
                    fileName,
                    tmpPath.toFile(),
                    null,
                    arguments);

        Optional<Metacard> parentMetacardOptional = transformResponse.getParentMetacard();
        if (parentMetacardOptional.isPresent()) {
          Metacard parentMetacard = parentMetacardOptional.get();
          metacardMap.put(parentMetacard.getId(), parentMetacard);
          contentItems.add(
              new ContentItemImpl(
                  parentMetacard.getId(),
                  StringUtils.isNotEmpty(contentItem.getQualifier())
                      ? contentItem.getQualifier()
                      : "",
                  com.google.common.io.Files.asByteSource(tmpPath.toFile()),
                  mimeTypeRaw,
                  fileName,
                  size,
                  parentMetacard));
        }

        transformResponse
            .getDerivedMetacards()
            .forEach(derivedMetacard -> metacardMap.put(derivedMetacard.getId(), derivedMetacard));

        transformResponse
            .getDerivedContentItems()
            .forEach(
                derivedContentItem -> {
                  Metacard metacard = derivedContentItem.getMetacard();
                  metacardMap.put(metacard.getId(), metacard);
                  contentItems.add(derivedContentItem);
                });

      } catch (Exception e) {
        tmpContentPaths
            .values()
            .stream()
            .flatMap(id -> id.values().stream())
            .forEach(path -> FileUtils.deleteQuietly(path.toFile()));
        tmpContentPaths.clear();
        throw new IngestException("Could not create metacard.", e);
      }
    }
  }

  /**
   * Updates any empty metacard attributes with those defined in the {@link
   * DefaultAttributeValueRegistry}.
   *
   * @param metacard the metacard to update with default attribute values
   */
  void setDefaultValues(Metacard metacard) {
    MetacardType metacardType = metacard.getMetacardType();
    DefaultAttributeValueRegistry registry = frameworkProperties.getDefaultAttributeValueRegistry();

    metacardType
        .getAttributeDescriptors()
        .stream()
        .map(AttributeDescriptor::getName)
        .filter(attributeName -> hasNoValue(metacard.getAttribute(attributeName)))
        .forEach(
            attributeName -> {
              registry
                  .getDefaultValue(metacardType.getName(), attributeName)
                  .ifPresent(
                      defaultValue ->
                          metacard.setAttribute(new AttributeImpl(attributeName, defaultValue)));
            });
  }

  private boolean hasNoValue(Attribute attribute) {
    return attribute == null || attribute.getValue() == null;
  }

  private String updateFileExtension(String mimeTypeRaw, String fileName) {
    String extension = FilenameUtils.getExtension(fileName);
    if (ContentItem.DEFAULT_FILE_NAME.equals(fileName)
            && !ContentItem.DEFAULT_MIME_TYPE.equals(mimeTypeRaw)
        || StringUtils.isEmpty(extension)) {
      try {
        extension =
            frameworkProperties.getMimeTypeMapper().getFileExtensionForMimeType(mimeTypeRaw);
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

  // package-private for unit testing
  String guessMimeType(String mimeTypeRaw, String fileName, Path tmpContentPath)
      throws IOException {
    if (ContentItem.DEFAULT_MIME_TYPE.equals(mimeTypeRaw)) {
      try (InputStream inputStreamMessageCopy =
          com.google.common.io.Files.asByteSource(tmpContentPath.toFile()).openStream()) {
        String mimeTypeGuess =
            frameworkProperties
                .getMimeTypeMapper()
                .guessMimeType(inputStreamMessageCopy, FilenameUtils.getExtension(fileName));
        if (StringUtils.isNotEmpty(mimeTypeGuess)) {
          mimeTypeRaw = mimeTypeGuess;
        }
      } catch (MimeTypeResolutionException e) {
        LOGGER.debug("Unable to guess mime type for file.", e);
      }
      if (ContentItem.DEFAULT_MIME_TYPE.equals(mimeTypeRaw)) {
        Detector detector = new DefaultProbDetector();
        try (InputStream inputStreamMessageCopy = TikaInputStream.get(tmpContentPath)) {
          MediaType mediaType = detector.detect(inputStreamMessageCopy, new Metadata());
          mimeTypeRaw = mediaType.toString();
        } catch (IOException e) {
          LOGGER.debug("Unable to guess mime type for file.", e);
        }
      }
      if (mimeTypeRaw.equals("text/plain")) {
        try (InputStream inputStreamMessageCopy =
                com.google.common.io.Files.asByteSource(tmpContentPath.toFile()).openStream();
            BufferedReader bufferedReader =
                new BufferedReader(
                    new InputStreamReader(inputStreamMessageCopy, Charset.forName("UTF-8")))) {
          String line =
              bufferedReader
                  .lines()
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
}
