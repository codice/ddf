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
package org.codice.ddf.catalog.harvest;

import ddf.catalog.Constants;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.codice.ddf.security.common.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container class for managing the configuration of a Camel Route that allows content to be
 * automatically ingested when dropped into the specified monitored directory.
 */
public class ContentDirectoryMonitor {

  private static final Logger LOGGER = LoggerFactory.getLogger(ContentDirectoryMonitor.class);

  private static final Logger INGEST_LOGGER = LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

  private static final int MAX_THREAD_SIZE = 8;

  private static final int MIN_THREAD_SIZE = 1;

  private static final int MIN_READLOCK_INTERVAL_MILLISECONDS = 100;

  private static final Security SECURITY = Security.getInstance();

  private final Executor configurationExecutor;

  private final CamelContext camelContext;

  private String monitoredDirectory = null;

  private String processingMechanism = SourceHarvester.DELETE;

  private List<RouteDefinition> routeCollection;

  private List<String> badFiles;

  private List<String> badFileExtensions;

  private Map<String, Serializable> attributeOverrides;

  private Integer numThreads;

  private Integer readLockIntervalMilliseconds;

  Processor systemSubjectBinder = new SystemSubjectBinder();

  /**
   * Constructs a monitor for a specific directory that will ingest files into the Content
   * Framework.
   *
   * @param camelContext the Camel context to use across all Content Directory Monitors. Note that
   *     if Apache changes this ModelCamelContext interface there is no guarantee that whatever DM
   *     is being used (Blueprint in this case) will be updated accordingly.
   */
  public ContentDirectoryMonitor(CamelContext camelContext) {
    this(
        camelContext,
        Executors.newSingleThreadExecutor(
            StandardThreadFactoryBuilder.newThreadFactory("contentDirectoryMonitorThread")));
  }

  /**
   * Constructs a monitor that uses the given RetryPolicy while waiting for the content scheme, and
   * the given Executor to run the setup and Camel configuration.
   *
   * @param camelContext the Camel context to use across all Content directory monitors.
   * @param configurationExecutor the executor used to run configuration and updates.
   */
  public ContentDirectoryMonitor(CamelContext camelContext, Executor configurationExecutor) {
    this.camelContext = camelContext;
    this.configurationExecutor = configurationExecutor;
    setBlacklist();
  }

  private void setBlacklist() {
    String badFileProperty = System.getProperty("bad.files");
    String badFileExtensionProperty = System.getProperty("bad.file.extensions");
    badFiles =
        StringUtils.isNotEmpty(badFileProperty)
            ? Arrays.asList(badFileProperty.split("\\s*,\\s*"))
            : null;
    badFileExtensions =
        StringUtils.isNotEmpty(badFileExtensionProperty)
            ? Arrays.asList(badFileExtensionProperty.split(","))
            : null;
  }

  /**
   * Set the thread pool size with the given argument. If the given argument is less than 1,
   * numThreads is set to 1. If the given argument is greater than the MAX_THREAD_SIZE, numThreads
   * is set to MAX_THREAD_SIZE.
   *
   * @param numThreads - the specified size to make the thread pool
   */
  public void setNumThreads(Integer numThreads) {
    this.numThreads = Math.max(numThreads, MIN_THREAD_SIZE);
    this.numThreads = Math.min(this.numThreads, MAX_THREAD_SIZE);
  }

  public Integer getNumThreads() {
    return numThreads;
  }

  /**
   * Set the read lock interval for the Camel Route with the given argument. If the
   * readLockIntervalMilliseconds is less than 100, set it to 100.
   *
   * @param readLockIntervalMilliseconds
   */
  public void setReadLockIntervalMilliseconds(Integer readLockIntervalMilliseconds) {
    this.readLockIntervalMilliseconds =
        Math.max(readLockIntervalMilliseconds, MIN_READLOCK_INTERVAL_MILLISECONDS);
  }

  public Integer getReadLockIntervalMilliseconds() {
    return readLockIntervalMilliseconds;
  }

  /**
   * This method will stop and remove any existing Camel routes in this context, and then configure
   * a new Camel route using the properties set in the setter methods.
   *
   * <p>Invoked after all of the setter methods have been called (for initial route creation), and
   * also called whenever an existing route is updated.
   */
  public void init() {
    if (routeCollection != null) {
      try {
        // This stops the route before trying to remove it
        LOGGER.trace("Removing {} routes", routeCollection.size());
        camelContext.removeRouteDefinitions(routeCollection);
      } catch (Exception e) {
        LOGGER.debug("Unable to remove Camel routes from Content Directory Monitor", e);
      }
    } else {
      LOGGER.trace("No routes to remove before configuring a new route");
    }

    SECURITY.runAsAdmin(this::configure);
  }

  private Object configure() {
    Validate.notBlank(monitoredDirectory, "Property monitoredDirectory may not be empty");
    CompletableFuture.runAsync(this::attemptAddRoutes, configurationExecutor);
    return null;
  }

  /**
   * Invoked when a configuration is destroyed from the container.
   *
   * <p>Only remove routes that this Content Directory Monitor created since the same CamelContext
   * is shared across all Content Directory Monitors.
   */
  public void destroy(int code) {
    if (routeCollection == null) {
      return;
    }

    for (RouteDefinition routeDef : routeCollection) {
      try {
        String routeId = routeDef.getId();
        if (isMyRoute(routeId)) {
          LOGGER.trace("Stopping route with ID = {} and path {}", routeId, monitoredDirectory);
          camelContext.stopRoute(routeId);
          boolean status = camelContext.removeRoute(routeId);
          LOGGER.trace("Status of removing route {} is {}", routeId, status);
          camelContext.removeRouteDefinition(routeDef);
        }
      } catch (Exception e) {
        LOGGER.debug("Unable to stop Camel route with route ID = {}", routeDef.getId(), e);
      }
    }
  }

