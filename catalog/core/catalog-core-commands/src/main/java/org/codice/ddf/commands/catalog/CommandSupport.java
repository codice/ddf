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
package org.codice.ddf.commands.catalog;

import java.io.PrintStream;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Action;
import org.fusesource.jansi.Ansi;

/** CommandSupport provides printing and progress bar support for extending classes */
public abstract class CommandSupport implements Action {

  protected static final double MS_PER_SECOND = 1000.0;

  private static final int PROGRESS_BAR_WIDTH = 72;

  protected static final Ansi.Color ERROR_COLOR = Ansi.Color.RED;

  private static final Ansi.Color HEADER_COLOR = Ansi.Color.CYAN;

  private static final Ansi.Color SUCCESS_COLOR = Ansi.Color.GREEN;

  protected final PrintStream console = System.out;

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

  /**
   * Logic mimics {@link org.apache.karaf.main.StartupListener#showProgressBar}
   *
   * @param start time started processing records
   * @param totalCount count of total records
   * @param currentCount count of records completed
   * @return
   */
  protected void printProgressAndFlush(long start, long totalCount, long currentCount) {
    final int progressPercentage;
    if (totalCount > 0) {
      progressPercentage = (int) ((currentCount * 100) / totalCount);
    } else {
      // display 100% completed progress bar if the totalCount is 0
      progressPercentage = 100;
    }

    final int notchesCount = (int) (PROGRESS_BAR_WIDTH * (progressPercentage / 100.0));
    final int rate = calculateRecordsPerSecond(currentCount, start, System.currentTimeMillis());

    console.print(
        String.format(
            "\r%1$3d%% [%2$-" + PROGRESS_BAR_WIDTH + "s] %3$5s records/sec",
            progressPercentage,
            StringUtils.repeat("=", notchesCount) + (notchesCount == PROGRESS_BAR_WIDTH ? "" : ">"),
            rate));
    console.flush();
  }

  protected int calculateRecordsPerSecond(long currentCount, long start, long end) {
    return (int) ((double) currentCount / ((double) (end - start) / MS_PER_SECOND));
  }
}
