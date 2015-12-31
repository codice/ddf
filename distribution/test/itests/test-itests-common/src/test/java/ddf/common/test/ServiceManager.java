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
package ddf.common.test;

import static org.apache.karaf.shell.osgi.BlueprintListener.BlueprintState.Created;
import static org.apache.karaf.shell.osgi.BlueprintListener.BlueprintState.Failure;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.get;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.osgi.BlueprintListener;
import org.codice.ddf.ui.admin.api.ConfigurationAdminExt;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.util.OsgiStringUtils;

import com.jayway.restassured.response.Response;

public class ServiceManager {
    public static final Logger LOGGER = LoggerFactory.getLogger(ServiceManager.class);

    private static final int CONFIG_UPDATE_WAIT_INTERVAL_MILLIS = 5;

    public static final long MANAGED_SERVICE_TIMEOUT = TimeUnit.MINUTES.toMillis(10);

    public static final long REQUIRED_BUNDLES_TIMEOUT = TimeUnit.MINUTES.toMillis(10);

    public static final long HTTP_ENDPOINT_TIMEOUT = TimeUnit.MINUTES.toMillis(10);

    private final BundleContext bundleCtx;

    private final MetaTypeService metatype;

    private final AdminConfig adminConfig;

    private BlueprintListener blueprintListener;

    public ServiceManager(BundleContext bundleCtx, MetaTypeService metatype,
            AdminConfig adminConfig) {
        this.bundleCtx = bundleCtx;
        this.metatype = metatype;
        this.adminConfig = adminConfig;
    }

    /**
     * Creates a Managed Service that is created from a Managed Service Factory. Waits for the
     * asynchronous call that the properties have been updated and the service can be used.
     * <p/>
     * For Managed Services not created from a Managed Service Factory, use
     * {@link #startManagedService(String, Map)} instead.
     *
     * @param factoryPid the factory pid of the Managed Service Factory
     * @param properties the service properties for the Managed Service
     * @throws IOException if access to persistent storage fails
     */
    public void createManagedService(String factoryPid, Map<String, Object> properties)
            throws IOException {

        final Configuration sourceConfig = adminConfig.createFactoryConfiguration(factoryPid, null);

        startManagedService(sourceConfig, properties);
    }

    /**
     * Starts a Managed Service. Waits for the asynchronous call that the properties have been
     * updated and the service can be used.
     * <p/>
     * For Managed Services created from a Managed Service Factory, use
     * {@link #createManagedService(String, Map)} instead.
     *
     * @param servicePid persistent identifier of the Managed Service to start
     * @param properties service configuration properties
     * @throws IOException thrown if if access to persistent storage fails
     */
    public void startManagedService(String servicePid, Map<String, Object> properties)
            throws IOException {
        Configuration sourceConfig = adminConfig.getConfiguration(servicePid, null);

        startManagedService(sourceConfig, properties);
    }

    private void startManagedService(Configuration sourceConfig, Map<String, Object> properties)
            throws IOException {
        ServiceConfigurationListener listener =
                new ServiceConfigurationListener(sourceConfig.getPid());

        bundleCtx.registerService(ConfigurationListener.class.getName(), listener, null);

        waitForService(sourceConfig);

        adminConfig.getDdfConfigAdmin()
                .update(sourceConfig.getPid(), properties);

        long millis = 0;
        while (!listener.isUpdated() && millis < MANAGED_SERVICE_TIMEOUT) {
            try {
                Thread.sleep(CONFIG_UPDATE_WAIT_INTERVAL_MILLIS);
                millis += CONFIG_UPDATE_WAIT_INTERVAL_MILLIS;
            } catch (InterruptedException e) {
                LOGGER.info("Interrupted exception while trying to sleep for configuration update",
                        e);
            }
            LOGGER.info("Waiting for configuration to be updated...{}ms", millis);
        }

        if (!listener.isUpdated()) {
            throw new RuntimeException(String.format(
                    "Service configuration %s was not updated within %d minute timeout.",
                    sourceConfig.getPid(),
                    TimeUnit.MILLISECONDS.toMinutes(MANAGED_SERVICE_TIMEOUT)));
        }
    }

