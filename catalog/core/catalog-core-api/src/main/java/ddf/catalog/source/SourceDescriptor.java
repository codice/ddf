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
package ddf.catalog.source;

import java.util.Date;
import java.util.Set;

import ddf.catalog.data.ContentType;
import ddf.catalog.util.Describable;

/**
 * The Interface {@link SourceDescriptor} is used to describe a {@link Source}.
 * 
 */
public interface SourceDescriptor extends Describable{
    
    /**
     * The name of this {@link Source}.
     *
     * @return the source id
     */
    public String getSourceId();
    
    /**
     * Gets the content types located within this {@link Source}
     *
     * @return the content types
     */
    public Set<ContentType> getContentTypes();
    
    /**
     * Checks if {@link Source} is available.
     *
     * @return true, if is available
     * 
     * @see Source#isAvailable()
     */
    public boolean isAvailable();
    
    /**
     * Gets the last availability date of the {@link Source}
     *
     * @return the last availability date
     * 
     */
    public Date getLastAvailabilityDate();
    
}
