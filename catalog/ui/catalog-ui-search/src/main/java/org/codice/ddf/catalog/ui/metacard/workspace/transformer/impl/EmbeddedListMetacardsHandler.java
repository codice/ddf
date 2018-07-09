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
import com.google.common.collect.ImmutableMap;
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

public class EmbeddedListMetacardsHandler extends EmbeddedMetacardsHandler {
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

    final Optional<List> listMetacardsOptional =
        super.metacardValueToJsonValue(transformer, metacardXMLStrings, workspaceMetacard);

    listMetacardsOptional.ifPresent(
        listMetacards ->
            ((List<Object>) listMetacards)
                .stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .forEach(
                    listMetacardMap ->
                        addListActionsToListMetacard(
                            listMetacardMap, workspaceMetacard, transformer)));

    return listMetacardsOptional;
  }

  private void addListActionsToListMetacard(
      Map listMetacardAsMap, Metacard workspaceMetacard, WorkspaceTransformer transformer) {
    final Metacard listMetacard = new ListMetacardImpl();
    transformer.transformIntoMetacard(listMetacardAsMap, listMetacard);
    final List<Map<String, Object>> listActions = getListActions(workspaceMetacard, listMetacard);
    listMetacardAsMap.put(ACTIONS_KEY, listActions);
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

    return super.jsonValueToMetacardValue(transformer, metacardJsonData);
  }

  private void removeExternalListAttributes(Map listMetacardMap) {
    EXTERNAL_LIST_ATTRIBUTES.forEach(listMetacardMap::remove);
  }

  /**
   * Given a {@link org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl} and a {@link
   * org.codice.ddf.catalog.ui.metacard.workspace.ListMetacardImpl}, get a list of actions that can
   * be executed on a list.
   */
  private List<Map<String, Object>> getListActions(
      Metacard workspaceMetacard, Metacard listMetacard) {
    final Map<String, Metacard> listContext =
        ImmutableMap.of("workspace", workspaceMetacard, "list", listMetacard);

    return actionRegistry
        .list(listContext)
        .stream()
        .filter(action -> action.getId().startsWith(LIST_ACTION_PREFIX))
        .map(
            action -> {
              final Map<String, Object> actionMap = new HashMap<>();
              actionMap.put("id", action.getId());
              actionMap.put("url", action.getUrl());
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
