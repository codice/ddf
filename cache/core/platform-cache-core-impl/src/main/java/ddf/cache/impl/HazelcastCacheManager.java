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
package ddf.cache.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapConfig.EvictionPolicy;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import ddf.cache.Cache;
import ddf.cache.CacheManager;

public class HazelcastCacheManager implements CacheManager {

    private static final String DATA_CACHE_CONFIGURATION_FILTER = "(service.factoryPid=ddf.cache.DataCache)";

    private static XLogger logger = new XLogger(LoggerFactory.getLogger(HazelcastCacheManager.class));

    private HazelcastInstance instance = null;

    private Configuration[] cacheConfigs = null;
    
    private static String XML_CONFIG_LOCATION = System.getProperty("karaf.home") + "/etc/hazelcast.xml";
    

    public HazelcastCacheManager() {
        this(XML_CONFIG_LOCATION);
    }
       
    public HazelcastCacheManager(String xmlConfigFilename) {    
        logger.info("ENTERING: CacheManagerImpl constructor ...");
        long startTime = System.nanoTime();
        
        System.setProperty("hazelcast.config", xmlConfigFilename);
        Config cfg = new XmlConfigBuilder().build();
        NetworkConfig networkConfig = cfg.getNetworkConfig();
        JoinConfig join = networkConfig.getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig().setEnabled(false);
        
        long estimatedTime = System.nanoTime() - startTime;
        logger.info("new Config() time = {} ms", estimatedTime/1000000);
        startTime = System.nanoTime();
        instance = Hazelcast.newHazelcastInstance(cfg);
        estimatedTime = System.nanoTime() - startTime;
        
        logger.info("newHazelcastInstance time = {} ms", estimatedTime/1000000);
        logger.info("EXITING: CacheManagerImpl constructor ...");
    }

    public HazelcastCacheManager(BundleContext context, ConfigurationAdmin configAdmin,
            String xmlConfigFilename) {
        Bundle bundle = context.getBundle();
        URL xmlConfigFileUrl = bundle.getResource(xmlConfigFilename);
        Config cfg = null;
        
        XmlConfigBuilder xmlConfigBuilder = null;
        try {
            xmlConfigBuilder = new XmlConfigBuilder(xmlConfigFileUrl.openStream());
            cfg = xmlConfigBuilder.build();
            logger.info("Successfully built hazelcast config from XML config file {}", xmlConfigFilename);
        } catch(FileNotFoundException e) {
            logger.info("FileNotFoundException trying to build hazelcast config from XML file " + xmlConfigFilename, e);
            cfg = null;
        } catch(IOException e) {
            logger.info("IOException trying to build hazelcast config from XML file " + xmlConfigFilename, e);
            cfg = null;
        }
        
        if (cfg == null) {
            logger.info("Falling back to using generic Config for hazelcast");
            cfg = new Config();
        } else if (logger.isDebugEnabled()) {
            MapConfig mapConfig = cfg.getMapConfig("persistentNotifications");
            if (mapConfig == null) {
                logger.debug("mapConfig is NULL for persistentNotifications - try persistent*");
                mapConfig = cfg.getMapConfig("persistent*");
                if (mapConfig == null) {
                    logger.debug("mapConfig is NULL for persistent*");
                }
            } else {
                MapStoreConfig mapStoreConfig = mapConfig.getMapStoreConfig();
                logger.debug("mapStoreConfig factoryClassName = {}", mapStoreConfig.getFactoryClassName());
            }
        }

        NetworkConfig networkConfig = cfg.getNetworkConfig();
        JoinConfig join = networkConfig.getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig().setEnabled(false);

        try {
            if (configAdmin != null) {
                try {
                    cacheConfigs = configAdmin.listConfigurations(DATA_CACHE_CONFIGURATION_FILTER);
                } catch (InvalidSyntaxException e) {
                    logger.error("Error in fetching data cache configuration: "
                            + e.getMessage());

                }
                logger.debug("data cache config obtained: ");
            }
            if (cacheConfigs == null) {
                logger.info("Data cache configuration is null");
            } else {
                String cacheName = null;
                int backupCount = 0;
                String evictionPolicy = null;
                int ttl = 0;
                int maxCacheSize = 0;

                for (int i = 0; i < cacheConfigs.length; ++i) {
                    Dictionary<String, Object> dict = cacheConfigs[i].getProperties();
                    Enumeration keys = dict.keys();

                    // Fetch Configuration for cache(s)
                    while (keys.hasMoreElements()) {
                        String key = (String) keys.nextElement();
                        Object value = dict.get(key);
                        logger.info(key + " : " + value);

                        if (key.equals(CONFIG_CACHE_NAME)) {
                            cacheName = (String) dict.get(key);
                        } else if (key.equals(CONFIG_BACKUP_COUNT)) {
                            backupCount = (Integer) dict.get(key);
                        } else if (key.equals(CONFIG_MAX_CACHE_SIZE)) {
                            maxCacheSize = (Integer) dict.get(key);
                        } else if (key.equals(CONFIG_TIME_TO_LIVE)) {
                            ttl = (Integer) dict.get(key);
                        } else if (key.equals(CONFIG_EVICTION_POLICY)) {
                            evictionPolicy = (String) dict.get(key);
                        }
                    }

                    // Setup Cache
                    MapConfig mapCfg = new MapConfig();
                    mapCfg.setName(cacheName);
                    mapCfg.setBackupCount(backupCount);
                    mapCfg.getMaxSizeConfig().setSize(maxCacheSize);
                    mapCfg.setTimeToLiveSeconds(ttl);

                    if (evictionPolicy.equals(EVICTION_POLICY_LRU)) {
                        mapCfg.setEvictionPolicy(EvictionPolicy.LRU);
                    } else if (evictionPolicy.equals(EVICTION_POLICY_LFU)) {
                        mapCfg.setEvictionPolicy(EvictionPolicy.LFU);
                    } else {
                        mapCfg.setEvictionPolicy(EvictionPolicy.NONE);
                    }
                    if (cfg == null) {
                        logger.info("Cache configuration is null");
                    }
                    cfg.addMapConfig(mapCfg);
                }
            }
        } catch (IOException ioe) {
            logger.warn("Unable to obtain the configuration admin");
        }
        instance = Hazelcast.newHazelcastInstance(cfg);
    }

