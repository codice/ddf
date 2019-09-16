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

import static org.awaitility.Awaitility.await;

import ddf.catalog.CatalogFramework;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.impl.SourceInfoRequestEnterprise;
import ddf.catalog.operation.impl.SourceInfoRequestLocal;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.SourceUnavailableException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  private static final long CATALOG_PROVIDER_TIMEOUT =
      AbstractIntegrationTest.GENERIC_TIMEOUT_MILLISECONDS;

  private static final String CATALOG_FRAMEWORK_PID = "ddf.catalog.CatalogFrameworkImpl";

  private static final String RESOURCE_DOWNLOAD_MANAGER_PID =
      "ddf.catalog.resource.download.ReliableResourceDownloadManager";

  private final ServiceManager serviceManager;

  private final AdminConfig adminConfig;

  CatalogBundle(ServiceManager serviceManager, AdminConfig adminConfig) {
    this.serviceManager = serviceManager;
    this.adminConfig = adminConfig;
  }

  public void waitForCatalogProvider() throws InterruptedException {
    LOGGER.info("Waiting for CatalogProvider to become available.");
    serviceManager.printInactiveBundlesInfo();

    await("Waiting for CatalogProvider to become available.")
        .atMost(CATALOG_PROVIDER_TIMEOUT, TimeUnit.MILLISECONDS)
        .pollDelay(1, TimeUnit.SECONDS)
        .until(this::isCatalogProviderReady);

    LOGGER.info("CatalogProvider is available.");
  }

  private boolean isCatalogProviderReady() {
    CatalogProvider provider = getService(CatalogProvider.class);
    CatalogFramework framework = getService(CatalogFramework.class);

    if (framework != null && provider != null) {
      SourceInfoRequestLocal sourceInfoRequestEnterprise = new SourceInfoRequestLocal(true);

      try {
        SourceInfoResponse sources = framework.getSourceInfo(sourceInfoRequestEnterprise);

        return sources
            .getSourceInfo()
            .stream()
            .filter(descriptor -> descriptor.getSourceId().endsWith(provider.getId()))
            .map(descriptor -> descriptor.isAvailable() && provider.isAvailable())
            .findFirst()
            .orElse(false);
      } catch (SourceUnavailableException ignored) {
      }
    }

    return false;
  }

  public void waitForFederatedSource(String id) throws InvalidSyntaxException {
    LOGGER.info("Waiting for FederatedSource {} to become available.", id);
    await()
        .atMost(CATALOG_PROVIDER_TIMEOUT, TimeUnit.MILLISECONDS)
        .pollDelay(1, TimeUnit.SECONDS)
        .until(() -> isFederatedSourceReady(id));
  }

  private boolean isFederatedSourceReady(String id) throws InvalidSyntaxException {
    CatalogFramework framework = getService(CatalogFramework.class);
    FederatedSource source =
        serviceManager
            .getServiceReferences(FederatedSource.class, null)
            .stream()
            .map(serviceManager::getService)
            .filter(src -> id.equals(src.getId()))
            .findFirst()
            .orElse(null);

    if (source != null && framework != null) {
      SourceInfoRequestEnterprise request = new SourceInfoRequestEnterprise(true);

      try {
        SourceInfoResponse sources = framework.getSourceInfo(request);
        return sources
            .getSourceInfo()
            .stream()
            .filter(descriptor -> descriptor.getSourceId().equals(source.getId()))
            .map(descriptor -> descriptor.isAvailable() && source.isAvailable())
            .findFirst()
            .orElse(false);
      } catch (SourceUnavailableException ignore) {
      }
    }

    return false;
  }

  public void setFanout(boolean fanoutEnabled) throws IOException {
    Map<String, Object> properties;
    try {
      properties =
          Optional.ofNullable(
                  adminConfig.getAdminConsoleService().getProperties(CATALOG_FRAMEWORK_PID))
              .orElse(new DictionaryMap<>());
    } catch (NotCompliantMBeanException e) {
      properties = new DictionaryMap<>();
    }

    properties.put("fanoutEnabled", Boolean.toString(fanoutEnabled));
    serviceManager.startManagedService(CATALOG_FRAMEWORK_PID, properties);
  }

  public void setFanoutTagBlacklist(List<String> blacklist)
      throws IOException, InterruptedException {
    Map<String, Object> properties;
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
      setResourceDownloadProperty("cacheEnabled", "True");
    } else {
      setResourceDownloadProperty("cacheEnabled", "False");
    }
  }

  public void setDownloadRetryDelayInSeconds(int delay) throws IOException {
    setResourceDownloadProperty("delayBetweenAttempts", delay);
  }

  public void setupMaxDownloadRetryAttempts(int maxRetryAttempts) throws IOException {
    setResourceDownloadProperty("maxRetryAttempts", maxRetryAttempts);
  }

  private void setResourceDownloadProperty(String propertyName, Object propertyValue)
      throws IOException {
    Map<String, Object> existingProperties;
    try {
      existingProperties =
          Optional.ofNullable(
                  adminConfig
                      .getAdminConsoleService()
                      .getProperties(CatalogBundle.RESOURCE_DOWNLOAD_MANAGER_PID))
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

  private <T> T getService(Class<T> c) {
    ServiceReference<T> ref = serviceManager.getServiceReference(c);

    if (ref != null) {
      return serviceManager.getService(ref);
    }

    return null;
  }
}
