/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.content.operation;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;


/**
 * Defines how to access the properties, if any, associated with a content CRUD 
 * {@link Request} or {@link Response} operation.
 * 
 * @author Hugh Rodgers, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public interface Operation
{
    /**
     * Get the list of the names of the properties associated with this operation.
     * 
     * @return the list of property names.
     */
    public Set<String> getPropertyNames();
    

    /**
     * Get the value for the specified property name associated with this operation.
     * 
     * @param name the name of the property
     * @return the value of the specified property
     */
    public Serializable getPropertyValue(String name);

    
    /**
     * Check if an operation has a property with the specified name.
     * 
     * @param name the name of the property
     * @return <code>true</code> if the operation has the specified property; 
     *     <code>false</code> otherwise
     */
    public boolean containsPropertyName(String name);
    
    
    /**
     * Check if an operation has any properties.
     * 
     * @return <code>true</code> if the operation has any properties; 
     *     <code>false</code> otherwise
     */
    public boolean hasProperties();
    
    
    /**
     * Get the {@link Map} of properties associated with an operation.
     * 
     * @return the map of properties
     */
    public Map<String,Serializable> getProperties();
}
