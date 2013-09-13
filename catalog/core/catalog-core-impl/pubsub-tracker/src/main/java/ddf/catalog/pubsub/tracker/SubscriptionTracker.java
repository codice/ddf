/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/

package ddf.catalog.pubsub.tracker;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import ddf.catalog.event.EventException;
import ddf.catalog.event.EventProcessor;
import ddf.catalog.event.Subscription;

public class SubscriptionTracker {
    protected Map<String, String> services = new HashMap<String, String>();

    protected EventProcessor provider;

    private static Logger logger = Logger.getLogger(SubscriptionTracker.class);

    public void startUp() {
        String methodName = "startUp";
        logger.debug("ENTERING: " + methodName);

        logger.debug("EXITING: " + methodName);
    }

    public void cleanUp() {
        String methodName = "cleanUp";
        logger.debug("ENTERING: " + methodName);

        logger.debug("EXITING: " + methodName);
    }

    public void setProvider(EventProcessor provider) {
        String methodName = "setProvider";
        logger.debug("ENTERING: " + methodName);

        this.provider = provider;

        logger.debug("EXITING: " + methodName);
    }

    public void addingService(Subscription subscription, Map props) {
        String methodName = "addingService";
        logger.debug("ENTERING: " + methodName);
        logger.debug("*********************************************");

        String serviceId = props.get("service.id").toString();

        logger.debug(SubscriptionTracker.class.getName() + " has detected a"
                + " subscription request(" + serviceId + ")  ");

        try {
            String subscriptionId = provider.createSubscription(subscription);
            logger.debug(provider.getClass().getName()
                    + " Provider has created the subscription for request (" + serviceId + "):"
                    + subscriptionId);
            services.put(serviceId, subscriptionId);
        } catch (EventException e) {
            logger.error("Error in creating subscription. " + serviceId, e);
        }

        logger.debug("EXITING: " + methodName);
    }

    public void removedService(Subscription subscription, Map props) {
        String methodName = "removedService";
        logger.debug("ENTERING: " + methodName);

        if (props != null) {
            if (props.get("service.id") != null) {
                String serviceId = props.get("service.id").toString();

                String subscriptionId = services.get(serviceId);

                logger.debug(SubscriptionTracker.class.getName()
                        + " has detected request for subscription " + "(" + serviceId
                        + ") deletion.");

                try {
                    provider.deleteSubscription(subscriptionId);

                    logger.debug("Subscription (" + serviceId + ") has been deleted.");

                    // cleanup the reference in our map
                    services.remove(serviceId);

                } catch (EventException e) {
                    logger.error("Error in deleting subscription. " + serviceId, e);
                }
            }
        }
        logger.debug("EXITING: " + methodName);
    }

}
