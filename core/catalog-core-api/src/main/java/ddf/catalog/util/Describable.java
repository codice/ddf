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
package ddf.catalog.util;

import ddf.catalog.data.Metacard;
import ddf.catalog.source.Source;
import ddf.catalog.transform.MetacardTransformer;

/**
 * Describable is used to capture a basic description. For an example of a how the Describable
 * interface is used view the {@link Source} interface and the {@link DescribableImpl} class.
 * 
 * @author ddf.isgs@lmco.com
 */
public interface Describable {

    /**
     * Retrieve the version.
     * 
     * @return the version of the item being described (example: 1.0)
     */
    public String getVersion();

    /**
     * Returns the name, aka ID, of the describable item. The name should be unique for each
     * instance. <br/>
     * Example:
     * <code>html<code> for a {@link MetacardTransformer} that transforms {@link Metacard}s to HTML
     * 
     * @return ID of the item
     */
    public String getId();

    /**
     * Returns the title of the describable item. It is generally more verbose than the name (aka
     * ID).
     * 
     * @return title of the item (example: Dummy Catalog Provider)
     */
    public String getTitle();

    /**
     * Returns a description of the describable item.
     * 
     * @return description of the item (example: Provider that returns back static results)
     */
    public String getDescription();

    /**
     * Returns the organization associated with the describable item.
     * 
     * @return organizational name or acronym (example: USAF)
     */
    public String getOrganization();

}