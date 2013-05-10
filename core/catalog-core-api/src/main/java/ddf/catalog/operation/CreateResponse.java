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

import java.util.List;

import ddf.catalog.data.Metacard;

/**
 * CreateResponse contains response information that should ber
 * returned on a {@link CreateRequest} operation.
 */
public interface CreateResponse extends Response<CreateRequest>{
	
	/**
	 * Gets the metacards returned from a create operation.
	 *
	 * @return the created metacards
	 */
	public List<Metacard> getCreatedMetacards();
}
