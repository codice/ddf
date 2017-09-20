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
import java.util.HashMap;
import java.util.Map;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscriptionsCommand implements Action {

  public static final String NAMESPACE = "subscriptions";

  public static final String SERVICE_PID = "ddf.catalog.event.Subscription";

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionsCommand.class);

  @Reference BundleContext bundleContext;

  @Override
  public Object execute() throws Exception {

    return null;
  }

  protected Map<String, ServiceReference<Subscription>> getSubscriptions(
      String id, boolean ldapFilter) throws InvalidSyntaxException {
    Map<String, ServiceReference<Subscription>> subscriptionIds = new HashMap<>();

    String subscriptionIdFilter = null;
    if (ldapFilter) {
      subscriptionIdFilter = id;
    } else if (id != null) {
      subscriptionIdFilter = "(subscription-id=" + id + ")";
    }
    LOGGER.debug("subscriptionIdFilter = {}", subscriptionIdFilter);
    ServiceReference[] serviceReferences =
        bundleContext.getServiceReferences(SERVICE_PID, subscriptionIdFilter);

    if (serviceReferences == null || serviceReferences.length == 0) {
      LOGGER.debug("Found no service references for {}", SERVICE_PID);
    } else {
      LOGGER.debug("Found {} service references for {}", serviceReferences.length, SERVICE_PID);

      for (ServiceReference ref : serviceReferences) {
        String[] propertyKeys = ref.getPropertyKeys();
        if (propertyKeys != null) {
          for (String key : propertyKeys) {
            LOGGER.debug("key = {}", key);
          }
          String subscriptionId = (String) ref.getProperty("subscription-id");
          LOGGER.debug("subscriptionId = {}", subscriptionId);
          subscriptionIds.put(subscriptionId, ref);
        } else {
          LOGGER.debug("propertyKeys = NULL");
        }
      }
    }

    return subscriptionIds;
  }

  public void setBundleContext(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }
}
