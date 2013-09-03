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
package ddf.sdk.plugin.presubscription;

import org.opengis.filter.Filter;

import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.delegate.CopyFilterDelegate;


public class FilterModifierDelegate extends CopyFilterDelegate
{
    public FilterModifierDelegate(FilterBuilder filterBuilder)
    {
        super(filterBuilder);
    }
    
    
    @Override
    public Filter propertyIsLike(String propertyName, String literal,
            boolean isCaseSensitive) {
        
        // Build the original filter
        Filter originalFilter = filterBuilder.attribute(propertyName).like().text(literal);
        
        // Add extra contextual search phrase on "releasableTo" field to only
        // allow catalog entries releasable to Canada to generate events when created/updated/deleted
        Filter extraFilter = filterBuilder.attribute("/ddms:Resource/ddms:security/@ICISM:releasableTo").like().text("CAN");
        
        // AND both filters together and return it
        Filter modifiedFilter = filterBuilder.allOf(originalFilter, extraFilter);
        
        return modifiedFilter;
    }
}
