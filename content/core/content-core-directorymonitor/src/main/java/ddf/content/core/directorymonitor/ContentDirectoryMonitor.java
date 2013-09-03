/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.content.core.directorymonitor;

import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ddf.content.operation.Request;

/**
 * @author rodgersh
 *
 */
public class ContentDirectoryMonitor implements DirectoryMonitor
{
    private String monitoredDirectory = null;
    
    private String directive = null;
    
    private boolean copyIngestedFiles = false;
    
    private CamelContext camelContext;

    private List<RouteDefinition> routeCollection;
        
    private static final Logger LOGGER = Logger.getLogger(ContentDirectoryMonitor.class);
    
    
    /**
     * @param bundleContext
     */
    public ContentDirectoryMonitor(final CamelContext camelContext) 
    {
        this.camelContext = camelContext;
        LOGGER.trace("ContentDirectoryMonitor constructor done");
    }
    
    
    /**
     * This method will stop and remove any existing Camel routes in this context,
     * and then configure a new Camel route using the properties set in the
     * setter methods.
     * 
     * Invoked after all of the setter methods have been called (for initial
     * route creation), and also called whenever an existing route is updated.
     */
    public void init()
    {
        LOGGER.trace("INSIDE: init()");
        
        if (routeCollection != null)
        {
            try
            {
                // This stops the route before trying to remove it
                LOGGER.debug("Removing " + routeCollection.size() + " routes");
                camelContext.removeRouteDefinitions(routeCollection);
            }
            catch (Exception e)
            {
                LOGGER.warn(e.getMessage());
            }
        }
        else
        {
            LOGGER.debug("No routes to remove before configuring a new route");
        }
        
        configureCamelRoute();
    }
    
    
    /**
     * 
     */
    public void destroy()
    {
        LOGGER.trace("INSIDE: destroy()");
    }
    
        
    /**
     * Invoked when updates are made to the configuration of existing directory monitors.
     * This method is invoked by the container as specified by the update-strategy and
     * update-method attributes in Spring beans XML file.
     * 
     * @param properties
     */
    public void updateCallback(Map<String, Object> properties)
    {
        LOGGER.trace("ENTERING: updateCallback");
        
        if (properties != null)
        {
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
    public void setMonitoredDirectoryPath(String monitoredDirectoryPath)
    {
        LOGGER.trace("INSIDE: setMonitoredDirectoryPath");
        
        this.monitoredDirectory = monitoredDirectoryPath;
    }
    
    
    /**
     * @param directive
     */
    public void setDirective(String directive)
    {
        LOGGER.trace("INSIDE: setDirective");
        
        this.directive = directive;
    }
    
    
    /**
     * @param copyIngestedFiles
     */
    public void setCopyIngestedFiles(boolean copyIngestedFiles)
    {
        LOGGER.trace("INSIDE: setCopyIngestedFiles");
        
        this.copyIngestedFiles = copyIngestedFiles;
    }
    
    
    /**
     * 
     */
    private void configureCamelRoute()
    {
        LOGGER.trace("ENTERING: configureCamelRoute");
        
        // Must have a directory to be monitored to be able to configure the Camel route.
        if (StringUtils.isEmpty(monitoredDirectory))
        {
            LOGGER.debug("Cannot setup camel route - must specify a directory to be monitored");
            return;
        }
        
        if (StringUtils.isEmpty(directive))
        {
            LOGGER.debug("Cannot setup camel route - must specify a directive for the directory to be monitored");
            return;
        }
        
        RouteBuilder routeBuilder = new RouteBuilder() 
        {
            @Override
            public void configure() throws Exception 
            {
                String inbox = "file:" + monitoredDirectory + "?moveFailed=.errors";
                if (copyIngestedFiles)
                {
                    inbox += "&move=.ingested";
                }
                else
                {
                    inbox += "&delete=true";
                }
                LOGGER.debug("inbox = " + inbox);

                from(inbox)
                   .setHeader(Request.OPERATION, constant("create"))
                   .setHeader(Request.DIRECTIVE, constant(directive))
                   .setHeader(Request.CONTENT_URI, constant(""))
                   .to("content:framework");
            }
        };
                
        try
        {
            // Add the routes that will be built by the RouteBuilder class above
            // to this CamelContext.
            // The addRoutes() method will instantiate the RouteBuilder class above.
            camelContext.addRoutes(routeBuilder);
            camelContext.start();
            
            // Save the routes created by RouteBuilder so that they can be
            // stopped and removed later if the route(s) are modified by the 
            // administrator.
            this.routeCollection = routeBuilder.getRouteCollection().getRoutes();
        }
        catch (Exception e)
        {
            LOGGER.warn("Unable to configure Camel route", e);
        }
        
        LOGGER.trace("EXITING: configureCamelRoute");
    }
    
    
    public List<RouteDefinition> getRouteDefinitions()
    {
        return camelContext.getRouteDefinitions();
    }
}
