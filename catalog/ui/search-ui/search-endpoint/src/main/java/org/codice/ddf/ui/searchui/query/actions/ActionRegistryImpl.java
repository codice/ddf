/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.ui.searchui.query.actions;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.ActionRegistry;

/**
 * The {@code ActionRegistryImpl} provides an ActionRegistry for {@code Metacard}s.
 */
public class ActionRegistryImpl implements ActionRegistry {

    public List<ActionProvider> providers;

    public ActionRegistryImpl(List<ActionProvider> actionProviders) {
        this.providers = actionProviders;
    }

    public <Metacard> List<Action> list(Metacard metacard) {
        List<Action> availableActions = new ArrayList<>();

        for (ActionProvider provider : providers) {
            if (provider.canHandle(metacard)) {
                List<Action> actions = provider.getActions(metacard);
                if (!CollectionUtils.isEmpty(actions)) {
                    availableActions.addAll(actions);
                }
            }
        }
        return availableActions;
    }
}
