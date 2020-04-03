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
import java.util.Set;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.fusesource.jansi.Ansi;

/**
 * Implements the "expand" command - taking an attribute name and the current value, and prints out
 * what the expansion of that value is using the currently-configured expansion service.
 */
@Service
@Command(
    scope = "security",
    name = "expand",
    description = "Expands a given key and set of values.")
public class ExpandCommand implements Action {
  @Argument(
      name = "key",
      description = "The of the value to be encrypted.",
      index = 0,
      multiValued = false,
      required = true)
  private String key = null;

  @Argument(
      name = "values",
      description = "The set of values to be expanded.",
      index = 1,
      multiValued = true,
      required = true)
  private Set<String> values = null;

  // live list of expansion services
  @Reference List<Expansion> expansionList;

  /** Called to execute the security:encrypt console command. */
  @Override
  public Object execute() throws Exception {
    if ((key == null) || (values == null)) {
      return null;
    }

    if ((expansionList != null) && (!expansionList.isEmpty())) {
      for (Expansion expansion : expansionList) {
        Set<String> expandedValues = expansion.expand(key, values);
        System.out.print(Ansi.ansi().fg(Ansi.Color.YELLOW).toString());
        System.out.println(expandedValues);
        System.out.print(Ansi.ansi().reset().toString());
      }
    } else {
      System.out.println("No expansion services currently available.");
    }
    return null;
  }
}
