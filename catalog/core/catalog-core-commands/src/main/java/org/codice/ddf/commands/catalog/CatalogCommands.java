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
package org.codice.ddf.commands.catalog;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.felix.gogo.commands.Option;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.catalog.facade.Framework;
import org.codice.ddf.commands.catalog.facade.Provider;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import ddf.catalog.CatalogFramework;
import ddf.catalog.cache.SolrCacheMBean;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.source.CatalogProvider;

/**
 * Parent object to all Catalog Commands. Provides common methods and instance variables as well as
 * the extension of {@link org.apache.karaf.shell.console.OsgiCommandSupport}  and
 * {@link SubjectCommands} that Catalog Commands can use.
 *
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 */
public abstract class CatalogCommands extends SubjectCommands {

    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat
            .forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");

    public static final String NAMESPACE = "catalog";

    protected static final int DEFAULT_NUMBER_OF_ITEMS = 15;

    protected static final String ESCAPE = "\\";

    protected static final String SINGLE_WILDCARD = "?";

    protected static final String WILDCARD = "*";

    protected static final String DEFAULT_TRANSFORMER_ID = "ser";

    // DDF-535: remove "-provider" alias in DDF 3.0
    @Option(name = "--provider", required = false, aliases = {"-p",
            "-provider"}, multiValued = false, description = "Interacts with the provider directly instead of the framework.")
    boolean isProvider = false;

    protected SolrCacheMBean getCacheProxy()
            throws IOException, MalformedObjectNameException, InstanceNotFoundException {

        ObjectName solrCacheObjectName = new ObjectName(SolrCacheMBean.OBJECTNAME);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        return MBeanServerInvocationHandler
                .newProxyInstance(mBeanServer, solrCacheObjectName, SolrCacheMBean.class, false);

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

}
