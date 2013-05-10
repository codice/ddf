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
package ddf.catalog.plugin;

import ddf.catalog.data.Metacard;

/**
 * 
 * {@link Exception} representing a situation where a Catalog Plugin has
 * executed successfully but will not allow an operation to continue.
 * 
 * <br/>
 * An example use of would be for a {@link PreIngestPlugin} to throw this
 * exception if a {@link Metacard} does not validate against a particular
 * schema.
 * 
 * @author michael.menousek@lmco.com
 */
public class StopProcessingException extends Exception {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -7229209585269709649L;

	public StopProcessingException(String string) {
		super(string);
	}


}
