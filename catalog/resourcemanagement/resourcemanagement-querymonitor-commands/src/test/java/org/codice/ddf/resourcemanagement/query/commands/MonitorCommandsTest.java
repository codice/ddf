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
package org.codice.ddf.resourcemanagement.query.commands;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;
import org.codice.ddf.resourcemanagement.query.plugin.ActiveSearch;
import org.codice.ddf.resourcemanagement.query.plugin.QueryMonitorPluginImpl;
import org.junit.Test;

public class MonitorCommandsTest {

  private QueryMonitorPluginImpl qmpi = new QueryMonitorPluginImpl();

  private static final String QUERY_TEXT = "queryText";

  private static final String CLIENT_TEXT = "clientText";

  @Test
  public void testPrintSearchTableToConsole() {
    final ByteArrayOutputStream[] baos = new ByteArrayOutputStream[1];
    PrintActiveSearchesCommand printCommand =
        new PrintActiveSearchesCommand(qmpi) {
          @Override
          PrintStream getPrintStream() {
            baos[0] = new ByteArrayOutputStream();
            return new PrintStream(baos[0]);
          }
        };
    qmpi.setRemoveSearchAfterComplete(false);
    UUID u = UUID.randomUUID();
    ActiveSearch as = new ActiveSearch(QUERY_TEXT, null, u, CLIENT_TEXT);
    qmpi.addActiveSearch(as);
    printCommand.printActiveSearchesToConsole();
    assertThat(baos[0].toString(), containsString(QUERY_TEXT));
    assertThat(baos[0].toString(), containsString(u.toString()));
  }

  @Test
  public void testSetRemoveSearches() {
    RemoveSearchAfterCompleteCommand q = new RemoveSearchAfterCompleteCommand(qmpi);
    assertThat(q.setRemoveSearchAfterComplete(true), is(true));
    assertThat(q.setRemoveSearchAfterComplete(false), is(true));
  }
}
