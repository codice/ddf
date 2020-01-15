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

import ddf.catalog.event.Subscriber;
import ddf.catalog.event.Subscription;
import java.io.PrintStream;
import java.util.Map;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.fusesource.jansi.Ansi;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Command(
  scope = SubscriptionsCommand.NAMESPACE,
  name = "delete",
  description = "Allows users to delete registered subscriptions."
)
public class DeleteCommand extends SubscriptionsCommand {
  static final String DEFAULT_CONSOLE_COLOR = Ansi.ansi().reset().toString();

  static final String RED_CONSOLE_COLOR = Ansi.ansi().fg(Ansi.Color.RED).toString();

  static final String SUBSCRIBER_SERVICE_PID = "ddf.catalog.event.Subscriber";

  static final String NO_SUBSCRIPTIONS_FOUND_MSG = "No subscriptions found to be deleted";

  static final String NO_SUBSCRIBERS_FOUND_MSG =
      "Unable to delete any subscriptions because no Subscriber services found";

  static final String DELETE_MSG = "Deleted subscription for ID = ";

  static final String UNABLE_TO_DELETE_MSG = "Unable to delete subscription for ID = ";

  static final String DELETION_SUMMARY_FORMAT =
      "Deleted %d subscriptions out of %s subscriptions found.";

  private static final Logger LOGGER = LoggerFactory.getLogger(DeleteCommand.class);

  @Argument(
    name = "search phrase or LDAP filter",
    description =
        "Subscription ID to search for. Wildcard characters (*) can be used in the ID, e.g., my*name or *123. ",
    index = 0,
    multiValued = false,
    required = true
  )
  String id = null;

  @Option(
    name = "filter",
    required = false,
    aliases = {"-f"},
    multiValued = false,
    description =
        "Allows user to specify any type of LDAP filter rather than deleting on single subscription ID.\n"
            + "You should enclose the LDAP filter in quotes since it will often have special characters in it.\n"
            + "An example would be:\n"
            + "(& (subscription-id=my*) (subscription-id=*169*))\n"
            + "which searches for all subscriptions starting with \"my\" and having 169 in the ID, which can be thought of as part of an IP address.\n"
            + "An example of the entire quote command would be:\n"
            + "subscriptions:delete -f \"\"(& (subscription-id=my*) (subscription-id=*169*))\""
  )
  boolean ldapFilter = false;

  @Override
  public Object execute() throws Exception {

    PrintStream console = System.out;

    Map<String, ServiceReference<Subscription>> subscriptionIds = getSubscriptions(id, ldapFilter);
    if (subscriptionIds.isEmpty()) {
      console.println(RED_CONSOLE_COLOR + NO_SUBSCRIPTIONS_FOUND_MSG + DEFAULT_CONSOLE_COLOR);
    } else {
      ServiceReference[] serviceReferences =
          bundleContext.getServiceReferences(SUBSCRIBER_SERVICE_PID, null);
      if (serviceReferences == null || serviceReferences.length == 0) {
        LOGGER.debug("Found no service references for {}", SUBSCRIBER_SERVICE_PID);
        console.println(RED_CONSOLE_COLOR + NO_SUBSCRIBERS_FOUND_MSG + DEFAULT_CONSOLE_COLOR);
      } else {
        LOGGER.debug(
            "Found {} service references for {}", serviceReferences.length, SUBSCRIBER_SERVICE_PID);

        int deletedSubscriptionsCount = 0;
        for (String subscriptionId : subscriptionIds.keySet()) {
          for (ServiceReference ref : serviceReferences) {
            Subscriber subscriber = (Subscriber) bundleContext.getService(ref);

            // NOTE: Not using the status returned by deleteSubscription() here because
            // it indicates
            // whether the attempt to delete the subscription was successful (i.e., no
            // exceptions
            // encountered while trying to unregister Subscription service or delete
            // DurableSubscription).
            // The status does not reflect if the Subscription was actually successfully
            // deleted. Hence the
            // need to call the getSubscriptions() method using this subscriptionId to
            // verify the
            // subscription has been actually deleted.
            // (Example: If 3 Subscribers existed and one subscription was being
            // deleted, only one of the
            // Subscribers would actually delete the Subscription, but all 3 Subscribers
            // would return a status
            // of true indicating their attempt to delete the Subscription was
            // successful).
            subscriber.deleteSubscription(subscriptionId);
          }

          // Verify the subscription was deleted and print appropriate message to console
          Map<String, ServiceReference<Subscription>> ids = getSubscriptions(subscriptionId, false);
          if (ids.isEmpty()) {
            console.println(DELETE_MSG + subscriptionId);
            deletedSubscriptionsCount++;
          } else {
            console.println(
                RED_CONSOLE_COLOR + UNABLE_TO_DELETE_MSG + subscriptionId + DEFAULT_CONSOLE_COLOR);
          }
        }

        // Print summary of deletions to console
        console.println();
        console.printf(
            "Deleted %d subscriptions out of %s subscriptions found.",
            deletedSubscriptionsCount, subscriptionIds.size());
        console.println();
      }
    }

    return null;
  }
}
