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
package ddf.catalog.transformer.resource;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceRequestByMetacard;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.impl.ResourceImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This transformer uses the Catalog Framework to obtain and return the resource based on the
 * metacard id.
 */
public class ResourceMetacardTransformer implements MetacardTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceMetacardTransformer.class);

  private static final String DEFAULT_MIME_TYPE_STR = "application/octet-stream";

  private CatalogFramework catalogFramework;

  /**
   * Construct instance with required framework to resolve the resource
   *
   * @param framework
   */
  public ResourceMetacardTransformer(CatalogFramework framework) {
    LOGGER.debug("constructing resource metacard transformer");
    this.catalogFramework = framework;
  }

  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
      throws CatalogTransformerException {

    LOGGER.trace("Entering resource ResourceMetacardTransformer.transform");

    if (!isValid(metacard)) {
      throw new CatalogTransformerException(
          "Could not transform metacard to a resource because the metacard is not valid.");
    }

    if (StringUtils.isNotEmpty(metacard.getResourceSize())) {
      arguments.put(Metacard.RESOURCE_SIZE, metacard.getResourceSize());
    }

    String id = metacard.getId();
    LOGGER.debug("executing resource request with metacard '{}'", id);

    final ResourceRequest resourceRequest = new ResourceRequestByMetacard(metacard, arguments);

    ResourceResponse resourceResponse = null;

    String sourceName = metacard.getSourceId();

    if (StringUtils.isBlank(sourceName)) {
      sourceName = catalogFramework.getId();
    }

    String resourceUriAscii = "";
    if (metacard.getResourceURI() != null) {
      resourceUriAscii = metacard.getResourceURI().toASCIIString();
    }

    try {
      resourceResponse = catalogFramework.getResource(resourceRequest, sourceName);
    } catch (IOException | ResourceNotFoundException | ResourceNotSupportedException e) {
      throw new CatalogTransformerException(
          retrieveResourceFailureMessage(id, sourceName, resourceUriAscii, e.getMessage()), e);
    }

    if (resourceResponse == null) {
      throw new CatalogTransformerException(
          retrieveResourceFailureMessage(id, sourceName, resourceUriAscii));
    }

    Resource transformedContent = resourceResponse.getResource();
    MimeType mimeType = transformedContent.getMimeType();

    if (mimeType == null) {
      try {
        mimeType = new MimeType(DEFAULT_MIME_TYPE_STR);
        // There is no method to set the MIME type, so in order to set it to our default
        // one, we need to create a new object.
        transformedContent =
            new ResourceImpl(
                transformedContent.getInputStream(), mimeType, transformedContent.getName());
      } catch (MimeTypeParseException e) {
        throw new CatalogTransformerException(
            "Could not create default mime type upon null mimeType, for default mime type '"
                + DEFAULT_MIME_TYPE_STR
                + "'.",
            e);
      }
    }
    LOGGER.debug(
        "Found mime type: '{}' for product of metacard with id: '{}'.\nGetting associated resource from input stream. \n",
        mimeType,
        id);

    LOGGER.trace("Exiting resource transform for metacard id: '{}'", id);
    return transformedContent;
  }

  /**
   * Checks to see whether the given metacard is valid. If it is not valid, it will return false,
   * otherwise true.
   *
   * @param metacard The metacard to be validated.
   * @return boolean indicating validity.
   */
  private boolean isValid(Metacard metacard) {
    if (metacard == null) {
      LOGGER.debug("Metacard cannot be null");
      return false;
    }
    if (metacard.getId() == null) {
      LOGGER.debug("Metacard id cannot be null");
      return false;
    }
    return true;
  }

  private String retrieveResourceFailureMessage(
      final String id, final String sourceId, final String resourceUri) {
    return retrieveResourceFailureMessage(id, sourceId, resourceUri, null);
  }

  private String retrieveResourceFailureMessage(
      final String id, final String sourceId, final String resourceUri, final String details) {
    StringBuilder msg = new StringBuilder("Unable to retrieve resource.");
    msg.append("\n\tMetacard id: " + (id == null ? "" : id));
    msg.append("\n\tUri: " + (resourceUri == null ? "" : resourceUri));
    msg.append("\n\tSource: " + (sourceId == null ? "" : sourceId));
    msg.append("\n\tDetails: " + (details == null ? "" : details));
    return msg.toString();
  }
}
