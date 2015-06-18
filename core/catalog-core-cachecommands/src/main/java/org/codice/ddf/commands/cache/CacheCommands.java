/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.commands.cache;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import ddf.catalog.cache.solr.impl.SolrCacheMBean;

public abstract class CacheCommands extends OsgiCommandSupport {

    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat
            .forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");

    protected static final String NAMESPACE = "cache";

    protected PrintStream console = System.out;

    protected static final double MILLISECONDS_PER_SECOND = 1000.0;

    private static final Color ERROR_COLOR = Ansi.Color.RED;

    private static final Color SUCCESS_COLOR = Ansi.Color.GREEN;

    private static final Color HEADER_COLOR = Ansi.Color.CYAN;

    protected abstract Object doExecute() throws Exception;

    protected SolrCacheMBean getCacheProxy() throws IOException, MalformedObjectNameException,
        InstanceNotFoundException {


        ObjectName solrCacheObjectName = new ObjectName(SolrCacheMBean.OBJECTNAME);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        return MBeanServerInvocationHandler.newProxyInstance(mBeanServer, solrCacheObjectName,
                SolrCacheMBean.class, false);

    }

    protected void printColor(Color color, String message) {
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

    protected void printSuccessMessage(String message) {
        printColor(SUCCESS_COLOR, message);
    }

    protected void printErrorMessage(String message) {
        printColor(ERROR_COLOR, message);
    }

    protected void printHeaderMessage(String message) {
        printColor(HEADER_COLOR, message);
    }

}
