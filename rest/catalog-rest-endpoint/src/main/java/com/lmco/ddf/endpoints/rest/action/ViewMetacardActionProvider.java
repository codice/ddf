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

import org.apache.log4j.Logger;

import ddf.action.Action;
import ddf.action.impl.ActionImpl;


public class ViewMetacardActionProvider extends AbstractMetacardActionProvider {

    public static final String TITLE = "View Complete Metacard";
    public static final String DESCRIPTION = "Provides a URL to the metacard";
    static final Logger LOGGER = Logger.getLogger(ViewMetacardActionProvider.class);
    
    public ViewMetacardActionProvider(String id) {
        this.actionProviderId = id;
    }

    @Override
    protected Action getAction(String metacardId, String metacardSource) {

        URL url = null;
        try {

            URI uri = new URI(protocol + host + ':' + port + contextRoot
                    + PATH + "/" + metacardSource + "/" + metacardId);
            url = uri.toURL();

        } catch (MalformedURLException e) {
            LOGGER.info(e);
            return null;
        } catch (URISyntaxException e) {
            LOGGER.info(e);
            return null;
        }
        
        return new ActionImpl(getId(), TITLE, DESCRIPTION, url);
        
    }


    
}
