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
package org.codice.ddf.catalog.transformer.explodingzip;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import ddf.catalog.Constants;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.transform.Transform;
import org.codice.ddf.catalog.transform.TransformResponse;
import org.codice.ddf.catalog.transform.impl.AbstractListMultiInputTransformer;
import org.codice.ddf.catalog.transform.impl.TransformResponseImpl;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExplodingZipTransformer extends AbstractListMultiInputTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExplodingZipTransformer.class);

  private final Transform transform;

  private final MimeTypeMapper mimeTypeMapper;

  private final String id;

  private final Set<MimeType> mimeTypes;

  public ExplodingZipTransformer(
      String id, List<String> mimeTypes, Transform transform, MimeTypeMapper mimeTypeMapper) {
    this.id = id;
    this.mimeTypes =
        mimeTypes
            .stream()
            .map(this::getMimeType)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    this.transform = transform;
    this.mimeTypeMapper = mimeTypeMapper;
  }

  private MimeType getMimeType(String s) {
    try {
      return new MimeType(s);
    } catch (MimeTypeParseException e) {
      LOGGER.debug("Unable to parse the mime-type {}", s, e);
    }
    return null;
  }

  @Override
  protected TransformResponse doTransform(
      InputStream input, Map<String, ? extends Serializable> arguments)
      throws IOException, CatalogTransformerException {

    List<ContentItem> contentItems = new LinkedList<>();
    List<Metacard> metacards = new LinkedList<>();

    try (ZipInputStream zipInputStream = new ZipInputStream(input)) {
      ZipEntry zipEntry;
      while ((zipEntry = zipInputStream.getNextEntry()) != null) {
        processZipEntry(arguments, contentItems, metacards, zipInputStream, zipEntry);
      }
    }

    return new TransformResponseImpl(null, metacards, contentItems);
  }

  private void processZipEntry(
      Map<String, ? extends Serializable> arguments,
      List<ContentItem> contentItems,
      List<Metacard> metacards,
      ZipInputStream zipInputStream,
      ZipEntry zipEntry)
      throws IOException {
    if (!zipEntry.isDirectory()) {
      processZipEntryFile(arguments, contentItems, metacards, zipInputStream, zipEntry);
    }
  }

  private void processZipEntryFile(
      Map<String, ? extends Serializable> arguments,
      List<ContentItem> contentItems,
      List<Metacard> metacards,
      ZipInputStream zipInputStream,
      ZipEntry zipEntry)
      throws IOException {
    try (TemporaryFileBackedOutputStream fileBackedOutputStream =
        new TemporaryFileBackedOutputStream()) {

      IOUtils.copy(zipInputStream, fileBackedOutputStream);

      MimeType mimeType = guessMimeType(zipEntry, fileBackedOutputStream);

      if (mimeType != null) {
        transformZipEntry(
            arguments, mimeType, fileBackedOutputStream, zipEntry, contentItems, metacards);
      }
    }
  }

  private void transformZipEntry(
      Map<String, ? extends Serializable> arguments,
      MimeType mimeType,
      TemporaryFileBackedOutputStream fileBackedOutputStream,
      ZipEntry zipEntry,
      List<ContentItem> contentItems,
      List<Metacard> metacards)
      throws IOException {
    try (InputStream inputStreamMessageCopy = fileBackedOutputStream.asByteSource().openStream()) {

      TransformResponse transformResponse =
          transform.transform(mimeType, null, null, inputStreamMessageCopy, null, arguments);

      transformResponse
          .getDerivedMetacards()
          .forEach(
              metacard ->
                  metacard.setAttribute(new AttributeImpl(Metacard.TITLE, zipEntry.getName())));

      Optional<Metacard> optionalMetacard = transformResponse.getParentMetacard();
      if (optionalMetacard.isPresent()) {

        ByteSource byteSource = ByteSource.wrap(fileBackedOutputStream.asByteSource().read());
        Metacard parentMetacard = optionalMetacard.get();
        ContentItem contentItem =
            new ContentItemImpl(
                parentMetacard.getId(),
                ByteSource.wrap(fileBackedOutputStream.asByteSource().read()),
                mimeType.toString(),
                zipEntry.getName(),
                byteSource.size(),
                parentMetacard);
        contentItems.add(contentItem);
      }

      contentItems.addAll(transformResponse.getDerivedContentItems());
      metacards.addAll(transformResponse.getDerivedMetacards());

    } catch (MetacardCreationException e) {
      LOGGER.debug("Unable to transform zip entry into a metacard", e);
    }
  }

  private MimeType guessMimeType(
      ZipEntry zipEntry, TemporaryFileBackedOutputStream fileBackedOutputStream)
      throws IOException {
    try (InputStream inputStreamMessageCopy = fileBackedOutputStream.asByteSource().openStream()) {
      return new MimeType(
          mimeTypeMapper.guessMimeType(
              inputStreamMessageCopy, FilenameUtils.getExtension(zipEntry.getName())));

    } catch (MimeTypeResolutionException | MimeTypeParseException e) {
      LOGGER.debug("Unable to guess mime type for file.", e);
    }
    return null;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public Set<MimeType> getMimeTypes() {
    return mimeTypes;
  }

  @Override
  public Map<String, Object> getProperties() {
    return new ImmutableMap.Builder<String, Object>()
        .put(Constants.SERVICE_ID, id)
        .put("mime-type", new ArrayList<>(mimeTypes))
        .build();
  }
}
