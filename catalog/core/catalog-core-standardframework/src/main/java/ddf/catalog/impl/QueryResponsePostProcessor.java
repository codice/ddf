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
package ddf.catalog.impl;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.MultiActionProvider;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.QueryResponse;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class called by the catalog framework after all the results have been received from the different
 * federated sources, before any of the post-query plug-ins are called. The current implementation
 * adds the {@link Metacard#RESOURCE_DOWNLOAD_URL} attribute to all the {@link Metacard} objects
 * contained in the {@link QueryResponse}. The download URL is generated using the {@link
 * ActionProvider} whose ID is "catalog.data.metacard.resource".
 */
public class QueryResponsePostProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(QueryResponsePostProcessor.class);

  private MultiActionProvider derivedMultiActionProvider;

  private ActionProvider resourceActionProvider;

  public QueryResponsePostProcessor(
      ActionProvider resourceActionProvider, MultiActionProvider derivedActionProvider) {
    this.resourceActionProvider = resourceActionProvider;
    this.derivedMultiActionProvider = derivedActionProvider;
  }

  /**
   * Performs any required post-processing on the {@link QueryResponse} object provided.
   *
   * <p>This implementation converts the resource URIs found in the {@link QueryResponse} {@link
   * Metacard}s using the {@link ActionProvider} injected in the constructor.
   *
   * @param queryResponse {@link QueryResponse} to process. Cannot be <code>null</code>.
   */
  public void processResponse(QueryResponse queryResponse) {

    if (resourceActionProvider == null && derivedMultiActionProvider == null) {
      LOGGER.debug(
          "No ActionProvider, skipping addition of {} attribute in Metacards",
          Metacard.RESOURCE_DOWNLOAD_URL);
      return;
    }

    for (Result result : queryResponse.getResults()) {
      final Metacard metacard = result.getMetacard();

      if (metacard.getResourceURI() != null && resourceActionProvider != null) {
        Action action = resourceActionProvider.getAction(metacard);

        if (action != null) {
          final URL resourceUrl = action.getUrl();

          if (resourceUrl != null) {
            metacard.setAttribute(
                new AttributeImpl(Metacard.RESOURCE_DOWNLOAD_URL, resourceUrl.toString()));
          }
        }
      }
      if (metacard.getAttribute(Metacard.DERIVED_RESOURCE_URI) != null
          && !metacard.getAttribute(Metacard.DERIVED_RESOURCE_URI).getValues().isEmpty()
          && derivedMultiActionProvider != null) {
        List<Action> actions = derivedMultiActionProvider.getActions(metacard);

        if (!CollectionUtils.isEmpty(actions)) {
          metacard.setAttribute(
              new AttributeImpl(
                  Metacard.DERIVED_RESOURCE_DOWNLOAD_URL,
                  actions.stream()
                      .map(action -> action.getUrl().toString())
                      .collect(Collectors.toList())));
        }
      }
    }
  }
}