    private void waitForService(Configuration sourceConfig) {
        long waitForService = 0;
        boolean serviceStarted = false;
        List<Map<String, Object>> servicesList;
        do {
            try {
                Thread.sleep(CONFIG_UPDATE_WAIT_INTERVAL_MILLIS);
                waitForService += CONFIG_UPDATE_WAIT_INTERVAL_MILLIS;
            } catch (InterruptedException e) {
                LOGGER.info("Interrupted waiting for service to init");
            }

            if (waitForService >= MANAGED_SERVICE_TIMEOUT) {
                throw new RuntimeException(String.format(
                        "Service %s not initialized within %d minute timeout",
                        sourceConfig.getPid(),
                        TimeUnit.MILLISECONDS.toMinutes(MANAGED_SERVICE_TIMEOUT)));
            }

            servicesList = adminConfig.getDdfConfigAdmin()
                    .listServices();
            for (Map<String, Object> service : servicesList) {
                String id = String.valueOf(service.get(ConfigurationAdminExt.MAP_ENTRY_ID));
                if (id.equals(sourceConfig.getPid()) || id.equals(sourceConfig.getFactoryPid())) {
                    serviceStarted = true;
                    break;
                }
            }

        } while (!serviceStarted);
    }

    public void startFeature(boolean wait, String... featureNames) throws Exception {
        ServiceReference<FeaturesService> featuresServiceRef = bundleCtx.getServiceReference(
                FeaturesService.class);
        FeaturesService featuresService = bundleCtx.getService(featuresServiceRef);
        for (String featureName : featureNames) {
            featuresService.installFeature(featureName);
        }
        if (wait) {
            waitForAllBundles();
        }
    }

    public void stopFeature(boolean wait, String... featureNames) throws Exception {
        ServiceReference<FeaturesService> featuresServiceRef = bundleCtx.getServiceReference(
                FeaturesService.class);
        FeaturesService featuresService = bundleCtx.getService(featuresServiceRef);
        for (String featureName : featureNames) {
            featuresService.uninstallFeature(featureName);
        }
        if (wait) {
            waitForAllBundles();
        }
    }

    /**
     * Restarts one or more bundles. The bundles will be stopped in the order provided and started
     * in the reverse order.
     *
     * @param bundleSymbolicNames list of bundle symbolic names to restart
     * @throws BundleException if one of the bundles fails to stop or start
     */
    public void restartBundles(String... bundleSymbolicNames) throws BundleException {
        LOGGER.debug("Restarting bundles {}", bundleSymbolicNames);

        Set<String> bundleSymbolicNameSet = new HashSet<>();
        Collections.addAll(bundleSymbolicNameSet, bundleSymbolicNames);

        Map<String, Bundle> bundlesToRestart = getBundlesToRestart(bundleSymbolicNameSet);

        for (int i = 0; i < bundleSymbolicNames.length; i++) {
            bundlesToRestart.get(bundleSymbolicNames[i])
                    .stop();
        }

        for (int i = bundleSymbolicNames.length - 1; i > 0; i--) {
            bundlesToRestart.get(bundleSymbolicNames[i])
                    .start();
        }
    }

    private Map<String, Bundle> getBundlesToRestart(Set<String> bundleSymbolicNames) {
        Map<String, Bundle> bundlesToRestart = new HashMap();

        for (Bundle bundle : bundleCtx.getBundles()) {
            if (bundleSymbolicNames.contains(bundle.getSymbolicName())) {
                bundlesToRestart.put(bundle.getSymbolicName(), bundle);
            }
        }

        return bundlesToRestart;
    }

    public void waitForAllBundles() throws InterruptedException {
        waitForRequiredBundles("");
    }

