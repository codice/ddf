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
package org.codice.ddf.endpoints.rest.action;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.action.ActionProvider;
import ddf.catalog.Constants;

/**
 * Registers {@link ActionProvider} objects into the Service Registry based upon the services
 * provided by other objects.
 *
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 */
public class ActionProviderRegistryProxy {

    static final String ACTION_ID_PREFIX = "catalog.data.metacard.";

    private static final String PROVIDER_INTERFACE_NAME = ActionProvider.class.getName();

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionProviderRegistryProxy.class);

    private Map<ServiceReference, ServiceRegistration> actionProviderRegistry = new HashMap<ServiceReference, ServiceRegistration>();

    private BundleContext bundleContext;

    private MetacardTransformerActionProviderFactory actionFactory;

    public ActionProviderRegistryProxy(BundleContext bundleContext,
            MetacardTransformerActionProviderFactory actionFactory) {

        this.bundleContext = bundleContext;
        this.actionFactory = actionFactory;
    }

    public void bind(ServiceReference reference) {

        LOGGER.info("New service registered [{}]", reference);

        String transformerId = null;

        if (reference.getProperty(Constants.SERVICE_ID) != null) {

            transformerId = reference.getProperty(Constants.SERVICE_ID).toString();

            // backwards compatibility
        } else if (reference.getProperty(Constants.SERVICE_SHORTNAME) != null) {

            transformerId = reference.getProperty(Constants.SERVICE_SHORTNAME).toString();
        }

        if (transformerId == null) {
            return;
        }

        String actionProviderId = ACTION_ID_PREFIX + transformerId;

        ActionProvider provider = actionFactory
                .createActionProvider(actionProviderId, transformerId);

        Dictionary actionProviderProperties = new Hashtable<String, String>();

        actionProviderProperties.put(Constants.SERVICE_ID, actionProviderId);

        ServiceRegistration actionServiceRegistration = bundleContext
                .registerService(PROVIDER_INTERFACE_NAME, provider, actionProviderProperties);

        LOGGER.info("Registered new {} [{}]", PROVIDER_INTERFACE_NAME, actionServiceRegistration);

        actionProviderRegistry.put(reference, actionServiceRegistration);

    }

    public void unbind(ServiceReference reference) {

        LOGGER.info("Service unregistered [" + reference + "]");

        ServiceRegistration actionProviderRegistration = actionProviderRegistry.remove(reference);

        if (actionProviderRegistration != null) {
            actionProviderRegistration.unregister();
            LOGGER.info("Unregistered {} [{}]", PROVIDER_INTERFACE_NAME,
                    actionProviderRegistration);
        }

    }
}
