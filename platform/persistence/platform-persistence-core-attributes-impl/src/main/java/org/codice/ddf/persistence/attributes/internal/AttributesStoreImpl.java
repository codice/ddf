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

                // add to usage and store
                PersistentItem item = new PersistentItem();
                item.addIdProperty(username);
                item.addProperty(USER_KEY, username);
                item.addProperty(DATA_USAGE_KEY, dataUsage);

                LOGGER.debug("Updating user {} data usage to {}", username, dataUsage);
                persistentStore.add(PersistentStore.USER_ATTRIBUTE_TYPE, item);
            } finally {
                readWriteLock.writeLock()
                        .unlock();

            }
        }
    }

    @Override
    public void setDataUsage(final String username, final long dataUsage) throws PersistenceException {

        if (StringUtils.isEmpty(username)) {
            throw new PersistenceException(EMPTY_USERNAME_ERROR);
        }
        if (dataUsage > 0) {
            try {
                readWriteLock.writeLock()
                        .lock();

                // add to usage and store
                PersistentItem item = new PersistentItem();
                item.addIdProperty(username);
                item.addProperty(USER_KEY, username);
                item.addProperty(DATA_USAGE_KEY, dataUsage);

                LOGGER.debug("Updating user {} data usage to {}", username, dataUsage);
                persistentStore.add(PersistentStore.USER_ATTRIBUTE_TYPE, item);
            } finally {
                readWriteLock.writeLock()
                        .unlock();

            }
        }
    }

    private long getCurrentDataUsageByUserNoLock(final String username)
            throws PersistenceException {
        long currentDataUsage = 0L;
        List<Map<String, Object>> attributesList;
        attributesList = persistentStore.get(PersistentStore.USER_ATTRIBUTE_TYPE,
                String.format("%s = '%s'", USER_KEY, username));

        if (attributesList != null && attributesList.size() == 1) {
            Map<String, Object> attributes = PersistentItem.stripSuffixes(attributesList.get(0));
            currentDataUsage = (long) attributes.get(DATA_USAGE_KEY);

            LOGGER.debug("User {} data usage {} ", username, String.valueOf(currentDataUsage));
        }
        return currentDataUsage;

    }

}
