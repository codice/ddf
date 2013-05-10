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
package ddf.catalog.operation;

import java.io.Serializable;
import java.net.URI;
import java.util.Map;

public class ResourceRequestByProductUri extends OperationImpl implements ResourceRequest {

	protected String name;
	protected URI uri;
	
	/**
	 * Implements a ResourceRequestByProductUri and specifies the ${@link URI}

	 * @param uri the URI
	 */
	public ResourceRequestByProductUri( URI uri ){
        this( uri, null );
    }
	
	/**
	 * Implements a ResourceRequestByProductUri and specifies the ${@link URI} and
	 * a ${@link Map} of properties

	 * @param uri the URI
	 * @param properties the properties
	 */
	public ResourceRequestByProductUri( URI uri, Map<String, Serializable> properties ){
	    super(properties);
	    this.name = GET_RESOURCE_BY_PRODUCT_URI;
	    this.uri = uri;
	}

	@Override
	public String getAttributeName() {
		return name;
	}

	@Override
	public URI getAttributeValue() {
		return uri;
	}

}
