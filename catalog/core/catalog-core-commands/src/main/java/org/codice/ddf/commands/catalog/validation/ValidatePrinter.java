/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.commands.catalog.validation;

import java.io.PrintStream;
import org.fusesource.jansi.Ansi;

public class ValidatePrinter {

  protected final PrintStream console = System.out;

  private final String errorColor = Ansi.ansi().fg(Ansi.Color.RED).toString();

  private final String warningColor = Ansi.ansi().fg(Ansi.Color.MAGENTA).toString();

  private final String defaultColor = Ansi.ansi().fg(Ansi.Color.DEFAULT).toString();

  public void print(ValidateReport report) {
    console.println(report.getId());
    report.getEntries().forEach(this::printEntry);
  }

  private void printEntry(ValidateReportEntry entry) {
    if (!entry.getWarnings().isEmpty() || !entry.getErrors().isEmpty()) {
      console.println("  " + entry.getValidatorName());
      entry.getErrors().forEach(e -> printError("\t" + e));
      entry.getWarnings().forEach(e -> printWarning("\t" + e));
    }
  }

  public void printError(String error) {
    console.printf("%s%s%s%n", errorColor, error, defaultColor);
  }

  public void printWarning(String warning) {
    console.printf("%s%s%s%n", warningColor, warning, defaultColor);
  }

  public void printSummary(int bad, int total) {
    console.printf("%nSUMMARY:%n  %d/%d metacards contain errors or warnings%n", bad, total);
  }
}
