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
package ddf.catalog.validation;

import java.util.List;

/**
 * @author Michael Menousek, Lockheed Martin
 * @author Shaun Morris, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public abstract class ValidationException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ValidationException(String message)
	{
		super(message);
	}
	
	/**
	 * Get a list of all of the assertion and report error messages in human-readable plain text.
	 * 
	 * @return list of error strings
	 */
	public abstract List<String> getErrors();
	
	
	/**
	 * Get a list of all of the assertion and report warning messages human-readable plain text.
	 * 
	 * @return list of warning strings
	 */
	public abstract List<String> getWarnings();
}
