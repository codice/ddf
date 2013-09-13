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
package ddf.catalog.pubsub;

import java.util.List;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventAdmin;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.plugin.PreDeliveryPlugin;
import ddf.catalog.plugin.PreSubscriptionPlugin;

public class FanoutEventProcessor extends EventProcessorImpl {
    private static Logger logger = Logger.getLogger(FanoutEventProcessor.class);

    public FanoutEventProcessor(BundleContext bundleContext, EventAdmin eventAdmin,
            List<PreSubscriptionPlugin> preSubscription, List<PreDeliveryPlugin> preDelivery,
            CatalogFramework catalog) {
        super(bundleContext, eventAdmin, preSubscription, preDelivery, catalog);

        logger.trace("EXITING: FanoutEventProcessor constructor");
    }

    public void init() {
        String methodName = "init";
        logger.debug("ENTERING: " + methodName);

        logger.debug("EXITING: " + methodName);
    }

    public void destroy() {
        String methodName = "destroy";
        logger.debug("ENTERING: " + methodName);

        logger.debug("EXITING: " + methodName);
    }

    @Override
    public void notifyCreated(Metacard newMetacard) {
        String methodName = "notifyCreated";
        logger.trace("ENTERING: " + methodName);

        // In fanout, set event metacard's site name to fanout site name
        // to mask name of site that sent event
        logger.trace("Setting metacard's source ID to " + catalog.getId());
        newMetacard.setSourceId(catalog.getId());

        // postEvent( EventProcessor.EVENTS_TOPIC_CREATED, newMetacard );
        super.notifyCreated(newMetacard);

        logger.trace("EXITING: " + methodName);
    }

    @Override
    public void notifyUpdated(Metacard newMetacard, Metacard oldMetacard) {
        String methodName = "notifyUpdated";
        logger.trace("ENTERING: " + methodName);

        // In fanout, set event metacard's site name to fanout site name
        // to mask name of site that sent event
        logger.trace("Setting metacard's source ID to " + catalog.getId());
        if (oldMetacard != null) {
            oldMetacard.setSourceId(catalog.getId());
        }
        newMetacard.setSourceId(catalog.getId());

        // postEvent( EventProcessor.EVENTS_TOPIC_UPDATED, newMetacard );
        super.notifyUpdated(newMetacard, oldMetacard);

        logger.trace("EXITING: " + methodName);
    }

    @Override
    public void notifyDeleted(Metacard oldMetacard) {
        String methodName = "notifyUDeleted";
        logger.trace("ENTERING: " + methodName);

        // In fanout, set event metacard's site name to fanout site name
        // to mask name of site that sent event
        logger.trace("Setting metacard's source ID to " + catalog.getId());
        oldMetacard.setSourceId(catalog.getId());

        // postEvent( EventProcessor.EVENTS_TOPIC_DELETED, oldMetacard );
        super.notifyDeleted(oldMetacard);

        logger.trace("EXITING: " + methodName);
    }

}
