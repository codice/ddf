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
package org.codice.ddf.endpoints.rest.action;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.impl.ActionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class MetacardTransformerActionProvider extends AbstractMetacardActionProvider {

    static final String DESCRIPTION_PREFIX =
            "Provides a URL to the metacard that transforms the return value via the";
    static final String DESCRIPTION_SUFFIX = " transformer";

    static final String TITLE_PREFIX = "Export as ";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(MetacardTransformerActionProvider.class);

    private String metacardTransformerId;

    /**
     * Constructor to instantiate this Metacard {@link ActionProvider}
     * 
     * @param actionProviderId
     * @param metacardTransformerId
     */
    public MetacardTransformerActionProvider(String actionProviderId, String metacardTransformerId) {

        this.actionProviderId = actionProviderId;
        this.metacardTransformerId = metacardTransformerId;

    }

    @Override
    protected Action getAction(String metacardId, String metacardSource) {

        URL url = null;
        try {

            URI uri = new URI(protocol + host + ':' + port + contextRoot + PATH + "/"
                    + metacardSource + "/" + metacardId + "?transform=" + metacardTransformerId);
            url = uri.toURL();

        } catch (MalformedURLException e) {
            LOGGER.info("Malformed URL exception", e);
            return null;
        } catch (URISyntaxException e) {
            LOGGER.info("URI Syntax exception", e);
            return null;
        }

        return new ActionImpl(getId(), TITLE_PREFIX + metacardTransformerId, DESCRIPTION_PREFIX
                + metacardTransformerId + DESCRIPTION_SUFFIX, url);

    }

    @Override
    public String toString() {

        return ActionProvider.class.getName() + " [" + getId() + "], Impl=" + getClass().getName();
    }
}
