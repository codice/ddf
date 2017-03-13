/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.broker.routemanager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle that monitors for camel routes defined in xml files placed in <GDES_HOME>/etc/routes and
 * then loads them into the camelContext
 */
public class DynamicRouteDeployer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicRouteDeployer.class);

    private final String monitoredDirectory;

    private final CamelContext camelContext;

    private Map<Path, List<RouteDefinition>> processedFiles;

    public DynamicRouteDeployer(CamelContext camelContext, String monitoredDirectory) {
        this.camelContext = camelContext;
        this.monitoredDirectory = monitoredDirectory;
    }

    public void init() {
        processedFiles = new HashMap<>();

        configureRoute();
    }

    private void configureRoute() {
        try {
            RouteBuilder routeBuilder = new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("timer:simple?period=5000").to(
                            "bean:dynamicRouteDeployer?method=checkForAddedFiles")
                            .to("bean:dynamicRouteDeployer?method=checkForRemovedFiles")
                            .routeId("dynamicRouteDeployer");
                }
            };
            camelContext.addRoutes(routeBuilder);
            camelContext.startAllRoutes();
        } catch (Exception e) {
            LOGGER.error("Error starting camel route: {}", e);
        }
    }

    public void checkForAddedFiles() throws IOException {
        if (!Files.isDirectory(Paths.get(monitoredDirectory))) {
            return;
        }

        Set<Path> addedFiles = Files.list(Paths.get(monitoredDirectory))
                .filter(p -> p.getFileName()
                        .toString()
                        .endsWith(".xml"))
                .collect(Collectors.toSet());
        addedFiles.removeAll(processedFiles.keySet());

        addedFiles.forEach(p -> processFile(p, true));
    }

    public void checkForRemovedFiles() throws IOException {
        if (!Files.isDirectory(Paths.get(monitoredDirectory))) {
            return;
        }

        Set<Path> removedFiles = new HashSet<>(processedFiles.keySet());
        removedFiles.removeAll(Files.list(Paths.get(monitoredDirectory))
                .collect(Collectors.toSet()));

        removedFiles.forEach(p -> processFile(p, false));
    }

    private void processFile(Path path, boolean added) {
        if (added) {
            try (InputStream is = new FileInputStream(path.toFile())) {
                List<RouteDefinition> routeDefinitions;

                LOGGER.info("Loading route path: {}", path.getFileName());

                routeDefinitions = camelContext.loadRoutesDefinition(is)
                        .getRoutes();

                camelContext.addRouteDefinitions(routeDefinitions);
                processedFiles.put(path, routeDefinitions);
                camelContext.startAllRoutes();

            } catch (FileNotFoundException e) {
                LOGGER.warn("File {} not found. See debug log for stack trace.",
                        path.getFileName());
                LOGGER.debug(e.getMessage(), e);
            } catch (IOException e) {
                LOGGER.warn("IO Exception when accessing {}. See debug log for stack trace.",
                        path.getFileName());
                LOGGER.debug(e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.warn("Failed to read route definition. See debug log for stack trace.");
                LOGGER.debug(e.getMessage(), e);
            }
        } else {
            LOGGER.info("Removing route path: {}", path.getFileName());
            try {
                camelContext.removeRouteDefinitions(processedFiles.get(path));
            } catch (Exception e) {
                LOGGER.warn("Failed to remove routes definition. See debug log for stack trace.");
                LOGGER.debug(e.getMessage(), e);
            }
            processedFiles.remove(path);
        }
    }

    public void destroy() throws Exception {
        camelContext.stop();
    }
}
