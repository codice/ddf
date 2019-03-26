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
package org.codice.ddf.catalog.ui.metacard.notes;

import static ddf.catalog.util.impl.ResultIterable.resultIterable;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

public class NoteUtil {

  private static final int PAGE_SIZE = 250;

  private final FilterBuilder filterBuilder;

  private final CatalogFramework catalogFramework;

  public NoteUtil(FilterBuilder filterBuilder, CatalogFramework catalogFramework) {
    this.filterBuilder = filterBuilder;
    this.catalogFramework = catalogFramework;
  }

  /**
   * Extracts the data required by the user from the note {@link Metacard} object. If any required
   * fields are null the return object will also be null.
   *
   * @param metacard - the metacard object to abstract response data from
   * @return map of the response note fields or null
   */
  @Nullable
  public Map<String, String> getResponseNote(Metacard metacard) {
    Map<String, String> requiredAttributes = new HashMap<>();
    if (metacard.getId() != null) {
      requiredAttributes.put("id", metacard.getId());
    } else {
      return null;
    }
    if (metacard.getAttribute(NoteConstants.PARENT_ID) != null) {
      requiredAttributes.put(
          "parent", (String) metacard.getAttribute(NoteConstants.PARENT_ID).getValue());
    } else {
      return null;
    }
    if (metacard.getAttribute(Core.CREATED).getValue() != null) {
      requiredAttributes.put("created", metacard.getAttribute(Core.CREATED).getValue().toString());
    }
    if (metacard.getAttribute(Core.MODIFIED).getValue() != null) {
      requiredAttributes.put(
          "modified", metacard.getAttribute(Core.MODIFIED).getValue().toString());
    }
    if (metacard.getAttribute(NoteConstants.COMMENT) != null) {
      requiredAttributes.put(
          "note", (String) metacard.getAttribute(NoteConstants.COMMENT).getValue());
    } else {
      return null;
    }
    if (metacard.getAttribute(Core.METACARD_OWNER) != null) {
      requiredAttributes.put(
          "owner", (String) metacard.getAttribute(Core.METACARD_OWNER).getValue());
    } else {
      return null;
    }
    return requiredAttributes;
  }

  /**
   * Returns all associated metacards given two relatable attributes.
   *
   * @param primaryFilterAttribute The primary attribute to search against
   * @param secondaryFilterAttribute The secondary attribute to search against
   * @param primaryAttributeValue A primary attribute value
   * @param secondaryAttributeValue A secondary attribute value
   * @return A set of metacards, filtered by the given attributes and it's values
   * @throws UnsupportedQueryException Error
   * @throws SourceUnavailableException Error
   * @throws FederationException Error
   */
  public List<Metacard> getAssociatedMetacardsByTwoAttributes(
      String primaryFilterAttribute,
      String secondaryFilterAttribute,
      String primaryAttributeValue,
      String secondaryAttributeValue)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {

    Filter idFilter =
        filterBuilder.attribute(primaryFilterAttribute).is().equalTo().text(primaryAttributeValue);
    Filter tagsFilter =
        filterBuilder
            .attribute(secondaryFilterAttribute)
            .is()
            .equalTo()
            .text(secondaryAttributeValue);
    Filter filter = filterBuilder.allOf(idFilter, tagsFilter);

    return resultIterable(
            catalogFramework,
            new QueryRequestImpl(
                new QueryImpl(
                    filter,
                    1,
                    PAGE_SIZE,
                    SortBy.NATURAL_ORDER,
                    true,
                    TimeUnit.SECONDS.toMillis(10)),
                false))
        .stream()
        .map(Result::getMetacard)
        .collect(Collectors.toList());
  }
}
