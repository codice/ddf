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
 **/
package org.codice.ddf.admin.api.persist.handlers;

import java.util.Arrays;

import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.bundle.core.BundleStateService;
import org.codice.ddf.admin.api.persist.ConfigHandler;
import org.codice.ddf.admin.api.persist.ConfiguratorException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transactional handler for starting and stopping bundles.
 */
public class BundleConfigHandler implements ConfigHandler<Void, Boolean> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BundleConfigHandler.class);

    private final boolean newState;

    private final BundleContext bundleContext;

    private final Bundle bundle;

    private final boolean initActivationState;

    private BundleConfigHandler(String bundleSymName, boolean activate,
            BundleContext bundleContext) {
        this.newState = activate;
        this.bundleContext = bundleContext;

        bundle = Arrays.stream(bundleContext.getBundles())
                .filter(b -> b.getSymbolicName()
                        .equals(bundleSymName))
                .findFirst()
                .orElseThrow(() -> new ConfiguratorException(String.format(
                        "No bundle found with symbolic name %s",
                        bundleSymName)));
        initActivationState = lookupBundleState();
    }

    /**
     * Creates a handler that will start a bundle as part of a transaction.
     *
     * @param bundleSymName the name of the bundle to start
     * @param bundleContext context needed for OSGi interaction
     * @return instance of this class
     */
    public static BundleConfigHandler forStart(String bundleSymName, BundleContext bundleContext) {
        return new BundleConfigHandler(bundleSymName, true, bundleContext);
    }

    /**
     * Creates a handler that will stop a bundle as part of a transaction.
     *
     * @param bundleSymName the name of the bundle to stop
     * @param bundleContext context needed for OSGi interaction
     * @return instance of this class
     */
    public static BundleConfigHandler forStop(String bundleSymName, BundleContext bundleContext) {
        return new BundleConfigHandler(bundleSymName, false, bundleContext);
    }

    @Override
    public Void commit() throws ConfiguratorException {
        try {
            if (initActivationState != newState) {
                if (newState) {
                    bundle.start();
                } else {
                    bundle.stop();
                }
            }
        } catch (BundleException e) {
            LOGGER.debug("Error starting/stopping bundle", e);
            throw new ConfiguratorException("Internal error");
        }

        return null;
    }

    @Override
    public Void rollback() throws ConfiguratorException {
        try {
            if (initActivationState != lookupBundleState()) {
                if (initActivationState) {
                    bundle.start();
                } else {
                    bundle.stop();
                }
            }
        } catch (BundleException e) {
            LOGGER.debug("Error starting/stopping bundle", e);
            throw new ConfiguratorException("Internal error");
        }

        return null;
    }

    @Override
    public Boolean readState() throws ConfiguratorException {
        return lookupBundleState();
    }

    private BundleStateService getBundleStateService() {
        ServiceReference<BundleStateService> serviceReference = bundleContext.getServiceReference(
                BundleStateService.class);
        return bundleContext.getService(serviceReference);
    }

    private boolean lookupBundleState() {
        return getBundleStateService().getState(bundle) == BundleState.Active;
    }
}
