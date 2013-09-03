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
package ddf.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import org.apache.commons.lang.builder.ToStringBuilder;

import ddf.catalog.resource.Resource;

// TODO: Auto-generated Javadoc
/**
 * The Class ProductDataSource.
 */
public class ResourceDataSource implements DataSource {

	/** The content type. */
	private String contentType;
	
	/** The is. */
	private InputStream is;
	
	/** The name. */
	private String name;

	/**
	 * Create a new product data source.
	 * @param resource 
	 *
	 */
	public ResourceDataSource( Resource resource ) {
	    if ( resource != null ){
	        this.contentType = resource.getMimeTypeValue();
	        this.is = resource.getInputStream();
	        this.name = resource.getName();
	    }
		
	}

	/* (non-Javadoc)
	 * @see javax.activation.DataSource#getContentType()
	 */
	@Override
	public String getContentType() {
		return contentType;
	}

	/* (non-Javadoc)
	 * @see javax.activation.DataSource#getInputStream()
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		return is;
	}

	/* (non-Javadoc)
	 * @see javax.activation.DataSource#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return null;
	}


    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString( this );
    }
}
