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
package ddf.content.operation.impl;

import java.io.Serializable;
import java.util.Map;

import ddf.content.data.ContentItem;
import ddf.content.operation.ReadRequest;
import ddf.content.operation.ReadResponse;


/**
 * ReadResponseImpl contains the {@link ReadResponse} information (retrieved {@link ContentItem})
 * from a {@link ReadRequest} operation.
 */
public class ReadResponseImpl extends ResponseImpl<ReadRequest> implements ReadResponse
{
    private ContentItem contentItem;
    

    /**
     * Instantiates a ReadResponseImpl object with {@link ContentItem} read.
     * 
     * @param request the original {@link ReadRequest} that initiated this response
     * @param contentItem the {@link ContentItem} read
     */
    public ReadResponseImpl( ReadRequest request, ContentItem contentItem )
    {
        this( request, contentItem, null, null );
    }
    

    /**
     * Instantiates a ReadResponseImpl object with {@link ContentItem} read.
     * 
     * @param request the original {@link ReadRequest} that initiated this response
     * @param contentItem the {@link ContentItem} read
     * @param the properties associated with the response that are intended for external distribution
     */
    public ReadResponseImpl( ReadRequest request, ContentItem contentItem, Map<String, String> responseProperties )
    {
        this( request, contentItem, responseProperties, null );
    }
    
    
    /**
     * Instantiates a ReadResponseImpl object with {@link ContentItem} read
     * a {@link Map} of properties.
     * 
     * @param request the original {@link ReadRequest} that initiated this response
     * @param contentItem the {@link ContentItem} read
     * @param the properties associated with the response that are intended for external distribution
     * @param properties the properties associated with the operation
     */
    public ReadResponseImpl( ReadRequest request, ContentItem contentItem, Map<String, String> responseProperties, Map<String, Serializable> properties )
    {
        super( request, responseProperties, properties );
        
        this.contentItem = contentItem;
    }
    
    
    @Override
    public ContentItem getContentItem()
    {
        return contentItem;
    }
}
