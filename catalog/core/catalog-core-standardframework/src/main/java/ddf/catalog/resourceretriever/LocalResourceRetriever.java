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
package ddf.catalog.resourceretriever;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalResourceRetriever implements ResourceRetriever {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalResourceRetriever.class);

  private List<ResourceReader> resourceReaders;

  private URI resourceUri;

  private Map<String, Serializable> properties;

  private final Metacard resourceMetacard;

  public LocalResourceRetriever(
      List<ResourceReader> resourceReaders,
      URI resourceUri,
      Metacard resourceMetacard,
      Map<String, Serializable> properties) {
    this.resourceReaders = resourceReaders;
    this.resourceUri = resourceUri;
    this.resourceMetacard = resourceMetacard;
    this.properties = properties;
  }

  @Override
  public ResourceResponse retrieveResource() throws ResourceNotFoundException {
    return retrieveResource(0);
  }

  @Override
  public ResourceResponse retrieveResource(long bytesToSkip) throws ResourceNotFoundException {
    final String methodName = "retrieveResource";
    LOGGER.trace("ENTERING: {}", methodName);
    ResourceResponse resource = null;

    if (resourceUri == null) {
      throw new ResourceNotFoundException("Unable to find resource due to null URI");
    }

    Map<String, Serializable> props = new HashMap<>(properties);

    if (bytesToSkip > 0) {
      props.put(BYTES_TO_SKIP, bytesToSkip);
    }

    URI derivedUri = null;
    Serializable serializable = props.get(ContentItem.QUALIFIER);
    if (serializable != null && serializable instanceof String) {
      LOGGER.debug(
          "Received qualifier in request properties, looking for qualified content on metacard with id [{}]",
          resourceMetacard.getId());

      String fragment = (String) serializable;
      derivedUri = getDerivedUriWithFragment(resourceMetacard, fragment);
    }

    String scheme;
    URI resourceRetrievalUri;
    if (derivedUri == null) {
      scheme = resourceUri.getScheme();
      resourceRetrievalUri = resourceUri;
    } else {
      scheme = derivedUri.getScheme();
      resourceRetrievalUri = derivedUri;
    }

    for (ResourceReader reader : resourceReaders) {
      if (reader != null && reader.getSupportedSchemes().contains(scheme)) {
        try {
          LOGGER.debug(
              "Found an acceptable resource reader ({}) for URI {}",
              reader.getId(),
              resourceRetrievalUri.toASCIIString());
          resource = reader.retrieveResource(resourceRetrievalUri, props);
          if (resource != null) {
            break;
          } else {
            LOGGER.debug(
                "Resource returned from ResourceReader {} was null. Checking other readers for URI: {}",
                reader.getId(),
                resourceRetrievalUri);
          }
        } catch (ResourceNotFoundException | ResourceNotSupportedException | IOException e) {
          LOGGER.debug("Product not found using resource reader with name {}", reader.getId(), e);
        }
      }
    }

    if (resource == null) {
      throw new ResourceNotFoundException(
          "Resource Readers could not find resource (or returned null resource) for URI: "
              + resourceRetrievalUri.toASCIIString()
              + ". Scheme: "
              + resourceRetrievalUri.getScheme());
    }
    LOGGER.debug("Received resource, sending back: {}", resource.getResource().getName());
    LOGGER.trace("EXITING: {}", methodName);

    return resource;
  }

  private URI getDerivedUriWithFragment(Metacard metacard, String fragment) {
    if (!attributeIsPresent(metacard, Core.DERIVED_RESOURCE_URI)) {
      return null;
    }

    Attribute attribute = metacard.getAttribute(Core.DERIVED_RESOURCE_URI);

    List<Serializable> derivedUris = attribute.getValues();
    if (!derivedUris.isEmpty()) {
      for (Serializable uri : derivedUris) {
        if (!(uri instanceof String)) {
          continue;
        }

        URI derivedUri = null;
        try {
          derivedUri = new URI((String) uri);
        } catch (URISyntaxException e) {
          LOGGER.debug(
              "Received invalid [{}] [{}] on metacard with id [{}].",
              Core.DERIVED_RESOURCE_URI,
              uri,
              metacard.getId());
        }

        if (uriContainsFragment(derivedUri, fragment)) {
          return derivedUri;
        }
      }
    }

    return null;
  }

  private boolean attributeIsPresent(Metacard metacard, String attributeName) {
    if (metacard == null) {
      return false;
    }

    Attribute attribute = metacard.getAttribute(attributeName);
    if (attribute == null) {
      return false;
    }

    return attribute.getValue() != null || !CollectionUtils.isEmpty(attribute.getValues());
  }

  private boolean uriContainsFragment(URI uri, String fragment) {
    return uri != null && uri.getFragment().equals(fragment);
  }
}
