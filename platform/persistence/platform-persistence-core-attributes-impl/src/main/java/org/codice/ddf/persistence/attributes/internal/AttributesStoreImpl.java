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

package org.codice.ddf.persistence.attributes.internal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.persistence.attributes.AttributesStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributesStoreImpl implements AttributesStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributesStoreImpl.class);

    private PersistentStore persistentStore;

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private static final String EMPTY_USERNAME_ERROR = "Empty username specified";

    private Long defaultLimit;

    public AttributesStoreImpl(PersistentStore persistentStore) {
        this.persistentStore = persistentStore;
    }

    @Override
    public long getCurrentDataUsageByUser(final String username) throws PersistenceException {
        long currentDataUsage = 0L;

        if (StringUtils.isEmpty(username)) {
            throw new PersistenceException(EMPTY_USERNAME_ERROR);
        }

        try {
            readWriteLock.readLock()
                    .lock();
            currentDataUsage = getCurrentDataUsageByUserNoLock(username);
        } finally {
            readWriteLock.readLock()
                    .unlock();
        }

        return currentDataUsage;
    }

    @Override
    public long getDataLimitByUser(final String username) throws PersistenceException {
        long dataLimit = defaultLimit;

        if (StringUtils.isEmpty(username)) {
            throw new PersistenceException(EMPTY_USERNAME_ERROR);
        }

        try {
            readWriteLock.readLock()
                    .lock();
            dataLimit = getDataLimitByUserNoLock(username);
        } finally {
            readWriteLock.readLock()
                    .unlock();
        }

        return dataLimit;
    }

    @Override
    public void updateUserDataUsage(final String username, final long newDataUsage)
            throws PersistenceException {
        if (StringUtils.isEmpty(username)) {
            throw new PersistenceException(EMPTY_USERNAME_ERROR);
        }

        if (newDataUsage > 0) {
            try {
                readWriteLock.writeLock()
                        .lock();
                long dataUsage = getCurrentDataUsageByUserNoLock(username);
                dataUsage += newDataUsage;

                LOGGER.debug("Updating user {} data usage to {}", username, dataUsage);
                persistentStore.add(PersistentStore.USER_ATTRIBUTE_TYPE, toPersistentItem(username,
                        dataUsage,
                        getDataLimitByUser(username)));
            } finally {
                readWriteLock.writeLock()
                        .unlock();

            }
        }
    }

    @Override
    public void setDataUsage(final String username, final long dataUsage)
            throws PersistenceException {

        if (StringUtils.isEmpty(username)) {
            throw new PersistenceException(EMPTY_USERNAME_ERROR);
        }
        if (dataUsage >= 0) {
            try {
                readWriteLock.writeLock()
                        .lock();

                LOGGER.debug("Updating user {} data usage to {}", username, dataUsage);
                persistentStore.add(PersistentStore.USER_ATTRIBUTE_TYPE, toPersistentItem(username,
                        dataUsage,
                        defaultLimit));
            } finally {
                readWriteLock.writeLock()
                        .unlock();

            }
        }
    }

    @Override
    public void setDataLimit(final String username, final long dataLimit)
            throws PersistenceException {

        if (StringUtils.isEmpty(username)) {
            throw new PersistenceException(EMPTY_USERNAME_ERROR);
        }
        if (dataLimit >= 0) {
            try {
                readWriteLock.writeLock()
                        .lock();

                LOGGER.debug("Updating user {} data limit to {}", username, dataLimit);
                persistentStore.add(PersistentStore.USER_ATTRIBUTE_TYPE, toPersistentItem(username,
                        getCurrentDataUsageByUser(username),
                        dataLimit));
            } finally {
                readWriteLock.writeLock()
                        .unlock();

            }
        }
    }

    @Override
    public List<Map<String, Object>> getAllUsers() throws PersistenceException {
        List<Map<String, Object>> userMap;
        try {
            readWriteLock.readLock()
                    .lock();
            userMap = persistentStore.get(PersistentStore.USER_ATTRIBUTE_TYPE);
        } finally {
            readWriteLock.readLock()
                    .unlock();
        }
        return userMap;
    }

    @Override
    public void resetUserDataUsages() throws PersistenceException {
        List<Map<String, Object>> users = getAllUsers();
        for (Map<String, Object> user : users) {
            String username = (String) user.get(AttributesStore.USER_KEY + "_txt");
            long dataLimit = (long) user.get(AttributesStore.DATA_USAGE_LIMIT_KEY + "_lng");
            try {
                readWriteLock.writeLock()
                        .lock();

                LOGGER.debug("Resetting Data usage for user : {}", username);
                persistentStore.add(PersistentStore.USER_ATTRIBUTE_TYPE, toPersistentItem(username,
                        0L,
                        dataLimit));
            } finally {
                readWriteLock.writeLock()
                        .unlock();
            }
        }
    }

    private PersistentItem toPersistentItem(final String username, final long dataUsage,
            final long dataLimit) throws PersistenceException {
        // add to usage and store
        PersistentItem item = new PersistentItem();
        item.addIdProperty(username);
        item.addProperty(USER_KEY, username);
        item.addProperty(DATA_USAGE_KEY, dataUsage);
        item.addProperty(DATA_USAGE_LIMIT_KEY, dataLimit);

        LOGGER.debug("Created PersistentItem : User {} Usage {} Limit {}",
                username,
                dataUsage,
                dataLimit);
        return item;
    }

    private long getCurrentDataUsageByUserNoLock(final String username)
            throws PersistenceException {
        long currentDataUsage = 0L;
        List<Map<String, Object>> attributesList;
        attributesList = persistentStore.get(PersistentStore.USER_ATTRIBUTE_TYPE, String.format(
                "%s = '%s'",
                USER_KEY,
                username));

        if (attributesList != null && attributesList.size() == 1) {
            Map<String, Object> attributes = PersistentItem.stripSuffixes(attributesList.get(0));
            currentDataUsage = (long) attributes.get(DATA_USAGE_KEY);

            LOGGER.debug("User {} data usage {} ", username, String.valueOf(currentDataUsage));
        }
        return currentDataUsage;

    }

    private long getDataLimitByUserNoLock(final String username) throws PersistenceException {
        long dataLimit = defaultLimit;
        List<Map<String, Object>> attributesList;
        attributesList = persistentStore.get(PersistentStore.USER_ATTRIBUTE_TYPE, String.format(
                "%s = '%s'",
                USER_KEY,
                username));

        if (attributesList != null && attributesList.size() == 1) {
            Map<String, Object> attributes = PersistentItem.stripSuffixes(attributesList.get(0));
            dataLimit = (long) attributes.get(DATA_USAGE_LIMIT_KEY);

            LOGGER.debug("User {} data limit {} ", username, String.valueOf(dataLimit));
        }
        return dataLimit;
    }

    public void setDefaultLimit(Long defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public Long getDefaultLimit() {
        return this.defaultLimit;
    }
}
