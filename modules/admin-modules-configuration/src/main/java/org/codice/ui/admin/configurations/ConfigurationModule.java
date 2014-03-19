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
package org.codice.ui.admin.configurations;

import java.net.URI;
import java.net.URISyntaxException;

import org.codice.ddf.ui.admin.api.module.AdminModule;

/**
 * Created by tustisos on 3/18/14.
 */
public class ConfigurationModule implements AdminModule {
    @Override
    public String getName() {
        return "Configurations";
    }

    @Override
    public String getId() {
        return "configurations";
    }

    @Override
    public URI getJSLocation() {
        try {
            return new URI("/configurations/module.js");
        } catch (URISyntaxException e) {
            //ignore
        }
        return null;
    }

    @Override
    public URI getCSSLocation() {
        try {
            return new URI("/configurations/css/style.css");
        } catch (URISyntaxException e) {
            //ignore
        }
        return null;
    }
}
