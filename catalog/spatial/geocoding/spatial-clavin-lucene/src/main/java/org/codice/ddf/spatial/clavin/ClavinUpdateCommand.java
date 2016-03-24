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

package org.codice.ddf.spatial.clavin;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "geonames", name = "clavinUpdate",
        description = "Create clavin index.")
public final class ClavinUpdateCommand extends OsgiCommandSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClavinUpdateCommand.class);

    @Argument(index = 0, name = "resource",
            description = "The resource whose contents you wish to insert into the index.  "
                    + "When the resource is a country code (ex: AU), "
                    + "that country's data will be downloaded from geonames.org "
                    + "and added to the index.  `cities1000`, `cities5000` and "
                    + "`cities15000` can be used to get all of the cities with at "
                    + "least 1000, 5000, 15000 people respectively.  "
                    + "To download all country codes, use the keyword 'all'.  "
                    + "When the resource is a path to a file, it will be imported locally.",
            required = true)
    private String resource = null;

    @Option(name = "-c", aliases = "--create",
            description = "Create a new index, overwriting any existing index at the destination.")
    private boolean create;

    public void setResource(String resource) {
        this.resource = resource;
    }

    public void setClavinUpdateCommandImpl(ClavinUpdateCommandImpl clavinUpdateCommandImpl) {
        this.clavinUpdateCommandImpl = clavinUpdateCommandImpl;
    }

    private ClavinUpdateCommandImpl clavinUpdateCommandImpl;

    @Override
    protected Object doExecute() {
        System.out.println("\nCreating clavin index.");
        clavinUpdateCommandImpl.createIndex(resource);
        System.out.println("\nDone creating clavin index.");
        return null;
    }
}