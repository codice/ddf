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
package org.codice.ddf.notifications.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.notifications.store.NotificationStore;
import org.codice.ddf.notifications.store.PersistentNotification;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.SqlPredicate;

/**
 * Stores (persists) the notifications for *all* users in a single persistent notifications cache to
 * disk. The default location for the persisted notification is
 * "<INSTALL_DIR>/data/persistentNotifications". Notifications are stored as a HashMap of attributes
 * vs. a Notification Java object. The object persisted must be Serializable.
 * 
 */
public class HazelcastNotificationStore implements NotificationStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(HazelcastNotificationStore.class);

    public static final String NOTIFICATION_CACHE_NAME = "persistentNotifications";

    private HazelcastInstance instance;

    private IMap<Object, Object> notificationsCache;

    public HazelcastNotificationStore(BundleContext context, String xmlConfigFilename) {
        LOGGER.info("Creating {} cache", NOTIFICATION_CACHE_NAME);
        if (this.instance == null) {
            Config cfg = getHazelcastConfig(context, xmlConfigFilename);
            cfg.setClassLoader(getClass().getClassLoader());
            NetworkConfig networkConfig = cfg.getNetworkConfig();
            JoinConfig join = networkConfig.getJoin();
            join.getMulticastConfig().setEnabled(false);
            join.getTcpIpConfig().setEnabled(false);
            this.instance = Hazelcast.newHazelcastInstance(cfg);
        }

        notificationsCache = this.instance.getMap(NOTIFICATION_CACHE_NAME);
    }

    private Config getHazelcastConfig(BundleContext context, String xmlConfigFilename) {
        Config cfg = null;
        Bundle bundle = context.getBundle();

        URL xmlConfigFileUrl = null;
        if (StringUtils.isNotBlank(xmlConfigFilename)) {
            xmlConfigFileUrl = bundle.getResource(xmlConfigFilename);
        }

        XmlConfigBuilder xmlConfigBuilder = null;

        if (xmlConfigFileUrl != null) {
            try {
                xmlConfigBuilder = new XmlConfigBuilder(xmlConfigFileUrl.openStream());
                cfg = xmlConfigBuilder.build();
                LOGGER.info("Successfully built hazelcast config from XML config file {}",
                        xmlConfigFilename);
            } catch (FileNotFoundException e) {
                LOGGER.info("FileNotFoundException trying to build hazelcast config from XML file "
                        + xmlConfigFilename, e);
                cfg = null;
            } catch (IOException e) {
                LOGGER.info("IOException trying to build hazelcast config from XML file "
                        + xmlConfigFilename, e);
                cfg = null;
            }
        }

        if (cfg == null) {
            LOGGER.info("Falling back to using generic Config for hazelcast");
            cfg = new Config();
        } else if (LOGGER.isDebugEnabled()) {
            MapConfig mapConfig = cfg.getMapConfig("persistentNotifications");
            if (mapConfig == null) {
                LOGGER.debug("mapConfig is NULL for persistentNotifications - try persistent*");
                mapConfig = cfg.getMapConfig("persistent*");
                if (mapConfig == null) {
                    LOGGER.debug("mapConfig is NULL for persistent*");
                }
            } else {
                MapStoreConfig mapStoreConfig = mapConfig.getMapStoreConfig();
                LOGGER.debug("mapStoreConfig factoryClassName = {}",
                        mapStoreConfig.getFactoryClassName());
            }
        }

        return cfg;
    }

    HazelcastInstance getHazelcastInstance() {
        return instance;
    }

    @Override
    public void putNotification(Map<String, String> notification) {
        notificationsCache.put(notification.get(PersistentNotification.NOTIFICATION_KEY_UUID),
                notification);
        LOGGER.debug("Successfully cached notification for user = "
                + notification.get(PersistentNotification.NOTIFICATION_KEY_USER_ID));
    }


    public void removeNotification(String notificationId, String userId) {
        Map<String, String> notification = (Map<String, String>) notificationsCache
                .get(notificationId);
        if (notification != null) {
            if (notification.get(PersistentNotification.NOTIFICATION_KEY_USER_ID).equals(userId)) {
                notificationsCache.remove(notificationId);
            }
        } else {
            LOGGER.debug("notification is null");
        }

    }

    @Override
    public List<Map<String, String>> getNotifications() {
        List<Map<String, String>> notificationsList = new ArrayList<Map<String, String>>();
        Collection<Object> notifications = notificationsCache.values(new SqlPredicate(
                "userId LIKE '%'"));
        for (Object notificationObj : notifications) {
            @SuppressWarnings("unchecked")
            Map<String, String> notificationMap = (Map<String, String>) notificationObj;
            notificationsList.add(notificationMap);
        }

        return notificationsList;
    }

    @Override
    public List<Map<String, String>> getNotifications(String userId) {
        List<Map<String, String>> notificationsList = new ArrayList<Map<String, String>>();
        if (StringUtils.isNotBlank(userId)) {
            Collection<Object> notifications = notificationsCache.values(new SqlPredicate(
                    "userId = '" + userId + "'"));
            for (Object notificationObj : notifications) {
                @SuppressWarnings("unchecked")
                Map<String, String> notificationMap = (Map<String, String>) notificationObj;
                notificationsList.add(notificationMap);
            }
        }

        return notificationsList;
    }
}
