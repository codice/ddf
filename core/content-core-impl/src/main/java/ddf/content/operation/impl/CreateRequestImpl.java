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
import ddf.content.operation.CreateRequest;


/**
 * CreateRequestImpl represents a {@link CreateRequest} and supports
 * passing a {@link Map} of properties for create operations.
 * 
 * @version 0.1.0
 * @since 2.1.0
 * 
 * @author Hugh Rodgers, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public class CreateRequestImpl extends OperationImpl implements CreateRequest
{
    private ContentItem contentItem;
    

    /**
     * Instantiates a new CreateRequestImpl with the {@link ContentItem} to be created.
     * 
     * @param contentItem the {@link ContentItem}
     */
    public CreateRequestImpl( ContentItem contentItem )
    {
        this( contentItem, null );
    }
    
    
    /**
     * Instantiates a new CreateRequestImpl with the {@link ContentItem} to be created
     * and a {@link Map} of properties.
     * 
     * @param contentItem the {@link ContentItem}
     * @param properties the properties of the operation
     */
    public CreateRequestImpl( ContentItem contentItem, Map<String, Serializable> properties )
    {
        super( properties );
        
        this.contentItem = contentItem;
    }
    
    
    @Override
    public ContentItem getContentItem()
    {
        return contentItem;
    }

}
