/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package com.lmco.ddf.endpoints.rest.action;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.impl.ActionImpl;

public class MetacardTransformerActionProvider extends
        AbstractMetacardActionProvider {

    static final String DESCRIPTION_PREFIX = "Gets the Metacard ";

    static final String TITLE_PREFIX = "Get ";

    private String metacardTransformerId;

    /**
     * Constructor to instantiate this Metacard {@link ActionProvider}
     * 
     * @param actionProviderId
     * @param metacardTransformerId
     */
    public MetacardTransformerActionProvider(String actionProviderId,
            String metacardTransformerId) {

        this.actionProviderId = actionProviderId;
        this.metacardTransformerId = metacardTransformerId;

    }

    @Override
    protected Action getAction(String metacardId, String metacardSource) {

        URL url = null;
        try {

            URI uri = new URI(protocol + host + ':' + port + contextRoot + PATH
                    + "/" + metacardSource + "/" + metacardId + "?transform="
                    + metacardTransformerId);
            url = uri.toURL();

        } catch (MalformedURLException e) {
            LOGGER.info(e);
            return null;
        } catch (URISyntaxException e) {
            LOGGER.info(e);
            return null;
        }

        return new ActionImpl(getId(), TITLE_PREFIX + metacardTransformerId,
                DESCRIPTION_PREFIX + metacardTransformerId, url);

    }

    @Override
    public String toString() {

        return ActionProvider.class.getName() + " [" + getId() + "], Impl=" + getClass().getName();
    }
}
