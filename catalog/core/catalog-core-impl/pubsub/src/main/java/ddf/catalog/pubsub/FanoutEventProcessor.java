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
package ddf.catalog.pubsub;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.plugin.PreDeliveryPlugin;
import ddf.catalog.plugin.PreSubscriptionPlugin;
import java.util.List;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FanoutEventProcessor extends EventProcessorImpl {
  private static final Logger LOGGER = LoggerFactory.getLogger(FanoutEventProcessor.class);

  private static final String ENTERING_STR = "ENTERING: {}";

  private static final String EXITING_STR = "EXITING: {}";

  private static final String METACARD_SOURCE_MSG = "Setting metacard's source ID to {}";

  public FanoutEventProcessor(
      BundleContext bundleContext,
      EventAdmin eventAdmin,
      List<PreSubscriptionPlugin> preSubscription,
      List<PreDeliveryPlugin> preDelivery,
      CatalogFramework catalog) {
    super(bundleContext, eventAdmin, preSubscription, preDelivery, catalog);

    LOGGER.trace(EXITING_STR, "FanoutEventProcessor constructor");
  }

  @Override
  public void init() {
    String methodName = "init";
    LOGGER.debug(ENTERING_STR, methodName);

    LOGGER.debug(EXITING_STR, methodName);
  }

  @Override
  public void destroy() {
    String methodName = "destroy";
    LOGGER.debug(ENTERING_STR, methodName);

    LOGGER.debug(EXITING_STR, methodName);
  }

  @Override
  public void notifyCreated(Metacard newMetacard) {
    String methodName = "notifyCreated";
    LOGGER.trace(ENTERING_STR, methodName);

    // In fanout, set event metacard's site name to fanout site name
    // to mask name of site that sent event
    LOGGER.trace(METACARD_SOURCE_MSG, catalog.getId());
    newMetacard.setSourceId(catalog.getId());

    // postEvent( EventProcessor.EVENTS_TOPIC_CREATED, newMetacard );
    super.notifyCreated(newMetacard);

    LOGGER.trace(EXITING_STR, methodName);
  }

  @Override
  public void notifyUpdated(Metacard newMetacard, Metacard oldMetacard) {
    String methodName = "notifyUpdated";
    LOGGER.trace(ENTERING_STR, methodName);

    // In fanout, set event metacard's site name to fanout site name
    // to mask name of site that sent event
    LOGGER.trace(METACARD_SOURCE_MSG, catalog.getId());
    if (oldMetacard != null) {
      oldMetacard.setSourceId(catalog.getId());
    }
    newMetacard.setSourceId(catalog.getId());

    // postEvent( EventProcessor.EVENTS_TOPIC_UPDATED, newMetacard );
    super.notifyUpdated(newMetacard, oldMetacard);

    LOGGER.trace(EXITING_STR, methodName);
  }

  @Override
  public void notifyDeleted(Metacard oldMetacard) {
    String methodName = "notifyUDeleted";
    LOGGER.trace(ENTERING_STR, methodName);

    // In fanout, set event metacard's site name to fanout site name
    // to mask name of site that sent event
    LOGGER.trace(METACARD_SOURCE_MSG, catalog.getId());
    oldMetacard.setSourceId(catalog.getId());

    // postEvent( EventProcessor.EVENTS_TOPIC_DELETED, oldMetacard );
    super.notifyDeleted(oldMetacard);

    LOGGER.trace(EXITING_STR, methodName);
  }
}
