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

import java.io.InputStream;

import javax.activation.MimeType;

import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transform.QueryResponseTransformer;


/**
 * The BinaryContent is used to return content as an {@link InputStream} that has been transformed
 * by a Transformer
 * 
 * @see InputTransformer
 * @see QueryResponseTransformer
 * 
 * @deprecated superceded by ddf.catalog.data.BinaryContent 
 * 
 * @author LMCO
 */
@Deprecated 
public interface BinaryContent {

	/**
	 * Gets the {@link InputStream}
	 *
	 * @return the InputStream
	 */
	public InputStream getInputStream();

	/**
	 * Gets the {@link MimeType}.
	 *
	 * @return the MimeType
	 */
	public MimeType getMimeType();
	
	/**
	 * Get the size if known.
	 * 
	 * @return the size in bytes, -1 if unknown
	 */
	public long getSize();

}