/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.impl;

import java.net.URISyntaxException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceResponse;

/**
 * {@link CatalogFramework} proxy class used to transform the URIs found in a {@link SourceResponse}
 * {@link Metacard}s using the injected {@link ActionProvider} before transformation by the
 * {@link CatalogFramework}.
 */
public class QueryResponsePostProcessorImpl implements QueryResponsePostProcessor {
    // PreCatalogTransformPlugin {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(QueryResponsePostProcessorImpl.class);

    private ActionProvider resourceActionProvider;

    public QueryResponsePostProcessorImpl() {
        this.resourceActionProvider = null;
    }

    public QueryResponsePostProcessorImpl(
            ActionProvider resourceActionProvider) {
        this.resourceActionProvider = resourceActionProvider;
    }

    /**
     * Converts the resource URIs found in the {@link SourceResponse} {@link Metacard}s using the
     * {@link ActionProvider} injected in the constructor before delegating the call to the proxied
     * {@link CatalogFramework} object.
     */
    @Override
    public void processResponse(QueryResponse queryResponse) {
        // TODO Auto-generated method stub

        for (Result result : queryResponse.getResults()) {
            final Metacard metacard = result.getMetacard();

            if (metacard.getResourceURI() != null && resourceActionProvider != null) {
                Action action = resourceActionProvider.getAction(metacard);

                if (action != null) {
                    final URL resourceUrl = action.getUrl();

                    if (resourceUrl != null) {
                        try {
                            metacard.setAttribute(new AttributeImpl(Metacard.RESOURCE_DOWNLOAD_URL,
                                    resourceUrl.toURI().toString()));
                        } catch (URISyntaxException e) {
                            LOGGER.warn("Unable to retrieve '{}' from '{}' for metacard ID [{}]",
                                    Metacard.RESOURCE_URI, resourceActionProvider.getClass()
                                            .getName(), metacard.getId());
                        }
                    }
                }
            }
        }
    }
}
