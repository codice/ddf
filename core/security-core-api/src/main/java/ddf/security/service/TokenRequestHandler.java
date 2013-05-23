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
package ddf.security.service;

import javax.servlet.http.HttpServletRequest;


/**
 * 
 * Interface used to decouple the implementations of various SSOs from the
 * endpoints which need to create tokens.
 * 
 */
public interface TokenRequestHandler
{

    /**
     * Creates a token from an incoming HttpServletRequest. This token is then
     * used by the {@link SecurityManager} to create a
     * {@link ddf.security.Subject} that will be used by the security framework.
     * 
     * @param request HttpServletRequest that contains information from which a
     *            token can be created.
     * @return The token object is specific to instances of the SecurityManager,
     *         but common ones are
     *         {@link org.apache.shiro.authc.AuthenticationToken} and
     *         {@link org.apache.cxf.ws.security.tokenstore.SecurityToken}.
     * @throws SecurityServiceException If the request does not contain enough
     *             information to create a token.
     */
    public Object createToken( HttpServletRequest request ) throws SecurityServiceException;

}
