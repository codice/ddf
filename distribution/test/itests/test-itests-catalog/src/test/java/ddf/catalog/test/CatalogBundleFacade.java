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

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.FederatedSource;
import ddf.common.test.ServiceManagerFacade;

public class CatalogBundleFacade {
    protected static final Logger LOGGER = LoggerFactory.getLogger(CatalogBundleFacade.class);

    private final ServiceManagerFacade serviceManagerFacade;

    public CatalogBundleFacade(ServiceManagerFacade serviceManagerFacade) {
        this.serviceManagerFacade = serviceManagerFacade;
    }

    public CatalogProvider waitForCatalogProvider() throws InterruptedException {
        LOGGER.info("Waiting for CatalogProvider to become available.");

        CatalogProvider provider = null;
        long timeoutLimit = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        boolean available = false;

        while (!available) {
            if (provider == null) {
                ServiceReference<CatalogProvider> providerRef = serviceManagerFacade
                        .getServiceReference(CatalogProvider.class);
                if (providerRef != null) {
                    provider = serviceManagerFacade.getService(providerRef);
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
                Collection<ServiceReference<FederatedSource>> srcRefs = serviceManagerFacade
                        .getServiceReferences(FederatedSource.class, null);
                for (ServiceReference<FederatedSource> srcRef : srcRefs) {
                    FederatedSource src = serviceManagerFacade.getService(srcRef);
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

    public CatalogFramework getCatalogFramework() throws InterruptedException {
        LOGGER.info("getting framework");

        CatalogFramework catalogFramework = null;

        ServiceReference<CatalogFramework> providerRef = serviceManagerFacade
                .getServiceReference(CatalogFramework.class);
        if (providerRef != null) {
            catalogFramework = serviceManagerFacade.getService(providerRef);
        }

        return catalogFramework;
    }

}
