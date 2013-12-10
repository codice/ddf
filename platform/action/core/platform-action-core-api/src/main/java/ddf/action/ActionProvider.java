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
package ddf.action;

/**
 * This class provides an {@link Action} for a given subject. Objects that the
 * {@link ActionProvider} can handle are not restricted to a particular class and can be whatever
 * the {@link ActionProvider} is able to handle. <br>
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 * @see Action
 * @see ActionRegistry
 */
public interface ActionProvider {

    /**
     * 
     * @param subject
     *            object for which the {@link ActionProvider} is requested to provide an
     *            {@link Action}
     * @return an {@link Action} object. If no action can be taken on the input, then
     *         <code>null</code> shall be returned
     */
    public <T> Action getAction(T subject);

    /**
     * 
     * @return a unique identifier to distinguish the type of service this {@link ActionProvider}
     *         provides
     */
    public String getId();

}