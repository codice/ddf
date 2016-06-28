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
package ddf.test.itests;

import static org.junit.Assert.fail;

import java.io.IOException;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.CatalogStore;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.Source;
import ddf.common.test.AdminConfig;
import ddf.common.test.ServiceManager;

public class CatalogBundle {
    protected static final Logger LOGGER = LoggerFactory.getLogger(CatalogBundle.class);

    public static final long CATALOG_PROVIDER_TIMEOUT = TimeUnit.MINUTES.toMillis(10);

    public static final String CATALOG_FRAMEWORK_PID = "ddf.catalog.CatalogFrameworkImpl";

    public static final String RESOURCE_DOWNLOAD_MANAGER_PID = "ddf.catalog.resource.download.ReliableResourceDownloadManager";

    private static final String CACHE_ENABLED_KEY = "cacheEnabled";

    private final ServiceManager serviceManager;

    private final AdminConfig adminConfig;

    public CatalogBundle(ServiceManager serviceManager, AdminConfig adminConfig) {
        this.serviceManager = serviceManager;
        this.adminConfig = adminConfig;
    }

    public CatalogProvider waitForCatalogProvider() throws InterruptedException {
        LOGGER.info("Waiting for CatalogProvider to become available.");
        serviceManager.printInactiveBundles();

        CatalogProvider provider = null;
        long timeoutLimit = System.currentTimeMillis() + CATALOG_PROVIDER_TIMEOUT;
        boolean available = false;

        while (!available) {
            if (provider == null) {
                ServiceReference<CatalogProvider> providerRef = serviceManager.getServiceReference(
                        CatalogProvider.class);
                if (providerRef != null) {
                    provider = serviceManager.getService(providerRef);
                }
            }

            if (provider != null) {
                available = provider.isAvailable();
            }

            if (!available) {
                if (System.currentTimeMillis() > timeoutLimit) {
                    LOGGER.info("CatalogProvider.isAvailable = false");
                    serviceManager.printInactiveBundles();
                    fail(String.format("Catalog provider timed out after %d minutes.",
                            TimeUnit.MILLISECONDS.toMinutes(CATALOG_PROVIDER_TIMEOUT)));
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
        return waitForSource(id, FederatedSource.class);
    }

    public CatalogStore waitForCatalogStore(String id)
            throws InterruptedException, InvalidSyntaxException {
        LOGGER.info("Waiting for CatalogStore {} to become available.", id);
        return waitForSource(id, CatalogStore.class);
    }

    private <T extends Source> T waitForSource(String id, Class<T> type)
            throws InterruptedException, InvalidSyntaxException {
        T source = null;
        long timeoutLimit = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        boolean available = false;

        while (!available) {
            if (source == null) {
                source = serviceManager.getServiceReferences(type, null)
                        .stream()
                        .map(serviceManager::getService)
                        .filter(src -> id.equals(src.getId()))
                        .findFirst()
                        .orElse(null);
            }
            if (source != null) {
                available = source.isAvailable();
            }
            if (!available) {
                if (System.currentTimeMillis() > timeoutLimit) {
                    fail("Source (" + id + ") was not created in a timely manner.");
                }
                Thread.sleep(100);
            }
        }
        LOGGER.info("Source {} is available.", id);
        return source;
    }

    public CatalogFramework getCatalogFramework() throws InterruptedException {
        LOGGER.info("getting framework");

        CatalogFramework catalogFramework = null;

        ServiceReference<CatalogFramework> providerRef = serviceManager.getServiceReference(
                CatalogFramework.class);
        if (providerRef != null) {
            catalogFramework = serviceManager.getService(providerRef);
        }

        return catalogFramework;
    }

    public void setFanout(boolean fanoutEnabled) throws IOException {
        Map<String, Object> properties = adminConfig.getDdfConfigAdmin()
                .getProperties(CATALOG_FRAMEWORK_PID);
        if (properties == null) {
            properties = new Hashtable<>();
        }
        if (fanoutEnabled) {
            properties.put("fanoutEnabled", "True");
        } else {
            properties.put("fanoutEnabled", "False");
        }

        serviceManager.startManagedService(CATALOG_FRAMEWORK_PID, properties);
    }

    public void setupCaching(boolean cachingEnabled) throws IOException {
        Map<String, Object> existingProperties = adminConfig.getDdfConfigAdmin()
                .getProperties(RESOURCE_DOWNLOAD_MANAGER_PID);
        if (existingProperties == null) {
            existingProperties = new Hashtable<String, Object>();
        }
        Hashtable<String, Object> updatedProperties = new Hashtable<>();
        updatedProperties.putAll(existingProperties);
        if (cachingEnabled) {
            updatedProperties.put(CACHE_ENABLED_KEY, Boolean.TRUE);
        } else {
            updatedProperties.put(CACHE_ENABLED_KEY, Boolean.FALSE);
        }

        Configuration configuration = adminConfig.getConfiguration(RESOURCE_DOWNLOAD_MANAGER_PID, null);
        configuration.update(updatedProperties);
    }

    public boolean isCacheEnabled() throws Exception {
        Map<String, Object> existingProperties = adminConfig.getDdfConfigAdmin()
                .getProperties(RESOURCE_DOWNLOAD_MANAGER_PID);

        if (existingProperties != null) {
            LOGGER.debug("Properties for PID [{}] are {}", RESOURCE_DOWNLOAD_MANAGER_PID,
                    existingProperties);

            if (!existingProperties.containsKey(CACHE_ENABLED_KEY)) {
                String message = String.format("Properties for PID [%s] do not contain key [%s]",
                        RESOURCE_DOWNLOAD_MANAGER_PID, CACHE_ENABLED_KEY);
                LOGGER.error(message);
                fail(message);
            }

            if (existingProperties.get(CACHE_ENABLED_KEY) == null) {
                String message = String.format("Value of property [%s] for PID [%s] is null",
                        CACHE_ENABLED_KEY, RESOURCE_DOWNLOAD_MANAGER_PID);
                LOGGER.error(message);
                fail(message);
            }

            if (existingProperties.get(CACHE_ENABLED_KEY) instanceof Boolean) {
                return (Boolean) existingProperties.get(CACHE_ENABLED_KEY);
            }

            String message = String.format(
                    "Unable to process [%s] property. [%s] type is: %s.  It must be of type %s",
                    CACHE_ENABLED_KEY, CACHE_ENABLED_KEY,
                    existingProperties.get(CACHE_ENABLED_KEY).getClass(), Boolean.class.getName());
            LOGGER.error(message);
            fail(message);
        }

        String message = String.format("Properties for PID [%s] are null.",
                RESOURCE_DOWNLOAD_MANAGER_PID);
        LOGGER.error(message);
        fail(message);
        return false;
    }
}
