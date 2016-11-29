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
package org.codice.ui.admin.wizard.config.handlers;

import static org.apache.karaf.features.FeaturesService.Option.NoAutoRefreshBundles;

import java.util.EnumSet;

import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesService;
import org.codice.ui.admin.wizard.config.ConfigHandler;
import org.codice.ui.admin.wizard.config.ConfiguratorException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureConfigHandler implements ConfigHandler<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureConfigHandler.class);

    private final String featureName;

    private final boolean newState;

    private final boolean initActivationState;

    private FeatureConfigHandler(String featureName, boolean newState) {
        this.featureName = featureName;
        this.newState = newState;

        initActivationState = lookupFeatureStatus(getFeaturesService(), featureName);
    }

    public static FeatureConfigHandler forStart(String featureName) {
        return new FeatureConfigHandler(featureName, true);
    }

    public static FeatureConfigHandler forStop(String featureName) {
        return new FeatureConfigHandler(featureName, false);
    }

    @Override
    public Void commit() throws ConfiguratorException {
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

        return null;
    }

    @Override
    public Void rollback() throws ConfiguratorException {
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

        return null;
    }

    private FeaturesService getFeaturesService() {
        BundleContext context = getBundleContext();
        ServiceReference<FeaturesService> serviceReference = context.getServiceReference(
                FeaturesService.class);
        return context.getService(serviceReference);
    }

    private Boolean lookupFeatureStatus(FeaturesService featuresService, String featureName) {
        return featuresService.getState(featureName) == FeatureState.Started;
    }
}
