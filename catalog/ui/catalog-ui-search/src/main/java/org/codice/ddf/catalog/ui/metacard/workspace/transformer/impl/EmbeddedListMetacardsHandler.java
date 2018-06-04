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
package org.codice.ddf.catalog.ui.metacard.workspace.transformer.impl;

import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import ddf.action.ActionRegistry;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.codice.ddf.catalog.ui.metacard.workspace.ListMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceConstants;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.EmbeddedMetacardsHandler;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceTransformer;
import org.codice.ddf.configuration.SystemBaseUrl;

public class EmbeddedListMetacardsHandler implements EmbeddedMetacardsHandler {
  @VisibleForTesting static final String LIST_ACTION_PREFIX = "catalog.data.metacard.list";

  @VisibleForTesting static final String ACTIONS_KEY = "actions";

  private static final Set<String> EXTERNAL_LIST_ATTRIBUTES = Collections.singleton(ACTIONS_KEY);

  private final ActionRegistry actionRegistry;

  public EmbeddedListMetacardsHandler(ActionRegistry actionRegistry) {
    this.actionRegistry = actionRegistry;
  }

  /** {@inheritDoc} Add "actions" key to list metacards. */
  @Override
  public Optional<List> metacardValueToJsonValue(
      WorkspaceTransformer transformer, List metacardXMLStrings, Metacard workspaceMetacard) {

    final List<Map<String, Object>> listActions = getListActions(workspaceMetacard);

    final Optional<List> listMetacardsOptional =
        EmbeddedMetacardsHandler.super.metacardValueToJsonValue(
            transformer, metacardXMLStrings, workspaceMetacard);

    listMetacardsOptional.ifPresent(
        listMetacards ->
            ((List<Object>) listMetacards)
                .stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .forEach(listMetacardMap -> listMetacardMap.put(ACTIONS_KEY, listActions)));

    return listMetacardsOptional;
  }

  /** {@inheritDoc} Remove "actions" key from list metacard map. */
  @Override
  public Optional<List> jsonValueToMetacardValue(
      WorkspaceTransformer transformer, List metacardJsonData) {

    ((List<Object>) metacardJsonData)
        .stream()
        .filter(Map.class::isInstance)
        .map(Map.class::cast)
        .forEach(this::removeExternalListAttributes);

    return EmbeddedMetacardsHandler.super.jsonValueToMetacardValue(transformer, metacardJsonData);
  }

  private void removeExternalListAttributes(Map listMetacardMap) {
    EXTERNAL_LIST_ATTRIBUTES.forEach(listMetacardMap::remove);
  }

  /**
   * Given a {@link org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl}, get a list
   * of actions that can be executed on a list.
   */
  private List<Map<String, Object>> getListActions(Metacard workspaceMetacard) {
    final String host = SystemBaseUrl.EXTERNAL.getBaseUrl();

    return actionRegistry
        .list(workspaceMetacard)
        .stream()
        .filter(action -> action.getId().startsWith(LIST_ACTION_PREFIX))
        .map(
            action -> {
              // Work-around for paths being behind VPCs with non-public DNS values
              final String url = action.getUrl().toString().replaceFirst(host, "");
              final Map<String, Object> actionMap = new HashMap<>();
              actionMap.put("id", action.getId());
              actionMap.put("url", url);
              actionMap.put("title", action.getTitle());
              actionMap.put("description", action.getDescription());
              return actionMap;
            })
        .collect(toList());
  }

  @Override
  public String getKey() {
    return WorkspaceConstants.WORKSPACE_LISTS;
  }

  @Override
  public MetacardType getMetacardType() {
    return ListMetacardImpl.TYPE;
  }
}
