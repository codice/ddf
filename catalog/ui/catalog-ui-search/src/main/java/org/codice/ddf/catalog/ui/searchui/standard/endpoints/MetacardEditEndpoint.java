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
package org.codice.ddf.catalog.ui.searchui.standard.endpoints;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.codice.ddf.catalog.ui.util.EndpointUtil;

@Path("/edit")
public class MetacardEditEndpoint {

  private final CatalogFramework catalogFramework;

  private final AttributeRegistry attributeRegistry;

  private final EndpointUtil endpointUtil;

  public MetacardEditEndpoint(
      CatalogFramework catalogFramework,
      AttributeRegistry attributeRegistry,
      EndpointUtil endpointUtil) {
    this.catalogFramework = catalogFramework;
    this.attributeRegistry = attributeRegistry;
    this.endpointUtil = endpointUtil;
  }

  @GET
  @Path("/{id}/{attribute}")
  public Response getAttribute(
      @Context HttpServletResponse response,
      @PathParam("id") String id,
      @PathParam("attribute") String attribute)
      throws MetacardEndpointException {
    try {
      Metacard metacard = endpointUtil.getMetacardById(id);
      Attribute metacardAttribute = metacard.getAttribute(attribute);
      if (metacardAttribute == null) {
        return Response.status(200).build();
      }
      Optional<AttributeDescriptor> attributeDescriptor = attributeRegistry.lookup(attribute);
      if (!attributeDescriptor.isPresent()) {
        /* Could not find attribute descriptor for requested attribute */
        return Response.status(404).build();
      }

      AttributeDescriptor descriptor = attributeDescriptor.get();
      Map<String, Object> result = getResponseMap(attribute, metacardAttribute, descriptor);
      return Response.ok(endpointUtil.getJson(result), MediaType.APPLICATION_JSON).build();
    } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
      throw new MetacardEndpointException("Error retrieving metacard attribute.", e);
    }
  }

  @PUT
  @Path("/{id}/{attribute}")
  @Consumes(MediaType.TEXT_PLAIN)
  public Response setAttribute(
      @Context HttpServletResponse response,
      @PathParam("id") String id,
      @PathParam("attribute") String attribute,
      String value)
      throws MetacardEndpointException {
    try {
      Metacard metacard = endpointUtil.getMetacardById(id);
      if (metacard == null) {
        return Response.status(404).build();
      }

      Attribute metacardAttribute = metacard.getAttribute(attribute);
      Optional<AttributeDescriptor> attributeDescriptor = attributeRegistry.lookup(attribute);
      if (!attributeDescriptor.isPresent()) {
        /* Could not find attribute descriptor for requested attribute */
        return Response.status(404).build();
      }

      AttributeDescriptor descriptor = attributeDescriptor.get();
      if (descriptor.isMultiValued()) {
        if (metacardAttribute == null || metacardAttribute.getValues() == null) {
          metacard.setAttribute(new AttributeImpl(attribute, Collections.singletonList(value)));
        } else {
          List<Serializable> values = new ArrayList<>(metacardAttribute.getValues());
          if (!values.contains(value)) {
            values.add(value);
          }
          metacard.setAttribute(new AttributeImpl(attribute, values));
        }
      } else { // not multivalued
        metacard.setAttribute(new AttributeImpl(attribute, value));
      }

      catalogFramework.update(new UpdateRequestImpl(id, metacard));
      Map<String, Object> responseMap =
          getResponseMap(attribute, metacard.getAttribute(attribute), descriptor);
      return Response.ok(endpointUtil.getJson(responseMap), MediaType.APPLICATION_JSON).build();
    } catch (UnsupportedQueryException
        | SourceUnavailableException
        | FederationException
        | IngestException e) {
      throw new MetacardEndpointException("Error updating metacard attribute.", e);
    }
  }

  @PUT
  @Path("/{id}/{attribute}")
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @SuppressWarnings("squid:S2175" /* byte[] in the list is appropriate */)
  public Response setBinaryAttribute(
      @Context HttpServletResponse response,
      @PathParam("id") String id,
      @PathParam("attribute") String attribute,
      byte[] value)
      throws MetacardEndpointException {
    try {
      Metacard metacard = endpointUtil.getMetacardById(id);
      if (metacard == null) {
        return Response.status(404).build();
      }

      Attribute metacardAttribute = metacard.getAttribute(attribute);
      Optional<AttributeDescriptor> attributeDescriptor = attributeRegistry.lookup(attribute);
      if (!attributeDescriptor.isPresent()) {
        /* Could not find attribute descriptor for requested attribute */
        response.setStatus(404);
        return Response.status(404).build();
      }
      AttributeDescriptor descriptor = attributeDescriptor.get();
      if (!descriptor.getType().getAttributeFormat().equals(AttributeType.AttributeFormat.BINARY)) {
        return Response.status(400).build();
      }

      if (descriptor.isMultiValued()) {
        List<Serializable> values;
        if (metacardAttribute == null) {
          values = new ArrayList<>();
        } else {
          values = metacardAttribute.getValues();
        }
        if (!values.contains(value)) {
          values.add(value);
        }
        metacard.setAttribute(new AttributeImpl(attribute, values));
      } else {
        metacard.setAttribute(new AttributeImpl(attribute, value));
      }

      catalogFramework.update(new UpdateRequestImpl(id, metacard));
      Map<String, Object> responseMap =
          getResponseMap(attribute, metacard.getAttribute(attribute), descriptor);
      return Response.ok(endpointUtil.getJson(responseMap), MediaType.APPLICATION_JSON).build();
    } catch (IngestException
        | SourceUnavailableException
        | UnsupportedQueryException
        | FederationException e) {
      throw new MetacardEndpointException("Error updating binary metacard attribute.", e);
    }
  }

  @DELETE
  @Path("/{id}/{attribute}")
  public Response deleteAttribute(
      @Context HttpServletResponse response,
      @PathParam("id") String id,
      @PathParam("attribute") String attribute,
      String value)
      throws MetacardEndpointException {
    try {
      Metacard metacard = endpointUtil.getMetacardById(id);
      Attribute metacardAttribute = metacard.getAttribute(attribute);

      if (metacardAttribute == null) {
        return Response.ok().build();
      }

      metacard.setAttribute(new AttributeImpl(attribute, (Serializable) null));
      catalogFramework.update(new UpdateRequestImpl(id, metacard));
      return Response.ok().build();
    } catch (UnsupportedQueryException
        | SourceUnavailableException
        | FederationException
        | IngestException e) {
      throw new MetacardEndpointException("Error deleting metacard attribute.", e);
    }
  }

  private Map<String, Object> getResponseMap(
      String attribute, Attribute metacardAttribute, AttributeDescriptor descriptor) {
    Map<String, Object> result = new HashMap<>();
    result.put("multivalued", descriptor.isMultiValued());
    result.put("type", descriptor.getType().getAttributeFormat().name());
    if (descriptor.isMultiValued()) {
      result.put(attribute, metacardAttribute.getValues());

    } else {
      result.put(attribute, metacardAttribute.getValue());
    }
    return result;
  }
}
