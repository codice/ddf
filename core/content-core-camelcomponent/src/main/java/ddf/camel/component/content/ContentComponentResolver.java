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
package ddf.camel.component.content;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.spi.ComponentResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ContentComponentResolver implements ComponentResolver
{
    private static final transient Logger LOGGER = LoggerFactory.getLogger(ContentComponentResolver.class);
    
    private Component component;

    
    /**
     * Constructs component resolver for specified Camel Content component.
     * 
     * @param component the Camel component associated with this component resolver
     */
    public ContentComponentResolver( Component component ) 
    {
        LOGGER.trace( "INSIDE: constructor" );
        this.component = component;
    }

    
    @Override
    public Component resolveComponent( String name, CamelContext context ) throws Exception
    {
        LOGGER.trace( "INSIDE: resolveComponent" );
        
        if ( ContentComponent.NAME.equals( name ) ) 
        {
            return component;
        }
        return null;
    }

}
