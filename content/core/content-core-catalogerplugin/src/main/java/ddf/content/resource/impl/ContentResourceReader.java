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
package ddf.content.resource.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.ResourceResponseImpl;
import ddf.catalog.resource.ResourceImpl;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;
import ddf.content.ContentFramework;
import ddf.content.ContentFrameworkException;
import ddf.content.data.ContentItem;
import ddf.content.operation.ReadRequest;
import ddf.content.operation.ReadResponse;
import ddf.content.operation.impl.ReadRequestImpl;


public class ContentResourceReader implements ResourceReader
{
    private static XLogger logger = new XLogger( LoggerFactory.getLogger( ContentResourceReader.class ) );
    
    private static final String URL_CONTENT_SCHEME = "content";
    
    private static final String VERSION = "1.0";
    private static final String ID = "ContentResourceReader";
    private static final String TITLE = "Content Resource Reader";
    private static final String DESCRIPTION = "Retrieves a file from the DDF Content Repository.";
    private static final String ORGANIZATION = "DDF";
    
    private static Set<String> qualifierSet;
    static 
    {
        qualifierSet = new HashSet<String>(1);
        qualifierSet.add(URL_CONTENT_SCHEME);
    }
    
    private ContentFramework contentFramework;
    

    public ContentResourceReader( ContentFramework contentFramework )
    {
        logger.trace( "INSIDE: ContentResourceReader constructor" );
        
        this.contentFramework = contentFramework;
    }
    
    
    @Override
    public String getDescription()
    {
        return DESCRIPTION;
    }

    
    @Override
    public String getId()
    {
        return ID;
    }

    
    @Override
    public String getOrganization()
    {
        return ORGANIZATION;
    }

    
    @Override
    public String getTitle()
    {
        return TITLE;
    }

    
    @Override
    public String getVersion()
    {
        return VERSION;
    }

    
    @Override
    public Set<String> getOptions( Metacard metacard )
    {
        logger.trace( "ENTERING/EXITING: getOptions" );
        logger.debug( "ContentResourceReader getOptions doesn't support options, returning empty set." );
        
        return Collections.emptySet();
    }

    
    @Override
    public Set<String> getSupportedSchemes()
    {
        return qualifierSet;
    }

    
    @Override
    public ResourceResponse retrieveResource( URI resourceUri, Map<String, Serializable> arguments ) throws IOException,
        ResourceNotFoundException, ResourceNotSupportedException
    {
        logger.trace( "ENTERING: retrieveResource" );
        
        ResourceResponse response = null;
        
        if ( resourceUri == null) 
        {
            logger.warn( "Resource URI was null" );
            throw new ResourceNotFoundException( "Unable to find resource - resource URI was null" );
        }
    
        if ( resourceUri.getScheme().equals( URL_CONTENT_SCHEME ) ) 
        {
            logger.debug( "Resource URI is content scheme" );
            String contentId = resourceUri.getSchemeSpecificPart();
            if ( contentId != null && !contentId.isEmpty() )
            {
                ReadRequest readRequest = new ReadRequestImpl( contentId );
                try
                {
                    ReadResponse readResponse = contentFramework.read( readRequest );
                    ContentItem contentItem = readResponse.getContentItem();
                    File filePathName = contentItem.getFile();
                    String fileName = filePathName.getName();
                    logger.debug( "resource name: " + fileName );
                    InputStream is = contentItem.getInputStream();
                    response = new ResourceResponseImpl( new ResourceImpl( new BufferedInputStream( is ), contentItem.getMimeType(),
                        fileName ) );
                }
                catch ( ContentFrameworkException e )
                {
                    throw new ResourceNotFoundException( e );
                }
            }
        }
        
        logger.trace( "EXITING: retrieveResource" );
        
        return response;
    }

}
