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
package org.codice.ddf.broker.route.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.camel.CamelContext;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.model.RouteDefinition;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle that monitors for camel routes defined in xml files placed in <DDF_HOME>/etc/routes and
 * then loads them into the camelContext
 */
public class DynamicRouteDeployer implements ArtifactInstaller {
  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicRouteDeployer.class);

  /**
   * This map should be static in order to maintain a reference to the existing map after a bundle
   * restart. This is because init() is called and not install() to process the RouteDefinitions, so
   * we need to make the map static so that it survives the instance being destroyed and recreated
   * during a restart.
   */
  private static Map<File, List<RouteDefinition>> processedFiles = new ConcurrentHashMap<>();

  private final CamelContext camelContext;

  public DynamicRouteDeployer(CamelContext camelContext) {
    this.camelContext = camelContext;
  }

  public void init() {
    processedFiles.values().forEach(this::addRouteDefinitions);
  }

  public void destroy() throws Exception {
    camelContext.stop();
  }

  @Override
  public void install(File file) throws Exception {
    try (InputStream is = new FileInputStream(file)) {
      List<RouteDefinition> routeDefinitions;
      LOGGER.info("Loading route path: {}", file.getName());

      routeDefinitions = camelContext.loadRoutesDefinition(is).getRoutes();
      camelContext.addRouteDefinitions(routeDefinitions);

      processedFiles.put(file, routeDefinitions);
      camelContext.startAllRoutes();
    } catch (Exception e) {
      LOGGER.warn("Failed to read route definition. See debug log for stack trace.");
      LOGGER.debug(e.getMessage(), e);
    }
  }

  @Override
  public void update(File file) throws Exception {
    uninstall(file);
    install(file);
  }

  @Override
  public void uninstall(File file) throws Exception {
    LOGGER.info("Removing route path: {}", file.getName());
    try {
      camelContext.removeRouteDefinitions(processedFiles.get(file));
    } catch (Exception e) {
      LOGGER.warn("Failed to remove routes definition. See debug log for stack trace.");
      LOGGER.debug(e.getMessage(), e);
    }
    processedFiles.remove(file);
  }

  @Override
  public boolean canHandle(File file) {
    return file.getName().endsWith(".xml");
  }

  private void addRouteDefinitions(List<RouteDefinition> routeDefinitions) {
    try {
      camelContext.addRouteDefinitions(routeDefinitions);
    } catch (Exception e) {
      throw new RuntimeCamelException(e);
    }
  }
}
