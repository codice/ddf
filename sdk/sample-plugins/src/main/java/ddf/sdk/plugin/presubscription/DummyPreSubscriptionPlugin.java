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
package ddf.sdk.plugin.presubscription;

import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.event.Subscription;
import ddf.catalog.event.SubscriptionImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.delegate.CopyFilterDelegate;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreSubscriptionPlugin;
import ddf.catalog.source.UnsupportedQueryException;

/***************************************************************************************
 * Follow DDF Developer's Guide to implement Life-cycle Services, Sources, or Transformers This
 * template/example shows the skeleton code for a Pre-Subscription Service
 ****************************************************************************************/

public class DummyPreSubscriptionPlugin implements PreSubscriptionPlugin {
    private static Logger LOGGER = LoggerFactory.getLogger(DummyPreSubscriptionPlugin.class);

    private static String ENTERING = "ENTERING {}";
    private static String EXITING = "EXITING {}";

    private FilterAdapter filterAdapter;

    private FilterBuilder filterBuilder;

    public DummyPreSubscriptionPlugin(FilterAdapter filterAdapter, FilterBuilder filterBuilder) {
        LOGGER.trace("INSIDE: DummyPreSubscriptionPlugin constructor");
        this.filterAdapter = filterAdapter;
        this.filterBuilder = filterBuilder;
    }

    public Subscription process(Subscription input) throws PluginExecutionException {
        String methodName = "process";
        LOGGER.trace(ENTERING, methodName);

        Subscription newSubscription = input;

        if (input != null) {
            FilterDelegate<Filter> delegate = new CopyFilterDelegate(filterBuilder);
            try {
                // Make a defensive copy of the original filter (just in case anyone else expects
                // it to remain unmodified)
                Filter copiedFilter = filterAdapter.adapt(input, delegate);

                // Define the extra query clause(s) to add to the copied filter
                Filter extraFilter = filterBuilder
                        .attribute("/ddms:Resource/ddms:security/@ICISM:releasableTo").like()
                        .text("CAN");

                // AND the extra query clause(s) to the copied filter
                Filter modifiedFilter = filterBuilder.allOf(copiedFilter, extraFilter);

                // Create a new subscription with the modified filter
                newSubscription = new SubscriptionImpl(modifiedFilter, input.getDeliveryMethod(),
                        input.getSourceIds(), input.isEnterprise());
            } catch (UnsupportedQueryException e) {
                throw new PluginExecutionException(e);
            }
        }

        LOGGER.trace(EXITING, methodName);

        return newSubscription;
    }

}