    public void waitForRequiredBundles(String symbolicNamePrefix) throws InterruptedException {
        boolean ready = false;
        if (blueprintListener == null) {
            blueprintListener = new BlueprintListener();
            bundleCtx.registerService("org.osgi.service.blueprint.container.BlueprintListener",
                    blueprintListener,
                    null);
        }

        long timeoutLimit = System.currentTimeMillis() + REQUIRED_BUNDLES_TIMEOUT;
        while (!ready) {
            List<Bundle> bundles = Arrays.asList(bundleCtx.getBundles());

            ready = true;
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName()
                        .startsWith(symbolicNamePrefix)) {
                    String bundleName = bundle.getHeaders()
                            .get(Constants.BUNDLE_NAME);
                    String blueprintState = blueprintListener.getState(bundle);
                    if (blueprintState != null) {
                        if (Failure.toString()
                                .equals(blueprintState)) {
                            fail("The blueprint for " + bundleName + " failed.");
                        } else if (!Created.toString()
                                .equals(blueprintState)) {
                            LOGGER.info("{} blueprint not ready with state {}",
                                    bundleName,
                                    blueprintState);
                            ready = false;
                        }
                    }

                    if (!((bundle.getHeaders()
                            .get("Fragment-Host") != null && bundle.getState() == Bundle.RESOLVED)
                            || bundle.getState() == Bundle.ACTIVE)) {
                        LOGGER.info("{} bundle not ready yet", bundleName);
                        ready = false;
                    }
                }
            }

