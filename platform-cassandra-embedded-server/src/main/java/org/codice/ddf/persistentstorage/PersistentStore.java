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
package org.codice.ddf.persistentstorage;

import java.util.Map;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.TableMetadata;

public interface PersistentStore {
    
    public Session getSession(String keyspaceName);
    
    public void createKeyspace(String keyspaceName);
    
    public void createTable(String keyspaceName, String tableName, String cqlColumnDescriptions);

    public TableMetadata getTable(String keyspaceName, String tableName);
    
    public void addEntry(String keyspaceName, String tableName, String cql);
    
    public void addEntry(String type, Map<String, Object> properties);
    
    public void getEntry(String type, String cql);
}
