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
package org.codice.ddf.cli.ui;

import com.github.rvesse.airline.io.colors.BasicColor;
import com.github.rvesse.airline.io.output.AnsiBasicColorizedOutputStream;
import com.github.rvesse.airline.io.output.ColorizedOutputStream;

/**
 * Controls notifications to the cli user interface
 */
public class Notify {

    private static ColorizedOutputStream<BasicColor> output = new AnsiBasicColorizedOutputStream(System.out);

    public static void error(String title, String summary, String message) {
        output.resetForegroundColor();
        output.setBold(true);
        output.setForegroundColor(BasicColor.RED);
        output.print(title + ": ");
        output.setBold(false);
        output.print(summary + "\n");
        output.setForegroundColor(BasicColor.MAGENTA);
        output.println("\t" + message);
        output.resetForegroundColor();
    }

    public static void error(String title, String message) {
        output.resetForegroundColor();
        output.setBold(true);
        output.setForegroundColor(BasicColor.RED);
        output.print(title + ":");
        output.setBold(false);
        output.print(message);
        output.resetForegroundColor();
    }

    public static void error(String message) {
        output.resetForegroundColor();
        output.setBold(true);
        output.setForegroundColor(BasicColor.RED);
        output.print(message);
        output.setBold(false);
        output.resetForegroundColor();
    }

    public static void normal(String title, String summary, String message) {
        output.resetForegroundColor();
        output.setBold(true);
        output.setForegroundColor(BasicColor.GREEN);
        output.print(title + ": ");
        output.setBold(false);
        output.print(summary + "\n");
        output.setForegroundColor(BasicColor.CYAN);
        output.println("\t" + message);
        output.resetForegroundColor();
    }

    public static void normal(String title, String message) {
        output.resetForegroundColor();
        output.setBold(true);
        output.setForegroundColor(BasicColor.GREEN);
        output.print(title + ":");
        output.setBold(false);
        output.print(message);
        output.resetForegroundColor();
    }

    public static void normal(String message) {
        output.resetForegroundColor();
        output.setBold(true);
        output.setForegroundColor(BasicColor.GREEN);
        output.print(message);
        output.setBold(false);
        output.resetForegroundColor();
    }
}
