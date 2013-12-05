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
package ddf.content.data.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.content.ContentFramework;
import ddf.content.data.ContentItem;
import ddf.content.operation.CreateRequest;
import ddf.content.operation.UpdateRequest;
import ddf.content.storage.StorageProvider;


/**
 * The IncomingContentItem class represents a {@link ContentItem} POJO that
 * is being sent in a {@link CreateRequest} or an {@link UpdateRequest} to the
 * {@link ContentFramework}.
 *
 */
public class IncomingContentItem implements ContentItem
{
    private static final XLogger LOGGER = new XLogger( LoggerFactory.getLogger( IncomingContentItem.class ) );

    private InputStream inputStream;

    private String mimeTypeRawData;
    
    private MimeType mimeType;

    private String id;
    
    private String uri;
    
    
    /**
     * An incoming content item for a {@link CreateRequest} since the ID will
     * initially be <code>null</code> because the {@link StorageProvider} will
     * assign its GUID.
     * 
     * @param stream the {@link ContentItem}'s input stream containing its actual data
     * @param mimeType the {@link ContentItem}'s mime type
     */
    public IncomingContentItem( InputStream stream, String mimeTypeRawData )
    {
        this( null, stream, mimeTypeRawData );
    }
    
    
    /**
     * An incoming content item for an {@link UpdateRequest} where the item's GUID
     * should be known.
     * 
     * @param id the {@link ContentItem}'s GUID
     * @param stream the {@link ContentItem}'s input stream containing its actual data
     * @param mimeType the {@link ContentItem}'s mime type
     */
    public IncomingContentItem( String id, InputStream stream, String mimeTypeRawData )
    {
        this.inputStream = stream;
        this.mimeTypeRawData = mimeTypeRawData;
        this.id = id;
        this.mimeType = null;
        
        if (mimeTypeRawData != null)
        {
            try
            {
                this.mimeType = new MimeType(mimeTypeRawData);
            }
            catch (MimeTypeParseException e)
            {
                LOGGER.debug("Unable to create MimeType from raw data " + mimeTypeRawData);                
            }
        }
    }
    
    
    public void setId( String id )
    {
        this.id = id;
    }
    
    
    @Override
    public String getId()
    {
        return id;
    }

    
    @Override
    public MimeType getMimeType()
    {
        return mimeType;
    }

    
    @Override
    public InputStream getInputStream() throws IOException
    {
        return inputStream;
    }
    
    
    @Override
    public File getFile() throws IOException
    {
        throw new IOException("Content Framework hasn't stored this ContentItem yet");
    }

    
    @Override
    public long getSize() throws IOException
    {
        throw new IOException("Content Framework hasn't stored this ContentItem yet");
    }


    @Override
    public void setUri( String uri )
    {
        this.uri = uri;
    }


    @Override
    public String getUri()
    {
        return uri;
    }


    @Override
    public String getMimeTypeRawData()
    {
        return mimeTypeRawData;
    }

}
