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
package ddf.catalog.resource;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * 
 * The ResourceWriter is used to store a {@link Resource}.
 * 
 * @author michael.menousek@lmco.com
 */

public interface ResourceWriter {

	/**
	 * Stores the {@link Resource} with optional arguments
	 * 
	 * @param resource the {@link Resource} to store
	 * @param arguments optional arguments associated with the operation
	 * @return the {@link URI} associated with the {@link Resource}
	 * @throws {@link ResourceNotSupportedException} thrown if this writer does not support the type of {@link Resource}
	 * @throws {@link IOException} thrown typically when there is an issue storing the {@link Resource}
	 */
	public URI storeResource(Resource resource, Map<String, Object> arguments) throws ResourceNotSupportedException, IOException;

	/**
	 * Stores the {@link Resource} using the supplied id and optional arguments
	 * 
	 * @param resource the {@link Resource} to store
	 * @param id the id with which to identify the {@link Resource}
	 * @param arguments optional arguments associated with the operation
	 * @return the {@link URI} associated with the {@link Resource}
	 * @throws {@link ResourceNotSupportedException} thrown if this writer does not support the {@link Resource}
	 * @throws {@link IOException} thrown typically when there is an issue storing the {@link Resource}
	 */
	public URI storeResource(Resource resource, String id, Map<String, Object> arguments) throws ResourceNotSupportedException, IOException;

	/**
	 * Deletes the {@link Resource}.
	 * 
	 * @param uri the {@link URI} to obtain the {@link Resource}
	 * @param arguments optional arguments associated with the operation
	 * @throws {@link ResourceNotSupportedException} thrown if this writer does not support the {@link Resource}
	 * @throws {@link IOException} thrown typically when there is an issue accessing the {@link Resource} file
	 */
	public void deleteResource(URI uri, Map<String, Object> arguments)
			throws ResourceNotFoundException, IOException;
}