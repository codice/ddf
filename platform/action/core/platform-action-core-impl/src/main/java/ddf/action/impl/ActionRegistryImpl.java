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
package ddf.action.impl;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.ActionRegistry;
import ddf.action.MultiActionProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ActionRegistryImpl implements ActionRegistry {

  private List<ActionProvider> actionProviders;

  private List<MultiActionProvider> multiActionProviders;

  public ActionRegistryImpl(
      List<ActionProvider> actionProviders, List<MultiActionProvider> multiActionProviders) {
    this.actionProviders = actionProviders;
    this.multiActionProviders = multiActionProviders;
  }

  public ActionRegistryImpl() {
    this.actionProviders = new LinkedList<>();
    this.multiActionProviders = new LinkedList<>();
  }

  @Override
  public <T> List<Action> list(T subject) {
    ArrayList<Action> actions =
        multiActionProviders.stream()
            .filter(provider -> provider.canHandle(subject))
            .map(multiActionProvider -> multiActionProvider.getActions(subject))
            .flatMap(Collection::stream)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toCollection(ArrayList::new));

    actions.addAll(
        actionProviders.stream()
            .map(actionProvider -> actionProvider.getAction(subject))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet()));

    return actions;
  }

  public void addActionProvider(ActionProvider actionProvider) {
    actionProviders.add(actionProvider);
  }

  public void removeActionProvider(ActionProvider actionProvider) {
    actionProviders.removeIf(ap -> ap.getId().equals(actionProvider.getId()));
  }

  public void addMultiActionProvider(MultiActionProvider multiActionProvider) {
    multiActionProviders.add(multiActionProvider);
  }

  public void removeMultiActionProvider(MultiActionProvider multiActionProvider) {
    multiActionProviders.removeIf(
        currentMultiActionProvider ->
            currentMultiActionProvider.getId().equals(multiActionProvider.getId()));
  }
}
