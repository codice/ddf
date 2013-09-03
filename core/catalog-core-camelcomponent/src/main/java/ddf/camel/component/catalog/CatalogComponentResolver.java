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
package ddf.camel.component.catalog;

import java.io.InputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.spi.ComponentResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.camel.component.catalog.converter.BinaryContentStringTypeConverter;
import ddf.catalog.data.BinaryContent;


/**
 * Custom Camel component resolver used to resolve the custom DDF <code>catalog</code> scheme in Camel
 * route nodes, i.e., route nodes with URIs like 
 *     <code>
 *     uri="catalog:inputtransformer?id=text/xml;id=identity"
 *     </code>
 *     
 * @author Hugh Rodgers, Lockheed Martin
 * @author William Miller, Lockheed Martin
 * @author ddf.isgs@lmco.com
 *
 */
public class CatalogComponentResolver implements ComponentResolver
{
    private static final transient Logger LOGGER = LoggerFactory.getLogger( CatalogComponentResolver.class );
    
    private final Component component;

    
    /**
     * Constructs component resolver for specified Camel component.
     * 
     * @param component the Camel component associated with this component resolver
     */
    public CatalogComponentResolver( Component component ) 
    {
        LOGGER.debug( "INSIDE: constructor" );
        this.component = component;
    }

    
    /* (non-Javadoc)
     * @see org.apache.camel.spi.ComponentResolver#resolveComponent(java.lang.String, org.apache.camel.CamelContext)
     */
    public Component resolveComponent( String name, CamelContext context ) 
    {
        LOGGER.debug( "INSIDE: resolveComponent" );
        
        if(null != context) 
        {
        	BinaryContentStringTypeConverter converter = new BinaryContentStringTypeConverter();
        	context.getTypeConverterRegistry().addTypeConverter(BinaryContent.class, String.class, converter);
        	context.getTypeConverterRegistry().addTypeConverter(BinaryContent.class, InputStream.class, converter);
        }
        if ( CatalogComponent.NAME.equals( name ) ) 
        {
            return component;
        }
        return null;
    }
}
