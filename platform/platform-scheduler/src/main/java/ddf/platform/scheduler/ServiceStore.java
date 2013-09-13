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
package ddf.platform.scheduler;

import java.util.HashMap;
import java.util.Map;

import org.quartz.Job;

/**
 * Allows {@link Job}s or other objects within this limited classpath to acquire them from a single
 * location.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class ServiceStore {

    private Map<String, Object> map = new HashMap<String, Object>();

    private static ServiceStore uniqueInstance;

    private ServiceStore() {
    }

    /**
     * 
     * @return a unique instance of {@link ServiceStore}
     */
    public static synchronized ServiceStore getInstance() {
        if (uniqueInstance == null) {
            uniqueInstance = new ServiceStore();
        }
        return uniqueInstance;
    }

    /**
     * Stores objects based on their interface
     * 
     * @param object
     *            implementation of at least one interface
     */
    public void setObject(Object object) {

        Class<?>[] interfaces = object.getClass().getInterfaces();

        for (Class<?> interfaceObject : interfaces) {
            String interfaceKey = interfaceObject.getSimpleName();
            map.put(interfaceKey, object);
        }

    }

    /**
     * 
     * @param key
     *            fully qualified interface name
     * @return object that implements the interface
     */
    public Object getObject(String key) {
        return map.get(key);
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
