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
package org.codice.ddf.commands.solr;

import java.io.PrintStream;
import java.util.Map;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SolrCommands extends OsgiCommandSupport {

    protected static final Logger LOGGER = LoggerFactory.getLogger(SolrCommands.class);

    protected static Map<String, String> configurationMap;

    protected static final String NAMESPACE = "solr";

    protected PrintStream console = System.out;

    private static final Color ERROR_COLOR = Ansi.Color.RED;

    private static final Color SUCCESS_COLOR = Ansi.Color.GREEN;

    protected ConfigurationAdmin configurationAdmin;

    protected abstract Object doExecute() throws Exception;

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        LOGGER.info("setConfigurationAdmin: {}", configurationAdmin);
        this.configurationAdmin = configurationAdmin;
    }

    protected HttpWrapper getHttpClient() {
        return new SolrHttpWrapper();
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

}