    //@Override
    public void createCache(String name) {
        Map<Integer, String> newMap = instance.getMap(name);
    }
    
    public Cache getCache(String name) {
        return new HazelcastCache(name, (IMap<Object, Object>) instance.getMap(name));
    }
    
    //@Override
    public Cache createCache(String name, Map<String, Object> properties) {
        MapConfig mapConfig = getMapConfig(name, properties);
        Config config = instance.getConfig();
        config.addMapConfig(mapConfig);
        return new HazelcastCache(name, (IMap<Object, Object>) instance.getMap(name));
    }
    
    public Cache getCache(String name, Map<String, Object> properties) {
        //return new HazelcastCache(name, (IMap<Object, Object>) instance.getMap(name), properties);
        return createCache(name, properties);
    }
    
    @Override
    public Map<String, Object> getCacheConfiguration(String cacheName) {
        Map<String, Object> cacheConfigProps = new HashMap<String, Object>();
        
        MapConfig mapConfig = instance.getConfig().getMapConfig(cacheName);
        if (mapConfig != null) {
            cacheConfigProps.put(CONFIG_BACKUP_COUNT, mapConfig.getBackupCount());
            cacheConfigProps.put(CONFIG_MAX_CACHE_SIZE, mapConfig.getMaxSizeConfig().getSize());
            cacheConfigProps.put(CONFIG_TIME_TO_LIVE, mapConfig.getTimeToLiveSeconds());
            cacheConfigProps.put(CONFIG_EVICTION_POLICY, mapConfig.getEvictionPolicy());
            MapStoreConfig mapStoreConfig = mapConfig.getMapStoreConfig();
            cacheConfigProps.put(CONFIG_MAP_STORE, mapStoreConfig);
        }
        
        return cacheConfigProps;        
    }

    @Override
    public List<String> listCaches() {
        List<String> names = new ArrayList<String>();
        Collection<DistributedObject> dObjects = instance.getDistributedObjects();
        for (DistributedObject obj : dObjects) {
            names.add(obj.getName());
        }
        return names;
    }

    @Override
    public void removeCache(String cacheName) {
        instance.getMap(cacheName).destroy();
    }

    public void put(String cacheName, Object name, Object value) {
        instance.getMap(cacheName).put(name, value);
    }

    public void update(String cacheName, Object name, Object value) {

        instance.getMap(cacheName).replace(name, value);
    }

    public Object get(String cacheName, Object name) {
        return instance.getMap(cacheName).get(name);
    }

    public void remove(String cacheName, Object name) {
        instance.getMap(cacheName).remove(name);
    }

    public Map list(String cacheName) {
        return instance.getMap(cacheName);
    }

    @Override
    public void shutdown() {
        if (instance != null) {
            instance.shutdown();
        }
    }
    
    private MapConfig getMapConfig(String cacheName, Map<String, Object> properties) {
        MapConfig mapCfg = new MapConfig();
        mapCfg.setName(cacheName);
        int backupCount = 0;
        String evictionPolicy = null;
        int ttl = 0;
        int maxCacheSize = 0;
        
        if (properties != null) {
            for (String key : properties.keySet()) {
                Object value = properties.get(key);
                logger.info(key + " : " + value);
    
                if (key.equals(CONFIG_CACHE_NAME)) {
                    //cacheName = (String) dict.get(key);
                } else if (key.equals(CONFIG_BACKUP_COUNT)) {
                    backupCount = (Integer) value;
                } else if (key.equals(CONFIG_MAX_CACHE_SIZE)) {
                    maxCacheSize = (Integer) value;
                } else if (key.equals(CONFIG_TIME_TO_LIVE)) {
                    ttl = (Integer) value;
                } else if (key.equals(CONFIG_EVICTION_POLICY)) {
                    evictionPolicy = (String) value;
                }
    
                // Setup Cache
                //mapCfg.setName(cacheName);
                mapCfg.setBackupCount(backupCount);
                mapCfg.getMaxSizeConfig().setSize(maxCacheSize);
                mapCfg.setTimeToLiveSeconds(ttl);
    
                if (evictionPolicy == null) {
                    mapCfg.setEvictionPolicy(EvictionPolicy.NONE);
                } else if (evictionPolicy.equals(EVICTION_POLICY_LRU)) {
                    mapCfg.setEvictionPolicy(EvictionPolicy.LRU);
                } else if (evictionPolicy.equals(EVICTION_POLICY_LFU)) {
                    mapCfg.setEvictionPolicy(EvictionPolicy.LFU);
                }
            }
        }
        
        return mapCfg;
    }
}