            if (!ready) {
                if (System.currentTimeMillis() > timeoutLimit) {
                    printInactiveBundles();
                    fail(String.format("Bundles and blueprint did not start within %d minutes.",
                            TimeUnit.MILLISECONDS.toMinutes(REQUIRED_BUNDLES_TIMEOUT)));
                }
                LOGGER.info("Bundles not up, sleeping...");
                Thread.sleep(1000);
            }
        }
    }

    public void waitForHttpEndpoint(String path) throws InterruptedException {
        LOGGER.info("Waiting for {}", path);

        long timeoutLimit = System.currentTimeMillis() + HTTP_ENDPOINT_TIMEOUT;
        boolean available = false;

        while (!available) {
            Response response = get(path);
            available = response.getStatusCode() == 200 && response.getBody()
                    .print()
                    .length() > 0;
            if (!available) {
                if (System.currentTimeMillis() > timeoutLimit) {
                    fail(String.format("%s did not start within %d minutes.",
                            path,
                            TimeUnit.MILLISECONDS.toMinutes(HTTP_ENDPOINT_TIMEOUT)));
                }
                Thread.sleep(100);
            }
        }

        LOGGER.info("{} ready.", path);
    }

    public void waitForSourcesToBeAvailable(String restPath, String... sources)
            throws InterruptedException {
        String path = restPath + "sources";
        LOGGER.info("Waiting for sources at {}", path);

        long timeoutLimit = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        boolean available = false;

        while (!available) {
            Response response = get(path);
            String body = response.getBody()
                    .asString();
            if (StringUtils.isNotBlank(body)) {
                available = response.getStatusCode() == 200 && body.length() > 0 && !body.contains(
                        "false") && response.getBody()
                        .jsonPath()
                        .getList("id") != null;
                if (available) {
                    List<Object> ids = response.getBody()
                            .jsonPath()
                            .getList("id");
                    for (String source : sources) {
                        if (!ids.contains(source)) {
                            available = false;
                        }
                    }
                }
            }
            if (!available) {
                if (System.currentTimeMillis() > timeoutLimit) {
                    response.prettyPrint();
                    fail("Sources at " + path + " did not start in time.");
                }
                Thread.sleep(1000);
            }
        }

        LOGGER.info("Sources at {} ready.", path);
    }

    public Map<String, Object> getMetatypeDefaults(String symbolicName, String factoryPid) {
        Map<String, Object> properties = new HashMap<>();
        ObjectClassDefinition bundleMetatype = getObjectClassDefinition(symbolicName, factoryPid);

        for (AttributeDefinition attributeDef : bundleMetatype.getAttributeDefinitions(
                ObjectClassDefinition.ALL)) {
            if (attributeDef.getID() != null) {
                if (attributeDef.getDefaultValue() != null) {
                    if (attributeDef.getCardinality() == 0) {
                        properties.put(attributeDef.getID(),
                                getAttributeValue(attributeDef.getDefaultValue()[0],
                                        attributeDef.getType()));
                    } else {
                        properties.put(attributeDef.getID(), attributeDef.getDefaultValue());
                    }
                } else if (attributeDef.getCardinality() != 0) {
                    properties.put(attributeDef.getID(), new String[0]);
                }
            }
        }

        return properties;
    }

    private Object getAttributeValue(String value, int type) {
        switch (type) {
        case AttributeDefinition.BOOLEAN:
            return Boolean.valueOf(value);
        case AttributeDefinition.BYTE:
            return Byte.valueOf(value);
        case AttributeDefinition.DOUBLE:
            return Double.valueOf(value);
        case AttributeDefinition.CHARACTER:
            return value.toCharArray()[0];
        case AttributeDefinition.FLOAT:
            return Float.valueOf(value);
        case AttributeDefinition.INTEGER:
            return Integer.valueOf(value);
        case AttributeDefinition.LONG:
            return Long.valueOf(value);
        case AttributeDefinition.SHORT:
            return Short.valueOf(value);
        case AttributeDefinition.PASSWORD:
        case AttributeDefinition.STRING:
        default:
            return value;
        }
    }

    private ObjectClassDefinition getObjectClassDefinition(String symbolicName, String pid) {
        Bundle[] bundles = bundleCtx.getBundles();
        for (Bundle bundle : bundles) {
            if (symbolicName.equals(bundle.getSymbolicName())) {
                try {
                    MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
                    if (mti != null) {
                        try {
                            ObjectClassDefinition ocd = mti.getObjectClassDefinition(pid,
                                    Locale.getDefault()
                                            .toString());
                            if (ocd != null) {
                                return ocd;
                            }
                        } catch (IllegalArgumentException e) {
                            // ignoring
                        }
                    }
                } catch (IllegalArgumentException iae) {
                    // ignoring
                }
            }
        }
        return null;
    }

    public void printInactiveBundles() {
        LOGGER.info("Listing inactive bundles");

        for (Bundle bundle : bundleCtx.getBundles()) {
            if (bundle.getState() != Bundle.ACTIVE) {
                StringBuffer headerString = new StringBuffer("[ ");
                Dictionary<String, String> headers = bundle.getHeaders();
                Enumeration<String> keys = headers.keys();

                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    headerString.append(key)
                            .append("=")
                            .append(headers.get(key))
                            .append(", ");
                }

                headerString.append(" ]");
                LOGGER.info("{} | {} | {} | {}",
                        bundle.getSymbolicName(),
                        bundle.getVersion()
                                .toString(),
                        OsgiStringUtils.bundleStateAsString(bundle),
                        headerString.toString());
            }
        }
    }

    public <S> ServiceReference<S> getServiceReference(Class<S> aClass) {
        return bundleCtx.getServiceReference(aClass);
    }

    public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> aClass, String s)
            throws InvalidSyntaxException {
        return bundleCtx.getServiceReferences(aClass, s);
    }

    public <S> S getService(ServiceReference<S> serviceReference) {
        return bundleCtx.getService(serviceReference);
    }

    public <S> S getService(Class<S> aClass) {
        return bundleCtx.getService(bundleCtx.getServiceReference(aClass));
    }

    private class ServiceConfigurationListener implements ConfigurationListener {

        private boolean updated = false;

        private String pid;

        public ServiceConfigurationListener(String pid) {
            this.pid = pid;
        }

        @Override
        public void configurationEvent(ConfigurationEvent event) {
            LOGGER.info("Configuration event received: {}", event);
            if (event.getPid()
                    .equals(pid) && ConfigurationEvent.CM_UPDATED == event.getType()) {
                updated = true;
            }
        }

        public boolean isUpdated() {
            return updated;
        }
    }

}
