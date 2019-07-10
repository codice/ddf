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
package ddf.catalog.pubsub.tracker;

import ddf.catalog.event.EventException;
import ddf.catalog.event.EventProcessor;
import ddf.catalog.event.Subscription;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscriptionTracker {
  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionTracker.class);

  protected Map<String, String> services = new HashMap<String, String>();

  protected EventProcessor provider;

  public void startUp() {
    String methodName = "startUp";
    LOGGER.debug("ENTERING: {}", methodName);

    LOGGER.debug("EXITING: {}", methodName);
  }

  public void cleanUp() {
    String methodName = "cleanUp";
    LOGGER.debug("ENTERING: {}", methodName);

    LOGGER.debug("EXITING: {}", methodName);
  }

  public void setProvider(EventProcessor provider) {
    String methodName = "setProvider";
    LOGGER.debug("ENTERING: {}", methodName);

    this.provider = provider;

    LOGGER.debug("EXITING: {}", methodName);
  }

  public void addingService(Subscription subscription, Map props) {
    String methodName = "addingService";
    LOGGER.debug("ENTERING: {}", methodName);
    LOGGER.debug("*********************************************");

    String serviceId = props.get("service.id").toString();

    LOGGER.debug(
        "{} has detected a subscription request({})",
        SubscriptionTracker.class.getName(),
        serviceId);

    if (provider != null) {
      try {
        String subscriptionId = provider.createSubscription(subscription);
        LOGGER.debug(
            "{} Provider has created the subscription for request ({}): {}",
            provider.getClass().getName(),
            serviceId,
            subscriptionId);
        services.put(serviceId, subscriptionId);
      } catch (EventException e) {
        LOGGER.info("Error in creating subscription. {}", serviceId, e);
      }
    } else {
      LOGGER.debug("EventProcessor was null.");
    }

    LOGGER.debug("EXITING: {}", methodName);
  }

  public void removedService(Subscription subscription, Map props) {
    String methodName = "removedService";
    LOGGER.debug("ENTERING: {}", methodName);

    if (props != null) {
      if (props.get("service.id") != null) {
        String serviceId = props.get("service.id").toString();

        String subscriptionId = services.get(serviceId);

        LOGGER.debug(
            "{} has detected request for subscription ({}) deletion.",
            SubscriptionTracker.class.getName(),
            serviceId);

        if (provider != null) {
          try {
            provider.deleteSubscription(subscriptionId);

            LOGGER.debug("Subscription ({}) has been deleted.", serviceId);

            // cleanup the reference in our map
            services.remove(serviceId);

          } catch (EventException e) {
            LOGGER.info("Error in deleting subscription. ", serviceId, e);
          }
        } else {
          LOGGER.debug("EventProcessor was null.");
        }
      }
    }
    LOGGER.debug("EXITING: {}", methodName);
  }
}
