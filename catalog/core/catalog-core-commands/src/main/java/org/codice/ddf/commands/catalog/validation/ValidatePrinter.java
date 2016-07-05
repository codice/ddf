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
package org.codice.ddf.commands.catalog.validation;

import java.io.PrintStream;

import org.fusesource.jansi.Ansi;

public class ValidatePrinter {

    private static final String ERROR_COLOR = Ansi.ansi()
            .fg(Ansi.Color.RED)
            .toString();

    private static final String WARNING_COLOR = Ansi.ansi()
            .fg(Ansi.Color.MAGENTA)
            .toString();

    private static final String DEFAULT_COLOR = Ansi.ansi()
            .fg(Ansi.Color.DEFAULT)
            .toString();

    protected static final PrintStream CONSOLE = System.out;

    public static void print(ValidateReport report) {
        CONSOLE.println(report.getId());
        report.getEntries()
                .forEach(ValidatePrinter::printEntry);
    }

    private static void printEntry(ValidateReportEntry entry) {
        if (entry.getWarnings()
                .isEmpty() || entry.getErrors()
                .isEmpty()) {
            CONSOLE.println("  " + entry.getValidatorName());
            entry.getErrors()
                    .forEach(e -> printError("\t" + e));
            entry.getWarnings()
                    .forEach(e -> printWarning("\t" + e));
        }
    }

    public static void printError(String error) {
        CONSOLE.printf("%s%s%s%n", ERROR_COLOR, error, DEFAULT_COLOR);
    }

    public static void printWarning(String warning) {
        CONSOLE.printf("%s%s%s%n", WARNING_COLOR, warning, DEFAULT_COLOR);
    }

    public static void printSummary(int bad, int total) {
        CONSOLE.printf("%nSUMMARY:%n  %d/%d metacards contain errors or warnings%n", bad, total);
    }
}
