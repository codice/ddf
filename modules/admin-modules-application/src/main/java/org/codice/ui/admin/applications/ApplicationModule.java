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
package org.codice.ui.admin.applications;

import org.codice.ddf.ui.admin.api.module.AdminModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Application module class
 */
public class ApplicationModule implements AdminModule {
    private Logger logger = LoggerFactory.getLogger(ApplicationModule.class);

    @Override
    public String getName() {
        return "Applications";
    }

    @Override
    public String getId() {
        return "applications";
    }

    @Override
    public URI getJSLocation() {
        try {
            return new URI("/applications/js/module.js");
        } catch (URISyntaxException e) {
            logger.warn("Must set the JS location to a valid URI.", e);
        }
        return null;
    }

    @Override
    public URI getCSSLocation() {
        try {
            return new URI("/applications/css/style.css");
        } catch (URISyntaxException e) {
            logger.warn("Must set the JS location to a valid URI.", e);
        }
        return null;
    }

    @Override
    public URI getIframeLocation() {
        return null;
    }
}
