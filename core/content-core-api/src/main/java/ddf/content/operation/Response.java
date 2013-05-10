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

import java.util.Map;
import java.util.Set;


/**
 * Marker interface to differentiate {@link Request}s from {@link Response}s.
 * 
 * @since 2.1.0
 * 
 * @author Hugh Rodgers
 * @author ddf.isgs@lmco.com
 *
 */
public interface Response<T extends Request> extends Operation
{
    /**
     * Get the request that generated this response
     * @return {@link Request} - the {@link Request} that generated this {@link Response}.
     */
    public T getRequest();
    
    
    /**
     * Get the list of the names of the response properties associated with this operation.
     * 
     * @return the list of response property names.
     */
    public Set<String> getResponsePropertyNames();
    

    /**
     * Get the value for the specified response property name associated with this operation.
     * 
     * @param name the name of the response property
     * @return the value of the specified response property
     */
    public String getResponsePropertyValue(String name);

    
    /**
     * Check if an operation has a response property with the specified name.
     * 
     * @param name the name of the response property
     * @return <code>true</code> if the operation has the specified response property, 
     *     <code>false</code> otherwise
     */
    public boolean containsResponsePropertyName(String name);
    
    
    /**
     * Check if an operation has any response properties.
     * 
     * @return <code>true</code> if the operation has any response properties, 
     *     <code>false</code> otherwise
     */
    public boolean hasResponseProperties();
    
    
    /**
     * Get the {@link Map} of properties associated with a response.
     * 
     * @return the map of response properties
     */
    public Map<String,String> getResponseProperties();
}
