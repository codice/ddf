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

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.resource.Resource;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import ddf.mime.MimeTypeToTransformerMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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
   * @return the metacard, or null if the resource could not be transformed
   */
  @Nullable
  public Metacard transformHarvestedResource(HarvestedResource harvestedResource) {
    return transform(harvestedResource, null);
  }

  /**
   * Creates a metacard with an update id.
   *
   * @param resource the {@link HarvestedResource} the metacard will be created from
   * @param metacardId id of the existing metacard to update
   * @return the metacard, or null if the resource could not be transformed
   */
  @Nullable
  public Metacard transformHarvestedResource(HarvestedResource resource, String metacardId) {
    return transform(resource, metacardId);
  }

  private Metacard transform(HarvestedResource harvestedResource, String metacardId) {
    MimeType resourceMimeType = harvestedResource.getMimeType();

    Metacard metacard = null;

    try (TemporaryFileBackedOutputStream tfbos = new TemporaryFileBackedOutputStream()) {
      IOUtils.copy(harvestedResource.getInputStream(), tfbos);
      if (resourceMimeType == null) {
        resourceMimeType =
            guessMimeTypeFor(
                tfbos.asByteSource().openStream(),
                FilenameUtils.getExtension(harvestedResource.getName()));
      }

      metacard =
          doTransform(
              resourceMimeType, tfbos.asByteSource().openStream(), harvestedResource, metacardId);

      if (metacard != null) {
        enrichMetacard(metacard, harvestedResource);
        return metacard;
      }
    } catch (IOException e) {
      LOGGER.debug("Failed to open TFBOS for harvested resource's input stream.", e);
    }
    return metacard;
  }

  private void enrichMetacard(Metacard metacard, HarvestedResource harvestedResource) {
    writeMetacardAttribute(metacard, Core.TITLE, harvestedResource.getName());
    writeMetacardAttribute(
        metacard, Core.RESOURCE_SIZE, Long.toString(harvestedResource.getSize()));
    writeMetacardAttribute(metacard, Core.RESOURCE_URI, harvestedResource.getUri().toASCIIString());
  }

  private Metacard doTransform(
      MimeType mimeType, InputStream is, Resource resource, String metacardId) {
    List<InputTransformer> transformerCandidates =
        mimeTypeToTransformerMapper.findMatches(InputTransformer.class, mimeType);

    for (InputTransformer inputTransformer : transformerCandidates) {
      Optional<Metacard> metacardOptional =
          doTransform(inputTransformer, metacardId, is, resource.getName());

      if (metacardOptional.isPresent()) {
        return metacardOptional.get();
      }
    }
    return null;
  }

  private Optional<Metacard> doTransform(
      InputTransformer inputTransformer, String metacardId, InputStream is, String resourceName) {
    try {
      if (StringUtils.isNotEmpty(metacardId)) {
        return Optional.of(inputTransformer.transform(is, metacardId));
      }

      return Optional.of(inputTransformer.transform(is));
    } catch (IOException e) {
      LOGGER.debug("Failed to retrieve inputStream for resource [{}].", resourceName, e);
    } catch (CatalogTransformerException e) {
      LOGGER.debug(
          "Failed to transform resource [{}] with [{}] transformer. Trying next one.",
          resourceName,
          inputTransformer);
    }

    return Optional.empty();
  }

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
