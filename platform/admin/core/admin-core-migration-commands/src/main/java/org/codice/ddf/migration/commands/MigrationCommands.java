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
package org.codice.ddf.migration.commands;

import java.io.PrintStream;

import org.apache.karaf.shell.api.action.Action;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;

/**
 * Parent object to all Platform Commands. Provides common methods and instance variables that
 * Platform Commands can use.
 */
public abstract class MigrationCommands implements Action {

    public static final String NAMESPACE = "migration";

    protected void outputErrorMessage(String message) {
        String colorAsString = Ansi.ansi()
                .a(Attribute.RESET)
                .fg(Ansi.Color.RED)
                .toString();
        PrintStream console = getConsole();
        console.print(colorAsString);
        console.print(message);
        console.println(Ansi.ansi()
                .a(Attribute.RESET)
                .toString());
    }

    protected void outputWarningMessage(String message) {
        String colorAsString = Ansi.ansi()
                .a(Attribute.RESET)
                .fg(Ansi.Color.YELLOW)
                .toString();
        PrintStream console = getConsole();
        console.print(colorAsString);
        console.print(message);
        console.println(Ansi.ansi()
                .a(Attribute.RESET)
                .toString());
    }

    protected void outputInfoMessage(String message) {
        String colorAsString = Ansi.ansi()
                .a(Attribute.RESET)
                .fg(Ansi.Color.WHITE)
                .toString();
        PrintStream console = getConsole();
        console.print(colorAsString);
        console.print(message);
        console.println(Ansi.ansi()
                .a(Attribute.RESET)
                .toString());
    }

    protected void outputSuccessMessage(String message) {
        String colorAsString = Ansi.ansi()
                .a(Attribute.RESET)
                .fg(Ansi.Color.GREEN)
                .toString();
        PrintStream console = getConsole();
        console.print(colorAsString);
        console.print(message);
        console.println(Ansi.ansi()
                .a(Attribute.RESET)
                .toString());
    }

    PrintStream getConsole() {
        return System.out;
    }
}
