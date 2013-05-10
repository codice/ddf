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
package ddf.catalog.transform;

import java.io.Serializable;
import java.util.Map;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Result;
import ddf.catalog.operation.SourceResponse;

/**
 * The {@code QueryResponseTransformer} is used to transform a list of
 * {@link Result} objects from a {@link SourceResponse}. For example, if the
 * list of results contains XML data, the entire list can be transformed to HTML
 * data.
 * 
 * @author ddf.isgs@lmco.com
 */
public interface QueryResponseTransformer {

	/**
	 * Transforms the list of results into the {@link BinaryContent}.
	 * 
	 * @param arguments the arguments that may be used to execute the transform
	 * @return the transformed content
	 * @throws CatalogTransformerException if the response cannot be transformed
	 */
	public BinaryContent transform(SourceResponse upstreamResponse,
			Map<String, Serializable> arguments) throws CatalogTransformerException;
}
