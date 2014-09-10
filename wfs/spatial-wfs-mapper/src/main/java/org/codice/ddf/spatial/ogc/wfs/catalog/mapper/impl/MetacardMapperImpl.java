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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Maps Metacards to WFS Features. 
 *
 */
public class MetacardMapperImpl implements MetacardMapper {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardMapperImpl.class);
    
    private static final String METACARD_ATTR_TO_FEATURE_PROP_MAPPING_REGEX = "([^=,]+)=([^=,]+)";
    
    private static final Pattern METACARD_ATTR_TO_FEATURE_PROP_MAPPING_PATTERN = Pattern.compile(METACARD_ATTR_TO_FEATURE_PROP_MAPPING_REGEX);
    
    private static final String FEATURE_TYPE_REGEX = "\\{\\S+\\}\\S+";
    
    private static final Pattern FEATURE_TYPE_PATTERN = Pattern.compile(FEATURE_TYPE_REGEX);
    
    private static final String FACTORY_PID = "org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper";
    
    private static final String CONFIG_FILTER = "(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + FACTORY_PID + ")";
    
    private BundleContext bundleContext;
    
    private String featureType;
    
    Map<String, String> metacardAttributeToFeaturePropertyMap;
    
    Map<String, String> featurePropertyToMetacardAttributeMap;
    
    String sortByTemporalFeatureProperty;
    
    String sortByRelevanceFeatureProperty;
    
    String sortByDistanceFeatureProperty;
    
    boolean invalidFeatureType;
 
    
    public MetacardMapperImpl() {
        LOGGER.debug("Creating {}", MetacardMapperImpl.class.getName());
        metacardAttributeToFeaturePropertyMap = new HashMap<String, String>();
        featurePropertyToMetacardAttributeMap = new HashMap<String, String>();
        invalidFeatureType = false;
    }
    
    public void init() {
        LOGGER.debug("In init()");
        try {
            if (invalidFeatureType) {
                LOGGER.warn(
                        "Use of invalid Feature Type {} during {} configuration. Attempting to delete configuration.",
                        this.featureType, MetacardMapperImpl.class.getSimpleName());

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
    public String getFeatureProperty(String metacardAttribute) {
        return metacardAttributeToFeaturePropertyMap.get(metacardAttribute);
    }
    
    @Override
    public String getMetacardAttribute(String featureProperty) {
        return featurePropertyToMetacardAttributeMap.get(featureProperty);
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

        for (String singleMapping : metacardAttrToFeaturePropList) {
            // workaround for admin console bug (https://issues.apache.org/jira/browse/KARAF-1701)
            if (StringUtils.contains(singleMapping, ",")) {
                metacardAttributeToFeaturePropertyMap.putAll(workaround(singleMapping));
                continue;
            }
            addMetacardAttributeToFeaturePropertyMap(metacardAttributeToFeaturePropertyMap,
                    singleMapping);
        }

        this.metacardAttributeToFeaturePropertyMap = metacardAttributeToFeaturePropertyMap;

        LOGGER.debug("Metacard attribute to Feature property mapping is {}.",
                this.metacardAttributeToFeaturePropertyMap);
    }
    
    public Map<String, String> getMetacardAttributeToFeaturePropertyMap() {
        return this.metacardAttributeToFeaturePropertyMap;
    }
    
    public void setFeaturePropToMetacardAttrMap(String[] featurePropToMetacardAttrList) {
        for (String singleMapping : featurePropToMetacardAttrList) {
            // workaround for admin console bug (https://issues.apache.org/jira/browse/KARAF-1701)
            if (StringUtils.contains(singleMapping, ",")) {
                featurePropertyToMetacardAttributeMap.putAll(workaround(singleMapping));
                continue;
            }
            addMetacardAttributeToFeaturePropertyMap(featurePropertyToMetacardAttributeMap,
                    singleMapping);
        }

        LOGGER.debug("Feature attribute to metacard property mapping is {}.",
                featurePropertyToMetacardAttributeMap);         
    }
    
    public Map<String, String> getFeaturePropertyToMetacardAttributeMap() {
        return this.featurePropertyToMetacardAttributeMap;
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
                LOGGER.debug("Single mapping [{}].", singleMapping);
                addMetacardAttributeToFeaturePropertyMap(metacardAttributeToFeaturePropertyMap, singleMapping);
        }
        
        LOGGER.debug("Workaround complete.");
        
        return metacardAttributeToFeaturePropertyMap;
    }
    
    private void addMetacardAttributeToFeaturePropertyMap(
            Map<String, String> metacardAttributeToFeaturePropertyMap, String mapping) {
        Matcher matcher = METACARD_ATTR_TO_FEATURE_PROP_MAPPING_PATTERN.matcher(mapping);
        if (matcher.matches()) {
            LOGGER.debug("{} matches pattern {}", mapping,
                    METACARD_ATTR_TO_FEATURE_PROP_MAPPING_PATTERN);
            String metacardAttribute = StringUtils.trim(matcher.group(1));
            String featureProperty = StringUtils.trim(matcher.group(2));
            if (metacardAttribute != null && featureProperty != null) {
                LOGGER.debug("Adding Metacard attribute to Feature property mapping [{}={}].",
                        metacardAttribute, featureProperty);
                metacardAttributeToFeaturePropertyMap.put(metacardAttribute, featureProperty);
            } else {
                LOGGER.debug(
                        "Unable to add mapping.  Received null value for Metacard attribute or Feature property.  metacardAttribute: {}; featureProperty: {}",
                        metacardAttribute, featureProperty);
            }
        } else {
            LOGGER.debug("Invalid mapping pattern. {} does not match pattern {}", mapping,
                    METACARD_ATTR_TO_FEATURE_PROP_MAPPING_PATTERN);
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
        if(configuration != null) {
            configuration.delete();
            LOGGER.info("Configuration with feature type of {} has been deleted.", this.featureType);
        } else {
            LOGGER.info("Unable to delete configuration with feature type {}.", this.featureType);
        }
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

        Configuration configuration = null;
        
        if (configurations != null) {
            LOGGER.debug("configurations length: {}.", configurations.length);

            for (Configuration config : configurations) {
                Dictionary<String, Object> dictionary = config.getProperties();
                String featureType = (String) dictionary.get("featureType");
                if (StringUtils.equals(featureType, this.featureType)) {
                    LOGGER.debug("Found featureType of {}", this.featureType);
                    configuration = config;
                    break;
                }
            }
        }
        
        return configuration;
    }
 
}
