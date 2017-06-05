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

import static org.apache.karaf.features.FeaturesService.Option.NoAutoRefreshBundles;
import static org.codice.ddf.admin.configurator.impl.ConfigValidator.validateString;
import static org.codice.ddf.admin.configurator.impl.OsgiUtils.getBundleContext;

import java.util.EnumSet;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesService;
import org.codice.ddf.admin.configurator.ConfiguratorException;
import org.codice.ddf.admin.configurator.Operation;
import org.codice.ddf.admin.configurator.Result;
import org.codice.ddf.internal.admin.configurator.actions.FeatureActions;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transactional handler for starting and stopping features.
 */
public class FeatureOperation implements Operation<Void> {
    public static class Actions implements FeatureActions {
        @Override
        public FeatureOperation start(String featureName) throws ConfiguratorException {
            validateString(featureName, "Missing feature name");
            return new FeatureOperation(featureName, true, getBundleContext());
        }

        @Override
        public FeatureOperation stop(String featureName) throws ConfiguratorException {
            validateString(featureName, "Missing feature name");
            return new FeatureOperation(featureName, false, getBundleContext());
        }

        @Override
        public boolean isFeatureStarted(String featureName) throws ConfiguratorException {
            return start(featureName).readState();
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureOperation.class);

    private final String featureName;

    private final boolean newState;

    private final BundleContext bundleContext;

    private final boolean initActivationState;

    private FeatureOperation(String featureName, boolean newState, BundleContext bundleContext) {
        this.featureName = featureName;
        this.newState = newState;
        this.bundleContext = bundleContext;

        initActivationState = lookupFeatureStatus(getFeaturesService(), featureName);
    }

    @Override
    public Result<Void> commit() throws ConfiguratorException {
        FeaturesService featuresService = getFeaturesService();
        try {
            if (initActivationState != newState) {
                if (newState) {
                    featuresService.installFeature(featureName, EnumSet.of(NoAutoRefreshBundles));
                } else {
                    featuresService.uninstallFeature(featureName);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error installing/uninstalling feature", e);
            throw new ConfiguratorException("Internal error");
        }

        return ResultImpl.pass();
    }

    @Override
    public Result<Void> rollback() throws ConfiguratorException {
        FeaturesService featuresService = getFeaturesService();
        try {
            if (initActivationState != lookupFeatureStatus(featuresService, featureName)) {
                if (initActivationState) {
                    featuresService.installFeature(featureName, EnumSet.of(NoAutoRefreshBundles));
                } else {
                    featuresService.uninstallFeature(featureName);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error installing/uninstalling feature", e);
            throw new ConfiguratorException("Internal error");
        }

        return ResultImpl.rollback();
    }

    Boolean readState() throws ConfiguratorException {
        return lookupFeatureStatus(getFeaturesService(), featureName);
    }

    private FeaturesService getFeaturesService() {
        ServiceReference<FeaturesService> serviceReference = bundleContext.getServiceReference(
                FeaturesService.class);
        return bundleContext.getService(serviceReference);
    }

    private Boolean lookupFeatureStatus(FeaturesService featuresService, String featureName)
            throws ConfiguratorException {
        try {
            Feature feature = featuresService.getFeature(featureName);
            return featuresService.getState(String.format("%s/%s",
                    feature.getName(),
                    feature.getVersion())) == FeatureState.Started;
        } catch (Exception e) {
            throw new ConfiguratorException(String.format("No feature found named %s", featureName),
                    e);
        }
    }
}
