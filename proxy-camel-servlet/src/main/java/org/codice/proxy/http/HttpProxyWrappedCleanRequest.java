/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/

package org.codice.proxy.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.lang.StringUtils;


/**
 * Wrapper for the HttpServletRequest which cleans up bad query strings
 * @author ddf
 *
 */
public class HttpProxyWrappedCleanRequest extends HttpServletRequestWrapper {
	
	/**
	 * Creates a new request wrapper that will clean up bad query parameters and mapped parameters.
	 * 
	 * @param request
	 */
	public HttpProxyWrappedCleanRequest(final HttpServletRequest request)
	{
	    super(request);
	}
	
	@Override
	public String getQueryString() {
		String newQueryString = null;
		String oldQueryString = super.getQueryString();
		if (!(oldQueryString == null)) {
			//If query string ends with an apersand, take it off
			if (StringUtils.endsWith(oldQueryString, "&")) {
				newQueryString = StringUtils.chop(oldQueryString);
			}
		}
		return newQueryString;
	}
}
