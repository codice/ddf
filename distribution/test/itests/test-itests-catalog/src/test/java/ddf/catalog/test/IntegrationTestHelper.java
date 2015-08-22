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
package ddf.catalog.test;

import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.get;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.osgi.BlueprintListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.restassured.response.Response;

import ddf.catalog.CatalogFramework;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.FederatedSource;

public class IntegrationTestHelper {
    private static final int CONFIG_UPDATE_WAIT_INTERVAL = 5;

    public static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTestHelper.class);

    public static final String LOG_CONFIG_PID = AbstractIntegrationTest.LOG_CONFIG_PID;

    public static final String LOGGER_PREFIX = AbstractIntegrationTest.LOGGER_PREFIX;

    public static final String KARAF_VERSION = AbstractIntegrationTest.KARAF_VERSION;

    public static final int ONE_MINUTE_MILLIS = AbstractIntegrationTest.ONE_MINUTE_MILLIS;

    public static final int FIVE_MINUTES_MILLIS = AbstractIntegrationTest.FIVE_MINUTES_MILLIS;

    // TODO: Use the Camel AvailablePortFinder.getNextAvailable() test method
    public static final String HTTP_PORT = AbstractIntegrationTest.HTTP_PORT;

    public static final String HTTPS_PORT = AbstractIntegrationTest.HTTPS_PORT;

    public static final String SSH_PORT = AbstractIntegrationTest.SSH_PORT;

    public static final String RMI_SERVER_PORT = AbstractIntegrationTest.RMI_SERVER_PORT;

    public static final String RMI_REG_PORT = AbstractIntegrationTest.RMI_REG_PORT;

    public static final String SERVICE_ROOT = AbstractIntegrationTest.SERVICE_ROOT;

    public static final String INSECURE_SERVICE_ROOT = AbstractIntegrationTest.INSECURE_SERVICE_ROOT;

    public static final String REST_PATH = AbstractIntegrationTest.REST_PATH;

    public static final String OPENSEARCH_PATH = AbstractIntegrationTest.OPENSEARCH_PATH;

    public static final String DEFAULT_LOG_LEVEL = AbstractIntegrationTest.DEFAULT_LOG_LEVEL;

    public static final String TEST_LOGLEVEL_PROPERTY = "org.codice.test.defaultLoglevel";

    public String logLevel = "";

    private BundleContext bundleCtx;

    private BlueprintListener blueprintListener;

    private ConfigurationAdmin configAdmin;

    private MetaTypeService metatype;

    public IntegrationTestHelper(BundleContext bundleCtx, ConfigurationAdmin configAdmin,
            MetaTypeService metatype) {
        this.bundleCtx = bundleCtx;
        this.configAdmin = configAdmin;
        this.metatype = metatype;
    }

    // Delegated configAdmin methods
    public Configuration createFactoryConfiguration(String s) throws IOException {
        return configAdmin.createFactoryConfiguration(s);
    }

    public Configuration createFactoryConfiguration(String s, String s1) throws IOException {
        return configAdmin.createFactoryConfiguration(s, s1);
    }

    public Configuration getConfiguration(String s, String s1) throws IOException {
        return configAdmin.getConfiguration(s, s1);
    }

    public Configuration getConfiguration(String s) throws IOException {
        return configAdmin.getConfiguration(s);
    }

    public Configuration[] listConfigurations(String s) throws IOException, InvalidSyntaxException {
        return configAdmin.listConfigurations(s);
    }

    /**
     * Creates a Managed Service that is created from a Managed Service Factory. Waits for the
     * asynchronous call that the properties have been updated and the service can be used.
     * <p>
     * For Managed Services not created from a Managed Service Factory, use
     * {@link #startManagedService(String, Map)} instead.
     *
     * @param factoryPid the factory pid of the Managed Service Factory
     * @param properties the service properties for the Managed Service
     * @throws IOException if access to persistent storage fails
     */
    public void createManagedService(String factoryPid, Map<String, Object> properties)
            throws IOException {

        final Configuration sourceConfig = configAdmin.createFactoryConfiguration(factoryPid, null);

        startManagedService(sourceConfig, properties);
    }

    /**
     * Starts a Managed Service. Waits for the asynchronous call that the properties have bee
     * updated and the service can be used.
     * <p>
     * For Managed Services created from a Managed Service Factory, use
     * {@link #createManagedService(String, Map)} instead.
     *
     * @param servicePid persistent identifier of the Managed Service to start
     * @param properties service configuration properties
     * @throws IOException thrown if if access to persistent storage fails
     */
    public void startManagedService(String servicePid, Map<String, Object> properties)
            throws IOException {
        Configuration sourceConfig = configAdmin.getConfiguration(servicePid);

        startManagedService(sourceConfig, properties);
    }

    private void startManagedService(Configuration sourceConfig, Map<String, Object> properties)
            throws IOException {
        ServiceConfigurationListener listener = new ServiceConfigurationListener(
                sourceConfig.getPid());

        bundleCtx.registerService(ConfigurationListener.class.getName(), listener, null);

        sourceConfig.update(new Hashtable<>(properties));

        long millis = 0;
        while (!listener.isUpdated() && millis < TimeUnit.MINUTES.toMillis(10)) {
            try {
                Thread.sleep(CONFIG_UPDATE_WAIT_INTERVAL);
                millis += CONFIG_UPDATE_WAIT_INTERVAL;
            } catch (InterruptedException e) {
                LOGGER.info("Interrupted exception while trying to sleep for configuration update",
                        e);
            }
            LOGGER.info("Waiting for configuration to be updated...{}ms", millis);
        }

        if (!listener.isUpdated()) {
            throw new RuntimeException("Service was not updated before timeout.");
        }
    }

    public void setLogLevels() throws IOException {

        logLevel = System.getProperty(TEST_LOGLEVEL_PROPERTY);

        Configuration logConfig = configAdmin.getConfiguration(LOG_CONFIG_PID, null);
        Dictionary<String, Object> properties = logConfig.getProperties();
        if (StringUtils.isEmpty(logLevel)) {
            properties.put(LOGGER_PREFIX + "ddf", DEFAULT_LOG_LEVEL);
            properties.put(LOGGER_PREFIX + "org.codice", DEFAULT_LOG_LEVEL);
        } else {
            properties.put(LOGGER_PREFIX + "*", logLevel);
        }

        logConfig.update(properties);
    }

    public void setFanout(boolean fanoutEnabled) throws IOException {
        Configuration configuration = configAdmin
                .getConfiguration("ddf.catalog.CatalogFrameworkImpl", null);

        Dictionary<String, Object> properties = configuration.getProperties();
        if (properties == null) {
            properties = new Hashtable<String, Object>();
        }
        if (fanoutEnabled) {
            properties.put("fanoutEnabled", "True");
        } else {
            properties.put("fanoutEnabled", "False");
        }

        configuration.update(properties);
    }

    public void waitForAllBundles() throws InterruptedException {
        waitForRequiredBundles("");
    }

    public void waitForRequiredBundles(String symbolicNamePrefix) throws InterruptedException {
        boolean ready = false;
        if (blueprintListener == null) {
            blueprintListener = new BlueprintListener();
            bundleCtx.registerService("org.osgi.service.blueprint.container.BlueprintListener",
                    blueprintListener, null);
        }

        long timeoutLimit = System.currentTimeMillis() + FIVE_MINUTES_MILLIS;
        while (!ready) {
            List<Bundle> bundles = Arrays.asList(bundleCtx.getBundles());

            ready = true;
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName().startsWith(symbolicNamePrefix)) {
                    String bundleName = bundle.getHeaders().get(Constants.BUNDLE_NAME);
                    String blueprintState = blueprintListener.getState(bundle);
                    if (blueprintState != null) {
                        if (BlueprintListener.BlueprintState.Failure.toString()
                                .equals(blueprintState)) {
                            fail("The blueprint for " + bundleName + " failed.");
                        } else if (!BlueprintListener.BlueprintState.Created.toString()
                                .equals(blueprintState)) {
                            LOGGER.info("{} blueprint not ready with state {}", bundleName,
                                    blueprintState);
                            ready = false;
                        }
                    }

                    if (!((bundle.getHeaders().get("Fragment-Host") != null
                            && bundle.getState() == Bundle.RESOLVED)
                            || bundle.getState() == Bundle.ACTIVE)) {
                        LOGGER.info("{} bundle not ready yet", bundleName);
                        ready = false;
                    }
                }
            }

            if (!ready) {
                if (System.currentTimeMillis() > timeoutLimit) {
                    fail("Bundles and blueprint did not start in time.");
                }
                LOGGER.info("Bundles not up, sleeping...");
                Thread.sleep(1000);
            }
        }
    }

    public CatalogProvider waitForCatalogProvider() throws InterruptedException {
        LOGGER.info("Waiting for CatalogProvider to become available.");

        CatalogProvider provider = null;
        long timeoutLimit = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        boolean available = false;

        while (!available) {
            if (provider == null) {
                ServiceReference<CatalogProvider> providerRef = bundleCtx
                        .getServiceReference(CatalogProvider.class);
                if (providerRef != null) {
                    provider = bundleCtx.getService(providerRef);
                }
            }

            if (provider != null) {
                available = provider.isAvailable();
            }
            if (!available) {
                if (System.currentTimeMillis() > timeoutLimit) {
                    fail("Catalog provider timed out.");
                }
                Thread.sleep(100);
            }
        }

        LOGGER.info("CatalogProvider is available.");
        return provider;
    }

    public FederatedSource waitForFederatedSource(String id)
            throws InterruptedException, InvalidSyntaxException {
        LOGGER.info("Waiting for FederatedSource {} to become available.", id);

        FederatedSource source = null;
        long timeoutLimit = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        boolean available = false;

        while (!available) {
            if (source == null) {
                Collection<ServiceReference<FederatedSource>> srcRefs = bundleCtx
                        .getServiceReferences(FederatedSource.class, null);
                for (ServiceReference<FederatedSource> srcRef : srcRefs) {
                    FederatedSource src = bundleCtx.getService(srcRef);
                    if (id.equals(src.getId())) {
                        source = src;
                    }
                }
            }

            if (source != null) {
                available = source.isAvailable();
            }

            if (!available) {
                if (System.currentTimeMillis() > timeoutLimit) {
                    fail("Federated Source was not created in a timely manner.");
                }
                Thread.sleep(100);
            }
        }

        LOGGER.info("FederatedSource {} is available.", id);
        return source;
    }

    public void waitForHttpEndpoint(String path) throws InterruptedException {
        LOGGER.info("Waiting for {}", path);

        long timeoutLimit = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        boolean available = false;

        while (!available) {
            Response response = get(path);
            available = response.getStatusCode() == 200 && response.getBody().print().length() > 0;
            if (!available) {
                if (System.currentTimeMillis() > timeoutLimit) {
                    fail(path + " did not start in time.");
                }
                Thread.sleep(100);
            }
        }

        LOGGER.info("{} ready.", path);
    }

    public void waitForSourcesToBeAvailable(String... sources) throws InterruptedException {
        String path = REST_PATH + "sources";
        LOGGER.info("Waiting for sources at {}", path);

        long timeoutLimit = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        boolean available = false;

        while (!available) {
            Response response = get(path);
            String body = response.getBody().asString();
            if (StringUtils.isNotBlank(body)) {
                available = response.getStatusCode() == 200 && body.length() > 0 && !body
                        .contains("false") && response.getBody().jsonPath().getList("id") != null;
                if (available) {
                    List<Object> ids = response.getBody().jsonPath().getList("id");
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

    public CatalogFramework getCatalogFramework() throws InterruptedException {
        LOGGER.info("getting framework");

        CatalogFramework catalogFramework = null;

        ServiceReference<CatalogFramework> providerRef = bundleCtx
                .getServiceReference(CatalogFramework.class);
        if (providerRef != null) {
            catalogFramework = bundleCtx.getService(providerRef);
        }

        return catalogFramework;
    }

    public Map<String, Object> getMetatypeDefaults(String symbolicName, String factoryPid) {
        Map<String, Object> properties = new HashMap<>();
        ObjectClassDefinition bundleMetatype = getObjectClassDefinition(symbolicName, factoryPid);

        for (AttributeDefinition attributeDef : bundleMetatype
                .getAttributeDefinitions(ObjectClassDefinition.ALL)) {
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
            return Character.valueOf(value.toCharArray()[0]);
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
                            ObjectClassDefinition ocd = mti
                                    .getObjectClassDefinition(pid, Locale.getDefault().toString());
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

    public void startFeature(boolean wait, String... featureNames) throws Exception {
        ServiceReference<FeaturesService> featuresServiceRef = bundleCtx
                .getServiceReference(FeaturesService.class);
        FeaturesService featuresService = bundleCtx.getService(featuresServiceRef);
        for (String featureName : featureNames) {
            featuresService.installFeature(featureName);
        }
        if (wait) {
            waitForAllBundles();
        }
    }

    public void stopFeature(boolean wait, String... featureNames) throws Exception {
        ServiceReference<FeaturesService> featuresServiceRef = bundleCtx
                .getServiceReference(FeaturesService.class);
        FeaturesService featuresService = bundleCtx.getService(featuresServiceRef);
        for (String featureName : featureNames) {
            featuresService.uninstallFeature(featureName);
        }
        if (wait) {
            waitForAllBundles();
        }
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
            if (event.getPid().equals(pid) && ConfigurationEvent.CM_UPDATED == event.getType()) {
                updated = true;
            }
        }

        public boolean isUpdated() {
            return updated;
        }
    }

}
