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
package org.codice.ddf.catalog.admin.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.ui.admin.api.plugin.ConfigurationAdminPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.CatalogFramework;
import ddf.catalog.operation.impl.SourceInfoRequestEnterprise;
import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;

public class SourceConfigurationAdminPlugin implements ConfigurationAdminPlugin {
    private static final XLogger logger = new XLogger(
            LoggerFactory.getLogger(SourceConfigurationAdminPlugin.class));

    private CatalogFramework catalogFramework;

    public SourceConfigurationAdminPlugin() {

    }

    public void init() {
    }

    public void destroy() {
    }

    public CatalogFramework getCatalogFramework() {
        return catalogFramework;
    }

    public void setCatalogFramework(CatalogFramework catalogFramework) {
        this.catalogFramework = catalogFramework;
    }

    /**
     * Returns a map of configuration data that should be appended to the configurationDataMap
     * parameter. The configurationDataMap that is passed into this function is unmodifiable and is
     * passed in to simply expose what information already exists.
     * 
     * @param configurationPid
     *            service.pid for the ConfigurationAdmin configuration
     * @param configurationDataMap
     *            map of what properties have already been added to the configuration in question
     * @param bundleContext
     *            used to retrieve list of services
     * @return Map defining additional properties to add to the configuration
     */
    @Override
    public Map<String, Object> getConfigurationData(String configurationPid,
            Map<String, Object> configurationDataMap, BundleContext bundleContext) {

        logger.debug("Obtaining configuration data for the following configuration PID: {}", 
                     configurationPid);

        Map<String, Object> statusMap = new HashMap<String, Object>();
        try {
            List<ServiceReference<? extends Source>> refs = 
                    new ArrayList<ServiceReference<? extends Source>>();
            
            refs.addAll(bundleContext.getServiceReferences(FederatedSource.class, null));
            refs.addAll(bundleContext.getServiceReferences(CatalogProvider.class, null));
            
            Set<SourceDescriptor> sources = null;
            if (catalogFramework != null) {
                sources = catalogFramework.getSourceInfo(
                        new SourceInfoRequestEnterprise(true)).getSourceInfo();
            }
            boolean foundSources = CollectionUtils.isNotEmpty(sources);
            
            for (ServiceReference<? extends Source> ref : refs) {
                Source superService = bundleContext.getService(ref);
                
                if (superService instanceof ConfiguredService) {
                    ConfiguredService cs = (ConfiguredService) superService;

                    logger.debug("ConfiguredService configuration PID: {}", 
                                 cs.getConfigurationPid());
                    
                    boolean csConfigPidMatchesTargetPid = false;
                    if (StringUtils.isNotEmpty(cs.getConfigurationPid()) &&
                        cs.getConfigurationPid().equals(configurationPid)) {
                        csConfigPidMatchesTargetPid = true;
                    }
                    
                    if (foundSources) {
                        // If the configured service pid does not match, for now
                        // we're just going to assume that the metatype pid is 
                        // the same as the class because we have no other way to
                        // match these things up. Obviously, this won't always 
                        // be the case and this isn't necessarily a good way to 
                        // do this. However, at the moment, there doesn't seem 
                        // to be any other way to match up this metatype pid 
                        // (which is what it ends up being in the case of a 
                        // ManagedService) with the actual service that gets 
                        // created. If, as in the Solr Catalog Provider case, 
                        // the metatype pid is actually the fully qualified 
                        // class name, then we can match them up this way.
                        if (csConfigPidMatchesTargetPid ||
                            cs.getClass().getCanonicalName().equals(configurationPid)) {
                            
                            for (SourceDescriptor descriptor : sources) {
                                if (descriptor.getSourceId().equals(superService.getId())) {
                                    statusMap.put("available", descriptor.isAvailable());
                                    statusMap.put("sourceId", descriptor.getSourceId());
                                    return statusMap;
                                }
                            }
                        }
                    } else if (csConfigPidMatchesTargetPid) {
                        // we don't want to call isAvailable because that can 
                        // potentially block execution but if for some reason we
                        // have no catalog framework, just hit the source 
                        // directly
                        statusMap.put("available", superService.isAvailable());
                        return statusMap;
                    }
                }
            }
        } catch (org.osgi.framework.InvalidSyntaxException ise) {
            // this should never happen because the filter is always null
            logger.error("Error reading LDAP service filter", ise);
        } catch (SourceUnavailableException sue) {
            logger.error("Unable to retrieve sources from Catalog Framework", sue);
        }
        
        return statusMap;
    }
}
