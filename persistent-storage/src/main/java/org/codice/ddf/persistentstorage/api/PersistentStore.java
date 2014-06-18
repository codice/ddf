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
package org.codice.ddf.persistentstorage.api;

import java.util.Map;

public interface PersistentStore {
    
    public static final String METACARD_TYPE = "metacard";
    public static final String SAVED_QUERY_TYPE = "saved_query";
    public static final String NOTIFICATION_TYPE = "notification";
    public static final String TASK_TYPE = "task";
    
    public void addEntry(String type, Map<String, Object> properties);
    
}
