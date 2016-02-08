/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.persistence.attributes.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.persistence.attributes.AttributesStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributesStoreImpl implements AttributesStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributesStoreImpl.class);

    private PersistentStore persistentStore;

    private static final String DATA_USAGE_KEY = "data_usage";

    private static final String USER_KEY = "user";

    public AttributesStoreImpl(PersistentStore persistentStore) {
        this.persistentStore = persistentStore;

    }

    @Override
    public long getCurrentDataUsageByUser(String username) {
        long currentDataUsage = 0L;
        List<Map<String, Object>> attributesList = new ArrayList<>();
        try {
            attributesList = persistentStore.get(PersistentStore.USER_ATTRIBUTE_TYPE,
                    USER_KEY + " = '" + username + "'");
        } catch (PersistenceException e) {
            LOGGER.info("PersistenceException trying to get attributes for user {}", username, e);
        }

        if (attributesList != null && attributesList.size() == 1) {
            Map<String, Object> attributes = PersistentItem.stripSuffixes(attributesList.get(0));
            currentDataUsage = (long) attributes.get(DATA_USAGE_KEY);

            LOGGER.debug("User {} data usage {} ", username, String.valueOf(currentDataUsage));
        }
        return currentDataUsage;
    }

    @Override
    public void updateUserDataUsage(String username, long dataUsage) {
        // add to usage and store
        PersistentItem item = new PersistentItem();
        item.addIdProperty(username);
        item.addProperty(USER_KEY, username);
        item.addProperty(DATA_USAGE_KEY, dataUsage);

        LOGGER.debug("Updating user {} data usage to {}", username, dataUsage);
        try {
            persistentStore.add(PersistentStore.USER_ATTRIBUTE_TYPE, item);
        } catch (PersistenceException e) {
            LOGGER.info("PersistenceException while trying to persist attribute for user {}",
                    username,
                    e);
        }
    }
}
