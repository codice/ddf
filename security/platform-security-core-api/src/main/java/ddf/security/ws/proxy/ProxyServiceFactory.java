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
package ddf.security.ws.proxy;

import java.io.Serializable;

import javax.xml.namespace.QName;

/**
 * Factory for creating proxy services for Service Endpoint Interfaces (SEIs)
 * @since 2.3.0
 * 
 */
public interface ProxyServiceFactory {

	/**
	 * 
	 * @param requiresCredentials
	 *            true if credentials will be required and verified
	 * @param serviceClass
	 *            the class of the type of the Service Endpoint Interface (SEI)
	 * @param serviceName
	 *            the QName of the WSDL service the proxy implements
	 * @param endpointName
	 *            the QName of the endpoint
	 * @param endpointAddress
	 *            address of actual SOAP endpoint
	 * @param dataObject
	 *            any other properties necessary for proxy such as security
	 *            objects or assertions
	 * @return instance of the type of the Service Endpoint Interface (SEI)
	 * @throws UnsupportedOperationException
	 *             if the implementation cannot verify the credentials requested
	 *             in the {@code requiresCredentials} param
	 */
	public <ProxyServiceType> ProxyServiceType create(
			boolean requiresCredentials, Class<ProxyServiceType> serviceClass,
			QName serviceName, QName endpointName, String endpointAddress,
			Serializable dataObject) throws UnsupportedOperationException;

}
