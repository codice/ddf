/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.content.core.directorymonitor;

import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ddf.content.operation.Request;

/**
 * @author rodgersh
 * 
 */
public class ContentDirectoryMonitor implements DirectoryMonitor {
    private String monitoredDirectory = null;

    private String directive = null;

    private boolean copyIngestedFiles = false;

    private ModelCamelContext camelContext;

    private List<RouteDefinition> routeCollection;

    private static final Logger LOGGER = Logger.getLogger(ContentDirectoryMonitor.class);

    
    /**
     * Constructs a monitor for a specific directory that will ingest files into
     * the Content Framework.
     * 
     * @param camelContext the Camel context to use across all Content Directory
     * Monitors. Note that if Apache changes this ModelCamelContext interface there
     * is no guarantee that whatever DM is being used (Spring in this case) will be
     * updated accordingly.
     */
    public ContentDirectoryMonitor(final ModelCamelContext camelContext) {
        this.camelContext = camelContext;
        LOGGER.trace("ContentDirectoryMonitor(CamelContext) constructor done");
    }

    /**
     * This method will stop and remove any existing Camel routes in this context, and then
     * configure a new Camel route using the properties set in the setter methods.
     * 
     * Invoked after all of the setter methods have been called (for initial route creation), and
     * also called whenever an existing route is updated.
     */
    public void init() {
        LOGGER.trace("INSIDE: init()");

        if (routeCollection != null) {
            try {
                // This stops the route before trying to remove it
                LOGGER.debug("Removing " + routeCollection.size() + " routes");
                camelContext.removeRouteDefinitions(routeCollection);
            } catch (Exception e) {
                LOGGER.warn(e.getMessage());
            }
        } else {
            LOGGER.debug("No routes to remove before configuring a new route");
        }

        configureCamelRoute();
    }

    /**
     * 
     */
    public void destroy() {
        LOGGER.trace("INSIDE: destroy()");        
        removeRoutes();
    }

    /**
     * Invoked when updates are made to the configuration of existing directory monitors. This
     * method is invoked by the container as specified by the update-strategy and update-method
     * attributes in Spring beans XML file.
     * 
     * @param properties
     */
    public void updateCallback(Map<String, Object> properties) {
        LOGGER.trace("ENTERING: updateCallback");

        if (properties != null) {
            setMonitoredDirectoryPath((String) properties.get("monitoredDirectoryPath"));
            setDirective((String) properties.get("directive"));
            setCopyIngestedFiles((Boolean) properties.get("copyIngestedFiles"));
            init();
        }

        LOGGER.trace("EXITING: updateCallback");
    }

    /**
     * @param monitoredDirectoryPath
     */
    public void setMonitoredDirectoryPath(String monitoredDirectoryPath) {
        LOGGER.trace("INSIDE: setMonitoredDirectoryPath");

        this.monitoredDirectory = monitoredDirectoryPath;
    }

    /**
     * @param directive
     */
    public void setDirective(String directive) {
        LOGGER.trace("INSIDE: setDirective");

        this.directive = directive;
    }

    /**
     * @param copyIngestedFiles
     */
    public void setCopyIngestedFiles(boolean copyIngestedFiles) {
        LOGGER.trace("INSIDE: setCopyIngestedFiles");

        this.copyIngestedFiles = copyIngestedFiles;
    }

