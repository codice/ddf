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
package org.codice.ddf.spatial.ogc.wfs.catalog.mapper.impl;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardAttributeMapper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Result;

/**
 *  Maps Metacard attributes to Feature properties. 
 *
 */
public class MetacardAttributeMapperImpl implements MetacardAttributeMapper {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardAttributeMapperImpl.class);
    
    private static final String METACARD_ATTR_TO_FEATURE_PROP_MAPPING_REGEX = "([^=,]+)=([^=,]+)";
    
    private static final Pattern METACARD_ATTR_TO_FEATURE_PROP_MAPPING_PATTERN = Pattern.compile(METACARD_ATTR_TO_FEATURE_PROP_MAPPING_REGEX);
    
    private static final String FEATURE_TYPE_REGEX = "\\{\\S+\\}\\S+";
    
    private static final Pattern FEATURE_TYPE_PATTERN = Pattern.compile(FEATURE_TYPE_REGEX);
    
    private static final String FACTORY_PID = "org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardAttributeMapper";
    
    private static final String CONFIG_FILTER = "(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + FACTORY_PID + ")";
    
    private BundleContext bundleContext;
    
    private String featureType;
    
    Map<String, String> metacardAttributeToFeaturePropertyMap;
    
    String sortByTemporalFeatureProperty;
    
    String sortByRelevanceFeatureProperty;
    
    String sortByDistanceFeatureProperty;
    
    boolean invalidFeatureType;
 
    
    public MetacardAttributeMapperImpl() {
        LOGGER.debug("Creating {}", MetacardAttributeMapperImpl.class.getName());
        metacardAttributeToFeaturePropertyMap = new HashMap<String, String>();
        invalidFeatureType = false;
    }
    
    public void init() {
        LOGGER.debug("In init()");
        try {
            if (invalidFeatureType) {
                LOGGER.warn(
                        "Use of invalid Feature Type {} during {} configuration. Attempting to delete configuration.",
                        this.featureType, MetacardAttributeMapperImpl.class.getSimpleName());

                deleteConfiguration();

            }
        } catch (IOException e) {
            LOGGER.error("Unable to delete configuration. " + e.getMessage());
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Unable to delete configuration. " + e.getMessage());
        } finally {
            invalidFeatureType = false;
        }
    }

    @Override
    public String getFeaturePropertyForMetacardAttribute(String metacardAttribute) {
        return metacardAttributeToFeaturePropertyMap.get(metacardAttribute);
    }
    
    public void setContext(BundleContext context) {
        LOGGER.debug("Setting bundle context");
        this.bundleContext = context;
    }
    
    public void setFeatureType(String featureType) {
        this.featureType = featureType;
        
        if (!isValidFeatureType(featureType)) {
            LOGGER.error("Invalid FeatureType pattern. {} does not match pattern {}", featureType, FEATURE_TYPE_REGEX);
        }
    }
    
    public String getFeatureType() {
        return this.featureType;
    }
    
    public void setMetacardAttrToFeaturePropMap(String[] metacardAttrToFeaturePropList) {
        Map<String, String> metacardAttributeToFeaturePropertyMap = new HashMap<String, String>();
        
        for(String singleMapping : metacardAttrToFeaturePropList) {
            // workaround for admin console bug (https://issues.apache.org/jira/browse/KARAF-1701)
            if(StringUtils.contains(singleMapping, ",")) {
                metacardAttributeToFeaturePropertyMap.putAll(workaround(singleMapping));
                continue;
            }
            
            if (isValidMapping(singleMapping)) {
                String metacardAttribute = getMetacardAttribute(singleMapping);
                String featureProperty = getFeatureProperty(singleMapping);
                LOGGER.debug("Adding Metacard attribute to Feature property mapping [{}={}].",
                        metacardAttribute, featureProperty);
                metacardAttributeToFeaturePropertyMap.put(metacardAttribute, featureProperty);
            } else {
                LOGGER.error("Invalid mapping pattern. {} does not match pattern {}", singleMapping, METACARD_ATTR_TO_FEATURE_PROP_MAPPING_PATTERN);
            }
        }
        
        this.metacardAttributeToFeaturePropertyMap = metacardAttributeToFeaturePropertyMap;
        
        LOGGER.debug("Metacard attribute to Feature property mapping is {}.", this.metacardAttributeToFeaturePropertyMap);
    }
    
    public Map<String, String> getMetacardAttributeToFeaturePropertyMap() {
        return this.metacardAttributeToFeaturePropertyMap;
    }
    
    public void setSortByTemporalFeatureProperty(String temporalFeatureProperty) {
        LOGGER.debug("Setting sortByTemporalFeatureProperty to: {}", temporalFeatureProperty);
        this.sortByTemporalFeatureProperty = temporalFeatureProperty;
    }
    
    @Override
    public String getSortByTemporalFeatureProperty() {
        return this.sortByTemporalFeatureProperty;
    }
    
    public void setSortByRelevanceFeatureProperty(String relevanceFeatureProperty) {
        LOGGER.debug("Setting sortByRelevanceFeatureProperty to: {}", relevanceFeatureProperty);
        this.sortByRelevanceFeatureProperty = relevanceFeatureProperty;
    }
    
    @Override
    public String getSortByRelevanceFeatureProperty() {
        return this.sortByRelevanceFeatureProperty;
    }
    
    public void setSortByDistanceFeatureProperty(String distanceFeatureProperty) {
        LOGGER.debug("Setting sortByDistanceFeatureProperty to: {}", distanceFeatureProperty);
        this.sortByDistanceFeatureProperty = distanceFeatureProperty;
    }
    
    @Override
    public String getSortByDistanceFeatureProperty() {
        return this.sortByDistanceFeatureProperty;
    }
    
    /**
     * Workaround for admin console bug (https://issues.apache.org/jira/browse/KARAF-1701)
     * 
     */
    private Map<String, String> workaround(String metacardAttrToFeaturePropList) {
        LOGGER.debug("Performing workaround on mapping [{}] due to KARAF bug (https://issues.apache.org/jira/browse/KARAF-1701).", metacardAttrToFeaturePropList);
        
        Map<String, String> metacardAttributeToFeaturePropertyMap = new HashMap<String, String>();
        
        for (String singleMapping : StringUtils.split(metacardAttrToFeaturePropList, ",")) {
            if (isValidMapping(singleMapping)) {
                LOGGER.debug("Single mapping [{}].", singleMapping);
                String metacardAttribute = getMetacardAttribute(singleMapping);
                String featureProperty = getFeatureProperty(singleMapping);
                LOGGER.debug("Adding Metacard attribute to Feature property mapping [{}={}].",
                        metacardAttribute, featureProperty);
                metacardAttributeToFeaturePropertyMap.put(metacardAttribute, featureProperty);
            } else {
                LOGGER.error("Invalid mapping pattern. {} does not match pattern {}", singleMapping, METACARD_ATTR_TO_FEATURE_PROP_MAPPING_PATTERN);
            }
        }
        
        LOGGER.debug("Workaround complete.");
        
        return metacardAttributeToFeaturePropertyMap;
    }
    
    private String getMetacardAttribute(String mapping) {
        String metacardAttribute = StringUtils.trim(StringUtils.split(mapping, "=")[0]);
        LOGGER.debug("metacardAttribute [{}]", metacardAttribute);
        return metacardAttribute;
    }
    
    private String getFeatureProperty(String mapping) {
        String featureProperty = StringUtils.trim(StringUtils.split(mapping, "=")[1]);
        LOGGER.debug("featureProperty [{}]", featureProperty);
        return featureProperty;
    }
    
    private boolean isValidMapping(String mapping) {
        if (METACARD_ATTR_TO_FEATURE_PROP_MAPPING_PATTERN.matcher(mapping).matches()) {
            LOGGER.debug("{} matches pattern {}", mapping,
                    METACARD_ATTR_TO_FEATURE_PROP_MAPPING_PATTERN);
            return true;
        } else {
            return false;
        }
    }
    
    private boolean isValidFeatureType(String featureType) {
        if (FEATURE_TYPE_PATTERN.matcher(featureType).matches()) {
            LOGGER.debug("{} matches pattern {}", featureType, FEATURE_TYPE_REGEX);
            return true;
        } else {
            invalidFeatureType = true;
            return false;
        }
    }
    
    private void deleteConfiguration() throws IOException, InvalidSyntaxException {
        LOGGER.info("Attempting to delete configuration with feature type of {}.", this.featureType);

        Configuration configuration = getConfiguration();
        configuration.delete();

        LOGGER.info("Configuration with feature type of {} has been deleted.", this.featureType);
    }
    
    private Configuration getConfiguration() throws IOException, InvalidSyntaxException {
        Configuration[] configurations = null;
        ConfigurationAdmin configurationAdmin = null;
        ServiceReference<?> configurationAdminReference = bundleContext
                .getServiceReference(ConfigurationAdmin.class.getName());
        if (configurationAdminReference != null) {
            configurationAdmin = (ConfigurationAdmin) bundleContext
                    .getService(configurationAdminReference);

            configurations = configurationAdmin.listConfigurations(CONFIG_FILTER);
        }

        if (configurations != null) {
            LOGGER.debug("configurations length: {}.", configurations.length);
        }

        Configuration configuration = null;

        for (Configuration config : configurations) {
            Dictionary<String, Object> dictionary = config.getProperties();
            String featureType = (String) dictionary.get("featureType");
            if (StringUtils.equals(featureType, this.featureType)) {
                LOGGER.debug("Found featureType of {}", this.featureType);
                configuration = config;
                break;
            }
        }
        
        return configuration;
    }
 
}
