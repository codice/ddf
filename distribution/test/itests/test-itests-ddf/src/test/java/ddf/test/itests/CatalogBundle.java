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
package ddf.test.itests;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.FederatedSource;
import ddf.common.test.AdminConfig;
import ddf.common.test.ServiceManager;

public class CatalogBundle {
    protected static final Logger LOGGER = LoggerFactory.getLogger(CatalogBundle.class);

    public static final long CATALOG_PROVIDER_TIMEOUT = TimeUnit.MINUTES.toMillis(10);

    public static final String CATALOG_FRAMEWORK_PID = "ddf.catalog.CatalogFrameworkImpl";

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

        FederatedSource source = null;
        long timeoutLimit = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        boolean available = false;

        while (!available) {
            if (source == null) {
                Collection<ServiceReference<FederatedSource>> srcRefs =
                        serviceManager.getServiceReferences(FederatedSource.class, null);
                for (ServiceReference<FederatedSource> srcRef : srcRefs) {
                    FederatedSource src = serviceManager.getService(srcRef);
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
                    fail("Federated source (" + id + ") was not created in a timely manner.");
                }
                Thread.sleep(100);
            }
        }

        LOGGER.info("FederatedSource {} is available.", id);
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
}
