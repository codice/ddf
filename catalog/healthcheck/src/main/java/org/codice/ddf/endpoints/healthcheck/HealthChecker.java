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
package org.codice.ddf.endpoints.healthcheck;

import com.google.common.collect.ImmutableMap;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.SourceInfoRequestLocal;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.security.Subject;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.ws.rs.core.Response;
import org.apache.karaf.bundle.core.BundleInfo;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.bundle.core.BundleState;
import org.apache.shiro.subject.ExecutionException;
import org.codice.ddf.security.Security;
import org.opengis.filter.Filter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthChecker {

  private static final Logger LOGGER = LoggerFactory.getLogger(HealthChecker.class);

  private static final Map<Integer, String> BUNDLE_STATES =
      new ImmutableMap.Builder<Integer, String>()
          .put(Bundle.UNINSTALLED, "UNINSTALLED")
          .put(Bundle.INSTALLED, "INSTALLED")
          .put(Bundle.RESOLVED, "RESOLVED")
          .put(Bundle.STARTING, "STARTING")
          .put(Bundle.STOPPING, "STOPPING")
          .put(Bundle.ACTIVE, "ACTIVE")
          .build();

  private final CatalogFramework catalogFramework;
  private BundleContext context;
  private BundleService bundleService;
  private Security security;
  FilterBuilder filterBuilder;

  public HealthChecker(
      CatalogFramework catalogFramework,
      BundleContext context,
      BundleService bundleService,
      FilterBuilder filterBuilder,
      Security security) {
    this.catalogFramework = catalogFramework;
    this.context = context;
    this.bundleService = bundleService;
    this.filterBuilder = filterBuilder;
    this.security = security;
  }

  public Response handle() {
    if (catalogFramework != null
        && filterBuilder != null
        && context != null
        && bundleService != null) {
      if (isSolrQueryable() && areBundlesReady() && isCatalogAvailable()) {
        LOGGER.info("bundles ready, solr queryable, catalog available");
        return Response.ok().build();
      } else {
        LOGGER.info("Server NOT OK");
        return Response.serverError().build();
      }
    } else {
      LOGGER.info("Server NOT OK");
      return Response.serverError().build();
    }
  }

  private boolean areBundlesReady() {
    return Arrays.stream(context.getBundles()).allMatch(this::isBundleReady);
  }

  private boolean isBundleReady(Bundle bundle) {
    String name = bundle.getHeaders().get(Constants.BUNDLE_NAME);
    BundleInfo info = bundleService.getInfo(bundle);
    BundleState state = info.getState();

    boolean ready;
    if (info.isFragment()) {
      ready = BundleState.Resolved.equals(state);
    } else {
      if (BundleState.Failure.equals(state)) {
        printInactiveBundles();
      }
      ready = BundleState.Active.equals(state);
    }

    if (!ready) {
      LOGGER.debug("{} bundle not ready yet", name);
    }

    return ready;
  }

  public void printInactiveBundles() {
    printInactiveBundles(LOGGER::debug, LOGGER::debug);
  }

  private void printInactiveBundles(
      Consumer<String> headerConsumer, BiConsumer<String, Object[]> logConsumer) {
    headerConsumer.accept("Listing inactive bundles");

    for (Bundle bundle : context.getBundles()) {
      if (bundle.getState() != Bundle.ACTIVE) {
        Dictionary<String, String> headers = bundle.getHeaders();
        if (headers.get("Fragment-Host") != null) {
          continue;
        }

        StringBuilder headerString = new StringBuilder("[ ");
        Enumeration<String> keys = headers.keys();

        while (keys.hasMoreElements()) {
          String key = keys.nextElement();
          headerString.append(key).append("=").append(headers.get(key)).append(", ");
        }

        headerString.append(" ]");
        logConsumer.accept(
            "\n\tBundle: {}_v{} | {}\n\tHeaders: {}",
            new Object[] {
              bundle.getSymbolicName(),
              bundle.getVersion(),
              BUNDLE_STATES.getOrDefault(bundle.getState(), "UNKNOWN"),
              headerString
            });
      }
    }
  }

  private boolean isCatalogAvailable() {

    SourceInfoResponse response = null;
    try {
      response = catalogFramework.getSourceInfo(new SourceInfoRequestLocal(false));
    } catch (SourceUnavailableException e) {
      LOGGER.debug("Local source is unavailable", e);
      return false;
    }
    Set<SourceDescriptor> descriptors = response.getSourceInfo();
    if (descriptors != null) {
      SourceDescriptor localSource =
          descriptors.stream()
              .filter(Objects::nonNull)
              .filter(descriptor -> catalogFramework.getId().equals(descriptor.getSourceId()))
              .findAny()
              .orElse(null);
      if (localSource != null) {
        LOGGER.debug(
            "SourceID {}, isAvail {}", localSource.getSourceId(), localSource.isAvailable());
        LOGGER.info("Catalog is available: {}", localSource.isAvailable());
        return localSource.isAvailable();
      }
    }
    return false;
  }

  private boolean isSolrQueryable() {

    final QueryResponse queryResponse;
    try {
      final QueryImpl query = new QueryImpl(getFilter());
      query.setPageSize(1);
      query.setStartIndex(1);
      query.setTimeoutMillis(15000);

      Subject subject = security.runAsAdmin(security::getSystemSubject);
      Callable<QueryResponse> callable = () -> catalogFramework.query(new QueryRequestImpl(query));
      queryResponse = subject.execute(callable);

    } catch (ExecutionException e) {
      LOGGER.debug("Catalog unavailable", e);
      LOGGER.info("Solr not queryable");
      return false;
    }
    if (queryResponse != null) {

      Set<ProcessingDetails> details = queryResponse.getProcessingDetails();
      if (details != null && details.size() > 0) {
        for (ProcessingDetails detail : details) {

          if (detail != null && detail.hasException()) {
            LOGGER.debug("Catalog query unsuccessful", detail.getException());
            return false;
          }
        }
      }
      LOGGER.info("Solr queryable");
      return true;
    }
    LOGGER.info("Solr queryable");
    return false;
  }

  private Filter getFilter() {
    return filterBuilder.attribute(Metacard.TITLE).is().like().text("");
  }
}
