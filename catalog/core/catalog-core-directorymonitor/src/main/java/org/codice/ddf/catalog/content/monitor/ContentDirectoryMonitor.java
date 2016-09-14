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
package org.codice.ddf.catalog.content.monitor;

import static org.codice.ddf.security.common.Security.runAsAdmin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.util.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.Constants;
import ddf.security.common.util.Security;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

/**
 * Container class for managing the configuration of a Camel Route that allows content to be
 * automatically ingested when dropped into the specified monitored directory.
 */
public class ContentDirectoryMonitor implements DirectoryMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentDirectoryMonitor.class);

    private String monitoredDirectory = null;

    private boolean copyIngestedFiles = false;

    private final CamelContext camelContext;

    private List<RouteDefinition> routeCollection;

    private List<String> attributeOverrides;

    private final int maxRetries;

    private final int delayBetweenRetries;

    private final Executor configurationExecutor;

    Processor systemSubjectBinder = new SystemSubjectBinder();

    /**
     * Constructs a monitor for a specific directory that will ingest files into
     * the Content Framework.
     *
     * @param camelContext the Camel context to use across all Content Directory
     *                     Monitors. Note that if Apache changes this ModelCamelContext interface there
     *                     is no guarantee that whatever DM is being used (Blueprint in this case) will be
     *                     updated accordingly.
     */
    public ContentDirectoryMonitor(CamelContext camelContext) {
        this(camelContext, 20, 5, Executors.newSingleThreadExecutor());
    }

    /**
     * Constructs a monitor that uses the given RetryPolicy while waiting for the content scheme,
     * and the given Executor to run the setup and Camel configuration.
     *
     * @param camelContext          the Camel context to use across all Content directory monitors.
     * @param maxRetries            Policy for polling the 'content' CamelComponent. Specifies, for any content
     *                              directory monitor, the number of times it will poll.
     * @param delayBetweenRetries   Policy for polling the 'content' CamelComponent. Specifies, for any content
     *                              directory monitor, the number of seconds it will wait between consecutive polls.
     * @param configurationExecutor the executor used to run configuration and updates.
     */
    public ContentDirectoryMonitor(CamelContext camelContext, int maxRetries,
            int delayBetweenRetries, Executor configurationExecutor) {
        this.camelContext = camelContext;
        this.maxRetries = maxRetries;
        this.delayBetweenRetries = delayBetweenRetries;
        this.configurationExecutor = configurationExecutor;
    }

    /**
     * This method will stop and remove any existing Camel routes in this context, and then
     * configure a new Camel route using the properties set in the setter methods.
     * <p>
     * Invoked after all of the setter methods have been called (for initial route creation), and
     * also called whenever an existing route is updated.
     */
    public void init() {
        if (routeCollection != null) {
            try {
                // This stops the route before trying to remove it
                LOGGER.debug("Removing {} routes", routeCollection.size());
                camelContext.removeRouteDefinitions(routeCollection);
            } catch (Exception e) {
                LOGGER.debug("Unable to remove Camel routes from Content Directory Monitor", e);
            }
        } else {
            LOGGER.debug("No routes to remove before configuring a new route");
        }

        runAsAdmin(this::configure);
    }

    private Object configure() {
        if (StringUtils.isEmpty(monitoredDirectory)) {
            LOGGER.warn("Cannot setup camel route - must specify a directory to be monitored");
            return null;
        }

        CompletableFuture.runAsync(this::attemptAddRoutes, configurationExecutor);
        return null;
    }

    /**
     * Invoked when a configuration is destroyed from the container.
     * <p>
     * Only remove routes that this Content Directory Monitor created since the same CamelContext
     * is shared across all Content Directory Monitors.
     */
    public void destroy(int code) {
        List<RouteDefinition> routeDefinitions = camelContext.getRouteDefinitions();
        for (RouteDefinition routeDef : routeDefinitions) {
            try {
                String routeId = routeDef.getId();
                if (isMyRoute(routeId)) {
                    LOGGER.debug("Stopping route with ID = {} and path {}",
                            routeId,
                            monitoredDirectory);
                    camelContext.stopRoute(routeId);
                    boolean status = camelContext.removeRoute(routeId);
                    LOGGER.debug("Status of removing route {} is {}", routeId, status);
                    camelContext.removeRouteDefinition(routeDef);
                }
            } catch (Exception e) {
                LOGGER.warn("Unable to stop Camel route with route ID = {}", routeDef.getId(), e);
            }
        }
    }

    /**
     * Invoked when updates are made to the configuration of existing directory monitors. This
     * method is invoked by the container as specified by the update-strategy and update-method
     * attributes in Blueprint XML file.
     *
     * @param properties - properties map for the configuration
     */
    public void updateCallback(Map<String, Object> properties) {
        if (properties != null) {
            setMonitoredDirectoryPath((String) properties.get("monitoredDirectoryPath"));
            setCopyIngestedFiles((Boolean) properties.get("copyIngestedFiles"));
            String[] parameterArray = (String[]) properties.get(Constants.ATTRIBUTE_OVERRIDES_KEY);
            if (parameterArray != null) {
                setAttributeOverrides(Arrays.asList(parameterArray));
            }
            init();
        }
    }

    /**
     * @param monitoredDirectoryPath - directory path for the monitored directory
     */
    public void setMonitoredDirectoryPath(String monitoredDirectoryPath) {
        this.monitoredDirectory = monitoredDirectoryPath;
    }

    /**
     * @param copyIngestedFiles - flag to copy ingested files
     */
    public void setCopyIngestedFiles(boolean copyIngestedFiles) {
        this.copyIngestedFiles = copyIngestedFiles;
    }

    /**
     * @param attributeOverrides - a list of attributes to override
     */
    public void setAttributeOverrides(List<String> attributeOverrides) {
        this.attributeOverrides = attributeOverrides;
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
        LOGGER.debug("Attempting to add routes for content directory monitor watching {}",
                monitoredDirectory);
        RouteBuilder routeBuilder = createRouteBuilder();
        try {
            verifyContentCamelComponentIsAvailable();
            camelContext.addRoutes(routeBuilder);
            setRouteCollection(routeBuilder);
        } catch (Exception e) {
            LOGGER.warn("{} {}",
                    "Unable to configure Camel route.",
                    "This content directory monitor will be unusable.");
        } finally {
            if (LOGGER.isDebugEnabled()) {
                dumpCamelContext("after attemptAddRoutes()");
            }
        }
    }

    /*
        Do not attempt to add routes to the CamelContext until we know the content scheme is ready.
     */
    private void verifyContentCamelComponentIsAvailable() {
        Failsafe.with(new RetryPolicy().retryWhen(null)
                .withMaxRetries(maxRetries)
                .withDelay(delayBetweenRetries, TimeUnit.SECONDS))
            .withFallback(() -> {
                throw new IllegalStateException("Could not get Camel component 'content'");
            })
            .get(() -> camelContext.getComponent("content"));
    }

    /*
        Assign this content directory monitor's routeCollection to the routes generated by
        this content directory monitor's RouteBuilder.
     */
    private void setRouteCollection(RouteBuilder routeBuilder) {
        routeCollection = routeBuilder.getRouteCollection()
                .getRoutes();
    }

    /*
        Does the given routeId belong to this content directory monitor's routeCollection?
     */
    private boolean isMyRoute(String routeId) {
        return this.routeCollection != null && this.routeCollection.stream()
                .map(RouteDefinition::getId)
                .anyMatch(routeId::equals);
    }

    private RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                String inbox = "file:" + monitoredDirectory + "?moveFailed=.errors";
                if (copyIngestedFiles) {
                    inbox += "&move=.ingested";
                } else {
                    inbox += "&delete=true";
                }
                LOGGER.debug("inbox = {}", inbox);

                RouteDefinition routeDefinition = from(inbox);

                if (attributeOverrides != null) {
                    String attributeOverrideString = attributeOverrides.stream()
                            .map(String::trim)
                            .collect(Collectors.joining(","));
                    routeDefinition.setHeader(Constants.ATTRIBUTE_OVERRIDES_KEY,
                            simple(attributeOverrideString));
                }

                LOGGER.trace("About to process scheme content:framework");
                routeDefinition.process(systemSubjectBinder)
                        .to("content:framework");
            }
        };
    }

    private void dumpCamelContext(String msg) {
        LOGGER.debug("\n\n***************  START: {}  *****************", msg);
        List<RouteDefinition> routeDefinitions = camelContext.getRouteDefinitions();
        if (routeDefinitions != null) {
            LOGGER.debug("Number of routes = {}", routeDefinitions.size());
            for (RouteDefinition routeDef : routeDefinitions) {
                String routeId = routeDef.getId();
                LOGGER.debug("route ID = {}", routeId);
                List<FromDefinition> routeInputs = routeDef.getInputs();
                if (routeInputs.isEmpty()) {
                    LOGGER.debug("routeInputs are EMPTY");
                } else {
                    for (FromDefinition fromDef : routeInputs) {
                        LOGGER.debug("route input's URI = {}", fromDef.getUri());
                    }
                }
                ServiceStatus routeStatus = camelContext.getRouteStatus(routeId);
                if (routeStatus != null) {
                    LOGGER.debug("Route ID {} is started = {}", routeId, routeStatus.isStarted());
                } else {
                    LOGGER.debug("routeStatus is NULL for routeId = {}", routeId);
                }
            }
        }
        LOGGER.debug("***************  END: {}  *****************\n\n", msg);
    }

    public static class SystemSubjectBinder implements Processor {

        /**
         * Adds the system subject to the {@link ThreadContext} to allow proper authentication
         * with the catalog framework.
         *
         * @param exchange Camel {@link Exchange} object
         */
        @Override
        public void process(Exchange exchange) {
            ThreadContext.bind(Security.getSystemSubject());
        }
    }
}
