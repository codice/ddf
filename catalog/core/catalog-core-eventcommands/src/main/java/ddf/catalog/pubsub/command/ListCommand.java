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
package ddf.catalog.pubsub.command;

import java.io.PrintStream;
import java.util.Map;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.jansi.Ansi;
import org.osgi.framework.ServiceReference;

import ddf.catalog.event.Subscription;
import ddf.catalog.operation.Pingable;

@Command(scope = SubscriptionsCommand.NAMESPACE, name = "list", description = "Allows users to view registered subscriptions.")
public class ListCommand extends SubscriptionsCommand {
    static final String DEFAULT_CONSOLE_COLOR = Ansi.ansi()
            .reset()
            .toString();

    static final String RED_CONSOLE_COLOR = Ansi.ansi()
            .fg(Ansi.Color.RED)
            .toString();

    static final String CYAN_CONSOLE_COLOR = Ansi.ansi()
            .fg(Ansi.Color.CYAN)
            .toString();

    static final String SUBSCRIPTION_ID_COLUMN_HEADER = "Subscription ID";

    static final String CALLBACK_COLUMN_HEADER = "Callback URL";

    static final String NO_SUBSCRIPTIONS_FOUND_MSG = "No subscriptions found";

    @Argument(name = "search phrase or LDAP filter", description =
            "Subscription ID to search for. Wildcard characters (*) can be used in the ID, e.g., my*name or *123. "
                    + "If an id is not provided, then all of the subscriptions are displayed.", index = 0, multiValued = false, required = false)
    String id = null;

    @Option(name = "filter", required = false, aliases = {"-f"}, multiValued = false, description =
            "Allows user to specify any type of LDAP filter rather than searching on single subscription ID.\n"
                    + "You should enclose the LDAP filter in quotes since it will often have special characters in it.\n"
                    + "An example LDAP filter would be:\n"
                    + "(& (subscription-id=my*) (subscription-id=*169*))\n"
                    + "which searches for all subscriptions starting with \"my\" and having 169 in the ID, which can be thought of as part of an IP address.\n"
                    + "An example of the entire quote command would be:\n"
                    + "subscriptions:list -f \"\"(& (subscription-id=my*) (subscription-id=*169*))\"")
    boolean ldapFilter = false;

    @Override
    protected Object doExecute() throws Exception {

        PrintStream console = System.out;

        Map<String, ServiceReference<Subscription>> subscriptionIds = getSubscriptions(id,
                ldapFilter);
        if (subscriptionIds.size() == 0) {
            console.println(RED_CONSOLE_COLOR + NO_SUBSCRIPTIONS_FOUND_MSG + DEFAULT_CONSOLE_COLOR);
        } else {
            console.println();
            console.print("Total subscriptions found: " + subscriptionIds.size());
            console.println();
            console.println();
            console.print(CYAN_CONSOLE_COLOR);
            console.print(SUBSCRIPTION_ID_COLUMN_HEADER);
            console.print("\t\t| ");
            console.print(CALLBACK_COLUMN_HEADER);
            console.println(DEFAULT_CONSOLE_COLOR);
            subscriptionIds.entrySet()
                    .forEach(this::printSubscription);
        }

        return null;
    }

    private void printSubscription(Map.Entry<String, ServiceReference<Subscription>> entry) {
        PrintStream console = System.out;
        Subscription subscription = bundleContext.getService(entry.getValue());
        String rowColor = "";
        if (subscription != null && subscription.getDeliveryMethod() instanceof Pingable &&
                !((Pingable) subscription.getDeliveryMethod()).ping()) {
            rowColor = RED_CONSOLE_COLOR;
        }
        console.println(String.format("%s%s\t %s%s",
                rowColor,
                entry.getKey(),
                entry.getValue()
                        .getProperty("event-endpoint"),
                DEFAULT_CONSOLE_COLOR));
    }

}
