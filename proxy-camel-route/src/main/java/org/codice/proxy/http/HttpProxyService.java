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

/**
 * Http Proxy service which creates a Camel based http proxy
 */
public interface HttpProxyService {
	
	/**
	 * Creates and starts a proxy given only a target Uri. Generated Endpoint name will be returned.
	 * @param targetUri
	 * @return
	 * @throws Exception
	 */
	public String startProxy(String targetUri) throws Exception;
	
	
	/**
	 * Creates and starts a proxy given an endpoint name and target Uri. Endpoint name will be returned.
	 * @param endpointName
	 * @param targetUri
	 * @return
	 * @throws Exception
	 */
	public String startProxy(String endpointName, String targetUri) throws Exception;
	
	
	/**
	 * Stops and destroys the proxy.
	 * @param endpointName
	 * @throws Exception
	 */
	public void stopProxy(String endpointName) throws Exception;
}

