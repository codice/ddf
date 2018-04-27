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
package org.codice.ddf.catalog.harvest.adaptor;

import com.google.common.io.ByteSource;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import ddf.mime.MimeTypeToTransformerMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.codice.ddf.catalog.harvest.HarvestException;
import org.codice.ddf.catalog.harvest.HarvestedResource;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO javadoc
public class HarvestedResourceTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(HarvestedResourceTransformer.class);

  private final MimeTypeToTransformerMapper mimeTypeToTransformerMapper;

  private final MimeTypeMapper mimeTypeMapper;

  public HarvestedResourceTransformer(
      MimeTypeToTransformerMapper mimeTypeToTransformerMapper, MimeTypeMapper mimeTypeMapper) {
    Validate.notNull(
        mimeTypeToTransformerMapper, "Argument mimeTypeToTransformerMapper may not be null");
    Validate.notNull(mimeTypeMapper, "Argument mimeTypeMapper may not be null");

    this.mimeTypeToTransformerMapper = mimeTypeToTransformerMapper;
    this.mimeTypeMapper = mimeTypeMapper;
  }

  /**
   * Creates a metacard.
   *
   * @param harvestedResource the {@link HarvestedResource} the metacard will be created from
   * @return the metacard
   * @throws HarvestException if the resource could not be transformed
   */
  public Metacard transformHarvestedResource(HarvestedResource harvestedResource)
      throws HarvestException {
    return transform(harvestedResource, null);
  }

  /**
   * Creates a metacard with an update id.
   *
   * @param resource the {@link HarvestedResource} the metacard will be created from
   * @param metacardId id of the existing metacard to update
   * @return the metacard
   * @throws HarvestException if the resource could not be transformed
   */
  public Metacard transformHarvestedResource(HarvestedResource resource, String metacardId)
      throws HarvestException {
    return transform(resource, metacardId);
  }

  private Metacard transform(HarvestedResource harvestedResource, String metacardId)
      throws HarvestException {
    try (TemporaryFileBackedOutputStream tfbos = new TemporaryFileBackedOutputStream()) {
      IOUtils.copy(harvestedResource.getInputStream(), tfbos);
      final ByteSource byteSource = tfbos.asByteSource();

      final MimeType mimeType =
          Optional.of(harvestedResource.getMimeType())
              .orElse(
                  guessMimeTypeFor(
                      byteSource.openStream(),
                      FilenameUtils.getExtension(harvestedResource.getName())));

      for (InputTransformer inputTransformer :
          mimeTypeToTransformerMapper.findMatches(InputTransformer.class, mimeType)) {

        final Optional<Metacard> metacardOptional =
            doTransform(byteSource, metacardId, inputTransformer, harvestedResource);

        if (metacardOptional.isPresent()) {
          final Metacard metacard = metacardOptional.get();
          writeMetacardAttribute(metacard, Core.TITLE, harvestedResource.getName());
          writeMetacardAttribute(
              metacard, Core.RESOURCE_SIZE, Long.toString(harvestedResource.getSize()));
          writeMetacardAttribute(
              metacard, Core.RESOURCE_URI, harvestedResource.getUri().toASCIIString());

          return metacard;
        }
      }

      throw new HarvestException(
          String.format(
              "Failed to find a transformer to transform resource [%s].",
              harvestedResource.getName()));
    } catch (IOException e) {
      throw new HarvestException(
          String.format(
              "Failed to read TFBOS for harvested resource[%s].", harvestedResource.getName()),
          e);
    }
  }

  private Optional<Metacard> doTransform(
      final ByteSource byteSource,
      final String metacardId,
      final InputTransformer inputTransformer,
      final HarvestedResource harvestedResource)
      throws IOException {
    try {
      final InputStream is = byteSource.openStream();

      final Metacard metacard;
      if (StringUtils.isNotEmpty(metacardId)) {
        metacard = inputTransformer.transform(is, metacardId);
      } else {
        metacard = inputTransformer.transform(is);
      }

      writeMetacardAttribute(metacard, Core.TITLE, harvestedResource.getName());
      writeMetacardAttribute(
          metacard, Core.RESOURCE_SIZE, Long.toString(harvestedResource.getSize()));
      writeMetacardAttribute(
          metacard, Core.RESOURCE_URI, harvestedResource.getUri().toASCIIString());
      return Optional.of(metacard);
    } catch (CatalogTransformerException e) {
      LOGGER.trace(
          "Failed to transform resource [{}] with [{}] transformer. Trying next one.",
          harvestedResource.getName(),
          inputTransformer);
      return Optional.empty();
    }
  }

  @Nullable
  private MimeType guessMimeTypeFor(InputStream is, String fileExt) {
    try (TemporaryFileBackedOutputStream tfbos = new TemporaryFileBackedOutputStream()) {
      IOUtils.copy(is, tfbos);

      return new MimeType(mimeTypeMapper.guessMimeType(tfbos.asByteSource().openStream(), fileExt));
    } catch (IOException e) {
      LOGGER.debug("Failed to get input stream for harvested resource", e);
    } catch (MimeTypeResolutionException | MimeTypeParseException me) {
      LOGGER.debug(
          "Failed to get mime type for input stream [{}] with file extension [{}].",
          is,
          fileExt,
          me);
    }

    return null;
  }

  /**
   * Sets the {@link Attribute} on a {@link Metacard}. If attributeValue is empty or null, then
   * nothing will be set on the {@link Metacard}. Does not override existing {@link Attribute}s.
   *
   * @param metacard metacard to add attribute to
   * @param attributeName attribute name to add
   * @param attributeValue attribute value to add
   */
  private void writeMetacardAttribute(
      Metacard metacard, String attributeName, String attributeValue) {
    if (StringUtils.isEmpty(attributeValue)) {
      return;
    }

    Attribute attribute = metacard.getAttribute(attributeName);
    if (attribute != null) {
      return;
    }
    metacard.setAttribute(new AttributeImpl(attributeName, attributeValue));
  }
}
