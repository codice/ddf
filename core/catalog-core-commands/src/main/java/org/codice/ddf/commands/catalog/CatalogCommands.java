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
package org.codice.ddf.commands.catalog;

import java.io.PrintStream;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.catalog.facade.Framework;
import org.codice.ddf.commands.catalog.facade.Provider;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.osgi.util.tracker.ServiceTracker;

import ddf.catalog.CatalogFramework;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.source.CatalogProvider;

/**
 * Parent object to all Catalog Commands. Provides common methods and instance variables as well as
 * the extension of {@link OsgiCommandSupport} that Catalog Commands can use.
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

    protected static final String ESCAPE = "\\";

    protected static final String SINGLE_WILDCARD = "?";

    protected static final String WILDCARD = "*";

    protected static final double MILLISECONDS_PER_SECOND = 1000.0;

    protected static final int ONE_SECOND = 1000;

    protected static final double PERCENTAGE_MULTIPLIER = 100.0;

    protected static final int PROGESS_BAR_NOTCH_LENGTH = 50;

    protected PrintStream console = System.out;

    protected static final String DEFAULT_TRANSFORMER_ID = "ser";

    private static final Color ERROR_COLOUR = Ansi.Color.RED;
    private static final Color HEADER_COLOUR = Ansi.Color.CYAN;
    private static final Color SUCCESS_COLOUR = Ansi.Color.GREEN;

    // DDF-535: remove "-provider" alias in DDF 3.0
    @Option(name = "--provider", required = false, aliases = {"-p", "-provider"}, multiValued = false, description = "Interacts with the provider directly instead of the framework.")
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

    protected String dash(int length) {
        StringBuilder sBuilder = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sBuilder.append("-");
        }
        return sBuilder.toString();
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

    protected void printErrorMessage(String message) {
        printColor(ERROR_COLOUR, message);
    }

    protected void printHeaderMessage(String message) {
        printColor(HEADER_COLOUR, message);
    }

    protected void printSuccessMessage(String message) {
        printColor(SUCCESS_COLOUR, message);
    }

    protected void printProgressAndFlush(long start, long totalCount, long currentCount) {
        console.print(getProgressBar(currentCount, totalCount, start,
                System.currentTimeMillis()));
        console.flush();
    }

    private String getProgressBar(long currentCount, long totalPossible, long start, long end) {

        int notches = calculateNotches(currentCount, totalPossible);

        int progressPercentage = calculateProgressPercentage(currentCount, totalPossible);

        int rate = calculateRecordsPerSecond(currentCount, start, end);

        String progressArrow = ">";

        // /r is required, it allows for the update in place
        String progressBarFormat = "%1$4s%% [=%2$-50s] %3$5s records/sec\t\r";

        return String.format(progressBarFormat, progressPercentage,
                StringUtils.repeat("=", notches) + progressArrow, rate);
    }

    private int calculateNotches(long currentCount, long totalPossible) {
        return (int) ((Double.valueOf(currentCount) / Double.valueOf(totalPossible)) * PROGESS_BAR_NOTCH_LENGTH);
    }

    private int calculateProgressPercentage(long currentCount, long totalPossible) {
        return (int) ((Double.valueOf(currentCount) / Double.valueOf(totalPossible)) * PERCENTAGE_MULTIPLIER);
    }

    protected int calculateRecordsPerSecond(long currentCount, long start, long end) {
        return (int) (Double.valueOf(currentCount) / (Double.valueOf(end - start) / MILLISECONDS_PER_SECOND));
    }
}
