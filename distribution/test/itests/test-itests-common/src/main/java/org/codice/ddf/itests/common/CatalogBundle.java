/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.itests.common;

import static org.junit.Assert.fail;

import ddf.catalog.CatalogFramework;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.impl.SourceInfoRequestEnterprise;
import ddf.catalog.operation.impl.SourceInfoRequestLocal;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.CatalogStore;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.management.NotCompliantMBeanException;
import org.codice.ddf.configuration.DictionaryMap;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogBundle {
  protected static final Logger LOGGER = LoggerFactory.getLogger(CatalogBundle.class);

  public static final long CATALOG_PROVIDER_TIMEOUT =
      AbstractIntegrationTest.GENERIC_TIMEOUT_MILLISECONDS;

  public static final String CATALOG_FRAMEWORK_PID = "ddf.catalog.CatalogFrameworkImpl";

  public static final String RESOURCE_DOWNLOAD_MANAGER_PID =
      "ddf.catalog.resource.download.ReliableResourceDownloadManager";

  private final ServiceManager serviceManager;

  private final AdminConfig adminConfig;

  public CatalogBundle(ServiceManager serviceManager, AdminConfig adminConfig) {
    this.serviceManager = serviceManager;
    this.adminConfig = adminConfig;
  }

  public CatalogProvider waitForCatalogProvider() throws InterruptedException {
    LOGGER.info("Waiting for CatalogProvider to become available.");
    serviceManager.printInactiveBundlesInfo();

    CatalogProvider provider = null;
    CatalogFramework framework = null;
    long timeoutLimit = System.currentTimeMillis() + CATALOG_PROVIDER_TIMEOUT;
    boolean available = false;

    while (!available) {
      if (provider == null) {
        ServiceReference<CatalogFramework> frameworkRef =
            serviceManager.getServiceReference(CatalogFramework.class);
        ServiceReference<CatalogProvider> providerRef =
            serviceManager.getServiceReference(CatalogProvider.class);
        if (providerRef != null) {
          provider = serviceManager.getService(providerRef);
        }
        if (frameworkRef != null) {
          framework = serviceManager.getService(frameworkRef);
        }
      }

      if (framework != null && provider != null) {
        SourceInfoRequestLocal sourceInfoRequestEnterprise = new SourceInfoRequestLocal(true);

        try {
          SourceInfoResponse sources = framework.getSourceInfo(sourceInfoRequestEnterprise);
          Set<SourceDescriptor> sourceInfo = sources.getSourceInfo();
          for (SourceDescriptor sourceDescriptor : sourceInfo) {
            if (sourceDescriptor.getSourceId().equals(provider.getId())) {
              available = sourceDescriptor.isAvailable() && provider.isAvailable();
              LOGGER.info("CatalogProvider.isAvailable = {}", available);
            }
          }
        } catch (SourceUnavailableException e) {
          available = false;
        }
      }

      if (!available) {
        if (System.currentTimeMillis() > timeoutLimit) {
          LOGGER.info("CatalogProvider.isAvailable = false");
          serviceManager.printInactiveBundles();
          fail(
              String.format(
                  "Catalog provider timed out after %d minutes.",
                  TimeUnit.MILLISECONDS.toMinutes(CATALOG_PROVIDER_TIMEOUT)));
        }
        Thread.sleep(1000);
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
    long timeoutLimit = System.currentTimeMillis() + CATALOG_PROVIDER_TIMEOUT;
    boolean available = false;

    while (!available) {
      ServiceReference<CatalogFramework> frameworkRef =
          serviceManager.getServiceReference(CatalogFramework.class);
      CatalogFramework framework = null;
      if (frameworkRef != null) {
        framework = serviceManager.getService(frameworkRef);
      }
      if (source == null) {
        source =
            serviceManager
                .getServiceReferences(type, null)
                .stream()
                .map(serviceManager::getService)
                .filter(src -> id.equals(src.getId()))
                .findFirst()
                .orElse(null);
      }
      if (source != null && framework != null) {
        SourceInfoRequestEnterprise sourceInfoRequestEnterprise =
            new SourceInfoRequestEnterprise(true);

        try {
          SourceInfoResponse sources = framework.getSourceInfo(sourceInfoRequestEnterprise);
          Set<SourceDescriptor> sourceInfo = sources.getSourceInfo();
          for (SourceDescriptor sourceDescriptor : sourceInfo) {
            if (sourceDescriptor.getSourceId().equals(source.getId())) {
              available = sourceDescriptor.isAvailable() && source.isAvailable();
              LOGGER.info(
                  "Source.isAvailable = {} Framework.isAvailable = {}",
                  source.isAvailable(),
                  sourceDescriptor.isAvailable());
            }
          }
        } catch (SourceUnavailableException e) {
          available = false;
        }
      } else {
        LOGGER.info(
            "Currently no source of type {} and name {} could be found", type.getName(), id);
      }
      if (!available) {
        if (System.currentTimeMillis() > timeoutLimit) {
          fail("Source (" + id + ") was not created in a timely manner.");
        }
        Thread.sleep(1000);
      }
    }
    LOGGER.info("Source {} is available.", id);
    return source;
  }

  public CatalogFramework getCatalogFramework() {
    LOGGER.info("getting framework");

    CatalogFramework catalogFramework = null;

    ServiceReference<CatalogFramework> providerRef =
        serviceManager.getServiceReference(CatalogFramework.class);
    if (providerRef != null) {
      catalogFramework = serviceManager.getService(providerRef);
    }

    return catalogFramework;
  }

  public void setFanout(boolean fanoutEnabled) throws IOException {
    Map<String, Object> properties = null;
    try {
      properties =
          Optional.ofNullable(
                  adminConfig.getAdminConsoleService().getProperties(CATALOG_FRAMEWORK_PID))
              .orElse(new DictionaryMap<>());
    } catch (NotCompliantMBeanException e) {
      properties = new DictionaryMap<>();
    }

    if (fanoutEnabled) {
      properties.put("fanoutEnabled", "True");
    } else {
      properties.put("fanoutEnabled", "False");
    }

    serviceManager.startManagedService(CATALOG_FRAMEWORK_PID, properties);
  }

  public void setFanoutTagBlacklist(List<String> blacklist)
      throws IOException, InterruptedException {
    Map<String, Object> properties = null;
    try {
      properties =
          Optional.ofNullable(
                  adminConfig.getAdminConsoleService().getProperties(CATALOG_FRAMEWORK_PID))
              .orElse(new DictionaryMap<>());
    } catch (NotCompliantMBeanException e) {
      properties = new DictionaryMap<>();
    }

    if (blacklist != null) {
      properties.put("fanoutTagBlacklist", String.join(",", blacklist));

      serviceManager.startManagedService(CATALOG_FRAMEWORK_PID, properties);
    }
    serviceManager.waitForAllBundles();
  }

  public void setupCaching(boolean cachingEnabled) throws IOException {
    if (cachingEnabled) {
      setConfigProperty(RESOURCE_DOWNLOAD_MANAGER_PID, "cacheEnabled", "True");
    } else {
      setConfigProperty(RESOURCE_DOWNLOAD_MANAGER_PID, "cacheEnabled", "False");
    }
  }

  public void setDownloadRetryDelayInSeconds(int delay) throws IOException {
    setConfigProperty(RESOURCE_DOWNLOAD_MANAGER_PID, "delayBetweenAttempts", delay);
  }

  public void setupMaxDownloadRetryAttempts(int maxRetryAttempts) throws IOException {
    setConfigProperty(RESOURCE_DOWNLOAD_MANAGER_PID, "maxRetryAttempts", maxRetryAttempts);
  }

  private void setConfigProperty(String pid, String propertyName, Object propertyValue)
      throws IOException {
    Map<String, Object> existingProperties = null;
    try {
      existingProperties =
          Optional.ofNullable(adminConfig.getAdminConsoleService().getProperties(pid))
              .orElse(new DictionaryMap<>());
    } catch (NotCompliantMBeanException e) {
      existingProperties = new DictionaryMap<>();
    }
    DictionaryMap<String, Object> updatedProperties = new DictionaryMap<>();
    updatedProperties.putAll(existingProperties);

    updatedProperties.put(propertyName, propertyValue);

    Configuration configuration = adminConfig.getConfiguration(RESOURCE_DOWNLOAD_MANAGER_PID, null);
    configuration.update(updatedProperties);
  }
}
