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
package ddf.sdk.metrics;

import ddf.catalog.filter.FilterDelegate;


/**
 * A custom filter delegate for this sample metric (point radius) to examine the
 * incoming QueryRequest to see if it is a point radius type of search.
 * 
 * @author rodgersh
 *
 */
public class QueryTypeFilterDelegate  extends FilterDelegate<Boolean>
{
    private boolean isPointRadius = false;
    
    
    // Only need to override the FilterDelegate methods of interest to your
    // sample metric, in this case DWithin maps to Point Radius searches
    @Override
    public Boolean dwithin(String propertyName, String wkt, double distance) 
    {
        return isPointRadius = true;
    }
    
    
    /**
     * Used by the SampleMetric to determine if this was a point radius query.
     * 
     * @return
     */
    public boolean isPointRadius() 
    {
        return isPointRadius;
    }
}
