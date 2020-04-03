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
package org.codice.ddf.catalog.pubsub.command;

import ddf.catalog.event.Subscription;
import ddf.catalog.operation.Pingable;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.fusesource.jansi.Ansi;
import org.osgi.framework.ServiceReference;

@Service
@Command(
    scope = SubscriptionsCommand.NAMESPACE,
    name = "list",
    description = "Allows users to view registered subscriptions.")
public class ListCommand extends SubscriptionsCommand {

  static final String DEFAULT_CONSOLE_COLOR = Ansi.ansi().reset().toString();

  static final String RED_CONSOLE_COLOR = Ansi.ansi().fg(Ansi.Color.RED).toString();

  static final String CYAN_CONSOLE_COLOR = Ansi.ansi().fg(Ansi.Color.CYAN).toString();

  static final String SUBSCRIPTION_ID_COLUMN_HEADER = "Subscription ID";

  static final String CALLBACK_COLUMN_HEADER = "Callback URL";

  static final String ENTERPRISE_COLUMN_HEADER = "Enterprise";

  static final String SOURCE_ID_COLUMN_HEADER = "Sources";

  static final String NO_SUBSCRIPTIONS_FOUND_MSG = "No subscriptions found";

  @Argument(
      name = "search phrase or LDAP filter",
      description =
          "Subscription ID to search for. Wildcard characters (*) can be used in the ID, e.g., my*name or *123. "
              + "If an id is not provided, then all of the subscriptions are displayed.",
      index = 0,
      multiValued = false,
      required = false)
  String id = null;

  @Option(
      name = "filter",
      required = false,
      aliases = {"-f"},
      multiValued = false,
      description =
          "Allows user to specify any type of LDAP filter rather than searching on single subscription ID.\n"
              + "You should enclose the LDAP filter in quotes since it will often have special characters in it.\n"
              + "An example LDAP filter would be:\n"
              + "(& (subscription-id=my*) (subscription-id=*169*))\n"
              + "which searches for all subscriptions starting with \"my\" and having 169 in the ID, which can be thought of as part of an IP address.\n"
              + "An example of the entire quote command would be:\n"
              + "subscriptions:list -f \"\"(& (subscription-id=my*) (subscription-id=*169*))\"")
  boolean ldapFilter = false;

  @Override
  public Object execute() throws Exception {
    PrintStream console = System.out;

    Map<String, ServiceReference<Subscription>> subscriptions = getSubscriptions(id, ldapFilter);
    List<SubscriptionListEntry> entries =
        subscriptions.entrySet().stream()
            .map(SubscriptionListEntry::new)
            .collect(Collectors.toList());

    if (entries.isEmpty()) {
      console.println(RED_CONSOLE_COLOR + NO_SUBSCRIPTIONS_FOUND_MSG + DEFAULT_CONSOLE_COLOR);
    } else {
      printSubscriptions(entries);
    }

    return null;
  }

  private void printSubscriptions(List<SubscriptionListEntry> entries) {
    PrintStream console = System.out;

    int[] columnWidth =
        new int[] {
          SUBSCRIPTION_ID_COLUMN_HEADER.length(),
          CALLBACK_COLUMN_HEADER.length(),
          ENTERPRISE_COLUMN_HEADER.length(),
          SOURCE_ID_COLUMN_HEADER.length()
        };
    for (SubscriptionListEntry entry : entries) {
      int column0Width = entry.id.length();
      int column1Width = entry.callbackUrl.length();
      int column2Width = entry.isEnterprise.length();
      int column3Width = entry.sourceIds.length();
      if (column0Width > columnWidth[0]) columnWidth[0] = column0Width;
      if (column1Width > columnWidth[1]) columnWidth[1] = column1Width;
      if (column2Width > columnWidth[2]) columnWidth[2] = column2Width;
      if (column3Width > columnWidth[3]) columnWidth[3] = column3Width;
    }

    String rowFormatString =
        String.format(
            "%%-%ds | %%-%ds | %%-%ds | %%-%ds",
            columnWidth[0], columnWidth[1], columnWidth[2], columnWidth[3]);

    console.println();
    console.print("Total subscriptions found: " + entries.size());
    console.println();
    console.println();
    console.print(CYAN_CONSOLE_COLOR);
    console.printf(
        rowFormatString,
        SUBSCRIPTION_ID_COLUMN_HEADER,
        CALLBACK_COLUMN_HEADER,
        ENTERPRISE_COLUMN_HEADER,
        SOURCE_ID_COLUMN_HEADER);
    console.println(DEFAULT_CONSOLE_COLOR);

    for (SubscriptionListEntry entry : entries) {
      String rowColor = (entry.ping()) ? DEFAULT_CONSOLE_COLOR : RED_CONSOLE_COLOR;
      console.print(rowColor);
      console.printf(
          rowFormatString, entry.id, entry.callbackUrl, entry.isEnterprise, entry.sourceIds);
      console.println(DEFAULT_CONSOLE_COLOR);
    }
  }

  private class SubscriptionListEntry {
    private String id;
    private String callbackUrl;
    private String isEnterprise;
    private String sourceIds;
    private Subscription subscription;

    public SubscriptionListEntry(Map.Entry<String, ServiceReference<Subscription>> entry) {
      Object eventEndpoint = entry.getValue().getProperty("event-endpoint");
      this.id = entry.getKey();
      this.callbackUrl = (eventEndpoint == null) ? "" : (String) eventEndpoint;
      this.subscription = bundleContext.getService(entry.getValue());

      if (subscription == null) {
        this.isEnterprise = "";
        this.sourceIds = "";
      } else {
        this.isEnterprise = Boolean.toString(subscription.isEnterprise());
        this.sourceIds =
            (subscription.getSourceIds() == null)
                ? "[]"
                : Arrays.toString(subscription.getSourceIds().toArray());
      }
    }

    public boolean ping() {
      return (subscription == null)
          || !(subscription.getDeliveryMethod() instanceof Pingable)
          || ((Pingable) subscription.getDeliveryMethod()).ping();
    }
  }
}
