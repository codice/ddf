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

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.content.ContentFramework;
import ddf.mime.MimeTypeMapper;


public class ContentComponent extends DefaultComponent
{
    private static final transient Logger LOGGER = LoggerFactory.getLogger(ContentComponent.class);
    
    /** The name of the scheme this custom Camel component resolves to. */
    public static final String NAME = "content";
    
    private BundleContext bundleContext;
    
    private ContentFramework contentFramework;
    
    private MimeTypeMapper mimeTypeMapper;
    
    
    public ContentComponent()
    {
        super();
        LOGGER.debug( "INSIDE ContentComponent constructor" );
    }
      
    
    /* (non-Javadoc)
     * @see org.apache.camel.impl.DefaultComponent#createEndpoint(java.lang.String, java.lang.String, java.util.Map)
     */
    protected Endpoint createEndpoint( String uri, String remaining, Map<String, Object> parameters ) 
        throws Exception 
    {
        LOGGER.trace( "ENTERING: createEndpoint" );
        
        LOGGER.debug( "uri = " + uri + ",  remaining = " + remaining );
        LOGGER.debug( "parameters = " + parameters );
        
        Endpoint endpoint = new ContentEndpoint(uri, this);
        
        try {
            setProperties( endpoint, parameters );
        } catch (Exception e) {
            throw new Exception("Failed to create content endpoint", e);
        }
        
        LOGGER.trace( "EXITING: createEndpoint" );
        
        return endpoint;
    }


    public ContentFramework getContentFramework()
    {
        return contentFramework;
    }


    public void setContentFramework( ContentFramework contentFramework )
    {
        this.contentFramework = contentFramework;
    }


    public MimeTypeMapper getMimeTypeMapper()
    {
        return mimeTypeMapper;
    }


    public void setMimeTypeMapper( MimeTypeMapper mimeTypeMapper )
    {
        this.mimeTypeMapper = mimeTypeMapper;
    } 
     
}