    /**
     * 
     */
    private void configureCamelRoute() {
        LOGGER.trace("ENTERING: configureCamelRoute");

        // Must have a directory to be monitored to be able to configure the Camel route.
        if (StringUtils.isEmpty(monitoredDirectory)) {
            LOGGER.debug("Cannot setup camel route - must specify a directory to be monitored");
            return;
        }

        if (StringUtils.isEmpty(directive)) {
            LOGGER.debug("Cannot setup camel route - must specify a directive for the directory to be monitored");
            return;
        }

        RouteBuilder routeBuilder = new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                String inbox = "file:" + monitoredDirectory + "?moveFailed=.errors";
                if (copyIngestedFiles) {
                    inbox += "&move=.ingested";
                } else {
                    inbox += "&delete=true";
                }
                LOGGER.debug("inbox = " + inbox);

                from(inbox).setHeader(Request.OPERATION, constant("create"))
                        .setHeader(Request.DIRECTIVE, constant(directive))
                        .setHeader(Request.CONTENT_URI, constant("")).to("content:framework");
            }
        };

        try {
            // Add the routes that will be built by the RouteBuilder class above
            // to this CamelContext.
            // The addRoutes() method will instantiate the RouteBuilder class above,
            // and start the routes (only) if the camelContext has already been started.
            camelContext.addRoutes(routeBuilder);

            // Save the routes created by RouteBuilder so that they can be
            // stopped and removed later if the route(s) are modified by the
            // administrator or this ContentDirectoryMonitor is deleted.
            this.routeCollection = routeBuilder.getRouteCollection().getRoutes();

            // Start route that was just added.
            // If the route was just added for the first time, i.e., this not a bundle
            // restart, then this method will do nothing since the addRoutes() above
            // already started the route. But for bundle (or system) restart this call
            // is needed since the addRoutes() for whatever reason did not start the route.
            startRoutes();
            
            if (LOGGER.isDebugEnabled()) {
                dumpCamelContext("after configureCamelRoute()");
            }
        } catch (Exception e) {
            LOGGER.error("Unable to configure Camel route - this Content Directory Monitor will be unusable", e);
        }

        LOGGER.trace("EXITING: configureCamelRoute");
    }

    public List<RouteDefinition> getRouteDefinitions() {
        return camelContext.getRouteDefinitions();
    }
    
    private void startRoutes() {
        LOGGER.trace("ENTERING: startRoutes");
        List<RouteDefinition> routeDefinitions = camelContext.getRouteDefinitions();
        for (RouteDefinition routeDef : routeDefinitions) {
            startRoute(routeDef);
        }
        LOGGER.trace("EXITING: startRoutes");
    }
    
    private void startRoute(RouteDefinition routeDef) {
        String routeId = routeDef.getId();
        try {
            if (isMyRoute(routeId)) {
                ServiceStatus routeStatus = camelContext.getRouteStatus(routeId);
                // Only start the route if it is not already started
                if (routeStatus == null || !routeStatus.isStarted()) {
                    LOGGER.trace("Starting route with ID = " + routeId);
                    camelContext.startRoute(routeDef);  //DEPRECATED
                    // this method does not reliably start a route that was created, then
                    // app shutdown, and restarted
    //                camelContext.startRoute(routeId);  
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to start Camel route with route ID = " + routeId, e);
        }
    }
    
    private void removeRoutes() {
        LOGGER.trace("ENTERING: stopRoutes");
        List<RouteDefinition> routeDefinitions = camelContext.getRouteDefinitions();
        for (RouteDefinition routeDef : routeDefinitions) {
            try {
                // Only remove routes that this Content Directory Monitor created
                // (since same camelContext shared across all ContentDirectoryMonitors
                // this is necessary)
                if (isMyRoute(routeDef.getId())) {
                    LOGGER.trace("Stopping route with ID = " + routeDef.getId());
                    camelContext.stopRoute(routeDef);  //DEPRECATED
    //                    camelContext.stopRoute(routeDef.getId());
                    boolean status = camelContext.removeRoute(routeDef.getId());
                    LOGGER.trace("Status of removing route " + routeDef.getId() + " is " + status);
                    camelContext.removeRouteDefinition(routeDef);
                }
            } catch (Exception e) {
                LOGGER.warn("Unable to stop Camel route with route ID = " + routeDef.getId(), e);
            }
        }

        LOGGER.trace("EXITING: stopRoutes");
    }
    
    private boolean isMyRoute(String routeId) {
        
        boolean status = false;
        
        if (this.routeCollection != null) {
            for (RouteDefinition routeDef : this.routeCollection) {
                if (routeDef.getId().equals(routeId)) {
                    return true;
                }
            }
        }
        
        return status;
    }
    
    private void dumpCamelContext(String msg) {
        LOGGER.debug("\n\n***************  START: " + msg + "  *****************");
        List<RouteDefinition> routeDefinitions = camelContext.getRouteDefinitions();
        if (routeDefinitions != null) {
            LOGGER.debug("Number of routes = " + routeDefinitions.size());
            for (RouteDefinition routeDef : routeDefinitions) {
                String routeId = routeDef.getId();
                LOGGER.debug("route ID = " + routeId);
                List<FromDefinition> routeInputs = routeDef.getInputs();
                if (routeInputs.isEmpty()) {
                    LOGGER.debug("routeInputs are EMPTY");
                } else {
                    for (FromDefinition fromDef : routeInputs) {
                        LOGGER.debug("route input's URI = " + fromDef.getUri());
                    }
                }
                ServiceStatus routeStatus = camelContext.getRouteStatus(routeId);
                if (routeStatus != null) {
                    LOGGER.debug("Route ID " + routeId + " is started = " + routeStatus.isStarted());
                } else {
                    LOGGER.debug("routeStatus is NULL for routeId = " + routeId);
                }
            }
        }
        LOGGER.debug("***************  END: " + msg + "  *****************\n\n");
    }
}
