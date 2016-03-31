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
 */

package org.codice.ddf.catalog.content.plugin.clavin;

import java.io.File;
import java.io.IOException;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bericotech.clavin.ClavinException;

import ddf.catalog.plugin.PluginExecutionException;

@Command(scope = "geocoding", name = "createClavinIndex",
        description = "Create clavin index.")
public class ClavinCommand extends OsgiCommandSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClavinCommand.class);

    @Argument(index = 0, name = "resource",
            description = "Create clavin index from a local file resource, like allCountries.txt.",
            required = true)
    private String resource = null;

    public void setResource(String resource) {
        this.resource = resource;
    }

    private ClavinWrapper clavinWrapper;

    public void setClavinWrapper(ClavinWrapper clavinWrapper) {
        this.clavinWrapper = clavinWrapper;
    }

    @Override
    protected Object doExecute() throws PluginExecutionException {
        try {
            File resourceFile = new File(resource);
            if (resourceFile.exists() && resourceFile.isFile() && resourceFile.isAbsolute()) {
                clavinWrapper.createIndex(resourceFile);
            } else {
                throw new IOException();
            }
        } catch (NullPointerException | IOException | ClavinException e) {
            PluginExecutionException pe = new PluginExecutionException(
                    "Failed to create clavin index from: " + resource,
                    e);
            LOGGER.error("geonames:clavinCreateIndex command failed.", pe);
            throw pe;
        }
        return null;
    }

}
