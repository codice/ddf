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
package ddf.catalog.plugin.facetattributeaccess;

import static ddf.catalog.Constants.EXPERIMENTAL_FACET_PROPERTIES_KEY;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.TermFacetProperties;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.StopProcessingException;
import java.util.Map;
import java.util.Set;

/**
 * Security-based plugin that checks to make sure that attributes attached to faceted query requests
 * are valid as defined by the whitelist in the admin console.
 */
public class FacetAttributeAccessPlugin implements AccessPlugin {

  private FacetWhitelistConfiguration config;

  public FacetAttributeAccessPlugin(FacetWhitelistConfiguration config) {
    this.config = config;
  }

  @Override
  public CreateRequest processPreCreate(CreateRequest input) throws StopProcessingException {
    return input;
  }

  @Override
  public UpdateRequest processPreUpdate(
      UpdateRequest input, Map<String, Metacard> existingMetacards) throws StopProcessingException {
    return input;
  }

  @Override
  public DeleteRequest processPreDelete(DeleteRequest input) throws StopProcessingException {
    return input;
  }

  @Override
  public DeleteResponse processPostDelete(DeleteResponse input) throws StopProcessingException {
    return input;
  }

  @Override
  public QueryRequest processPreQuery(QueryRequest input) throws StopProcessingException {
    if (input.getProperties().get(EXPERIMENTAL_FACET_PROPERTIES_KEY)
        instanceof TermFacetProperties) {
      TermFacetProperties facetProperties =
          (TermFacetProperties) input.getProperties().get(EXPERIMENTAL_FACET_PROPERTIES_KEY);
      Set<String> facetAttributes = facetProperties.getFacetAttributes();

      for (String attr : facetAttributes) {
        if (!config.getFacetAttributeWhitelist().contains(attr)) {
          throw new StopProcessingException("Invalid Facet Attribute Detected: " + attr);
        }
      }
    }

    return input;
  }

  @Override
  public QueryResponse processPostQuery(QueryResponse input) throws StopProcessingException {
    return input;
  }

  @Override
  public ResourceRequest processPreResource(ResourceRequest input) throws StopProcessingException {
    return input;
  }

  @Override
  public ResourceResponse processPostResource(ResourceResponse input, Metacard metacard)
      throws StopProcessingException {
    return input;
  }
}
