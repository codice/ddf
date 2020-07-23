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
package org.codice.ddf.security.command;

import ddf.security.expansion.Expansion;
import java.util.List;
import java.util.Map;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.fusesource.jansi.Ansi;

/**
 * Implements the "expansions" command - dumps the current expansion mapping table for each
 * expansion service.
 */
@Service
@Command(
    scope = "security",
    name = "expansions",
    description = "Dumps the current expansion tables.")
public class ExpansionsCommand implements Action {
  // live list of expansion services
  @Reference List<Expansion> expansionList;

  /** Called to execute the security:encrypt console command. */
  @Override
  public Object execute() throws Exception {
    if ((expansionList != null) && (!expansionList.isEmpty())) {
      for (Expansion expansion : expansionList) {
        Map<String, List<String[]>> map = expansion.getExpansionMap();
        System.out.print(Ansi.ansi().fg(Ansi.Color.YELLOW).toString());
        if ((map != null) && (!map.isEmpty())) {
          for (Map.Entry<String, List<String[]>> entry : map.entrySet()) {
            for (String[] mapping : entry.getValue()) {
              System.out.printf("%s : %s : %s%n", entry.getKey(), mapping[0], mapping[1]);
            }
          }
        }
        System.out.print(Ansi.ansi().reset().toString());
      }
    } else {
      System.out.println("No expansion services currently available.");
    }
    return null;
  }
}