  /** @param monitoredDirectoryPath - directory path for the monitored directory */
  public void setMonitoredDirectoryPath(String monitoredDirectoryPath) {
    this.monitoredDirectory = monitoredDirectoryPath;
  }

  /** @param processingMechanism - what to do with the files after ingest */
  public void setProcessingMechanism(String processingMechanism) {
    this.processingMechanism = processingMechanism;
  }

  /** @param attributeOverrides - a list of attributes to override */
  public void setAttributeOverrides(List<String> attributeOverrides) {
    Map<String, Serializable> attributeOverrideMap = new HashMap<>();
    for (String s : attributeOverrides) {
      String[] keyValue = s.split("=", 2);

      if (keyValue.length < 2) {
        LOGGER.error("Invalid attribute override configured for monitored directory");
        throw new IllegalStateException(
            "Invalid attribute override configured for monitored directory");
      }

      attributeOverrideMap.computeIfAbsent(keyValue[0], k -> new ArrayList<String>());
      ArrayList valueList = (ArrayList) attributeOverrideMap.get(keyValue[0]);
      valueList.add(keyValue[1]);
    }
    this.attributeOverrides = attributeOverrideMap;
  }

  private String getBlackListAsRegex() {
    List<String> patterns = new ArrayList<>();

    if (CollectionUtils.isNotEmpty(badFileExtensions)) {
      patterns.addAll(badFileExtensions.stream().map(s -> (".*" + s)).collect(Collectors.toList()));
    }

    if (CollectionUtils.isNotEmpty(badFiles)) {
      patterns.addAll(badFiles);
    }

    if (CollectionUtils.isEmpty(patterns)) {
      return null;
    }

    return String.join("|", patterns);
  }

  public List<RouteDefinition> getRouteDefinitions() {
    return camelContext.getRouteDefinitions();
  }

  /*
     Task that waits for the "content" CamelComponent before adding the routes from the
     RouteBuilder to the CamelContext. This ensures content directory monitors will
     automatically start after a system shutdown.
  */
  private void attemptAddRoutes() {
    LOGGER.trace(
        "Attempting to add routes for content directory monitor watching {}", monitoredDirectory);
    try {
      RouteBuilder routeBuilder = createRouteBuilder();
      camelContext.addRoutes(routeBuilder);
      setRouteCollection(routeBuilder);
    } catch (Exception e) {
      LOGGER.info("Unable to configure Camel route.", e);
      INGEST_LOGGER.warn("Unable to configure Camel route.", e);
    }
  }

  /*
     Assign this content directory monitor's routeCollection to the routes generated by
     this content directory monitor's RouteBuilder.
  */
  private void setRouteCollection(RouteBuilder routeBuilder) {
    routeCollection = routeBuilder.getRouteCollection().getRoutes();
  }

  /*
     Does the given routeId belong to this content directory monitor's routeCollection?
  */
  private boolean isMyRoute(String routeId) {
    return this.routeCollection != null
        && this.routeCollection.stream().map(RouteDefinition::getId).anyMatch(routeId::equals);
  }

  private RouteBuilder createRouteBuilder() {
    return new RouteBuilder() {
      @Override
      public void configure() {
        StringBuilder stringBuilder = new StringBuilder();
        // Configure the camel route to ignore changing files (larger files that are in the process
        // of being copied)
        // Set the readLockTimeout to 2 * readLockIntervalMilliseconds
        // Set the readLockCheckInterval to check every readLockIntervalMilliseconds

        stringBuilder
            .append("file:")
            .append(monitoredDirectory)
            .append("?recursive=true")
            .append("&moveFailed=.errors")
            /* ReadLock Configuration */
            .append("&readLockMinLength=1")
            .append("&readLock=changed")
            .append("&readLockTimeout=")
            .append(2 * readLockIntervalMilliseconds)
            .append("&readLockCheckInterval=")
            .append(readLockIntervalMilliseconds);

        /* File Exclusions */
        String exclusions = getBlackListAsRegex();
        if (StringUtils.isNotBlank(exclusions)) {
          stringBuilder.append("&exclude=");
          stringBuilder.append(exclusions);
        }

        switch (processingMechanism) {
          case SourceHarvester.DELETE:
            stringBuilder.append("&delete=true");
            break;
          case SourceHarvester.MOVE:
            stringBuilder.append("&move=.ingested");
            break;
          default:
            throw new IllegalArgumentException(
                String.format("Received invalid processingMechanism [%s]", processingMechanism));
        }

        LOGGER.trace("ContentDirectoryMonitor inbox = {}", stringBuilder);

        RouteDefinition routeDefinition = from(stringBuilder.toString());

        if (attributeOverrides != null) {
          routeDefinition.setHeader(Constants.ATTRIBUTE_OVERRIDES_KEY).constant(attributeOverrides);
        }

        LOGGER.trace("About to process scheme content:framework");
        routeDefinition.threads(numThreads).process(systemSubjectBinder).to("content:framework");
      }
    };
  }

  public static class SystemSubjectBinder implements Processor {

    /**
     * Adds the system subject to the {@link ThreadContext} to allow proper authentication with the
     * catalog framework.
     *
     * @param exchange Camel {@link Exchange} object
     */
    @Override
    public void process(Exchange exchange) {
      ThreadContext.bind(SECURITY.getSystemSubject());
    }
  }
}
