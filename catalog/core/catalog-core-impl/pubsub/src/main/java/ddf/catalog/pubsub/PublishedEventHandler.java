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

package ddf.catalog.pubsub;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.commons.collections.CollectionUtils;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.event.Subscription;
import ddf.catalog.operation.Pingable;
import ddf.catalog.plugin.PreDeliveryPlugin;
import ddf.catalog.pubsub.internal.DeliveryProcessor;
import ddf.catalog.pubsub.internal.PubSubConstants;
import ddf.catalog.pubsub.predicate.Predicate;

public class PublishedEventHandler implements EventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishedEventHandler.class);

    private final ExecutorService threadPool;

    private Predicate predicate;

    private Subscription subscription;

    private List<PreDeliveryPlugin> preDelivery;

    private CatalogFramework catalog;

    public PublishedEventHandler(Predicate finalPredicate, Subscription subscription,
            List<PreDeliveryPlugin> preDelivery, CatalogFramework catalog,
            ExecutorService threadPool) {
        this.predicate = finalPredicate;
        this.subscription = subscription;
        this.preDelivery = preDelivery;
        this.catalog = catalog;
        this.threadPool = threadPool;
    }

    public void handleEvent(Event event) {
        threadPool.submit(new EventProcessor(event));
    }

    private class EventProcessor implements Runnable {

        private Event event;

        public EventProcessor(Event event) {
            this.event = event;
        }

        @Override
        public void run() {

            String methodName = "handleEvent";
            LOGGER.trace("ENTERING: {}", methodName);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("topic = {}", event.getTopic());
                for (String propertyName : event.getPropertyNames()) {
                    LOGGER.debug("name = {},   value = {}",
                            propertyName,
                            event.getProperty(propertyName));
                }
            }

            if (subscription.getDeliveryMethod() instanceof Pingable
                    && !((Pingable) subscription.getDeliveryMethod()).ping()) {
                LOGGER.debug("Subscription is not active ignoring event");
                return;
            }

            LOGGER.debug("subscription is enterprise? {}", subscription.isEnterprise());
            Set<String> sourceIds = subscription.getSourceIds();
            LOGGER.debug("subscription has source names: {}", sourceIds);

            Metacard eventMetacard = (Metacard) event.getProperty(PubSubConstants.HEADER_ENTRY_KEY);
            String metacardSourceId = eventMetacard.getSourceId();
            LOGGER.debug("metacard source id: {}", metacardSourceId);

            if (subscription.isEnterprise()) {
                // if the subscription is an enterprise subscription then evaluate all incoming events
                evaluateEvent(event);
            } else if (CollectionUtils.isEmpty(sourceIds)) {
                evaluateLocalSubscription(metacardSourceId);
            } else {
                evaluateSiteBasedSubscription(sourceIds, metacardSourceId);
            }

            LOGGER.trace("EXITING: {}", methodName);
        }

        private void evaluateSiteBasedSubscription(Set<String> sourceIds, String metacardSourceId) {
            LOGGER.debug("subscription is a site-based subscription starting with site id {}",
                    sourceIds.iterator()
                            .next());
            // perform site based filtering on subscription
            if (sourceIds.contains(metacardSourceId)) {
                LOGGER.debug("event received from subscribed site");
                evaluateEvent(event);
            } else {
                LOGGER.debug(
                        "event received from remote site that is not in list of source IDs of subscription - not evaluating event");
            }
        }

        private void evaluateLocalSubscription(String metacardSourceId) {
            LOGGER.debug("subscription is a local subscription. Local Source Id: {}",
                    catalog.getId());
            if (catalog.getId() != null && catalog.getId()
                    .equals(metacardSourceId)) {
                LOGGER.debug("event received from local site");
                evaluateEvent(event);
            } else {
                LOGGER.debug(
                        "event is from remote site but subscription is local - not evaluating event against subscription filter");
            }
        }

        private void evaluateEvent(Event event) {
            // If predicate is NULL then we are handling a filterless subscription - publish all events
            if (predicate == null || predicate.matches(event)) {
                new DeliveryProcessor(subscription, preDelivery).process(event);
            }
        }
    }

}
