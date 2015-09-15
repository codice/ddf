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

import java.io.PrintStream;

import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.jansi.Ansi;

/**
 * CommandSupport provides printing and progress bar support for
 * extending classes
 */
public abstract class CommandSupport extends OsgiCommandSupport {

    protected static final double MS_PER_SECOND = 1000.0;

    protected static final double PERCENTAGE_MULTIPLIER = 100.0;

    protected static final int PROGESS_BAR_NOTCH_LENGTH = 50;

    private static final Ansi.Color ERROR_COLOR = Ansi.Color.RED;

    private static final Ansi.Color HEADER_COLOR = Ansi.Color.CYAN;

    private static final Ansi.Color SUCCESS_COLOR = Ansi.Color.GREEN;

    protected PrintStream console = System.out;

    protected String dash(int length) {
        StringBuilder sBuilder = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sBuilder.append("-");
        }
        return sBuilder.toString();
    }

    protected void printColor(Ansi.Color color, String message) {
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
        printColor(ERROR_COLOR, message);
    }

    protected void printHeaderMessage(String message) {
        printColor(HEADER_COLOR, message);
    }

    protected void printSuccessMessage(String message) {
        printColor(SUCCESS_COLOR, message);
    }

    protected void printProgressAndFlush(long start, long totalCount, long currentCount) {
        console.print(getProgressBar(currentCount, totalCount, start, System.currentTimeMillis()));
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
        return (int) ((Double.valueOf(currentCount) / Double.valueOf(totalPossible))
                * PROGESS_BAR_NOTCH_LENGTH);
    }

    private int calculateProgressPercentage(long currentCount, long totalPossible) {
        return (int) ((Double.valueOf(currentCount) / Double.valueOf(totalPossible))
                * PERCENTAGE_MULTIPLIER);
    }

    protected int calculateRecordsPerSecond(long currentCount, long start, long end) {
        return (int) (Double.valueOf(currentCount) / (Double.valueOf(end - start)
                / MS_PER_SECOND));
    }
}
