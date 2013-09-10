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
package ddf.content.endpoint.rest;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.content.data.ContentItem;
import ddf.content.operation.CreateRequest;
import ddf.content.operation.CreateResponse;
import ddf.content.operation.DeleteRequest;
import ddf.content.operation.DeleteResponse;
import ddf.content.operation.ReadRequest;
import ddf.content.operation.ReadResponse;
import ddf.content.operation.UpdateRequest;
import ddf.content.operation.UpdateResponse;
import ddf.content.operation.impl.CreateResponseImpl;
import ddf.content.storage.StorageException;
import ddf.content.storage.StorageProvider;


public class MockStorageProvider implements StorageProvider
{
    private static final XLogger LOGGER = new XLogger( LoggerFactory.getLogger( MockStorageProvider.class ) );
    
    
    @Override
    public CreateResponse create( CreateRequest createRequest ) throws StorageException
    {
        ContentItem item = createRequest.getContentItem();
      try
      {
          LOGGER.debug( "item mime type = " + item.getMimeType() );
          String data = IOUtils.toString( item.getInputStream() );
          LOGGER.debug( "input stream has " + data.length() + " bytes" );
      }
      catch (IOException e1)
      {
		LOGGER.warn("IOException while obtaining content item",e1);
      }
      CreateResponse response = new CreateResponseImpl( createRequest, item, null, null );
      
        return response;
    }
    

    @Override
    public ReadResponse read( ReadRequest readRequest ) throws StorageException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UpdateResponse update( UpdateRequest updateRequest ) throws StorageException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DeleteResponse delete( DeleteRequest deleteRequest ) throws StorageException
    {
        // TODO Auto-generated method stub
        return null;
    }

}
