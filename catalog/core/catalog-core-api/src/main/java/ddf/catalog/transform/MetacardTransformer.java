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

import ddf.catalog.Constants;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;

/**
 * The MetacardTransfomer is used to transform a {@link Metacard} to a provided
 * {@link BinaryContent}. The purpose of a {@code MetacardTransformer} is to change the
 * format of the {@link Metacard}. For example if the {@link Metacard} content is in an XML format,
 * then a {@code MetacardTransformer} implementation can be used to transform the
 * {@link Metacard} content into an HTML format.
 * <p>
 * <b>Implementations of this interface <em>must</em>:</b>
 * <ul>
 * <li/>Register with the OSGi Service Registry using the
 * {@code MetacardTransformer} interface.
 * <li/>Include a Service property with name "id" ({@link Constants#SERVICE_ID})
 * and a {@link String} value uniquely identifying the particular implementation
 * 
 * </ul>
 * </p>
 * 
 * @author ddf.isgs@lmco.com
 */
public interface MetacardTransformer {

	/**
	 * Transforms the provided {@link Metacard} into a {@link BinaryContent}
	 * 
	 * @param metacard the {@link Metacard} to be transformed
	 * @param arguments any arguments to be used in the transformation. Keys are
	 *            specific to each {@link MetacardTransformer} implementation
	 * 
	 * @return {@link BinaryContent} the result of the {@link Metacard}
	 *         transformation
	 * @throws CatalogTransformerException if the {@link Metacard} can not be transformed
	 */
	public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments) throws CatalogTransformerException;
	
}
