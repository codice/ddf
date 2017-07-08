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
package org.codice.ddf.persistence;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PersistentStore {

    String METACARD_TYPE = "metacard";
    String SAVED_QUERY_TYPE = "saved_query";
    String NOTIFICATION_TYPE = "notification";
    String ACTIVITY_TYPE = "activity";
    String WORKSPACE_TYPE = "workspace";
    String PREFERENCES_TYPE = "preferences";
    String USER_ATTRIBUTE_TYPE = "attributes";
    String SUBSCRIPTION_TYPE = "subscriptions";
    String EVENT_SUBSCRIPTIONS_TYPE = "event_subscriptions";
    String ALERT_TYPE = "alerts";
    String DECANTER_TYPE = "decanter";

    Set<String> PERSISTENCE_TYPES = new HashSet<String>(Arrays.asList(METACARD_TYPE,
            SAVED_QUERY_TYPE,
            NOTIFICATION_TYPE,
            ACTIVITY_TYPE,
            WORKSPACE_TYPE,
            PREFERENCES_TYPE,
            USER_ATTRIBUTE_TYPE,
            SUBSCRIPTION_TYPE,
            EVENT_SUBSCRIPTIONS_TYPE,
            ALERT_TYPE,
            DECANTER_TYPE));

    /**
     * Adds item of specified type with the specified properties.
     *
     * @param type A non-empty string identifying the type of item being added.
     * @param properties A map of properties making up the item. Property keys must have a suffix that identifies the type of value for the entry. The PersistentItem class should be used for creating this map.
     * @throws PersistenceException If the type is empty or there was an issue persisting the item.
     */
    public void add(String type, Map<String, Object> properties) throws PersistenceException;

    /**
     * Adds a collection of items of specified type.
     *
     * @param type A non-empty string identifying the type of items being added.
     * @param items A list of map properties making up the items. Property keys must have a suffix that identifies the type of value for the entry. The PersistentItem class should be used for creating these maps.
     * @throws PersistenceException If the type is empty or there was an issue persisting the item.
     */
    public void add(String type, Collection<Map<String, Object>> items) throws PersistenceException;

    /**
     * Get all of the items of the specified type.
     *
     * @param type
     * @return
     * @throws PersistenceException
     */
    public List<Map<String, Object>> get(String type) throws PersistenceException;

    /**
     * Get items matching the ECQL query criteria.
     *
     * @param type
     * @param ecql
     * @return
     * @throws PersistenceException
     */
    public List<Map<String, Object>> get(String type, String ecql) throws PersistenceException;

    /**
     * Delete items matching the ECQL query criteria.
     *
     * @param type
     * @param ecql
     * @return Count of the items deleted
     * @throws PersistenceException
     */
    public int delete(String type, String ecql) throws PersistenceException;

}
