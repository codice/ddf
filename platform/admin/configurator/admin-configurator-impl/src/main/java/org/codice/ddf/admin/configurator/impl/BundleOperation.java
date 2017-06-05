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
package org.codice.ddf.admin.configurator.impl;

import static org.codice.ddf.admin.configurator.impl.ConfigValidator.validateString;
import static org.codice.ddf.admin.configurator.impl.OsgiUtils.getBundleContext;

import java.util.Arrays;

import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.bundle.core.BundleStateService;
import org.codice.ddf.admin.configurator.ConfiguratorException;
import org.codice.ddf.admin.configurator.Operation;
import org.codice.ddf.admin.configurator.Result;
import org.codice.ddf.internal.admin.configurator.actions.BundleActions;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transactional handler for starting and stopping bundles.
 */
public class BundleOperation implements Operation<Void> {
    public static class Actions implements BundleActions {
        @Override
        public BundleOperation start(String bundleSymName) throws ConfiguratorException {
            validateString(bundleSymName, "Missing bundle name");
            return new BundleOperation(bundleSymName, true, getBundleContext());
        }

        @Override
        public BundleOperation stop(String bundleSymName) throws ConfiguratorException {
            validateString(bundleSymName, "Missing bundle name");
            return new BundleOperation(bundleSymName, false, getBundleContext());
        }

        @Override
        public boolean isStarted(String bundleSymName) throws ConfiguratorException {
            return start(bundleSymName).readState();
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleOperation.class);

    private final boolean newState;

    private final BundleContext bundleContext;

    private final Bundle bundle;

    private final boolean initActivationState;

    private BundleOperation(String bundleSymName, boolean activate, BundleContext bundleContext) {
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

    @Override
    public Result<Void> commit() throws ConfiguratorException {
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

        return ResultImpl.pass();
    }

    @Override
    public Result<Void> rollback() throws ConfiguratorException {
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

        return ResultImpl.rollback();
    }

    Boolean readState() throws ConfiguratorException {
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
