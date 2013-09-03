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
package com.lmco.ddf.commands.catalog;

import java.io.PrintStream;

import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.osgi.util.tracker.ServiceTracker;

import com.lmco.ddf.commands.catalog.facade.CatalogFacade;
import com.lmco.ddf.commands.catalog.facade.Framework;
import com.lmco.ddf.commands.catalog.facade.Provider;

import ddf.catalog.CatalogFramework;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.source.CatalogProvider;

/**
 * Parent object to all Catalog Commands. Provides common methods and instance
 * variables as well as the extension of {@link OsgiCommandSupport} that Catalog
 * Commands can use.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class CatalogCommands extends OsgiCommandSupport {

    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat
            .forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");

    public static final String NAMESPACE = "catalog";

    protected static final int DEFAULT_NUMBER_OF_ITEMS = 15;

    static final String ESCAPE = "\\";

    static final String SINGLE_WILDCARD = "?";

    static final String WILDCARD = "*";

    static final double MILLISECONDS_PER_SECOND = 1000.0;

    private static final int ONE_SECOND = 1000;

    @Option(name = "-p", required = false, aliases = {"-provider"}, multiValued = false, description = "Interacts with the provider directly instead of the framework.")
    boolean isProvider = false;

    @Override
    protected Object doExecute() throws Exception {
        return null;
    }

    protected CatalogFacade getCatalog() throws InterruptedException {

        if (isProvider) {
            return new Provider(getService(CatalogProvider.class));
        } else {
            // otherwise use default
            return new Framework(getService(CatalogFramework.class));
        }
    }

    protected FilterBuilder getFilterBuilder() throws InterruptedException {
        return getService(FilterBuilder.class);
    }

    protected <T> T getService(Class<T> classObject)
            throws InterruptedException {

        ServiceTracker st = new ServiceTracker(getBundleContext(),
                classObject.getName(), null);
        st.open();

        @SuppressWarnings("unchecked")
        T service = (T) st.waitForService(ONE_SECOND);
        try {
            if (service == null) {
                throw new InterruptedException("Could not find a service for: "
                        + classObject.getName());
            }
        } finally {
            st.close();
        }

        return service;
    }

    protected String dash(int length) {
        StringBuilder sBuilder = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sBuilder.append("-");
        }
        return sBuilder.toString();
    }

    protected void printColor(PrintStream console, Color color, String message) {
        String colorString;
        if (color == null || color.equals(Ansi.Color.DEFAULT)) {
            colorString = Ansi.ansi().reset().toString();
        } else {
            colorString = Ansi.ansi().fg(color).toString();
        }
        console.print(colorString);
        console.print(message);
        console.println(Ansi.ansi().reset().toString());
    }
}
