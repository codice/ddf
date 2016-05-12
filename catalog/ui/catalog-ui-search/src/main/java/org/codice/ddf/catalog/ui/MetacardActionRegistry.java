/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.catalog.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.ActionRegistry;
import ddf.action.MultiActionProvider;

public class MetacardActionRegistry implements ActionRegistry {
    public List<ActionProvider> actionProviders;

    private List<MultiActionProvider> multiActionProviders;

    public MetacardActionRegistry(List<ActionProvider> actionProviders,
            List<MultiActionProvider> multiActionProviders) {
        this.actionProviders = actionProviders;
        this.multiActionProviders = multiActionProviders;
    }

    public <Metacard> List<Action> list(Metacard metacard) {
        ArrayList<Action> actions = new ArrayList<>(multiActionProviders.stream()
                .filter(provider -> provider.canHandle(metacard))
                .map(multiActionProvider -> multiActionProvider.getActions(metacard))
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));


        actions.addAll(actionProviders.stream()
                .map(actionProvider -> actionProvider.getAction(metacard))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        
        return actions;
    }
}
