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
package ddf.catalog.operation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class OperationImpl implements Operation {

    protected Map<String, Serializable> properties;

    public OperationImpl(Map<String, Serializable> properties) {
        this.properties = properties;
        if (this.properties == null) {
            this.properties = new HashMap<String, Serializable>();
        }
    }

    public void setProperties(Map<String, Serializable> newProperties) {
        this.properties = newProperties;
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public Serializable getPropertyValue(String name) {
        return properties.get(name);
    }

    @Override
    public boolean containsPropertyName(String name) {
        return properties.containsKey(name);
    }

    @Override
    public boolean hasProperties() {
        return !properties.isEmpty();
    }

    @Override
    public Map<String, Serializable> getProperties() {
        return properties;
    }

}
