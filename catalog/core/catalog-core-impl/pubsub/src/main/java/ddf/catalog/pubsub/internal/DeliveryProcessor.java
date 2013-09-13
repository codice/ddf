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

package ddf.catalog.pubsub.internal;

import java.util.List;

import org.apache.log4j.Logger;
import org.osgi.service.event.Event;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateImpl;
import ddf.catalog.event.Subscription;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreDeliveryPlugin;
import ddf.catalog.plugin.StopProcessingException;

public class DeliveryProcessor {
    private static Logger logger = Logger.getLogger(DeliveryProcessor.class);

    private Subscription subscription;

    private List<PreDeliveryPlugin> preDelivery;

    public DeliveryProcessor(Subscription subscription, List<PreDeliveryPlugin> preDelivery) {
        this.subscription = subscription;
        this.preDelivery = preDelivery;
    }

    public void process(Event event) {
        String methodName = "process";
        logger.debug("ENTERING: " + methodName);

        Metacard entry = (Metacard) event.getProperty(PubSubConstants.HEADER_ENTRY_KEY);
        String operation = event.getProperty(PubSubConstants.HEADER_OPERATION_KEY).toString();

        logger.debug("Delivering catalog entry.");
        if (subscription != null) {
            if (entry != null) {
                if (operation.equalsIgnoreCase(PubSubConstants.CREATE)) {
                    try {
                        for (PreDeliveryPlugin plugin : preDelivery) {
                            logger.debug("Processing 'created' entry with preDelivery plugin");
                            entry = plugin.processCreate(entry);
                        }
                        subscription.getDeliveryMethod().created(entry);
                    } catch (PluginExecutionException e) {
                        logger.debug(
                                "Plugin had exception during execution - still delivering the entry",
                                e);
                        subscription.getDeliveryMethod().created(entry);
                    } catch (StopProcessingException e) {
                        logger.error("Pre-delivery plugin determined entry cannot be delivered", e);
                    }
                } else if (operation.equalsIgnoreCase(PubSubConstants.UPDATE)) {
                    // TODO: Handle hit or miss
                    try {
                        for (PreDeliveryPlugin plugin : preDelivery) {
                            logger.debug("Processing 'updated' entry with preDelivery plugin");
                            Update updatedEntry = plugin.processUpdateHit(new UpdateImpl(entry,
                                    null));
                            entry = updatedEntry.getNewMetacard();
                        }
                        subscription.getDeliveryMethod().updatedHit(entry, entry);
                    } catch (PluginExecutionException e) {
                        logger.debug(
                                "Plugin had exception during execution - still delivering the entry",
                                e);
                        subscription.getDeliveryMethod().updatedHit(entry, entry);
                    } catch (StopProcessingException e) {
                        logger.error("Pre-delivery plugin determined entry cannot be delivered", e);
                    }
                } else if (operation.equalsIgnoreCase(PubSubConstants.DELETE)) {

                    try {
                        for (PreDeliveryPlugin plugin : preDelivery) {
                            logger.debug("Processing 'deleted' entry with preDelivery plugin");
                            entry = plugin.processCreate(entry);
                        }
                        subscription.getDeliveryMethod().deleted(entry);
                    } catch (PluginExecutionException e) {
                        logger.debug(
                                "Plugin had exception during execution - still delivering the entry",
                                e);
                        subscription.getDeliveryMethod().deleted(entry);
                    } catch (StopProcessingException e) {
                        logger.error("Pre-delivery plugin determined entry cannot be delivered", e);
                    }
                } else {
                    logger.warn("Could not deliver hit for subscription.");
                }
            } else {
                logger.warn("Could not deliver hit for subscription. Catalog entry is null.");
            }
        } else {
            logger.warn("Could not deliver hit for subscription. Subscription is null.");
        }

        logger.debug("EXITING: " + methodName);
    }

}
