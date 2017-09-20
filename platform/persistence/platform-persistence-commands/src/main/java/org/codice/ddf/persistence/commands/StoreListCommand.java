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
package org.codice.ddf.persistence.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;

@Service
@Command(
  scope = "store",
  name = "list",
  description = "Lists entries that are available in the persistent store."
)
public class StoreListCommand extends AbstractStoreCommand {

  @Option(
    name = "User ID",
    aliases = {"-u", "--user"},
    required = false,
    description =
        "User ID to search for notifications. If an id is not provided, then all of the notifications for all users are displayed.",
    multiValued = false
  )
  private String user;

  private Set<String> headerSet = new TreeSet<String>();

  @Override
  public void storeCommand() throws PersistenceException {

    List<Map<String, Object>> storeResults;

    cql = createCql(user, cql);

    if (StringUtils.isNotBlank(cql)) {
      storeResults = persistentStore.get(type, cql);
    } else {
      storeResults = persistentStore.get(type);
    }
    console.println("Results found: " + storeResults.size() + "\n");

    // output the entries
    for (int i = 0; i < storeResults.size(); i++) {
      Map<String, Object> curStore = PersistentItem.stripSuffixes(storeResults.get(i));
      console.println("Result {" + i + "}:");
      if (headerSet.isEmpty()) {
        // populates the header with the keys from the first entry
        headerSet.addAll(curStore.keySet());
      }

      for (String curKey : headerSet) {
        console.println(curKey + ":");
        console.println("\t" + curStore.get(curKey).toString());
      }
    }
    console.println("");
  }

  private String createCql(String user, String cql) {
    if (StringUtils.isNotBlank(user)) {
      if (StringUtils.isNotBlank(cql)) {
        cql = cql + " AND user='" + user + "'";
      } else {
        cql = "user='" + user + "'";
      }
    }
    return cql;
  }
}
