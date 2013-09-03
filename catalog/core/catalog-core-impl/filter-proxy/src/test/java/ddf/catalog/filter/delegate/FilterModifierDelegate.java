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
package ddf.catalog.filter.delegate;

import org.opengis.filter.Filter;

import ddf.catalog.filter.FilterBuilder;


public class FilterModifierDelegate extends CopyFilterDelegate
{
    public FilterModifierDelegate(FilterBuilder filterBuilder)
    {
        super(filterBuilder);
    }
    
    
    @Override
    public Filter propertyIsEqualTo(String propertyName, String literal,
            boolean isCaseSensitive) {
        
        // Build the original filter
        Filter originalFilter = filterBuilder.attribute(propertyName).equalTo().text(literal);
        
        // Add extra contextual search phrase on "classification" field
        Filter extraFilter = filterBuilder.attribute("classification").equalTo().text("UNCLASS");
        
        // AND both filters together and return it
        Filter modifiedFilter = filterBuilder.allOf(originalFilter, extraFilter);
        
        return modifiedFilter;
    }
}
