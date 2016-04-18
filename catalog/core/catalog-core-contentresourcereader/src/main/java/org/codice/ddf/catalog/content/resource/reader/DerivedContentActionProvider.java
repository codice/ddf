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
 */
package org.codice.ddf.catalog.content.resource.reader;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.impl.ActionImpl;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.Metacard;

public class DerivedContentActionProvider implements ActionProvider {

    private static final String ID = "catalog.data.metacard.derived-content";

    private static final Logger LOGGER = LoggerFactory.getLogger(DerivedContentActionProvider.class);

    private final ActionProvider resourceActionProvider;

    private static final String DESCRIPTION_PREFIX = "Retrieves derived resource: ";

    private static final String OPTIONS = "options";

    public DerivedContentActionProvider(ActionProvider actionProvider) {
        this.resourceActionProvider = actionProvider;
    }

    @Override
    public <T> List<Action> getActions(T input) {
        if (!canHandle(input)) {
            return Collections.emptyList();
        }
        // Expect only 1
        List<Action> resourceActions = resourceActionProvider.getActions(input);
        if (resourceActions.isEmpty()) {
            return Collections.emptyList();
        }
        List<Action> actions = new ArrayList<>();
        ((Metacard) input).getAttribute(Metacard.DERIVED_RESOURCE_URI)
                .getValues()
                .stream()
                .forEach(value -> {
                    try {
                        URI uri = new URI(value.toString());
                        URIBuilder builder = new URIBuilder(resourceActions.get(0)
                                .getUrl()
                                .toURI());
                        if (StringUtils.equals(uri.getScheme(), ContentItem.CONTENT_SCHEME)) {
                            String qualifier = uri.getFragment();

                            builder.addParameters(Arrays.asList(new BasicNameValuePair(OPTIONS,
                                    qualifier)));
                            ActionImpl newAction = new ActionImpl(ID,
                                    "View " + qualifier,
                                    DESCRIPTION_PREFIX + qualifier,
                                    builder.build()
                                            .toURL());
                            actions.add(newAction);
                        } else {
                            ActionImpl newAction = new ActionImpl(ID,
                                    "View " + uri.toString(),
                                    DESCRIPTION_PREFIX + uri.toString(),
                                    uri.toURL());
                            actions.add(newAction);
                        }
                    } catch (URISyntaxException | MalformedURLException e) {
                        LOGGER.debug("Unable to create action URL.", e);
                    }
                });
        return actions;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public <T> boolean canHandle(T subject) {
        if (subject != null && Metacard.class.isAssignableFrom(subject.getClass())) {
            Metacard metacard = (Metacard) subject;
            if (metacard.getAttribute(Metacard.DERIVED_RESOURCE_URI) != null
                    && !metacard.getAttribute(Metacard.DERIVED_RESOURCE_URI)
                    .getValues()
                    .isEmpty()) {
                return true;
            }
        }
        return false;
    }

}
