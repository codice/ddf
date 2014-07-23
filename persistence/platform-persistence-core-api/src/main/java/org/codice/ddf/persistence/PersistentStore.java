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
package org.codice.ddf.persistence;

import java.util.List;
import java.util.Map;

public interface PersistentStore {
    
    public static final String METACARD_TYPE = "metacard";
    public static final String SAVED_QUERY_TYPE = "saved_query";
    public static final String NOTIFICATION_TYPE = "notification";
    public static final String ACTIVITY_TYPE = "activity";
    public static final String WORKSPACE_TYPE = "workspace";

    /**
     * Adds item of specified type with the specified properties.
     * 
     * @param type
     * @param properties
     * @throws PersistenceException
     */
    public void add(String type, Map<String, Object> properties) throws PersistenceException;
    
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
