/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package com.lmco.ddf.endpoints.rest.action;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import ddf.action.ActionProvider;
import ddf.catalog.Constants;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.util.DdfConfigurationWatcher;

/**
 * Registers {@link ActionProvider} objects into the Service Registry based upon
 * the services provided by other objects.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class ActionProviderRegistryProxy {

    private static final String WATCHER_INTERFACE_NAME = DdfConfigurationWatcher.class
            .getName();

    private static final String PROVIDER_INTERFACE_NAME = ActionProvider.class
            .getName();

    static final String ACTION_ID_PREFIX = "catalog.data.metacard.";

    private Map<ServiceReference, ServiceRegistration> actionProviderRegistry = new HashMap<ServiceReference, ServiceRegistration>();

    private Map<ServiceReference, ServiceRegistration> configWatcherRegistry = new HashMap<ServiceReference, ServiceRegistration>();

    private BundleContext bundleContext;

    private static final Logger LOGGER = Logger
            .getLogger(ActionProviderRegistryProxy.class);

    public ActionProviderRegistryProxy(BundleContext bundleContext) {

        this.bundleContext = bundleContext;

    }

    public void bind(ServiceReference reference) {

        LOGGER.info("New service registered [" + reference + "]");

        String transformerId = null;

        if (reference.getProperty(Constants.SERVICE_ID) != null) {

            transformerId = reference.getProperty(Constants.SERVICE_ID)
                    .toString();

        }
        // backwards compatibility
        else if (reference.getProperty(Constants.SERVICE_SHORTNAME) != null) {

            transformerId = reference.getProperty(Constants.SERVICE_SHORTNAME)
                    .toString();
        }

        if (transformerId == null) {
            return;
        }

        String actionProviderId = ACTION_ID_PREFIX + transformerId;

        ActionProvider provider = new MetacardTransformerActionProvider(
                actionProviderId, transformerId);

        Dictionary actionProviderProperties = new Hashtable<String, String>();

        actionProviderProperties.put(Constants.SERVICE_ID, actionProviderId);

        ServiceRegistration actionServiceRegistration = bundleContext
                .registerService(PROVIDER_INTERFACE_NAME, provider,
                        actionProviderProperties);

        ServiceRegistration configWatchServiceRegistration = bundleContext
                .registerService(WATCHER_INTERFACE_NAME, provider,
                        actionProviderProperties);

        LOGGER.info("Registered new " + PROVIDER_INTERFACE_NAME + " ["
                + actionServiceRegistration + "]");
        LOGGER.info("Registered new " + WATCHER_INTERFACE_NAME + "["
                + configWatchServiceRegistration + "]");

        actionProviderRegistry.put(reference, actionServiceRegistration);
        configWatcherRegistry.put(reference, configWatchServiceRegistration);

    }

    public void unbind(ServiceReference reference) {

        LOGGER.info("Service unregistered [" + reference + "]");

        ServiceRegistration actionProviderRegistration = actionProviderRegistry
                .remove(reference);

        ServiceRegistration configWatcherRegistration = configWatcherRegistry
                .remove(reference);

        if (actionProviderRegistration != null) {
            actionProviderRegistration.unregister();

            LOGGER.info("Unregistered " + PROVIDER_INTERFACE_NAME + " ["
                    + actionProviderRegistration + "]");
        }

        if (configWatcherRegistration != null) {
            configWatcherRegistration.unregister();

            LOGGER.info("Unregistered " + WATCHER_INTERFACE_NAME + " ["
                    + configWatcherRegistration + "]");
        }

    }
}
