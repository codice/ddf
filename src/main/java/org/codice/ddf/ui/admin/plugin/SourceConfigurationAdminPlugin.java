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
package org.codice.ddf.ui.admin.plugin;

import java.util.ArrayList;
import java.util.Arrays;
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
import ddf.catalog.operation.SourceInfoRequestEnterprise;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;

/**
 * @author Scott Tustison
 */
public class SourceConfigurationAdminPlugin implements ConfigurationAdminPlugin {
    private final XLogger logger = new XLogger(
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
        Map<String, Object> statusMap = new HashMap<String, Object>();
        try {
            ServiceReference[] fedRefs = bundleContext.getAllServiceReferences(
                    FederatedSource.class.getCanonicalName(), null);
            ServiceReference[] provRefs = bundleContext.getAllServiceReferences(
                    CatalogProvider.class.getCanonicalName(), null);
            List<ServiceReference> refs = new ArrayList<ServiceReference>();
            if (fedRefs != null && fedRefs.length > 0) {
                refs.addAll(Arrays.asList(fedRefs));
            }
            if (provRefs != null && provRefs.length > 0) {
                refs.addAll(Arrays.asList(provRefs));
            }
            Set<SourceDescriptor> sources = null;
            if (catalogFramework != null) {
                SourceInfoResponse response = catalogFramework
                        .getSourceInfo(new SourceInfoRequestEnterprise(true));
                sources = response.getSourceInfo();
            }
            if (CollectionUtils.isNotEmpty(refs)) {
                for (ServiceReference ref : refs) {
                    Object superService = bundleContext.getService(ref);
                    if ((superService instanceof FederatedSource || superService instanceof CatalogProvider)
                            && superService instanceof ConfiguredService) {
                        ConfiguredService cs = (ConfiguredService) superService;

                        if (StringUtils.isNotEmpty(cs.getConfigurationPid())
                                && cs.getConfigurationPid().equals(configurationPid)) {
                            if (CollectionUtils.isNotEmpty(sources)) {
                                for (SourceDescriptor descriptor : sources) {
                                    if (superService instanceof FederatedSource
                                            && descriptor.getSourceId().equals(
                                                    ((FederatedSource) superService).getId())
                                            || superService instanceof CatalogProvider
                                            && descriptor.getSourceId().equals(
                                                    ((CatalogProvider) superService).getId())) {
                                        statusMap.put("available", descriptor.isAvailable());
                                        statusMap.put("sourceId", descriptor.getSourceId());
                                        return statusMap;
                                    }
                                }
                            } else {
                                // we don't want to call isAvailable because that can potentially
                                // block execution
                                // but if for some reason we have no catalog framework, just hit the
                                // source directly
                                if (superService instanceof FederatedSource) {
                                    statusMap.put("available",
                                            ((FederatedSource) superService).isAvailable());
                                    return statusMap;
                                } else if (superService instanceof CatalogProvider) {
                                    statusMap.put("available",
                                            ((CatalogProvider) superService).isAvailable());
                                    return statusMap;
                                }
                            }
                        }
                        // For now we're just going to assume that the metatype pid is the same as
                        // the class because we have no other way to
                        // match these things up. Obviously, this won't always be the case and this
                        // isn't necessarily a good way to do this.
                        // However, at the moment, there doesn't seem to be any other way to match
                        // up this metatype pid (which is what it ends
                        // up being in the case of a ManagedService) with the actual service that
                        // gets created. If, as in the solr case, the
                        // metatype pid is actually the fully qualified class name, then we can
                        // match them up this way.
                        else if (StringUtils.isEmpty(cs.getConfigurationPid())
                                && cs.getClass().getCanonicalName().equals(configurationPid)) {
                            // might be an unconfigured source so we'll make a best guess as to
                            // which source this
                            // configuration belongs to
                            if (CollectionUtils.isNotEmpty(sources)) {
                                for (SourceDescriptor descriptor : sources) {
                                    if (superService instanceof FederatedSource
                                            && descriptor.getSourceId().equals(
                                                    ((FederatedSource) superService).getId())
                                            || superService instanceof CatalogProvider
                                            && descriptor.getSourceId().equals(
                                                    ((CatalogProvider) superService).getId())) {
                                        statusMap.put("available", descriptor.isAvailable());
                                        statusMap.put("sourceId", descriptor.getSourceId());
                                        return statusMap;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (org.osgi.framework.InvalidSyntaxException e) {
            // this should never happen because the filter is always null
            logger.error("Error reading LDAP service filter", e);
        } catch (SourceUnavailableException e) {
            logger.error("Unable to retrieve sources from Catalog Framework", e);
        }
        return statusMap;
    }
}
